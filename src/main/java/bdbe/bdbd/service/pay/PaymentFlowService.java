package bdbe.bdbd.service.pay;

import bdbe.bdbd.dto.pay.PayRequest;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.model.member.Member;
import org.springframework.http.ResponseEntity;

public interface PaymentFlowService {

    ResponseEntity<?> requestPaymentReady(
            PayRequest.PayReadyRequestDTO requestDto,
            ReservationRequest.SaveDTO saveDTO,
            Member member);

    ResponseEntity<?> requestPaymentApproval(
            PayRequest.PayApprovalRequestDTO requestDto,
            Long bayId,
            Member member,
            ReservationRequest.SaveDTO saveDTO);
}
