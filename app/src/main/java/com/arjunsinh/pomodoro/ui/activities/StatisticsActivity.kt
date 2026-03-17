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
import com.arjunsinh.pomodoro.utils.Constants
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
            // Apply top padding to the toolbar area
            binding.toolbar.updatePadding(top = systemBars.top)
            // Apply bottom padding to the nested scroll view content container
            // We apply it to the ScrollView itself if it's the bottom-most view
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
        val xpProgress = (stats.totalXP % Constants.XP_PER_LEVEL)
        val xpProgressPercent = ((xpProgress.toFloat() / Constants.XP_PER_LEVEL) * 100).toInt()
        binding.progressXP.progress = xpProgressPercent
        binding.tvXP.text = "$xpProgress / ${Constants.XP_PER_LEVEL} XP"

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
