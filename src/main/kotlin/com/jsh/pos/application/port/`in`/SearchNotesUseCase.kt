package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note

/**
 * 노트 검색 유스케이스 계약입니다.
 */
interface SearchNotesUseCase {
    fun search(command: Command): List<Note>

    data class Command(
        val keyword: String,
    )
}

