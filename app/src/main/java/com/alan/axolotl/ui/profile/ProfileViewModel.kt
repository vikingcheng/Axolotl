package com.alan.axolotl.ui.profile

import androidx.lifecycle.ViewModel
import com.alan.axolotl.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ProfileUiState(
    val currentPassword: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(currentPassword = settingsRepository.getTimerPassword())
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** Saves a new 4-digit password. Returns true if the input was valid and saved. */
    fun savePassword(newPassword: String): Boolean {
        if (newPassword.length != 4) return false
        settingsRepository.setTimerPassword(newPassword)
        _uiState.update { it.copy(currentPassword = newPassword) }
        return true
    }
}
