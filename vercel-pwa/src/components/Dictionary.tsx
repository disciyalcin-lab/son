import React, { useState, useMemo } from 'react';
import { QURAN_WORDS_300, TOTAL_QURAN_WORDS, QuranWord } from '../data/wordsData';
import { WordProgress, getDeck12Number } from '../utils/storage';
import { audioPlayer } from '../utils/audioPlayer';
import {
  Search,
  Volume2,
  Check,
  X,
  Sun,
  Moon,
  BookOpen,
  CheckCircle2,
  Circle,
  FilterX
} from 'lucide-react';

interface DictionaryProps {
  progressMap: Record<number, WordProgress>;
  isDark: boolean;
  onToggleTheme: () => void;
  onToggleLearned: (wordId: number, learned: boolean) => void;
}

type StatusFilter = 'ALL' | 'UNLEARNED' | 'LEARNED';

export const Dictionary: React.FC<DictionaryProps> = ({
  progressMap,
  isDark,
  onToggleTheme,
  onToggleLearned
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [wordTypeFilter, setWordTypeFilter] = useState<string | null>(null);
  const [deckFilter, setDeckFilter] = useState<number | null>(null);
  const [selectedWordForDetail, setSelectedWordForDetail] = useState<QuranWord | null>(null);

  // Strictly sorted by frequency DESC, id ASC (1..300)
  const sortedBaseWords = useMemo(() => {
    return [...QURAN_WORDS_300].sort((a, b) => b.frequency - a.frequency || a.id - b.id);
  }, []);

  // Global mastery progress
  const globalLearnedWords = useMemo(
    () => sortedBaseWords.filter((w) => progressMap[w.id]?.isLearned),
    [sortedBaseWords, progressMap]
  );
  const globalLearnedCount = globalLearnedWords.length;
  const globalTotalCount = sortedBaseWords.length;
  const coveragePercentage = useMemo(() => {
    const freqSum = globalLearnedWords.reduce((sum, w) => sum + w.frequency, 0);
    return Math.min(100, (freqSum / TOTAL_QURAN_WORDS) * 100);
  }, [globalLearnedWords]);

  // Available word types
  const availableWordTypes = useMemo(() => {
    const set = new Set<string>();
    sortedBaseWords.forEach((w) => {
      if (w.wordType) set.add(w.wordType);
    });
    return Array.from(set);
  }, [sortedBaseWords]);

  // Words matching search, wordType, and deckFilter
  const contextWords = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return sortedBaseWords.filter((w) => {
      if (deckFilter !== null && getDeck12Number(w.id) !== deckFilter) return false;
      if (wordTypeFilter !== null && w.wordType !== wordTypeFilter) return false;
      if (!q) return true;
      return (
        w.arabic.includes(q) ||
        w.turkishMeaning.toLowerCase().includes(q) ||
        w.transliteration.toLowerCase().includes(q) ||
        String(w.id) === q
      );
    });
  }, [sortedBaseWords, searchQuery, deckFilter, wordTypeFilter]);

  // Live counts on each filter tab
  const counts = useMemo(() => {
    const learned = contextWords.filter((w) => progressMap[w.id]?.isLearned).length;
    return {
      all: contextWords.length,
      learned,
      unlearned: contextWords.length - learned
    };
  }, [contextWords, progressMap]);

  const displayedWords = useMemo(() => {
    return contextWords
      .filter((w) => {
        const isLearned = !!progressMap[w.id]?.isLearned;
        if (statusFilter === 'LEARNED') return isLearned;
        if (statusFilter === 'UNLEARNED') return !isLearned;
        return true;
      })
      .sort((a, b) => b.frequency - a.frequency || a.id - b.id);
  }, [contextWords, progressMap, statusFilter]);

  const hasActiveSecondaryFilters =
    searchQuery.trim().length > 0 || wordTypeFilter !== null || deckFilter !== null;

  const clearAllFilters = () => {
    setSearchQuery('');
    setWordTypeFilter(null);
    setDeckFilter(null);
    setStatusFilter('ALL');
  };

  return (
    <div className="space-y-3 pb-8">
      {/* 1. Header & Live Mastery Progress Banner ("Kur'an 300 Kelime Hazinesi") */}
      <div className="overflow-hidden rounded-[20px] bg-white dark:bg-[#1B1B22] shadow-sm border border-[#E4E4EC] dark:border-[#32323E]">
        <div className="bg-gradient-to-r from-[#B7282E]/[0.12] to-[#DEC286]/[0.25] dark:from-[#E2474D]/[0.15] dark:to-[#4F3E1E]/[0.35] px-4 py-3.5">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0 flex-1">
              <h2 className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                Kur'an 300 Kelime Hazinesi
              </h2>
              <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4] mt-0.5">
                {globalLearnedCount} / {globalTotalCount} kelime öğrenildi •{' '}
                {globalTotalCount - globalLearnedCount} kalan
              </p>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <span className="rounded-xl bg-[#DEC286] dark:bg-[#4F3E1E] px-2.5 py-1.5 text-xs font-extrabold text-[#2C2214] dark:text-[#F7E3B5]">
                %{coveragePercentage.toFixed(1)} Kapsam
              </span>

              <button
                onClick={onToggleTheme}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-[#F0F0F4] dark:bg-[#262630] transition"
                aria-label={isDark ? 'Gündüz Moduna Geç' : 'Gece Moduna Geç'}
              >
                {isDark ? (
                  <Sun className="h-4 w-4 text-[#C89D42]" />
                ) : (
                  <Moon className="h-4 w-4 text-[#B7282E]" />
                )}
              </button>
            </div>
          </div>

          <div className="mt-2.5 h-2 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
            <div
              className="h-full rounded-full bg-[#B7282E] dark:bg-[#E2474D] transition-all duration-500"
              style={{
                width: `${
                  globalTotalCount > 0 ? (globalLearnedCount / globalTotalCount) * 100 : 0
                }%`
              }}
            />
          </div>
        </div>
      </div>

      {/* 2. Primary Status Filter Bar (Tümü / Öğrenilecek / Öğrenilen with Live Counts) */}
      <div className="grid grid-cols-3 gap-1.5 rounded-2xl bg-[#F0F0F4]/80 dark:bg-[#262630]/80 p-1">
        <button
          onClick={() => setStatusFilter('ALL')}
          className={`h-10 rounded-xl text-xs font-bold transition ${
            statusFilter === 'ALL'
              ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white shadow-sm'
              : 'text-[#5A5A64] dark:text-[#B8B8C4]'
          }`}
        >
          Tümü ({counts.all})
        </button>
        <button
          onClick={() => setStatusFilter('UNLEARNED')}
          className={`h-10 rounded-xl text-xs font-bold transition ${
            statusFilter === 'UNLEARNED'
              ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white shadow-sm'
              : 'text-[#5A5A64] dark:text-[#B8B8C4]'
          }`}
        >
          Öğrenilecek ({counts.unlearned})
        </button>
        <button
          onClick={() => setStatusFilter('LEARNED')}
          className={`h-10 rounded-xl text-xs font-bold transition ${
            statusFilter === 'LEARNED'
              ? 'bg-[#2E7D32] text-white shadow-sm'
              : 'text-[#5A5A64] dark:text-[#B8B8C4]'
          }`}
        >
          Öğrenilen ({counts.learned})
        </button>
      </div>

      {/* 3. Pill-Shaped Search Bar */}
      <div className="relative">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-[#B7282E] dark:text-[#E2474D]" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Türkçe anlam, okunuş veya Arapça ara..."
          className="w-full rounded-[28px] bg-white dark:bg-[#1B1B22] pl-11 pr-10 py-3 text-sm border border-[#8A8A96]/35 dark:border-[#7E7E8C]/35 focus:outline-none focus:border-[#B7282E] dark:focus:border-[#E2474D] text-[#1A1A1E] dark:text-[#F2F2F7]"
        />
        {searchQuery.length > 0 && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[#8A8A96] hover:text-[#1A1A1E] dark:hover:text-white"
            aria-label="Aramayı Temizle"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* 4. Secondary Filter Chips (Word Categories & Decks 1..12) */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
        {hasActiveSecondaryFilters && (
          <button
            onClick={clearAllFilters}
            className="shrink-0 inline-flex items-center gap-1 rounded-full bg-[#FDECEC] dark:bg-[#4E161A] px-3 py-1.5 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA]"
          >
            <FilterX className="h-3.5 w-3.5" />
            <span>Sıfırla</span>
          </button>
        )}

        {availableWordTypes.map((wt) => {
          const selected = wordTypeFilter === wt;
          return (
            <button
              key={wt}
              onClick={() => setWordTypeFilter(selected ? null : wt)}
              className={`shrink-0 inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-xs font-semibold border transition ${
                selected
                  ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/40 text-[#8E1B20] dark:text-[#FFD9DA]'
                  : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
              }`}
            >
              {selected && <Check className="h-3 w-3" />}
              <span>{wt}</span>
            </button>
          );
        })}

        {Array.from({ length: 12 }, (_, i) => i + 1).map((deckNum) => {
          const selected = deckFilter === deckNum;
          return (
            <button
              key={deckNum}
              onClick={() => setDeckFilter(selected ? null : deckNum)}
              className={`shrink-0 inline-flex items-center gap-1 rounded-full px-3 py-1.5 text-xs font-semibold border transition ${
                selected
                  ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/40 text-[#8E1B20] dark:text-[#FFD9DA]'
                  : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
              }`}
            >
              {selected && <Check className="h-3 w-3" />}
              <span>Deste {deckNum}</span>
            </button>
          );
        })}
      </div>

      {/* 5. Vertical List of 300 Quranic Words */}
      {displayedWords.length === 0 ? (
        <div className="rounded-[24px] bg-white dark:bg-[#1B1B22] p-8 text-center border border-[#E4E4EC] dark:border-[#32323E] my-4">
          <p className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
            {statusFilter === 'LEARNED'
              ? 'Henüz Öğrenilen Kelime Yok'
              : statusFilter === 'UNLEARNED'
              ? 'Tüm Kelimeler Öğrenildi!'
              : 'Eşleşen Kelime Bulunamadı'}
          </p>
          <p className="mt-1 text-xs text-[#5A5A64] dark:text-[#B8B8C4]">
            {statusFilter === 'LEARNED'
              ? 'Listeden bildiğiniz kelimelerin yanındaki sıra butonuna dokunarak veya kelime kartlarıyla çalışarak öğrenilenlere ekleyebilirsiniz.'
              : 'Arama teriminizi veya seçili filtreleri değiştirerek 300 Kur\'an kelimesine göz atabilirsiniz.'}
          </p>
          {hasActiveSecondaryFilters && (
            <button
              onClick={clearAllFilters}
              className="mt-4 inline-flex items-center gap-1.5 rounded-full border border-[#B7282E] dark:border-[#E2474D] px-4 py-2 text-xs font-bold text-[#B7282E] dark:text-[#E2474D]"
            >
              <FilterX className="h-3.5 w-3.5" />
              <span>Tüm Filtreleri Temizle</span>
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-2.5">
          {displayedWords.map((word) => {
            const isLearned = !!progressMap[word.id]?.isLearned;
            const deckNum = getDeck12Number(word.id);
            return (
              <div
                key={word.id}
                onClick={() => setSelectedWordForDetail(word)}
                className={`flex items-center justify-between gap-2 rounded-[18px] p-3 border cursor-pointer transition ${
                  isLearned
                    ? 'bg-[#FDECEC]/30 dark:bg-[#4E161A]/25 border-[#2E7D32]/45'
                    : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] shadow-sm'
                }`}
              >
                <div className="flex items-center gap-2.5 min-w-0 flex-1">
                  {/* One-Tap Learned / Unlearned Circle Button */}
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onToggleLearned(word.id, !isLearned);
                    }}
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-extrabold transition ${
                      isLearned
                        ? 'bg-[#2E7D32] text-white'
                        : 'bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]'
                    }`}
                    title={isLearned ? 'Öğrenildi - Değiştirmek için dokunun' : 'Öğrenildi olarak işaretle'}
                  >
                    {isLearned ? <Check className="h-4 w-4" /> : word.id}
                  </button>

                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7] truncate">
                      {word.turkishMeaning}
                    </p>
                    <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4] truncate">
                      {word.transliteration} • {word.wordType}
                    </p>
                    <div className="mt-1 flex items-center gap-1.5 flex-wrap">
                      <span className="rounded-md bg-[#C89D42]/20 px-1.5 py-0.5 text-[11px] font-semibold text-[#1A1A1E] dark:text-[#F2F2F7]">
                        Kur'an'da {word.frequency} kez
                      </span>
                      <span className="rounded-md bg-[#F0F0F4] dark:bg-[#262630] px-1.5 py-0.5 text-[11px] text-[#5A5A64] dark:text-[#B8B8C4]">
                        Deste {deckNum}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <p
                    className="font-arabic text-3xl font-bold text-[#B7282E] dark:text-[#E2474D] px-1"
                    dir="rtl"
                  >
                    {word.arabic}
                  </p>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      audioPlayer.playWord(word.arabic, word.audioUrl);
                    }}
                    className="flex h-9 w-9 items-center justify-center rounded-full bg-[#B7282E]/[0.12] dark:bg-[#E2474D]/[0.15] text-[#B7282E] dark:text-[#E2474D] hover:opacity-80 transition"
                    aria-label="Kelimeyi Dinle"
                  >
                    <Volume2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Word Detail Bottom Sheet / Modal */}
      {selectedWordForDetail && (
        <div
          onClick={() => setSelectedWordForDetail(null)}
          className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-xs p-0 sm:p-4"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-xl rounded-t-[28px] sm:rounded-[28px] bg-white dark:bg-[#1B1B22] p-6 shadow-2xl border-t sm:border border-[#E4E4EC] dark:border-[#32323E] space-y-4"
          >
            <div className="flex items-center justify-between">
              <span className="rounded-xl bg-[#FDECEC] dark:bg-[#4E161A] px-3 py-1 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA]">
                #{selectedWordForDetail.id} • Deste {getDeck12Number(selectedWordForDetail.id)}
              </span>
              <span className="rounded-xl bg-[#C89D42]/20 px-3 py-1 text-xs font-bold text-[#1A1A1E] dark:text-[#F2F2F7]">
                Kur'an'da {selectedWordForDetail.frequency} kez
              </span>
            </div>

            <div className="text-center space-y-1">
              <p
                className="font-arabic text-5xl font-bold text-[#B7282E] dark:text-[#E2474D]"
                dir="rtl"
              >
                {selectedWordForDetail.arabic}
              </p>
              <p className="text-sm text-[#8A8A96] dark:text-[#7E7E8C]">
                {selectedWordForDetail.transliteration} ({selectedWordForDetail.wordType})
              </p>
              <h3 className="text-2xl font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7] pt-1">
                {selectedWordForDetail.turkishMeaning}
              </h3>

              <div className="pt-2">
                <button
                  onClick={() =>
                    audioPlayer.playWord(
                      selectedWordForDetail.arabic,
                      selectedWordForDetail.audioUrl
                    )
                  }
                  className="inline-flex items-center gap-2 rounded-3xl bg-[#FDECEC] dark:bg-[#4E161A] px-5 py-2.5 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA]"
                >
                  <Volume2 className="h-4 w-4" />
                  <span>Kelimeyi Telaffuz Et</span>
                </button>
              </div>
            </div>

            {/* Example Ayah Card */}
            <div className="rounded-[18px] bg-[#F0F0F4]/60 dark:bg-[#262630]/60 p-4 space-y-2 border border-[#E4E4EC] dark:border-[#32323E]">
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#1A1A1E] dark:text-[#F2F2F7]">
                  <BookOpen className="h-4 w-4 text-[#C89D42]" />
                  Örnek Ayet ({selectedWordForDetail.exampleAyahReference})
                </span>
                <button
                  onClick={() =>
                    audioPlayer.playAyah(
                      selectedWordForDetail.exampleAudioUrl,
                      selectedWordForDetail.exampleAyahArabic
                    )
                  }
                  className="flex h-9 w-9 items-center justify-center rounded-full text-[#B7282E] dark:text-[#E2474D]"
                >
                  <Volume2 className="h-5 w-5" />
                </button>
              </div>
              <p
                className="font-arabic text-xl text-right text-[#1A1A1E] dark:text-[#F2F2F7] leading-relaxed"
                dir="rtl"
              >
                {selectedWordForDetail.exampleAyahArabic}
              </p>
              <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4]">
                "{selectedWordForDetail.exampleAyahTurkish}"
              </p>
            </div>

            {/* Learned / Unlearned Action Button */}
            {progressMap[selectedWordForDetail.id]?.isLearned ? (
              <button
                onClick={() => {
                  onToggleLearned(selectedWordForDetail.id, false);
                  setSelectedWordForDetail(null);
                }}
                className="flex w-full h-[52px] items-center justify-center gap-2 rounded-2xl border-[1.5px] border-[#2E7D32] text-[#2E7D32] font-bold text-sm"
              >
                <CheckCircle2 className="h-5 w-5" />
                <span>Öğrenildi (Öğrenilecek Olarak İşaretle)</span>
              </button>
            ) : (
              <button
                onClick={() => {
                  onToggleLearned(selectedWordForDetail.id, true);
                  setSelectedWordForDetail(null);
                }}
                className="flex w-full h-[52px] items-center justify-center gap-2 rounded-2xl bg-[#2E7D32] text-white font-bold text-sm"
              >
                <Circle className="h-5 w-5" />
                <span>Öğrenildi Olarak İşaretle</span>
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
