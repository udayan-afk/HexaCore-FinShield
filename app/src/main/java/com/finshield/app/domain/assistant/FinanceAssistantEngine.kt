package com.finshield.app.domain.assistant

import kotlin.math.max
import kotlin.math.min

/**
 * Interface contract for FinShield Assistant engines.
 * Keeps matching and future AI providers decoupled from Compose UI.
 */
interface FinanceAssistantEngine {
    suspend fun answerQuestion(query: String): AssistantResponse
    fun getSuggestedQuestions(category: FaqCategory? = null): List<String>
    fun getAllCategories(): List<FaqCategory>
}

/**
 * Local FAQ matching implementation.
 * Performs robust token matching, keyword weighing, typo tolerance,
 * and strict sensitive-credential safety detection.
 */
class LocalFaqAssistantEngine(
    private val faqItems: List<FaqItem> = FaqKnowledgeBase.items,
) : FinanceAssistantEngine {

    private val stopWords = setOf(
        "what", "is", "a", "an", "the", "how", "does", "do", "can", "i", "to",
        "in", "for", "of", "and", "or", "my", "me", "are", "about", "with",
        "on", "by", "it", "be", "at", "from", "as", "if", "why", "should", "you",
    )

    private val sensitivePatterns = listOf(
        "otp", "one time password", "pin", "upi pin", "atm pin",
        "password", "cvv", "passcode", "secret", "card number", "bank password",
    )

    override suspend fun answerQuestion(query: String): AssistantResponse {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return AssistantResponse(
                text = "Please enter a question about loans, EMIs, CIBIL scores, banking safety, or FinShield features.",
                isFallback = true,
                relatedSuggestions = getPopularSuggestions(),
            )
        }

        val normalized = normalize(trimmed)

        // 1. Check for sensitive credential inquiries
        if (containsSensitiveRequest(normalized)) {
            return AssistantResponse(
                text = "⚠️ Financial Security Notice:\n\nFinShield, banks, and legitimate lenders will NEVER ask you for your internet banking passwords, OTPs, UPI PINs, ATM PINs, or card CVVs.\n\nPlease never share these credentials with anyone or enter them into unverified apps. If you suspect an unauthorized transaction, contact your bank immediately.",
                isSafetyAlert = true,
                relatedSuggestions = listOf(
                    "Why should I never share my OTP, UPI PIN or bank password?",
                    "What are common loan scams?",
                    "How can I verify a lender before borrowing?",
                ),
            )
        }

        // 2. Score FAQ candidates
        val queryTokens = extractTokens(normalized)
        var bestItem: FaqItem? = null
        var highestScore = 0.0

        for (item in faqItems) {
            val score = calculateMatchScore(normalized, queryTokens, item)
            if (score > highestScore) {
                highestScore = score
                bestItem = item
            }
        }

        // 3. Evaluate match threshold (minimum confidence of 0.28)
        if (bestItem != null && highestScore >= 0.28) {
            val related = FaqKnowledgeBase.getRelatedQuestions(bestItem)
            return AssistantResponse(
                text = bestItem.answer,
                matchedFaqId = bestItem.id,
                category = bestItem.category,
                relatedSuggestions = related.ifEmpty { getPopularSuggestions() },
            )
        }

        // 4. Fallback when no reliable match is found
        return AssistantResponse(
            text = "I couldn't find a direct answer to that in the FAQ knowledge base. As a local finance guide, I can help you with loans, EMI calculation, CIBIL scores, banking safety, and FinShield agreement analysis.",
            isFallback = true,
            relatedSuggestions = getPopularSuggestions(),
        )
    }

    override fun getSuggestedQuestions(category: FaqCategory?): List<String> {
        return if (category != null) {
            FaqKnowledgeBase.getQuestionsForCategory(category).take(4).map { it.question }
        } else {
            getPopularSuggestions()
        }
    }

    override fun getAllCategories(): List<FaqCategory> = FaqCategory.entries

    private fun getPopularSuggestions(): List<String> = listOf(
        "What is reducing-balance interest?",
        "What factors affect a credit score?",
        "What is the difference between interest rate and APR?",
        "How does Analyze Agreement work?",
        "What is a secured versus unsecured loan?",
        "Why should I never share my OTP, UPI PIN or bank password?",
    )

    private fun containsSensitiveRequest(normalized: String): Boolean {
        return sensitivePatterns.any { pattern ->
            normalized.contains(pattern)
        }
    }

    private fun calculateMatchScore(
        normalizedQuery: String,
        queryTokens: List<String>,
        item: FaqItem,
    ): Double {
        val normalizedQuestion = normalize(item.question)
        val questionTokens = extractTokens(normalizedQuestion)
        val keywords = item.keywords.map { normalize(it) }

        // Exact question match
        if (normalizedQuery == normalizedQuestion) return 1.0

        // Substring containment
        if (normalizedQuestion.contains(normalizedQuery) || normalizedQuery.contains(normalizedQuestion)) {
            return 0.90
        }

        var score = 0.0

        // Token overlap with question
        val matchingQuestionTokens = queryTokens.count { qToken ->
            questionTokens.any { it == qToken || isFuzzyMatch(qToken, it) }
        }
        if (queryTokens.isNotEmpty()) {
            val overlapRatio = matchingQuestionTokens.toDouble() / queryTokens.size.toDouble()
            score += overlapRatio * 0.55
        }

        // Keyword matches
        for (kw in keywords) {
            val kwTokens = extractTokens(kw)
            if (normalizedQuery.contains(kw)) {
                score += 0.40
            } else if (kwTokens.all { kt -> queryTokens.any { it == kt || isFuzzyMatch(it, kt) } }) {
                score += 0.30
            }
        }

        return min(score, 1.0)
    }

    private fun isFuzzyMatch(s1: String, s2: String): Boolean {
        if (s1 == s2) return true
        if (s1.length >= 4 && s2.length >= 4) {
            if (s1.startsWith(s2) || s2.startsWith(s1)) {
                if (kotlin.math.abs(s1.length - s2.length) <= 2) return true
            }
            val dist = levenshteinDistance(s1, s2)
            if (dist <= 1) return true
        }
        return false
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length
        var cost = IntArray(lhsLen + 1) { it }
        var newCost = IntArray(lhsLen + 1) { 0 }

        for (i in 1..rhsLen) {
            newCost[0] = i
            for (j in 1..lhsLen) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLen]
    }

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractTokens(normalized: String): List<String> {
        return normalized.split(" ")
            .filter { it.isNotBlank() && it !in stopWords }
    }
}
