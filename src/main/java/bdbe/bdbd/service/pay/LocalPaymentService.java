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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("local")
public class LocalPaymentService implements PaymentFlowService {

    private static final Duration PENDING_TTL = Duration.ofMinutes(5);

    private final ReservationService reservationService;
    private final BayJPARepository bayRepository;
    private final CarwashJPARepository carwashRepository;
    private final boolean externalPaymentEnabled;
    private final String approvalUrl;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final Map<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();

    @Autowired
    public LocalPaymentService(
            ReservationService reservationService,
            BayJPARepository bayRepository,
            CarwashJPARepository carwashRepository,
            @Value("${payment.external.enabled:false}") boolean externalPaymentEnabled,
            @Value("${payment.approval-url}") String approvalUrl) {
        this(reservationService, bayRepository, carwashRepository, externalPaymentEnabled,
                approvalUrl, new SecureRandom(), Clock.systemDefaultZone());
    }

    LocalPaymentService(
            ReservationService reservationService,
            BayJPARepository bayRepository,
            CarwashJPARepository carwashRepository,
            boolean externalPaymentEnabled,
            String approvalUrl,
            SecureRandom secureRandom,
            Clock clock) {
        this.reservationService = reservationService;
        this.bayRepository = bayRepository;
        this.carwashRepository = carwashRepository;
        this.externalPaymentEnabled = externalPaymentEnabled;
        this.approvalUrl = approvalUrl;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @PostConstruct
    void verifyLocalConfiguration() {
        if (externalPaymentEnabled) {
            throw new IllegalStateException("External payment must be disabled for the local payment service.");
        }
    }

    @Override
    public ResponseEntity<?> requestPaymentReady(
            PayRequest.PayReadyRequestDTO requestDto,
            ReservationRequest.SaveDTO saveDTO,
            Member member) {
        requireLocalPaymentEnabled();
        cleanupExpired();

        Bay bay = findBay(saveDTO.getBayId());
        findCarwash(bay.getCarwash().getId());
        ReservationResponse.PayAmountDTO amount = reservationService.findPayAmount(
                toReservationTime(saveDTO), saveDTO.getBayId());

        if (requestDto.getTotal_amount() == null || amount.getPrice() != requestDto.getTotal_amount()) {
            throw invalid("pay", "Invalid pay amount");
        }

        String tid = uniqueToken("local_tid_");
        String pgToken = randomToken();
        Instant createdAt = clock.instant();
        PendingPayment pending = new PendingPayment(
                tid,
                pgToken,
                member.getId(),
                saveDTO.getBayId(),
                saveDTO.getStartTime(),
                saveDTO.getEndTime(),
                amount.getPrice(),
                createdAt,
                createdAt.plus(PENDING_TTL));
        pendingPayments.put(tid, pending);

        PayResponse.PayReadyResponseDTO response = new PayResponse.PayReadyResponseDTO();
        response.setTid(tid);
        response.setTms_result(false);
        String callback = approvalUrl + "?pg_token=" + pgToken;
        response.setNext_redirect_app_url(callback);
        response.setNext_redirect_mobile_url(callback);
        response.setNext_redirect_pc_url(callback);
        response.setCreated_at(LocalDateTime.now(clock).toString());

        return ResponseEntity.ok(ApiUtils.success(response));
    }

    @Override
    @Transactional
    public ResponseEntity<?> requestPaymentApproval(
            PayRequest.PayApprovalRequestDTO requestDto,
            Long bayId,
            Member member,
            ReservationRequest.SaveDTO saveDTO) {
        requireLocalPaymentEnabled();

        PendingPayment pending = pendingPayments.get(requestDto.getTid());
        if (pending == null) {
            throw invalid("tid", "Invalid or already used local payment tid.");
        }
        if (!pending.pgToken.equals(requestDto.getPg_token())) {
            throw invalid("pg_token", "Invalid local payment token.");
        }
        if (!pending.memberId.equals(member.getId())) {
            throw invalid("member", "Local payment belongs to another member.");
        }
        if (!pending.bayId.equals(bayId) || !pending.bayId.equals(saveDTO.getBayId())) {
            throw invalid("bayId", "Bay does not match the ready request.");
        }
        if (!pending.startTime.equals(saveDTO.getStartTime()) ||
                !pending.endTime.equals(saveDTO.getEndTime())) {
            throw invalid("datetime", "Reservation time does not match the ready request.");
        }
        if (!clock.instant().isBefore(pending.expiresAt)) {
            pendingPayments.remove(pending.tid, pending);
            throw invalid("payment", "Local payment has expired.");
        }
        cleanupExpired();

        Bay bay = findBay(bayId);
        Carwash carwash = findCarwash(bay.getCarwash().getId());
        Reservation reservation = reservationService.save(saveDTO, carwash.getId(), bayId, member);
        ReservationResponse.findLatestOneResponseDTO response =
                reservationService.fetchLatestReservation(reservation.getId());

        removeAfterSuccessfulCommit(pending);
        return ResponseEntity.ok(response);
    }

    private void removeAfterSuccessfulCommit(PendingPayment pending) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pendingPayments.remove(pending.tid, pending);
                }
            });
        } else {
            pendingPayments.remove(pending.tid, pending);
        }
    }

    private ReservationRequest.ReservationTimeDTO toReservationTime(ReservationRequest.SaveDTO saveDTO) {
        ReservationRequest.ReservationTimeDTO dto = new ReservationRequest.ReservationTimeDTO();
        dto.setStartTime(saveDTO.getStartTime());
        dto.setEndTime(saveDTO.getEndTime());
        return dto;
    }

    private Bay findBay(Long bayId) {
        return bayRepository.findById(bayId)
                .orElseThrow(() -> invalid("bayId", "Bay not found."));
    }

    private Carwash findCarwash(Long carwashId) {
        return carwashRepository.findById(carwashId)
                .orElseThrow(() -> invalid("carwashId", "Carwash not found."));
    }

    private String uniqueToken(String prefix) {
        String token;
        do {
            token = prefix + randomToken();
        } while (pendingPayments.containsKey(token));
        return token;
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void cleanupExpired() {
        Instant now = clock.instant();
        pendingPayments.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt));
    }

    private void requireLocalPaymentEnabled() {
        if (externalPaymentEnabled) {
            throw new IllegalStateException("Local payment is disabled while external payment is enabled.");
        }
    }

    private BadRequestError invalid(String key, String message) {
        return new BadRequestError(
                BadRequestError.ErrorCode.VALIDATION_FAILED,
                Collections.singletonMap(key, message));
    }

    private static class PendingPayment {
        private final String tid;
        private final String pgToken;
        private final Long memberId;
        private final Long bayId;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final int amount;
        private final Instant createdAt;
        private final Instant expiresAt;

        private PendingPayment(
                String tid,
                String pgToken,
                Long memberId,
                Long bayId,
                LocalDateTime startTime,
                LocalDateTime endTime,
                int amount,
                Instant createdAt,
                Instant expiresAt) {
            this.tid = tid;
            this.pgToken = pgToken;
            this.memberId = memberId;
            this.bayId = bayId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.amount = amount;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }
}
