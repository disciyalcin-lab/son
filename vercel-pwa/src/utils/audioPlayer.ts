import { useState, useEffect } from 'react';

export type AudioPlaybackStatus = 'idle' | 'loading' | 'playing' | 'error';

export interface AudioPlayerSnapshot {
  status: AudioPlaybackStatus;
  activeKey: string | null;
  errorMessage: string | null;
}

type AudioListener = (snapshot: AudioPlayerSnapshot) => void;

const DEBOUNCE_MS = 280;

class QuranAudioPlayer {
  private currentAudio: HTMLAudioElement | null = null;
  private status: AudioPlaybackStatus = 'idle';
  private activeKey: string | null = null;
  private errorMessage: string | null = null;
  private lastTriggerTimestamp = 0;
  private lastTriggeredKey: string | null = null;
  private errorTimer: ReturnType<typeof setTimeout> | null = null;
  private listeners: Set<AudioListener> = new Set();

  public getSnapshot(): AudioPlayerSnapshot {
    return {
      status: this.status,
      activeKey: this.activeKey,
      errorMessage: this.errorMessage
    };
  }

  public subscribe(listener: AudioListener): () => void {
    this.listeners.add(listener);
    listener(this.getSnapshot());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private emit(): void {
    const snap = this.getSnapshot();
    this.listeners.forEach((listener) => listener(snap));
  }

  private setState(
    status: AudioPlaybackStatus,
    activeKey: string | null,
    errorMessage: string | null = null
  ): void {
    this.status = status;
    this.activeKey = activeKey;
    if (errorMessage !== null) {
      this.errorMessage = errorMessage;
      if (this.errorTimer) clearTimeout(this.errorTimer);
      this.errorTimer = setTimeout(() => {
        this.errorMessage = null;
        if (this.status === 'error') {
          this.status = 'idle';
          this.activeKey = null;
        }
        this.emit();
      }, 3800);
    }
    this.emit();
  }

  public clearError(): void {
    if (this.errorTimer) clearTimeout(this.errorTimer);
    this.errorMessage = null;
    if (this.status === 'error') {
      this.status = 'idle';
      this.activeKey = null;
    }
    this.emit();
  }

  private isDebounced(key: string): boolean {
    const now = Date.now();
    if (this.lastTriggeredKey === key && now - this.lastTriggerTimestamp < DEBOUNCE_MS) {
      return true;
    }
    this.lastTriggeredKey = key;
    this.lastTriggerTimestamp = now;
    return false;
  }

  public stop(): void {
    try {
      if (this.currentAudio) {
        this.currentAudio.onended = null;
        this.currentAudio.onerror = null;
        this.currentAudio.oncanplaythrough = null;
        this.currentAudio.onplaying = null;
        this.currentAudio.pause();
        this.currentAudio.currentTime = 0;
        this.currentAudio = null;
      }
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        window.speechSynthesis.cancel();
      }
    } catch {
      // Ignore cleanup errors
    }
    this.setState('idle', null);
  }

  public playWord(
    arabicText: string,
    fallbackUrl?: string,
    onEnd?: () => void,
    key?: string
  ): void {
    const resolvedKey = key || `word:${arabicText}`;

    // Debounce rapid consecutive clicks
    if (this.isDebounced(resolvedKey)) {
      return;
    }

    // Toggle stop if already playing or loading this exact item
    if (
      (this.status === 'playing' || this.status === 'loading') &&
      this.activeKey === resolvedKey
    ) {
      this.stop();
      onEnd?.();
      return;
    }

    this.stop();
    this.setState('loading', resolvedKey);

    try {
      // Prefer authentic audio URL if provided, with instant Arabic male TTS fallback
      if (fallbackUrl) {
        this.playUrl(
          fallbackUrl,
          resolvedKey,
          onEnd,
          () => {
            this.speakWithTTS(arabicText, resolvedKey, onEnd);
          }
        );
        return;
      }

      this.speakWithTTS(arabicText, resolvedKey, onEnd);
    } catch {
      this.setState(
        'error',
        null,
        'Ses oynatılırken bir sorun oluştu. Lütfen tekrar deneyin.'
      );
      onEnd?.();
    }
  }

