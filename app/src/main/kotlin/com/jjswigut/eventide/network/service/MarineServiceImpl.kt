package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.data.models.BuoyConditions
import com.jjswigut.eventide.data.models.MarineAlert
import com.jjswigut.eventide.data.models.MarineConditions
import com.jjswigut.eventide.data.models.MarineObservation
import com.jjswigut.eventide.network.client.MarineServiceClient
import com.jjswigut.eventide.network.responses.NwsAlertsResponse
import com.jjswigut.eventide.network.utils.Either
import com.jjswigut.eventide.utils.GenericError
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MarineServiceImpl(
    private val client: MarineServiceClient,
    private val clock: Clock = Clock.systemUTC(),
) : MarineService {
    private val activeStationsMutex = Mutex()
    private var activeStationsCache: ActiveStationsCache? = null

    override suspend fun getMarineConditions(
        stationId: String,
        latitude: Double,
        longitude: Double,
    ): Either<MarineConditions, GenericError> = coroutineScope {
        val coopsDeferred = async { fetchCoopsObservations(stationId) }
        val alertsDeferred = async { fetchAlerts(latitude, longitude) }
        val buoyDeferred = async { fetchNearestBuoy(latitude, longitude) }

        Either.success(
            MarineConditions(
                observations = coopsDeferred.await(),
                alerts = alertsDeferred.await(),
                buoy = buoyDeferred.await(),
            ),
        )
    }

    private suspend fun fetchCoopsObservations(stationId: String): List<MarineObservation> = coroutineScope {
        COOPS_PRODUCTS.map { product ->
            async {
                runCatching {
                    val text = client.getCoopsLatestObservation(
                        stationId = stationId,
                        product = product.product,
                    ).bodyAsText()
                    product.toObservation(text)
                }.getOrNull()
            }
        }.mapNotNull { it.await() }
    }

    private suspend fun fetchAlerts(
        latitude: Double,
        longitude: Double,
    ): List<MarineAlert> {
        return runCatching {
            client.getActiveAlerts(latitude, longitude)
                .body<NwsAlertsResponse>()
                .features
                .mapNotNull { feature ->
                    val event = feature.properties.event?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    MarineAlert(
                        event = event,
                        headline = feature.properties.headline?.takeIf { it.isNotBlank() } ?: event,
                        severity = feature.properties.severity?.takeIf { it.isNotBlank() } ?: "Unknown",
                        effectiveAt = parseNwsTimestamp(feature.properties.onset)
                            ?: parseNwsTimestamp(feature.properties.effective),
                        expiresAt = parseNwsTimestamp(feature.properties.ends)
                            ?: parseNwsTimestamp(feature.properties.expires),
                    )
                }
                .filterNot { alert -> alert.expiresAt?.isBefore(Instant.now(clock)) == true }
                .take(MAX_ALERTS)
        }.getOrDefault(emptyList())
    }

    private suspend fun fetchNearestBuoy(
        latitude: Double,
        longitude: Double,
    ): BuoyConditions? {
        return runCatching {
            val activeStations = getActiveStations()
            val nearest = activeStations
                .filter { it.hasMetData }
                .map { station ->
                    station to distanceMiles(latitude, longitude, station.latitude, station.longitude)
                }
                .filter { (_, distance) -> distance <= MAX_BUOY_DISTANCE_MILES }
                .minByOrNull { (_, distance) -> distance }
                ?: return null

            val station = nearest.first.copy(id = nearest.first.id.uppercase(Locale.US))
            val latestText = client.getNdbcRealtimeText(station.id).bodyAsText()
            parseNdbcRealtimeText(
                station = station,
                distanceMiles = nearest.second,
                text = latestText,
            )
        }.getOrNull()
    }

    private suspend fun getActiveStations(): List<NdbcStation> {
        val now = Instant.now(clock)
        return activeStationsMutex.withLock {
            activeStationsCache?.takeIf { now.isBefore(it.expiresAt) }?.let { return@withLock it.stations }

            val fallback = activeStationsCache?.stations.orEmpty()
            val stations = runCatching {
                parseActiveStations(client.getNdbcActiveStations().bodyAsText())
            }.getOrDefault(fallback)

            if (stations.isNotEmpty()) {
                activeStationsCache = ActiveStationsCache(
                    stations = stations,
                    expiresAt = now.plusSeconds(ACTIVE_STATIONS_CACHE_SECONDS),
                )
            }
            stations
        }
    }

    companion object {
        private const val MAX_ALERTS = 3
        private const val MAX_BUOY_DISTANCE_MILES = 150.0
        private const val ACTIVE_STATIONS_CACHE_SECONDS = 12 * 60 * 60L

        private val json = Json {
            ignoreUnknownKeys = true
        }
        private val coopsTimestampFormatters = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
        )

        private val COOPS_PRODUCTS = listOf(
            CoopsProduct("water_temperature", "Water") { data -> data.stringValue("v")?.formatObservationValue("°F") },
            CoopsProduct("air_temperature", "Air") { data -> data.stringValue("v")?.formatObservationValue("°F") },
            CoopsProduct("wind", "CO-OPS wind") { value -> formatCoopsWind(value) },
            CoopsProduct("air_pressure", "Pressure") { data -> data.stringValue("v")?.formatObservationValue("mb") },
            CoopsProduct("visibility", "Visibility") { data -> data.stringValue("v")?.formatObservationValue("nmi") },
            CoopsProduct("humidity", "Humidity") { data -> data.stringValue("v")?.formatObservationValue("%") },
            CoopsProduct("salinity", "Salinity") { data -> data.stringValue("v")?.formatObservationValue("psu") },
        )

        internal fun parseActiveStations(xml: String): List<NdbcStation> {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            runCatching {
                documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)
                documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val document = documentBuilderFactory
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray()))
            val stationNodes = document.getElementsByTagName("station")
            return (0 until stationNodes.length).mapNotNull { index ->
                val element = stationNodes.item(index) as? Element ?: return@mapNotNull null
                NdbcStation(
                    id = element.getAttribute("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                    name = element.getAttribute("name").takeIf { it.isNotBlank() } ?: element.getAttribute("id"),
                    latitude = element.getAttribute("lat").toDoubleOrNull() ?: return@mapNotNull null,
                    longitude = element.getAttribute("lon").toDoubleOrNull() ?: return@mapNotNull null,
                    hasMetData = element.getAttribute("met").equals("y", ignoreCase = true),
                )
            }
        }

        internal fun parseNdbcRealtimeText(
            station: NdbcStation,
            distanceMiles: Double,
            text: String,
        ): BuoyConditions? {
            val lines = text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()
            val header = lines.firstOrNull { it.startsWith("#YY") || it.startsWith("#yr") }
                ?.removePrefix("#")
                ?.trim()
                ?.split(Regex("\\s+"))
                ?: return null
            val latestData = lines.firstOrNull { !it.startsWith("#") }
                ?.split(Regex("\\s+"))
                ?: return null

            fun value(column: String): Double? {
                val index = header.indexOf(column)
                if (index == -1) return null
                return latestData.getOrNull(index)
                    ?.toDoubleOrNull()
                    ?.takeUnless { it.isMissingNdbcValue(column) }
            }

            val windSpeed = value("WSPD")
            val gust = value("GST")
            val windDirection = value("WDIR")
            val wind = windSpeed?.let {
                buildString {
                    append("${windDirection?.roundToInt()?.let { direction -> "$direction° " } ?: ""}${it.metersPerSecondToKnots()}")
                    gust?.let { gustSpeed -> append(" gust ${gustSpeed.metersPerSecondToKnots()}") }
                }
            }

            return BuoyConditions(
                stationId = station.id,
                stationName = station.name,
                distanceMiles = distanceMiles,
                wind = wind,
                waveHeight = value("WVHT")?.metersToFeet()?.formatFeet(),
                wavePeriod = value("DPD")?.formatSeconds(),
                waterTemperature = value("WTMP")?.celsiusToFahrenheit()?.formatFahrenheit(),
                pressure = value("PRES")?.formatPressure(),
                observedAt = parseNdbcTimestamp(header, latestData),
            ).takeIf {
                it.wind != null ||
                    it.waveHeight != null ||
                    it.wavePeriod != null ||
                    it.waterTemperature != null ||
                    it.pressure != null
            }
        }

        internal fun parseCoopsTimestamp(value: String?): Instant? {
            val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
            return coopsTimestampFormatters.firstNotNullOfOrNull { formatter ->
                runCatching {
                    LocalDateTime.parse(trimmed, formatter).toInstant(ZoneOffset.UTC)
                }.getOrNull()
            } ?: parseNwsTimestamp(trimmed)
        }

        internal fun parseNdbcTimestamp(
            header: List<String>,
            row: List<String>,
        ): Instant? {
            fun token(vararg names: String): String? {
                val exactIndex = header.indexOfFirst { headerName ->
                    names.any { name -> headerName == name }
                }
                val index = exactIndex.takeIf { it >= 0 } ?: header.indexOfFirst { headerName ->
                    names.any { name -> headerName.equals(name, ignoreCase = true) }
                }
                return row.getOrNull(index.takeIf { it >= 0 } ?: return null)
            }

            val year = token("YY", "yr")?.toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: return null
            val month = token("MM", "mo")?.toIntOrNull() ?: return null
            val day = token("DD", "dy")?.toIntOrNull() ?: return null
            val hour = token("hh", "hr")?.toIntOrNull() ?: return null
            val minute = token("mm", "mn")?.toIntOrNull() ?: return null

            return runCatching {
                LocalDateTime.of(year, month, day, hour, minute)
                    .toInstant(ZoneOffset.UTC)
            }.getOrNull()
        }

        internal fun parseNwsTimestamp(value: String?): Instant? {
            val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
            return runCatching { Instant.parse(trimmed) }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(trimmed).toInstant() }.getOrNull()
        }

        private fun CoopsProduct.toObservation(text: String): MarineObservation? {
            val data = runCatching {
                json.parseToJsonElement(text)
                    .jsonObject["data"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
            }.getOrNull() ?: return null
            val value = formatter(data) ?: return null
            return MarineObservation(
                label = label,
                value = value,
                observedAt = parseCoopsTimestamp(data.stringValue("t")),
            )
        }

        private fun formatCoopsWind(data: JsonObject): String? {
            val speed = data.stringValue("s") ?: return null
            val direction = data.stringValue("dr") ?: data.stringValue("d")?.let { "$it°" }
            val gust = data.stringValue("g")
            return buildString {
                direction?.let {
                    append(it)
                    append(" ")
                }
                append(speed.formatObservationValue("kt"))
                gust?.let {
                    append(" gust ")
                    append(it.formatObservationValue("kt"))
                }
            }
        }

        private fun JsonObject.stringValue(key: String): String? {
            val primitive = this[key] as? JsonPrimitive ?: return null
            return primitive.jsonPrimitive.content.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        }

        private fun String.formatObservationValue(unit: String): String? {
            val value = toDoubleOrNull() ?: return null
            return "${value.formatOneDecimal()}$unit"
        }

        private fun distanceMiles(
            startLatitude: Double,
            startLongitude: Double,
            endLatitude: Double,
            endLongitude: Double,
        ): Double {
            val earthRadiusMiles = 3958.8
            val dLat = Math.toRadians(endLatitude - startLatitude)
            val dLng = Math.toRadians(endLongitude - startLongitude)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(startLatitude)) * cos(Math.toRadians(endLatitude)) *
                sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusMiles * c
        }

        private fun Double.metersToFeet(): Double = this * 3.28084

        private fun Double.metersPerSecondToKnots(): String = "${(this * 1.94384).formatOneDecimal()}kt"

        private fun Double.celsiusToFahrenheit(): Double = (this * 9 / 5) + 32

        private fun Double.formatFeet(): String = "${formatOneDecimal()}ft"

        private fun Double.formatSeconds(): String = "${formatOneDecimal()}s"

        private fun Double.formatFahrenheit(): String = "${formatOneDecimal()}°F"

        private fun Double.formatPressure(): String = "${formatOneDecimal()}hPa"

        private fun Double.formatOneDecimal(): String = String.format(Locale.US, "%.1f", this)

        private fun Double.isMissingNdbcValue(column: String): Boolean {
            return when (column) {
                "WDIR", "MWD" -> this >= 999.0
                "PRES" -> this >= 9999.0
                else -> this >= 99.0 || this <= -99.0
            }
        }
    }
}

internal data class NdbcStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val hasMetData: Boolean,
)

private data class ActiveStationsCache(
    val stations: List<NdbcStation>,
    val expiresAt: Instant,
)

private data class CoopsProduct(
    val product: String,
    val label: String,
    val formatter: (JsonObject) -> String?,
)
