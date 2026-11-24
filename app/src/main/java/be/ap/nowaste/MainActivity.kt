package be.ap.nowaste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import be.ap.nowaste.ui.navigation.NoWasteApp
import be.ap.nowaste.ui.theme.NoWasteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoWasteTheme {
                // Surface sets the theme's text color, so screens without a Scaffold (login, register) are readable in dark mode
                Surface(modifier = Modifier.fillMaxSize()) {
                    NoWasteApp()
                }
            }
        }
    }
}
