package be.ap.nowaste.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import be.ap.nowaste.R
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** A store with available bags, shown as one marker (K4). */
data class StoreOnMap(
    val id: String,
    val name: String,
    val location: Coordinates,
    val bagCount: Int,
)

private const val STORES_ZOOM = 14.0

private class CenteredOn(var location: Coordinates? = null)

/**
 * OSMDroid map with your own location and a marker per store. Tapping a marker calls [onStoreClick],
 * tapping the map elsewhere calls [onMapClick]. The map moves to [ownLocation] whenever it changes.
 */
@Composable
fun StoresMap(
    ownLocation: Coordinates,
    stores: List<StoreOnMap>,
    onStoreClick: (String) -> Unit,
    onMapClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnStoreClick by rememberUpdatedState(onStoreClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val mapView = rememberMapViewWithLifecycle()
    val ownMarker = remember(mapView) {
        Marker(mapView).apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_own_location)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            infoWindow = null
            setOnMarkerClickListener { _, _ -> true }
        }
    }
    // Not Compose state: only the map itself needs these, changing them must not recompose
    val storeMarkers = remember(mapView) { mutableListOf<Marker>() }
    val centeredOn = remember(mapView) { CenteredOn() }

    AndroidView(
        factory = {
            mapView.apply {
                overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                        currentOnMapClick()
                        return true
                    }

                    override fun longPressHelper(point: GeoPoint): Boolean = false
                }))
                controller.setZoom(STORES_ZOOM)
            }
        },
        modifier = modifier,
        update = { map ->
            ownMarker.position = GeoPoint(ownLocation.latitude, ownLocation.longitude)
            if (ownMarker !in map.overlays) map.overlays.add(ownMarker)

            // Realtime (K5): rebuild the store markers from the newest list
            map.overlays.removeAll(storeMarkers)
            storeMarkers.clear()
            stores.forEach { store ->
                val marker = Marker(map).apply {
                    position = GeoPoint(store.location.latitude, store.location.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    infoWindow = null
                    setOnMarkerClickListener { _, _ ->
                        currentOnStoreClick(store.id)
                        true
                    }
                }
                storeMarkers.add(marker)
                map.overlays.add(marker)
            }

            // Only when the location changes: otherwise every new list would undo the user's panning
            if (centeredOn.location != ownLocation) {
                map.controller.setCenter(ownMarker.position)
                centeredOn.location = ownLocation
            }
            map.invalidate()
        },
    )
}
