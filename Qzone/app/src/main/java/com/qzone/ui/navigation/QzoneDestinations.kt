package com.qzone.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed class QzoneDestination(val route: String) {
    data object SignIn : QzoneDestination("auth/sign_in")
    data object Register : QzoneDestination("auth/register")
    data object Feed : QzoneDestination("home/feed")
    data object SurveyDetail : QzoneDestination("survey/detail/{surveyId}") {
        const val surveyIdArg = "surveyId"
        fun createRoute(id: String) = "survey/detail/$id"
    }
    data object History : QzoneDestination("history")
    data object Rewards : QzoneDestination("rewards")
    data object RewardDetail : QzoneDestination("rewards/detail/{rewardId}") {
        const val rewardIdArg = "rewardId"
        fun createRoute(id: String) = "rewards/detail/$id"
    }
    data object Profile : QzoneDestination("profile")
    data object EditProfile : QzoneDestination("profile/edit")
    data object ProfileSettings : QzoneDestination("profile/settings")
    data object Wallet : QzoneDestination("profile/wallet")
    data object NearbyMap : QzoneDestination("map/nearby")
}

data class QzoneTopLevelDestination(
    val destination: QzoneDestination,
    val label: String,
    val icon: ImageVector
)

val TOP_LEVEL_DESTINATIONS = listOf(
    QzoneTopLevelDestination(
        destination = QzoneDestination.Feed,
        label = "Home",
        icon = Icons.Filled.Place
    ),
    QzoneTopLevelDestination(
        destination = QzoneDestination.NearbyMap,
        label = "Map",
        icon = Icons.Filled.Map
    ),
    QzoneTopLevelDestination(
        destination = QzoneDestination.History,
        label = "History",
        icon = Icons.Filled.History
    ),
    QzoneTopLevelDestination(
        destination = QzoneDestination.Rewards,
        label = "Rewards",
        icon = Icons.Filled.CardGiftcard
    ),
    QzoneTopLevelDestination(
        destination = QzoneDestination.Profile,
        label = "Profile",
        icon = Icons.Filled.AccountCircle
    )
)
