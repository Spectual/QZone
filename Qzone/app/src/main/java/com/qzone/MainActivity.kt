package com.qzone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.qzone.ui.navigation.QzoneApp
import com.qzone.ui.navigation.rememberQzoneAppState
import com.qzone.ui.theme.QzoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val container = (application as QzoneApp).container
        setContent {
            QzoneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val appState = rememberQzoneAppState(
                        surveyRepository = container.surveyRepository,
                        rewardRepository = container.rewardRepository,
                        userRepository = container.userRepository,
                        locationRepository = container.locationRepository,
                        localSurveyRepository = container.localSurveyRepository
                    )
                    QzoneApp(appState)
                }
            }
        }
    }
}
