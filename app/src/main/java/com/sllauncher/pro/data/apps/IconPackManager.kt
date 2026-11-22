package com.sllauncher.pro.data.apps

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "IconPackManager"

/** An installed icon pack the user can choose. */
data class IconPack(val packageName: String, val label: String)

/**
 * Supports icon packs in the widely used ADW/Nova format: the pack declares
 * an `appfilter.xml` mapping `ComponentInfo{package/class}` to drawable
 * names. Apps the pack does not cover keep their own icon.
 */
@Singleton
class IconPackManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    @Volatile var activePackage: String? = null
        private set

    @Volatile private var mapping: Map<String, String> = emptyMap()
    @Volatile private var packResources: Resources? = null

    fun installedPacks(): List<IconPack> {
        val pm = context.packageManager
        val intents = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")
        return intents.flatMap { action ->
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
        }.map { it.activityInfo.packageName }
            .distinct()
            .map { pkg ->
                val label = runCatching {
                    @Suppress("DEPRECATION")
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                IconPack(pkg, label)
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Loads [packageName]'s mapping, or clears it when null. Returns false if the pack is unusable. */
    fun select(packageName: String?): Boolean {
        if (packageName == activePackage) return true
        if (packageName == null) {
            activePackage = null; mapping = emptyMap(); packResources = null
            return true
        }
        return try {
            val res = context.packageManager.getResourcesForApplication(packageName)
            mapping = parseAppFilter(res, packageName)
            packResources = res
            activePackage = packageName
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Icon pack not installed: $packageName", e)
            false
        }
    }

    @SuppressLint("DiscouragedApi") // Icon packs are addressed by resource name by design.
    fun iconFor(component: ComponentName): Drawable? {
        val res = packResources ?: return null
        val pkg = activePackage ?: return null
        val name = mapping["${component.packageName}/${component.className}"] ?: return null
        val id = res.getIdentifier(name, "drawable", pkg)
        if (id == 0) return null
        return runCatching { res.getDrawableForDensity(id, context.resources.displayMetrics.densityDpi, null) }.getOrNull()
    }

    @SuppressLint("DiscouragedApi")
    private fun parseAppFilter(res: Resources, pkg: String): Map<String, String> {
        val result = HashMap<String, String>()
        val parser: XmlPullParser = run {
            val xmlId = res.getIdentifier("appfilter", "xml", pkg)
            if (xmlId != 0) {
                res.getXml(xmlId)
            } else {
                val stream = runCatching { res.assets.open("appfilter.xml") }.getOrNull() ?: return result
                XmlPullParserFactory.newInstance().newPullParser().apply { setInput(stream, "UTF-8") }
            }
        }
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    parseComponentInfo(component)?.let { key -> if (drawable != null) result[key] = drawable }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            // A malformed pack must never break the launcher; use what parsed so far.
            Log.w(TAG, "Malformed appfilter in $pkg", e)
        }
        return result
    }
}

/** "ComponentInfo{com.app/com.app.Main}" -> "com.app/com.app.Main". Relative class names are expanded. */
fun parseComponentInfo(raw: String?): String? {
    if (raw == null || !raw.startsWith("ComponentInfo{") || !raw.endsWith("}")) return null
    val inner = raw.removePrefix("ComponentInfo{").removeSuffix("}")
    val slash = inner.indexOf('/')
    if (slash <= 0 || slash == inner.length - 1) return null
    val pkg = inner.substring(0, slash)
    val cls = inner.substring(slash + 1).let { if (it.startsWith(".")) pkg + it else it }
    return "$pkg/$cls"
}
