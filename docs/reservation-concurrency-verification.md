# 예약 동시성 검증 및 PP-01 CI 연결

## 요약

기존 backend는 KakaoPay 승인 요청 경로에서 `Bay` 행을 잠갔지만, `ReservationService.save`는 중복 예약을 확인하기 전에 잠금을 잡지 않았다. 따라서 `save`를 직접 호출하는 다른 경로(로컬 결제 포함)는 경쟁 트랜잭션이 아직 커밋하지 않은 예약을 보지 못하고 같은 시간의 예약을 저장할 수 있었다. MariaDB 재현에서 직접 저장 경로로 겹치는 예약 두 건이 저장되는 현상을 확인했다.

후속 수정에서는 `ReservationService.save`가 운영 시간과 기존 예약을 조회하기 전에 기존 `PESSIMISTIC_WRITE` Bay 잠금을 잡도록 했다. 두 결제 승인 경로도 보통의 Bay/Carwash 조회보다 먼저 잠금을 잡는다. `save`는 같은 트랜잭션 안에서 해당 행을 다시 잠그고 예약 가능 시간을 재검사한 뒤 저장한다.

2026-11-19에는 PP-01 작업으로 `.github/workflows/ci.yml`에 MariaDB 전용 job을 추가했다. 기본 H2 `test-and-build`는 유지했다. 로컬 기본 suite/build와 별도 격리 MariaDB suite를 실행했다. PR 원격 Actions도 별도로 실행해 두 job 결과와 로그를 확인했다.

## 요청 흐름과 트랜잭션 경계

1. `UserPayController`는 `/api/user/payment/ready`와 `/api/user/payment/approve`를 제공한다. ready 요청은 결제 의도를 계산·기록하며 예약을 만들지 않는다.
2. 승인 요청은 `PaymentFlowService`를 통해 처리된다. 운영 `PayService.requestPaymentApproval`은 `@Transactional`이며 `BayJPARepository.findByIdForUpdate`를 호출하고 Carwash와 운영 시간을 조회한 다음 `ReservationService.validateReservationTime`을 실행한다.
3. 최초 검증 뒤 운영 경로는 `RestTemplate`을 통해 KakaoPay 승인을 요청한다. 응답이 2xx이면 `ReservationService.save`를 호출하고, 그렇지 않으면 `BadRequestError`를 던진다.
4. `LocalPaymentService.requestPaymentApproval`도 `@Transactional`이며 Bay와 Carwash의 일반 조회보다 먼저 Bay 잠금을 잡는다. 운영 `PayService`도 검증 및 KakaoPay 승인 전에 같은 순서로 잠근다. 두 경로가 호출하는 `ReservationService.save` 역시 `@Transactional`이고 기본 `REQUIRED` 전파로 기존 트랜잭션에 합류한다. `save`는 같은 Bay 행을 다시 잠근 후 가용성을 검증하고 예약을 저장한다.

잠금 대상은 기존 예약 행이 아니라 영속적인 `Bay` 행이다. 예약이 생기기 전부터 Bay는 존재하므로, 아직 예약 행이 없는 시간대도 같은 Bay 기준으로 직렬화할 수 있다. 같은 Bay 요청은 순서대로 처리하면서 서로 다른 Bay 요청은 독립적으로 진행할 수 있다.

승인 경로와 `ReservationService.save`의 두 번째 잠금은 한 트랜잭션 안의 같은 행을 대상으로 한다. 별도 잠금 소유자나 잠금 순환을 만들지 않는다. `save`를 다른 곳에서 호출하더라도 중복 확인과 저장이 잠금 규칙을 따르도록 하고, 가용성 확인을 저장에 인접시킨다. 승인 경로 모두 일반 Bay/Carwash 조회 전에 Bay를 잠가 같은 잠금 순서를 사용한다.

중복 확인은 해당 Bay의 삭제되지 않은 예약을 조회한다. member가 전달된 경우 그 member의 삭제되지 않은 다른 예약도 확인한다. 따라서 현재 업무 규칙상 한 사용자가 서로 다른 Bay를 같은 시간에 예약하는 것도 거부된다. 다른 Bay 동시성 테스트는 Bay 행 단위 잠금의 독립성을 확인하기 위해 서로 다른 member를 사용한다.

