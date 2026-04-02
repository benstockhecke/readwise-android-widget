package com.readwise.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.readwise.widget.ui.highlights.HighlightsListScreen
import com.readwise.widget.ui.settings.SettingsScreen
import com.readwise.widget.ui.theme.ReadwiseWidgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReadwiseWidgetTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "settings") {
                    composable("settings") {
                        SettingsScreen(
                            onNavigateToHighlights = { navController.navigate("highlights") },
                        )
                    }
                    composable("highlights") {
                        HighlightsListScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
