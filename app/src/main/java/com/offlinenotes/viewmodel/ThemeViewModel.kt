package com.offlinenotes.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinenotes.data.SettingsRepository
import com.offlinenotes.ui.theme.ThemeMode
import com.offlinenotes.ui.theme.ThemePalette
import com.offlinenotes.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(ThemeSettings())
    val uiState: StateFlow<ThemeSettings> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.themeSettingsFlow.collectLatest { settings ->
                _uiState.update { settings }
            }
        }
    }

    fun setThemePalette(palette: ThemePalette) {
        viewModelScope.launch {
            settingsRepository.saveThemePalette(palette)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.saveThemeMode(mode)
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return ThemeViewModel(application) as T
                }
            }
        }
    }
}
