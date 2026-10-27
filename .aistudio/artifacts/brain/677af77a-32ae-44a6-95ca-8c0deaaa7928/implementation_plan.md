# 300 Quranic Words Vertical List & Filtering Experience

An interactive, vertical browsing and management screen for all 300 high-frequency Quranic words, equipped with a live mastery progress header, instant learned vs. unlearned filter tabs with dynamic counts, deck and grammatical category filters, and one-tap learned status toggling.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design choices incorporate your preferences and recommended defaults for a complete, responsive vocabulary browser:

- **Confirmed Decision — Progress & Live Filter Counts**: Display a mastery progress banner with a progress bar and percentage at the top of the screen, paired with live word counts on each status tab (`All (300)`, `Learned (X)`, `Unlearned (Y)`).
- **Confirmed Decision — Search, Deck & Category Filtering**: Provide a multi-language search bar (Turkish meaning, Arabic script, and transliteration) alongside secondary filter chips for Decks (`Deck 1`–`Deck 12`) and grammatical categories (`Noun`, `Verb`, `Particle / Pronoun`).
- **Default Decision — Direct Learned Status Toggle**: Allow users to toggle a word's `Learned` / `Unlearned` state both directly from the vertical list item badge and inside the word detail bottom sheet so users can rapidly mark words they already know.

---

## 1. Overview & Core Concept

- **What It Does**: Presents the complete corpus of 300 Quranic words ordered by frequency in a smooth vertical list. Users can filter between all words, learned words, and unlearned words, narrow down by deck or word type, listen to pronunciation, inspect example verses, and toggle mastery status on the spot.
- **Target Audience / Persona**: Learners studying the 300 most frequent words of the Quran who want a clear birds-eye view of their vocabulary mastery, quick review of unlearned words, and fast audio-visual reference.
- **Key Value**: Bridges active study (Flashcards/Quizzes) with structured reference browsing, letting learners audit their progress and customize which words are marked as mastered.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Progress & Status Filtering**: Upon opening the Words screen, the user sees a compact progress card showing total learned words out of 300, total Quranic occurrence coverage, and a smooth progress bar. Below it, a primary segmented filter bar (`All`, `Unlearned`, `Learned`) displays live item counts and filters the vertical list immediately.
  2. **Search & Secondary Filtering**: Users type in the search bar or tap secondary filter chips (Decks 1–12 or Word Types) to combine filters with the active Learned/Unlearned status. An active filter summary with a quick "Reset Filters" action appears when filters narrow the list.
  3. **Direct Mastery Toggle & Audio Playback**: Each vertical list card displays the frequency rank, Turkish meaning, transliteration, repetition count, vocalized Arabic script, an audio pronunciation button, and a tappable learned/unlearned status toggle button.
  4. **Rich Word Detail Sheet**: Tapping any word card opens a modal bottom sheet displaying the large vocalized Arabic word, deck and frequency badges, audio triggers for both the isolated word and its example Quranic verse, and a full-width button to toggle `Learned` / `Unlearned` status.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Refined Islamic editorial clarity—warm, high-contrast surfaces with emerald and gold accents that honor classical Arabic calligraphy while maintaining modern Material 3 ergonomics.
  - *Color Palette & Mood*: Deep emerald primary (`MaterialTheme.colorScheme.primary`), warm gold highlights (`QuranGold`) for Quranic frequency and verse badges, and vibrant emerald green (`SuccessGreen`) for learned indicators against clean surface cards.
  - *Typography & Hierarchy*: Vocalized Arabic rendered in `AmiriFontFamily` (`28sp` in list cards, `54sp` in the detail sheet) paired with crisp sans-serif titles for Turkish meanings and subtle muted metadata for transliteration and frequency counts.
  - *Component Styling & Layout*: 8dp baseline grid spacing, rounded `16dp` cards with subtle elevation and tinted borders for learned items, ensuring minimum `48dp` interactive touch targets on audio and status toggle controls.

