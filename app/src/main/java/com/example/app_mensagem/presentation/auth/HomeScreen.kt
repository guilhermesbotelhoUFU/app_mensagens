package com.example.app_mensagem.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.presentation.viewmodel.AuthViewModel
import com.example.app_mensagem.presentation.viewmodel.ConversationUiState
import com.example.app_mensagem.presentation.viewmodel.ConversationsViewModel
import com.example.app_mensagem.ui.theme.App_mensagemTheme
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    conversationsViewModel: ConversationsViewModel = viewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    val conversationState by conversationsViewModel.uiState.collectAsState()

    LaunchedEffect(FirebaseAuth.getInstance().currentUser) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            conversationsViewModel.loadConversations()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu de opções",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sair") },
                            onClick = {
                                showMenu = false
                                authViewModel.logout()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("contacts")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nova Conversa")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = conversationState) {
                is ConversationUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ConversationUiState.Success -> {
                    if (state.conversations.isEmpty()) {
                        Text(
                            "Nenhuma conversa encontrada.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Ordena as conversas pela mais recente
                            items(state.conversations.sortedByDescending { it.timestamp }) { conversation ->
                                ConversationItem(conversation = conversation) {
                                    navController.navigate("chat/${conversation.id}")
                                }
                            }
                        }
                    }
                }
                is ConversationUiState.Error -> {
                    Text(
                        text = "Erro: ${state.message}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = formatTimestamp(conversation.timestamp),
            style = MaterialTheme.typography.bodySmall
        )
    }
    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}

private fun formatTimestamp(timestamp: Long): String {
    val messageDate = Date(timestamp)
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(messageDate)
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    App_mensagemTheme {
        val mockNavController = NavController(LocalContext.current)
        val mockAuthViewModel: AuthViewModel = viewModel()
        HomeScreen(navController = mockNavController, authViewModel = mockAuthViewModel)
    }
}