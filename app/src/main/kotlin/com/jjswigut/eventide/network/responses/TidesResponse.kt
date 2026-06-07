package com.jjswigut.eventide.network.responses

import com.jjswigut.eventide.data.models.Tide
import com.jjswigut.eventide.data.models.TideDay
import com.jjswigut.eventide.data.models.TideValue.High
import com.jjswigut.eventide.data.models.TideValue.Low
import com.jjswigut.eventide.network.responses.TidesResponse.TideDTO
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class TidesResponse(
    val predictions: List<TideDTO>,
) {
    @Serializable
    data class TideDTO(
        val t: String,
        val type: String,
        val v: String,
    )

    fun toListOfTideDays(): List<TideDay> {
        return predictions.groupBy {
            LocalDate.parse(it.t, dateTimeParser)
        }.map { (date, tideDTOS) ->
            TideDay(
                date = date.format(dayFormatter),
                tides = tideDTOS.map { it.toModel() },
                dateValue = date,
            )
        }
    }
}

private fun TideDTO.toModel(): Tide {
    val dateTime = LocalDateTime.parse(t, dateTimeParser)
    val heightFeet = v.toDoubleOrNull()?.metersToFeet()
    return Tide(
        time = dateTime.format(timeFormatter).lowercase(Locale.US),
        tideValue = if (type == "H") High else Low,
        height = heightFeet?.formatFeet() ?: v.formatMetersFallback(),
        dateTime = dateTime,
        heightFeet = heightFeet,
    )
}

private fun Double.metersToFeet(): Double = this * METERS_TO_FEET

private fun Double.formatFeet(): String = String.format(Locale.US, "%.2fft", this)

private fun String.formatMetersFallback(): String = "${this}m"

private val dateTimeParser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")

private val timeFormatter = DateTimeFormatter.ofPattern("h:mma")

private const val METERS_TO_FEET = 3.28084
