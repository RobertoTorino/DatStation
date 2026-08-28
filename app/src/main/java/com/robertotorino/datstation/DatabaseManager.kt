package com.robertotorino.datstation

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure.ANDROID_ID
import android.provider.Settings.Secure.getString
import android.util.Log
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Entity(tableName = "dummy_table")
data class DummyEntity(@PrimaryKey val id: Int)

@Database(entities = [DummyEntity::class], version =87, exportSchema = false)
abstract class AppDatabase : RoomDatabase()

object DatabaseManager {
    private var dbInstance: SupportSQLiteDatabase? = null
    private const val TAG = "DatabaseManager"

    @SuppressLint("HardwareIds")
    private fun getEncryptionKey(context: Context): ByteArray {
        // Use ANDROID_ID as a base for the encryption key to bind data to this device
        val androidId = getString(context.contentResolver, ANDROID_ID)
        return androidId?.toByteArray() ?: "default_key".toByteArray()
    }

    /**
     * Automatically detects if the datstation.db file in assets is different (by size)
     * from the one currently installed on the device. If it is, we delete the
     * local copy to force Room to copy the new one from assets.
     */
    private fun forceUpdateIfAssetChanged(context: Context) {
        val dbFile = context.getDatabasePath("datstation.db")
        if (!dbFile.exists()) return

        try {
            val assetSize = context.assets.open("datstation.db").use { it.available().toLong() }
            if ((dbFile.length() != assetSize) && assetSize > 0) {
                Log.d(TAG, "Database asset update detected. Refreshing local database...")
                context.deleteDatabase("datstation.db")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check database asset size", e)
        }
    }

    fun getDatabase(context: Context): SupportSQLiteDatabase {
        if (dbInstance == null || !dbInstance!!.isOpen) {
            try {
                Log.d(TAG, "Initializing database...")

                // Initialize SQLCipher
                SQLiteDatabase.loadLibs(context)

                // 1. Force update if the file in assets has changed
                forceUpdateIfAssetChanged(context)

                // 2. Open the main database with NO password 
                // This allows you to use plain SQLite files from your other repo
                val factory = SupportFactory("".toByteArray())

                val roomDb = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "datstation.db",
                )
                    .createFromAsset("datstation.db")
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                val db = roomDb.openHelper.writableDatabase

                // --- ATTACH PERSISTENT USER DATA ---
                val userDataFile = context.getDatabasePath("user_data.db")
                val userDataPath = userDataFile.absolutePath

                // 3. Encrypt ONLY the user_data.db using the ANDROID_ID
                val passphrase = getEncryptionKey(context)
                val keyHex = passphrase.joinToString("") { "%02x".format(it) }
                db.execSQL("ATTACH DATABASE '$userDataPath' AS user_db KEY x'$keyHex'")

                // Ensure favorites table exists in the ATTACHED database
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_db.favorites (
                        tableName TEXT,
                        gameId TEXT,
                        PRIMARY KEY (tableName, gameId)
                    )
                """.trimIndent()
                )

                // Ensure 'have' table exists in the ATTACHED database
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_db.have (
                        tableName TEXT,
                        gameId TEXT,
                        PRIMARY KEY (tableName, gameId)
                    )
                """.trimIndent()
                )

                // Ensure scores table exists in the ATTACHED database
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_db.scores (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        tableName TEXT,
                        score INTEGER,
                        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent()
                )

                // Ensure user_profile table exists
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_db.user_profile (
                        key TEXT PRIMARY KEY,
                        value TEXT
                    )
                """.trimIndent()
                )

                // Ensure trophies table exists in the ATTACHED database
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_db.trophies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        gameName TEXT,
                        trophyType TEXT,
                        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent()
                )

                dbInstance = db
                Log.d(TAG, "Database opened with encryption and user_data attached.")
            } catch (e: Exception) {
                Log.e(TAG, "Error opening database", e)
                throw e
            }
        }
        return dbInstance!!
    }

    fun closeDatabase() {
        dbInstance?.close()
        dbInstance = null
    }
}
