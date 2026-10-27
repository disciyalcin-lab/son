package com.example.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.QuranGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAudioPlaying by viewModel.audioPlayerManager.isPlaying.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // 1. Quiz Mode Tabs
        ScrollableTabRow(
            selectedTabIndex = uiState.currentMode.ordinal,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            QuizMode.values().forEach { mode ->
                Tab(
                    selected = uiState.currentMode == mode,
                    onClick = { viewModel.startNewQuiz(mode) },
                    text = {
                        Text(
                            text = mode.title,
                            fontWeight = if (uiState.currentMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // 2. Question Count & Deck Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            item {
                Text(
                    text = "Soru:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 10.dp, end = 2.dp)
                )
            }
            listOf(5, 10, 15).forEach { count ->
                item {
                    FilterChip(
                        selected = uiState.questionCount == count,
                        onClick = { viewModel.setQuestionCount(count) },
                        label = { Text("$count Soru") },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.selectedDeckFilter == null,
                    onClick = { viewModel.setDeckFilter(null) },
                    label = { Text("Tüm Kur'an (300)") }
                )
            }

            (1..12).forEach { deckNum ->
                item {
                    FilterChip(
                        selected = uiState.selectedDeckFilter == deckNum,
                        onClick = { viewModel.setDeckFilter(deckNum) },
                        label = { Text("Deste $deckNum") }
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }

        if (uiState.isQuizFinished) {
            // Quiz Results
            QuizResultsView(
                score = uiState.score,
                total = uiState.totalQuestions,
                mode = uiState.currentMode,
                onRestart = { viewModel.startNewQuiz(uiState.currentMode) }
            )
            return
        }

        val question = uiState.questions.getOrNull(uiState.currentQuestionIndex)
        if (question == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Soru bulunamadı.")
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Progress Bar & Score
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Soru ${uiState.currentQuestionIndex + 1} / ${uiState.totalQuestions}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Doğru: ${uiState.score}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }

            // Question Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, QuranGold.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .testTag("quiz_question_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (uiState.currentMode) {
                            QuizMode.CLASSIC_AR_TO_TR -> {
                                Text(
                                    text = question.questionPrompt,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = question.targetWord.transliteration,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                IconButton(
                                    onClick = { viewModel.playQuestionAudio(question.targetWord) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Dinle",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            QuizMode.CLASSIC_TR_TO_AR -> {
                                Text(
                                    text = "Bu Türkçe anlamın Arapça karşılığı nedir?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = question.questionPrompt,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            QuizMode.AUDIO_QUIZ -> {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { viewModel.playQuestionAudio(question.targetWord) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Headphones,
                                        contentDescription = "Sesi Tekrar Dinle",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Sesi Dinlemek İçin Dokunun",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Doğru Türkçe anlamı aşağıdan seçin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            QuizMode.AYAH_GAP_FILL -> {
                                question.ayahContext?.let { ayah ->
                                    Text(
                                        text = ayah,
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 22.sp,
                                        lineHeight = 38.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                question.ayahTranslation?.let { meal ->
                                    Text(
                                        text = "\"$meal\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                IconButton(
                                    onClick = { viewModel.playAyahAudio(question.targetWord) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Ayeti Dinle",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4 Options
            question.options.forEachIndexed { index, optionText ->
                item {
                    val isSelected = uiState.selectedOptionIndex == index
                    val isCorrect = index == question.correctOptionIndex
                    val isChecked = uiState.isAnswerChecked

                    val (containerColor, borderColor, contentColor) = when {
                        !isChecked && isSelected -> Triple(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onSurface
                        )
                        isChecked && isCorrect -> Triple(
                            SuccessGreen.copy(alpha = 0.15f),
                            SuccessGreen,
                            SuccessGreen
                        )
                        isChecked && isSelected && !isCorrect -> Triple(
                            WarningOrange.copy(alpha = 0.15f),
                            WarningOrange,
                            WarningOrange
                        )
                        else -> Triple(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isChecked) {
                                viewModel.selectOption(index)
                            }
                            .testTag("quiz_option_$index")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val optionLetter = ('A' + index).toString()
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(borderColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optionLetter,
                                        fontWeight = FontWeight.Bold,
                                        color = borderColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isChecked && isCorrect) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor
                                )
                            }

                            if (isChecked) {
                                if (isCorrect) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Doğru",
                                        tint = SuccessGreen
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Yanlış",
                                        tint = WarningOrange
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Explanation & Next Question Button
            item {
                AnimatedVisibility(visible = uiState.isAnswerChecked) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Explanation Box
                        OutlinedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${question.targetWord.arabic} • ${question.targetWord.transliteration}",
                                        fontFamily = AmiriFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Anlamı: ${question.targetWord.turkishMeaning} (${question.targetWord.frequency} kez)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.playQuestionAudio(question.targetWord) }) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Dinle",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.nextQuestion() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("quiz_next_button")
                        ) {
                            Text(
                                text = if (uiState.currentQuestionIndex + 1 >= uiState.totalQuestions) "Sonuçları Gör" else "Sonraki Soru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun QuizResultsView(
    score: Int,
    total: Int,
    mode: QuizMode,
    onRestart: () -> Unit
) {
    val percentage = (score.toFloat() / total * 100).toInt()
    val (message, color) = when {
        percentage >= 90 -> "Mükemmel! Kur'an kelimelerine çok iyi hâkimsiniz! 🌟" to SuccessGreen
        percentage >= 70 -> "Çok İyi! Biraz daha pratikle üstat olacaksınız! 👏" to MaterialTheme.colorScheme.primary
        percentage >= 50 -> "İyi deneme! Kelime kartlarıyla tekrar yapmanızı öneririz. 📖" to QuranGold
        else -> "Yılmak yok! Günlük kart tekrarlarıyla başaracaksınız! 💪" to WarningOrange
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quiz_results_card")
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "%$percentage Başarı",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Text(
                    text = "$score / $total Doğru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRestart,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yeni Quiz Başlat", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
