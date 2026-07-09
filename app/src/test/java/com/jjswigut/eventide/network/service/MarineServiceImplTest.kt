package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.network.client.MarineServiceClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class MarineServiceImplTest {
    @Test
    fun `parseActiveStations reads station metadata`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <stations>
              <station id="44060" lat="41.263" lon="-72.067" name="Eastern Long Island Sound" met="y"/>
              <station id="skip" lat="" lon="-72.067" name="Broken" met="y"/>
            </stations>
        """.trimIndent()

        val stations = MarineServiceImpl.parseActiveStations(xml)

        assertEquals(1, stations.size)
        assertEquals("44060", stations.first().id)
        assertEquals("Eastern Long Island Sound", stations.first().name)
        assertEquals(41.263, stations.first().latitude, 0.0)
        assertEquals(-72.067, stations.first().longitude, 0.0)
        assertEquals(true, stations.first().hasMetData)
    }

    @Test
    fun `parseNdbcRealtimeText maps latest conditions and ignores sentinels`() {
        val station = NdbcStation(
            id = "44060",
            name = "Eastern Long Island Sound",
            latitude = 41.263,
            longitude = -72.067,
            hasMetData = true,
        )
        val text = """
            #YY  MM DD hh mm WDIR WSPD GST WVHT DPD APD MWD PRES ATMP WTMP DEWP VIS PTDY TIDE
            #yr  mo dy hr mn degT m/s  m/s m    sec sec degT hPa  degC degC degC nmi hPa  ft
            2026 07 07 18 50 220  5.0  7.0 1.2  8   7.5 230  1013.4 19.0 18.1 17.1 99.0 1.2 99.0
        """.trimIndent()

        val buoy = MarineServiceImpl.parseNdbcRealtimeText(
            station = station,
            distanceMiles = 12.4,
            text = text,
        )

        requireNotNull(buoy)
        assertEquals("44060", buoy.stationId)
        assertEquals("220° 9.7kt gust 13.6kt", buoy.wind)
        assertEquals("3.9ft", buoy.waveHeight)
        assertEquals("8.0s", buoy.wavePeriod)
        assertEquals("64.6°F", buoy.waterTemperature)
        assertEquals("1013.4hPa", buoy.pressure)
        assertEquals(Instant.parse("2026-07-07T18:50:00Z"), buoy.observedAt)
    }

    @Test
    fun `parseNdbcRealtimeText returns null when all values are missing`() {
        val station = NdbcStation(
            id = "44060",
            name = "Eastern Long Island Sound",
            latitude = 41.263,
            longitude = -72.067,
            hasMetData = true,
        )
        val text = """
            #YY  MM DD hh mm WDIR WSPD GST WVHT DPD PRES WTMP
            2026 07 07 18 50 999  99.0 99.0 99.0 99 9999.0 99.0
        """.trimIndent()

        val buoy = MarineServiceImpl.parseNdbcRealtimeText(
            station = station,
            distanceMiles = 12.4,
            text = text,
        )

        assertNull(buoy)
    }

    @Test
    fun `parseCoopsTimestamp reads CO-OPS UTC timestamps resiliently`() {
        assertEquals(
            Instant.parse("2026-07-07T18:50:00Z"),
            MarineServiceImpl.parseCoopsTimestamp("2026-07-07 18:50"),
        )
        assertEquals(
            Instant.parse("2026-07-07T18:50:30Z"),
            MarineServiceImpl.parseCoopsTimestamp("2026-07-07 18:50:30"),
        )
        assertNull(MarineServiceImpl.parseCoopsTimestamp("not a timestamp"))
    }

    @Test
    fun `parseNwsTimestamp reads alert offset times`() {
        assertEquals(
            Instant.parse("2026-07-07T18:50:00Z"),
            MarineServiceImpl.parseNwsTimestamp("2026-07-07T14:50:00-04:00"),
        )
        assertNull(MarineServiceImpl.parseNwsTimestamp("not a timestamp"))
    }

    @Test
    fun `getMarineConditions caches NDBC active station metadata`() = runTest {
        val activeStationsRequests = AtomicInteger(0)
        val client = MarineServiceClient(
            MockEngine { request ->
                val url = request.url.toString()
                when {
                    url.endsWith("/activestations.xml") -> {
                        activeStationsRequests.incrementAndGet()
                        respond(
                            content = """
                                <?xml version="1.0" encoding="utf-8"?>
                                <stations>
                                  <station id="44060" lat="41.263" lon="-72.067" name="Eastern Long Island Sound" met="y"/>
                                </stations>
                            """.trimIndent(),
                            headers = headersOf(HttpHeaders.ContentType, "application/xml"),
                        )
                    }
                    url.endsWith("/data/realtime2/44060.txt") -> respond(
                        content = """
                            #YY  MM DD hh mm WDIR WSPD GST WVHT DPD PRES WTMP
                            2026 07 07 18 50 220  5.0 7.0 1.2 8 1013.4 18.1
                        """.trimIndent(),
                    )
                    url.contains("/alerts/active") -> respond(
                        content = """{"features":[]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    url.contains("/api/prod/datagetter") -> respond(
                        content = """{"data":[{"t":"2026-07-07 18:50","v":"64.1"}]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    else -> error("Unexpected URL: $url")
                }
            },
        )
        val service = MarineServiceImpl(client)

        service.getMarineConditions("8461490", 41.3, -72.0)
        service.getMarineConditions("8461490", 41.3, -72.0)

        assertEquals(1, activeStationsRequests.get())
    }
}
