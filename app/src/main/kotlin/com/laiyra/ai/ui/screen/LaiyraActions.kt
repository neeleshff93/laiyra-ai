package com.laiyra.ai.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build

// ============================================================
// 🧠 SMART WORD DICTIONARY — 100+ synonyms
// ============================================================
object Words {

    val ON = listOf(
        "on", "chalu", "chaalu", "chaaloo", "start", "chalao", "chala do",
        "activate", "enable", "jalа", "jalao", "jlao",
        "चालू", "चालू करो", "जला", "जलाओ", "ऑन"
    )

    val OFF = listOf(
        "off", "band", "bandh", "band karo", "close", "stop",
        "deactivate", "disable", "bujha", "bujhao",
        "बंद", "बन्द", "बुझा", "बुझाओ", "ऑफ", "रोको"
    )

    val OPEN = listOf(
        "open", "kholo", "khol", "khol do", "kholo na", "launch", "start", "run",
        "खोलो", "खोल", "चालू करो", "ओपन"
    )

    val CLOSE = listOf(
        "close", "band karo", "band", "बंद करो", "बन्द करो", "exit", "kill", "stop"
    )

    val INCREASE = listOf(
        "increase", "badha", "badhao", "badha do", "up", "zyada", "tez",
        "बढ़ा", "बढ़ाओ", "तेज", "ऊपर"
    )

    val DECREASE = listOf(
        "decrease", "kam", "kam karo", "ghata", "ghatao", "down", "dheema",
        "कम", "कम करो", "घटा", "घटाओ", "धीमा", "नीचे"
    )

    // 🎯 Target keywords — kis cheez ke liye on/off
    val TORCH_TARGETS = listOf(
        "torch", "flashlight", "torchlight", "light", "batti", "bati",
        "टॉर्च", "बत्ती", "लाइट", "flash"
    )

    val VOLUME_TARGETS = listOf(
        "volume", "awaaz", "aawaz", "sound", "आवाज़", "आवाज", "वॉल्यूम"
    )

    val WIFI_TARGETS = listOf(
        "wifi", "wi-fi", "internet", "वाईफाई", "वाई-फाई"
    )

    val BT_TARGETS = listOf(
        "bluetooth", "bt", "ब्लूटूथ"
    )

    val BRIGHTNESS_TARGETS = listOf(
        "brightness", "screen light", "चमक", "ब्राइटनेस"
    )

    val CALL_VERBS = listOf(
        "call", "dial", "phone", "ring", "कॉल", "फ़ोन", "फोन", "मिलाओ"
    )

    // Common Hindi/English noise words to strip
    val NOISE = listOf(
        "karo", "kar", "do", "de", "please", "plz", "करो", "दो", "ना", "yaar",
        "haan", "ok", "okay", "जरा", "जी", "thoda"
    )
}

// ============================================================
// 🧠 SMART INTENT PARSER
// ============================================================
object IntentParser {

    /** Checks if text contains any word from the list */
    fun containsAny(text: String, words: List<String>): Boolean {
        val lower = text.lowercase()
        return words.any { lower.contains(it.lowercase()) }
    }

    /** Finds the best matching app name from remaining text */
    fun extractAppName(text: String): String {
        var t = text.lowercase().trim()

        // Remove leading verbs
        (Words.OPEN + Words.CLOSE).forEach { verb ->
            t = t.replace(verb.lowercase(), "").trim()
        }

        // Remove noise words
        Words.NOISE.forEach { noise ->
            t = t.replace(Regex("\\b${Regex.escape(noise.lowercase())}\\b"), "").trim()
        }

        // Remove punctuation
        t = t.replace(Regex("[.,!?।]+"), "").trim()

        return t
    }

    /** Which direction — ON or OFF? */
    fun detectDirection(text: String): String? {
        val t = text.lowercase()
        // OFF first (because 'band' contains no 'on', but 'off' contains 'off')
        if (Words.OFF.any { t.contains(it.lowercase()) }) return "OFF"
        if (Words.ON.any { t.contains(it.lowercase()) }) return "ON"
        return null
    }

    /** Which system target? */
    fun detectTarget(text: String): String? {
        val t = text.lowercase()
        if (Words.TORCH_TARGETS.any { t.contains(it.lowercase()) }) return "TORCH"
        if (Words.WIFI_TARGETS.any { t.contains(it.lowercase()) }) return "WIFI"
        if (Words.BT_TARGETS.any { t.contains(it.lowercase()) }) return "BLUETOOTH"
        if (Words.VOLUME_TARGETS.any { t.contains(it.lowercase()) }) return "VOLUME"
        if (Words.BRIGHTNESS_TARGETS.any { t.contains(it.lowercase()) }) return "BRIGHTNESS"
        return null
    }

    /** Is this a call request? */
    fun isCallRequest(text: String): Boolean {
        return Words.CALL_VERBS.any { text.lowercase().contains(it.lowercase()) }
    }

    /** Is this an app-open request? */
    fun isOpenAppRequest(text: String): Boolean {
        return Words.OPEN.any { text.lowercase().contains(it.lowercase()) }
    }

