package be.ap.nowaste.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import be.ap.nowaste.data.geocoding.NominatimClient
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File

data class Coordinates(val latitude: Double, val longitude: Double)

// AP Hogeschool, Antwerpen: start position when there is no pin yet
private val DEFAULT_CENTER = GeoPoint(51.2172, 4.4127)
private const val DEFAULT_ZOOM = 13.0
private const val PIN_ZOOM = 17.0

/**
 * OSMDroid map with one pin. Tap the map to place the pin, or drag it.
 * [onPinPlaced] is only called when the pin is placed or released, never during a drag.
 */
@Composable
fun PinPickerMap(
    pin: Coordinates?,
    onPinPlaced: (Coordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPinPlaced by rememberUpdatedState(onPinPlaced)
    val mapView = rememberMapViewWithLifecycle()
    val marker = remember(mapView) { PinMarker(mapView) { currentOnPinPlaced(it) } }

    AndroidView(
        factory = {
            mapView.apply {
                // Index 0, so the marker gets touches before the "tap on map" overlay
                overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                        currentOnPinPlaced(Coordinates(point.latitude, point.longitude))
                        return true
                    }

                    override fun longPressHelper(point: GeoPoint): Boolean = false
                }))
                controller.setZoom(if (pin != null) PIN_ZOOM else DEFAULT_ZOOM)
                controller.setCenter(pin?.toGeoPoint() ?: DEFAULT_CENTER)
            }
        },
        modifier = modifier,
        update = { map ->
            if (pin == null) {
                map.overlays.remove(marker)
            } else if (!marker.isBeingDragged) {
                // While dragging, the finger decides where the pin is
                val firstPin = marker !in map.overlays
                marker.position = pin.toGeoPoint()
                if (firstPin) {
                    map.overlays.add(marker)
                    // A pin that comes from a loaded store is not in view yet
                    map.controller.animateTo(marker.position, PIN_ZOOM, null)
                }
            }
            map.invalidate()
        },
    )
}

private fun Coordinates.toGeoPoint() = GeoPoint(latitude, longitude)

private class PinMarker(mapView: MapView, onReleased: (Coordinates) -> Unit) : Marker(mapView) {
    var isBeingDragged = false
        private set

    init {
        setAnchor(ANCHOR_CENTER, ANCHOR_BOTTOM)
        isDraggable = true
        infoWindow = null
        setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDrag(marker: Marker) = Unit
            override fun onMarkerDragStart(marker: Marker) {
                isBeingDragged = true
            }

            override fun onMarkerDragEnd(marker: Marker) {
                isBeingDragged = false
                onReleased(Coordinates(marker.position.latitude, marker.position.longitude))
            }
        })
    }
}

/** MapView that follows the screen lifecycle: onResume/onPause, and onDetach when the screen leaves. */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            isTilesScaledToDpi = true
            // The map sits in a scrolling screen: let the map handle its own drags
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) view.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    return mapView
}

private fun configureOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        // OSM tile usage policy: identify the app
        userAgentValue = NominatimClient.USER_AGENT
        val base = File(context.cacheDir, "osmdroid")
        osmdroidBasePath = base
        osmdroidTileCache = File(base, "tiles")
    }
}
