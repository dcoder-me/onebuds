package com.example.budsbattery.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.budsbattery.data.isSelectedDevice
import com.example.budsbattery.data.readSelectedAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Safety net: starts the read service if the companion wake-up was missed
class AclReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val address = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)?.address
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAcl(context, intent.action, address)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleAcl(context: Context, action: String?, address: String?) {
        val selected = readSelectedAddress(context)
        if (!isSelectedDevice(selected, address)) return
        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> startBatteryRead(context)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> stopBatteryRead(context)
        }
    }
}
