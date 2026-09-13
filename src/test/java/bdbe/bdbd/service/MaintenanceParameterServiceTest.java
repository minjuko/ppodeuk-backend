package bdbe.bdbd.service;

import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.reservation.Reservation;
import bdbe.bdbd._core.utils.MemberUtils;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.file.FileJPARepository;
import bdbe.bdbd.repository.location.LocationJPARepository;
import bdbe.bdbd.repository.optime.OptimeJPARepository;
import bdbe.bdbd.repository.reservation.ReservationJPARepository;
import bdbe.bdbd.repository.member.MemberJPARepository;
import bdbe.bdbd.service.member.OwnerService;
import bdbe.bdbd.service.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MaintenanceParameterServiceTest {

    private ReservationJPARepository reservationRepository;
    private CarwashJPARepository carwashRepository;
    private BayJPARepository bayRepository;
    private FileJPARepository fileRepository;
    private ReservationService service;
    private Member member;
    private Bay bay;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationJPARepository.class);
        carwashRepository = mock(CarwashJPARepository.class);
        bayRepository = mock(BayJPARepository.class);
        fileRepository = mock(FileJPARepository.class);
        service = new ReservationService(
                reservationRepository, carwashRepository, bayRepository,
                mock(LocationJPARepository.class), mock(OptimeJPARepository.class), fileRepository);
        member = Member.builder().id(1000L).build();
        Carwash carwash = Carwash.builder().id(20L).name("test").build();
        bay = Bay.builder().id(10L).bayNum(1).carwash(carwash).build();
        given(bayRepository.findById(10L)).willReturn(Optional.of(bay));
        given(carwashRepository.findById(20L)).willReturn(Optional.of(carwash));
        given(fileRepository.findByCarwash_IdAndIsDeletedFalse(20L)).willReturn(Collections.emptyList());
    }

    @Test
    void selectedAtControlsCurrentUpcomingAndCompletedClassification() {
        LocalDateTime selectedAt = LocalDateTime.of(2025, 2, 14, 12, 0);
        given(reservationRepository.findByMemberIdAndIsDeletedFalse(1000L)).willReturn(Arrays.asList(
                reservation(1L, selectedAt.minusMinutes(30), selectedAt.plusMinutes(30)),
                reservation(2L, selectedAt.plusHours(1), selectedAt.plusHours(2)),
                reservation(3L, selectedAt.minusHours(2), selectedAt.minusHours(1))));

        ReservationResponse.fetchCurrentStatusReservationDTO result =
                service.findCurrentStatusReservation(member, selectedAt);

        assertThat(result.getCurrentReservationList()).hasSize(1);
        assertThat(result.getUpcomingReservationList()).hasSize(1);
        assertThat(result.getCompleteReservationList()).hasSize(1);
    }

    @Test
    void recentSelectedAtUsesOnlyBeforeQuery() {
        LocalDateTime selectedAt = LocalDateTime.of(2025, 2, 14, 12, 0);
        given(reservationRepository.findByMemberIdJoinFetchBefore(any(), any(), any()))
                .willReturn(Collections.emptyList());

        service.findRecentReservation(member, selectedAt);

        verify(reservationRepository).findByMemberIdJoinFetchBefore(any(), any(), any(Pageable.class));
        verify(reservationRepository, never()).findByMemberIdJoinFetch(any(), any(Pageable.class));
    }

    @Test
    void recentWithoutSelectedAtPreservesCurrentTimestampQuery() {
        given(reservationRepository.findByMemberIdJoinFetch(any(), any()))
                .willReturn(Collections.emptyList());

        service.findRecentReservation(member);

        verify(reservationRepository).findByMemberIdJoinFetch(any(), any(Pageable.class));
        verify(reservationRepository, never()).findByMemberIdJoinFetchBefore(any(), any(), any());
    }

    @Test
    void ownerSelectedDateControlsDailyAndMonthlyRepositoryDatesWithinOwnedScope() {
        LocalDate selectedDate = LocalDate.of(2025, 2, 14);
        Carwash ownedCarwash = Carwash.builder().id(20L).member(member).name("test").build();
        given(carwashRepository.findByMember_Id(1000L)).willReturn(Collections.singletonList(ownedCarwash));
        given(carwashRepository.findByIdAndMember_Id(20L, 1000L)).willReturn(Optional.of(ownedCarwash));
        given(bayRepository.findByCarwashId(20L)).willReturn(Collections.emptyList());
        given(fileRepository.findByCarwash_IdAndIsDeletedFalse(20L)).willReturn(Collections.emptyList());
        OptimeJPARepository ownerOptimeRepository = mock(OptimeJPARepository.class);
        OwnerService ownerService = new OwnerService(
                mock(PasswordEncoder.class), mock(MemberJPARepository.class), carwashRepository,
                reservationRepository, ownerOptimeRepository, bayRepository, fileRepository, mock(MemberUtils.class));

        ownerService.fetchOwnerReservationOverview(member, selectedDate);
        ownerService.findCarwashReservationOverview(20L, member, selectedDate);

        verify(reservationRepository, times(2))
                .findTodaysReservationsByCarwashId(20L, java.sql.Date.valueOf(selectedDate));
        verify(reservationRepository).findTotalRevenueByCarwashIdAndDate(20L, LocalDate.of(2025, 2, 1));
        verify(reservationRepository).findMonthlyReservationCountByCarwashIdAndDate(20L, LocalDate.of(2025, 2, 1));
    }

    private Reservation reservation(Long id, LocalDateTime start, LocalDateTime end) {
        return Reservation.builder().id(id).startTime(start).endTime(end).bay(bay).member(member).build();
    }
}
