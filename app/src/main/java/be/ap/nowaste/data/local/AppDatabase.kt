package be.ap.nowaste.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Everything in here is a cache, except FavoriteStore (K12). The destructive fallback also wipes the favorites
// on a schema change, which is acceptable during development.
@Database(entities = [CachedAddress::class, CachedReservation::class, FavoriteStore::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedAddressDao(): CachedAddressDao
    abstract fun cachedReservationDao(): CachedReservationDao
    abstract fun favoriteStoreDao(): FavoriteStoreDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "nowaste.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
