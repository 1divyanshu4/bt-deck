package dev.btdeck.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.btdeck.core.designsystem.component.TopBar
import dev.btdeck.core.model.BTDeckSettings
import dev.btdeck.core.model.ButtonSpacing
import dev.btdeck.core.model.ButtonStyle

/**
 * App settings. Each row is one line: choices open a small menu, on/off settings are switches.
 * [onChange] receives a function that turns the current settings into the new ones.
 */
@Composable
fun SettingsScreen(
    settings: BTDeckSettings,
    onChange: ((BTDeckSettings) -> BTDeckSettings) -> Unit,
    onOpenWalkthrough: () -> Unit = {},
    onOpenTutorials: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    status: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(leading = status)
        // Same edge as the status above it, like the Pad.
        Column(Modifier.padding(horizontal = 12.dp)) {
            SectionTitle("Buttons")
            ChoiceRow("Show on buttons", ButtonStyle.entries, settings.buttonStyle, { it.label }) { style ->
                onChange { it.copy(buttonStyle = style) }
            }
            SwitchRow("Show shortcut", settings.showShortcut) { on -> onChange { it.copy(showShortcut = on) } }
            ChoiceRow("Spacing", ButtonSpacing.entries, settings.spacing, { it.label }) { spacing ->
                onChange { it.copy(spacing = spacing) }
            }

            SectionTitle("Feel")
            SwitchRow("Vibrate on press", settings.vibrate) { on -> onChange { it.copy(vibrate = on) } }
            SwitchRow("Keep screen on", settings.keepScreenOn) { on -> onChange { it.copy(keepScreenOn = on) } }

            Spacer(Modifier.height(16.dp))
            LinkRow("App walkthrough", onOpenWalkthrough)
            LinkRow("Tutorials", onOpenTutorials)
            LinkRow("Privacy policy", onOpenPrivacy)
            LinkRow("About BTDeck", onOpenAbout)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

/** One line with the current value on the right; tapping opens a menu of the options. */
@Composable
private fun <T> ChoiceRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(title, onClick = { open = true }) {
            Text(label(selected), color = Color.Gray)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in options) {
                val weight = if (option == selected) FontWeight.Bold else null
                DropdownMenuItem(text = { Text(label(option), fontWeight = weight) }, onClick = {
                    open = false
                    onSelect(option)
                })
            }
        }
    }
}

/** An on/off setting. The whole row is tappable. */
@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, role = Role.Switch, onClick = { onChange(!checked) }) {
        // The row handles clicks, so the switch itself doesn't need to.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun LinkRow(title: String, onClick: () -> Unit) {
    SettingRow(title, onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

/** The shared shape of every row: a title on the left, something on the right. */
@Composable
internal fun SettingRow(title: String, onClick: () -> Unit, role: Role? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = role, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}
