package bdbe.bdbd.service.bay;


import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.exception.ForbiddenError;
import bdbe.bdbd._core.exception.NotFoundError;
import bdbe.bdbd.dto.bay.BayRequest;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.reservation.ReservationJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

import static bdbe.bdbd.dto.bay.BayResponse.BayRevenueResponseDTO;

@Transactional
@RequiredArgsConstructor
@Service
public class BayService {
    private final BayJPARepository bayJPARepository;
    private final CarwashJPARepository carwashJPARepository;
    private final ReservationJPARepository reservationJPARepository;

    public void createBay(BayRequest.SaveDTO dto, Long carwashId, Member member) {
        Carwash carwash = carwashJPARepository.findById(carwashId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "Carwash not found")
                ));

        if (!Objects.equals(carwash.getMember().getId(), member.getId())) {
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not the owner of the carwash.")
            );
        }

        int bayNum = dto.getBayNum();
        boolean exists = bayJPARepository.findByCarwashId(carwashId)
                .stream()
                .anyMatch(bay -> bay.getBayNum() == bayNum);

        if (exists) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("BayNum", "Bay number " + bayNum + " is already in use.")
            );
        }

        Bay bay = dto.toBayEntity(carwash);
        bayJPARepository.save(bay);
    }


    public void changeStatus(Long bayId, int status, Member member) {
        Bay bay = bayJPARepository.findById(bayId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("Bay", "Bay not found")
                ));

        if (!Objects.equals(bay.getCarwash().getMember().getId(), member.getId())) {
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not the owner of the carwash.")
            );
        }
        bay.changeStatus(status);
    }

    public void deleteBay(Long bayId, Member member) {
        Bay bay = bayJPARepository.findById(bayId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("Bay", "Bay not found")
                ));

        if (!Objects.equals(bay.getCarwash().getMember().getId(), member.getId())) {
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not the owner of the carwash.")
            );
        }

        if (!reservationJPARepository.findByBay_IdAndIsDeletedFalse(bayId).isEmpty()) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("Bay", "예약이 있는 베이는 삭제할 수 없습니다.")
            );
        }
        bayJPARepository.delete(bay);
    }

    public BayRevenueResponseDTO findBayRevenue(Long bayId, Member member) {
        return findBayRevenue(bayId, member, null);
    }

    public BayRevenueResponseDTO findBayRevenue(Long bayId, Member member, LocalDate selectedDate) {
        Bay bay = bayJPARepository.findById(bayId)
                .orElseThrow(() -> {
                    throw new NotFoundError(
                            NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                            Collections.singletonMap("BayId", "Bay not found"));
                });

        if (!Objects.equals(bay.getCarwash().getMember().getId(), member.getId())) {
            throw new ForbiddenError(
                        ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                        Collections.singletonMap("MemberId", "Member is not the owner of the carwash."));
        }

        BayRevenueResponseDTO dto = new BayRevenueResponseDTO();

        LocalDate firstDayOfCurrentMonth = (selectedDate != null ? selectedDate : LocalDate.now()).withDayOfMonth(1);
        Long monthlyReservationCountByBayIdAndDate = reservationJPARepository.findMonthlyReservationCountByBayIdAndDate(bayId, firstDayOfCurrentMonth);
        dto.setReservationCnt(monthlyReservationCountByBayIdAndDate);

        Long totalRevenueByBayIdAndDate = reservationJPARepository.findTotalRevenueByBayIdAndDate(bayId, firstDayOfCurrentMonth);
        dto.setRevenue(totalRevenueByBayIdAndDate);

        return dto;
    }
}
