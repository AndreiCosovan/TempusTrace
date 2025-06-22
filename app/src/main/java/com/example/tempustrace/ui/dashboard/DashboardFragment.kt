package com.example.tempustrace.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tempustrace.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat

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
    }

    private fun observeViewModel() {
        // Observe work statistics
        dashboardViewModel.workStats.observe(viewLifecycleOwner) { stats ->
            if (stats.totalTrackedDays > 0) {
                binding.cardWeekStats.visibility = View.VISIBLE
                binding.cardMonthStats.visibility = View.VISIBLE
                binding.cardOverallStats.visibility = View.VISIBLE
                binding.textDashboard.visibility = View.GONE

                val decimalFormat = DecimalFormat("#0.0")

                // Update week stats
                binding.textDaysWorkedWeek.text = stats.daysWorkedThisWeek.toString()
                binding.textTotalHoursWeek.text = decimalFormat.format(stats.totalWeekHours)

                // Update month stats
                binding.textDaysWorkedMonth.text = stats.daysWorkedThisMonth.toString()
                binding.textTotalHoursMonth.text = decimalFormat.format(stats.totalMonthHours)

                // Update overall stats
                binding.textTotalDays.text = stats.totalTrackedDays.toString()
                binding.textAvgHours.text = decimalFormat.format(stats.averageDailyHours)

                // Format and update time balance with + sign for positive values
                val timeBalanceText = if (stats.timeBalance >= 0)
                    "+" + decimalFormat.format(stats.timeBalance)
                else
                    decimalFormat.format(stats.timeBalance)

                binding.textTimeBalance.text = timeBalanceText

                // Set color based on balance (green for positive, red for negative)
                val colorRes = if (stats.timeBalance >= 0)
                    android.R.color.holo_green_dark
                else
                    android.R.color.holo_red_dark

                binding.textTimeBalance.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            } else {
                binding.cardWeekStats.visibility = View.GONE
                binding.cardMonthStats.visibility = View.GONE
                binding.cardOverallStats.visibility = View.GONE
                binding.textDashboard.visibility = View.VISIBLE
            }
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

        // Observe loading state
        dashboardViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}