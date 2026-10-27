package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: Int, // 1'den 300'e frekans sırası
    val arabic: String, // Harekeli Arapça
    val transliteration: String, // Okunuş
    val turkishMeaning: String, // Türkçe anlam
    val wordType: String = "İsim", // İsim, Fiil, Harf / Edat, Zamir
    val frequency: Int, // Kur'an'daki tekrar sayısı
    val level: Int, // 1: 1-50, 2: 51-100, 3: 101-200, 4: 201-300
    val deck: Int = 1, // 1..12 (her deste 25 kelime)
    val audioUrl: String, // Kelime telaffuz URL'si
    val exampleAyahArabic: String, // Örnek ayet metni
    val exampleAyahTranslation: String, // Örnek ayet meali
    val exampleAyahReference: String = "1:1", // Sure:Ayet referansı
    val exampleAyahAudioUrl: String, // Ayet seslendirme URL'si
    val isLearned: Boolean = false, // Öğrenildi mi?
    val reviewCount: Int = 0, // Kaç kez tekrar edildi?
    val nextReviewTimestamp: Long = 0L, // Bir sonraki SRS tekrar zamanı
    val srsIntervalDays: Int = 1, // Aralıklı tekrar gün sayısı
    val srsEaseFactor: Float = 2.5f // Kolaylık çarpanı
)
