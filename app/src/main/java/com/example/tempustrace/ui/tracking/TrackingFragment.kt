package com.example.tempustrace.ui.tracking

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tempustrace.data.AppDatabase
import com.example.tempustrace.data.Break
import com.example.tempustrace.data.UserPreferencesRepository
import com.example.tempustrace.data.WorkDay
import com.example.tempustrace.databinding.FragmentTrackingBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.text.format
import kotlin.toString

@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!
    private var preferencesJob: Job? = null
    
    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    
    private val trackingViewModel: TrackingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textTracking
        val editTextDate: EditText = binding.editTextDate
        val editTextFirstBreak: EditText = binding.editTextFirstBreak
        val editTextSecondBreak: EditText = binding.editTextSecondBreak
        val editTextWorkedFrom: EditText = binding.editTextWorkedFrom
        val editTextWorkedTo: EditText = binding.editTextWorkedTo
        val saveButton: MaterialButton = binding.saveButton

        editTextDate.setOnClickListener {
            showDatePickerDialog(editTextDate)
        }

        editTextFirstBreak.setOnClickListener {
            showNumberPickerDialog(editTextFirstBreak, 18)
        }

        editTextSecondBreak.setOnClickListener {
            showNumberPickerDialog(editTextSecondBreak, 36)
        }

        editTextWorkedFrom.setOnClickListener {
            showTimePickerDialog(editTextWorkedFrom)
        }

        editTextWorkedTo.setOnClickListener {
            showTimePickerDialog(editTextWorkedTo)
        }

        // Set today's date as default value for the date field
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        editTextDate.setText(todayDate)

        saveButton.setOnClickListener {
            saveDate()
        }

        // Set default values
        setupDefaultValues()

        return root
    }

    private fun setupDefaultValues() {
        // Set today's date as default value for the date field
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.editTextDate.setText(todayDate)

        // Set default values from preferences using lifecycleScope
        preferencesJob?.cancel()
        preferencesJob = viewLifecycleOwner.lifecycleScope.launch {
            // Only collect when the view is at least STARTED
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                    // The binding should always be valid here because we're respecting the lifecycle
                    binding.editTextWorkedFrom.setText(preferences.defaultWorkStartTime)
                    binding.editTextWorkedTo.setText(preferences.defaultWorkEndTime)
                    binding.editTextFirstBreak.setText(preferences.defaultFirstBreakDuration.toString())
                    binding.editTextSecondBreak.setText(preferences.defaultSecondBreakDuration.toString())
                }
            }
        }
    }

    private fun showNumberPickerDialog(editText: EditText, defaultValue: Int) {
        val numberPicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 60
            value = defaultValue
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Select value")
            .setView(numberPicker)
            .setPositiveButton("OK") { _, _ ->
                editText.setText(numberPicker.value.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePickerDialog(editText: EditText) {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()


        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selection))
            editText.setText(selectedDate)
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                editText.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute))
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun saveDate() {
        // Save the data to the database using Room
        val date = binding.editTextDate.text.toString()
        val workedFrom = binding.editTextWorkedFrom.text.toString()
        val workedTo = binding.editTextWorkedTo.text.toString()
        val firstBreak = binding.editTextFirstBreak.text.toString().toIntOrNull() ?: 18
        val secondBreak = binding.editTextSecondBreak.text.toString().toIntOrNull() ?: 36

        // Validate required fields
        if (date.isEmpty() || workedFrom.isEmpty() || workedTo.isEmpty()) {
            showErrorDialog("Please fill in all required fields.")
            return
        }

        // Parse times
        val startTime = LocalTime.parse(workedFrom)
        val endTime = LocalTime.parse(workedTo)

        // Create a WorkDay entity
        val workDay = WorkDay(
            date = LocalDate.parse(date),
            startTime = startTime,
            endTime = endTime
        )

        // Use a coroutine to perform database operation
        CoroutineScope(Dispatchers.IO).launch {
            // Insert the WorkDay and get its ID
            val workDayId = db.workDayDao().insertWorkDay(workDay)

            // Create Break entities
            val breaks = listOf(
                Break(
                    workDayId = workDayId,
                    startTime = startTime.plusMinutes(120), // Example: First break starts 2 hours after work starts
                    durationMinutes = firstBreak
                ),
                Break(
                    workDayId = workDayId,
                    startTime = startTime.plusMinutes(240), // Example: Second break starts 4 hours after work starts
                    durationMinutes = secondBreak
                )
            )

            // Insert breaks
            breaks.forEach { db.breakDao().insertBreak(it) }

            // Switch back to main thread for UI updates
            CoroutineScope(Dispatchers.Main).launch {
                // Reset data
                setupDefaultValues()

                // Show success dialog
                showSuccessDialog()
            }
        }
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun showSuccessDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Success")
        builder.setMessage("Work time saved successfully!")
        builder.setPositiveButton("OK") { dialog, which -> dialog.dismiss() }
        builder.show()
    }

    override fun onDestroyView() {
        preferencesJob?.cancel()
        preferencesJob = null
        super.onDestroyView()
        _binding = null
    }
}
