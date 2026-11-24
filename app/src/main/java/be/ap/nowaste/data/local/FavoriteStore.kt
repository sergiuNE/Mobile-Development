package be.ap.nowaste.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * K12: a store the customer marked with the heart. Only on this device: not in Firestore and not per user.
 * [addedAt] is epoch milliseconds.
 */
@Entity(tableName = "favorite_store")
data class FavoriteStore(
    @PrimaryKey val storeId: String,
    val name: String,
    val address: String,
    val addedAt: Long,
)

@Dao
interface FavoriteStoreDao {
    @Query("SELECT * FROM favorite_store ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteStore>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_store WHERE storeId = :storeId)")
    fun observeIsFavorite(storeId: String): Flow<Boolean>

    @Upsert
    suspend fun add(favorite: FavoriteStore)

    @Query("UPDATE favorite_store SET address = :address WHERE storeId = :storeId AND address = ''")
    suspend fun fillMissingAddress(storeId: String, address: String)

    @Query("DELETE FROM favorite_store WHERE storeId = :storeId")
    suspend fun delete(storeId: String)
}
