package com.maxrave.simpmusic.ui.component

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.extension.NonLazyGrid
import com.maxrave.simpmusic.ui.theme.typo

@Composable
fun LibraryTilingBox() {
    val listItem = listOf(
        LibraryTilingState.Favorite,
        LibraryTilingState.Followed,
        LibraryTilingState.MostPlayed,
        LibraryTilingState.Downloaded
    )
    NonLazyGrid(
        columns = 2,
        itemCount = 4,
        modifier = Modifier.fillMaxWidth()
            .padding(bottom = 10.dp, end = 10.dp)
    ) { number ->
        Box(
            Modifier.padding(start = 10.dp, top = 10.dp)
        ) {
            LibraryTilingItem(listItem[number])
        }
    }
}

@Composable
fun LibraryTilingItem(state: LibraryTilingState) {
    val context = LocalContext.current
    val title = context.getString(state.title)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable {
            state.onClick()
        },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors().copy(
            containerColor = state.containerColor
        )
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                state.icon,
                contentDescription = title,
                modifier = Modifier.size(50.dp)
                    .padding(10.dp),
                tint = state.iconColor
            )
            Text(
                title,
                style = typo.titleSmall,
                color = Color.Black
            )
        }
    }
}

data class LibraryTilingState(
    @StringRes val title: Int,
    val containerColor: Color,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
) {
    companion object {
        val Favorite = LibraryTilingState(
            title = R.string.favorite,
            containerColor = Color(0xffff99ae),
            icon = Icons.Default.Favorite,
            iconColor = Color(0xffD10000),
            onClick = {}
        )
        val Followed = LibraryTilingState(
            title = R.string.followed,
            containerColor = Color(0xffFFEB3B),
            icon = Icons.Default.Insights,
            iconColor = Color.Black,
            onClick = {}
        )
        val MostPlayed = LibraryTilingState(
            title = R.string.most_played,
            containerColor = Color(0xff00BCD4),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconColor = Color.Black,
            onClick = {}
        )
        val Downloaded = LibraryTilingState(
            title = R.string.downloaded,
            containerColor = Color(0xff4CAF50),
            icon = Icons.Default.Downloading,
            iconColor = Color.Black,
            onClick = {}
        )
    }
}


@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun LibraryScreenPreview() {
    LibraryTilingBox()
}