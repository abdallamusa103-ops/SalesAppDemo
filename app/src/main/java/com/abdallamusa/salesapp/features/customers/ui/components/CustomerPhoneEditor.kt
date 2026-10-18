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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.abdallamusa.salesapp.R
import com.abdallamusa.salesapp.core.theme.AppDimens
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
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(AppDimens.SpacingLarge)) {
            Text(
                text = stringResource(R.string.phone_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.phone_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (saveSuccess) {
                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.phone_saved_message),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))

            Button(
                onClick = onSaveClick,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSaving) {
                        stringResource(R.string.saving_button)
                    } else {
                        stringResource(R.string.edit_phone_title)
                    }
                )
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
            modifier = Modifier.padding(AppDimens.SpacingLarge)
        )
    }
}
