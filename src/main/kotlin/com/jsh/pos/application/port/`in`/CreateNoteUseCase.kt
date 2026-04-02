package com.jsh.pos.application.port.`in`

import com.jsh.pos.domain.note.Note
import com.jsh.pos.domain.note.Visibility

/**
 * 노트 생성 유스케이스의 계약(Contract)입니다.
 *
 * 포트란?
 * - Clean Architecture에서 "어댑터"와 "비즈니스 로직"의 경계선
 * - 외부가 사용 가능한 기능을 인터페이스로 정의
 * - 구현체는 application/service에서 제공
 *
 * port.in의 의미:
 * - 애플리케이션으로 들어오는 요청의 진입점
 * - REST 컨트롤러는 이 포트를 호출해서 비즈니스 로직 실행
 * - 여러 진입점(REST API, 배치, 이벤트)이 모두 이 포트를 사용 가능
 */
interface CreateNoteUseCase {
    /**
     * 노트를 생성합니다.
     *
     * @param command 노트 생성 명령 (제목, 본문, 공개범위, 태그)
     * @return 생성된 노트 (ID, 타임스탬프 포함)
     */
    fun create(command: Command): Note

    /**
     * 노트 생성에 필요한 데이터를 담는 명령 객체입니다.
     *
     * 데이터 클래스 사용 이유:
     * - 불변 객체로 실수로 인한 변경 방지
     * - equals/hashCode/toString 자동 생성
     * - 테스트 시 데이터 조립이 간편
     *
     * @param title 노트 제목
     * @param content 노트 본문
     * @param visibility 공개/비공개 (기본값: PRIVATE)
     * @param tags 태그들 (기본값: 빈 집합)
     */
    data class Command(
        val title: String,
        val content: String,
        val visibility: Visibility = Visibility.PRIVATE,
        val tags: Set<String> = emptySet(),
    )
}


