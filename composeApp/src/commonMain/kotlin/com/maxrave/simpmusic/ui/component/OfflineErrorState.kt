package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.home_offline_subtitle
import simpmusic.composeapp.generated.resources.home_offline_title
import simpmusic.composeapp.generated.resources.listen_to_downloaded
import simpmusic.composeapp.generated.resources.retry
import simpmusic.composeapp.generated.resources.downloaded
import simpmusic.composeapp.generated.resources.cached_songs
import androidx.compose.material.icons.filled.Cached

/**
 * Spotify-style minimal offline / error state shown on the Home tab when
 * the home feed cannot load (no network or backend failure).
 *
 * Hosts:
 * - Centered cloud-off icon
 * - Title + subtitle copy
 * - Primary action: retry the home feed
 * - Secondary action: jump to the locally downloaded library
 */
@Composable
fun OfflineErrorState(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onRetry: () -> Unit,
    onOpenDownloaded: () -> Unit,
    onOpenOfflinePlaylist: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.home_offline_title),
                style = typo().titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_offline_subtitle),
                style = typo().bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            
            // 2-box row for offline listening options
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.ElevatedCard(
                    modifier = Modifier.weight(1f).height(60.dp),
                    onClick = onOpenDownloaded,
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors().copy(
                        containerColor = Color(0xff4CAF50),
                    ),
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(30.dp),
                            tint = Color.Black
                        )
                        Text(
                            text = stringResource(Res.string.downloaded),
                            style = typo().titleSmall,
                            color = Color.Black
                        )
                    }
                }
                
                androidx.compose.material3.ElevatedCard(
                    modifier = Modifier.weight(1f).height(60.dp),
                    onClick = onOpenOfflinePlaylist,
                    shape = RoundedCornerShape(8.dp),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors().copy(
                        containerColor = Color(0xffdcd3ff),
                    ),
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.Cached,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp).size(30.dp),
                            tint = Color.Black
                        )
                        Text(
                            text = stringResource(Res.string.cached_songs),
                            style = typo().titleSmall,
                            color = Color.Black
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.retry),
                    color = Color.Black,
                )
            }
        }
    }
}
