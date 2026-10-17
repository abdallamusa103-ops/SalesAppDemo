package com.abdallamusa.salesapp.features.customers.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abdallamusa.salesapp.core.theme.SalesAppTheme
import com.abdallamusa.salesapp.features.customers.domain.model.Customer

@Composable
fun CustomerCard(
    customer: Customer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                SyncStatusBadge(isSynced = customer.isSynced)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!customer.phone.isNullOrBlank()) {
                Text(
                    text = "📞 ${customer.phone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!customer.city.isNullOrBlank()) {
                Text(
                    text = "📍 ${customer.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomerCardPreview() {
    SalesAppTheme {
        CustomerCard(
            customer = Customer(
                id = 1,
                name = "Azure Interior",
                phone = "+1 555-0199",
                email = "azure@example.com",
                street = "4557 De Silva St",
                city = "Fremont",
                isSynced = true,
                lastModified = System.currentTimeMillis()
            ),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}