package com.maxrave.simpmusic.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.maxrave.simpmusic.ui.theme.typo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownButton(
    defaultSelected: String = "",
    items: List<String>,
    onItemSelected: (String) -> Unit,
) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }
    var selected by rememberSaveable {
        mutableStateOf(defaultSelected)
    }
    Box(Modifier.width(150.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { new -> expanded = new }) {
            OutlinedTextField(
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
                textStyle = typo.bodyMedium,
                readOnly = true,
                value = selected,
                onValueChange = {},
                maxLines = 1,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(40),
                colors =
                    ExposedDropdownMenuDefaults.textFieldColors(
                        focusedIndicatorColor = Gray,
                        unfocusedIndicatorColor = Gray,
                        unfocusedContainerColor = Transparent,
                        focusedContainerColor = Transparent,
                    ),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEachIndexed { index, s ->
                    DropdownMenuItem(
                        text = { Text(s, style = typo.bodyMedium) },
                        onClick = {
                            selected = s
                            expanded = false
                            onItemSelected(s)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun DropdownButtonPreview() {
    DropdownButton(
        defaultSelected = "Item 1 SIMPMUSIC",
        items = listOf("Item 1 SIMPMUSIC", "Item 2", "Item 3", "Item 4", "Item 5"),
        onItemSelected = { /*TODO*/ },
    )
}