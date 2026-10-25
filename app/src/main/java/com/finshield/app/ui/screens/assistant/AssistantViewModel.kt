package com.finshield.app.ui.screens.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finshield.app.domain.assistant.AssistantResponse
import com.finshield.app.domain.assistant.ChatMessage
import com.finshield.app.domain.assistant.FaqCategory
import com.finshield.app.domain.assistant.FinanceAssistantEngine
import com.finshield.app.domain.assistant.LocalFaqAssistantEngine
import com.finshield.app.domain.assistant.MessageSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val suggestedQuestions: List<String> = emptyList(),
    val selectedCategory: FaqCategory? = null,
    val inputText: String = "",
    val isTyping: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
)

class AssistantViewModel(
    private val engine: FinanceAssistantEngine = LocalFaqAssistantEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AssistantUiState(
            messages = listOf(createInitialGreeting()),
            suggestedQuestions = engine.getSuggestedQuestions(null),
        )
    )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun selectCategory(category: FaqCategory?) {
        val nextCategory = if (_uiState.value.selectedCategory == category) null else category
        _uiState.value = _uiState.value.copy(
            selectedCategory = nextCategory,
            suggestedQuestions = engine.getSuggestedQuestions(nextCategory),
        )
    }

    fun sendMessage(query: String? = null) {
        val textToSend = (query ?: _uiState.value.inputText).trim()
        if (textToSend.isBlank()) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = textToSend,
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            inputText = "",
            isTyping = true,
        )

        viewModelScope.launch {
            // Natural brief delay for smooth interaction
            delay(350)
            val response: AssistantResponse = engine.answerQuestion(textToSend)

            val assistantMessage = ChatMessage(
                sender = MessageSender.ASSISTANT,
                text = response.text,
                relatedSuggestions = response.relatedSuggestions,
                isSafetyAlert = response.isSafetyAlert,
            )

            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + assistantMessage,
                suggestedQuestions = response.relatedSuggestions.ifEmpty {
                    engine.getSuggestedQuestions(_uiState.value.selectedCategory)
                },
                isTyping = false,
            )
        }
    }

    fun requestClearHistory() {
        _uiState.value = _uiState.value.copy(showClearConfirmDialog = true)
    }

    fun dismissClearDialog() {
        _uiState.value = _uiState.value.copy(showClearConfirmDialog = false)
    }

    fun confirmClearHistory() {
        _uiState.value = _uiState.value.copy(
            messages = listOf(createInitialGreeting()),
            suggestedQuestions = engine.getSuggestedQuestions(null),
            selectedCategory = null,
            inputText = "",
            isTyping = false,
            showClearConfirmDialog = false,
        )
    }

    companion object {
        fun createInitialGreeting(): ChatMessage = ChatMessage(
            sender = MessageSender.ASSISTANT,
            text = "Hello! I'm FinShield Assistant, your personal finance guide.\n\nAsk me anything about loans, EMI calculations, CIBIL scores, banking safety, or FinShield agreement reviews. Select a suggested topic below or type your question.",
            relatedSuggestions = listOf(
                "What is reducing-balance interest?",
                "What factors affect a credit score?",
                "What is the difference between interest rate and APR?",
                "Why should I never share my OTP, UPI PIN or bank password?",
            ),
        )
    }
}
