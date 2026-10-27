package com.example.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.WordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MilestoneBadge(
    val id: Int,
    val title: String,
    val targetWords: Int,
    val coverageTargetText: String,
    val description: String,
    val isUnlocked: Boolean,
    val progress: Float
)

data class StatsUiState(
    val learnedCount: Int = 0,
    val totalWords: Int = 300,
    val cumulativeFrequency: Int = 0,
    val totalQuranOccurrences: Int = 77800,
    val coveragePercentage: Float = 0f,
    val streakDays: Int = 1,
    val badges: List<MilestoneBadge> = emptyList(),
    val deckStats: List<Pair<Int, Float>> = emptyList(), // Pair(deckNum, progress)
    val themeMode: String = "SYSTEM"
)

class StatsViewModel(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        wordRepository.allWords,
        wordRepository.learnedWords,
        wordRepository.learnedFrequencySum,
        preferencesRepository.userPreferencesFlow
    ) { allWords, learnedWords, freqSum, prefs ->
        val count = learnedWords.size
        val freq = freqSum ?: 0
        val coverage = if (freq > 0) (freq.toFloat() / 77800f * 100f).coerceAtMost(100f) else 0f

        val badges = listOf(
            MilestoneBadge(
                id = 1,
                title = "Temel Aşina",
                targetWords = 50,
                coverageTargetText = "~%40 Kur'an Kapsamı",
                description = "En sık geçen ilk 50 kelimeyi tamamlayarak Kur'an metninin neredeyse %40'ına aşina olun.",
                isUnlocked = count >= 50,
                progress = (count.toFloat() / 50f).coerceAtMost(1f)
            ),
            MilestoneBadge(
                id = 2,
                title = "Sayfa Okuyucu",
                targetWords = 100,
                coverageTargetText = "~%55 Kur'an Kapsamı",
                description = "İlk 100 kelime ile açtığınız herhangi bir Kur'an sayfasındaki her iki kelimeden birini anlayın.",
                isUnlocked = count >= 100,
                progress = (count.toFloat() / 100f).coerceAtMost(1f)
            ),
            MilestoneBadge(
                id = 3,
                title = "Anlam Kaşifi",
                targetWords = 200,
                coverageTargetText = "~%68 Kur'an Kapsamı",
                description = "İlk 200 kelimeyle ayetlerin ana temasını ve cümle yapılarını doğrudan kavramaya başlayın.",
                isUnlocked = count >= 200,
                progress = (count.toFloat() / 200f).coerceAtMost(1f)
            ),
            MilestoneBadge(
                id = 4,
                title = "Ayet Kavrayıcı",
                targetWords = 300,
                coverageTargetText = "~%75 Kur'an Kapsamı",
                description = "300 kelimeyi tamamlayarak Kur'an-ı Kerim'in yaklaşık 4'te 3'ünü meale bakmadan anlama eşiğine ulaşın!",
                isUnlocked = count >= 300,
                progress = (count.toFloat() / 300f).coerceAtMost(1f)
            )
        )

        val deckStats = (1..12).map { deckNum ->
            val deckWords = allWords.filter { it.deck == deckNum }
            val learned = deckWords.count { it.isLearned }
            val p = if (deckWords.isNotEmpty()) learned.toFloat() / deckWords.size else 0f
            deckNum to p
        }

        StatsUiState(
            learnedCount = count,
            totalWords = 300,
            cumulativeFrequency = freq,
            totalQuranOccurrences = 77800,
            coveragePercentage = coverage,
            streakDays = prefs.streakDays,
            badges = badges,
            deckStats = deckStats,
            themeMode = prefs.themeMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            wordRepository.resetAllProgress()
        }
    }
}

class StatsViewModelFactory(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StatsViewModel(wordRepository, preferencesRepository) as T
    }
}
