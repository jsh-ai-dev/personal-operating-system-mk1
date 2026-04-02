package com.jsh.pos.application.service

import com.jsh.pos.application.port.`in`.CreateNoteUseCase
import com.jsh.pos.application.port.out.NoteCommandPort
import com.jsh.pos.domain.note.Note
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.UUID

/**
 * 노트 생성 유스케이스의 구현체입니다.
 *
 * 역할:
 * - CreateNoteUseCase 인터페이스를 구현해 비즈니스 흐름 정의
 * - UUID 생성, 도메인 엔티티 생성, 저장소 위임을 조율
 *
 * 왜 이렇게 설계했나?
 * - 포트(인터페이스) 구현으로 test할 때 저장소를 mock으로 대체 가능
 * - @Service로 Spring에 등록되어 DI로 주입됨
 * - Clock 주입으로 테스트에서 시간을 고정할 수 있음 (테스트 안정성)
 *
 * 관심사 분리:
 * - 이 서비스는 "조율"만 함 (실제 검증은 도메인, 저장은 포트)
 * - 복잡한 비즈니스 규칙이 여기 있으면 테스트하기 어려워짐
 */
@Service
class CreateNoteService(
    // port.out 주입: 저장 로직 (구현체는 adapter/out에서 제공)
    private val noteCommandPort: NoteCommandPort,
    // Clock 주입: 현재 시간 (테스트에서 고정 가능)
    private val clock: Clock,
) : CreateNoteUseCase {

    override fun create(command: CreateNoteUseCase.Command): Note {
        // 1. 도메인 엔티티 생성 (검증 수행)
        val note = Note.create(
            id = UUID.randomUUID().toString(), // 새 ID 생성 (Java 표준)
            title = command.title,
            content = command.content,
            visibility = command.visibility,
            tags = command.tags,
            now = clock.instant(),            // 현재 시간 (Clock 의존)
        )

        // 2. 저장소를 통해 영속화 (포트 위임)
        // 저장소 구현이 무엇인지(JPA, Redis 등)는 알 필요 없음
        return noteCommandPort.save(note)
    }
}


