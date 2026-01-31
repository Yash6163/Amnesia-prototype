package com.example.amnesia.logic

sealed class LoopResult {

    data class Repeated(
        val currentText: String,
        val previousText: String,
        val secondsAgo: Long
    ) : LoopResult()

    data class Recorded(
        val text: String
    ) : LoopResult()
}
