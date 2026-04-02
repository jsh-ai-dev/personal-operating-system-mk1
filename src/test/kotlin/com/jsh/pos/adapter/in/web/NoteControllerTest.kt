package com.jsh.pos.adapter.`in`.web

import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.`in`.DeleteNoteUseCase
import com.jsh.pos.application.port.`in`.GetNoteUseCase
import com.jsh.pos.application.port.`in`.UpdateNoteUseCase
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.BDDMockito.given
import java.time.Instant

/**
 * NoteController의 통합 테스트입니다.
 *
 * 통합 테스트와 단위 테스트의 차이:
 * - 단위 테스트: 한 클래스의 비즈니스 규칙 검증 (CreateNoteServiceTest 참조)
 * - 통합 테스트: HTTP 요청 -> 컨트롤러 -> 유스케이스의 전체 흐름 검증 (이 테스트)
 *
 * @WebMvcTest 사용:
 * - NoteController만 테스트 범위에 포함
 * - 의존성(유스케이스)은 mock으로 대체
 * - @MockBean: Spring이 mock 객체로 등록해 DI 가능하게 함
 *
 * MockMvc:
 * - 서버 시작 없이 HTTP 요청을 시뮬레이션
 * - .perform(): HTTP 요청 실행
 * - .andExpect(): 응답 검증
 */
@WebMvcTest(NoteController::class)
class NoteControllerTest {

    // Spring이 관리하는 MockMvc 인스턴스
    // HTTP 요청을 만들고 응답을 검증할 도구
    @Autowired
    lateinit var mockMvc: MockMvc

    // Mock 유스케이스 (실제 구현 대신 사용)
    // given()으로 동작을 설정 가능
    @MockBean
    lateinit var createNoteUseCase: CreateNoteUseCase

    @MockBean
    lateinit var getNoteUseCase: GetNoteUseCase

    @MockBean
    lateinit var deleteNoteUseCase: DeleteNoteUseCase

    @MockBean
    lateinit var updateNoteUseCase: UpdateNoteUseCase

    /**
     * 테스트: POST /api/v1/notes로 노트를 생성하면 201 Created를 반환하는가?
     *
     * 검증 항목:
     * 1. HTTP 상태 코드가 201 (Created)인가?
     * 2. 응답 JSON에 ID, 제목이 포함되는가?
     *
     * 요청 흐름:
     * JSON 요청 -> 컨트롤러 -> 유스케이스(mock) -> JSON 응답
     */
    @Test
    fun `POST note returns 201`() {
        // Arrange (준비): 유스케이스의 동작 설정
        // "어떤 Command가 들어오면 이 Note를 반환해"라고 mock에 지시
        val command = CreateNoteUseCase.Command(
            title = "title",
            content = "content",
            visibility = Visibility.PRIVATE,
            tags = setOf("spring"),
        )

        given(createNoteUseCase.create(command)).willReturn(Note(
            id = "note-1",
            title = "title",
            content = "content",
            visibility = Visibility.PRIVATE,
            tags = setOf("spring"),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ))

        // Act & Assert (수행 및 검증)
        mockMvc.perform(
            post("/api/v1/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "title",
                      "content": "content",
                      "visibility": "PRIVATE",
                      "tags": ["spring"]
                    }
                """.trimIndent()),
        )
            // 상태 코드 검증: 201 Created
            .andExpect(status().isCreated)
            // 응답 JSON 검증: $.id = "note-1"
            .andExpect(jsonPath("$.id").value("note-1"))
            // 응답 JSON 검증: $.title = "title"
            .andExpect(jsonPath("$.title").value("title"))
    }

    /**
     * 테스트: GET /api/v1/notes/{id}로 노트를 조회하면 200 OK를 반환하는가?
     *
     * 검증 항목:
     * 1. HTTP 상태 코드가 200 (OK)인가?
     * 2. 응답 JSON의 visibility가 PUBLIC인가?
     */
    @Test
    fun `GET note returns 200 when found`() {
        // Arrange (준비): 유스케이스의 동작 설정
        // "note-1을 조회하면 이 Note를 반환해"
        given(getNoteUseCase.getById("note-1")).willReturn(Note(
            id = "note-1",
            title = "title",
            content = "content",
            visibility = Visibility.PUBLIC,      // PUBLIC으로 설정해 응답 검증
            tags = setOf("kotlin"),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ))

        // Act & Assert (수행 및 검증)
        mockMvc.perform(
            get("/api/v1/notes/note-1")  // 경로 변수 전달
        )
            // 상태 코드 검증: 200 OK
            .andExpect(status().isOk)
            // 응답 JSON 검증: $.visibility = "PUBLIC"
            .andExpect(jsonPath("$.visibility").value("PUBLIC"))
    }

    /**
     * 테스트: PUT /api/v1/notes/{id} 성공 시 200 OK를 반환하는가?
     */
    @Test
    fun `PUT note returns 200 when updated`() {
        val command = UpdateNoteUseCase.Command(
            title = "updated title",
            content = "updated content",
            visibility = Visibility.PUBLIC,
            tags = setOf("updated", "kotlin"),
        )

        given(updateNoteUseCase.updateById("note-1", command)).willReturn(
            Note(
                id = "note-1",
                title = "updated title",
                content = "updated content",
                visibility = Visibility.PUBLIC,
                tags = setOf("updated", "kotlin"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            put("/api/v1/notes/note-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "updated title",
                      "content": "updated content",
                      "visibility": "PUBLIC",
                      "tags": ["updated", "kotlin"]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("updated title"))
            .andExpect(jsonPath("$.visibility").value("PUBLIC"))
    }

    /**
     * 테스트: PUT /api/v1/notes/{id} 실패(대상 없음) 시 404를 반환하는가?
     */
    @Test
    fun `PUT note returns 404 when target not found`() {
        val command = UpdateNoteUseCase.Command(
            title = "updated title",
            content = "updated content",
            visibility = Visibility.PUBLIC,
            tags = setOf("updated", "kotlin"),
        )

        given(updateNoteUseCase.updateById("missing-note", command)).willReturn(null)

        mockMvc.perform(
            put("/api/v1/notes/missing-note")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "updated title",
                      "content": "updated content",
                      "visibility": "PUBLIC",
                      "tags": ["updated", "kotlin"]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isNotFound)
    }

    /**
     * 테스트: DELETE /api/v1/notes/{id} 성공 시 204 No Content를 반환하는가?
     */
    @Test
    fun `DELETE note returns 204 when deleted`() {
        given(deleteNoteUseCase.deleteById("note-1")).willReturn(true)

        mockMvc.perform(delete("/api/v1/notes/note-1"))
            .andExpect(status().isNoContent)
    }

    /**
     * 테스트: DELETE /api/v1/notes/{id} 실패(대상 없음) 시 404를 반환하는가?
     */
    @Test
    fun `DELETE note returns 404 when not found`() {
        given(deleteNoteUseCase.deleteById("missing-note")).willReturn(false)

        mockMvc.perform(delete("/api/v1/notes/missing-note"))
            .andExpect(status().isNotFound)
    }
}








