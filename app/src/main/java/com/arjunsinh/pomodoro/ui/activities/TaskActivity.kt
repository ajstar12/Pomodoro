package com.arjunsinh.pomodoro.ui.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arjunsinh.pomodoro.R
import com.arjunsinh.pomodoro.data.entity.Task
import com.arjunsinh.pomodoro.databinding.ActivityTaskBinding
import com.arjunsinh.pomodoro.databinding.DialogAddTaskBinding
import com.arjunsinh.pomodoro.ui.adapters.TaskAdapter
import com.arjunsinh.pomodoro.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class TaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter

    private var showingCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupFAB()
        observeTasks()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply top padding to the root view to clear status bar
            v.updatePadding(top = systemBars.top)
            
            // Apply bottom padding to RecyclerView so content isn't hidden by nav bar
            binding.rvTasks.updatePadding(bottom = systemBars.bottom + 100) // extra padding for FAB space
            
            // Adjust FAB margin so it sits above the navigation bar
            // Since root has no bottom padding, we use systemBars.bottom for margin
            binding.fabAddTask.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + 16.toPx()
            }
            
            insets
        }
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                // Start timer with this task
                finish()
            },
            onTaskLongClick = { task ->
                showTaskOptionsDialog(task)
            }
        )

        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@TaskActivity)
            adapter = taskAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                showingCompleted = tab?.position == 1
                observeTasks()
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupFAB() {
        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun observeTasks() {
        lifecycleScope.launch {
            if (showingCompleted) {
                viewModel.allCompletedTasks.collect { tasks ->
                    taskAdapter.submitList(tasks)
                    updateEmptyState(tasks.isEmpty())
                }
            } else {
                viewModel.allActiveTasks.collect { tasks ->
                    taskAdapter.submitList(tasks)
                    updateEmptyState(tasks.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvTasks.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showAddTaskDialog(taskToEdit: Task? = null) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)

        if (taskToEdit != null) {
            dialogBinding.etTaskTitle.setText(taskToEdit.title)
            dialogBinding.etTaskDescription.setText(taskToEdit.description)
            dialogBinding.etTargetPomodoros.setText(taskToEdit.targetPomodoros.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (taskToEdit == null) "Add Task" else "Edit Task")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val title = dialogBinding.etTaskTitle.text.toString()
                val description = dialogBinding.etTaskDescription.text.toString()
                val targetPomodoros = dialogBinding.etTargetPomodoros.text.toString().toIntOrNull() ?: 1

                if (title.isNotBlank()) {
                    if (taskToEdit == null) {
                        val task = Task(
                            title = title,
                            description = description,
                            targetPomodoros = targetPomodoros
                        )
                        viewModel.insertTask(task)
                    } else {
                        val updatedTask = taskToEdit.copy(
                            title = title,
                            description = description,
                            targetPomodoros = targetPomodoros,
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.updateTask(updatedTask)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTaskOptionsDialog(task: Task) {
        val options = arrayOf("Edit", "Delete", if (task.isCompleted) "Mark Active" else "Mark Complete")

        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddTaskDialog(task)
                    1 -> viewModel.deleteTask(task)
                    2 -> viewModel.toggleTaskCompletion(task)
                }
            }
            .show()
    }
}
