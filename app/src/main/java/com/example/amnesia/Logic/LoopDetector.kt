package com.example.amnesia.logic

import android.util.Log
import java.util.Locale

class LoopDetector {

    // --- RAM MEMORY (Stores only the last user input) ---
    private var lastInputText: String? = null
    private var lastTimestamp: Long = 0

    // --- CONSTANTS ---
    private val SIMILARITY_THRESHOLD = 0.45f
    private val TIME_WINDOW_MS = 45000L // 45 seconds

    // Words we ignore during comparison
    private val STOP_WORDS = setOf("is", "the", "a", "an", "to", "of", "you", "me", "please", "can", "what", "tell", "say")

    fun processInput(currentInput: String): LoopResult {
        val currentTime = System.currentTimeMillis()
        Log.d("LoopDetector", "Input: '$currentInput'")

        // 1. CHECK FOR LOOP (Logic: Is this similar to the LAST thing I heard?)
        if (lastInputText != null) {
            val timeDiff = currentTime - lastTimestamp

            // Only check if within the 45s window
            if (timeDiff <= TIME_WINDOW_MS) {
                val similarity = calculateSimilarity(currentInput, lastInputText!!)
                Log.d("LoopDetector", "Similarity: $similarity vs $lastInputText")

                if (similarity >= SIMILARITY_THRESHOLD) {
                    // 🔴 LOOP DETECTED
                    // We return the 'Repeated' result.
                    // We do NOT update memory, so the 'anchor' remains the original question.
                    return LoopResult.Repeated(
                        currentText = currentInput,
                        previousText = lastInputText!!,
                        secondsAgo = timeDiff / 1000
                    )
                }
            }
        }

        // 2. NEW INPUT (Logic: This is fresh, so it becomes the new 'Anchor')
        // Overwrite the previous memory
        lastInputText = currentInput
        lastTimestamp = currentTime

        return LoopResult.Recorded(currentInput)
    }

    // --- MATH HELPERS ---
    private fun calculateSimilarity(text1: String, text2: String): Float {
        val set1 = preprocess(text1)
        val set2 = preprocess(text2)
        val union = (set1 + set2).size
        if (union == 0) return 0f
        return set1.intersect(set2).size.toFloat() / union
    }

    private fun preprocess(text: String): Set<String> {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9 ]"), "")
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() && it !in STOP_WORDS }
            .toSet()
    }
}