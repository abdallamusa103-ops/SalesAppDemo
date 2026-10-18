package com.abdallamusa.salesapp.features.customers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.abdallamusa.salesapp.core.theme.AppDimens
import com.abdallamusa.salesapp.core.theme.SalesAppTheme

@Composable
fun CustomerDetailHeader(
    name: String,
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.AvatarContainerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(AppDimens.AvatarIconSize)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))

        SyncStatusBadge(isSynced = isSynced)
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomerDetailHeaderPreview() {
    SalesAppTheme {
        CustomerDetailHeader(
            name = "Azure Interior",
            isSynced = false,
            modifier = Modifier.padding(AppDimens.SpacingLarge)
        )
    }
}
