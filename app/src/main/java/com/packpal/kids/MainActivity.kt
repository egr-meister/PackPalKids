package com.packpal.kids

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packpal.kids.data.prefs.AppPreferences
import com.packpal.kids.ui.PackPalNavHost
import com.packpal.kids.ui.theme.PackPalColors
import com.packpal.kids.ui.theme.PackPalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Cream UI in light and dark system themes: always dark system-bar icons, never sticky immersive.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val container = (application as PackPalApp).container
        setContent {
            val prefs by container.preferences.preferences.collectAsStateWithLifecycle(
                initialValue = AppPreferences(selectedTemplateId = null, animationsEnabled = true, seeded = false)
            )
            PackPalTheme(animationsEnabled = prefs.animationsEnabled) {
                Box(Modifier.fillMaxSize().background(PackPalColors.Cream)) {
                    PackPalNavHost()
                }
            }
        }
    }
}
