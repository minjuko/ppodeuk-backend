package bdbe.bdbd.reservation;

import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.Code.DayType;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.optime.Optime;
import bdbe.bdbd.model.reservation.Reservation;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.file.FileJPARepository;
import bdbe.bdbd.repository.location.LocationJPARepository;
import bdbe.bdbd.repository.optime.OptimeJPARepository;
import bdbe.bdbd.repository.reservation.ReservationJPARepository;
import bdbe.bdbd.service.reservation.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ReservationDateTimeRegressionTest {

    private static final Long BAY_ID = 1L;

    private ReservationJPARepository reservationRepository;
    private BayJPARepository bayRepository;
    private CarwashJPARepository carwashRepository;
    private OptimeJPARepository optimeRepository;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationJPARepository.class);
        bayRepository = mock(BayJPARepository.class);
        carwashRepository = mock(CarwashJPARepository.class);
        optimeRepository = mock(OptimeJPARepository.class);
        reservationService = new ReservationService(
                reservationRepository,
                carwashRepository,
                bayRepository,
                mock(LocationJPARepository.class),
                optimeRepository,
                mock(FileJPARepository.class));

        given(reservationRepository.findByBay_IdAndIsDeletedFalse(BAY_ID))
                .willReturn(Collections.emptyList());
    }

    @Test
    void acceptsSixtyMinuteDaytimeReservation() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 12, 9, 30);
        LocalDateTime end = LocalDateTime.of(2026, 8, 12, 10, 30);

        reservationService.validateReservationTime(start, end, daytimeOptime(), BAY_ID);

        assertThat(Duration.between(start, end).toMinutes()).isEqualTo(60);
    }

    @Test
    void preservesSixtyMinuteCrossDayReservation() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 12, 23, 30);
        LocalDateTime end = LocalDateTime.of(2026, 8, 13, 0, 30);
        Carwash carwash = Carwash.builder().id(1L).price(6000).build();
        Bay bay = Bay.builder().id(BAY_ID).carwash(carwash).bayNum(1).status(1).build();
        given(bayRepository.findById(BAY_ID)).willReturn(java.util.Optional.of(bay));
        given(carwashRepository.findById(1L)).willReturn(java.util.Optional.of(carwash));
        given(optimeRepository.findByCarwash_Id(1L))
                .willReturn(Collections.singletonList(twentyFourHourOptime()));

        ReservationResponse.PayAmountDTO result = reservationService.findPayAmount(
                timeRequest(start.toString(), end.toString()), BAY_ID);

        assertThat(Duration.between(start, end).toMinutes()).isEqualTo(60);
        assertThat(result.getPrice()).isEqualTo(12000);
    }

    @Test
    void rejectsReverseDateTimeInsteadOfAssumingNextDay() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 12, 23, 30);
        LocalDateTime end = LocalDateTime.of(2026, 8, 12, 0, 30);

        assertThatThrownBy(() -> reservationService.validateReservationTime(
                start, end, twentyFourHourOptime(), BAY_ID))
                .isInstanceOf(BadRequestError.class)
                .satisfies(error -> assertThat(((BadRequestError) error).getErrors())
                        .containsEntry("datetime", "End time must be after start time."));
    }

    @Test
    void keepsThirtyAndSixtyMinuteDaytimePrices() {
        Carwash carwash = Carwash.builder().id(1L).price(6000).build();
        Bay bay = Bay.builder().id(BAY_ID).carwash(carwash).bayNum(1).status(1).build();
        given(bayRepository.findById(BAY_ID)).willReturn(java.util.Optional.of(bay));
        given(carwashRepository.findById(1L)).willReturn(java.util.Optional.of(carwash));
        given(optimeRepository.findByCarwash_Id(1L))
                .willReturn(Collections.singletonList(daytimeOptime()));

        ReservationResponse.PayAmountDTO thirtyMinutes = reservationService.findPayAmount(
                timeRequest("2026-08-12T09:30", "2026-08-12T10:00"), BAY_ID);
        ReservationResponse.PayAmountDTO sixtyMinutes = reservationService.findPayAmount(
                timeRequest("2026-08-12T10:30", "2026-08-12T11:30"), BAY_ID);

        assertThat(thirtyMinutes.getPrice()).isEqualTo(6000);
        assertThat(sixtyMinutes.getPrice()).isEqualTo(12000);
    }

    @Test
    void allowsNewReservationAtExistingReservationEndBoundary() {
        Reservation existing = Reservation.builder()
                .startTime(LocalDateTime.of(2026, 8, 12, 10, 0))
                .endTime(LocalDateTime.of(2026, 8, 12, 11, 0))
                .build();
        given(reservationRepository.findByBay_IdAndIsDeletedFalse(BAY_ID))
                .willReturn(Collections.singletonList(existing));

        reservationService.validateReservationTime(
                LocalDateTime.of(2026, 8, 12, 11, 0),
                LocalDateTime.of(2026, 8, 12, 11, 30),
                daytimeOptime(),
                BAY_ID);
    }

    @Test
    void rejectsOverlappingReservationAtAnotherCarwashForSameMember() {
        Member member = Member.builder().id(99L).build();
        Reservation existing = Reservation.builder()
                .id(10L)
                .member(member)
                .startTime(LocalDateTime.of(2026, 8, 12, 10, 0))
                .endTime(LocalDateTime.of(2026, 8, 12, 12, 0))
                .build();
        given(reservationRepository.findByMemberIdAndIsDeletedFalse(member.getId()))
                .willReturn(Collections.singletonList(existing));

        assertThatThrownBy(() -> reservationService.validateReservationTime(
                LocalDateTime.of(2026, 8, 12, 11, 0),
                LocalDateTime.of(2026, 8, 12, 12, 30),
                daytimeOptime(),
                2L,
                member))
                .isInstanceOf(BadRequestError.class)
                .satisfies(error -> assertThat(((BadRequestError) error).getErrors())
                        .containsEntry("Reservation time", "Reservation time overlaps with an existing reservation."));
    }

    private ReservationRequest.ReservationTimeDTO timeRequest(String start, String end) {
        ReservationRequest.ReservationTimeDTO dto = new ReservationRequest.ReservationTimeDTO();
        dto.setStartTime(LocalDateTime.parse(start));
        dto.setEndTime(LocalDateTime.parse(end));
        return dto;
    }

    private Optime daytimeOptime() {
        return Optime.builder()
                .dayType(DayType.WEEKDAY)
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(18, 30))
                .build();
    }

    private Optime twentyFourHourOptime() {
        return Optime.builder()
                .dayType(DayType.WEEKDAY)
                .startTime(LocalTime.MIDNIGHT)
                .endTime(LocalTime.MIDNIGHT)
                .build();
    }
}
