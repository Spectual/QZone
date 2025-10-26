package com.qzone.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun QzoneApp(appState: QzoneAppState) {
    val navBackStackEntry by appState.navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = appState.isTopLevelDestination(currentDestination)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                QzoneBottomBar(
                    destinations = TOP_LEVEL_DESTINATIONS,
                    currentDestination = currentDestination,
                    onDestinationSelected = appState::navigateTopLevel
                )
            }
        }
    ) { innerPadding ->
        QzoneNavHost(
            navController = appState.navController,
            modifier = Modifier.padding(innerPadding),
            appState = appState
        )
    }
}

@Composable
private fun QzoneBottomBar(
    destinations: List<QzoneTopLevelDestination>,
    currentDestination: NavDestination?,
    onDestinationSelected: (QzoneDestination) -> Unit
) {
    NavigationBar {
        destinations.forEach { topLevel ->
            val selected = currentDestination.isDestinationInHierarchy(topLevel.destination)
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(topLevel.destination) },
                icon = { androidx.compose.material3.Icon(imageVector = topLevel.icon, contentDescription = topLevel.label) },
                label = { Text(text = topLevel.label) }
            )
        }
    }
}

private fun NavDestination?.isDestinationInHierarchy(destination: QzoneDestination): Boolean {
    return this?.hierarchy?.any { it.route == destination.route } == true
}
