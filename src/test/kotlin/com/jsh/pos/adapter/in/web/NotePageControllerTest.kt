package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.BookmarkNoteUseCase
import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetAllNotesUseCase
import com.jsh.pos.application.port.`in`.GetBookmarkedNotesUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.ui.ExtendedModelMap
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap
import java.time.Instant

class NotePageControllerTest {

    private val createUseCase = FakeCreateUseCase()
    private val getUseCase = FakeGetUseCase()
    private val updateUseCase = FakeUpdateUseCase()
    private val searchUseCase = FakeSearchUseCase()
    private val deleteUseCase = FakeDeleteUseCase()
    private val bookmarkUseCase = FakeBookmarkUseCase()
    private val bookmarkedUseCase = FakeGetBookmarkedUseCase()
    private val getAllUseCase = FakeGetAllUseCase()

    private val controller = NotePageController(
        createNoteUseCase = createUseCase,
        getNoteUseCase = getUseCase,
        updateNoteUseCase = updateUseCase,
        searchNotesUseCase = searchUseCase,
        deleteNoteUseCase = deleteUseCase,
        bookmarkNoteUseCase = bookmarkUseCase,
        getBookmarkedNotesUseCase = bookmarkedUseCase,
        getAllNotesUseCase = getAllUseCase,
    )

    @Test
    fun `list uses getAll when no filter`() {
        getAllUseCase.notes = listOf(sampleNote("note-1", "제목"))

        val model = ExtendedModelMap()
        val viewName = controller.list(keyword = null, bookmarkedOnly = false, model = model)

        assertEquals("notes/list", viewName)
        assertEquals(1, (model["notes"] as List<*>).size)
        assertEquals(1, getAllUseCase.callCount)
    }

    @Test
    fun `list uses search when keyword exists`() {
        searchUseCase.notes = listOf(sampleNote("note-s-1", "검색 결과", tags = setOf("spring", "kotlin")))

        val model = ExtendedModelMap()
        val viewName = controller.list(keyword = "  kotlin  ", bookmarkedOnly = false, model = model)

        assertEquals("notes/list", viewName)
        assertEquals("kotlin", searchUseCase.lastKeyword)
        assertEquals("kotlin", model["keyword"])
        assertEquals(1, (model["notes"] as List<*>).size)
        val tagsDisplayById = model["tagsDisplayById"] as Map<*, *>
        assertEquals("kotlin, spring", tagsDisplayById["note-s-1"])
    }

    @Test
    fun `list uses bookmarked when bookmarkedOnly true`() {
        bookmarkedUseCase.notes = listOf(sampleNote("note-b-1", "북마크", bookmarked = true))

        val model = ExtendedModelMap()
        val viewName = controller.list(keyword = null, bookmarkedOnly = true, model = model)

        assertEquals("notes/list", viewName)
        assertEquals(1, bookmarkedUseCase.callCount)
        assertEquals(1, (model["notes"] as List<*>).size)
    }

    @Test
    fun `newForm returns form view with create mode`() {
        val model = ExtendedModelMap()

        val viewName = controller.newForm(model)

        assertEquals("notes/form", viewName)
        assertEquals("create", model["mode"])
    }

    @Test
    fun `create returns redirect to detail when valid`() {
        val form = NoteForm(
            title = "제목",
            content = "본문",
            visibility = Visibility.PRIVATE,
            tagsText = "kotlin, spring",
        )
        val bindingResult = BeanPropertyBindingResult(form, "form")

        val viewName = controller.create(form, bindingResult, ExtendedModelMap())

        assertEquals("redirect:/notes/note-1", viewName)
        assertEquals(setOf("kotlin", "spring"), createUseCase.lastCommand?.tags)
    }

    @Test
    fun `create returns form when binding has errors`() {
        val form = NoteForm(title = "", content = "")
        val bindingResult = BeanPropertyBindingResult(form, "form")
        bindingResult.rejectValue("title", "NotBlank")

        val model = ExtendedModelMap()
        val viewName = controller.create(form, bindingResult, model)

        assertEquals("notes/form", viewName)
        assertEquals("create", model["mode"])
    }

    @Test
    fun `detail returns detail view when note exists`() {
        getUseCase.note = sampleNote("note-1", "상세", tags = setOf("zeta", "alpha"))

        val model = ExtendedModelMap()
        val redirect = RedirectAttributesModelMap()
        val viewName = controller.detail("note-1", model, redirect)

        assertEquals("notes/detail", viewName)
        assertEquals("상세", (model["note"] as Note).title)
        assertEquals("alpha, zeta", model["tagsDisplay"])
    }

