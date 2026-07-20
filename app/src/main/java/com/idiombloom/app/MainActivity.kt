package com.idiombloom.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.idiombloom.app.ui.IdiomBloomApp
import com.idiombloom.app.ui.IdiomBloomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IdiomBloomTheme {
                IdiomBloomApp()
            }
        }
    }
}
