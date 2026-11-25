package io.github.wuttttta.uwb.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.wuttttta.uwb.BuildConfig

enum class Screen { Start, About }

@Composable
fun PoeApp() {
    val ctx = LocalContext.current
    val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    MaterialTheme(colorScheme = colors) {
        var screen by rememberSaveable { mutableStateOf(Screen.Start) }
        Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
            Column(Modifier.padding(inner)) {
                if (BuildConfig.INSECURE_STATIC_STS) InsecureBanner()
                when (screen) {
                    Screen.Start -> StartScreen(onAbout = { screen = Screen.About })
                    Screen.About -> AboutScreen(onBack = { screen = Screen.Start })
                }
            }
        }
    }
}
