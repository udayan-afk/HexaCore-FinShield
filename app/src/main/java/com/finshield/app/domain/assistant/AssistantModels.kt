package com.finshield.app.domain.assistant

import java.util.UUID

/**
 * High-level categories for financial FAQs.
 */
enum class FaqCategory(val displayName: String, val description: String) {
    LOANS("Loans", "Personal, secured vs unsecured loans, tenure, and application process"),
    EMI_AND_INTEREST("EMI & Interest", "EMI calculation, reducing vs flat rate, APR, and fees"),
    CIBIL_AND_CREDIT("CIBIL & Credit", "Credit scores, factors, disputes, and report maintenance"),
    BANKING_SAFETY("Banking & Safety", "KYC, disbursal, avoiding scams, and credential protection"),
    FINSHIELD_FEATURES("FinShield Features", "Agreement analysis, risk flags, PDF reports, and eligibility"),
}

/**
 * An item in the FAQ knowledge base.
 */
data class FaqItem(
    val id: String,
    val category: FaqCategory,
    val question: String,
    val answer: String,
    val keywords: List<String>,
    val relatedQuestionIds: List<String> = emptyList(),
)

/**
 * Sender of a chat message.
 */
enum class MessageSender {
    USER,
    ASSISTANT,
}

/**
 * Message in the conversation history.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedSuggestions: List<String> = emptyList(),
    val isSafetyAlert: Boolean = false,
)

/**
 * Structured response from the assistant engine.
 */
data class AssistantResponse(
    val text: String,
    val matchedFaqId: String? = null,
    val category: FaqCategory? = null,
    val relatedSuggestions: List<String> = emptyList(),
    val isSafetyAlert: Boolean = false,
    val isFallback: Boolean = false,
)
