# 뽀득뽀득 Backend

[![Backend CI](https://github.com/minjuko/Team10_BE/actions/workflows/ci.yml/badge.svg)](https://github.com/minjuko/Team10_BE/actions/workflows/ci.yml)

셀프 세차장 탐색·예약 서비스 **뽀득뽀득**의 Spring Boot API 서버입니다. 사용자용 USER Frontend와 사업자용 OWNER Frontend에 인증, 세차장, 예약, 결제, 리뷰와 운영 관리 API를 제공합니다.

> Backend의 2023년 구현은 Backend 팀원이 담당했습니다.  
> 개인의 팀 프로젝트 주요 담당은 [USER Frontend](https://github.com/minjuko/Team10_FE_USER)이며, Backend에서는 API 요청·응답 명세와 연동 과정에 참여했습니다. 이후 로컬 재현 환경, 테스트와 CI 등 개선 작업을 진행했습니다.

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
| Test Files | 16개 |
| Tests | 82개 통과 |
| Failure / Skip | 0 / 0 |
| Executable JAR | `bdbd.jar` 빌드 성공 |
| GitHub Actions | test·bootJar 성공 |

CI는 `main` 브랜치 push와 pull request에서 Gradle Wrapper를 검증하고 `./gradlew clean test bootJar --no-daemon`을 실행합니다.

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
SPRING_PROFILES_ACTIVE=test ./gradlew clean test bootJar --no-daemon
```

테스트 프로필은 H2 인메모리 DB와 테스트 전용 fixture를 사용하며 별도 secret이나 외부 환경변수가 필요하지 않습니다.

## 실행 환경 구분

| 프로필 | 용도 | 외부 결제 | 파일 저장 |
| --- | --- | --- | --- |
| `local` | 로컬 통합 실행 | 비활성화 | 로컬 저장 |
| `demo` | 격리된 데모 환경 | 비활성화 | 정적 데모 자원 |
| `test` | 자동 테스트·CI | 비활성화 | 사용하지 않음 |
| `prod` | 운영 설정 | 환경변수 필요 | AWS S3 |

## 배포 상태

로컬 실행, 자동 테스트와 빌드는 검증했지만 현재 공개 배포는 운영하지 않습니다. 외부 공개 주소는 전체 배포 검증이 끝난 뒤 안내합니다.

## 관련 저장소

- [USER Frontend](https://github.com/minjuko/Team10_FE_USER) — 개인 주요 담당, 사용자 예약 흐름
- [OWNER Frontend](https://github.com/minjuko/Team10_FE_OWNER) — 사업자용 관리 화면

## 문서

- [2023년 Backend 팀 README 보존본](./docs/archive/README-2023-original.md)
