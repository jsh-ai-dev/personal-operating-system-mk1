package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.GetAllNotesUseCase
import com.jsh.pos.application.port.`in`.GetBookmarkedNotesUseCase
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class GetNoteListPageService(
    private val searchNotesUseCase: SearchNotesUseCase,
    private val getBookmarkedNotesUseCase: GetBookmarkedNotesUseCase,
    private val getAllNotesUseCase: GetAllNotesUseCase,
    private val noteListCachePort: NoteListCachePort,
) : GetNoteListPageUseCase {

    override fun get(command: GetNoteListPageUseCase.Command): GetNoteListPageUseCase.Result {
        val ownerUsername = command.ownerUsername.trim().ifBlank { "anonymousUser" }
        val normalizedKeyword = command.keyword?.trim().orEmpty()
        val normalizedSort = normalizeSort(command.sort)

        val notes = noteListCachePort.getOrLoad(
            NoteListCachePort.Query(
                ownerUsername = ownerUsername,
                keyword = normalizedKeyword,
                bookmarkedOnly = command.bookmarkedOnly,
                sort = normalizedSort,
            ),
        ) {
            val foundNotes = when {
                normalizedKeyword.isNotBlank() -> searchNotesUseCase.search(SearchNotesUseCase.Command(normalizedKeyword))
                command.bookmarkedOnly -> getBookmarkedNotesUseCase.getBookmarked()
                else -> getAllNotesUseCase.getAll()
            }

            sortNotes(
                foundNotes.filter { it.ownerUsername == ownerUsername },
                normalizedSort,
            )
        }

        return GetNoteListPageUseCase.Result(
            notes = notes,
            keyword = normalizedKeyword,
            bookmarkedOnly = command.bookmarkedOnly,
            sort = normalizedSort,
        )
    }

    private fun normalizeSort(sort: String): String =
        if (sort.equals("title", ignoreCase = true)) "title" else "recent"

    private fun sortNotes(notes: List<Note>, sort: String): List<Note> =
        when (sort) {
            "title" -> notes.sortedBy { it.title.lowercase(Locale.getDefault()) }
            else -> notes.sortedWith(compareByDescending<Note> { it.updatedAt }.thenByDescending { it.createdAt })
        }
}



