# Personal Operating System mk1

`mk1`은 Personal Operating System의 노트 저장소입니다. 개인 메모, 학습 자료, PDF를 저장하고 검색하며, OpenAI 또는 Gemini로 요약을 생성하는 Kotlin/Spring Boot 애플리케이션입니다.

`mk2`의 auth-service가 발급한 JWT를 받아 같은 사용자 기준으로 데이터를 분리하고, `mk2`의 Next.js 화면에서 노트 기능을 호출할 수 있게 REST API를 제공합니다.

## 시스템 구조

```text
mk2 Next.js BFF :3000
  └─ /api/notes/*  ->  mk1 Spring Boot :8080

mk1 Spring Boot
  ├─ Thymeleaf UI     # /login, /notes, /summary
  ├─ REST API         # /api/v1/notes/*
  ├─ PostgreSQL       # 노트, 파일 메타데이터, 사용자 테이블
  ├─ Redis            # Spring Session, 노트 목록 캐시
  ├─ Elasticsearch    # 선택형 검색 인덱스
  └─ External AI      # OpenAI 또는 Gemini 요약
```

REST API는 mk2 BFF가 전달한 Bearer JWT를 검증하고, Thymeleaf UI는 mk2 auth-service 원격 로그인으로 받은 JWT를 mk1 전용 httpOnly 쿠키에 저장해 사용합니다.

## 기능

- 노트 생성, 조회, 수정, 삭제
- 태그, 공개 범위, 북마크 관리
- `.txt`, `.pdf` 업로드와 원본 다운로드/새 탭 열기
- PDFBox 기반 PDF 텍스트 추출 후 AI 요약 생성
- OpenAI/Gemini 요약 provider 전환, 모델 tier 선택, 토큰/예상 비용 저장
- PostgreSQL 데이터 저장, Redis 세션 및 노트 목록 캐시
- Elasticsearch 검색 선택 지원, 비활성화 또는 장애 시 DB 검색 fallback
- Thymeleaf UI와 REST API 동시 제공

## 저장소 구조

```text
personal-operating-system-mk1/
├─ src/main/kotlin/com/jsh/pos
│  ├─ domain/note              # Note, Visibility 도메인 모델
│  ├─ application
│  │  ├─ port/in               # 유스케이스 입력 포트
│  │  ├─ port/out              # 저장소, 검색, 캐시, AI 출력 포트
│  │  └─ service               # 노트/검색/요약 유스케이스 구현
│  ├─ adapter
│  │  ├─ in/web                # REST API, Thymeleaf controller
│  │  └─ out                   # AI, persistence, search adapter
│  └─ infrastructure           # cache, config, search, security
├─ src/main/resources
│  ├─ templates/               # Thymeleaf 화면
│  ├─ static/                  # CSS, JavaScript
│  └─ application.yaml         # Spring 설정
├─ k8s/                        # Kubernetes base/AWS overlay
├─ compose.yaml                # app, PostgreSQL, Redis, Elasticsearch
├─ dev.ps1                     # 로컬 통합 실행 스크립트
└─ bootRun.ps1                 # 환경 파일 로딩 후 bootRun
```

도메인과 유스케이스가 Spring MVC, JPA, Elasticsearch, AI SDK에 직접 의존하지 않도록 포트/어댑터 형태로 나뉘어 있습니다.

## 주요 API

| Method | Endpoint | 설명 |
|---|---|---|
| `GET` | `/api/v1/notes` | 목록 조회, 검색어/북마크/정렬/페이지 조건 지원 |
| `POST` | `/api/v1/notes` | 노트 생성 |
| `POST` | `/api/v1/notes/upload` | `.txt`, `.pdf` 업로드 |
| `GET` | `/api/v1/notes/{id}` | 노트 상세 조회 |
| `PUT` | `/api/v1/notes/{id}` | 노트 수정 |
| `DELETE` | `/api/v1/notes/{id}` | 노트 삭제 |
| `GET` | `/api/v1/notes/{id}/download` | 원본 파일 또는 텍스트 다운로드, `inline=true` 지원 |
| `GET` | `/api/v1/notes/bookmarks` | 북마크 목록 |
| `POST` / `DELETE` | `/api/v1/notes/{id}/bookmark` | 북마크 설정/해제 |
| `POST` | `/api/v1/notes/{id}/summary/generate` | AI 요약 생성 |
| `POST` | `/api/v1/notes/{id}/summary/save` | 생성된 요약 저장 |

Thymeleaf 화면은 `/login`, `/notes`, `/notes/new`, `/notes/{id}`, `/notes/{id}/edit`, `/summary`에서 제공됩니다.

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
| File | Apache PDFBox |
| Infra | Docker Compose, Kubernetes, Kustomize, GitHub Actions, AWS ECR |

## 로컬 실행

### 1. 환경 파일 준비

```powershell
Copy-Item .env.example .env
```

주요 값:

```text
POS_DB_URL=jdbc:postgresql://localhost:5432/pos_mk1
POS_DB_USERNAME=pos
POS_DB_PASSWORD=pos
POS_REDIS_HOST=localhost
POS_REDIS_PORT=6379
POS_JWT_SECRET=
POS_AUTH_SERVICE_URL=http://127.0.0.1:3002
POS_AUTH_TOKEN_COOKIE=mk1_pos_jwt
POS_AI_PROVIDER=openai
OPENAI_API_KEY=
GEMINI_API_KEY=
```

`POS_JWT_SECRET`는 mk2 auth-service와 같은 값을 써야 Bearer 토큰과 httpOnly 쿠키 인증이 통과합니다.
`POS_AUTH_SERVICE_URL`과 `POS_AUTH_TOKEN_COOKIE`는 기본값이 있어 생략해도 되지만, mk2 auth-service 주소나 쿠키명을 바꿀 때 `.env`에 추가합니다. Thymeleaf 로그인까지 확인하려면 mk2 auth-service가 `:3002`에서 먼저 실행 중이어야 합니다.

### 2. 개발용 전체 실행

```powershell
.\dev.ps1
```

`dev.ps1`은 Docker Compose로 PostgreSQL, Redis, Elasticsearch를 올리고 readiness 확인 후 Spring Boot를 실행합니다.

### 3. 앱/인프라 분리 실행

```powershell
.\bootRun.ps1              # PostgreSQL/Redis 실행 후 bootRun
.\bootRun.ps1 -InfraOnly   # 인프라만 실행
.\bootRun.ps1 -SkipInfra   # 이미 떠 있는 인프라에 앱만 실행
```

### 4. Docker Compose

```powershell
docker compose up -d --build
```

기본 포트:

- Web/API: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Elasticsearch: `http://localhost:9200`

Elasticsearch 검색을 켜려면 `.env`에서 다음 값을 조정합니다.

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

테스트는 도메인 규칙, 유스케이스, JPA adapter, REST/Thymeleaf controller, 인증 흐름, 파일 업로드/다운로드, 검색 fallback, AI 응답 파서를 다룹니다.

## 배포 구성

- `Dockerfile`: Java 21 기반 Spring Boot 이미지
- `compose.yaml`: app, PostgreSQL, Redis, Elasticsearch 로컬 스택
- `k8s/base`: Namespace, ConfigMap, Secret 예시, PostgreSQL, Redis, App, Ingress
- `k8s/overlays/aws`: 외부 데이터 서비스와 ECR 이미지를 사용하는 AWS overlay
- `.github/workflows/ecr-push.yml`: 수동 실행으로 ECR push 후 self-hosted runner에서 k3s rollout restart
