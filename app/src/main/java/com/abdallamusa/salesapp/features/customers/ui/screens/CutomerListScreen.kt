package com.abdallamusa.salesapp.features.customers.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abdallamusa.salesapp.R
import com.abdallamusa.salesapp.core.theme.SalesAppTheme
import com.abdallamusa.salesapp.features.customers.domain.model.Customer
import com.abdallamusa.salesapp.features.customers.ui.CustomerListUiState
import com.abdallamusa.salesapp.features.customers.ui.components.CustomerCard
import com.abdallamusa.salesapp.features.customers.ui.components.CustomerSearchBar
import com.abdallamusa.salesapp.features.customers.ui.viewmodel.CustomerViewModel

@Composable
fun CustomerListScreen(
    onCustomerClick: (customerId: Int) -> Unit,
    modifier: Modifier = Modifier,
    onLogoutClick: (() -> Unit)? = null,
    viewModel: CustomerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CustomerListContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onCustomerClick = { customer -> onCustomerClick(customer.id) },
        onLogoutClick = onLogoutClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListContent(
    uiState: CustomerListUiState,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCustomerClick: (Customer) -> Unit,
    modifier: Modifier = Modifier,
    onLogoutClick: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.customers_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    }
                    if (onLogoutClick != null) {
                        IconButton(onClick = onLogoutClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            CustomerSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.customers.isEmpty() && !uiState.isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_customers),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.customers,
                        key = { it.id }
                    ) { customer ->
                        CustomerCard(
                            customer = customer,
                            onClick = { onCustomerClick(customer) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomerListContentPreview() {
    SalesAppTheme {
        CustomerListContent(
            uiState = CustomerListUiState(
                customers = listOf(
                    Customer(
                        id = 1,
                        name = "Azure Interior",
                        phone = "+1 555-0199",
                        email = "azure@example.com",
                        street = "4557 De Silva St",
                        city = "Fremont",
                        isSynced = true,
                        lastModified = System.currentTimeMillis()
                    ),
                    Customer(
                        id = 2,
                        name = "Deco Addict",
                        phone = "+1 555-0144",
                        email = "deco@example.com",
                        street = "77 Isaac St",
                        city = "Oakland",
                        isSynced = false,
                        lastModified = System.currentTimeMillis()
                    )
                ),
                isRefreshing = false
            ),
            onRefresh = {},
            onSearchQueryChange = {},
            onCustomerClick = {}
        )
    }
}