package com.abdallamusa.salesapp.features.customers.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

@Composable
fun CustomerInfoSection(
    email: String?,
    street: String?,
    city: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contact & Address",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoItem(icon = "✉️", label = "Email", value = email ?: "No email provided")
            Spacer(modifier = Modifier.height(8.dp))
            InfoItem(icon = "📍", label = "Address", value = listOfNotNull(street, city).joinToString(", ").ifEmpty { "No address provided" })
        }
    }
}

@Composable
private fun InfoItem(icon: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(text = value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomerInfoSectionPreview() {
    SalesAppTheme {
        CustomerInfoSection(
            email = "azure@interior.com",
            street = "4557 De Silva St",
            city = "Fremont",
            modifier = Modifier.padding(16.dp)
        )
    }
}
