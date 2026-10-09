package dev.btdeck.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.btdeck.core.designsystem.theme.ZexDeckShapes

/**
 * A single-line text field, 48dp tall instead of Material's 56dp. [placeholder] names the field
 * while it's empty (screen readers read it too); there's no floating label taking up room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        // 48dp: its touch area is never smaller than that, so a shorter field would have that
        // area spill onto whatever sits above and below it.
        modifier = modifier.heightIn(min = 48.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interaction,
        decorationBox = { field ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value.text,
                innerTextField = field,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                placeholder = { Text(placeholder, color = Color.Gray) },
                colors = colors,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interaction,
                        colors = colors,
                        shape = ZexDeckShapes.Small,
                    )
                },
            )
        },
    )
}

/** [CompactTextField] for plain text. */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    // The text comes from the caller; the cursor and selection are kept here.
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    val shown = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
    CompactTextField(
        value = shown,
        onValueChange = {
            field = it
            if (it.text != value) onValueChange(it.text)
        },
        placeholder = placeholder,
        modifier = modifier,
    )
}
