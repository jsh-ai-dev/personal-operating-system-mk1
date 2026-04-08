package com.jsh.pos.adapter.out.search.elasticsearch

import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.springframework.data.annotation.Id
import java.time.Instant

data class NoteSearchDocument(
    @Id
    val id: String,
    val ownerUsername: String,
    val title: String,
    val content: String,
    val aiSummary: String? = null,
    val tags: Set<String> = emptySet(),
    val visibility: String,
    val bookmarked: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun toDomain(): Note = Note(
        id = id,
        ownerUsername = ownerUsername,
        title = title,
        content = content,
        aiSummary = aiSummary,
        visibility = runCatching { Visibility.valueOf(visibility) }.getOrDefault(Visibility.PRIVATE),
        tags = tags,
        bookmarked = bookmarked,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(note: Note): NoteSearchDocument = NoteSearchDocument(
            id = note.id,
            ownerUsername = note.ownerUsername,
            title = note.title,
            content = note.content,
            aiSummary = note.aiSummary,
            tags = note.tags,
            visibility = note.visibility.name,
            bookmarked = note.bookmarked,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
        )
    }
}

