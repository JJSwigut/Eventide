package com.jjswigut.eventide.astronomy

import com.google.android.gms.maps.model.LatLng
import com.jjswigut.eventide.data.models.MoonPhase
import com.jjswigut.eventide.data.models.SunMoonData
import com.jjswigut.eventide.data.models.TideDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object AstronomyCalculator {
    fun calculate(
        date: LocalDate,
        location: LatLng,
    ): SunMoonData {
        return SunMoonData(
            sunrise = calculateSunTime(
                date = date,
                latitude = location.latitude,
                longitude = location.longitude,
                event = SolarEvent.Sunrise,
            ),
            sunset = calculateSunTime(
                date = date,
                latitude = location.latitude,
                longitude = location.longitude,
                event = SolarEvent.Sunset,
            ),
            moonPhase = calculateMoonPhase(date),
            moonIlluminationPercent = calculateMoonIlluminationPercent(date),
        )
    }

    fun calculateMoonPhase(date: LocalDate): MoonPhase {
        val phase = normalizedMoonPhase(date)
        return when {
            phase < ONE_SIXTEENTH || phase >= FIFTEEN_SIXTEENTHS -> MoonPhase.New
            phase < THREE_SIXTEENTHS -> MoonPhase.WaxingCrescent
            phase < FIVE_SIXTEENTHS -> MoonPhase.FirstQuarter
            phase < SEVEN_SIXTEENTHS -> MoonPhase.WaxingGibbous
            phase < NINE_SIXTEENTHS -> MoonPhase.Full
            phase < ELEVEN_SIXTEENTHS -> MoonPhase.WaningGibbous
            phase < THIRTEEN_SIXTEENTHS -> MoonPhase.LastQuarter
            else -> MoonPhase.WaningCrescent
        }
    }

    fun calculateMoonIlluminationPercent(date: LocalDate): Int {
        val phase = normalizedMoonPhase(date)
        val illumination = (1 - cos(TWO_PI * phase)) / 2
        return (illumination * 100).roundToInt().coerceIn(0, 100)
    }

    private fun calculateSunTime(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        event: SolarEvent,
    ): LocalTime? {
        val dayOfYear = date.dayOfYear
        val longitudeHour = longitude / DEGREES_PER_HOUR
        val approximateTime = dayOfYear + ((event.approximateHour - longitudeHour) / HOURS_PER_DAY)
        val meanAnomaly = (SOLAR_MEAN_ANOMALY_RATE * approximateTime) - SOLAR_MEAN_ANOMALY_OFFSET
        val trueLongitude = normalizeDegrees(
            meanAnomaly +
                (1.916 * sinDegrees(meanAnomaly)) +
                (0.020 * sinDegrees(2 * meanAnomaly)) +
                SOLAR_LONGITUDE_OFFSET,
        )
        val rightAscension = adjustedRightAscension(trueLongitude)
        val sinDeclination = 0.39782 * sinDegrees(trueLongitude)
        val cosDeclination = sqrt(1 - (sinDeclination * sinDeclination))
        val cosHourAngle = (
            cosDegrees(OFFICIAL_ZENITH) -
                (sinDeclination * sinDegrees(latitude))
            ) / (cosDeclination * cosDegrees(latitude))

        if (cosHourAngle > 1 || cosHourAngle < -1) return null

        val hourAngleDegrees = when (event) {
            SolarEvent.Sunrise -> 360 - acosDegrees(cosHourAngle)
            SolarEvent.Sunset -> acosDegrees(cosHourAngle)
        }
        val hourAngle = hourAngleDegrees / DEGREES_PER_HOUR
        val localMeanTime = hourAngle + rightAscension - (0.06571 * approximateTime) - 6.622
        val utcTime = normalizeHours(localMeanTime - longitudeHour)
        val localTime = utcTime + estimatedUtcOffsetHours(date, latitude, longitude)
        val minutesOfDay = normalizeMinutes((localTime * MINUTES_PER_HOUR).roundToInt())

        return LocalTime.of(minutesOfDay / MINUTES_PER_HOUR, minutesOfDay % MINUTES_PER_HOUR)
    }

    private fun adjustedRightAscension(trueLongitude: Double): Double {
        val rawRightAscension = normalizeDegrees(atanDegrees(0.91764 * tanDegrees(trueLongitude)))
        val longitudeQuadrant = floor(trueLongitude / RIGHT_ANGLE_DEGREES) * RIGHT_ANGLE_DEGREES
        val rightAscensionQuadrant = floor(rawRightAscension / RIGHT_ANGLE_DEGREES) * RIGHT_ANGLE_DEGREES
        return (rawRightAscension + longitudeQuadrant - rightAscensionQuadrant) / DEGREES_PER_HOUR
    }

    private fun estimatedUtcOffsetHours(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
    ): Int {
        val standardOffset = when {
            longitude <= -169.0 -> -11
            longitude <= -153.0 -> -10
            longitude <= -129.0 -> -9
            longitude <= -114.0 -> -8
            longitude <= -99.0 -> -7
            longitude <= -84.0 -> -6
            longitude <= -66.0 -> -5
            longitude <= -50.0 -> -4
            else -> (longitude / DEGREES_PER_HOUR).roundToInt().coerceIn(-12, 14)
        }

        return if (observesUsDaylightTime(latitude, longitude) && date.isUsDaylightTimeDate()) {
            standardOffset + 1
        } else {
            standardOffset
        }
    }

    private fun observesUsDaylightTime(
        latitude: Double,
        longitude: Double,
    ): Boolean {
        val isHawaii = latitude in 18.0..23.0 && longitude in -161.0..-154.0
        val isPuertoRico = latitude in 17.0..19.0 && longitude in -68.0..-64.0
        val isGuamOrMarianas = latitude in 13.0..22.0 && longitude in 144.0..147.0
        val isAmericanSamoa = latitude in -15.0..-10.0 && longitude in -172.0..-168.0

        return !isHawaii && !isPuertoRico && !isGuamOrMarianas && !isAmericanSamoa
    }

    private fun LocalDate.isUsDaylightTimeDate(): Boolean {
        val start = nthWeekdayOfMonth(year, Month.MARCH, DayOfWeek.SUNDAY, 2)
        val end = nthWeekdayOfMonth(year, Month.NOVEMBER, DayOfWeek.SUNDAY, 1)
        return this >= start && this < end
    }

    private fun nthWeekdayOfMonth(
        year: Int,
        month: Month,
        dayOfWeek: DayOfWeek,
        occurrence: Int,
    ): LocalDate {
        val firstOfMonth = LocalDate.of(year, month, 1)
        val daysUntilWeekday = Math.floorMod(
            dayOfWeek.value - firstOfMonth.dayOfWeek.value,
            DAYS_PER_WEEK,
        )
        return firstOfMonth.plusDays(daysUntilWeekday.toLong() + ((occurrence - 1) * DAYS_PER_WEEK))
    }

    private fun normalizedMoonPhase(date: LocalDate): Double {
        val daysSinceKnownNewMoon = ChronoUnit.MINUTES.between(KNOWN_NEW_MOON, date.atTime(12, 0)) /
            MINUTES_PER_DAY
        return positiveModulo(daysSinceKnownNewMoon, SYNODIC_MONTH_DAYS) / SYNODIC_MONTH_DAYS
    }

    private fun normalizeDegrees(value: Double): Double = positiveModulo(value, FULL_CIRCLE_DEGREES)

    private fun normalizeHours(value: Double): Double = positiveModulo(value, HOURS_PER_DAY)

    private fun normalizeMinutes(value: Int): Int = Math.floorMod(value, MINUTES_PER_DAY_INT)

    private fun positiveModulo(
        value: Double,
        modulus: Double,
    ): Double = ((value % modulus) + modulus) % modulus

    private fun sinDegrees(value: Double): Double = sin(Math.toRadians(value))

    private fun cosDegrees(value: Double): Double = cos(Math.toRadians(value))

    private fun tanDegrees(value: Double): Double = tan(Math.toRadians(value))

    private fun atanDegrees(value: Double): Double = Math.toDegrees(kotlin.math.atan(value))

    private fun acosDegrees(value: Double): Double = Math.toDegrees(acos(value))

    private enum class SolarEvent(val approximateHour: Double) {
        Sunrise(6.0),
        Sunset(18.0),
    }

    private val KNOWN_NEW_MOON = LocalDate.of(2000, 1, 6).atTime(18, 14)

    private const val SYNODIC_MONTH_DAYS = 29.530588853
    private const val OFFICIAL_ZENITH = 90.833
    private const val SOLAR_MEAN_ANOMALY_RATE = 0.9856
    private const val SOLAR_MEAN_ANOMALY_OFFSET = 3.289
    private const val SOLAR_LONGITUDE_OFFSET = 282.634
    private const val RIGHT_ANGLE_DEGREES = 90.0
    private const val FULL_CIRCLE_DEGREES = 360.0
    private const val DEGREES_PER_HOUR = 15.0
    private const val HOURS_PER_DAY = 24.0
    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 1440.0
    private const val MINUTES_PER_DAY_INT = 1440
    private const val DAYS_PER_WEEK = 7
    private const val TWO_PI = 2 * PI
    private const val ONE_SIXTEENTH = 1.0 / 16.0
    private const val THREE_SIXTEENTHS = 3.0 / 16.0
    private const val FIVE_SIXTEENTHS = 5.0 / 16.0
    private const val SEVEN_SIXTEENTHS = 7.0 / 16.0
    private const val NINE_SIXTEENTHS = 9.0 / 16.0
    private const val ELEVEN_SIXTEENTHS = 11.0 / 16.0
    private const val THIRTEEN_SIXTEENTHS = 13.0 / 16.0
    private const val FIFTEEN_SIXTEENTHS = 15.0 / 16.0
}

fun List<TideDay>.withSunMoonData(location: LatLng): List<TideDay> {
    return map { tideDay ->
        val date = tideDay.dateValue ?: return@map tideDay
        tideDay.copy(sunMoonData = AstronomyCalculator.calculate(date, location))
    }
}
