package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.BookmarkNoteUseCase
import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.SaveNoteSummaryUseCase
import com.jsh.pos.application.port.`in`.SummarizeUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.application.port.out.AiSummaryException
import com.jsh.pos.domain.note.Note
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import com.jsh.pos.domain.note.Visibility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Controller
@RequestMapping("/notes")
class NotePageController(
    private val createNoteUseCase: CreateNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getNoteListPageUseCase: GetNoteListPageUseCase,
    private val summarizeUseCase: SummarizeUseCase,
    private val saveNoteSummaryUseCase: SaveNoteSummaryUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val bookmarkNoteUseCase: BookmarkNoteUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "false") bookmarkedOnly: Boolean,
        @RequestParam(defaultValue = "recent") sort: String = "recent",
        @RequestParam(defaultValue = "0") page: Int = 0,
        @RequestParam(defaultValue = "20") size: Int = GetNoteListPageUseCase.DEFAULT_PAGE_SIZE,
        model: Model,
        authentication: Authentication? = null,
    ): String {
        val result = getNoteListPageUseCase.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = currentUsername(authentication),
                keyword = keyword,
                bookmarkedOnly = bookmarkedOnly,
                sort = sort,
                page = page,
                size = size,
            ),
        )

        model.addAttribute("notes", result.notes)
        model.addAttribute("keyword", result.keyword)
        model.addAttribute("bookmarkedOnly", result.bookmarkedOnly)
        model.addAttribute("sort", result.sort)
        model.addAttribute("page", result.page)
        model.addAttribute("size", result.size)
        model.addAttribute("totalElements", result.totalElements)
        model.addAttribute("totalPages", result.totalPages)
        model.addAttribute("hasPrevious", result.hasPrevious)
        model.addAttribute("hasNext", result.hasNext)
        model.addAttribute("currentPageDisplay", result.page + 1)
        model.addAttribute("pageNumbers", (0 until result.totalPages).toList())
        model.addAttribute("highlightsById", result.highlightsById)
        model.addAttribute("tagsDisplayById", result.notes.associate { it.id to formatTags(it.tags) })
        model.addAttribute("createdAtDisplayById", result.notes.associate { it.id to formatDateTime(it.createdAt) })
        model.addAttribute("updatedAtDisplayById", result.notes.associate { it.id to formatDateTime(it.updatedAt) })
        return "notes/list"
    }

    @GetMapping("/new")
    fun newForm(model: Model): String {
        model.addAttribute("form", NoteForm())
        model.addAttribute("mode", "create")
        return "notes/form"
    }

    @PostMapping
    fun create(
        @Valid @ModelAttribute("form") form: NoteForm,
        bindingResult: BindingResult,
        model: Model,
        authentication: Authentication? = null,
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create")
            return "notes/form"
        }

        val created = createNoteUseCase.create(
            CreateNoteUseCase.Command(
                ownerUsername = currentUsername(authentication),
                title = form.title,
                content = form.content,
                visibility = form.visibility,
                tags = parseTags(form.tagsText),
            ),
        )

        return "redirect:/notes/${created.id}"
    }

    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: String,
        model: Model,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        model.addAttribute("note", note)
        model.addAttribute("tagsDisplay", formatTags(note.tags))
        model.addAttribute("selectedSummaryModelTier", DEFAULT_SUMMARY_MODEL_TIER)
        return "notes/detail"
    }

    @PostMapping("/{id}/summary/generate")
    fun generateSummary(
        @PathVariable id: String,
        @RequestParam(name = "modelTier", defaultValue = DEFAULT_SUMMARY_MODEL_TIER) modelTier: String,
        model: Model,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        val normalizedModelTier = normalizeSummaryModelTier(modelTier)
        val sourceText = try {
            extractSummarySourceText(note)
        } catch (e: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("message", e.message)
            return "redirect:/notes/$id"
        }

        val generatedSummary = try {
            summarizeUseCase.summarize(
                SummarizeUseCase.Command(
                    text = sourceText,
                    fileName = note.originalFileName ?: note.title,
                    modelTier = normalizedModelTier,
                )
            ).summary
        } catch (e: AiSummaryException) {
            redirectAttributes.addFlashAttribute("message", e.message)
            return "redirect:/notes/$id"
        } catch (e: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("message", e.message)
            return "redirect:/notes/$id"
        }

        model.addAttribute("note", note)
        model.addAttribute("tagsDisplay", formatTags(note.tags))
        model.addAttribute("selectedSummaryModelTier", normalizedModelTier)
        model.addAttribute("generatedSummary", generatedSummary)
        model.addAttribute("message", "AI 요약이 생성되었습니다. 저장 버튼을 눌러 반영해주세요.")
        return "notes/detail"
    }

    @PostMapping("/{id}/summary/save")
    fun saveSummary(
        @PathVariable id: String,
        @RequestParam("summary") summary: String,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        return try {
            val saved = saveNoteSummaryUseCase.save(
                SaveNoteSummaryUseCase.Command(
                    id = id,
                    summary = summary,
                )
            )
            if (saved == null) {
                redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
                "redirect:/notes"
            } else {
                redirectAttributes.addFlashAttribute("message", "AI 요약이 저장되었습니다.")
                "redirect:/notes/$id"
            }
        } catch (e: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("message", e.message)
            "redirect:/notes/$id"
        }
    }

    @GetMapping("/{id}/edit")
    fun editForm(
        @PathVariable id: String,
        model: Model,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }
        if (note.hasStoredFile) {
            redirectAttributes.addFlashAttribute("message", "파일 노트는 수정할 수 없습니다. 다운로드해서 확인해주세요.")
            return "redirect:/notes/$id"
        }

        model.addAttribute(
            "form",
            NoteForm(
                title = note.title,
                content = note.content,
                visibility = note.visibility,
                tagsText = note.tags.joinToString(", "),
            ),
        )
        model.addAttribute("mode", "edit")
        model.addAttribute("noteId", id)
        return "notes/form"
    }

    @PostMapping("/{id}/edit")
    fun edit(
        @PathVariable id: String,
        @Valid @ModelAttribute("form") form: NoteForm,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val existing = getNoteUseCase.getById(id)
        if (existing == null || !isOwnedByCurrentUser(existing, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }
        if (existing.hasStoredFile) {
            redirectAttributes.addFlashAttribute("message", "파일 노트는 수정할 수 없습니다. 다운로드해서 확인해주세요.")
            return "redirect:/notes/$id"
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit")
            model.addAttribute("noteId", id)
            return "notes/form"
        }

        val updated = updateNoteUseCase.updateById(
            id,
            UpdateNoteUseCase.Command(
                title = form.title,
                content = form.content,
                visibility = form.visibility,
                tags = parseTags(form.tagsText),
            ),
        )

        if (updated == null) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        return "redirect:/notes/${updated.id}"
    }

    @PostMapping("/{id}/delete")
    fun delete(
        @PathVariable id: String,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "삭제할 노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        val deleted = deleteNoteUseCase.deleteById(id)
        if (!deleted) {
            redirectAttributes.addFlashAttribute("message", "삭제할 노트를 찾을 수 없습니다.")
        }
        return "redirect:/notes"
    }

    /**
     * 메모장(.txt) 또는 PDF 파일을 업로드해 노트로 저장합니다.
     *
     * HTTP 메서드: POST
     * 경로: /notes/upload
     *
     * - txt  : 파일명(확장자 제거) → 제목, 파일 내용(UTF-8) → 본문
     * - pdf  : 파일 자체를 저장하고, 상세에서는 다운로드만 제공
     */
    @PostMapping("/upload")
    fun upload(
        @RequestParam("file") file: MultipartFile,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        if (file.isEmpty) {
            redirectAttributes.addFlashAttribute("message", "파일을 선택해주세요.")
            return "redirect:/notes"
        }

        val originalName = file.originalFilename ?: ""
        val fileType = detectFileType(originalName)
        if (fileType == FileType.UNSUPPORTED) {
            redirectAttributes.addFlashAttribute("message", "현재 .txt 또는 .pdf 파일만 업로드할 수 있습니다.")
            return "redirect:/notes"
        }

        val contentType = file.contentType ?: ""
        if (!isAllowedMime(contentType, fileType)) {
            redirectAttributes.addFlashAttribute("message", "파일 형식이 올바르지 않습니다.")
            return "redirect:/notes"
        }

        // 파일명에서 확장자 제거 → 노트 제목
        val title = originalName.substringBeforeLast(".").trim().ifBlank { originalName }

        val created = try {
            when (fileType) {
                FileType.TEXT -> {
                    val text = file.bytes.toString(Charsets.UTF_8)
                    if (text.isBlank()) {
                        redirectAttributes.addFlashAttribute("message", "파일에 내용이 없습니다.")
                        return "redirect:/notes"
                    }

                    createNoteUseCase.create(
                        CreateNoteUseCase.Command(
                            ownerUsername = currentUsername(authentication),
                            title = title,
                            content = text,
                            visibility = Visibility.PRIVATE,
                            tags = emptySet(),
                            originalFileName = originalName,
                        ),
                    )
                }

                FileType.PDF -> {
                    val fileBytes = file.bytes
                    if (fileBytes.isEmpty()) {
                        redirectAttributes.addFlashAttribute("message", "파일에 내용이 없습니다.")
                        return "redirect:/notes"
                    }

                    createNoteUseCase.create(
                        CreateNoteUseCase.Command(
                            ownerUsername = currentUsername(authentication),
                            title = title,
                            content = buildFilePlaceholderContent(originalName),
                            visibility = Visibility.PRIVATE,
                            tags = emptySet(),
                            originalFileName = originalName,
                            fileContentType = normalizeContentType(contentType, fileType),
                            fileBytes = fileBytes,
                        ),
                    )
                }

                FileType.UNSUPPORTED -> {
                    redirectAttributes.addFlashAttribute("message", "현재 .txt 또는 .pdf 파일만 업로드할 수 있습니다.")
                    return "redirect:/notes"
                }
            }
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("message", "파일을 읽는 중 오류가 발생했습니다: ${e.message}")
            return "redirect:/notes"
        }

        redirectAttributes.addFlashAttribute("message", "'$originalName' 파일이 노트로 저장되었습니다.")
        return "redirect:/notes/${created.id}"
    }

    /**
     * 업로드된 노트의 원본 파일 또는 텍스트 내용을 다운로드합니다.
     *
     * HTTP 메서드: GET
     * 경로: /notes/{id}/download
     */
    @GetMapping("/{id}/download")
    fun download(
        @PathVariable id: String,
        authentication: Authentication? = null,
    ): ResponseEntity<ByteArray> {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            return ResponseEntity.notFound().build()
        }

        val fileName = note.originalFileName ?: if (note.hasStoredFile) "${note.title}.bin" else "${note.title}.txt"
        val bytes = if (note.hasStoredFile) {
            note.fileBytes ?: return ResponseEntity.internalServerError().build()
        } else {
            note.content.toByteArray(StandardCharsets.UTF_8)
        }

        val headers = HttpHeaders()
        headers.contentType = if (note.hasStoredFile) {
            MediaType.parseMediaType(note.fileContentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE)
        } else {
            MediaType.parseMediaType("text/plain; charset=UTF-8")
        }
        headers.contentDisposition = ContentDisposition.attachment()
            .filename(fileName, StandardCharsets.UTF_8)
            .build()

        return ResponseEntity.ok()
            .headers(headers)
            .body(bytes)
    }

    @PostMapping("/{id}/bookmark")
    fun bookmark(
        @PathVariable id: String,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "북마크할 노트를 찾을 수 없습니다.")
            return "redirect:/notes/$id"
        }

        bookmarkNoteUseCase.bookmark(id)
            ?: redirectAttributes.addFlashAttribute("message", "북마크할 노트를 찾을 수 없습니다.")
        return "redirect:/notes/$id"
    }

    @PostMapping("/{id}/unbookmark")
    fun unbookmark(
        @PathVariable id: String,
        redirectAttributes: RedirectAttributes,
        authentication: Authentication? = null,
    ): String {
        val note = getNoteUseCase.getById(id)
        if (note == null || !isOwnedByCurrentUser(note, authentication)) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes/$id"
        }

        bookmarkNoteUseCase.unbookmark(id)
            ?: redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
        return "redirect:/notes/$id"
    }

    private fun isOwnedByCurrentUser(note: Note, authentication: Authentication?): Boolean =
        note.ownerUsername == currentUsername(authentication)

    private fun currentUsername(authentication: Authentication?): String {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication
        val isAuthenticated = auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken
        return if (isAuthenticated) auth.name else "anonymousUser"
    }

    private fun parseTags(tagsText: String): Set<String> =
        tagsText.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun detectFileType(fileName: String): FileType {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".txt") -> FileType.TEXT
            lowerName.endsWith(".pdf") -> FileType.PDF
            else -> FileType.UNSUPPORTED
        }
    }

    private fun isAllowedMime(contentType: String, fileType: FileType): Boolean {
        val normalized = contentType.lowercase()
        return when (fileType) {
            FileType.TEXT -> normalized.startsWith("text/") || normalized == "application/octet-stream"
            FileType.PDF -> normalized == "application/pdf" || normalized == "application/octet-stream"
            FileType.UNSUPPORTED -> false
        }
    }

    private fun normalizeContentType(contentType: String, fileType: FileType): String = when (fileType) {
        FileType.TEXT -> if (contentType.isBlank()) "text/plain" else contentType
        FileType.PDF -> if (contentType.isBlank() || contentType == "application/octet-stream") "application/pdf" else contentType
        FileType.UNSUPPORTED -> MediaType.APPLICATION_OCTET_STREAM_VALUE
    }

    private fun buildFilePlaceholderContent(fileName: String): String =
        "업로드된 PDF 파일 '$fileName' 입니다. 상세 화면에서 다운로드해 확인하세요."

    private fun formatTags(tags: Set<String>): String =
        tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sortedBy { it.lowercase() }
            .joinToString(", ")
            .ifBlank { "-" }

    private fun extractSummarySourceText(note: Note): String {
        if (!note.hasStoredFile) {
            return note.content
        }

        val bytes = note.fileBytes ?: throw IllegalArgumentException("파일 본문을 읽을 수 없습니다. 다시 업로드해주세요.")
        val contentType = note.fileContentType?.lowercase().orEmpty()

        return when {
            contentType == "application/pdf" || note.originalFileName?.lowercase()?.endsWith(".pdf") == true -> {
                ByteArrayInputStream(bytes).use { input ->
                    PDDocument.load(input).use { document ->
                        PDFTextStripper().getText(document)
                    }
                }.trim().ifBlank {
                    throw IllegalArgumentException("PDF에서 읽을 수 있는 텍스트가 없습니다.")
                }
            }

            else -> throw IllegalArgumentException("현재 AI 요약은 텍스트와 PDF 파일만 지원합니다.")
        }
    }

    private fun normalizeSummaryModelTier(modelTier: String): String {
        val normalized = modelTier.trim().lowercase()
        return if (normalized in ALLOWED_SUMMARY_MODEL_TIERS) normalized else DEFAULT_SUMMARY_MODEL_TIER
    }


    private fun formatDateTime(value: Instant): String = DISPLAY_DATE_FORMATTER.format(value)

    companion object {
        private val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withLocale(Locale.KOREA)
                .withZone(ZoneId.systemDefault())
        private const val DEFAULT_SUMMARY_MODEL_TIER = "flash"
        private val ALLOWED_SUMMARY_MODEL_TIERS = setOf("flash", "pro")
    }

    private enum class FileType {
        TEXT,
        PDF,
        UNSUPPORTED,
    }
}

data class NoteForm(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String = "",
    @field:NotBlank(message = "본문은 필수입니다")
    val content: String = "",
    val visibility: Visibility = Visibility.PRIVATE,
    val tagsText: String = "",
)





