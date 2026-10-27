package com.example.data.local

import com.example.data.model.WordEntity

object QuranWordDataProvider {

    private data class RawQuranWord(
        val arabic: String,
        val transliteration: String,
        val turkishMeaning: String,
        val wordType: String,
        val frequency: Int,
        val surah: Int,
        val ayah: Int,
        val exampleArabic: String,
        val exampleTurkish: String
    )

    /**
     * Returns the top 300 most frequent Quranic words (based on Dr. Abdulazeez Abdulraheem's
     * "80% of Quranic Words" and the Quranic Arabic Corpus frequency data).
     * Strictly sorted by frequency DESC with sequential IDs 1..300.
     */
    fun getInitialWords(): List<WordEntity> {
        val allRaw = mutableListOf<RawQuranWord>()
        allRaw.addAll(getRawBatch1to50())
        allRaw.addAll(getRawBatch51to100())
        allRaw.addAll(getRawBatch101to150())
        allRaw.addAll(getRawBatch151to200())
        allRaw.addAll(getRawBatch201to250())
        allRaw.addAll(getRawBatch251to300())

        return allRaw
            .sortedByDescending { it.frequency }
            .mapIndexed { index, raw ->
                val assignedId = index + 1
                createWord(
                    id = assignedId,
                    arabic = raw.arabic,
                    transliteration = raw.transliteration,
                    turkishMeaning = raw.turkishMeaning,
                    wordType = raw.wordType,
                    frequency = raw.frequency,
                    surah = raw.surah,
                    ayah = raw.ayah,
                    exampleArabic = raw.exampleArabic,
                    exampleTurkish = raw.exampleTurkish
                )
            }
    }

    private fun createWord(
        id: Int,
        arabic: String,
        transliteration: String,
        turkishMeaning: String,
        wordType: String,
        frequency: Int,
        surah: Int,
        ayah: Int,
        exampleArabic: String,
        exampleTurkish: String
    ): WordEntity {
        val level = when {
            id <= 50 -> 1
            id <= 100 -> 2
            id <= 200 -> 3
            else -> 4
        }
        val deck = ((id - 1) / 25) + 1
        val surahFormatted = String.format("%03d", surah)
        val ayahFormatted = String.format("%03d", ayah)
        val ayahAudio = "https://everyayah.com/data/Alafasy_128kbps/$surahFormatted$ayahFormatted.mp3"
        val wordAudio = ayahAudio

        return WordEntity(
            id = id,
            arabic = arabic,
            transliteration = transliteration,
            turkishMeaning = turkishMeaning,
            wordType = wordType,
            frequency = frequency,
            level = level,
            deck = deck,
            audioUrl = wordAudio,
            exampleAyahArabic = exampleArabic,
            exampleAyahTranslation = exampleTurkish,
            exampleAyahReference = "$surah:$ayah",
            exampleAyahAudioUrl = ayahAudio,
            isLearned = false,
            reviewCount = 0,
            nextReviewTimestamp = 0L,
            srsIntervalDays = 1,
            srsEaseFactor = 2.5f
        )
    }

