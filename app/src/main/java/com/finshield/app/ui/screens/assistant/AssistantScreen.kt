package com.finshield.app.ui.screens.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finshield.app.domain.assistant.ChatMessage
import com.finshield.app.domain.assistant.FaqCategory
import com.finshield.app.domain.assistant.MessageSender
import com.finshield.app.ui.components.ShieldLogo
import com.finshield.app.ui.screens.auth.StatusBarIcons
import com.finshield.app.ui.theme.AmberDark
import com.finshield.app.ui.theme.AmberTint
import com.finshield.app.ui.theme.AppBackground
import com.finshield.app.ui.theme.CardWhite
import com.finshield.app.ui.theme.DividerColor
import com.finshield.app.ui.theme.EmeraldButton
import com.finshield.app.ui.theme.EmeraldDark
import com.finshield.app.ui.theme.EmeraldSoft
import com.finshield.app.ui.theme.EmeraldTint
import com.finshield.app.ui.theme.InfoBlue
import com.finshield.app.ui.theme.InfoBlueTint
import com.finshield.app.ui.theme.Navy
import com.finshield.app.ui.theme.Outline
import com.finshield.app.ui.theme.Rose
import com.finshield.app.ui.theme.RoseTint
import com.finshield.app.ui.theme.TextHint
import com.finshield.app.ui.theme.TextPrimary
import com.finshield.app.ui.theme.TextSecondary

@Composable
fun AssistantScreen(
    onBack: () -> Unit,
    vm: AssistantViewModel = viewModel(),
) {
    StatusBarIcons(darkIcons = true)
    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Auto-scroll to latest message
    LaunchedEffect(state.messages.size, state.isTyping) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            AssistantTopBar(
                onBack = onBack,
                onClear = vm::requestClearHistory,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            // Category Filter Pills
            CategoryFilterBar(
                selectedCategory = state.selectedCategory,
                onSelectCategory = vm::selectCategory,
            )

            // Conversation Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(
                    items = state.messages,
                    key = { it.id },
                ) { msg ->
                    MessageBubble(
                        message = msg,
                        onSelectSuggestion = { question ->
                            vm.sendMessage(question)
                        },
                    )
                }

                if (state.isTyping) {
                    item(key = "typing_indicator") {
                        TypingIndicatorBubble()
                    }
                }
            }

            // Quick Suggestion Chips (if any available)
            if (state.suggestedQuestions.isNotEmpty()) {
                SuggestionChipsBar(
                    suggestions = state.suggestedQuestions,
                    onSelect = { question ->
                        vm.sendMessage(question)
                    },
                )
            }

            // Input Bar
            AssistantInputBar(
                value = state.inputText,
                onValueChange = vm::onInputChange,
                onSend = {
                    focusManager.clearFocus()
                    vm.sendMessage()
                },
            )
        }
    }

    // Clear Conversation Confirmation Dialog
    if (state.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = vm::dismissClearDialog,
            title = {
                Text("Clear conversation?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text(
                    "This will reset your conversation history with FinShield Assistant.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmClearHistory) {
                    Text("Clear", color = Rose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissClearDialog) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}

/**
 * Top App Bar for FinShield Assistant.
 */
@Composable
private fun AssistantTopBar(
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                )
            }

            ShieldLogo(size = 32.dp)

            Column {
                Text(
                    text = "FinShield Assistant",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
                Text(
                    text = "Your finance guide",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = EmeraldDark,
                )
            }
        }

        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = "Clear conversation",
                tint = TextSecondary,
            )
        }
    }
}

/**
 * Horizontally scrollable category filter pills.
 */
@Composable
private fun CategoryFilterBar(
    selectedCategory: FaqCategory?,
    onSelectCategory: (FaqCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryPill(
            label = "All Topics",
            isSelected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
        )
        FaqCategory.entries.forEach { cat ->
            CategoryPill(
                label = cat.displayName,
                isSelected = selectedCategory == cat,
                onClick = { onSelectCategory(cat) },
            )
        }
    }
    HorizontalDivider(color = DividerColor.copy(alpha = 0.6f))
}

@Composable
private fun CategoryPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) Navy else Color(0xFFF1F5F9)
    val textCol = if (isSelected) Color.White else TextSecondary
    val shape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .background(bg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = textCol,
        )
    }
}

/**
 * Message Bubble for user or assistant.
 */
@Composable
private fun MessageBubble(
    message: ChatMessage,
    onSelectSuggestion: (String) -> Unit,
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Row(
            modifier = Modifier.widthIn(max = 330.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(EmeraldTint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    ShieldLogo(size = 18.dp)
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier
                    .shadow(
                        elevation = if (isUser) 1.dp else 2.dp,
                        shape = if (isUser) {
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                        } else {
                            RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                        },
                    )
                    .background(
                        color = when {
                            isUser -> Navy
                            message.isSafetyAlert -> AmberTint
                            else -> CardWhite
                        },
                        shape = if (isUser) {
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                        } else {
                            RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                        },
                    )
                    .then(
                        if (!isUser && !message.isSafetyAlert) {
                            Modifier.border(1.dp, DividerColor, RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        } else if (message.isSafetyAlert) {
                            Modifier.border(1.dp, AmberDark.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (message.isSafetyAlert) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Security, null, tint = AmberDark, modifier = Modifier.size(16.dp))
                        Text(
                            text = "SECURITY ALERT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = AmberDark,
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 20.sp),
                    color = if (isUser) Color.White else if (message.isSafetyAlert) AmberDark else TextPrimary,
                )

                // Follow-up suggestions attached to assistant messages
                if (!isUser && message.relatedSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Related questions:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = EmeraldDark,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.relatedSuggestions.take(3).forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .clickable { onSelectSuggestion(suggestion) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "→ $suggestion",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Typing indicator.
 */
@Composable
private fun TypingIndicatorBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "dot1",
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 200), repeatMode = RepeatMode.Reverse),
        label = "dot2",
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, delayMillis = 400), repeatMode = RepeatMode.Reverse),
        label = "dot3",
    )

    Row(
        modifier = Modifier.padding(start = 38.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .background(CardWhite, RoundedCornerShape(14.dp))
                .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).alpha(alpha1).background(EmeraldDark, CircleShape))
            Box(Modifier.size(6.dp).alpha(alpha2).background(EmeraldDark, CircleShape))
            Box(Modifier.size(6.dp).alpha(alpha3).background(EmeraldDark, CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(
                text = "FinShield Assistant is typing…",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
            )
        }
    }
}

/**
 * Bottom suggestion bar with quick clickable chips.
 */
@Composable
private fun SuggestionChipsBar(
    suggestions: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.take(4).forEach { question ->
                Box(
                    modifier = Modifier
                        .background(EmeraldTint, RoundedCornerShape(12.dp))
                        .border(1.dp, EmeraldDark.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { onSelect(question) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = EmeraldDark,
                    )
                }
            }
        }
    }
}

/**
 * Input Bar for chatting.
 */
@Composable
private fun AssistantInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask about loans, EMIs, CIBIL...", style = MaterialTheme.typography.bodySmall, color = TextHint)
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldDark,
                    unfocusedBorderColor = Outline,
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                ),
            )

            val canSend = value.trim().isNotBlank()
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(if (canSend) EmeraldButton else Color(0xFFE2E8F0), CircleShape)
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send question",
                    tint = if (canSend) Color.White else TextHint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Disclaimer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Info, null, tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Educational guide only. Not personal financial advice or a loan approval guarantee.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = TextSecondary.copy(alpha = 0.7f),
            )
        }
    }
}
