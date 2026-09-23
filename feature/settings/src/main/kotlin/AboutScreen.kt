package dev.keyset.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Version, help, and the licenses of KeySet and the libraries it uses. */
@Composable
fun AboutScreen(
    appVersion: String,
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        PageTitle("About KeySet", onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Version $appVersion", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            LinkRow("Connection help", onOpenHelp)
            LinkRow("Open-source licenses", onOpenLicenses)
            Text(
                "KeySet is open source under the Apache License 2.0.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

/** A back arrow and a title, for pages opened from Settings. */
@Composable
fun PageTitle(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
