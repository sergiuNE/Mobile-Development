package be.ap.nowaste.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

// Reverse geocoding result from Nominatim, reused so we do not ask the same location twice.
@Entity(tableName = "cached_address")
data class CachedAddress(
    @PrimaryKey val key: String,
    val displayName: String,
    val street: String,
    val postcode: String,
    val city: String,
    val fetchedAt: Long,
)

@Dao
interface CachedAddressDao {
    @Query("SELECT * FROM cached_address WHERE `key` = :key")
    suspend fun get(key: String): CachedAddress?

    @Upsert
    suspend fun upsert(address: CachedAddress)
}
