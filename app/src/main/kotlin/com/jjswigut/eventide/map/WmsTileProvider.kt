package com.jjswigut.eventide.map

import android.net.Uri
import com.google.android.gms.maps.model.UrlTileProvider
import java.net.URL

class WmsTileProvider(
    private val baseUrl: String,
    private val layers: String,
) : UrlTileProvider(TILE_SIZE_PIXELS, TILE_SIZE_PIXELS) {

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
        val bounds = WebMercatorTileBounds.from(x, y, zoom)
        val uri = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("SERVICE", "WMS")
            .appendQueryParameter("VERSION", "1.3.0")
            .appendQueryParameter("REQUEST", "GetMap")
            .appendQueryParameter("FORMAT", "image/png")
            .appendQueryParameter("TRANSPARENT", "true")
            .appendQueryParameter("LAYERS", layers)
            .appendQueryParameter("STYLES", "")
            .appendQueryParameter("CRS", "EPSG:3857")
            .appendQueryParameter("BBOX", bounds.toBboxParameter())
            .appendQueryParameter("WIDTH", TILE_SIZE_PIXELS.toString())
            .appendQueryParameter("HEIGHT", TILE_SIZE_PIXELS.toString())
            .build()

        return URL(uri.toString())
    }
}

private data class WebMercatorTileBounds(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double,
) {
    fun toBboxParameter(): String = "$minX,$minY,$maxX,$maxY"

    companion object {
        fun from(x: Int, y: Int, zoom: Int): WebMercatorTileBounds {
            val tileCount = 1 shl zoom
            val tileSpan = WEB_MERCATOR_WORLD_WIDTH / tileCount
            val minX = (x * tileSpan) - WEB_MERCATOR_EXTENT
            val maxX = ((x + 1) * tileSpan) - WEB_MERCATOR_EXTENT
            val maxY = WEB_MERCATOR_EXTENT - (y * tileSpan)
            val minY = WEB_MERCATOR_EXTENT - ((y + 1) * tileSpan)

            return WebMercatorTileBounds(minX, minY, maxX, maxY)
        }
    }
}

private const val TILE_SIZE_PIXELS = 256
private const val WEB_MERCATOR_EXTENT = 20_037_508.342789244
private const val WEB_MERCATOR_WORLD_WIDTH = WEB_MERCATOR_EXTENT * 2
