package com.example.traductor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.traductor.databinding.ActivityMainBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

/**
 * Traductor de voz: micrófono -> texto -> traducción -> bocina.
 * Una de las dos lenguas siempre es español (español <-> inglés EE. UU./Reino
 * Unido, francés, alemán, portugués, italiano, chino, japonés, coreano o árabe).
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private data class Language(val name: String, val mlKitCode: String, val locale: Locale)

    private val languages = listOf(
        Language("Español", TranslateLanguage.SPANISH, Locale("es", "MX")),
        Language("English (US)", TranslateLanguage.ENGLISH, Locale.US),
        Language("English (UK)", TranslateLanguage.ENGLISH, Locale.UK),
        Language("Français", TranslateLanguage.FRENCH, Locale.FRANCE),
        Language("Deutsch", TranslateLanguage.GERMAN, Locale.GERMANY),
        Language("Português", TranslateLanguage.PORTUGUESE, Locale("pt", "BR")),
        Language("Italiano", TranslateLanguage.ITALIAN, Locale.ITALY),
        Language("中文", TranslateLanguage.CHINESE, Locale.SIMPLIFIED_CHINESE),
        Language("日本語", TranslateLanguage.JAPANESE, Locale.JAPAN),
        Language("한국어", TranslateLanguage.KOREAN, Locale.KOREA),
        Language("العربية", TranslateLanguage.ARABIC, Locale("ar"))
    )

    private lateinit var binding: ActivityMainBinding

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var isListening = false

    private var translator: Translator? = null
    private var translatorKey: String? = null

    // Idioma en el que está el último texto traducido (para el botón "Escuchar")
    private var lastTranslatedLanguage: Language? = null

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening() else setStatus(R.string.status_permission)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            languages.map { it.name }
        )
        binding.spinnerFrom.adapter = adapter
        binding.spinnerTo.adapter = adapter
        binding.spinnerFrom.setSelection(SPANISH_INDEX)
        binding.spinnerTo.setSelection(ENGLISH_INDEX)

        binding.spinnerFrom.onItemSelectedListener = spinnerListener(changedFrom = true)
        binding.spinnerTo.onItemSelectedListener = spinnerListener(changedFrom = false)

        binding.btnSwap.setOnClickListener { swapLanguages() }

        binding.btnMic.setOnClickListener {
            if (isListening) {
                recognizer?.stopListening()
                return@setOnClickListener
            }
            tts?.stop()
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) startListening() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        binding.btnListen.setOnClickListener {
            val text = binding.txtTranslated.text.toString()
            speak(text, lastTranslatedLanguage ?: toLanguage())
        }
    }

    // ---------------------------------------------------------------- idiomas

    private fun fromLanguage() = languages[binding.spinnerFrom.selectedItemPosition]
    private fun toLanguage() = languages[binding.spinnerTo.selectedItemPosition]

    private fun spinnerListener(changedFrom: Boolean) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            keepSpanishOnOneSide(changedFrom)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    /** Garantiza que exactamente uno de los dos idiomas sea español. */
    private fun keepSpanishOnOneSide(changedFrom: Boolean) {
        val from = binding.spinnerFrom.selectedItemPosition
        val to = binding.spinnerTo.selectedItemPosition
        val other = if (changedFrom) binding.spinnerTo else binding.spinnerFrom
        when {
            from != SPANISH_INDEX && to != SPANISH_INDEX -> other.setSelection(SPANISH_INDEX)
            from == SPANISH_INDEX && to == SPANISH_INDEX -> other.setSelection(ENGLISH_INDEX)
        }
    }

    private fun swapLanguages() {
        val from = binding.spinnerFrom.selectedItemPosition
        val to = binding.spinnerTo.selectedItemPosition
        binding.spinnerFrom.setSelection(to)
        binding.spinnerTo.setSelection(from)

        val original = binding.txtOriginal.text
        binding.txtOriginal.text = binding.txtTranslated.text
        binding.txtTranslated.text = original
        lastTranslatedLanguage = null
    }

    // ------------------------------------------------- micrófono (voz a texto)

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            setStatus(R.string.status_error_speech)
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(recognitionListener)
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, fromLanguage().locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        binding.txtOriginal.text = ""
        binding.txtTranslated.text = ""
        recognizer?.startListening(intent)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            updateMicButton()
            setStatus(R.string.status_listening)
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
            updateMicButton()
        }

        override fun onError(error: Int) {
            isListening = false
            updateMicButton()
            setStatus(R.string.status_error_speech)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!partial.isNullOrBlank()) binding.txtOriginal.text = partial
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            updateMicButton()
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (text.isNullOrBlank()) {
                setStatus(R.string.status_error_speech)
                return
            }
            binding.txtOriginal.text = text
            translate(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun updateMicButton() {
        binding.btnMic.setText(if (isListening) R.string.btn_stop else R.string.btn_mic)
    }

    // ------------------------------------------------------------ traducción

    private fun translatorFor(from: Language, to: Language): Translator {
        val key = "${from.mlKitCode}>${to.mlKitCode}"
        if (key != translatorKey) {
            translator?.close()
            translator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(from.mlKitCode)
                    .setTargetLanguage(to.mlKitCode)
                    .build()
            )
            translatorKey = key
        }
        return translator!!
    }

    private fun translate(text: String) {
        val from = fromLanguage()
        val to = toLanguage()
        if (from == to) {
            setStatus(R.string.status_same_language)
            return
        }
        val client = translatorFor(from, to)

        // La primera vez de cada idioma se descarga un modelo (~30 MB).
        setStatus(R.string.status_downloading)
        client.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                setStatus(R.string.status_translating)
                client.translate(text)
                    .addOnSuccessListener { translated ->
                        binding.txtTranslated.text = translated
                        lastTranslatedLanguage = to
                        setStatus(R.string.status_ready)
                        speak(translated, to) // salida automática por la bocina
                    }
                    .addOnFailureListener { setStatus(R.string.status_error_translate) }
            }
            .addOnFailureListener { setStatus(R.string.status_error_translate) }
    }

    // ------------------------------------------------- bocina (texto a voz)

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
    }

    private fun speak(text: String, language: Language) {
        if (text.isBlank()) return
        val engine = tts
        if (!ttsReady || engine == null) {
            setStatus(R.string.status_no_tts)
            return
        }
        var result = engine.setLanguage(language.locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            result = engine.setLanguage(Locale(language.locale.language))
        }
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            setStatus(R.string.status_no_tts)
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "traduccion")
    }

    // ---------------------------------------------------------------- varios

    private fun setStatus(@StringRes id: Int) {
        binding.txtStatus.setText(id)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        translator?.close()
        super.onDestroy()
    }

    private companion object {
        const val SPANISH_INDEX = 0
        const val ENGLISH_INDEX = 1
    }
}
