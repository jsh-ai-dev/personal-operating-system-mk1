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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.security.test.context.support.WithMockUser
import org.hamcrest.Matchers.containsString
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.BDDMockito.given
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.text.Charsets

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
@AutoConfigureMockMvc(addFilters = false)
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
    lateinit var getNoteListPageUseCase: GetNoteListPageUseCase

    @MockBean
    lateinit var updateNoteUseCase: UpdateNoteUseCase

    // 북마크 관련 Mock 유스케이스
    @MockBean
    lateinit var bookmarkNoteUseCase: BookmarkNoteUseCase

    @MockBean
    lateinit var summarizeUseCase: SummarizeUseCase

    @MockBean
    lateinit var saveNoteSummaryUseCase: SaveNoteSummaryUseCase

            @Test
            fun `GET note list returns notes`() {
                        given(
                            getNoteListPageUseCase.get(
                                GetNoteListPageUseCase.Command(
                                    ownerUsername = "anonymousUser",
                                    keyword = null,
                                    bookmarkedOnly = false,
                                    sort = "recent",
                                ),
                            ),
                        ).willReturn(
                    GetNoteListPageUseCase.Result(
                        notes = listOf(
                            Note(
                                id = "note-list-1",
                                title = "list title",
                                content = "list content",
                                visibility = Visibility.PRIVATE,
                                tags = setOf("list"),
                                createdAt = Instant.now(),
                                updatedAt = Instant.now(),
                            ),
                        ),
                        keyword = "",
                        bookmarkedOnly = false,
                        sort = "recent",
                        page = 0,
                        size = 20,
                        totalElements = 1,
                        totalPages = 1,
                    ),
                )

                mockMvc.perform(get("/api/v1/notes"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$[0].id").value("note-list-1"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Page", "0"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Size", "20"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Total-Elements", "1"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Total-Pages", "1"))
            }

    @Test
    fun `GET note list forwards page and size params`() {
        given(
            getNoteListPageUseCase.get(
                GetNoteListPageUseCase.Command(
                    ownerUsername = "anonymousUser",
                    keyword = null,
                    bookmarkedOnly = false,
                    sort = "recent",
                    page = 2,
                    size = 5,
                ),
            ),
        ).willReturn(
            GetNoteListPageUseCase.Result(
                notes = emptyList(),
                keyword = "",
                bookmarkedOnly = false,
                sort = "recent",
                page = 2,
                size = 5,
                totalElements = 12,
                totalPages = 3,
                hasPrevious = true,
            ),
        )

        mockMvc.perform(get("/api/v1/notes").param("page", "2").param("size", "5"))
            .andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Page", "2"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Size", "5"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Has-Previous", "true"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Has-Next", "false"))
    }

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
            ownerUsername = "anonymousUser",
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

    @Test
    @WithMockUser(username = "alice")
    fun `POST note uses authenticated username as owner`() {
        val command = CreateNoteUseCase.Command(
            ownerUsername = "alice",
            title = "title",
            content = "content",
            visibility = Visibility.PRIVATE,
            tags = setOf("spring"),
        )

        given(createNoteUseCase.create(command)).willReturn(
            Note(
                id = "note-auth-1",
                title = "title",
                content = "content",
                visibility = Visibility.PRIVATE,
                tags = setOf("spring"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/api/v1/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "title",
                      "content": "content",
                      "visibility": "PRIVATE",
                      "tags": ["spring"]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("note-auth-1"))
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

    @Test
    @WithMockUser(username = "alice")
    fun `GET note returns 404 when owner does not match`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "bob",
                title = "title",
                content = "content",
                visibility = Visibility.PUBLIC,
                tags = setOf("kotlin"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(get("/api/v1/notes/note-1"))
            .andExpect(status().isNotFound)
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

        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "anonymousUser",
                title = "before",
                content = "before",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
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

        given(getNoteUseCase.getById("missing-note")).willReturn(null)

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

    @Test
    @WithMockUser(username = "alice")
    fun `PUT note returns 404 when owner does not match`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "bob",
                title = "before",
                content = "before",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
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
            .andExpect(status().isNotFound)
    }

    /**
     * 테스트: DELETE /api/v1/notes/{id} 성공 시 204 No Content를 반환하는가?
     */
    @Test
    fun `DELETE note returns 204 when deleted`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "anonymousUser",
                title = "title",
                content = "content",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        given(deleteNoteUseCase.deleteById("note-1")).willReturn(true)

        mockMvc.perform(delete("/api/v1/notes/note-1"))
            .andExpect(status().isNoContent)
    }

    /**
     * 테스트: DELETE /api/v1/notes/{id} 실패(대상 없음) 시 404를 반환하는가?
     */
    @Test
    fun `DELETE note returns 404 when not found`() {
        given(getNoteUseCase.getById("missing-note")).willReturn(null)

        mockMvc.perform(delete("/api/v1/notes/missing-note"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "alice")
    fun `DELETE note returns 404 when owner does not match`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "bob",
                title = "title",
                content = "content",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(delete("/api/v1/notes/note-1"))
            .andExpect(status().isNotFound)
    }

    /**
     * 테스트: GET /api/v1/notes/search?keyword=... 호출 시 검색 결과를 반환하는가?
     */
    @Test
    fun `GET search returns matched notes`() {
        given(
            getNoteListPageUseCase.get(
                GetNoteListPageUseCase.Command(
                    ownerUsername = "anonymousUser",
                    keyword = "kotlin",
                    bookmarkedOnly = false,
                    sort = "recent",
                ),
            ),
        ).willReturn(
            GetNoteListPageUseCase.Result(
                notes = listOf(
                    Note(
                        id = "note-1",
                        title = "kotlin memo",
                        content = "clean architecture",
                        visibility = Visibility.PUBLIC,
                        tags = setOf("kotlin"),
                        bookmarked = false,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    ),
                ),
                keyword = "kotlin",
                bookmarkedOnly = false,
                sort = "recent",
            ),
        )

        mockMvc.perform(get("/api/v1/notes/search").param("keyword", "kotlin"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("note-1"))
            .andExpect(jsonPath("$[0].title").value("kotlin memo"))
    }

    /**
     * 테스트: 빈 검색어가 들어오면 500이 아니라 400 Bad Request로 응답하는가?
     */
    @Test
    fun `GET search returns 400 when keyword is blank`() {
        mockMvc.perform(get("/api/v1/notes/search").param("keyword", "   "))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("검색어는 비워둘 수 없습니다"))
            .andExpect(jsonPath("$.path").value("/api/v1/notes/search"))
    }

    /**
     * 테스트: GET /api/v1/notes/bookmarks 호출 시 북마크된 노트 목록을 반환하는가?
     */
    @Test
    fun `GET bookmarks returns bookmarked notes`() {
        given(
            getNoteListPageUseCase.get(
                GetNoteListPageUseCase.Command(
                    ownerUsername = "anonymousUser",
                    keyword = null,
                    bookmarkedOnly = true,
                    sort = "recent",
                ),
            ),
        ).willReturn(
            GetNoteListPageUseCase.Result(
                notes = listOf(
                    Note(
                        id = "note-bm-1",
                        title = "북마크된 메모",
                        content = "자주 보는 내용",
                        visibility = Visibility.PRIVATE,
                        tags = setOf("중요"),
                        bookmarked = true,
                        createdAt = Instant.now(),
                        updatedAt = Instant.now(),
                    ),
                ),
                keyword = "",
                bookmarkedOnly = true,
                sort = "recent",
            ),
        )

        mockMvc.perform(get("/api/v1/notes/bookmarks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value("note-bm-1"))
            .andExpect(jsonPath("$[0].bookmarked").value(true))
    }

    /**
     * 테스트: POST /api/v1/notes/{id}/bookmark 성공 시 200 OK + bookmarked=true를 반환하는가?
     */
    @Test
    fun `POST bookmark returns 200 with bookmarked true`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "anonymousUser",
                title = "테스트 메모",
                content = "내용",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        given(bookmarkNoteUseCase.bookmark("note-1")).willReturn(
            Note(
                id = "note-1",
                title = "테스트 메모",
                content = "내용",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                bookmarked = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(post("/api/v1/notes/note-1/bookmark"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("note-1"))
            .andExpect(jsonPath("$.bookmarked").value(true))
    }

    /**
     * 테스트: 없는 노트에 북마크하면 404를 반환하는가?
     */
    @Test
    fun `POST bookmark returns 404 when note not found`() {
        given(getNoteUseCase.getById("missing")).willReturn(null)

        mockMvc.perform(post("/api/v1/notes/missing/bookmark"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "alice")
    fun `POST bookmark returns 404 when owner does not match`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "bob",
                title = "테스트 메모",
                content = "내용",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(post("/api/v1/notes/note-1/bookmark"))
            .andExpect(status().isNotFound)
    }

    /**
     * 테스트: DELETE /api/v1/notes/{id}/bookmark 성공 시 200 OK + bookmarked=false를 반환하는가?
     */
    @Test
    fun `DELETE bookmark returns 200 with bookmarked false`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "anonymousUser",
                title = "테스트 메모",
                content = "내용",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        given(bookmarkNoteUseCase.unbookmark("note-1")).willReturn(
            Note(
                id = "note-1",
                title = "테스트 메모",
                content = "내용",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                bookmarked = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(delete("/api/v1/notes/note-1/bookmark"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bookmarked").value(false))
    }

    /**
     * 테스트: 없는 노트의 북마크를 해제하면 404를 반환하는가?
     */
    @Test
    fun `DELETE bookmark returns 404 when note not found`() {
        given(getNoteUseCase.getById("missing")).willReturn(null)

        mockMvc.perform(delete("/api/v1/notes/missing/bookmark"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(username = "alice")
    fun `DELETE bookmark returns 404 when owner does not match`() {
        given(getNoteUseCase.getById("note-1")).willReturn(
            Note(
                id = "note-1",
                ownerUsername = "bob",
                title = "테스트 메모",
                content = "내용",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(delete("/api/v1/notes/note-1/bookmark"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST upload returns 201 for txt file`() {
        val command = CreateNoteUseCase.Command(
            ownerUsername = "anonymousUser",
            title = "hello",
            content = "hi",
            visibility = Visibility.PRIVATE,
            tags = emptySet(),
            originalFileName = "hello.txt",
        )
        given(createNoteUseCase.create(command)).willReturn(
            Note(
                id = "note-up-1",
                ownerUsername = "anonymousUser",
                title = "hello",
                content = "hi",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                originalFileName = "hello.txt",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        val file = MockMultipartFile("file", "hello.txt", "text/plain", "hi".toByteArray(Charsets.UTF_8))

        mockMvc.perform(multipart("/api/v1/notes/upload").file(file))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("note-up-1"))
            .andExpect(jsonPath("$.originalFileName").value("hello.txt"))
            .andExpect(jsonPath("$.hasStoredFile").value(false))
    }

    @Test
    fun `GET download returns utf8 text for note without stored file`() {
        given(getNoteUseCase.getById("t1")).willReturn(
            Note(
                id = "t1",
                ownerUsername = "anonymousUser",
                title = "제목",
                content = "본문",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                originalFileName = "a.txt",
                hasStoredFile = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(get("/api/v1/notes/t1/download"))
            .andExpect(status().isOk)
            .andExpect(content().bytes("본문".toByteArray(StandardCharsets.UTF_8)))
    }

    @Test
    fun `GET download with inline uses inline content disposition`() {
        given(getNoteUseCase.getById("t1")).willReturn(
            Note(
                id = "t1",
                ownerUsername = "anonymousUser",
                title = "제목",
                content = "본문",
                visibility = Visibility.PRIVATE,
                tags = emptySet(),
                originalFileName = "a.txt",
                hasStoredFile = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

        mockMvc.perform(get("/api/v1/notes/t1/download").param("inline", "true"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
    }
}












