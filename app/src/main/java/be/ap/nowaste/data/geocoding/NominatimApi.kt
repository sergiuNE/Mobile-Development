package be.ap.nowaste.data.geocoding

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("zoom") zoom: Int,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
    ): NominatimResponse
}

@Serializable
data class NominatimResponse(
    @SerialName("display_name") val displayName: String? = null,
    val address: NominatimAddress? = null,
)

@Serializable
data class NominatimAddress(
    val road: String? = null,
    @SerialName("house_number") val houseNumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
)

object NominatimClient {
    // Nominatim usage policy: identify the app, never pretend to be a browser
    const val USER_AGENT = "NoWaste/1.0 (be.ap.nowaste; student project; +https://github.com/sergiuNE/Mobile-Development)"
    private const val BASE_URL = "https://nominatim.openstreetmap.org/"
    private const val MIN_INTERVAL_MS = 1_000L
    private val json = Json { ignoreUnknownKeys = true }

    val api: NominatimApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NominatimApi::class.java)
    }

    private val mutex = Mutex()
    private var lastRequestAt: Long? = null

    // System.nanoTime instead of SystemClock, so this also runs in JVM unit tests
    private fun nowMs() = System.nanoTime() / 1_000_000

    /** Runs [request] at most once per second for the whole app (Nominatim usage policy). */
    suspend fun <T> throttled(request: suspend () -> T): T = mutex.withLock {
        lastRequestAt?.let { last ->
            val wait = last + MIN_INTERVAL_MS - nowMs()
            if (wait > 0) delay(wait)
        }
        try {
            request()
        } finally {
            lastRequestAt = nowMs()
        }
    }
}