    @Test
    fun `detail redirects to list when note missing`() {
        val model = ExtendedModelMap()
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.detail("missing", model, redirect)

        assertEquals("redirect:/notes", viewName)
        assertEquals("노트를 찾을 수 없습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `editForm returns form with note data when note exists`() {
        getUseCase.note = sampleNote("note-1", "수정 대상")

        val model = ExtendedModelMap()
        val viewName = controller.editForm("note-1", model, RedirectAttributesModelMap())

        assertEquals("notes/form", viewName)
        assertEquals("edit", model["mode"])
        assertEquals("note-1", model["noteId"])
    }

    @Test
    fun `editForm redirects when note missing`() {
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.editForm("missing", ExtendedModelMap(), redirect)

        assertEquals("redirect:/notes", viewName)
        assertEquals("노트를 찾을 수 없습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `edit returns redirect when update target missing`() {
        val form = NoteForm(title = "수정", content = "수정 본문", tagsText = "a, b")
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val model = ExtendedModelMap()
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.edit("missing", form, bindingResult, model, redirect)

        assertEquals("redirect:/notes", viewName)
        assertEquals("노트를 찾을 수 없습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `edit updates and redirects when target exists`() {
        updateUseCase.nextResult = sampleNote("note-1", "수정된 제목")
        val form = NoteForm(title = "수정된 제목", content = "수정 본문", tagsText = "kotlin, spring, kotlin")
        val bindingResult = BeanPropertyBindingResult(form, "form")

        val viewName = controller.edit("note-1", form, bindingResult, ExtendedModelMap(), RedirectAttributesModelMap())

        assertEquals("redirect:/notes/note-1", viewName)
        assertEquals(setOf("kotlin", "spring"), updateUseCase.lastCommand?.tags)
    }

    @Test
    fun `edit returns form when binding has errors`() {
        val form = NoteForm(title = "", content = "")
        val bindingResult = BeanPropertyBindingResult(form, "form")
        bindingResult.rejectValue("content", "NotBlank")
        val model = ExtendedModelMap()

        val viewName = controller.edit("note-1", form, bindingResult, model, RedirectAttributesModelMap())

        assertEquals("notes/form", viewName)
        assertEquals("edit", model["mode"])
        assertEquals("note-1", model["noteId"])
    }

    @Test
    fun `delete adds flash message when target missing`() {
        deleteUseCase.nextDeleted = false
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.delete("missing", redirect)

        assertEquals("redirect:/notes", viewName)
        assertEquals("삭제할 노트를 찾을 수 없습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `delete redirects without message when target exists`() {
        deleteUseCase.nextDeleted = true
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.delete("note-1", redirect)

        assertEquals("redirect:/notes", viewName)
        assertNull(redirect.flashAttributes["message"])
    }

    @Test
    fun `bookmark adds flash when target missing`() {
        bookmarkUseCase.bookmarkResult = null
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.bookmark("missing", redirect)

        assertEquals("redirect:/notes/missing", viewName)
        assertEquals("북마크할 노트를 찾을 수 없습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `bookmark redirects to detail when success`() {
        bookmarkUseCase.bookmarkResult = sampleNote("note-1", "북마크 대상", bookmarked = true)
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.bookmark("note-1", redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        assertNull(redirect.flashAttributes["message"])
    }

    @Test
    fun `unbookmark adds flash when target missing`() {
        bookmarkUseCase.unbookmarkResult = null
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.unbookmark("missing", redirect)

        assertEquals("redirect:/notes/missing", viewName)
        assertEquals("노트를 찾을 수 없습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `unbookmark redirects to detail when success`() {
        bookmarkUseCase.unbookmarkResult = sampleNote("note-1", "북마크 해제 대상", bookmarked = false)
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.unbookmark("note-1", redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        assertNull(redirect.flashAttributes["message"])
    }

    private class FakeCreateUseCase : CreateNoteUseCase {
        var lastCommand: CreateNoteUseCase.Command? = null

        override fun create(command: CreateNoteUseCase.Command): Note {
            lastCommand = command
            return sampleNote("note-1", command.title)
        }
    }

    private class FakeGetUseCase : GetNoteUseCase {
        var note: Note? = null
        override fun getById(id: String): Note? = note
    }

    private class FakeUpdateUseCase : UpdateNoteUseCase {
        var lastCommand: UpdateNoteUseCase.Command? = null
        var nextResult: Note? = null

        override fun updateById(id: String, command: UpdateNoteUseCase.Command): Note? {
            lastCommand = command
            return nextResult
        }
    }

    private class FakeSearchUseCase : SearchNotesUseCase {
        var lastKeyword: String? = null
        var notes: List<Note> = emptyList()

        override fun search(command: SearchNotesUseCase.Command): List<Note> {
            lastKeyword = command.keyword
            return notes
        }
    }

    private class FakeDeleteUseCase : DeleteNoteUseCase {
        var nextDeleted: Boolean = false
        override fun deleteById(id: String): Boolean = nextDeleted
    }

    private class FakeBookmarkUseCase : BookmarkNoteUseCase {
        var bookmarkResult: Note? = null
        var unbookmarkResult: Note? = null

        override fun bookmark(id: String): Note? = bookmarkResult
        override fun unbookmark(id: String): Note? = unbookmarkResult
    }

    private class FakeGetBookmarkedUseCase : GetBookmarkedNotesUseCase {
        var callCount: Int = 0
        var notes: List<Note> = emptyList()

        override fun getBookmarked(): List<Note> {
            callCount += 1
            return notes
        }
    }

    private class FakeGetAllUseCase : GetAllNotesUseCase {
        var callCount: Int = 0
        var notes: List<Note> = emptyList()

        override fun getAll(): List<Note> {
            callCount += 1
            return notes
        }
    }

    companion object {
        private fun sampleNote(
            id: String,
            title: String,
            bookmarked: Boolean = false,
            tags: Set<String> = emptySet(),
        ): Note = Note(
            id = id,
            title = title,
            content = "본문",
            visibility = Visibility.PRIVATE,
            tags = tags,
            bookmarked = bookmarked,
            createdAt = Instant.parse("2026-04-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
        )
    }
}








