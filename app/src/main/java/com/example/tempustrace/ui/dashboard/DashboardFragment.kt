package com.example.tempustrace.ui.dashboard

import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tempustrace.R
import com.example.tempustrace.data.WorkTimeCalculator
import com.example.tempustrace.databinding.FragmentDashboardBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private lateinit var recentWorkdaysAdapter: RecentWorkdaysAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        recentWorkdaysAdapter = RecentWorkdaysAdapter()
        binding.recyclerRecentDays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentWorkdaysAdapter
        }

        // Add swipe-to-delete functionality with visual feedback
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, // No drag and drop
            ItemTouchHelper.LEFT // Only enable left swipe
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // Not handling move events
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val entry = recentWorkdaysAdapter.getItemAtPosition(position)
                dashboardViewModel.deleteWorkDay(entry.workDay.id)
                val navView = requireActivity().findViewById<View>(R.id.nav_view)
                Snackbar.make(binding.root, "Workday deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { dashboardViewModel.restoreWorkDay(entry) }
                    .setAnchorView(navView)
                    .show()
            }

            // Add visual feedback during swipe
            override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = Color.RED.toDrawable()
                val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)

                // Calculate positioning
                val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                val iconBottom = iconTop + deleteIcon.intrinsicHeight

                // Set background
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(canvas)

                // Set icon (appears from the right when swiping left)
                if (dX < 0) {
                    val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.draw(canvas)
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        // Attach to RecyclerView
        itemTouchHelper.attachToRecyclerView(binding.recyclerRecentDays)
    }

    private fun observeViewModel() {
        dashboardViewModel.workStats.observe(viewLifecycleOwner) {
            renderStats()
        }

        // Observe recent workdays
        dashboardViewModel.recentWorkDays.observe(viewLifecycleOwner) { workdays ->
            if (workdays.isEmpty()) {
                binding.recyclerRecentDays.visibility = View.GONE
                binding.textRecentDaysTitle.visibility = View.GONE
            } else {
                binding.recyclerRecentDays.visibility = View.VISIBLE
                binding.textRecentDaysTitle.visibility = View.VISIBLE
                recentWorkdaysAdapter.setData(workdays)
            }
        }

        dashboardViewModel.loading.observe(viewLifecycleOwner) {
            renderStats()
        }
    }

    private fun renderStats() {
        val isLoading = dashboardViewModel.loading.value == true
        val stats = dashboardViewModel.workStats.value
        val hasData = stats != null && stats.totalTrackedDays > 0

        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.cardWeekStats.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.cardMonthStats.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.cardOverallStats.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.textDashboard.visibility = if (!isLoading && !hasData) View.VISIBLE else View.GONE

        if (stats == null || !hasData) return

        binding.textDaysWorkedWeek.text = stats.daysWorkedThisWeek.toString()
        binding.textTotalHoursWeek.text = WorkTimeCalculator.formatHoursAndMinutes(stats.totalWeekMinutes)
        binding.textDaysWorkedMonth.text = stats.daysWorkedThisMonth.toString()
        binding.textTotalHoursMonth.text = WorkTimeCalculator.formatHoursAndMinutes(stats.totalMonthMinutes)
        binding.textTotalDays.text = stats.totalTrackedDays.toString()
        binding.textAvgHours.text = WorkTimeCalculator.formatHoursAndMinutes(stats.averageDailyMinutes)
        binding.textTimeBalance.text = WorkTimeCalculator.formatSignedHoursAndMinutes(stats.timeBalanceMinutes)

        val colorRes = if (stats.timeBalanceMinutes >= 0) {
            android.R.color.holo_green_dark
        } else {
            android.R.color.holo_red_dark
        }
        binding.textTimeBalance.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}