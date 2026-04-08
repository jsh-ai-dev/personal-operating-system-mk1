package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.BookmarkNoteUseCase
import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.SaveNoteSummaryUseCase
import com.jsh.pos.application.port.`in`.SummarizeUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.ui.ExtendedModelMap
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap
import java.time.Instant

class NotePageControllerTest {

    private val createUseCase = FakeCreateUseCase()
    private val getUseCase = FakeGetUseCase()
    private val updateUseCase = FakeUpdateUseCase()
    private val noteListPageService = FakeGetNoteListPageService()
    private val summarizeUseCase = FakeSummarizeUseCase()
    private val saveSummaryUseCase = FakeSaveSummaryUseCase()
    private val deleteUseCase = FakeDeleteUseCase()
    private val bookmarkUseCase = FakeBookmarkUseCase()

    private val controller = NotePageController(
        createNoteUseCase = createUseCase,
        getNoteUseCase = getUseCase,
        updateNoteUseCase = updateUseCase,
        getNoteListPageUseCase = noteListPageService,
        summarizeUseCase = summarizeUseCase,
        saveNoteSummaryUseCase = saveSummaryUseCase,
        deleteNoteUseCase = deleteUseCase,
        bookmarkNoteUseCase = bookmarkUseCase,
    )

    @Test
    fun `list uses getAll when no filter`() {
        noteListPageService.nextResult = GetNoteListPageUseCase.Result(
            notes = listOf(sampleNote("note-1", "제목")),
            keyword = "",
            bookmarkedOnly = false,
            sort = "recent",
        )

        val model = ExtendedModelMap()
        val viewName = controller.list(keyword = null, bookmarkedOnly = false, model = model)

        assertEquals("notes/list", viewName)
        assertEquals(1, (model["notes"] as List<*>).size)
        assertEquals(false, noteListPageService.lastCommand?.bookmarkedOnly)
        assertEquals("recent", noteListPageService.lastCommand?.sort)
    }

    @Test
    fun `list uses search when keyword exists`() {
        noteListPageService.nextResult = GetNoteListPageUseCase.Result(
            notes = listOf(sampleNote("note-s-1", "검색 결과", tags = setOf("spring", "kotlin"))),
            keyword = "kotlin",
            bookmarkedOnly = false,
            sort = "recent",
        )

        val model = ExtendedModelMap()
        val viewName = controller.list(keyword = "  kotlin  ", bookmarkedOnly = false, model = model)

        assertEquals("notes/list", viewName)
        assertEquals("  kotlin  ", noteListPageService.lastCommand?.keyword)
        assertEquals("kotlin", model["keyword"])
        assertEquals(1, (model["notes"] as List<*>).size)
        val tagsDisplayById = model["tagsDisplayById"] as Map<*, *>
        assertEquals("kotlin, spring", tagsDisplayById["note-s-1"])
    }

    @Test
    fun `list uses bookmarked when bookmarkedOnly true`() {
        noteListPageService.nextResult = GetNoteListPageUseCase.Result(
            notes = listOf(sampleNote("note-b-1", "북마크", bookmarked = true)),
            keyword = "",
            bookmarkedOnly = true,
            sort = "recent",
        )

        val model = ExtendedModelMap()
        val viewName = controller.list(keyword = null, bookmarkedOnly = true, model = model)

        assertEquals("notes/list", viewName)
        assertEquals(true, noteListPageService.lastCommand?.bookmarkedOnly)
        assertEquals(1, (model["notes"] as List<*>).size)
    }

    @Test
    fun `list sorts by title when sort is title`() {
        noteListPageService.nextResult = GetNoteListPageUseCase.Result(
            notes = listOf(
                sampleNote("note-1", "a-title"),
                sampleNote("note-2", "z-title"),
            ),
            keyword = "",
            bookmarkedOnly = false,
            sort = "title",
        )

        val model = ExtendedModelMap()
        controller.list(keyword = null, bookmarkedOnly = false, sort = "title", model = model)

        val notes = model["notes"] as List<Note>
        assertEquals(listOf("note-1", "note-2"), notes.map { it.id })
        assertEquals("title", model["sort"])
    }

