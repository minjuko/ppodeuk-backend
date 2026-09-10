package bdbe.bdbd.controller.owner;

import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.security.CustomUserDetails;
import bdbe.bdbd._core.utils.ApiUtils;
import bdbe.bdbd.dto.carwash.CarwashRequest;
import bdbe.bdbd.dto.carwash.CarwashResponse;
import bdbe.bdbd.dto.member.owner.OwnerResponse;
import bdbe.bdbd.service.carwash.CarwashService;
import bdbe.bdbd.service.file.FileService;
import bdbe.bdbd.service.member.OwnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 세차장 관련 기능을 제공하는 사장님 API
 * - 모든 자동세차장 목록 조회
 * - 키워드 기반 자동세차장 검색
 * - 사용자 위치 기반 근처 자동세차장 조회
 * - 추천 자동세차장 조회
 * - 특정 자동세차장 정보 조회
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/owner")
public class OwnerCarwashController {

    private final CarwashService carwashService;

    private final FileService fileService;

    private final OwnerService ownerService;

    @PostMapping(value = "/carwashes/register")
    public ResponseEntity<?> saveCarwash(@Valid @RequestPart("carwash") CarwashRequest.SaveDTO saveDTOs,
                                         @RequestPart(value = "images", required = false) Optional<MultipartFile[]> images,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (images.isPresent()) {
            for (MultipartFile file : images.get()) {
                if (file.isEmpty()) {
                    throw new BadRequestError(
                            BadRequestError.ErrorCode.MISSING_PART,
                            Collections.singletonMap("images", "Empty image file is not allowed"));
                }
            }
        }
        carwashService.saveCarwash(saveDTOs, images.orElse(null), userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(null));
    }


    @GetMapping("/carwashes/{carwash-id}/details")
    public ResponseEntity<?> findCarwashByDetails(@PathVariable("carwash-id") Long carwashId,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        CarwashResponse.carwashDetailsDTO carwashDetailsDTO = carwashService.findCarwashByDetails(carwashId, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(carwashDetailsDTO));
    }

    @DeleteMapping("/images/{image-id}")
    public ResponseEntity<?> deleteImage(
            @PathVariable("image-id") Long imageId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        fileService.deleteFile(imageId, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(null));
    }

    @PutMapping("/carwashes/{carwash-id}/details")
    public ResponseEntity<?> updateCarwashDetails(
            @PathVariable("carwash-id") Long carwashId,
            @Valid @RequestPart("updateData") CarwashRequest.updateCarwashDetailsDTO updatedto,
            @RequestPart(value = "imageFileList", required = false) MultipartFile[] imageFileList,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (imageFileList != null) {
            for (MultipartFile file : imageFileList) {
                if (file.isEmpty()) {
                    throw new BadRequestError(
                            BadRequestError.ErrorCode.MISSING_PART,
                            Collections.singletonMap("images", "Empty image file is not allowed")
                    );
                }
            }
        }
        CarwashResponse.updateCarwashDetailsResponseDTO updateCarwashDetailsDTO =
                carwashService.updateCarwashDetails(carwashId, updatedto, imageFileList, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(updateCarwashDetailsDTO));
    }

    @GetMapping("/carwashes")
    public ResponseEntity<?> fetchOwnerReservationOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OwnerResponse.ReservationOverviewResponseDTO dto = ownerService.fetchOwnerReservationOverview(userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(dto));
    }

    @GetMapping("/sales")
    public ResponseEntity<?> findAllOwnerReservation(
            @RequestParam(value = "carwash-ids", required = false) List<Long> carwashIds,
            @RequestParam(value = "selected-date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate selectedDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (carwashIds == null || carwashIds.isEmpty()) {
            OwnerResponse.SaleResponseDTO dto = ownerService.findCarwashList(userDetails.getMember());

            return ResponseEntity.ok(ApiUtils.success(dto));
        }
        OwnerResponse.SaleResponseDTO saleResponseDTO = ownerService.findSales(carwashIds, selectedDate, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(saleResponseDTO));
    }

    @GetMapping("/carwashes/{carwash-id}")
    public ResponseEntity<?> fetchCarwashReservationOverview(
            @PathVariable("carwash-id") Long carwashId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OwnerResponse.CarwashManageDTO dto = ownerService.findCarwashReservationOverview(carwashId, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(dto));
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> findMonthRevenueByCarwash(
            @RequestParam(value = "carwash-ids") List<Long> carwashIds,
            @RequestParam(value = "selected-date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate selectedDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Long> map = ownerService.findMonthRevenue(carwashIds, selectedDate, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(map));
    }

    @GetMapping("/reservations/{bay-id}")
    public ResponseEntity<?> fetchOwnerReservation(
            @PathVariable("bay-id") Long bayId,
            @RequestParam(value = "selected-date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate selectedDate,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OwnerResponse.ReservationCarwashListDTO dto = ownerService.findBayReservation(bayId, userDetails.getMember(), selectedDate);

        return ResponseEntity.ok(ApiUtils.success(dto));
    }

    @GetMapping("/home")
    public ResponseEntity<?> fetchOwnerHomepage(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        OwnerResponse.OwnerDashboardDTO dto = ownerService.fetchOwnerHomepage(userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(dto));
    }
}




