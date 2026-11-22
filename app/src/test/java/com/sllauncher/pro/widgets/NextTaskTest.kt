package com.sllauncher.pro.widgets

import com.sllauncher.pro.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextTaskTest {
    private val now = 1_000_000_000L

    @Test fun `earliest upcoming due task wins`() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "later", dueAt = now + 7_200_000, createdAt = 0),
            TaskEntity(id = 2, title = "soon", dueAt = now + 600_000, createdAt = 0),
            TaskEntity(id = 3, title = "no date", createdAt = 5),
        )
        assertEquals("soon", nextTask(tasks, now)?.title)
    }

    @Test fun `done tasks are ignored and undated tasks are the fallback`() {
        val tasks = listOf(
            TaskEntity(id = 1, title = "done", dueAt = now + 600_000, done = true, createdAt = 0),
            TaskEntity(id = 2, title = "undated", createdAt = 1),
        )
        assertEquals("undated", nextTask(tasks, now)?.title)
    }

    @Test fun `nothing open gives null`() {
        assertNull(nextTask(listOf(TaskEntity(id = 1, title = "x", done = true, createdAt = 0)), now))
    }
}
