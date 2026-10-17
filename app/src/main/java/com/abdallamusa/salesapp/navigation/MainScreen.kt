package com.abdallamusa.salesapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.abdallamusa.salesapp.R
import com.abdallamusa.salesapp.core.theme.SalesAppTheme
import com.abdallamusa.salesapp.features.customers.ui.screens.CustomerListScreen
import com.abdallamusa.salesapp.features.sales.ui.screens.SalesOrdersScreen

enum class MainTab {
    CUSTOMERS,
    SALES_ORDERS
}

@Composable
fun MainScreen(
    isInternalUser: Boolean,
    onNavigateToCustomerDetails: (customerId: Int) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    MainContent(
        isInternalUser = isInternalUser,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
        onNavigateToCustomerDetails = onNavigateToCustomerDetails,
        onRequestLogout = { showLogoutDialog = true },
        modifier = modifier
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = stringResource(R.string.logout_dialog_title)) },
            text = { Text(text = stringResource(R.string.logout_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(text = stringResource(R.string.action_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
fun MainContent(
    isInternalUser: Boolean,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToCustomerDetails: (customerId: Int) -> Unit,
    onRequestLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        bottomBar = {
            if (isInternalUser) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = { onTabSelected(0) },
                        icon = { Icon(imageVector = Icons.Default.People, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_customers)) }
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = { onTabSelected(1) },
                        icon = { Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_sales_orders)) }
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedTabIndex) {
                0 -> {
                    CustomerListScreen(
                        onCustomerClick = onNavigateToCustomerDetails,
                        onLogoutClick = onRequestLogout
                    )
                }
                1 -> {
                    if (isInternalUser) {
                        SalesOrdersScreen(
                            onLogoutClick = onRequestLogout
                        )
                    } else {
                        CustomerListScreen(
                            onCustomerClick = onNavigateToCustomerDetails,
                            onLogoutClick = onRequestLogout
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainContentInternalUserPreview() {
    SalesAppTheme {
        MainContent(
            isInternalUser = true,
            selectedTabIndex = 0,
            onTabSelected = {},
            onNavigateToCustomerDetails = {},
            onRequestLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainContentExternalUserPreview() {
    SalesAppTheme {
        MainContent(
            isInternalUser = false,
            selectedTabIndex = 0,
            onTabSelected = {},
            onNavigateToCustomerDetails = {},
            onRequestLogout = {}
        )
    }
}
