package com.sllauncher.pro.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRoleRepositoryTest {

    @Test
    fun `initial value reflects the checker`() {
        val repository = HomeRoleRepository(FakeDefaultHomeChecker(isDefault = true))
        assertTrue(repository.isDefaultHome.value)
    }

    @Test
    fun `refresh picks up a change made in system settings`() {
        val checker = FakeDefaultHomeChecker(isDefault = false)
        val repository = HomeRoleRepository(checker)
        assertFalse(repository.isDefaultHome.value)

        checker.isDefault = true
        repository.refresh()

        assertTrue(repository.isDefaultHome.value)
        assertEquals(2, checker.checkCount)
    }
}
