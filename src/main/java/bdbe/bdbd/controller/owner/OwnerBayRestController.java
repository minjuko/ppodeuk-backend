package bdbe.bdbd.controller.owner;

import bdbe.bdbd._core.security.CustomUserDetails;
import bdbe.bdbd._core.utils.ApiUtils;
import bdbe.bdbd.dto.bay.BayRequest;
import bdbe.bdbd.dto.bay.BayResponse;
import bdbe.bdbd.service.bay.BayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.Valid;

/**
 * 세차장의 '베이'(세차 작업 공간) 관련 기능을 처리하는 사장님 API
 * - 베이 생성: 특정 세차장에 새로운 베이를 생성합니다.
 * - 베이 상태 업데이트: 특정 베이의 상태(예: 사용 가능, 사용 불가 등)를 업데이트합니다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/owner")
public class OwnerBayRestController {

    private final BayService bayService;

    @PostMapping("/carwashes/{carwash-id}/bays")
    public ResponseEntity<?> createBay(
            @PathVariable("carwash-id") Long carwashId,
            @Valid @RequestBody BayRequest.SaveDTO saveDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        bayService.createBay(saveDTO, carwashId, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(null));
    }

    @PutMapping("/bays/{bay-id}/status")
    public ResponseEntity<?> findBayRevenue(
            @PathVariable("bay-id") Long bayId,
            @RequestParam int status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        bayService.changeStatus(bayId, status, userDetails.getMember());

        return ResponseEntity.ok(ApiUtils.success(null));
    }

    @DeleteMapping("/bays/{bay-id}")
    public ResponseEntity<?> deleteBay(
            @PathVariable("bay-id") Long bayId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        bayService.deleteBay(bayId, userDetails.getMember());
        return ResponseEntity.ok(ApiUtils.success(null));
    }

    @GetMapping("/bays/{bay-id}/revenue")
    public ResponseEntity<?> findBayRevenue(
            @PathVariable("bay-id") Long bayId,
            @RequestParam(value = "selected-date", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate selectedDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BayResponse.BayRevenueResponseDTO dto = bayService.findBayRevenue(bayId, userDetails.getMember(), selectedDate);

        return ResponseEntity.ok(ApiUtils.success(dto));
    }
}
