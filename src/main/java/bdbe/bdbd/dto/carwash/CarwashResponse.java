package bdbe.bdbd.dto.carwash;

import bdbe.bdbd._core.utils.DateUtils;
import bdbe.bdbd.dto.reservation.ReservationResponse;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.file.File;
import bdbe.bdbd.model.location.Location;
import bdbe.bdbd.model.optime.Optime;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

public class CarwashResponse {
    @Getter
    @Setter
    public static class FindAllDTO {
        private Long id;
        private String name;
        private double rate;
        private String tel;
        private String des;
        private int price;
        private Long lId;
        private Long userId;

        public FindAllDTO(Carwash carwash) {
            this.id = carwash.getId();
            this.name = carwash.getName();
            this.rate = carwash.getRate();
            this.tel = carwash.getTel();
            this.des = carwash.getDes();
            this.price = carwash.getPrice();
            this.lId = carwash.getLocation().getId();
            this.userId = carwash.getMember().getId();
        }
    }

    @Getter
    @Setter
    public class KeywordResponseDTO {
        private String keywordName;

        public KeywordResponseDTO(String keywordName) {
            this.keywordName = keywordName;
        }
    }


    @Getter
    @Setter
    public static class findByIdDTO {
        private Long id;
        private String name;
        private double rate;
        private int reviewCnt;
        private int bayCnt;
        private OperatingTimeDTOResponse optime;
        private CarwashResponse.locationDTO locationDTO;
        private List<Long> keywordIdList;
        private String description;
        private String tel;
        private List<FileDTO> imageFileList;

        public findByIdDTO(Carwash carwash, int reviewCnt, int bayCnt, Location location, List<Long> keywordId, Optime weekOptime, Optime endOptime, List<File> files) {
            this.id = carwash.getId();
            this.name = carwash.getName();
            this.rate = carwash.getRate();
            this.reviewCnt = reviewCnt;
            this.bayCnt = bayCnt;
            this.optime = toOptimeListDTO(weekOptime, endOptime);
            this.locationDTO = toLocationDTO(location);
            this.keywordIdList = keywordId;
            this.description = carwash.getDes();
            this.tel = carwash.getTel();
            this.imageFileList = files.stream().filter(file -> !file.isDeleted()).map(FileDTO::new).collect(Collectors.toList());
        }

        public OperatingTimeDTOResponse toOptimeListDTO(Optime weekOptime, Optime endOptime) {
            OperatingTimeDTOResponse dto = new OperatingTimeDTOResponse();

            OperatingTimeDTOResponse.TimeSlotResponse weekSlot = new OperatingTimeDTOResponse.TimeSlotResponse();
            weekSlot.setStart(DateUtils.formatTime(weekOptime.getStartTime()));
            weekSlot.setEnd(DateUtils.formatTime(weekOptime.getEndTime()));
            dto.setWeekday(weekSlot);

            OperatingTimeDTOResponse.TimeSlotResponse endSlot = new OperatingTimeDTOResponse.TimeSlotResponse();
            endSlot.setStart(DateUtils.formatTime(endOptime.getStartTime()));
            endSlot.setEnd(DateUtils.formatTime(endOptime.getEndTime()));
            dto.setWeekend(endSlot);

            return dto;

        }

        public locationDTO toLocationDTO(Location location) {
            locationDTO locationDTO = new locationDTO();
            locationDTO.setAddress(location.getAddress());
            locationDTO.setLatitude(location.getLatitude());
            locationDTO.setLongitude(location.getLongitude());

            return locationDTO;
        }
    }

    @Getter
    @Setter
    public static class OperatingTimeDTOResponse {
        private TimeSlotResponse weekday;
        private TimeSlotResponse weekend;

        @Getter
        @Setter
        public static class TimeSlotResponse {
            private String start;
            private String end;
        }
    }

    @Getter
    @Setter
    public static class locationDTO {
        private String address;
        private double latitude;
        private double longitude;
    }

    @Getter
    @Setter
    public static class FileDTO {
        private Long id;
        private String name;
        private String url;

        public FileDTO(File file) {
            this.id = file.getId();
            this.name = file.getName();
            this.url = file.getUrl();
        }
    }

    @Getter
    @Setter
    public static class carwashDetailsDTO {
        private Long id;
        private String name;
        private int price;
        private String tel;
        private detailLocationDTO locationDTO;
        private detailsOperatingTimeDTO optime;
        private List<Long> keywordIdList;
        private String description;
        private List<FileDTO> imageFileList;

