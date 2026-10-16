package com.robertotorino.datstation

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

@Entity(tableName = "questions_table")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val text: String,
    val type: String,
    val category: String,
    val optionsJson: String,
    val correctAnswer: String,
    val explanation: String = "",
    val points: Int = 10,
    val hasBeenAsked: Boolean = false
)

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions_table WHERE hasBeenAsked = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomUnaskedQuestions(limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions_table WHERE category = :category AND hasBeenAsked = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomUnaskedQuestionsByCategory(
        category: String,
        limit: Int
    ): List<QuestionEntity>

    @Query("SELECT * FROM questions_table WHERE type = :type AND hasBeenAsked = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomUnaskedQuestionsByType(
        type: String,
        limit: Int
    ): List<QuestionEntity>

    @Query("SELECT * FROM questions_table ORDER BY RANDOM() LIMIT 1")
    suspend fun getAnyRandomQuestion(): QuestionEntity?

    @Query("UPDATE questions_table SET hasBeenAsked = 1 WHERE id = :questionId")
    suspend fun markQuestionAsAsked(questionId: String)

    @Query("UPDATE questions_table SET hasBeenAsked = 0")
    suspend fun resetAllQuestions()

    @Query("SELECT COUNT(*) FROM questions_table")
    suspend fun getQuestionCount(): Int

    @Query("DELETE FROM questions_table")
    suspend fun deleteAllQuestions()
}

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val list = mutableListOf<String>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }
}

/**
 * Local cache of the quiz questions, filled from `assets/questions.json`.
 * The questions are public data, so the database is a plain SQLite file.
 */
@Database(entities = [QuestionEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class QuizDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
}

object DatabaseProvider {
    private const val DB_NAME = "quiz_questions.db"

    /** Name used by versions up to 1.0.2, which kept this cache SQLCipher-encrypted. */
    private const val LEGACY_DB_NAME = "encrypted_quiz_database.db"

    @Volatile
    private var INSTANCE: QuizDatabase? = null

    fun getDatabase(context: Context): QuizDatabase {
        return INSTANCE ?: synchronized(this) {
            val appContext = context.applicationContext
            appContext.deleteDatabase(LEGACY_DB_NAME)
            val instance = Room.databaseBuilder(appContext, QuizDatabase::class.java, DB_NAME)
                // The cache is rebuilt from the asset whenever the schema changes.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
