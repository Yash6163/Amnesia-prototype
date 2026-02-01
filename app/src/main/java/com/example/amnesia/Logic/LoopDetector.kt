package com.example.amnesia.logic

import android.util.Log
import java.util.Locale

class LoopDetector {

    private var lastInputText: String? = null
    private var lastTimestamp: Long = 0

    private val SIMILARITY_THRESHOLD = 0.20f
    private val TIME_WINDOW_MS = 60000L
    private val STOP_WORDS = setOf("the", "a", "an", "to", "of")

    fun processInput(currentInput: String): LoopResult {
        val currentTime = System.currentTimeMillis()
        Log.d("LoopDetector", "Processing: '$currentInput'")

        if (lastInputText != null) {
            val timeDiff = currentTime - lastTimestamp
            if (timeDiff <= TIME_WINDOW_MS) {

                val currentTokens = preprocess(currentInput)
                val lastTokens = preprocess(lastInputText!!)

                val similarity = calculateSimilarity(currentTokens, lastTokens)

                if (similarity >= SIMILARITY_THRESHOLD) {
                    return LoopResult.Repeated(
                        currentText = currentInput,
                        previousText = lastInputText!!,
                        secondsAgo = timeDiff / 1000
                    )
                }
            }
        }

        lastInputText = currentInput
        lastTimestamp = currentTime
        return LoopResult.Recorded(currentInput)
    }

    private fun calculateSimilarity(set1: Set<String>, set2: Set<String>): Float {
        val union = (set1 + set2).size
        if (union == 0) return 0f
        val intersection = set1.intersect(set2).size
        return intersection.toFloat() / union
    }

    private fun preprocess(text: String): Set<String> {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9 ]"), "")
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() && it !in STOP_WORDS }
            .toSet()
    }
}
