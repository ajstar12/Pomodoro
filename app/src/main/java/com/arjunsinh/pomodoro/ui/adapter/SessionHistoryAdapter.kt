package com.arjunsinh.pomodoro.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arjunsinh.pomodoro.R
import com.arjunsinh.pomodoro.databinding.ItemDateHeaderBinding
import com.arjunsinh.pomodoro.databinding.ItemHistoryStatisticsHeaderBinding
import com.arjunsinh.pomodoro.databinding.ItemSessionHistoryBinding
import com.arjunsinh.pomodoro.ui.model.HistoryItem
import com.arjunsinh.pomodoro.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryAdapter : ListAdapter<HistoryItem, RecyclerView.ViewHolder>(HistoryItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_STATS = 0
        private const val VIEW_TYPE_DATE_HEADER = 1
        private const val VIEW_TYPE_SESSION = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryItem.StatisticsHeader -> VIEW_TYPE_STATS
            is HistoryItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is HistoryItem.SessionItem -> VIEW_TYPE_SESSION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_STATS -> {
                val binding = ItemHistoryStatisticsHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                StatsViewHolder(binding)
            }
            VIEW_TYPE_DATE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                DateHeaderViewHolder(binding)
            }
            VIEW_TYPE_SESSION -> {
                val binding = ItemSessionHistoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SessionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryItem.StatisticsHeader -> (holder as StatsViewHolder).bind(item)
            is HistoryItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is HistoryItem.SessionItem -> (holder as SessionViewHolder).bind(item.session)
        }
    }

    class StatsViewHolder(private val binding: ItemHistoryStatisticsHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(stats: HistoryItem.StatisticsHeader) {
            binding.tvTotalSessionsCount.text = stats.totalSessions.toString()
        }
    }

    class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: HistoryItem.DateHeader) {
            binding.tvDateHeader.text = header.date
        }
    }

    class SessionViewHolder(private val binding: ItemSessionHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: com.arjunsinh.pomodoro.data.entity.Session) {
            val (typeName, typeColor) = when (session.sessionType) {
                Constants.SessionType.FOCUS -> "Focus Session" to R.color.primary
                Constants.SessionType.SHORT_BREAK -> "Short Break" to R.color.secondary
            }

            binding.tvSessionType.text = typeName
            binding.vSessionTypeIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, typeColor)
            )
            binding.tvDuration.setTextColor(
                ContextCompat.getColor(binding.root.context, typeColor)
            )

            val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
            binding.tvStartTime.text = "Started: ${timeFormat.format(Date(session.startedAt))}"
            binding.tvEndTime.text = "Ended: ${timeFormat.format(Date(session.completedAt))}"

            val totalSeconds = (session.duration / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            binding.tvDuration.text = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

            if (session.xpEarned > 0 && session.wasCompleted) {
                binding.tvXP.visibility = View.VISIBLE
                binding.tvXP.text = "+${session.xpEarned} XP"
            } else {
                binding.tvXP.visibility = View.GONE
            }

            if (!session.wasCompleted) {
                binding.tvSkipped.visibility = View.VISIBLE
            } else {
                binding.tvSkipped.visibility = View.GONE
            }

            binding.tvTaskName.visibility = View.GONE
        }
    }

    class HistoryItemDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return when {
                oldItem is HistoryItem.StatisticsHeader && newItem is HistoryItem.StatisticsHeader -> true
                oldItem is HistoryItem.DateHeader && newItem is HistoryItem.DateHeader ->
                    oldItem.timestamp == newItem.timestamp
                oldItem is HistoryItem.SessionItem && newItem is HistoryItem.SessionItem ->
                    oldItem.session.id == newItem.session.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
