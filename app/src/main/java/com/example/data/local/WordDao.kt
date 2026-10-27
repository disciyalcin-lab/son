package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY frequency DESC, id ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words ORDER BY frequency DESC, id ASC")
    suspend fun getAllWordsSync(): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Int): WordEntity?

    @Query("SELECT * FROM words WHERE level = :level ORDER BY frequency DESC, id ASC")
    fun getWordsByLevel(level: Int): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE deck = :deck ORDER BY frequency DESC, id ASC")
    fun getWordsByDeck(deck: Int): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE deck = :deck ORDER BY frequency DESC, id ASC")
    suspend fun getWordsByDeckSync(deck: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE isLearned = 1 ORDER BY frequency DESC, id ASC")
    fun getLearnedWords(): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words WHERE isLearned = 1")
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getTotalWordsCount(): Int

    @Query("SELECT SUM(frequency) FROM words WHERE isLearned = 1")
    fun getLearnedFrequencySum(): Flow<Int?>

    @Query("SELECT * FROM words WHERE nextReviewTimestamp > 0 AND nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC, frequency DESC")
    suspend fun getDueWords(currentTime: Long): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY isLearned ASC, reviewCount ASC, frequency DESC, id ASC LIMIT 1")
    suspend fun getWeakestOrNextWord(): WordEntity?

    @Query("SELECT * FROM words WHERE id != :excludeId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomDistractors(excludeId: Int, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(limit: Int): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    @Update
    suspend fun updateWord(word: WordEntity)

    @Query("""
        UPDATE words 
        SET isLearned = :isLearned, 
            reviewCount = reviewCount + 1, 
            nextReviewTimestamp = :nextReviewTimestamp,
            srsIntervalDays = :srsIntervalDays,
            srsEaseFactor = :srsEaseFactor
        WHERE id = :id
    """)
    suspend fun updateSrsProgress(
        id: Int,
        isLearned: Boolean,
        nextReviewTimestamp: Long,
        srsIntervalDays: Int,
        srsEaseFactor: Float
    )

    @Query("UPDATE words SET isLearned = 0, reviewCount = 0, nextReviewTimestamp = 0, srsIntervalDays = 1, srsEaseFactor = 2.5")
    suspend fun resetAllProgress()
}
