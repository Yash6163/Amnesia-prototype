package com.example.amnesia.data

class ConvoStore {

    private val convoList = mutableListOf<Map<String, String>>()

    fun addConvo(ques: String, ans: String, time: String) {
        convoList.add(
            mapOf(
                "question" to ques.lowercase(),
                "answer" to ans,
                "time" to time
            )
        )
    }

    fun returnResultIfFound(ques: String): String? {
        val droppers = setOf(
            "is", "the", "what", "can", "you", "me", "tell", "a", "an", "it"
        )

        val trimmedWords = mutableListOf<String>()
        for (word in ques.lowercase().split(" ")) {
            if (word !in droppers) {
                trimmedWords.add(word)
            }
        }

        for (convo in convoList) {
            val verify = convo["question"]?.split(" ") ?: continue

            var similar = 0
            for (w in verify) {
                if (w in trimmedWords) {
                    similar++
                }
            }

            val union = (trimmedWords.toSet() union verify.toSet()).size
            if (union == 0) continue

            if (similar.toDouble() / union >= 0.1) {
                return convo["answer"]
            }
        }
        return null
    }
}