# Reservation concurrency verification

## Summary

The backend already serialized the KakaoPay approval path by locking a `Bay` row, but `ReservationService.save` performed its duplicate check without taking that lock. That left other callers of the same save method, including the local payment flow, able to validate against a stale view while a competing insert was uncommitted. A MariaDB reproduction produced two overlapping reservations through that direct save path.

The follow-up change takes the existing `PESSIMISTIC_WRITE` Bay lock at the start of `ReservationService.save`, before loading the operating hours and checking existing reservations. Both payment approval paths also lock the Bay before their first ordinary Bay/Carwash reads. The save method then reacquires the same Bay lock and rechecks availability before inserting.

## Existing request and transaction flow

1. `UserPayController` exposes `/api/user/payment/ready` and `/api/user/payment/approve`. The ready request calculates and records payment intent; it does not create a reservation.
2. The approval request delegates to `PaymentFlowService`. In the production `PayService`, `requestPaymentApproval` is `@Transactional`, calls `BayJPARepository.findByIdForUpdate`, loads the carwash and operating hours, and calls `ReservationService.validateReservationTime`.
3. After the first validation, production code calls KakaoPay approval through `RestTemplate`. On a 2xx response it calls `ReservationService.save`; otherwise it throws `BadRequestError`.
4. `LocalPaymentService.requestPaymentApproval` is `@Transactional` and now locks the Bay before loading the Bay and Carwash. Production `PayService` already did this before validation and KakaoPay approval. Both services call `ReservationService.save`, which is also `@Transactional` and joins the caller transaction with default `REQUIRED` propagation. `save` reacquires the same Bay lock, validates again, then inserts the reservation.

The lock is on a persistent `Bay` row, not on an existing reservation row. A Bay exists before reservations can reference it, so this lock works even when the time slot has no reservation row yet. The database can serialize requests for the same Bay while requests for separate Bay rows proceed independently.

The second lock in `ReservationService.save` is on the same row and in the same transaction as the approval-path lock. It does not create a second lock owner or a lock-order cycle. It maintains the save method's invariant for other callers and keeps the availability check adjacent to the insert. Both approval paths acquire the Bay before ordinary Bay/Carwash reads, so they use the same lock order.

The duplicate check loads non-deleted reservations for the Bay and, when a member is supplied, that member's non-deleted reservations as well. Consequently, the existing business rule also rejects overlapping bookings by the same member on different Bays. The different-Bay test uses two different members to isolate the Bay-scoped lock behavior.

The overlap predicate permits exact adjacency: an existing reservation ending at the requested start, or beginning at the requested end, is not treated as a conflict. The JUnit regression test covers the end-to-start boundary; the MariaDB test confirms the same result with persisted rows.

## Audit findings and change

- Before this change, the production `PayService` took the Bay lock before the external approval call and revalidated in `ReservationService.save` after approval.
- `ReservationService.save` itself had no lock. `LocalPaymentService` also read Bay and Carwash before calling it, which could establish a `REPEATABLE-READ` snapshot before the save lock. Thus neither the save method nor the local approval path guaranteed that the post-wait duplicate query saw the competing commit.
- The MariaDB direct-save scenario held a Bay lock in one committed transaction, created an overlapping reservation without committing, and started a second save from a different connection. Before the fix, both saves committed; the final persisted count was two.
- The minimal fix calls the existing `BayJPARepository.findByIdForUpdate` at the beginning of `ReservationService.save` and uses the returned Bay for the insert. `LocalPaymentService` also takes that lock before its initial Bay/Carwash reads so its transaction snapshot is created only after it owns the Bay.
- The `update` method still validates and changes a reservation without taking the Bay lock. Concurrent update-versus-create behavior was not changed or tested in this task and remains outside the verified guarantee.

The repository query for duplicate detection is a Java-side check over fetched rows rather than a database exclusion constraint. Correctness therefore depends on all relevant writers taking the same Bay lock before the check. New write paths should use `ReservationService.save` or follow the same lock protocol.

## Test environment

