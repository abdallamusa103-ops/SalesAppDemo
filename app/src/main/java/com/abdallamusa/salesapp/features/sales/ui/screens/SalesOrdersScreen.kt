package com.abdallamusa.salesapp.features.sales.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.abdallamusa.salesapp.R
import com.abdallamusa.salesapp.core.theme.AppDimens
import com.abdallamusa.salesapp.core.theme.SalesAppTheme
import com.abdallamusa.salesapp.core.ui.components.ErrorBanner
import com.abdallamusa.salesapp.core.ui.components.RefreshIconButton
import com.abdallamusa.salesapp.features.sales.domain.OrderLine
import com.abdallamusa.salesapp.features.sales.domain.SalesOrder
import com.abdallamusa.salesapp.features.sales.ui.components.OrderDetailsBottomSheet
import com.abdallamusa.salesapp.features.sales.ui.components.SalesOrderCard
import com.abdallamusa.salesapp.features.sales.ui.viewmodel.SalesOrderViewModel
import com.abdallamusa.salesapp.features.sales.ui.viewmodel.SalesOrdersUiState

@Composable
fun SalesOrdersScreen(
    modifier: Modifier = Modifier,
    onLogoutClick: (() -> Unit)? = null,
    viewModel: SalesOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SalesOrdersContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onOrderClick = viewModel::onOrderClick,
        onDismissDetails = viewModel::onDismissDetails,
        onConfirmOrder = viewModel::confirmOrder,
        onDismissError = viewModel::dismissError,
        onLogoutClick = onLogoutClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrdersContent(
    uiState: SalesOrdersUiState,
    onRefresh: () -> Unit,
    onOrderClick: (SalesOrder) -> Unit,
    onDismissDetails: () -> Unit,
    onConfirmOrder: (orderId: Int) -> Unit,
    modifier: Modifier = Modifier,
    onDismissError: () -> Unit = {},
    onLogoutClick: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.sales_orders_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        RefreshIconButton(
                            isRefreshing = uiState.isLoading,
                            onClick = onRefresh
                        )
                        if (onLogoutClick != null) {
                            IconButton(onClick = onLogoutClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = stringResource(R.string.action_logout)
                                )
                            }
                        }
                    }
                )
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.orders.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_sales_orders),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = AppDimens.ScreenHorizontalPadding,
                        vertical = AppDimens.SpacingSmall
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingCompact)
                ) {
                    items(
                        items = uiState.orders,
                        key = { it.id }
                    ) { order ->
                        SalesOrderCard(
                            order = order,
                            onClick = { onOrderClick(order) }
                        )
                    }
                }
            }

            ErrorBanner(
                errorMessage = uiState.errorMessage,
                onDismiss = onDismissError,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            uiState.selectedOrder?.let { selected ->
                OrderDetailsBottomSheet(
                    order = selected,
                    onDismiss = onDismissDetails,
                    onConfirmOrder = onConfirmOrder,
                    isConfirming = uiState.isConfirming,
                    errorMessage = uiState.errorMessage,
                    onDismissError = onDismissError
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SalesOrdersContentPreview() {
    SalesAppTheme {
        SalesOrdersContent(
            uiState = SalesOrdersUiState(
                orders = listOf(
                    SalesOrder(
                        id = 1,
                        name = "SO001",
                        customerId = 14,
                        customerName = "Azure Interior",
                        dateOrder = "2026-10-16 10:15:00",
                        amountTotal = 2649.36,
                        currency = "LE",
                        state = "draft",
                        products = listOf(
                            OrderLine(id = 1, orderId = 1, productName = "Office Desk", quantity = 1.0)
                        )
                    ),
                    SalesOrder(
                        id = 2,
                        name = "SO002",
                        customerId = 22,
                        customerName = "Deco Addict",
                        dateOrder = "2026-10-15 16:40:00",
                        amountTotal = 850.00,
                        currency = "LE",
                        state = "sale",
                        products = emptyList()
                    )
                ),
                isLoading = false
            ),
            onRefresh = {},
            onOrderClick = {},
            onDismissDetails = {},
            onConfirmOrder = {}
        )
    }
}
