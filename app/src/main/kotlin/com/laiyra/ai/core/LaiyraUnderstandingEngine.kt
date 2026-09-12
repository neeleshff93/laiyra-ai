package com.laiyra.ai.core

import java.util.Locale
import kotlin.math.min

enum class LaiyraIntent {
    GREETING,
    HOW_ARE_YOU,
    IDENTITY,
    THANKS,
    GOOD_MORNING,
    GOOD_NIGHT,
    HELP,
    UNKNOWN
}

data class LaiyraUnderstanding(
    val originalText: String,
    val cleanedText: String,
    val wakeWordDetected: Boolean,
    val intent: LaiyraIntent
)

object LaiyraUnderstandingEngine {

    /*
     * --------------------------------
     * WAKE WORDS
     * --------------------------------
     *
     * Speech recognition may hear:
     *
     * Laiyra
     * Lyra
     * Lara
     * Leera
     * Layra
     * Laira
     *
     * All are accepted.
     */

    private val wakeWords = listOf(
        "laiyra",
        "lyra",
        "laira",
        "layra",
        "lara",
        "leera",
        "leyra"
    )

    /*
     * --------------------------------
     * MAIN UNDERSTANDING FUNCTION
     * --------------------------------
     */

    fun understand(
        text: String
    ): LaiyraUnderstanding {

        val original =
            text.trim()

        val cleaned =
            normalize(original)

        val wakeDetected =
            containsWakeWord(cleaned)

        val command =
            removeWakeWord(cleaned)

        val intent =
            detectIntent(command)

        return LaiyraUnderstanding(
            originalText = original,
            cleanedText = command,
            wakeWordDetected = wakeDetected,
            intent = intent
        )
    }

    /*
     * --------------------------------
     * NORMALIZE TEXT
     * --------------------------------
     */

