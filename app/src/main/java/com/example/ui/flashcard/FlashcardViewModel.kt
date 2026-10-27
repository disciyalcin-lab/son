package com.example.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.data.model.WordEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DeckFilterMode(val label: String) {
    ALL("Tümü"),
    UNLEARNED("Öğrenilecekler"),
    LEARNED("Öğrenilenler")
}

data class FlashcardUiState(
    val currentDeck: Int = 1,
    val allDeckWords: List<WordEntity> = emptyList(),
    val words: List<WordEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isSessionComplete: Boolean = false,
    val learnedInSessionCount: Int = 0,
    val repeatInSessionCount: Int = 0,
    val autoPlayAudio: Boolean = false,
    val filterMode: DeckFilterMode = DeckFilterMode.ALL,
    val isLoading: Boolean = true
)

class FlashcardViewModel(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    fun loadDeck(deckId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, currentDeck = deckId)
            preferencesRepository.updateCurrentDeck(deckId)
            val deckWords = wordRepository.getWordsByDeckSync(deckId)
                .sortedWith(compareByDescending<WordEntity> { it.frequency }.thenBy { it.id })
            applyFilterAndReset(deckWords, _uiState.value.filterMode)
        }
    }

    fun setFilterMode(mode: DeckFilterMode) {
        val state = _uiState.value
        applyFilterAndReset(state.allDeckWords, mode)
    }

    private fun applyFilterAndReset(allWords: List<WordEntity>, mode: DeckFilterMode) {
        val sortedAll = allWords.sortedWith(
            compareByDescending<WordEntity> { it.frequency }.thenBy { it.id }
        )
        val filtered = when (mode) {
            DeckFilterMode.ALL -> sortedAll
            DeckFilterMode.UNLEARNED -> sortedAll.filter { !it.isLearned }
            DeckFilterMode.LEARNED -> sortedAll.filter { it.isLearned }
        }

        _uiState.value = _uiState.value.copy(
            allDeckWords = sortedAll,
            words = filtered,
            currentIndex = 0,
            isFlipped = false,
            isSessionComplete = filtered.isEmpty(),
            filterMode = mode,
            isLoading = false
        )

        if (_uiState.value.autoPlayAudio && filtered.isNotEmpty()) {
            playWordAudio(filtered[0])
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun toggleAutoPlayAudio() {
        val newAutoPlay = !_uiState.value.autoPlayAudio
        _uiState.value = _uiState.value.copy(autoPlayAudio = newAutoPlay)
        if (newAutoPlay) {
            val currentWord = _uiState.value.words.getOrNull(_uiState.value.currentIndex)
            if (currentWord != null) {
                playWordAudio(currentWord)
            }
        }
    }

    fun goToPreviousCard() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            val prevIndex = state.currentIndex - 1
            _uiState.value = state.copy(
                currentIndex = prevIndex,
                isFlipped = false
            )
            if (state.autoPlayAudio) {
                state.words.getOrNull(prevIndex)?.let { playWordAudio(it) }
            }
        }
    }

    fun goToNextCard() {
        val state = _uiState.value
        if (state.currentIndex < state.words.size - 1) {
            val nextIndex = state.currentIndex + 1
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                isFlipped = false
            )
            if (state.autoPlayAudio) {
                state.words.getOrNull(nextIndex)?.let { playWordAudio(it) }
            }
        }
    }

    fun jumpToCard(index: Int) {
        val state = _uiState.value
        if (index in state.words.indices) {
            _uiState.value = state.copy(
                currentIndex = index,
                isFlipped = false
            )
            if (state.autoPlayAudio) {
                state.words.getOrNull(index)?.let { playWordAudio(it) }
            }
        }
    }

    fun shuffleDeck() {
        val state = _uiState.value
        val shuffled = state.words.shuffled()
        _uiState.value = state.copy(
            words = shuffled,
            currentIndex = 0,
            isFlipped = false
        )
    }

    fun onRemembered() {
        val state = _uiState.value
        val currentWord = state.words.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            wordRepository.recordReview(currentWord, remembered = true)
            preferencesRepository.recordActivityToday()
        }

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.words.size) {
            _uiState.value = state.copy(
                isSessionComplete = true,
                learnedInSessionCount = state.learnedInSessionCount + 1,
                isFlipped = false
            )
        } else {
            _uiState.value = state.copy(
                currentIndex = nextIndex,
                isFlipped = false,
                learnedInSessionCount = state.learnedInSessionCount + 1
            )
            if (state.autoPlayAudio) {
                state.words.getOrNull(nextIndex)?.let { playWordAudio(it) }
            }
        }
    }

    fun onNeedReview() {
        val state = _uiState.value
        val currentWord = state.words.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            wordRepository.recordReview(currentWord, remembered = false)
        }

        // Add current word to the end of the session list so user sees it again
        val updatedWords = state.words.toMutableList()
        updatedWords.add(currentWord)

        val nextIndex = state.currentIndex + 1
        _uiState.value = state.copy(
            words = updatedWords,
            currentIndex = nextIndex,
            isFlipped = false,
            repeatInSessionCount = state.repeatInSessionCount + 1
        )
        if (state.autoPlayAudio) {
            updatedWords.getOrNull(nextIndex)?.let { playWordAudio(it) }
        }
    }

    fun playWordAudio(word: WordEntity) {
        audioPlayerManager.speakArabic(word.arabic, fallbackUrl = word.exampleAyahAudioUrl)
    }

    fun playAyahAudio(word: WordEntity) {
        audioPlayerManager.play(word.exampleAyahAudioUrl, fallbackArabicText = word.exampleAyahArabic)
    }

    fun restartDeck() {
        loadDeck(_uiState.value.currentDeck)
    }

    fun nextDeck() {
        val next = if (_uiState.value.currentDeck < 12) _uiState.value.currentDeck + 1 else 1
        loadDeck(next)
    }
}

class FlashcardViewModelFactory(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FlashcardViewModel(wordRepository, preferencesRepository, audioPlayerManager) as T
    }
}
