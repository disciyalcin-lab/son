import React, { useState, useMemo, useEffect } from 'react';
import { QURAN_WORDS_300 } from '../data/wordsData';
import {
  WordProgress,
  getDeck12Number,
  getLast7DaysLearnedData
} from '../utils/storage';
import { audioPlayer } from '../utils/audioPlayer';
import {
  Volume2,
  RotateCcw,
  Check,
  ArrowLeft,
  ArrowRight,
  Shuffle,
  PlayCircle,
  Sun,
  Moon,
  BookOpen,
  Trophy,
  RefreshCw
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Cell
} from 'recharts';

interface FlashcardsProps {
  selectedDeckId: number | null;
  progressMap: Record<number, WordProgress>;
  isDark: boolean;
  autoPlayAudio: boolean;
  onToggleTheme: () => void;
  onToggleAutoPlay: () => void;
  onSelectDeck: (deckId: number) => void;
  onMarkWord: (wordId: number, knewIt: boolean) => void;
}

type FilterMode = 'ALL' | 'UNLEARNED' | 'LEARNED';

export const Flashcards: React.FC<FlashcardsProps> = ({
  selectedDeckId,
  progressMap,
  isDark,
  autoPlayAudio,
  onToggleTheme,
  onToggleAutoPlay,
  onSelectDeck,
  onMarkWord
}) => {
  const [filterMode, setFilterMode] = useState<FilterMode>('ALL');
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [playingType, setPlayingType] = useState<'word' | 'ayah' | null>(null);
  const [shuffleSeed, setShuffleSeed] = useState<number>(0);
  const [isSessionComplete, setIsSessionComplete] = useState(false);
  const [learnedInSessionCount, setLearnedInSessionCount] = useState(0);
  const [repeatInSessionCount, setRepeatInSessionCount] = useState(0);

  const activeDeck = selectedDeckId || 1;

  // Last 7 days daily learned words bar chart data
  const last7DaysData = useMemo(
    () => getLast7DaysLearnedData(progressMap),
    [progressMap]
  );
  const last7DaysTotal = useMemo(
    () => last7DaysData.reduce((sum, item) => sum + item.count, 0),
    [last7DaysData]
  );

  // 12 Decks (25 words each), strictly frequency DESC by default
  const deckWords = useMemo(() => {
    const base = QURAN_WORDS_300.filter((w) => getDeck12Number(w.id) === activeDeck).sort(
      (a, b) => b.frequency - a.frequency || a.id - b.id
    );
    if (shuffleSeed > 0) {
      return [...base].sort(() => Math.random() - 0.5);
    }
    return base;
  }, [activeDeck, shuffleSeed]);

  const filteredWords = useMemo(() => {
    return deckWords.filter((w) => {
      const isLearned = !!progressMap[w.id]?.isLearned;
      if (filterMode === 'LEARNED') return isLearned;
      if (filterMode === 'UNLEARNED') return !isLearned;
      return true;
    });
  }, [deckWords, progressMap, filterMode]);

  const safeIndex = filteredWords.length > 0 ? Math.min(currentIndex, filteredWords.length - 1) : 0;
  const currentWord = filteredWords[safeIndex];

  useEffect(() => {
    if (autoPlayAudio && currentWord && !isSessionComplete) {
      setPlayingType('word');
      audioPlayer.playWord(currentWord.arabic, currentWord.audioUrl, () => setPlayingType(null));
    }
  }, [currentWord?.id, autoPlayAudio, isSessionComplete]);

  const handlePlayWord = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!currentWord) return;
    setPlayingType('word');
    audioPlayer.playWord(currentWord.arabic, currentWord.audioUrl, () => setPlayingType(null));
  };

  const handlePlayAyah = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!currentWord) return;
    setPlayingType('ayah');
    audioPlayer.playAyah(currentWord.exampleAudioUrl, currentWord.exampleAyahArabic, () =>
      setPlayingType(null)
    );
  };

  const handleAction = (knewIt: boolean) => {
    if (!currentWord) return;
    setIsFlipped(false);
    onMarkWord(currentWord.id, knewIt);
    if (knewIt) {
      setLearnedInSessionCount((c) => c + 1);
    } else {
      setRepeatInSessionCount((c) => c + 1);
    }

    if (safeIndex + 1 < filteredWords.length) {
      setCurrentIndex(safeIndex + 1);
    } else {
      setIsSessionComplete(true);
    }
  };

  const handleSwitchDeck = (deckNum: number) => {
    onSelectDeck(deckNum);
    setCurrentIndex(0);
    setIsFlipped(false);
    setIsSessionComplete(false);
    setLearnedInSessionCount(0);
    setRepeatInSessionCount(0);
    setShuffleSeed(0);
  };

  return (
    <div className="space-y-3 pb-8">
      {/* 1. Deck Selector Tab Row (Deste 1 .. Deste 12) */}
      <div className="-mx-4 flex items-center overflow-x-auto border-b border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22] px-4 no-scrollbar">
        {Array.from({ length: 12 }, (_, i) => i + 1).map((deckNum) => {
          const isSelected = activeDeck === deckNum;
          return (
            <button
              key={deckNum}
              onClick={() => handleSwitchDeck(deckNum)}
              className={`shrink-0 border-b-2 px-3.5 py-3 text-xs transition ${
                isSelected
                  ? 'border-[#B7282E] dark:border-[#E2474D] font-extrabold text-[#B7282E] dark:text-[#E2474D]'
                  : 'border-transparent font-medium text-[#5A5A64] dark:text-[#B8B8C4]'
              }`}
            >
              Deste {deckNum}
            </button>
          );
        })}
      </div>

      {/* 2. Secondary Control Bar (Filter Chips & Utility Toolbar) */}
      <div className="flex items-center justify-between gap-2 pt-1">
        {/* Filter Chips */}
        <div className="flex items-center gap-1.5">
          {(
            [
              { key: 'ALL', label: 'Tümü' },
              { key: 'UNLEARNED', label: 'Öğrenilecek' },
              { key: 'LEARNED', label: 'Öğrenilen' }
            ] as const
          ).map((tab) => {
            const active = filterMode === tab.key;
            return (
              <button
                key={tab.key}
                onClick={() => {
                  setFilterMode(tab.key);
                  setCurrentIndex(0);
                  setIsFlipped(false);
                  setIsSessionComplete(false);
                }}
                className={`rounded-lg px-2.5 py-1.5 text-xs font-bold border transition ${
                  active
                    ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/40 dark:border-[#E2474D]/40 text-[#8E1B20] dark:text-[#FFD9DA]'
                    : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Quick Tool Icons: Shuffle, Autoplay & Day/Night Theme Toggle */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => {
              setShuffleSeed((s) => s + 1);
              setCurrentIndex(0);
              setIsFlipped(false);
            }}
            className="flex h-9 w-9 items-center justify-center rounded-full bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4] hover:opacity-80 transition"
            title="Karıştır"
          >
            <Shuffle className="h-4 w-4" />
          </button>

          <button
            onClick={onToggleAutoPlay}
            className={`flex h-9 w-9 items-center justify-center rounded-full transition ${
              autoPlayAudio
                ? 'bg-[#FDECEC] dark:bg-[#4E161A] text-[#B7282E] dark:text-[#E2474D]'
                : 'bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]'
            }`}
            title="Otomatik Ses"
          >
            {autoPlayAudio ? <PlayCircle className="h-4 w-4" /> : <Volume2 className="h-4 w-4" />}
          </button>

          <button
            onClick={onToggleTheme}
            className="flex h-9 w-9 items-center justify-center rounded-full bg-[#F0F0F4] dark:bg-[#262630] transition"
            title={isDark ? 'Gündüz Moduna Geç' : 'Gece Moduna Geç'}
          >
            {isDark ? (
              <Sun className="h-4 w-4 text-[#DEC286]" />
            ) : (
              <Moon className="h-4 w-4 text-[#B7282E]" />
            )}
          </button>
        </div>
      </div>

      {/* Empty Filter State */}
      {filteredWords.length === 0 ? (
        <div className="rounded-[28px] bg-white dark:bg-[#1B1B22] p-10 text-center border border-[#E4E4EC] dark:border-[#32323E] my-6">
          <p className="text-base font-bold text-[#8A8A96] dark:text-[#7E7E8C]">
            Bu filtrede gösterilecek kelime yok.
          </p>
          <button
            onClick={() => setFilterMode('ALL')}
            className="mt-4 rounded-xl border border-[#B7282E] dark:border-[#E2474D] px-4 py-2 text-xs font-bold text-[#B7282E] dark:text-[#E2474D]"
          >
            Tüm Kelimeleri Göster
          </button>
        </div>
      ) : isSessionComplete ? (
        /* Deck Completion Celebration Card */
        <div className="rounded-[28px] bg-white dark:bg-[#1B1B22] p-7 text-center shadow-md border border-[#E4E4EC] dark:border-[#32323E] my-4">
          <div className="mx-auto flex h-18 w-18 items-center justify-center rounded-full bg-[#C89D42]/20 p-4 w-fit">
            <Trophy className="h-10 w-10 text-[#C89D42]" />
          </div>
          <h2 className="mt-4 text-2xl font-extrabold text-[#B7282E] dark:text-[#E2474D]">
            Tebrikler! MâşâAllah
          </h2>
          <p className="mt-1 text-sm text-[#5A5A64] dark:text-[#B8B8C4]">
            Deste {activeDeck} oturumunu tamamladınız.
          </p>

          <div className="mt-5 flex items-center justify-around">
            <div>
              <p className="text-2xl font-extrabold text-[#B7282E] dark:text-[#E2474D]">
                {learnedInSessionCount}
              </p>
              <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">Öğrenilen</p>
            </div>
            <div>
              <p className="text-2xl font-extrabold text-[#C89D42]">
                {repeatInSessionCount}
              </p>
              <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">Tekrar Edilen</p>
            </div>
          </div>

          <div className="mt-6 space-y-3">
            <button
              onClick={() => handleSwitchDeck(activeDeck < 12 ? activeDeck + 1 : 1)}
              className="flex w-full h-13 items-center justify-center gap-2 rounded-2xl bg-[#B7282E] dark:bg-[#E2474D] py-3.5 text-sm font-bold text-white shadow-sm"
            >
              <span>Sonraki Desteye Geç</span>
              <ArrowRight className="h-4 w-4" />
            </button>
            <button
              onClick={() => {
                setCurrentIndex(0);
                setIsFlipped(false);
                setIsSessionComplete(false);
                setLearnedInSessionCount(0);
                setRepeatInSessionCount(0);
              }}
              className="flex w-full h-13 items-center justify-center gap-2 rounded-2xl border border-[#8A8A96]/40 py-3.5 text-sm font-bold text-[#1A1A1E] dark:text-[#F2F2F7]"
            >
              <RefreshCw className="h-4 w-4" />
              <span>Bu Desteyi Tekrar Et</span>
            </button>
          </div>
        </div>
      ) : (
        <>
          {/* Progress Header */}
          <div className="space-y-1.5 pt-1">
            <div className="flex items-center justify-between">
              <span className="text-xs font-extrabold text-[#B7282E] dark:text-[#E2474D]">
                Deste {activeDeck} • Kelime {safeIndex + 1} / {filteredWords.length}
              </span>
              {progressMap[currentWord.id]?.isLearned && (
                <span className="rounded-lg bg-[#FDECEC] dark:bg-[#4E161A] px-2 py-0.5 text-[11px] font-bold text-[#8E1B20] dark:text-[#FFD9DA]">
                  ✓ Öğrenildi
                </span>
              )}
            </div>
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
              <div
                className="h-full rounded-full bg-[#B7282E] dark:bg-[#E2474D] transition-all duration-300"
                style={{ width: `${((safeIndex + 1) / Math.max(1, filteredWords.length)) * 100}%` }}
              />
            </div>
          </div>

          {/* 3. 3D Flip Card */}
          <div
            onClick={() => setIsFlipped(!isFlipped)}
            className="perspective-1000 cursor-pointer select-none py-1"
          >
            <div
              className={`relative min-h-[370px] w-full rounded-[28px] transition-transform duration-500 transform-style-3d ${
                isFlipped ? 'rotate-y-180' : ''
              }`}
            >
              {/* FRONT FACE */}
              <div className="absolute inset-0 backface-hidden rounded-[28px] bg-white dark:bg-[#1B1B22] p-6 shadow-md border-[1.5px] border-[#C89D42]/40 flex flex-col justify-between">
                <div className="flex items-center justify-between">
                  <span className="rounded-xl bg-[#FDECEC] dark:bg-[#4E161A] px-3 py-1.5 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA]">
                    #{currentWord.id} • {currentWord.wordType}
                  </span>
                  <span className="rounded-xl bg-[#C89D42]/15 px-3 py-1.5 text-xs font-semibold text-[#1A1A1E] dark:text-[#F2F2F7]">
                    Kur'an'da {currentWord.frequency} kez
                  </span>
                </div>

                <div className="my-auto text-center py-4">
                  <p
                    className="font-arabic text-6xl font-bold text-[#B7282E] dark:text-[#E2474D] leading-relaxed"
                    dir="rtl"
                  >
                    {currentWord.arabic}
                  </p>
                  <p className="mt-3 text-lg font-medium text-[#5A5A64] dark:text-[#B8B8C4]">
                    {currentWord.transliteration}
                  </p>

                  <div className="mt-5">
                    <button
                      type="button"
                      onClick={handlePlayWord}
                      className="inline-flex items-center gap-2 rounded-3xl bg-[#FDECEC] dark:bg-[#4E161A] px-5 py-2.5 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA] shadow-sm hover:opacity-90 transition"
                    >
                      <Volume2 className="h-4 w-4" />
                      <span>{playingType === 'word' ? 'Oynatılıyor...' : 'Kelimeyi Dinle'}</span>
                    </button>
                  </div>
                </div>

                <div className="text-center text-xs text-[#8A8A96] dark:text-[#7E7E8C]">
                  ⇄ Anlamını ve ayetini görmek için dokunun
                </div>
              </div>

              {/* BACK FACE */}
              <div className="absolute inset-0 backface-hidden rotate-y-180 rounded-[28px] bg-white dark:bg-[#1B1B22] p-6 shadow-md border-[1.5px] border-[#C89D42]/40 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between">
                    <span className="rounded-xl bg-[#DEC286] dark:bg-[#4F3E1E] px-3 py-1.5 text-xs font-bold text-[#2C2214] dark:text-[#F7E3B5]">
                      {currentWord.wordType}
                    </span>
                    <span
                      className="font-arabic text-3xl font-bold text-[#B7282E] dark:text-[#E2474D]"
                      dir="rtl"
                    >
                      {currentWord.arabic}
                    </span>
                  </div>

                  <h3 className="mt-4 text-center text-2xl font-extrabold text-[#B7282E] dark:text-[#E2474D]">
                    {currentWord.turkishMeaning}
                  </h3>

                  {/* Example Ayah Box */}
                  <div className="mt-4 rounded-[18px] bg-[#F0F0F4]/60 dark:bg-[#262630]/60 p-4 space-y-2 border border-[#E4E4EC] dark:border-[#32323E]">
                    <div className="flex items-center justify-between">
                      <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#5A5A64] dark:text-[#B8B8C4]">
                        <BookOpen className="h-4 w-4 text-[#C89D42]" />
                        Örnek Ayet ({currentWord.exampleAyahReference})
                      </span>
                      <button
                        type="button"
                        onClick={handlePlayAyah}
                        className="flex h-8 w-8 items-center justify-center rounded-full text-[#B7282E] dark:text-[#E2474D] hover:bg-[#B7282E]/10 transition"
                        title="Ayeti Dinle"
                      >
                        <Volume2 className="h-5 w-5" />
                      </button>
                    </div>
                    <p
                      className="font-arabic text-xl text-right text-[#1A1A1E] dark:text-[#F2F2F7] leading-relaxed"
                      dir="rtl"
                    >
                      {currentWord.exampleAyahArabic}
                    </p>
                    <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4] leading-relaxed">
                      "{currentWord.exampleAyahTurkish}"
                    </p>
                  </div>
                </div>

                <div className="text-center text-xs text-[#8A8A96] dark:text-[#7E7E8C]">
                  ⇄ Kelimeye dönmek için dokunun
                </div>
              </div>
            </div>
          </div>

          {/* 4. Middle Navigation Bar (Previous Card | Flip Card | Next Card) */}
          <div className="flex items-center justify-between py-1">
            <button
              type="button"
              disabled={safeIndex === 0}
              onClick={() => {
                setIsFlipped(false);
                setCurrentIndex(Math.max(0, safeIndex - 1));
              }}
              className="flex h-11 w-11 items-center justify-center rounded-full border border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22] text-[#1A1A1E] dark:text-[#F2F2F7] disabled:opacity-35 transition"
              aria-label="Önceki Kart"
            >
              <ArrowLeft className="h-5 w-5" />
            </button>

            <button
              type="button"
              onClick={() => setIsFlipped(!isFlipped)}
              className="flex h-11 items-center gap-2 rounded-full bg-[#F0F0F4] dark:bg-[#262630] border border-[#E4E4EC] dark:border-[#32323E] px-5 text-xs font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7] transition"
            >
              <span>⇄ {isFlipped ? 'Ön Yüzü Gör' : 'Anlamı Gör'}</span>
            </button>

            <button
              type="button"
              disabled={safeIndex >= filteredWords.length - 1}
              onClick={() => {
                setIsFlipped(false);
                setCurrentIndex(Math.min(filteredWords.length - 1, safeIndex + 1));
              }}
              className="flex h-11 w-11 items-center justify-center rounded-full border border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22] text-[#1A1A1E] dark:text-[#F2F2F7] disabled:opacity-35 transition"
              aria-label="Sonraki Kart"
            >
              <ArrowRight className="h-5 w-5" />
            </button>
          </div>

          {/* 5. Bottom Review Buttons ("Tekrar Et" in Champagne Gold & "Öğrendim" in Ruby Crimson) */}
          <div className="grid grid-cols-2 gap-3.5 pt-1">
            <button
              type="button"
              onClick={() => handleAction(false)}
              className="flex h-[54px] items-center justify-center gap-2 rounded-2xl bg-[#DEC286] dark:bg-[#4F3E1E] text-[#2C2214] dark:text-[#F7E3B5] border border-[#C89D42]/50 font-bold text-sm shadow-sm transition active:scale-[0.99]"
            >
              <RotateCcw className="h-5 w-5" />
              <span>Tekrar Et</span>
            </button>

            <button
              type="button"
              onClick={() => handleAction(true)}
              className="flex h-[54px] items-center justify-center gap-2 rounded-2xl bg-[#B7282E] dark:bg-[#E2474D] text-white font-bold text-sm shadow-sm transition active:scale-[0.99]"
            >
              <Check className="h-5 w-5" />
              <span>Öğrendim</span>
            </button>
          </div>
        </>
      )}

      {/* 6. Son 7 Günlük Öğrenilen Kelimeler Bar Grafiği (Kartlar sayfasının en altında) */}
      <div className="mt-4 rounded-[20px] bg-white dark:bg-[#1B1B22] p-4 shadow-sm border border-[#E4E4EC] dark:border-[#32323E]">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div>
            <h3 className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
              Son 7 Günlük Öğrenilen Kelimeler
            </h3>
            <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">
              Günlük öğrenme performansı
            </p>
          </div>
          <span className="rounded-xl bg-[#FDECEC] dark:bg-[#4E161A] px-2.5 py-1 text-xs font-bold tabular-nums text-[#8E1B20] dark:text-[#FFD9DA]">
            Toplam: {last7DaysTotal}
          </span>
        </div>

        <div className="h-44 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={last7DaysData}
              margin={{ top: 10, right: 8, left: -24, bottom: 0 }}
            >
              <CartesianGrid
                strokeDasharray="3 3"
                vertical={false}
                stroke={isDark ? '#262630' : '#F0F0F4'}
              />
              <XAxis
                dataKey="dayName"
                axisLine={false}
                tickLine={false}
                tick={{
                  fill: isDark ? '#B8B8C4' : '#5A5A64',
                  fontSize: 12,
                  fontWeight: 600
                }}
              />
              <YAxis
                allowDecimals={false}
                axisLine={false}
                tickLine={false}
                tick={{
                  fill: isDark ? '#7E7E8C' : '#8A8A96',
                  fontSize: 11
                }}
              />
              <Tooltip
                cursor={{
                  fill: isDark ? 'rgba(226, 71, 77, 0.08)' : 'rgba(183, 40, 46, 0.06)',
                  radius: 8
                }}
                content={({ active, payload }) => {
                  if (active && payload && payload.length > 0) {
                    const item = payload[0].payload;
                    return (
                      <div className="rounded-xl bg-[#1B1B22] text-[#F2F2F7] px-3 py-2 shadow-lg border border-[#C89D42]/40 text-xs">
                        <p className="font-bold text-[#DEC286]">
                          {item.fullDateLabel} ({item.dayName})
                        </p>
                        <p className="mt-0.5 font-semibold text-[#E2474D]">
                          {item.count} Kelime Öğrenildi
                        </p>
                      </div>
                    );
                  }
                  return null;
                }}
              />
              <Bar dataKey="count" radius={[6, 6, 2, 2]} maxBarSize={32}>
                {last7DaysData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={
                      entry.isToday
                        ? '#C89D42'
                        : isDark
                        ? '#E2474D'
                        : '#B7282E'
                    }
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};
