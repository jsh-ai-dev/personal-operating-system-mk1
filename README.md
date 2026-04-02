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

초기 Clean Architecture 계층이 완성되었습니다:

- `POST /api/v1/notes` - 노트 생성 API
- `GET /api/v1/notes/{id}` - 노트 조회 API
- `PUT /api/v1/notes/{id}` - 노트 수정 API
- `DELETE /api/v1/notes/{id}` - 노트 삭제 API
- 계층화된 패키지 구조 (`domain`, `application`, `adapter`, `infrastructure`)
- TDD 기반 단위 테스트 + 웹 통합 테스트

## 기획 문서

- `docs/mvp-v1.md` - MVP 범위 및 완료 기준
- `docs/architecture-draft.md` - 아키텍처 계층 설명
- `docs/sprint-1-plan.md` - 1주차 작업 계획
- `docs/LEARNING_GUIDE.md` - **← 개발 방식/설계 패턴 상세 설명** (필독!)

## 테스트 실행

```powershell
.\gradlew.bat test
```

## 시작하기

이 프로젝트를 이해하려면:

1. `docs/LEARNING_GUIDE.md`를 읽어 전체 구조와 설계 원칙 파악
2. 각 레이어의 코드를 위에서 아래로(컨트롤러 → 서비스 → 도메인) 읽기
3. 테스트 코드를 보며 각 계층의 책임 확인