| Item | Value |
| --- | --- |
| Java source compatibility | 11 |
| Runtime used for this verification | OpenJDK 17.0.20.1 |
| Spring Boot | 2.7.6 |
| Hibernate ORM | 5.6.14.Final |
| MariaDB JDBC driver | 3.0.9 |
| Existing default test database | H2 2.1.214, in-memory, `MODE=MySQL` |
| Concurrency database | MariaDB 11.4.12, InnoDB, `REPEATABLE-READ` |
| Gradle | 8.2.1 |

The normal test profile uses H2, `ddl-auto: create-drop`, and `sql/test-fixture.sql`. H2's MySQL compatibility mode does not make it an InnoDB concurrency test. Testcontainers was considered, but Docker was unavailable and the project had no Testcontainers dependency. Instead, this verification used an independently initialized MariaDB data directory under the OS temporary directory, a randomly selected loopback port, and a dedicated `ppodeuk_mariadb_it` schema. The preconfigured Windows MariaDB service and the project's `.env` values were not used; the service was left untouched.

`application-mariadb-it.yml` requires explicit DB environment variables and uses `ddl-auto: create-drop`. The test class only enables when `BDBD_MARIADB_IT_ENABLED=true`, and before Spring initializes the context it requires a URL of the form `jdbc:mariadb://127.0.0.1:<non-default-port>/ppodeuk_mariadb_it`. Use a disposable instance and schema because Hibernate drops and recreates schema objects during this test.

Example PowerShell setup (replace placeholders with credentials for a disposable local instance):

```powershell
$env:BDBD_MARIADB_IT_ENABLED = "true"
$env:BDBD_MARIADB_IT_URL = "jdbc:mariadb://127.0.0.1:<port>/ppodeuk_mariadb_it"
$env:BDBD_MARIADB_IT_USERNAME = "<test-user>"
$env:BDBD_MARIADB_IT_PASSWORD = "<test-password>"
./gradlew.bat test --tests bdbe.bdbd.reservation.ReservationConcurrencyMariaDbTest
```

Do not point this profile at a shared or production database.

## Test design and results

The MariaDB integration test is not annotated with `@Transactional`. Fixture inserts are committed before each case. Each competing request executes in a `TransactionTemplate`, which gives the threads separate transaction-scoped connections; the test records MariaDB `CONNECTION_ID()` values and asserts that the competing requests use two IDs. One test method covers both direct `ReservationService.save` callers and a real `LocalPaymentService` approval flow, retaining six test nodes while exercising both paths.

For the same-Bay case, the first approval is held inside a mocked `RestTemplate` response while the second request starts. The test observes the second connection's live `SELECT ... FOR UPDATE` in MariaDB's process list while the first transaction still holds the Bay, verifies the approval stub has only been called once, then releases the first request. The first commits and the second fails the duplicate check. `INNODB_LOCK_WAITS` returned no rows in this MariaDB run despite the active lock query, so the test uses the live query, separate connection IDs, API-stub ordering, and post-commit result together as its evidence instead of claiming that the lock-wait system view reported a row.

| Scenario | Result |
| --- | --- |
| Same Bay, overlapping times, independent transactions | Passed 5 repetitions. One reservation committed per round; one request failed duplicate validation; the successful approval stub ran once per round. |
| Direct `ReservationService.save` path under a held Bay lock | Passed after the fix. A separate pre-fix run reproduced two overlapping rows. |
| Local payment approval with a competing same-Bay request | Passed. The second transaction waited for the Bay lock before its ordinary Bay/Carwash reads, then rejected after the first commit. |
| Same Bay, adjacent intervals | Passed; both reservations were stored. |
| Different Bays, overlapping intervals | Passed concurrently. The second Bay completed while the first Bay remained held in the payment stub. |
| Duplicate validation failure | Passed; existing row remained and the payment stub was not called. |
| Non-2xx payment stub | Passed; no reservation row remained. |

