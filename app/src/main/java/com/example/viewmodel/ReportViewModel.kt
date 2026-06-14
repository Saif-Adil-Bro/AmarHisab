package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.ui.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.Calendar

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository

    private val _startDate = MutableStateFlow<Long>(0L)
    val startDate: StateFlow<Long> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long>(0L)
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    private val _selectedPreset = MutableStateFlow<String>("THIS_MONTH")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    private val _isGenerating = MutableStateFlow<Boolean>(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database)
        setPreset("THIS_MONTH")
    }

    fun setPreset(preset: String) {
        _selectedPreset.value = preset
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        when (preset) {
            "THIS_MONTH" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                _startDate.value = calendar.timeInMillis
                _endDate.value = now
            }
            "LAST_7_DAYS" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                _startDate.value = calendar.timeInMillis
                _endDate.value = now
            }
            "LAST_30_DAYS" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                _startDate.value = calendar.timeInMillis
                _endDate.value = now
            }
            "ALL_TIME" -> {
                _startDate.value = 0L // Epoch beginning
                _endDate.value = now
            }
            "CUSTOM" -> {
                // Do not change current dates, user will select manually
            }
        }
    }

    fun setCustomStartDate(dateMillis: Long) {
        _startDate.value = dateMillis
        _selectedPreset.value = "CUSTOM"
    }

    fun setCustomEndDate(dateMillis: Long) {
        _endDate.value = dateMillis
        _selectedPreset.value = "CUSTOM"
    }

    fun generatePdfStream(
        context: Context,
        profileId: Long,
        profileName: String,
        outputStream: OutputStream,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                // Fetch filtered expenses in date range on IO thread pool balance
                val expenses = withContext(Dispatchers.IO) {
                    repository.getExpensesInDateRange(profileId, _startDate.value, _endDate.value)
                }

                if (expenses.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onError("নির্বাচিত সময়সীমার মধ্যে কোনো খরচ খুঁজে পাওয়া যায়নি!")
                    }
                    _isGenerating.value = false
                    return@launch
                }

                // Generate PDF
                val success = withContext(Dispatchers.IO) {
                    PdfGenerator.generatePdf(
                        context = context,
                        expenses = expenses,
                        profileName = profileName,
                        startDate = _startDate.value,
                        endDate = _endDate.value,
                        outputStream = outputStream
                    )
                }

                withContext(Dispatchers.Main) {
                    if (success) {
                        onSuccess()
                    } else {
                        onError("PDF রিপোর্ট তৈরি করতে সমস্যা হয়েছে!")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("ত্রুটি: ${e.message}")
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReportViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
