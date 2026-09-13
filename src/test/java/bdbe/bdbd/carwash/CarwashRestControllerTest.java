package bdbe.bdbd.carwash;

import bdbe.bdbd.dto.carwash.CarwashRequest;
import bdbe.bdbd.model.Code.KeywordType;
import bdbe.bdbd.model.keyword.Keyword;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.keyword.KeywordJPARepository;
import bdbe.bdbd.repository.location.LocationJPARepository;
import bdbe.bdbd.repository.member.MemberJPARepository;
import bdbe.bdbd.repository.optime.OptimeJPARepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CarwashRestControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    LocationJPARepository locationJPARepository;

    @Autowired
    MemberJPARepository memberJPARepository;

    @Autowired
    CarwashJPARepository carwashJPARepository;

    @Autowired
    KeywordJPARepository keywordJPARepository;

    @Autowired
    OptimeJPARepository optimeJPARepository;

    @Autowired
    private ObjectMapper om;


    @Test
    @DisplayName("전체 세차장 목록 조회")
    public void findAll_test() throws Exception {
        // given

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/open/carwashes")
        );

        // eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body : " + responseBody);

        // verify
        resultActions.andExpect(jsonPath("$.success").value("true"));
    }


    @WithUserDetails(value = "owner@nate.com")
    @Test
    @DisplayName("세차장 등록 기능")
    public void save_test() throws Exception {
        // given
        CarwashRequest.SaveDTO dto = new CarwashRequest.SaveDTO();

        Keyword keyword = Keyword.builder()
                .name("하부세차")
                .type(KeywordType.CARWASH)
                .build();
        Keyword savedKeyword = keywordJPARepository.save(keyword);
        dto.setKeywordIdList(Arrays.asList(savedKeyword.getId()));
        dto.setName("test 세차장");

        dto.setTel("01012345678");
        dto.setDescription("테스트 설명");
        dto.setPrice("100");

        CarwashRequest.LocationDTO locationDTO = new CarwashRequest.LocationDTO();
        locationDTO.setAddress("test 주소");
        locationDTO.setLatitude(1.234);
        locationDTO.setLongitude(5.678);
        dto.setLocation(locationDTO);

        CarwashRequest.OperatingTimeDTO optimeDTO = new CarwashRequest.OperatingTimeDTO();
        CarwashRequest.OperatingTimeDTO.TimeSlot weekdaySlot = new CarwashRequest.OperatingTimeDTO.TimeSlot();
        weekdaySlot.setStart(LocalTime.of(9, 0));
        weekdaySlot.setEnd(LocalTime.of(17, 0));
        optimeDTO.setWeekday(weekdaySlot);

        CarwashRequest.OperatingTimeDTO.TimeSlot weekendSlot = new CarwashRequest.OperatingTimeDTO.TimeSlot();
        weekendSlot.setStart(LocalTime.of(10, 0));
        weekendSlot.setEnd(LocalTime.of(16, 0));
        optimeDTO.setWeekend(weekendSlot);
        dto.setOptime(optimeDTO);

        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image1 content".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "image2 content".getBytes());

        MockMultipartFile updatedtoFile = new MockMultipartFile("updateData", "", "application/json", om.writeValueAsBytes(dto));
        MockMultipartFile carwashFile = new MockMultipartFile("carwash", "", "application/json", om.writeValueAsBytes(dto));

        String requestBody = om.writeValueAsString(dto);
        System.out.println("요청 데이터 : " + requestBody);
        // when
        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.multipart("/api/owner/carwashes/register")
                        .file(image1)
                        .file(image2)
                        .file(updatedtoFile)
                        .file(carwashFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
        );

        // eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body : " + responseBody);
        // verify
        resultActions.andExpect(jsonPath("$.success").value("true"));

    }


    @WithUserDetails(value = "user@nate.com")
    @Test
    @DisplayName("주변 세차장 검색")
    public void findNearbyCarwashes_test() throws Exception {
        // given
        double testLatitude = 1.23;
        double testLongitude = 2.34;

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/open/carwashes/nearby")
                        .param("latitude", String.valueOf(testLatitude))
                        .param("longitude", String.valueOf(testLongitude))
        );

        // eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body : " + responseBody);

        // verify
        resultActions.andExpect(jsonPath("$.response").isArray());

    }

    @WithUserDetails(value = "user@nate.com")
    @Test
    @DisplayName("가장 가까운 세차장 검색(추천세차장)")
    public void findNearestCarwash_test() throws Exception {
        // given
        double testLatitude = 1.23;
        double testLongitude = 2.34;

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/open/carwashes/recommended")
                        .param("latitude", String.valueOf(testLatitude))
                        .param("longitude", String.valueOf(testLongitude))
        );

        // eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body : " + responseBody);

        // verify
        resultActions.andExpect(jsonPath("$.success").value("true"));
    }

    @WithUserDetails(value = "user@nate.com")
    @Test
    @DisplayName("키워드로 세차장 검색")
    public void findCarwashesByKeywords_test() throws Exception {
        // given
        Keyword keyword = keywordJPARepository.findByType(KeywordType.CARWASH).get(0);
        String keywordId = String.valueOf(keyword.getId());
        String testLatitude = "1.23";
        String testLongitude = "2.34";

        // when
        ResultActions resultActions = mvc.perform(
                get("/api/open/carwashes/search")
                        .param("keywordIdList", keywordId)
                        .param("latitude", testLatitude)
                        .param("longitude", testLongitude)
        );

        // eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body : " + responseBody);

        // verify
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response").isArray());
    }

    @Test
    @DisplayName("세차장 상세 정보 조회 기능")
    public void findByIdTest() throws Exception {

        Long carwashId = carwashJPARepository.findFirstBy().getId();
        System.out.println("carwashId:" + carwashId);

        ResultActions resultActions = mvc.perform(
                get(String.format("/api/open/carwashes/%d/info", carwashId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        );

        resultActions.andExpect(status().isOk());

        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body:" + responseBody);

        resultActions.andExpect(jsonPath("$.success").value("true"));
    }

    @WithUserDetails(value = "owner@nate.com")
    @Test
    @DisplayName("세차장 기존 정보 불러오기")
    public void findCarwashByDetailsTest() throws Exception {

        Long carwashId = carwashJPARepository.findFirstBy().getId();
        System.out.println("carwashId: " + carwashId);

        ResultActions resultActions = mvc.perform(
                get(String.format("/api/owner/carwashes/%d/details", carwashId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)


        );

        resultActions.andExpect(status().isOk());

        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body:" + responseBody);

        resultActions.andExpect(jsonPath("$.success").value("true"));
    }

    @WithUserDetails(value = "owner@nate.com")
    @Test
    @DisplayName("세차장 세부 정보 수정")
    public void updateCarwashDetailsTest() throws Exception {

        Long carwashId = carwashJPARepository.findFirstBy().getId();
        System.out.println("carwashId:" + carwashId);

        CarwashRequest.updateCarwashDetailsDTO updateCarwashDetailsDTO = new CarwashRequest.updateCarwashDetailsDTO();
        updateCarwashDetailsDTO.setName("풍영 세차장");
        updateCarwashDetailsDTO.setPrice(3000);
        updateCarwashDetailsDTO.setTel("010-2222-3333");
        updateCarwashDetailsDTO.setDescription("안녕하세요");

        CarwashRequest.updateLocationDTO updateLocationDTO = new CarwashRequest.updateLocationDTO();
        updateLocationDTO.setAddress("풍영 주소");
        updateLocationDTO.setLatitude(1.121);
        updateLocationDTO.setLongitude(2.232);
        updateCarwashDetailsDTO.setLocation(updateLocationDTO);

        CarwashRequest.updateOperatingTimeDTO optimeDTO = new CarwashRequest.updateOperatingTimeDTO();

        CarwashRequest.updateOperatingTimeDTO.updateTimeSlot weekday = new CarwashRequest.updateOperatingTimeDTO.updateTimeSlot();
        weekday.setStart(LocalTime.of(9, 0));
        weekday.setEnd(LocalTime.of(20, 0));
        optimeDTO.setWeekday(weekday);

        CarwashRequest.updateOperatingTimeDTO.updateTimeSlot weekend = new CarwashRequest.updateOperatingTimeDTO.updateTimeSlot();
        weekend.setStart(LocalTime.of(9, 0));
        weekend.setEnd(LocalTime.of(20, 0));
        optimeDTO.setWeekend(weekend);
        updateCarwashDetailsDTO.setOptime(optimeDTO);

        updateCarwashDetailsDTO.setKeywordIdList(Arrays.asList(8L));

        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image1 content".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "image2 content".getBytes());

        MockMultipartFile updatedtoFile = new MockMultipartFile("updateData", "", "application/json", om.writeValueAsBytes(updateCarwashDetailsDTO));

        String requestBody = om.writeValueAsString(updateCarwashDetailsDTO);
        System.out.println("요청 데이터 : " + requestBody);


        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.multipart(String.format("/api/owner/carwashes/%d/details", carwashId))
                        .file(image1)
                        .file(image2)
                        .file(updatedtoFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
        );

        resultActions.andExpect(status().isOk());

        String responseBody = resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println("응답 Body:" + responseBody);

        resultActions.andExpect(jsonPath("$.success").value("true"));
    }
}


