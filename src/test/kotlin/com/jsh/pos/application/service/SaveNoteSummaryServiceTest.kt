package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.SaveNoteSummaryUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SaveNoteSummaryServiceTest {

    private val fixedNow: Instant = Instant.parse("2026-04-07T00:00:00Z")
    private val fixedClock: Clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
    private val noteQueryPort = FakeNoteQueryPort()
    private val noteCommandPort = FakeNoteCommandPort()
    private val noteListCachePort = RecordingNoteListCachePort()

    private val service = SaveNoteSummaryService(
        noteQueryPort = noteQueryPort,
        noteCommandPort = noteCommandPort,
        noteListCachePort = noteListCachePort,
        clock = fixedClock,
    )

    @Test
    fun `save returns null when note does not exist`() {
        noteQueryPort.note = null

        val result = service.save(SaveNoteSummaryUseCase.Command(id = "missing", summary = "요약"))

        assertNull(result)
    }

    @Test
    fun `save updates ai summary and updatedAt`() {
        val existing = sampleNote(id = "note-1", aiSummary = null, updatedAt = Instant.parse("2026-04-01T00:00:00Z"))
        noteQueryPort.note = existing

        val result = service.save(SaveNoteSummaryUseCase.Command(id = "note-1", summary = "  저장할 요약  "))

        assertEquals("저장할 요약", result?.aiSummary)
        assertEquals(fixedNow, result?.updatedAt)
        assertEquals("pos-admin", noteListCachePort.lastEvictedOwner)
    }

    @Test
    fun `save throws when summary is blank`() {
        noteQueryPort.note = sampleNote(id = "note-1")

        assertThrows(IllegalArgumentException::class.java) {
            service.save(SaveNoteSummaryUseCase.Command(id = "note-1", summary = "   "))
        }
    }

    private class FakeNoteQueryPort : NoteQueryPort {
        var note: Note? = null
        override fun findById(id: String): Note? = note
    }

    private class FakeNoteCommandPort : NoteCommandPort {
        override fun save(note: Note): Note = note
        override fun deleteById(id: String): Boolean = false
    }

    private class RecordingNoteListCachePort : NoteListCachePort {
        var lastEvictedOwner: String? = null

        override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> = loader()

        override fun evictOwner(ownerUsername: String) {
            lastEvictedOwner = ownerUsername
        }
    }

    companion object {
        private fun sampleNote(
            id: String,
            aiSummary: String? = null,
            updatedAt: Instant = Instant.parse("2026-04-01T00:00:00Z"),
        ): Note = Note(
            id = id,
            ownerUsername = "pos-admin",
            title = "title",
            content = "content",
            visibility = Visibility.PRIVATE,
            tags = emptySet(),
            bookmarked = false,
            createdAt = Instant.parse("2026-04-01T00:00:00Z"),
            updatedAt = updatedAt,
            aiSummary = aiSummary,
        )
    }
}

