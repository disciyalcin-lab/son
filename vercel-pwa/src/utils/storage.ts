export interface WordProgress {
  wordId: number;
  isLearned: boolean;
  srsLevel: number;
  nextReviewTime: number;
  correctCount: number;
  wrongCount: number;
  lastStudiedAt: number;
  learnedAt?: number;
}

export interface DailyLearnedStat {
  dateKey: string;       // YYYY-MM-DD
  dayName: string;       // Pzt, Sal, Çar...
  fullDateLabel: string; // 26 Eki
  count: number;
  isToday: boolean;
}

export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';

export interface UserStats {
  streakDays: number;
  lastStudyDate: string; // YYYY-MM-DD
  totalQuizzesTaken: number;
  totalQuizCorrect: number;
  totalQuizQuestions: number;
  darkMode: boolean;
  themeMode?: ThemeMode;
  autoPlayAudio?: boolean;
  userName?: string;
  dailyGoalWords?: number;
  reminderEnabled?: boolean;
  reminderTime?: string; // HH:MM
  lastNotifiedDate?: string; // YYYY-MM-DD
}

const PROGRESS_KEY = 'kuran300_word_progress_v1';
const STATS_KEY = 'kuran300_user_stats_v1';

// SRS intervals in milliseconds: [1 day, 3 days, 7 days, 14 days, 30 days]
const SRS_INTERVALS_MS = [
  1 * 24 * 60 * 60 * 1000,
  3 * 24 * 60 * 60 * 1000,
  7 * 24 * 60 * 60 * 1000,
  14 * 24 * 60 * 60 * 1000,
  30 * 24 * 60 * 60 * 1000
];

export function toLocalDateKey(timestampMs: number): string {
  const d = new Date(timestampMs);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function getDeck12Number(wordId: number): number {
  return Math.min(12, Math.max(1, Math.ceil(wordId / 25)));
}

export function loadWordProgressMap(): Record<number, WordProgress> {
  try {
    const raw = localStorage.getItem(PROGRESS_KEY);
    if (!raw) return {};
    return JSON.parse(raw);
  } catch {
    return {};
  }
}

export function saveWordProgressMap(map: Record<number, WordProgress>): void {
  try {
    localStorage.setItem(PROGRESS_KEY, JSON.stringify(map));
  } catch {
    // ignore storage quota errors
  }
}

export function markWordResult(
  currentMap: Record<number, WordProgress>,
  wordId: number,
  knewIt: boolean
): Record<number, WordProgress> {
  const existing = currentMap[wordId] || {
    wordId,
    isLearned: false,
    srsLevel: 0,
    nextReviewTime: 0,
    correctCount: 0,
    wrongCount: 0,
    lastStudiedAt: 0
  };

  const now = Date.now();
  let newLevel = existing.srsLevel;
  let nextReviewTime = now;

  if (knewIt) {
    newLevel = Math.min(existing.srsLevel + 1, 5);
    const interval = SRS_INTERVALS_MS[Math.max(0, newLevel - 1)] || SRS_INTERVALS_MS[0];
    nextReviewTime = now + interval;
  } else {
    newLevel = 0;
    nextReviewTime = now + 10 * 60 * 1000; // Review again in 10 minutes
  }

  const updated: WordProgress = {
    wordId,
    isLearned: knewIt,
    srsLevel: newLevel,
    nextReviewTime,
    correctCount: existing.correctCount + (knewIt ? 1 : 0),
    wrongCount: existing.wrongCount + (knewIt ? 0 : 1),
    lastStudiedAt: now,
    learnedAt: knewIt ? (existing.learnedAt || now) : undefined
  };

  const nextMap = { ...currentMap, [wordId]: updated };
  saveWordProgressMap(nextMap);
  touchStudyStreak();
  return nextMap;
}

export function resetAllStoredProgress(): Record<number, WordProgress> {
  try {
    localStorage.removeItem(PROGRESS_KEY);
  } catch {
    // ignore
  }
  return {};
}

export function getLast7DaysLearnedData(
  progressMap: Record<number, WordProgress>
): DailyLearnedStat[] {
  const TR_DAYS = ['Paz', 'Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt'];
  const TR_MONTHS = ['Oca', 'Şub', 'Mar', 'Nis', 'May', 'Haz', 'Tem', 'Ağu', 'Eyl', 'Eki', 'Kas', 'Ara'];

  const countsByDate: Record<string, number> = {};
  Object.values(progressMap).forEach((p) => {
    if (p.isLearned) {
      const ts = p.learnedAt || p.lastStudiedAt;
      if (ts && ts > 0) {
        const key = toLocalDateKey(ts);
        countsByDate[key] = (countsByDate[key] || 0) + 1;
      }
    }
  });

  const result: DailyLearnedStat[] = [];
  const now = new Date();
  const todayKey = toLocalDateKey(now.getTime());

  for (let i = 6; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth(), now.getDate() - i);
    const key = toLocalDateKey(d.getTime());
    const dayName = TR_DAYS[d.getDay()];
    const fullDateLabel = `${d.getDate()} ${TR_MONTHS[d.getMonth()]}`;
    result.push({
      dateKey: key,
      dayName,
      fullDateLabel,
      count: countsByDate[key] || 0,
      isToday: key === todayKey
    });
  }

  return result;
}

