package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.unit.Dp
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
    Box(Modifier.wrapContentWidth(align = Alignment.CenterHorizontally)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { new -> expanded = new }) {
            OutlinedTextField(
                modifier =
                    Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .widthIn(1.dp, Dp.Infinity),
                textStyle = typo().bodyMedium,
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
                        text = { Text(s, style = typo().bodyMedium) },
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