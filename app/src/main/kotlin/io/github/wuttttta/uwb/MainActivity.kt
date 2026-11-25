package io.github.wuttttta.uwb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.wuttttta.uwb.ui.PoeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PoeLog.i("start version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) sdk=${android.os.Build.VERSION.SDK_INT} model=${android.os.Build.MODEL}")
        setContent { PoeApp() }
    }
}
