package com.sllauncher.pro.data

import android.content.pm.ApplicationInfo
import com.sllauncher.pro.data.apps.AppCategory
import com.sllauncher.pro.data.apps.AppKey
import com.sllauncher.pro.data.apps.classifyApp
import com.sllauncher.pro.productivity.daysUntil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ClassifyAppTest {

    @Test
    fun `declared categories map to SL categories`() {
        assertEquals(AppCategory.Games, classifyApp(ApplicationInfo.CATEGORY_GAME, false, "com.example.puzzle"))
        assertEquals(AppCategory.Social, classifyApp(ApplicationInfo.CATEGORY_SOCIAL, false, "com.example.social"))
        assertEquals(AppCategory.Media, classifyApp(ApplicationInfo.CATEGORY_AUDIO, false, "com.example.audio"))
    }

    @Test
    fun `finance and education are detected from package names`() {
        assertEquals(AppCategory.Finance, classifyApp(ApplicationInfo.CATEGORY_PRODUCTIVITY, false, "com.kuda.android"))
        assertEquals(AppCategory.Education, classifyApp(ApplicationInfo.CATEGORY_UNDEFINED, false, "com.duolingo"))
        assertEquals(AppCategory.Social, classifyApp(ApplicationInfo.CATEGORY_UNDEFINED, false, "com.whatsapp"))
    }

    @Test
    fun `game flag wins and unknown apps are Other`() {
        assertEquals(AppCategory.Games, classifyApp(ApplicationInfo.CATEGORY_UNDEFINED, true, "com.whatever"))
        assertEquals(AppCategory.Other, classifyApp(ApplicationInfo.CATEGORY_UNDEFINED, false, "org.example.zzz"))
    }

    @Test
    fun `app keys round trip`() {
        val key = AppKey("com.whatsapp", "com.whatsapp.Main", 0)
        assertEquals(key, AppKey.parse(key.flatten()))
        assertNull(AppKey.parse("garbage"))
        assertNull(AppKey.parse("a/b#notanumber"))
    }

    @Test
    fun `countdown days`() {
        val today = LocalDate.of(2026, 11, 20)
        val target = LocalDate.of(2026, 12, 25).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(35L, daysUntil(target, ZoneOffset.UTC, today))
    }
}
