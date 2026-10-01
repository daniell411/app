package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.example.notification.NotificationHelper
import com.example.ui.ApuntaApp
import com.example.ui.viewmodel.ApuntaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ApuntaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel for reminder alerts
        NotificationHelper.createNotificationChannel(this)

        setContent {
            // Permission requester for microphone and notifications
            val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { _ ->
                // Permissions handled
            }

            LaunchedEffect(Unit) {
                val needsRequest = permissionsToRequest.any {
                    ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                }
                if (needsRequest) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
            }

            ApuntaApp(viewModel = viewModel)
        }
    }
}
