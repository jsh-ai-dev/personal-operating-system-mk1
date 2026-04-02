package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
 * - PUT 200 OK: 수정 성공
 * - PUT 404 Not Found: 수정 대상 없음
 * - DELETE 204 No Content: 삭제 성공
 * - DELETE 404 Not Found: 삭제 대상 없음
 */
@RestController
@RequestMapping("/api/v1/notes")
class NoteController(
    // port.in 주입: 노트 생성 유스케이스
    private val createNoteUseCase: CreateNoteUseCase,
    // port.in 주입: 노트 조회 유스케이스
    private val getNoteUseCase: GetNoteUseCase,
    // port.in 주입: 노트 수정 유스케이스
    private val updateNoteUseCase: UpdateNoteUseCase,
    // port.in 주입: 노트 삭제 유스케이스
    private val deleteNoteUseCase: DeleteNoteUseCase,
) {

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
        // 1. 유스케이스 호출해 노트 조회
        val note = getNoteUseCase.getById(id)

        // 2. 없으면 404 반환
        ?: return ResponseEntity.notFound().build()

        // 3. 있으면 200 OK로 응답 DTO 반환
        return ResponseEntity.ok(note.toResponse())
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
        val updated = updateNoteUseCase.updateById(
            id = id,
            command = UpdateNoteUseCase.Command(
                title = request.title,
                content = request.content,
                visibility = request.visibility,
                tags = request.tags,
            ),
        ) ?: return ResponseEntity.notFound().build()

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
        val deleted = deleteNoteUseCase.deleteById(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
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
)






