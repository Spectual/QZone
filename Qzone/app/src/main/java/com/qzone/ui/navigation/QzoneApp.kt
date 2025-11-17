package com.qzone.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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
        containerColor = Color.Transparent,
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
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
    ) {
        NavigationBar(
            modifier = Modifier
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    shape = shape
                ),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 8.dp
        ) {
            destinations.forEach { topLevel ->
                val selected = currentDestination.isDestinationInHierarchy(topLevel.destination)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onDestinationSelected(topLevel.destination) },
                    icon = { androidx.compose.material3.Icon(imageVector = topLevel.icon, contentDescription = topLevel.label) },
                    label = { Text(text = topLevel.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

private fun NavDestination?.isDestinationInHierarchy(destination: QzoneDestination): Boolean {
    return this?.hierarchy?.any { it.route == destination.route } == true
}
