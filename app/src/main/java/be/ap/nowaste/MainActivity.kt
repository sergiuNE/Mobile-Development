package be.ap.nowaste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import be.ap.nowaste.ui.navigation.NoWasteApp
import be.ap.nowaste.ui.theme.NoWasteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoWasteTheme {
                NoWasteApp()
            }
        }
    }
}
