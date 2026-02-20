package com.example.llmui

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.llmui.navigation.Routes
import com.example.llmui.ui.daf.DafScreen
import com.example.llmui.ui.exercises.ExercisesScreen
import com.example.llmui.ui.faf.FafScreen
import com.example.llmui.ui.home.HomeScreen
import com.example.llmui.ui.results.ResultsScreen
import com.example.llmui.ui.settings.SettingsScreen
import com.example.llmui.ui.theme.FluencyCoachTheme

class MainActivity : ComponentActivity() {

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // jeśli odmówi, DAF/FAF po prostu nie zadziała
        }

    private fun ensureAudioPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureAudioPermission()
        setContent {
            FluencyCoachTheme {
                val navController = rememberNavController()
                FluencyCoachScaffold(navController = navController)
            }
        }
    }
}

@Composable
fun FluencyCoachScaffold(navController: NavHostController) {
    Scaffold(
        bottomBar = {
            FluencyCoachBottomBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToDaf = { navController.navigate(Routes.DAF) },
                    onNavigateToExercises = { navController.navigate(Routes.EXERCISES) },
                    onNavigateToResults = { navController.navigate(Routes.RESULTS) }
                )
            }
            composable(Routes.DAF) {
                DafScreen()
            }
            composable(Routes.FAF) {
                FafScreen()
            }
            composable(Routes.EXERCISES) {
                ExercisesScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.RESULTS) {
                ResultsScreen()
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun FluencyCoachBottomBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Start", Routes.HOME, Icons.Filled.Home),
        BottomNavItem("DAF", Routes.DAF, Icons.Filled.Headset),
        BottomNavItem("FAF", Routes.FAF, Icons.Filled.Headset),
        BottomNavItem("Ćwiczenia", Routes.EXERCISES, Icons.Filled.List),
        BottomNavItem("Ustawienia", Routes.SETTINGS, Icons.Filled.Settings),
        BottomNavItem("Wyniki", Routes.RESULTS, Icons.Filled.Assessment)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            NavigationBarItem(
                selected = currentDestination.isRouteInHierarchy(item.route),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}

private fun NavDestination?.isRouteInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}
