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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.abdallamusa.salesapp.R
import com.abdallamusa.salesapp.core.theme.AppDimens
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
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(AppDimens.SpacingLarge)) {
            Text(
                text = stringResource(R.string.contact_address_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            InfoItem(
                icon = stringResource(R.string.icon_email),
                label = stringResource(R.string.email_label),
                value = email ?: stringResource(R.string.no_email_provided)
            )
            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
            InfoItem(
                icon = stringResource(R.string.icon_location),
                label = stringResource(R.string.address_label),
                value = listOfNotNull(street, city).joinToString(", ").ifEmpty {
                    stringResource(R.string.no_address_provided)
                }
            )
        }
    }
}

@Composable
private fun InfoItem(icon: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
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
            modifier = Modifier.padding(AppDimens.SpacingLarge)
        )
    }
}
