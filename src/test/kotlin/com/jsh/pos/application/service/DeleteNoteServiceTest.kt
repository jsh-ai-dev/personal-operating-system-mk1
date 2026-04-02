package com.jsh.pos.application.service

import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.domain.note.Note
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * DeleteNoteService 단위 테스트입니다.
 *
 * 목표:
 * - 삭제 성공/실패 여부가 서비스에서 그대로 전달되는지 검증
 * - 외부 저장소 구현 없이 서비스 로직만 빠르게 확인
 */
class DeleteNoteServiceTest {

    private val commandPort = RecordingNoteCommandPort()
    private val deleteNoteService = DeleteNoteService(commandPort)

    @Test
    fun `deleteById returns true when repository deletes`() {
        commandPort.nextDeleteResult = true

        val result = deleteNoteService.deleteById("note-1")

        assertTrue(result)
    }

    @Test
    fun `deleteById returns false when repository does not find target`() {
        commandPort.nextDeleteResult = false

        val result = deleteNoteService.deleteById("missing-note")

        assertFalse(result)
    }

    private class RecordingNoteCommandPort : NoteCommandPort {
        var nextDeleteResult: Boolean = false

        override fun save(note: Note): Note = note

        override fun deleteById(id: String): Boolean = nextDeleteResult
    }
}

