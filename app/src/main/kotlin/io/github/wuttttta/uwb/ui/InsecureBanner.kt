package io.github.wuttttta.uwb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shown on every screen when the INSECURE_STATIC_STS debug flag is on. */
@Composable
fun InsecureBanner() {
    Text(
        "INSECURE: static STS. Relay attacks are possible; records are marked insecure.",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().background(Color(0xFFC62828)).padding(12.dp),
    )
}
