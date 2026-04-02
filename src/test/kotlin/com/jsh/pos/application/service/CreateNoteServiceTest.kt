package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * CreateNoteService의 단위 테스트입니다.
 *
 * 단위 테스트의 특징:
 * - 한 클래스만 테스트 (의존성은 mock 사용)
 * - 빠른 실행 (DB 없음)
 * - 비즈니스 규칙을 검증
 *
 * TDD (Test-Driven Development) 관점:
 * - 테스트를 먼저 작성해 "무엇을 해야 하는가"를 명확히 함
 * - 그 다음 구현으로 테스트 통과
 * - 리팩토링할 때도 테스트가 회귀 방지
 */
class CreateNoteServiceTest {

    // 고정된 시간: 모든 테스트가 같은 시간으로 노트 생성
    // 이렇게 하면 "createdAt이 특정 시간"을 검증 가능
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-04-02T00:00:00Z"),
        ZoneOffset.UTC
    )

    // Mock 저장소: 실제 DB 없이 메모리에만 저장
    private val noteCommandPort = InMemoryNoteCommandPort()

    // 테스트 대상: CreateNoteService (의존성은 위에서 주입)
    private val createNoteService = CreateNoteService(noteCommandPort, fixedClock)

    /**
     * 테스트: 노트 생성 시 필드가 올바르게 정제되고 저장되는가?
     *
     * 검증 항목:
     * 1. 제목 공백이 제거되는가?
     * 2. 본문 공백이 제거되는가?
     * 3. 태그가 정제되고 중복이 제거되는가? (empty 태그 제외)
     */
    @Test
    fun `create note trims fields and persists note`() {
        // Arrange (준비): 테스트 데이터
        val created = createNoteService.create(
            CreateNoteUseCase.Command(
                title = "  Kotlin note  ",      // 앞뒤 공백
                content = "  clean architecture  ", // 앞뒤 공백
                visibility = Visibility.PRIVATE,
                tags = setOf(" kotlin ", " ", "spring"),  // 공백 있고, 빈 문자열 포함
            ),
        )

        // Act & Assert (수행 및 검증)
        // 실제로는 Arrange와 Act가 한 줄에 되어 있음

        // 공백 제거 확인
        assertEquals("Kotlin note", created.title)
        assertEquals("clean architecture", created.content)

        // 태그 정제 확인: " kotlin " -> "kotlin", " " 제외, "spring" 유지, 중복 제외
        assertEquals(setOf("kotlin", "spring"), created.tags)
    }

    /**
     * 테스트: 제목이 공백이면 예외가 발생하는가?
     *
     * 이 테스트는 도메인의 검증 규칙을 확인합니다.
     * 도메인 엔티티(Note.create)에서 검증하므로 여기서도 테스트합니다.
     */
    @Test
    fun `create note throws when title is blank`() {
        // 제목이 공백인 경우 IllegalArgumentException 발생 확인
        assertThrows(IllegalArgumentException::class.java) {
            createNoteService.create(
                CreateNoteUseCase.Command(
                    title = "   ",  // 공백만 있음
                    content = "content",
                    visibility = Visibility.PUBLIC,
                ),
            )
        }
    }

    /**
     * 테스트용 저장소 구현입니다.
     *
     * NoteCommandPort의 mock 구현:
     * - 실제 DB 대신 메모리 맵 사용
     * - 저장된 노트를 그대로 반환
     */
    private class InMemoryNoteCommandPort : NoteCommandPort {
        private val store = mutableMapOf<String, Note>()

        override fun save(note: Note): Note {
            store[note.id] = note
            return note
        }

        override fun deleteById(id: String): Boolean = store.remove(id) != null
    }
}



