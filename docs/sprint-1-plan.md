# 스프린트 1 계획 (1~2주)

## 스프린트 목표
데모 가능한 핵심 흐름 완성: 노트 생성/조회 API + Clean Architecture 테스트 검증

## 작업 항목

- [x] 작업 1: 노트 생성 API
  - 테스트 우선: 유스케이스 단위 테스트, 컨트롤러 웹 테스트
  - 산출물: `CreateNoteUseCase`, `CreateNoteService`, `POST /api/v1/notes`
  - 검증: 입력값 공백 제거, 검증 예외 처리, 201 응답
  
- [x] 작업 2: 노트 조회 API
  - 테스트 우선: 유스케이스 단위 테스트, 컨트롤러 웹 테스트
  - 산출물: `GetNoteUseCase`, `GetNoteService`, `GET /api/v1/notes/{id}`
  - 검증: 존재하는 노트 200 반환, 없는 노트 404 반환
  
- [x] 작업 3: 공개/비공개 도메인 규칙
  - 테스트 우선: 도메인 검증 테스트
  - 산출물: `Visibility` enum, 노트 생성 시 visibility 강제
  - 검증: 유효한 값만 수용, 무결성 보장
  
- [x] 작업 4: 기초 문서 & 패키지 구조
  - 테스트: 전체 테스트 통과
  - 산출물: MVP/아키텍처/스프린트 계획 문서, 패키지 스캐폴딩
  - 검증: README에서 현재 상태 명시

## 완료 조건
- `./gradlew.bat test` 실행 시 모든 테스트 통과
- 신규 코드가 계층 경계 준수 (`domain` → `application` → `adapter`)
- API 샘플 요청을 로컬에서 실행 가능

## 다음 우선순위
- 검색(Search) 유스케이스 추가
- 북마크(Bookmark) 유스케이스 추가
- 수정/삭제(Update/Delete) 유스케이스 추가

