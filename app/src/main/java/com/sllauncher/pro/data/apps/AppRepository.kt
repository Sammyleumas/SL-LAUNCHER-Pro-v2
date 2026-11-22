package com.sllauncher.pro.data.apps

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.sllauncher.pro.data.db.AppMetaDao
import com.sllauncher.pro.data.db.AppMetaEntity
import com.sllauncher.pro.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppRepository"

/** App categories a focus session guards against. */
private val DistractingCategories = setOf(AppCategory.Social, AppCategory.Games, AppCategory.Media)

/**
 * Installed, launchable apps for every user profile on the device, kept up
 * to date through [LauncherApps.Callback]. Uses the launcher-intent
 * <queries> entry in the manifest; no QUERY_ALL_PACKAGES permission.
 */
@Singleton
class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appMetaDao: AppMetaDao,
    private val appLaunchDao: com.sllauncher.pro.data.db.AppLaunchDao,
    private val focusTimer: com.sllauncher.pro.productivity.FocusTimer,
    private val settings: com.sllauncher.pro.data.settings.SettingsRepository,
    private val bus: com.sllauncher.pro.common.UiCommandBus,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    /** Launches from the last 30 days, for time-of-day suggestions. */
    val launchLog: kotlinx.coroutines.flow.Flow<List<com.sllauncher.pro.data.db.AppLaunchEntity>> =
        appLaunchDao.observeSince(System.currentTimeMillis() - 30L * 86_400_000L)
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val ownComponent = "${context.packageName}/com.sllauncher.pro.MainActivity"

    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    val apps: StateFlow<List<LauncherApp>> = _apps.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Apps merged with hidden/favourite/category/usage data. Sorted by label. */
    val entries: StateFlow<List<AppEntry>> = combine(_apps, appMetaDao.observeAll()) { apps, metas ->
        val byKey = metas.associateBy { it.appKey }
        apps.map { app ->
            val meta = byKey[app.key.flatten()]
            val override = meta?.categoryOverride
            AppEntry(
                app = app,
                hidden = meta?.hidden == true,
                favorite = meta?.favorite == true,
                category = override?.removePrefix("builtin:")
                    ?.takeIf { override.startsWith("builtin:") }
                    ?.let { name -> AppCategory.entries.firstOrNull { it.name == name } }
                    ?: app.autoCategory,
                customCategoryId = override?.takeIf { it.startsWith("custom:") }?.removePrefix("custom:")?.toLongOrNull(),
                launchCount = meta?.launchCount ?: 0,
                lastLaunchedAt = meta?.lastLaunchedAt ?: 0,
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun refresh() {
        scope.launch {
            _apps.value = withContext(Dispatchers.IO) { loadApps() }
            _loaded.value = true
        }
    }

    private fun loadApps(): List<LauncherApp> {
        val collator = Collator.getInstance()
        val myUser = Process.myUserHandle()
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList()).mapNotNull { info ->
                val component = info.componentName
                if ("${component.packageName}/${component.className}" == ownComponent) return@mapNotNull null
                toLauncherApp(info, user, serial, isWork = user != myUser)
            }
        }.sortedWith(compareBy(collator) { it.label })
    }

    private fun toLauncherApp(info: LauncherActivityInfo, user: UserHandle, serial: Long, isWork: Boolean): LauncherApp {
        val appInfo = info.applicationInfo
        @Suppress("DEPRECATION")
        val isGame = appInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0
        return LauncherApp(
            key = AppKey(info.componentName.packageName, info.componentName.className, serial),
            label = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName },
            user = user,
            isWorkProfile = isWork,
            autoCategory = classifyApp(appInfo.category, isGame, info.componentName.packageName),
            firstInstallTime = info.firstInstallTime,
        )
    }

    fun findApp(key: AppKey): LauncherApp? = _apps.value.firstOrNull { it.key == key }

    fun userForSerial(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    /** False while a work profile is paused (its apps are temporarily hidden, not uninstalled). */
    fun isProfileAvailable(serial: Long): Boolean {
        val user = userForSerial(serial) ?: return false
        return !userManager.isQuietModeEnabled(user) && userManager.isUserUnlocked(user)
    }

    fun activityInfo(key: AppKey): LauncherActivityInfo? {
        val user = userForSerial(key.userSerial) ?: return null
        return launcherApps.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(key.component), user)
    }

    /**
     * Opens an app and records the launch for Recent/Most used. During a
     * focus session, social, games and media apps ask first (unless [force]).
     */
    fun launch(key: AppKey, sourceBounds: Rect? = null, force: Boolean = false): Boolean {
        if (!force && isDistraction(key)) {
            val label = entries.value.firstOrNull { it.app.key == key }?.app?.label ?: "this app"
            bus.send(com.sllauncher.pro.common.UiCommand.ConfirmFocusLaunch(key, label))
            return true
        }
        val user = userForSerial(key.userSerial) ?: return false
        return try {
            launcherApps.startMainActivity(key.component, user, sourceBounds, ActivityOptions.makeBasic().toBundle())
            recordLaunch(key)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot launch $key", e)
            false
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "App gone: $key", e)
            refresh()
            false
        }
    }

    private fun isDistraction(key: AppKey): Boolean {
        val session = focusTimer.session.value ?: return false
        if (session.phase != com.sllauncher.pro.productivity.FocusPhase.Focus) return false
        if (!settings.current.focusGuardDistractions) return false
        val category = entries.value.firstOrNull { it.app.key == key }?.category ?: return false
        return category in DistractingCategories
    }

    private fun recordLaunch(key: AppKey) {
        scope.launch {
            val flat = key.flatten()
            val now = System.currentTimeMillis()
            val meta = appMetaDao.get(flat) ?: AppMetaEntity(flat)
            appMetaDao.upsert(meta.copy(launchCount = meta.launchCount + 1, lastLaunchedAt = now))
            appLaunchDao.insert(com.sllauncher.pro.data.db.AppLaunchEntity(appKey = flat, at = now))
            appLaunchDao.pruneBefore(now - 30L * 86_400_000L)
        }
    }

    /** Clears launch history (Privacy › clear usage data). */
    suspend fun clearLaunchHistory() = appLaunchDao.clear()

    fun openAppInfo(key: AppKey) {
        val user = userForSerial(key.userSerial) ?: return
        launcherApps.startAppDetailsActivity(key.component, user, null, null)
    }

    /** System uninstall confirmation. Android shows its own dialog; SL never removes apps itself. */
    fun uninstallIntent(packageName: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun updateMeta(key: AppKey, transform: (AppMetaEntity) -> AppMetaEntity) {
        val flat = key.flatten()
        appMetaDao.upsert(transform(appMetaDao.get(flat) ?: AppMetaEntity(flat)))
    }

    // --- App shortcuts (long-press menus and pinned shortcuts) ---

    /** Shortcuts are only readable while SL Launcher is the default home app. */
    val canReadShortcuts: Boolean get() = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    fun shortcutsFor(key: AppKey): List<ShortcutInfo> {
        if (!canReadShortcuts) return emptyList()
        val user = userForSerial(key.userSerial) ?: return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(key.packageName)
            .setActivity(key.component)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return runCatching { launcherApps.getShortcuts(query, user).orEmpty() }
            .getOrDefault(emptyList())
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(5)
    }

    fun pinnedShortcut(packageName: String, id: String, userSerial: Long): ShortcutInfo? {
        if (!canReadShortcuts) return null
        val user = userForSerial(userSerial) ?: return null
        val query = LauncherApps.ShortcutQuery()
            .setPackage(packageName)
            .setShortcutIds(listOf(id))
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        return runCatching { launcherApps.getShortcuts(query, user)?.firstOrNull() }.getOrNull()
    }

    fun startShortcut(packageName: String, id: String, userSerial: Long): Boolean {
        val user = userForSerial(userSerial) ?: return false
        return try {
            launcherApps.startShortcut(packageName, id, null, null, user)
            true
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Shortcut unavailable (SL not default home?)", e)
            false
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Shortcut target missing", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Shortcut not allowed", e)
            false
        }
    }

    /** Keeps a pinned shortcut pinned when the user adds it to home. */
    fun pinShortcut(packageName: String, id: String, userSerial: Long) {
        if (!canReadShortcuts) return
        val user = userForSerial(userSerial) ?: return
        val existing = runCatching {
            launcherApps.getShortcuts(
                LauncherApps.ShortcutQuery().setPackage(packageName).setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED),
                user,
            ).orEmpty().map { it.id }
        }.getOrDefault(emptyList())
        runCatching { launcherApps.pinShortcuts(packageName, (existing + id).distinct(), user) }
            .onFailure { Log.w(TAG, "Could not pin shortcut", it) }
    }

    fun launcherApps(): LauncherApps = launcherApps
}
