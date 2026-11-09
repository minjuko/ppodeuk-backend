package bdbe.bdbd.reservation;

import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.utils.ApiUtils;
import bdbe.bdbd.dto.pay.PayRequest;
import bdbe.bdbd.dto.pay.PayResponse;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.service.pay.LocalPaymentService;
import bdbe.bdbd.service.pay.PayService;
import bdbe.bdbd.service.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.status;

@SpringBootTest
@ActiveProfiles("mariadb-it")
@Import(ReservationConcurrencyMariaDbTest.PaymentTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "BDBD_MARIADB_IT_ENABLED", matches = "true")
class ReservationConcurrencyMariaDbTest {

    static {
        if ("true".equalsIgnoreCase(System.getenv("BDBD_MARIADB_IT_ENABLED"))) {
            String url = System.getenv("BDBD_MARIADB_IT_URL");
            if (url == null || !url.matches("jdbc:mariadb://127\\.0\\.0\\.1:(\\d+)/ppodeuk_mariadb_it(?:\\?.*)?")) {
                throw new IllegalStateException(
                        "MariaDB concurrency tests require the dedicated loopback schema ppodeuk_mariadb_it.");
            }
            int port = Integer.parseInt(url.replaceFirst("^jdbc:mariadb://127\\.0\\.0\\.1:(\\d+)/.*$", "$1"));
            if (port == 3306 || port < 1024 || port > 65535) {
                throw new IllegalStateException("MariaDB concurrency tests require a non-default isolated port.");
            }
        }
    }

    private static final long CARWASH_ID = 1L;
    private static final long FIRST_BAY_ID = 1L;
    private static final long SECOND_BAY_ID = 2L;
    private static final long FIRST_MEMBER_ID = 1L;
    private static final long SECOND_MEMBER_ID = 2L;

    @Autowired
    private PayService payService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BayJPARepository bayRepository;

    @Autowired
    private CarwashJPARepository carwashRepository;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private LocalPaymentService localPaymentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private RestTemplate restTemplate;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void prepareCommittedFixture() {
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM bay");
        jdbcTemplate.update("DELETE FROM optime");
        jdbcTemplate.update("DELETE FROM carwash");
        jdbcTemplate.update("DELETE FROM location");
        jdbcTemplate.update("DELETE FROM member");

        jdbcTemplate.update("INSERT INTO member (id, email, username, password, tel, role) VALUES " +
                        "(1, 'first@test.invalid', 'first', 'placeholder', '010-0000-0001', 'ROLE_USER')," +
                        "(2, 'second@test.invalid', 'second', 'placeholder', '010-0000-0002', 'ROLE_USER')");
        jdbcTemplate.update("INSERT INTO location (id, address, latitude, longitude) VALUES " +
                "(1, 'Isolated MariaDB fixture', 37.0, 127.0)");
        jdbcTemplate.update("INSERT INTO carwash (id, name, rate, tel, des, price, l_id, m_id) VALUES " +
                "(1, 'Concurrency fixture', 0, '02-0000-0001', 'Test only', 10000, 1, 1)");
        jdbcTemplate.update("INSERT INTO optime (id, day_type, start_time, end_time, c_id) VALUES " +
                "(1, 'WEEKDAY', '00:00:00', '23:59:59', 1)," +
                "(2, 'WEEKEND', '00:00:00', '23:59:59', 1)");
        jdbcTemplate.update("INSERT INTO bay (id, bay_num, w_id, status) VALUES " +
                "(1, 1, 1, 1), (2, 2, 1, 1)");
        transactionTemplate = new TransactionTemplate(transactionManager);
        reset(restTemplate);
    }

