package com.example

import com.example.data.model.ContactEntity
import com.example.t9.T9SearchEngine
import org.junit.Assert.*
import org.junit.Test

class DialerEngineUnitTest {

    @Test
    fun testT9NameToDigitsConversion() {
        val converted = T9SearchEngine.nameToDigits("Alex")
        // A -> 2, L -> 5, E -> 3, X -> 9
        assertEquals("2539", converted)

        val bob = T9SearchEngine.nameToDigits("Bob")
        // B -> 2, O -> 6, B -> 2
        assertEquals("262", bob)
    }

    @Test
    fun testT9SearchMatches() {
        val contacts = listOf(
            ContactEntity(
                displayName = "Alex Vance",
                phoneNumber = "+1 555-019-2831",
                normalizedNumber = "15550192831"
            ),
            ContactEntity(
                displayName = "Elena Rostova",
                phoneNumber = "+1 555-442-8921",
                normalizedNumber = "15554428921"
            ),
            ContactEntity(
                displayName = "Kai Tanaka",
                phoneNumber = "+1 555-773-6102",
                normalizedNumber = "15557736102"
            )
        )

        // Query "253" (matches "ALE" in Alex)
        val resultsAlex = T9SearchEngine.search(contacts, "253")
        assertTrue(resultsAlex.isNotEmpty())
        assertEquals("Alex Vance", resultsAlex[0].contact.displayName)
        assertTrue(resultsAlex[0].matchedByName)

        // Query "524" (matches "KAI" -> 5 2 4)
        val resultsKai = T9SearchEngine.search(contacts, "524")
        assertTrue(resultsKai.isNotEmpty())
        assertEquals("Kai Tanaka", resultsKai[0].contact.displayName)

        // Query by number substring "773"
        val resultsNumber = T9SearchEngine.search(contacts, "773")
        assertTrue(resultsNumber.isNotEmpty())
        assertEquals("Kai Tanaka", resultsNumber[0].contact.displayName)
    }

    @Test
    fun testPrefixMatchingPattern() {
        val cleanNumber = "8005551234"
        val prefix = "800"
        assertTrue(cleanNumber.startsWith(prefix))

        val nonBlocked = "5551234567"
        assertFalse(nonBlocked.startsWith(prefix))
    }
}
