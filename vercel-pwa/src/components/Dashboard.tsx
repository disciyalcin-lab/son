import React, { useState } from 'react';
import { QURAN_WORDS_300, TOTAL_QURAN_WORDS } from '../data/wordsData';
import {
  WordProgress,
  UserStats,
  getLast7DaysLearnedData,
  getDeck12Number,
  requestAppNotificationPermission,
  triggerSwNotification
} from '../utils/storage';
import {
  Sparkles,
  Volume2,
  Flame,
  Play,
  HelpCircle,
  CheckCircle2,
  Sun,
  Moon,
  BellRing,
  User,
  Download
} from 'lucide-react';
import { audioPlayer } from '../utils/audioPlayer';
import { AppLogo } from './AppLogo';
import { ProfileNotificationModal } from './ProfileNotificationModal';
import { usePWAInstall } from '../utils/usePWAInstall';

interface DashboardProps {
  progressMap: Record<number, WordProgress>;
  stats: UserStats;
  isDark: boolean;
  onToggleTheme: () => void;
  onUpdateStats: (updated: UserStats) => void;
  onSelectDeck: (deckId: number) => void;
  onNavigateToQuiz: () => void;
}

export const Dashboard: React.FC<DashboardProps> = ({
  progressMap,
  stats,
  isDark,
  onToggleTheme,
  onUpdateStats,
  onSelectDeck,
  onNavigateToQuiz
}) => {
  const [srsToast, setSrsToast] = useState<string | null>(null);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const { isInstallable, isInstalled, isIOS, install } = usePWAInstall();

  // Strictly sorted by frequency DESC, id ASC (1..300)
  const sortedWords = React.useMemo(
    () => [...QURAN_WORDS_300].sort((a, b) => b.frequency - a.frequency || a.id - b.id),
    []
  );

  const learnedWords = sortedWords.filter((w) => progressMap[w.id]?.isLearned);
  const totalLearnedCount = learnedWords.length;
  const frequencyLearned = learnedWords.reduce((sum, w) => sum + w.frequency, 0);

  const coveragePercentage = Math.min(100, (frequencyLearned / TOTAL_QURAN_WORDS) * 100);

  const { currentMilestone, nextMilestoneRemaining } = React.useMemo(() => {
    if (totalLearnedCount < 50) {
      return { currentMilestone: 'Temel Aşina (%40)', nextMilestoneRemaining: 50 - totalLearnedCount };
    }
    if (totalLearnedCount < 100) {
      return { currentMilestone: 'Sayfa Okuyucu (%55)', nextMilestoneRemaining: 100 - totalLearnedCount };
    }
    if (totalLearnedCount < 200) {
      return { currentMilestone: 'Anlam Kaşifi (%68)', nextMilestoneRemaining: 200 - totalLearnedCount };
    }
    if (totalLearnedCount < 300) {
      return { currentMilestone: 'Ayet Kavrayıcı (%75)', nextMilestoneRemaining: 300 - totalLearnedCount };
    }
    return { currentMilestone: "Kur'an Kelime Üstadı!", nextMilestoneRemaining: 0 };
  }, [totalLearnedCount]);

  const todayLearnedCount = React.useMemo(() => {
    const data = getLast7DaysLearnedData(progressMap);
    return data.find((d) => d.isToday)?.count || 0;
  }, [progressMap]);

  // Daily featured word (weakest unlearned or word of the day)
  const dailyWord = React.useMemo(() => {
    const unlearned = sortedWords.filter((w) => !progressMap[w.id]?.isLearned);
    if (unlearned.length > 0) {
      const dayOfYear = Math.floor(Date.now() / 86400000);
      return unlearned[dayOfYear % unlearned.length];
    }
    return sortedWords[0];
  }, [sortedWords, progressMap]);

  // 12 Decks (25 words each: 1-25, 26-50, ..., 276-300)
  const decks = React.useMemo(() => {
    return Array.from({ length: 12 }, (_, idx) => {
      const deckNumber = idx + 1;
      const startId = (deckNumber - 1) * 25 + 1;
      const endId = Math.min(deckNumber * 25, 300);
      const deckWords = sortedWords.filter((w) => getDeck12Number(w.id) === deckNumber);
      const learnedInDeck = deckWords.filter((w) => progressMap[w.id]?.isLearned).length;
      return {
        deckNumber,
        title: `Deste ${deckNumber}`,
        rangeText: `${startId} - ${endId}. Kelimeler`,
        totalWords: deckWords.length || 25,
        learnedWords: learnedInDeck,
        isCompleted: deckWords.length > 0 && learnedInDeck >= deckWords.length
      };
    });
  }, [sortedWords, progressMap]);

  // Circular Progress Ring geometry
  const radius = 32;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (coveragePercentage / 100) * circumference;

  const handleTestSrsNotification = async () => {
    if (!dailyWord) return;
    audioPlayer.playWord(dailyWord.arabic, dailyWord.audioUrl);
    const perm = await requestAppNotificationPermission();
    if (perm === 'granted') {
      await triggerSwNotification(
        `Günün Kur'an Kelimesi: ${dailyWord.arabic}`,
        `${dailyWord.transliteration} — ${dailyWord.turkishMeaning} (Kur'an'da ${dailyWord.frequency} kez)`
      );
    }
    setSrsToast(
      `SRS Hatırlatma: ${dailyWord.arabic} (${dailyWord.transliteration}) — ${dailyWord.turkishMeaning}`
    );
    setTimeout(() => setSrsToast(null), 4000);
  };

  return (
    <div className="space-y-4 pb-8">
      {/* 1. Organized Top Bar & Centered Bismillah Banner */}
      <header className="rounded-[22px] bg-white dark:bg-[#1B1B22] border border-[#E4E4EC] dark:border-[#32323E] p-3.5 shadow-sm space-y-3">
        <div className="flex items-center justify-between gap-2">
          {/* Left: Logo + App Title & Personal Greeting */}
          <div className="flex items-center gap-2.5 min-w-0">
            <AppLogo size={42} />
            <div className="min-w-0">
              <h1 className="text-lg font-extrabold tracking-tight text-[#1A1A1E] dark:text-[#F2F2F7] truncate leading-tight">
                Kur'an 300 Kelime
              </h1>
              <p className="text-xs font-semibold text-[#B7282E] dark:text-[#E2474D] truncate">
                {stats.userName
                  ? `Selâmün Aleyküm, ${stats.userName}`
                  : "300 Kelime ile %75 Kur'an Kapsamı"}
              </p>
            </div>
          </div>

          {/* Right: Streak Badge + Profile/Notification Button + Theme Toggle */}
          <div className="flex items-center gap-1.5 shrink-0">
            {/* Daily Streak Badge */}
            <div
              className="flex h-10 items-center gap-1 rounded-xl bg-[#DEC286] dark:bg-[#4F3E1E] px-2.5 text-[#2C2214] dark:text-[#F7E3B5]"
              title="Günlük Çalışma Serisi"
            >
              <Flame className="h-4 w-4 text-[#B7282E] dark:text-[#E2474D] fill-current shrink-0" />
              <span className="text-xs font-extrabold tabular-nums whitespace-nowrap">
                {stats.streakDays} Gün
              </span>
            </div>

            {/* Personal Profile & Reminder Trigger */}
            <button
              type="button"
              onClick={() => setIsProfileModalOpen(true)}
              className={`flex h-10 w-10 items-center justify-center rounded-xl border transition active:scale-95 ${
                stats.userName || stats.reminderEnabled
                  ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/30 dark:border-[#E2474D]/35 text-[#B7282E] dark:text-[#E2474D]'
                  : 'bg-[#F0F0F4] dark:bg-[#262630] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
              }`}
              aria-label="Kişisel Profil ve Bildirim Ayarları"
              title="Kişisel Profil ve Bildirimler"
            >
              <User className="h-4 w-4" />
            </button>

            {/* Quick Day / Night Toggle Button */}
            <button
              type="button"
              onClick={onToggleTheme}
              className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#F0F0F4] dark:bg-[#262630] border border-[#E4E4EC] dark:border-[#32323E] transition active:scale-95"
              aria-label={isDark ? 'Gündüz Moduna Geç' : 'Gece Moduna Geç'}
              title={isDark ? 'Gündüz Modu' : 'Gece Modu'}
            >
              {isDark ? (
                <Sun className="h-4 w-4 text-[#C89D42]" />
              ) : (
                <Moon className="h-4 w-4 text-[#B7282E]" />
              )}
            </button>
          </div>
        </div>

        {/* Dedicated Centered Bismillah Frame */}
        <div className="flex items-center justify-between rounded-xl bg-[#FDECEC]/60 dark:bg-[#4E161A]/40 border border-[#C89D42]/30 px-3 py-2">
          <span className="text-xs text-[#C89D42] select-none">۞</span>
          <p
            className="font-arabic text-xl sm:text-2xl font-bold text-[#B7282E] dark:text-[#E2474D] text-center leading-relaxed tracking-wide"
            dir="rtl"
          >
            بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ
          </p>
          <span className="text-xs text-[#C89D42] select-none">۞</span>
        </div>

        {/* Quick Install / Personal Reminder Action Strip */}
        <div className="flex items-center justify-between gap-2 pt-0.5 text-xs">
          <button
            type="button"
            onClick={() => setIsProfileModalOpen(true)}
            className="inline-flex items-center gap-1.5 font-bold text-[#B7282E] dark:text-[#E2474D] hover:underline"
          >
            <BellRing className="h-3.5 w-3.5" />
            <span>
              {stats.reminderEnabled
                ? `Hatırlatıcı Aktif (${stats.reminderTime || '20:00'})`
                : 'Kişisel Bildirim & Hedef Ayarla'}
            </span>
          </button>

          {!isInstalled && (
            <button
              type="button"
              onClick={() => {
                if (isInstallable) {
                  install();
                } else {
                  setIsProfileModalOpen(true);
                }
              }}
              className="inline-flex items-center gap-1.5 rounded-lg bg-[#B7282E] dark:bg-[#E2474D] px-2.5 py-1 text-[11px] font-bold text-white shadow-2xs transition hover:opacity-90"
            >
              <Download className="h-3 w-3" />
              <span>Uygulamayı Yükle</span>
            </button>
          )}
        </div>
      </header>

      {/* SRS Toast Alert if triggered */}
      {srsToast && (
        <div className="rounded-2xl bg-[#FDECEC] dark:bg-[#4E161A] border border-[#B7282E]/30 dark:border-[#E2474D]/40 px-4 py-3 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA] flex items-center gap-2 shadow-sm">
          <BellRing className="h-4 w-4 shrink-0 text-[#B7282E] dark:text-[#E2474D]" />
          <span>{srsToast}</span>
        </div>
      )}

      {/* 2. Big Quran Coverage Card (Milestone Engine with Circular Progress Ring) */}
      <div className="relative overflow-hidden rounded-[24px] bg-white dark:bg-[#1B1B22] shadow-md border border-[#E4E4EC] dark:border-[#32323E]">
        <div className="bg-gradient-to-b from-[#B7282E]/[0.08] dark:from-[#E2474D]/[0.12] to-transparent p-5">
          <div className="flex items-center justify-between gap-4">
            <div>
              <span className="inline-block rounded-lg bg-[#FDECEC] dark:bg-[#4E161A] px-2.5 py-1 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA]">
                {currentMilestone}
              </span>
              <h2 className="mt-2 text-lg font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                Kur'an'ı Anlama Kapsamı
              </h2>
              <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C] mt-0.5">
                Bugün: {todayLearnedCount} / {stats.dailyGoalWords || 10} kelime hedefi
              </p>
            </div>

            {/* Circular Progress Indicator */}
            <div className="relative flex h-20 w-20 items-center justify-center shrink-0">
              <svg className="h-20 w-20 -rotate-90 transform" viewBox="0 0 80 80">
                <circle
                  cx="40"
                  cy="40"
                  r={radius}
                  stroke={isDark ? '#262630' : '#F0F0F4'}
                  strokeWidth="8"
                  fill="transparent"
                />
                <circle
                  cx="40"
                  cy="40"
                  r={radius}
                  stroke={isDark ? '#E2474D' : '#B7282E'}
                  strokeWidth="8"
                  strokeDasharray={circumference}
                  strokeDashoffset={strokeDashoffset}
                  strokeLinecap="round"
                  fill="transparent"
                  className="transition-all duration-700 ease-out"
                />
              </svg>
              <span className="absolute text-sm font-extrabold tabular-nums text-[#B7282E] dark:text-[#E2474D]">
                %{coveragePercentage.toFixed(1)}
              </span>
            </div>
          </div>

          {/* Stats Summary Row */}
          <div className="mt-4 flex items-center justify-between">
            <div>
              <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">Öğrenilen Kelime</p>
              <p className="text-base font-extrabold tabular-nums text-[#1A1A1E] dark:text-[#F2F2F7]">
                {totalLearnedCount} / 300
              </p>
            </div>
            <div className="text-right">
              <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">Sonraki Aşamaya</p>
              <p className="text-base font-extrabold text-[#C89D42]">
                {nextMilestoneRemaining > 0 ? `${nextMilestoneRemaining} kelime kaldı` : 'Tamamlandı!'}
              </p>
            </div>
          </div>

          {/* Milestone Linear Progress Bar */}
          <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
            <div
              className="h-full rounded-full bg-[#B7282E] dark:bg-[#E2474D] transition-all duration-500"
              style={{ width: `${Math.min(100, (totalLearnedCount / 300) * 100)}%` }}
            />
          </div>
        </div>
      </div>

      {/* 3. Quick Action Buttons ("Kelime Kartları" & "Quiz Çöz") */}
      <div className="grid grid-cols-2 gap-3">
        <button
          onClick={() => onSelectDeck(1)}
          className="flex h-14 items-center justify-center gap-2 rounded-2xl bg-[#B7282E] dark:bg-[#E2474D] text-white font-bold text-sm shadow-sm hover:opacity-95 transition active:scale-[0.99]"
        >
          <Play className="h-4 w-4 fill-current" />
          <span>Kelime Kartları</span>
        </button>

        <button
          onClick={onNavigateToQuiz}
          className="flex h-14 items-center justify-center gap-2 rounded-2xl bg-[#DEC286] dark:bg-[#4F3E1E] text-[#2C2214] dark:text-[#F7E3B5] font-bold text-sm shadow-sm hover:opacity-95 transition active:scale-[0.99]"
        >
          <HelpCircle className="h-4 w-4" />
          <span>Quiz Çöz</span>
        </button>
      </div>

      {/* 4. Günün Kelimesi (Daily Featured Word Card) */}
      {dailyWord && (
        <div className="rounded-[20px] bg-[#F0F0F4]/70 dark:bg-[#262630]/70 p-4 border border-[#E4E4EC] dark:border-[#32323E]">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1.5">
              <Sparkles className="h-5 w-5 text-[#C89D42]" />
              <span className="text-xs font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                Günün Kelimesi (#{dailyWord.id})
              </span>
            </div>

            <button
              onClick={() => audioPlayer.playWord(dailyWord.arabic, dailyWord.audioUrl)}
              className="flex h-9 w-9 items-center justify-center rounded-full bg-[#B7282E] dark:bg-[#E2474D] text-white shadow-sm hover:opacity-90 transition"
              aria-label="Kelimeyi Dinle"
            >
              <Volume2 className="h-4 w-4" />
            </button>
          </div>

          <div className="mt-2.5 flex items-center justify-between gap-3">
            <div className="min-w-0 flex-1">
              <p className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                {dailyWord.turkishMeaning}
              </p>
              <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C] mt-0.5">
                {dailyWord.transliteration} · {dailyWord.wordType}
              </p>
              <p className="text-xs font-semibold text-[#B7282E] dark:text-[#E2474D] mt-0.5">
                Kur'an'da {dailyWord.frequency} kez geçiyor
              </p>
            </div>

            <p
              className="font-arabic text-4xl font-bold text-[#B7282E] dark:text-[#E2474D] pl-3"
              dir="rtl"
            >
              {dailyWord.arabic}
            </p>
          </div>

          <button
            onClick={handleTestSrsNotification}
            className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl border border-[#8A8A96]/40 dark:border-[#7E7E8C]/40 py-2.5 text-xs font-bold text-[#B7282E] dark:text-[#E2474D] hover:bg-[#B7282E]/5 transition"
          >
            <Sparkles className="h-4 w-4" />
            <span>Hatırlatma Bildirimini Test Et (SRS)</span>
          </button>
        </div>
      )}

      {/* 5. Öğrenme Desteleri Bölümü (12 Deste - 25'er Kelime) */}
      <div className="space-y-3 pt-1">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
            Öğrenme Desteleri (12 Deste)
          </h3>
          <span className="text-xs font-semibold text-[#8A8A96] dark:text-[#7E7E8C]">
            25'er Kelime
          </span>
        </div>

        {/* Horizontal Quick Suggestion Chips */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar">
          {decks.map((d) => (
            <button
              key={`chip-${d.deckNumber}`}
              onClick={() => onSelectDeck(d.deckNumber)}
              className="shrink-0 rounded-xl border border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22] px-3 py-1.5 text-xs font-semibold tabular-nums text-[#1A1A1E] dark:text-[#F2F2F7] hover:border-[#B7282E] dark:hover:border-[#E2474D] transition"
            >
              Deste {d.deckNumber} ({d.learnedWords}/{d.totalWords})
            </button>
          ))}
        </div>

        {/* 12 Deck Item Cards */}
        <div className="space-y-2.5">
          {decks.map((deck) => {
            const pct = deck.totalWords > 0 ? Math.round((deck.learnedWords / deck.totalWords) * 100) : 0;
            return (
              <button
                key={deck.deckNumber}
                onClick={() => onSelectDeck(deck.deckNumber)}
                className={`w-full rounded-2xl p-4 text-left border transition ${
                  deck.isCompleted
                    ? 'bg-[#FDECEC]/40 dark:bg-[#4E161A]/30 border-[#2E7D32]/50'
                    : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] hover:border-[#B7282E] dark:hover:border-[#E2474D]'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div
                      className={`flex h-9 w-9 items-center justify-center rounded-full text-xs font-extrabold tabular-nums ${
                        deck.isCompleted
                          ? 'bg-[#2E7D32] text-white'
                          : 'bg-[#FDECEC] dark:bg-[#4E161A] text-[#8E1B20] dark:text-[#FFD9DA]'
                      }`}
                    >
                      {deck.isCompleted ? (
                        <CheckCircle2 className="h-5 w-5" />
                      ) : (
                        deck.deckNumber
                      )}
                    </div>
                    <div>
                      <h4 className="text-sm font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                        {deck.title}
                      </h4>
                      <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">
                        {deck.rangeText}
                      </p>
                    </div>
                  </div>

                  <span
                    className={`text-xs font-extrabold tabular-nums ${
                      deck.isCompleted
                        ? 'text-[#2E7D32]'
                        : 'text-[#B7282E] dark:text-[#E2474D]'
                    }`}
                  >
                    {deck.learnedWords} / {deck.totalWords}
                  </span>
                </div>

                <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      deck.isCompleted
                        ? 'bg-[#2E7D32]'
                        : 'bg-[#B7282E] dark:bg-[#E2474D]'
                    }`}
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Personal Profile & Smart Notification Modal */}
      <ProfileNotificationModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
        stats={stats}
        onUpdateStats={onUpdateStats}
        isInstallable={isInstallable}
        isInstalled={isInstalled}
        isIOS={isIOS}
        onInstallPWA={install}
        sampleWord={
          dailyWord || {
            arabic: 'اللَّه',
            transliteration: 'Allah',
            turkishMeaning: 'Allah (Celle Celâluhû)',
            frequency: 2699
          }
        }
      />
    </div>
  );
};
