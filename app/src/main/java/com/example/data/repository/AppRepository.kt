package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.api.GenerationConfig
import com.example.data.db.AppDatabase
import com.example.data.model.Flashcard
import com.example.data.model.ChatHistory
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import android.util.Log

class AppRepository(private val database: AppDatabase) {

    private val flashcardDao = database.flashcardDao()
    private val chatHistoryDao = database.chatHistoryDao()
    private val userProfileDao = database.userProfileDao()

    // DB flows
    val allFlashcards: Flow<List<Flashcard>> = flashcardDao.getAllFlashcards()
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()

    fun getChatHistory(tutorId: String): Flow<List<ChatHistory>> =
        chatHistoryDao.getChatHistoryByTutor(tutorId)

    // DB Mutators
    suspend fun insertFlashcard(flashcard: Flashcard) {
        flashcardDao.insertFlashcard(flashcard)
    }

    suspend fun updateFlashcard(flashcard: Flashcard) {
        flashcardDao.updateFlashcard(flashcard)
    }

    suspend fun deleteFlashcard(flashcard: Flashcard) {
        flashcardDao.deleteFlashcard(flashcard)
    }

    suspend fun insertChat(chat: ChatHistory) {
        chatHistoryDao.insertChat(chat)
    }

    suspend fun deleteChat(tutorId: String) {
        chatHistoryDao.deleteChatByTutor(tutorId)
    }

    suspend fun updateProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    // Initialize Mock/Default data if empty
    suspend fun initializeDatabaseIfEmpty() {
        val currentCards = allFlashcards.firstOrNull()
        if (currentCards.isNullOrEmpty()) {
            val starterCards = listOf(
                Flashcard(
                    word = "To make a decision",
                    translation = "Prendre une décision",
                    definition = "Finaliser un choix parmi plusieurs options",
                    example = "I need to make a decision about my career path by next Monday.",
                    category = "Professionnel"
                ),
                Flashcard(
                    word = "Present Perfect vs Past Simple",
                    translation = "Le passé connecté au présent vs le passé révolu",
                    definition = "Present Perfect s'emploie lorsque l'action a un impact ou se prolonge dans le présent.",
                    example = "I have lived in London for 3 years (I still live here). I lived in Paris for 1 year (Over).",
                    category = "Grammaire"
                ),
                Flashcard(
                    word = "Break a leg",
                    translation = "Bonne chance !",
                    definition = "Expression idiomatique de théâtre servant à souhaiter de la réussite.",
                    example = "You have a big job interview today? Break a leg!",
                    category = "Idiome"
                ),
                Flashcard(
                    word = "Get in touch",
                    translation = "Prendre contact / Rester en contact",
                    definition = "Établir une communication écrite ou orale avec autrui.",
                    example = "Feel free to get in touch with me if you need help.",
                    category = "Général"
                ),
                Flashcard(
                    word = "To look forward to",
                    translation = "Avoir hâte de / Attendre avec impatience",
                    definition = "Attendre avec hâte quelque chose de plaisant (suivi de la marque -ing)",
                    example = "I am looking forward to hearing from you soon.",
                    category = "Professionnel"
                )
            )
            for (card in starterCards) {
                flashcardDao.insertFlashcard(card)
            }
        }

        val profile = userProfile.firstOrNull()
        if (profile == null) {
            userProfileDao.insertOrUpdateProfile(UserProfile())
        }
    }

    // Call Gemini to converse and evaluate user text
    suspend fun sendChatToTutor(
        tutorId: String,
        tutorName: String,
        tutorPersonality: String,
        userMessage: String,
        userLevel: String,
        modelName: String = "gemini-2.5-flash",
        customAccent: String? = null
    ): ChatHistory {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!hasKey) {
            // Simulator fallbacks (Beautiful offline simulator)
            val responseText = simulateTutorReply(tutorId, userMessage, userLevel)
            val (correction, correctedText) = simulateCorrectionWithPerfectText(userMessage)

            val chatReply = ChatHistory(
                tutorId = tutorId,
                message = responseText,
                isUser = false,
                correction = correction,
                correctedText = correctedText
            )
            chatHistoryDao.insertChat(chatReply)
            return chatReply
        }