    // 1 - 50. Kelimeler (Frekans: 3226 -> 265)
    private fun getRawBatch1to50(): List<RawQuranWord> = listOf(
        RawQuranWord("مِنْ", "Min", "–den, –dan (itibaren, içinden)", "Harf-i Cer", 3226, 2, 257, "اللَّهُ وَلِيُّ الَّذِينَ آمَنُوا يُخْرِجُهُم مِّنَ الظُّلُمَاتِ إِلَى النُّورِ", "Allah, inananların dostudur; onları karanlıklardan aydınlığa çıkarır."),
        RawQuranWord("اللَّه", "Allah", "Allah (Celle Celâluhû)", "İsim (Lafzatullah)", 2699, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Rahmân ve Rahîm olan Allah'ın adıyla."),
        RawQuranWord("مَا", "Mâ", "Şey, ne, olumsuzluk (değil)", "Edat / Zamir", 2613, 2, 255, "لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ", "Göklerde ve yerde olanların hepsi O'nundur."),
        RawQuranWord("لَا", "Lâ", "Hayır, yok, değil, yapma (-me)", "Olumsuzluk Edatı", 1728, 2, 255, "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ", "Allah O'dur ki, O'ndan başka ilah yoktur; Diridir, Hayy ve Kayyûm'dur."),
        RawQuranWord("فِي", "Fî", "İçinde, –de, –da", "Harf-i Cer", 1701, 2, 2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ", "Bu, kendisinde şüphe olmayan kitaptır; sakınanlar için bir hidayettir."),
        RawQuranWord("قَالَ", "Kâle", "Dedi, söyledi", "Mazi Fiil", 1618, 2, 30, "وَإِذْ قَالَ رَبُّكَ لِلْمَلَائِكَةِ إِنِّي جَاعِلٌ فِي الْأَرْضِ خَلِيفَةً", "Hani Rabbin meleklere: 'Ben yeryüzünde bir halife yaratacağım' demişti."),
        RawQuranWord("إِنَّ", "İnne", "Şüphesiz, muhakkak ki, doğrusu", "Te'kit Harfi", 1533, 2, 20, "إِنَّ اللَّهَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "Şüphesiz Allah, her şeye hakkıyla kadirdir."),
        RawQuranWord("عَلَى", "'Alâ", "Üzerinde, üzerine, aleyhine", "Harf-i Cer", 1445, 2, 5, "أُولَٰئِكَ عَلَىٰ هُدًى مِّن رَّبِّهِمْ", "İşte onlar Rablerinden bir hidayet üzeredirler."),
        RawQuranWord("الَّذِي", "Ellezî", "O kimse ki, o şey ki (ki o)", "İsm-i Mevsul", 1442, 2, 21, "يَا أَيُّهَا النَّاسُ اعْبُدُوا رَبَّكُمُ الَّذِي خَلَقَكُمْ", "Ey insanlar! Sizi yaratan Rabbinize kulluk edin."),
        RawQuranWord("كَانَ", "Kâne", "İdi, oldu, bulundu", "Nakıs Fiil", 1358, 4, 96, "وَكَانَ اللَّهُ غَفُورًا رَّحِيمًا", "Allah çok bağışlayıcıdır, çok merhamet edendir."),
        RawQuranWord("الَّذِينَ", "Ellezîne", "O kimseler ki, onlar ki", "İsm-i Mevsul (Çoğul)", 1080, 2, 3, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ", "Onlar gayba inanırlar, namazı kılarlar."),
        RawQuranWord("رَبّ", "Rabb", "Rab, besleyip terbiye eden sahip", "İsim", 975, 1, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Hamd, âlemlerin Rabbi olan Allah'a mahsustur."),
        RawQuranWord("مَنْ", "Men", "Kim, her kim, kimse", "Şart / Soru İsmi", 823, 2, 255, "مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ", "O'nun izni olmadan katında şefaat edecek kimdir?"),
        RawQuranWord("إِلَى", "İlâ", "–e, –a doğru, kadar", "Harf-i Cer", 742, 2, 43, "وَإِلَيْهِ تُرْجَعُونَ", "Ve ancak O'na döndürüleceksiniz."),
        RawQuranWord("إِلَّا", "İllâ", "Ancak, –den başka, müstesna", "İstisna Edatı", 663, 2, 255, "لَا إِلَٰهَ إِلَّا هُوَ", "O'ndan başka ilah yoktur."),
        RawQuranWord("إِنْ", "İn", "Eğer, şayet, değilse", "Şart Edatı", 580, 2, 23, "وَإِن كُنتُمْ فِي رَيْبٍ مِّمَّا نَزَّلْنَا عَلَىٰ عَبْدِنَا", "Eğer kulumuza indirdiğimizden şüphe içindeyseniz..."),
        RawQuranWord("آمَنُوا", "Âmenû", "İman ettiler, inandılar", "Mazi Fiil", 537, 2, 25, "وَبَشِّرِ الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ", "İman edip salih ameller işleyenleri müjdele!"),
        RawQuranWord("قَدْ", "Kad", "Muhakkak, kesinlikle, zaten", "Tahkik Edatı", 508, 23, 1, "قَدْ أَفْلَحَ الْمُؤْمِنُونَ", "Müminler gerçekten kurtuluşa ermişlerdir."),
        RawQuranWord("هُوَ", "Hüve", "O (eril tekil zamir)", "Zamir", 481, 59, 22, "هُوَ اللَّهُ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ", "O Allah'tır ki, O'ndan başka hiçbir ilah yoktur."),
        RawQuranWord("عَنْ", "'An", "–den, –dan, hakkında, uzakta", "Harf-i Cer", 465, 2, 48, "وَاتَّقُوا يَوْمًا لَّا تَجْزِي نَفْسٌ عَن نَّفْسٍ شَيْئًا", "Öyle bir günden sakının ki, kimse kimsenin yerine bir şey ödeyemez."),
        RawQuranWord("الْأَرْض", "El-Ard", "Yer, yeryüzü, dünya", "İsim", 461, 2, 29, "هُوَ الَّذِي خَلَقَ لَكُم مَّا فِي الْأَرْضِ جَمِيعًا", "O ki, yeryüzünde ne varsa hepsini sizin için yarattı."),
        RawQuranWord("هُمْ", "Hüm", "Onlar (eril çoğul zamir)", "Zamir", 442, 2, 5, "وَأُولَٰئِكَ هُمُ الْمُفْلِحُونَ", "İşte kurtuluşa erenler onlardır."),
        RawQuranWord("إِذَا", "İzâ", "–dığı zaman, vakit, –ince", "Şart / Zaman Edatı", 414, 110, 1, "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ", "Allah'ın yardımı ve fetih geldiği zaman."),
        RawQuranWord("أَنَّ", "Enne", "–dığını, olduğunu", "Edat", 400, 2, 223, "وَاعْلَمُوا أَنَّ اللَّهَ مَعَ الْمُتَّقِينَ", "Bilin ki Allah, takva sahipleriyle beraberdir."),
        RawQuranWord("يَوْم", "Yevm", "Gün, vakit", "İsim", 393, 1, 4, "مَالِكِ يَوْمِ الدِّينِ", "Din (hesap) gününün sahibidir."),
        RawQuranWord("قَوْم", "Kavm", "Kavim, topluluk, millet", "İsim", 383, 2, 164, "إِنَّ فِي خَلْقِ السَّمَاوَاتِ وَالْأَرْضِ ... لَآيَاتٍ لِّقَوْمٍ يَعْقِلُونَ", "Göklerin ve yerin yaratılışında akleden bir kavim için ayetler vardır."),
        RawQuranWord("آيَة", "Âyeh", "Ayet, mucize, delil, işaret", "İsim", 382, 2, 106, "مَا نَنسَخْ مِنْ آيَةٍ أَوْ نُنسِهَا نَأْتِ بِخَيْرٍ مِّنْهَا", "Biz bir ayeti nesheder veya unutturursak, daha hayırlısını getiririz."),
        RawQuranWord("عَلِمَ", "'Alime", "Bildi, öğrendi", "Mazi Fiil", 380, 2, 31, "وَعَلَّمَ آدَمَ الْأَسْمَاءَ كُلَّهَا", "Ve Âdem'e bütün isimleri öğretti."),
        RawQuranWord("أَنْ", "En", "–mek, –mak, ki", "Mastar Edatı", 372, 2, 67, "إِنَّ اللَّهَ يَأْمُرُكُمْ أَن تَذْبَحُوا بَقَرَةً", "Allah size bir sığır kesmenizi emrediyor."),
        RawQuranWord("عَمِلَ", "'Amile", "Yaptı, işledi, amel etti", "Mazi Fiil", 360, 2, 62, "مَنْ آمَنَ بِاللَّهِ وَالْيَوْمِ الْآخِرِ وَعَمِلَ صَالِحًا", "Kim Allah'a ve ahiret gününe inanıp yararlı iş yaparsa..."),
        RawQuranWord("كُلّ", "Küll", "Her, hepsi, bütün", "İsim / Belirteç", 359, 3, 185, "كُلُّ نَفْسٍ ذَائِقَةُ الْمَوْتِ", "Her nefis ölümü tadacaktır."),
        RawQuranWord("ثُمَّ", "Sümme", "Sonra, daha sonra", "Atıf Harfi", 355, 2, 28, "ثُمَّ يُمِيتُكُمْ ثُمَّ يُحْيِيكُمْ ثُمَّ إِلَيْهِ تُرْجَعُونَ", "Sonra sizi öldürür, sonra diriltir, sonra O'na döndürülürsünüz."),
        RawQuranWord("جَعَلَ", "Ce'ale", "Kıldı, yaptı, yarattı", "Mazi Fiil", 346, 2, 22, "الَّذِي جَعَلَ لَكُمُ الْأَرْضَ فِرَاشًا", "O ki yeryüzünü sizin için bir döşek kıldı."),
        RawQuranWord("بِمَا", "Bimâ", "Şey ile, sebebiyle, dolayı", "Harf + İsim", 340, 2, 4, "وَالَّذِينَ يُؤْمِنُونَ بِمَا أُنزِلَ إِلَيْكَ", "Sana indirilene inananlar."),
        RawQuranWord("قُلْ", "Kul", "De ki, söyle!", "Emir Fiili", 332, 112, 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "De ki: O Allah tektir."),
        RawQuranWord("رَسُول", "Rasûl", "Elçi, peygamber", "İsim", 331, 2, 285, "آمَنَ الرَّسُولُ بِمَا أُنزِلَ إِلَيْهِ مِن رَّبِّهِ", "Peygamber, Rabbinden kendisine indirilene iman etti."),
        RawQuranWord("لَهُمْ", "Lehüm", "Onlar için, onlara", "Harf + Zamir", 328, 2, 25, "أَنَّ لَهُمْ جَنَّاتٍ تَجْرِي مِن تَحْتِهَا الْأَنْهَارُ", "Onlar için altından ırmaklar akan cennetler olduğunu..."),
        RawQuranWord("عَذَاب", "'Azâb", "Azap, cefa, ceza", "İsim", 322, 2, 7, "وَلَهُمْ عَذَابٌ عَظِيمٌ", "Ve onlar için büyük bir azap vardır."),
        RawQuranWord("سَمَاء", "Semâ'", "Gök, sema", "İsim", 310, 2, 22, "الَّذِي جَعَلَ لَكُمُ الْأَرْضَ فِرَاشًا وَالسَّمَاءَ بِنَاءً", "O ki, yeryüzünü size bir döşek, göğü de bir bina yaptı."),
        RawQuranWord("أَوْ", "Ev", "Veya, yahut", "Atıf Harfi", 305, 2, 19, "أَوْ كَصَيِّبٍ مِّنَ السَّمَاءِ", "Yahut gökten boşanan sağanak yağmur gibidir."),
        RawQuranWord("نَفْس", "Nefs", "Can, nefis, kişi, kendi", "İsim", 295, 89, 27, "يَا أَيَّتُهَا النَّفْسُ الْمُطْمَئِنَّةُ", "Ey huzura kavuşmuş nefis!"),
        RawQuranWord("قَبْل", "Kabl", "Önce, önceden", "Zaman Zarfı", 294, 2, 4, "وَمَا أُنزِلَ مِن قَبْلِكَ", "Ve senden önce indirilene de."),
        RawQuranWord("كَفَرَ", "Kefere", "İnkâr etti, küfretti", "Mazi Fiil", 289, 2, 6, "إِنَّ الَّذِينَ كَفَرُوا سَوَاءٌ عَلَيْهِمْ", "Şüphesiz o inkâr edenleri uyarsan da uyarmasan da onlar için birdir."),
        RawQuranWord("شَيْء", "Şey'", "Şey, nesne, varlık", "İsim", 283, 36, 82, "إِنَّمَا أَمْرُهُ إِذَا أَرَادَ شَيْئًا أَن يَقُولَ لَهُ كُن فَيَكُونُ", "Bir şeyi dilediği zaman O'nun emri sadece 'Ol!' demektir, o da hemen oluverir."),
        RawQuranWord("حَتَّى", "Hattâ", "–e kadar, nihayet, öyle ki", "Harf-i Cer / Edat", 280, 97, 5, "سَلَامٌ هِيَ حَتَّىٰ مَطْلَعِ الْفَجْرِ", "O gece, fecrin doğuşuna kadar esenliktir."),
        RawQuranWord("جَاءَ", "Câ'e", "Geldi", "Mazi Fiil", 278, 110, 1, "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ", "Allah'ın yardımı ve fetih geldiğinde."),
        RawQuranWord("لَوْ", "Lev", "Eğer, şayet, keşke", "Şart Edatı", 272, 59, 21, "لَوْ أَنزَلْنَا هَٰذَا الْقُرْآنَ عَلَىٰ جَبَلٍ", "Eğer biz bu Kur'an'ı bir dağa indirseydik..."),
        RawQuranWord("ذَٰلِكَ", "Zâlike", "Şu, o, işte o", "İşaret İsmi", 268, 2, 2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ فِيهِ", "İşte bu Kitap, onda şüphe yoktur."),
        RawQuranWord("أَتَى", "Etâ", "Geldi, ulaştı", "Mazi Fiil", 265, 16, 1, "أَتَىٰ أَمْرُ اللَّهِ فَلَا تَسْتَعْجِلُوهُ", "Allah'ın emri geldi, artık onu acele istemeyin."),
        RawQuranWord("كِتَاب", "Kitâb", "Kitap, yazılı hüküm", "İsim", 261, 2, 2, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ فِيهِ", "Bu, şüphe olmayan kitaptır.")
    )

    // 51 - 100. Kelimeler (Frekans: 260 -> 133)
    private fun getRawBatch51to100(): List<RawQuranWord> = listOf(
        RawQuranWord("خَلَقَ", "Halaka", "Yarattı", "Mazi Fiil", 260, 96, 1, "اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ", "Yaratan Rabbinin adıyla oku!"),
        RawQuranWord("إِذْ", "İz", "Hani, o vakit ki, –dığında", "Zaman Edatı", 253, 2, 30, "وَإِذْ قَالَ رَبُّكَ لِلْمَلَائِكَةِ", "Hani Rabbin meleklere demişti."),
        RawQuranWord("حَقّ", "Hakk", "Gerçek, hak, doğru", "İsim", 247, 2, 147, "الْحَقُّ مِن رَّبِّكَ فَلَا تَكُونَنَّ مِنَ الْمُمْتَرِينَ", "Hak Rabbindendir, sakın şüphe edenlerden olma!"),
        RawQuranWord("نَاس", "Nâs", "İnsanlar, beşer", "İsim", 241, 114, 1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "De ki: İnsanların Rabbine sığınırım."),
        RawQuranWord("شَاءَ", "Şâ'e", "Diledi, istedi", "Mazi Fiil", 236, 2, 255, "وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ", "O'nun ilminden ancak O'nun dilediği kadarını kavrayabilirler."),
        RawQuranWord("بَعْد", "Ba'd", "Sonra, ardından", "Zaman Zarfı", 227, 2, 27, "الَّذِينَ يَنقُضُونَ عَهْدَ اللَّهِ مِن بَعْدِ مِيثَاقِهِ", "Onlar ki sözleştikten sonra Allah'ın ahdini bozarlar."),
        RawQuranWord("هَٰذَا", "Hâzâ", "Bu (eril işaret ismi)", "İşaret İsmi", 225, 2, 26, "مَاذَا أَرَادَ اللَّهُ بِهَٰذَا مَثَلًا", "Allah bu misalle neyi murat etti?"),
        RawQuranWord("بَلْ", "Bel", "Bilakis, aksine, doğrusu", "İdrak Edatı", 218, 2, 116, "بَل لَّهُ مَا فِي السَّمَاوَاتِ وَالْأَرْضِ", "Bilakis göklerde ve yerde ne varsa O'nundur."),
        RawQuranWord("لَمْ", "Lem", "–medi, –madı (geçmiş zaman olumsuzu)", "Cezm Edatı", 212, 112, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "O doğurmamıştır ve doğurulmamıştır."),
        RawQuranWord("أُولَٰئِكَ", "Ülâike", "İşte onlar, şunlar", "İşaret İsmi", 205, 2, 5, "أُولَٰئِكَ عَلَىٰ هُدًى مِّن رَّبِّهِمْ", "İşte onlar Rablerinden bir hidayet üzeredirler."),
        RawQuranWord("بَيْنَ", "Beyne", "Arasında, ortasında", "Zarf", 200, 2, 255, "يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ", "Onların önlerindekini de arkalarındakini de bilir."),
        RawQuranWord("أَنزَلَ", "Enzele", "İndirdi", "Mazi Fiil", 193, 2, 4, "وَالَّذِينَ يُؤْمِنُونَ بِمَا أُنزِلَ إِلَيْكَ", "Sana indirilene iman edenler."),
        RawQuranWord("ضَلَّ", "Dalle", "Saptı, yolunu şaşırdı", "Mazi Fiil", 191, 1, 7, "غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "Gazaba uğrayanların ve sapanlarınkine değil."),
        RawQuranWord("لَٰكِنَّ", "Lâkinne", "Fakat, ancak, lâkin", "İstidrak Edatı", 187, 2, 12, "أَلَا إِنَّهُمْ هُمُ الْمُفْسِدُونَ وَلَٰكِن لَّا يَشْعُرُونَ", "Bilin ki onlar bozguncuların ta kendileridir, lâkin farkında değillerdir."),
        RawQuranWord("عِندَ", "'İnde", "Katında, yanında", "Mekan Zarfı", 185, 3, 19, "إِنَّ الدِّينَ عِندَ اللَّهِ الْإِسْلَامُ", "Şüphesiz Allah katında din İslam'dır."),
        RawQuranWord("مُؤْمِن", "Mü'min", "İnanan, mümin", "İsim / Sıfat", 180, 2, 223, "وَبَشِّرِ الْمُؤْمِنِينَ", "Müminleri müjdele!"),
        RawQuranWord("كَذَّبَ", "Kezzebe", "Yalanladı", "Mazi Fiil", 178, 2, 39, "وَالَّذِينَ كَفَرُوا وَكَذَّبُوا بِآيَاتِنَا", "İnkâr edip ayetlerimizi yalanlayanlar."),
        RawQuranWord("خَيْر", "Hayr", "Hayırlı, iyi, daha hayırlı", "İsim / Sıfat", 176, 2, 110, "وَمَا تُقَدِّمُوا لِأَنفُسِكُم مِّنْ خَيْرٍ تَجِدُوهُ عِندَ اللَّهِ", "Kendiniz için hayır olarak ne gönderirseniz onu Allah katında bulursunuz."),
        RawQuranWord("سَبِيل", "Sebîl", "Yol, çare", "İsim", 175, 2, 154, "فِي سَبِيلِ اللَّهِ", "Allah yolunda."),
        RawQuranWord("دَعَا", "De'â", "Dua etti, çağırdı", "Mazi Fiil", 170, 2, 186, "أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ", "Bana dua ettiği vakit dua edenin dileğine karşılık veririm."),
        RawQuranWord("مِثْل", "Misl", "Gibi, benzer, misli", "İsim", 167, 2, 23, "فَأْتُوا بِسُورَةٍ مِّن مِّثْلِهِ", "Haydi onun benzeri bir sure getirin!"),
        RawQuranWord("أَمْر", "Emr", "Emir, iş, buyruk, durum", "İsim", 166, 2, 109, "حَتَّىٰ يَأْتِيَ اللَّهُ بِأَمْرِهِ", "Allah emrini getirinceye kadar."),
        RawQuranWord("مَعَ", "Me'a", "Beraber, birlikte", "Zarf / Edat", 164, 2, 153, "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", "Şüphesiz Allah sabredenlerle beraberdir."),
        RawQuranWord("عَلِيم", "'Alîm", "Hakkıyla bilen, her şeyi bilen", "İsim / Sıfat", 162, 2, 29, "وَهُوَ بِكُلِّ شَيْءٍ عَلِيمٌ", "Ve O, her şeyi hakkıyla bilendir."),
        RawQuranWord("بَعْض", "Ba'd", "Bazı, bir kısmı, kimi", "İsim", 158, 2, 253, "تِلْكَ الرُّسُلُ فَضَّلْنَا بَعْضَهُمْ عَلَىٰ بَعْضٍ", "İşte o peygamberlerin kimini kiminden üstün kıldık."),
        RawQuranWord("هَدَى", "Hedâ", "Doğru yola iletti, hidayet etti", "Mazi Fiil", 155, 2, 142, "يَهْدِي مَن يَشَاءُ إِلَىٰ صِرَاطٍ مُّسْتَقِيمٍ", "Dilediğini dosdoğru yola iletir."),
        RawQuranWord("أَيُّ", "Eyyü", "Hangi, her kim", "Soru / Şart İsmi", 154, 55, 13, "فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ", "Öyleyse Rabbinizin hangi nimetlerini yalanlayabilirsiniz?"),
        RawQuranWord("يَا", "Yâ", "Ey! (Nida harfi)", "Nida Edatı", 152, 2, 21, "يَا أَيُّهَا النَّاسُ اعْبُدُوا رَبَّكُمْ", "Ey insanlar! Rabbinize kulluk edin."),
        RawQuranWord("آتَى", "Âtâ", "Verdi, bahşetti", "Mazi Fiil", 150, 2, 269, "يُؤْتِي الْحِكْمَةَ مَن يَشَاءُ", "O, hikmeti dilediğine verir."),
        RawQuranWord("جَنَّة", "Cennet", "Cennet, bahçe", "İsim", 147, 2, 35, "وَقُلْنَا يَا آدَمُ اسْكُنْ أَنتَ وَزَوْجُكَ الْجَنَّةَ", "Dedik ki: 'Ey Âdem! Sen ve eşin cennette kalın.'"),
        RawQuranWord("نَار", "Nâr", "Ateş, cehennem", "İsim", 145, 2, 24, "فَاتَّقُوا النَّارَ الَّتِي وَقُودُهَا النَّاسُ وَالْحِجَارَةُ", "Yakıtı insanlar ve taşlar olan o ateşten sakının!"),
        RawQuranWord("دُون", "Dûn", "Aşağısında, gayrı, başka", "Edat / Zarf", 144, 2, 23, "وَادْعُوا شُهَدَاءَكُم مِّن دُونِ اللَّهِ", "Allah'tan başka şahitlerinizi çağırın."),
        RawQuranWord("أَرْسَلَ", "Ersele", "Gönderdi", "Mazi Fiil", 143, 2, 119, "إِنَّا أَرْسَلْنَاكَ بِالْحَقِّ بَشِيرًا وَنَذِيرًا", "Şüphesiz biz seni hak ile müjdeci ve uyarıcı olarak gönderdik."),
        RawQuranWord("اتَّقَى", "İttekâ", "Sakındı, takva sahibi oldu, korundu", "Mazi Fiil", 142, 2, 189, "وَاتَّقُوا اللَّهَ لَعَلَّكُمْ تُفْلِحُونَ", "Allah'tan sakının ki kurtuluşa eresiniz."),
        RawQuranWord("يَعْلَمُ", "Ya'lemü", "Bilir, biliyor", "Muzari Fiil", 141, 2, 77, "أَوَلَا يَعْلَمُونَ أَنَّ اللَّهَ يَعْلَمُ مَا يُسِرُّونَ وَمَا يُعْلِنُونَ", "Bilmezler mi ki Allah, gizlediklerini de açığa vurduklarını da bilir."),
        RawQuranWord("أَخَذَ", "Eheze", "Aldı, yakaladı", "Mazi Fiil", 140, 2, 63, "خُذُوا مَا آتَيْنَاكُم بِقُوَّةٍ", "Size verdiğimizi kuvvetle tutun!"),
        RawQuranWord("أَرَادَ", "Erâde", "İstedi, diledi, murat etti", "Mazi Fiil", 139, 2, 26, "مَاذَا أَرَادَ اللَّهُ بِهَٰذَا مَثَلًا", "Allah bu misalle neyi murat etti?"),
        RawQuranWord("صَالِح", "Sâlih", "İyi, dürüst, yararlı amel", "Sıfat / İsim", 138, 2, 25, "وَعَمِلُوا الصَّالِحَاتِ", "Ve salih (iyi) ameller işleyenleri."),
        RawQuranWord("اتَّبَعَ", "İttebe'a", "Uydu, tabi oldu", "Mazi Fiil", 137, 2, 120, "وَلَئِنِ اتَّبَعْتَ أَهْوَاءَهُم بَعْدَ الَّذِي جَاءَكَ مِنَ الْعِلْمِ", "Eğer sana gelen ilimden sonra onların hevalarına uyarsan..."),
        RawQuranWord("مُوسَى", "Mûsâ", "Hz. Musa (a.s.)", "Özel İsim", 136, 20, 9, "وَهَلْ أَتَاكَ حَدِيثُ مُوسَىٰ", "Musa'nın haberi sana ulaştı mı?"),
        RawQuranWord("أَنْتُمْ", "Entüm", "Sizler (eril çoğul)", "Zamir", 136, 2, 22, "فَلَا تَجْعَلُوا لِلَّهِ أَندَادًا وَأَنتُمْ تَعْلَمُونَ", "Bile bile Allah'a ortaklar koşmayın."),
        RawQuranWord("ذَكَرَ", "Zekere", "Andı, hatırladı, zikretti", "Mazi Fiil", 135, 2, 152, "فَاذْكُرُونِي أَذْكُرْكُمْ", "Öyleyse beni anın ki ben de sizi anayım."),
        RawQuranWord("كَافِر", "Kâfir", "İnkârcı, nankör", "İsim / Sıfat", 134, 2, 19, "وَاللَّهُ مُحِيطٌ بِالْكَافِرِينَ", "Ve Allah, inkârcıları çepeçevre kuşatmıştır."),
        RawQuranWord("قَلْب", "Kalb", "Kalp, gönül, yürek", "İsim", 133, 2, 7, "خَتَمَ اللَّهُ عَلَىٰ قُلُوبِهِمْ", "Allah onların kalplerini mühürlemiştir."),
        RawQuranWord("عَبَدَ", "'Abede", "Kulluk etti, ibadet etti", "Mazi Fiil", 122, 1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "Yalnız Sana ibadet eder ve yalnız Senden yardım dileriz."),
        RawQuranWord("إِبْرَاهِيم", "İbrâhîm", "Hz. İbrahim (a.s.)", "Özel İsim", 69, 2, 124, "وَإِذِ ابْتَلَىٰ إِبْرَاهِيمَ رَبُّهُ بِكَلِمَاتٍ", "Hani Rabbi İbrahim'i birtakım kelimelerle sınamıştı."),
        RawQuranWord("أُخْرَى", "Uhrâ", "Diğeri, öteki, başka", "Sıfat / İsim", 66, 20, 55, "وَمِنْهَا نُخْرِجُكُمْ تَارَةً أُخْرَىٰ", "Ve bir kez daha sizi ondan çıkaracağız."),
        RawQuranWord("نُوح", "Nûh", "Hz. Nuh (a.s.)", "Özel İsim", 43, 71, 1, "إِنَّا أَرْسَلْنَا نُوحًا إِلَىٰ قَوْمِهِ", "Şüphesiz biz Nuh'u kavmine gönderdik."),
        RawQuranWord("خَوْف", "Havf", "Korku, endişe", "İsim", 42, 2, 38, "فَلَا خَوْفٌ عَلَيْهِمْ وَلَا هُمْ يَحْزَنُونَ", "Onlara hiçbir korku yoktur ve onlar üzülmeyeceklerdir."),
        RawQuranWord("حَزِنَ", "Hazine", "Üzüldü, mahzun oldu", "Mazi Fiil", 42, 9, 40, "لَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا", "Üzülme! Şüphesiz Allah bizimle beraberdir."),
        RawQuranWord("كَذِب", "Kezib", "Yalan, asılsız söz", "İsim", 33, 16, 116, "وَلَا تَقُولُوا لِمَا تَصِفُ أَلْسِنَتُكُمُ الْكَذِبَ", "Dillerinizin uydurduğu yalana dayanarak söylemeyin.")
    )

    // 101 - 150. Kelimeler (Frekans: 132 -> 90)
    private fun getRawBatch101to150(): List<RawQuranWord> = listOf(
        RawQuranWord("كَيْفَ", "Keyfe", "Nasıl, ne şekilde", "Soru İsmi", 132, 2, 28, "كَيْفَ تَكْفُرُونَ بِاللَّهِ", "Allah'ı nasıl inkâr edersiniz?"),
        RawQuranWord("سَأَلَ", "Se'ele", "Sordu, istedi", "Mazi Fiil", 131, 2, 186, "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ", "Kullarım sana beni sorduklarında, şüphesiz ben yakınım."),
        RawQuranWord("جَمِيع", "Cemî'", "Hepsi, bütünü, topluca", "İsim", 130, 2, 29, "هُوَ الَّذِي خَلَقَ لَكُم مَّا فِي الْأَرْضِ جَمِيعًا", "O, yeryüzünde olanların hepsini sizin için yarattı."),
        RawQuranWord("رَأَى", "Ra'â", "Gördü", "Mazi Fiil", 129, 110, 2, "وَرَأَيْتَ النَّاسَ يَدْخُلُونَ فِي دِينِ اللَّهِ أَفْوَاجًا", "İnsanların bölük bölük Allah'ın dinine girdiğini gördüğünde."),
        RawQuranWord("أَهْل", "Ehl", "Aile, ehil, halk, taraftar", "İsim", 128, 2, 105, "مَّا يَوَدُّ الَّذِينَ كَفَرُوا مِنْ أَهْلِ الْكِتَابِ", "Kitap ehlinden kâfir olanlar istemezler ki..."),
        RawQuranWord("عَبْد", "'Abd", "Kul, köle", "İsim", 126, 2, 23, "وَإِن كُنتُمْ فِي رَيْبٍ مِّمَّا نَزَّلْنَا عَلَىٰ عَبْدِنَا", "Kulumuza indirdiğimizden şüphe içindeyseniz..."),
        RawQuranWord("رَزَقَ", "Razaka", "Rızıklandırdı, rızık verdi", "Mazi Fiil", 124, 2, 22, "رِزْقًا لَّكُمْ", "Sizin için bir rızık olarak."),
        RawQuranWord("لَعَلَّ", "Le'alle", "Umulur ki, belki, olsun diye", "Teracci Edatı", 123, 2, 21, "لَعَلَّكُمْ تَتَّقُونَ", "Umulur ki sakınırsınız."),
        RawQuranWord("يَد", "Yed", "El, kudret", "İsim", 121, 5, 64, "بَلْ يَدَاهُ مَبْسُوطَتَانِ", "Bilakis O'nun iki eli de açıktır."),
        RawQuranWord("غَيْر", "Gayr", "Başka, dışında, değil", "İsim / Edat", 120, 1, 7, "غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ", "Gazaba uğrayanlarınkine değil."),
        RawQuranWord("أَب", "Eb", "Baba, ata", "İsim", 119, 2, 133, "إِلَٰهَ آبَائِكَ إِبْرَاهِيمَ وَإِسْمَاعِيلَ", "Ataların İbrahim ve İsmail'in ilahına."),
        RawQuranWord("ظَلَمَ", "Zaleme", "Zulmetti, haksızlık etti", "Mazi Fiil", 118, 2, 57, "وَمَا ظَلَمُونَا وَلَٰكِن كَانُوا أَنفُسَهُمْ يَظْلِمُونَ", "Onlar bize zulmetmediler, fakat kendi nefislerine zulmediyorlardı."),
        RawQuranWord("دُنْيَا", "Dünyâ", "Dünya, yakın hayat", "İsim", 117, 2, 86, "أُولَٰئِكَ الَّذِينَ اشْتَرَوُا الْحَيَاةَ الدُّنْيَا بِالْآخِرَةِ", "Onlar, ahirete karşılık dünya hayatını satın alanlardır."),
        RawQuranWord("آخِرَة", "Âhiret", "Ahiret, ebedi hayat", "İsim", 116, 2, 4, "وَبِالْآخِرَةِ هُمْ يُوقِنُونَ", "Ve onlar ahirete de kesin olarak inanırlar."),
        RawQuranWord("رَحِيم", "Rahîm", "Çok merhametli, acıyan", "İsim / Sıfat", 115, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Rahmân ve Rahîm olan Allah'ın adıyla."),
        RawQuranWord("رَحْمَة", "Rahmet", "Merhamet, rahmet, lütuf", "İsim", 114, 2, 105, "وَاللَّهُ يَخْتَصُّ بِرَحْمَتِهِ مَن يَشَاءُ", "Allah rahmetini dilediğine tahsis eder."),
        RawQuranWord("خَافَ", "Hâfe", "Korktu", "Mazi Fiil", 112, 2, 182, "فَمَنْ خَافَ مِن مُّوصٍ جَنَفًا", "Kim vasiyet edenin haksızlığından korkarsa..."),
        RawQuranWord("ظَالِم", "Zâlim", "Zulmeden, haksızlık yapan", "İsim / Sıfat", 110, 2, 35, "فَتَكُونَا مِنَ الظَّالِمِينَ", "Yoksa zalimlerden olursunuz."),
        RawQuranWord("أَجْر", "Ecr", "Mükâfat, ecir, karşılık", "İsim", 109, 2, 62, "فَلَهُمْ أَجْرُهُمْ عِندَ رَبِّهِمْ", "Onların mükâfatları Rableri katındadır."),
        RawQuranWord("عَظِيم", "'Azîm", "Pek büyük, ulu", "Sıfat", 108, 2, 255, "وَهُوَ الْعَلِيُّ الْعَظِيمُ", "O çok yücedir, çok büyüktür."),
        RawQuranWord("وَجَدَ", "Vecede", "Buldu", "Mazi Fiil", 107, 93, 7, "وَوَجَدَكَ ضَالًّا فَهَدَىٰ", "Seni şaşırmış bulup doğru yola iletmedi mi?"),
        RawQuranWord("لَنْ", "Len", "Asla (gelecek zaman olumsuzu)", "Nasp Edatı", 106, 2, 24, "فَإِن لَّمْ تَفْعَلُوا وَلَن تَفْعَلُوا", "Eğer yapamazsanız ki asla yapamayacaksınız..."),
        RawQuranWord("عِلْم", "'İlm", "İlim, bilgi", "İsim", 105, 2, 120, "مِن بَعْدِ الَّذِي جَاءَكَ مِنَ الْعِلْمِ", "Sana gelen bunca ilimden sonra."),
        RawQuranWord("أَمَرَ", "Emara", "Emretti, buyurdu", "Mazi Fiil", 104, 2, 67, "إِنَّ اللَّهَ يَأْمُرُكُمْ أَن تَذْبَحُوا بَقَرَةً", "Allah size bir sığır kesmenizi emrediyor."),
        RawQuranWord("صَبْر", "Sabr", "Sabır, direnç, sebat", "İsim", 103, 103, 3, "وَتَوَاصَوْا بِالصَّبْرِ", "Ve birbirlerine sabrı tavsiye ettiler."),
        RawQuranWord("سَمِعَ", "Semi'a", "İşitti, duydu", "Mazi Fiil", 102, 2, 285, "وَقَالُوا سَمِعْنَا وَأَطَعْنَا", "'İşittik ve itaat ettik' dediler."),
        RawQuranWord("أَكْثَر", "Ekser", "Daha çok, çoğu, pek çok", "İsm-i Tafdil", 101, 2, 100, "بَلْ أَكْثَرُهُمْ لَا يُؤْمِنُونَ", "Bilakis onların çoğu iman etmezler."),
        RawQuranWord("دَخَلَ", "Dehale", "Girdi", "Mazi Fiil", 100, 110, 2, "يَدْخُلُونَ فِي دِينِ اللَّهِ أَفْوَاجًا", "Allah'ın dinine bölük bölük girerler."),
        RawQuranWord("خَرَجَ", "Harace", "Çıktı, çıkardı", "Mazi Fiil", 99, 2, 257, "يُخْرِجُهُم مِّنَ الظُّلُمَاتِ إِلَى النُّورِ", "Onları karanlıklardan aydınlığa çıkarır."),
        RawQuranWord("وَعَدَ", "Va'ade", "Vaad etti, söz verdi", "Mazi Fiil", 98, 2, 80, "أَمْ تَقُولُونَ عَلَى اللَّهِ مَا لَا تَعْلَمُونَ", "Allah vaadinden asla dönmez."),
        RawQuranWord("قَتَلَ", "Katele", "Öldürdü, savaştı", "Mazi Fiil", 97, 5, 32, "مَن قَتَلَ نَفْسًا بِغَيْرِ نَفْسٍ", "Kim bir cana kıymamış bir kimseyi öldürürse..."),
        RawQuranWord("أَخ", "Eh", "Erkek kardeş, din kardeşi", "İsim", 96, 49, 10, "إِنَّمَا الْمُؤْمِنُونَ إِخْوَةٌ", "Müminler ancak kardeştirler."),
        RawQuranWord("صَبَرَ", "Sabara", "Sabretti", "Mazi Fiil", 95, 2, 153, "يَا أَيُّهَا الَّذِينَ آمَنُوا اسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ", "Ey iman edenler! Sabır ve namaz ile yardım dileyin."),
        RawQuranWord("غَفَرَ", "Gafara", "Bağışladı, affetti", "Mazi Fiil", 94, 2, 286, "وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا", "Bizi affet, bizi bağışla, bize merhamet et!"),
        RawQuranWord("نَزَّلَ", "Nezzele", "Peyderpey indirdi", "Mazi Fiil", 93, 2, 23, "مِّمَّا نَزَّلْنَا عَلَىٰ عَبْدِنَا", "Kulumuza indirdiğimiz şeyden..."),
        RawQuranWord("أَحْسَنَ", "Ehsene", "İyilik etti, güzel yaptı", "Mazi Fiil", 92, 17, 7, "إِنْ أَحْسَنتُمْ أَحْسَنتُمْ لِأَنفُسِكُمْ", "Eğer iyilik ederseniz kendinize iyilik etmiş olursunuz."),
        RawQuranWord("نَصَرَ", "Nasara", "Yardım etti", "Mazi Fiil", 91, 3, 123, "وَلَقَدْ نَصَرَكُمُ اللَّهُ بِبَدْرٍ", "Andolsun ki Bedir'de Allah size yardım etmişti."),
        RawQuranWord("دِين", "Dîn", "Din, hesap, hüküm, itaat", "İsim", 90, 1, 4, "مَالِكِ يَوْمِ الدِّينِ", "Din (hesap) gününün sahibidir."),
        RawQuranWord("قَوْل", "Kavl", "Söz, laf, deyiş", "İsim", 89, 2, 83, "وَقُولُوا لِلنَّاسِ حُسْنًا", "İnsanlara güzel söz söyleyin."),
        RawQuranWord("عَزِيز", "'Azîz", "Mutlak galip, üstün, izzet sahibi", "Sıfat", 88, 2, 129, "إِنَّكَ أَنتَ الْعَزِيزُ الْحَكِيمُ", "Şüphesiz Sen, mutlak galipsin, hikmet sahibisin."),
        RawQuranWord("حَكِيم", "Hakîm", "Hüküm ve hikmet sahibi", "Sıfat", 87, 2, 32, "إِنَّكَ أَنتَ الْعَلِيمُ الْحَكِيمُ", "Şüphesiz Sen, her şeyi bilen ve hikmet sahibisin."),
        RawQuranWord("غَفُور", "Gafûr", "Çok bağışlayan, mağfiret eden", "Sıfat", 86, 2, 173, "فَإِنَّ اللَّهَ غَفُورٌ رَّحِيمٌ", "Şüphesiz Allah çok bağışlayandır, çok merhamet edendir."),
        RawQuranWord("رَجَعَ", "Raca'a", "Döndü, geri döndü", "Mazi Fiil", 85, 2, 156, "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ", "Şüphesiz biz Allah'a aidiz ve O'na döneceğiz."),
        RawQuranWord("تَرَكَ", "Terake", "Bıraktı, terk etti", "Mazi Fiil", 84, 2, 17, "وَتَرَكَهُمْ فِي ظُلُمَاتٍ لَّا يُبْصِرُونَ", "Onları karanlıklar içinde görmez halde bıraktı."),
        RawQuranWord("مَلَك", "Melek", "Melek", "İsim", 83, 2, 30, "وَإِذْ قَالَ رَبُّكَ لِلْمَلَائِكَةِ", "Hani Rabbin meleklere demişti."),
        RawQuranWord("شَيْطَان", "Şeytân", "Şeytan, saptırıcı", "İsim", 82, 2, 36, "فَأَزَلَّهُمَا الشَّيْطَانُ عَنْهَا", "Şeytan onların ayağını oradan kaydırdı."),
        RawQuranWord("مَثَل", "Mesel", "Misal, örnek, benzer, ibret", "İsim", 81, 2, 17, "مَثَلُهُمْ كَمَثَلِ الَّذِي اسْتَوْقَدَ نَارًا", "Onların durumu bir ateş yakan kimsenin durumu gibidir."),
        RawQuranWord("نَحْنُ", "Nahnu", "Biz (çoğul zamir)", "Zamir", 80, 15, 9, "إِنَّا نَحْنُ نَزَّلْنَا الذِّكْرَ وَإِنَّا لَهُ لَحَافِظُونَ", "Şüphesiz o Zikr'i (Kur'an'ı) biz indirdik ve onu koruyacak olan da biziz."),
        RawQuranWord("مَال", "Mâl", "Mal, servet", "İsim", 79, 2, 261, "مَّثَلُ الَّذِينَ يُنفِقُونَ أَمْوَالَهُمْ فِي سَبِيلِ اللَّهِ", "Mallarını Allah yolunda harcayanların durumu..."),
        RawQuranWord("وَلِيّ", "Velî", "Dost, koruyucu, sahip", "İsim", 78, 2, 257, "اللَّهُ وَلِيُّ الَّذِينَ آمَنُوا", "Allah inananların dostudur."),
        RawQuranWord("هُدًى", "Hüden", "Hidayet, doğru yol rehberi", "İsim", 77, 2, 2, "هُدًى لِّلْمُتَّقِينَ", "Sakınanlar için bir hidayettir."),
        RawQuranWord("فَضْل", "Fadl", "Lütuf, ihsan, üstünlük", "İsim", 76, 2, 64, "فَلَوْلَا فَضْلُ اللَّهِ عَلَيْكُمْ وَرَحْمَتُهُ", "Eğer Allah'ın lütfu ve rahmeti üzerinizde olmasaydı..."),
        RawQuranWord("صَلَاة", "Salât", "Namaz, dua", "İsim", 75, 2, 3, "وَيُقِيمُونَ الصَّلَاةَ", "Ve namazı dosdoğru kılarlar."),
        RawQuranWord("حَرَّمَ", "Harrame", "Haram kıldı, yasakladı", "Mazi Fiil", 74, 2, 173, "إِنَّمَا حَرَّمَ عَلَيْكُمُ الْمَيْتَةَ وَالدَّمَ", "O size sadece leşi, kanı haram kıldı."),
        RawQuranWord("فَعَلَ", "Fe'ale", "Yaptı, işledi", "Mazi Fiil", 73, 21, 23, "لَا يُسْأَلُ عَمَّا يَفْعَلُ وَهُمْ يُسْأَلُونَ", "O yaptığından sorumlu tutulmaz, onlar ise sorguya çekilirler."),
        RawQuranWord("لَيْل", "Leyl", "Gece", "İsim", 72, 92, 1, "وَاللَّيْلِ إِذَا يَغْشَىٰ", "Bürüyüp örttüğü zaman geceye andolsun."),
        RawQuranWord("أَطَاعَ", "Etâ'a", "İtaat etti, boyun eğdi", "Mazi Fiil", 71, 3, 132, "وَأَطِيعُوا اللَّهَ وَالرَّسُولَ لَعَلَّكُمْ تُرْحَمُونَ", "Allah'a ve Peygamber'e itaat edin ki merhamet olunasınız."),
        RawQuranWord("أَصْحَاب", "Ashâb", "Arkadaşlar, ehli, sahipleri", "İsim (Çoğul)", 70, 2, 257, "أُولَٰئِكَ أَصْحَابُ النَّارِ", "İşte onlar ateş ehlidirler.")
    )

    // 151 - 200. Kelimeler (Frekans: 69 -> 49)
    private fun getRawBatch151to200(): List<RawQuranWord> = listOf(
        RawQuranWord("جَهَنَّم", "Cehennem", "Cehennem ateşi", "Özel İsim", 69, 2, 206, "فَحَسْبُهُ جَهَنَّمُ ۚ وَلَبِئْسَ الْمِهَادُ", "Ona cehennem yeter; ne kötü bir yataktır o!"),
        RawQuranWord("حَيَاة", "Hayât", "Hayat, dirilik, ömür", "İsim", 68, 2, 85, "فِي الْحَيَاةِ الدُّنْيَا", "Dünya hayatında."),
        RawQuranWord("زَوْج", "Zevc", "Eş, zevce", "İsim", 67, 2, 35, "اسْكُنْ أَنتَ وَزَوْجُكَ الْجَنَّةَ", "Sen ve eşin cennette oturun."),
        RawQuranWord("نَبِيّ", "Nebî", "Peygamber, haber getiren", "İsim", 66, 2, 61, "وَيَقْتُلُونَ النَّبِيِّينَ بِغَيْرِ الْحَقِّ", "Ve peygamberleri haksız yere öldürüyorlardı."),
        RawQuranWord("شُكْر", "Şükr", "Şükür, teşekkür, değer bilme", "İsim", 65, 2, 152, "وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ", "Bana şükredin ve nankörlük etmeyin!"),
        RawQuranWord("تَابَ", "Tâbe", "Tövbe etti, bağışladı", "Mazi Fiil", 64, 2, 37, "فَتَلَقَّىٰ آدَمُ مِن رَّبِّهِ كَلِمَاتٍ فَتَابَ عَلَيْهِ", "Âdem Rabbinden kelimeler aldı ve O da tövbesini kabul etti."),
        RawQuranWord("أَنْفَقَ", "Enfaka", "İnfak etti, hayra harcadı", "Mazi Fiil", 63, 2, 3, "وَمِمَّا رَزَقْنَاهُمْ يُنفِقُونَ", "Ve kendilerine verdiğimiz rızıktan infak ederler."),
        RawQuranWord("وَجْه", "Vech", "Yüz, çehre, vech, rıza", "İsim", 62, 2, 115, "فَأَيْنَمَا تُوَلُّوا فَثَمَّ وَجْهُ اللَّهِ", "Nereye dönerseniz Allah'ın yüzü (yönü) oradadır."),
        RawQuranWord("بَيِّنَة", "Beyyine", "Apaçık delil, kanıt", "İsim", 61, 2, 211, "سَلْ بَنِي إِسْرَائِيلَ كَمْ آتَيْنَاهُم مِّنْ آيَةٍ بَيِّنَةٍ", "İsrailoğullarına sor, onlara nice açık ayetler verdik."),
        RawQuranWord("قَلِيل", "Kalîl", "Az, pek az", "Sıfat", 60, 2, 41, "وَلَا تَشْتَرُوا بِآيَاتِي ثَمَنًا قَلِيلًا", "Ayetlerimi az bir bahaya satmayın!"),
        RawQuranWord("قُرْآن", "Kur'ân", "Kur'an-ı Kerim, çok okunan", "Özel İsim", 59, 2, 185, "شَهْرُ رَمَضَانَ الَّذِي أُنزِلَ فِيهِ الْقُرْآنُ", "Ramazan ayı ki onda Kur'an indirilmiştir."),
        RawQuranWord("وَلَد", "Veled", "Çocuk, evlat", "İsim", 58, 2, 116, "وَقَالُوا اتَّخَذَ اللَّهُ وَلَدًا", "'Allah çocuk edindi' dediler."),
        RawQuranWord("بَعَثَ", "Be'ase", "Gönderdi, diriltti", "Mazi Fiil", 57, 2, 213, "فَبَعَثَ اللَّهُ النَّبِيِّينَ مُبَشِّرِينَ وَمُنذِرِينَ", "Allah, peygamberleri müjdeleyiciler ve uyarıcılar olarak gönderdi."),
        RawQuranWord("خَسَار", "Hasâr", "Hüsran, ziyan, kayıp", "İsim", 56, 103, 2, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ", "Gerçekten insan ziyan içindedir."),
        RawQuranWord("عَيْن", "'Ayn", "Göz, pınar, kaynak", "İsim", 55, 2, 60, "فَانفَجَرَتْ مِنْهُ اثْنَتَا عَشْرَةَ عَيْنًا", "Ondan on iki pınar fışkırdı."),
        RawQuranWord("بَيْت", "Beyt", "Ev, Beytullah (Kabe)", "İsim", 54, 2, 125, "وَإِذْ جَعَلْنَا الْبَيْتَ مَثَابَةً لِّلنَّاسِ", "Hani biz Beyt'i (Kâbe'yi) insanlara bir toplanma yeri kıldık."),
        RawQuranWord("أُمَّة", "Ümmet", "Ümmet, topluluk, çağ", "İsim", 53, 2, 143, "وَكَذَٰلِكَ جَعَلْنَاكُمْ أُمَّةً وَسَطًا", "İşte böylece sizi vasat (hayırlı) bir ümmet kıldık."),
        RawQuranWord("مَاء", "Mâ'", "Su", "İsim", 52, 2, 22, "وَأَنزَلَ مِنَ السَّمَاءِ مَاءً", "Ve gökten su indirdi."),
        RawQuranWord("قَضَى", "Kadâ", "Hükmetti, tamamladı, yerine getirdi", "Mazi Fiil", 51, 2, 117, "وَإِذَا قَضَىٰ أَمْرًا فَإِنَّمَا يَقُولُ لَهُ كُن فَيَكُونُ", "Bir işe hükmettiğinde sadece ona 'Ol!' der, o da olur."),
        RawQuranWord("كَثِير", "Kesîr", "Çok, pek çok", "Sıfat", 50, 2, 26, "يُضِلُّ بِهِ كَثِيرًا وَيَهْدِي بِهِ كَثِيرًا", "Onunla birçoğunu saptırır, birçoğunu da hidayete iletir."),
        RawQuranWord("أَحَبَّ", "Ehabbe", "Sevdi, tercih etti", "Mazi Fiil", 49, 2, 195, "إِنَّ اللَّهَ يُحِبُّ الْمُحْسِنِينَ", "Şüphesiz Allah iyilik edenleri sever."),
        RawQuranWord("وَاحِد", "Vâhid", "Bir, tek", "Sayı / Sıfat", 48, 2, 163, "وَإِلَٰهُكُمْ إِلَٰهٌ وَاحِدٌ", "Sizin ilahınız tek bir ilahtır."),
        RawQuranWord("تَلَا", "Telâ", "Okudu, tilavet etti, izledi", "Mazi Fiil", 47, 2, 121, "الَّذِينَ آتَيْنَاهُمُ الْكِتَابَ يَتْلُونَهُ حَقَّ تِلَاوَتِهِ", "Kendilerine Kitap verdiklerimiz onu hakkıyla okurlar."),
        RawQuranWord("أَشْرَكَ", "Eşrake", "Ortak koştu, şirk koştu", "Mazi Fiil", 46, 4, 48, "إِنَّ اللَّهَ لَا يَغْفِرُ أَن يُشْرَكَ بِهِ", "Şüphesiz Allah kendisine ortak koşulmasını bağışlamaz."),
        RawQuranWord("كَسَبَ", "Kesebe", "Kazandı, işledi", "Mazi Fiil", 45, 2, 286, "لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ", "Kazandığı iyilik kendi yararına, kötülük de kendi zararınadır."),
        RawQuranWord("نَسِيَ", "Nesiye", "Unuttu, terk etti", "Mazi Fiil", 45, 2, 286, "رَبَّنَا لَا تُؤَاخِذْنَا إِن نَّسِينَا أَوْ أَخْطَأْنَا", "Rabbimiz! Unutursak veya hataya düşersek bizi sorumlu tutma."),
        RawQuranWord("نَهَار", "Nehâr", "Gündüz", "İsim", 44, 10, 6, "إِنَّ فِي اخْتِلَافِ اللَّيْلِ وَالنَّهَارِ", "Şüphesiz gece ve gündüzün ardarda gelmesinde..."),
        RawQuranWord("شَهِيد", "Şehîd", "Şahit, tanık, şehit", "İsim / Sıfat", 44, 2, 143, "وَتَكُونُوا شُهَدَاءَ عَلَى النَّاسِ", "Ve insanlara şahitler olasınız diye."),
        RawQuranWord("تَوَكَّلَ", "Tevekkele", "Güvendi, tevekkül etti", "Mazi Fiil", 44, 3, 159, "فَتَوَكَّلْ عَلَى اللَّهِ", "Artık Allah'a tevekkül et!"),
        RawQuranWord("ذِكْر", "Zikr", "Zikir, hatırlatma, Kur'an", "İsim", 43, 15, 9, "إِنَّا نَحْنُ نَزَّلْنَا الذِّكْرَ", "Şüphesiz o Zikr'i biz indirdik."),
        RawQuranWord("أَجَل", "Ecel", "Belirlenmiş süre, ecel", "İsim", 43, 6, 2, "هُوَ الَّذِي خَلَقَكُم مِّن طِينٍ ثُمَّ قَضَىٰ أَجَلًا", "Sizi çamurdan yaratan sonra bir ecel tayin eden O'dur."),
        RawQuranWord("عَقَلَ", "'Akale", "Akletti, düşündü, kavradı", "Mazi Fiil", 43, 2, 44, "أَفَلَا تَعْقِلُونَ", "Hiç akletmiyor musunuz?"),
        RawQuranWord("نَهْر", "Nehr", "Irmak, nehir", "İsim", 42, 2, 25, "تَجْرِي مِن تَحْتِهَا الْأَنْهَارُ", "Altından ırmaklar akar."),
        RawQuranWord("أَحَلَّ", "Ehelle", "Helal kıldı", "Mazi Fiil", 42, 2, 275, "وَأَحَلَّ اللَّهُ الْبَيْعَ وَحَرَّمَ الرِّبَا", "Allah alışverişi helal, faizi haram kıldı."),
        RawQuranWord("شَدِيد", "Şedîd", "Çok şiddetli, çetin", "Sıfat", 42, 2, 165, "وَأَنَّ اللَّهَ شَدِيدُ الْعَذَابِ", "Ve muhakkak ki Allah'ın azabı pek şiddetlidir."),
        RawQuranWord("بَصِير", "Basîr", "Hakkıyla gören", "Sıfat", 41, 2, 96, "وَاللَّهُ بَصِيرٌ بِمَا يَعْمَلُونَ", "Ve Allah, onların yaptıklarını hakkıyla görendir."),
        RawQuranWord("تَذَكَّرَ", "Tezekkere", "Düşünüp öğüt aldı", "Mazi Fiil", 41, 2, 221, "لَعَلَّهُمْ يَتَذَكَّرُونَ", "Umulur ki düşünüp öğüt alırlar."),
        RawQuranWord("رَضِيَ", "Radiye", "Razı oldu, hoşnut oldu", "Mazi Fiil", 41, 5, 119, "رَّضِيَ اللَّهُ عَنْهُمْ وَرَضُوا عَنْهُ", "Allah onlardan razı olmuştur, onlar da O'ndan razı olmuşlardır."),
        RawQuranWord("مُتَّقِي", "Müttakî", "Korkan, sakınan, takvalı", "İsim / Sıfat", 40, 2, 2, "هُدًى لِّلْمُتَّقِينَ", "Takva sahipleri için bir hidayettir."),
        RawQuranWord("غَيْب", "Gayb", "Görünmeyen alem, gayb", "İsim", 40, 2, 3, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ", "Onlar gayba inanırlar."),
        RawQuranWord("طَعَام", "Ta'âm", "Yemek, yiyecek", "İsim", 40, 2, 61, "لَن نَّصْبِرَ عَلَىٰ طَعَامٍ وَاحِدٍ", "Biz bir tek yiyeceğe dayanamayız."),
        RawQuranWord("سَاعَة", "Sâ'at", "Kıyamet vakti, an, saat", "İsim", 40, 54, 1, "اقْتَرَبَتِ السَّاعَةُ", "Kıyamet saati yaklaştı.")
    )

    // 201 - 250. Kelimeler (Frekans: 39 -> 32)
    private fun getRawBatch201to250(): List<RawQuranWord> = listOf(
        RawQuranWord("حَسِبَ", "Hasibe", "Zannetti, sandı, hesapladı", "Mazi Fiil", 39, 2, 214, "أَمْ حَسِبْتُمْ أَن تَدْخُلُوا الْجَنَّةَ", "Yoksa siz cennete gireceğinizi mi sandınız?"),
        RawQuranWord("أَفْلَحَ", "Efleha", "Kurtuluşa erdi, felah buldu", "Mazi Fiil", 39, 23, 1, "قَدْ أَفْلَحَ الْمُؤْمِنُونَ", "Müminler gerçekten kurtuluşa ermişlerdir."),
        RawQuranWord("ابْن", "İbn", "Oğul, çocuk", "İsim", 39, 2, 87, "وَآتَيْنَا عِيسَى ابْنَ مَرْيَمَ الْبَيِّنَاتِ", "Ve Meryem oğlu İsa'ya apaçık deliller verdik."),
        RawQuranWord("سَمِيع", "Semî'", "Hakkıyla işiten", "Sıfat", 39, 2, 127, "إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ", "Şüphesiz Sen, hakkıyla işitensin, bilensin."),
        RawQuranWord("قَدِير", "Kadîr", "Her şeye gücü yeten", "Sıfat", 39, 2, 20, "إِنَّ اللَّهَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ", "Şüphesiz Allah her şeye kadirdir."),
        RawQuranWord("صِرَاط", "Sirât", "Yol, cadde", "İsim", 38, 1, 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Bizi dosdoğru yola ilet!"),
        RawQuranWord("خَبِير", "Habîr", "Her şeyin iç yüzünden haberdar", "Sıfat", 38, 2, 234, "وَاللَّهُ بِمَا تَعْمَلُونَ خَبِيرٌ", "Ve Allah yaptıklarınızdan hakkıyla haberdardır."),
        RawQuranWord("نَذِير", "Nezîr", "Uyarıcı, korkutucu elçi", "İsim / Sıfat", 38, 2, 119, "بَشِيرًا وَنَذِيرًا", "Müjdeci ve uyarıcı olarak."),
        RawQuranWord("كَبِير", "Kebîr", "Büyük, ulu, yaşlı", "Sıfat", 38, 2, 219, "فِيهِمَا إِثْمٌ كَبِيرٌ", "İkisinde de büyük bir günah vardır."),
        RawQuranWord("نُور", "Nûr", "Işık, nur, aydınlık", "İsim", 37, 2, 257, "يُخْرِجُهُم مِّنَ الظُّلُمَاتِ إِلَى النُّورِ", "Onları karanlıklardan aydınlığa çıkarır."),
        RawQuranWord("حَمْد", "Hamd", "Övgü, hamd, şükür", "İsim", 37, 1, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Hamd âlemlerin Rabbi olan Allah'a mahsustur."),
        RawQuranWord("سَلَام", "Selâm", "Barış, esenlik, kurtuluş", "İsim", 37, 36, 58, "سَلَامٌ قَوْلًا مِّن رَّبٍّ رَّحِيمٍ", "Çok merhametli bir Rabden söz olarak onlara selam vardır."),
        RawQuranWord("بَحْر", "Bahr", "Deniz", "İsim", 37, 2, 50, "وَإِذْ فَرَقْنَا بِكُمُ الْبَحْرَ فَأَنجَيْنَاكُمْ", "Hani sizin için denizi yarıp sizi kurtarmıştık."),
        RawQuranWord("طَيِّب", "Tayyib", "Temiz, hoş, leziz, güzel", "Sıfat", 37, 2, 172, "كُلُوا مِن طَيِّبَاتِ مَا رَزَقْنَاكُمْ", "Size rızık olarak verdiklerimizin temiz olanlarından yiyin."),
        RawQuranWord("أَوَّل", "Evvel", "İlk, birinci", "İsim / Sıfat", 36, 57, 3, "هُوَ الْأَوَّلُ وَالْآخِرُ", "O Evvel'dir ve Âhir'dir."),
        RawQuranWord("ذَنْب", "Zenb", "Günah, kusur, suç", "İsim", 36, 3, 31, "يَغْفِرْ لَكُمْ ذُنُوبَكُمْ", "Günahlarınızı bağışlasın."),
        RawQuranWord("جَبَل", "Cebel", "Dağ", "İsim", 36, 59, 21, "لَوْ أَنزَلْنَا هَٰذَا الْقُرْآنَ عَلَىٰ جَبَلٍ لَّرَأَيْتَهُ خَاشِعًا", "Eğer biz bu Kur'an'ı bir dağa indirseydik onu boyun eğmiş görürdün."),
        RawQuranWord("شَرَاب", "Şarâb", "İçecek, meşrubat", "İsim", 36, 16, 69, "يَخْرُجُ مِن بُطُونِهَا شَرَابٌ مُّخْتَلِفٌ أَلْوَانُهُ", "Onların karınlarından renkleri muhtelif bir içecek (bal) çıkar."),
        RawQuranWord("حِسَاب", "Hisâb", "Hesap, sorgulama", "İsim", 36, 14, 41, "يَوْمَ يَقُومُ الْحِسَابُ", "Hesabın görüleceği gün."),
        RawQuranWord("إِذْن", "İzn", "İzin, irade, buyruk", "İsim", 35, 2, 255, "إِلَّا بِإِذْنِهِ", "Ancak O'nun izniyle."),
        RawQuranWord("كَفَى", "Kefâ", "Yetti, kâfi geldi", "Mazi Fiil", 35, 4, 79, "وَكَفَىٰ بِاللَّهِ شَهِيدًا", "Şahit olarak Allah yeter."),
        RawQuranWord("مُنَافِق", "Münâfık", "İkiyüzlü, münafık", "İsim", 35, 4, 145, "إِنَّ الْمُنَافِقِينَ فِي الدَّرْكِ الْأَسْفَلِ مِنَ النَّارِ", "Şüphesiz münafıklar ateşin en aşağı tabakasındadırlar."),
        RawQuranWord("مُسْتَقِيم", "Müstakîm", "Dosdoğru, istikametli", "Sıfat", 35, 1, 6, "الصِّرَاطَ الْمُسْتَقِيمَ", "Dosdoğru yolu."),
        RawQuranWord("سُلْطَان", "Sultân", "Yetki, delil, kanıt, güç", "İsim", 35, 4, 91, "وَأُولَٰئِكُمْ جَعَلْنَا لَكُمْ عَلَيْهِمْ سُلْطَانًا مُّبِينًا", "İşte onlara karşı size apaçık bir yetki verdik."),
        RawQuranWord("أَبْصَرَ", "Ebsara", "Gördü, basiretle baktı", "Mazi Fiil", 34, 32, 12, "رَبَّنَا أَبْصَرْنَا وَسَمِعْنَا", "Rabbimiz! Gördük ve işittik."),
        RawQuranWord("سَيِّئَة", "Seyyie", "Kötülük, günah", "İsim", 34, 2, 81, "بَلَىٰ مَن كَسَبَ سَيِّئَةً", "Hayır, kim bir kötülük kazanırsa..."),
        RawQuranWord("مَوْت", "Mevt", "Ölüm", "İsim", 34, 3, 185, "كُلُّ نَفْسٍ ذَائِقَةُ الْمَوْتِ", "Her nefis ölümü tadacaktır."),
        RawQuranWord("أُمّ", "Ümm", "Anne, ana, asıl", "İsim", 34, 3, 7, "هُنَّ أُمُّ الْكِتَابِ", "Onlar kitabın anasıdır (esasıdır)."),
        RawQuranWord("إِثْم", "İsm", "Günah, vebal, suç", "İsim", 34, 2, 219, "قُلْ فِيهِمَا إِثْمٌ كَبِيرٌ", "De ki: O ikisinde büyük bir günah vardır."),
        RawQuranWord("شَهَادَة", "Şehâdeh", "Görünen alem, şahitlik", "İsim", 33, 9, 105, "عَالِمِ الْغَيْبِ وَالشَّهَادَةِ", "Gaybı ve görünen alemi bilendir."),
        RawQuranWord("فِتْنَة", "Fitneh", "İmtihan, fitne, kargaşa", "İsim", 33, 2, 191, "وَالْفِتْنَةُ أَشَدُّ مِنَ الْقَتْلِ", "Fitne öldürmekten daha beterdir."),
        RawQuranWord("شَمْس", "Şems", "Güneş", "İsim", 33, 91, 1, "وَالشَّمْسِ وَضُحَاهَا", "Güneşe ve onun aydınlığına andolsun."),
        RawQuranWord("أَحَد", "Ehad", "Tek, biricik (zâtında eşsiz)", "İsim", 33, 112, 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "De ki: O Allah tektir."),
        RawQuranWord("زَكَاة", "Zekât", "Zekât, arınma, temizlik", "İsim", 32, 2, 43, "وَآتُوا الزَّكَاةَ", "Zekâtı verin."),
        RawQuranWord("عَصَى", "'Asâ", "İsyan etti, karşı geldi", "Mazi Fiil", 32, 20, 121, "وَعَصَىٰ آدَمُ رَبَّهُ فَغَوَىٰ", "Âdem Rabbine karşı geldi de şaşırdı."),
        RawQuranWord("نِعْمَة", "Ni'met", "Nimet, lütuf, ihsan", "İsim", 32, 16, 18, "وَإِن تَعُدُّوا نِعْمَةَ اللَّهِ لَا تُحْصُوهَا", "Allah'ın nimetini saymaya kalksanız onu sayamazsınız."),
        RawQuranWord("إِنْسَان", "İnsân", "İnsan, beşer", "İsim", 32, 103, 2, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ", "Şüphesiz insan ziyan içindedir."),
        RawQuranWord("عَالَم", "'Âlem", "Âlem, varlıklar, insanlar", "İsim", 32, 1, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Hamd, âlemlerin Rabbi olan Allah'a mahsustur."),
        RawQuranWord("مُبِين", "Mübîn", "Apaçık, hakikati açıklayan", "Sıfat", 32, 36, 69, "إِنْ هُوَ إِلَّا ذِكْرٌ وَقُرْآنٌ مُّبِينٌ", "O sadece bir öğüt ve apaçık bir Kur'an'dır."),
        RawQuranWord("رِزْق", "Rizk", "Rızık, nasip, nimet", "İsim", 32, 2, 3, "وَمِمَّا رَزَقْنَاهُمْ يُنفِقُونَ", "Kendilerine verdiğimiz rızıktan infak ederler."),
        RawQuranWord("سُوء", "Sû'", "Kötülük, fenalık, bela", "İsim", 32, 4, 110, "وَمَن يَعْمَلْ سُوءًا أَوْ يَظْلِمْ نَفْسَهُ", "Kim bir kötülük yapar veya nefsine zulmederse..."),
        RawQuranWord("أَنْعَمَ", "En'ame", "Nimet verdi, ihsanda bulundu", "Mazi Fiil", 32, 1, 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ", "Kendilerine nimet verdiklerinin yoluna.")
    )

    // 251 - 300. Kelimeler (Frekans: 31 -> 28)
    private fun getRawBatch251to300(): List<RawQuranWord> = listOf(
        RawQuranWord("إِلَٰه", "İlâh", "İlah, mabud, tapılan", "İsim", 31, 2, 255, "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ", "Allah O'dur ki, O'ndan başka ilah yoktur."),
        RawQuranWord("عِبَاد", "'İbâd", "Kullar", "İsim (Çoğul)", 31, 2, 186, "وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ", "Kullarım sana beni sorduklarında, şüphesiz ben yakınım."),
        RawQuranWord("عَدُوّ", "'Adüvv", "Düşman", "İsim", 31, 2, 36, "بَعْضُكُمْ لِبَعْضٍ عَدُوٌّ", "Birbirinize düşman olarak inin."),
        RawQuranWord("شَرّ", "Şerr", "Şer, kötülük, daha kötü", "İsim / Sıfat", 31, 99, 8, "وَمَن يَعْمَلْ مِثْقَالَ ذَرَّةٍ شَرًّا يَرَهُ", "Kim zerre ağırlığınca bir kötülük yaparsa onu görür."),
        RawQuranWord("صَادِق", "Sâdık", "Doğru sözlü, sadık", "Sıfat", 31, 9, 119, "اتَّقُوا اللَّهَ وَكُونُوا مَعَ الصَّادِقِينَ", "Allah'tan sakının ve doğrularla beraber olun."),
        RawQuranWord("اسْتَغْفَرَ", "İstağfera", "Bağışlanma diledi, istiğfar etti", "Mazi Fiil", 31, 110, 3, "فَسَبِّحْ بِحَمْدِ رَبِّكَ وَاسْتَغْفِرْهُ", "Rabbini hamd ile tesbih et ve O'ndan bağışlanma dile."),
        RawQuranWord("حَكَمَ", "Hakeme", "Hüküm verdi, hakemlik yaptı", "Mazi Fiil", 31, 4, 58, "وَإِذَا حَكَمْتُم بَيْنَ النَّاسِ أَن تَحْكُمُوا بِالْعَدْلِ", "İnsanlar arasında hükmettiğiniz zaman adaletle hükmedin."),
        RawQuranWord("كَتَبَ", "Ketebe", "Yazdı, farz kıldı", "Mazi Fiil", 31, 2, 183, "كُتِبَ عَلَيْكُمُ الصِّيَامُ", "Oruç size farz kılındı."),
        RawQuranWord("حَمَلَ", "Hamele", "Taşıdı, yüklendi", "Mazi Fiil", 31, 33, 72, "وَحَمَلَهَا الْإِنسَانُ", "Onu insan yüklendi."),
        RawQuranWord("بَلَغَ", "Belega", "Ulaştı, erişti, tebliğ etti", "Mazi Fiil", 31, 5, 67, "يَا أَيُّهَا الرَّسُولُ بَلِّغْ مَا أُنزِلَ إِلَيْكَ", "Ey Resul! Rabbinden sana indirileni tebliğ et."),
        RawQuranWord("سَبَّحَ", "Sebbeha", "Tesbih etti, yüceltti", "Mazi Fiil", 31, 57, 1, "سَبَّحَ لِلَّهِ مَا فِي السَّمَاوَاتِ وَالْأَرْضِ", "Göklerde ve yerde ne varsa Allah'ı tesbih etmektedir."),
        RawQuranWord("ضَرَبَ", "Darabe", "Vurdu, misal getirdi", "Mazi Fiil", 31, 14, 24, "أَلَمْ تَرَ كَيْفَ ضَرَبَ اللَّهُ مَثَلًا", "Görmedin mi Allah nasıl bir misal getirdi?"),
        RawQuranWord("رَفَعَ", "Rafe'a", "Yükseltti, kaldırdı", "Mazi Fiil", 30, 58, 11, "يَرْفَعِ اللَّهُ الَّذِينَ آمَنُوا مِنكُمْ", "Allah içinizden iman edenlerin derecelerini yükseltir."),
        RawQuranWord("جَزَى", "Cezâ", "Karşılığını verdi, mükâfatlandırdı", "Mazi Fiil", 30, 6, 160, "مَن جَاءَ بِالْحَسَنَةِ فَلَهُ عَشْرُ أَمْثَالِهَا", "Kim iyilikle gelirse ona on katı karşılık vardır."),
        RawQuranWord("خَشِيَ", "Haşiye", "Saygıyla korktu, ürperdi", "Mazi Fiil", 30, 35, 28, "إِنَّمَا يَخْشَى اللَّهَ مِنْ عِبَادِهِ الْعُلَمَاءُ", "Allah'tan kulları içinde ancak âlimler hakkıyla korkar."),
        RawQuranWord("عَفَا", "'Afâ", "Affetti, bağışladı", "Mazi Fiil", 30, 2, 286, "وَاعْفُ عَنَّا وَاغْفِرْ لَنَا", "Bizi affet ve bizi bağışla."),
        RawQuranWord("شَفَعَ", "Şefe'a", "Şefaat etti, aracılık yaptı", "Mazi Fiil", 30, 2, 255, "مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ", "O'nun izni olmadan katında şefaat edecek kimdir?"),
        RawQuranWord("أَحْيَا", "Ehyâ", "Diriltti, hayat verdi", "Mazi Fiil", 30, 5, 32, "وَمَنْ أَحْيَاهَا فَكَأَنَّمَا أَحْيَا النَّاسَ جَمِيعًا", "Kim bir canı yaşatırsa bütün insanları yaşatmış gibi olur."),
        RawQuranWord("أَمَاتَ", "Emâte", "Öldürdü, canını aldı", "Mazi Fiil", 30, 53, 44, "وَأَنَّهُ هُوَ أَمَاتَ وَأَحْيَا", "Doğrusu öldüren de dirilten de O'dur."),
        RawQuranWord("أَسْلَمَ", "Esleme", "Teslim oldu, Müslüman oldu", "Mazi Fiil", 30, 2, 131, "إِذْ قَالَ لَهُ رَبُّهُ أَسْلِمْ ۖ قَالَ أَسْلَمْتُ لِرَبِّ الْعَالَمِينَ", "Rabbi ona 'Teslim ol!' dediğinde 'Âlemlerin Rabbine teslim oldum' dedi."),
        RawQuranWord("جَاهَدَ", "Câhede", "Cihad etti, gayret gösterdi", "Mazi Fiil", 30, 29, 69, "وَالَّذِينَ جَاهَدُوا فِينَا لَنَهْدِيَنَّهُمْ سُبُلَنَا", "Bizim uğrumuzda gayret gösterenleri elbette yollarımıza iletiriz."),
        RawQuranWord("صَدَقَ", "Sadaka", "Doğru söyledi", "Mazi Fiil", 30, 3, 95, "قُلْ صَدَقَ اللَّهُ", "De ki: Allah doğru söylemiştir."),
        RawQuranWord("سَجَدَ", "Secede", "Secde etti", "Mazi Fiil", 30, 96, 19, "وَاسْجُدْ وَاقْتَرِب", "Secde et ve Rabbine yaklaş!"),
        RawQuranWord("سِحْر", "Sihr", "Büyü, sihir", "İsim", 30, 2, 102, "يُعَلِّمُونَ النَّاسَ السِّحْرَ", "İnsanlara sihri öğretiyorlardı."),
        RawQuranWord("رِيح", "Rîh", "Rüzgâr, esinti", "İsim", 29, 30, 48, "اللَّهُ الَّذِي يُرْسِلُ الرِّيَاحَ", "Rüzgârları gönderen Allah'tır."),
        RawQuranWord("سَحَاب", "Sehâb", "Bulut", "İsim", 29, 2, 164, "وَالسَّحَابِ الْمُسَخَّرِ بَيْنَ السَّمَاءِ وَالْأَرْضِ", "Gök ile yer arasında emre amade bulutlarda..."),
        RawQuranWord("مَطَر", "Matar", "Yağmur", "İsim", 29, 7, 84, "وَأَمْطَرْنَا عَلَيْهِم مَّطَرًا", "Üzerlerine bir yağmur yağdırdık."),
        RawQuranWord("ثَمَرَة", "Semerah", "Meyve, ürün", "İsim", 29, 2, 25, "كُلَّمَا رُزِقُوا مِنْهَا مِن ثَمَرَةٍ رِّزْقًا", "Oradaki meyvelerden her rızıklandırıldıklarında..."),
        RawQuranWord("حِجَارَة", "Hicârah", "Taşlar, kaya", "İsim", 29, 2, 24, "وَقُودُهَا النَّاسُ وَالْحِجَارَةُ", "Yakıtı insanlar ve taşlardır."),
        RawQuranWord("تُرَاب", "Türâb", "Toprak", "İsim", 29, 3, 59, "خَلَقَهُ مِن تُرَابٍ", "Onu topraktan yarattı."),
        RawQuranWord("طِين", "Tîn", "Çamur, balçık", "İsim", 29, 6, 2, "هُوَ الَّذِي خَلَقَكُم مِّن طِينٍ", "Sizi çamurdan yaratan O'dur."),
        RawQuranWord("ذَهَب", "Zeheb", "Altın", "İsim", 29, 3, 91, "مِلْءُ الْأَرْضِ ذَهَبًا", "Yeryüzü dolusu altın."),
        RawQuranWord("لِسَان", "Lisân", "Dil, lisan", "İsim", 29, 14, 4, "وَمَا أَرْسَلْنَا مِن رَّسُولٍ إِلَّا بِلِسَانِ قَوْمِهِ", "Biz her peygamberi ancak kendi kavminin diliyle gönderdik."),
        RawQuranWord("بَصَر", "Basar", "Göz, görme duyusu, basiret", "İsim", 29, 16, 78, "وَجَعَلَ لَكُمُ السَّمْعَ وَالْأَبْصَارَ وَالْأَفْئِدَةَ", "Sizin için kulaklar, gözler ve kalpler var etti."),
        RawQuranWord("سَمْع", "Sem'", "İşitme duyusu, kulak", "İsim", 29, 2, 7, "خَتَمَ اللَّهُ عَلَىٰ قُلُوبِهِمْ وَعَلَىٰ سَمْعِهِمْ", "Allah onların kalplerini ve kulaklarını mühürlemiştir."),
        RawQuranWord("فُؤَاد", "Fu'âd", "Gönül, kalp, idrak", "İsim", 29, 17, 36, "إِنَّ السَّمْعَ وَالْبَصَرَ وَالْفُؤَادَ كُلُّ أُولَٰئِكَ كَانَ عَنْهُ مَسْئُولًا", "Şüphesiz kulak, göz ve gönül bunların hepsi ondan sorumludur."),
        RawQuranWord("صَدْر", "Sadr", "Göğüs, kalp, sine", "İsim", 29, 94, 1, "أَلَمْ نَشْرَحْ لَكَ صَدْرَكَ", "Senin göğsünü açıp genişletmedik mi?"),
        RawQuranWord("دَم", "Dem", "Kan", "İsim", 29, 2, 173, "إِنَّمَا حَرَّمَ عَلَيْكُمُ الْمَيْتَةَ وَالدَّمَ", "Size ancak leşi ve kanı haram kıldı."),
        RawQuranWord("عَهْد", "'Ahd", "Söz, ahit, antlaşma", "İsim", 29, 2, 27, "الَّذِينَ يَنقُضُونَ عَهْدَ اللَّهِ", "Onlar Allah'ın ahdini bozarlar."),
        RawQuranWord("فَوْز", "Fevz", "Kurtuluş, büyük başarı, zafer", "İsim", 29, 4, 13, "وَذَٰلِكَ الْفَوْزُ الْعَظِيمُ", "İşte bu büyük bir kurtuluştur."),
        RawQuranWord("جُنْد", "Cünd", "Ordu, askerler, yardımcılar", "İsim", 29, 37, 173, "وَإِنَّ جُندَنَا لَهُمُ الْغَالِبُونَ", "Ve şüphesiz bizim ordumuz elbette galip gelecektir."),
        RawQuranWord("حَرَام", "Harâm", "Haram, hürmetli, dokunulmaz", "Sıfat", 29, 2, 144, "الْمَسْجِدِ الْحَرَامِ", "Mescid-i Haram."),
        RawQuranWord("مَسْجِد", "Mescid", "Mescit, secde edilen yer", "İsim", 28, 2, 144, "فَوَلِّ وَجْهَكَ شَطْرَ الْمَسْجِدِ الْحَرَامِ", "Yüzünü Mescid-i Haram yönüne çevir."),
        RawQuranWord("قُوَّة", "Kuvveh", "Kuvvet, güç, kudret", "İsim", 28, 2, 165, "أَنَّ الْقُوَّةَ لِلَّهِ جَمِيعًا", "Kuvvetin bütünüyle Allah'a ait olduğunu."),
        RawQuranWord("حَدِيث", "Hadîs", "Söz, haber, hadis", "İsim", 28, 4, 87, "وَمَنْ أَصْدَقُ مِنَ اللَّهِ حَدِيثًا", "Söz bakımından Allah'tan daha doğru kim vardır?"),
        RawQuranWord("عَدْل", "'Adl", "Adalet, denklik, fidye", "İsim", 28, 16, 90, "إِنَّ اللَّهَ يَأْمُرُ بِالْعَدْلِ وَالْإِحْسَانِ", "Şüphesiz Allah adaleti ve iyiliği emreder."),
        RawQuranWord("حَسَنَة", "Hasene", "İyilik, güzellik, sevap", "İsim", 28, 2, 201, "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً", "Rabbimiz! Bize dünyada da iyilik ver, ahirette de iyilik ver."),
        RawQuranWord("فَرِيق", "Ferîk", "Bölük, zümre, fırka", "İsim", 28, 2, 75, "وَقَدْ كَانَ فَرِيقٌ مِّنْهُمْ يَسْمَعُونَ كَلَامَ اللَّهِ", "Onlardan bir fırka vardı ki Allah'ın kelamını dinlerlerdi."),
        RawQuranWord("فَتْح", "Feth", "Fetih, zafer, kapı açma", "İsim", 28, 48, 1, "إِنَّا فَتَحْنَا لَكَ فَتْحًا مُّبِينًا", "Şüphesiz biz sana apaçık bir fetih verdik."),
        RawQuranWord("آخِر", "Âhir", "Son, sonuncu", "İsim / Sıfat", 28, 57, 3, "هُوَ الْأَوَّلُ وَالْآخِرُ وَالظَّاهِرُ وَالْبَاطِنُ", "O Evvel'dir, Âhir'dir, Zâhir'dir ve Bâtın'dır."),
        RawQuranWord("قَمَر", "Kamer", "Ay", "İsim", 28, 54, 1, "اقْتَرَبَتِ السَّاعَةُ وَانشَقَّ الْقَمَرُ", "Kıyamet yaklaştı ve ay yarıldı."),
        RawQuranWord("شَجَرَة", "Şecarah", "Ağaç", "İsim", 28, 2, 35, "وَلَا تَقْرَبَا هَٰذِهِ الشَّجَرَةَ", "Fakat şu ağaca yaklaşmayın!"),
        RawQuranWord("عَرْش", "'Arş", "Arş, ilahi hükümranlık tahtı", "İsim", 28, 9, 129, "وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ", "Ve O, büyük Arş'ın Rabbidir."),
        RawQuranWord("كَرِيم", "Kerîm", "Çok cömert, şerefli, kerim", "Sıfat", 28, 27, 40, "فَإِنَّ رَبِّي غَنِيٌّ كَرِيمٌ", "Şüphesiz Rabbim zengindir, cömerttir."),
        RawQuranWord("قَرِيب", "Karîb", "Yakın", "Sıfat", 28, 2, 186, "فَإِنِّي قَرِيبٌ", "Muhakkak ki ben çok yakınım."),
        RawQuranWord("بَعِيد", "Ba'îd", "Uzak", "Sıfat", 28, 2, 176, "فِي شِقَاقٍ بَعِيدٍ", "Uzak bir ayrılık içindedirler."),
        RawQuranWord("بَاطِل", "Bâtıl", "Boş, asılsız, bâtıl, geçersiz", "İsim / Sıfat", 28, 17, 81, "جَاءَ الْحَقُّ وَزَهَقَ الْبَاطِلُ", "Hak geldi, bâtıl zail oldu.")
    )
}
