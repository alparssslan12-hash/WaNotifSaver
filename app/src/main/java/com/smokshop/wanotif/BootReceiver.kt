package com.smokshop.wanotif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Telefon yeniden başladığında servis otomatik devreye girer
// (NotificationListenerService zaten sistem tarafından yönetilir,
//  bu receiver gerekirse ekstra başlatma için kullanılabilir)
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // NotificationListenerService sistem tarafından otomatik başlatılır
            // Ekstra bir şey yapmaya gerek yok
        }
    }
}
