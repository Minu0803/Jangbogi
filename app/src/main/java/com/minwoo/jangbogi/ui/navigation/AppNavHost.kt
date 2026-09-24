package com.minwoo.jangbogi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minwoo.jangbogi.ui.screens.HomeScreen
import com.minwoo.jangbogi.ui.screens.ListScreen
import com.minwoo.jangbogi.ui.screens.PlanScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable(route = "home") {
            HomeScreen(
                onOpenList = { listId -> navController.navigate("list/$listId") },
                onOpenPlan = { navController.navigate("plan") }
            )
        }
        composable(
            route = "list/{listId}",
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { entry ->
            val listId = entry.arguments?.getLong("listId") ?: 0L
            ListScreen(listId = listId, onBack = { navController.popBackStack() })
        }
        composable(route = "plan") {
            PlanScreen(onBack = { navController.popBackStack() })
        }
    }
}
