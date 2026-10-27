import React, { useState, useMemo, useEffect } from 'react';
import { QURAN_WORDS_300, QuranWord } from '../data/wordsData';
import { getDeck12Number, UserStats } from '../utils/storage';
import { audioPlayer } from '../utils/audioPlayer';
import {
  Volume2,
  CheckCircle2,
  XCircle,
  RefreshCw,
  Headphones,
  ArrowRight,
  Trophy,
  Timer,
  Flame,
  Award,
  Sparkles,
  X
} from 'lucide-react';

interface QuizProps {
  stats: UserStats;
  onCompleteQuiz: (correct: number, total: number) => void;
  onMarkWord: (wordId: number, knewIt: boolean) => void;
}

type QuizMode = 'AR_TO_TR' | 'TR_TO_AR' | 'AUDIO_QUIZ' | 'AYAH_GAP';

const QUIZ_MODES: { key: QuizMode; title: string }[] = [
  { key: 'AR_TO_TR', title: 'Arapça → Türkçe' },
  { key: 'TR_TO_AR', title: 'Türkçe → Arapça' },
  { key: 'AUDIO_QUIZ', title: 'Sesli Quiz' },
  { key: 'AYAH_GAP', title: 'Ayet Boşluk Doldurma' }
];

const QUESTION_TIME_SECONDS = 20;

interface QuizQuestion {
  target: QuranWord;
  options: QuranWord[];
  correctIndex: number;
  maskedAyah?: string;
}

function buildQuestions(count: number, deckFilter: number | null): QuizQuestion[] {
  const sourcePool =
    deckFilter !== null
      ? QURAN_WORDS_300.filter((w) => getDeck12Number(w.id) === deckFilter)
      : QURAN_WORDS_300;

  const shuffled = [...sourcePool].sort(() => Math.random() - 0.5);
  const selected = shuffled.slice(0, Math.min(count, shuffled.length));

  return selected.map((target) => {
    const distractors = QURAN_WORDS_300.filter((w) => w.id !== target.id)
      .sort(() => Math.random() - 0.5)
      .slice(0, 3);
    const options = [target, ...distractors].sort(() => Math.random() - 0.5);
    const correctIndex = options.findIndex((o) => o.id === target.id);
    const maskedAyah = target.exampleAyahArabic.includes(target.arabic)
      ? target.exampleAyahArabic.replace(target.arabic, ' [ ... ] ')
      : `${target.exampleAyahArabic}  [ ... ]`;
    return { target, options, correctIndex, maskedAyah };
  });
}

