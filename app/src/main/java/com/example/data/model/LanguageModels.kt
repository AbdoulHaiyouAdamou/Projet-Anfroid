package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val translation: String,
    val definition: String,
    val example: String,
    val masteryScore: Int = 0, // 0 to 5
    val nextReviewTime: Long = System.currentTimeMillis(),
    val category: String = "Général"
)

@Entity(tableName = "chat_history")
data class ChatHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tutorId: String,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val correction: String? = null, // Optional annotation / grammatical correction in French
    val correctedText: String? = null // Perfect English form
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Single profile row
    val xp: Int = 120, // Starter XP
    val coins: Int = 150, // Starter coins
    val streak: Int = 3, // Initial mock starter progress
    val currentLevel: String = "B1", // A1 to C2
    val languageFocus: String = "Anglais Général",
    val username: String = "Apprenant LinguaAI",
    val lastDailyUpdate: Long = System.currentTimeMillis()
)
