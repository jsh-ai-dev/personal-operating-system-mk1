package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.BookmarkNoteUseCase
import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteListPageUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 노트 REST 컨트롤러입니다.
 *
 * 역할 (어댑터 패턴):
 * - HTTP 요청을 받아 유스케이스 호출로 변환
 * - 유스케이스 결과를 HTTP 응답으로 변환
 * - 유스케이스 구현 방식(서비스, 저장소 등)은 몰라야 함
 *
 * 설계:
 * - port.in (CreateNoteUseCase, GetNoteUseCase)에만 의존
 * - port.in은 인터페이스이므로 나중에 구현 교체 가능
 * - 테스트에서 mock으로 대체 가능
 *
 * HTTP 규약:
 * - POST 201 Created: 자원 생성 성공
 * - GET 200 OK: 조회 성공
 * - GET 404 Not Found: 자원 없음
 * - GET /search 200 OK: 검색 성공 (결과 배열)
 * - GET /bookmarks 200 OK: 북마크 목록 조회 성공
 * - PUT 200 OK: 수정 성공
 * - PUT 404 Not Found: 수정 대상 없음
 * - DELETE 204 No Content: 삭제 성공
 * - DELETE 404 Not Found: 삭제 대상 없음
 * - POST /{id}/bookmark 200 OK: 북마크 등록 성공
 * - POST /{id}/bookmark 404 Not Found: 북마크 대상 없음
 * - DELETE /{id}/bookmark 200 OK: 북마크 해제 성공
 * - DELETE /{id}/bookmark 404 Not Found: 북마크 해제 대상 없음
 */
