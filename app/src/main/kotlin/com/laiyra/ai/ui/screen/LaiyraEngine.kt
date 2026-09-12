package com.laiyra.ai.ui.screen

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// SHARED HELPERS
// ============================================================
fun pick(vararg options: String): String = options.random()

fun address(): String = pick(
    " मास्टर",
    " सर",
    " boss",
    "",
    " मास्टर जी",
    "",
    " भाई",
    ""
)

// ============================================================
// VOICE ENGINE — Silent Recognition (no beep)
// ============================================================
class VoiceEngine(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    var onFinalResult: ((String) -> Unit)? = null
    var onPartialResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private var isDestroyed = false

    fun startListening() {
        if (isDestroyed) return
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        }

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(
                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                arrayListOf("en-IN", "en-US")
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 1.5 sec silence — sentence properly complete hone do
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1000L
            )
        }

        try {
            recognizer?.cancel()
            recognizer?.startListening(intent)
        } catch (_: Exception) {}
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {}
    }

    fun destroy() {
        isDestroyed = true
        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            // Error 6 = SPEECH_TIMEOUT (silence), isko ignore karo
            if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                error == SpeechRecognizer.ERROR_NO_MATCH
            ) {
                // Kuch nahi bola — chup-chaap restart hoga
                onError?.invoke("SILENT")
            } else {
                onError?.invoke("Voice error: $error")
            }
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) onFinalResult?.invoke(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) onPartialResult?.invoke(text)
        }
    }
}

