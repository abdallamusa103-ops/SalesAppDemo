package com.abdallamusa.salesapp.features.customers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.abdallamusa.salesapp.R
import com.abdallamusa.salesapp.core.theme.AppDimens
import com.abdallamusa.salesapp.core.theme.SalesAppTheme
import com.abdallamusa.salesapp.core.theme.StatusDraftAmber
import com.abdallamusa.salesapp.core.theme.StatusSaleGreen

@Composable
fun SyncStatusBadge(
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSynced) StatusSaleGreen.copy(alpha = 0.15f) else StatusDraftAmber.copy(alpha = 0.2f)
    val textColor = if (isSynced) StatusSaleGreen else StatusDraftAmber
    val textRes = if (isSynced) R.string.status_synced else R.string.status_pending

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
            .background(backgroundColor)
            .padding(
                horizontal = AppDimens.SpacingSmall,
                vertical = AppDimens.SpacingTiny
            )
    ) {
        Text(
            text = stringResource(textRes),
            color = textColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncStatusBadgePreview() {
    SalesAppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
            modifier = Modifier.padding(AppDimens.SpacingLarge)
        ) {
            SyncStatusBadge(isSynced = true)
            SyncStatusBadge(isSynced = false)
        }
    }
}