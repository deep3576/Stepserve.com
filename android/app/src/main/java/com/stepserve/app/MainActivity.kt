package com.stepserve.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stepserve.app.ui.navigation.NavGraph
import com.stepserve.app.ui.theme.StepServeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepServeTheme {
                NavGraph()
            }
        }
    }
}
