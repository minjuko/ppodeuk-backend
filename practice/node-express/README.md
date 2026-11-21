# Node.js + Express 예약 실습

## 학습 목적

기존 Spring Boot 예약 일부를 참고해 Node.js 런타임, TypeScript, Express의 요청 흐름과 계층 분리를 연습한다. 기존 서비스를 새 백엔드로 옮기거나 대체하는 프로젝트가 아니다. 실제 DB, OAuth, 결제, 운영 시간 조회는 연결하지 않는다.

## 원본 Java와 대응하는 코드

| Spring Boot 원본 | 실습 코드 | 역할 |
| --- | --- | --- |
| `ReservationRequest.SaveDTO` (`bayId`, `startTime`, `endTime`) | `CreateReservationInput`, `ReservationService.create` | 입력 형태와 필수 값 검증 |
| `UserReservationController`의 예약 관련 엔드포인트 | `reservationRoutes` + `ReservationController` | HTTP 경로와 요청/응답 처리 |
| `ReservationService.validateReservationTime` | `ReservationService.create` | 시간 선후, 30분 단위, 베이별 겹침 검증 |
| `ReservationJPARepository` | `ReservationRepository` | 저장소 접근. 실습에서는 배열 메모리 저장 |
| Spring 예외 처리 | `HttpError`, `app.ts` 에러 미들웨어 | 상태 코드와 JSON 오류 응답 |

원본의 겹침 검사에 해당하는 핵심 조건은 `새 시작 < 기존 종료 && 새 종료 > 기존 시작`이다. 따라서 두 예약이 맞닿는 경우는 겹치지 않는다. 원본의 결제 금액, 회원별 중복, 영업 시간 조회는 이 실습 API 범위에서 생략했다.

## API

- `GET /reservations`: 메모리에 있는 예약 목록을 반환한다.
- `POST /reservations`: `{ "bayId": 1, "startTime": "2026-08-12T09:30", "endTime": "2026-08-12T10:00" }` 형태로 생성한다. 성공은 `201`, 잘못된 입력은 `400`, 같은 베이의 시간 겹침은 `409`다.
- `DELETE /reservations/:id`: 삭제 성공은 `204`, 없는 예약은 `404`다.

날짜 입력은 타임존 표시가 없는 `YYYY-MM-DDTHH:mm` 벽시계 형식이다. 내부 비교에는 날짜와 시간을 UTC 숫자 타임스탬프로 변환하되 입력된 시각 숫자를 그대로 보존한다. 따라서 이 실습은 시간대 변환이나 DST 규칙을 구현하지 않는다.

## 새롭게 익히는 Node.js 개념

- **런타임과 이벤트 루프**: Node.js는 V8에서 JavaScript/TypeScript 결과물을 실행하고, 네트워크 같은 I/O 완료를 이벤트 루프가 이어 처리한다. 동기 CPU 작업은 이벤트 루프를 막을 수 있다.
- **Promise와 async/await**: 저장소 메서드는 `Promise`를 반환하고 서비스와 컨트롤러는 `await`로 비동기 작업의 순서를 읽기 쉽게 표현한다. `await`는 스레드를 차단하지 않는다.
- **Express Router와 Middleware**: Router가 예약 URL과 컨트롤러를 연결한다. JSON 파서는 라우트보다 앞에서 동작하고, 오류 미들웨어는 라우트 뒤에서 공통 오류 응답을 만든다.
- **Spring 구조와 비교**: Controller → Service → Repository 역할은 익숙한 구조로 유지했지만, Spring의 DI 컨테이너·트랜잭션 프록시·JPA 영속성 대신 생성자 주입과 직접 만든 메모리 배열을 사용한다.
- **Java와 TypeScript 날짜 처리**: 원본은 `LocalDateTime`/`Duration`으로 날짜와 기간을 타입으로 다룬다. JavaScript `Date`는 타임존/epoch 기반이므로 실습은 `YYYY-MM-DDTHH:mm`을 명시적으로 검사하고 UTC epoch 값으로 비교한다. 실제 국제 서비스에서는 날짜 전용 값, 로컬 시각, 타임존, DST 정책을 분리해야 한다.

## 실행 및 검증

```sh
npm install
npm test
npm run build
npm run lint
npm run dev
```

Vitest와 Supertest 테스트는 생성/목록, 잘못된 ID·날짜·시간 역전·30분 단위 위반, 같은 베이의 겹침 거부, 경계 맞닿음 허용, 다른 베이 허용, 삭제 및 404를 확인한다. 위 명령 실행 결과는 작업 완료 보고에 기록한다.

## 실습의 한계

- 데이터는 프로세스 메모리에만 있으므로 서버를 재시작하면 사라진다.
- 동시 요청에서 중복 검사를 원자적으로 보장하지 않는다. 운영 시스템에서는 DB 제약/트랜잭션 등 동시성 제어가 필요하다.
- 인증·인가, 회원별 예약 제한, 운영 시간/휴일, 결제, 요금 계산, 페이지네이션은 구현하지 않았다.
- 초 단위/타임존/DST 입력은 받지 않으며 날짜 비교는 학습을 위한 고정 규칙이다.
- Spring Boot 원본의 API URL이나 응답 래퍼를 대체하지 않는다.
