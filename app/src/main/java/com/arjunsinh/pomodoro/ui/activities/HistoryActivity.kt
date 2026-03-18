package com.arjunsinh.pomodoro.ui.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arjunsinh.pomodoro.databinding.ActivityHistoryBinding
import com.arjunsinh.pomodoro.ui.adapter.SessionHistoryAdapter
import com.arjunsinh.pomodoro.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: SessionHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerView()
        observeSessions()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top padding to the root view to clear status bar
            v.updatePadding(top = systemBars.top)
            // Apply bottom padding to RecyclerView so content isn't hidden by nav bar
            binding.rvHistory.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SessionHistoryAdapter()
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun observeSessions() {
        lifecycleScope.launch {
            viewModel.historyItems.collect { items ->
                // Check if we only have the header (length 1) and no sessions
                if (items.size <= 1) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    // We still want to show the header even if list is empty? 
                    // Usually yes, so we show 0 focus sessions.
                    // If you want to hide the list completely when empty:
                    // binding.rvHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                }
                
                // Always submit to show the Stats header
                binding.rvHistory.visibility = View.VISIBLE
                adapter.submitList(items)
            }
        }
    }
}
