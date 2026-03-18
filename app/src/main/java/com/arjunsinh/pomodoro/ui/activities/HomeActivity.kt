package com.arjunsinh.pomodoro.ui.activities

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.arjunsinh.pomodoro.R
import com.arjunsinh.pomodoro.databinding.ActivityHomeBinding
import com.arjunsinh.pomodoro.service.PomodoroForegroundService
import com.arjunsinh.pomodoro.utils.Constants
import com.arjunsinh.pomodoro.viewmodel.TimerViewModel
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: TimerViewModel by viewModels()

    private var timerService: PomodoroForegroundService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PomodoroForegroundService.LocalBinder
            timerService = binder.getService()
            isServiceBound = true

            lifecycleScope.launch {
                timerService?.timerState?.collect { state ->
                    updateUI(state)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            isServiceBound = false
        }
    }

    private val timerUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.BROADCAST_TIMER_UPDATE -> {
                    val remainingTime = intent.getLongExtra(Constants.EXTRA_REMAINING_TIME, 0L)
                    val sessionType = intent.getSerializableExtra(Constants.EXTRA_SESSION_TYPE) as? Constants.SessionType
                    val isRunning = intent.getBooleanExtra(Constants.EXTRA_IS_RUNNING, false)
                    val sessionCount = intent.getIntExtra(Constants.EXTRA_CURRENT_SESSION_COUNT, 0)

                    if (sessionType != null) {
                        val progress = if (timerService != null) {
                            timerService!!.getTimerManager().getProgress()
                        } else 0f

                        viewModel.updateTimerState(remainingTime, sessionType, isRunning, sessionCount, progress)
                    }
                }
                Constants.BROADCAST_SESSION_COMPLETE -> {
                    // Handle session completion if needed
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        setupUI()
        observeTimerState()
        registerReceivers()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Only apply TOP padding to the root. 
            // BottomNavigationView handles its own bottom padding automatically.
            v.updatePadding(top = systemBars.top)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        bindToService()
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(timerUpdateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
        binding.fabPlayPause.setOnClickListener {
            handlePlayPauseClick()
        }

        binding.fabReset.setOnClickListener {
            handleResetClick()
        }

        binding.fabSkip.setOnClickListener {
            handleSkipClick()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_timer -> true
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TaskActivity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatisticsActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.cvSelectedTask.setOnClickListener {
            startActivity(Intent(this, TaskActivity::class.java))
        }
    }

    private fun observeTimerState() {
        lifecycleScope.launch {
            viewModel.timerState.collect { state ->
                updateTimerDisplay(state)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedTask.collect { taskId ->
                if (taskId != null) {
                    binding.cvSelectedTask.visibility = android.view.View.VISIBLE
                    timerService?.setCurrentTask(taskId)
                } else {
                    binding.cvSelectedTask.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun updateTimerDisplay(state: TimerViewModel.TimerState) {
        binding.tvTimer.text = viewModel.getFormattedTime(state.remainingTime)

        val sessionTypeName = when (state.sessionType) {
            Constants.SessionType.FOCUS -> "Focus Session"
            Constants.SessionType.SHORT_BREAK -> "Short Break"
        }
        binding.tvSessionType.text = sessionTypeName
        
        val completed = state.sessionCount
        val remaining = (12 - completed).coerceAtLeast(0)
        binding.tvSessionCount.text = "Completed: $completed | Remaining: $remaining"

        val progress = (state.progress * 100).toInt()
        binding.circularProgressBar.setProgressWithAnimation(progress.toFloat(), 1000)

        val iconRes = if (state.isRunning) R.drawable.ic_pause else R.drawable.ic_play
        binding.fabPlayPause.setImageResource(iconRes)
    }

    private fun updateUI(state: PomodoroForegroundService.TimerStateData) {
        viewModel.updateTimerState(
            state.remainingTime,
            state.sessionType,
            state.timerState == Constants.TimerState.RUNNING,
            state.sessionCount,
            state.progress
        )
    }

    private fun handlePlayPauseClick() {
        val state = viewModel.timerState.value

        if (state.isRunning) {
            // Pause
            val intent = Intent(this, PomodoroForegroundService::class.java).apply {
                action = Constants.ACTION_PAUSE
            }
            startService(intent)
        } else if (state.remainingTime > 0) {
            // Resume
            val intent = Intent(this, PomodoroForegroundService::class.java).apply {
                action = Constants.ACTION_RESUME
            }
            startService(intent)
        } else {
            // Start new session
            val intent = Intent(this, PomodoroForegroundService::class.java).apply {
                action = Constants.ACTION_START
                putExtra(Constants.EXTRA_SESSION_TYPE, Constants.SessionType.FOCUS)
                val taskId = viewModel.selectedTask.value
                if (taskId != null) {
                    putExtra("task_id", taskId)
                }
            }
            startService(intent)
        }
    }

    private fun handleResetClick() {
        val intent = Intent(this, PomodoroForegroundService::class.java).apply {
            action = Constants.ACTION_STOP
        }
        startService(intent)

        viewModel.updateTimerState(
            0L,
            Constants.SessionType.FOCUS,
            false,
            0,
            0f
        )
    }

    private fun handleSkipClick() {
        val intent = Intent(this, PomodoroForegroundService::class.java).apply {
            action = Constants.ACTION_SKIP
        }
        startService(intent)
    }

    private fun bindToService() {
        val intent = Intent(this, PomodoroForegroundService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Constants.BROADCAST_TIMER_UPDATE)
            addAction(Constants.BROADCAST_SESSION_COMPLETE)
        }
        registerReceiver(timerUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
}
