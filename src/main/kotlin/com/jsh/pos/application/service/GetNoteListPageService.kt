package com.jsh.pos.application.service

import com.jsh.pos.application.model.NoteSearchHighlight
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.application.port.out.NoteQueryPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service
import org.springframework.web.util.HtmlUtils
import java.util.Locale

@Service
class GetNoteListPageService(
    private val searchNotesUseCase: SearchNotesUseCase,
    private val noteQueryPort: NoteQueryPort,
    private val noteListCachePort: NoteListCachePort,
) : GetNoteListPageUseCase {

    override fun get(command: GetNoteListPageUseCase.Command): GetNoteListPageUseCase.Result {
        val ownerUsername = command.ownerUsername.trim().ifBlank { "anonymousUser" }
        val normalizedKeyword = command.keyword?.trim().orEmpty()
        val normalizedSort = normalizeSort(command.sort)
        val normalizedPage = normalizePage(command.page)
        val normalizedSize = normalizeSize(command.size)

        val highlightsById = mutableMapOf<String, NoteSearchHighlight>()

        if (normalizedKeyword.isNotBlank()) {
            val searchResult = searchNotesUseCase.search(
                SearchNotesUseCase.Command(
                    ownerUsername = ownerUsername,
                    keyword = normalizedKeyword,
                    sort = normalizedSort,
                    page = normalizedPage,
                    size = normalizedSize,
                ),
            )
            searchResult.hits.forEach { hit ->
                val highlight = if (hasAnyHighlight(hit.highlight)) {
                    hit.highlight
                } else {
                    buildFallbackHighlight(hit.note, normalizedKeyword)
                }
                if (hasAnyHighlight(highlight)) {
                    highlightsById[hit.note.id] = highlight
                }
            }

            return GetNoteListPageUseCase.Result(
                notes = searchResult.hits.map { it.note },
                keyword = normalizedKeyword,
                bookmarkedOnly = command.bookmarkedOnly,
                sort = normalizedSort,
                page = searchResult.page,
                size = searchResult.size,
                totalElements = searchResult.totalElements,
                totalPages = searchResult.totalPages,
                hasPrevious = searchResult.hasPrevious,
                hasNext = searchResult.hasNext,
                highlightsById = highlightsById,
            )
        }

        val pageResult = noteListCachePort.getOrLoadPage(
            NoteListCachePort.Query(
                ownerUsername = ownerUsername,
                keyword = normalizedKeyword,
                bookmarkedOnly = command.bookmarkedOnly,
                sort = normalizedSort,
                page = normalizedPage,
                size = normalizedSize,
            ),
        ) {
            when {
                command.bookmarkedOnly -> noteQueryPort.findBookmarkedPageByOwner(
                    ownerUsername = ownerUsername,
                    sort = normalizedSort,
                    page = normalizedPage,
                    size = normalizedSize,
                )

                else -> noteQueryPort.findPageByOwner(
                    ownerUsername = ownerUsername,
                    sort = normalizedSort,
                    page = normalizedPage,
                    size = normalizedSize,
                )
            }
        }

        return GetNoteListPageUseCase.Result(
            notes = pageResult.items,
            keyword = normalizedKeyword,
            bookmarkedOnly = command.bookmarkedOnly,
            sort = normalizedSort,
            page = pageResult.page,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
            hasPrevious = pageResult.hasPrevious,
            hasNext = pageResult.hasNext,
            highlightsById = highlightsById,
        )
    }

    private fun normalizePage(page: Int): Int = page.coerceAtLeast(0)

    private fun normalizeSize(size: Int): Int =
        size.coerceIn(1, GetNoteListPageUseCase.MAX_PAGE_SIZE)

    private fun normalizeSort(sort: String): String {
        val normalized = sort.trim().lowercase(Locale.getDefault())
        return if (normalized in ALLOWED_SORTS) normalized else "recent"
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



