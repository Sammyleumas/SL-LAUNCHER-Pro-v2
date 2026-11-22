package com.sllauncher.pro.setup

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sllauncher.pro.launcher.DefaultHomeChecker
import com.sllauncher.pro.launcher.HomeRoleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LauncherSetupUiState(
    val isDefaultHome: Boolean,
    /**
     * True when the system dialog cannot be used or did not result in SL being
     * selected. Some devices suppress the dialog after the user declines it,
     * so we then point the user to the Settings screen instead.
     */
    val showSettingsFallback: Boolean,
)

@HiltViewModel
class LauncherSetupViewModel @Inject constructor(
    private val homeRoleRepository: HomeRoleRepository,
    private val checker: DefaultHomeChecker,
) : ViewModel() {

    private val roleRequestAttempted = MutableStateFlow(false)
    private val roleDialogAvailable = checker.createRoleRequestIntent() != null

    val uiState: StateFlow<LauncherSetupUiState> = combine(
        homeRoleRepository.isDefaultHome,
        roleRequestAttempted,
    ) { isDefault, attempted ->
        LauncherSetupUiState(
            isDefaultHome = isDefault,
            showSettingsFallback = !isDefault && (attempted || !roleDialogAvailable),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = LauncherSetupUiState(
            isDefaultHome = homeRoleRepository.isDefaultHome.value,
            showSettingsFallback = !homeRoleRepository.isDefaultHome.value && !roleDialogAvailable,
        ),
    )

    fun roleRequestIntent(): Intent? = checker.createRoleRequestIntent()

    fun homeSettingsIntent(): Intent = checker.createHomeSettingsIntent()

    /** Called when the system role dialog closes, whatever the user chose. */
    fun onRoleRequestFinished() {
        roleRequestAttempted.value = true
        homeRoleRepository.refresh()
    }

    fun refresh() = homeRoleRepository.refresh()
}
