package com.kapos.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kapos.mobile.core.theme.KaPosTheme
import com.kapos.mobile.ui.KaPosApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KaPosApplication

        setContent {
            KaPosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KaPosApp(app = app)
                }
            }
        }
    }
}
