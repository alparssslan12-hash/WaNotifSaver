package com.smokshop.wanotif

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class NotifListenerService : NotificationListenerService() {

    companion object {
        const val CHANNEL_ID = "wa_notif_saver_channel"
        const val NOTIF_ID = 1001
        const val PREFS_NAME = "wa_notif_prefs"
        const val KEY_MESSAGES = "saved_messages"
        val WA_PACKAGES = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b"  // WhatsApp Business
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildForegroundNotification())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName !in WA_PACKAGES) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // Grup bildirimlerini atla (sadece gerçek mesajları kaydet)
        if (text.isBlank() || title.isBlank()) return
        // "N yeni mesaj" tarzı özet bildirimlerini atla
        if (text.matches(Regex("\\d+ yeni mesaj.*")) || text.matches(Regex("\\d+ new message.*"))) return

        val timestamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val isGroup = sbn.notification.extras.getBoolean("android.isGroupConversation", false)
        val type = if (isGroup) "Grup" else "DM"

        val msg = SavedMessage(
            timestamp = timestamp,
            sender = title,
            message = text,
            type = type,
            packageName = sbn.packageName
        )

        saveMessage(msg)
    }

    private fun saveMessage(msg: SavedMessage) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val gson = Gson()
        val existing = prefs.getString(KEY_MESSAGES, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<SavedMessage>>() {}.type
        val list: MutableList<SavedMessage> = gson.fromJson(existing, type)

        list.add(0, msg) // Yeniler üste gelsin

        // Max 1000 mesaj tut
        if (list.size > 1000) list.subList(1000, list.size).clear()

        prefs.edit().putString(KEY_MESSAGES, gson.toJson(list)).apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WA Bildirim Kaydedici",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "WhatsApp bildirimlerini kaydeder"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WA Bildirim Kaydedici")
            .setContentText("WhatsApp bildirimleri kaydediliyor...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
