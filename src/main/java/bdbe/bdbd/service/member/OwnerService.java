package bdbe.bdbd.service.member;

import bdbe.bdbd._core.exception.ForbiddenError;
import bdbe.bdbd._core.exception.InternalServerError;
import bdbe.bdbd._core.exception.NotFoundError;
import bdbe.bdbd._core.exception.UnAuthorizedError;
import bdbe.bdbd._core.security.JWTProvider;
import bdbe.bdbd._core.utils.MemberUtils;
import bdbe.bdbd.dto.member.owner.OwnerResponse;
import bdbe.bdbd.dto.member.owner.OwnerResponse.OwnerDashboardDTO;
import bdbe.bdbd.dto.member.user.UserRequest;
import bdbe.bdbd.dto.member.user.UserResponse;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.file.File;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.optime.Optime;
import bdbe.bdbd.model.reservation.Reservation;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.file.FileJPARepository;
import bdbe.bdbd.repository.member.MemberJPARepository;
import bdbe.bdbd.repository.optime.OptimeJPARepository;
import bdbe.bdbd.repository.reservation.ReservationJPARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.lang.String.valueOf;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service

public class OwnerService {
    private final PasswordEncoder passwordEncoder;
    private final MemberJPARepository memberJPARepository;
    private final CarwashJPARepository carwashJPARepository;
    private final ReservationJPARepository reservationJPARepository;
    private final OptimeJPARepository optimeJPARepository;
    private final BayJPARepository bayJPARepository;
    private final FileJPARepository fileJPARepository;
    private final MemberUtils memberUtils;

    @Transactional
    public void joinOwner(UserRequest.JoinDTO requestDTO) {
        memberUtils.checkSameEmail(requestDTO.getEmail());
        String encodedPassword = passwordEncoder.encode(requestDTO.getPassword());

        try {
            memberJPARepository.save(requestDTO.toOwnerEntity(encodedPassword));
        } catch (Exception e) {
            throw new InternalServerError(
                    InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR,
                    Collections.singletonMap("error", "Unknown server error occurred."));
        }
    }


    public UserResponse.LoginResponse loginOwner(UserRequest.LoginDTO requestDTO) {
        Member member = memberJPARepository.findByEmail(requestDTO.getEmail()).orElseThrow(
                () -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("Email", "email not found : " + requestDTO.getEmail())
                ));

        if (!passwordEncoder.matches(requestDTO.getPassword(), member.getPassword())) {
            throw new UnAuthorizedError(
                    UnAuthorizedError.ErrorCode.AUTHENTICATION_FAILED,
                    Collections.singletonMap("Password", "Wrong password")
            );
        }

        String userRole = valueOf(member.getRole());
        if (!"ROLE_OWNER".equals(userRole) && !"ROLE_ADMIN".equals(userRole)) {
            throw new UnAuthorizedError(
                    UnAuthorizedError.ErrorCode.ACCESS_DENIED,
                    Collections.singletonMap("Role", "Cannot access page by your Role")
            );
        }
        String jwt = JWTProvider.create(member);
        String redirectUrl = "/owner/home";

