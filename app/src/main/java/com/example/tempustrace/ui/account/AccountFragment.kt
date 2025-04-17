package com.example.tempustrace.ui.account

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.tempustrace.databinding.FragmentAccountBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Time pickers
        binding.editWorkStart.setOnClickListener {
            showTimePickerDialog(binding.editWorkStart.text.toString()) { time ->
                binding.editWorkStart.setText(time)
            }
        }

        binding.editWorkEnd.setOnClickListener {
            showTimePickerDialog(binding.editWorkEnd.text.toString()) { time ->
                binding.editWorkEnd.setText(time)
            }
        }

        // Number pickers for break durations
        binding.editFirstBreak.setOnClickListener {
            showNumberPickerDialog(
                "First Break Duration",
                binding.editFirstBreak.text.toString().toIntOrNull() ?: 18
            ) { value ->
                binding.editFirstBreak.setText(value.toString())
            }
        }

        binding.editSecondBreak.setOnClickListener {
            showNumberPickerDialog(
                "Second Break Duration",
                binding.editSecondBreak.text.toString().toIntOrNull() ?: 36
            ) { value ->
                binding.editSecondBreak.setText(value.toString())
            }
        }

        // Save button
        binding.buttonSaveSettings.setOnClickListener {
            saveSettings()
        }
    }

    // In AccountFragment.kt, update the observeViewModel() function:

    private fun observeViewModel() {
        viewModel.preferences.observe(viewLifecycleOwner) { preferences ->
            binding.editWorkStart.setText(preferences.defaultWorkStartTime)
            binding.editWorkEnd.setText(preferences.defaultWorkEndTime)
            binding.editFirstBreak.setText(preferences.defaultFirstBreakDuration.toString())
            binding.editSecondBreak.setText(preferences.defaultSecondBreakDuration.toString())
        }

        viewModel.saveStatus.observe(viewLifecycleOwner) { result ->
            result?.let {
                Snackbar.make(
                    binding.root,
                    result.message,
                    if (result.success) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
                ).show()
                viewModel.resetSaveStatus()
            }
        }
    }

    private fun showTimePickerDialog(
        currentTime: String,
        onTimeSelected: (String) -> Unit
    ) {
        val hour: Int
        val minute: Int

        if (currentTime.matches(Regex("\\d{2}:\\d{2}"))) {
            val parts = currentTime.split(":")
            hour = parts[0].toInt()
            minute = parts[1].toInt()
        } else {
            val calendar = Calendar.getInstance()
            hour = calendar.get(Calendar.HOUR_OF_DAY)
            minute = calendar.get(Calendar.MINUTE)
        }

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
                )
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun showNumberPickerDialog(
        title: String,
        currentValue: Int,
        onValueSelected: (Int) -> Unit
    ) {
        val numberPicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 120
            value = currentValue
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(numberPicker)
            .setPositiveButton("OK") { _, _ ->
                onValueSelected(numberPicker.value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSettings() {
        val startTime = binding.editWorkStart.text.toString()
        val endTime = binding.editWorkEnd.text.toString()
        val firstBreakDuration = binding.editFirstBreak.text.toString().toIntOrNull() ?: 18
        val secondBreakDuration = binding.editSecondBreak.text.toString().toIntOrNull() ?: 36

        // Input validation
        if (startTime.isEmpty() || endTime.isEmpty()) {
            Snackbar.make(
                binding.root,
                "Please enter valid times",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.savePreferences(
            startTime,
            endTime,
            firstBreakDuration,
            secondBreakDuration
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}