겹침 조건은 정확히 맞닿은 시간 구간을 허용한다. 기존 예약이 요청 시작 시각에 끝나거나 요청 종료 시각에 시작하면 충돌로 처리하지 않는다. JUnit 회귀 테스트는 종료·시작 경계를 다루고, MariaDB 테스트는 저장된 행을 사용해 같은 조건을 확인한다.

## 원인 분석과 구현 변경

- 변경 전 운영 `PayService`는 외부 승인 요청 전에 Bay를 잠그고, 승인 뒤 `ReservationService.save`에서 다시 예약 가능 시간을 검사했다.
- 그러나 `ReservationService.save` 자체에는 잠금이 없었다. `LocalPaymentService`도 `save`에 도달하기 전에 Bay와 Carwash를 먼저 조회했으므로, `REPEATABLE-READ`에서는 저장 메서드가 잠금 대기 후 중복 조회를 하더라도 경쟁 트랜잭션의 커밋을 반영하지 않는 snapshot이 생길 수 있었다. 이 때문에 저장 메서드 단독 호출과 로컬 승인 경로 모두 경쟁 커밋을 확인하지 못할 수 있었다.
- 직접 저장 경로 재현에서는 한 트랜잭션이 Bay 잠금을 유지하고 겹치는 예약을 삽입한 채 커밋을 보류했다. 다른 연결에서 두 번째 저장을 시작하자 수정 전에는 두 건 모두 커밋되어 최종 행 수가 2건이었다.
- 최소 변경은 `ReservationService.save`가 시작할 때 `BayJPARepository.findByIdForUpdate`를 호출하고 반환된 Bay를 저장에도 사용하도록 한 것이다. `LocalPaymentService`도 최초 일반 Bay/Carwash 조회보다 먼저 같은 잠금을 잡아, 자기 Bay 잠금 획득 후 트랜잭션 snapshot을 만들도록 했다.
- `ReservationService.update`는 아직 Bay 잠금 없이 유효성을 검사하고 예약을 수정한다. 동시 update 대 create 동작은 변경하거나 검증하지 않았으며, 현재 보장 범위 밖이다.

중복 조회는 DB exclusion constraint가 아니라 읽어 온 예약 행에 대한 Java 검사다. 그러므로 올바른 결과는 모든 예약 쓰기 경로가 검사 전에 같은 Bay 잠금 규칙을 지키는 데 달려 있다. 새 저장 경로는 `ReservationService.save`를 사용하거나 동일한 잠금 순서를 따라야 한다.

## 테스트 환경 및 안전 경계

| 항목 | 값 |
|---|---|
| Java source compatibility | 11 |
| 과거 검증 당시 Runtime | OpenJDK 17.0.20.1 |
| Spring Boot | 2.7.6 |
| Hibernate ORM | 5.6.14.Final |
| MariaDB JDBC driver | 3.0.9 |
| 기본 테스트 DB | H2 2.1.214, in-memory, `MODE=MySQL` |
| 동시성 검증 DB | MariaDB 11.4.12, InnoDB, `REPEATABLE-READ` |
| Gradle | 8.2.1 |

기본 `test` profile은 H2, `ddl-auto: create-drop`, `sql/test-fixture.sql`을 사용한다. H2의 MySQL 호환 모드는 InnoDB 동시성 검증을 대신하지 않는다. Testcontainers도 검토했지만 해당 로컬 검증 환경에서 Docker를 사용할 수 없었고 프로젝트에 Testcontainers 의존성도 없었다. 대신 OS 임시 작업 영역에 독립 MariaDB data directory를 새로 만들고, loopback 임의 포트와 전용 `ppodeuk_mariadb_it` schema를 사용했다. 설치된 Windows MariaDB 서비스와 프로젝트 `.env`는 사용하지 않았고 기존 서비스를 변경하지 않았다.

`src/test/resources/application-mariadb-it.yml`은 `BDBD_MARIADB_IT_URL`, `BDBD_MARIADB_IT_USERNAME`, `BDBD_MARIADB_IT_PASSWORD` 환경 변수를 요구하며 `ddl-auto: create-drop`을 사용한다. 테스트 클래스는 `BDBD_MARIADB_IT_ENABLED=true`일 때만 실행된다. Spring context를 만들기 전에 JDBC URL이 `jdbc:mariadb://127.0.0.1:<port>/ppodeuk_mariadb_it` 형식인지, 포트가 3306이 아니고 1024 이상인지 검사한다. 이 profile은 Hibernate가 schema 객체를 지우고 다시 만들기 때문에 폐기 가능한 전용 인스턴스/schema에서만 사용한다.

