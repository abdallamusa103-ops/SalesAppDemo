package com.abdallamusa.salesapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abdallamusa.salesapp.features.auth.ui.screens.LoginScreen
import com.abdallamusa.salesapp.features.customers.ui.screens.CustomerDetailsScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()

    if (sessionState == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val session = sessionState
    val isLoggedIn = session?.isLoggedIn == true
    val startDestination = if (isLoggedIn) NavRoute.Main.route else NavRoute.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.Main.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NavRoute.Main.route) {
            MainScreen(
                isInternalUser = session?.isInternalUser ?: false,
                onNavigateToCustomerDetails = { customerId ->
                    navController.navigate(NavRoute.CustomerDetails.createRoute(customerId))
                },
                onLogout = {
                    viewModel.logout {
                        navController.navigate(NavRoute.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = NavRoute.CustomerDetails.route,
            arguments = listOf(
                navArgument(NavRoute.CustomerDetails.ARG_CUSTOMER_ID) {
                    type = NavType.IntType
                }
            )
        ) {
            CustomerDetailsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
