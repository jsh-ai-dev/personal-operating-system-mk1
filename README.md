# Personal Operating System mk1

개인 지식과 업무 메모를 저장하고, 검색하고, AI로 요약하는 Kotlin/Spring Boot 기반 노트 서비스입니다.

`mk1`은 Personal Operating System 시리즈에서 노트와 문서 저장 도메인을 담당하며, `mk2`의 인증/BFF와 `mk3`의 AI 대화 저장소와 함께 동작하도록 설계했습니다.

시리즈 저장소:

- [mk1 - Spring Boot 노트/검색/파일/AI 요약](https://github.com/jsh-ai-dev/personal-operating-system-mk1)
- [mk2 - Next.js/NestJS 통합 웹, 인증, 일정](https://github.com/jsh-ai-dev/personal-operating-system-mk2)
- [mk3 - FastAPI AI 대화 저장, 임포트, 검색](https://github.com/jsh-ai-dev/personal-operating-system-mk3)

## 한눈에 보기

| 구분 | 내용 |
|---|---|
| 목적 | 개인 메모, 학습 자료, PDF를 장기 보관 가능한 지식 저장소로 관리 |
| 핵심 기능 | 노트 CRUD, 태그/북마크, 파일 업로드, AI 요약, 검색, 사용자별 소유권 분리 |
| 설계 | Clean Architecture 스타일의 domain/application/adapter/infrastructure 분리 |
| 운영 요소 | PostgreSQL, Redis 세션/캐시, Elasticsearch 검색, Docker, Kubernetes, ECR 배포 workflow |
| 연동 | mk2 auth-service JWT를 받아 동일 사용자 기준으로 mk1 노트 접근 제어 |

## 주요 기능

- 노트 작성, 수정, 삭제, 상세 조회
- 태그, 공개 범위, 북마크 관리
- 사용자별 노트 소유권 분리
- 페이지네이션 응답 헤더, 작성일/수정일/제목/관련도 정렬
- `.txt`, `.pdf` 업로드와 원본 다운로드 또는 새 탭 열기
- PDF 원본 바이트 저장, PDFBox 기반 텍스트 추출 후 AI 요약
- OpenAI/Gemini 기반 AI 요약 생성, 요약 결과와 토큰/예상 비용 메타데이터 저장
- Redis 기반 Spring Session과 노트 목록 캐시
- Elasticsearch 기반 검색 인덱싱, 제목/태그/요약/본문 가중치 검색, 하이라이트, DB fallback
- Thymeleaf 웹 UI와 REST API 동시 제공

## 아키텍처

```text
src/main/kotlin/com/jsh/pos
├─ domain
│  └─ note                 # Note, Visibility 등 순수 도메인 모델
├─ application
│  ├─ port/in              # 유스케이스 입력 포트
│  ├─ port/out             # 저장소, 검색, 캐시, AI 출력 포트
│  └─ service              # 유스케이스 구현
├─ adapter
│  ├─ in/web               # REST API, Thymeleaf Controller
│  └─ out
│     ├─ ai                # OpenAI/Gemini 요약 어댑터
│     ├─ persistence       # JPA/InMemory 저장소 어댑터
│     └─ search            # Elasticsearch 검색 어댑터
└─ infrastructure
   ├─ cache                # Redis 목록 캐시
   ├─ config               # Security, Cache, Clock, Auth 설정
   ├─ search               # 검색 인덱스 재색인 Runner
   └─ security             # JWT 인증 필터, mk2 auth-service 원격 로그인
```

도메인과 유스케이스는 Spring MVC, JPA, Elasticsearch, AI SDK에 직접 의존하지 않습니다. 저장소, 검색 엔진, AI provider, 캐시는 포트 뒤의 어댑터로 격리해 테스트와 교체가 쉽도록 구성했습니다.

## 구현 포인트

### 1. 노트 도메인

`Note`는 불변 `data class`로 설계했습니다. 생성/수정 시 제목과 본문 공백 검증, 태그 정규화, 북마크 상태, 파일 메타데이터, AI 요약 저장 규칙을 도메인 메서드에 모았습니다.

- `create(...)`: 입력 trim/filter, owner 기본값, 파일 노트의 본문 예외 처리
- `update(...)`: 제목/본문 검증, `updatedAt` 갱신
- `bookmark()` / `unbookmark()`: 내용 수정 시간과 북마크 상태 변경 분리
- `updateSummary(...)`: 요약 본문, 모델 tier, 토큰, 예상 비용 저장

### 2. 파일 노트와 AI 요약

텍스트 파일은 본문으로 저장해 일반 노트처럼 수정할 수 있고, PDF는 원본 바이트와 MIME 타입을 저장합니다. AI 요약 시 PDF는 PDFBox로 텍스트를 추출하고, 읽을 수 있는 텍스트가 없으면 명확한 사용자 오류로 처리합니다.

요약 provider는 `AiSummaryPort` 뒤로 분리했습니다.

- OpenAI: 기본 모델 `gpt-5-nano`, UI에서 `gpt-5-nano` / `gpt-5-mini` 선택
- Gemini: `gemini-2.5-flash`, `gemini-2.5-pro` tier 매핑
- 응답에는 요약문, 모델 tier, 원문 길이, input/output token, 예상 비용 포함
- 생성과 저장을 분리해 사용자가 요약 결과를 확인한 뒤 노트에 반영

### 3. 검색과 캐시

목록 조회는 Redis 캐시를 사용합니다.

- 사용자, 조회 모드, 검색어 hash, 정렬, 페이지, 사이즈를 포함한 캐시 키 구성
- 전체 목록, 북마크 목록, 검색 목록을 mode별 인덱스로 관리
- 노트 생성/수정/삭제/북마크/요약 저장 시 해당 사용자의 관련 캐시 무효화
- TTL 기반 자동 만료

검색은 Elasticsearch를 선택적으로 사용합니다.

- `title`, `tags`, `aiSummary`, `content` 필드에 가중치 적용
- 하이라이트 결과를 UI에 전달하고, 검색 엔진 하이라이트가 없을 때 서버에서 fallback snippet 생성
- Elasticsearch 비활성화 또는 장애 시 JPA 기반 DB 검색으로 fallback
- 시작 시 기존 노트 재색인 옵션 제공

### 4. 인증과 소유권

웹 화면은 Spring Security 세션 로그인을 지원하고, REST API는 stateless JWT 인증을 지원합니다.

- 세션은 Redis에 저장해 재시작과 다중 인스턴스를 고려
- `Authorization: Bearer <JWT>` 또는 설정된 httpOnly 쿠키에서 토큰 추출
- mk2 auth-service가 발급한 JWT의 `sub`를 사용자 식별자로 사용
- 다른 사용자의 노트는 존재 여부를 노출하지 않도록 404 처리
- mk2 auth-service로 원격 로그인/로그아웃하는 컨트롤러 포함

## API 요약

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/v1/notes` | 목록 조회, 검색어/북마크/정렬/페이지 조건 지원 |
| `POST` | `/api/v1/notes` | 노트 생성 |
| `POST` | `/api/v1/notes/upload` | `.txt`/`.pdf` 파일 업로드 |
| `GET` | `/api/v1/notes/{id}` | 노트 상세 조회 |
| `PUT` | `/api/v1/notes/{id}` | 노트 수정 |
| `DELETE` | `/api/v1/notes/{id}` | 노트 삭제 |
| `GET` | `/api/v1/notes/{id}/download` | 원본 파일 또는 텍스트 다운로드, `inline=true` 지원 |
| `GET` | `/api/v1/notes/bookmarks` | 북마크 목록 조회 |
| `POST` | `/api/v1/notes/{id}/bookmark` | 북마크 등록 |
| `DELETE` | `/api/v1/notes/{id}/bookmark` | 북마크 해제 |
| `POST` | `/api/v1/notes/{id}/summary/generate` | AI 요약 생성 |
| `POST` | `/api/v1/notes/{id}/summary/save` | 생성된 요약 저장 |

웹 UI:

| Endpoint | 설명 |
|---|---|
| `/login` | 로그인 |
| `/notes` | 목록, 검색, 북마크 필터, 정렬, 페이지 이동 |
| `/notes/new` | 노트 작성 |
| `/notes/{id}` | 상세, 다운로드, 북마크, AI 요약 생성/저장 |
| `/notes/{id}/edit` | 직접 작성한 노트 수정 |
| `/summary` | 파일 업로드 기반 단독 요약 화면 |

## 기술 스택

| 영역 | 기술 |
|---|---|
| Language | Kotlin 2.1, Java 21 |
| Backend | Spring Boot 3.5, Spring MVC, Spring Security |
| Persistence | Spring Data JPA, PostgreSQL, H2 test runtime |
| Cache/Session | Redis, Spring Session |
| Search | Elasticsearch, Spring Data Elasticsearch |
| AI | OpenAI API, Gemini API |
| View | Thymeleaf, CSS, JavaScript |
| File Processing | Apache PDFBox |
| Infra | Docker, Docker Compose, Kubernetes, Kustomize, GitHub Actions, AWS ECR |
| Test | JUnit 5, Spring Boot Test, Spring Security Test, Mockito Kotlin |

## 로컬 실행

### 1. 환경 파일 준비

```powershell
Copy-Item .env.example .env
```

주요 환경변수:

```text
POS_DB_URL=jdbc:postgresql://localhost:5432/pos_mk1
POS_DB_USERNAME=pos
POS_DB_PASSWORD=pos
POS_REDIS_HOST=localhost
POS_REDIS_PORT=6379
POS_AI_PROVIDER=openai
OPENAI_API_KEY=
GEMINI_API_KEY=
POS_JWT_SECRET=
```

### 2. 개발용 전체 실행

```powershell
.\dev.ps1
```

`dev.ps1`은 PostgreSQL, Redis, Elasticsearch를 Docker Compose로 올리고 readiness를 확인한 뒤 애플리케이션을 실행합니다.

### 3. 인프라/앱 분리 실행

```powershell
.\bootRun.ps1              # PostgreSQL/Redis 실행 후 bootRun
.\bootRun.ps1 -InfraOnly   # 인프라만 실행
.\bootRun.ps1 -SkipInfra   # 이미 떠 있는 인프라에 앱만 실행
```

### 4. Docker Compose 실행

```powershell
docker compose up -d --build
```

기본 접속:

- Web UI: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Elasticsearch: `http://localhost:9200`

Elasticsearch 검색을 사용하려면 다음 값을 켭니다.

```text
POS_SEARCH_ELASTICSEARCH_ENABLED=true
POS_ELASTICSEARCH_URIS=http://localhost:9200
POS_SEARCH_ELASTICSEARCH_INDEX_NAME=notes-mk1
POS_SEARCH_ELASTICSEARCH_REINDEX_ON_STARTUP=true
```

## 테스트

```powershell
.\gradlew.bat test
```

테스트는 도메인 규칙, 유스케이스, JPA 매핑, REST/Thymeleaf 컨트롤러, 인증 흐름, 업로드/다운로드, AI 응답 파서, 검색/캐시 조건 처리를 다룹니다.

대표 범위:

- `NoteTest`: 생성/수정/북마크/요약 도메인 규칙
- `GetNoteListPageServiceTest`, `SearchNotesServiceTest`: 페이지네이션, 정렬, 검색 하이라이트 fallback
- `JpaNoteRepositoryTest`, `JpaNotePersistenceAdapterTest`: 파일 노트 저장, owner 필터, 검색 조건
- `NoteControllerTest`, `NotePageControllerTest`: REST와 Thymeleaf 플로우
- `AuthenticationFlowTest`: 로그인, JWT 쿠키, Bearer 인증, 로그아웃
- `GeminiModelResolverTest`, `GeminiResponseExtractorTest`: AI 어댑터 보조 로직

## 배포와 운영 구성

- `Dockerfile`: Java 21 기반 멀티 스테이지 빌드, non-root `app` 사용자 실행
- `compose.yaml`: app, PostgreSQL 16, Redis 7, Elasticsearch 8 단일 로컬 스택
- `compose.redis.yaml`: 외부 data-box에서 Redis만 분리 운영하는 구성
- `k8s/base`: Namespace, ConfigMap, Secret 예시, PostgreSQL, Redis, App, Ingress
- `k8s/overlays/aws`: 외부 RDS/Redis/Elasticsearch 연결을 전제로 한 AWS 오버레이와 ECR 이미지 매핑
- `.github/workflows/ecr-push.yml`: 수동 실행으로 Docker 이미지를 ECR에 push하고, self-hosted runner에서 k3s rollout restart 수행
