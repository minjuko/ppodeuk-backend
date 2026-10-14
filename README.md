# 뽀득뽀득 Backend

[![Backend CI](https://github.com/minjuko/ppodeuk-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/minjuko/ppodeuk-backend/actions/workflows/ci.yml)

셀프 세차장 탐색·예약 서비스 **뽀득뽀득**의 Spring Boot API 서버입니다. 사용자용 USER Frontend와 사업자용 OWNER Frontend가 인증, 세차장, 예약, 결제, 리뷰와 운영 관리 기능을 사용할 수 있도록 API를 제공합니다.

> Backend의 2023년 구현은 Backend 팀원이 담당했습니다.  
> 개인의 팀 프로젝트 주요 담당은 [USER Frontend](https://github.com/minjuko/ppodeuk-user-frontend)입니다. Backend에서는 API 요청·응답 명세와 연동 과정에 참여했고, 이후 로컬 재현 환경, 테스트와 CI를 보완했습니다.

## 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 개발 기간 | 2023.09.14 ~ 2023.12.02 |
| 팀 구성 | 6명 (Frontend 3명, Backend 3명) |
| 저장소 역할 | Backend API |
| 개인 담당 | USER Frontend 구현, Backend API 명세·연동 협업 |
| 수상 | 카카오 테크 캠퍼스 1기 신규 서비스 개발 프로젝트 대상 |

## 주요 기능

아래는 Backend 팀 저장소에 구현된 기능입니다.

- JWT 인증과 USER·OWNER 역할별 접근 제어
- 위치·키워드 기반 세차장 탐색과 상세 조회
- 영업시간·기존 예약을 반영한 예약 검증
- KakaoPay 결제 준비·승인과 예약 생성
- 리뷰 작성·조회와 키워드 관리
- OWNER 세차장·Bay·예약·매출 관리
- AWS S3 기반 이미지 업로드

## API 구조

| 경로 | 접근 범위 | 주요 기능 |
| --- | --- | --- |
| `/api/open/**` | 비회원 포함 | 회원가입·로그인, 세차장·Bay·리뷰 조회 |
| `/api/user/**` | USER·OWNER | 예약·결제·리뷰 관리 |
| `/api/owner/**` | OWNER | 세차장·Bay·예약·매출 관리 |

Frontend 검증은 사용자가 유효한 값을 선택하도록 돕는 역할을 하고, Backend는 영업시간, 예약 중복, 요청 금액과 접근 권한을 다시 검증합니다.

## 개선 작업

- `local`, `demo`, `test`, `prod` 실행 환경 분리
- 외부 결제를 호출하지 않는 로컬·데모 결제 흐름 구성
- 자정 경계를 포함한 예약 시간 계산과 중복 검증 보완
- 회원·세차장·Bay 소유권 검사를 ID 값 기준으로 정리
- 고정된 외부 주소와 proxy 설정을 환경변수 기반으로 전환
- H2 기반의 독립적인 통합 테스트 환경과 결정론적 fixture 구성
- GitHub Actions에서 전체 테스트와 실행 JAR 빌드 자동화

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java, Spring Boot 2.7, Spring MVC |
| Data | Spring Data JPA, MariaDB |
| Security | Spring Security, JWT |
| External | KakaoPay API, AWS S3 |
| Test | JUnit 5, Spring Boot Test, MockMvc, H2 |
| Build·Quality | Gradle, Docker, GitHub Actions |

## 품질 검증

JDK 17과 `test` 프로필에서 외부 DB, AWS, KakaoPay와 Frontend 서버 없이 검증했습니다.

| 검증 항목 | 결과 |
| --- | --- |
| Test Files | 17개 |
| Tests | 85개 통과 |
| Failure / Skip | 0 / 0 |
| Executable JAR | `bdbd.jar` 빌드 성공 |
| GitHub Actions | `f5133a4`의 `test`·`build` 실행 성공 |

CI는 `main` 브랜치 push와 pull request에서 Gradle Wrapper를 검증하고 `./gradlew clean test build --no-daemon`을 실행하도록 설정되어 있습니다.

## 실행 방법

### 요구 환경

- JDK 17
- MariaDB
- Gradle Wrapper 사용

### 로컬 환경 변수

```env
LOCAL_DB_URL=jdbc:mariadb://localhost:3306/bdbd?allowPublicKeyRetrieval=true&useSSL=false
LOCAL_DB_USERNAME=bdbd
LOCAL_DB_PASSWORD=your_local_password
LOCAL_JWT_SECRET=your_local_jwt_secret
LOCAL_FRONTEND_ORIGIN=http://localhost:5173
```

### 로컬 실행

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

로컬 검증용 데이터는 `scripts/seed-local.sql`에서 확인할 수 있습니다.

### 테스트와 빌드

```bash
SPRING_PROFILES_ACTIVE=test ./gradlew clean test build --no-daemon
```

테스트 프로필은 H2 인메모리 DB와 테스트 전용 fixture를 사용하며 별도 secret이나 외부 환경변수가 필요하지 않습니다.

## 실행 환경 구분

| 프로필 | 용도 | 외부 결제 | 파일 저장 |
| --- | --- | --- | --- |
| `local` | 로컬 통합 실행 | 비활성화 | 로컬 저장 |
| `demo` | 격리된 데모 환경 | 비활성화 | 정적 데모 자원 |
| `test` | 자동 테스트·CI | 비활성화 | 사용하지 않음 |
| `prod` | 운영 설정 | 환경변수 필요 | AWS S3 |

## 관련 저장소

- [USER Frontend](https://github.com/minjuko/ppodeuk-user-frontend) — 개인 주요 담당, 사용자 예약 흐름
- [OWNER Frontend](https://github.com/minjuko/ppodeuk-owner-frontend) — 사업자용 관리 화면

## 문서

- [2023년 Backend 팀 README 보존본](./docs/archive/README-2023-original.md)

## 2026년 개인 개선·검증: 예약·결제 서버 경계

2026-09-18 원본 커밋 기준 JDK 17, Gradle Wrapper 8.2.1, Spring Boot 2.7.6에서 테스트 소스 파일 17개, 83건 통과(실패·건너뜀 0), `gradlew.bat build` 성공을 확인했습니다. 변경 후 같은 17개 파일에서 85건 통과(실패·건너뜀 0), `gradlew.bat build`와 `git diff --check` 성공을 확인했습니다. 별도 정적 분석 도구는 실행하지 않았습니다. CI 워크플로는 Wrapper 검증, Java 17 설정, Gradle 캐시 준비 후 `clean test build`를 실행합니다. 커밋 `f5133a4`의 [GitHub Actions 실행](https://github.com/minjuko/ppodeuk-backend/actions/runs/35303591289)은 전체 단계가 성공했습니다.

프론트엔드는 예약 시간과 예상 금액을 표시하고 요청을 보냅니다. 서버는 영업시간, 30분 단위·최소 이용 시간, 기존 예약과의 시간 겹침을 확인하고 세차장 단가로 금액을 다시 계산합니다. 결제 준비 시 요청 금액과 서버 계산 금액을 비교합니다. 이번 변경으로 회원의 기존 예약뿐 아니라 **같은 베이에 있는 다른 회원의 예약**도 겹침 검증 대상에 포함했습니다. 회귀 테스트는 다른 회원의 같은 베이 시간 겹침을 거부하고, 기존 예약 종료 시각에 시작하는 요청은 허용하는 두 경로를 추가했습니다.

공개 예약 조회는 현재 점유 현황을 보여 주지만 예약 확정은 아닙니다. 예약은 결제 승인 이후 저장됩니다. 로컬·데모 결제는 서버 메모리의 준비 정보에 대해 토큰·회원·베이·시간·만료를 확인하며, 성공 후 재사용을 막습니다. 외부 KakaoPay 경로는 실제 승인 응답 성공 뒤 예약을 저장하지만 준비 요청과 승인 요청의 결제 식별자·금액을 서버에 영속적으로 묶거나 승인 재시도를 멱등 처리하지 않습니다. 외부 승인 성공 후 예약 저장에 실패하면 결제와 예약이 불일치할 수 있습니다. 두 경로 모두 조회와 저장 사이의 동시 요청을 DB 잠금·제약으로 직렬화하지 않으므로 동시 예약 방지는 보장하지 않습니다. 운영 결제와 운영 DB를 사용한 검증은 수행하지 않았습니다.
