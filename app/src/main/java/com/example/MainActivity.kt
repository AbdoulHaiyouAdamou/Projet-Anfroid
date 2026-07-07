package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.ui.viewmodel.GrammarLesson
import com.example.ui.viewmodel.LinguaViewModel
import com.example.ui.viewmodel.Scenario
import com.example.ui.viewmodel.Tutor
import com.example.data.model.Flashcard
import com.example.data.model.ChatHistory
import java.util.Locale
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Android Native Text To Speech Engine
        tts = TextToSpeech(this, this)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val vm: LinguaViewModel = viewModel()
                
                LaunchedEffect(vm) {
                    vm.speakEvents.collect { (text, lang) ->
                        speak(text, lang)
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        viewModel = vm,
                        onSpeakText = { text, lang -> speak(text, lang) }
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("LinguaAI_TTS", "TTS English Language is not supported on this device.")
            } else {
                isTtsReady = true
                Log.d("LinguaAI_TTS", "TTS English Engine successfully initialized.")
            }
        } else {
            Log.e("LinguaAI_TTS", "Initialization of TTS failed.")
        }
    }

    private fun speak(text: String, languageCode: String) {
        if (!isTtsReady) {
            Toast.makeText(this, "Moteur voix en cours de chargement...", Toast.LENGTH_SHORT).show()
            return
        }
        val locale = when (languageCode) {
            "UK", "GB" -> Locale.UK
            "CA" -> Locale.CANADA
            else -> Locale.US
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LinguaViewModel,
    onSpeakText: (String, String) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    var showLevelDialog by remember { mutableStateOf(false) }
    var showCoinsInfoDialog by remember { mutableStateOf(false) }
    var showStreakInfoDialog by remember { mutableStateOf(false) }
    var showLevelInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LinguaTopAppBar(
                streak = userProfile.streak,
                coins = userProfile.coins,
                currentLevel = userProfile.currentLevel,
                onStreakClick = { showStreakInfoDialog = true },
                onCoinsClick = { showCoinsInfoDialog = true },
                onLevelClick = { showLevelInfoDialog = true }
            )
        },
        bottomBar = {
            LinguaBottomNavigationBar(
                selectedTab = selectedTab,
                onSelectTab = viewModel::selectTab
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                is LinguaViewModel.Tab.Home -> HomeScreen(viewModel = viewModel)
                is LinguaViewModel.Tab.Learn -> LearnScreen(viewModel = viewModel, onSpeakText = onSpeakText)
                is LinguaViewModel.Tab.Speak -> SpeakScreen(viewModel = viewModel, onSpeakText = onSpeakText)
                is LinguaViewModel.Tab.Progress -> ProgressScreen(viewModel = viewModel)
                is LinguaViewModel.Tab.Community -> CommunityScreen(viewModel = viewModel)
            }
        }
    }

    // Social feature: Modifying username
    if (showLevelDialog) {
        Dialog(onDismissRequest = { showLevelDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sélectionner votre niveau CECRL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf("A1 Débutant", "A2 Élémentaire", "B1 Intermédiaire", "B2 Avancé Courant", "C1 Autonome", "C2 Expert Bilingue").forEach { levelStr ->
                        val code = levelStr.take(2).trim()
                        Button(
                            onClick = {
                                viewModel.selectUserLevel(code)
                                showLevelDialog = false
                                showLevelInfoDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (userProfile.currentLevel == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                contentColor = if (userProfile.currentLevel == code) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(levelStr, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showLevelInfoDialog) {
        AlertDialog(
            onDismissRequest = { showLevelInfoDialog = false },
            title = { Text("💪 Niveaux d'apprentissage", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Les niveaux A1 à C2 correspondent au CECRL (Cadre européen commun de référence pour les langues) :", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• A1 et A2 : Débutants absolus ou élémentaires. Vocabulaire de base.", fontSize = 12.sp, color = Color.Gray)
                    Text("• B1 et B2 : Indépendants. Capables de converser couramment.", fontSize = 12.sp, color = Color.Gray)
                    Text("• C1 et C2 : Maîtrise / Bilingue. Langue pointue et abstraite.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Plus votre niveau est élevé, plus les scénari d'IA s'adapteront à vous (vocabulaire complexe, vitesse).", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Button(onClick = { showLevelInfoDialog = false; showLevelDialog = true }) { Text("Changer de niveau") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLevelInfoDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showCoinsInfoDialog) {
        AlertDialog(
            onDismissRequest = { showCoinsInfoDialog = false },
            title = { Text("🪙 LinguaCoins", fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text("Que sont les Coins et les XP ?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• XP (Expérience) : Vous la gagnez en apprenant (Leçons, Quiz, AI Chat). Elle vous permet de grimper dans le classement \"Ligue Diamant\" contre d'autres joueurs.", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• 🪙 Coins : C'est la monnaie du jeu. Vous la gagnez en réussissant des leçons et en progressant. Vous pouvez les dépenser dans le menu Social pour envoyer des \"High Fives\" et soutenir vos amis.", fontSize = 13.sp, color = Color.DarkGray)
                }
            },
            confirmButton = {
                Button(onClick = { showCoinsInfoDialog = false }) { Text("Compris !") }
            }
        )
    }

    if (showStreakInfoDialog) {
        AlertDialog(
            onDismissRequest = { showStreakInfoDialog = false },
            title = { Text("🔥 Flammes de régularité", fontWeight = FontWeight.Black) },
            text = {
                Text("Votre Streak indique le nombre de jours consécutifs où vous avez pratiqué sur l'application. Ne ratez pas un seul jour pour éviter que vos flammes ne tombent à zéro !", fontSize = 14.sp)
            },
            confirmButton = {
                Button(onClick = { showStreakInfoDialog = false }) { Text("Continuer !") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinguaTopAppBar(
    streak: Int,
    coins: Int,
    currentLevel: String,
    onStreakClick: () -> Unit,
    onCoinsClick: () -> Unit,
    onLevelClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LinguaAI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        actions = {
            Row(
                modifier = Modifier
                    .clickable { onStreakClick() }
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$streak J",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .clickable { onCoinsClick() }
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🪙", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$coins",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            OutlinedButton(
                onClick = onLevelClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("header_level_badge")
            ) {
                Text(
                    text = currentLevel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun LinguaBottomNavigationBar(
    selectedTab: LinguaViewModel.Tab,
    onSelectTab: (LinguaViewModel.Tab) -> Unit
) {
    NavigationBar(
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab is LinguaViewModel.Tab.Home,
            onClick = { onSelectTab(LinguaViewModel.Tab.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Accueil", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_home_btn")
        )
        NavigationBarItem(
            selected = selectedTab is LinguaViewModel.Tab.Learn,
            onClick = { onSelectTab(LinguaViewModel.Tab.Learn) },
            icon = { Icon(Icons.Default.School, contentDescription = "Learn") },
            label = { Text("Apprendre", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_learn_btn")
        )
        NavigationBarItem(
            selected = selectedTab is LinguaViewModel.Tab.Speak,
            onClick = { onSelectTab(LinguaViewModel.Tab.Speak) },
            icon = { Icon(Icons.Default.Forum, contentDescription = "Speak") },
            label = { Text("Converser", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_speak_btn")
        )
        NavigationBarItem(
            selected = selectedTab is LinguaViewModel.Tab.Progress,
            onClick = { onSelectTab(LinguaViewModel.Tab.Progress) },
            icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Progress") },
            label = { Text("Progrès", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_progress_btn")
        )
        NavigationBarItem(
            selected = selectedTab is LinguaViewModel.Tab.Community,
            onClick = { onSelectTab(LinguaViewModel.Tab.Community) },
            icon = { Icon(Icons.Default.Groups, contentDescription = "Community") },
            label = { Text("Social", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_social_btn")
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. HOME SCREEN (ACCUEIL)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(viewModel: LinguaViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcardsList.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("Tout") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // High-premium master hero card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦",
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Bienvenue, explorateur",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Niveau actuel : ${userProfile.currentLevel} • Score total : ${userProfile.xp} XP",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prêt à parler anglais couramment ? Sélectionnez un scénario ou conversez avec vos coachs IA.",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Scenarios",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scénarios Réels de Conversation",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Horizontal category chips filter layout (Tout, Professionnel, Voyage, Débutant)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categories = listOf("Tout", "Professionnel", "Voyage", "Débutant")
                categories.forEach { cat ->
                    val isSel = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clickable { selectedCategory = cat }
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Custom Horizontal grid logic for filtered Scenarios
        item {
            val filteredScenarios = viewModel.scenarioList.filter {
                selectedCategory == "Tout" || it.category == selectedCategory
            }

            if (filteredScenarios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun scénario dans cette section pour l'instant.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredScenarios.chunked(2).forEach { rowScenarios ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowScenarios.forEach { scenario ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.startScenario(scenario) }
                                        .testTag("scenario_${scenario.id}"),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .size(46.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(scenario.emoji, fontSize = 24.sp)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = scenario.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = scenario.description,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = scenario.category,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            if (rowScenarios.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = "Word of the day",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Expression & Mot du Jour",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            val randomCard = flashcards.firstOrNull()
            if (randomCard != null) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Vocabulaire Thématique",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .background(
                                        AccentGold.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Maîtrise: ${randomCard.masteryScore}/5 ⭐️",
                                    fontSize = 11.sp,
                                    color = AccentGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = randomCard.word,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Traduction : ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = randomCard.translation,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Exemple contextuel :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"${randomCard.example}\"",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Chargement du dictionnaire de base...",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. LEARN SCREEN (APPRENDRE : GRAMMAIRE & VOCABULAIRE)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LearnScreen(
    viewModel: LinguaViewModel,
    onSpeakText: (String, String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Grammaire, 1 = Dictionnaire
    val flashcards by viewModel.flashcardsList.collectAsStateWithLifecycle()
    
    // Quiz dialog state
    var activeQuizLesson by remember { mutableStateOf<GrammarLesson?>(null) }
    var selectedQuizIndex by remember { mutableStateOf<Int?>(null) }
    var showQuizExplanation by remember { mutableStateOf(false) }

    // Flashcard addition states
    var showAddWordDialog by remember { mutableStateOf(false) }
    var newWordInput by remember { mutableStateOf("") }
    var newTranslationInput by remember { mutableStateOf("") }
    var newDefinitionInput by remember { mutableStateOf("") }
    var newExampleInput by remember { mutableStateOf("") }
    var newCategoryInput by remember { mutableStateOf("Général") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedSubTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cours de Grammaire",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedSubTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.testTag("learn_tabs_grammar").padding(vertical = 12.dp)
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedSubTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vocabulaire (${flashcards.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedSubTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.testTag("learn_tabs_vocab").padding(vertical = 12.dp)
            )
        }

        if (selectedSubTab == 0) {
            // Grammaire tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Leçons adaptées aux Francophones",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(viewModel.grammarLessons) { lesson ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Left accent line
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .align(Alignment.CenterVertically)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Text(
                                    text = lesson.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lesson.description,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                // Styled Rule box
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = lesson.rule,
                                        modifier = Modifier.padding(12.dp),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Exemples d'usage :",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                lesson.examples.forEach { itemText ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("•", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(itemText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        activeQuizLesson = lesson
                                        selectedQuizIndex = null
                                        showQuizExplanation = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Lancer le Quiz (+20 XP)")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Vocabulaire tab
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mes Flashcards de Révision",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { showAddWordDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Word", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Nouveau Mot", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (flashcards.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📚", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Votre dictionnaire est vide.",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Ajoutez un mot pour vous exercer !",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    items(flashcards) { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = card.word,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Real Speak button configuration!
                                        IconButton(
                                            onClick = { onSpeakText(card.word, "en-US") },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.VolumeUp,
                                                contentDescription = "Pronounce Word",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${card.masteryScore}/5 ⭐",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentGold,
                                            modifier = Modifier
                                                .background(
                                                    AccentGold.copy(alpha = 0.08f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteFlashcard(card) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Traduction : ",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = card.translation,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (card.example.isNotBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Exemple d'exercice :",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = card.example,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.reviewFlashcard(card, -1) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Red.copy(alpha = 0.08f),
                                            contentColor = Color.Red
                                        ),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("À revoir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.reviewFlashcard(card, 1) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00796B).copy(alpha = 0.08f),
                                            contentColor = Color(0xFF00796B)
                                        ),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("Acquis ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Grammar Quiz Dialog
    val quizLesson = activeQuizLesson
    if (quizLesson != null) {
        Dialog(onDismissRequest = { activeQuizLesson = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mini Quiz Grammaire",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = quizLesson.quizQuestion,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    quizLesson.quizOptions.forEachIndexed { index, option ->
                        val isSelected = selectedQuizIndex == index
                        Button(
                            onClick = {
                                if (selectedQuizIndex == null) {
                                    selectedQuizIndex = index
                                    showQuizExplanation = true
                                    viewModel.completeLessonQuiz(quizLesson.id, index == quizLesson.correctIndex)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSelected && index == quizLesson.correctIndex -> Color.Green.copy(alpha = 0.10f)
                                    isSelected -> Color.Red.copy(alpha = 0.10f)
                                    selectedQuizIndex != null && index == quizLesson.correctIndex -> Color.Green.copy(alpha = 0.10f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                },
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(option, fontWeight = FontWeight.SemiBold)
                        }
                     }
                    if (showQuizExplanation) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val isCorrect = selectedQuizIndex == quizLesson.correctIndex
                        Text(
                            text = if (isCorrect) "🎉 Correct ! +20 XP ! +10 🪙" else "❌ Erreur ! Re-essaye !",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCorrect) Color(0xFF2E7D32) else Color.Red,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = quizLesson.explanation,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { activeQuizLesson = null },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Fermer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Words Dialog
    if (showAddWordDialog) {
        Dialog(onDismissRequest = { showAddWordDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ajouter un mot",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newWordInput,
                        onValueChange = { newWordInput = it },
                        label = { Text("Mot en Anglais") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_word_en")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTranslationInput,
                        onValueChange = { newTranslationInput = it },
                        label = { Text("Traduction en Français") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("add_word_fr")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDefinitionInput,
                        onValueChange = { newDefinitionInput = it },
                        label = { Text("Définition / Note") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newExampleInput,
                        onValueChange = { newExampleInput = it },
                        label = { Text("Exemple d'exercice") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddWordDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (newWordInput.isNotBlank() && newTranslationInput.isNotBlank()) {
                                    viewModel.addNewFlashcard(
                                        word = newWordInput,
                                        translation = newTranslationInput,
                                        definition = newDefinitionInput,
                                        example = newExampleInput,
                                        category = newCategoryInput
                                    )
                                    newWordInput = ""
                                    newTranslationInput = ""
                                    newDefinitionInput = ""
                                    newExampleInput = ""
                                    showAddWordDialog = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ConfigureCustomTutorDialog(
    currentName: String,
    currentAccent: String,
    currentPersonality: String,
    currentLevel: String,
    currentGender: String,
    currentAssistantType: String,
    onDismiss: () -> Unit,
    onSave: (name: String, accent: String, personality: String, level: String, gender: String, assistantType: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var accentSelection by remember { mutableStateOf(currentAccent) }
    var personalityInput by remember { mutableStateOf(currentPersonality) }
    var levelSelection by remember { mutableStateOf(currentLevel) }
    var genderSelection by remember { mutableStateOf(currentGender) }
    var assistantTypeSelection by remember { mutableStateOf(currentAssistantType) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Créer votre Natif IA",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Personnalisez son nom, son accent et sa façon d'interagir.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nom du Coach") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Accent / Origine :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val accents = listOf("USA" to "🇺🇸", "UK" to "🇬🇧", "CA" to "🇨🇦", "AUS" to "🇦🇺")
                    accents.forEach { (code, flag) ->
                        val isSel = accentSelection == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { accentSelection = code }
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(flag, fontSize = 18.sp)
                                Text(code, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Sexe du Coach :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val genders = listOf("Femme", "Homme", "Neutre")
                    genders.forEach { gender ->
                        val isSel = genderSelection == gender
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { genderSelection = gender }
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(gender, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Style d'Interlocuteur :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val types = listOf("Coach Linguistique", "Assistant Virtuel (IA)", "Ami Conversationnel")
                    types.forEach { type ->
                        val isSel = assistantTypeSelection == type
                        Box(
                            modifier = Modifier
                                .clickable { assistantTypeSelection = type }
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(type, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Niveau d'Anglais Ciblé :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val levels = listOf("Débutant", "Intermédiaire", "Avancé")
                    levels.forEach { lvl ->
                        val isSel = levelSelection == lvl
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { levelSelection = lvl }
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(lvl, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Caractère & Thématiques favorites :",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val traits = listOf("Amical & Encourageant", "Formel & Business", "Pétillant & Voyage", "Plein d'humour")
                    traits.forEach { trait ->
                        Box(
                            modifier = Modifier
                                .clickable { personalityInput = trait }
                                .background(
                                    if (personalityInput == trait) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(trait, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                OutlinedTextField(
                    value = personalityInput,
                    onValueChange = { personalityInput = it },
                    label = { Text("Personnalité / Sujets favoris") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onSave(nameInput, accentSelection, personalityInput, levelSelection, genderSelection, assistantTypeSelection)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakScreen(
    viewModel: LinguaViewModel,
    onSpeakText: (String, String) -> Unit
) {
    val context = LocalContext.current
    val tutors = viewModel.tutorsList
    val selectedTutor by viewModel.selectedTutor.collectAsStateWithLifecycle()
    val chatHistory by viewModel.currentChatHistory.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val isAutoplayState by viewModel.isAutoplayVoice.collectAsStateWithLifecycle()
    
    var textInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Setup of Native Google STT Oral Input
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spokenText != null) {
                textInput = spokenText
            }
        }
    }

    val triggerSpeechInput = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez pour transcrire votre voix...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Mode dictée non disponible ou désactivé.", Toast.LENGTH_SHORT).show()
        }
    }

    // Keep scrolled to bottom when new messages arrive
    LaunchedEffect(chatHistory.size, isGenerating) {
        if (chatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tutor selection horizontal slider
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tutors) { tutor ->
                val isSelected = tutor.id == selectedTutor.id
                Card(
                    modifier = Modifier
                        .width(134.dp)
                        .clickable { viewModel.selectTutor(tutor) }
                        .testTag("tutor_selector_${tutor.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(tutor.emoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tutor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tutor.accent,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // AI Model Speed Selection & Auto-play toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Speed / Engine Choice
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(2.dp)
            ) {
                val models = listOf(
                    "gemini-3.5-flash" to "⚡ Rapide (Flash)",
                    "gemini-3.1-pro-preview" to "🧠 Avancé (Gemini)"
                )
                models.forEach { (id, label) ->
                    val isSel = selectedModel == id
                    Box(
                        modifier = Modifier
                            .clickable { viewModel.selectModel(id) }
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Autoplay toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { viewModel.toggleAutoplayVoice() }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = if (isAutoplayState) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                    contentDescription = "Autoplay Voice Toggle",
                    tint = if (isAutoplayState) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAutoplayState) "Audio Auto" else "Audio Manuel",
                    fontSize = 10.sp,
                    color = if (isAutoplayState) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Selected Tutor Summary Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedTutor.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Coach: ${selectedTutor.name} (${selectedTutor.accent})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = selectedTutor.personality,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Configurator launcher button for custom coach
                if (selectedTutor.id == "custom") {
                    var showConfigDialog by remember { mutableStateOf(false) }
                    
                    Button(
                        onClick = { showConfigDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Config", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Créer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    if (showConfigDialog) {
                        val customName by viewModel.customTutorName.collectAsStateWithLifecycle()
                        val customAccent by viewModel.customTutorAccent.collectAsStateWithLifecycle()
                        val customPersonality by viewModel.customTutorPersonality.collectAsStateWithLifecycle()
                        val customLevel by viewModel.customTutorLevel.collectAsStateWithLifecycle()
                        val customGender by viewModel.customTutorGender.collectAsStateWithLifecycle()
                        val customAssistantType by viewModel.customTutorAssistantType.collectAsStateWithLifecycle()

                        ConfigureCustomTutorDialog(
                            currentName = customName,
                            currentAccent = customAccent,
                            currentPersonality = customPersonality,
                            currentLevel = customLevel,
                            currentGender = customGender,
                            currentAssistantType = customAssistantType,
                            onDismiss = { showConfigDialog = false },
                            onSave = { name, accent, personality, level, gender, assistantType ->
                                viewModel.saveCustomTutor(name, accent, personality, level, gender, assistantType)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                TextButton(
                    onClick = { viewModel.clearChatHistory() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Effacer", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Chat conversation flow
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (chatHistory.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🛋️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aucun message sur ce tuteur.",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Écrivez ou dites votre premier mot en anglais pour lancer la discussion !",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatHistory) { chat ->
                        val isUser = chat.isUser
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                Color(android.graphics.Color.parseColor("#" + selectedTutor.colorHex)),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(selectedTutor.emoji, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                
                                Column {
                                    Surface(
                                        color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface,
                                        contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp, 
                                            topEnd = 16.dp, 
                                            bottomStart = if (isUser) 16.dp else 4.dp, 
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        ),
                                        tonalElevation = if (isUser) 0.dp else 1.dp,
                                        modifier = Modifier.padding(1.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = chat.message,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                    
                                    // Spoken Audio synthesis toggle next to speech bubble (No mock data!)
                                    if (!isUser) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Read Aloud",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(top = 4.dp, start = 8.dp)
                                                .size(22.dp)
                                                .clickable { 
                                                    onSpeakText(chat.message, selectedTutor.origin)
                                                }
                                        )
                                    }
                                }
                            }
                            
                            // Displays real corrective feedback analyzed by Gemini on French learning issues!
                            if (chat.correction != null) {
                                Card(
                                    modifier = Modifier
                                        .padding(top = 6.dp, start = 34.dp, end = 12.dp)
                                        .fillMaxWidth(0.9f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Spellcheck,
                                                contentDescription = "Correction",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Analyse & Correction IA",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        if (chat.correctedText != null) {
                                            Text(
                                                text = "Dites plutôt :",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = chat.correctedText,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface, // Elegant Dark Green
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    // Copy button
                                                    IconButton(
                                                        onClick = {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            val clip = ClipData.newPlainText("Corrected Phrase", chat.correctedText)
                                                            clipboard.setPrimaryClip(clip)
                                                            Toast.makeText(context, "Copié !", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ContentCopy,
                                                            contentDescription = "Copier",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    // Read aloud button
                                                    IconButton(
                                                        onClick = {
                                                            onSpeakText(chat.correctedText, selectedTutor.origin)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.VolumeUp,
                                                            contentDescription = "Écouter",
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        Text(
                                            text = chat.correction,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isGenerating) {
                        item {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedTutor.name} réfléchit...",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Chat message input bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speech icon for voice-to-text
                IconButton(
                    onClick = { triggerSpeechInput() },
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Vocal Input",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Écrivez en anglais...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    }),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .testTag("chat_send_button")
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. PROGRESS SCREEN (TABLEAU DE BORD DU PROGRÈS DE L'APPRENANT)
// ─────────────────────────────────────────────────────────────────────────────
data class EvaluationQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

@Composable
fun EditUsernameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier votre Pseudo", fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("Personnalisez votre pseudo visible sur le classement public.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nom d'utilisateur") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onSave(nameInput.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun ProgressScreen(viewModel: LinguaViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcardsList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Username editor states
    var showEditNameDialog by remember { mutableStateOf(false) }

    // Evaluation Quiz states
    var showEvaluationQuiz by remember { mutableStateOf(false) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var quizSubmitted by remember { mutableStateOf(false) }
    var quizSuccessMsg by remember { mutableStateOf("") }

    val evaluationQuiz = remember(userProfile.currentLevel) {
        when (userProfile.currentLevel) {
            "A1" -> listOf(
                EvaluationQuestion("Traduisez : \"Je veux un thé.\"", listOf("I want a tea", "I like tea", "I tea want"), 0),
                EvaluationQuestion("Complétez : \"She _______ English perfectly.\"", listOf("speak", "speaks", "speaking"), 1),
                EvaluationQuestion("Que signifie : \"What time is it?\"", listOf("Comment vas-tu ?", "Quelle heure est-il ?", "Quel jour est-on ?"), 1)
            )
            "A2" -> listOf(
                EvaluationQuestion("Complétez : \"Where ________ she go yesterday?\"", listOf("did", "does", "do"), 0),
                EvaluationQuestion("Complétez : \"This is the ________ book in the library.\"", listOf("goodest", "better", "best"), 2),
                EvaluationQuestion("Complétez : \"I am interested ________ learning Spanish.\"", listOf("in", "on", "at"), 0)
            )
            "B1" -> listOf(
                EvaluationQuestion("Complétez : \"By this time tomorrow, I ________ to London.\"", listOf("will fly", "will have flown", "flew"), 1),
                EvaluationQuestion("Complétez : \"If I ________ rich, I would travel around the world.\"", listOf("am", "were", "was"), 1),
                EvaluationQuestion("Complétez : \"Although it was raining, they ________ for a walk.\"", listOf("went", "go", "had gone"), 0)
            )
            "B2" -> listOf(
                EvaluationQuestion("Complétez : \"I look forward to ________ from you soon.\"", listOf("hear", "hearing", "to hear"), 1),
                EvaluationQuestion("Complétez : \"I'd rather you ________ tell her the truth yet.\"", listOf("didn't", "don't", "shouldn't"), 0),
                EvaluationQuestion("Complétez : \"Rarely ________ seen such a beautiful painting.\"", listOf("I have", "have I", "had I"), 1)
            )
            "C1" -> listOf(
                EvaluationQuestion("Complétez : \"Hardly had the meeting started ________ the alarm went off.\"", listOf("than", "when", "then"), 1),
                EvaluationQuestion("Complétez : \"The suspect denied ________ any involvement.\"", listOf("to have", "having", "have"), 1),
                EvaluationQuestion("Complétez : \"It is vital that he ________ present at the opening.\"", listOf("is", "be", "was"), 1)
            )
            else -> listOf( // C2
                EvaluationQuestion("Complétez : \"Had we known the risks, we ________ differently.\"", listOf("would act", "acted", "would have acted"), 2),
                EvaluationQuestion("Complétez : \"She left the party early, ________ to have a headache.\"", listOf("pleaded", "pleading", "plead"), 1),
                EvaluationQuestion("Complétez : \"They made ________ progress despite initial fears.\"", listOf("reputable", "noteworthy", "trivial"), 1)
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mon Espace Banner Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎓", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.clickable { 
                                    showEditNameDialog = true 
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = userProfile.username,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit name",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Niveau CECRL sélectionné : ${userProfile.currentLevel}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick Level Assessment boost action trigger
                    Button(
                        onClick = {
                            selectedAnswers.clear()
                            quizSubmitted = false
                            showEvaluationQuiz = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Évaluer & Booster mon Niveau (+50 XP) 🚀", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Statistiques de Performance",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Dashboard Stats Grid
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Score Total (XP)", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${userProfile.xp}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Vocabulaire Maîtrisé", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${flashcards.filter { it.masteryScore >= 4 }.size} mots", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Série Actuelle", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${userProfile.streak} Jours 🔥", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Solde LinguaCoins", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${userProfile.coins} 🪙", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Feuille de Route CECRL",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Progression Map of global language level scale
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { level ->
                        val isCurrent = level == userProfile.currentLevel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isCurrent) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrent) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Niveau $level",
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = when (level) {
                                    "A1" -> "Débutant Absolu"
                                    "A2" -> "Niveau Élémentaire"
                                    "B1" -> "Niveau Intermédiaire (Inclus)"
                                    "B2" -> "Intermédiaire Supérieur"
                                    "C1" -> "Anglais Professionnel"
                                    "C2" -> "Maîtrise bilingue"
                                    else -> ""
                                },
                                fontSize = 12.sp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Badges & Succès Décrochés",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Achievements Grid
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("💼 Business Executive", "Discuter d'affaires ou démarrer le scénario de salaire", true),
                    Triple("🔥 Assidu", "Maintenir une série de plus de 3 jours consécutifs", true),
                    Triple("📚 Lexicologue", "Avoir plus de 5 mots enregistrés dans ses flashcards", flashcards.size >= 5),
                    Triple("🗣️ Accent Parfait", "S'exprimer sur tous les accents tuteurs", false)
                ).forEach { (title, desc, unlocked) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (unlocked) "⭐" else "🔒",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (unlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Username Dialog
    if (showEditNameDialog) {
        EditUsernameDialog(
            currentName = userProfile.username,
            onDismiss = { showEditNameDialog = false },
            onSave = { 
                viewModel.updateUsername(it)
                showEditNameDialog = false
            }
        )
    }

    // Evaluation test dialog
    if (showEvaluationQuiz) {
        Dialog(onDismissRequest = { showEvaluationQuiz = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Évaluation Niveau ${userProfile.currentLevel} 📝",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Répondez correctement pour débloquer le niveau supérieur !",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    evaluationQuiz.forEachIndexed { qIdx, questionObj ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${qIdx + 1}. ${questionObj.question}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                questionObj.options.forEachIndexed { oIdx, opt ->
                                    val isSelected = selectedAnswers[qIdx] == oIdx
                                    val isCorrect = oIdx == questionObj.correctIndex
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable(enabled = !quizSubmitted) {
                                                selectedAnswers[qIdx] = oIdx
                                            }
                                            .background(
                                                when {
                                                    isSelected && quizSubmitted && isCorrect -> Color.Green.copy(alpha = 0.15f)
                                                    isSelected && quizSubmitted && !isCorrect -> Color.Red.copy(alpha = 0.15f)
                                                    quizSubmitted && isCorrect -> Color.Green.copy(alpha = 0.08f)
                                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else -> Color.Transparent
                                                },
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isSelected && quizSubmitted && isCorrect -> Color.Green
                                                    isSelected && quizSubmitted && !isCorrect -> Color.Red
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                },
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { if (!quizSubmitted) selectedAnswers[qIdx] = oIdx },
                                                enabled = !quizSubmitted,
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!quizSubmitted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEvaluationQuiz = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Annuler")
                            }
                            Button(
                                onClick = {
                                    if (selectedAnswers.size == evaluationQuiz.size) {
                                        quizSubmitted = true
                                        val pass = evaluationQuiz.indices.all { idx -> selectedAnswers[idx] == evaluationQuiz[idx].correctIndex }
                                        if (pass) {
                                            val currentLevels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
                                            val currentIdx = currentLevels.indexOf(userProfile.currentLevel)
                                            val nextLevel = if (currentIdx in 0..4) currentLevels[currentIdx + 1] else "C2"
                                            
                                            viewModel.selectUserLevel(nextLevel)
                                            viewModel.addXpAndCoins(50, 30)
                                            
                                            quizSuccessMsg = if (currentIdx in 0..4) {
                                                "🎉 Parfait ! Vous passez au Niveau $nextLevel ! Reçu +50 XP et +30 🪙 !"
                                            } else {
                                                "🎉 Incroyable ! Vous avez un score parfait de niveau C2 ! Reçu +50 XP et +30 🪙 !"
                                            }
                                        } else {
                                            quizSuccessMsg = "❌ Oups, certaines réponses sont incorrectes ! Réessayez après avoir parcouru vos cours de grammaire !"
                                        }
                                    } else {
                                        Toast.makeText(context, "Veuillez répondre à toutes les questions !", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Corriger le test")
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = quizSuccessMsg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (quizSuccessMsg.startsWith("🎉")) Color(0xFF2E7D32) else Color.Red
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showEvaluationQuiz = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Terminer")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. COMMUNITY SCREEN (SOCIAL : CLASSEMENT HEBDOMADAIRE LIGUE DIAMANT)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CommunityScreen(viewModel: LinguaViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Name modifier in social tab
    var showEditNameDialog by remember { mutableStateOf(false) }

    val leaderboard = listOf(
        Triple("Mohamed K.", 540, "🇸🇳"),
        Triple("Sarah L.", 490, "🇨🇦"),
        Triple(userProfile.username + " (Vous)", userProfile.xp, "🇫🇷"),
        Triple("Pierre B.", 110, "🇧🇪"),
        Triple("Paul D.", 95, "🇫🇷"),
        Triple("Fatoumata S.", 80, "🇨🇮")
    ).sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ligue de Diamant 💎",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        IconButton(
                            onClick = { 
                                showEditNameDialog = true 
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "Compétition amicale face aux apprenants francophones du monde entier !",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Votre Pseudo : ${userProfile.username} ✏️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            Text(
                text = "Classement de la semaine",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(leaderboard.size) { index ->
            val player = leaderboard[index]
            val isUser = player.first.contains("(Vous)")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 4.dp else 1.dp),
                border = BorderStroke(
                    1.dp,
                    if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = when (index) {
                                0 -> AccentGold
                                1 -> Color.Gray
                                2 -> Color(0xFF8D6E63)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            },
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(player.third, fontSize = 18.sp) // Flag emoji
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = player.first,
                                fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${player.second} XP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (!isUser) {
                        // Social support button using coins
                        Button(
                            onClick = {
                                if (viewModel.spendCoins(5)) {
                                    viewModel.addXpAndCoins(2, 0)
                                    Toast.makeText(context, "Vous avez envoyé des encouragements à ${player.first.substringBefore(" (")} ! ✨ +2 XP", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "🪙 Solde de LinguaCoins insuffisant !", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("🙌 Féliciter (5 🪙)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Vous 👑", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ Défi Hebdo : Dépassez vos limites !",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val topPlayerName = leaderboard.first().first.substringBefore(" (")
                    Text(
                        text = if (leaderboard.first().first.contains("(Vous)")) 
                                "Faites des leçons de grammaire ou exercez-vous en parlant avec nos tuteurs pour garder la tête du classement !" 
                               else "Faites des leçons de grammaire ou exercez-vous en parlant avec nos tuteurs pour dépasser $topPlayerName !",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    // Edit Username Dialog
    if (showEditNameDialog) {
        EditUsernameDialog(
            currentName = userProfile.username,
            onDismiss = { showEditNameDialog = false },
            onSave = { 
                viewModel.updateUsername(it)
                showEditNameDialog = false
            }
        )
    }
}
