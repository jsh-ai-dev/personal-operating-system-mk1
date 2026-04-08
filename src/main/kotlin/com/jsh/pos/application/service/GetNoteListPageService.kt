package com.jsh.pos.application.service

import com.jsh.pos.application.model.NoteSearchHighlight
import com.jsh.pos.application.port.`in`.GetAllNotesUseCase
import com.jsh.pos.application.port.`in`.GetBookmarkedNotesUseCase
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
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

        val highlightsById = mutableMapOf<String, NoteSearchHighlight>()

        val notes = if (normalizedKeyword.isNotBlank()) {
            val searchHits = searchNotesUseCase.search(
                    SearchNotesUseCase.Command(
                        ownerUsername = ownerUsername,
                        keyword = normalizedKeyword,
                        sort = normalizedSort,
                    ),
                )
            searchHits.forEach { hit ->
                val highlight = if (hasAnyHighlight(hit.highlight)) {
                    hit.highlight
                } else {
                    buildFallbackHighlight(hit.note, normalizedKeyword)
                }
                if (hasAnyHighlight(highlight)) {
                    highlightsById[hit.note.id] = highlight
                }
            }
            val foundNotes = searchHits.map { it.note }

            sortNotes(
                foundNotes.filter { it.ownerUsername == ownerUsername },
                normalizedSort,
            )
        } else {
            noteListCachePort.getOrLoad(
                NoteListCachePort.Query(
                    ownerUsername = ownerUsername,
                    keyword = normalizedKeyword,
                    bookmarkedOnly = command.bookmarkedOnly,
                    sort = normalizedSort,
                ),
            ) {
                val foundNotes = when {
                    command.bookmarkedOnly -> getBookmarkedNotesUseCase.getBookmarked()
                    else -> getAllNotesUseCase.getAll()
                }

                sortNotes(
                    foundNotes.filter { it.ownerUsername == ownerUsername },
                    normalizedSort,
                )
            }
        }

        return GetNoteListPageUseCase.Result(
            notes = notes,
            keyword = normalizedKeyword,
            bookmarkedOnly = command.bookmarkedOnly,
            sort = normalizedSort,
            highlightsById = highlightsById,
        )
    }

    private fun normalizeSort(sort: String): String {
        val normalized = sort.trim().lowercase(Locale.getDefault())
        return if (normalized in ALLOWED_SORTS) normalized else "recent"
    }

    private fun sortNotes(notes: List<Note>, sort: String): List<Note> =
        when (sort) {
            "title" -> notes.sortedBy { it.title.lowercase(Locale.getDefault()) }
            "relevance" -> notes
            else -> notes.sortedWith(compareByDescending<Note> { it.updatedAt }.thenByDescending { it.createdAt })
        }

    private fun hasAnyHighlight(highlight: NoteSearchHighlight): Boolean =
        highlight.title != null || highlight.summary != null || highlight.content != null

    private fun buildFallbackHighlight(note: Note, keyword: String): NoteSearchHighlight {
        val titleSnippet = buildMarkedSnippet(note.title, keyword, around = 40)
        val summarySnippet = buildMarkedSnippet(note.aiSummary ?: "", keyword, around = 80)
        val contentSnippet = buildMarkedSnippet(note.content, keyword, around = 80)
        return NoteSearchHighlight(
            title = titleSnippet,
            summary = summarySnippet,
            content = contentSnippet,
        )
    }

    private fun buildMarkedSnippet(text: String, keyword: String, around: Int): String? {
        if (text.isBlank() || keyword.isBlank()) {
            return null
        }
        val index = text.indexOf(keyword, ignoreCase = true)
        if (index < 0) {
            return null
        }

        val start = (index - around).coerceAtLeast(0)
        val end = (index + keyword.length + around).coerceAtMost(text.length)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < text.length) "..." else ""
        val rawSnippet = text.substring(start, end)
        val escaped = HtmlUtils.htmlEscape(rawSnippet)
        val escapedKeyword = Regex.escape(HtmlUtils.htmlEscape(keyword))
        val marked = escaped.replace(Regex(escapedKeyword, RegexOption.IGNORE_CASE), "<mark>$0</mark>")
        return prefix + marked + suffix
    }

    private companion object {
        private val ALLOWED_SORTS = setOf("recent", "title", "relevance")
    }
}



