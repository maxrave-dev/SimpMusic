package com.maxrave.simpmusic.ui.screen.other

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.adaptiveIconPainterResource
import com.maxrave.simpmusic.ui.component.RippleIconButton
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun CreditScreen(
    paddingValues: PaddingValues,
    navController: NavController,
) {
    val context = LocalContext.current
    val hazeState = rememberHazeState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 64.dp)
                .verticalScroll(rememberScrollState())
                .hazeSource(state = hazeState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // App icon
        Image(
            painter = adaptiveIconPainterResource(R.mipmap.ic_launcher_round) ?: painterResource(R.drawable.holder),
            contentDescription = "App Icon",
            modifier =
                Modifier
                    .size(150.dp)
                    .clip(CircleShape),
        )

        Spacer(modifier = Modifier.height(30.dp))

        // App name
        Text(
            text = stringResource(id = R.string.app_name),
            style = typo.titleLarge,
            fontSize = 22.sp,
        )

        // Version
        Text(
            text = stringResource(R.string.version_format, VersionManager.getVersionName()),
            style = typo.bodySmall,
            fontSize = 13.sp,
        )

        // Developer
        Text(
            text = stringResource(id = R.string.maxrave_dev),
            style = typo.bodyMedium,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // App description
        Text(
            text = stringResource(id = R.string.credit_app),
            style = typo.bodyMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(10.dp))

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            // GitHub button
            TextButton(
                onClick = {
                    val urlIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://simpmusic.org".toUri(),
                        )
                    context.startActivity(urlIntent)
                },
                modifier =
                    Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 25.dp)
                        .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
            ) {
                Text(text = stringResource(id = R.string.website))
            }

            // GitHub button
            TextButton(
                onClick = {
                    val urlIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/maxrave-dev/SimpMusic".toUri(),
                        )
                    context.startActivity(urlIntent)
                },
                modifier =
                    Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 25.dp)
                        .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
            ) {
                Text(text = stringResource(id = R.string.github))
            }

            // Issue tracker button
            TextButton(
                onClick = {
                    val urlIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/maxrave-dev/SimpMusic/issues".toUri(),
                        )
                    context.startActivity(urlIntent)
                },
                modifier =
                    Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 25.dp)
                        .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
            ) {
                Text(text = stringResource(id = R.string.issue_tracker))
            }

            // Buy me a coffee button
            TextButton(
                onClick = {
                    val urlIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://github.com/sponsors/maxrave-dev".toUri(),
                        )
                    context.startActivity(urlIntent)
                },
                modifier =
                    Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 25.dp)
                        .defaultMinSize(minHeight = 1.dp, minWidth = 1.dp),
            ) {
                Text(text = stringResource(id = R.string.buy_me_a_coffee))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Copyright text
        Text(
            text = stringResource(id = R.string.copyright),
            style = typo.bodySmall,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp, vertical = 5.dp),
            textAlign = TextAlign.Start,
        )

        // Bottom spacing
        Spacer(modifier = Modifier.height(200.dp))
    }
    TopAppBar(
        modifier =
            Modifier
                .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                    blurEnabled = true
                },
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = typo.titleMedium,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(
                            align = Alignment.CenterVertically,
                        ).basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                        ).focusable(),
            )
        },
        navigationIcon = {
            Box(Modifier.padding(horizontal = 5.dp)) {
                RippleIconButton(
                    R.drawable.baseline_arrow_back_ios_new_24,
                    Modifier
                        .size(32.dp),
                    true,
                ) {
                    navController.navigateUp()
                }
            }
        },
        colors =
            TopAppBarDefaults.largeTopAppBarColors(Color.Transparent),
    )
}