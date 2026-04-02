# 지금까지 한 작업 요약

## 전체 흐름도

```
HTTP 요청
  ↓
NoteController (어댑터 - 요청/응답 변환)
  ↓
CreateNoteUseCase / GetNoteUseCase (포트 - 계약)
  ↓
CreateNoteService / GetNoteService (구현 - 비즈니스 흐름)
  ↓
Note.create() (도메인 - 검증)
  ↓
NoteCommandPort / NoteQueryPort (포트 - 저장소 계약)
  ↓
InMemoryNoteRepository (어댑터 - 저장소 구현)
  ↓
메모리 저장
```

---

## 각 계층의 역할

### 1. Domain 계층 (`domain/note`)
- **책임**: 순수한 비즈니스 규칙과 검증
- **특징**: 
  - Spring 의존성 없음
  - 불변(immutable) 객체
  - 공장 메서드(`Note.create`)로 유효성 검증 강제
- **파일**:
  - `Visibility.kt`: PUBLIC/PRIVATE 열거형
  - `Note.kt`: 노트 엔티티 + 생성 검증

### 2. Application 계층 (`application/port`, `application/service`)
- **책임**: 비즈니스 유스케이스 정의와 조율
- **특징**:
  - 포트 인터페이스로 의존성 역전 (의존성 주입)
  - `port.in`: 외부가 호출할 계약
  - `port.out`: 외부 리소스에 의존할 계약
- **파일**:
  - `CreateNoteUseCase.kt`: 노트 생성 계약
  - `GetNoteUseCase.kt`: 노트 조회 계약
  - `NoteCommandPort.kt`: 저장 포트
  - `NoteQueryPort.kt`: 조회 포트
  - `CreateNoteService.kt`: 생성 구현
  - `GetNoteService.kt`: 조회 구현

### 3. Adapter 계층 (`adapter/in/web`, `adapter/out/persistence`)
- **책임**: 외부와의 통신 (HTTP, DB 등)
- **특징**:
  - 프로토콜별로 분리 (REST, GraphQL 등 추가 가능)
  - 도메인/유스케이스와 격리
  - 변경 영향이 내부까지 미치지 않음
- **파일**:
  - `NoteController.kt`: HTTP 요청 처리
  - `InMemoryNoteRepository.kt`: 임시 저장소 구현

### 4. Infrastructure 계층 (`infrastructure/config`)
- **책임**: 기술적 구성 (설정, 라이브러리 통합)
- **특징**:
  - Spring 설정, 보안, 예외 처리 등
- **파일**:
  - `ClockConfig.kt`: 시간 공급자 설정

---

## 테스트 전략

### 1. 단위 테스트 (`*ServiceTest`)
- 한 클래스와 그 의존성(mock)만 테스트
- 빠른 실행 (DB 없음)
- 비즈니스 규칙 검증
- 예: `CreateNoteServiceTest`

### 2. 통합 테스트 (`*ControllerTest`)
- HTTP 요청 → 컨트롤러 → 유스케이스(mock)의 흐름
- MockMvc로 서버 없이 HTTP 시뮬레이션
- API 계약(201, 404 등) 검증
- 예: `NoteControllerTest`

---

## Clean Architecture의 핵심 원리

### 1. 의존성 역전 (Dependency Inversion)
```
❌ 잘못된 방식:
컨트롤러 → 서비스 → JPA Repository
(외부 구현에 의존)

✅ 올바른 방식:
컨트롤러 → CreateNoteUseCase (포트)
서비스 ← CreateNoteUseCase (포트)
서비스 → NoteCommandPort (포트)
JpaRepository ← NoteCommandPort (포트)
(추상화에 의존)
```

### 2. 계층 독립성
- 각 계층이 독립적으로 테스트 가능
- 하위 계층 변경이 상위 계층에 영향 없음
- 예: DB를 JPA에서 MongoDB로 바꿔도 서비스/컨트롤러는 불변

### 3. 명확한 책임 분리
- 도메인: "이게 유효한가?" 검증만
- 서비스: "어떤 순서로 할 것인가?" 조율만
- 컨트롤러: "HTTP 요청을 어떻게 변환할 것인가?" 변환만
- 저장소: "어떻게 저장할 것인가?" 구현만

---

## MVP V1 진행 상황

| 작업 | 상태 | 비고 |
|---|---|---|
| 패키지 구조 | ✅ 완료 | domain/application/adapter/infrastructure |
| 노트 CRUD - 생성 | ✅ 완료 | POST /api/v1/notes → 201 |
| 노트 CRUD - 조회 | ✅ 완료 | GET /api/v1/notes/{id} → 200/404 |
| 공개/비공개 도메인 규칙 | ✅ 완료 | Visibility enum 강제 |
| 문서 & 테스트 | ✅ 완료 | 모든 테스트 통과 |
| 수정/삭제 | ⏳ 다음 | PUT/DELETE API |
| 검색 | ⏳ 다음 | SearchNotesUseCase + 페이징 |
| 북마크 | ⏳ 다음 | BookmarkUseCase |
| 요약 | ⏳ 다음 | SummarizeNoteUseCase |

---

## 다음 우선순위

1. **수정/삭제 API 추가**
   - `UPDATE /api/v1/notes/{id}`
   - `DELETE /api/v1/notes/{id}`

2. **검색 기능 추가**
   - `SearchNotesUseCase`
   - 제목/본문/태그 키워드 검색
   - 페이징 지원

3. **북마크 기능 추가**
   - `ToggleBookmarkUseCase`
   - 북마크 목록 조회

4. **JPA 저장소로 마이그레이션**
   - `InMemoryNoteRepository` → `JpaNoteRepository`
   - PostgreSQL 연동
   - 테스트는 그대로 통과

---

## 포트폴리오 관점

이 구조로 만든 이유:
- **포트폴리오에 보여주고 싶은 포인트**:
  1. Clean Architecture 이해와 실제 적용
  2. SOLID 원칙 준수 (특히 DIP - Dependency Inversion Principle)
  3. TDD 기반 개발 프로세스
  4. 테스트 가능한 설계

- **면접에서 설명할 수 있는 포인트**:
  1. "왜 포트를 만들었나?": 의존성 역전으로 DB 바꾸기 쉽게
  2. "왜 계층을 나눴나?": 각 계층의 책임이 명확하고 테스트 가능
  3. "왜 mock 저장소부터?": 빠른 기능 검증, 나중에 DB 추가 가능
  4. "테스트를 먼저 쓴 이유?": 요구사항 명확화, 회귀 방지

