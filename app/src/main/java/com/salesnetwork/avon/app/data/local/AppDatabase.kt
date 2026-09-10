package com.salesnetwork.avon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [CachedProduct::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val securityPrefs = appContext.getSharedPreferences("database_security", Context.MODE_PRIVATE)
                if (!securityPrefs.getBoolean("encrypted_v1", false)) {
                    appContext.deleteDatabase(DATABASE_NAME)
                    securityPrefs.edit().putBoolean("encrypted_v1", true).commit()
                }
                System.loadLibrary("sqlcipher")
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).openHelperFactory(SupportOpenHelperFactory(DatabasePassphraseStore(appContext).getOrCreate()))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearForLogout(context: Context) = synchronized(this) {
            INSTANCE?.close()
            INSTANCE = null
            val appContext = context.applicationContext
            appContext.deleteDatabase(DATABASE_NAME)
            DatabasePassphraseStore(appContext).clear()
            appContext.getSharedPreferences("database_security", Context.MODE_PRIVATE)
                .edit().remove("encrypted_v1").commit()
        }

        private const val DATABASE_NAME = "sales_network_encrypted.db"
    }
}