export function loadUserStats(): UserStats {
  try {
    const raw = localStorage.getItem(STATS_KEY);
    if (!raw) {
      return {
        streakDays: 1,
        lastStudyDate: toLocalDateKey(Date.now()),
        totalQuizzesTaken: 0,
        totalQuizCorrect: 0,
        totalQuizQuestions: 0,
        darkMode: false,
        themeMode: 'LIGHT',
        autoPlayAudio: false,
        userName: '',
        dailyGoalWords: 10,
        reminderEnabled: false,
        reminderTime: '20:00'
      };
    }
    const parsed = JSON.parse(raw);
    return {
      streakDays: parsed.streakDays ?? 1,
      lastStudyDate: parsed.lastStudyDate ?? toLocalDateKey(Date.now()),
      totalQuizzesTaken: parsed.totalQuizzesTaken ?? 0,
      totalQuizCorrect: parsed.totalQuizCorrect ?? 0,
      totalQuizQuestions: parsed.totalQuizQuestions ?? 0,
      darkMode: parsed.darkMode ?? false,
      themeMode: parsed.themeMode ?? (parsed.darkMode ? 'DARK' : 'LIGHT'),
      autoPlayAudio: parsed.autoPlayAudio ?? false,
      userName: parsed.userName ?? '',
      dailyGoalWords: parsed.dailyGoalWords ?? 10,
      reminderEnabled: parsed.reminderEnabled ?? false,
      reminderTime: parsed.reminderTime ?? '20:00',
      lastNotifiedDate: parsed.lastNotifiedDate
    };
  } catch {
    return {
      streakDays: 1,
      lastStudyDate: toLocalDateKey(Date.now()),
      totalQuizzesTaken: 0,
      totalQuizCorrect: 0,
      totalQuizQuestions: 0,
      darkMode: false,
      themeMode: 'LIGHT',
      autoPlayAudio: false,
      userName: '',
      dailyGoalWords: 10,
      reminderEnabled: false,
      reminderTime: '20:00'
    };
  }
}

export function saveUserStats(stats: UserStats): void {
  try {
    localStorage.setItem(STATS_KEY, JSON.stringify(stats));
  } catch {
    // ignore
  }
}

export function touchStudyStreak(): UserStats {
  const stats = loadUserStats();
  const today = toLocalDateKey(Date.now());
  if (stats.lastStudyDate !== today) {
    const yesterday = toLocalDateKey(Date.now() - 86400000);
    const nextStreak = stats.lastStudyDate === yesterday ? stats.streakDays + 1 : 1;
    const updated: UserStats = {
      ...stats,
      streakDays: nextStreak,
      lastStudyDate: today
    };
    saveUserStats(updated);
    return updated;
  }
  return stats;
}

export async function requestAppNotificationPermission(): Promise<NotificationPermission | 'unsupported'> {
  if (typeof window === 'undefined' || !('Notification' in window)) {
    return 'unsupported';
  }
  if (Notification.permission === 'granted') {
    return 'granted';
  }
  try {
    return await Notification.requestPermission();
  } catch {
    return Notification.permission;
  }
}

export async function triggerSwNotification(title: string, body: string): Promise<boolean> {
  if (typeof window === 'undefined' || !('Notification' in window)) return false;
  if (Notification.permission !== 'granted') return false;

  try {
    if ('serviceWorker' in navigator) {
      const reg = await navigator.serviceWorker.getRegistration();
      if (reg && 'showNotification' in reg) {
        await reg.showNotification(title, {
          body,
          icon: '/pwa-192x192.png',
          badge: '/pwa-192x192.png',
          tag: 'kuran-300-srs'
        });
        return true;
      }
    }
    new Notification(title, {
      body,
      icon: '/pwa-192x192.png'
    });
    return true;
  } catch {
    return false;
  }
}
