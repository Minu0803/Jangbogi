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
import com.minwoo.jangbogi.ui.screens.ShoppingHomeScreen
import com.minwoo.jangbogi.ui.viewmodel.ShoppingHomeViewModel
import com.minwoo.jangbogi.JangbogiApp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as JangbogiApp
    NavHost(navController = navController, startDestination = "shopping") {
        composable(route = "shopping") { entry ->
            val vm: ShoppingHomeViewModel = viewModel(entry, factory = app.container.shoppingHomeViewModelFactory())
            ShoppingHomeScreen(
                vm = vm,
                onOpenPlan = { navController.navigate("plan") },
                onManageLists = { navController.navigate("home") }
            )
        }
        composable(route = "home") {
            val shoppingEntry = remember(navController) { navController.getBackStackEntry("shopping") }
            val shoppingVm: ShoppingHomeViewModel = viewModel(shoppingEntry, factory = app.container.shoppingHomeViewModelFactory())
            HomeScreen(
                onOpenList = { listId -> shoppingVm.selectList(listId); navController.popBackStack() },
                onOpenPlan = { navController.navigate("plan") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = "plan") {
            PlanScreen(onBack = { navController.popBackStack() })
        }
    }
}
