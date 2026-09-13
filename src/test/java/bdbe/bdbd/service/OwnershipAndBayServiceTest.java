package bdbe.bdbd.service;

import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.exception.ForbiddenError;
import bdbe.bdbd._core.exception.NotFoundError;
import bdbe.bdbd._core.utils.FileUploadUtil;
import bdbe.bdbd._core.utils.MemberUtils;
import bdbe.bdbd.model.Code.DayType;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.file.File;
import bdbe.bdbd.model.location.Location;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.optime.Optime;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.file.FileJPARepository;
import bdbe.bdbd.repository.keyword.KeywordJPARepository;
import bdbe.bdbd.repository.keyword.carwashKeyword.CarwashKeywordJPARepository;
import bdbe.bdbd.repository.location.LocationJPARepository;
import bdbe.bdbd.repository.member.MemberJPARepository;
import bdbe.bdbd.repository.optime.OptimeJPARepository;
import bdbe.bdbd.repository.reservation.ReservationJPARepository;
import bdbe.bdbd.repository.review.ReviewJPARepository;
import bdbe.bdbd.service.bay.BayService;
import bdbe.bdbd.service.carwash.CarwashService;
import bdbe.bdbd.service.file.FileService;
import bdbe.bdbd.service.member.OwnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OwnershipAndBayServiceTest {

    private BayJPARepository bayRepository;
    private CarwashJPARepository carwashRepository;
    private ReservationJPARepository reservationRepository;
    private FileJPARepository fileRepository;
    private LocationJPARepository locationRepository;
    private OptimeJPARepository optimeRepository;
    private BayService bayService;
    private CarwashService carwashService;
    private FileService fileService;
    private OwnerService ownerService;

    @BeforeEach
    void setUp() {
        bayRepository = mock(BayJPARepository.class);
        carwashRepository = mock(CarwashJPARepository.class);
        reservationRepository = mock(ReservationJPARepository.class);
        fileRepository = mock(FileJPARepository.class);
        locationRepository = mock(LocationJPARepository.class);
        optimeRepository = mock(OptimeJPARepository.class);
        fileService = new FileService(fileRepository, mock(FileUploadUtil.class));
        bayService = new BayService(bayRepository, carwashRepository, reservationRepository);
        carwashService = new CarwashService(
                carwashRepository, mock(KeywordJPARepository.class), locationRepository,
                optimeRepository, mock(CarwashKeywordJPARepository.class), mock(ReviewJPARepository.class),
                bayRepository, fileRepository, fileService);
        ownerService = new OwnerService(
                mock(PasswordEncoder.class), mock(MemberJPARepository.class), carwashRepository,
                reservationRepository, optimeRepository, bayRepository, fileRepository, mock(MemberUtils.class));
    }

    @Test
    void bayOwnerWithEqualNonCachedLongValueCanDeleteBay() {
        Bay bay = ownedBay(Long.valueOf("1000"));
        given(bayRepository.findById(10L)).willReturn(Optional.of(bay));
        given(reservationRepository.findByBay_IdAndIsDeletedFalse(10L)).willReturn(Collections.emptyList());

        assertThatCode(() -> bayService.deleteBay(10L, member(Long.valueOf("1000"))))
                .doesNotThrowAnyException();
        verify(bayRepository).delete(bay);
    }

    @Test
    void deleteBayRejectsMissingBay() {
        given(bayRepository.findById(10L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> bayService.deleteBay(10L, member(1000L)))
                .isInstanceOf(NotFoundError.class);
    }

    @Test
    void deleteBayRejectsDifferentOwner() {
        given(bayRepository.findById(10L)).willReturn(Optional.of(ownedBay(1000L)));
        assertThatThrownBy(() -> bayService.deleteBay(10L, member(2000L)))
                .isInstanceOf(ForbiddenError.class);
    }

    @Test
    void deleteBayRejectsAnyActiveReservation() {
        given(bayRepository.findById(10L)).willReturn(Optional.of(ownedBay(1000L)));
        given(reservationRepository.findByBay_IdAndIsDeletedFalse(10L))
                .willReturn(Collections.singletonList(mock(bdbe.bdbd.model.reservation.Reservation.class)));
        assertThatThrownBy(() -> bayService.deleteBay(10L, member(1000L)))
                .isInstanceOf(BadRequestError.class);
    }

    @Test
    void carwashOwnershipUsesLongValueEqualityAndStillRejectsDifferentOwner() {
        Carwash carwash = carwash(Long.valueOf("1000"));
        given(carwashRepository.findById(20L)).willReturn(Optional.of(carwash));
        given(locationRepository.findById(30L)).willReturn(Optional.of(carwash.getLocation()));
        given(optimeRepository.findByCarwash_Id(20L)).willReturn(Arrays.asList(
                optime(DayType.WEEKDAY, carwash), optime(DayType.WEEKEND, carwash)));
        given(fileRepository.findByCarwash_IdAndIsDeletedFalse(20L)).willReturn(Collections.emptyList());

        assertThatCode(() -> carwashService.findCarwashByDetails(20L, member(Long.valueOf("1000"))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> carwashService.findCarwashByDetails(20L, member(2000L)))
                .isInstanceOf(ForbiddenError.class);
    }

    @Test
    void fileOwnershipUsesLongValueEqualityAndStillRejectsDifferentOwner() {
        File file = File.builder().id(40L).carwash(carwash(Long.valueOf("1000"))).build();
        given(fileRepository.findById(40L)).willReturn(Optional.of(file));

        assertThatCode(() -> fileService.deleteFile(40L, member(Long.valueOf("1000"))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> fileService.deleteFile(40L, member(2000L)))
                .isInstanceOf(ForbiddenError.class);
    }

    @Test
    void ownerBayLookupUsesLongValueEqualityAndStillRejectsDifferentOwner() {
        Bay bay = ownedBay(Long.valueOf("1000"));
        given(bayRepository.findById(10L)).willReturn(Optional.of(bay));
        given(carwashRepository.findById(20L)).willReturn(Optional.of(bay.getCarwash()));
        given(reservationRepository.findByBay_IdWithJoinsAndIsDeletedFalseAndMonthOrderByStartTimeDesc(10L, null))
                .willReturn(Collections.emptyList());

        assertThatCode(() -> ownerService.findBayReservation(10L, member(Long.valueOf("1000")), null))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ownerService.findBayReservation(10L, member(2000L), null))
                .isInstanceOf(ForbiddenError.class);
    }

    private Member member(Long id) {
        return Member.builder().id(id).build();
    }

    private Carwash carwash(Long ownerId) {
        return Carwash.builder().id(20L).member(member(ownerId))
                .location(Location.builder().id(30L).address("test").build())
                .name("test").tel("test").des("test").build();
    }

    private Bay ownedBay(Long ownerId) {
        return Bay.builder().id(10L).bayNum(1).carwash(carwash(ownerId)).status(1).build();
    }

    private Optime optime(DayType type, Carwash carwash) {
        return Optime.builder().dayType(type).startTime(LocalTime.MIN).endTime(LocalTime.MAX).carwash(carwash).build();
    }
}
