package bdbe.bdbd.service.pay;

import bdbe.bdbd.dto.pay.PayRequest;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.optime.Optime;
import bdbe.bdbd.model.reservation.Reservation;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.service.reservation.ReservationService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class PayServiceConcurrencyGuardTest {

    @Test
    void locksBayBeforeCallingExternalApproval() {
        ReservationService reservations = mock(ReservationService.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        CarwashJPARepository carwashes = mock(CarwashJPARepository.class);
        BayJPARepository bays = mock(BayJPARepository.class);
        PayService service = new PayService(reservations, restTemplate, carwashes, bays);
        ReflectionTestUtils.setField(service, "externalPaymentEnabled", true);
        ReflectionTestUtils.setField(service, "adminKey", "test-key");

        long bayId = 10L;
        long carwashId = 20L;
        Member member = Member.builder().id(30L).build();
        Carwash carwash = Carwash.builder().id(carwashId).build();
        Bay bay = Bay.builder().id(bayId).carwash(carwash).build();
        Optime optime = mock(Optime.class);
        Reservation saved = Reservation.builder().id(40L).build();
        ReservationResponse.findLatestOneResponseDTO response =
                mock(ReservationResponse.findLatestOneResponseDTO.class);

        given(bays.findByIdForUpdate(bayId)).willReturn(Optional.of(bay));
        given(carwashes.findById(carwashId)).willReturn(Optional.of(carwash));
        given(reservations.findOptime(eq(carwash), any())).willReturn(optime);
        given(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .willReturn(ResponseEntity.ok("{}"));
        given(reservations.save(any(), eq(carwashId), eq(bayId), eq(member))).willReturn(saved);
        given(reservations.fetchLatestReservation(40L)).willReturn(response);

        service.requestPaymentApproval(approvalRequest(), bayId, member, saveRequest(bayId));

        InOrder order = inOrder(bays, restTemplate);
        order.verify(bays).findByIdForUpdate(bayId);
        order.verify(restTemplate).postForEntity(any(String.class), any(), eq(String.class));
    }

    private PayRequest.PayApprovalRequestDTO approvalRequest() {
        PayRequest.PayApprovalRequestDTO request = new PayRequest.PayApprovalRequestDTO();
        request.setCid("cid");
        request.setTid("tid");
        request.setPartner_order_id("order");
        request.setPartner_user_id("user");
        request.setPg_token("token");
        return request;
    }

    private ReservationRequest.SaveDTO saveRequest(Long bayId) {
        ReservationRequest.SaveDTO request = new ReservationRequest.SaveDTO();
        request.setBayId(bayId);
        request.setStartTime(LocalDateTime.of(2026, 8, 13, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 8, 13, 11, 0));
        return request;
    }
}