    @RepeatedTest(5)
    void overlappingApprovalsOnOneBaySerializeBeforePaymentAndCommitOnlyOneReservation() throws Exception {
        CountDownLatch firstApprovalEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstApproval = new CountDownLatch(1);
        CountDownLatch secondLockAttempted = new CountDownLatch(1);
        AtomicInteger approvalStubs = new AtomicInteger();
        Set<Long> connectionIds = ConcurrentHashMap.newKeySet();

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenAnswer(invocation -> {
            approvalStubs.incrementAndGet();
            firstApprovalEntered.countDown();
            if (!releaseFirstApproval.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to release the test payment stub.");
            }
            return status(OK).body("{}");
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            LocalDateTime start = LocalDateTime.of(2027, 8, 11, 10, 0);
            Future<?> winner = executor.submit(() -> approveInTransaction(FIRST_BAY_ID, FIRST_MEMBER_ID,
                    start, start.plusMinutes(60), connectionIds, null));

            assertThat(firstApprovalEntered.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> loser = executor.submit(() -> approveInTransaction(FIRST_BAY_ID, SECOND_MEMBER_ID,
                    start.plusMinutes(30), start.plusMinutes(90), connectionIds, secondLockAttempted));

            assertThat(secondLockAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            awaitCompetingBayLockQuery(approvalStubs, connectionIds);
            releaseFirstApproval.countDown();

            winner.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> loser.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(BadRequestError.class);
        } finally {
            releaseFirstApproval.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(connectionIds).hasSize(2);
        assertThat(approvalStubs).hasValue(1);
        assertThat(countReservations(FIRST_BAY_ID)).isEqualTo(1);
    }

    @Test
    void adjacentTimeOnSameBayIsAllowed() {
        LocalDateTime start = LocalDateTime.of(2027, 8, 11, 10, 0);
        successfulStub();

        approve(FIRST_BAY_ID, FIRST_MEMBER_ID, start, start.plusMinutes(60));
        approve(FIRST_BAY_ID, SECOND_MEMBER_ID, start.plusMinutes(60), start.plusMinutes(90));

        assertThat(countReservations(FIRST_BAY_ID)).isEqualTo(2);
    }

    @Test
    void differentBayApprovalCanFinishWhileAnotherBayIsLocked() throws Exception {
        LocalDateTime start = LocalDateTime.of(2027, 8, 11, 10, 0);
        CountDownLatch firstApprovalEntered = new CountDownLatch(1);
        CountDownLatch secondApprovalEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstApproval = new CountDownLatch(1);
        AtomicInteger approvalStubs = new AtomicInteger();
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenAnswer(invocation -> {
            if (approvalStubs.incrementAndGet() == 1) {
                firstApprovalEntered.countDown();
                if (!releaseFirstApproval.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release the test payment stub.");
                }
            } else {
                secondApprovalEntered.countDown();
            }
            return status(OK).body("{}");
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> approve(FIRST_BAY_ID, FIRST_MEMBER_ID,
                    start, start.plusMinutes(60)));
            assertThat(firstApprovalEntered.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> approve(SECOND_BAY_ID, SECOND_MEMBER_ID,
                    start, start.plusMinutes(60)));
            assertThat(secondApprovalEntered.await(5, TimeUnit.SECONDS)).isTrue();
            second.get(5, TimeUnit.SECONDS);
            assertThat(countReservations(SECOND_BAY_ID)).isEqualTo(1);

            releaseFirstApproval.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            releaseFirstApproval.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(countReservations(FIRST_BAY_ID)).isEqualTo(1);
        assertThat(countReservations(SECOND_BAY_ID)).isEqualTo(1);
    }

    @Test
    void validationFailureLeavesNoReservationAndDoesNotCallPaymentApproval() {
        LocalDateTime start = LocalDateTime.of(2027, 8, 11, 10, 0);
        jdbcTemplate.update("INSERT INTO reservation " +
                        "(price, start_time, end_time, is_deleted, created_at, updated_at, b_id, m_id) " +
                        "VALUES (20000, ?, ?, false, NOW(), NOW(), ?, ?)",
                start, start.plusMinutes(60), FIRST_BAY_ID, FIRST_MEMBER_ID);

        assertThatThrownBy(() -> approve(FIRST_BAY_ID, SECOND_MEMBER_ID,
                start.plusMinutes(30), start.plusMinutes(90)))
                .isInstanceOf(BadRequestError.class);

        assertThat(countReservations(FIRST_BAY_ID)).isEqualTo(1);
        org.mockito.Mockito.verifyNoInteractions(restTemplate);
    }

    @Test
    void unsuccessfulPaymentApprovalLeavesNoReservation() {
        LocalDateTime start = LocalDateTime.of(2027, 8, 11, 10, 0);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(status(org.springframework.http.HttpStatus.BAD_GATEWAY).body("stub failure"));

        assertThatThrownBy(() -> approve(FIRST_BAY_ID, FIRST_MEMBER_ID,
                start, start.plusMinutes(60)))
                .isInstanceOf(BadRequestError.class);

        assertThat(countReservations(FIRST_BAY_ID)).isZero();
    }

    @Test
    void directReservationSaveSerializesConcurrentRequestsOnTheBay() throws Exception {
        CountDownLatch firstBayLocked = new CountDownLatch(1);
        CountDownLatch firstReservationCreated = new CountDownLatch(1);
        CountDownLatch secondSaveStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstCommit = new CountDownLatch(1);
        Set<Long> connectionIds = ConcurrentHashMap.newKeySet();
        LocalDateTime start = LocalDateTime.of(2027, 8, 11, 14, 0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> transactionTemplate.execute(status -> {
                connectionIds.add(jdbcTemplate.queryForObject("SELECT CONNECTION_ID()", Long.class));
                bayRepository.findByIdForUpdate(FIRST_BAY_ID).orElseThrow();
                firstBayLocked.countDown();
                reservationService.save(saveRequest(FIRST_BAY_ID, start, start.plusMinutes(60)),
                        CARWASH_ID, FIRST_BAY_ID, Member.builder().id(FIRST_MEMBER_ID).build());
                firstReservationCreated.countDown();
                awaitRelease(releaseFirstCommit);
                return null;
            }));

            assertThat(firstBayLocked.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(firstReservationCreated.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> transactionTemplate.execute(status -> {
                connectionIds.add(jdbcTemplate.queryForObject("SELECT CONNECTION_ID()", Long.class));
                secondSaveStarted.countDown();
                reservationService.save(saveRequest(FIRST_BAY_ID, start.plusMinutes(30),
                                start.plusMinutes(90)),
                        CARWASH_ID, FIRST_BAY_ID, Member.builder().id(SECOND_MEMBER_ID).build());
                return null;
            }));

            assertThat(secondSaveStarted.await(5, TimeUnit.SECONDS)).isTrue();
            boolean lockQueryVisible = awaitLockQueryOrCompletion(connectionIds, second, 5);
            boolean secondFinishedBeforeFirstCommit = second.isDone();
            releaseFirstCommit.countDown();
            first.get(10, TimeUnit.SECONDS);

            ExecutionException secondFailure = null;
            try {
                second.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                secondFailure = e;
            }
            assertThat(connectionIds).hasSize(2);
            assertThat(countReservations(FIRST_BAY_ID)).isEqualTo(1);
            assertThat(lockQueryVisible)
                    .as("the direct reservation creation path must request a Bay row lock")
                    .isTrue();
            assertThat(secondFinishedBeforeFirstCommit).isFalse();
            assertThat(secondFailure).isNotNull().hasCauseInstanceOf(BadRequestError.class);
        } finally {
            releaseFirstCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertLocalPaymentPathSerializes(start.plusDays(1));
    }

    private void assertLocalPaymentPathSerializes(LocalDateTime start) throws Exception {
        LocalPaymentApproval firstRequest = prepareLocalPaymentApproval(
                FIRST_MEMBER_ID, SECOND_BAY_ID, start, start.plusMinutes(60));
        LocalPaymentApproval secondRequest = prepareLocalPaymentApproval(
                SECOND_MEMBER_ID, SECOND_BAY_ID, start.plusMinutes(30), start.plusMinutes(90));

        CountDownLatch firstReservationCreated = new CountDownLatch(1);
        CountDownLatch secondApprovalStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstCommit = new CountDownLatch(1);
        Set<Long> connectionIds = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> transactionTemplate.execute(status -> {
                connectionIds.add(jdbcTemplate.queryForObject("SELECT CONNECTION_ID()", Long.class));
                bayRepository.findByIdForUpdate(SECOND_BAY_ID).orElseThrow();
                approveLocally(firstRequest);
                firstReservationCreated.countDown();
                awaitRelease(releaseFirstCommit);
                return null;
            }));

            assertThat(firstReservationCreated.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> transactionTemplate.execute(status -> {
                connectionIds.add(jdbcTemplate.queryForObject("SELECT CONNECTION_ID()", Long.class));
                secondApprovalStarted.countDown();
                approveLocally(secondRequest);
                return null;
            }));

            assertThat(secondApprovalStarted.await(5, TimeUnit.SECONDS)).isTrue();
            boolean lockQueryVisible = awaitLockQueryOrCompletion(connectionIds, second, 5);
            boolean secondFinishedBeforeFirstCommit = second.isDone();
            releaseFirstCommit.countDown();
            first.get(10, TimeUnit.SECONDS);

            ExecutionException secondFailure = null;
            try {
                second.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                secondFailure = e;
            }

            assertThat(connectionIds).hasSize(2);
            assertThat(countReservations(SECOND_BAY_ID)).isEqualTo(1);
            assertThat(lockQueryVisible)
                    .as("local approval must lock the Bay before its ordinary Bay and Carwash reads")
                    .isTrue();
            assertThat(secondFinishedBeforeFirstCommit).isFalse();
            assertThat(secondFailure).isNotNull().hasCauseInstanceOf(BadRequestError.class);
        } finally {
            releaseFirstCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private LocalPaymentApproval prepareLocalPaymentApproval(long memberId, long bayId,
                                                              LocalDateTime start, LocalDateTime end) {
        Member member = Member.builder().id(memberId).build();
        ReservationRequest.SaveDTO save = saveRequest(bayId, start, end);
        PayRequest.PayReadyRequestDTO readyRequest = new PayRequest.PayReadyRequestDTO();
        readyRequest.setCid("local-test-cid");
        readyRequest.setPartner_order_id("local-test-order-" + memberId);
        readyRequest.setPartner_user_id("local-test-user-" + memberId);
        readyRequest.setItem_name("Test reservation");
        readyRequest.setQuantity(1);
        readyRequest.setTotal_amount(20000);
        readyRequest.setTax_free_amount(0);

        ResponseEntity<?> response = localPaymentService.requestPaymentReady(readyRequest, save, member);
        ApiUtils.ApiResult<?> apiResult = (ApiUtils.ApiResult<?>) response.getBody();
        PayResponse.PayReadyResponseDTO readyResponse =
                (PayResponse.PayReadyResponseDTO) apiResult.getResponse();
        String pgToken = URI.create(readyResponse.getNext_redirect_app_url())
                .getQuery().substring("pg_token=".length());

        PayRequest.PayApprovalRequestDTO approvalRequest = new PayRequest.PayApprovalRequestDTO();
        approvalRequest.setCid("local-test-cid");
        approvalRequest.setTid(readyResponse.getTid());
        approvalRequest.setPartner_order_id("local-test-order-" + memberId);
        approvalRequest.setPartner_user_id("local-test-user-" + memberId);
        approvalRequest.setPg_token(pgToken);
        return new LocalPaymentApproval(approvalRequest, save, member);
    }

    private void approveLocally(LocalPaymentApproval request) {
        localPaymentService.requestPaymentApproval(
                request.approvalRequest, request.saveRequest.getBayId(), request.member, request.saveRequest);
    }

    private void awaitCompetingBayLockQuery(AtomicInteger approvalStubs, Set<Long> connectionIds) {
        if (!awaitPendingBayLockQuery(connectionIds, 5, approvalStubs)) {
            throw new AssertionError("MariaDB did not keep the competing SELECT ... FOR UPDATE visible while the first request held the Bay.");
        }
    }

    private boolean awaitPendingBayLockQuery(Set<Long> connectionIds, long timeoutSeconds) {
        return awaitPendingBayLockQuery(connectionIds, timeoutSeconds, null);
    }

    private boolean awaitLockQueryOrCompletion(Set<Long> connectionIds, Future<?> competingRequest,
                                               long timeoutSeconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (competingRequest.isDone()) {
                return false;
            }
            if (hasPendingBayLockQuery(connectionIds)) {
                return true;
            }
            Thread.yield();
        }
        return false;
    }

    private boolean awaitPendingBayLockQuery(Set<Long> connectionIds, long timeoutSeconds,
                                             AtomicInteger approvalStubs) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (approvalStubs != null && approvalStubs.get() > 1) {
                throw new AssertionError("The competing request reached the payment stub before the first transaction committed.");
            }
            if (hasPendingBayLockQuery(connectionIds)) {
                return true;
            }
            Thread.yield();
        }
        return false;
    }

    private boolean hasPendingBayLockQuery(Set<Long> connectionIds) {
        if (connectionIds.isEmpty()) {
            return false;
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(connectionIds.size(), "?"));
        Integer pendingLocks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE id IN (" + placeholders + ") " +
                        "AND LOWER(COALESCE(info, '')) LIKE '%for update%'",
                Integer.class,
                connectionIds.toArray());
        return pendingLocks != null && pendingLocks > 0;
    }

    private void awaitRelease(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to release the first test transaction.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to release the first transaction.", e);
        }
    }

    private ReservationRequest.SaveDTO saveRequest(long bayId, LocalDateTime start, LocalDateTime end) {
        ReservationRequest.SaveDTO save = new ReservationRequest.SaveDTO();
        save.setBayId(bayId);
        save.setStartTime(start);
        save.setEndTime(end);
        return save;
    }

    private void successfulStub() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(status(OK).body("{}"));
    }

