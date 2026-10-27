package com.example.data.repository

import com.example.data.local.WordDao
import com.example.data.model.WordEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

class WordRepository(private val wordDao: WordDao) {

    val allWords: Flow<List<WordEntity>> = wordDao.getAllWords()
    val learnedWords: Flow<List<WordEntity>> = wordDao.getLearnedWords()
    val learnedCount: Flow<Int> = wordDao.getLearnedCount()
    val learnedFrequencySum: Flow<Int?> = wordDao.getLearnedFrequencySum()

    fun getWordsByLevel(level: Int): Flow<List<WordEntity>> = wordDao.getWordsByLevel(level)

    fun getWordsByDeck(deck: Int): Flow<List<WordEntity>> = wordDao.getWordsByDeck(deck)

    suspend fun getWordsByDeckSync(deck: Int): List<WordEntity> = wordDao.getWordsByDeckSync(deck)

    suspend fun getWordById(id: Int): WordEntity? = wordDao.getWordById(id)

    suspend fun getWeakestOrNextWord(): WordEntity? = wordDao.getWeakestOrNextWord()

    suspend fun getRandomDistractors(excludeId: Int, limit: Int = 3): List<WordEntity> =
        wordDao.getRandomDistractors(excludeId, limit)

    suspend fun getRandomWords(limit: Int = 10): List<WordEntity> =
        wordDao.getRandomWords(limit)

    suspend fun updateWord(word: WordEntity) = wordDao.updateWord(word)

    suspend fun recordReview(word: WordEntity, remembered: Boolean) {
        val now = System.currentTimeMillis()
        val dayInMillis = 86_400_000L

        if (remembered) {
            val newReviewCount = word.reviewCount + 1
            val newInterval = when (newReviewCount) {
                1 -> 1
                2 -> 3
                3 -> 7
                4 -> 14
                else -> (word.srsIntervalDays * word.srsEaseFactor).toInt().coerceAtLeast(1)
            }
            val newEase = (word.srsEaseFactor + 0.1f).coerceAtMost(3.0f)
            val nextReview = now + (newInterval * dayInMillis)

            wordDao.updateSrsProgress(
                id = word.id,
                isLearned = true,
                nextReviewTimestamp = nextReview,
                srsIntervalDays = newInterval,
                srsEaseFactor = newEase
            )
        } else {
            // Failed recall: reset interval to 1 day, decrease ease factor
            val newEase = max(1.3f, word.srsEaseFactor - 0.2f)
            val nextReview = now + (1 * dayInMillis)

            wordDao.updateSrsProgress(
                id = word.id,
                isLearned = false,
                nextReviewTimestamp = nextReview,
                srsIntervalDays = 1,
                srsEaseFactor = newEase
            )
        }
    }

    suspend fun resetAllProgress() = wordDao.resetAllProgress()
}
