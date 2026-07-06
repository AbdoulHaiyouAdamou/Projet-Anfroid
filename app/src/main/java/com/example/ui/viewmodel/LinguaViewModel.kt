package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ChatHistory
import com.example.data.model.Flashcard
import com.example.data.model.UserProfile
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class Tutor(
    val id: String,
    val name: String,
    val origin: String,
    val accent: String,
    val description: String,
    val personality: String,
    val emoji: String,
    val colorHex: String
)

data class GrammarLesson(
    val id: String,
    val title: String,
    val description: String,
    val rule: String,
    val examples: List<String>,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class Scenario(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val prompt: String,
    val emoji: String
)

class LinguaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database)

    // Tutors List (Dynamic to inject custom coach variables)
    val tutorsList: List<Tutor>
        get() {
            val prefs = getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            val cName = prefs.getString("custom_tutor_name", "Sarah") ?: "Sarah"
            val cAccent = prefs.getString("custom_tutor_accent", "USA") ?: "USA"
            val cPersonality = prefs.getString("custom_tutor_personality", "Amicale, encourageante, fan de voyage, adore discuter de divers sujets.") ?: "Amicale, encourageante, fan de voyage, adore discuter de divers sujets."
            val cLevel = prefs.getString("custom_tutor_level", "Intermédiaire") ?: "Intermédiaire"
            val cGender = prefs.getString("custom_tutor_gender", "Neutre") ?: "Neutre"
            val cAssistantType = prefs.getString("custom_tutor_assistant_type", "Coach Linguistique") ?: "Coach Linguistique"

            return listOf(
                Tutor(
                    id = "julien",
                    name = "Julien",
                    origin = "Paris",
                    accent = "Français",
                    description = "Le prof de Paris, expert en pédagogie.",
                    personality = "A patient and clear French teacher from Paris who helps you learn naturally through engaging conversations.",
                    emoji = "👨‍🏫",
                    colorHex = "1E88E5" // Blue
                ),
                Tutor(
                    id = "maria",
                    name = "Maria",
                    origin = "Madrid",
                    accent = "Espagnol",
                    description = "Une amie de Madrid, chaleureuse et spontanée.",
                    personality = "Une amie de Madrid, super chaleureuse. She is a friendly native Spanish speaker from Madrid who acts as your penpal and friend.",
                    emoji = "💃",
                    colorHex = "E53935" // Red
                ),
                Tutor(
                    id = "alex",
                    name = "Alex",
                    origin = "USA",
                    accent = "New York",
                    description = "Décontracté, pop culture, humour.",
                    personality = "Extremely friendly, high-energy, loves American slangs, casual, loves food and gaming.",
                    emoji = "🇺🇸",
                    colorHex = "E64A19"
                ),
                Tutor(
                    id = "custom",
                    name = cName,
                    origin = cAccent,
                    accent = "Perso ($cAccent)",
                    description = "Votre propre coach IA créé sur mesure !",
                    personality = "Type de rôle: $cAssistantType. Sexe: $cGender. $cPersonality / Niveau ciblé: $cLevel",
                    emoji = if (cGender == "Homme") "👨‍🏫" else if (cGender == "Femme") "👩‍🏫" else "🤖",
                    colorHex = "8E24AA" // Purple
                )
            )
        }

    // Grammar Lessons List
    val grammarLessons = listOf(
        GrammarLesson(
            id = "articles",
            title = "Les Articles (A, An, The, ∅)",
            description = "Maîtriser le déterminant correct dans chaque contexte.",
            rule = "Utilisez 'A' devant une consonne sonore (a car), 'An' devant une voyelle sonore (an hour), 'The' pour une chose déjà définie, et '∅' (rien) pour des généralités au pluriel ou indénombrables (I love cheese).",
            examples = listOf(
                "I bought a white car. (Un objet parmi d'autres)",
                "I enjoyed the movie you recommended. (Spécifique)",
                "Cats are beautiful. (Généralité - Pas d'article !)"
            ),
            quizQuestion = "Complétez : 'I love listening to ... music.'",
            quizOptions = listOf("a", "the", "∅ (aucun article)"),
            correctIndex = 2,
            explanation = "En anglais, quand on exprime un goût général pour un indénombrable (la musique en général), on n'utilise aucun article !"
        ),
        GrammarLesson(
            id = "present_perfect",
            title = "Present Perfect vs Past Simple",
            description = "La différence fondamentale qui perd les francophones.",
            rule = "Le Past Simple s'emploie pour une action passée, terminée, coupée du présent (hier, l'an dernier). Le Present Perfect s'emploie pour un lien présent : une action commencée au passé qui continue, ou un constat actuel.",
            examples = listOf(
                "I lost my keys yesterday. (Hier - Terminé)",
                "I have lost my keys! (Je ne les ai pas actuellement - Lien présent)",
                "I have worked here for 2 years. (J'y travaille encore)"
            ),
            quizQuestion = "Complétez : 'She ... in New York in 2018.'",
            quizOptions = listOf("lived", "has lived", "lives"),
            correctIndex = 0,
            explanation = "'In 2018' désigne une période passée, terminée et datée. On doit impérativement employer le Past Simple 'lived'."
        ),
        GrammarLesson(
            id = "phrasal_verbs",
            title = "Les Phrasal Verbs essentiels",
            description = "Comment la particule change totalement le sens.",
            rule = "Un Phrasal Verb associe un verbe de base à une préposition ou particule adverbiale, modifiant complètement sa signification.",
            examples = listOf(
                "To give (Donner) -> To give up (Abandonner)",
                "To look (Regarder) -> To look forward to (Attendre avec impatience)",
                "To put (Poser) -> To put off (Reporter / Remettre à plus tard)"
            ),
            quizQuestion = "Que signifie : 'Don't give up on your dreams!'",
            quizOptions = listOf("Ne donnes pas d'argent", "N'abandonne pas tes rêves", "Ne regarde pas tes rêves"),
            correctIndex = 1,
            explanation = "'Give up' signifie abandonner. C'est l'un des phrasal verbs les plus importants en anglais."
        ),
        GrammarLesson(
            id = "faux_amis",
            title = "Les Faux Amis Franco-Anglais",
            description = "Éviter les contresens embarrassants.",
            rule = "Certains mots se ressemblent mais ont des définitions contraires ou différentes.",
            examples = listOf(
                "Actually = En fait (et NON 'Actuellement')",
                "Currently = Actuellement",
                "Eventually = Finalement / À long terme (et NON 'Éventuellement')"
            ),
            quizQuestion = "Complétez : 'I thought he was British, but ... he is Canadian.'",
            quizOptions = listOf("actually", "eventually", "currently"),
            correctIndex = 0,
            explanation = "On veut exprimer l'opposition 'qu'en réalité il est canadien'. 'Actually' est le terme exact pour signifier 'en réalité' ou 'en fait'."
        )
    )

    // Preset Scenarios
    val scenarioList = listOf(
        Scenario(
            id = "job",
            title = "Entretien d'embauche chez Apple",
            category = "Professionnel",
            description = "Passez un entretien d'embauche chez Apple pour le poste d'ingénieur.",
            prompt = "[Scenario: Entretien d'embauche chez Apple] Bonjour! I am here for my mock job interview for the Software Engineer position at Apple. Ask me the first question!",
            emoji = "👔"
        ),
        Scenario(
            id = "restaurant",
            title = "Commander au restaurant avec des allergies",
            category = "Voyage",
            description = "Réservez une table et commandez un repas en précisant vos allergies sévères.",
            prompt = "[Scenario: Restaurant Allergies] Waiter, excuse me! I would like to order but I am severely allergic to nuts and dairy. Can you help me choose?",
            emoji = "🍔"
        ),
        Scenario(
            id = "pitch_project",
            title = "Pitch de Projet",
            category = "Professionnel",
            description = "Présentez brièvement un projet d'application mobile à un investisseur.",
            prompt = "[Scenario: Pitch] Welcome, thank you for your time. I am excited to present our new language learning app with real-time AI feedback.",
            emoji = "🚀"
        ),
        Scenario(
            id = "hotel_complaint",
            title = "Problème d'Hôtel",
            category = "Voyage",
            description = "Plaignez-vous d'une chambre bruyante et demandez un surclassement.",
            prompt = "[Scenario: Problème Hôtel] Hello, I am at the reception desk. My room is extremely noisy and there is no hot water! Can you help me?",
            emoji = "🏨"
        ),
        Scenario(
            id = "beg_greeting",
            title = "Première Rencontre",
            category = "Débutant",
            description = "Faites connaissance avec un partenaire d'échange linguistique.",
            prompt = "[Scenario: Rencontre] Hello there! Nice to meet you. My name is Alex, and I am a beginner in English. Can we speak slowly?",
            emoji = "👋"
        ),
        Scenario(
            id = "beg_directions",
            title = "Demander son chemin",
            category = "Débutant",
            description = "Demandez à un passant comment aller à la gare routière en anglais.",
            prompt = "[Scenario: Chemin] Excuse me, sorry to bother you. Can you tell me how to go to the nearest train station, please?",
            emoji = "🗺️"
        )
    )

    // Sealed screens
    sealed interface Tab {
        object Home : Tab
        object Learn : Tab
        object Speak : Tab
        object Progress : Tab
        object Community : Tab
    }

    // Selected Tutor
    private val _selectedTutor = MutableStateFlow(tutorsList[0])
    val selectedTutor = _selectedTutor.asStateFlow()

    // Screen navigation tab
    private val _selectedTab = MutableStateFlow<Tab>(Tab.Home)
    val selectedTab = _selectedTab.asStateFlow()

    // UI Loading state for chats
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    // Custom coach reactive fields
    private val _customTutorName = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getString("custom_tutor_name", "Sarah") ?: "Sarah"
    )
    val customTutorName = _customTutorName.asStateFlow()

    private val _customTutorAccent = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getString("custom_tutor_accent", "USA") ?: "USA"
    )
    val customTutorAccent = _customTutorAccent.asStateFlow()

    private val _customTutorPersonality = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getString("custom_tutor_personality", "Amicale, encourageante, fan de voyage, adore discuter de divers sujets.") ?: "Amicale, encourageante, fan de voyage, adore discuter de divers sujets."
    )
    val customTutorPersonality = _customTutorPersonality.asStateFlow()

    private val _customTutorLevel = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getString("custom_tutor_level", "Intermédiaire") ?: "Intermédiaire"
    )
    val customTutorLevel = _customTutorLevel.asStateFlow()

    private val _customTutorGender = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getString("custom_tutor_gender", "Neutre") ?: "Neutre"
    )
    val customTutorGender = _customTutorGender.asStateFlow()

    private val _customTutorAssistantType = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getString("custom_tutor_assistant_type", "Coach Linguistique") ?: "Coach Linguistique"
    )
    val customTutorAssistantType = _customTutorAssistantType.asStateFlow()

    // Autoplay voice preference
    private val _isAutoplayVoice = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("autoplay_voice", false)
    )
    val isAutoplayVoice = _isAutoplayVoice.asStateFlow()

    fun toggleAutoplayVoice() {
        val nextVal = !_isAutoplayVoice.value
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("autoplay_voice", nextVal)
            .apply()
        _isAutoplayVoice.value = nextVal
    }

    // Model selection state: "gemini-3.5-flash" (Flash / rapid response) or "gemini-3.1-pro-preview" (Deep Conversation)
    private val _selectedModel = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .getString("selected_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
    )
    val selectedModel = _selectedModel.asStateFlow()

    fun selectModel(modelName: String) {
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("selected_model", modelName)
            .apply()
        _selectedModel.value = modelName
    }

    // TTS Speak events stream for reactive playback in the view layer
    private val _speakEvents = MutableSharedFlow<Pair<String, String>>()
    val speakEvents = _speakEvents.asSharedFlow()

    fun saveCustomTutor(name: String, accent: String, personality: String, level: String, gender: String, assistantType: String) {
        getApplication<Application>().getSharedPreferences("lingua_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("custom_tutor_name", name)
            .putString("custom_tutor_accent", accent)
            .putString("custom_tutor_personality", personality)
            .putString("custom_tutor_level", level)
            .putString("custom_tutor_gender", gender)
            .putString("custom_tutor_assistant_type", assistantType)
            .apply()

        _customTutorName.value = name
        _customTutorAccent.value = accent
        _customTutorPersonality.value = personality
        _customTutorLevel.value = level
        _customTutorGender.value = gender
        _customTutorAssistantType.value = assistantType

        // Force notify selected tutor update
        if (_selectedTutor.value.id == "custom") {
            _selectedTutor.value = tutorsList.first { it.id == "custom" }
        }
    }

    // UI States
    val flashcardsList: StateFlow<List<Flashcard>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // Currently observed chat list
    val currentChatHistory: StateFlow<List<ChatHistory>> = selectedTutor
        .flatMapLatest { tutor -> repository.getChatHistory(tutor.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize preset cards & profile state
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    fun selectTutor(tutor: Tutor) {
        _selectedTutor.value = tutor
    }

    fun selectTab(tab: Tab) {
        _selectedTab.value = tab
    }

    // Send a message from the user
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val currentTutorVal = selectedTutor.value
        val currentProfileVal = userProfile.value
        val activeModel = selectedModel.value

        viewModelScope.launch {
            // 1. Save user chat
            val userChat = ChatHistory(
                tutorId = currentTutorVal.id,
                message = text,
                isUser = true
            )
            repository.insertChat(userChat)

            // 2. Set loading
            _isGenerating.value = true

            // 3. Request Gemini reply
            try {
                val responseTutorMsg = repository.sendChatToTutor(
                    tutorId = currentTutorVal.id,
                    tutorName = currentTutorVal.name,
                    tutorPersonality = currentTutorVal.personality,
                    userMessage = text,
                    userLevel = currentProfileVal.currentLevel,
                    modelName = activeModel,
                    customAccent = if (currentTutorVal.id == "custom") currentTutorVal.origin else null
                )

                // Try auto-play if enabled
                if (isAutoplayVoice.value) {
                    _speakEvents.emit(Pair(responseTutorMsg.message, currentTutorVal.origin))
                }

                // Award XP & Coins for sending messages!
                val rewardXp = 10
                val rewardCoins = 5
                repository.updateProfile(
                    currentProfileVal.copy(
                        xp = currentProfileVal.xp + rewardXp,
                        coins = currentProfileVal.coins + rewardCoins
                    )
                )
            } catch (e: Exception) {
                // Done in repository fallback
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // Start a custom scenario pre-configured message
    fun startScenario(scenario: Scenario) {
        // Automatically set tab to Speak
        _selectedTab.value = Tab.Speak
        // Clear old chats with the selected tutor to start fresh
        viewModelScope.launch {
            repository.deleteChat(selectedTutor.value.id)
            sendMessage(scenario.prompt)
        }
    }

    // Complete a grammar lesson quiz
    fun completeLessonQuiz(lessonId: String, isCorrect: Boolean) {
        val currentProfileVal = userProfile.value
        viewModelScope.launch {
            if (isCorrect) {
                // Award 20 XP and 10 coins
                repository.updateProfile(
                    currentProfileVal.copy(
                        xp = currentProfileVal.xp + 20,
                        coins = currentProfileVal.coins + 10
                    )
                )
            }
        }
    }

    // Reset current selected tutor chat logs
    fun clearChatHistory() {
        val currentTutorVal = selectedTutor.value
        viewModelScope.launch {
            repository.deleteChat(currentTutorVal.id)
        }
    }

    // Add a new raw flashcard to the dictionary
    fun addNewFlashcard(word: String, translation: String, definition: String, example: String, category: String) {
        viewModelScope.launch {
            repository.insertFlashcard(
                Flashcard(
                    word = word,
                    translation = translation,
                    definition = definition,
                    example = example,
                    category = category
                )
            )
            // Reward 5 coins for expansion
            val p = userProfile.value
            repository.updateProfile(p.copy(coins = p.coins + 5))
        }
    }

    // Review / grade flashcard
    fun reviewFlashcard(card: Flashcard, scoreDelta: Int) {
        viewModelScope.launch {
            val newScore = (card.masteryScore + scoreDelta).coerceIn(0, 5)
            // Spaced repetition interval (review schedule hours depending on mastery score)
            val addedHours = when (newScore) {
                0 -> 0L
                1 -> 1L
                2 -> 4L
                3 -> 12L
                4 -> 24L
                5 -> 72L
                else -> 12L
            }
            val nextTime = System.currentTimeMillis() + (addedHours * 3600 * 1000)
            repository.updateFlashcard(
                card.copy(
                    masteryScore = newScore,
                    nextReviewTime = nextTime
                )
            )
            // Award minor XP
            val p = userProfile.value
            repository.updateProfile(p.copy(xp = p.xp + 2, coins = p.coins + 1))
        }
    }

    fun deleteFlashcard(card: Flashcard) {
        viewModelScope.launch {
            repository.deleteFlashcard(card)
        }
    }

    // Set user focus level (A1 -> C2)
    fun selectUserLevel(level: String) {
        val p = userProfile.value
        viewModelScope.launch {
            repository.updateProfile(p.copy(currentLevel = level))
        }
    }

    fun updateUsername(newName: String) {
        val p = userProfile.value
        viewModelScope.launch {
            repository.updateProfile(p.copy(username = newName))
        }
    }

    fun spendCoins(amount: Int): Boolean {
        val p = userProfile.value
        if (p.coins >= amount) {
            viewModelScope.launch {
                repository.updateProfile(p.copy(coins = p.coins - amount))
            }
            return true
        }
        return false
    }

    fun addXpAndCoins(xpAmount: Int, coinsAmount: Int) {
        val p = userProfile.value
        viewModelScope.launch {
            repository.updateProfile(p.copy(xp = p.xp + xpAmount, coins = p.coins + coinsAmount))
        }
    }
}