    private void approve(long bayId, long memberId, LocalDateTime start, LocalDateTime end) {
        approveInTransaction(bayId, memberId, start, end, null, null);
    }

    private void approveInTransaction(long bayId, long memberId, LocalDateTime start, LocalDateTime end,
                                     Set<Long> connectionIds, CountDownLatch lockAttempted) {
        transactionTemplate.execute(status -> {
            if (connectionIds != null) {
                connectionIds.add(jdbcTemplate.queryForObject("SELECT CONNECTION_ID()", Long.class));
            }
            if (lockAttempted != null) {
                lockAttempted.countDown();
            }
            Member member = Member.builder().id(memberId).build();
            ReservationRequest.SaveDTO save = new ReservationRequest.SaveDTO();
            save.setBayId(bayId);
            save.setStartTime(start);
            save.setEndTime(end);

            PayRequest.PayApprovalRequestDTO request = new PayRequest.PayApprovalRequestDTO();
            request.setCid("test-cid");
            request.setTid("test-tid-" + memberId + "-" + start);
            request.setPartner_order_id("test-order");
            request.setPartner_user_id("test-user-" + memberId);
            request.setPg_token("test-token");

            payService.requestPaymentApproval(request, bayId, member, save);
            return null;
        });
    }

    private int countReservations(long bayId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation WHERE b_id = ? AND is_deleted = false", Integer.class, bayId);
    }

