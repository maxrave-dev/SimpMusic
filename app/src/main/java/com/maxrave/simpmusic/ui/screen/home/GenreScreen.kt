package com.maxrave.simpmusic.ui.screen.home

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.MoodAndGenresContentItem
import com.maxrave.simpmusic.ui.component.NormalAppBar
import com.maxrave.simpmusic.viewModel.GenreViewModel

@OptIn(UnstableApi::class)
@Composable
fun GenreScreen(
    modifier: Modifier = Modifier, navController: NavController, viewModel: GenreViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {

    val genreObject by viewModel.genreObject.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        NormalAppBar(title = {
            Text(text = genreObject?.header ?: "")
        }, leftIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    painterResource(id = R.drawable.baseline_arrow_back_ios_new_24), contentDescription = "Back"
                )
            }
        })
        AnimatedVisibility(visible = !loading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(genreObject?.itemsPlaylist ?: emptyList()) { item ->
                    MoodAndGenresContentItem(
                        data = item, navController = navController
                    )
                }
                item {
                    EndOfPage()
                }
            }
        }
        AnimatedVisibility(visible = loading) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
