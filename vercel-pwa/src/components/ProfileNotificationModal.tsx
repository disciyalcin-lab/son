import React, { useState } from 'react';
import {
  UserStats,
  requestAppNotificationPermission,
  triggerSwNotification
} from '../utils/storage';
import {
  X,
  Bell,
  BellRing,
  UserCheck,
  Download,
  CheckCircle2,
  Clock,
  Target,
  Smartphone
} from 'lucide-react';
import { AppLogo } from './AppLogo';

interface ProfileNotificationModalProps {
  isOpen: boolean;
  onClose: () => void;
  stats: UserStats;
  onUpdateStats: (updated: UserStats) => void;
  isInstallable: boolean;
  isInstalled: boolean;
  isIOS: boolean;
  onInstallPWA: () => Promise<boolean>;
  sampleWord: {
    arabic: string;
    transliteration: string;
    turkishMeaning: string;
    frequency: number;
  };
}

export const ProfileNotificationModal: React.FC<ProfileNotificationModalProps> = ({
  isOpen,
  onClose,
  stats,
  onUpdateStats,
  isInstallable,
  isInstalled,
  isIOS,
  onInstallPWA,
  sampleWord
}) => {
  const [nameInput, setNameInput] = useState(stats.userName || '');
  const [dailyGoal, setDailyGoal] = useState<number>(stats.dailyGoalWords || 10);
  const [reminderTime, setReminderTime] = useState(stats.reminderTime || '20:00');
  const [reminderEnabled, setReminderEnabled] = useState(!!stats.reminderEnabled);
  const [feedbackMsg, setFeedbackMsg] = useState<string | null>(null);
  const [permissionState, setPermissionState] = useState<string>(() =>
    typeof window !== 'undefined' && 'Notification' in window
      ? Notification.permission
      : 'unsupported'
  );

  if (!isOpen) return null;

  const handleEnableNotifications = async () => {
    const perm = await requestAppNotificationPermission();
    setPermissionState(perm);
    if (perm === 'granted') {
      setReminderEnabled(true);
      const updated: UserStats = {
        ...stats,
        userName: nameInput.trim(),
        dailyGoalWords: dailyGoal,
        reminderEnabled: true,
        reminderTime
      };
      onUpdateStats(updated);
      await triggerSwNotification(
        `Selâmün Aleyküm${nameInput.trim() ? `, ${nameInput.trim()}` : ''}!`,
        `Günlük Kur'an 300 Kelime hatırlatıcınız ${reminderTime} saati için aktif edildi. Günün kelimesi: ${sampleWord.arabic} (${sampleWord.turkishMeaning})`
      );
      setFeedbackMsg('Bildirim izni onaylandı ve örnek bildirim gönderildi!');
    } else if (perm === 'denied') {
      setFeedbackMsg(
        'Tarayıcı bildirim izni kapalı. Tarayıcı adres çubuğundaki kilit ikonundan bildirimlere izin verebilirsiniz.'
      );
    } else {
      setFeedbackMsg('Bu tarayıcıda bildirim desteği bulunmuyor.');
    }
  };

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    const updated: UserStats = {
      ...stats,
      userName: nameInput.trim(),
      dailyGoalWords: dailyGoal,
      reminderEnabled,
      reminderTime
    };
    onUpdateStats(updated);
    setFeedbackMsg('Kişisel profil ve hatırlatma ayarlarınız kaydedildi.');
    setTimeout(() => {
      setFeedbackMsg(null);
      onClose();
    }, 900);
  };

  return (
    <div
      onClick={onClose}
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 backdrop-blur-xs p-0 sm:p-4"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-lg rounded-t-3xl sm:rounded-3xl bg-white dark:bg-[#1B1B22] border-t sm:border border-[#E4E4EC] dark:border-[#32323E] p-5 sm:p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b border-[#E4E4EC] dark:border-[#32323E] pb-3.5">
          <div className="flex items-center gap-3">
            <AppLogo size={38} />
            <div>
              <h3 className="text-base font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                Kişisel Profil & Bildirimler
              </h3>
              <p className="text-xs text-[#5A5A64] dark:text-[#B8B8C4]">
                Şifresiz hızlı kayıt, günlük hedef ve SRS hatırlatıcı
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-full bg-[#F0F0F4] dark:bg-[#262630] text-[#5A5A64] dark:text-[#B8B8C4]"
            aria-label="Kapat"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <form onSubmit={handleSaveProfile} className="space-y-4">
          {/* 1. Lightweight Name Input */}
          <div>
            <label className="block text-xs font-bold text-[#1A1A1E] dark:text-[#F2F2F7] mb-1.5">
              İsminiz / Hitap Tercihiniz
            </label>
            <div className="relative">
              <UserCheck className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-[#B7282E] dark:text-[#E2474D]" />
              <input
                type="text"
                value={nameInput}
                onChange={(e) => setNameInput(e.target.value)}
                placeholder="Örn: Zahide Hanım, Ahmet..."
                className="w-full rounded-xl bg-[#F7F7F9] dark:bg-[#111115] border border-[#E4E4EC] dark:border-[#32323E] pl-10 pr-4 py-2.5 text-sm text-[#1A1A1E] dark:text-[#F2F2F7] focus:outline-none focus:border-[#B7282E] dark:focus:border-[#E2474D]"
              />
            </div>
          </div>

          {/* 2. Daily Word Goal */}
          <div>
            <label className="flex items-center gap-1.5 text-xs font-bold text-[#1A1A1E] dark:text-[#F2F2F7] mb-1.5">
              <Target className="h-3.5 w-3.5 text-[#C89D42]" />
              <span>Günlük Kelime Hedefi</span>
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[5, 10, 20].map((goal) => {
                const active = dailyGoal === goal;
                return (
                  <button
                    key={goal}
                    type="button"
                    onClick={() => setDailyGoal(goal)}
                    className={`h-10 rounded-xl text-xs font-bold border transition ${
                      active
                        ? 'bg-[#B7282E] dark:bg-[#E2474D] text-white border-transparent'
                        : 'bg-[#F7F7F9] dark:bg-[#111115] border-[#E4E4EC] dark:border-[#32323E] text-[#5A5A64] dark:text-[#B8B8C4]'
                    }`}
                  >
                    Günde {goal} Kelime
                  </button>
                );
              })}
            </div>
          </div>

          {/* 3. Smart Notification & Reminder Time */}
          <div className="rounded-2xl bg-[#F7F7F9] dark:bg-[#111115] border border-[#E4E4EC] dark:border-[#32323E] p-3.5 space-y-3">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <BellRing className="h-4 w-4 text-[#B7282E] dark:text-[#E2474D]" />
                <div>
                  <p className="text-xs font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                    Günlük Kelime & SRS Bildirimi
                  </p>
                  <p className="text-[11px] text-[#5A5A64] dark:text-[#B8B8C4]">
                    Durum:{' '}
                    {permissionState === 'granted'
                      ? 'Aktif (İzin Verildi ✓)'
                      : permissionState === 'denied'
                      ? 'Engellendi (Tarayıcı Ayarı)'
                      : 'İzin Bekleniyor'}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5 text-[#C89D42]" />
                <input
                  type="time"
                  value={reminderTime}
                  onChange={(e) => setReminderTime(e.target.value)}
                  className="rounded-lg bg-white dark:bg-[#1B1B22] border border-[#E4E4EC] dark:border-[#32323E] px-2 py-1 text-xs font-bold text-[#1A1A1E] dark:text-[#F2F2F7]"
                />
              </div>
            </div>

            <button
              type="button"
              onClick={handleEnableNotifications}
              className="flex w-full h-10 items-center justify-center gap-2 rounded-xl bg-[#DEC286] dark:bg-[#4F3E1E] text-[#2C2214] dark:text-[#F7E3B5] text-xs font-extrabold transition hover:opacity-90"
            >
              <Bell className="h-4 w-4" />
              <span>
                {permissionState === 'granted'
                  ? 'Anlık Test Bildirimi Gönder'
                  : 'Bildirimleri Aç ve Test Et'}
              </span>
            </button>
          </div>

          {/* 4. PWA App Installation Section */}
          {!isInstalled && (
            <div className="rounded-2xl bg-[#FDECEC]/60 dark:bg-[#4E161A]/40 border border-[#B7282E]/25 dark:border-[#E2474D]/30 p-3.5 space-y-2">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Smartphone className="h-4 w-4 text-[#B7282E] dark:text-[#E2474D]" />
                  <p className="text-xs font-extrabold text-[#1A1A1E] dark:text-[#F2F2F7]">
                    Ana Ekrana Uygulama Olarak Yükle
                  </p>
                </div>
              </div>

              {isInstallable ? (
                <button
                  type="button"
                  onClick={onInstallPWA}
                  className="flex w-full h-10 items-center justify-center gap-2 rounded-xl bg-[#B7282E] dark:bg-[#E2474D] text-white text-xs font-bold shadow-xs"
                >
                  <Download className="h-4 w-4" />
                  <span>Uygulamayı Şimdi Yükle (PWA)</span>
                </button>
              ) : isIOS ? (
                <p className="text-[11px] text-[#5A5A64] dark:text-[#B8B8C4] leading-relaxed">
                  iPhone/iPad'de yüklemek için Safari alt menüsündeki <strong>Paylaş</strong> butonuna basıp <strong>"Ana Ekrana Ekle"</strong> seçeneğine dokunun.
                </p>
              ) : (
                <p className="text-[11px] text-[#5A5A64] dark:text-[#B8B8C4] leading-relaxed">
                  Android Chrome'da sağ üstteki <strong>⋮ (Üç Nokta)</strong> menüsünden <strong>"Uygulamayı yükle"</strong> veya <strong>"Ana ekrana ekle"</strong> seçeneğiyle telefonunuza tam ekran uygulama olarak kurabilirsiniz.
                </p>
              )}
            </div>
          )}

          {feedbackMsg && (
            <div className="flex items-center gap-2 rounded-xl bg-[#2E7D32]/15 border border-[#2E7D32]/30 px-3.5 py-2.5 text-xs font-bold text-[#2E7D32]">
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>{feedbackMsg}</span>
            </div>
          )}

          <button
            type="submit"
            className="flex w-full h-12 items-center justify-center gap-2 rounded-2xl bg-[#B7282E] dark:bg-[#E2474D] text-white font-bold text-sm shadow-sm transition active:scale-[0.99]"
          >
            <span>Ayarları Kaydet</span>
          </button>
        </form>
      </div>
    </div>
  );
};