function formatDuration(totalSeconds: number): string {
  const mins = Math.floor(totalSeconds / 60);
  const secs = totalSeconds % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

export const Quiz: React.FC<QuizProps> = ({ stats, onCompleteQuiz, onMarkWord }) => {
  const [currentMode, setCurrentMode] = useState<QuizMode>('AR_TO_TR');
  const [questionCount, setQuestionCount] = useState<number>(10);
  const [selectedDeckFilter, setSelectedDeckFilter] = useState<number | null>(null);
  const [questions, setQuestions] = useState<QuizQuestion[]>(() => buildQuestions(10, null));
  const [qIndex, setQIndex] = useState(0);
  const [selectedOptionIndex, setSelectedOptionIndex] = useState<number | null>(null);
  const [score, setScore] = useState(0);
  const [currentStreak, setCurrentStreak] = useState(0);
  const [bestStreak, setBestStreak] = useState(0);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [questionTimeLeft, setQuestionTimeLeft] = useState(QUESTION_TIME_SECONDS);
  const [timedOut, setTimedOut] = useState(false);
  const [isFinished, setIsFinished] = useState(false);
  const [showCelebrationModal, setShowCelebrationModal] = useState(false);

  const currentQ = questions[qIndex];
  const isAnswerChecked = selectedOptionIndex !== null || timedOut;

  // Live Quiz Timer & Per-Question Countdown
  useEffect(() => {
    if (isFinished || isAnswerChecked || !currentQ) return;

    const interval = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
      setQuestionTimeLeft((prev) => {
        if (prev <= 1) {
          setTimedOut(true);
          setCurrentStreak(0);
          onMarkWord(currentQ.target.id, false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isFinished, isAnswerChecked, currentQ, onMarkWord]);

  const startNewQuiz = (
    mode: QuizMode = currentMode,
    count: number = questionCount,
    deck: number | null = selectedDeckFilter
  ) => {
    const fresh = buildQuestions(count, deck);
    setCurrentMode(mode);
    setQuestionCount(count);
    setSelectedDeckFilter(deck);
    setQuestions(fresh);
    setQIndex(0);
    setSelectedOptionIndex(null);
    setTimedOut(false);
    setScore(0);
    setCurrentStreak(0);
    setBestStreak(0);
    setElapsedSeconds(0);
    setQuestionTimeLeft(QUESTION_TIME_SECONDS);
    setIsFinished(false);
    setShowCelebrationModal(false);
    if (mode === 'AUDIO_QUIZ' && fresh[0]) {
      setTimeout(() => audioPlayer.playWord(fresh[0].target.arabic, fresh[0].target.audioUrl), 250);
    }
  };

  const handleSelectOption = (index: number) => {
    if (isAnswerChecked || !currentQ) return;
    setSelectedOptionIndex(index);
    const isCorrect = index === currentQ.correctIndex;
    if (isCorrect) {
      setScore((s) => s + 1);
      setCurrentStreak((prev) => {
        const next = prev + 1;
        setBestStreak((best) => Math.max(best, next));
        return next;
      });
      onMarkWord(currentQ.target.id, true);
    } else {
      setCurrentStreak(0);
      onMarkWord(currentQ.target.id, false);
    }
  };

  const handleNextQuestion = () => {
    if (qIndex + 1 < questions.length) {
      const nextIdx = qIndex + 1;
      setQIndex(nextIdx);
      setSelectedOptionIndex(null);
      setTimedOut(false);
      setQuestionTimeLeft(QUESTION_TIME_SECONDS);
      if (currentMode === 'AUDIO_QUIZ' && questions[nextIdx]) {
        const nextWord = questions[nextIdx].target;
        setTimeout(() => audioPlayer.playWord(nextWord.arabic, nextWord.audioUrl), 200);
      }
    } else {
      setIsFinished(true);
      setShowCelebrationModal(true);
      onCompleteQuiz(score, questions.length);
    }
  };

  const percentage = useMemo(
    () => Math.round((score / Math.max(1, questions.length)) * 100),
    [score, questions.length]
  );

  const avgSecondsPerQuestion = useMemo(
    () => (questions.length > 0 ? (elapsedSeconds / questions.length).toFixed(1) : '0.0'),
    [elapsedSeconds, questions.length]
  );

  const modeTitle = QUIZ_MODES.find((m) => m.key === currentMode)?.title || 'Quiz';
  const resultInfo =
    percentage >= 90
      ? {
          msg: "Mükemmel! Kur'an kelimelerine çok iyi hâkimsiniz! 🌟",
          color: '#2E7D32'
        }
      : percentage >= 70
      ? {
          msg: 'Çok İyi! Biraz daha pratikle üstat olacaksınız! 👏',
          color: '#B7282E'
        }
      : percentage >= 50
      ? {
          msg: 'İyi deneme! Kelime kartlarıyla tekrar yapmanızı öneririz. 📖',
          color: '#C89D42'
        }
      : {
          msg: 'Yılmak yok! Günlük kart tekrarlarıyla başaracaksınız! 💪',
          color: '#E65100'
        };

  const overallAccuracy = useMemo(() => {
    if (!stats.totalQuizQuestions || stats.totalQuizQuestions <= 0) return percentage;
    return Math.round((stats.totalQuizCorrect / stats.totalQuizQuestions) * 100);
  }, [stats.totalQuizCorrect, stats.totalQuizQuestions, percentage]);

  return (
    <div className="space-y-3 pb-8">
      {/* 1. Quiz Mode Tabs */}
      <div className="-mx-4 flex items-center overflow-x-auto border-b border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22] px-4 no-scrollbar">
        {QUIZ_MODES.map((m) => {
          const active = currentMode === m.key;
          return (
            <button
              key={m.key}
              onClick={() => startNewQuiz(m.key, questionCount, selectedDeckFilter)}
              className={`shrink-0 border-b-2 px-3.5 py-3 text-xs transition ${
                active
                  ? 'border-[#B7282E] dark:border-[#E2474D] font-extrabold text-[#B7282E] dark:text-[#E2474D]'
                  : 'border-transparent font-medium text-[#5A5A64] dark:text-[#B8B8C4]'
              }`}
            >
              {m.title}
            </button>
          );
        })}
      </div>

      {/* 2. Question Count & Deck Filter Chips */}
      <div className="flex items-center gap-2 overflow-x-auto py-1 no-scrollbar">
        <span className="text-xs font-semibold text-[#8A8A96] dark:text-[#7E7E8C] shrink-0">
          Soru:
        </span>
        {[5, 10, 15].map((cnt) => {
          const active = questionCount === cnt;
          return (
            <button
              key={cnt}
              onClick={() => startNewQuiz(currentMode, cnt, selectedDeckFilter)}
              className={`shrink-0 rounded-lg px-2.5 py-1.5 text-xs font-bold border transition ${
                active
                  ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/40 text-[#8E1B20] dark:text-[#FFD9DA]'
                  : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
              }`}
            >
              {cnt} Soru
            </button>
          );
        })}

        <button
          onClick={() => startNewQuiz(currentMode, questionCount, null)}
          className={`shrink-0 rounded-lg px-2.5 py-1.5 text-xs font-bold border transition ml-1 ${
            selectedDeckFilter === null
              ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/40 text-[#8E1B20] dark:text-[#FFD9DA]'
              : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
          }`}
        >
          Tüm Kur'an (300)
        </button>

        {Array.from({ length: 12 }, (_, i) => i + 1).map((deckNum) => {
          const active = selectedDeckFilter === deckNum;
          return (
            <button
              key={deckNum}
              onClick={() => startNewQuiz(currentMode, questionCount, deckNum)}
              className={`shrink-0 rounded-lg px-2.5 py-1.5 text-xs font-bold border transition ${
                active
                  ? 'bg-[#FDECEC] dark:bg-[#4E161A] border-[#B7282E]/40 text-[#8E1B20] dark:text-[#FFD9DA]'
                  : 'bg-white dark:bg-[#1B1B22] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
              }`}
            >
              Deste {deckNum}
            </button>
          );
        })}
      </div>

      {/* Finished Summary Card (behind or after closing celebration modal) */}
      {isFinished ? (
        <div className="rounded-[28px] bg-white dark:bg-[#1B1B22] p-7 text-center shadow-md border border-[#E4E4EC] dark:border-[#32323E] my-4 space-y-4">
          <div
            className="mx-auto flex h-20 w-20 items-center justify-center rounded-full"
            style={{ backgroundColor: `${resultInfo.color}26` }}
          >
            <Trophy className="h-11 w-11" style={{ color: resultInfo.color }} />
          </div>

          <div>
            <p className="text-sm font-semibold text-[#8A8A96] dark:text-[#7E7E8C]">
              {modeTitle}
            </p>
            <h2
              className="mt-1 text-3xl font-extrabold tabular-nums"
              style={{ color: resultInfo.color }}
            >
              %{percentage} Başarı
            </h2>
            <p className="mt-1 text-base font-bold tabular-nums text-[#1A1A1E] dark:text-[#F2F2F7]">
              {score} / {questions.length} Doğru
            </p>
            <p className="mt-2 text-sm text-[#5A5A64] dark:text-[#B8B8C4]">
              {resultInfo.msg}
            </p>
          </div>

          <div className="grid grid-cols-3 gap-2 pt-2">
            <div className="rounded-2xl bg-[#F0F0F4] dark:bg-[#262630] p-3">
              <p className="text-[11px] text-[#8A8A96] dark:text-[#7E7E8C]">En İyi Seri</p>
              <p className="mt-0.5 text-base font-extrabold tabular-nums text-[#B7282E] dark:text-[#E2474D]">
                🔥 {bestStreak}
              </p>
            </div>
            <div className="rounded-2xl bg-[#F0F0F4] dark:bg-[#262630] p-3">
              <p className="text-[11px] text-[#8A8A96] dark:text-[#7E7E8C]">Toplam Süre</p>
              <p className="mt-0.5 text-base font-extrabold tabular-nums text-[#1A1A1E] dark:text-[#F2F2F7]">
                {formatDuration(elapsedSeconds)}
              </p>
            </div>
            <div className="rounded-2xl bg-[#F0F0F4] dark:bg-[#262630] p-3">
              <p className="text-[11px] text-[#8A8A96] dark:text-[#7E7E8C]">Ort. Hız</p>
              <p className="mt-0.5 text-base font-extrabold tabular-nums text-[#C89D42]">
                {avgSecondsPerQuestion}s
              </p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-2.5 pt-2">
            <button
              onClick={() => setShowCelebrationModal(true)}
              className="flex flex-1 h-[50px] items-center justify-center gap-2 rounded-2xl border border-[#C89D42] text-[#C89D42] font-bold text-sm transition"
            >
              <Award className="h-4 w-4" />
              <span>İstatistik Detayı</span>
            </button>
            <button
              onClick={() => startNewQuiz(currentMode, questionCount, selectedDeckFilter)}
              className="flex flex-1 h-[50px] items-center justify-center gap-2 rounded-2xl bg-[#B7282E] dark:bg-[#E2474D] text-white font-bold text-sm shadow-sm transition"
            >
              <RefreshCw className="h-4 w-4" />
              <span>Yeni Quiz Başlat</span>
            </button>
          </div>
        </div>
      ) : (
        currentQ && (
          <>
            {/* 3. Progress Bar, Timer & Consecutive Streak Header */}
            <div className="space-y-2 pt-1">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-extrabold tabular-nums text-[#B7282E] dark:text-[#E2474D]">
                  Soru {qIndex + 1} / {questions.length}
                </span>

                <div className="flex items-center gap-1.5">
                  {/* Consecutive Answer Streak Badge */}
                  <div
                    className={`flex items-center gap-1 rounded-xl px-2.5 py-1 text-xs font-extrabold tabular-nums transition ${
                      currentStreak >= 2
                        ? 'bg-[#DEC286] dark:bg-[#4F3E1E] text-[#2C2214] dark:text-[#F7E3B5] scale-105'
                        : 'bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]'
                    }`}
                    title="Ardışık Doğru Cevap Serisi"
                  >
                    <Flame
                      className={`h-3.5 w-3.5 ${
                        currentStreak > 0
                          ? 'text-[#B7282E] dark:text-[#E2474D] fill-current'
                          : 'text-[#8A8A96]'
                      }`}
                    />
                    <span>{currentStreak} Seri</span>
                  </div>

                  {/* Live Question Countdown & Total Timer */}
                  <div
                    className={`flex items-center gap-1 rounded-xl px-2.5 py-1 text-xs font-extrabold tabular-nums transition ${
                      questionTimeLeft <= 5 && !isAnswerChecked
                        ? 'bg-[#E65100]/15 text-[#E65100]'
                        : 'bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]'
                    }`}
                    title="Soru Süresi / Toplam Süre"
                  >
                    <Timer className="h-3.5 w-3.5" />
                    <span>
                      {questionTimeLeft}sn · {formatDuration(elapsedSeconds)}
                    </span>
                  </div>

                  {/* Correct Score Badge */}
                  <span className="rounded-xl bg-[#FDECEC] dark:bg-[#4E161A] px-2.5 py-1 text-xs font-bold tabular-nums text-[#8E1B20] dark:text-[#FFD9DA]">
                    Doğru: {score}
                  </span>
                </div>
              </div>

              {/* Dual Progress Bars: Quiz Progress + Question Time Countdown */}
              <div className="space-y-1">
                <div className="h-1.5 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
                  <div
                    className="h-full rounded-full bg-[#B7282E] dark:bg-[#E2474D] transition-all duration-300"
                    style={{ width: `${((qIndex + 1) / questions.length) * 100}%` }}
                  />
                </div>
                <div className="h-1 w-full overflow-hidden rounded-full bg-[#F0F0F4] dark:bg-[#262630]">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      questionTimeLeft <= 5 ? 'bg-[#E65100]' : 'bg-[#C89D42]'
                    }`}
                    style={{
                      width: `${(questionTimeLeft / QUESTION_TIME_SECONDS) * 100}%`
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Question Card */}
            <div className="rounded-[24px] bg-white dark:bg-[#1B1B22] p-5 text-center shadow-sm border border-[#C89D42]/35 space-y-2">
              {currentMode === 'AR_TO_TR' && (
                <>
                  <p
                    className="font-arabic text-5xl font-bold text-[#B7282E] dark:text-[#E2474D]"
                    dir="rtl"
                  >
                    {currentQ.target.arabic}
                  </p>
                  <p className="text-sm font-medium text-[#5A5A64] dark:text-[#B8B8C4]">
                    {currentQ.target.transliteration}
                  </p>
                  <button
                    onClick={() =>
                      audioPlayer.playWord(currentQ.target.arabic, currentQ.target.audioUrl)
                    }
                    className="mx-auto flex h-10 w-10 items-center justify-center rounded-full text-[#B7282E] dark:text-[#E2474D] hover:bg-[#B7282E]/10 transition"
                    aria-label="Dinle"
                  >
                    <Volume2 className="h-5 w-5" />
                  </button>
                </>
              )}

              {currentMode === 'TR_TO_AR' && (
                <>
                  <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">
                    Bu Türkçe anlamın Arapça karşılığı nedir?
                  </p>
                  <p className="text-2xl font-extrabold text-[#B7282E] dark:text-[#E2474D] pt-1">
                    {currentQ.target.turkishMeaning}
                  </p>
                </>
              )}

              {currentMode === 'AUDIO_QUIZ' && (
                <div className="py-2 space-y-2">
                  <button
                    onClick={() =>
                      audioPlayer.playWord(currentQ.target.arabic, currentQ.target.audioUrl)
                    }
                    className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-[#FDECEC] dark:bg-[#4E161A] text-[#8E1B20] dark:text-[#FFD9DA] shadow-sm transition active:scale-95"
                  >
                    <Headphones className="h-8 w-8" />
                  </button>
                  <p className="text-sm font-bold text-[#B7282E] dark:text-[#E2474D]">
                    Sesi Dinlemek İçin Dokunun
                  </p>
                  <p className="text-xs text-[#8A8A96] dark:text-[#7E7E8C]">
                    Doğru Türkçe anlamı aşağıdan seçin
                  </p>
                </div>
              )}

              {currentMode === 'AYAH_GAP' && (
                <div className="space-y-2">
                  <p
                    className="font-arabic text-2xl text-[#1A1A1E] dark:text-[#F2F2F7] leading-relaxed"
                    dir="rtl"
                  >
                    {currentQ.maskedAyah}
                  </p>
                  <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4]">
                    "{currentQ.target.exampleAyahTurkish}"
                  </p>
                  <button
                    onClick={() =>
                      audioPlayer.playAyah(
                        currentQ.target.exampleAudioUrl,
                        currentQ.target.exampleAyahArabic
                      )
                    }
                    className="mx-auto flex h-10 w-10 items-center justify-center rounded-full text-[#B7282E] dark:text-[#E2474D] hover:bg-[#B7282E]/10 transition"
                  >
                    <Volume2 className="h-5 w-5" />
                  </button>
                </div>
              )}
            </div>

            {/* Timeout Banner if time ran out */}
            {timedOut && (
              <div className="rounded-xl bg-[#E65100]/15 border border-[#E65100]/30 px-3.5 py-2 text-xs font-bold text-[#E65100] text-center">
                ⏱️ Süre doldu! Doğru cevap aşağıda işaretlendi.
              </div>
            )}

            {/* 4 Options (A, B, C, D) */}
            <div className="space-y-2.5">
              {currentQ.options.map((opt, index) => {
                const isSelected = selectedOptionIndex === index;
                const isCorrect = index === currentQ.correctIndex;
                const optionLetter = String.fromCharCode(65 + index);
                const optionLabel =
                  currentMode === 'TR_TO_AR' || currentMode === 'AYAH_GAP'
                    ? `${opt.arabic} (${opt.transliteration})`
                    : opt.turkishMeaning;

                let borderColor = 'border-[#E4E4EC] dark:border-[#32323E]';
                let bgColor = 'bg-white dark:bg-[#1B1B22]';
                let textColor = 'text-[#1A1A1E] dark:text-[#F2F2F7]';
                let badgeColor =
                  'bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]';

                if (isAnswerChecked) {
                  if (isCorrect) {
                    borderColor = 'border-[#2E7D32]';
                    bgColor = 'bg-[#2E7D32]/15';
                    textColor = 'text-[#2E7D32] font-bold';
                    badgeColor = 'bg-[#2E7D32]/20 text-[#2E7D32]';
                  } else if (isSelected) {
                    borderColor = 'border-[#E65100]';
                    bgColor = 'bg-[#E65100]/15';
                    textColor = 'text-[#E65100]';
                    badgeColor = 'bg-[#E65100]/20 text-[#E65100]';
                  }
                }

                return (
                  <button
                    key={opt.id}
                    disabled={isAnswerChecked}
                    onClick={() => handleSelectOption(index)}
                    className={`flex w-full items-center justify-between rounded-2xl border-[1.5px] p-3.5 text-left transition ${borderColor} ${bgColor}`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <span
                        className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-extrabold ${badgeColor}`}
                      >
                        {optionLetter}
                      </span>
                      <span
                        className={`text-sm ${
                          currentMode === 'TR_TO_AR' || currentMode === 'AYAH_GAP'
                            ? 'font-arabic text-xl'
                            : ''
                        } ${textColor}`}
                      >
                        {optionLabel}
                      </span>
                    </div>

                    {isAnswerChecked && isCorrect && (
                      <CheckCircle2 className="h-5 w-5 text-[#2E7D32] shrink-0" />
                    )}
                    {isAnswerChecked && isSelected && !isCorrect && (
                      <XCircle className="h-5 w-5 text-[#E65100] shrink-0" />
                    )}
                  </button>
                );
              })}
            </div>

            {/* Explanation & Next Question Button */}
            {isAnswerChecked && (
              <div className="space-y-3 pt-1">
                <div className="flex items-center justify-between rounded-2xl border border-[#E4E4EC] dark:border-[#32323E] bg-white dark:bg-[#1B1B22] p-3.5">
                  <div>
                    <p className="font-arabic text-xl font-bold text-[#B7282E] dark:text-[#E2474D]">
                      {currentQ.target.arabic} • {currentQ.target.transliteration}
                    </p>
                    <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4]">
                      Anlamı: {currentQ.target.turkishMeaning} ({currentQ.target.frequency} kez)
                    </p>
                  </div>
                  <button
                    onClick={() =>
                      audioPlayer.playWord(currentQ.target.arabic, currentQ.target.audioUrl)
                    }
                    className="flex h-10 w-10 items-center justify-center rounded-full text-[#B7282E] dark:text-[#E2474D] hover:bg-[#B7282E]/10"
                  >
                    <Volume2 className="h-5 w-5" />
                  </button>
                </div>

                <button
                  onClick={handleNextQuestion}
                  className="flex w-full h-[54px] items-center justify-center gap-2 rounded-2xl bg-[#B7282E] dark:bg-[#E2474D] text-white font-bold text-sm shadow-sm transition"
                >
                  <span>
                    {qIndex + 1 >= questions.length ? 'Sonuçları Gör' : 'Sonraki Soru'}
                  </span>
                  <ArrowRight className="h-4 w-4" />
                </button>
              </div>
            )}
          </>
        )
      )}

      {/* Celebration Modal Window at Quiz Completion */}
      {showCelebrationModal && (
        <div
          onClick={() => setShowCelebrationModal(false)}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="relative w-full max-w-md rounded-[28px] bg-white dark:bg-[#1B1B22] p-6 text-center shadow-2xl border border-[#E4E4EC] dark:border-[#32323E] space-y-4"
          >
            <button
              onClick={() => setShowCelebrationModal(false)}
              className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-full bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]"
              aria-label="Kapat"
            >
              <X className="h-4 w-4" />
            </button>

            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-[#DEC286]/35 dark:bg-[#4F3E1E]">
              <Trophy className="h-11 w-11 text-[#C89D42]" />
            </div>

            <div>
              <div className="inline-flex items-center gap-1 rounded-full bg-[#FDECEC] dark:bg-[#4E161A] px-3 py-1 text-xs font-bold text-[#8E1B20] dark:text-[#FFD9DA]">
                <Sparkles className="h-3.5 w-3.5" />
                <span>Quiz Tamamlandı • {modeTitle}</span>
              </div>
              <h2
                className="mt-2 text-3xl font-extrabold tabular-nums"
                style={{ color: resultInfo.color }}
              >
                %{percentage} Başarı!
              </h2>
              <p className="mt-1 text-xs text-[#5A5A64] dark:text-[#B8B8C4]">
                {resultInfo.msg}
              </p>
            </div>

            {/* Celebration Statistics Grid */}
            <div className="grid grid-cols-2 gap-2.5 text-left">
              <div className="rounded-2xl bg-[#F7F7F9] dark:bg-[#262630] p-3 border border-[#E4E4EC] dark:border-[#32323E]">
                <p className="text-[11px] font-semibold text-[#8A8A96] dark:text-[#7E7E8C]">
                  Doğru / Toplam
                </p>
                <p className="mt-0.5 text-lg font-extrabold tabular-nums text-[#2E7D32]">
                  {score} / {questions.length}
                </p>
              </div>

              <div className="rounded-2xl bg-[#F7F7F9] dark:bg-[#262630] p-3 border border-[#E4E4EC] dark:border-[#32323E]">
                <p className="text-[11px] font-semibold text-[#8A8A96] dark:text-[#7E7E8C]">
                  En Yüksek Seri
                </p>
                <p className="mt-0.5 text-lg font-extrabold tabular-nums text-[#B7282E] dark:text-[#E2474D]">
                  🔥 {bestStreak} Seri
                </p>
              </div>

              <div className="rounded-2xl bg-[#F7F7F9] dark:bg-[#262630] p-3 border border-[#E4E4EC] dark:border-[#32323E]">
                <p className="text-[11px] font-semibold text-[#8A8A96] dark:text-[#7E7E8C]">
                  Toplam Süre
                </p>
                <p className="mt-0.5 text-lg font-extrabold tabular-nums text-[#1A1A1E] dark:text-[#F2F2F7]">
                  ⏱️ {formatDuration(elapsedSeconds)}
                </p>
              </div>

              <div className="rounded-2xl bg-[#F7F7F9] dark:bg-[#262630] p-3 border border-[#E4E4EC] dark:border-[#32323E]">
                <p className="text-[11px] font-semibold text-[#8A8A96] dark:text-[#7E7E8C]">
                  Soru Başına Hız
                </p>
                <p className="mt-0.5 text-lg font-extrabold tabular-nums text-[#C89D42]">
                  {avgSecondsPerQuestion} sn
                </p>
              </div>
            </div>

            {/* Cumulative User Quiz Stats Banner */}
            <div className="rounded-2xl bg-[#FDECEC]/60 dark:bg-[#4E161A]/40 border border-[#B7282E]/20 px-4 py-2.5 flex items-center justify-between text-xs">
              <span className="font-semibold text-[#5A5A64] dark:text-[#B8B8C4]">
                Toplam Çözülen Quiz: <strong className="text-[#1A1A1E] dark:text-[#F2F2F7]">{stats.totalQuizzesTaken}</strong>
              </span>
              <span className="font-bold text-[#B7282E] dark:text-[#E2474D] tabular-nums">
                Genel Ort: %{overallAccuracy}
              </span>
            </div>

            <div className="space-y-2 pt-1">
              <button
                onClick={() => startNewQuiz(currentMode, questionCount, selectedDeckFilter)}
                className="flex w-full h-12 items-center justify-center gap-2 rounded-2xl bg-[#B7282E] dark:bg-[#E2474D] text-white font-bold text-sm shadow-sm transition"
              >
                <RefreshCw className="h-4 w-4" />
                <span>Yeni Quiz Başlat</span>
              </button>
              <button
                onClick={() => setShowCelebrationModal(false)}
                className="flex w-full h-10 items-center justify-center rounded-xl text-xs font-bold text-[#5A5A64] dark:text-[#B8B8C4]"
              >
                Pencereyi Kapat
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
