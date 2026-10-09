package com.example.t9

import com.example.data.model.ContactEntity

data class T9MatchResult(
    val contact: ContactEntity,
    val matchedByName: Boolean,
    val matchedRange: IntRange?,
    val score: Int
)

object T9SearchEngine {

    private val CHAR_TO_DIGIT = HashMap<Char, Char>().apply {
        put('a', '2'); put('b', '2'); put('c', '2')
        put('d', '3'); put('e', '3'); put('f', '3')
        put('g', '4'); put('h', '4'); put('i', '4')
        put('j', '5'); put('k', '5'); put('l', '5')
        put('m', '6'); put('n', '6'); put('o', '6')
        put('p', '7'); put('q', '7'); put('r', '7'); put('s', '7')
        put('t', '8'); put('u', '8'); put('v', '8')
        put('w', '9'); put('x', '9'); put('y', '9'); put('z', '9')
    }

    /**
     * Converts a string name into a sequence of T9 digits. Non-alphabetic chars keep their char or ' '
     */
    fun nameToDigits(name: String): String {
        val sb = StringBuilder(name.length)
        for (ch in name.lowercase()) {
            val digit = CHAR_TO_DIGIT[ch]
            if (digit != null) {
                sb.append(digit)
            } else if (ch.isDigit()) {
                sb.append(ch)
            } else {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    /**
     * Filters and ranks contacts based on typed T9 digits query.
     */
    fun search(contacts: List<ContactEntity>, query: String): List<T9MatchResult> {
        if (query.isEmpty() || query.contains('*') || query.contains('#')) {
            return emptyList()
        }
        val cleanQuery = query.filter { it.isDigit() }
        if (cleanQuery.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<T9MatchResult>()

        for (contact in contacts) {
            val nameDigits = nameToDigits(contact.displayName)
            val cleanNumber = contact.phoneNumber.filter { it.isDigit() }

            // 1. Check Name T9 Match
            val nameIdx = nameDigits.indexOf(cleanQuery)
            var matchedByName = false
            var matchedRange: IntRange? = null
            var score = 0

            if (nameIdx >= 0) {
                matchedByName = true
                matchedRange = nameIdx until (nameIdx + cleanQuery.length)
                // Score boosts
                score = when {
                    nameIdx == 0 -> 1000 // Exact name prefix
                    nameDigits.getOrNull(nameIdx - 1) == ' ' -> 800 // Word boundary prefix
                    else -> 400 // Substring match
                }
            }

            // 2. Check Number Match
            val numberIdx = cleanNumber.indexOf(cleanQuery)
            if (numberIdx >= 0) {
                val numberScore = if (numberIdx == 0) 500 else 200
                if (numberScore > score) {
                    score = numberScore
                    matchedByName = false
                    matchedRange = numberIdx until (numberIdx + cleanQuery.length)
                }
            }

            if (score > 0) {
                // Add frequency bonus
                val totalScore = score + (contact.callCount * 10) + (if (contact.isStarred) 50 else 0)
                results.add(
                    T9MatchResult(
                        contact = contact,
                        matchedByName = matchedByName,
                        matchedRange = matchedRange,
                        score = totalScore
                    )
                )
            }
        }

        return results.sortedByDescending { it.score }
    }
}
