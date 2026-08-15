package com.crossk.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Manages optional sound effects for key actions.
 * All sounds are optional and can be disabled in settings.
 */
class SoundManager(context: Context) {

    private var soundPool: SoundPool? = null
    private var loaded = false
    var enabled: Boolean = true

    // Sound IDs
    private var parseCompleteId: Int = 0
    private var graphConnectId: Int = 0
    private var postcardSaveId: Int = 0
    private var xpGainId: Int = 0

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            loaded = status == 0
        }
    }

    /**
     * Play the "file parsed successfully" sound.
     */
    fun playParseComplete() {
        playIfEnabled(parseCompleteId)
    }

    /**
     * Play the "graph connection formed" sound.
     */
    fun playGraphConnect() {
        playIfEnabled(graphConnectId)
    }

    /**
     * Play the "postcard saved" sound.
     */
    fun playPostcardSave() {
        playIfEnabled(postcardSaveId)
    }

    /**
     * Play the "XP gained" sound.
     */
    fun playXpGain() {
        playIfEnabled(xpGainId)
    }

    private fun playIfEnabled(soundId: Int) {
        if (enabled && loaded && soundId > 0 && soundPool != null) {
            soundPool?.play(soundId, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loaded = false
    }
}
