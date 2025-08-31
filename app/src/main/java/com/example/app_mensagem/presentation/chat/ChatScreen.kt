package com.example.app_mensagem.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.presentation.viewmodel.ChatViewModel
import com.example.app_mensagem.ui.theme.App_mensagemTheme
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, conversationId: String?) {
    val chatViewModel: ChatViewModel = viewModel()
    val uiState by chatViewModel.uiState.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var selectedMessageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(conversationId) {
        if (conversationId != null) {
            chatViewModel.loadMessages(conversationId)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.conversationTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Digite uma mensagem...") }
                )
                IconButton(onClick = {
                    if (conversationId != null && text.isNotBlank()) {
                        chatViewModel.sendMessage(conversationId, text)
                        text = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isSelected = selectedMessageId == message.id,
                        onLongClick = {
                            // Se o mesmo item for selecionado, deseleciona, senão seleciona o novo
                            selectedMessageId = if (selectedMessageId == message.id) null else message.id
                        },
                        onReaction = { emoji ->
                            if (conversationId != null) {
                                chatViewModel.onReactionClick(conversationId, message.id, emoji)
                            }
                            selectedMessageId = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onReaction: (String) -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isSentByCurrentUser = message.senderId == currentUserId
    val alignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        AnimatedVisibility(visible = isSelected) {
            EmojiPicker(onReaction = onReaction)
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = { /* Pode ser usado para selecionar a msg no futuro */ },
                    onLongClick = onLongClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Remove o efeito visual de clique
                )
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bubbleColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start
            ) {
                Text(text = message.text, textAlign = TextAlign.Start)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (isSentByCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIcon(message = message)
                    }
                }
            }
        }
        if (message.reactions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 2.dp)
            ) {
                message.reactions.entries.groupBy { it.value }
                    .forEach { (emoji, userEntries) ->
                        ReactionDisplay(emoji = emoji, count = userEntries.size)
                    }
            }
        }
    }
}

@Composable
fun EmojiPicker(onReaction: (String) -> Unit) {
    val emojis = listOf("👍", "❤️", "😂", "😯", "😢", "🙏")
    Surface(
        shape = CircleShape,
        tonalElevation = 4.dp,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            emojis.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onReaction(emoji) }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun ReactionDisplay(emoji: String, count: Int) {
    Box(
        modifier = Modifier
            .padding(top = 2.dp, end = 2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = "$emoji $count", fontSize = 12.sp)
    }
}

@Composable
fun MessageStatusIcon(message: Message) {
    val (icon, color) = when {
        message.status == "FAILED" -> Icons.Default.Error to MaterialTheme.colorScheme.error
        message.status == "SENDING" -> Icons.Default.Schedule to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        message.readTimestamp > 0 -> Icons.Default.DoneAll to Color.Blue // Cor de destaque para lido
        message.deliveredTimestamp > 0 -> Icons.Default.DoneAll to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        else -> Icons.Default.Check to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Icon(
        imageVector = icon,
        contentDescription = "Status da mensagem",
        tint = color,
        modifier = Modifier.size(14.dp)
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val messageDate = Date(timestamp)
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(messageDate)
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    App_mensagemTheme {
        ChatScreen(navController = NavController(LocalContext.current), conversationId = "preview_id")
    }
}