package bdbe.bdbd.controller.user;

import bdbe.bdbd._core.security.CustomUserDetails;
import bdbe.bdbd.dto.pay.PayRequest;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.service.pay.PaymentFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
/**
 * 결제 관련 요청을 처리하는 사용자 API
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserPayController {

    private final PaymentFlowService paymentFlowService;

    @PostMapping("/payment/ready")
    public ResponseEntity<?> requestPaymentReady(
            @Valid @RequestBody PayRequest.PaymentReadyRequest paymentReadyRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Errors errors
    ) {
        ReservationRequest.SaveDTO saveDTO = paymentReadyRequest.getSaveDTO();
        return paymentFlowService.requestPaymentReady(
                paymentReadyRequest.getRequestDto(),
                saveDTO,
                userDetails.getMember()
        );
    }


    @PostMapping("/payment/approve")
    public ResponseEntity<?> requestPaymentApproval(
            @Valid @RequestBody PayRequest.PaymentApprovalRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long bayId = requestDTO.getSaveDTO().getBayId();

        return paymentFlowService.requestPaymentApproval(
                requestDTO.getPayApprovalRequestDTO(),
                bayId,
                userDetails.getMember(),
                requestDTO.getSaveDTO()
        );
    }
}