로컬에서 실행할 때도 다음처럼 테스트 DB 환경 변수를 지정한다. 값은 별도로 만든 임시 DB 자격증명으로 대체한다.

```powershell
$env:BDBD_MARIADB_IT_ENABLED = "true"
$env:BDBD_MARIADB_IT_URL = "jdbc:mariadb://127.0.0.1:<port>/ppodeuk_mariadb_it"
$env:BDBD_MARIADB_IT_USERNAME = "<test-user>"
$env:BDBD_MARIADB_IT_PASSWORD = "<test-password>"
./gradlew.bat test --tests bdbe.bdbd.reservation.ReservationConcurrencyMariaDbTest
```

이 profile을 공유 DB나 운영 DB에 연결하지 않는다.

## 테스트 구조와 시나리오

MariaDB 테스트 클래스에는 `@Transactional`이 없다. `@BeforeEach` fixture insert는 각 케이스 전에 커밋된다. 경합 요청은 각각 `TransactionTemplate` 안에서 실행하여 thread별 트랜잭션 연결을 사용한다. 테스트는 MariaDB `CONNECTION_ID()`를 기록하고 두 연결이 서로 다른 ID인지 단언한다. 한 테스트 메서드에서 직접 `ReservationService.save` 경로와 `LocalPaymentService` 승인 경로를 모두 다루며, 현재 클래스는 6개의 JUnit test node다.

같은 Bay 예약 경합 테스트는 첫 요청을 mock `RestTemplate` 응답에서 대기시키고 두 번째 요청을 시작한다. 첫 트랜잭션이 잠금을 유지하는 동안 두 번째 연결의 `SELECT ... FOR UPDATE`가 MariaDB process list에서 실행 중인지 확인한다. 이때 결제 stub이 한 번만 호출됐는지 확인하고 첫 요청을 풀어 준다. 첫 요청은 커밋되고 두 번째 요청은 중복 검증에서 실패해야 한다.

해당 MariaDB 실행에서 `INNODB_LOCK_WAITS`는 잠금 query가 관찰되는 동안에도 행을 반환하지 않았다. 그래서 해당 시스템 뷰가 행을 보고했다고 주장하지 않는다. 실제 확인 근거는 실행 중 query, 서로 다른 연결 ID, API stub 호출 순서, 커밋 후 DB 결과를 함께 사용한다.

| 시나리오 | 검증 결과 |
|---|---|
| 서로 다른 트랜잭션의 같은 Bay·겹치는 시간 | 5회 반복 통과. 매번 예약 1건만 커밋, 나머지 요청은 중복 검증 실패. 성공 결제 stub은 회당 1회 호출. |
| Bay 잠금이 잡힌 상태의 직접 `ReservationService.save` | 수정 후 통과. 별도 수정 전 실험에서는 겹치는 예약 행 2건을 재현. |
| 같은 Bay 동시 로컬 결제 승인 | 통과. 두 번째 트랜잭션은 일반 Bay/Carwash 조회 전에 잠금 대기 후 첫 커밋을 반영해 거절. |
| 같은 Bay에서 맞닿는 시간 구간 | 통과. 두 예약 모두 저장. |
| 서로 다른 Bay에서 겹치는 시간 | 통과. 첫 Bay 잠금이 유지되는 동안 두 번째 Bay 요청 완료. |
| 중복 예약 검증 실패 | 통과. 기존 행 유지, 결제 stub 미호출. |
| 결제 stub의 비-2xx 응답 | 통과. 예약 행 미생성. |

옵트인 MariaDB suite는 총 10회 실행, 실패 0, skip 0이었다. 여기에는 같은 Bay 경합 5회가 포함된다. 기본 JUnit suite에서는 MariaDB opt-in flag가 없으므로 이 클래스의 6개 test node가 skip된다. 2026-11-19 기본 H2 실행에서는 총 92회 중 86 pass, 6 skip, 실패/오류 0이었고 Gradle build도 성공했다.

## PP-01 · MariaDB CI 연결

### workflow 검수 결과

`.github/workflows/ci.yml`의 기존 `test-and-build` job과 다음 동작은 유지했다.

```yaml
SPRING_PROFILES_ACTIVE: test
./gradlew clean test build --no-daemon
```

