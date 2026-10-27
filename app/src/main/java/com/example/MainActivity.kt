package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.DashboardViewModelFactory
import com.example.ui.flashcard.FlashcardScreen
import com.example.ui.flashcard.FlashcardViewModel
import com.example.ui.flashcard.FlashcardViewModelFactory
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavScreens
import com.example.ui.quiz.QuizScreen
import com.example.ui.quiz.QuizViewModel
import com.example.ui.quiz.QuizViewModelFactory
import com.example.ui.stats.StatsScreen
import com.example.ui.stats.StatsViewModel
import com.example.ui.stats.StatsViewModelFactory
import com.example.ui.theme.QuranVocabTheme
import com.example.ui.words.WordListScreen
import com.example.ui.words.WordListViewModel
import com.example.ui.words.WordListViewModelFactory

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QuranApp
        val openScreenTarget = intent?.getStringExtra("OPEN_SCREEN")

        setContent {
            val scope = rememberCoroutineScope()
            val prefs by app.userPreferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences()
            )
            val isDark = when (prefs.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            QuranVocabTheme(darkTheme = isDark) {
                MainAppScaffold(
                    app = app,
                    openScreenTarget = openScreenTarget,
                    isDarkTheme = isDark,
                    onToggleTheme = {
                        scope.launch {
                            app.userPreferencesRepository.updateThemeMode(
                                if (isDark) "LIGHT" else "DARK"
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    app: QuranApp,
    openScreenTarget: String?,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Navigate if intent has specific screen
        if (!openScreenTarget.isNullOrEmpty()) {
            when (openScreenTarget) {
                "flashcard" -> navController.navigate(Screen.Flashcard.createRoute(1))
                "stats" -> navController.navigate(Screen.Stats.route)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute?.startsWith(screen.route.substringBefore("?")) == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route.substringBefore("?")}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(
                        wordRepository = app.wordRepository,
                        preferencesRepository = app.userPreferencesRepository,
                        audioPlayerManager = app.audioPlayerManager
                    )
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToFlashcards = { deckId ->
                        navController.navigate(Screen.Flashcard.createRoute(deckId))
                    },
                    onNavigateToQuiz = {
                        navController.navigate(Screen.Quiz.route)
                    },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }

            // Flashcard
            composable(
                route = Screen.Flashcard.route,
                arguments = listOf(
                    navArgument("deckId") {
                        type = NavType.IntType
                        defaultValue = 1
                    }
                )
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getInt("deckId") ?: 1
                val viewModel: FlashcardViewModel = viewModel(
                    factory = FlashcardViewModelFactory(
                        wordRepository = app.wordRepository,
                        preferencesRepository = app.userPreferencesRepository,
                        audioPlayerManager = app.audioPlayerManager
                    )
                )
                FlashcardScreen(
                    viewModel = viewModel,
                    initialDeckId = deckId,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }

            // Quiz
            composable(Screen.Quiz.route) {
                val viewModel: QuizViewModel = viewModel(
                    factory = QuizViewModelFactory(
                        wordRepository = app.wordRepository,
                        preferencesRepository = app.userPreferencesRepository,
                        audioPlayerManager = app.audioPlayerManager
                    )
                )
                QuizScreen(viewModel = viewModel)
            }

            // Word List
            composable(Screen.WordList.route) {
                val viewModel: WordListViewModel = viewModel(
                    factory = WordListViewModelFactory(
                        wordRepository = app.wordRepository,
                        audioPlayerManager = app.audioPlayerManager
                    )
                )
                WordListScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme
                )
            }

            // Stats
            composable(Screen.Stats.route) {
                val viewModel: StatsViewModel = viewModel(
                    factory = StatsViewModelFactory(
                        wordRepository = app.wordRepository,
                        preferencesRepository = app.userPreferencesRepository
                    )
                )
                StatsScreen(viewModel = viewModel)
            }
        }
    }
}
