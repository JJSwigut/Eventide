package com.jjswigut.eventide.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.applyCanvas
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.ComposeUiViewRenderer
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberComposeUiViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Groups many items on a map based on zoom level.
 *
 * @param items all items to show
 * @param onClusterClick a lambda invoked when the user clicks a cluster of items
 * @param onClusterItemClick a lambda invoked when the user clicks a non-clustered item
 * @param onClusterItemInfoWindowClick a lambda invoked when the user clicks the info window of a
 * non-clustered item
 * @param onClusterItemInfoWindowLongClick a lambda invoked when the user long-clicks the info
 * window of a non-clustered item
 * @param clusterContent an optional Composable that is rendered for each [Cluster].
 * @param clusterItemContent an optional Composable that is rendered for each non-clustered item.
 */
@Composable
@GoogleMapComposable
@MapsComposeExperimentalApi
fun <T : ClusterItem> EventideClustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    onClusterItemInfoWindowClick: (T) -> Unit = { },
    onClusterItemInfoWindowLongClick: (T) -> Unit = { },
    clusterContent: @[UiComposable Composable] ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @[UiComposable Composable] ((T) -> Unit)? = null,
) {
    val clusterManager = rememberClusterManager(clusterContent, clusterItemContent)

    SideEffect {
        clusterManager ?: return@SideEffect
        clusterManager.setOnClusterClickListener(onClusterClick)
        clusterManager.setOnClusterItemClickListener(onClusterItemClick)
        clusterManager.setOnClusterItemInfoWindowClickListener(onClusterItemInfoWindowClick)
        clusterManager.setOnClusterItemInfoWindowLongClickListener(onClusterItemInfoWindowLongClick)
    }

    if (clusterManager != null) {
        Clustering(
            items = items,
            clusterManager = clusterManager,
        )
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun <T : ClusterItem> rememberClusterManager(
    clusterContent: @Composable ((Cluster<T>) -> Unit)?,
    clusterItemContent: @Composable ((T) -> Unit)?,
    clusterRenderer: ClusterRenderer<T>? = null,
): ClusterManager<T>? {
    val clusterContentState = rememberUpdatedState(clusterContent)
    val clusterItemContentState = rememberUpdatedState(clusterItemContent)
    val context = LocalContext.current
    val viewRendererState = rememberUpdatedState(rememberComposeUiViewRenderer())
    val clusterManagerState: MutableState<ClusterManager<T>?> = remember { mutableStateOf(null) }
    MapEffect(context) { map ->
        val clusterManager = ClusterManager<T>(context, map)

        launch {
            snapshotFlow {
                clusterContentState.value != null || clusterItemContentState.value != null
            }
                .collect { hasCustomContent ->
                    val renderer = clusterRenderer
                        ?: if (hasCustomContent) {
                            ComposeUiClusterRenderer(
                                context,
                                scope = this,
                                map,
                                clusterManager,
                                viewRendererState,
                                clusterContentState,
                                clusterItemContentState,
                            )
                        } else {
                            DefaultClusterRenderer(context, map, clusterManager)
                        }
                    clusterManager.renderer = renderer
                }
        }

        clusterManagerState.value = clusterManager
    }
    return clusterManagerState.value
}

/**
 * Implementation of [ClusterRenderer] that renders marker bitmaps from Compose UI content.
 * [clusterContentState] renders clusters, and [clusterItemContentState] renders non-clustered
 * items.
 */
internal class ComposeUiClusterRenderer<T : ClusterItem>(
    private val context: Context,
    private val scope: CoroutineScope,
    map: GoogleMap,
    clusterManager: ClusterManager<T>,
    private val viewRendererState: State<ComposeUiViewRenderer>,
    private val clusterContentState: State<@Composable ((Cluster<T>) -> Unit)?>,
    private val clusterItemContentState: State<@Composable ((T) -> Unit)?>,
) : DefaultClusterRenderer<T>(
    context,
    map,
    clusterManager,
) {

    private val fakeCanvas = Canvas()
    private val keysToViews = mutableMapOf<ViewKey<T>, ViewInfo>()

    // Cache for rendered bitmaps to avoid recreating identical bitmaps
    private val bitmapCache = mutableMapOf<String, WeakReference<BitmapDescriptor>>()
    private val MAX_CACHE_SIZE = 50

    override fun onClustersChanged(clusters: Set<Cluster<T>>) {
        super.onClustersChanged(clusters)
        val keys = clusters.flatMap { it.computeViewKeys() }

        with(keysToViews.iterator()) {
            forEach { (key, viewInfo) ->
                if (key !in keys) {
                    remove()
                    viewInfo.onRemove()
                }
            }
        }
        keys.forEach { key ->
            if (key !in keysToViews.keys) {
                createAndAddView(key)
            }
        }

        // Clean up bitmap cache if it gets too large
        if (bitmapCache.size > MAX_CACHE_SIZE) {
            // Remove entries with cleared weak references
            bitmapCache.entries.removeIf { it.value.get() == null }
        }
    }

    override fun shouldRenderAsCluster(cluster: Cluster<T>): Boolean {
        return cluster.size >= 2
    }

    /**
     * A [Cluster] is represented by one or more elements on screen. Even if a cluster contains
     * multiple items, it still might only need a single element, depending on
     * [shouldRenderAsCluster].
     * @return a set of [ViewKey]s for each element.
     */
    private fun Cluster<T>.computeViewKeys(): Set<ViewKey<T>> {
        return if (shouldRenderAsCluster(this)) {
            setOf(ViewKey.Cluster(this))
        } else {
            items.mapTo(mutableSetOf()) { ViewKey.Item(it) }
        }
    }

    private fun createAndAddView(key: ViewKey<T>): ViewInfo {
        val view = InvalidatingComposeView(
            context,
            content = when (key) {
                is ViewKey.Cluster -> {
                    { clusterContentState.value?.invoke(key.cluster) }
                }

                is ViewKey.Item -> {
                    { clusterItemContentState.value?.invoke(key.item) }
                }
            },
        )
        val renderHandle = viewRendererState.value.startRenderingView(view)
        val rerenderJob = scope.launch {
            collectInvalidationsAndRerender(key, view)
        }

        val viewInfo = ViewInfo(
            view,
            onRemove = {
                rerenderJob.cancel()
                renderHandle.dispose()
            },
        )
        keysToViews[key] = viewInfo
        return viewInfo
    }

    /** Re-render the corresponding marker whenever [view] invalidates */
    private suspend fun collectInvalidationsAndRerender(
        key: ViewKey<T>,
        view: InvalidatingComposeView,
    ) {
        callbackFlow {
            // When invalidated, emit on the next frame
            var invalidated = false
            view.onInvalidate = {
                if (!invalidated) {
                    launch {
                        awaitFrame()
                        trySend(Unit)
                        invalidated = false
                    }
                    invalidated = true
                }
            }
            view.doOnAttach {
                view.doOnDetach { close() }
            }
            awaitClose()
        }
            .collectLatest {
                val cacheKey = when (key) {
                    is ViewKey.Cluster -> "cluster_${key.cluster.size}"
                    is ViewKey.Item -> "item_${key.item.position.latitude}_${key.item.position.longitude}"
                }

                val marker = when (key) {
                    is ViewKey.Cluster -> getMarker(key.cluster)
                    is ViewKey.Item -> getMarker(key.item)
                }

                marker?.setIcon(renderViewToBitmapDescriptor(view, cacheKey))
            }
    }

    override fun getDescriptorForCluster(cluster: Cluster<T>): BitmapDescriptor {
        return if (clusterContentState.value != null) {
            val cacheKey = "cluster_${cluster.size}"

            // Check cache first
            bitmapCache[cacheKey]?.get()?.let { return it }

            val viewInfo = keysToViews.entries
                .firstOrNull { (key, _) -> (key as? ViewKey.Cluster)?.cluster == cluster }
                ?.value
                ?: createAndAddView(cluster.computeViewKeys().first())

            val descriptor = renderViewToBitmapDescriptor(viewInfo.view, cacheKey)
            bitmapCache[cacheKey] = WeakReference(descriptor)
            descriptor
        } else {
            super.getDescriptorForCluster(cluster)
        }
    }

    override fun onBeforeClusterItemRendered(item: T, markerOptions: MarkerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions)

        if (clusterItemContentState.value != null) {
            val cacheKey = "item_${item.title}_${item.position.latitude}_${item.position.longitude}"

            // Check cache first
            bitmapCache[cacheKey]?.get()?.let {
                markerOptions.icon(it)
                return
            }

            val viewInfo = keysToViews.entries
                .firstOrNull { (key, _) -> (key as? ViewKey.Item)?.item == item }
                ?.value
                ?: createAndAddView(ViewKey.Item(item))

            val descriptor = renderViewToBitmapDescriptor(viewInfo.view, cacheKey)
            bitmapCache[cacheKey] = WeakReference(descriptor)
            markerOptions.icon(descriptor)
        }
    }

    private fun renderViewToBitmapDescriptor(
        view: AbstractComposeView,
        cacheKey: String? = null,
    ): BitmapDescriptor {
        // Check if we already have this cached
        cacheKey?.let { key ->
            bitmapCache[key]?.get()?.let { return it }
        }

    /* AndroidComposeView triggers LayoutNode's layout phase in the View draw phase,
       so trigger a draw to an empty canvas to force that */
        view.draw(fakeCanvas)
        val viewParent = (view.parent as ViewGroup)

        val measuredWidth = view.measuredWidth
        val measuredHeight = view.measuredHeight

        // Only remeasure if dimensions changed
        if (measuredWidth == 0 || measuredHeight == 0) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(viewParent.width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(viewParent.height, View.MeasureSpec.AT_MOST),
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }

        val bitmap = Bitmap.createBitmap(
            view.measuredWidth.takeIf { it > 0 } ?: 1,
            view.measuredHeight.takeIf { it > 0 } ?: 1,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.applyCanvas {
            view.draw(this)
        }

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)

        // Cache the result
        cacheKey?.let { key ->
            bitmapCache[key] = WeakReference(descriptor)
        }

        return descriptor
    }

    private sealed class ViewKey<T : ClusterItem> {
        data class Cluster<T : ClusterItem>(
            val cluster: com.google.maps.android.clustering.Cluster<T>,
        ) : ViewKey<T>()

        data class Item<T : ClusterItem>(
            val item: T,
        ) : ViewKey<T>()
    }

    private class ViewInfo(
        val view: AbstractComposeView,
        val onRemove: () -> Unit,
    )

    /**
     * An [AbstractComposeView] that calls [onInvalidate] whenever the Compose render layer is
     * invalidated. Works by reporting invalidations from its inner AndroidComposeView.
     */
    private class InvalidatingComposeView(
        context: Context,
        private val content: @Composable () -> Unit,
    ) : AbstractComposeView(context) {

        var onInvalidate: (() -> Unit)? = null

        @Composable
        override fun Content() = content()

        override fun onDescendantInvalidated(child: View, target: View) {
            super.onDescendantInvalidated(child, target)
            onInvalidate?.invoke()
        }
    }
}
