package com.example.budsbattery.service

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.companion.DevicePresenceEvent
import com.example.budsbattery.bt.associationAddress
import com.example.budsbattery.data.isSelectedDevice
import com.example.budsbattery.data.readSelectedAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Android wakes this service when the chosen buds connect or disconnect, even if the app is not running
class BudsPresenceService : CompanionDeviceService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Android 16 sends this instead of the two callbacks below
    override fun onDevicePresenceEvent(event: DevicePresenceEvent) {
        when (event.event) {
            DevicePresenceEvent.EVENT_BT_CONNECTED -> startBatteryRead(this, announce = true)
            DevicePresenceEvent.EVENT_BT_DISCONNECTED -> stopBatteryRead(this)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        handlePresence(associationInfo, connected = true)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        handlePresence(associationInfo, connected = false)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun handlePresence(info: AssociationInfo, connected: Boolean) {
        scope.launch {
            val selected = readSelectedAddress(this@BudsPresenceService)
            if (!isSelectedDevice(selected, associationAddress(info))) return@launch
            if (connected) startBatteryRead(this@BudsPresenceService, announce = true) else stopBatteryRead(this@BudsPresenceService)
        }
    }
}
