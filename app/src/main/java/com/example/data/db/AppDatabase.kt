package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.data.model.Flashcard
import com.example.data.model.ChatHistory
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE category = :category ORDER BY id DESC")
    fun getFlashcardsByCategory(category: String): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard)

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("DELETE FROM flashcards")
    suspend fun deleteAllFlashcards()
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM chat_history WHERE tutorId = :tutorId ORDER BY timestamp ASC")
    fun getChatHistoryByTutor(tutorId: String): Flow<List<ChatHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatHistory)

    @Query("DELETE FROM chat_history WHERE tutorId = :tutorId")
    suspend fun deleteChatByTutor(tutorId: String)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}

@Database(entities = [Flashcard::class, ChatHistory::class, UserProfile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lingua_ai_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
