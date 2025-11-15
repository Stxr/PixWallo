package com.example.pixlwallo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pixlwallo.ui.AppRoot
import com.example.pixlwallo.ui.theme.PixlWalloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixlWalloTheme {
                AppRoot()
            }
        }
    }
}