@RestController
@RequestMapping("/api/v1/notes")
class NoteController(
    private val createNoteUseCase: CreateNoteUseCase,
    private val getNoteUseCase: GetNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getNoteListPageUseCase: GetNoteListPageUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    // port.in 주입: 북마크 ON/OFF 유스케이스
    private val bookmarkNoteUseCase: BookmarkNoteUseCase,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "false") bookmarkedOnly: Boolean,
        @RequestParam(defaultValue = "recent") sort: String,
        authentication: Authentication? = null,
    ): ResponseEntity<List<NoteResponse>> {
        val result = getNoteListPageUseCase.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = currentUsername(authentication),
                keyword = keyword,
                bookmarkedOnly = bookmarkedOnly,
                sort = sort,
            ),
        )

        return ResponseEntity.ok(result.notes.map { it.toResponse() })
    }

    /**
     * 새 노트를 생성합니다.
     *
     * HTTP 메서드: POST
     * 경로: /api/v1/notes
     * Content-Type: application/json
     *
     * @param request 생성 요청 DTO (제목, 본문, 공개범위, 태그)
     * @return 201 Created + 생성된 노트의 응답 DTO
     *
     * 요청 예시:
     * POST /api/v1/notes
     * {
     *   "title": "Kotlin 공부",
     *   "content": "data class의 장점...",
     *   "visibility": "PRIVATE",
     *   "tags": ["kotlin", "spring"]
     * }
     */
    @PostMapping
    fun create(@Valid @RequestBody request: CreateNoteRequest): ResponseEntity<NoteResponse> {
        // [1-POST] HTTP 요청이 가장 먼저 들어오는 진입점입니다.
        // 브레이크포인트 추천: request 값(title/content/visibility/tags)이 예상대로 들어오는지 확인
        // 1. 요청 DTO를 유스케이스 Command로 변환
        val note = createNoteUseCase.create(
            CreateNoteUseCase.Command(
                title = request.title,
                content = request.content,
                visibility = request.visibility,
                tags = request.tags,
            ),
        )

        // 2. 응답 DTO로 변환 후 201 Created로 반환
        // 201은 HTTP 표준: 새 자원이 생성되었을 때 사용
        // [5-POST] 최종 HTTP 응답 반환 지점입니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(note.toResponse())
    }

    /**
     * ID로 노트를 조회합니다.
     *
     * HTTP 메서드: GET
     * 경로: /api/v1/notes/{id}
     *
     * @param id 조회할 노트의 ID (경로 변수)
     * @return 200 OK + 노트 응답 DTO, 또는 404 Not Found
     *
     * 요청 예시:
     * GET /api/v1/notes/550e8400-e29b-41d4-a716-446655440000
     */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<NoteResponse> {
        // [1-GET] 단건 조회 요청의 시작점입니다.
        // 브레이크포인트 추천: path variable인 id가 올바른지 확인
        // 1. 유스케이스 호출해 노트 조회
        val note = getNoteUseCase.getById(id)

        // 2. 없으면 404 반환
        ?: return ResponseEntity.notFound().build()

        // 3. 있으면 200 OK로 응답 DTO 반환
        // [5-GET] 조회 성공 시 응답을 반환하는 지점입니다.
        return ResponseEntity.ok(note.toResponse())
    }

    /**
     * 키워드로 노트를 검색합니다.
     *
     * HTTP 메서드: GET
     * 경로: /api/v1/notes/search?keyword=...
     *
     * @param keyword 검색어
     * @return 200 OK + 검색 결과 배열
     */
    @GetMapping("/search")
    fun search(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "recent") sort: String,
        authentication: Authentication? = null,
    ): ResponseEntity<List<NoteResponse>> {
        // [1-SEARCH] 검색 요청의 시작점입니다.
        // 브레이크포인트 추천: keyword에 앞뒤 공백이 포함되어 들어오는지 확인
        require(keyword.isNotBlank()) { "검색어는 비워둘 수 없습니다" }
        val result = getNoteListPageUseCase.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = currentUsername(authentication),
                keyword = keyword,
                bookmarkedOnly = false,
                sort = sort,
            ),
        )
        // [5-SEARCH] 검색 결과를 응답 DTO로 변환해 반환하는 지점입니다.
        return ResponseEntity.ok(result.notes.map { it.toResponse() })
    }

    /**
     * ID로 노트를 수정합니다.
     *
     * HTTP 메서드: PUT
     * 경로: /api/v1/notes/{id}
     */
    @PutMapping("/{id}")
    fun updateById(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateNoteRequest,
    ): ResponseEntity<NoteResponse> {
        // [1-PUT] 수정 요청의 시작점입니다.
        // 브레이크포인트 추천: id와 request 본문이 함께 올바르게 들어오는지 확인
        val updated = updateNoteUseCase.updateById(
            id = id,
            command = UpdateNoteUseCase.Command(
                title = request.title,
                content = request.content,
                visibility = request.visibility,
                tags = request.tags,
            ),
        ) ?: return ResponseEntity.notFound().build()

        // [5-PUT] 수정 성공 후 200 OK 응답을 반환하는 지점입니다.
        return ResponseEntity.ok(updated.toResponse())
    }

    /**
     * ID로 노트를 삭제합니다.
     *
     * HTTP 메서드: DELETE
     * 경로: /api/v1/notes/{id}
     *
     * 응답 규약:
     * - 삭제 성공: 204 No Content
     * - 대상 없음: 404 Not Found
     */
    @DeleteMapping("/{id}")
    fun deleteById(@PathVariable id: String): ResponseEntity<Void> {
        // [1-DELETE] 삭제 요청의 시작점입니다.
        // 브레이크포인트 추천: 삭제 대상 id와 delete 결과(true/false) 분기 확인
        val deleted = deleteNoteUseCase.deleteById(id)
        return if (deleted) {
            // [5-DELETE] 삭제 성공 시 204 No Content 반환
            ResponseEntity.noContent().build()
        } else {
            // [5-DELETE] 삭제 대상이 없으면 404 Not Found 반환
            ResponseEntity.notFound().build()
        }
    }

    /**
     * 북마크된 노트 목록을 최신 수정순으로 조회합니다.
     *
     * HTTP 메서드: GET
     * 경로: /api/v1/notes/bookmarks
     *
     * 주의: Spring MVC는 리터럴 경로(/bookmarks)를 경로 변수(/{id})보다 우선합니다.
     * 따라서 "bookmarks"가 ID로 오인되지 않습니다.
     *
     * @return 200 OK + 북마크된 노트 배열
     */
    @GetMapping("/bookmarks")
    fun getBookmarked(
        @RequestParam(defaultValue = "recent") sort: String,
        authentication: Authentication? = null,
    ): ResponseEntity<List<NoteResponse>> {
        val result = getNoteListPageUseCase.get(
            GetNoteListPageUseCase.Command(
                ownerUsername = currentUsername(authentication),
                keyword = null,
                bookmarkedOnly = true,
                sort = sort,
            ),
        )
        return ResponseEntity.ok(result.notes.map { it.toResponse() })
    }

    /**
     * 특정 노트에 북마크를 등록합니다.
     *
     * HTTP 메서드: POST
     * 경로: /api/v1/notes/{id}/bookmark
     *
     * 왜 POST인가?
     * - "북마크 자원을 생성"하는 의미로 POST를 씁니다.
     * - 이미 북마크된 상태에서 다시 호출해도 같은 결과 (멱등)
     *
     * @param id 북마크할 노트의 ID
     * @return 200 OK + 북마크 적용된 노트, 또는 404 Not Found
     */
    @PostMapping("/{id}/bookmark")
    fun bookmark(@PathVariable id: String): ResponseEntity<NoteResponse> {
        val note = bookmarkNoteUseCase.bookmark(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(note.toResponse())
    }

    /**
     * 특정 노트의 북마크를 해제합니다.
     *
     * HTTP 메서드: DELETE
     * 경로: /api/v1/notes/{id}/bookmark
     *
     * @param id 북마크를 해제할 노트의 ID
     * @return 200 OK + 북마크 해제된 노트, 또는 404 Not Found
     */
    @DeleteMapping("/{id}/bookmark")
    fun unbookmark(@PathVariable id: String): ResponseEntity<NoteResponse> {
        val note = bookmarkNoteUseCase.unbookmark(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(note.toResponse())
    }

    private fun currentUsername(authentication: Authentication?): String {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication
        val isAuthenticated = auth != null && auth.isAuthenticated && auth !is AnonymousAuthenticationToken
        return if (isAuthenticated) auth.name else "anonymousUser"
    }
}

/**
 * 노트 생성 요청 DTO입니다.
 *
 * DTO (Data Transfer Object)란?
 * - API 계약을 정의하는 데이터 구조
 * - 도메인 엔티티(Note)와 분리해 API 변경이 도메인에 영향 없게 함
 * - 입력 검증(@Valid, @NotBlank) 담당
 *
 * @param title 제목 (필수)
 * @param content 본문 (필수)
 * @param visibility 공개범위 (기본: PRIVATE)
 * @param tags 태그 (기본: 빈 집합)
 */
data class CreateNoteRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:NotBlank(message = "본문은 필수입니다")
    val content: String,
    val visibility: Visibility = Visibility.PRIVATE,
    val tags: Set<String> = emptySet(),
)

