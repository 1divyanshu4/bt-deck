package dev.btdeck.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.btdeck.core.designsystem.component.PageTitle

/** Version, and the licenses of BTDeck and the libraries it uses. */
@Composable
fun AboutScreen(appVersion: String, onBack: () -> Unit, onOpenLicenses: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        PageTitle("About BTDeck", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Version $appVersion", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            LinkRow("Open-source licenses", onOpenLicenses)
            Text(
                "BTDeck is open source under the Apache License 2.0.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}
