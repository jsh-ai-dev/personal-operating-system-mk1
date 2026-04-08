package com.jsh.pos.application.model

import com.jsh.pos.domain.note.Note

data class NoteSearchHighlight(
    val title: String? = null,
    val summary: String? = null,
    val content: String? = null,
)

data class NoteSearchHit(
    val note: Note,
    val highlight: NoteSearchHighlight = NoteSearchHighlight(),
)

