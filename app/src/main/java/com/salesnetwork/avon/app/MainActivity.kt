package com.salesnetwork.avon.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.salesnetwork.avon.app.ui.SalesNetworkMainApp
import com.salesnetwork.avon.app.update.AppUpdateChecker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var availableUpdate by remember { mutableStateOf<com.salesnetwork.avon.app.update.AppUpdateInfo?>(null) }
            LaunchedEffect(Unit) { availableUpdate = AppUpdateChecker().check() }
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF165C59), onPrimary = Color.White,
                    primaryContainer = Color(0xFFD5EEE3), onPrimaryContainer = Color(0xFF123D49),
                    secondary = Color(0xFF526578), secondaryContainer = Color(0xFFE2EAF0),
                    background = Color(0xFFF3F6F5), surface = Color.White,
                    surfaceVariant = Color(0xFFE0E8E5), surfaceContainerLowest = Color.White,
                    surfaceContainerLow = Color(0xFFF8FAF9), surfaceContainer = Color(0xFFF0F5F3),
                    surfaceContainerHigh = Color(0xFFE8EFEC), surfaceContainerHighest = Color(0xFFE0E8E5),
                    onSurface = Color(0xFF192D2C), onSurfaceVariant = Color(0xFF4F625E),
                    outline = Color(0xFF7C918A), outlineVariant = Color(0xFFD5DFDB),
                    error = Color(0xFFBA1A1A)
                )
            ) {
                Surface {
                    SalesNetworkMainApp(
                        availableUpdate = availableUpdate,
                        onOpenUpdate = { url ->
                            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            catch (_: ActivityNotFoundException) { }
                        }
                    )
                }
            }
        }
    }
}

