package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BackupManager
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * BackupUiState manages state status feedback during backup exports or restoration imports.
 */
sealed interface BackupUiState {
    object Idle : BackupUiState
    object Loading : BackupUiState
    data class Success(val message: String) : BackupUiState
    data class Error(val errorMsg: String) : BackupUiState
}

/**
 * BackupViewModel wraps BackupManager operations using MVVM framework.
 */
class BackupViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    private val backupManager = BackupManager(repository)

    private val _exportState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val exportState: StateFlow<BackupUiState> = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val importState: StateFlow<BackupUiState> = _importState.asStateFlow()

    /**
     * Executes manual backup serialization and stores under standard Android storage system.
     */
    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _exportState.value = BackupUiState.Loading
            val success = backupManager.exportDataToUri(context, uri)
            _exportState.value = if (success) {
                BackupUiState.Success("ডাটা সফলভাবে এক্সপোর্ট হয়েছে।")
            } else {
                BackupUiState.Error("ডাটা ইমপোর্ট/এক্সপোর্ট ব্যর্থ হয়েছে।")
            }
        }
    }

    /**
     * Restores application databases utilizing transaction-safe routines from the parsed file.
     */
    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importState.value = BackupUiState.Loading
            val result = backupManager.importDataFromUri(context, uri)
            _importState.value = result.fold(
                onSuccess = {
                    BackupUiState.Success("ডাটা সফলভাবে ইমপোর্ট হয়েছে।")
                },
                onFailure = { error ->
                    BackupUiState.Error("ডাটা ইমপোর্ট/এক্সপোর্ট ব্যর্থ হয়েছে।")
                }
            )
        }
    }

    /**
     * Resets states after showing Toast / Dialog feedback message to avoid loop runs.
     */
    fun resetStates() {
        _exportState.value = BackupUiState.Idle
        _importState.value = BackupUiState.Idle
    }

    /**
     * ViewModel Factory definition corresponding to standard constructor dependency injection.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
                val database = AppDatabase.getDatabase(application)
                val repository = ExpenseRepository(database)
                return BackupViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
