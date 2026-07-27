package com.fileintelligence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fileintelligence.ui.navigation.FileIntelligenceNavGraph
import com.fileintelligence.ui.theme.FileIntelligenceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FileIntelligenceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FileIntelligenceNavGraph(
                        navController = rememberNavController(),
                    )
                }
            }
        }
    }
}
