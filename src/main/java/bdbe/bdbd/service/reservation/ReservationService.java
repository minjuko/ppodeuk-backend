package bdbe.bdbd.service.reservation;


import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.exception.ForbiddenError;
import bdbe.bdbd._core.exception.NotFoundError;
import bdbe.bdbd._core.utils.DateUtils;
import bdbe.bdbd.dto.reservation.ReservationRequest;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.dto.reservation.ReservationResponse.ReservationInfoDTO;
import bdbe.bdbd.model.Code.DayType;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.file.File;
import bdbe.bdbd.model.location.Location;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.optime.Optime;
import bdbe.bdbd.model.reservation.Reservation;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.file.FileJPARepository;
import bdbe.bdbd.repository.location.LocationJPARepository;
import bdbe.bdbd.repository.optime.OptimeJPARepository;
import bdbe.bdbd.repository.reservation.ReservationJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ReservationService {
    private final ReservationJPARepository reservationJPARepository;
    private final CarwashJPARepository carwashJPARepository;
    private final BayJPARepository bayJPARepository;
    private final LocationJPARepository locationJPARepository;
    private final OptimeJPARepository optimeJPARepository;
    private final FileJPARepository fileJPARepository;

    @Transactional
    public Reservation save(ReservationRequest.SaveDTO dto, Long carwashId, Long bayId, Member sessionMember) {
        Carwash carwash = findCarwashById(carwashId);
        Optime optime = findOptime(carwash, dto.getStartTime());

        validateReservationTime(dto.getStartTime(), dto.getEndTime(), optime, bayId, sessionMember);

        Bay bay = findBayById(bayId);
        Reservation reservation = dto.toReservationEntity(carwash, bay, sessionMember);
        reservationJPARepository.save(reservation);

        return reservation;
    }

    @Transactional
    public void update(ReservationRequest.UpdateDTO dto, Long reservationId, Member member) {
        Reservation reservation = reservationJPARepository.findById(reservationId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("ReservationId", "Reservation not found")
                ));

        if (!reservation.getMember().getId().equals(member.getId()))
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not have permission to modify this reservation.")
            );

        Long carwashId = reservation.getBay().getCarwash().getId();
        Carwash carwash = carwashJPARepository.findById(carwashId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found" + carwashId)
                ));


        Long bayId = reservation.getBay().getId();
        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();
        Optime optime = findOptime(carwash, startTime);

        validateReservationTime(startTime, endTime, optime, bayId, member, reservationId);

        reservation.updateReservation(dto.getStartTime(), dto.getEndTime(), carwash);
    }

    @Transactional
    public void delete(Long reservationId, Member member) {
        Reservation reservation = reservationJPARepository.findById(reservationId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("ReservationId", "Reservation not found")
                ));

        if (!reservation.getMember().getId().equals(member.getId()))
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not have permission to modify this reservation.")
            );

        reservation.changeDeletedFlag(true);
    }


    private Carwash findCarwashById(Long id) {
        return carwashJPARepository.findById(id)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found")
                ));
    }

    public Optime findOptime(Carwash carwash, LocalDateTime startTime) {
        List<Optime> optimeList = optimeJPARepository.findByCarwash_Id(carwash.getId());
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        DayType dayType = DateUtils.getDayType(dayOfWeek);

        return optimeList.stream()
                .filter(o -> o.getDayType() == dayType)
                .findFirst()
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("Optime", "Optime not found")
                ));
    }

    public void validateReservationTime(LocalDateTime startTime, LocalDateTime endTime, Optime optime, Long bayId) {
        validateReservationTime(startTime, endTime, optime, bayId, null, null);
    }

    public void validateReservationTime(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Optime optime,
            Long bayId,
            Member member) {
        validateReservationTime(startTime, endTime, optime, bayId, member, null);
    }

    public void validateReservationTime(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Optime optime,
            Long bayId,
            Member member,
            Long excludedReservationId) {
        LocalTime opStartTime = optime.getStartTime();
        LocalTime opEndTime = optime.getEndTime();
        LocalTime requestStartTimePart = startTime.toLocalTime();
        LocalTime requestEndTimePart = endTime.toLocalTime();

        if (!endTime.isAfter(startTime)) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("datetime", "End time must be after start time.")
            );
        }

        if (startTime.getMinute() % 30 != 0 || endTime.getMinute() % 30 != 0) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("time", "Reservation time must be in 30-minute increments.")
            );
        }

        long minutesBetween = Duration.between(startTime, endTime).toMinutes();

        // 예약 시간에 대한 검증 로직
        if (minutesBetween < 30) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("duration", "Reservation duration must be at least 30 minutes.")
            );
        }

        if (minutesBetween % 30 != 0) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("duration", "Reservation duration must be a multiple of 30 minutes.")
            );
        }

        if (!((opStartTime.equals(LocalTime.MIDNIGHT) && opEndTime.equals(LocalTime.MIDNIGHT)) ||
                (opStartTime.isBefore(requestStartTimePart) || opStartTime.equals(requestStartTimePart)) &&
                        (opEndTime.isAfter(requestEndTimePart) || opEndTime.equals(requestEndTimePart)))) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("operatingHours", "Reservation time is out of operating hours")
            );
        }

        List<Reservation> reservationList = member == null
                ? reservationJPARepository.findByBay_IdAndIsDeletedFalse(bayId)
                : reservationJPARepository.findByMemberIdAndIsDeletedFalse(member.getId());

        boolean isOverlapping = reservationList.stream()
        .filter(existingReservation -> excludedReservationId == null
                || !excludedReservationId.equals(existingReservation.getId()))
        .anyMatch(existingReservation -> {
            LocalDateTime existingStartTime = existingReservation.getStartTime();
            LocalDateTime existingEndTime = existingReservation.getEndTime();
    
            // 새 예약이 기존 예약과 중복되는지 확인합니다.
            // 중복은 새 예약이 기존 예약의 시간과 겹칠 때 발생합니다.
            return !(endTime.isEqual(existingStartTime) || startTime.isEqual(existingEndTime)) &&
                   !(endTime.isBefore(existingStartTime) || startTime.isAfter(existingEndTime));
        });
        if (isOverlapping) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.DUPLICATE_RESOURCE,
                    Collections.singletonMap("Reservation time", "Reservation time overlaps with an existing reservation.")
            );
        }

    }

    private Bay findBayById(Long id) {
        return bayJPARepository.findById(id)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("BayId", "Bay not found")
                ));
    }


    public ReservationResponse.findAllResponseDTO findAllByCarwash(Long carwashId) {
        carwashJPARepository.findById(carwashId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found")
                ));
        List<Bay> bayList = bayJPARepository.findByCarwashIdAndStatus(carwashId);
        List<Long> bayIdList = bayJPARepository.findIdsByCarwashId(carwashId);
        List<Reservation> reservationList = reservationJPARepository.findByBayIdInAndIsDeletedFalse(bayIdList);

        return new ReservationResponse.findAllResponseDTO(bayList, reservationList);
    }

    public ReservationResponse.findLatestOneResponseDTO fetchLatestReservation(Long reservationId) {
        Reservation reservation = reservationJPARepository.findById(reservationId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("ReservationId", "Reservation not found")
                ));

        Bay bay = bayJPARepository.findById(reservation.getBay().getId())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("BayId", "Bay not found")
                ));

        Carwash carwash = carwashJPARepository.findById(bay.getCarwash().getId())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found")
                ));

        Location location = locationJPARepository.findById(carwash.getLocation().getId())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("LocationId", "Location not found")
                ));

        File file = fileJPARepository.findFirstByCarwashIdAndIsDeletedFalseOrderByUploadedAtAsc(carwash.getId()).orElse(null);

        return new ReservationResponse.findLatestOneResponseDTO(reservation, bay, carwash, location, file);
    }

    public ReservationResponse.fetchCurrentStatusReservationDTO findCurrentStatusReservation(Member sessionMember) {
        return findCurrentStatusReservation(sessionMember, null);
    }

    public ReservationResponse.fetchCurrentStatusReservationDTO findCurrentStatusReservation(Member sessionMember, LocalDateTime selectedAt) {
        List<Reservation> reservationList = reservationJPARepository.findByMemberIdAndIsDeletedFalse(sessionMember.getId());

        List<ReservationInfoDTO> current = new ArrayList<>();
        List<ReservationInfoDTO> upcoming = new ArrayList<>();
        List<ReservationInfoDTO> completed = new ArrayList<>();

        LocalDateTime now = selectedAt != null ? selectedAt : LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        // 예약 분류하기
        for (Reservation reservation : reservationList) {
            Bay bay = bayJPARepository.findById(reservation.getBay().getId())
                    .orElseThrow(() -> new NotFoundError(
                            NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                            Collections.singletonMap("BayId", "Bay not found")
                    ));
            Carwash carwash = carwashJPARepository.findById(bay.getCarwash().getId())
                    .orElseThrow(() -> new NotFoundError(
                            NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                            Collections.singletonMap("CarwashId", "Carwash not found")
                    ));

            LocalDateTime startDateTime = reservation.getStartTime();
            LocalDate reservationDate = startDateTime.toLocalDate();
            LocalDateTime endDateTime = reservation.getEndTime();

            if (reservationDate.equals(today)) {
                if (now.isAfter(startDateTime) && now.isBefore(endDateTime)) {
                    current.add(new ReservationInfoDTO(reservation, bay, carwash));
                } else if (now.isBefore(startDateTime)) {
                    upcoming.add(new ReservationInfoDTO(reservation, bay, carwash));
                } else if (now.isAfter(endDateTime)) {
                    completed.add(new ReservationInfoDTO(reservation, bay, carwash));
                }
            } else if (reservationDate.isBefore(today)) {
                completed.add(new ReservationInfoDTO(reservation, bay, carwash));
            } else if (reservationDate.isAfter(today)) {
                upcoming.add(new ReservationInfoDTO(reservation, bay, carwash));
            } else {
                throw new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("ReservationId", "Reservation not found")
                );
            }
        }

        return new ReservationResponse.fetchCurrentStatusReservationDTO(current, upcoming, completed);
    }

    public ReservationResponse.fetchRecentReservationDTO findRecentReservation(Member sessionMember) {
        return findRecentReservation(sessionMember, null);
    }

    public ReservationResponse.fetchRecentReservationDTO findRecentReservation(Member sessionMember, LocalDateTime selectedAt) {

        Pageable pageable = PageRequest.of(0, 5); // 최대 5개까지만 가져오기
        List<Reservation> reservationList = selectedAt == null
                ? reservationJPARepository.findByMemberIdJoinFetch(sessionMember.getId(), pageable)
                : reservationJPARepository.findByMemberIdJoinFetchBefore(sessionMember.getId(), selectedAt, pageable);
        List<ReservationResponse.RecentReservation> recentReservations = new ArrayList<>();

        for (Reservation reservation : reservationList) {
            Bay bay = bayJPARepository.findById(reservation.getBay().getId())
                    .orElseThrow(() -> new NotFoundError(
                            NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                            Collections.singletonMap("BayId", "Bay not found")
                    ));
            Carwash carwash = carwashJPARepository.findById(bay.getCarwash().getId())
                    .orElseThrow(() -> new NotFoundError(
                            NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                            Collections.singletonMap("CarwashId", "Carwash not found")
                    ));
            List<File> carwashImages = fileJPARepository.findByCarwash_IdAndIsDeletedFalse(carwash.getId());
            File carwashImage = carwashImages.stream().findFirst().orElse(null);

            recentReservations.add(new ReservationResponse.RecentReservation(reservation, carwashImage));
        }

        return new ReservationResponse.fetchRecentReservationDTO(recentReservations);
    }

    public ReservationResponse.PayAmountDTO findPayAmount(ReservationRequest.ReservationTimeDTO dto, Long bayId) {
        return findPayAmount(dto, bayId, null);
    }

    public ReservationResponse.PayAmountDTO findPayAmount(
            ReservationRequest.ReservationTimeDTO dto,
            Long bayId,
            Member member) {
        Bay bay = bayJPARepository.findById(bayId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("BayId", "Bay not found")
                ));

        Long carwashId = bay.getCarwash().getId();
        Carwash carwash = carwashJPARepository.findById(carwashId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found")
                ));

        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = dto.getEndTime();
        Optime optime = findOptime(carwash, startTime);

        validateReservationTime(startTime, endTime, optime, bayId, member);

        int perPrice = carwash.getPrice();
        int minutesDifference = (int) ChronoUnit.MINUTES.between(startTime, endTime); //시간 차 계산
        int blocksOf30Minutes = minutesDifference / 30; //30분 단위로 계산
        int totalPrice = perPrice * blocksOf30Minutes;

        return new ReservationResponse.PayAmountDTO(startTime, endTime, totalPrice);
    }
}

