package com.jsh.pos.domain.note

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Note 도메인 규칙 테스트입니다.
 */
class NoteTest {

    @Test
    fun `update keeps createdAt and changes updatedAt`() {
        val createdAt = Instant.parse("2026-04-01T00:00:00Z")
        val updatedAt = Instant.parse("2026-04-02T00:00:00Z")

        val note = Note.create(
            id = "note-1",
            title = "title",
            content = "content",
            visibility = Visibility.PRIVATE,
            tags = setOf("one"),
            now = createdAt,
        )

        val updated = note.update(
            title = "  updated title  ",
            content = "  updated content  ",
            visibility = Visibility.PUBLIC,
            tags = setOf(" kotlin ", " "),
            now = updatedAt,
        )

        assertEquals(createdAt, updated.createdAt)
        assertEquals(updatedAt, updated.updatedAt)
        assertEquals("updated title", updated.title)
        assertEquals("updated content", updated.content)
        assertEquals(setOf("kotlin"), updated.tags)
        assertEquals(Visibility.PUBLIC, updated.visibility)
    }

    @Test
    fun `update throws when content is blank`() {
        val note = Note.create(
            id = "note-1",
            title = "title",
            content = "content",
            visibility = Visibility.PRIVATE,
            tags = emptySet(),
            now = Instant.parse("2026-04-01T00:00:00Z"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            note.update(
                title = "updated",
                content = "   ",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                now = Instant.parse("2026-04-02T00:00:00Z"),
            )
        }
    }
}

