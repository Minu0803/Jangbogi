package com.minwoo.jangbogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.minwoo.jangbogi.ui.navigation.AppNavHost
import com.minwoo.jangbogi.ui.theme.JangbogiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JangbogiTheme {
                AppNavHost()
            }
        }
    }
}
