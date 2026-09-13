package bdbe.bdbd.service.pay;

import bdbe.bdbd._core.utils.ApiUtils;
import bdbe.bdbd.dto.pay.PayRequest;
import bdbe.bdbd.dto.pay.PayResponse;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.service.reservation.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PaymentProfileConfigurationTest {

    @Test
    void localProfileRegistersExactlyOneLocalPaymentFlow() {
        assertSingleImplementation("local", LocalPaymentService.class);
    }

    @Test
    void demoProfileRegistersExactlyOneLocalPaymentFlow() {
        assertSingleImplementation("demo", LocalPaymentService.class);
    }

    @Test
    void testProfileRegistersExactlyOneLocalPaymentFlow() {
        assertSingleImplementation("test", LocalPaymentService.class);
    }

    @Test
    void prodProfileRegistersExactlyOneExternalPaymentFlow() {
        try (AnnotationConfigApplicationContext context = context("prod", true)) {
                    assertThat(context.getBeansOfType(PaymentFlowService.class)).hasSize(1);
                    assertThat(context.getBeansOfType(PayService.class)).hasSize(1);
                    assertThat(context.getBeansOfType(LocalPaymentService.class)).isEmpty();
        }
    }

    @Test
    void demoPaymentReadyUsesFakeFlowWithoutCallingKakaoPay() {
        try (AnnotationConfigApplicationContext context = context("demo", false)) {
                    ReservationService reservationService = context.getBean(ReservationService.class);
                    BayJPARepository bayRepository = context.getBean(BayJPARepository.class);
                    CarwashJPARepository carwashRepository = context.getBean(CarwashJPARepository.class);
                    RestTemplate restTemplate = context.getBean(RestTemplate.class);

                    Member member = Member.builder().id(102L).build();
                    Carwash carwash = Carwash.builder().id(1001L).build();
                    Bay bay = Bay.builder().id(1001L).carwash(carwash).build();
                    LocalDateTime start = LocalDateTime.of(2026, 9, 14, 10, 0);
                    LocalDateTime end = start.plusHours(1);

                    given(bayRepository.findById(1001L)).willReturn(Optional.of(bay));
                    given(carwashRepository.findById(1001L)).willReturn(Optional.of(carwash));
                    given(reservationService.findPayAmount(any(), eq(1001L), eq(member)))
                            .willReturn(new ReservationResponse.PayAmountDTO(start, end, 12000));

                    PayRequest.PayReadyRequestDTO payRequest = new PayRequest.PayReadyRequestDTO();
                    payRequest.setTotal_amount(12000);
                    ReservationRequest.SaveDTO reservationRequest = new ReservationRequest.SaveDTO();
                    reservationRequest.setBayId(1001L);
                    reservationRequest.setStartTime(start);
                    reservationRequest.setEndTime(end);

                    ResponseEntity<?> response = context.getBean(PaymentFlowService.class)
                            .requestPaymentReady(payRequest, reservationRequest, member);
                    ApiUtils.ApiResult<?> result = (ApiUtils.ApiResult<?>) response.getBody();
                    PayResponse.PayReadyResponseDTO body =
                            (PayResponse.PayReadyResponseDTO) result.getResponse();

                    assertThat(body.getTid()).startsWith("local_tid_");
                    assertThat(body.getNext_redirect_pc_url())
                            .startsWith("http://localhost.test/paymentwaiting?pg_token=");
                    verifyNoInteractions(restTemplate);
        }
    }

    private void assertSingleImplementation(String profile, Class<?> expectedType) {
        try (AnnotationConfigApplicationContext context = context(profile, false)) {
                    assertThat(context.getBeansOfType(PaymentFlowService.class)).hasSize(1);
                    assertThat(context.getBeansOfType(expectedType)).hasSize(1);
        }
    }

    private AnnotationConfigApplicationContext context(String profile, boolean externalPaymentEnabled) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        TestPropertyValues.of(
                "payment.external.enabled=" + externalPaymentEnabled,
                "payment.approval-url=http://localhost.test/paymentwaiting",
                "payment.cancel-url=http://localhost.test/paymentfail",
                "payment.fail-url=http://localhost.test/paymentfail",
                "kakao.admin.key=test-placeholder-kakao-admin-key")
                .applyTo(context);
        context.register(PaymentDependencies.class, LocalPaymentService.class, PayService.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    static class PaymentDependencies {
        @Bean
        ReservationService reservationService() {
            return mock(ReservationService.class);
        }

        @Bean
        BayJPARepository bayJPARepository() {
            return mock(BayJPARepository.class);
        }

        @Bean
        CarwashJPARepository carwashJPARepository() {
            return mock(CarwashJPARepository.class);
        }

        @Bean
        RestTemplate restTemplate() {
            return mock(RestTemplate.class);
        }
    }
}
