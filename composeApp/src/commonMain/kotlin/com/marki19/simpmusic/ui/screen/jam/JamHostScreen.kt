package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamHostScreen(
    viewModel: JamViewModel,
    onNavigateToSession: () -> Unit,
    onBack: () -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()

    LaunchedEffect(sessionState) {
        if (sessionState != null && sessionState!!.isHost) {
            onNavigateToSession()
        }
    }

    if (isConnecting) {
        var elapsedSeconds by remember { mutableIntStateOf(0) }
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
        
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("Hosting Jam") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    if (elapsedSeconds < 5) {
                        Text("Connecting to server...")
                    } else {
                        Text("Waking up server...")
                        Text("This usually takes ~50s.", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${elapsedSeconds}s elapsed", style = MaterialTheme.typography.labelMedium)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Host a Jam") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Start a Jam Session", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.createSession() },
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF87CEEB), // Light Blue
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF87CEEB).copy(alpha = 0.5f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Text("Create Room")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onBack,
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
