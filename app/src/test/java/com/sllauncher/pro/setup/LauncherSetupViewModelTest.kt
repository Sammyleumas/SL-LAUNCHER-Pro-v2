package com.sllauncher.pro.setup

import com.sllauncher.pro.MainDispatcherRule
import com.sllauncher.pro.launcher.FakeDefaultHomeChecker
import com.sllauncher.pro.launcher.HomeRoleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(checker: FakeDefaultHomeChecker) =
        LauncherSetupViewModel(HomeRoleRepository(checker), checker)

    @Test
    fun `settings fallback is hidden before the user tries the system dialog`() {
        val vm = viewModel(FakeDefaultHomeChecker(isDefault = false))
        assertFalse(vm.uiState.value.showSettingsFallback)
    }

    @Test
    fun `settings fallback is shown immediately when no role dialog exists`() {
        val vm = viewModel(FakeDefaultHomeChecker(isDefault = false, roleDialogAvailable = false))
        assertTrue(vm.uiState.value.showSettingsFallback)
    }

    @Test
    fun `declining the dialog shows the settings fallback`() = runTest {
        val vm = viewModel(FakeDefaultHomeChecker(isDefault = false))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onRoleRequestFinished()

        assertFalse(vm.uiState.value.isDefaultHome)
        assertTrue(vm.uiState.value.showSettingsFallback)
    }

    @Test
    fun `accepting the dialog marks SL as default and hides the fallback`() = runTest {
        val checker = FakeDefaultHomeChecker(isDefault = false)
        val vm = viewModel(checker)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        checker.isDefault = true
        vm.onRoleRequestFinished()

        assertTrue(vm.uiState.value.isDefaultHome)
        assertFalse(vm.uiState.value.showSettingsFallback)
    }
}
