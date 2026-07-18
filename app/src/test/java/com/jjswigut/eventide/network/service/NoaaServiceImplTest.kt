package com.jjswigut.eventide.network.service

import com.jjswigut.eventide.network.client.NoaaServiceClient
import com.jjswigut.eventide.network.utils.Either
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoaaServiceImplTest {
    @Test
    fun `tide request recovers from transient NOAA error payload`() = runTest {
        var requestCount = 0
        val requestedUrls = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestCount += 1
            requestedUrls += request.url.toString()
            respond(
                content = if (requestCount == 1) noaaErrorPayload else predictionsPayload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val service = NoaaServiceImpl(NoaaServiceClient(engine))

        val result = service.getTidesForStation("8461490")

        assertTrue("Expected Eventide to recover from NOAA's transient error, got $result", result is Either.Success)
        result as Either.Success
        assertEquals(2, result.value.predictions.size)
        assertEquals(2, requestCount)
        assertTrue(requestedUrls.first().contains("application=Eventide"))
        assertTrue(requestedUrls.first().contains("end_date="))
        assertTrue(requestedUrls.last().contains("range=192"))
    }

    @Test
    fun `tide request returns failure after bounded NOAA error retries`() = runTest {
        var requestCount = 0
        val engine = MockEngine {
            requestCount += 1
            respond(
                content = noaaErrorPayload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val service = NoaaServiceImpl(NoaaServiceClient(engine))

        val result = service.getTidesForStation("8461490")

        assertTrue(result is Either.Failure)
        assertEquals(3, requestCount)
    }
}

private const val noaaErrorPayload =
    """{"error":{"message":"No Predictions data was found. Please make sure the Datum input is valid."}}"""

private const val predictionsPayload =
    """{"predictions":[{"t":"2026-07-18 00:46","v":"0.937","type":"H"},{"t":"2026-07-18 07:24","v":"0.017","type":"L"}]}"""
