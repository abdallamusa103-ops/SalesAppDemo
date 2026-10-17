package com.abdallamusa.salesapp.features.customers.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.abdallamusa.salesapp.core.theme.SalesAppTheme

@Composable
fun CustomerPhoneEditor(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    isSaving: Boolean,
    saveSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Phone Number (Offline-First)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = onPhoneChange,
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (saveSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Saved locally! Will sync with Odoo automatically.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSaveClick,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Saving…" else "Update Phone")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomerPhoneEditorPreview() {
    SalesAppTheme {
        CustomerPhoneEditor(
            phoneInput = "+1 555-0199",
            onPhoneChange = {},
            onSaveClick = {},
            isSaving = false,
            saveSuccess = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
