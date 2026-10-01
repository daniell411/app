package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
            ApuntaApp(viewModel = viewModel)
        }
    }
}