    /** Extract phone number or contact name */
    fun extractCallTarget(text: String): String {
        var t = text.lowercase().trim()
        Words.CALL_VERBS.forEach { t = t.replace(it.lowercase(), "") }
        Words.NOISE.forEach { t = t.replace(Regex("\\b${Regex.escape(it.lowercase())}\\b"), "") }
        return t.replace(Regex("[.,!?।]+"), "").trim()
    }
}

// ============================================================
// 📱 SMART APP LAUNCHER — Multi-strategy fuzzy matching
// ============================================================
class SmartAppLauncher(private val context: Context) {

    // Direct package mapping (fastest)
    private val directMap = mapOf(
        "whatsapp" to "com.whatsapp",
        "व्हाट्सएप" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "यूट्यूब" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "camera" to "com.android.camera",
        "कैमरा" to "com.android.camera",
        "settings" to "com.android.settings",
        "setting" to "com.android.settings",
        "instagram" to "com.instagram.android",
        "insta" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "fb" to "com.facebook.katana",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "telegram" to "org.telegram.messenger",
        "playstore" to "com.android.vending",
        "play store" to "com.android.vending",
        "gallery" to "com.google.android.apps.photos",
        "photos" to "com.google.android.apps.photos",
        "phone" to "com.android.dialer",
        "dialer" to "com.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "clock" to "com.google.android.deskclock",
        "calculator" to "com.google.android.calculator",
        "calendar" to "com.google.android.calendar",
        "contacts" to "com.google.android.contacts"
    )

    /**
     * Smart open — tries 4 strategies:
     * 1. Direct mapping
     * 2. Fuzzy match on installed app labels
     * 3. Contains match (word in label)
     * 4. Score-based best match
     */
    fun openApp(name: String): Boolean {
        if (name.isBlank()) return false

        val pm = context.packageManager
        val key = name.lowercase()
            .replace(" ", "")
            .replace("-", "")
            .trim()

        // ── Strategy 1: Direct mapping ──
        directMap[key]?.let { pkg ->
            pm.getLaunchIntentForPackage(pkg)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                return true
            }
        }

        // ── Strategy 2: Query all launchable apps ──
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchables = pm.queryIntentActivities(launcherIntent, 0)

        if (launchables.isEmpty()) return false

        // ── Strategy 3: Exact label match (normalized) ──
        for (info in launchables) {
            val label = info.loadLabel(pm).toString().lowercase()
                .replace(" ", "")
                .replace("-", "")
                .trim()
            if (label == key) {
                pm.getLaunchIntentForPackage(info.activityInfo.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                    return true
                }
            }
        }

        // ── Strategy 4: Contains match ──
        for (info in launchables) {
            val label = info.loadLabel(pm).toString().lowercase()
                .replace(" ", "")
                .trim()
            if (label.contains(key) || key.contains(label)) {
                pm.getLaunchIntentForPackage(info.activityInfo.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                    return true
                }
            }
        }

        // ── Strategy 5: Word-by-word match ──
        val words = key.split(" ").filter { it.length > 2 }
        for (info in launchables) {
            val label = info.loadLabel(pm).toString().lowercase()
            val matchCount = words.count { label.contains(it) }
            if (matchCount >= 1 && words.isNotEmpty()) {
                pm.getLaunchIntentForPackage(info.activityInfo.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                    return true
                }
            }
        }

        return false
    }
}
// ============================================================
// 💡 SMART SYSTEM ACTIONS — ON/OFF states (not toggle)
// ============================================================
class SmartSystemAction(private val context: Context) {

    // Torch state — actual on/off track karo
    private var torchOn = false

    fun setTorch(on: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE)
                    as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: return false

            // Agar already same state hai — kuch nahi karo
            if (torchOn == on) return true

            cm.setTorchMode(id, on)
            torchOn = on
            true
        } catch (e: Exception) {
            false
        }
    }

    // Volume — increase / decrease / set
    fun changeVolume(up: Boolean): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)

            val step = (max / 8).coerceAtLeast(1)
            val target = if (up) {
                (current + step).coerceAtMost(max)
            } else {
                (current - step).coerceAtLeast(0)
            }

            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            val percent = (target * 100 / max)

            if (up) "Volume badha diya — ab $percent%"
            else "Volume kam kar diya — ab $percent%"
        } catch (e: Exception) {
            "Volume change nahi ho paaya"
        }
    }

    // Silent mode
    fun setSilent(on: Boolean): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (on) {
                    am.ringerMode = AudioManager.RINGER_MODE_SILENT
                    "Silent mode on kar diya"
                } else {
                    am.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    "Ringer wapas on kar diya"
                }
            } else {
                "Silent mode is device pe support nahi hai"
            }
        } catch (e: Exception) {
            "Silent mode change nahi ho paaya — permission chahiye"
        }
    }

    // Battery
    fun getBattery(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE)
                    as android.os.BatteryManager
            val level = bm.getIntProperty(
                android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
            )
            when {
                level >= 80 -> "Battery $level% — full charged hai Master"
                level >= 50 -> "Battery $level% — theek hai"
                level >= 20 -> "Battery $level% — thoda charge karna padega"
                else -> "Battery sirf $level% — jaldi charge laga dijiye!"
            }
        } catch (e: Exception) {
            "Battery info nahi mili"
        }
    }
}