        // Real Gemini API Call with dynamic system instruction
        val systemPrompt = """
            You are an AI language agent acting as $tutorName.
            Your exact persona, gender, and interaction style: $tutorPersonality.
            Accent/Origin context: ${customAccent ?: "Standard"}.
            The user is a French speaker learning English with current CEFR level: $userLevel.
            If your personality is configured as an 'Assistant Virtuel (IA)' or 'Ami Conversationnel', you must fully adopt the friendly, helpful, and highly intelligent conversational style of a modern AI assistant (like ChatGPT), while still helping with language learning. 
            If you have a specified gender (e.g., Homme, Femme), adapt your expressions where it naturally reflects your identity.
            Your job is to carry out an interactive, engaging English dialogue.
            You must look at the user's latest message and:
            1. Analyze it for any grammatical, spelling, or styling errors in English. If there are any mistakes, explain them briefly and clearly in French (no more than 2 short sentences). If there are no mistakes, provide null.
            2. Provide the corrected/improved version of the user sentence in English in the "correctedText" field. If there are no mistakes, provide null.
            3. Reply to their chat in character, using natural English vocabulary appropriate for their level ($userLevel).

            You MUST return as a valid JSON object with EXACTLY these fields:
            - "correction": (string, brief explanation of mistakes in French, or null if correct)
            - "correctedText": (string, the perfect corrected English phrase, or null if correct)
            - "reply": (string, your in-character natural English response)

            Do not enclose in markdown code tags. Return pure valid JSON string only.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "User message: \"$userMessage\"")))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                responseMimeType = "application/json"
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            // Map any stored preference to a currently-valid Gemini model name.
            // NOTE: gemini-3.5-flash / gemini-3.1-pro-preview do not exist and caused
            // every real call to fail and silently fall back to the offline simulator.
            val activeModelName = if (modelName.contains("pro")) "gemini-2.5-pro" else "gemini-2.5-flash"
            val response = RetrofitClient.service.generateContent(activeModelName, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("LinguaAI_Repo", "Received from Gemini ($activeModelName): $jsonText")

            if (jsonText != null) {
                val cleanedJson = cleanJsonString(jsonText)
                val jsonObject = JSONObject(cleanedJson)
                val correctionValue = if (jsonObject.isNull("correction")) null else jsonObject.getString("correction")
                val correctedValue = if (jsonObject.isNull("correctedText")) null else jsonObject.getString("correctedText")
                val replyText = jsonObject.getString("reply")

                val chatReply = ChatHistory(
                    tutorId = tutorId,
                    message = replyText,
                    isUser = false,
                    correction = correctionValue,
                    correctedText = correctedValue
                )
                chatHistoryDao.insertChat(chatReply)
                return chatReply
            }
        } catch (e: Exception) {
            Log.e("LinguaAI_Repo", "Gemini API failed, using fallback simulator", e)
        }

        // Catch-all response
        val fallbackReply = "Thank you for practicing with me! (Simulation Mode: API call failed or rate limit reached)."
        val chatReply = ChatHistory(
            tutorId = tutorId,
            message = fallbackReply,
            isUser = false,
            correction = "Désolé, la connexion directe à l'IA de Google a échoué. Pensez à vérifier votre connexion ou configurer votre GEMINI_API_KEY dans AI Studio.",
            correctedText = null
        )
        chatHistoryDao.insertChat(chatReply)
        return chatReply
    }

    // New simulator that returns both correction and the reconstructed text
    private fun simulateCorrectionWithPerfectText(userMsg: String): Pair<String?, String?> {
        val msg = userMsg.lowercase().trim()
        return when {
            msg.contains("i am agree") -> Pair(
                "⚠️ Erreur courante : En anglais, on ne dit pas 'I am agree' mais 'I agree'. 'to agree' est directement un verbe.",
                userMsg.replace("i am agree", "I agree", ignoreCase = true).replace("I am agree", "I agree", ignoreCase = true)
            )
            msg.contains("i write you") -> Pair(
                "⚠️ Correction : Dites 'I am writing to you' ou 'I write to you'. Vous devez ajouter la préposition 'to' devant la personne à qui vous écrivez.",
                userMsg.replace("i write you", "I am writing to you", ignoreCase = true).replace("I write you", "I am writing to you", ignoreCase = true)
            )
            msg.contains("informations") -> Pair(
                "⚠️ Attention : Le mot 'information' est indénombrable en anglais. Il ne prend jamais de 's' à la fin.",
                userMsg.replace("informations", "information", ignoreCase = true)
            )
            msg.contains("since 3 years") -> Pair(
                "⚠️ Différence For/Since : Utilisez 'for' pour désigner une durée (for 3 years) et 'since' uniquement pour un point de départ fixe (since 2021).",
                userMsg.replace("since 3 years", "for 3 years", ignoreCase = true)
            )
            else -> Pair(null, null)
        }
    }

    // Clean markdown code blocks from Gemini response if present
    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }

    // Simulated rich offline replies when API Key is missing
    private fun simulateTutorReply(tutorId: String, userMsg: String, level: String): String {
        val msg = userMsg.lowercase()
        return when (tutorId) {
            "alex" -> { // US
                when {
                    msg.contains("hello") || msg.contains("hi") -> "Hey! Super glad to chat with you today! Grab a cup of coffee and let's talk about anything you like. What's on your mind?!"
                    msg.contains("how are you") -> "Man, I am doing awesome! Just thinking about some nice burgers and playing some video games later. How are things on your end?"
                    msg.contains("business") || msg.contains("work") -> "Ah, business! That can be key. In the US, we love to say 'Let's get down to business.' What field are you working in?"
                    else -> "That is so cool! Practicing English with me is the absolute best way to improve. Tell me more, buddy!"
                }
            }
            "emma" -> { // UK
                when {
                    msg.contains("hello") || msg.contains("hi") -> "Hello, lovely to meet you. I hope your day is going exceptionally well! Shall we discuss some British culture, or would you prefer a general topic?"
                    msg.contains("how are you") -> "I am very well indeed, thank you for asking. I am currently sipping some wonderful tea. What about you?"
                    msg.contains("thank") -> "You are most welcome. It is an absolute pleasure to assist you on this lovely journey."
                    else -> "Splendid statement! Conversing in English really is an art, and you are doing marvellously. Let us continue, shall we?"
                }
            }
            "sophie" -> { // Canada
                when {
                    msg.contains("hello") || msg.contains("hi") -> "Salut! Or should I say hello, eh? I am so glad to practice with you. Don't worry, we can take our time, I have lots of patience!"
                    msg.contains("how are you") -> "I am doing great, eh! The weather is beautiful today. How are you feeling about your English learning process?"
                    else -> "That's wonderful! Rest assured that every sentence you say helps you become bilingual. Tu fais d'excellents progrès !"
                }
            }
            "marcus" -> { // Business
                when {
                    msg.contains("hello") || msg.contains("hi") -> "Good day. Let's make our practice efficient. Our focus today: mastering clear corporate messaging and negotiation skills. State your current career targets."
                    msg.contains("how are you") -> "My performance metrics are optimal today. Proceed with our conversation. Time is resources."
                    else -> "Acknowledgement received. A strong business executive must articulate ideas cleanly. Let's practice phrasing that to sound more assertive."
                }
            }
            else -> "Hello there! Let's continue practicing. Tell me about your goals!"
        }
    }
}
