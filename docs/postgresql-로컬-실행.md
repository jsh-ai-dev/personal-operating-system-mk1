# PostgreSQL 로컬 실행 가이드

이 문서는 `Personal Operating System` 프로젝트에서 **메모를 PostgreSQL에 실제로 저장해보는 방법**을 설명합니다.

## 1. 이번 단계에서 바뀐 점

이전에는 `InMemoryNoteRepository`가 메모를 JVM 메모리에만 저장했습니다.
즉, 서버를 재시작하면 데이터가 사라졌습니다.

이번에는 다음 구조로 바뀌었습니다.

```text
NoteController
  -> UseCase / Service
    -> NoteCommandPort / NoteQueryPort
      -> JpaNotePersistenceAdapter
        -> NoteJpaRepository
          -> PostgreSQL
```

핵심은 **서비스 계층은 그대로 두고 저장소 어댑터만 JPA 구현으로 교체**했다는 점입니다.
이게 Clean Architecture에서 포트를 두는 큰 이유 중 하나입니다.

---

## 2. 데이터가 어디에 저장되나?

- `notes` 테이블: 메모 본문 데이터
- `note_tags` 테이블: 태그 목록

즉, 태그를 문자열 하나로 우겨넣지 않고 별도 테이블로 분리했습니다.
나중에 태그 기능이 커져도 확장하기 쉽습니다.

---

## 3. 가장 쉬운 실행 방법

Docker Desktop 이 설치되어 있다면 루트의 `compose.yaml`로 PostgreSQL을 바로 띄울 수 있습니다.

먼저 `.env`를 준비하세요. (`.env` 파일은 Git에 커밋되지 않도록 설정되어 있습니다.)

```powershell
cd D:\dev\personal-operating-system
Copy-Item .env.example .env
```

그다음 `.env`에서 `POS_DB_PASSWORD`를 본인 로컬 값으로 바꿔주세요.

```powershell
cd D:\dev\personal-operating-system
docker compose up -d
```

정상 실행 후에는 다음 정보로 DB가 열립니다.

- DB: `.env`의 `POS_DB_NAME` (기본값 `personal_operating_system`)
- User: `.env`의 `POS_DB_USERNAME` (기본값 `pos`)
- Password: `.env`의 `POS_DB_PASSWORD`
- Port: `5432`

---

## 4. 애플리케이션 실행

기본 `application.yaml`은 로컬 PostgreSQL 기준으로 작성되어 있습니다.
필요하면 환경변수로 덮어쓸 수 있습니다.

```powershell
cd D:\dev\personal-operating-system
.\gradlew.bat bootRun
```

환경변수를 바꿔서 실행하고 싶다면:

```powershell
cd D:\dev\personal-operating-system
$env:POS_DB_URL="jdbc:postgresql://localhost:5432/personal_operating_system"
$env:POS_DB_USERNAME="pos"
$env:POS_DB_PASSWORD="pos"
.\gradlew.bat bootRun
```

---

## 5. 메모 생성 테스트

서버가 켜진 뒤 아래 요청을 보내면 실제 PostgreSQL에 저장됩니다.

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/notes" -ContentType "application/json" -Body '{"title":"PostgreSQL 저장 실습","content":"이 메모는 PostgreSQL에 저장됩니다.","visibility":"PRIVATE","tags":["postgresql","jpa","kotlin"]}'
```

응답으로 `id`가 내려오면 저장 성공입니다.

---

## 6. 저장 확인

같은 `id`로 조회하면 됩니다.

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/notes/{id}"
```

검색도 가능합니다.

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/notes/search?keyword=postgresql"
```

DB 클라이언트(DBeaver, Toad) 없이 터미널에서 직접 확인하려면:

```powershell
cd D:\dev\personal-operating-system
.\scripts\db-query.ps1 -Sql "select id, title, visibility, created_at from notes order by created_at desc;"
```

태그 테이블 확인:

```powershell
cd D:\dev\personal-operating-system
.\scripts\db-query.ps1 -Sql "select note_id, tag from note_tags order by note_id, tag;"
```

---

## 7. 왜 테스트는 PostgreSQL 없이도 돌아가나?

테스트는 `src/test/resources/application.yaml`을 사용해서 H2 메모리 DB로 실행됩니다.

이렇게 한 이유:
- CI나 로컬에서 테스트를 빠르게 돌리기 쉬움
- PostgreSQL이 없어도 테스트 가능
- 하지만 실제 실행은 PostgreSQL 기준으로 구성 가능

즉,
- **개발/실행**: PostgreSQL
- **자동 테스트**: H2

로 역할을 분리한 것입니다.

---

## 8. 개인정보/시크릿 안전 수칙

1. `.env`에는 실제 비밀번호/민감정보를 넣고, **절대 커밋하지 않습니다.**
2. GitHub에는 `.env.example`만 올리고, 실제 값은 각자 로컬에서 관리합니다.
3. 실제 개인정보 메모는 로컬/사설 DB에서만 테스트하고, 공개 원격 저장소에는 더미 데이터만 사용합니다.
4. 실수로 커밋했다면 즉시 비밀번호를 교체하고 Git 히스토리 정리를 진행해야 합니다.

---

## 9. 다음에 확장하기 좋은 포인트

1. `ddl-auto: update` 대신 Flyway 적용
2. 검색을 PostgreSQL Full Text Search로 고도화
3. 공개/비공개 조건을 인증과 연동
4. Redis 캐시를 조회 포트 앞단에 추가
5. PostgreSQL + Testcontainers 기반 통합 테스트 도입




