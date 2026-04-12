package com.jsh.pos.application.port.`in`

import com.jsh.pos.application.model.NoteSearchHit

/**
 * 노트 검색 유스케이스 계약입니다.
 */
interface SearchNotesUseCase {
    fun search(command: Command): Result

    data class Command(
        val ownerUsername: String,
        val keyword: String,
        val sort: String,
        val page: Int,
        val size: Int,
    )

    data class Result(
        val hits: List<NoteSearchHit>,
        val page: Int,
        val size: Int,
        val totalElements: Int,
        val totalPages: Int,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
    )
}

