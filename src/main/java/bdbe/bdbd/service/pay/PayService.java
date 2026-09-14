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
@Profile("prod | local-external")
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

        reservationService.validateReservationTime(startTime, endTime, optime, bayId, member);

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
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + adminKey);

        requestDto.setTotal_amount(totalAmount);

        Map<String, Object> parameters = new java.util.LinkedHashMap<>();
        parameters.put("cid", requestDto.getCid());
        parameters.put("partner_order_id", requestDto.getPartner_order_id());
        parameters.put("partner_user_id", requestDto.getPartner_user_id());
        parameters.put("item_name", requestDto.getItem_name());
        parameters.put("quantity", requestDto.getQuantity());
        parameters.put("total_amount", requestDto.getTotal_amount());
        parameters.put("tax_free_amount", requestDto.getTax_free_amount());
        parameters.put("approval_url", approval_url);
        parameters.put("cancel_url", cancel_url);
        parameters.put("fail_url", fail_url);
        log.info("Requesting KakaoPay ready state for bayId={}", bayId);

        String url = "https://open-api.kakaopay.com/online/v1/payment/ready";

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
            log.warn("KakaoPay ready rejected: status={}, message={}", status.value(), errorMessage);

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
            String message = jsonNode.path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }

            String errorMessage = jsonNode.path("error_message").asText("");
            String errorCode = jsonNode.path("error_code").asText("");
            if (!errorMessage.isBlank()) {
                return errorCode.isBlank()
                        ? errorMessage
                        : errorCode + ": " + errorMessage;
            }

            return response == null || response.isBlank()
                    ? "Error processing the request"
                    : response;
        } catch (JsonProcessingException e) {
            return response == null || response.isBlank()
                    ? "Error processing the request"
                    : response;
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

        reservationService.validateReservationTime(startTime, endTime, optime, bayId, member);

        // API 요청 보내기
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + adminKey);

        Map<String, Object> parameters = new java.util.LinkedHashMap<>();
        parameters.put("cid", requestDto.getCid());
        parameters.put("tid", requestDto.getTid());
        parameters.put("partner_order_id", requestDto.getPartner_order_id());
        parameters.put("partner_user_id", requestDto.getPartner_user_id());
        parameters.put("pg_token", requestDto.getPg_token());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(parameters, headers);
        String url = "https://open-api.kakaopay.com/online/v1/payment/approve";

        ResponseEntity<String> paymentApprovalResponse = restTemplate.postForEntity(url, request, String.class);

        Reservation reservation;

        if (paymentApprovalResponse.getStatusCode().is2xxSuccessful()) {
            reservation = reservationService.save(saveDTO, carwash.getId(), bayId, member);  // 변수 이름 변경
        } else {
            log.error("KakaoPay approval failed with status={}", paymentApprovalResponse.getStatusCodeValue());
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
