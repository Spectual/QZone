package com.qzone.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.qzone.data.repository.LocalSurveyRepository
import com.qzone.domain.repository.LocationRepository
import com.qzone.domain.repository.RewardRepository
import com.qzone.domain.repository.SurveyRepository
import com.qzone.domain.repository.UserRepository

@Composable
fun rememberQzoneAppState(
    surveyRepository: SurveyRepository,
    rewardRepository: RewardRepository,
    userRepository: UserRepository,
    locationRepository: LocationRepository,
    localSurveyRepository: LocalSurveyRepository,
    navController: NavHostController = rememberNavController()
): QzoneAppState {
    return remember(navController, surveyRepository, rewardRepository, userRepository, locationRepository, localSurveyRepository) {
        QzoneAppState(
            navController = navController,
            surveyRepository = surveyRepository,
            rewardRepository = rewardRepository,
            userRepository = userRepository,
            locationRepository = locationRepository,
            localSurveyRepository = localSurveyRepository
        )
    }
}

@Stable
class QzoneAppState(
    val navController: NavHostController,
    val surveyRepository: SurveyRepository,
    val rewardRepository: RewardRepository,
    val userRepository: UserRepository,
    val locationRepository: LocationRepository,
    val localSurveyRepository: LocalSurveyRepository
) {

    fun isTopLevelDestination(destination: NavDestination?): Boolean {
        if (destination == null) return false
        return TOP_LEVEL_DESTINATIONS.any { top ->
            destination.hierarchy.any { it.route == top.destination.route }
        }
    }

    fun navigateTopLevel(toDestination: QzoneDestination) {
        navController.navigate(toDestination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}
