package com.jsh.pos.application.port.`in`

import com.jsh.pos.application.model.NoteSearchHit

/**
 * 노트 검색 유스케이스 계약입니다.
 */
interface SearchNotesUseCase {
    fun search(command: Command): List<NoteSearchHit>

    data class Command(
        val ownerUsername: String,
        val keyword: String,
        val sort: String,
    )
}

