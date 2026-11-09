# 뽀득뽀득 Backend

[![Backend CI](https://github.com/minjuko/ppodeuk-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/minjuko/ppodeuk-backend/actions/workflows/ci.yml)

셀프 세차장 탐색·예약 서비스 **뽀득뽀득**의 Spring Boot API 서버입니다. 사용자용 USER Frontend와 사업자용 OWNER Frontend가 인증, 세차장, 예약, 결제, 리뷰와 운영 관리 기능을 사용할 수 있도록 API를 제공합니다.

## 프로젝트 개요

| 항목 | 내용 |
| --- | --- |
| 개발 기간 | 2023.09.14 ~ 2023.12.02 |
| 팀 구성 | 6명 (Frontend 3명, Backend 3명) |
| 저장소 역할 | Backend API |
| 개인 담당 | USER Frontend 구현, Backend API 명세·연동 협업 |
| 수상 | 카카오 테크 캠퍼스 1기 신규 서비스 개발 프로젝트 대상 |

## 주요 기능 및 화면

아래는 Backend 팀 저장소에 구현된 기능입니다.

- JWT 인증과 USER·OWNER 역할별 접근 제어
- 위치·키워드 기반 세차장 탐색과 상세 조회
- 영업시간·기존 예약을 반영한 예약 검증
- KakaoPay 결제 준비·승인과 예약 생성
- 리뷰 작성·조회와 키워드 관리
- OWNER 세차장·Bay·예약·매출 관리
- AWS S3 기반 이미지 업로드

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java, Spring Boot 2.7, Spring MVC |
| Data | Spring Data JPA, MariaDB |
| Security | Spring Security, JWT |
| External | KakaoPay API, AWS S3 |
| Test | JUnit 5, Spring Boot Test, MockMvc, H2 |
| Build·Quality | Gradle, Docker, GitHub Actions |

## 담당 역할 및 핵심 구현

2023년 팀 프로젝트에서는 USER 프론트엔드와 API 명세 협업을 담당했으며, 2026년 개인 후속 작업으로 서버 예약 검증·테스트·실행 환경을 개선했습니다.

- `local`, `demo`, `test`, `prod` 실행 환경 분리
- 외부 결제를 호출하지 않는 로컬·데모 결제 흐름 구성
- 자정 경계를 포함한 예약 시간 계산과 중복 검증 보완
- 회원·세차장·Bay 소유권 검사를 ID 값 기준으로 정리
- 고정된 외부 주소와 proxy 설정을 환경변수 기반으로 전환
- H2 기반의 독립적인 통합 테스트 환경과 결정론적 fixture 구성
- GitHub Actions에서 전체 테스트와 실행 JAR 빌드 자동화

## 트러블슈팅

### 서로 다른 사용자의 동일 세차 베이 예약 중복

- **문제:** 같은 베이·시간대에 다른 사용자의 예약이 존재할 때도 중복 검증이 필요했습니다.
- **해결:** 예약 중복 검사를 회원별 조회에서 베이 기준 검사로 확장하고, 예약 종료 시각과 다음 예약 시작 시각이 인접한 경우는 허용했습니다.
- **검증:** 겹침 거부와 인접 예약 허용 회귀 테스트를 추가해 Backend 86건 테스트와 CI에서 확인했습니다.

### 개인 후속 개선 · 예약 동시성 검증

- **문제:** 동일 Bay에 대한 동시 예약 요청에서 중복 예약이 저장되는 경합을 격리 MariaDB로 재현했습니다.
- **해결:** 예약 생성·로컬 결제 경로에서 Bay 비관적 잠금을 중복 검사보다 먼저 획득하도록 수정했습니다.
- **검증:** MariaDB 독립 트랜잭션 테스트 6개 노드·총 10회 통과, 기본 JUnit 86개 통과. [검증 기록](docs/reservation-concurrency-verification.md)

## 시작하기 (Getting Started)

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

## 테스트 및 코드 품질

JDK 17과 `test` 프로필에서 외부 DB, AWS, KakaoPay와 Frontend 서버 없이 검증했습니다.

| 검증 항목 | 결과 |
| --- | --- |
| Test Files | 17개 |
| Tests | 17개 테스트 클래스 · 86개 통과 |
| Failure / Skip | 0 / 0 |
| Executable JAR | `bdbd.jar` 빌드 성공 |
| GitHub Actions | `f5133a4`의 `test`·`build` 실행 성공 |

CI는 `main` 브랜치 push와 pull request에서 Gradle Wrapper를 검증하고 `./gradlew clean test build --no-daemon`을 실행하도록 설정되어 있습니다.

## 산출물 및 관련 문서

- [USER Frontend](https://github.com/minjuko/ppodeuk-user-frontend) · [OWNER Frontend](https://github.com/minjuko/ppodeuk-owner-frontend)
- [기존 백엔드 문서](docs/archive/README-2023-original.md)
