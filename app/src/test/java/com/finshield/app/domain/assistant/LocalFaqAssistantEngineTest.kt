package com.finshield.app.domain.assistant

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalFaqAssistantEngineTest {

    private lateinit var engine: LocalFaqAssistantEngine

    @Before
    fun setUp() {
        engine = LocalFaqAssistantEngine()
    }

    @Test
    fun answerQuestion_exactQuestionMatch() = runBlocking {
        val response = engine.answerQuestion("What is a personal loan?")
        assertFalse(response.isFallback)
        assertEquals("loan_personal", response.matchedFaqId)
        assertTrue(response.text.contains("unsecured"))
        assertTrue(response.relatedSuggestions.isNotEmpty())
    }

    @Test
    fun answerQuestion_keywordVariation() = runBlocking {
        val response = engine.answerQuestion("explain reducing balance emi")
        assertFalse(response.isFallback)
        assertEquals("interest_reducing", response.matchedFaqId)
        assertTrue(response.text.contains("outstanding principal"))
    }

    @Test
    fun answerQuestion_fuzzyTypoTolerance() = runBlocking {
        // Minor typos: "cibil skore" instead of "cibil score"
        val response = engine.answerQuestion("what affects cibil skore?")
        assertFalse(response.isFallback)
        assertEquals("cibil_factors", response.matchedFaqId)
        assertTrue(response.text.contains("Payment History"))
    }

    @Test
    fun answerQuestion_sensitiveCredentialWarning() = runBlocking {
        // Asking about OTP, PIN, password
        val response = engine.answerQuestion("Where should I enter my bank OTP and UPI PIN?")
        assertTrue(response.isSafetyAlert)
        assertTrue(response.text.contains("NEVER ask you for your internet banking passwords, OTPs, UPI PINs"))
    }

    @Test
    fun answerQuestion_unknownQuestionFallback() = runBlocking {
        val response = engine.answerQuestion("How do I make chocolate chip cookies with rocket fuel?")
        assertTrue(response.isFallback)
        assertTrue(response.text.contains("couldn't find a direct answer"))
        assertTrue(response.relatedSuggestions.isNotEmpty())
    }

    @Test
    fun getSuggestedQuestions_returnsItems() {
        val suggestions = engine.getSuggestedQuestions(FaqCategory.LOANS)
        assertTrue(suggestions.isNotEmpty())
        assertEquals(4, suggestions.size)
    }
}
