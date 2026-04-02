# personal-operating-system

AI 기반의 개인용 지식 관리 및 업무 자동화 시스템입니다.

## 프로젝트 방향

이 프로젝트는 두 가지 목표를 동시에 달성합니다:

- 이력서에 포트폴리오로 첨부할 수 있는 데모급 프로젝트
- 개인의 일상적인 지식 관리와 메모 운영에 장기적으로 사용할 시스템

## 주요 기술 스택

- Kotlin, Spring Boot, JPA
- PostgreSQL, Redis
- Thymeleaf (UI)
- JUnit (테스트)

## 현재 구현 상태

초기 Clean Architecture 계층과 PostgreSQL 저장 기반이 준비되었습니다:

- `POST /api/v1/notes` - 노트 생성 API
- `GET /api/v1/notes/{id}` - 노트 조회 API
- `GET /api/v1/notes/search?keyword=...` - 노트 검색 API
- `PUT /api/v1/notes/{id}` - 노트 수정 API
- `DELETE /api/v1/notes/{id}` - 노트 삭제 API
- 계층화된 패키지 구조 (`domain`, `application`, `adapter`, `infrastructure`)
- `JpaNotePersistenceAdapter`를 통한 PostgreSQL 저장/조회
- `note_tags` 분리 테이블 기반 태그 저장
- TDD 기반 단위 테스트 + 웹 통합 테스트

## 기획 문서

- `docs/mvp-v1.md` - MVP 범위 및 완료 기준
- `docs/architecture-draft.md` - 아키텍처 계층 설명
- `docs/sprint-1-plan.md` - 1주차 작업 계획
- `docs/LEARNING_GUIDE.md` - **← 개발 방식/설계 패턴 상세 설명** (필독!)
- `docs/postgresql-로컬-실행.md` - **← PostgreSQL로 실제 저장해보는 방법**

## 테스트 실행

```powershell
.\gradlew.bat test
```

## PostgreSQL로 실제 저장해보기

기본 실행 설정은 PostgreSQL 기준으로 작성되어 있습니다.

1. 환경파일 준비 (`.env`는 Git에 올라가지 않음)

```powershell
Copy-Item .env.example .env
```

2. PostgreSQL 준비
   - 직접 설치하거나
   - Docker Desktop이 있다면 루트의 `compose.yaml` 사용
3. 애플리케이션 실행

```powershell
.\gradlew.bat bootRun
```

4. 자세한 실행 방법은 `docs/postgresql-로컬-실행.md` 문서 참고

## 시작하기

이 프로젝트를 이해하려면:

1. `docs/LEARNING_GUIDE.md`를 읽어 전체 구조와 설계 원칙 파악
2. 각 레이어의 코드를 위에서 아래로(컨트롤러 → 서비스 → 도메인) 읽기
3. 테스트 코드를 보며 각 계층의 책임 확인
4. `docs/postgresql-로컬-실행.md`를 보며 인메모리 저장소와 PostgreSQL 저장소 차이 이해

