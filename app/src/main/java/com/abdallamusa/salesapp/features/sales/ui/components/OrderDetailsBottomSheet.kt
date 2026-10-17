package com.abdallamusa.salesapp.features.sales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abdallamusa.salesapp.R
import com.abdallamusa.salesapp.core.theme.SalesAppTheme
import com.abdallamusa.salesapp.features.sales.domain.OrderLine
import com.abdallamusa.salesapp.features.sales.domain.SalesOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsBottomSheet(
    order: SalesOrder,
    onDismiss: () -> Unit,
    onConfirmOrder: (orderId: Int) -> Unit,
    isConfirming: Boolean,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        OrderDetailsContent(
            order = order,
            onConfirmOrder = onConfirmOrder,
            isConfirming = isConfirming,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun OrderDetailsContent(
    order: SalesOrder,
    onConfirmOrder: (orderId: Int) -> Unit,
    isConfirming: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = order.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = order.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OrderStatusChip(state = order.state)
        }

        if (!order.dateOrder.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📅 ${order.dateOrder}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.products_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (order.products.isEmpty()) {
            Text(
                text = "No products loaded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                order.products.forEach { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        val qtyText = if (product.quantity % 1.0 == 0.0) {
                            product.quantity.toInt().toString()
                        } else {
                            product.quantity.toString()
                        }
                        Text(
                            text = "× $qtyText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = order.formattedTotal,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (order.isConfirmable) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onConfirmOrder(order.id) },
                enabled = !isConfirming,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.confirm_order_button),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderDetailsContentPreview() {
    SalesAppTheme {
        OrderDetailsContent(
            order = SalesOrder(
                id = 1,
                name = "SO001",
                customerId = 14,
                customerName = "Azure Interior",
                dateOrder = "2026-10-16 14:30:00",
                amountTotal = 2649.36,
                currency = "LE",
                state = "draft",
                products = listOf(
                    OrderLine(id = 1, orderId = 1, productName = "Office Chair", quantity = 2.0),
                    OrderLine(id = 2, orderId = 1, productName = "Acoustic Screen", quantity = 1.0)
                )
            ),
            onConfirmOrder = {},
            isConfirming = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}
