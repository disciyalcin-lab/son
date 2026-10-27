// Web Audio / HTML5 Audio + Arabic Male TTS Fallback Service
// Uses Mishary Rashid Alafasy / Mahmoud Khalil Al-Husary authentic Quranic audio with male voice fallback

class QuranAudioPlayer {
  private currentAudio: HTMLAudioElement | null = null;

  public stop(): void {
    if (this.currentAudio) {
      this.currentAudio.pause();
      this.currentAudio.currentTime = 0;
      this.currentAudio = null;
    }
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
  }

  public playWord(arabicText: string, fallbackUrl?: string, onEnd?: () => void): void {
    this.stop();

    // Speak isolated word clearly with male Arabic TTS if available, or fallback to audio URL
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      const voices = window.speechSynthesis.getVoices();
      const arabicVoice =
        voices.find((v) => v.lang.startsWith('ar') && /male|majed|naayf|tarik|hamed|maged/i.test(v.name)) ||
        voices.find((v) => v.lang.startsWith('ar'));

      if (arabicVoice) {
        const utterance = new SpeechSynthesisUtterance(arabicText);
        utterance.voice = arabicVoice;
        utterance.lang = arabicVoice.lang || 'ar-SA';
        utterance.rate = 0.78;
        utterance.pitch = 0.85; // Deep male recitation resonance
        utterance.onend = () => onEnd?.();
        utterance.onerror = () => {
          if (fallbackUrl) this.playUrl(fallbackUrl, onEnd);
          else onEnd?.();
        };
        window.speechSynthesis.speak(utterance);
        return;
      }
    }

    if (fallbackUrl) {
      this.playUrl(fallbackUrl, onEnd);
    } else {
      onEnd?.();
    }
  }

  public playAyah(audioUrl: string, arabicText?: string, onEnd?: () => void): void {
    this.stop();
    this.playUrl(audioUrl, onEnd, () => {
      if (arabicText) {
        this.playWord(arabicText, undefined, onEnd);
      } else {
        onEnd?.();
      }
    });
  }

  private playUrl(url: string, onEnd?: () => void, onErrorFallback?: () => void): void {
    try {
      const audio = new Audio(url);
      this.currentAudio = audio;
      audio.onended = () => {
        this.currentAudio = null;
        onEnd?.();
      };
      audio.onerror = () => {
        this.currentAudio = null;
        if (onErrorFallback) onErrorFallback();
        else onEnd?.();
      };
      audio.play().catch(() => {
        this.currentAudio = null;
        if (onErrorFallback) onErrorFallback();
        else onEnd?.();
      });
    } catch {
      if (onErrorFallback) onErrorFallback();
      else onEnd?.();
    }
  }
}

export const audioPlayer = new QuranAudioPlayer();
