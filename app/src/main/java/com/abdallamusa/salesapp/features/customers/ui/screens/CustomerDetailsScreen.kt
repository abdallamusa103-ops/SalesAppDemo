package com.abdallamusa.salesapp.features.customers.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.abdallamusa.salesapp.features.customers.ui.components.CustomerDetailHeader
import com.abdallamusa.salesapp.features.customers.ui.components.CustomerInfoSection
import com.abdallamusa.salesapp.features.customers.ui.components.CustomerPhoneEditor
import com.abdallamusa.salesapp.features.customers.ui.viewmodel.CustomerDetailsUiState
import com.abdallamusa.salesapp.features.customers.ui.viewmodel.CustomerDetailsViewModel

@Composable
fun CustomerDetailsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    CustomerDetailsContent(
        uiState = uiState,
        onPhoneChange = viewModel::onPhoneChanged,
        onSavePhone = viewModel::savePhone,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsContent(
    uiState: CustomerDetailsUiState,
    onPhoneChange: (String) -> Unit,
    onSavePhone: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.customer_details_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_desc)
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val customer = uiState.customer
            if (customer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    CustomerDetailHeader(
                        name = customer.name,
                        isSynced = customer.isSynced
                    )

                    CustomerInfoSection(
                        email = customer.email,
                        street = customer.street,
                        city = customer.city
                    )

                    CustomerPhoneEditor(
                        phoneInput = uiState.phoneInput,
                        onPhoneChange = onPhoneChange,
                        onSaveClick = onSavePhone,
                        isSaving = uiState.isSaving,
                        saveSuccess = uiState.saveSuccess
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomerDetailsContentPreview() {
    SalesAppTheme {
        CustomerDetailsContent(
            uiState = CustomerDetailsUiState(
                customer = Customer(
                    id = 1,
                    name = "Azure Interior",
                    phone = "+1 555-0199",
                    email = "azure@interior.com",
                    street = "4557 De Silva St",
                    city = "Fremont",
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                ),
                phoneInput = "+1 555-0199",
                isLoading = false,
                isSaving = false,
                saveSuccess = true
            ),
            onPhoneChange = {},
            onSavePhone = {},
            onBackClick = {}
        )
    }
}