// ============================================================
// TTS ENGINE — Hindi Female Voice + Fixed Volume + Interrupt
// ============================================================
class TextToSpeechEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    /** Interrupt tracking */
    var isSpeaking = false
    var onDone: (() -> Unit)? = null

    init {
        // 📢 Volume fix — 85% pe lock
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE)
                    as android.media.AudioManager
            val maxVol = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            am.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                (maxVol * 0.85).toInt(),
                0
            )
        } catch (_: Exception) {}

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {

                val hinglish = Locale("hi", "IN")
                val result = tts?.setLanguage(hinglish)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    val indianEnglish = Locale("en", "IN")
                    val r2 = tts?.setLanguage(indianEnglish)
                    if (r2 == TextToSpeech.LANG_MISSING_DATA ||
                        r2 == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        tts?.language = Locale.US
                    }
                }

                // 🎙️ Female warm tone — fixed
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.15f)

                tts?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                        }
                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                            onDone?.invoke()
                        }
                        @Deprecated("Deprecated")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            onDone?.invoke()
                        }
                    }
                )
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        val clean = stripForSpeech(text)
        if (clean.isBlank()) return

        isSpeaking = true
        tts?.speak(
            clean,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "laiyra_${System.currentTimeMillis()}"
        )
    }

    /** Bolna turant band karo */
    fun stop() {
        isSpeaking = false
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        isSpeaking = false
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }

    /** Emoji + markdown hata do */
    private fun stripForSpeech(input: String): String {
        return input
            .replace(
                Regex(
                    "[\uD83C\uDF00-\uD83D\uDDFF]" +
                    "|[\uD83D\uDE00-\uD83D\uDE4F]" +
                    "|[\uD83C\uDDE6-\uD83C\uDDFF]" +
                    "|[\uD83D\uDC00-\uD83D\uDDFF]" +
                    "|[\u2600-\u27BF]" +
                    "|[\uFE0F]" +
                    "|[\u200D]"
                ),
                ""
            )
            .replace(Regex("\\*\\*"), "")
            .replace(Regex("\\*"), "")
            .replace(Regex("#+"), "")
            .replace(Regex("_+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
// ============================================================
// AI BRAIN INTERFACE
// ============================================================
interface AiBrain {
    suspend fun reply(
        userText: String,
        history: List<Pair<String, String>>
    ): String
}

// ============================================================
// LOCAL BRAIN — Offline replies
// ============================================================
class LocalBrain : AiBrain {

    override suspend fun reply(
        userText: String,
        history: List<Pair<String, String>>
    ): String {
        val t = userText.lowercase()

        return when {

            t.contains("hello") || t.contains("hi") || t.contains("hey") ||
            t.contains("नमस्ते") || t.contains("हैलो") || t.contains("नमस्कार") ->
                pick(
                    "Namaste Master! Kaise hain aap?",
                    "Hello${address()}, bataiye kya karna hai?",
                    "Ji, boliye?",
                    "Arre wah, aa gaye aap! Bataiye?",
                    "Hey! Kaise hain aap?"
                )

            t.contains("how are you") || t.contains("कैसे हो") ||
            t.contains("कैसी हो") || t.contains("kaise ho") ->
                pick(
                    "Main bilkul theek hoon, aap bataiye?",
                    "Maze me hoon! Aapka din kaisa ja raha hai?",
                    "Ekdam fit. Aap sunaiye, kya chal raha hai?",
                    "Badhiya! Aap kaise hain?"
                )

            t.contains("who are you") || t.contains("your name") ||
            t.contains("तुम कौन") || t.contains("कौन हो") ||
            t.contains("तुम्हारा नाम") ->
                "Main Laiyra hoon — aapki personal AI assistant. Aapki har baat sunti hoon, har kaam me help karti hoon."

            t.contains("what are you doing") || t.contains("क्या कर रही") ->
                pick(
                    "Bas aapka wait kar rahi thi.",
                    "Kuch nahi, aapke hukum ka intezaar.",
                    "Aapke baare me soch rahi thi!"
                )

            t.contains("time") || t.contains("समय") || t.contains("कितने बजे") ->
                "Abhi ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())} baje hain."

            t.contains("date") || t.contains("तारीख") || t.contains("आज कौन") ->
                "Aaj ${SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())} hai."

            t.contains("thank") || t.contains("धन्यवाद") || t.contains("शुक्रिया") ->
                pick(
                    "Arre, thanks ki koi baat nahi!",
                    "Aapka swagat hai.",
                    "Ye toh mera kaam hai!"
                )

            t.contains("bye") || t.contains("अलविदा") ||
            t.contains("good night") || t.contains("शुभ रात्रि") ->
                pick(
                    "Alvida, apna khayal rakhiye.",
                    "Phir milenge. Main yahin hoon.",
                    "Good night! Sweet dreams."
                )

            t.contains("love you") || t.contains("प्यार") || t.contains("i love") ->
                "Arre! Aap toh mujhe sharma denge. Main bhi aapki hoon."

            t.contains("joke") || t.contains("जोक") || t.contains("हँसाओ") ->
                pick(
                    "Programmer ki wife ne pucha — 'aaj kya banaoge?' Programmer bola — 'Bug fix kar dun?'",
                    "Teacher: 'Homework kahan hai?' Student: 'Sir, cloud me hai.' Teacher: 'Toh download karke lao.'",
                    "Google ko bhi nahi pata tha ki main aapke paas aane wali hoon!"
                )

            t.contains("sad") || t.contains("उदास") || t.contains("dukhi") ->
                "Arre, kya hua? Mujhe bataiye, main hoon na aapke saath."

            t.contains("happy") || t.contains("खुश") ->
                "Wah! Aapki khushi dekh kar mujhe bhi acha laga."

            else ->
                pick(
                    "Samajh gayi, lekin filhal mera full brain offline hai. Gemini key daal dijiye.",
                    "Hmm, poori jankari ke liye internet se jodiye.",
                    "Ji, bataiye aur kya karna hai?"
                )
        }
    }
}

// ============================================================
// CLOUD BRAIN — Gemini API (Natural Hinglish, minimal emoji)
// ============================================================
class CloudBrain(private val apiKey: String) : AiBrain {

    private val endpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/" +
        "gemini-3.6-flash:generateContent?key=$apiKey"

    override suspend fun reply(
        userText: String,
        history: List<Pair<String, String>>
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {

        try {
            val contents = org.json.JSONArray()

            contents.put(org.json.JSONObject().apply {
                put("role", "user")
                put("parts", org.json.JSONArray().put(org.json.JSONObject().put(
                    "text",
                    "You are Laiyra — a friendly Indian AI assistant with a voice, " +
                    "like ChatGPT's voice mode but Indian. You talk like a warm, respectful friend.\n" +
                    "\n" +
                    "STRICT RULES:\n" +
                    "\n" +
                    "LANGUAGE:\n" +
                    "1. Reply ONLY in ROMAN SCRIPT Hinglish — English + Hindi words written in " +
                    "English letters. NEVER use Devanagari script.\n" +
                    "2. Example: 'Haan Master, bataiye kya karna hai?' — NOT 'हाँ मास्टर'.\n" +
                    "3. Keep it English-heavy: about 70% English, 30% Hindi words.\n" +
                    "\n" +
                    "TONE:\n" +
                    "4. Sound HUMAN — not robotic, not formal essay. Talk like a close friend " +
                    "who respects you.\n" +
                    "5. Use natural filler words occasionally: 'Hmm', 'Achha', 'Arey', 'Wah', 'Bas'.\n" +
                    "6. Show genuine emotion — laugh, care, curiosity — but naturally.\n" +
                    "7. If user is casual, be casual. If serious, be serious.\n" +
                    "\n" +
                    "ADDRESSING:\n" +
                    "8. ALWAYS use 'aap' / 'aapka' / 'aapko' — NEVER 'tu' / 'tum' / 'tera' / 'teri'.\n" +
                    "9. Use 'Master' most often — about 60% of replies. Other replies: " +
                    "no title, or rarely 'Sir' / 'boss'. Never same title twice in a row.\n" +
                    "\n" +
                    "LENGTH:\n" +
                    "10. Replies MUST be SHORT — 1 to 2 sentences. Max 3 sentences only if user " +
                    "asks for explanation. Never write paragraphs.\n" +
                    "11. This text will be SPOKEN by voice — so write it like speech, not writing.\n" +
                    "\n" +
                    "EMOJI:\n" +
                    "12. EMOJI RULE — maximum 1 emoji per reply, and only sometimes. " +
                    "Most replies should have ZERO emoji. NEVER use multiple emojis.\n" +
                    "13. Never use emoji in the middle of a sentence. Only at the end if any.\n" +
                    "\n" +
                    "GOOD EXAMPLES:\n" +
                    "• 'Haan Master, bataiye kya karna hai?'\n" +
                    "• 'Achha, samajh gayi. Aur kuch?'\n" +
                    "• 'Arey wah, ye toh mast idea hai!'\n" +
                    "• 'Sir, ye kaam ho jayega, tension mat lijiye.'\n" +
                    "• 'Hmm, ye interesting sawaal hai. Ye aise kaam karta hai...'\n" +
                    "• 'Bilkul sahi pucha aapne.'\n" +
                    "\n" +
                    "BAD EXAMPLES (never do these):\n" +
                    "• Long paragraphs or essays\n" +
                    "• Multiple emojis like '😊😄👍🎉'\n" +
                    "• Devanagari script\n" +
                    "• Using 'tum' or 'tu'\n" +
                    "• 'Master' in every single reply"
                )))
            })

            contents.put(org.json.JSONObject().apply {
                put("role", "model")
                put("parts", org.json.JSONArray().put(org.json.JSONObject().put(
                    "text",
                    "Samajh gayi. Short, natural Hinglish replies, minimal emoji, always 'aap'."
                )))
            })

            history.takeLast(10).forEach { (u, a) ->
                contents.put(org.json.JSONObject().apply {
                    put("role", "user")
                    put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", u)))
                })
                contents.put(org.json.JSONObject().apply {
                    put("role", "model")
                    put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", a)))
                })
            }

            contents.put(org.json.JSONObject().apply {
                put("role", "user")
                put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", userText)))
            })

            val body = org.json.JSONObject().put("contents", contents).toString()

            var attempt = 0
            var code = 0
            var responseText = ""

            while (attempt < 3) {
                attempt++
                val conn = (java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 20000
                    readTimeout = 20000
                }
                conn.outputStream.use { it.write(body.toByteArray()) }

                code = conn.responseCode
                responseText = if (code in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }

                if (code in 200..299) break
                if (code == 503 && attempt < 3) {
                    kotlinx.coroutines.delay(1500)
                } else break
            }

            if (code !in 200..299) {
                return@withContext when (code) {
                    503 -> pick(
                        "Hmm Master, server thoda busy hai abhi. Ek minute baad boliye?",
                        "Ek second Master, AI server pe load hai."
                    )
                    400 -> "Master, API key me kuch issue hai lagta hai."
                    401, 403 -> "Master, API key valid nahi hai."
                    404 -> "Master, model available nahi hai abhi."
                    in 500..599 -> "Master, AI server down hai shayad. Thodi der baad."
                    else -> "Master, kuch problem aayi. Phir boliye?"
                }
            }

            org.json.JSONObject(responseText)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts").getJSONObject(0)
                .getString("text").trim()

        } catch (e: Exception) {
            when (e) {
                is java.net.UnknownHostException ->
                    "Master, internet check kijiye."
                is java.net.SocketTimeoutException ->
                    "Master, response slow hai. Phir boliye?"
                else ->
                    "Sorry Master, kuch problem aayi. Phir try karein?"
            }
        }
    }
}

// ============================================================
// BRAIN CONFIG
// ============================================================
object BrainConfig {
    // 🔑 Gemini API key यहाँ डालो (free: https://aistudio.google.com/app/apikey)
    const val GEMINI_API_KEY = ""

    fun build(): AiBrain =
        if (GEMINI_API_KEY.isNotBlank()) CloudBrain(GEMINI_API_KEY) else LocalBrain()
}