별도 `mariadb-concurrency` job은 GitHub-hosted `ubuntu-latest`에서 `mariadb:11.4.12` 서비스를 실행한다. 컨테이너의 3306 포트는 host 임의 포트로 매핑하고, 실제 포트는 `${{ job.services.mariadb.ports[3306] }}`에서 읽는다. 따라서 테스트 URL은 `127.0.0.1`의 비기본 포트를 사용한다. 스키마는 `ppodeuk_mariadb_it`, 사용자는 `bdbd_ci`로 제한했다. DB 사용자 비밀번호는 repository secret `PP01_MARIADB_PASSWORD`, root 비밀번호는 별도 secret `PP01_MARIADB_ROOT_PASSWORD`를 사용한다. 사용자 비밀번호는 서비스 컨테이너와 테스트 step에서 동일하게 전달한다. 두 secret이 비어 있거나 실제 값이 서로 같으면 MariaDB 테스트 전에 job이 실패한다. GitHub Actions가 컨테이너 초기화 로그의 secret 값을 자동 마스킹하며, 테스트 step 전에 두 비밀번호와 JDBC URL도 `add-mask`로 등록한다. Fork에서 시작된 외부 PR에는 repository secrets가 제공되지 않으므로 MariaDB job은 통과하지 않고 실패한다. 실제 운영 자격증명, 운영 DB, 실제 결제 API는 사용하지 않는다.

서비스 health check는 MariaDB의 `healthcheck.sh --connect --innodb_initialized`를 이용한다. readiness가 충족되지 않으면 job은 테스트 전에 실패한다. Java 17과 저장소의 Gradle wrapper를 사용한다. 먼저 `dependencyInsight`로 테스트 runtime classpath의 MariaDB JDBC driver 버전을 로그에 남기고, 이후 다음 클래스만 명시 실행한다.

```text
bdbe.bdbd.reservation.ReservationConcurrencyMariaDbTest
```

후속 Python 검사 step은 해당 클래스의 JUnit XML을 찾아 현재 기준 테스트 실행 수 10회 미만, skip, failure 또는 error가 있으면 job을 실패시킨다. 이 10회는 클래스에 있는 `@RepeatedTest(5)` 5회와 나머지 `@Test` 5개의 JUnit invocation 수다. 이후 시나리오를 추가하면 검사 기준도 함께 조정한다. Gradle 테스트 step 자체가 실패해도 후속 검사를 수행한다. GitHub Actions workflow YAML은 로컬 `js-yaml` parser로 읽었고, 서비스 이미지·job·port context 구문이 파싱됨을 확인했다.

### 2026-11-19 로컬 검증

로컬 검증과 원격 Actions 검증 결과를 혼합하지 않는다.

| 검증 종류 | 환경 / 명령 | 결과 |
|---|---|---|
| 기본 H2 suite 및 build (PR 준비 재검증) | Java 17, Gradle 8.2.1. `./gradlew.bat test build --no-daemon --rerun-tasks` | 성공. JUnit XML 18 suites, 총 92회: 86 pass, 6 skip, failure/error 0. 6 skip은 opt-in MariaDB 테스트이며 flag가 없는 기본 H2 실행에서 예상된 결과다. 기존 build 산출물을 보존하려고 로컬에서는 `clean`을 실행하지 않았다. |
| MariaDB 통합 suite (최초 PP-01 확인) | 폐기 가능한 로컬 MariaDB 11.4.12, InnoDB, `REPEATABLE-READ`, loopback 임의 포트 63123, 별도 임시 data directory/schema/test 계정. Java 17, Gradle 8.2.1, MariaDB JDBC driver 3.0.9 | `./gradlew.bat test --tests bdbe.bdbd.reservation.ReservationConcurrencyMariaDbTest --no-daemon --rerun-tasks` 성공. JUnit XML 10회 실행, skip/failure/error 0. |
| MariaDB 통합 suite (PR 준비 재검증) | 폐기 가능한 로컬 MariaDB 11.4.12, InnoDB, `REPEATABLE-READ`, loopback 임의 포트 52681, 별도 임시 data directory/schema/test 계정. Java 17, Gradle 8.2.1, MariaDB JDBC driver 3.0.9 | `./gradlew.bat test --tests bdbe.bdbd.reservation.ReservationConcurrencyMariaDbTest --no-daemon --rerun-tasks` 성공. JUnit XML 10회 실행, skip/failure/error 0. |
| MariaDB 통합 suite (로그 마스킹 보완 후 재검증) | 새 폐기 가능 로컬 MariaDB 11.4.12, InnoDB, `REPEATABLE-READ`, loopback 임의 포트 52682, 별도 임시 data directory/schema/test 계정. Java 17, Gradle 8.2.1, MariaDB JDBC driver 3.0.9 | `./gradlew.bat test --tests bdbe.bdbd.reservation.ReservationConcurrencyMariaDbTest --no-daemon --rerun-tasks` 성공. JUnit XML 10회 실행, skip/failure/error 0. |
| workflow YAML 구문 | `js-yaml`로 `.github/workflows/ci.yml` 파싱 | 성공. job 2개와 MariaDB 이미지 `mariadb:11.4.12` 확인. |
| 공백 검사 | `git diff --check` | 통과. |

