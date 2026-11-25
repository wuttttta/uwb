package io.github.wuttttta.uwb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.wuttttta.uwb.BuildConfig

/** First screen: pick a role. Host = phone A (UWB controller), Join = phone B (controlee). */
@Composable
fun StartScreen(onAbout: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Proof of Encounter", style = MaterialTheme.typography.headlineMedium)
        Text("Two phones prove they were within about half a meter of each other, using UWB secure ranging and hardware-backed keys.")
        // Ranging arrives in M1; the buttons stay disabled until then.
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Host (phone A)") }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Join (phone B)") }
        Text("Scaffold build (M0): ranging is not wired up yet.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onAbout) { Text("About and known limits") }
        Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall)
    }
}
