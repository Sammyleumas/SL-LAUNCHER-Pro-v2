package com.sllauncher.pro.launcher

import android.content.Intent

/** Test double: the test decides whether SL is the default Home app. */
class FakeDefaultHomeChecker(
    var isDefault: Boolean = false,
    private val roleDialogAvailable: Boolean = true,
) : DefaultHomeChecker {
    var checkCount = 0
        private set

    override fun isDefaultHome(): Boolean {
        checkCount++
        return isDefault
    }

    // Unit tests run without the Android framework (isReturnDefaultValues = true),
    // so a bare Intent() is a harmless placeholder object here.
    override fun createRoleRequestIntent(): Intent? = if (roleDialogAvailable) Intent() else null

    override fun createHomeSettingsIntent(): Intent = Intent()
}
