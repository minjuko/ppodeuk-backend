package bdbe.bdbd.service.pay;

import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.utils.ApiUtils;
import bdbe.bdbd.dto.pay.PayRequest;
import bdbe.bdbd.dto.pay.PayResponse;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.reservation.Reservation;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.service.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class LocalPaymentServiceTest {

    private static final Long BAY_ID = 1001L;
    private static final Long CARWASH_ID = 1001L;
    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 13, 11, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 13, 12, 0);

    private ReservationService reservationService;
    private BayJPARepository bayRepository;
    private CarwashJPARepository carwashRepository;
    private MutableClock clock;
    private LocalPaymentService service;
    private Member member;
    private ReservationResponse.findLatestOneResponseDTO approvalResponse;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        bayRepository = mock(BayJPARepository.class);
        carwashRepository = mock(CarwashJPARepository.class);
        clock = new MutableClock(Instant.parse("2026-08-12T00:00:00Z"));
        service = new LocalPaymentService(
                reservationService,
                bayRepository,
                carwashRepository,
                false,
                "http://localhost:5173/paymentwaiting",
                new SecureRandom(),
                clock);
        member = Member.builder().id(102L).email("test-user@example.com").build();

        Carwash carwash = Carwash.builder().id(CARWASH_ID).name("Portfolio Day Carwash").build();
        Bay bay = Bay.builder().id(BAY_ID).bayNum(1).carwash(carwash).status(1).build();
        given(bayRepository.findById(BAY_ID)).willReturn(Optional.of(bay));
        given(carwashRepository.findById(CARWASH_ID)).willReturn(Optional.of(carwash));
        given(reservationService.findPayAmount(any(), eq(BAY_ID), eq(member)))
                .willReturn(new ReservationResponse.PayAmountDTO(START, END, 12000));

        Reservation reservation = Reservation.builder().id(2001L).build();
        given(reservationService.save(any(), eq(CARWASH_ID), eq(BAY_ID), eq(member)))
                .willReturn(reservation);
        approvalResponse = mock(ReservationResponse.findLatestOneResponseDTO.class);
        given(reservationService.fetchLatestReservation(2001L)).willReturn(approvalResponse);
    }

    @Test
    void readyCreatesUniqueFakeTidAndLocalCallbackWithoutExternalHttp() {
        ReadyContext ready = ready(member, BAY_ID, START, END, 12000);
        ReadyContext second = ready(member, BAY_ID, START, END, 12000);

        assertThat(ready.tid).startsWith("local_tid_").isNotEqualTo(second.tid);
        assertThat(ready.pgToken).isNotBlank();
        assertThat(ready.redirectUrl)
                .startsWith("http://localhost:5173/paymentwaiting?pg_token=")
                .doesNotContain("127.0.0.1");
        verify(reservationService, never()).save(any(), any(), any(), any());
    }

    @Test
    void readyRejectsClientAmountMismatch() {
        assertThatThrownBy(() -> ready(member, BAY_ID, START, END, 1))
                .isInstanceOf(BadRequestError.class)
                .satisfies(error -> assertThat(((BadRequestError) error).getErrors())
                        .containsEntry("pay", "Invalid pay amount"));
    }

    @Test
    void readyPropagatesOperatingHoursAndOverlapValidation() {
        BadRequestError outside = validation("operatingHours", "out");
        given(reservationService.findPayAmount(any(), eq(BAY_ID), eq(member))).willThrow(outside);
        assertThatThrownBy(() -> ready(member, BAY_ID, START, END, 12000)).isSameAs(outside);

        BadRequestError overlap = validation("Reservation time", "overlap");
        given(reservationService.findPayAmount(any(), eq(BAY_ID), eq(member))).willThrow(overlap);
        assertThatThrownBy(() -> ready(member, BAY_ID, START, END, 12000)).isSameAs(overlap);
    }

    @Test
    void approveRejectsInvalidTidAndToken() {
        ReadyContext ready = ready(member, BAY_ID, START, END, 12000);

        assertThatThrownBy(() -> approve("missing", ready.pgToken, member, BAY_ID, START, END))
                .isInstanceOf(BadRequestError.class);
        assertThatThrownBy(() -> approve(ready.tid, "wrong", member, BAY_ID, START, END))
                .isInstanceOf(BadRequestError.class);
        verify(reservationService, never()).save(any(), any(), any(), any());
    }

    @Test
    void approveRejectsDifferentMemberBayAndDateTime() {
        ReadyContext ready = ready(member, BAY_ID, START, END, 12000);
        Member anotherMember = Member.builder().id(103L).build();

        assertThatThrownBy(() -> approve(ready.tid, ready.pgToken, anotherMember, BAY_ID, START, END))
                .isInstanceOf(BadRequestError.class);
        assertThatThrownBy(() -> approve(ready.tid, ready.pgToken, member, 1002L, START, END))
                .isInstanceOf(BadRequestError.class);
        assertThatThrownBy(() -> approve(ready.tid, ready.pgToken, member, BAY_ID,
                START.plusMinutes(30), END.plusMinutes(30)))
                .isInstanceOf(BadRequestError.class);
        verify(reservationService, never()).save(any(), any(), any(), any());
    }

    @Test
    void expiredPendingPaymentIsRejected() {
        ReadyContext ready = ready(member, BAY_ID, START, END, 12000);
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> approve(ready.tid, ready.pgToken, member, BAY_ID, START, END))
                .isInstanceOf(BadRequestError.class)
                .satisfies(error -> assertThat(((BadRequestError) error).getErrors())
                        .containsKey("payment"));
        verify(reservationService, never()).save(any(), any(), any(), any());
    }

    @Test
    void successfulApproveSavesReservationReturnsProductionContractAndRejectsReplay() {
        ReadyContext ready = ready(member, BAY_ID, START, END, 12000);

        ResponseEntity<?> response = approve(ready.tid, ready.pgToken, member, BAY_ID, START, END);

        assertThat(response.getBody()).isSameAs(approvalResponse);
        verify(reservationService).save(any(), eq(CARWASH_ID), eq(BAY_ID), eq(member));
        assertThatThrownBy(() -> approve(ready.tid, ready.pgToken, member, BAY_ID, START, END))
                .isInstanceOf(BadRequestError.class);
    }

    @Test
    void failedSaveKeepsPendingPaymentForSafeRetry() {
        ReadyContext ready = ready(member, BAY_ID, START, END, 12000);
        BadRequestError overlap = validation("Reservation time", "overlap");
        given(reservationService.save(any(), eq(CARWASH_ID), eq(BAY_ID), eq(member)))
                .willThrow(overlap)
                .willReturn(Reservation.builder().id(2001L).build());

        assertThatThrownBy(() -> approve(ready.tid, ready.pgToken, member, BAY_ID, START, END))
                .isSameAs(overlap);
        ResponseEntity<?> retried = approve(ready.tid, ready.pgToken, member, BAY_ID, START, END);

        assertThat(retried.getBody()).isSameAs(approvalResponse);
    }

    @Test
    void profileAnnotationsKeepLocalAndProductionImplementationsSeparate() {
        assertThat(LocalPaymentService.class.getAnnotation(Profile.class).value())
                .containsExactly("local", "demo", "test");
        assertThat(PayService.class.getAnnotation(Profile.class).value()).containsExactly("prod");
    }

    @Test
    void localServiceRefusesExternalPaymentEnabledConfiguration() {
        LocalPaymentService invalid = new LocalPaymentService(
                reservationService, bayRepository, carwashRepository, true,
                "http://localhost:5173/paymentwaiting", new SecureRandom(), clock);

        assertThatThrownBy(invalid::verifyLocalConfiguration)
                .isInstanceOf(IllegalStateException.class);
    }

    private ReadyContext ready(
            Member readyMember,
            Long bayId,
            LocalDateTime start,
            LocalDateTime end,
            int amount) {
        PayRequest.PayReadyRequestDTO request = new PayRequest.PayReadyRequestDTO();
        request.setTotal_amount(amount);
        ResponseEntity<?> response = service.requestPaymentReady(request, save(bayId, start, end), readyMember);
        ApiUtils.ApiResult<?> result = (ApiUtils.ApiResult<?>) response.getBody();
        PayResponse.PayReadyResponseDTO body = (PayResponse.PayReadyResponseDTO) result.getResponse();
        String redirect = body.getNext_redirect_pc_url();
        return new ReadyContext(body.getTid(), redirect.substring(redirect.indexOf("pg_token=") + 9), redirect);
    }

    private ResponseEntity<?> approve(
            String tid,
            String pgToken,
            Member approveMember,
            Long bayId,
            LocalDateTime start,
            LocalDateTime end) {
        PayRequest.PayApprovalRequestDTO request = new PayRequest.PayApprovalRequestDTO();
        request.setTid(tid);
        request.setPg_token(pgToken);
        return service.requestPaymentApproval(request, bayId, approveMember, save(bayId, start, end));
    }

    private ReservationRequest.SaveDTO save(Long bayId, LocalDateTime start, LocalDateTime end) {
        ReservationRequest.SaveDTO dto = new ReservationRequest.SaveDTO();
        dto.setBayId(bayId);
        dto.setStartTime(start);
        dto.setEndTime(end);
        return dto;
    }

    private BadRequestError validation(String key, String message) {
        return new BadRequestError(
                BadRequestError.ErrorCode.VALIDATION_FAILED,
                java.util.Collections.singletonMap(key, message));
    }

    private static class ReadyContext {
        private final String tid;
        private final String pgToken;
        private final String redirectUrl;

        private ReadyContext(String tid, String pgToken, String redirectUrl) {
            this.tid = tid;
            this.pgToken = pgToken;
            this.redirectUrl = redirectUrl;
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
