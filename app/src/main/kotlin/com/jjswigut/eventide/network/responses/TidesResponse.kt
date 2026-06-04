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
            )
        }
    }
}

private fun TideDTO.toModel(): Tide {
    return Tide(
        time = LocalDateTime.parse(t, dateTimeParser).format(timeFormatter).lowercase(),
        tideValue = if (type == "H") High else Low,
        height = v.convertMeterStringToFeet(),
    )
}

private fun String.convertMeterStringToFeet(): String {
    return try {
        String.format("%.2fft", (toDouble() * 3.28084))
    } catch (e: Exception) {
        String.format("%.2fm", this)
    }
}

private val dateTimeParser = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")

private val timeFormatter = DateTimeFormatter.ofPattern("h:mma")
