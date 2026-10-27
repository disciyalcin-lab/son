package com.example.ui.quiz

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

enum class QuizMode(val title: String, val subtitle: String) {
    CLASSIC_AR_TO_TR("Klasik Quiz", "Arapça -> Türkçe"),
    CLASSIC_TR_TO_AR("Ters Quiz", "Türkçe -> Arapça"),
    AUDIO_QUIZ("Sesli Quiz", "Kelimeyi dinle, anlamını bul"),
    AYAH_GAP_FILL("Ayet İçi Boşluk Doldurma", "Ayetteki eksik kelimeyi tamamla")
}

data class QuizQuestion(
    val targetWord: WordEntity,
    val questionPrompt: String,
    val ayahContext: String? = null,
    val ayahTranslation: String? = null,
    val options: List<String>,
    val correctOptionIndex: Int
)

data class QuizUiState(
    val currentMode: QuizMode = QuizMode.CLASSIC_AR_TO_TR,
    val questionCount: Int = 10,
    val selectedDeckFilter: Int? = null, // null: All decks, 1..12
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 10,
    val questions: List<QuizQuestion> = emptyList(),
    val selectedOptionIndex: Int? = null,
    val isAnswerChecked: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Int = 0,
    val isQuizFinished: Boolean = false,
    val isLoading: Boolean = false
)

class QuizViewModel(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository,
    val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        startNewQuiz(QuizMode.CLASSIC_AR_TO_TR)
    }

    fun setQuestionCount(count: Int) {
        _uiState.value = _uiState.value.copy(questionCount = count)
        startNewQuiz(_uiState.value.currentMode, count, _uiState.value.selectedDeckFilter)
    }

    fun setDeckFilter(deck: Int?) {
        _uiState.value = _uiState.value.copy(selectedDeckFilter = deck)
        startNewQuiz(_uiState.value.currentMode, _uiState.value.questionCount, deck)
    }

    fun startNewQuiz(
        mode: QuizMode = _uiState.value.currentMode,
        count: Int = _uiState.value.questionCount,
        deck: Int? = _uiState.value.selectedDeckFilter
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentMode = mode,
                questionCount = count,
                selectedDeckFilter = deck
            )

            val baseWords = if (deck != null) {
                wordRepository.getWordsByDeckSync(deck)
            } else {
                wordRepository.getRandomWords(limit = count.coerceAtLeast(10))
            }

            val targetWords = baseWords.shuffled().take(count)
            val generatedQuestions = mutableListOf<QuizQuestion>()

            for (target in targetWords) {
                val distractors = wordRepository.getRandomDistractors(excludeId = target.id, limit = 3)
                val allChoices = (distractors + target).shuffled()
                val correctIndex = allChoices.indexOf(target)

                val question = when (mode) {
                    QuizMode.CLASSIC_AR_TO_TR -> QuizQuestion(
                        targetWord = target,
                        questionPrompt = target.arabic,
                        options = allChoices.map { it.turkishMeaning },
                        correctOptionIndex = correctIndex
                    )
                    QuizMode.CLASSIC_TR_TO_AR -> QuizQuestion(
                        targetWord = target,
                        questionPrompt = target.turkishMeaning,
                        options = allChoices.map { "${it.arabic} (${it.transliteration})" },
                        correctOptionIndex = correctIndex
                    )
                    QuizMode.AUDIO_QUIZ -> QuizQuestion(
                        targetWord = target,
                        questionPrompt = "Kelimeyi dinleyin ve doğru anlamı seçin:",
                        options = allChoices.map { it.turkishMeaning },
                        correctOptionIndex = correctIndex
                    )
                    QuizMode.AYAH_GAP_FILL -> {
                        val maskedAyah = if (target.exampleAyahArabic.contains(target.arabic)) {
                            target.exampleAyahArabic.replace(target.arabic, " [ ..... ] ")
                        } else {
                            target.exampleAyahArabic
                        }
                        QuizQuestion(
                            targetWord = target,
                            questionPrompt = "Ayetteki boşluğa hangi kelime gelmelidir?",
                            ayahContext = maskedAyah,
                            ayahTranslation = target.exampleAyahTranslation,
                            options = allChoices.map { "${it.arabic} (${it.turkishMeaning})" },
                            correctOptionIndex = correctIndex
                        )
                    }
                }
                generatedQuestions.add(question)
            }

            _uiState.value = QuizUiState(
                currentMode = mode,
                questionCount = count,
                selectedDeckFilter = deck,
                questions = generatedQuestions,
                currentQuestionIndex = 0,
                totalQuestions = generatedQuestions.size,
                selectedOptionIndex = null,
                isAnswerChecked = false,
                isCorrect = false,
                score = 0,
                isQuizFinished = false,
                isLoading = false
            )

            if (mode == QuizMode.AUDIO_QUIZ && generatedQuestions.isNotEmpty()) {
                playQuestionAudio(generatedQuestions.first().targetWord)
            }
        }
    }

    fun selectOption(index: Int) {
        if (_uiState.value.isAnswerChecked) return

        val state = _uiState.value
        val currentQ = state.questions.getOrNull(state.currentQuestionIndex) ?: return
        val isCorrect = (index == currentQ.correctOptionIndex)

        viewModelScope.launch {
            wordRepository.recordReview(currentQ.targetWord, remembered = isCorrect)
            preferencesRepository.recordActivityToday()
        }

        _uiState.value = state.copy(
            selectedOptionIndex = index,
            isAnswerChecked = true,
            isCorrect = isCorrect,
            score = if (isCorrect) state.score + 1 else state.score
        )
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentQuestionIndex + 1

        if (nextIndex >= state.questions.size) {
            _uiState.value = state.copy(
                isQuizFinished = true,
                isAnswerChecked = false,
                selectedOptionIndex = null
            )
        } else {
            _uiState.value = state.copy(
                currentQuestionIndex = nextIndex,
                selectedOptionIndex = null,
                isAnswerChecked = false,
                isCorrect = false
            )
            val nextQ = state.questions[nextIndex]
            if (state.currentMode == QuizMode.AUDIO_QUIZ) {
                playQuestionAudio(nextQ.targetWord)
            }
        }
    }

    fun playQuestionAudio(word: WordEntity) {
        audioPlayerManager.speakArabic(word.arabic, fallbackUrl = word.exampleAyahAudioUrl)
    }

    fun playAyahAudio(word: WordEntity) {
        audioPlayerManager.play(word.exampleAyahAudioUrl, fallbackArabicText = word.exampleAyahArabic)
    }
}

class QuizViewModelFactory(
    private val wordRepository: WordRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return QuizViewModel(wordRepository, preferencesRepository, audioPlayerManager) as T
    }
}
