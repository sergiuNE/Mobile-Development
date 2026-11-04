package be.ap.nowaste.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

// Firestore queues writes while offline and only finishes them later, so check first to show "no internet" right away.
fun Context.isOnline(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    // VALIDATED: the network really reaches the internet (not a captive portal or dead Wi-Fi)
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
