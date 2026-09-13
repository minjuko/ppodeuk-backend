package bdbe.bdbd.controller;

import bdbe.bdbd._core.security.CustomUserDetails;
import bdbe.bdbd.controller.owner.OwnerBayRestController;
import bdbe.bdbd.controller.owner.OwnerCarwashController;
import bdbe.bdbd.controller.user.UserReservationController;
import bdbe.bdbd.model.Code.MemberRole;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.service.bay.BayService;
import bdbe.bdbd.service.carwash.CarwashService;
import bdbe.bdbd.service.file.FileService;
import bdbe.bdbd.service.member.OwnerService;
import bdbe.bdbd.service.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceControllerBindingTest {

    private final Member member = Member.builder().id(1000L).role(MemberRole.ROLE_OWNER)
            .email("owner@test.invalid").password("test").username("owner").tel("test").build();
    private final CustomUserDetails userDetails = new CustomUserDetails(member);
    private BayService bayService;
    private OwnerService ownerService;
    private ReservationService reservationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bayService = mock(BayService.class);
        ownerService = mock(OwnerService.class);
        reservationService = mock(ReservationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new OwnerBayRestController(bayService),
                        new OwnerCarwashController(mock(CarwashService.class), mock(FileService.class), ownerService),
                        new UserReservationController(reservationService))
                .setCustomArgumentResolvers(authenticationPrincipalResolver())
                .build();
    }

    @Test
    void deleteBayPathPassesAuthenticatedMember() throws Exception {
        mockMvc.perform(delete("/api/owner/bays/77"))
                .andExpect(status().isOk());
        verify(bayService).deleteBay(77L, member);
    }

    @Test
    void ownerSelectedDateIsBoundAndForwarded() throws Exception {
        LocalDate selectedDate = LocalDate.of(2025, 2, 14);
        mockMvc.perform(get("/api/owner/carwashes").param("selected-date", "2025-02-14"))
                .andExpect(status().isOk());
        verify(ownerService).fetchOwnerReservationOverview(member, selectedDate);
    }

    @Test
    void absentOwnerSelectedDatePreservesDefaultServicePath() throws Exception {
        mockMvc.perform(get("/api/owner/carwashes"))
                .andExpect(status().isOk());
        verify(ownerService).fetchOwnerReservationOverview(member, null);
    }

    @Test
    void userSelectedAtIsBoundForCurrentAndRecentQueries() throws Exception {
        LocalDateTime selectedAt = LocalDateTime.of(2025, 2, 14, 12, 30);
        mockMvc.perform(get("/api/user/reservations/current-status")
                        .param("selected-at", "2025-02-14T12:30:00"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user/reservations/recent")
                        .param("selected-at", "2025-02-14T12:30:00"))
                .andExpect(status().isOk());
        verify(reservationService).findCurrentStatusReservation(member, selectedAt);
        verify(reservationService).findRecentReservation(member, selectedAt);
    }

    @Test
    void malformedOptionalDatesKeepSpringBadRequestBehavior() throws Exception {
        mockMvc.perform(get("/api/owner/carwashes").param("selected-date", "2025-99-99"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/user/reservations/current-status").param("selected-at", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    private HandlerMethodArgumentResolver authenticationPrincipalResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                          NativeWebRequest request, WebDataBinderFactory binderFactory) {
                return userDetails;
            }
        };
    }
}
