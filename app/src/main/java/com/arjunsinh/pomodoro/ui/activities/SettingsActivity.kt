package com.arjunsinh.pomodoro.ui.activities

import android.app.AlertDialog
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.arjunsinh.pomodoro.R
import com.arjunsinh.pomodoro.databinding.ActivitySettingsBinding
import com.arjunsinh.pomodoro.utils.Constants

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var currentRingtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        setupEdgeToEdge()
        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top padding to the root view to clear status bar
            v.updatePadding(top = systemBars.top)
            // Apply bottom padding to the root view or scrollable container if needed
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop any playing ringtone
        currentRingtone?.stop()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        // Load durations
        val focusDuration = sharedPreferences.getLong(
            Constants.PREFS_FOCUS_DURATION,
            Constants.DEFAULT_FOCUS_DURATION
        ) / 60000
        val shortBreak = sharedPreferences.getLong(
            Constants.PREFS_SHORT_BREAK_DURATION,
            Constants.DEFAULT_SHORT_BREAK_DURATION
        ) / 60000
        val longBreak = sharedPreferences.getLong(
            Constants.PREFS_LONG_BREAK_DURATION,
            Constants.DEFAULT_LONG_BREAK_DURATION
        ) / 60000

        binding.tvFocusDuration.text = "$focusDuration minutes"
        binding.tvShortBreak.text = "$shortBreak minutes"
        binding.tvLongBreak.text = "$longBreak minutes"

        // Load notification settings
        binding.switchVibration.isChecked = sharedPreferences.getBoolean(
            Constants.PREFS_ENABLE_VIBRATION,
            true
        )
        binding.switchFocusSound.isChecked = sharedPreferences.getBoolean(
            Constants.PREFS_ENABLE_FOCUS_SOUND,
            true
        )
        binding.switchBreakSound.isChecked = sharedPreferences.getBoolean(
            Constants.PREFS_ENABLE_BREAK_SOUND,
            true
        )

        // Load selected sound
        updateSoundDisplay()

        // Load automation settings
        binding.switchAutoStart.isChecked = sharedPreferences.getBoolean(
            Constants.PREFS_AUTO_START_NEXT,
            false
        )
    }

    private fun setupListeners() {
        // Duration settings
        binding.llFocusDuration.setOnClickListener {
            showDurationPicker(
                "Focus Duration",
                Constants.PREFS_FOCUS_DURATION,
                Constants.DEFAULT_FOCUS_DURATION
            ) { minutes ->
                binding.tvFocusDuration.text = "$minutes minutes"
            }
        }

        binding.llShortBreak.setOnClickListener {
            showDurationPicker(
                "Short Break Duration",
                Constants.PREFS_SHORT_BREAK_DURATION,
                Constants.DEFAULT_SHORT_BREAK_DURATION
            ) { minutes ->
                binding.tvShortBreak.text = "$minutes minutes"
            }
        }

        binding.llLongBreak.setOnClickListener {
            showDurationPicker(
                "Long Break Duration",
                Constants.PREFS_LONG_BREAK_DURATION,
                Constants.DEFAULT_LONG_BREAK_DURATION
            ) { minutes ->
                binding.tvLongBreak.text = "$minutes minutes"
            }
        }

        // Notification settings
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(Constants.PREFS_ENABLE_VIBRATION, isChecked)
                .apply()
        }

        binding.switchFocusSound.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(Constants.PREFS_ENABLE_FOCUS_SOUND, isChecked)
                .apply()
        }

        binding.switchBreakSound.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(Constants.PREFS_ENABLE_BREAK_SOUND, isChecked)
                .apply()
        }

        // Notification sound picker
        binding.llNotificationSound.setOnClickListener {
            showSoundPicker()
        }

        // Automation settings
        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(Constants.PREFS_AUTO_START_NEXT, isChecked)
                .apply()
        }
    }

    private fun showDurationPicker(
        title: String,
        prefKey: String,
        defaultValue: Long,
        onSelected: (Int) -> Unit
    ) {
        val currentMinutes = (sharedPreferences.getLong(prefKey, defaultValue) / 60000).toInt()
        val options = (1..60).map { "$it minutes" }.toTypedArray()
        var selectedMinutes = currentMinutes

        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, currentMinutes - 1) { _, which ->
                selectedMinutes = which + 1
            }
            .setPositiveButton("OK") { dialog, _ ->
                val millis = selectedMinutes * 60000L
                sharedPreferences.edit().putLong(prefKey, millis).apply()
                onSelected(selectedMinutes)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSoundPicker() {
        // Stop any currently playing ringtone
        currentRingtone?.stop()

        val ringtoneManager = RingtoneManager(this)
        ringtoneManager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = ringtoneManager.cursor

        val soundNames = mutableListOf<String>()
        val soundUris = mutableListOf<String>()

        // Add default option
        soundNames.add("Default notification sound")
        soundUris.add("")

        // Add all available notification sounds
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = ringtoneManager.getRingtoneUri(cursor.position)
            soundNames.add(title)
            soundUris.add(uri.toString())
        }

        // Get currently selected sound
        val currentSoundUri = sharedPreferences.getString(Constants.PREFS_NOTIFICATION_SOUND_URI, "")
        val currentSelection = soundUris.indexOf(currentSoundUri)

        var selectedPosition = if (currentSelection >= 0) currentSelection else 0

        AlertDialog.Builder(this)
            .setTitle(R.string.select_sound)
            .setSingleChoiceItems(soundNames.toTypedArray(), selectedPosition) { _, which ->
                selectedPosition = which

                // Preview the sound
                currentRingtone?.stop()

                val uri = if (soundUris[which].isEmpty()) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else {
                    Uri.parse(soundUris[which])
                }

                currentRingtone = RingtoneManager.getRingtone(this, uri)
                currentRingtone?.play()

                // Stop after 1 second
                Handler(Looper.getMainLooper()).postDelayed({
                    currentRingtone?.stop()
                }, 1000)
            }
            .setPositiveButton("OK") { dialog, _ ->
                currentRingtone?.stop()
                sharedPreferences.edit()
                    .putString(Constants.PREFS_NOTIFICATION_SOUND_URI, soundUris[selectedPosition])
                    .apply()
                updateSoundDisplay()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                currentRingtone?.stop()
                dialog.dismiss()
            }
            .setOnCancelListener {
                currentRingtone?.stop()
            }
            .show()
    }

    private fun updateSoundDisplay() {
        val soundUri = sharedPreferences.getString(Constants.PREFS_NOTIFICATION_SOUND_URI, "")

        val soundName = if (soundUri.isNullOrEmpty()) {
            "Default notification sound"
        } else {
            try {
                val ringtone = RingtoneManager.getRingtone(this, Uri.parse(soundUri))
                ringtone.getTitle(this)
            } catch (e: Exception) {
                "Default notification sound"
            }
        }

        binding.tvNotificationSound.text = soundName
    }
}
