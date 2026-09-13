package bdbe.bdbd.service.carwash;


import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.exception.ForbiddenError;
import bdbe.bdbd._core.exception.NotFoundError;
import bdbe.bdbd._core.utils.Haversine;
import bdbe.bdbd.dto.carwash.CarwashRequest;
import bdbe.bdbd.dto.carwash.CarwashResponse;
import bdbe.bdbd.dto.carwash.CarwashResponse.updateCarwashDetailsResponseDTO;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.Code.DayType;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.file.File;
import bdbe.bdbd.model.keyword.Keyword;
import bdbe.bdbd.model.keyword.carwashKeyword.CarwashKeyword;
import bdbe.bdbd.model.location.Location;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.optime.Optime;
import bdbe.bdbd.repository.bay.BayJPARepository;
import bdbe.bdbd.repository.carwash.CarwashJPARepository;
import bdbe.bdbd.repository.file.FileJPARepository;
import bdbe.bdbd.repository.keyword.KeywordJPARepository;
import bdbe.bdbd.repository.keyword.carwashKeyword.CarwashKeywordJPARepository;
import bdbe.bdbd.repository.location.LocationJPARepository;
import bdbe.bdbd.repository.optime.OptimeJPARepository;
import bdbe.bdbd.repository.review.ReviewJPARepository;
import bdbe.bdbd.service.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class CarwashService {
    private final CarwashJPARepository carwashJPARepository;
    private final KeywordJPARepository keywordJPARepository;
    private final LocationJPARepository locationJPARepository;
    private final OptimeJPARepository optimeJPARepository;
    private final CarwashKeywordJPARepository carwashKeywordJPARepository;
    private final ReviewJPARepository reviewJPARepository;
    private final BayJPARepository bayJPARepository;
    private final FileJPARepository fileJPARepository;
    private final FileService fileService;

    public List<CarwashResponse.FindAllDTO> findAll(int page) {
        if (page < 0) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("Page", "Invalid page number")
            );
        }

        Pageable pageable = PageRequest.of(page, 10);
        Page<Carwash> carwashEntities = carwashJPARepository.findAll(pageable);

        if (carwashEntities == null || !carwashEntities.hasContent()) {
            throw new NotFoundError(
                    NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                    Collections.singletonMap("CarwashEntities", "CarwashEntities not found")
            );
        }

        List<CarwashResponse.FindAllDTO> carwashResponses = carwashEntities.getContent().stream()
                .map(CarwashResponse.FindAllDTO::new)
                .collect(Collectors.toList());

        if (carwashResponses == null || carwashResponses.isEmpty()) {
            throw new NotFoundError(
                    NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                    Collections.singletonMap("CarwashEntities", "No carwash entities transformed")
            );
        }

        return carwashResponses;
    }

    @Transactional
    public void saveCarwash(CarwashRequest.SaveDTO saveDTO, MultipartFile[] images, Member sessionMember) {
        Location location = saveDTO.toLocationEntity();
        location = locationJPARepository.save(location);

        Carwash carwash = saveDTO.toCarwashEntity(location, sessionMember);
        carwashJPARepository.save(carwash);

        List<Optime> optimes = saveDTO.toOptimeEntities(carwash);
        optimeJPARepository.saveAll(optimes);

        List<Long> keywordIdList = saveDTO.getKeywordIdList();
        if (keywordIdList != null && !keywordIdList.isEmpty()) {
            if (keywordIdList.stream().anyMatch(id -> id < 8 || id > 14)) {
                throw new BadRequestError(
                        BadRequestError.ErrorCode.VALIDATION_FAILED,
                        Collections.singletonMap("message", "Carwash Keyword ID must be between 8 and 14")
                );
            }
            List<CarwashKeyword> carwashKeywordList = new ArrayList<>();
            for (Long keywordId : keywordIdList) {
                Keyword keyword = keywordJPARepository.findById(keywordId)
                        .orElseThrow(() -> new NotFoundError(
                                NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                                Collections.singletonMap("message", "Carwash Keyword not found")));

                CarwashKeyword carwashKeyword = CarwashKeyword.builder().carwash(carwash).keyword(keyword).build();
                carwashKeywordList.add(carwashKeyword);
            }
            carwashKeywordJPARepository.saveAll(carwashKeywordList);
        }

        if (images != null && images.length > 0) {
            fileService.uploadAndSaveFiles(images, carwash);
        }
    }

    public List<CarwashRequest.CarwashDistanceDTO> findNearbyCarwashesByUserLocation(CarwashRequest.UserLocationDTO userLocation) {
        List<Carwash> carwashes = carwashJPARepository.findCarwashesWithin10Kilometers(userLocation.getLatitude(), userLocation.getLongitude());

        return carwashes.stream()
                .map(carwash -> {
                    double distance = Haversine.distance(userLocation.getLatitude(), userLocation.getLongitude(),
                            carwash.getLocation().getLatitude(), carwash.getLocation().getLongitude());
                    double rate = carwash.getRate();
                    long reviewCount = reviewJPARepository.countByCarwash_Id(carwash.getId());
                    int price = carwash.getPrice();

                    File file = fileJPARepository.findFirstByCarwashIdAndIsDeletedFalseOrderByUploadedAtAsc(carwash.getId()).orElse(null);
                    CarwashRequest.CarwashDistanceDTO dto = new CarwashRequest.CarwashDistanceDTO(carwash.getId(), carwash.getName(), carwash.getLocation(), distance, rate, reviewCount, price, file);

                    return dto;
                })
                .sorted(Comparator.comparingDouble(CarwashRequest.CarwashDistanceDTO::getDistance))
                .collect(Collectors.toList());
    }

    public CarwashRequest.CarwashDistanceDTO findNearestCarwashByUserLocation(CarwashRequest.UserLocationDTO userLocation) {
        List<Carwash> carwashes = carwashJPARepository.findCarwashesWithin10Kilometers(userLocation.getLatitude(), userLocation.getLongitude());

        return carwashes.stream()
                .map(carwash -> {
                    double distance = Haversine.distance(userLocation.getLatitude(), userLocation.getLongitude(),
                            carwash.getLocation().getLatitude(), carwash.getLocation().getLongitude());
                    double rate = carwash.getRate();
                    long reviewCount = reviewJPARepository.countByCarwash_Id(carwash.getId());
                    int price = carwash.getPrice();

                    File file = fileJPARepository.findFirstByCarwashIdAndIsDeletedFalseOrderByUploadedAtAsc(carwash.getId()).orElse(null);
                    CarwashRequest.CarwashDistanceDTO dto = new CarwashRequest.CarwashDistanceDTO(carwash.getId(), carwash.getName(), carwash.getLocation(), distance, rate, reviewCount, price, file);

                    return dto;
                })
                .min(Comparator.comparingDouble(CarwashRequest.CarwashDistanceDTO::getDistance))
                .orElse(null);
    }

    public List<CarwashRequest.CarwashDistanceDTO> findCarwashesByKeywords(CarwashRequest.SearchRequestDTO searchRequest) {
        // 키워드 ID 범위 검증
        List<Long> keywordIds = searchRequest.getKeywordIds();
        if (keywordIds.stream().anyMatch(id -> id < 8 || id > 14)) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("message", "Carwash Keyword ID must be between 8 and 14")
            );
        }

        List<Carwash> carwashesWithin10Km = carwashJPARepository.findCarwashesWithin10Kilometers(searchRequest.getLatitude(), searchRequest.getLongitude());

        List<Keyword> selectedKeywords = keywordJPARepository.findAllById(keywordIds);
        if (keywordIds.size() != selectedKeywords.size()) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("KeywordId", "KeywordId is invalid")
            );
        }

        List<CarwashRequest.CarwashDistanceDTO> result = carwashesWithin10Km.stream()
                .filter(carwash -> {
                    List<Long> keywordIdsForCarwash = findKeywordIdsByCarwashId(carwash.getId());
                    return keywordIds.stream()
                            .allMatch(keywordIdsForCarwash::contains);
                })
                .map(carwash -> {
                    double distance = Haversine.distance(
                            searchRequest.getLatitude(), searchRequest.getLongitude(),
                            carwash.getLocation().getLatitude(), carwash.getLocation().getLongitude()
                    );
                    double rate = carwash.getRate();
                    long reviewCount = reviewJPARepository.countByCarwash_Id(carwash.getId());
                    int price = carwash.getPrice();

                    File file = fileJPARepository.findFirstByCarwashIdAndIsDeletedFalseOrderByUploadedAtAsc(carwash.getId()).orElse(null);
                    CarwashRequest.CarwashDistanceDTO dto = new CarwashRequest.CarwashDistanceDTO(carwash.getId(), carwash.getName(), carwash.getLocation(), distance, rate, reviewCount, price, file);

                    return dto;
                })
                .sorted(Comparator.comparingDouble(CarwashRequest.CarwashDistanceDTO::getDistance))
                .collect(Collectors.toList());

        return result;
    }

    public List<Long> findKeywordIdsByCarwashId(Long carwashId) {
        return carwashKeywordJPARepository.findKeywordIdsByCarwashId(carwashId);
    }

    public CarwashResponse.findByIdDTO findById(Long carwashId) {

        Carwash carwash = carwashJPARepository.findById(carwashId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "CarwashId not found")
                ));
        int reviewCnt = reviewJPARepository.findByCarwash_Id(carwashId).size();
        int bayCnt = bayJPARepository.findByCarwashIdAndStatus(carwashId).size();
        Location location = locationJPARepository.findById(carwash.getLocation().getId())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("LocationId", "LocationId not found")
                ));
        List<Long> keywordIds = carwashKeywordJPARepository.findKeywordIdsByCarwashId(carwashId);

        List<Optime> optimeList = optimeJPARepository.findByCarwash_Id(carwashId);
        Map<DayType, Optime> optimeByDayType = new EnumMap<>(DayType.class);
        optimeList.forEach(ol -> optimeByDayType.put(ol.getDayType(), ol));

        Optime weekOptime = optimeByDayType.get(DayType.WEEKDAY);
        Optime endOptime = optimeByDayType.get(DayType.WEEKEND);

        List<File> imageFiles = fileJPARepository.findByCarwash_IdAndIsDeletedFalse(carwashId);

        return new CarwashResponse.findByIdDTO(carwash, reviewCnt, bayCnt, location, keywordIds, weekOptime, endOptime, imageFiles);
    }

    public CarwashResponse.carwashDetailsDTO findCarwashByDetails(Long carwashId, Member member) {

        Carwash carwash = carwashJPARepository.findById(carwashId)
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("CarwashId", "CarwashId not found")
                ));
        if (!Objects.equals(carwash.getMember().getId(), member.getId()))
            throw new ForbiddenError(
                    ForbiddenError.ErrorCode.RESOURCE_ACCESS_FORBIDDEN,
                    Collections.singletonMap("MemberId", "Member is not the owner of the carwash.")
            );

        Location location = locationJPARepository.findById(carwash.getLocation().getId())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("LocationId", "LocationId not found")
                ));
        List<Long> keywordIds = carwashKeywordJPARepository.findKeywordIdsByCarwashId(carwashId);

        List<Optime> optimeList = optimeJPARepository.findByCarwash_Id(carwashId);
        Map<DayType, Optime> optimeByDayType = new EnumMap<>(DayType.class);
        optimeList.forEach(ol -> optimeByDayType.put(ol.getDayType(), ol));

        Optime weekOptime = optimeByDayType.get(DayType.WEEKDAY);
        Optime endOptime = optimeByDayType.get(DayType.WEEKEND);

        List<File> imageFiles = fileJPARepository.findByCarwash_IdAndIsDeletedFalse(carwashId);

        return new CarwashResponse.carwashDetailsDTO(carwash, location, keywordIds, weekOptime, endOptime, imageFiles);
    }

    @Transactional
    public CarwashResponse.updateCarwashDetailsResponseDTO updateCarwashDetails(Long carwashId, CarwashRequest.updateCarwashDetailsDTO updatedto, MultipartFile[] images, Member member) {
        updateCarwashDetailsResponseDTO response = new updateCarwashDetailsResponseDTO();
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
        carwash.setName(updatedto.getName());
        carwash.setTel(updatedto.getTel());
        carwash.setDes(updatedto.getDescription());
        carwash.setPrice(updatedto.getPrice());
        response.updateCarwashPart(carwash);

        CarwashRequest.updateLocationDTO updateLocationDTO = updatedto.getLocation();
        Location location = locationJPARepository.findById(carwash.getLocation().getId())
                .orElseThrow(() -> new NotFoundError(
                        NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                        Collections.singletonMap("LocationId", "Location not found")
                ));

        location.updateAddress(updateLocationDTO.getAddress(),
                updateLocationDTO.getLatitude(), updateLocationDTO.getLongitude());
        response.updateLocationPart(location);

        CarwashRequest.updateOperatingTimeDTO updateOperatingTimeDTO = updatedto.getOptime();

        List<Optime> optimeList = optimeJPARepository.findByCarwash_Id(carwashId);
        Map<DayType, Optime> optimeByDayType = new EnumMap<>(DayType.class);
        optimeList.forEach(ol -> optimeByDayType.put(ol.getDayType(), ol));

        Optime weekOptime = optimeByDayType.get(DayType.WEEKDAY);
        Optime endOptime = optimeByDayType.get(DayType.WEEKEND);

        weekOptime.setStartTime(updateOperatingTimeDTO.getWeekday().getStart());
        weekOptime.setEndTime(updateOperatingTimeDTO.getWeekday().getEnd());
        endOptime.setStartTime(updateOperatingTimeDTO.getWeekend().getStart());
        endOptime.setEndTime(updateOperatingTimeDTO.getWeekend().getEnd());

        response.updateOptimePart(weekOptime, endOptime);

        List<Long> newKeywordIds = updatedto.getKeywordIdList();
        if (newKeywordIds.stream().anyMatch(id -> id < 8 || id > 14)) {
            throw new BadRequestError(
                    BadRequestError.ErrorCode.VALIDATION_FAILED,
                    Collections.singletonMap("message", "Carwash Keyword ID must be between 8 and 14")
            );
        }

        List<Long> existingKeywordIds = carwashKeywordJPARepository.findKeywordIdsByCarwashId(carwashId);

        List<Long> keywordsToDelete = existingKeywordIds.stream()
                .filter(id -> !newKeywordIds.contains(id))
                .collect(Collectors.toList());
        if (!keywordsToDelete.isEmpty()) {
            carwashKeywordJPARepository.deleteByCarwashIdAndKeywordIds(carwashId, keywordsToDelete);
        }

        List<Long> keywordsToAdd = newKeywordIds.stream()
                .filter(id -> !existingKeywordIds.contains(id))
                .collect(Collectors.toList());

        List<Keyword> keywordList = keywordJPARepository.findAllById(keywordsToAdd);
        if (keywordList.size() != keywordsToAdd.size()) {
            throw new NotFoundError(
                    NotFoundError.ErrorCode.RESOURCE_NOT_FOUND,
                    Collections.singletonMap("KeywordId", "Keyword not found")
            );
        }

        List<CarwashKeyword> newCarwashKeywords = new ArrayList<>();
        for (Keyword keyword : keywordList) {
            CarwashKeyword carwashKeyword = CarwashKeyword.builder()
                    .carwash(carwash)
                    .keyword(keyword)
                    .build();
            newCarwashKeywords.add(carwashKeyword);
        }
        carwashKeywordJPARepository.saveAll(newCarwashKeywords);

        List<Long> updateKeywordIds = carwashKeywordJPARepository.findKeywordIdsByCarwashId(carwashId);
        response.updateKeywordPart(updateKeywordIds);

        if (images != null && images.length > 0) {
            List<ReservationResponse.ImageDTO> updatedImageList = fileService.uploadAndSaveFiles(images, carwash);
            response.setImageFileList(updatedImageList);
        }

        return response;
    }

}
