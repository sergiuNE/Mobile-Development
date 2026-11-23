package be.ap.nowaste.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import be.ap.nowaste.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

private const val STORE_ZOOM = 16.0

/** Small map with one store pin (K7). Only shows where the store is: the pin cannot be moved. */
@Composable
fun StoreLocationMap(location: Coordinates, modifier: Modifier = Modifier) {
    val mapView = rememberMapViewWithLifecycle()
    val marker = remember(mapView) {
        Marker(mapView).apply {
            icon = ContextCompat.getDrawable(mapView.context, R.drawable.ic_store_marker)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            infoWindow = null
            setOnMarkerClickListener { _, _ -> true }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                controller.setZoom(STORE_ZOOM)
                overlays.add(marker)
            }
        },
        modifier = modifier,
        update = { map ->
            val point = GeoPoint(location.latitude, location.longitude)
            if (marker.position != point) {
                marker.position = point
                map.controller.setCenter(point)
                map.invalidate()
            }
        },
    )
}