        public carwashDetailsDTO(Carwash carwash, Location location, List<Long> keywordId, Optime weekOptime, Optime endOptime, List<File> files) {
            this.id = carwash.getId();
            this.name = carwash.getName();
            this.price = carwash.getPrice();
            this.tel = carwash.getTel();
            this.locationDTO = toLocationDTO(location);
            this.optime = toOptimeListDTO(weekOptime, endOptime);
            this.keywordIdList = keywordId;
            this.description = carwash.getDes();
            this.imageFileList = files.stream()
                    .filter(file -> !file.isDeleted())  // 삭제되지 않은 파일만 포함
                    .map(FileDTO::new)
                    .collect(Collectors.toList());
        }

        public detailsOperatingTimeDTO toOptimeListDTO(Optime weekOptime, Optime endOptime) {
            detailsOperatingTimeDTO dto = new detailsOperatingTimeDTO();

            detailsOperatingTimeDTO.detailsTimeSlot weekSlot = new detailsOperatingTimeDTO.detailsTimeSlot();
            weekSlot.setStart(DateUtils.formatTime(weekOptime.getStartTime()));
            weekSlot.setEnd(DateUtils.formatTime(weekOptime.getEndTime()));
            dto.setWeekday(weekSlot);

            detailsOperatingTimeDTO.detailsTimeSlot endSlot = new detailsOperatingTimeDTO.detailsTimeSlot();
            endSlot.setStart(DateUtils.formatTime(endOptime.getStartTime()));
            endSlot.setEnd(DateUtils.formatTime(endOptime.getEndTime()));
            dto.setWeekend(endSlot);

            return dto;

        }

        public detailLocationDTO toLocationDTO(Location location) {
            detailLocationDTO detailLocationDTO = new detailLocationDTO();
            detailLocationDTO.setAddress(location.getAddress());
            detailLocationDTO.setLatitude(location.getLatitude());
            detailLocationDTO.setLongitude(location.getLongitude());

            return detailLocationDTO;
        }
    }

    @Getter
    @Setter
    public static class detailsOperatingTimeDTO {
        private detailsTimeSlot weekday;
        private detailsTimeSlot weekend;

        @Getter
        @Setter
        public static class detailsTimeSlot {
            private String start;
            private String end;
        }
    }

    @Getter
    @Setter
    public static class detailLocationDTO {
        private String address;
        private double latitude;
        private double longitude;
    }

    @Getter
    @Setter
    @ToString
    public static class updateCarwashDetailsResponseDTO {
        private Long id;
        private String name;
        private int price;
        private String tel;
        private CarwashResponse.updateLocationDTO location;
        private CarwashResponse.updateOperatingTimeDTO optime;
        private List<Long> keywordIdList;
        private String description;
        private List<ReservationResponse.ImageDTO> imageFileList;

        public void updateCarwashPart(Carwash carwash) {
            this.id = carwash.getId();
            this.name = carwash.getName();
            this.price = carwash.getPrice();
            this.tel = carwash.getTel();
            this.imageFileList = carwash.getFileList().stream()
                    .filter(file -> !file.isDeleted())
                    .map(ReservationResponse.ImageDTO::new)
                    .collect(Collectors.toList());
        }

        public void updateOptimePart(Optime weekdayOptime, Optime weekendOptime) {
            CarwashResponse.updateOperatingTimeDTO dto = new CarwashResponse.updateOperatingTimeDTO();

            CarwashResponse.updateOperatingTimeDTO.updateTimeSlot weekSlot = new CarwashResponse.updateOperatingTimeDTO.updateTimeSlot();
            weekSlot.setStart(DateUtils.formatTime(weekdayOptime.getStartTime()));
            weekSlot.setEnd(DateUtils.formatTime(weekdayOptime.getEndTime()));
            dto.setWeekday(weekSlot);

            CarwashResponse.updateOperatingTimeDTO.updateTimeSlot endSlot = new CarwashResponse.updateOperatingTimeDTO.updateTimeSlot();
            endSlot.setStart(DateUtils.formatTime(weekendOptime.getStartTime()));
            endSlot.setEnd(DateUtils.formatTime(weekendOptime.getEndTime()));
            dto.setWeekend(endSlot);

            this.optime = dto;
        }

        public void updateKeywordPart(List<Long> keywordIdList) {
            this.keywordIdList = keywordIdList;
        }

        public void updateLocationPart(Location location) {
            CarwashResponse.updateLocationDTO dto = new CarwashResponse.updateLocationDTO();
            dto.setAddress(location.getAddress());
            dto.setLatitude(location.getLatitude());
            dto.setLongitude(location.getLongitude());

            this.location = dto;
        }
    }

    @Getter
    @Setter
    public static class updateOperatingTimeDTO {
        private CarwashResponse.updateOperatingTimeDTO.updateTimeSlot weekday;
        private CarwashResponse.updateOperatingTimeDTO.updateTimeSlot weekend;

        @Getter
        @Setter
        public static class updateTimeSlot {
            private String start;
            private String end;
        }
    }

    @Getter
    @Setter
    public static class updateLocationDTO {
        private String address;
        private double latitude;
        private double longitude;
    }
}
