package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.settings.AppSettingsManager
import com.example.navigation.AppNavigation
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppSettingsManager.init(this)
    enableEdgeToEdge()
    setContent {
      val appSettings by AppSettingsManager.settingsState.collectAsState()
      MyApplicationTheme(themeMode = appSettings.themeMode) {
        val colors = LocalAppColors.current
        Surface(
          modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
          color = colors.background
        ) {
          AppNavigation()
        }
      }
    }
  }
}
