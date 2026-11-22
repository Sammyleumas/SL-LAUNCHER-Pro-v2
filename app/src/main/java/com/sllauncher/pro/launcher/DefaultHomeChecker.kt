package com.sllauncher.pro.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Answers "is SL Launcher the user's default Home app?" and builds the
 * system intents that let the user choose it.
 *
 * Android never lets an app make itself the default launcher silently;
 * the user must confirm in a system dialog or in Settings.
 */
interface DefaultHomeChecker {
    fun isDefaultHome(): Boolean

    /**
     * Intent for the Android 10+ system dialog that asks the user to grant
     * SL Launcher the HOME role, or null when that dialog is unavailable
     * (Android 8–9, or OEM builds without the role).
     */
    fun createRoleRequestIntent(): Intent?

    /** Intent for the system "Default home app" settings screen. */
    fun createHomeSettingsIntent(): Intent
}

class AndroidDefaultHomeChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DefaultHomeChecker {

    override fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            homeRoleManager()?.let { return it.isRoleHeld(RoleManager.ROLE_HOME) }
        }

        // Android 8–9 fallback: ask which activity currently handles the Home intent.
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolved?.activityInfo?.packageName == context.packageName
    }

    override fun createRoleRequestIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return homeRoleManager()?.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }

    override fun createHomeSettingsIntent(): Intent =
        Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** RoleManager, or null on OEM builds that do not offer the HOME role. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun homeRoleManager(): RoleManager? =
        context.getSystemService(RoleManager::class.java)
            ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_HOME) }
}
