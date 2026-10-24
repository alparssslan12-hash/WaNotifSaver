package com.smokshop.wanotif

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var fabClear: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        fabClear = findViewById(R.id.fabClear)

        adapter = MessageAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Tüm mesajları sil")
                .setMessage("Kaydedilen tüm mesajlar silinecek. Emin misin?")
                .setPositiveButton("Sil") { _, _ ->
                    clearMessages()
                }
                .setNegativeButton("İptal", null)
                .show()
        }

        checkPermissionAndStart()
    }

    override fun onResume() {
        super.onResume()
        loadMessages()
    }

    private fun checkPermissionAndStart() {
        if (!isNotificationListenerEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("İzin Gerekli")
                .setMessage("WhatsApp bildirimlerini kaydetmek için Bildirim Erişim iznini açman gerekiyor.\n\nAçılan sayfada 'WA Bildirim Kaydedici'yi bul ve etkinleştir.")
                .setPositiveButton("İzin Ver") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("İptal") { _, _ ->
                    Toast.makeText(this, "İzin olmadan çalışmaz!", Toast.LENGTH_LONG).show()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":")
            val myComponent = ComponentName(packageName, NotifListenerService::class.java.name)
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && myComponent == cn) return true
            }
        }
        return false
    }

    private fun loadMessages() {
        val prefs = getSharedPreferences(NotifListenerService.PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(NotifListenerService.KEY_MESSAGES, "[]") ?: "[]"
        val type = object : TypeToken<List<SavedMessage>>() {}.type
        val messages: List<SavedMessage> = Gson().fromJson(json, type)
        adapter.updateMessages(messages)

        // Başlık güncelle
        supportActionBar?.title = "WA Bildirimler (${messages.size})"
    }

    private fun clearMessages() {
        val prefs = getSharedPreferences(NotifListenerService.PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(NotifListenerService.KEY_MESSAGES, "[]").apply()
        loadMessages()
        Toast.makeText(this, "Tüm mesajlar silindi", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadMessages()
                true
            }
            R.id.action_permission -> {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
