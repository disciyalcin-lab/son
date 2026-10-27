package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.data.model.WordEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeckProgress(
    val deckNumber: Int,
    val title: String,
    val rangeText: String,
    val totalWords: Int,
    val learnedWords: Int,
    val isCompleted: Boolean
)

data class DailyLearnedBarItem(
    val dayLabel: String,
    val dateLabel: String,
    val count: Int,
    val isToday: Boolean
)

data class DashboardUiState(
    val totalLearnedCount: Int = 0,
    val totalWords: Int = 300,
    val cumulativeFrequency: Int = 0,
    val totalQuranWords: Int = 77800,
    val coveragePercentage: Float = 0f,
    val currentMilestone: String = "Temel Aşina",
    val nextMilestoneRemaining: Int = 50,
    val dailyStreak: Int = 1,
    val dailyGoal: Int = 10,
    val dailyGoalProgress: Int = 0,
    val dailyWord: WordEntity? = null,
    val decks: List<DeckProgress> = emptyList(),
    val last7DaysLearned: List<DailyLearnedBarItem> = emptyList(),
    val isAudioPlaying: Boolean = false
)

class DashboardViewModel(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _dailyWord = MutableStateFlow<WordEntity?>(null)
    val dailyWord: StateFlow<WordEntity?> = _dailyWord.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        wordRepository.allWords,
        wordRepository.learnedWords,
        wordRepository.learnedFrequencySum,
        preferencesRepository.userPreferencesFlow,
        _dailyWord
    ) { allWords, learnedWords, freqSum, prefs, dailyWord ->
        val learnedCount = learnedWords.size
        val frequencyLearned = freqSum ?: 0
        // Quran has ~77,800 words total
        val coverage = if (frequencyLearned > 0) {
            (frequencyLearned.toFloat() / 77800f * 100f).coerceAtMost(100f)
        } else {
            0f
        }

        val (milestone, remaining) = when {
            learnedCount < 50 -> "Temel Aşina (%40)" to (50 - learnedCount)
            learnedCount < 100 -> "Sayfa Okuyucu (%55)" to (100 - learnedCount)
            learnedCount < 200 -> "Anlam Kaşifi (%68)" to (200 - learnedCount)
            learnedCount < 300 -> "Ayet Kavrayıcı (%75)" to (300 - learnedCount)
            else -> "Kur'an Kelime Üstadı! 🌟" to 0
        }

        // 12 Decks (25 words each)
        val deckList = (1..12).map { deckNum ->
            val startId = (deckNum - 1) * 25 + 1
            val endId = (deckNum * 25).coerceAtMost(300)
            val deckWords = allWords.filter { it.deck == deckNum }
            val deckLearned = deckWords.count { it.isLearned }
            DeckProgress(
                deckNumber = deckNum,
                title = "Deste $deckNum",
                rangeText = "$startId - $endId. Kelimeler",
                totalWords = deckWords.size.coerceAtLeast(25),
                learnedWords = deckLearned,
                isCompleted = deckWords.isNotEmpty() && deckLearned >= deckWords.size
            )
        }

        val dayNames = listOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
        val dayMs = 86_400_000L
        val calNow = java.util.Calendar.getInstance()
        calNow.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calNow.set(java.util.Calendar.MINUTE, 0)
        calNow.set(java.util.Calendar.SECOND, 0)
        calNow.set(java.util.Calendar.MILLISECOND, 0)
        val todayStartMs = calNow.timeInMillis

        val last7Days = (6 downTo 0).map { daysAgo ->
            val dayStart = todayStartMs - daysAgo * dayMs
            val dayEnd = dayStart + dayMs
            val calDay = java.util.Calendar.getInstance().apply { timeInMillis = dayStart }
            val dow = calDay.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0..6
            val dom = calDay.get(java.util.Calendar.DAY_OF_MONTH)
            val month = calDay.get(java.util.Calendar.MONTH) + 1
            val countForDay = learnedWords.count { w ->
                val studiedAt = if (w.nextReviewTimestamp > 0L) {
                    w.nextReviewTimestamp - (w.srsIntervalDays.coerceAtLeast(1) * dayMs)
                } else {
                    todayStartMs + 1000L
                }
                if (daysAgo == 0 && studiedAt < todayStartMs - 6 * dayMs) {
                    false
                } else {
                    studiedAt in dayStart until dayEnd
                }
            }
            DailyLearnedBarItem(
                dayLabel = dayNames[dow.coerceIn(0, 6)],
                dateLabel = "$dom/$month",
                count = countForDay,
                isToday = daysAgo == 0
            )
        }

        DashboardUiState(
            totalLearnedCount = learnedCount,
            totalWords = 300,
            cumulativeFrequency = frequencyLearned,
            totalQuranWords = 77800,
            coveragePercentage = coverage,
            currentMilestone = milestone,
            nextMilestoneRemaining = remaining,
            dailyStreak = prefs.streakDays,
            dailyGoal = prefs.dailyGoal,
            dailyGoalProgress = (learnedCount % prefs.dailyGoal),
            dailyWord = dailyWord ?: allWords.firstOrNull(),
            decks = deckList,
            last7DaysLearned = last7Days,
            isAudioPlaying = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch {
            preferencesRepository.recordActivityToday()
            val weakest = wordRepository.getWeakestOrNextWord()
            _dailyWord.value = weakest
        }
    }

    fun playWordAudio(word: WordEntity) {
        audioPlayerManager.speakArabic(word.arabic, fallbackUrl = word.exampleAyahAudioUrl)
    }

    fun sendTestNotification(context: android.content.Context) {
        val word = _dailyWord.value ?: return
        com.example.notification.NotificationHelper.showDailyWordReminder(
            context = context,
            arabic = word.arabic,
            transliteration = word.transliteration,
            turkishMeaning = word.turkishMeaning,
            frequency = word.frequency
        )
    }

    fun stopAudio() {
        audioPlayerManager.stop()
    }
}

class DashboardViewModelFactory(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DashboardViewModel(wordRepository, preferencesRepository, audioPlayerManager) as T
    }
}