로컬 MariaDB 10회는 같은 Bay 겹침 5회 반복 외에 직접 저장 및 로컬 결제 경합, 맞닿는 시간 허용, 서로 다른 Bay 독립성, validation 실패, 결제 stub 실패를 포함한다. `RestTemplate`은 mock이며 실제 PG/AWS/외부 API를 호출하지 않았다. 두 로컬 MariaDB 프로세스는 각각 실행 종료 뒤 중지했다. 최초 검증 자료는 `_audit-finalize/test-instances/ppodeuk-pp01-adee5d00a4694a07a7a5b933f5e973c6/`, PR 준비 재검증 자료는 `_audit-finalize/test-instances/ppodeuk-pp01-pr-final-20261119/`에 저장했다. 기존 감사 로그를 덮어쓰지 않았다.

### 원격 GitHub Actions 검증

PR [#1](https://github.com/minjuko/ppodeuk-backend/pull/1)의 최초 Actions 실행 [#44442758166](https://github.com/minjuko/ppodeuk-backend/actions/runs/44442758166)에서는 두 job이 성공했고 MariaDB JUnit XML은 10회 실행, skip/failure/error 0이었다. 다만 로그 검토에서 test step의 환경 변수 출력에 테스트용 비밀번호와 JDBC URL이 노출된 점을 발견했다. credentials를 `secrets.GITHUB_TOKEN`으로 바꾸고 테스트 step 전에 비밀번호와 JDBC URL을 마스킹하도록 수정했다. 보완 후 실행 [#44443888009](https://github.com/minjuko/ppodeuk-backend/actions/runs/44443888009)에서도 `test-and-build`와 `mariadb-concurrency` 두 job이 성공했다. MariaDB JUnit XML은 `tests=10, skipped=0, failures=0, errors=0`이었고, 로그에서 두 값은 모두 `***`로 마스킹되어 원문이 나타나지 않는 것을 확인했다.
후속 보안 변경에서는 DB 인증에 GitHub API 권한이 포함된 `GITHUB_TOKEN`을 재사용하지 않고 위 두 전용 repository secret으로 분리했다. PR 보안 변경 실행 [#44446746576](https://github.com/minjuko/ppodeuk-backend/actions/runs/44446746576)에서 두 job이 성공했고, MariaDB XML은 `tests=10, skipped=0, failures=0, errors=0`이었다. Docker 초기화 로그의 두 비밀번호와 테스트 로그의 JDBC URL은 모두 `***`로 마스킹되어 원문이 출력되지 않았다.

- `test-and-build`: 기존 H2 테스트 및 Gradle build.
- `mariadb-concurrency`: 서비스 readiness, MariaDB 전용 테스트 실행, JUnit XML의 `tests > 0`, `skipped = 0`, `failures = 0`, `errors = 0`.

로컬 통과는 원격 Actions 통과를 대신하지 않는다. workflow 변경을 push할 때마다 PR의 두 job 및 자격 증명 로그 마스킹을 확인한다. 자동 merge하지 않는다.

## 검증 한계 및 위험

- 테스트 DB는 폐기 가능한 MariaDB 11.4.12이며 운영 DB가 아니다. 운영 MariaDB 버전과 transaction 설정을 대조하기 전에는 운영과 동일하다고 주장하지 않는다.
- 실제 KakaoPay 요청·결제 승인 취소는 수행하지 않았다. `RestTemplate`은 mock이며 이 테스트는 해당 mock이 같은 Bay 경합에서 승자 요청에만 호출되는 흐름을 확인한다.
- 운영 결제 승인 요청이 처리되는 동안 DB transaction이 열린 채 유지된다. 현재 직렬화 방식을 보존하지만 provider 지연 시 Bay 잠금도 오래 유지될 수 있다.
- 외부 결제 승인 성공과 뒤이은 DB rollback은 서로 다른 시스템에서 일어난다. 현재 순서상 provider 승인 후 예약을 저장하므로, 저장 뒤 DB 오류가 나면 row는 rollback될 수 있어도 이미 승인된 결제를 자동 취소할 수 없다. 보상/취소 처리는 이번 검증에서 구현하거나 확인하지 않았다.
- `ReservationService.update`는 Bay lock 보장 범위가 아니며 동시 update 테스트도 없다.
- 중복 규칙은 동일 사용자가 서로 다른 Bay를 동시에 예약하는 경우도 거부한다. 서로 다른 Bay 독립성 테스트는 다른 사용자 두 명을 사용했다. 같은 사용자의 서로 다른 Bay 동시 요청은 Bay 잠금만으로 직렬화되지 않았고 테스트하지 않았다.
- 다중 프로세스 배포나 운영 부하 테스트는 하지 않았다. InnoDB lock은 DB 수준이지만 이 suite는 격리 로컬 인스턴스와 명시한 요청 경로만 검증한다.
- GitHub Actions 원격 결과는 PR 생성 후 기록한다. 원격 job을 확인하기 전에는 PP-01 완료로 판정하지 않는다.

## 팀 기여와 개인 후속 작업

이 서비스는 팀 프로젝트다. 프로젝트 진행 당시 사용자 기여는 frontend 작업이었다. 동시성 감사, MariaDB 재현, Bay 잠금 보완, 본 문서의 후속 검증은 프로젝트 종료 뒤 개인적으로 진행한 backend 작업이다. checkout 당시 운영 승인 경로의 잠금 및 Mockito 호출 순서 테스트(`10f2e97`, 2026-10-18)는 이미 존재했다. 이 후속 작업을 팀 구현 기여로 돌리지 않는다.

## 포트폴리오 활용 문구

### 포트폴리오 (2–3문장)

팀 프로젝트에서는 프론트엔드 작업을 담당했고, 프로젝트 종료 후 예약 정합성 검증과 백엔드 보완을 개인적으로 진행했습니다. 기존 결제 승인 경로에만 있던 Bay 비관적 잠금을 예약 저장 경로에도 적용해, 격리된 MariaDB의 독립 트랜잭션 경합에서 중복 예약 두 건이 저장되던 문제를 막았습니다. 동일 Bay 경합 5회 반복과 인접 시간·다른 Bay·실패 시나리오를 포함한 MariaDB 테스트 10회 실행으로 확인했습니다.

### 이력서 (한 문장)

팀 프로젝트 종료 후 개인 후속 작업으로 Spring Boot/JPA 예약 저장 경로에 MariaDB `PESSIMISTIC_WRITE` Bay 잠금을 적용하고, 독립 커넥션 기반 동시성·시간 경계·잠금 범위·실패 시나리오를 통합 테스트 10회로 검증했습니다.

### 기술 면접 답변 (약 1분)

“팀 프로젝트에서 저는 프론트엔드 작업을 맡았습니다. 프로젝트 종료 후 백엔드 예약 동시성을 개인적으로 검토했습니다. 운영 결제 승인 경로는 결제사 호출 전에 Bay를 잠갔지만, 예약 저장 메서드 자체는 중복 조회 전에 잠금을 잡지 않아 그 경로를 거치지 않는 저장 요청은 경쟁할 수 있었습니다. 격리된 MariaDB에서 독립된 두 커넥션으로 중복 예약 두 건이 저장되는 상황을 재현했습니다. 이후 트랜잭션 저장 메서드가 중복을 검사하기 전에 같은 Bay 행을 비관적 잠금으로 잡도록 보완했습니다. 첫 요청을 결제 stub에서 멈춰 둔 채 두 번째 커넥션의 `FOR UPDATE` 쿼리가 진행 중인 것을 확인했고, 첫 요청이 한 번만 승인 stub을 호출하고 예약 한 건을 커밋한 뒤 두 번째 요청은 중복으로 거절되는 순서를 검증했습니다. 이 경쟁을 다섯 번 반복했고 인접 시간, 다른 Bay의 동시 처리, 검증 및 결제 stub 실패도 확인했습니다. 실제 결제사나 운영 DB는 사용하지 않았습니다.”
