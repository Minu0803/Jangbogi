package com.minwoo.jangbogi.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minwoo.jangbogi.JangbogiApp
import com.minwoo.jangbogi.ui.screens.*
import com.minwoo.jangbogi.ui.viewmodel.HomeViewModel
import com.minwoo.jangbogi.ui.viewmodel.ShoppingHomeViewModel
import com.minwoo.jangbogi.ui.viewmodel.StoreAction

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as JangbogiApp
    NavHost(navController = nav, startDestination = "home",
        enterTransition = { slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 7 } + fadeIn(tween(220)) },
        exitTransition = { slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 14 } + fadeOut(tween(180)) },
        popEnterTransition = { slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 14 } + fadeIn(tween(220)) },
        popExitTransition = { slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 7 } + fadeOut(tween(180)) }
    ) {
        composable("home") { entry ->
            val vm: HomeViewModel = viewModel(entry, factory = app.container.homeViewModelFactory())
            val createRequested by entry.savedStateHandle.getStateFlow("create_store", false).collectAsStateWithLifecycle()
            HomeScreen(vm, onOpenList = { nav.navigate("shopping/$it") { launchSingleTop = true } },
                onOpenPlan = { nav.navigate("plan") { launchSingleTop = true } }, createRequested = createRequested,
                onCreateRequestConsumed = { entry.savedStateHandle["create_store"] = false })
        }
        composable("shopping/{listId}", arguments = listOf(navArgument("listId") { type = NavType.LongType })) { entry ->
            val homeEntry = remember(entry) { nav.getBackStackEntry("home") }
            val homeVm: HomeViewModel = viewModel(homeEntry, factory = app.container.homeViewModelFactory())
            val vm: ShoppingHomeViewModel = viewModel(entry, factory = app.container.shoppingHomeViewModelFactory())
            val operation by homeVm.operation.collectAsStateWithLifecycle()
            LaunchedEffect(operation) {
                operation?.takeIf { it.action == StoreAction.OPEN }?.let { result ->
                    if (result.error != null) nav.popBackStack("home", false)
                    else {
                        homeVm.consumeOperation(result)
                        nav.navigate("shopping/${requireNotNull(result.storeId)}") { popUpTo("home"); launchSingleTop = true }
                    }
                }
            }
            ShoppingHomeScreen(vm, onOpenPlan = { nav.navigate("plan") { launchSingleTop = true } },
                onBack = { nav.popBackStack("home", false) },
                onSelectStore = homeVm::selectStore,
                onNewStore = { homeEntry.savedStateHandle["create_store"] = true; nav.popBackStack("home", false) },
                initialQuery = homeVm.savedQuery(vm.storeId), onSaveQuery = { homeVm.saveQuery(vm.storeId, it) })
        }
        composable("plan") { PlanScreen(onBack = { nav.popBackStack() }) }
    }
}
