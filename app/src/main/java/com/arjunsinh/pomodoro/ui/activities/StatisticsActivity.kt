package com.arjunsinh.pomodoro.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.arjunsinh.pomodoro.databinding.ActivityStatisticsBinding
import com.arjunsinh.pomodoro.utils.GamificationUtils
import com.arjunsinh.pomodoro.viewmodel.StatsViewModel
import kotlinx.coroutines.launch

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val viewModel: StatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        observeStats()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top padding to the root view to clear status bar
            v.updatePadding(top = systemBars.top)
            // Apply bottom padding to the ScrollView to ensure content isn't hidden by nav bar
            binding.scrollView.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun observeStats() {
        lifecycleScope.launch {
            viewModel.stats.collect { stats ->
                updateUI(stats)
            }
        }
    }

    private fun updateUI(stats: StatsViewModel.Statistics) {
        // Level and XP
        binding.tvLevel.text = "Level ${stats.userLevel}"

        val xpAtStartOfLevel = GamificationUtils.getTotalXpForLevel(stats.userLevel)
        val xpIntoCurrentLevel = (stats.totalXP - xpAtStartOfLevel).coerceAtLeast(0)
        val xpRequiredForNextLevel = GamificationUtils.getXpRequiredForNextLevel(stats.userLevel)
        val xpProgressPercent = if (xpRequiredForNextLevel > 0) {
            ((xpIntoCurrentLevel.toFloat() / xpRequiredForNextLevel) * 100).toInt().coerceIn(0, 100)
        } else 0

        binding.progressXP.progress = xpProgressPercent
        binding.tvXP.text = "$xpIntoCurrentLevel / $xpRequiredForNextLevel XP"

        // Today stats
        binding.tvTodayTime.text = viewModel.formatTime(stats.todayFocusTime)
        binding.tvTodaySessions.text = "${stats.todaySessionCount} sessions"

        // Week stats
        binding.tvWeekTime.text = viewModel.formatTime(stats.weekFocusTime)

        // Streak
        binding.tvStreak.text = "${stats.focusStreak} 🔥"

        // Total XP
        binding.tvTotalXP.text = stats.totalXP.toString()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadStatistics()
    }
}
