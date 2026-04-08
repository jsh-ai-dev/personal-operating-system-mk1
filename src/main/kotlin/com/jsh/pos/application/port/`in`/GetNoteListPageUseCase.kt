package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note

interface GetNoteListPageUseCase {
    fun get(command: Command): Result

    data class Command(
        val ownerUsername: String,
        val keyword: String?,
        val bookmarkedOnly: Boolean,
        val sort: String,
    )

    data class Result(
        val notes: List<Note>,
        val keyword: String,
        val bookmarkedOnly: Boolean,
        val sort: String,
    )
}

