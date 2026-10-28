package com.example.budsbattery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.budsbattery.R
import com.example.budsbattery.bt.PairedDevice

private val ITEM_PADDING = 12.dp

/**
 * Lists paired devices so the user picks the buds by name.
 *
 * @param devices - paired devices, likely buds first
 * @param onPick - called with the chosen device
 * @param onDismiss - called when the dialog closes without a pick
 */
@Composable
fun BudsPickerDialog(devices: List<PairedDevice>, onPick: (PairedDevice) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_buds_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (device in devices) DeviceItem(device, onPick)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun DeviceItem(device: PairedDevice, onPick: (PairedDevice) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(device) }
            .padding(vertical = ITEM_PADDING),
    ) {
        Text(device.name, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = device.address,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