        return new UserResponse.LoginResponse(jwt, redirectUrl);
    }

    public OwnerResponse.SaleResponseDTO findSales(List<Long> carwashIds, LocalDate selectedDate, Member sessionMember) {
        return findSales(carwashIds, selectedDate, null, sessionMember);
    }

    public OwnerResponse.SaleResponseDTO findSales(List<Long> carwashIds, LocalDate selectedDate, LocalDateTime selectedAt, Member sessionMember) {
        validateCarwashOwnership(carwashIds, sessionMember);

        List<Carwash> carwashList = carwashJPARepository.findCarwashesByMemberId(sessionMember.getId());

        LocalDate normalizedDate = LocalDate.of(selectedDate.getYear(), selectedDate.getMonth(), selectedDate.getDayOfMonth());
        List<Reservation> reservationList = selectedAt == null
                ? reservationJPARepository.findAllByCarwash_IdInOrderByStartTimeDesc(carwashIds, normalizedDate)
                : reservationJPARepository.findAllByCarwash_IdInOrderByStartTimeDescBefore(carwashIds, normalizedDate, selectedAt);
        if (reservationList.isEmpty()) return new OwnerResponse.SaleResponseDTO(carwashList, new ArrayList<>());

        return new OwnerResponse.SaleResponseDTO(carwashList, reservationList);
    }

    public OwnerResponse.SaleResponseDTO findCarwashList(Member sessionMember) {
        List<Carwash> carwashList = carwashJPARepository.findCarwashesByMemberId(sessionMember.getId());

        return new OwnerResponse.SaleResponseDTO(carwashList, null);
    }

    public OwnerResponse.ReservationCarwashListDTO findBayReservation(Long bayId, Member sessionMember, LocalDate selectedDate) {
        validateBayOwnership(bayId, sessionMember);

        List<Reservation> reservationList = reservationJPARepository.findByBay_IdWithJoinsAndIsDeletedFalseAndMonthOrderByStartTimeDesc(bayId, selectedDate);
        Bay bay = bayJPARepository.findById(bayId)
                .orElseThrow(() -> {
                    throw new NotFoundError(
                            NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                            Collections.singletonMap("Bay", "bay not found")
                    );
                });

        return new OwnerResponse.ReservationCarwashListDTO(reservationList, bay.getBayNum());
    }

    private void validateCarwashOwnership(List<Long> carwashIds, Member sessionMember) {
        List<Long> userCarwashIdList = carwashJPARepository.findCarwashIdsByMemberId(sessionMember.getId());

        if (!userCarwashIdList.containsAll(carwashIds)) {
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not the owner of the carwash.")
            );
        }
    }

    private void validateBayOwnership(Long bayId, Member sessionMember) {
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

        if (carwash.getMember().getId() != sessionMember.getId()) {
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("CarwashId", "Member is not the owner of the carwash.")
            );
        }
    }


    public Map<String, Long> findMonthRevenue(List<Long> carwashIds, LocalDate selectedDate, Member sessionMember) {
        List<Carwash> carwashList = carwashJPARepository.findAllByIdInAndMember_Id(carwashIds, sessionMember.getId());
        if (carwashIds.size() != carwashList.size())
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("CarwashIds", "Member is not the owner of the carwash.")
            );

        Map<String, Long> response = new HashMap<>();
        Long revenue = reservationJPARepository.findTotalRevenueByCarwashIdsAndDate(carwashIds, LocalDate.of(selectedDate.getYear(), selectedDate.getMonth(), selectedDate.getDayOfMonth()));
        response.put("revenue", revenue);

        return response;
    }

    public OwnerResponse.ReservationOverviewResponseDTO fetchOwnerReservationOverview(Member sessionMember) {
        return fetchOwnerReservationOverview(sessionMember, null);
    }

    public OwnerResponse.ReservationOverviewResponseDTO fetchOwnerReservationOverview(Member sessionMember, LocalDate selectedDate) {
        List<Carwash> carwashList = carwashJPARepository.findByMember_Id(sessionMember.getId());

        OwnerResponse.ReservationOverviewResponseDTO response = new OwnerResponse.ReservationOverviewResponseDTO();

        for (Carwash carwash : carwashList) {
            List<Bay> bayList = bayJPARepository.findByCarwashId(carwash.getId());
            List<Optime> optimeList = optimeJPARepository.findByCarwash_Id(carwash.getId());

            Date today = java.sql.Date.valueOf(selectedDate != null ? selectedDate : LocalDate.now());
            List<Reservation> reservationList = reservationJPARepository.findTodaysReservationsByCarwashId(carwash.getId(), today);

            List<File> carwashImageList = fileJPARepository.findByCarwash_IdAndIsDeletedFalse(carwash.getId());

            OwnerResponse.CarwashManageByOwnerDTO dto = new OwnerResponse.CarwashManageByOwnerDTO(carwash, bayList, optimeList, reservationList, carwashImageList);
            response.addCarwashManageByOwnerDTO(dto);
        }

        return response;
    }

    public OwnerResponse.CarwashManageDTO findCarwashReservationOverview(Long carwashId, Member sessionMember) {
        return findCarwashReservationOverview(carwashId, sessionMember, null);
    }

    public OwnerResponse.CarwashManageDTO findCarwashReservationOverview(Long carwashId, Member sessionMember, LocalDate selectedDate) {
        Carwash carwash = carwashJPARepository.findByIdAndMember_Id(carwashId, sessionMember.getId())
                .orElseThrow(() -> new ForbiddenError(
                        ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                        Collections.singletonMap("CarwashId", "Member is not the owner of the carwash.")
                ));

        LocalDate referenceDate = selectedDate != null ? selectedDate : LocalDate.now();
        LocalDate firstDayOfCurrentMonth = referenceDate.withDayOfMonth(1);

        Long monthlySales = reservationJPARepository.findTotalRevenueByCarwashIdAndDate(carwashId, firstDayOfCurrentMonth);
        Long monthlyReservations = reservationJPARepository.findMonthlyReservationCountByCarwashIdAndDate(carwashId, firstDayOfCurrentMonth);

        List<Bay> bayList = bayJPARepository.findByCarwashId(carwash.getId());
        List<Optime> optimeList = optimeJPARepository.findByCarwash_Id(carwash.getId());

        Date today = java.sql.Date.valueOf(selectedDate != null ? selectedDate : LocalDate.now());
        List<Reservation> reservationList = reservationJPARepository.findTodaysReservationsByCarwashId(carwash.getId(), today);

        List<File> carwashImageList = fileJPARepository.findByCarwash_IdAndIsDeletedFalse(carwash.getId());
        File carwashImage = carwashImageList.stream().findFirst().orElse(null);

        OwnerResponse.CarwashManageDTO dto = new OwnerResponse.CarwashManageDTO(carwash, monthlySales, monthlyReservations, bayList, optimeList, reservationList, carwashImage);

        return dto;
    }

    public double calculateGrowthPercentage(Long currentValue, Long previousValue) {
        if (previousValue == 0 && currentValue == 0) {
            return 0;
        } else if (previousValue == 0) {
            return 100;
        }
        return ((double) (currentValue - previousValue) / previousValue) * 100;
    }

    public OwnerDashboardDTO fetchOwnerHomepage(Member sessionMember) {
        return fetchOwnerHomepage(sessionMember, null);
    }

    public OwnerDashboardDTO fetchOwnerHomepage(Member sessionMember, LocalDate selectedDate) {
        List<Long> carwashIdList = carwashJPARepository.findCarwashIdsByMemberId(sessionMember.getId());
        LocalDate referenceDate = selectedDate != null ? selectedDate : LocalDate.now();
        LocalDate firstDayOfCurrentMonth = referenceDate.withDayOfMonth(1);
        LocalDate firstDayOfPreviousMonth = referenceDate.minusMonths(1).withDayOfMonth(1);

        Long currentMonthSales = reservationJPARepository.findTotalRevenueByCarwashIdsAndDate(carwashIdList, firstDayOfCurrentMonth);
        Long previousMonthSales = reservationJPARepository.findTotalRevenueByCarwashIdsAndDate(carwashIdList, firstDayOfPreviousMonth);
        Long currentMonthReservations = reservationJPARepository.findMonthlyReservationCountByCarwashIdsAndDate(carwashIdList, firstDayOfCurrentMonth);
        Long previousMonthReservations = reservationJPARepository.findMonthlyReservationCountByCarwashIdsAndDate(carwashIdList, firstDayOfPreviousMonth);

        double salesGrowthPercentage = calculateGrowthPercentage(currentMonthSales, previousMonthSales); // 전월대비 판매 성장률 (단위: %)
        double reservationGrowthPercentage = calculateGrowthPercentage(currentMonthReservations, previousMonthReservations); // 전월대비 예약 성장률 (단위: %)

        List<OwnerResponse.CarwashInfoDTO> carwashInfoDTOList = new ArrayList<>();
        for (Long carwashId : carwashIdList) {
            Carwash carwash = carwashJPARepository.findById(carwashId)
                    .orElseThrow(() -> new NotFoundError(
                            NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                            Collections.singletonMap("carwashId", "Carwash not found")
                    ));
            Long monthlySales = reservationJPARepository.findTotalRevenueByCarwashIdAndDate(carwashId, firstDayOfCurrentMonth);

            Long monthlyReservations = reservationJPARepository.findMonthlyReservationCountByCarwashIdAndDate(carwashId, firstDayOfCurrentMonth);
            List<File> carwashImageList = fileJPARepository.findByCarwash_IdAndIsDeletedFalse(carwashId);

            OwnerResponse.CarwashInfoDTO dto = new OwnerResponse.CarwashInfoDTO(carwash, monthlySales, monthlyReservations, carwashImageList);
            carwashInfoDTOList.add(dto);
        }
        return new OwnerDashboardDTO(currentMonthSales, salesGrowthPercentage, currentMonthReservations, reservationGrowthPercentage, carwashInfoDTOList);
    }
}
