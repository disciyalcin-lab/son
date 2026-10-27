import React, { useState, useEffect } from 'react';
import {
  loadWordProgressMap,
  markWordResult,
  loadUserStats,
  saveUserStats,
  resetAllStoredProgress,
  toLocalDateKey,
  triggerSwNotification,
  WordProgress,
  UserStats,
  ThemeMode
} from './utils/storage';
import { QURAN_WORDS_300 } from './data/wordsData';
import { Dashboard } from './components/Dashboard';
import { Flashcards } from './components/Flashcards';
import { Quiz } from './components/Quiz';
import { Dictionary } from './components/Dictionary';
import { Stats } from './components/Stats';
import { Home, BookMarked, HelpCircle, Sparkles, BarChart3 } from 'lucide-react';

type TabKey = 'DASHBOARD' | 'FLASHCARDS' | 'QUIZ' | 'DICTIONARY' | 'STATS';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabKey>('DASHBOARD');
  const [selectedDeckId, setSelectedDeckId] = useState<number>(1);
  const [progressMap, setProgressMap] = useState<Record<number, WordProgress>>(() =>
    loadWordProgressMap()
  );
  const [stats, setStats] = useState<UserStats>(() => loadUserStats());
  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(() =>
    typeof window !== 'undefined' && window.matchMedia
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
      : false
  );

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = (e: MediaQueryListEvent) => setSystemPrefersDark(e.matches);
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);

  const themeMode: ThemeMode = stats.themeMode || (stats.darkMode ? 'DARK' : 'LIGHT');
  const isDark =
    themeMode === 'DARK' ? true : themeMode === 'LIGHT' ? false : systemPrefersDark;

  useEffect(() => {
    const root = document.documentElement;
    const metaTheme = document.querySelector('meta[name="theme-color"]');
    if (isDark) {
      root.classList.add('dark');
      if (metaTheme) metaTheme.setAttribute('content', '#111115');
    } else {
      root.classList.remove('dark');
      if (metaTheme) metaTheme.setAttribute('content', '#B7282E');
    }
  }, [isDark]);

  // Lightweight Daily Reminder Scheduler (checks every 30s when app is open)
  useEffect(() => {
    if (!stats.reminderEnabled || !stats.reminderTime) return;

    const checkReminder = () => {
      const now = new Date();
      const todayKey = toLocalDateKey(now.getTime());
      if (stats.lastNotifiedDate === todayKey) return;

      const currentHHMM = `${String(now.getHours()).padStart(2, '0')}:${String(
        now.getMinutes()
      ).padStart(2, '0')}`;

      if (currentHHMM >= stats.reminderTime!) {
        const unlearned = QURAN_WORDS_300.filter((w) => !progressMap[w.id]?.isLearned);
        const word = unlearned[0] || QURAN_WORDS_300[0];
        triggerSwNotification(
          `Kur'an 300 Kelime Hatırlatması${stats.userName ? ` • ${stats.userName}` : ''}`,
          `Günün kelimesi: ${word.arabic} (${word.transliteration}) — ${word.turkishMeaning}`
        );
        const updated: UserStats = {
          ...stats,
          lastNotifiedDate: todayKey
        };
        setStats(updated);
        saveUserStats(updated);
      }
    };

    checkReminder();
    const timer = setInterval(checkReminder, 30000);
    return () => clearInterval(timer);
  }, [stats, progressMap]);

  const handleUpdateStats = (updated: UserStats) => {
    setStats(updated);
    saveUserStats(updated);
  };

  const handleToggleTheme = () => {
    const nextDark = !isDark;
    const updated: UserStats = {
      ...stats,
      darkMode: nextDark,
      themeMode: nextDark ? 'DARK' : 'LIGHT'
    };
    setStats(updated);
    saveUserStats(updated);
  };

  const handleSetThemeMode = (mode: ThemeMode) => {
    const nextDark =
      mode === 'DARK' ? true : mode === 'LIGHT' ? false : systemPrefersDark;
    const updated: UserStats = {
      ...stats,
      darkMode: nextDark,
      themeMode: mode
    };
    setStats(updated);
    saveUserStats(updated);
  };

  const handleToggleAutoPlay = () => {
    const updated: UserStats = {
      ...stats,
      autoPlayAudio: !stats.autoPlayAudio
    };
    setStats(updated);
    saveUserStats(updated);
  };

  const handleMarkWord = (wordId: number, knewIt: boolean) => {
    const nextMap = markWordResult(progressMap, wordId, knewIt);
    setProgressMap(nextMap);
    setStats(loadUserStats());
  };

  const handleCompleteQuiz = (correct: number, total: number) => {
    const updated: UserStats = {
      ...stats,
      totalQuizzesTaken: stats.totalQuizzesTaken + 1,
      totalQuizCorrect: stats.totalQuizCorrect + correct,
      totalQuizQuestions: stats.totalQuizQuestions + total
    };
    setStats(updated);
    saveUserStats(updated);
  };

  const handleResetAllProgress = () => {
    const cleared = resetAllStoredProgress();
    setProgressMap(cleared);
  };

  return (
    <div className="min-h-screen bg-[#F7F7F9] dark:bg-[#111115] text-[#1A1A1E] dark:text-[#F2F2F7] flex flex-col">
      {/* Main Mobile-First Container */}
      <main className="mx-auto w-full max-w-xl flex-1 px-4 pt-3 pb-24">
        {activeTab === 'DASHBOARD' && (
          <Dashboard
            progressMap={progressMap}
            stats={stats}
            isDark={isDark}
            onToggleTheme={handleToggleTheme}
            onUpdateStats={handleUpdateStats}
            onSelectDeck={(deckId) => {
              setSelectedDeckId(deckId);
              setActiveTab('FLASHCARDS');
            }}
            onNavigateToQuiz={() => setActiveTab('QUIZ')}
          />
        )}

        {activeTab === 'FLASHCARDS' && (
          <Flashcards
            selectedDeckId={selectedDeckId}
            progressMap={progressMap}
            isDark={isDark}
            autoPlayAudio={!!stats.autoPlayAudio}
            onToggleTheme={handleToggleTheme}
            onToggleAutoPlay={handleToggleAutoPlay}
            onSelectDeck={setSelectedDeckId}
            onMarkWord={handleMarkWord}
          />
        )}

        {activeTab === 'QUIZ' && (
          <Quiz
            stats={stats}
            onCompleteQuiz={handleCompleteQuiz}
            onMarkWord={handleMarkWord}
          />
        )}

        {activeTab === 'DICTIONARY' && (
          <Dictionary
            progressMap={progressMap}
            isDark={isDark}
            onToggleTheme={handleToggleTheme}
            onToggleLearned={handleMarkWord}
          />
        )}

        {activeTab === 'STATS' && (
          <Stats
            progressMap={progressMap}
            themeMode={themeMode}
            onSetThemeMode={handleSetThemeMode}
            onResetAllProgress={handleResetAllProgress}
          />
        )}
      </main>

      {/* 5-Tab Bottom Navigation Bar (matching Screen.kt & MainActivity.kt) */}
      <nav className="fixed bottom-0 left-0 right-0 z-30 border-t border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22]">
        <div className="mx-auto grid max-w-xl grid-cols-5 px-2 py-2">
          {(
            [
              { key: 'DASHBOARD', label: 'Ana Sayfa', icon: Home },
              { key: 'FLASHCARDS', label: 'Kartlar', icon: BookMarked },
              { key: 'QUIZ', label: 'Quiz', icon: HelpCircle },
              { key: 'DICTIONARY', label: 'Sözlük', icon: Sparkles },
              { key: 'STATS', label: 'Başarı', icon: BarChart3 }
            ] as const
          ).map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.key;
            return (
              <button
                key={item.key}
                onClick={() => setActiveTab(item.key)}
                className="flex flex-col items-center justify-center py-1 transition"
              >
                <div
                  className={`flex h-8 w-14 items-center justify-center rounded-full transition ${
                    active
                      ? 'bg-[#FDECEC] dark:bg-[#4E161A] text-[#B7282E] dark:text-[#E2474D]'
                      : 'text-[#5A5A64] dark:text-[#B8B8C4]'
                  }`}
                >
                  <Icon className="h-5 w-5" />
                </div>
                <span
                  className={`mt-1 text-[11px] whitespace-nowrap ${
                    active
                      ? 'font-bold text-[#B7282E] dark:text-[#E2474D]'
                      : 'font-normal text-[#5A5A64] dark:text-[#B8B8C4]'
                  }`}
                >
                  {item.label}
                </span>
              </button>
            );
          })}
        </div>
      </nav>
    </div>
  );
};

export default App;