// ============================================================
// 🎯 SMART ACTION DISPATCHER — Sab kuch connect karta hai
// ============================================================
class SmartActionDispatcher(private val context: Context) {

    private val launcher = SmartAppLauncher(context)
    private val sys = SmartSystemAction(context)

    /**
     * Returns reply string if handled locally, else null (AI brain ko do).
     */
    fun tryHandle(text: String): String? {
        val raw = text.trim()
        val lower = raw.lowercase()

       // ─── TORCH ───
if (IntentParser.containsAny(lower, Words.TORCH_TARGETS)) {
    val dir = IntentParser.detectDirection(lower)
    return when (dir) {
        "ON" -> {
            if (sys.setTorch(true)) {
                pick(
                    "Torch on kar di Master",
                    "Batti jala di!",
                    "Flashlight chalu hai ab"
                )
            } else {
                "Torch on nahi ho paaya"
            }
        }
        "OFF" -> {
            if (sys.setTorch(false)) {
                pick(
                    "Torch off kar di",
                    "Batti bujha di",
                    "Light band hai ab"
                )
            } else {
                "Torch off nahi ho paaya"
            }
        }
        else -> {
            val newState = sys.setTorch(!getTorchStateFromContext())
            if (newState) "Torch toggle kar di" else "Torch control nahi hua"
        }
    }
}

        // ─── VOLUME ───
        if (IntentParser.containsAny(lower, Words.VOLUME_TARGETS)) {
            if (IntentParser.containsAny(lower, Words.INCREASE)) {
                return sys.changeVolume(true)
            }
            if (IntentParser.containsAny(lower, Words.DECREASE)) {
                return sys.changeVolume(false)
            }
            if (IntentParser.containsAny(lower, Words.OFF)) {
                return sys.setSilent(true)
            }
            if (IntentParser.containsAny(lower, Words.ON)) {
                return sys.setSilent(false)
            }
        }

        // ─── BATTERY ───
        if (lower.contains("battery") || lower.contains("बैटरी") ||
            lower.contains("charge kitna") || lower.contains("charge status")
        ) {
            return sys.getBattery()
        }

        // ─── SILENT MODE ───
        if (lower.contains("silent") || lower.contains("साइलेंट") ||
            lower.contains("mute")
        ) {
            val dir = IntentParser.detectDirection(lower) ?: "ON"
            return sys.setSilent(dir == "ON")
        }

        // ─── CALL ───
        if (IntentParser.isCallRequest(lower)) {
            val who = IntentParser.extractCallTarget(raw)
            if (who.isNotBlank()) {
                return try {
                    val uri = Uri.parse("tel:$who")
                    val intent = Intent(Intent.ACTION_CALL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    pick(
                        "$who ko call laga rahi hoon Master",
                        "$who ko dial kar rahi hoon",
                        "Ringing $who..."
                    )
                } catch (e: Exception) {
                    // Fallback: dialer kholo
                    try {
                        val uri = Uri.parse("tel:$who")
                        val dial = Intent(Intent.ACTION_DIAL, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(dial)
                        "Call permission nahi hai — dialer khol diya"
                    } catch (e2: Exception) {
                        "Call nahi lag paayi Master"
                    }
                }
            }
        }

        // ─── OPEN APP ───
        if (IntentParser.isOpenAppRequest(lower)) {
            val appName = IntentParser.extractAppName(raw)

            if (appName.isBlank()) {
                return "Kaunsi app kholni hai Master?"
            }

            return if (launcher.openApp(appName)) {
                pick(
                    "Haan, $appName khol rahi hoon",
                    "Ji Master, $appName chalu kar rahi hoon",
                    "$appName khul raha hai...",
                    "Done Master, $appName on hai"
                )
            } else {
                pick(
                    "Sorry Master, $appName nahi mila. Naam check kijiye?",
                    "Hmm, $appName naam se koi app nahi mili",
                    "$appName installed nahi hai shayad"
                )
            }
        }

        return null
    }

    // Helper — current torch state guess karo (approx)
    private fun getTorchStateFromContext(): Boolean = false
}

// ============================================================
// 💾 CONVERSATION MEMORY
// ============================================================
object ConversationMemory {
    val messages = androidx.compose.runtime.mutableStateListOf<Pair<Boolean, String>>()

    fun addUser(text: String) { messages.add(true to text) }
    fun addAi(text: String) { messages.add(false to text) }

    fun history(): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        var i = 0
        while (i < messages.size - 1) {
            if (messages[i].first && !messages[i + 1].first) {
                result.add(messages[i].second to messages[i + 1].second)
                i += 2
            } else i++
        }
        return result
    }
}
