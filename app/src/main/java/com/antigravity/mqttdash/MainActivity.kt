package com.antigravity.mqttdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.antigravity.mqttdash.ui.MqttDashApp
import com.antigravity.mqttdash.ui.theme.MqttDashTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MqttDashTheme {
                MqttDashApp()
            }
        }
    }
}
