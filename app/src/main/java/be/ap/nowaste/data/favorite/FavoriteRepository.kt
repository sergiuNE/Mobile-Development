package be.ap.nowaste.data.favorite

import be.ap.nowaste.data.local.FavoriteStore
import be.ap.nowaste.data.local.FavoriteStoreDao
import kotlinx.coroutines.flow.Flow

/** K12: the favorite stores of this device, kept in Room. */
class FavoriteRepository(private val dao: FavoriteStoreDao) {

    fun observeFavorites(): Flow<List<FavoriteStore>> = dao.observeAll()

    fun isFavorite(storeId: String): Flow<Boolean> = dao.observeIsFavorite(storeId)

    suspend fun add(storeId: String, name: String, address: String) =
        dao.add(FavoriteStore(storeId, name, address, System.currentTimeMillis()))

    suspend fun fillMissingAddress(storeId: String, address: String) = dao.fillMissingAddress(storeId, address)

    suspend fun remove(storeId: String) = dao.delete(storeId)
}
