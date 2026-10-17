package com.abdallamusa.salesapp.navigation

sealed class NavRoute(val route: String) {
    data object Login : NavRoute("login")
    data object Main : NavRoute("main")
    data object CustomerDetails : NavRoute("customer_details/{customerId}") {
        const val ARG_CUSTOMER_ID = "customerId"
        fun createRoute(customerId: Int): String = "customer_details/$customerId"
    }
}