    private fun normalize(
        text: String
    ): String {

        return text
            .lowercase(Locale.ROOT)
            .replace(
                Regex("[^a-zA-Z0-9\\u0900-\\u097F ]"),
                " "
            )
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    /*
     * --------------------------------
     * WAKE WORD DETECTION
     * --------------------------------
     */

    private fun containsWakeWord(
        text: String
    ): Boolean {

        val words =
            text.split(" ")

        for (word in words) {

            if (word.isBlank()) {
                continue
            }

            for (wakeWord in wakeWords) {

                if (word == wakeWord) {
                    return true
                }

                /*
                 * Fuzzy matching.
                 *
                 * Example:
                 *
                 * lara -> laiyra
                 * leera -> laiyra
                 * lyra -> laiyra
                 */

                if (
                    similarity(
                        word,
                        wakeWord
                    ) >= 0.72
                ) {

                    return true
                }
            }
        }

        return false
    }

    /*
     * --------------------------------
     * REMOVE WAKE WORD
     * --------------------------------
     */

    private fun removeWakeWord(
        text: String
    ): String {

        val words =
            text
                .split(" ")
                .toMutableList()

        words.removeAll { word ->

            wakeWords.any { wakeWord ->

                word == wakeWord ||
                        similarity(
                            word,
                            wakeWord
                        ) >= 0.72
            }
        }

        return words
            .joinToString(" ")
            .trim()
    }

    /*
     * --------------------------------
     * INTENT DETECTION
     * --------------------------------
     */

    private fun detectIntent(
        text: String
    ): LaiyraIntent {

        if (text.isBlank()) {

            return LaiyraIntent.UNKNOWN
        }

        val normalized =
            normalizeCommonWords(text)

        return when {

            containsAny(
                normalized,
                "hello",
                "hi",
                "hey",
                "namaste",
                "नमस्ते"
            ) -> {

                LaiyraIntent.GREETING
            }

            containsAny(
                normalized,
                "how are you",
                "how r u",
                "how r you",
                "kaisi ho",
                "kaise ho",
                "kesi ho",
                "kese ho",
                "aap kaisi ho",
                "aap kaise ho"
            ) -> {

                LaiyraIntent.HOW_ARE_YOU
            }

            containsAny(
                normalized,
                "who are you",
                "what are you",
                "tum kon ho",
                "tum kaun ho",
                "aap kon ho",
                "aap kaun ho"
            ) -> {

                LaiyraIntent.IDENTITY
            }

            containsAny(
                normalized,
                "thank you",
                "thanks",
                "thank",
                "shukriya",
                "धन्यवाद"
            ) -> {

                LaiyraIntent.THANKS
            }

            containsAny(
                normalized,
                "good morning",
                "morning",
                "suprabhat",
                "शुभ प्रभात"
            ) -> {

                LaiyraIntent.GOOD_MORNING
            }

            containsAny(
                normalized,
                "good night",
                "night",
                "shubh ratri",
                "शुभ रात्रि"
            ) -> {

                LaiyraIntent.GOOD_NIGHT
            }

            containsAny(
                normalized,
                "help",
                "help me",
                "madad",
                "madad karo",
                "मदद"
            ) -> {

                LaiyraIntent.HELP
            }

            else -> {

                LaiyraIntent.UNKNOWN
            }
        }
    }

    /*
     * --------------------------------
     * COMMON HINGLISH NORMALIZATION
     * --------------------------------
     */

    private fun normalizeCommonWords(
        text: String
    ): String {

        var result =
            text

        val replacements =
            mapOf(

                "kesi" to "kaisi",

                "kese" to "kaise",

                "kr" to "kar",

                "kro" to "karo",

                "krna" to "karna",

                "h" to "hai",

                "hu" to "hoon",

                "mai" to "main",

                "m" to "main",

                "ap" to "aap",

                "aapko" to "aap ko",

                "btao" to "batao",

                "btana" to "batana",

                "pls" to "please",

                "plz" to "please"
            )

        for (
            (wrong, correct)
            in replacements
        ) {

            result =
                Regex(
                    "\\b${Regex.escape(wrong)}\\b"
                ).replace(
                    result,
                    correct
                )
        }

        return result
    }

    /*
     * --------------------------------
     * PHRASE MATCHING
     * --------------------------------
     */

    private fun containsAny(
        text: String,
        vararg phrases: String
    ): Boolean {

        for (phrase in phrases) {

            if (
                text.contains(
                    phrase.lowercase(
                        Locale.ROOT
                    )
                )
            ) {

                return true
            }
        }

        return false
    }

    /*
     * --------------------------------
     * TEXT SIMILARITY
     * --------------------------------
     */

    private fun similarity(
        first: String,
        second: String
    ): Double {

        if (
            first.isEmpty() ||
            second.isEmpty()
        ) {

            return 0.0
        }

        val distance =
            levenshteinDistance(
                first,
                second
            )

        val maxLength =
            maxOf(
                first.length,
                second.length
            )

        return 1.0 -
                (
                    distance.toDouble() /
                            maxLength.toDouble()
                    )
    }

    /*
     * --------------------------------
     * LEVENSHTEIN DISTANCE
     * --------------------------------
     */

    private fun levenshteinDistance(
        first: String,
        second: String
    ): Int {

        val previous =
            IntArray(
                second.length + 1
            ) {
                it
            }

        val current =
            IntArray(
                second.length + 1
            )

        for (
            i in 1..first.length
        ) {

            current[0] = i

            for (
                j in 1..second.length
            ) {

                val cost =
                    if (
                        first[i - 1] ==
                        second[j - 1]
                    ) {

                        0

                    } else {

                        1
                    }

                current[j] =
                    min(
                        min(
                            current[j - 1] + 1,
                            previous[j] + 1
                        ),
                        previous[j - 1] + cost
                    )
            }

            for (
                j in previous.indices
            ) {

                previous[j] =
                    current[j]
            }
        }

        return previous[
            second.length
        ]
    }
}