The enabled MariaDB suite executed 10 test invocations with zero failures or skips. The same-Bay payment race ran five times. The baseline JUnit suite had 86 tests; all 86 passed on the final build. In the default run the six MariaDB test nodes were skipped because the opt-in flag was not set. Gradle `build` completed successfully.

## Boundaries and risks

- The database was a disposable MariaDB 11.4.12 instance, not the production database. Confirm production's MariaDB version and transaction settings before claiming version-for-version parity.
- No real KakaoPay request or payment cancellation was performed. `RestTemplate` was mocked. The test proves the stub is called only for the winning same-Bay request in that flow.
- The database transaction stays open while the production payment approval request is in flight. This preserves the current serialization design but means a slow provider can hold a Bay lock longer.
- A successful external payment approval and a later database rollback are separate systems. The current approval method calls the provider before saving the reservation; a later database failure can roll back the row but cannot roll back the provider's approval automatically. This task did not implement or verify compensation/cancellation.
- `ReservationService.update` is not covered by the Bay lock guarantee, and no concurrent update test was added.
- The overlap rule also rejects one member's simultaneous reservations across different Bays. The different-Bay concurrency case uses different members; a simultaneous same-member cross-Bay race was not tested and is not serialized by the Bay-only lock.
- No multi-process deployment or production-load test was run. InnoDB row locking is database-level, but this suite validates only the local isolated instance and tested request flow.

## Team work and individual follow-up

This remains a team project. The user's project-time contribution was frontend work; the audit, MariaDB reproduction, central Bay-lock addition, and this document are post-project individual backend follow-up. The checked-out base already contained the external approval lock and its Mockito call-order test (`10f2e97`, 2026-10-18); this follow-up did not introduce that prior work and does not attribute it to the team contribution.

## Portfolio wording

### Portfolio (2–3 sentences)

팀 프로젝트에서는 프론트엔드 작업을 담당했고, 프로젝트 종료 후 예약 정합성 검증과 백엔드 보완을 개인적으로 진행했습니다. 기존 결제 승인 경로에만 있던 Bay 비관적 잠금을 예약 저장 경로에도 적용해, 격리된 MariaDB의 독립 트랜잭션 경합에서 중복 예약 두 건이 저장되던 문제를 막았습니다. 동일 Bay 경합 5회 반복과 인접 시간·다른 Bay·실패 시나리오를 포함한 MariaDB 테스트 10회 실행으로 확인했습니다.

### Resume (one sentence)

팀 프로젝트의 개인 후속 작업으로 Spring Boot/JPA 예약 저장 경로에 MariaDB `PESSIMISTIC_WRITE` Bay 잠금을 적용하고, 독립 커넥션 기반 동시성·시간 경계·잠금 범위·실패 시나리오를 통합 테스트 10회로 검증.

### Technical interview (about one minute)

“팀 프로젝트에서 저는 프론트엔드 작업을 맡았습니다. 프로젝트 종료 후 백엔드 예약 동시성을 개인적으로 검토했습니다. 운영 결제 승인 경로는 결제사 호출 전에 Bay를 잠갔지만, 예약 저장 메서드 자체는 중복 조회 전에 잠금을 잡지 않아 그 경로를 거치지 않는 저장 요청은 경쟁할 수 있었습니다. 격리된 MariaDB에서 독립된 두 커넥션으로 중복 예약 두 건이 저장되는 상황을 재현했습니다. 이후 트랜잭션 저장 메서드가 중복을 검사하기 전에 같은 Bay 행을 비관적 잠금으로 잡도록 보완했습니다. 첫 요청을 결제 stub에서 멈춰 둔 채 두 번째 커넥션의 `FOR UPDATE` 쿼리가 진행 중인 것을 확인했고, 첫 요청이 한 번만 승인 stub을 호출하고 예약 한 건을 커밋한 뒤 두 번째 요청은 중복으로 거절되는 순서를 검증했습니다. 이 경쟁을 다섯 번 반복했고 인접 시간, 다른 Bay의 동시 처리, 검증 및 결제 stub 실패도 확인했습니다. 실제 결제사나 운영 DB는 사용하지 않았습니다.”
