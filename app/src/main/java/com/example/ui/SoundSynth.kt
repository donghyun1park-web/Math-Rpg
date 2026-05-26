package com.example.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object SoundSynth {
    private const val SAMPLE_RATE = 22050
    private var isMuted = false
    private var bgmJob: Job? = null
    private var currentBgmType: String? = null // "MAP", "BATTLE", null

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopBgm()
        } else {
            // Bgm will resume or can be restarted by ViewModel
        }
    }

    fun isMuted(): Boolean = isMuted

    // Helper to play a raw tone buffer safely on static mode
    private fun playBuffer(buffer: ShortArray) {
        if (isMuted) return
        try {
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer.size * 2,
                AudioTrack.MODE_STATIC
            )
            track.write(buffer, 0, buffer.size)
            track.play()
            
            // Auto release after the length of the audio track has finished
            GlobalScope.launch(Dispatchers.IO) {
                delay((buffer.size * 1000L) / SAMPLE_RATE + 150L)
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // Ignore already released/stopped exception
                }
            }
        } catch (e: Exception) {
            Log.e("SoundSynth", "Error playing wave buffer", e)
        }
    }

    // Audio SFX Generators
    fun playStep() {
        val durationMs = 50
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        // Quick upward chirp for stepping sound
        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val freq = 450f + (t * 800f) 
            val envelope = (1f - (i.toFloat() / size))
            buffer[i] = (sin(2.0 * Math.PI * freq * t) * 11000 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    fun playClick() {
        val durationMs = 30
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val freq = 820f
            val envelope = (1f - (i.toFloat() / size))
            buffer[i] = (sin(2.0 * Math.PI * freq * t) * 7500 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    fun playCorrect() {
        // High ascending double-tone chime: nice bright happy sound
        val durationMs = 300
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        val split = size / 2
        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val freq = if (i < split) 587.33f else 880.00f // D5 -> A5 ascending
            val currentProgress = if (i < split) i.toFloat() / split else (i - split).toFloat() / split
            val envelope = 1f - currentProgress
            buffer[i] = (sin(2.0 * Math.PI * freq * t) * 13000 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    fun playIncorrect() {
        // Low vibrating buzz sound
        val durationMs = 450
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val tremolo = 1f + 0.35f * sin(2.0 * Math.PI * 40.0 * t).toFloat()
            val freq = 138.59f // Db3
            val envelope = 1f - (i.toFloat() / size)
            
            val sineVal = sin(2.0 * Math.PI * freq * t) * tremolo
            val squareVal = if (sineVal > 0) 1.0 else -1.0
            buffer[i] = ((sineVal * 0.3 + squareVal * 0.7) * 9800 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    fun playSwordSlash() {
        // Dynamic swift sword slash curve
        val durationMs = 280
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val currentFraction = i.toFloat() / size
            val freq = 2200f - (currentFraction * 1950f)
            val envelope = 1f - currentFraction
            
            val noise = (Math.random() * 2.0 - 1.0) * 0.25
            buffer[i] = ((sin(2.0 * Math.PI * freq * t) * 0.75 + noise) * 15000 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    fun playFireball() {
        // Warm roaring explosing flame spell
        val durationMs = 460
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val currentFraction = i.toFloat() / size
            val freq = 140f + sin(2.0 * Math.PI * 30.0 * t).toFloat() * 40f
            val envelope = 1f - currentFraction
            
            val noise = (Math.random() * 2.0 - 1.0) * 0.75
            buffer[i] = ((sin(2.0 * Math.PI * freq * t) * 0.25 + noise * 0.75) * 15000 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    fun playChestOpen() {
        // Retro sweet arpeggio scale: C4 -> E4 -> G4 -> C5 -> E5
        val durationMs = 650
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        val notes = floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f, 659.25f)
        val noteCount = notes.size
        val segment = size / noteCount

        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val noteIdx = (i / segment).coerceIn(0, noteCount - 1)
            val freq = notes[noteIdx]
            val stepFraction = (i % segment).toFloat() / segment
            val envelope = 1f - stepFraction
            
            buffer[i] = (sin(2.0 * Math.PI * freq * t) * 12000 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    fun playLevelUp() {
        // Triumphant 8-bit hero fanfare
        val durationMs = 1300
        val size = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
        val buffer = ShortArray(size)
        
        // Progression: C4 -> F4 -> G4 -> C5
        val notes = floatArrayOf(261.63f, 349.23f, 392.00f, 523.25f)
        val segment = size / 4
        
        for (i in 0 until size) {
            val t = i.toFloat() / SAMPLE_RATE
            val noteIdx = (i / segment).coerceIn(0, 3)
            val freq = notes[noteIdx]
            
            val rawSine = sin(2.0 * Math.PI * freq * t)
            val pulseVal = if (rawSine > 0.12) 1.0 else -1.0
            
            val stepFraction = (i % segment).toFloat() / segment
            val envelope = if (noteIdx == 3) {
                1.0f - ((i - (segment * 3)).toFloat() / segment)
            } else {
                1.0f - stepFraction * 0.15f
            }
            
            buffer[i] = (pulseVal * 8500 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    // BG MUSIC ENGINE PLAYERS
    fun startBgm(type: String) {
        if (isMuted) return
        if (currentBgmType == type && bgmJob?.isActive == true) return
        
        stopBgm()
        currentBgmType = type
        
        bgmJob = GlobalScope.launch(Dispatchers.Default) {
            try {
                val bgmTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    4096 * 4,
                    AudioTrack.MODE_STREAM
                )
                bgmTrack.play()

                if (type == "MAP") {
                    // Soft forest exploration lullaby arpeggio melody (loopable)
                    val melody = floatArrayOf(261.63f, 293.66f, 329.63f, 392.00f, 440.00f, 392.00f, 329.63f, 293.66f)
                    var melodyIdx = 0
                    
                    while (isActive && !isMuted && currentBgmType == "MAP") {
                        val freq = melody[melodyIdx]
                        val noteLengthMs = 450
                        val bufSize = (SAMPLE_RATE * (noteLengthMs / 1000f)).toInt()
                        val soundBuffer = ShortArray(bufSize)
                        
                        for (i in 0 until bufSize) {
                            val t = i.toFloat() / SAMPLE_RATE
                            val env = 1f - (i.toFloat() / bufSize)
                            val s1 = sin(2.0 * Math.PI * freq * t)
                            val s2 = sin(2.0 * Math.PI * (freq * 1.5f) * t) * 0.25f // Harmonizing fifth
                            soundBuffer[i] = ((s1 + s2) * 5500 * env).toInt().toShort()
                        }
                        
                        bgmTrack.write(soundBuffer, 0, soundBuffer.size)
                        melodyIdx = (melodyIdx + 1) % melody.size
                        
                        delay(noteLengthMs.toLong() - 20)
                    }
                } else if (type == "BATTLE") {
                    // High-adrenaline fast tension loop
                    val bassLine = floatArrayOf(110.00f, 116.54f, 130.81f, 116.54f, 110.00f, 110.00f, 130.81f, 146.83f)
                    var stepIdx = 0
                    
                    while (isActive && !isMuted && currentBgmType == "BATTLE") {
                        val freq = bassLine[stepIdx]
                        val noteLengthMs = 300
                        val bufSize = (SAMPLE_RATE * (noteLengthMs / 1000f)).toInt()
                        val soundBuffer = ShortArray(bufSize)
                        
                        for (i in 0 until bufSize) {
                            val t = i.toFloat() / SAMPLE_RATE
                            val env = 1f - (i.toFloat() / bufSize)
                            val rawVal = sin(2.0 * Math.PI * freq * t)
                            val squareVal = if (rawVal > 0.0) 1.0 else -1.0
                            
                            val beatTick = if (stepIdx % 2 == 0 && i < bufSize * 0.15) {
                                (Math.random() * 2.0 - 1.0) * 0.45
                            } else 0.0
                            
                            soundBuffer[i] = ((squareVal * 0.35 + beatTick) * 7500 * env).toInt().toShort()
                        }
                        
                        bgmTrack.write(soundBuffer, 0, soundBuffer.size)
                        stepIdx = (stepIdx + 1) % bassLine.size
                        
                        delay(noteLengthMs.toLong() - 20)
                    }
                }

                try {
                    bgmTrack.stop()
                    bgmTrack.release()
                } catch (e: Exception) {
                    // Ignore
                }
            } catch (e: Exception) {
                Log.e("SoundSynth", "Bgm loop crash", e)
            }
        }
    }

    fun stopBgm() {
        currentBgmType = null
        bgmJob?.cancel()
        bgmJob = null
    }
}
