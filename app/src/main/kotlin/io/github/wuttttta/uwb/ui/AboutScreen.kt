package io.github.wuttttta.uwb.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val KNOWN_LIMITS = listOf(
    "Proves two devices were within about half a meter, not who held them.",
    "Everything rests on Android Key Attestation: an exploited phone could lie about distances.",
    "Researchers have shown physical-layer attacks that shrink UWB distances. Requiring many consecutive close readings is a mitigation, not a guarantee.",
    "Wall-clock times in a proof are self-reported by the phones.",
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Known limits", style = MaterialTheme.typography.headlineSmall)
        KNOWN_LIMITS.forEach { Text("• $it") }
        Text(
            "The app works offline and makes no internet requests. Proximity comes from UWB time of flight " +
                "with a per-session scrambled timestamp sequence; the proof is co-signed by both phones' hardware keys.",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onBack) { Text("Back") }
    }
}
