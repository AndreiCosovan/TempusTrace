package com.example.tempustrace.ui.tracking

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tempustrace.R
import com.example.tempustrace.data.UserPreferences
import com.example.tempustrace.data.UserPreferencesRepository
import com.example.tempustrace.data.WorkTimeCalculator
import com.example.tempustrace.databinding.FragmentTrackingBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val trackingViewModel: TrackingViewModel by viewModels()
    private var appliedDefaults: UserPreferences? = null

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
        if (binding.editTextDate.text.isNullOrBlank()) {
            binding.editTextDate.setText(LocalDate.now().toString())
        }
        observePreferences()
        observeSaveResult()
    }

    private fun setupInputFields() {
        binding.editTextDate.setOnClickListener {
            showDatePickerDialog(binding.editTextDate)
        }
        binding.editTextWorkedFrom.setOnClickListener {
            showTimePickerDialog(binding.editTextWorkedFrom)
        }
        binding.editTextWorkedTo.setOnClickListener {
            showTimePickerDialog(binding.editTextWorkedTo)
        }
        binding.editTextFirstBreak.setOnClickListener {
            showNumberPickerDialog(binding.editTextFirstBreak, 18)
        }
        binding.editTextSecondBreak.setOnClickListener {
            showNumberPickerDialog(binding.editTextSecondBreak, 36)
        }
        binding.saveButton.setOnClickListener {
            saveDate()
        }
    }

    private fun observePreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                    if (_binding == null) return@collect
                    applyDefaults(preferences, force = false)
                }
            }
        }
    }

    private fun applyDefaults(preferences: UserPreferences, force: Boolean) {
        setIfUnedited(
            binding.editTextWorkedFrom,
            appliedDefaults?.defaultWorkStartTime,
            preferences.defaultWorkStartTime,
            force
        )
        setIfUnedited(
            binding.editTextWorkedTo,
            appliedDefaults?.defaultWorkEndTime,
            preferences.defaultWorkEndTime,
            force
        )
        setIfUnedited(
            binding.editTextFirstBreak,
            appliedDefaults?.defaultFirstBreakDuration?.toString(),
            preferences.defaultFirstBreakDuration.toString(),
            force
        )
        setIfUnedited(
            binding.editTextSecondBreak,
            appliedDefaults?.defaultSecondBreakDuration?.toString(),
            preferences.defaultSecondBreakDuration.toString(),
            force
        )
        appliedDefaults = preferences
    }

    private fun setIfUnedited(field: EditText, previous: String?, updated: String, force: Boolean) {
        val current = field.text?.toString().orEmpty()
        if (force || current.isBlank() || current == previous) {
            field.setText(updated)
        }
    }

    private fun observeSaveResult() {
        trackingViewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                TrackingViewModel.SaveResult.Success -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val preferences = userPreferencesRepository.userPreferencesFlow.first()
                        if (_binding == null) return@launch
                        binding.editTextDate.setText(LocalDate.now().toString())
                        applyDefaults(preferences, force = true)
                    }
                    binding.saveButton.isEnabled = true
                    showSuccessSnackbar("Work time saved successfully!")
                    trackingViewModel.resetSaveResult()
                }
                is TrackingViewModel.SaveResult.Error -> {
                    binding.saveButton.isEnabled = true
                    showErrorSnackbar(result.message)
                    trackingViewModel.resetSaveResult()
                }
                null -> Unit
            }
        }
    }

    private fun showNumberPickerDialog(editText: EditText, defaultValue: Int) {
        val numberPicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = WorkTimeCalculator.MAX_BREAK_MINUTES
            value = (editText.text.toString().toIntOrNull() ?: defaultValue)
                .coerceIn(0, WorkTimeCalculator.MAX_BREAK_MINUTES)
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
        val current = runCatching { LocalDate.parse(editText.text.toString()) }
            .getOrDefault(LocalDate.now())
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(WorkTimeCalculator.pickerSelectionMillis(current))
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            editText.setText(WorkTimeCalculator.localDateFromPicker(selection).toString())
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePickerDialog(editText: EditText) {
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

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                editText.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute))
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun saveDate() {
        val dateText = binding.editTextDate.text.toString()
        val workedFrom = binding.editTextWorkedFrom.text.toString()
        val workedTo = binding.editTextWorkedTo.text.toString()
        val firstBreak = binding.editTextFirstBreak.text.toString().toIntOrNull()
        val secondBreak = binding.editTextSecondBreak.text.toString().toIntOrNull()

        if (dateText.isBlank() || workedFrom.isBlank() || workedTo.isBlank() ||
            firstBreak == null || secondBreak == null
        ) {
            showErrorSnackbar("Please fill in all required fields.")
            return
        }

        val localDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
        val startTime = runCatching { LocalTime.parse(workedFrom) }.getOrNull()
        val endTime = runCatching { LocalTime.parse(workedTo) }.getOrNull()
        if (localDate == null || startTime == null || endTime == null) {
            showErrorSnackbar("Could not read the date or times.")
            return
        }

        binding.saveButton.isEnabled = false
        trackingViewModel.saveWorkDay(localDate, startTime, endTime, firstBreak, secondBreak)
    }

    private fun showErrorSnackbar(message: String) {
        val navView = requireActivity().findViewById<View>(R.id.nav_view)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(navView)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            .show()
    }

    private fun showSuccessSnackbar(message: String) {
        val navView = requireActivity().findViewById<View>(R.id.nav_view)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(navView)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
