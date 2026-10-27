package com.example.ui.words

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntity
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.QuranGold
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    viewModel: WordListViewModel,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = if (uiState.globalTotalCount > 0) {
            uiState.globalLearnedCount.toFloat() / uiState.globalTotalCount.toFloat()
        } else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "word_list_progress_anim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Header & Live Mastery Progress Banner
        MasteryProgressHeaderCard(
            learnedCount = uiState.globalLearnedCount,
            totalCount = uiState.globalTotalCount,
            coveragePercentage = uiState.coveragePercentage,
            animatedProgress = animatedProgress,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Primary Status Filter Bar (All / Unlearned / Learned with Live Counts)
        LearnedStatusFilterTabs(
            selectedFilter = uiState.filterLearned,
            totalCount = uiState.totalWordsCount,
            unlearnedCount = uiState.unlearnedWordsCount,
            learnedCount = uiState.learnedWordsCount,
            onSelectFilter = { viewModel.setLearnedFilter(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Pill-Shaped Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = {
                Text(
                    text = "Türkçe anlam, okunuş veya Arapça ara...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Kelime Ara",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Aramayı Temizle"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("word_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Secondary Filter Chips (Word Categories & Decks 1..12)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("secondary_filters_row")
        ) {
            if (uiState.hasActiveSecondaryFilters) {
                item {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.clearAllFilters() },
                        label = { Text("Sıfırla") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.FilterAltOff,
                                contentDescription = "Filtreleri Sıfırla",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.testTag("clear_filters_chip")
                    )
                }
            }

            // Word Type Category Chips
            items(uiState.availableWordTypes) { wordType ->
                val isSelected = uiState.filterWordType == wordType
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setWordTypeFilter(wordType) },
                    label = { Text(wordType) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Deck Filter Chips (Deste 1 .. Deste 12)
            items((1..12).toList()) { deckNum ->
                val isSelected = uiState.filterDeck == deckNum
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setDeckFilter(deckNum) },
                    label = { Text("Deste $deckNum") },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("filter_deck_$deckNum")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Vertical List of 300 Quranic Words
        if (uiState.words.isEmpty()) {
            EmptyWordListState(
                filterLearned = uiState.filterLearned,
                hasFilters = uiState.hasActiveSecondaryFilters,
                onClearFilters = { viewModel.clearAllFilters() }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("words_vertical_list")
            ) {
                items(uiState.words, key = { it.id }) { word ->
                    WordItemCard(
                        word = word,
                        onClick = { viewModel.selectWordForDetail(word) },
                        onPlayAudio = { viewModel.playWordAudio(word) },
                        onToggleLearned = { viewModel.toggleLearned(word) }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Word Detail
    uiState.selectedWordForDetail?.let { selectedWord ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectWordForDetail(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            WordDetailBottomSheet(
                word = selectedWord,
                onPlayWordAudio = { viewModel.playWordAudio(selectedWord) },
                onPlayAyahAudio = { viewModel.playAyahAudio(selectedWord) },
                onToggleLearned = { viewModel.toggleLearned(selectedWord) }
            )
        }
    }
}

@Composable
private fun MasteryProgressHeaderCard(
    learnedCount: Int,
    totalCount: Int,
    coveragePercentage: Float,
    animatedProgress: Float,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_list_progress_header")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kur'an 300 Kelime Hazinesi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$learnedCount / $totalCount kelime öğrenildi • ${totalCount - learnedCount} kalan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "%${String.format("%.1f", coveragePercentage)} Kapsam",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleTheme)
                                .testTag("wordlist_theme_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = if (isDarkTheme) "Gündüz Moduna Geç" else "Gece Moduna Geç",
                                    tint = if (isDarkTheme) QuranGold else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("word_list_progress_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun LearnedStatusFilterTabs(
    selectedFilter: Boolean?,
    totalCount: Int,
    unlearnedCount: Int,
    learnedCount: Int,
    onSelectFilter: (Boolean?) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("learned_status_filter_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusTabItem(
                title = "Tümü ($totalCount)",
                selected = selectedFilter == null,
                onClick = { onSelectFilter(null) },
                activeColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("filter_all_chip")
            )
            StatusTabItem(
                title = "Öğrenilecek ($unlearnedCount)",
                selected = selectedFilter == false,
                onClick = { onSelectFilter(false) },
                activeColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("filter_unlearned_chip")
            )
            StatusTabItem(
                title = "Öğrenilen ($learnedCount)",
                selected = selectedFilter == true,
                onClick = { onSelectFilter(true) },
                activeColor = SuccessGreen,
                modifier = Modifier
                    .weight(1f)
                    .testTag("filter_learned_chip")
            )
        }
    }
}

@Composable
private fun StatusTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeColor else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.height(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyWordListState(
    filterLearned: Boolean?,
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.BookmarkAdded,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val emptyTitle = when (filterLearned) {
                true -> "Henüz Öğrenilen Kelime Yok"
                false -> "Tüm Kelimeler Öğrenildi!"
                null -> "Eşleşen Kelime Bulunamadı"
            }
            val emptySubtitle = when (filterLearned) {
                true -> "Listeden bildiğiniz kelimelerin yanındaki onay ikonuna dokunarak veya kelime kartlarıyla çalışarak öğrenilenlere ekleyebilirsiniz."
                false -> "Seçili filtredeki tüm kelimeleri tamamladınız. Diğer desteleri veya tüm kelimeleri inceleyebilirsiniz."
                null -> "Arama teriminizi veya seçili filtreleri değiştirerek 300 Kur'an kelimesine göz atabilirsiniz."
            }

            Text(
                text = emptyTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = emptySubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (hasFilters) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("empty_state_clear_filters_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterAltOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tüm Filtreleri Temizle")
                }
            }
        }
    }
}

@Composable
fun WordItemCard(
    word: WordEntity,
    onClick: () -> Unit,
    onPlayAudio: () -> Unit,
    onToggleLearned: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (word.isLearned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (word.isLearned) 0.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (word.isLearned) {
                    SuccessGreen.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .testTag("word_item_${word.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // One-Tap Learned / Unlearned Toggle Button (48dp touch target)
                IconButton(
                    onClick = onToggleLearned,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("word_toggle_learned_${word.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (word.isLearned) SuccessGreen
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (word.isLearned) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Öğrenildi - Değiştirmek için dokunun",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "${word.id}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = word.turkishMeaning,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${word.transliteration} • ${word.wordType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = QuranGold.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "Kur'an'da ${word.frequency} kez",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Deste ${word.deck}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Vocalized Arabic Word
                Text(
                    text = word.arabic,
                    fontFamily = AmiriFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Audio Button (48dp touch target for accessibility)
                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("word_audio_${word.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Kelimeyi Dinle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordDetailBottomSheet(
    word: WordEntity,
    onPlayWordAudio: () -> Unit,
    onPlayAyahAudio: () -> Unit,
    onToggleLearned: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Metadata Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "#${word.id} • Deste ${word.deck}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = QuranGold.copy(alpha = 0.22f)
            ) {
                Text(
                    text = "Kur'an'da ${word.frequency} kez",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Vocalized Arabic Script
        Text(
            text = word.arabic,
            fontFamily = AmiriFontFamily,
            fontSize = 54.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${word.transliteration} (${word.wordType})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = word.turkishMeaning,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Word Audio Pronunciation Button
        Surface(
            onClick = onPlayWordAudio,
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.testTag("detail_play_word_audio_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kelimeyi Telaffuz Et",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Example Ayah Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = QuranGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Örnek Ayet (${word.exampleAyahReference})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onPlayAyahAudio,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_play_ayah_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Ayeti Dinle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = word.exampleAyahArabic,
                    fontFamily = AmiriFontFamily,
                    fontSize = 22.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${word.exampleAyahTranslation}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Toggle Learned / Unlearned Action Button
        if (word.isLearned) {
            OutlinedButton(
                onClick = onToggleLearned,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, SuccessGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SuccessGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("detail_toggle_learned_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Öğrenildi (Öğrenilecek Olarak İşaretle)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onToggleLearned,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuccessGreen,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("detail_toggle_learned_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Öğrenildi Olarak İşaretle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
