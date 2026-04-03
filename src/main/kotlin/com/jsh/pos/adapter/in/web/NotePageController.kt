package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.BookmarkNoteUseCase
import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetAllNotesUseCase
import com.jsh.pos.application.port.`in`.GetBookmarkedNotesUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.SearchNotesUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import com.jsh.pos.domain.note.Visibility
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/notes")
class NotePageController(
    private val createNoteUseCase: CreateNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val bookmarkNoteUseCase: BookmarkNoteUseCase,
    private val getBookmarkedNotesUseCase: GetBookmarkedNotesUseCase,
    private val getAllNotesUseCase: GetAllNotesUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "false") bookmarkedOnly: Boolean,
        model: Model,
    ): String {
        val normalizedKeyword = keyword?.trim().orEmpty()
        val notes = when {
            normalizedKeyword.isNotBlank() -> searchNotesUseCase.search(SearchNotesUseCase.Command(normalizedKeyword))
            bookmarkedOnly -> getBookmarkedNotesUseCase.getBookmarked()
            else -> getAllNotesUseCase.getAll()
        }

        model.addAttribute("notes", notes)
        model.addAttribute("keyword", normalizedKeyword)
        model.addAttribute("bookmarkedOnly", bookmarkedOnly)
        model.addAttribute("tagsDisplayById", notes.associate { it.id to formatTags(it.tags) })
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
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create")
            return "notes/form"
        }

        val created = createNoteUseCase.create(
            CreateNoteUseCase.Command(
                title = form.title,
                content = form.content,
                visibility = form.visibility,
                tags = parseTags(form.tagsText),
            ),
        )

        return "redirect:/notes/${created.id}"
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: String, model: Model, redirectAttributes: RedirectAttributes): String {
        val note = getNoteUseCase.getById(id)
        if (note == null) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
        }

        model.addAttribute("note", note)
        model.addAttribute("tagsDisplay", formatTags(note.tags))
        return "notes/detail"
    }

    @GetMapping("/{id}/edit")
    fun editForm(@PathVariable id: String, model: Model, redirectAttributes: RedirectAttributes): String {
        val note = getNoteUseCase.getById(id)
        if (note == null) {
            redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
            return "redirect:/notes"
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
    ): String {
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
    fun delete(@PathVariable id: String, redirectAttributes: RedirectAttributes): String {
        val deleted = deleteNoteUseCase.deleteById(id)
        if (!deleted) {
            redirectAttributes.addFlashAttribute("message", "삭제할 노트를 찾을 수 없습니다.")
        }
        return "redirect:/notes"
    }

    @PostMapping("/{id}/bookmark")
    fun bookmark(@PathVariable id: String, redirectAttributes: RedirectAttributes): String {
        bookmarkNoteUseCase.bookmark(id)
            ?: redirectAttributes.addFlashAttribute("message", "북마크할 노트를 찾을 수 없습니다.")
        return "redirect:/notes/$id"
    }

    @PostMapping("/{id}/unbookmark")
    fun unbookmark(@PathVariable id: String, redirectAttributes: RedirectAttributes): String {
        bookmarkNoteUseCase.unbookmark(id)
            ?: redirectAttributes.addFlashAttribute("message", "노트를 찾을 수 없습니다.")
        return "redirect:/notes/$id"
    }

    private fun parseTags(tagsText: String): Set<String> =
        tagsText.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun formatTags(tags: Set<String>): String =
        tags
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sortedBy { it.lowercase() }
            .joinToString(", ")
            .ifBlank { "-" }
}

data class NoteForm(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String = "",
    @field:NotBlank(message = "본문은 필수입니다")
    val content: String = "",
    val visibility: Visibility = Visibility.PRIVATE,
    val tagsText: String = "",
)





