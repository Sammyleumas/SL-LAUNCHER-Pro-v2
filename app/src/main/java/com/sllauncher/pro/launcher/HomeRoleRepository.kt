package com.sllauncher.pro.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for whether SL Launcher is the default Home app.
 *
 * Android does not broadcast default-launcher changes, so screens call
 * [refresh] when they resume (the user may have changed it in Settings).
 */
@Singleton
class HomeRoleRepository @Inject constructor(
    private val checker: DefaultHomeChecker,
) {
    private val _isDefaultHome = MutableStateFlow(checker.isDefaultHome())
    val isDefaultHome: StateFlow<Boolean> = _isDefaultHome.asStateFlow()

    fun refresh() {
        _isDefaultHome.value = checker.isDefaultHome()
    }
}
