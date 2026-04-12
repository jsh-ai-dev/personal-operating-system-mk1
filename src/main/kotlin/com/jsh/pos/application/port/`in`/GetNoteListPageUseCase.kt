package com.jsh.pos.application.port.`in`

import com.jsh.pos.application.model.NoteSearchHighlight
import com.jsh.pos.domain.note.Note

interface GetNoteListPageUseCase {
    fun get(command: Command): Result

    companion object {
        const val DEFAULT_PAGE_SIZE: Int = 20
        const val MAX_PAGE_SIZE: Int = 100
    }

    data class Command(
        val ownerUsername: String,
        val keyword: String?,
        val bookmarkedOnly: Boolean,
        val sort: String,
        val page: Int = 0,
        val size: Int = DEFAULT_PAGE_SIZE,
    )

    data class Result(
        val notes: List<Note>,
        val keyword: String,
        val bookmarkedOnly: Boolean,
        val sort: String,
        val page: Int = 0,
        val size: Int = DEFAULT_PAGE_SIZE,
        val totalElements: Int = 0,
        val totalPages: Int = 0,
        val hasPrevious: Boolean = false,
        val hasNext: Boolean = false,
        val highlightsById: Map<String, NoteSearchHighlight> = emptyMap(),
    )
}

