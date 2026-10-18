package com.abdallamusa.salesapp.features.sales.ui.components

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
import com.abdallamusa.salesapp.core.theme.StatusCancelledRed
import com.abdallamusa.salesapp.core.theme.StatusDraftAmber
import com.abdallamusa.salesapp.core.theme.StatusSaleGreen

@Composable
fun OrderStatusChip(
    state: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, textRes) = when (state.lowercase()) {
        "sale" -> Triple(StatusSaleGreen.copy(alpha = 0.15f), StatusSaleGreen, R.string.status_confirmed)
        "cancel" -> Triple(StatusCancelledRed.copy(alpha = 0.15f), StatusCancelledRed, R.string.status_cancelled)
        else -> Triple(StatusDraftAmber.copy(alpha = 0.18f), StatusDraftAmber, R.string.status_quotation)
    }

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
private fun OrderStatusChipPreview() {
    SalesAppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
            modifier = Modifier.padding(AppDimens.SpacingLarge)
        ) {
            OrderStatusChip(state = "draft")
            OrderStatusChip(state = "sale")
            OrderStatusChip(state = "cancel")
        }
    }
}
