package com.qzone.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qzone.feature.auth.AuthViewModel
import com.qzone.feature.auth.ui.RegisterScreen
import com.qzone.feature.auth.ui.SignInScreen
import com.qzone.feature.feed.FeedViewModel
import com.qzone.feature.feed.ui.FeedScreen
import com.qzone.feature.history.HistoryViewModel
import com.qzone.feature.history.ui.HistoryScreen
import com.qzone.feature.profile.ProfileViewModel
import com.qzone.feature.profile.ui.EditProfileScreen
import com.qzone.feature.profile.ui.ProfileScreen
import com.qzone.feature.profile.ui.ProfileSettingsScreen
import com.qzone.feature.rewards.RewardDetailViewModel
import com.qzone.feature.rewards.RewardsViewModel
import com.qzone.feature.rewards.ui.RewardDetailScreen
import com.qzone.feature.rewards.ui.RewardsScreen
import com.qzone.feature.survey.SurveyViewModel
import com.qzone.feature.survey.ui.SurveyScreen

@Composable
fun QzoneNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier,
    appState: QzoneAppState
) {
    NavHost(
        navController = navController,
        startDestination = QzoneDestination.SignIn.route,
        modifier = modifier
    ) {
        composable(QzoneDestination.SignIn.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(appState.userRepository))
            SignInScreen(
                state = authViewModel.uiState,
                onEmailChanged = authViewModel::onEmailChanged,
                onPasswordChanged = authViewModel::onPasswordChanged,
                onSignIn = {
                    authViewModel.signIn {
                        navController.navigate(QzoneDestination.Feed.route) {
                            popUpTo(QzoneDestination.SignIn.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate(QzoneDestination.Register.route) }
            )
        }
        composable(QzoneDestination.Register.route) {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(appState.userRepository))
            RegisterScreen(
                state = authViewModel.registerState,
                onUsernameChanged = authViewModel::onRegisterUsernameChanged,
                onEmailChanged = authViewModel::onRegisterEmailChanged,
                onPasswordChanged = authViewModel::onRegisterPasswordChanged,
                onRegister = {
                    authViewModel.register {
                        navController.navigate(QzoneDestination.Feed.route) {
                            popUpTo(QzoneDestination.SignIn.route) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(QzoneDestination.Feed.route) {
            val feedViewModel: FeedViewModel = viewModel(
                factory = FeedViewModel.factory(appState.surveyRepository, appState.locationRepository)
            )
            FeedScreen(
                state = feedViewModel.uiState,
                onRefresh = feedViewModel::refresh,
                onSurveySelected = { surveyId ->
                    navController.navigate(QzoneDestination.SurveyDetail.createRoute(surveyId))
                },
                onLocationPermissionGranted = feedViewModel::onLocationPermissionGranted
            )
        }
        composable(
            route = QzoneDestination.SurveyDetail.route,
            arguments = listOf(navArgument(QzoneDestination.SurveyDetail.surveyIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val surveyId = backStackEntry.arguments?.getString(QzoneDestination.SurveyDetail.surveyIdArg).orEmpty()
            val surveyViewModel: SurveyViewModel = viewModel(
                factory = SurveyViewModel.factory(
                    appState.surveyRepository,
                    appState.userRepository,
                    surveyId
                )
            )
            SurveyScreen(
                state = surveyViewModel.uiState,
                onPrevious = surveyViewModel::onPrevious,
                onNext = surveyViewModel::onNext,
                onClose = { navController.popBackStack() },
                onSubmit = { surveyViewModel.submit() },
                onCompletionAcknowledged = { navController.popBackStack() },
                onAnswerChanged = surveyViewModel::onAnswerChanged
            )
        }
        composable(QzoneDestination.Profile.route) {
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(appState.userRepository, appState.rewardRepository))
            ProfileScreen(
                state = profileViewModel.uiState,
                onViewRewards = { navController.navigate(QzoneDestination.Rewards.route) },
                onHistoryClick = { navController.navigate(QzoneDestination.History.route) },
                onOpenSettings = { navController.navigate(QzoneDestination.ProfileSettings.route) }
            )
        }
        composable(QzoneDestination.EditProfile.route) {
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(appState.userRepository, appState.rewardRepository))
            EditProfileScreen(
                state = profileViewModel.editState,
                onNameChanged = profileViewModel::onNameChanged,
                onEmailChanged = profileViewModel::onEmailChanged,
                onPasswordChanged = profileViewModel::onPasswordChanged,
                onCountryChanged = profileViewModel::onCountryChanged,
                onSave = {
                    profileViewModel.saveEdits()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(QzoneDestination.ProfileSettings.route) {
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(appState.userRepository, appState.rewardRepository))
            ProfileSettingsScreen(
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(QzoneDestination.EditProfile.route) },
                isDarkTheme = appState.useDarkTheme,
                onToggleDarkMode = appState.onToggleDarkTheme,
                onLogout = {
                    profileViewModel.signOut {
                        navController.navigate(QzoneDestination.SignIn.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(QzoneDestination.History.route) {
            val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(appState.userRepository))
            HistoryScreen(
                state = historyViewModel.uiState,
                onQueryChanged = historyViewModel::onQueryChange
            )
        }
        composable(QzoneDestination.Rewards.route) {
            val rewardsViewModel: RewardsViewModel = viewModel(factory = RewardsViewModel.factory(appState.rewardRepository))
            RewardsScreen(
                state = rewardsViewModel.uiState,
                onRewardSelected = { id -> navController.navigate(QzoneDestination.RewardDetail.createRoute(id)) }
            )
        }
        composable(
            route = QzoneDestination.RewardDetail.route,
            arguments = listOf(navArgument(QzoneDestination.RewardDetail.rewardIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val rewardId = backStackEntry.arguments?.getString(QzoneDestination.RewardDetail.rewardIdArg).orEmpty()
            val rewardDetailViewModel: RewardDetailViewModel = viewModel(factory = RewardDetailViewModel.factory(appState.rewardRepository, rewardId))
            RewardDetailScreen(
                state = rewardDetailViewModel.uiState,
                onBack = { navController.popBackStack() },
                onRedeem = { /* Placeholder for backend integration */ }
            )
        }
    }
}
