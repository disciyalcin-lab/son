package com.example.audio

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AudioPlayerManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var exoPlayer: ExoPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var hasArabicTts = false
    private var hasExplicitMaleArabicVoice = false
    private var selectedMaleVoice: Voice? = null

    private var pendingFallbackArabicText: String? = null
    private var backupStreamUrls: MutableList<String> = mutableListOf()
    private var bufferingTimeoutJob: Job? = null
    private var ttsSafetyJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    init {
        mainHandler.post {
            initExoPlayer()
        }
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "TTS initialization failed: ${e.message}")
        }
    }

    private fun initExoPlayer() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(C.USAGE_MEDIA)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    _isLoading.value = true
                                }
                                Player.STATE_READY -> {
                                    bufferingTimeoutJob?.cancel()
                                    _isLoading.value = false
                                    _isPlaying.value = playWhenReady
                                }
                                Player.STATE_ENDED -> {
                                    bufferingTimeoutJob?.cancel()
                                    _isLoading.value = false
                                    _isPlaying.value = false
                                    _currentUrl.value = null
                                    pendingFallbackArabicText = null
                                    backupStreamUrls.clear()
                                }
                                Player.STATE_IDLE -> {
                                    _isLoading.value = false
                                    _isPlaying.value = false
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                            _isPlaying.value = isPlayingNow
                            if (isPlayingNow) {
                                bufferingTimeoutJob?.cancel()
                                _isLoading.value = false
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            Log.w("AudioPlayerManager", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}")
                            bufferingTimeoutJob?.cancel()
                            if (backupStreamUrls.isNotEmpty()) {
                                val nextUrl = backupStreamUrls.removeAt(0)
                                startExoPlayerStream(nextUrl)
                            } else {
                                val fallbackText = pendingFallbackArabicText
                                pendingFallbackArabicText = null
                                _isLoading.value = false
                                _isPlaying.value = false
                                _currentUrl.value = null
                                if (!fallbackText.isNullOrBlank()) {
                                    speakArabic(fallbackText)
                                }
                            }
                        }
                    })
                }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to initialize ExoPlayer", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return
            val arSaudiLocale = Locale.forLanguageTag("ar-SA")
            val arGeneralLocale = Locale.forLanguageTag("ar")

            var result = tts.setLanguage(arSaudiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts.setLanguage(arGeneralLocale)
            }

            val available = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
            hasArabicTts = available
            isTtsInitialized = true

            configureMaleQuranicVoice(tts)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                    _isLoading.value = false
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                    _isLoading.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                    _isLoading.value = false
                }
            })
        }
    }

    /**
     * Selects a Male Arabic voice (Kârî / Muallim erkek sesi) from installed TTS voices
     * and configures a deep baritone pitch and measured tartil speech rate.
     */
    private fun configureMaleQuranicVoice(tts: TextToSpeech) {
        try {
            val allVoices = tts.voices ?: emptySet()
            val arabicVoices = allVoices.filter { voice ->
                voice.locale.language.equals("ar", ignoreCase = true)
            }

            // Known female voice markers in Google TTS & OEM engines to exclude
            val femaleMarkers = listOf(
                "female", "#female", "ar-xa-x-arc", "ar-xa-x-arz", "ar-xa-x-arf",
                "laila", "mariam", "salma", "amira", "zeina", "hoda", "fatima"
            )
            // Known male voice markers in Google TTS (ard, are, arb) & OEM engines
            val maleMarkers = listOf(
                "ar-xa-x-ard", "ar-xa-x-are", "ar-xa-x-arb",
                "male", "#male", "maged", "majed", "tarik", "tariq", "naayf", "omar", "hamza"
            )

            val explicitMaleVoice = arabicVoices
                .filter { v ->
                    val nameLower = v.name.lowercase(Locale.ROOT)
                    val featuresLower = v.features?.joinToString(" ")?.lowercase(Locale.ROOT).orEmpty()
                    femaleMarkers.none { marker -> nameLower.contains(marker) || featuresLower.contains(marker) } &&
                        maleMarkers.any { marker -> nameLower.contains(marker) || featuresLower.contains(marker) }
                }
                .sortedWith(
                    compareBy<Voice> { it.isNetworkConnectionRequired }
                        .thenByDescending { it.quality }
                )
                .firstOrNull()

            val nonFemaleArabicVoice = explicitMaleVoice ?: arabicVoices
                .filter { v ->
                    val nameLower = v.name.lowercase(Locale.ROOT)
                    val featuresLower = v.features?.joinToString(" ")?.lowercase(Locale.ROOT).orEmpty()
                    femaleMarkers.none { marker -> nameLower.contains(marker) || featuresLower.contains(marker) }
                }
                .firstOrNull()

            if (explicitMaleVoice != null) {
                selectedMaleVoice = explicitMaleVoice
                hasExplicitMaleArabicVoice = true
                tts.voice = explicitMaleVoice
                // Resonant, calm male Qari/Muallim register and measured tartil cadence
                tts.setPitch(0.82f)
                tts.setSpeechRate(0.75f)
            } else {
                if (nonFemaleArabicVoice != null) {
                    selectedMaleVoice = nonFemaleArabicVoice
                    tts.voice = nonFemaleArabicVoice
                }
                hasExplicitMaleArabicVoice = false
                // Shift default system voice fundamental frequency into a deep male baritone register
                tts.setPitch(0.60f)
                tts.setSpeechRate(0.74f)
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Could not configure male Arabic TTS voice: ${e.message}")
            try {
                tts.setPitch(0.60f)
                tts.setSpeechRate(0.74f)
            } catch (_: Exception) {}
        }
    }

    /**
     * Speaks the Arabic word or verse in a deep, measured Male Quranic (Muallim) voice.
     * Falls back to streaming authentic male Qari audio if TTS is unavailable.
     */
    fun speakArabic(text: String, fallbackUrl: String? = null) {
        stop()
        if (text.isBlank()) return

        val tts = textToSpeech
        if (hasArabicTts && tts != null) {
            configureMaleQuranicVoice(tts)
            _isLoading.value = false
            _isPlaying.value = true

            val utteranceId = "quran_male_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            val status = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (status != TextToSpeech.SUCCESS) {
                if (!fallbackUrl.isNullOrBlank()) {
                    play(fallbackUrl)
                } else {
                    _isPlaying.value = false
                }
            } else {
                val estimatedDurationMs = (text.length * 140L).coerceIn(1800L, 9000L)
                ttsSafetyJob?.cancel()
                ttsSafetyJob = mainScope.launch {
                    delay(estimatedDurationMs)
                    if (!isExoPlayerActive()) {
                        _isPlaying.value = false
                    }
                }
            }
        } else if (!fallbackUrl.isNullOrBlank()) {
            play(fallbackUrl)
        } else {
            _isPlaying.value = true
            mainScope.launch {
                delay(800)
                _isPlaying.value = false
            }
        }
    }

    /**
     * Streams authentic Male Quran Qari recitation (Mahmoud Khalil Al-Husary / Mishary Rashid Alafasy)
     * using Jetpack Media3 ExoPlayer, and automatically falls back to the Male Quranic TTS voice
     * if offline or unreachable.
     */
    fun play(url: String, fallbackArabicText: String? = null) {
        stop()
        if (url.isBlank()) {
            if (!fallbackArabicText.isNullOrEmpty()) {
                speakArabic(fallbackArabicText)
            }
            return
        }

        pendingFallbackArabicText = fallbackArabicText
        backupStreamUrls.clear()

        // Extract 6-digit surah+ayah code (e.g. 001001.mp3) to prioritize classic male Qaris used on Quran pages:
        // 1. Sheikh Mahmoud Khalil Al-Husary (Muallim / Mushaf male voice)
        // 2. Sheikh Mishary Rashid Alafasy
        // 3. verses.quran.com official CDN mirror
        val fileMatch = Regex("(\\d{6})\\.mp3$").find(url)
        val primaryUrl = if (fileMatch != null) {
            val code = fileMatch.groupValues[1]
            val husaryUrl = "https://everyayah.com/data/Husary_128kbps/$code.mp3"
            val alafasyUrl = "https://everyayah.com/data/Alafasy_128kbps/$code.mp3"
            val quranComMirror = "https://verses.quran.com/Alafasy/mp3/$code.mp3"
            backupStreamUrls.add(alafasyUrl)
            backupStreamUrls.add(quranComMirror)
            husaryUrl
        } else {
            url
        }

        startExoPlayerStream(primaryUrl)
    }

    private fun startExoPlayerStream(streamUrl: String) {
        _currentUrl.value = streamUrl
        _isLoading.value = true

        bufferingTimeoutJob?.cancel()
        bufferingTimeoutJob = mainScope.launch {
            delay(3500)
            if (_isLoading.value && !isExoPlayerActive()) {
                // Stream timed out in restricted network; fall back to male Quranic TTS voice
                val fallbackText = pendingFallbackArabicText
                if (!fallbackText.isNullOrBlank()) {
                    speakArabic(fallbackText)
                } else {
                    stop()
                }
            }
        }

        mainHandler.post {
            try {
                if (exoPlayer == null) {
                    initExoPlayer()
                }
                exoPlayer?.let { player ->
                    player.stop()
                    player.clearMediaItems()
                    val mediaItem = MediaItem.fromUri(streamUrl)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.playWhenReady = true
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Failed to stream audio with ExoPlayer for $streamUrl", e)
                bufferingTimeoutJob?.cancel()
                val fallbackText = pendingFallbackArabicText
                pendingFallbackArabicText = null
                _isLoading.value = false
                _isPlaying.value = false
                _currentUrl.value = null
                if (!fallbackText.isNullOrEmpty()) {
                    speakArabic(fallbackText)
                }
            }
        }
    }

    private fun isExoPlayerActive(): Boolean {
        return exoPlayer?.isPlaying == true
    }

    fun stop() {
        bufferingTimeoutJob?.cancel()
        ttsSafetyJob?.cancel()
        pendingFallbackArabicText = null
        backupStreamUrls.clear()
        mainHandler.post {
            try {
                exoPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.clearMediaItems()
                }
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Error stopping ExoPlayer: ${e.message}")
            }
        }
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        _isPlaying.value = false
        _isLoading.value = false
        _currentUrl.value = null
    }

    fun release() {
        stop()
        mainHandler.post {
            try {
                exoPlayer?.release()
                exoPlayer = null
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Error releasing ExoPlayer: ${e.message}")
            }
        }
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (_: Exception) {}
    }
}
