package com.example.amnesia.logic

sealed class LoopResult {
    // 🟢 Case 1: New Input (Just acknowledge and store)
    data class Recorded(
        val text: String
    ) : LoopResult()

    // 🔴 Case 2: Repetition (Warn the user)
    data class Repeated(
        val currentText: String,
        val previousText: String,
        val secondsAgo: Long
    ) : LoopResult()
}