package bdbe.bdbd.service.pay;

import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.exception.InternalServerError;
import bdbe.bdbd._core.exception.NotFoundError;
import bdbe.bdbd._core.exception.UnAuthorizedError;
import bdbe.bdbd._core.utils.ApiUtils;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@Profile("!local")
@RequiredArgsConstructor
public class PayService implements PaymentFlowService {

    @Value("${payment.external.enabled:false}")
    private boolean externalPaymentEnabled;

    @Value("${kakao.admin.key}")
    private String adminKey;

    @Value("${payment.approval-url}")
    private String approval_url;

    @Value("${payment.cancel-url}")
    private String cancel_url;

    @Value("${payment.fail-url}")
    private String fail_url;

    private final ReservationService reservationService;

    private final RestTemplate restTemplate;

    private final CarwashJPARepository carwashJpaRepository;

    private final BayJPARepository bayJPARepository;

    @Override
    public ResponseEntity<?> requestPaymentReady(
            PayRequest.PayReadyRequestDTO requestDto,
            ReservationRequest.SaveDTO saveDTO,
            Member member) {

        requireExternalPaymentEnabled();

        Long bayId = saveDTO.getBayId();
        Bay bay = bayJPARepository.findById(bayId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("BayId", "Bay not found")
                ));

        Long carwashId = bay.getCarwash().getId();
        Carwash carwash = carwashJpaRepository.findById(carwashId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found")
                ));

        LocalDateTime startTime = saveDTO.getStartTime();
        LocalDateTime endTime = saveDTO.getEndTime();
        Optime optime = reservationService.findOptime(carwash, startTime);

        reservationService.validateReservationTime(startTime, endTime, optime, bayId);

        int perPrice = carwash.getPrice();
        int minutesDifference = (int) ChronoUnit.MINUTES.between(startTime, endTime);
        int blocksOf30Minutes = minutesDifference / 30;
        int price = perPrice * blocksOf30Minutes;

        int totalAmount = price;

        if (totalAmount != requestDto.getTotal_amount())
            throw new BadRequestError(
                    BadRequestError.ErrorCode.WRONG_REQUEST_TRANSMISSION,
                    Collections.singletonMap("pay", "Invalid pay amount")
            );

        PayRequest.PayReadyRequestDTO dto = new PayRequest.PayReadyRequestDTO();
        dto.setTotal_amount(totalAmount);

        // API 요청 보내기
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "KakaoAK " + adminKey);

        requestDto.setTotal_amount(totalAmount);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("cid", requestDto.getCid());
        parameters.add("partner_order_id", requestDto.getPartner_order_id());
        parameters.add("partner_user_id", requestDto.getPartner_user_id());
        parameters.add("item_name", requestDto.getItem_name());
        parameters.add("quantity", requestDto.getQuantity().toString());
        parameters.add("total_amount", requestDto.getTotal_amount().toString());
        parameters.add("tax_free_amount", requestDto.getTax_free_amount().toString());
        parameters.add("approval_url", approval_url);
        parameters.add("cancel_url", cancel_url);
        parameters.add("fail_url", fail_url);
        log.info("Parameters: " + parameters.toString());

        String url = "https://kapi.kakao.com/v1/payment/ready";

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(parameters, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }
            );
            ApiUtils.ApiResult<Map<String, Object>> successResult = ApiUtils.success(response.getBody());

            return ResponseEntity.ok(successResult);
        } catch (HttpClientErrorException e) {
            HttpStatus status = e.getStatusCode();
            String errorMessage = getString(objectMapper, e.getResponseBodyAsString());

            UnAuthorizedError error = new UnAuthorizedError(
                    UnAuthorizedError.ErrorCode.AUTHENTICATION_FAILED,
                    Collections.singletonMap("error", errorMessage)
            );

            return new ResponseEntity<>(error.body(), status);
        } catch (HttpServerErrorException e) {
            HttpStatus status = e.getStatusCode();
            String errorMessage = getString(objectMapper, e.getResponseBodyAsString());

            InternalServerError error = new InternalServerError(
                    InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                    Collections.singletonMap("error", errorMessage)
            );

            return new ResponseEntity<>(error.body(), status);
        } catch (RestClientException e) {
            String errorMessage = getString(objectMapper, e.getMessage());

            InternalServerError error = new InternalServerError(
                    InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                    Collections.singletonMap("error", errorMessage)
            );

            return new ResponseEntity<>(error.body(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getString(ObjectMapper objectMapper, String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("message").asText("Error processing the request");
        } catch (JsonProcessingException e) {
            return "Error processing the request";
        }
    }

    @Transactional
    @Override
    public ResponseEntity<ReservationResponse.findLatestOneResponseDTO> requestPaymentApproval(
            PayRequest.PayApprovalRequestDTO requestDto,
            Long bayId,
            Member member,
            ReservationRequest.SaveDTO saveDTO) {

        requireExternalPaymentEnabled();

        Bay bay = bayJPARepository.findById(bayId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("BayId", "BayId not found")
                ));

        Carwash carwash = carwashJpaRepository.findById(bay.getCarwash().getId())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found")
                ));

        LocalDateTime startTime = saveDTO.getStartTime();
        LocalDateTime endTime = saveDTO.getEndTime();
        Optime optime = reservationService.findOptime(carwash, startTime);

        reservationService.validateReservationTime(startTime, endTime, optime, bayId);

        // API 요청 보내기
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "KakaoAK " + adminKey);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("cid", requestDto.getCid());
        parameters.add("tid", requestDto.getTid());
        parameters.add("partner_order_id", requestDto.getPartner_order_id());
        parameters.add("partner_user_id", requestDto.getPartner_user_id());
        parameters.add("pg_token", requestDto.getPg_token());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
        String url = "https://kapi.kakao.com/v1/payment/approve";

        ResponseEntity<String> paymentApprovalResponse = restTemplate.postForEntity(url, request, String.class);

        Reservation reservation;

        if (paymentApprovalResponse.getStatusCode().is2xxSuccessful()) {
            reservation = reservationService.save(saveDTO, carwash.getId(), bayId, member);  // 변수 이름 변경
        } else {
            log.error("Payment approval failed: " + paymentApprovalResponse.getBody());
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("PayResponse", "Payment approval failed")
            );
        }
        ReservationResponse.findLatestOneResponseDTO responseDto = reservationService.fetchLatestReservation(reservation.getId());

        return ResponseEntity.ok(responseDto);
    }

    private void requireExternalPaymentEnabled() {
        if (!externalPaymentEnabled) {
            throw new InternalServerError(
                    InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                    Collections.singletonMap("Payment", "KakaoPay is unavailable in the local profile."));
        }
    }

}
