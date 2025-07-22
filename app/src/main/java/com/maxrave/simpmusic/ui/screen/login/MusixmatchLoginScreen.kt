package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.MusixmatchViewModel
import org.koin.androidx.compose.koinViewModel

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusixmatchLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: MusixmatchViewModel = koinViewModel(),
) {
    var debugInfo by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var userToken by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    LaunchedEffect(
        debugInfo,
    ) {
        if (debugInfo.isNotEmpty()) {
            val fullUserIdRegex = """\[UserId\]:\s*([a-zA-Z0-9]+:[a-f0-9]+)""".toRegex()
            val fullUserId = fullUserIdRegex.find(debugInfo)?.groupValues?.get(1)

            val userTokenRegex = """\[UserToken\]:\s*([a-f0-9]+)""".toRegex()
            val userTokenData = userTokenRegex.find(debugInfo)?.groupValues?.get(1)

            val deviceIdRegex = """\[DeviceId\]:\s*([a-f0-9]+)""".toRegex()
            val deviceIdData = deviceIdRegex.find(debugInfo)?.groupValues?.get(1)

            if (fullUserId != null && userTokenData != null && deviceIdData != null) {
                userId = fullUserId
                userToken = userTokenData
                deviceId = deviceIdData
            } else {
                userId = ""
                userToken = ""
                deviceId = ""
            }
        }
    }
    Column(
        Modifier
            .verticalScroll(
                scrollState,
            ).padding(
                innerPadding,
            ).padding(
                horizontal = 16.dp,
            ).padding(
                top = 64.dp,
            ),
    ) {
        Text("Follow this instruction below")
        Spacer(Modifier.size(5.dp))
        Text(
            "Step 1: Go to Musixmatch app and ensure you are logged in",
            style = typo.bodyMedium,
        )
        Text(
            "Step 2: Open Settings (top right corner)",
            style = typo.bodyMedium,
        )
        Text(
            "Step 3: Scroll down to the Support section and tap on 'Get Help'",
            style = typo.bodyMedium,
        )
        Text(
            "Step 4: Tap on 'Copy debug info'",
            style = typo.bodyMedium,
        )
        Text(
            "Step 5: Paste the copied info here",
            style = typo.bodyMedium,
        )
        Spacer(Modifier.size(15.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = debugInfo,
            onValueChange = {
                debugInfo = it
            },
            maxLines = 3,
            textStyle =
                typo.bodySmall.copy(
                    color = Color.White,
                ),
            supportingText = {
                if (debugInfo.isNotEmpty() && userToken.isEmpty() && userId.isEmpty()) {
                    Text(
                        text = "Can not parse userId and userToken",
                        style = typo.bodySmall,
                    )
                }
            },
        )
        Spacer(Modifier.size(5.dp))
        Crossfade(
            (userId.isNotEmpty() && userToken.isNotEmpty() && deviceId.isNotEmpty()),
        ) {
            if (it) {
                Column {
                    Text(
                        "Your userId: $userId",
                        style = typo.labelSmall,
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(
                        "Your userToken: $userToken",
                        style = typo.labelSmall,
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(
                        "Your deviceId: $deviceId",
                        style = typo.labelSmall,
                    )
                }
            }
        }
        Spacer(Modifier.size(5.dp))
        ElevatedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = userId.isNotEmpty() && userToken.isNotEmpty() && deviceId.isNotEmpty(),
            onClick = {
                viewModel.login(
                    userId = userId,
                    userToken = userToken,
                    deviceId = deviceId,
                )
                navController.navigateUp()
            },
        ) {
            Text(
                stringResource(R.string.save),
            )
        }
        EndOfPage()
    }
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.log_in_to_Musixmatch),
                style = typo.titleMedium,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { navController.navigateUp() },
            ) {
                Icon(
                    Icons.Filled.ArrowBackIosNew,
                    "Back",
                )
            }
        },
    )
}