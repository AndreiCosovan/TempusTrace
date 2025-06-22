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
import com.example.tempustrace.R
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInputFields()
        setupDefaultValues()
    }

    private fun setupInputFields() {
        // Setup date picker
        binding.editTextDate.setOnClickListener {
            showDatePickerDialog(binding.editTextDate)
        }

        // Setup time pickers
        binding.editTextWorkedFrom.setOnClickListener {
            showTimePickerDialog(binding.editTextWorkedFrom)
        }

        binding.editTextWorkedTo.setOnClickListener {
            showTimePickerDialog(binding.editTextWorkedTo)
        }

        // Setup break duration pickers
        binding.editTextFirstBreak.setOnClickListener {
            showNumberPickerDialog(binding.editTextFirstBreak, 18)
        }

        binding.editTextSecondBreak.setOnClickListener {
            showNumberPickerDialog(binding.editTextSecondBreak, 36)
        }

        // Setup save button
        binding.saveButton.setOnClickListener {
            saveDate()
        }
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
            value = editText.text.toString().toIntOrNull() ?: defaultValue
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Break Duration (minutes)")
            .setView(numberPicker)
            .setPositiveButton("OK") { _, _ ->
                editText.setText(numberPicker.value.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDatePickerDialog(editText: EditText) {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selection))
            editText.setText(selectedDate)
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePickerDialog(editText: EditText) {
        // Parse current value if exists, otherwise default to current time
        val hour: Int
        val minute: Int

        if (editText.text.toString().matches(Regex("\\d{2}:\\d{2}"))) {
            val parts = editText.text.toString().split(":")
            hour = parts[0].toInt()
            minute = parts[1].toInt()
        } else {
            val calendar = Calendar.getInstance()
            hour = calendar.get(Calendar.HOUR_OF_DAY)
            minute = calendar.get(Calendar.MINUTE)
        }

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
            showErrorSnackbar("Please fill in all required fields.")
            return
        }

        try {
            // Parse times
            val startTime = LocalTime.parse(workedFrom)
            val endTime = LocalTime.parse(workedTo)
            val localDate = LocalDate.parse(date)

            // Use viewLifecycleOwner.lifecycleScope to respect the fragment's lifecycle
            viewLifecycleOwner.lifecycleScope.launch {
                // Check if entry already exists for this date
                if (trackingViewModel.hasWorkDayForDate(localDate)) {
                    showErrorSnackbar("An entry for this date already exists.")
                    return@launch
                }

                // Create a WorkDay entity
                val workDay = WorkDay(
                    date = localDate,
                    startTime = startTime,
                    endTime = endTime
                )

                // Insert the WorkDay and get its ID
                val workDayId = db.workDayDao().insertWorkDay(workDay)

                // Create Break entities
                val breaks = listOf(
                    Break(
                        workDayId = workDayId,
                        startTime = startTime.plusMinutes(120),
                        durationMinutes = firstBreak
                    ),
                    Break(
                        workDayId = workDayId,
                        startTime = startTime.plusMinutes(240),
                        durationMinutes = secondBreak
                    )
                )

                // Insert breaks
                breaks.forEach { db.breakDao().insertBreak(it) }

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    // Reset data
                    setupDefaultValues()
                    // Show success message
                    showSuccessSnackbar("Work time saved successfully!")
                }
            }
        } catch (e: Exception) {
            showErrorSnackbar("Error saving data: ${e.localizedMessage}")
        }
    }

    private fun showErrorSnackbar(message: String) {
        val navView = requireActivity().findViewById<View>(R.id.nav_view)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(navView)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_light, null))
            .show()
    }

    private fun showSuccessSnackbar(message: String) {
        val navView = requireActivity().findViewById<View>(R.id.nav_view)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(navView)
            .setBackgroundTint(resources.getColor(android.R.color.holo_green_light, null))
            .show()
    }

    override fun onDestroyView() {
        preferencesJob?.cancel()
        preferencesJob = null
        super.onDestroyView()
        _binding = null
    }
}
