package com.example.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.data.model.WordEntity
import com.example.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WordListUiState(
    val words: List<WordEntity> = emptyList(),
    val totalWordsCount: Int = 300,
    val learnedWordsCount: Int = 0,
    val unlearnedWordsCount: Int = 300,
    val globalLearnedCount: Int = 0,
    val globalTotalCount: Int = 300,
    val coveragePercentage: Float = 0f,
    val searchQuery: String = "",
    val filterLearned: Boolean? = null, // null: all, true: learned, false: unlearned
    val filterDeck: Int? = null, // null: all decks (1..12)
    val filterWordType: String? = null, // null: all word types
    val availableWordTypes: List<String> = listOf("İsim", "Fiil", "Harf / Edat", "Zamir"),
    val selectedWordForDetail: WordEntity? = null
) {
    val hasActiveSecondaryFilters: Boolean
        get() = searchQuery.isNotBlank() || filterDeck != null || filterWordType != null || filterLearned != null
}

private data class FilterCriteria(
    val query: String = "",
    val filterLearned: Boolean? = null,
    val filterDeck: Int? = null,
    val filterWordType: String? = null,
    val selectedWordId: Int? = null
)

class WordListViewModel(
    private val wordRepository: WordRepository,
    val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterLearned = MutableStateFlow<Boolean?>(null)
    private val _filterDeck = MutableStateFlow<Int?>(null)
    private val _filterWordType = MutableStateFlow<String?>(null)
    private val _selectedWordId = MutableStateFlow<Int?>(null)

    private val filterCriteriaFlow = combine(
        _searchQuery,
        _filterLearned,
        _filterDeck,
        _filterWordType,
        _selectedWordId
    ) { query, filterLearned, filterDeck, filterWordType, selectedWordId ->
        FilterCriteria(
            query = query,
            filterLearned = filterLearned,
            filterDeck = filterDeck,
            filterWordType = filterWordType,
            selectedWordId = selectedWordId
        )
    }

    val uiState: StateFlow<WordListUiState> = combine(
        wordRepository.allWords,
        filterCriteriaFlow
    ) { rawAllWords, criteria ->
        val allWords = rawAllWords.sortedWith(
            compareByDescending<WordEntity> { it.frequency }.thenBy { it.id }
        )
        val globalTotal = if (allWords.isNotEmpty()) allWords.size else 300
        val globalLearnedWords = allWords.filter { it.isLearned }
        val globalLearned = globalLearnedWords.size
        val learnedFreqSum = globalLearnedWords.sumOf { it.frequency }
        val coverage = if (learnedFreqSum > 0) {
            (learnedFreqSum.toFloat() / 77800f * 100f).coerceAtMost(100f)
        } else {
            0f
        }

        val distinctTypes = allWords
            .map { it.wordType }
            .distinct()
            .ifEmpty { listOf("İsim", "Fiil", "Harf / Edat", "Zamir") }

        // First apply search, deck, and wordType filters so status tabs show live counts within current context
        val contextFiltered = allWords.filter { word ->
            val matchesQuery = if (criteria.query.isBlank()) true else {
                word.turkishMeaning.contains(criteria.query, ignoreCase = true) ||
                    word.transliteration.contains(criteria.query, ignoreCase = true) ||
                    word.arabic.contains(criteria.query) ||
                    word.id.toString() == criteria.query.trim()
            }
            val matchesDeck = if (criteria.filterDeck != null) word.deck == criteria.filterDeck else true
            val matchesWordType = if (criteria.filterWordType != null) {
                word.wordType.equals(criteria.filterWordType, ignoreCase = true)
            } else {
                true
            }
            matchesQuery && matchesDeck && matchesWordType
        }.sortedWith(compareByDescending<WordEntity> { it.frequency }.thenBy { it.id })

        val learnedInContext = contextFiltered.count { it.isLearned }
        val unlearnedInContext = contextFiltered.count { !it.isLearned }

        // Apply learned vs. unlearned filter while preserving strict frequency DESC order
        val finalFiltered = when (criteria.filterLearned) {
            true -> contextFiltered.filter { it.isLearned }
            false -> contextFiltered.filter { !it.isLearned }
            null -> contextFiltered
        }.sortedWith(compareByDescending<WordEntity> { it.frequency }.thenBy { it.id })

        val selectedWord = criteria.selectedWordId?.let { id ->
            allWords.find { it.id == id }
        }

        WordListUiState(
            words = finalFiltered,
            totalWordsCount = contextFiltered.size,
            learnedWordsCount = learnedInContext,
            unlearnedWordsCount = unlearnedInContext,
            globalLearnedCount = globalLearned,
            globalTotalCount = globalTotal,
            coveragePercentage = coverage,
            searchQuery = criteria.query,
            filterLearned = criteria.filterLearned,
            filterDeck = criteria.filterDeck,
            filterWordType = criteria.filterWordType,
            availableWordTypes = distinctTypes,
            selectedWordForDetail = selectedWord
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WordListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setLearnedFilter(learned: Boolean?) {
        _filterLearned.value = learned
    }

    fun setDeckFilter(deck: Int?) {
        _filterDeck.value = if (_filterDeck.value == deck) null else deck
    }

    fun setWordTypeFilter(wordType: String?) {
        _filterWordType.value = if (_filterWordType.value == wordType) null else wordType
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _filterLearned.value = null
        _filterDeck.value = null
        _filterWordType.value = null
    }

    fun selectWordForDetail(word: WordEntity?) {
        _selectedWordId.value = word?.id
    }

    fun toggleLearned(word: WordEntity) {
        viewModelScope.launch {
            wordRepository.recordReview(word, remembered = !word.isLearned)
        }
    }

    fun playWordAudio(word: WordEntity) {
        audioPlayerManager.speakArabic(word.arabic, fallbackUrl = word.exampleAyahAudioUrl)
    }

    fun playAyahAudio(word: WordEntity) {
        audioPlayerManager.play(word.exampleAyahAudioUrl, fallbackArabicText = word.exampleAyahArabic)
    }
}

class WordListViewModelFactory(
    private val wordRepository: WordRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WordListViewModel(wordRepository, audioPlayerManager) as T
    }
}
