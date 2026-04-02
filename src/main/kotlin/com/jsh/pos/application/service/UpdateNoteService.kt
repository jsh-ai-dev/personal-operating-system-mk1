package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service
import java.time.Clock

/**
 * 노트 수정 유스케이스 구현체입니다.
 */
@Service
class UpdateNoteService(
    private val noteQueryPort: NoteQueryPort,
    private val noteCommandPort: NoteCommandPort,
    private val clock: Clock,
) : UpdateNoteUseCase {

    override fun updateById(id: String, command: UpdateNoteUseCase.Command): Note? {
        val existing = noteQueryPort.findById(id) ?: return null

        val updated = existing.update(
            title = command.title,
            content = command.content,
            visibility = command.visibility,
            tags = command.tags,
            now = clock.instant(),
        )

        return noteCommandPort.save(updated)
    }
}

