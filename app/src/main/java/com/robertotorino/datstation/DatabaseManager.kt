package com.robertotorino.datstation

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings.Secure.ANDROID_ID
import android.provider.Settings.Secure.getString
import android.util.Log
import androidx.core.content.edit
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.io.IOException

/**
 * Opens the read-only game catalogue shipped in assets and attaches the
 * encrypted, per-device `user_data.db` that holds favorites, collection,
 * scores, trophies and profile values.
 *
 * The catalogue is copied out of the APK once. The size of the asset at copy
 * time is remembered in SharedPreferences; when a new APK ships a different
 * asset the local copy is replaced. Comparing against the recorded size (and
 * not against the local file) matters because SQLite may grow the local file
 * on its own, which must not trigger a 63 MB re-copy on every launch.
 */
object DatabaseManager {
    private const val TAG = "DatabaseManager"
    private const val MAIN_DB_NAME = "datstation.db"
    private const val USER_DB_NAME = "user_data.db"
    private const val PREFS_NAME = "database_prefs"
    private const val KEY_COPIED_ASSET_SIZE = "copied_asset_size"

    @Volatile
    private var dbInstance: SupportSQLiteDatabase? = null

    /**
     * Returns the shared open database, opening it on first use. This does
     * file copying and native library loading, so call it off the main thread.
     */
    @Synchronized
    fun getDatabase(context: Context): SupportSQLiteDatabase {
        dbInstance?.takeIf { it.isOpen }?.let { return it }

        val appContext = context.applicationContext
        try {
            Log.d(TAG, "Initializing database...")
            SQLiteDatabase.loadLibs(appContext)
            copyCatalogueIfNeeded(appContext)

            // The catalogue itself is a plain SQLite file, so it is opened with an empty key.
            val helper = SupportFactory("".toByteArray()).create(
                SupportSQLiteOpenHelper.Configuration.builder(appContext)
                    .name(MAIN_DB_NAME)
                    .callback(CatalogueCallback)
                    .build()
            )
            val db = helper.writableDatabase
            attachUserData(appContext, db)

            dbInstance = db
            Log.d(TAG, "Database opened and user_data attached.")
            return db
        } catch (e: Exception) {
            Log.e(TAG, "Error opening database", e)
            throw e
        }
    }

    fun closeDatabase() {
        dbInstance?.close()
        dbInstance = null
    }

    /** The catalogue is never migrated: a changed asset is copied in whole. */
    private object CatalogueCallback : SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: SupportSQLiteDatabase) = Unit
        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

        // Copies made by earlier app versions carry Room's schema version; accept them.
        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private fun copyCatalogueIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dbFile = context.getDatabasePath(MAIN_DB_NAME)
        val assetSize = context.assets.open(MAIN_DB_NAME).use { it.available().toLong() }
        val copiedSize = prefs.getLong(KEY_COPIED_ASSET_SIZE, -1L)

        if (dbFile.exists() && copiedSize == assetSize) return

        Log.i(TAG, "Copying catalogue from assets (local=${dbFile.exists()}, recorded=$copiedSize, asset=$assetSize)")
        context.deleteDatabase(MAIN_DB_NAME)
        dbFile.parentFile?.mkdirs()

        val tempFile = File(dbFile.path + ".tmp")
        val written = context.assets.open(MAIN_DB_NAME).use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        if (!tempFile.renameTo(dbFile)) {
            tempFile.delete()
            throw IOException("Could not move the copied catalogue into place")
        }
        if (written != assetSize) {
            Log.w(TAG, "Asset reported $assetSize bytes but $written were copied")
        }
        prefs.edit { putLong(KEY_COPIED_ASSET_SIZE, assetSize) }
    }

    @SuppressLint("HardwareIds")
    private fun getEncryptionKey(context: Context): ByteArray {
        // ANDROID_ID binds the user data to this device and app signing key.
        val androidId = getString(context.contentResolver, ANDROID_ID)
        return androidId?.toByteArray() ?: "default_key".toByteArray()
    }

    private fun attachUserData(context: Context, db: SupportSQLiteDatabase) {
        val userDataPath = context.getDatabasePath(USER_DB_NAME).absolutePath
        val keyHex = getEncryptionKey(context).joinToString("") { "%02x".format(it) }
        db.execSQL("ATTACH DATABASE '$userDataPath' AS user_db KEY x'$keyHex'")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_db.favorites (
                tableName TEXT,
                gameId TEXT,
                PRIMARY KEY (tableName, gameId)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_db.have (
                tableName TEXT,
                gameId TEXT,
                PRIMARY KEY (tableName, gameId)
            )
            """.trimIndent()
        )
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_db.user_profile (
                key TEXT PRIMARY KEY,
                value TEXT
            )
            """.trimIndent()
        )
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
    }
}
