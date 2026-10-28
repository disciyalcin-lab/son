import React, { useState, useMemo, useEffect, useRef, useCallback } from 'react';
import { QURAN_WORDS_300, TOTAL_QURAN_WORDS, QuranWord } from '../data/wordsData';
import { WordProgress, getDeck12Number } from '../utils/storage';
import { audioPlayer, useAudioPlayerState } from '../utils/audioPlayer';
import {
  matchesMultilingualSearch,
  splitAyahWithHighlight
} from '../utils/arabicUtils';
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
  FilterX,
  Loader2,
  Square,
  AlertCircle,
  Sparkles
} from 'lucide-react';
import { AppLogo } from './AppLogo';

interface DictionaryProps {
  progressMap: Record<number, WordProgress>;
  isDark: boolean;
  onToggleTheme: () => void;
  onToggleLearned: (wordId: number, learned: boolean) => void;
}

type StatusFilter = 'ALL' | 'UNLEARNED' | 'LEARNED';

// Fixed row height (86px card + 10px gap = 96px) for 0 CLS & 60/120 FPS virtualization
const VIRTUAL_ROW_HEIGHT = 96;
const VIRTUAL_OVERSCAN = 6;
const SWIPE_TRIGGER_PX = 56;

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

  // Optimistic UI & Race-Condition lock state
  const [optimisticMap, setOptimisticMap] = useState<Record<number, boolean>>({});
  const updatingIdsRef = useRef<Set<number>>(new Set());
  const [swipeFeedbackToast, setSwipeFeedbackToast] = useState<string | null>(null);

  // Touch Swipe state for quick-toggle
  const [swipingWordId, setSwipingWordId] = useState<number | null>(null);
  const [swipeOffsetX, setSwipeOffsetX] = useState<number>(0);
  const touchStartRef = useRef<{ x: number; y: number; wordId: number } | null>(null);

  // Audio player reactive state (loading spinner, active playing, error toast)
  const audioState = useAudioPlayerState();

  // Sync optimistic overrides when parent progressMap updates
  useEffect(() => {
    setOptimisticMap((prev) => {
      if (Object.keys(prev).length === 0) return prev;
      const next = { ...prev };
      let changed = false;
      for (const key of Object.keys(next)) {
        const id = Number(key);
        if (!!progressMap[id]?.isLearned === next[id]) {
          delete next[id];
          changed = true;
        }
      }
      return changed ? next : prev;
    });
  }, [progressMap]);

  const isWordLearned = useCallback(
    (wordId: number): boolean => {
      if (wordId in optimisticMap) {
        return optimisticMap[wordId];
      }
      return !!progressMap[wordId]?.isLearned;
    },
    [optimisticMap, progressMap]
  );

  // Race-condition safe optimistic toggle
  const handleSafeToggleLearned = useCallback(
    (word: QuranWord, targetState?: boolean) => {
      if (updatingIdsRef.current.has(word.id)) {
        return;
      }
      updatingIdsRef.current.add(word.id);

      const currentLearned = isWordLearned(word.id);
      const nextLearned = targetState !== undefined ? targetState : !currentLearned;

      // 1. Instant Optimistic UI update
      setOptimisticMap((prev) => ({ ...prev, [word.id]: nextLearned }));

      // 2. Persist state
      try {
        onToggleLearned(word.id, nextLearned);
      } finally {
        setTimeout(() => {
          updatingIdsRef.current.delete(word.id);
        }, 180);
      }
    },
    [isWordLearned, onToggleLearned]
  );

  // Strictly sorted by frequency DESC, id ASC (1..300)
  const sortedBaseWords = useMemo(() => {
    return [...QURAN_WORDS_300].sort((a, b) => b.frequency - a.frequency || a.id - b.id);
  }, []);

  // Global mastery progress
  const globalLearnedWords = useMemo(
    () => sortedBaseWords.filter((w) => isWordLearned(w.id)),
    [sortedBaseWords, isWordLearned]
  );
  const globalLearnedCount = globalLearnedWords.length;
  const globalTotalCount = sortedBaseWords.length;
  const coveragePercentage = useMemo(() => {
    const freqSum = globalLearnedWords.reduce((sum, w) => sum + w.frequency, 0);
    return Math.min(100, (freqSum / TOTAL_QURAN_WORDS) * 100);
  }, [globalLearnedWords]);

  // Deck-by-Deck progress statistics (Decks 1..12) for Secondary Filter Chips
  const deckStatsList = useMemo(() => {
    return Array.from({ length: 12 }, (_, idx) => {
      const deckNum = idx + 1;
      const wordsInDeck = sortedBaseWords.filter((w) => getDeck12Number(w.id) === deckNum);
      const total = wordsInDeck.length || 25;
      const learned = wordsInDeck.filter((w) => isWordLearned(w.id)).length;
      const pct = total > 0 ? Math.round((learned / total) * 100) : 0;
      return {
        deckNum,
        learned,
        total,
        pct,
        isComplete: learned >= total && total > 0
      };
    });
  }, [sortedBaseWords, isWordLearned]);

  // Available word types
  const availableWordTypes = useMemo(() => {
    const set = new Set<string>();
    sortedBaseWords.forEach((w) => {
      if (w.wordType) set.add(w.wordType);
    });
    return Array.from(set);
  }, [sortedBaseWords]);

  // Multilingual normalized search (Arabic tashkeel/alef/ta-marbuta + Turkish tr-TR locale)
  const contextWords = useMemo(() => {
    return sortedBaseWords.filter((w) => {
      if (deckFilter !== null && getDeck12Number(w.id) !== deckFilter) return false;
      if (wordTypeFilter !== null && w.wordType !== wordTypeFilter) return false;
      return matchesMultilingualSearch(w, searchQuery);
    });
  }, [sortedBaseWords, searchQuery, deckFilter, wordTypeFilter]);

  // Live counts on each filter tab
  const counts = useMemo(() => {
    const learned = contextWords.filter((w) => isWordLearned(w.id)).length;
    return {
      all: contextWords.length,
      learned,
      unlearned: contextWords.length - learned
    };
  }, [contextWords, isWordLearned]);

  const displayedWords = useMemo(() => {
    return contextWords
      .filter((w) => {
        const learned = isWordLearned(w.id);
        if (statusFilter === 'LEARNED') return learned;
        if (statusFilter === 'UNLEARNED') return !learned;
        return true;
      })
      .sort((a, b) => b.frequency - a.frequency || a.id - b.id);
  }, [contextWords, isWordLearned, statusFilter]);

  // Virtual List Engine (window scroll synchronized with container offsetTop)
  const listContainerRef = useRef<HTMLDivElement | null>(null);
  const [scrollWindow, setScrollWindow] = useState<{ scrollTop: number; viewportHeight: number }>({
    scrollTop: 0,
    viewportHeight: typeof window !== 'undefined' ? window.innerHeight : 800
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;
    let rafId: number | null = null;

    const updateScrollMetrics = () => {
      rafId = null;
      const containerTop = listContainerRef.current
        ? listContainerRef.current.getBoundingClientRect().top + window.scrollY
        : 0;
      const relativeScrollTop = Math.max(0, window.scrollY - containerTop);
      setScrollWindow({
        scrollTop: relativeScrollTop,
        viewportHeight: window.innerHeight
      });
    };

    const onScrollOrResize = () => {
      if (rafId === null) {
        rafId = window.requestAnimationFrame(updateScrollMetrics);
      }
    };

    updateScrollMetrics();
    window.addEventListener('scroll', onScrollOrResize, { passive: true });
    window.addEventListener('resize', onScrollOrResize, { passive: true });

    return () => {
      if (rafId !== null) window.cancelAnimationFrame(rafId);
      window.removeEventListener('scroll', onScrollOrResize);
      window.removeEventListener('resize', onScrollOrResize);
    };
  }, [displayedWords.length, statusFilter, deckFilter, wordTypeFilter, searchQuery]);

  const { virtualItems, totalListHeight } = useMemo(() => {
    const totalCount = displayedWords.length;
    const totalHeight = totalCount * VIRTUAL_ROW_HEIGHT;
    const rawStartIndex = Math.floor(scrollWindow.scrollTop / VIRTUAL_ROW_HEIGHT);
    const visibleCount = Math.ceil(scrollWindow.viewportHeight / VIRTUAL_ROW_HEIGHT);

    const startIndex = Math.max(0, rawStartIndex - VIRTUAL_OVERSCAN);
    const endIndex = Math.min(
      totalCount - 1,
      rawStartIndex + visibleCount + VIRTUAL_OVERSCAN
    );

    const items: { word: QuranWord; index: number; top: number }[] = [];
    for (let i = startIndex; i <= endIndex; i++) {
      if (displayedWords[i]) {
        items.push({
          word: displayedWords[i],
          index: i,
          top: i * VIRTUAL_ROW_HEIGHT
        });
      }
    }

    return {
      virtualItems: items,
      totalListHeight: totalHeight
    };
  }, [displayedWords, scrollWindow]);

  const hasActiveSecondaryFilters =
    searchQuery.trim().length > 0 || wordTypeFilter !== null || deckFilter !== null;

  const clearAllFilters = () => {
    setSearchQuery('');
    setWordTypeFilter(null);
    setDeckFilter(null);
    setStatusFilter('ALL');
  };

  // Touch Swipe handlers for mobile quick-toggle
  const handleTouchStart = (e: React.TouchEvent, wordId: number) => {
    if (e.touches.length !== 1) return;
    touchStartRef.current = {
      x: e.touches[0].clientX,
      y: e.touches[0].clientY,
      wordId
    };
    setSwipingWordId(wordId);
    setSwipeOffsetX(0);
  };

  const handleTouchMove = (e: React.TouchEvent, wordId: number) => {
    const start = touchStartRef.current;
    if (!start || start.wordId !== wordId || e.touches.length !== 1) return;

    const deltaX = e.touches[0].clientX - start.x;
    const deltaY = Math.abs(e.touches[0].clientY - start.y);

    // Ignore vertical scrolling gestures
    if (deltaY > 28 && Math.abs(deltaX) < deltaY) {
      touchStartRef.current = null;
      setSwipingWordId(null);
      setSwipeOffsetX(0);
      return;
    }

    if (deltaX > 8) {
      const clamped = Math.min(84, deltaX);
      setSwipeOffsetX(clamped);
    } else {
      setSwipeOffsetX(0);
    }
  };

  const handleTouchEnd = (word: QuranWord) => {
    if (swipingWordId === word.id && swipeOffsetX >= SWIPE_TRIGGER_PX) {
      const nextState = !isWordLearned(word.id);
      handleSafeToggleLearned(word, nextState);
      setSwipeFeedbackToast(
        nextState
          ? `✓ "${word.turkishMeaning}" öğrenilenlere eklendi`
          : `↺ "${word.turkishMeaning}" tekrar listesine alındı`
      );
      setTimeout(() => setSwipeFeedbackToast(null), 2200);
    }
    touchStartRef.current = null;
    setSwipingWordId(null);
    setSwipeOffsetX(0);
  };

  // Highlighted Ayah segments for WordDetailBottomSheet
  const highlightedAyahSegments = useMemo(() => {
    if (!selectedWordForDetail) return [];
    return splitAyahWithHighlight(
      selectedWordForDetail.exampleAyahArabic,
      selectedWordForDetail.arabic
    );
  }, [selectedWordForDetail]);

  return (
    <div className="space-y-3 pb-8">
      {/* 1. Header & Live Mastery Progress Banner ("Kur'an 300 Kelime Hazinesi") */}
      <div className="overflow-hidden rounded-[20px] bg-white dark:bg-[#1B1B22] shadow-sm border border-[#E4E4EC] dark:border-[#32323E]">
        <div className="bg-gradient-to-r from-[#B7282E]/[0.12] to-[#DEC286]/[0.25] dark:from-[#E2474D]/[0.15] dark:to-[#4F3E1E]/[0.35] px-4 py-3.5">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2.5 min-w-0 flex-1">
              <AppLogo size={38} />
              <div className="min-w-0 flex-1">
                <h2 className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7] truncate">
                  Kur'an 300 Kelime Hazinesi
                </h2>
                <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4] mt-0.5 truncate">
                  {globalLearnedCount} / {globalTotalCount} öğrenildi •{' '}
                  {globalTotalCount - globalLearnedCount} kalan
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <span className="rounded-xl bg-[#DEC286] dark:bg-[#4F3E1E] px-2.5 py-1.5 text-xs font-extrabold tabular-nums text-[#2C2214] dark:text-[#F7E3B5]">
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

      {/* Audio Error or Swipe Feedback Snackbar */}
      {(audioState.errorMessage || swipeFeedbackToast) && (
        <div
          className={`flex items-center justify-between gap-2 rounded-2xl px-4 py-2.5 text-xs font-bold shadow-sm border transition ${
            audioState.errorMessage
              ? 'bg-[#E65100]/15 border-[#E65100]/35 text-[#E65100]'
              : 'bg-[#2E7D32]/15 border-[#2E7D32]/35 text-[#2E7D32]'
          }`}
        >
          <div className="flex items-center gap-2 min-w-0">
            {audioState.errorMessage ? (
              <AlertCircle className="h-4 w-4 shrink-0" />
            ) : (
              <Sparkles className="h-4 w-4 shrink-0" />
            )}
            <span className="truncate">
              {audioState.errorMessage || swipeFeedbackToast}
            </span>
          </div>
          {audioState.errorMessage && (
            <button
              onClick={() => audioPlayer.clearError()}
              className="shrink-0 rounded-full p-1 hover:bg-black/10"
              aria-label="Kapat"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      )}

      {/* 2. Primary Status Filter Bar (Tümü / Öğrenilecek / Öğrenilen with Live Counts) */}
      <div className="grid grid-cols-3 gap-1.5 rounded-2xl bg-[#F0F0F4]/80 dark:bg-[#262630]/80 p-1">
        <button
          onClick={() => setStatusFilter('ALL')}
          className={`h-10 rounded-xl text-xs font-bold tabular-nums transition ${
            statusFilter === 'ALL'
              ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white shadow-sm'
              : 'text-[#5A5A64] dark:text-[#B8B8C4]'
          }`}
        >
          Tümü ({counts.all})
        </button>
        <button
          onClick={() => setStatusFilter('UNLEARNED')}
          className={`h-10 rounded-xl text-xs font-bold tabular-nums transition ${
            statusFilter === 'UNLEARNED'
              ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white shadow-sm'
              : 'text-[#5A5A64] dark:text-[#B8B8C4]'
          }`}
        >
          Öğrenilecek ({counts.unlearned})
        </button>
        <button
          onClick={() => setStatusFilter('LEARNED')}
          className={`h-10 rounded-xl text-xs font-bold tabular-nums transition ${
            statusFilter === 'LEARNED'
              ? 'bg-[#2E7D32] text-white shadow-sm'
              : 'text-[#5A5A64] dark:text-[#B8B8C4]'
          }`}
        >
          Öğrenilen ({counts.learned})
        </button>
      </div>

      {/* 3. Multilingual Normalized Search Bar (Arabic harekeli/harekesiz + Turkish tr-TR) */}
      <div className="relative">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-[#B7282E] dark:text-[#E2474D]" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Türkçe anlam, okunuş veya Arapça (harekeli/harekesiz) ara..."
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

      {/* 4. Secondary Filter Chips: Deck Progress Chips (Deste 1..12 with learned ratio & mini ring) + Word Types */}
      <div className="space-y-1.5">
        {/* Row A: Deck-Based Progress Chips (Deste 1..12 with live X/25 & mini progress ring) */}
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

          {deckStatsList.map((d) => {
            const selected = deckFilter === d.deckNum;
            const ringRadius = 6;
            const ringCircumference = 2 * Math.PI * ringRadius;
            const ringOffset = ringCircumference - (d.pct / 100) * ringCircumference;

            return (
              <button
                key={`deck-chip-${d.deckNum}`}
                onClick={() => setDeckFilter(selected ? null : d.deckNum)}
                className={`shrink-0 inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-semibold border transition ${
                  selected
                    ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/50 text-[#8E1B20] dark:text-[#FFD9DA] shadow-2xs'
                    : d.isComplete
                    ? 'bg-[#2E7D32]/10 border-[#2E7D32]/40 text-[#2E7D32]'
                    : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
                }`}
              >
                {/* Mini Circular Progress Indicator */}
                {d.isComplete ? (
                  <CheckCircle2 className="h-3.5 w-3.5 text-[#2E7D32] shrink-0" />
                ) : (
                  <svg className="h-4 w-4 -rotate-90 shrink-0" viewBox="0 0 16 16">
                    <circle
                      cx="8"
                      cy="8"
                      r={ringRadius}
                      stroke={isDark ? '#32323E' : '#E4E4EC'}
                      strokeWidth="2.2"
                      fill="transparent"
                    />
                    <circle
                      cx="8"
                      cy="8"
                      r={ringRadius}
                      stroke={
                        selected
                          ? isDark
                            ? '#FFD9DA'
                            : '#B7282E'
                          : '#C89D42'
                      }
                      strokeWidth="2.2"
                      strokeDasharray={ringCircumference}
                      strokeDashoffset={ringOffset}
                      strokeLinecap="round"
                      fill="transparent"
                    />
                  </svg>
                )}

                <span>Deste {d.deckNum}</span>
                <span
                  className={`rounded-md px-1.5 py-0.2 text-[10px] font-extrabold tabular-nums ${
                    selected
                      ? 'bg-[#B7282E]/15 dark:bg-black/25 text-[#8E1B20] dark:text-[#FFD9DA]'
                      : d.isComplete
                      ? 'bg-[#2E7D32]/15 text-[#2E7D32]'
                      : 'bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]'
                  }`}
                >
                  {d.learned}/{d.total}
                </span>
              </button>
            );
          })}
        </div>

        {/* Row B: Word Category Chips (İsim, Fiil, Harf/Edat, Zamir, vb.) */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-0.5 no-scrollbar">
          {availableWordTypes.map((wt) => {
            const selected = wordTypeFilter === wt;
            return (
              <button
                key={wt}
                onClick={() => setWordTypeFilter(selected ? null : wt)}
                className={`shrink-0 inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-semibold border transition ${
                  selected
                    ? 'bg-[#DEC286] dark:bg-[#4F3E1E] border-[#C89D42] text-[#2C2214] dark:text-[#F7E3B5]'
                    : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
                }`}
              >
                {selected && <Check className="h-3 w-3" />}
                <span>{wt}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Subtle Swipe Hint & Virtualized Count Info */}
      <div className="flex items-center justify-between px-1 text-[11px] text-[#8A8A96] dark:text-[#7E7E8C]">
        <span>💡 Kartı sağa kaydırarak veya sıra numarasına dokunarak işaretleyin</span>
        <span className="font-semibold tabular-nums">{displayedWords.length} kelime</span>
      </div>

      {/* 5. Virtualized Vertical List of 300 Quranic Words (60/120 FPS, 0 CLS) */}
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
              ? 'Listeden bildiğiniz kelimelerin yanındaki sıra butonuna dokunarak veya kartı sağa kaydırarak öğrenilenlere ekleyebilirsiniz.'
              : "Arama teriminizi (harekeli/harekesiz Arapça veya Türkçe) ya da seçili filtreleri değiştirerek 300 Kur'an kelimesine göz atabilirsiniz."}
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
        <div
          ref={listContainerRef}
          className="relative w-full"
          style={{ height: `${totalListHeight}px` }}
        >
          {virtualItems.map(({ word, top }) => {
            const isLearned = isWordLearned(word.id);
            const deckNum = getDeck12Number(word.id);
            const audioKey = `dict-word-${word.id}`;
            const isAudioLoading =
              audioState.status === 'loading' && audioState.activeKey === audioKey;
            const isAudioPlaying =
              audioState.status === 'playing' && audioState.activeKey === audioKey;
            const currentSwipeOffset = swipingWordId === word.id ? swipeOffsetX : 0;

            return (
              <div
                key={word.id}
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  height: `${VIRTUAL_ROW_HEIGHT - 10}px`,
                  transform: `translate3d(0, ${top}px, 0)`
                }}
                className="overflow-hidden rounded-[18px]"
              >
                {/* Background Swipe Action Layer */}
                {currentSwipeOffset > 0 && (
                  <div
                    className={`absolute inset-0 flex items-center pl-4 rounded-[18px] text-xs font-extrabold text-white ${
                      isLearned ? 'bg-[#C89D42]' : 'bg-[#2E7D32]'
                    }`}
                  >
                    <span>
                      {isLearned ? '↺ Tekrar Listesine Al' : '✓ Öğrenildi İşaretle'}
                    </span>
                  </div>
                )}

                {/* Foreground Card */}
                <div
                  onTouchStart={(e) => handleTouchStart(e, word.id)}
                  onTouchMove={(e) => handleTouchMove(e, word.id)}
                  onTouchEnd={() => handleTouchEnd(word)}
                  onClick={() => setSelectedWordForDetail(word)}
                  style={{
                    transform:
                      currentSwipeOffset > 0
                        ? `translate3d(${currentSwipeOffset}px, 0, 0)`
                        : undefined
                  }}
                  className={`flex h-full items-center justify-between gap-2 rounded-[18px] px-3 py-2.5 border cursor-pointer select-none transition-colors ${
                    isLearned
                      ? 'bg-[#FDECEC]/35 dark:bg-[#4E161A]/30 border-[#2E7D32]/45'
                      : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] shadow-xs'
                  }`}
                >
                  <div className="flex items-center gap-2.5 min-w-0 flex-1">
                    {/* Optimistic One-Tap Learned / Unlearned Circle Button */}
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleSafeToggleLearned(word);
                      }}
                      className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-extrabold tabular-nums transition active:scale-90 ${
                        isLearned
                          ? 'bg-[#2E7D32] text-white shadow-2xs'
                          : 'bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]'
                      }`}
                      title={
                        isLearned
                          ? 'Öğrenildi - Değiştirmek için dokunun'
                          : 'Öğrenildi olarak işaretle'
                      }
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
                      <div className="mt-1 flex items-center gap-1.5">
                        <span className="rounded-md bg-[#C89D42]/20 px-1.5 py-0.5 text-[11px] font-semibold tabular-nums text-[#1A1A1E] dark:text-[#F2F2F7]">
                          Kur'an'da {word.frequency} kez
                        </span>
                        <span className="rounded-md bg-[#F0F0F4] dark:bg-[#262630] px-1.5 py-0.5 text-[11px] tabular-nums text-[#5A5A64] dark:text-[#B8B8C4]">
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
                        audioPlayer.playWord(
                          word.arabic,
                          word.audioUrl,
                          undefined,
                          audioKey
                        );
                      }}
                      className={`flex h-9 w-9 items-center justify-center rounded-full transition ${
                        isAudioPlaying
                          ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white ring-2 ring-[#C89D42]/50'
                          : 'bg-[#B7282E]/[0.12] dark:bg-[#E2474D]/[0.15] text-[#B7282E] dark:text-[#E2474D] hover:opacity-80'
                      }`}
                      aria-label={isAudioPlaying ? 'Durdur' : 'Kelimeyi Dinle'}
                    >
                      {isAudioLoading ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : isAudioPlaying ? (
                        <Square className="h-3.5 w-3.5 fill-current" />
                      ) : (
                        <Volume2 className="h-4 w-4" />
                      )}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* 6. Word Detail Bottom Sheet / Modal with Dynamic QuranGold Ayah Word Highlighting */}
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
              <div className="flex items-center gap-2">
                <span className="rounded-xl bg-[#C89D42]/20 px-3 py-1 text-xs font-bold text-[#1A1A1E] dark:text-[#F2F2F7]">
                  Kur'an'da {selectedWordForDetail.frequency} kez
                </span>
                <button
                  onClick={() => setSelectedWordForDetail(null)}
                  className="flex h-8 w-8 items-center justify-center rounded-full bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]"
                  aria-label="Kapat"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
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
                {(() => {
                  const detailWordAudioKey = `detail-word-${selectedWordForDetail.id}`;
                  const isLoading =
                    audioState.status === 'loading' &&
                    audioState.activeKey === detailWordAudioKey;
                  const isPlaying =
                    audioState.status === 'playing' &&
                    audioState.activeKey === detailWordAudioKey;
                  return (
                    <button
                      onClick={() =>
                        audioPlayer.playWord(
                          selectedWordForDetail.arabic,
                          selectedWordForDetail.audioUrl,
                          undefined,
                          detailWordAudioKey
                        )
                      }
                      className={`inline-flex items-center gap-2 rounded-3xl px-5 py-2.5 text-xs font-bold transition ${
                        isPlaying
                          ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white'
                          : 'bg-[#FDECEC] dark:bg-[#4E161A] text-[#8E1B20] dark:text-[#FFD9DA]'
                      }`}
                    >
                      {isLoading ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : isPlaying ? (
                        <Square className="h-3.5 w-3.5 fill-current" />
                      ) : (
                        <Volume2 className="h-4 w-4" />
                      )}
                      <span>
                        {isLoading
                          ? 'Yükleniyor...'
                          : isPlaying
                          ? 'Oynatılıyor (Durdur)'
                          : 'Kelimeyi Telaffuz Et'}
                      </span>
                    </button>
                  );
                })()}
              </div>
            </div>

            {/* Example Ayah Card with Dynamic QuranGold Word Highlighting */}
            <div className="rounded-[18px] bg-[#F0F0F4]/60 dark:bg-[#262630]/60 p-4 space-y-2.5 border border-[#E4E4EC] dark:border-[#32323E]">
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#1A1A1E] dark:text-[#F2F2F7]">
                  <BookOpen className="h-4 w-4 text-[#C89D42]" />
                  Örnek Ayet ({selectedWordForDetail.exampleAyahReference})
                </span>
                {(() => {
                  const detailAyahAudioKey = `detail-ayah-${selectedWordForDetail.id}`;
                  const isLoading =
                    audioState.status === 'loading' &&
                    audioState.activeKey === detailAyahAudioKey;
                  const isPlaying =
                    audioState.status === 'playing' &&
                    audioState.activeKey === detailAyahAudioKey;
                  return (
                    <button
                      onClick={() =>
                        audioPlayer.playAyah(
                          selectedWordForDetail.exampleAudioUrl,
                          selectedWordForDetail.exampleAyahArabic,
                          undefined,
                          detailAyahAudioKey
                        )
                      }
                      className={`flex h-9 w-9 items-center justify-center rounded-full transition ${
                        isPlaying
                          ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white'
                          : 'text-[#B7282E] dark:text-[#E2474D] hover:bg-[#B7282E]/10'
                      }`}
                      title={isPlaying ? 'Durdur' : 'Ayeti Dinle'}
                    >
                      {isLoading ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : isPlaying ? (
                        <Square className="h-3.5 w-3.5 fill-current" />
                      ) : (
                        <Volume2 className="h-5 w-5" />
                      )}
                    </button>
                  );
                })()}
              </div>

              {/* Arabic Ayah with Highlighted Target Word in QuranGold */}
              <p
                className="font-arabic text-2xl text-right text-[#1A1A1E] dark:text-[#F2F2F7] leading-loose"
                dir="rtl"
              >
                {highlightedAyahSegments.map((seg, idx) =>
                  seg.isMatch ? (
                    <mark
                      key={idx}
                      className="rounded-lg bg-[#C89D42]/30 dark:bg-[#DEC286]/25 px-1.5 py-0.5 font-bold text-[#B7282E] dark:text-[#DEC286] underline decoration-[#C89D42] decoration-2 underline-offset-8"
                    >
                      {seg.text}
                    </mark>
                  ) : (
                    <React.Fragment key={idx}>{seg.text}</React.Fragment>
                  )
                )}
              </p>

              <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4] leading-relaxed">
                "{selectedWordForDetail.exampleAyahTurkish}"
              </p>
            </div>

            {/* Learned / Unlearned Action Button */}
            {isWordLearned(selectedWordForDetail.id) ? (
              <button
                onClick={() => {
                  handleSafeToggleLearned(selectedWordForDetail, false);
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
                  handleSafeToggleLearned(selectedWordForDetail, true);
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
