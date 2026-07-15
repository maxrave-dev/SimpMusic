package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamGuestScreen(
    viewModel: JamViewModel,
    onNavigateToSession: () -> Unit,
    onBack: () -> Unit
) {
    var roomCode by remember { mutableStateOf(TextFieldValue("")) }
    val sessionState by viewModel.sessionState.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()

    LaunchedEffect(sessionState) {
        if (sessionState != null && !sessionState!!.isHost) {
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
            title = { Text("Joining Jam") },
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
                title = { Text("Join a Jam") },
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
            Text("Join a Jam Session", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = roomCode,
                onValueChange = { if (it.text.length <= 6) roomCode = it },
                label = { Text("6-Digit Room Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.joinSession(roomCode.text.uppercase()) },
                enabled = roomCode.text.length == 6 && !isConnecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
            ) {
                Text("Join Room")
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
