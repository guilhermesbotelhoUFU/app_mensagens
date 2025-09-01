package com.example.app_mensagem.presentation.group

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app_mensagem.presentation.contacts.UserItem
import com.example.app_mensagem.presentation.viewmodel.GroupInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    navController: NavController,
    groupId: String?,
    groupInfoViewModel: GroupInfoViewModel = viewModel()
) {
    val uiState by groupInfoViewModel.uiState.collectAsState()

    LaunchedEffect(groupId) {
        if (groupId != null) {
            groupInfoViewModel.loadGroupInfo(groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dados do Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Erro: ${uiState.error}",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.group != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.group!!.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${uiState.members.size} participantes")
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    LazyColumn {
                        items(uiState.members) { member ->
                            // Reutilizamos o UserItem da tela de contatos!

                        }
                    }
                }
            }
        }
    }
}