package com.ai4biz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ai4biz.app.navigation.Ai4bizNavHost
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.theme.Ai4bizTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val container = (application as Ai4bizApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                Ai4bizTheme {
                    Ai4bizNavHost()
                }
            }
        }
    }
}
