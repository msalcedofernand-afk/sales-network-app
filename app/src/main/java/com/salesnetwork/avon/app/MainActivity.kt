package com.salesnetwork.avon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import com.salesnetwork.avon.app.ui.SalesNetworkMainApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF5B35B5),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFEADDFF),
                    onPrimaryContainer = Color(0xFF21005D),
                    secondary = Color(0xFF6750A4),
                    secondaryContainer = Color(0xFFE8DEF8),
                    background = Color(0xFFF9F7FC),
                    surface = Color(0xFFF9F7FC),
                    surfaceVariant = Color(0xFFEDE7F6),
                    error = Color(0xFFBA1A1A)
                )
            ) {
                Surface {
                    SalesNetworkMainApp()
                }
            }
        }
    }
}
