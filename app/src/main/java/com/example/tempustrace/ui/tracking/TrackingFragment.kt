package com.example.tempustrace.ui.tracking

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tempustrace.databinding.FragmentTrackingBinding
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class TrackingFragment : Fragment() {

    private var _binding: FragmentTrackingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val trackingViewModel =
            ViewModelProvider(this)[TrackingViewModel::class.java]

        _binding = FragmentTrackingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textTracking
        val editTextDate: EditText = binding.editTextDate
        val editTextFirstBreak: EditText = binding.editTextFirstBreak
        val editTextSecondBreak: EditText = binding.editTextSecondBreak

        trackingViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        editTextDate.setOnClickListener {
            showDatePickerDialog(editTextDate)
        }

        editTextFirstBreak.setOnClickListener {
            showNumberPickerDialog(editTextFirstBreak, 18)
        }

        editTextSecondBreak.setOnClickListener {
            showNumberPickerDialog(editTextSecondBreak, 36)
        }

        // Set default values for the break fields
        editTextFirstBreak.setText("18")
        editTextSecondBreak.setText("36")

        // Set today's date as default value for the date field
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        editTextDate.setText(todayDate)

        return root
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}