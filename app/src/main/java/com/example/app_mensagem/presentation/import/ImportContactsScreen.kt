package com.example.app_mensagem.presentation.import

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app_mensagem.data.model.DeviceContact
import com.example.app_mensagem.presentation.viewmodel.ImportContactsUiState
import com.example.app_mensagem.presentation.viewmodel.ImportContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsScreen(
    navController: NavController,
    viewModel: ImportContactsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                permissionGranted = true
                viewModel.loadDeviceContacts()
            }
        }
    )

    // Carrega os contatos se a permissão já foi concedida ao entrar na tela
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.loadDeviceContacts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contatos da Agenda") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (permissionGranted) {
                when (val state = uiState) {
                    is ImportContactsUiState.Idle, is ImportContactsUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ImportContactsUiState.Success -> {
                        if (state.contacts.isEmpty()) {
                            Text("Nenhum contato encontrado na sua agenda.", modifier = Modifier.align(Alignment.Center))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(state.contacts) { contact ->
                                    ContactRow(contact = contact)
                                }
                            }
                        }
                    }
                }
            } else {
                PermissionRequestView(onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                })
            }
        }
    }
}

@Composable
fun PermissionRequestView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Para encontrar seus amigos no app, permita o acesso aos seus contatos.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Permitir Acesso")
        }
    }
}

@Composable
fun ContactRow(contact: DeviceContact) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(contact.name, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
    }
    Divider()
}