/**
 * 노트 수정 요청 DTO입니다.
 */
data class UpdateNoteRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,
    @field:NotBlank(message = "본문은 필수입니다")
    val content: String,
    val visibility: Visibility,
    val tags: Set<String> = emptySet(),
)

/**
 * 노트 조회/생성 응답 DTO입니다.
 *
 * 특징:
 * - 도메인 Note와 비슷하지만 완전히 분리된 구조
 * - JSON 직렬화로 API 클라이언트에 전송
 * - 필요한 필드만 포함 (예: updatedAt은 제외 가능)
 *
 * @param id 노트 고유 ID
 * @param title 제목
 * @param content 본문
 * @param visibility 공개범위
 * @param tags 태그
 */
data class NoteResponse(
    val id: String,
    val title: String,
    val content: String,
    val visibility: Visibility,
    val tags: Set<String>,
    val bookmarked: Boolean,  // 북마크 여부
)

/**
 * 도메인 엔티티(Note)를 응답 DTO로 변환하는 확장 함수입니다.
 *
 * 이렇게 하는 이유:
 * - 계층 간 변환 로직을 한 곳에 집중
 * - 도메인과 API 응답의 구조가 다를 때 유연함
 * - 예: 나중에 Note에 password 필드가 추가되어도 응답에는 안 넣을 수 있음
 */
private fun Note.toResponse(): NoteResponse = NoteResponse(
    id = id,
    title = title,
    content = content,
    visibility = visibility,
    tags = tags,
    bookmarked = bookmarked,
)