    @Test
    fun `list falls back to recent sort and exposes date display maps`() {
        noteListPageService.nextResult = GetNoteListPageUseCase.Result(
            notes = listOf(
                sampleNote(
                    id = "newer",
                    title = "new",
                    updatedAt = Instant.parse("2026-04-02T00:00:00Z"),
                ),
                sampleNote(
                    id = "older",
                    title = "old",
                    updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
                ),
            ),
            keyword = "",
            bookmarkedOnly = false,
            sort = "recent",
        )

        val model = ExtendedModelMap()
        controller.list(keyword = null, bookmarkedOnly = false, sort = "unknown", model = model)

        val notes = model["notes"] as List<Note>
        assertEquals(listOf("newer", "older"), notes.map { it.id })
        assertEquals("recent", model["sort"])

        val createdMap = model["createdAtDisplayById"] as Map<*, *>
        val updatedMap = model["updatedAtDisplayById"] as Map<*, *>
        assertTrue(createdMap.containsKey("newer"))
        assertTrue(updatedMap.containsKey("older"))
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
        assertEquals("flash", model["selectedSummaryModelTier"])
    }

    @Test
    fun `generateSummary renders detail with generated summary`() {
        getUseCase.note = sampleNote("note-1", "요약 대상", content = "본문")
        summarizeUseCase.nextSummary = "생성된 요약"

        val model = ExtendedModelMap()
        val redirect = RedirectAttributesModelMap()
        val viewName = controller.generateSummary("note-1", "pro", model, redirect)

        assertEquals("notes/detail", viewName)
        assertEquals("생성된 요약", model["generatedSummary"])
        assertEquals("pro", model["selectedSummaryModelTier"])
    }