  public playAyah(
    audioUrl: string,
    arabicText?: string,
    onEnd?: () => void,
    key?: string
  ): void {
    const resolvedKey = key || `ayah:${audioUrl || arabicText || 'default'}`;

    // Debounce rapid consecutive clicks
    if (this.isDebounced(resolvedKey)) {
      return;
    }

    // Toggle stop if already playing or loading this exact item
    if (
      (this.status === 'playing' || this.status === 'loading') &&
      this.activeKey === resolvedKey
    ) {
      this.stop();
      onEnd?.();
      return;
    }

    this.stop();
    this.setState('loading', resolvedKey);

    if (!audioUrl && arabicText) {
      this.speakWithTTS(arabicText, resolvedKey, onEnd);
      return;
    }

    this.playUrl(
      audioUrl,
      resolvedKey,
      onEnd,
      () => {
        if (arabicText) {
          this.speakWithTTS(arabicText, resolvedKey, onEnd);
        } else {
          this.setState(
            'error',
            null,
            'Ayet ses dosyasına ulaşılamadı. İnternet bağlantınızı kontrol edin.'
          );
          onEnd?.();
        }
      }
    );
  }

  private speakWithTTS(
    arabicText: string,
    resolvedKey: string,
    onEnd?: () => void
  ): void {
    try {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        const voices = window.speechSynthesis.getVoices();
        const arabicVoice =
          voices.find(
            (v) =>
              v.lang.startsWith('ar') &&
              /male|majed|naayf|tarik|hamed|maged/i.test(v.name)
          ) || voices.find((v) => v.lang.startsWith('ar'));

        const utterance = new SpeechSynthesisUtterance(arabicText);
        if (arabicVoice) {
          utterance.voice = arabicVoice;
          utterance.lang = arabicVoice.lang || 'ar-SA';
        } else {
          utterance.lang = 'ar-SA';
        }
        utterance.rate = 0.78;
        utterance.pitch = 0.85;

        utterance.onstart = () => {
          this.setState('playing', resolvedKey);
        };
        utterance.onend = () => {
          this.setState('idle', null);
          onEnd?.();
        };
        utterance.onerror = () => {
          this.setState(
            'error',
            null,
            'Ses dosyası yüklenemedi ve cihazda Arapça ses motoru bulunamadı.'
          );
          onEnd?.();
        };

        this.setState('playing', resolvedKey);
        window.speechSynthesis.speak(utterance);
        return;
      }

      this.setState(
        'error',
        null,
        'Ses dosyası yüklenemedi. Lütfen internet bağlantınızı kontrol edin.'
      );
      onEnd?.();
    } catch {
      this.setState(
        'error',
        null,
        'Ses oynatılamadı. Lütfen bağlantınızı kontrol edin.'
      );
      onEnd?.();
    }
  }

  private playUrl(
    url: string,
    resolvedKey: string,
    onEnd?: () => void,
    onErrorFallback?: () => void
  ): void {
    try {
      const audio = new Audio();
      audio.preload = 'auto';
      this.currentAudio = audio;

      audio.onplaying = () => {
        if (this.currentAudio === audio) {
          this.setState('playing', resolvedKey);
        }
      };

      audio.onended = () => {
        if (this.currentAudio === audio) {
          this.currentAudio = null;
          this.setState('idle', null);
        }
        onEnd?.();
      };

      audio.onerror = () => {
        if (this.currentAudio === audio) {
          this.currentAudio = null;
        }
        if (onErrorFallback) {
          onErrorFallback();
        } else {
          this.setState(
            'error',
            null,
            'Ses dosyası yüklenemedi. Lütfen internet bağlantınızı kontrol edin.'
          );
          onEnd?.();
        }
      };

      audio.src = url;
      const playPromise = audio.play();
      if (playPromise !== undefined) {
        playPromise
          .then(() => {
            if (this.currentAudio === audio) {
              this.setState('playing', resolvedKey);
            }
          })
          .catch(() => {
            if (this.currentAudio === audio) {
              this.currentAudio = null;
            }
            if (onErrorFallback) {
              onErrorFallback();
            } else {
              this.setState(
                'error',
                null,
                'Ses dosyası oynatılamadı. Bağlantınızı kontrol edin.'
              );
              onEnd?.();
            }
          });
      }
    } catch {
      if (onErrorFallback) {
        onErrorFallback();
      } else {
        this.setState(
          'error',
          null,
          'Ses oynatma hatası oluştu.'
        );
        onEnd?.();
      }
    }
  }
}

export const audioPlayer = new QuranAudioPlayer();

/**
 * React hook subscribing to the singleton QuranAudioPlayer state
 * for instant loading spinners, active playing indicators, and error toasts.
 */
export function useAudioPlayerState(): AudioPlayerSnapshot {
  const [snapshot, setSnapshot] = useState<AudioPlayerSnapshot>(() =>
    audioPlayer.getSnapshot()
  );

  useEffect(() => {
    return audioPlayer.subscribe(setSnapshot);
  }, []);

  return snapshot;
}
