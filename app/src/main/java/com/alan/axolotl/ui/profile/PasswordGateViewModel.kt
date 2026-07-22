package com.alan.axolotl.ui.profile

import androidx.lifecycle.ViewModel
import com.alan.axolotl.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PasswordGateViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun isPasswordCorrect(input: String): Boolean =
        input == settingsRepository.getTimerPassword()
}
