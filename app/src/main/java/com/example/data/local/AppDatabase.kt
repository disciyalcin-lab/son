package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.WordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [WordEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quran_vocab_300.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDatabase(database.wordDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val dao = database.wordDao()
                        val existing = dao.getAllWordsSync()
                        val needsRefresh = existing.size != 300 ||
                            existing.any { it.frequency < 25 } ||
                            existing.zipWithNext().any { (a, b) -> a.frequency < b.frequency || a.id > b.id }

                        if (needsRefresh) {
                            // Preserve user learned status by Arabic word when refreshing dataset
                            val learnedArabicSet = existing.filter { it.isLearned }.map { it.arabic }.toSet()
                            val freshWords = QuranWordDataProvider.getInitialWords().map { word ->
                                if (word.arabic in learnedArabicSet) {
                                    word.copy(isLearned = true, reviewCount = 1)
                                } else {
                                    word
                                }
                            }
                            dao.deleteAllWords()
                            dao.insertAll(freshWords)
                        }
                    }
                }
            }

            private suspend fun populateInitialDatabase(dao: WordDao) {
                val words = QuranWordDataProvider.getInitialWords()
                dao.insertAll(words)
            }
        }
    }
}
