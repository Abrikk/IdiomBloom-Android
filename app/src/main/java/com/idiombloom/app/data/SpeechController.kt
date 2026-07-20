package com.idiombloom.app.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechController(context: Context) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            textToSpeech.language = Locale.SIMPLIFIED_CHINESE
            textToSpeech.setSpeechRate(0.82f)
        }
    }

    fun speak(text: String) {
        if (ready) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "idiom-$text")
        }
    }

    fun close() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
