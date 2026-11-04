package be.ap.nowaste.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// FavoriteStore (K12) and CachedReservation (K10) are added here as extra entities.
@Database(entities = [CachedAddress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedAddressDao(): CachedAddressDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "nowaste.db")
                .build()
                .also { instance = it }
        }
    }
}
