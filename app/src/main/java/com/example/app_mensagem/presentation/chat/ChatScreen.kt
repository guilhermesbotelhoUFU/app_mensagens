package com.example.app_mensagem.presentation.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.presentation.viewmodel.ChatViewModel
import com.example.app_mensagem.ui.theme.App_mensagemTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(navController: NavController, conversationId: String?) {
    val chatViewModel: ChatViewModel = viewModel()
    val uiState by chatViewModel.uiState.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showStickerSheet by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> chatViewModel.onMediaSelected(uri, "IMAGE") }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> chatViewModel.onMediaSelected(uri, "VIDEO") }
    )

    LaunchedEffect(conversationId) {
        if (conversationId != null) {
            chatViewModel.loadMessages(conversationId)
        }
    }

    LaunchedEffect(uiState.chatItems.size) {
        if (uiState.searchQuery.isBlank() && uiState.chatItems.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatItems.size - 1)
        }
    }

    Scaffold(
        topBar = {
            val isGroup = uiState.conversation?.isGroup ?: false
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { chatViewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Buscar na conversa...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.onPrimary,
                                focusedTextColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onPrimary)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = isGroup) {
                                if (conversationId != null) {
                                    navController.navigate("group_info/$conversationId")
                                }
                            }
                        ) {
                            AsyncImage(
                                model = uiState.conversation?.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                                contentDescription = "Foto de Perfil de ${uiState.conversationTitle}",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(uiState.conversationTitle)
                        }
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            chatViewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar Busca", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar Mensagem", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = uiState.mediaToSendUri != null) {
                    Box(modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
                        .height(80.dp)
                        .width(80.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uiState.mediaToSendUri),
                            contentDescription = "Mídia selecionada",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (uiState.mediaType == "VIDEO") {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.Center).size(32.dp)
                            )
                        }
                        IconButton(
                            onClick = { chatViewModel.onMediaSelected(null, "") },
                            modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remover mídia", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Anexar Imagem")
                    }
                    IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Anexar Vídeo")
                    }
                    IconButton(onClick = { showStickerSheet = true }) {
                        Icon(Icons.Default.Mood, contentDescription = "Enviar Sticker")
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Digite uma mensagem...") },
                        enabled = uiState.mediaToSendUri == null
                    )
                    IconButton(onClick = {
                        if (conversationId != null) {
                            chatViewModel.sendMessage(conversationId, text)
                            text = ""
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            AnimatedVisibility(visible = uiState.pinnedMessage != null && !isSearchActive) {
                uiState.pinnedMessage?.let { PinnedMessageView(message = it) }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(
                        items = uiState.chatItems,
                        key = { item ->
                            when (item) {
                                is ChatItem.DateHeader -> item.date
                                is ChatItem.MessageItem -> item.message.id
                            }
                        }
                    ) { item ->
                        when (item) {
                            is ChatItem.DateHeader -> {
                                DateHeader(date = item.date)
                            }
                            is ChatItem.MessageItem -> {
                                MessageBubble(
                                    message = item.message,
                                    searchQuery = uiState.searchQuery,
                                    isSelected = selectedMessageId == item.message.id,
                                    isGroup = uiState.conversation?.isGroup ?: false,
                                    groupMembers = uiState.groupMembers,
                                    onLongClick = {
                                        selectedMessageId = if (selectedMessageId == item.message.id) null else item.message.id
                                    },
                                    onReaction = { emoji ->
                                        if (conversationId != null) {
                                            chatViewModel.onReactionClick(conversationId, item.message.id, emoji)
                                        }
                                        selectedMessageId = null
                                    },
                                    onPin = {
                                        if (conversationId != null) {
                                            chatViewModel.onPinMessageClick(conversationId, item.message)
                                        }
                                        selectedMessageId = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStickerSheet = false },
            sheetState = sheetState
        ) {
            StickerPicker(
                onStickerSelected = { stickerId ->
                    if (conversationId != null) {
                        chatViewModel.sendSticker(conversationId, stickerId)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showStickerSheet = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PinnedMessageView(message: Message) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = "Mensagem Fixada",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (message.type) {
                    "IMAGE" -> "📷 Imagem"
                    "VIDEO" -> "🎥 Vídeo"
                    "STICKER" -> "Figurinha"
                    else -> message.content
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StickerPicker(
    onStickerSelected: (String) -> Unit
) {
    val stickers = listOf(
        "sticker_01" to R.drawable.sticker_01,
        "sticker_02" to R.drawable.sticker_02,
        "sticker_03" to R.drawable.sticker_03,
        "sticker_04" to R.drawable.sticker_04
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        items(stickers) { (id, drawable) ->
            Image(
                painter = painterResource(id = drawable),
                contentDescription = "Sticker $id",
                modifier = Modifier
                    .size(100.dp)
                    .padding(8.dp)
                    .clickable { onStickerSelected(id) }
            )
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 1.dp
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    searchQuery: String,
    isSelected: Boolean,
    isGroup: Boolean,
    groupMembers: Map<String, User>,
    onLongClick: () -> Unit,
    onReaction: (String) -> Unit,
    onPin: () -> Unit
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isSentByCurrentUser = message.senderId == currentUserId
    val alignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (isGroup && !isSentByCurrentUser) {
            val sender = groupMembers[message.senderId]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            ) {
                AsyncImage(
                    model = sender?.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                    contentDescription = "Avatar de ${sender?.name}",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sender?.name ?: "Usuário",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(visible = isSelected) {
            EmojiPicker(onReaction = onReaction, onPin = onPin)
        }

        BubbleContent(message = message, searchQuery = searchQuery, onLongClick = onLongClick)

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
private fun getStickerResource(stickerId: String?): Int {
    return when (stickerId) {
        "sticker_01" -> R.drawable.sticker_01
        "sticker_02" -> R.drawable.sticker_02
        "sticker_03" -> R.drawable.sticker_03
        "sticker_04" -> R.drawable.sticker_04
        else -> R.drawable.ic_launcher_foreground
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BubbleContent(
    message: Message,
    searchQuery: String,
    onLongClick: () -> Unit
) {
    val isSentByCurrentUser = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
    val bubbleColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val context = LocalContext.current
    val hasPadding = message.type == "TEXT" || message.type == "IMAGE" || message.type == "VIDEO"

    Box(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .combinedClickable(
                onClick = {
                    if (message.type == "VIDEO") {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.content))
                        intent.setDataAndType(Uri.parse(message.content), "video/mp4")
                        context.startActivity(intent)
                    }
                },
                onLongClick = onLongClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (message.type == "STICKER") Color.Transparent else bubbleColor)
                .padding(if (hasPadding) 1.dp else 0.dp),
            horizontalAlignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start
        ) {
            when (message.type) {
                "TEXT" -> Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    HighlightingText(text = message.content, query = searchQuery)
                }
                "IMAGE" -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = message.content),
                        contentDescription = "Imagem enviada",
                        modifier = Modifier
                            .sizeIn(maxHeight = 250.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                "VIDEO" -> {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = rememberAsyncImagePainter(model = message.thumbnailUrl),
                            contentDescription = "Miniatura do vídeo",
                            modifier = Modifier
                                .sizeIn(maxHeight = 250.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Reproduzir vídeo",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                "STICKER" -> {
                    Image(
                        painter = painterResource(id = getStickerResource(message.content)),
                        contentDescription = "Sticker",
                        modifier = Modifier
                            .size(120.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            if (hasPadding) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 4.dp).align(Alignment.End)
                ) {
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
    }
}


@Composable
fun EmojiPicker(onReaction: (String) -> Unit, onPin: () -> Unit) {
    val emojis = listOf("👍", "❤️", "😂", "😯", "😢", "🙏")
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Divider(modifier = Modifier
                .height(24.dp)
                .width(1.dp)
                .padding(horizontal = 4.dp))
            IconButton(onClick = onPin, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Fixar Mensagem",
                    modifier = Modifier.size(20.dp)
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
        message.readTimestamp > 0 -> Icons.Default.DoneAll to Color.Blue
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

@Composable
fun HighlightingText(text: String, query: String) {
    if (query.isBlank() || text.isBlank()) {
        Text(text = text, textAlign = TextAlign.Start)
        return
    }

    val annotatedString = buildAnnotatedString {
        val pattern = Pattern.compile(query, Pattern.LITERAL or Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)

        var lastEnd = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastEnd) {
                append(text.substring(lastEnd, start))
            }

            withStyle(style = SpanStyle(background = Color.Yellow, fontWeight = FontWeight.Bold)) {
                append(text.substring(start, end))
            }
            lastEnd = end
        }
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
    Text(text = annotatedString, textAlign = TextAlign.Start)
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    App_mensagemTheme {
        ChatScreen(navController = NavController(LocalContext.current), conversationId = "preview_id")
    }
}