package be.ap.nowaste.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// FavoriteStore (K12) is added here as an extra entity. Everything in here is a cache, so a new version may wipe it.
@Database(entities = [CachedAddress::class, CachedReservation::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedAddressDao(): CachedAddressDao
    abstract fun cachedReservationDao(): CachedReservationDao

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
