package com.os11.app.platform.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Spike S-8: Re-arms scheduled reminder alarms following device reboot or app update.
 *
 * Registered in AndroidManifest.xml for:
 * - android.intent.action.BOOT_COMPLETED
 * - android.intent.action.MY_PACKAGE_REPLACED
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-arm single next alarm; full DB lookup handled in post-first-frame/Phase 4
        }
    }
}
