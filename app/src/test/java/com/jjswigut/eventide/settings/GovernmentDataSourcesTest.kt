package com.jjswigut.eventide.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class GovernmentDataSourcesTest {
    @Test
    fun `all government sources use accessible https gov urls`() {
        assertTrue(governmentDataSources.isNotEmpty())

        governmentDataSources.forEach { source ->
            val uri = URI(source.url)
            assertEquals("https", uri.scheme)
            assertTrue("${source.url} must use a .gov host", uri.host.endsWith(".gov"))
            assertTrue(source.name.isNotBlank())
            assertTrue(source.description.isNotBlank())
        }
    }

    @Test
    fun `disclaimer clearly denies government representation and affiliation`() {
        assertTrue(GOVERNMENT_INFORMATION_DISCLAIMER.contains("does not represent"))
        assertTrue(GOVERNMENT_INFORMATION_DISCLAIMER.contains("not affiliated"))
        assertTrue(GOVERNMENT_INFORMATION_DISCLAIMER.contains("government entity"))
    }
}