- **Interactive Feedback & Motion**:
  - Animated progress bar transitions (`animateFloatAsState`) when marking words learned or unlearned.
  - Contextual empty states with a clear icon, helpful message, and a one-tap "Clear Filters" button when a combination of search and filters yields no matches.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Unfiltered Global Counts vs. Filtered List State**
  - *Chosen Approach*: Compute global `totalCount` (300), `learnedCount`, and `unlearnedCount` independently of the currently selected status tab in `WordListUiState`, while also exposing the filtered list count.
  - *Why*: If the tab counts only reflected the already-filtered list, switching to `Learned` would hide the count of `Unlearned` words. Maintaining live counts across all three status tabs gives continuous visibility into progress.
  - *Alternatives Considered*: Showing only the count of the active tab, which reduces context and forces users to switch tabs to check remaining unlearned words.

- **Decision 2: Synchronized SRS & Learned Toggle**
  - *Chosen Approach*: Toggling a word's learned status from the vertical list or detail sheet invokes `wordRepository.recordReview(word, remembered = !word.isLearned)` so that Room persistence, SRS review intervals, and dashboard statistics update atomically.
  - *Why*: Ensures that marking a word as learned in the list immediately updates the Dashboard mastery metrics and Spaced Repetition schedule.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Room Database (WordDao)                          │
│         300 Quranic Words Flow (allWords: Flow<List<WordEntity>>)       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           WordListViewModel                             │
│  ┌──────────────────┐  ┌────────────────────┐  ┌─────────────────────┐  │
│  │ Search & Filter  │  │ Mastery Progress & │  │ Audio & SRS Toggle  │  │
│  │ StateFlows       │  │ Tab Count Computer │  │ Action Handlers     │  │
│  └────────┬─────────┘  └─────────┬──────────┘  └──────────┬──────────┘  │
└───────────┼──────────────────────┼────────────────────────┼─────────────┘
            │                      │                        │
            ▼                      ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            WordListScreen                               │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ MasteryProgressHeader (Learned / 300 Progress Bar & Coverage %)   │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │ Primary Status Tabs: [ All (300) ] [ Unlearned (Y) ] [ Learned ]  │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │ Search Bar + Deck & Word Type Filter Chips + Active Reset Action  │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │ LazyColumn (300 Words Vertical List)                              │  │
│  │   └── WordItemCard (Rank/Toggle, Meaning, Arabic, Audio Button)   │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │ WordDetailBottomSheet (Verse Card, Dual Audio, Learned Toggle)    │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - `WordListUiState` holds:
    - `words: List<WordEntity>`: The filtered vertical list of words matching the active status filter, deck filter, word type filter, and search query.
    - `totalWordsCount: Int`, `learnedWordsCount: Int`, `unlearnedWordsCount: Int`: Live counts computed from the underlying 300-word dataset (respecting search/deck/category filters so users see accurate counts).
    - `filterLearned: Boolean?`: `null` for All, `true` for Learned, `false` for Unlearned.
    - `filterDeck: Int?`: `null` for all decks, or `1..12` for specific 25-word decks.
    - `filterWordType: String?`: `null` for all types, or specific grammatical categories.
    - `availableWordTypes: List<String>`: Distinct word types extracted from the 300-word dataset.
    - `selectedWordForDetail: WordEntity?`: Live-synced selected word for the modal bottom sheet.

- **Interactive Component & State Mapping**:
  - **Primary Status Filter Tabs (`All` / `Learned` / `Unlearned`)**: Tapping updates `_filterLearned` in `WordListViewModel`, immediately recomputing `uiState.words`.
  - **Deck & Category Filter Row**: Tapping a deck chip (`Deste 1..12`) or category chip toggles `_filterDeck` or `_filterWordType`. Tapping "Filtreleri Temizle" resets search, status, deck, and category filters to their default state.
  - **Card & Sheet Learned Toggle**: Tapping the check/status badge on `WordItemCard` or the primary action button inside `WordDetailBottomSheet` calls `viewModel.toggleLearned(word)`, persisting the change in Room and automatically refreshing both the vertical list and the open bottom sheet.
