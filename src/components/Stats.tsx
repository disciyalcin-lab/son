import React, { useState, useMemo } from 'react';
import { QURAN_WORDS_300, TOTAL_QURAN_WORDS } from '../data/wordsData';
import { WordProgress, ThemeMode, getDeck12Number } from '../utils/storage';
import { Trophy, Lock, Smartphone, Sun, Moon, RotateCcw } from 'lucide-react';

interface StatsProps {
  progressMap: Record<number, WordProgress>;
  themeMode: ThemeMode;
  onSetThemeMode: (mode: ThemeMode) => void;
  onResetAllProgress: () => void;
}

export const Stats: React.FC<StatsProps> = ({
  progressMap,
  themeMode,
  onSetThemeMode,
  onResetAllProgress
}) => {
  const [showResetDialog, setShowResetDialog] = useState(false);

  const sortedWords = useMemo(
    () => [...QURAN_WORDS_300].sort((a, b) => b.frequency - a.frequency || a.id - b.id),
    []
  );

  const learnedWords = useMemo(
    () => sortedWords.filter((w) => progressMap[w.id]?.isLearned),
    [sortedWords, progressMap]
  );

  const learnedCount = learnedWords.length;
  const cumulativeFrequency = useMemo(
    () => learnedWords.reduce((sum, w) => sum + w.frequency, 0),
    [learnedWords]
  );
  const coveragePercentage = Math.min(100, (cumulativeFrequency / TOTAL_QURAN_WORDS) * 100);

  const badges = useMemo(
    () => [
      {
        id: 1,
        title: 'Temel Aşina',
        description: 'En sık geçen ilk 50 kelimeyi öğrenerek Kur\'an kelimelerinin temelini kavradınız.',
        coverageTargetText: '50 Kelime • ~%40 Kapsam',
        isUnlocked: learnedCount >= 50,
        progress: Math.min(1, learnedCount / 50)
      },
      {
        id: 2,
        title: 'Sayfa Okuyucu',
        description: '100 kelime barajını aşarak her Kur\'an sayfasındaki kelimelerin yarısından fazlasına aşina oldunuz.',
        coverageTargetText: '100 Kelime • ~%55 Kapsam',
        isUnlocked: learnedCount >= 100,
        progress: Math.min(1, learnedCount / 100)
      },
      {
        id: 3,
        title: 'Anlam Kaşifi',
        description: '200 kelimeye ulaşarak ayetlerin ana mesajını ve bağlamını doğrudan takip etmeye başladınız.',
        coverageTargetText: '200 Kelime • ~%68 Kapsam',
        isUnlocked: learnedCount >= 200,
        progress: Math.min(1, learnedCount / 200)
      },
      {
        id: 4,
        title: "Kur'an Kelime Üstadı",
        description: '300 kelimenin tamamını tamamlayarak Kur\'an-ı Kerim\'deki kelime geçişlerinin %70-75\'ini öğrendiniz!',
        coverageTargetText: '300 Kelime • ~%75 Kapsam',
        isUnlocked: learnedCount >= 300,
        progress: Math.min(1, learnedCount / 300)
      }
    ],
    [learnedCount]
  );

  // 12 Decks Progress Matrix
  const deckStats = useMemo(() => {
    return Array.from({ length: 12 }, (_, idx) => {
      const deckNum = idx + 1;
      const deckWords = sortedWords.filter((w) => getDeck12Number(w.id) === deckNum);
      const learnedInDeck = deckWords.filter((w) => progressMap[w.id]?.isLearned).length;
      const progress = deckWords.length > 0 ? learnedInDeck / deckWords.length : 0;
      return { deckNum, progress };
    });
  }, [sortedWords, progressMap]);

  return (
    <div className="space-y-4 pb-8">
      <h1 className="text-2xl font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7] pt-1">
        Kur'an Kapsama Analizi
      </h1>

      {/* 1. Main Coverage Card */}
      <div className="rounded-[24px] bg-white dark:bg-[#1B1B22] p-5 shadow-sm border border-[#E4E4EC] dark:border-[#32323E]">
        <h2 className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
          Genel Kümülatif Kapsama
        </h2>
        <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C] mt-1">
          Öğrendiğiniz kelimelerin Kur'an-ı Kerim metnindeki toplam karşılığı:
        </p>

        <div className="mt-4 flex items-end justify-between">
          <div>
            <p className="text-4xl font-extrabold text-[#B7282E] dark:text-[#E2474D]">
              %{coveragePercentage.toFixed(1)}
            </p>
            <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C] mt-0.5">
              Kur'an Kelime Kapsamı
            </p>
          </div>

          <div className="text-right">
            <p className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
              {cumulativeFrequency} / {TOTAL_QURAN_WORDS}
            </p>
            <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">
              Kelime Tekrarı Kapsandı
            </p>
          </div>
        </div>

        <div className="mt-3.5 h-2.5 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
          <div
            className="h-full rounded-full bg-[#B7282E] dark:bg-[#E2474D] transition-all duration-500"
            style={{ width: `${coveragePercentage}%` }}
          />
        </div>
      </div>

      {/* 2. Milestone Badges Section */}
      <div className="space-y-3 pt-1">
        <h2 className="text-lg font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
          Aşama Rozetleri (Milestones)
        </h2>

        <div className="space-y-2.5">
          {badges.map((badge) => (
            <div
              key={badge.id}
              className={`flex items-center gap-3.5 rounded-[18px] p-4 border transition ${
                badge.isUnlocked
                  ? 'bg-[#FDECEC]/40 dark:bg-[#4E161A]/35 border-[#C89D42]'
                  : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E]'
              }`}
            >
              <div
                className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full ${
                  badge.isUnlocked
                    ? 'bg-[#C89D42]/20 text-[#C89D42]'
                    : 'bg-[#F0F0F4] dark:bg-[#262630] text-[#8A8A96] dark:text-[#7E7E8C]'
                }`}
              >
                {badge.isUnlocked ? (
                  <Trophy className="h-6 w-6" />
                ) : (
                  <Lock className="h-6 w-6" />
                )}
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <h3
                    className={`text-sm font-extrabold ${
                      badge.isUnlocked
                        ? 'text-[#B7282E] dark:text-[#E2474D]'
                        : 'text-[#1A1A1E] dark:text-[#F2F2F7]'
                    }`}
                  >
                    {badge.title}
                  </h3>
                  <span className="text-[11px] font-bold text-[#C89D42] shrink-0">
                    {badge.coverageTargetText}
                  </span>
                </div>
                <p className="mt-1 text-xs text-[#5A5A64] dark:text-[#B8B8C4]">
                  {badge.description}
                </p>
                <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      badge.isUnlocked
                        ? 'bg-[#2E7D32]'
                        : 'bg-[#B7282E] dark:bg-[#E2474D]'
                    }`}
                    style={{ width: `${badge.progress * 100}%` }}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 3. 12 Decks Mini Matrix */}
      <div className="space-y-3 pt-1">
        <h2 className="text-lg font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
          Deste İlerleme Durumu (12 Deste)
        </h2>

        <div className="rounded-[20px] bg-white dark:bg-[#1B1B22] p-4 border border-[#E4E4EC] dark:border-[#32323E]">
          <div className="grid grid-cols-3 gap-2">
            {deckStats.map(({ deckNum, progress }) => {
              const completed = progress >= 1;
              return (
                <div
                  key={deckNum}
                  className={`rounded-xl p-2.5 text-center ${
                    completed
                      ? 'bg-[#2E7D32]/15'
                      : 'bg-[#F0F0F4] dark:bg-[#262630]'
                  }`}
                >
                  <p className="text-xs font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                    Deste {deckNum}
                  </p>
                  <p
                    className={`text-xs font-bold mt-0.5 ${
                      completed
                        ? 'text-[#2E7D32]'
                        : 'text-[#B7282E] dark:text-[#E2474D]'
                    }`}
                  >
                    %{Math.round(progress * 100)}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* 4. Uygulama Tema Seçimi (Sistem / Açık / Koyu) */}
      <div className="space-y-2.5 pt-1">
        <h2 className="text-lg font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
          Uygulama Tema Seçimi
        </h2>

        <div className="grid grid-cols-3 gap-2.5">
          {(
            [
              { key: 'SYSTEM', label: 'Sistem', icon: Smartphone },
              { key: 'LIGHT', label: 'Açık', icon: Sun },
              { key: 'DARK', label: 'Koyu', icon: Moon }
            ] as const
          ).map((item) => {
            const Icon = item.icon;
            const isSelected = themeMode === item.key;
            return (
              <button
                key={item.key}
                onClick={() => onSetThemeMode(item.key)}
                className={`flex flex-col items-start rounded-2xl p-3.5 text-left transition ${
                  isSelected
                    ? 'bg-white dark:bg-[#1B1B22] border-2 border-[#B7282E] dark:border-[#E2474D]'
                    : 'bg-[#F0F0F4]/70 dark:bg-[#262630]/70 border border-transparent'
                }`}
              >
                <Icon className="h-6 w-6 text-[#B7282E] dark:text-[#E2474D]" />
                <span
                  className={`mt-3 text-sm ${
                    isSelected ? 'font-extrabold' : 'font-medium'
                  } text-[#1A1A1E] dark:text-[#F2F2F7]`}
                >
                  {item.label}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 5. Reset Progress Button */}
      <button
        onClick={() => setShowResetDialog(true)}
        className="flex w-full h-[50px] items-center justify-center gap-2 rounded-2xl border border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22] text-[#E65100] font-bold text-sm"
      >
        <RotateCcw className="h-4 w-4" />
        <span>İlerlemeyi Sıfırla</span>
      </button>

      {showResetDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm rounded-3xl bg-white dark:bg-[#1B1B22] p-6 shadow-xl border border-[#E4E4EC] dark:border-[#32323E] space-y-4">
            <h3 className="text-lg font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
              İlerlemeyi Sıfırla
            </h3>
            <p className="text-sm text-[#5A5A64] dark:text-[#B8B8C4]">
              Tüm öğrenme ve tekrar verileriniz sıfırlanacaktır. Bu işlem geri alınamaz. Emin misiniz?
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setShowResetDialog(false)}
                className="px-4 py-2 text-sm font-bold text-[#5A5A64] dark:text-[#B8B8C4]"
              >
                İptal
              </button>
              <button
                onClick={() => {
                  onResetAllProgress();
                  setShowResetDialog(false);
                }}
                className="rounded-xl bg-[#E65100]/15 px-4 py-2 text-sm font-extrabold text-[#E65100]"
              >
                Evet, Sıfırla
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
