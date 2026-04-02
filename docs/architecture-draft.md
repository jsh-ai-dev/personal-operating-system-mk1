# 아키텍처 초안

## 계층 구조
기본 패키지: `com.jsh.pos`

- `domain`
  - 순수한 비즈니스 규칙과 엔티티만 포함
  - Spring 프레임워크 의존성 금지
  - 다른 계층으로부터 독립적
- `application`
  - 유스케이스와 포트(port.in, port.out) 정의
  - 도메인 로직 조립
  - 프레임워크와 무관한 비즈니스 흐름
- `adapter.in`
  - REST 컨트롤러 등 인바운드 어댑터
  - 외부 요청을 유스케이스로 변환
- `adapter.out`
  - JPA 저장소, Redis 캐시 등 아웃바운드 어댑터
  - 포트의 실제 구현 (DB, 외부 API 등)
- `infrastructure`
  - Spring 설정, 예외 처리, 보안 설정 등 기술적 관심사
  - 다른 계층에서 필요로 하는 인프라 지원

## 초기 도메인 모델
- `Note` (불변 데이터 객체)
  - `id`, `title`, `content`, `visibility`, `tags`, `createdAt`, `updatedAt`
- `Visibility` (열거형)
  - `PUBLIC`, `PRIVATE`

## 초기 유스케이스
- `CreateNoteUseCase` (노트 생성)
- `GetNoteUseCase` (노트 조회)
- `SearchNotesUseCase` (검색) - 다음 스프린트
- `ToggleBookmarkUseCase` (북마크) - 다음 스프린트
- `SummarizeNoteUseCase` (요약) - 다음 스프린트

## 초기 기술 결정사항
- 초기 구현은 인메모리 저장소로 시작, 나중에 JPA로 마이그레이션
- API 경로는 `/api/v1` 기준
- 요청/응답 DTO는 인바운드 어댑터 계층에서만 관리
- API 계약은 테스트(MockMvc)로 검증
- 유스케이스는 단위 테스트로 검증

