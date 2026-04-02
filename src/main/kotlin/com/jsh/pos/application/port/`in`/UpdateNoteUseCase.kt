package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility

/**
 * 노트 수정 유스케이스 계약입니다.
 */
interface UpdateNoteUseCase {
    fun updateById(id: String, command: Command): Note?

    data class Command(
        val title: String,
        val content: String,
        val visibility: Visibility,
        val tags: Set<String> = emptySet(),
    )
}

