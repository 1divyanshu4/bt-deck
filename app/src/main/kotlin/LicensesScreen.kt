package dev.keyset

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import dev.keyset.feature.settings.PageTitle

/** Every library KeySet uses and its license, generated at build time by AboutLibraries. */
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    Column(modifier.fillMaxSize()) {
        PageTitle("Open-source licenses", onBack)
        LibrariesContainer(libraries, Modifier.weight(1f))
    }
}