    @TestConfiguration
    static class PaymentTestConfiguration {
        @Bean
        @Primary
        PayService testPayService(
                ReservationService reservationService,
                RestTemplate restTemplate,
                CarwashJPARepository carwashRepository,
                BayJPARepository bayRepository) {
            PayService service = new PayService(reservationService, restTemplate, carwashRepository, bayRepository);
            ReflectionTestUtils.setField(service, "externalPaymentEnabled", true);
            ReflectionTestUtils.setField(service, "adminKey", "test-placeholder-kakao-admin-key");
            return service;
        }

        @Bean
        LocalPaymentService testLocalPaymentService(
                ReservationService reservationService,
                BayJPARepository bayRepository,
                CarwashJPARepository carwashRepository) {
            return new LocalPaymentService(
                    reservationService, bayRepository, carwashRepository, false,
                    "http://localhost.test/paymentwaiting");
        }
    }

    private static class LocalPaymentApproval {
        private final PayRequest.PayApprovalRequestDTO approvalRequest;
        private final ReservationRequest.SaveDTO saveRequest;
        private final Member member;

        private LocalPaymentApproval(PayRequest.PayApprovalRequestDTO approvalRequest,
                                     ReservationRequest.SaveDTO saveRequest,
                                     Member member) {
            this.approvalRequest = approvalRequest;
            this.saveRequest = saveRequest;
            this.member = member;
        }
    }
}
