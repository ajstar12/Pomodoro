package com.arjunsinh.pomodoro.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.arjunsinh.pomodoro.databinding.ActivitySettingsBinding
import com.arjunsinh.pomodoro.utils.Constants

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        setupToolbar()
        loadSettings()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadSettings() {
        val focusDuration = sharedPreferences.getLong(Constants.PREFS_FOCUS_DURATION, Constants.DEFAULT_FOCUS_DURATION) / 60000
        val shortBreak = sharedPreferences.getLong(Constants.PREFS_SHORT_BREAK_DURATION, Constants.DEFAULT_SHORT_BREAK_DURATION) / 60000
        val longBreak = sharedPreferences.getLong(Constants.PREFS_LONG_BREAK_DURATION, Constants.DEFAULT_LONG_BREAK_DURATION) / 60000

        binding.tvFocusDuration.text = "$focusDuration minutes"
        binding.tvShortBreak.text = "$shortBreak minutes"
        binding.tvLongBreak.text = "$longBreak minutes"

        binding.switchVibration.isChecked = sharedPreferences.getBoolean(Constants.PREFS_ENABLE_VIBRATION, true)
        binding.switchSound.isChecked = sharedPreferences.getBoolean(Constants.PREFS_ENABLE_SOUND, true)
        binding.switchAutoStart.isChecked = sharedPreferences.getBoolean(Constants.PREFS_AUTO_START_NEXT, false)
    }

    private fun setupClickListeners() {
        binding.llFocusDuration.setOnClickListener {
            showDurationPicker("Focus Duration", Constants.PREFS_FOCUS_DURATION, Constants.DEFAULT_FOCUS_DURATION)
        }

        binding.llShortBreak.setOnClickListener {
            showDurationPicker("Short Break", Constants.PREFS_SHORT_BREAK_DURATION, Constants.DEFAULT_SHORT_BREAK_DURATION)
        }

        binding.llLongBreak.setOnClickListener {
            showDurationPicker("Long Break", Constants.PREFS_LONG_BREAK_DURATION, Constants.DEFAULT_LONG_BREAK_DURATION)
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(Constants.PREFS_ENABLE_VIBRATION, isChecked).apply()
        }

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(Constants.PREFS_ENABLE_SOUND, isChecked).apply()
        }

        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(Constants.PREFS_AUTO_START_NEXT, isChecked).apply()
        }
    }

    private fun showDurationPicker(title: String, prefKey: String, defaultValue: Long) {
        val currentValue = (sharedPreferences.getLong(prefKey, defaultValue) / 60000).toInt()

        val numberPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 60
            value = currentValue
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(numberPicker)
            .setPositiveButton("Save") { _, _ ->
                val newValue = numberPicker.value.toLong() * 60000
                sharedPreferences.edit().putLong(prefKey, newValue).apply()
                loadSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