    @Test
    fun `saveSummary redirects to detail after save`() {
        getUseCase.note = sampleNote("note-1", "요약 대상", content = "본문")
        saveSummaryUseCase.nextNote = sampleNote("note-1", "요약 대상", content = "본문", aiSummary = "저장된 요약")
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.saveSummary("note-1", "저장된 요약", redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        assertEquals("AI 요약이 저장되었습니다.", redirect.flashAttributes["message"])
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
    fun `editForm redirects when note is stored file`() {
        getUseCase.note = sampleNote(
            id = "note-1",
            title = "pdf-note",
            originalFileName = "pdf-note.pdf",
            fileContentType = "application/pdf",
            hasStoredFile = true,
            fileBytes = byteArrayOf(1, 2, 3),
        )
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.editForm("note-1", ExtendedModelMap(), redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        assertEquals("파일 노트는 수정할 수 없습니다. 다운로드해서 확인해주세요.", redirect.flashAttributes["message"])
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
    fun `edit redirects when target is stored file`() {
        getUseCase.note = sampleNote(
            id = "note-1",
            title = "pdf-note",
            originalFileName = "pdf-note.pdf",
            fileContentType = "application/pdf",
            hasStoredFile = true,
            fileBytes = byteArrayOf(1, 2, 3),
        )
        val form = NoteForm(title = "수정", content = "수정 본문")
        val bindingResult = BeanPropertyBindingResult(form, "form")
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.edit("note-1", form, bindingResult, ExtendedModelMap(), redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        assertEquals("파일 노트는 수정할 수 없습니다. 다운로드해서 확인해주세요.", redirect.flashAttributes["message"])
    }

    @Test
    fun `edit updates and redirects when target exists`() {
        getUseCase.note = sampleNote("note-1", "기존 제목")
        updateUseCase.nextResult = sampleNote("note-1", "수정된 제목")
        val form = NoteForm(title = "수정된 제목", content = "수정 본문", tagsText = "kotlin, spring, kotlin")
        val bindingResult = BeanPropertyBindingResult(form, "form")

        val viewName = controller.edit("note-1", form, bindingResult, ExtendedModelMap(), RedirectAttributesModelMap())

        assertEquals("redirect:/notes/note-1", viewName)
        assertEquals(setOf("kotlin", "spring"), updateUseCase.lastCommand?.tags)
    }

    @Test
    fun `edit returns form when binding has errors`() {
        getUseCase.note = sampleNote("note-1", "기존 제목")
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
        getUseCase.note = sampleNote("note-1", "삭제 대상")
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
        getUseCase.note = sampleNote("note-1", "북마크 대상")
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
        getUseCase.note = sampleNote("note-1", "북마크 해제 대상", bookmarked = true)
        bookmarkUseCase.unbookmarkResult = sampleNote("note-1", "북마크 해제 대상", bookmarked = false)
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.unbookmark("note-1", redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        assertNull(redirect.flashAttributes["message"])
    }

    // ── 파일 업로드 테스트 ──

    @Test
    fun `upload redirects with error when file is empty`() {
        val file = MockMultipartFile("file", "note.txt", "text/plain", ByteArray(0))
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.upload(file, redirect)

        assertEquals("redirect:/notes", viewName)
        assertEquals("파일을 선택해주세요.", redirect.flashAttributes["message"])
    }

    @Test
    fun `upload redirects with error when extension is unsupported`() {
        val file = MockMultipartFile("file", "note.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "내용".toByteArray())
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.upload(file, redirect)

        assertEquals("redirect:/notes", viewName)
        assertEquals("현재 .txt 또는 .pdf 파일만 업로드할 수 있습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `upload redirects with error when file content is blank`() {
        val file = MockMultipartFile("file", "empty.txt", "text/plain", "   ".toByteArray())
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.upload(file, redirect)

        assertEquals("redirect:/notes", viewName)
        assertEquals("파일에 내용이 없습니다.", redirect.flashAttributes["message"])
    }

    @Test
    fun `upload creates note from txt file and redirects to detail`() {
        val content = "코틀린 학습 노트\n코루틴은 가볍다"
        val file = MockMultipartFile("file", "kotlin-study.txt", "text/plain", content.toByteArray(Charsets.UTF_8))
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.upload(file, redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        // 노트 제목은 파일명 확장자 제거
        assertEquals("kotlin-study", createUseCase.lastCommand?.title)
        // 노트 본문은 파일 내용
        assertEquals(content, createUseCase.lastCommand?.content)
        // 원본 파일명 보존
        assertEquals("kotlin-study.txt", createUseCase.lastCommand?.originalFileName)
        assertNull(createUseCase.lastCommand?.fileBytes)
        assertNotNull(redirect.flashAttributes["message"])
    }

    @Test
    fun `upload creates stored file note from pdf file and redirects to detail`() {
        val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        val file = MockMultipartFile("file", "architecture.pdf", "application/pdf", pdfBytes)
        val redirect = RedirectAttributesModelMap()

        val viewName = controller.upload(file, redirect)

        assertEquals("redirect:/notes/note-1", viewName)
        assertEquals("architecture", createUseCase.lastCommand?.title)
        assertEquals("architecture.pdf", createUseCase.lastCommand?.originalFileName)
        assertEquals("application/pdf", createUseCase.lastCommand?.fileContentType)
        assertTrue(createUseCase.lastCommand?.fileBytes?.contentEquals(pdfBytes) == true)
        assertTrue(createUseCase.lastCommand?.content?.contains("architecture.pdf") == true)
    }

    // ── 파일 다운로드 테스트 ──

    @Test
    fun `download returns 404 when note missing`() {
        val response = controller.download("missing")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `download returns file bytes with correct headers`() {
        val content = "코틀린 노트 내용"
        getUseCase.note = sampleNote("note-1", "kotlin-study", originalFileName = "kotlin-study.txt", content = content)
        val response = controller.download("note-1")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(content, response.body?.toString(Charsets.UTF_8))
        val disposition = response.headers.contentDisposition.toString()
        assertTrue(disposition.contains("kotlin-study.txt"), "파일명이 Content-Disposition에 포함되어야 합니다")
    }

    @Test
    fun `download uses note title as filename when originalFileName is null`() {
        val content = "직접 작성한 노트"
        getUseCase.note = sampleNote("note-1", "my-note", content = content)
        val response = controller.download("note-1")

        assertEquals(HttpStatus.OK, response.statusCode)
        val disposition = response.headers.contentDisposition.toString()
        assertTrue(disposition.contains("my-note.txt"), "제목.txt가 파일명으로 사용되어야 합니다")
    }

    @Test
    fun `download returns stored pdf bytes with content type`() {
        val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        getUseCase.note = sampleNote(
            id = "note-1",
            title = "architecture",
            originalFileName = "architecture.pdf",
            fileContentType = "application/pdf",
            hasStoredFile = true,
            fileBytes = pdfBytes,
        )

        val response = controller.download("note-1")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body?.contentEquals(pdfBytes) == true)
        assertEquals("application/pdf", response.headers.contentType?.toString())
        assertTrue(response.headers.contentDisposition.toString().contains("architecture.pdf"))
    }

    @Test
    fun `download returns 500 when stored file bytes are missing`() {
        getUseCase.note = sampleNote(
            id = "note-1",
            title = "broken-pdf",
            originalFileName = "broken-pdf.pdf",
            fileContentType = "application/pdf",
            hasStoredFile = true,
            fileBytes = null,
        )

        val response = controller.download("note-1")

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    private class FakeCreateUseCase : CreateNoteUseCase {
        var lastCommand: CreateNoteUseCase.Command? = null

        override fun create(command: CreateNoteUseCase.Command): Note {
            lastCommand = command
            return sampleNote(
                id = "note-1",
                title = command.title,
                content = command.content,
                originalFileName = command.originalFileName,
                fileContentType = command.fileContentType,
                hasStoredFile = command.fileBytes != null,
                fileBytes = command.fileBytes,
            )
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

    private class FakeGetNoteListPageService : GetNoteListPageUseCase {
        var lastCommand: GetNoteListPageUseCase.Command? = null
        var nextResult: GetNoteListPageUseCase.Result = GetNoteListPageUseCase.Result(emptyList(), "", false, "recent")

        override fun get(command: GetNoteListPageUseCase.Command): GetNoteListPageUseCase.Result {
            lastCommand = command
            return nextResult
        }
    }

    private class FakeDeleteUseCase : DeleteNoteUseCase {
        var nextDeleted: Boolean = false
        override fun deleteById(id: String): Boolean = nextDeleted
    }

    private class FakeSummarizeUseCase : SummarizeUseCase {
        var nextSummary: String = "요약"

        override fun summarize(command: SummarizeUseCase.Command): SummarizeUseCase.Result =
            SummarizeUseCase.Result(
                summary = nextSummary,
                fileName = command.fileName,
                originalLength = command.text.length,
                modelTier = command.modelTier,
            )
    }

    private class FakeSaveSummaryUseCase : SaveNoteSummaryUseCase {
        var nextNote: Note? = null

        override fun save(command: SaveNoteSummaryUseCase.Command): Note? = nextNote
    }

    private class FakeBookmarkUseCase : BookmarkNoteUseCase {
        var bookmarkResult: Note? = null
        var unbookmarkResult: Note? = null

        override fun bookmark(id: String): Note? = bookmarkResult
        override fun unbookmark(id: String): Note? = unbookmarkResult
    }


    companion object {
        private fun sampleNote(
            id: String,
            title: String,
            bookmarked: Boolean = false,
            tags: Set<String> = emptySet(),
            content: String = "본문",
            originalFileName: String? = null,
            fileContentType: String? = null,
            hasStoredFile: Boolean = false,
            fileBytes: ByteArray? = null,
            aiSummary: String? = null,
            createdAt: Instant = Instant.parse("2026-04-01T00:00:00Z"),
            updatedAt: Instant = Instant.parse("2026-04-01T00:00:00Z"),
        ): Note = Note(
            id = id,
            title = title,
            content = content,
            visibility = Visibility.PRIVATE,
            tags = tags,
            bookmarked = bookmarked,
            originalFileName = originalFileName,
            fileContentType = fileContentType,
            hasStoredFile = hasStoredFile,
            fileBytes = fileBytes,
            aiSummary = aiSummary,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}








