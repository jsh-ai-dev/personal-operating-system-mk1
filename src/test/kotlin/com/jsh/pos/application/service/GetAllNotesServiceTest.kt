package com.jsh.pos.application.service

import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class GetAllNotesServiceTest {

    private val queryPort = FakeNoteQueryPort()
    private val getAllNotesService = GetAllNotesService(queryPort)

    @Test
    fun `getAll delegates to query port`() {
        val expected = listOf(
            sampleNote("note-1", "제목 1"),
            sampleNote("note-2", "제목 2"),
        )
        queryPort.nextResult = expected

        val result = getAllNotesService.getAll()

        assertEquals(1, queryPort.callCount)
        assertEquals(expected, result)
    }

    private class FakeNoteQueryPort : NoteQueryPort {
        var callCount: Int = 0
        var nextResult: List<Note> = emptyList()

        override fun findById(id: String): Note? = null

        override fun findAll(): List<Note> {
            callCount += 1
            return nextResult
        }
    }

    private fun sampleNote(id: String, title: String): Note = Note(
        id = id,
        title = title,
        content = "content",
        visibility = Visibility.PRIVATE,
        tags = emptySet(),
        createdAt = Instant.parse("2026-04-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
    )
}

