package bdbe.bdbd.pay;

import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.dto.pay.PayRequest;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class PayRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    CarwashJPARepository carwashJPARepository;

    @Autowired
    private ObjectMapper om;

    @WithUserDetails("user@nate.com")
    @Test
    @DisplayName("Payment Ready Request Test")
    public void requestPaymentReadyTest() throws Exception {

        Long carwashId = 1L;

        PayRequest.PayReadyRequestDTO requestDto = new PayRequest.PayReadyRequestDTO();
        requestDto.setCid("TC0ONETIME");
        requestDto.setPartner_order_id("partner_order_id");
        requestDto.setPartner_user_id("partner_user_id");
        requestDto.setItem_name("구름 세차장 예약");
        requestDto.setQuantity(1);
        requestDto.setTax_free_amount(0);
        requestDto.setTotal_amount(20000);

        ReservationRequest.SaveDTO saveDTO = new ReservationRequest.SaveDTO();
        saveDTO.setBayId(1L);
        saveDTO.setStartTime(LocalDateTime.parse("2024-11-01T14:00:00"));
        saveDTO.setEndTime(LocalDateTime.parse("2024-11-01T15:00:00"));

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("requestDto", requestDto);
        requestBodyMap.put("saveDTO", saveDTO);

        String jsonRequestBody = om.writeValueAsString(requestBodyMap);

        ResultActions resultActions = mvc.perform(
                post("/api/user/payment/ready")
                        .content(jsonRequestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().isOk());
    }

    @WithUserDetails("user@nate.com")
    @Test
    @DisplayName("invalid amount Payment Ready Request Test")
    public void invalidAmonutPaymentReadyTest() throws Exception {

        Long carwashId = 1L;

        PayRequest.PayReadyRequestDTO requestDto = new PayRequest.PayReadyRequestDTO();
        requestDto.setCid("TC0ONETIME");
        requestDto.setPartner_order_id("partner_order_id");
        requestDto.setPartner_user_id("partner_user_id");
        requestDto.setItem_name("구름 세차장 예약");
        requestDto.setQuantity(1);
        requestDto.setTax_free_amount(0);
        requestDto.setTotal_amount(9999);

        ReservationRequest.SaveDTO saveDTO = new ReservationRequest.SaveDTO();
        saveDTO.setBayId(1L);
        saveDTO.setStartTime(LocalDateTime.parse("2024-11-01T14:00:00"));
        saveDTO.setEndTime(LocalDateTime.parse("2024-11-01T15:00:00"));

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("requestDto", requestDto);
        requestBodyMap.put("saveDTO", saveDTO);

        String jsonRequestBody = om.writeValueAsString(requestBodyMap);

        ResultActions resultActions = mvc.perform(
                post("/api/user/payment/ready")
                        .content(jsonRequestBody)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().is4xxClientError());
    }

}
