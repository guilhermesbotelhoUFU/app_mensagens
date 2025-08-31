package com.example.app_mensagem.presentation.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.presentation.common.PlaceholderAvatar
import com.example.app_mensagem.presentation.viewmodel.ContactNavigationState
import com.example.app_mensagem.presentation.viewmodel.ContactsUiState
import com.example.app_mensagem.presentation.viewmodel.ContactsViewModel
import com.example.app_mensagem.ui.theme.App_mensagemTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    navController: NavController,
    contactsViewModel: ContactsViewModel = viewModel()
) {
    val contactsState by contactsViewModel.uiState.collectAsState()
    val navigationState by contactsViewModel.navigationState.collectAsState()

    // Este LaunchedEffect foi removido pois a chamada agora é feita no init do ViewModel
    // LaunchedEffect(FirebaseAuth.getInstance().currentUser) { ... }

    LaunchedEffect(navigationState) {
        if (navigationState is ContactNavigationState.NavigateToChat) {
            val conversationId = (navigationState as ContactNavigationState.NavigateToChat).conversationId
            navController.navigate("chat/$conversationId") {
                popUpTo("home")
            }
            contactsViewModel.onNavigated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar Conversa") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { navController.navigate("import_contacts") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Importar da Agenda do Celular")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = contactsState) {
                is ContactsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ContactsUiState.Success -> {
                    if (state.users.isEmpty()) {
                        Text(
                            "Nenhum outro usuário encontrado.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.users) { user ->
                                UserItem(user = user) {
                                    contactsViewModel.onUserClicked(user)
                                }
                            }
                        }
                    }
                }
                is ContactsUiState.Error -> {
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
fun UserItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaceholderAvatar(name = user.name, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(user.name, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(user.email, style = MaterialTheme.typography.bodySmall)
        }
    }
    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}

@Preview(showBackground = true)
@Composable
fun ContactsScreenPreview() {
    App_mensagemTheme {
        ContactsScreen(navController = rememberNavController())
    }
}