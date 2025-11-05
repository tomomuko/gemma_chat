package com.example.gemmabench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gemmabench.ui.GemmaScreen
import com.example.gemmabench.ui.theme.GemmaBenchTheme

/**
 * Gemma Benchmarká¤ó¢¯Æ£ÓÆ£
 *
 * Jetpack Compose’(W_·ó°ë¢¯Æ£ÓÆ£¢ü­Æ¯Áã
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ¨Ã¸to¨Ã¸h:’	¹
        enableEdgeToEdge()

        setContent {
            GemmaBenchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GemmaScreen()
                }
            }
        }
    }
}
