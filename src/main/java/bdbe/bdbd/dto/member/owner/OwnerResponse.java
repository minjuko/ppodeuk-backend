package bdbe.bdbd.dto.member.owner;

import bdbe.bdbd._core.utils.DateUtils;
import bdbe.bdbd.model.Code.DayType;
import bdbe.bdbd.model.bay.Bay;
import bdbe.bdbd.model.carwash.Carwash;
import bdbe.bdbd.model.file.File;
import bdbe.bdbd.model.member.Member;
import bdbe.bdbd.model.optime.Optime;
import bdbe.bdbd.model.reservation.Reservation;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OwnerResponse {

    @Getter
    @Setter
    @ToString
    public static class SaleResponseDTO {
        private List<CarwashListDTO> carwashList;
        private List<ReservationCarwashDTO> reservationList;
        private PageMetaDTO page;

        public SaleResponseDTO(List<Carwash> carwashList, List<Reservation> reservationList) {
            this(carwashList, reservationList, 0, reservationList == null ? 20 : Math.max(1, reservationList.size()), reservationList == null ? 0 : reservationList.size());
        }

        public SaleResponseDTO(List<Carwash> carwashList, List<Reservation> reservationList, int page, int size, long totalElements) {
            if (carwashList != null) {
                this.carwashList = carwashList.stream()
                        .map(CarwashListDTO::new)
                        .collect(Collectors.toList());
            } else {
                this.carwashList = Collections.emptyList();
            }

            if (reservationList != null) {
                this.reservationList = reservationList.stream()
                        .map(ReservationCarwashDTO::new)
                        .collect(Collectors.toList());
            } else {
                this.reservationList = Collections.emptyList();
            }
            this.page = new PageMetaDTO(page, size, totalElements);
        }
    }

    @Getter
    public static class PageMetaDTO {
        private final int number;
        private final int size;
        private final long totalElements;
        private final int totalPages;

        public PageMetaDTO(int number, int size, long totalElements) {
            this.number = number;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        }
    }

    @Getter
    @Setter
    @ToString
    public static class CarwashListDTO {
        private Long carwashId;
        private String name;

        public CarwashListDTO(Carwash carwash) {
            this.carwashId = carwash.getId();
            this.name = carwash.getName();
        }
    }

    @Getter
    @Setter
    @ToString
    public static class ReservationCarwashDTO {
        private ReservationDTO reservation;
        private CarwashDTO carwash;

        public ReservationCarwashDTO(Reservation reservation) {
            this.reservation = new ReservationDTO(reservation);
            this.carwash = new CarwashDTO(reservation.getBay().getCarwash());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class ReservationListDTO {
        List<ReservationDTO> reservationDTOList;

        public ReservationListDTO(List<Reservation> reservationList) {
            this.reservationDTOList = reservationList.stream()
                    .map(ReservationDTO::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class ReservationDTO {
        private Long reservationId;
        private Long bayId;
        private int bayNo;
        private String nickname;
        private int totalPrice;
        private String startTime;
        private String endTime;

        public ReservationDTO(Reservation reservation) {
            this.reservationId = reservation.getId();
            this.bayId = reservation.getBay().getId();
            this.bayNo = reservation.getBay().getBayNum();
            this.nickname = reservation.getMember().getUsername();
            this.totalPrice = reservation.getPrice();
            this.startTime = DateUtils.formatDateTime(reservation.getStartTime());
            this.endTime = DateUtils.formatDateTime(reservation.getEndTime());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class CarwashDTO {
        private Long carwashId;
        private String name;

        public CarwashDTO(Carwash carwash) {
            this.carwashId = carwash.getId();
            this.name = carwash.getName();
        }
    }

    @Getter
    @Setter
    public static class FileDTO {
        private Long id;
        private String name;
        private String url;
        private String uploadedAt;

        public FileDTO(File file) {
            this.id = file.getId();
            this.name = file.getName();
            this.url = file.getUrl();
            this.uploadedAt = DateUtils.formatDateTime(file.getUploadedAt());
        }
    }


    @Getter
    @Setter
    @ToString
    public static class ReservationOverviewResponseDTO {
        private List<CarwashManageByOwnerDTO> carwashList = new ArrayList<>();

        public void addCarwashManageByOwnerDTO(CarwashManageByOwnerDTO carwashManageByOwnerDTO) {
            this.carwashList.add(carwashManageByOwnerDTO);
        }
    }

    @Getter
    @Setter
    @ToString
    public static class CarwashManageByOwnerDTO {
        private Long id;
        private String name;
        private OperationTimeDTO optime;
        private List<BayReservationDTO> bayReservationList = new ArrayList<>();
        private List<FileDTO> imageList;

        public CarwashManageByOwnerDTO(Carwash carwash, List<Bay> bayList, List<Optime> optimeList, List<Reservation> reservationList, List<File> files) {
            this.id = carwash.getId();
            this.name = carwash.getName();
            this.optime = new OperationTimeDTO(optimeList);
            for (Bay bay : bayList) {
                BayReservationDTO bayReservationDTO = new BayReservationDTO(bay, reservationList);
                this.bayReservationList.add(bayReservationDTO);
            }
            this.imageList = files.stream().map(FileDTO::new).collect(Collectors.toList());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class CarwashManageDTO {
        private Long id;
        private String name;
        private Long monthlySales;
        private Long monthlyReservations;
        private OperationTimeDTO optime;
        private List<BayReservationDTO> bayReservationList = new ArrayList<>();
        private FileDTO image;

        public CarwashManageDTO(Carwash carwash, Long monthlySales, Long monthlyReservations, List<Bay> bayList, List<Optime> optimeList, List<Reservation> reservationList, File file) {
            this.id = carwash.getId();
            this.name = carwash.getName();
            this.monthlySales = monthlySales;
            this.monthlyReservations = monthlyReservations;
            this.optime = new OperationTimeDTO(optimeList);
            for (Bay bay : bayList) {
                BayReservationDTO bayReservationDTO = new BayReservationDTO(bay, reservationList);
                this.bayReservationList.add(bayReservationDTO);
            }
            if (file != null && !file.isDeleted()) {
                this.image = new FileDTO(file);
            } else {
                this.image = null;
            }
        }
    }

    @Getter
    @Setter
    @ToString
    public static class OperationTimeDTO {
        private TimeFrameDTO weekday;
        private TimeFrameDTO weekend;

        public OperationTimeDTO(List<Optime> optimeList) {
            optimeList.forEach(optime -> {
                if (optime.getDayType() == DayType.WEEKDAY) {
                    this.weekday = new TimeFrameDTO(optime);
                } else if (optime.getDayType() == DayType.WEEKEND) {
                    this.weekend = new TimeFrameDTO(optime);
                }
            });
        }
    }

    @Getter
    @Setter
    @ToString
    public static class TimeFrameDTO {
        private String start;
        private String end;

        public TimeFrameDTO(Optime optime) {
            this.start = DateUtils.formatTime(optime.getStartTime());
            this.end = DateUtils.formatTime(optime.getEndTime());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class BayReservationDTO {
        private Long bayId;
        private int bayNo;
        private int status;
        private List<BookedTimeDTO> bayBookedTimeList;

        public BayReservationDTO(Bay bay, List<Reservation> reservationList) {
            this.bayId = bay.getId();
            this.bayNo = bay.getBayNum();
            this.status = bay.getStatus();
            this.bayBookedTimeList = reservationList.stream()
                    .filter(reservation -> reservation.getBay() != null && reservation.getBay().getId().equals(bay.getId()))
                    .map(BookedTimeDTO::new)
                    .collect(Collectors.toList());
        }

        @Getter
        @Setter
        @ToString
        public static class BookedTimeDTO {
            private String start;
            private String end;

            public BookedTimeDTO(Reservation reservation) {
                this.start = DateUtils.formatDateTime(reservation.getStartTime());
                this.end = DateUtils.formatDateTime(reservation.getEndTime());
            }
        }
    }

    @Getter
    @Setter
    public static class OwnerDashboardDTO {
        private Long monthlySales;
        private double salesGrowthPercentage;
        private Long monthlyReservations;
        private double reservationGrowthPercentage;
        private List<CarwashInfoDTO> carwashInfoList;

        public OwnerDashboardDTO(Long monthlySales, double salesGrowthPercentage, Long monthlyReservations, double reservationGrowthPercentage, List<CarwashInfoDTO> myStores) {
            this.monthlySales = monthlySales;
            this.salesGrowthPercentage = salesGrowthPercentage;
            this.monthlyReservations = monthlyReservations;
            this.reservationGrowthPercentage = reservationGrowthPercentage;
            this.carwashInfoList = myStores;
        }

    }

    @Getter
    @Setter
    public static class CarwashInfoDTO {
        private Long carwashId;
        private String name;
        private Long monthlySales;
        private Long monthlyReservations;
        private List<FileDTO> imageFileList;


        public CarwashInfoDTO(Carwash carwash, Long monthlySales, Long monthlyReservations, List<File> files) {
            this.carwashId = carwash.getId();
            this.name = carwash.getName();
            this.monthlySales = monthlySales;
            this.monthlyReservations = monthlyReservations;
            this.imageFileList = files.stream()
                    .map(FileDTO::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    @Setter
    @ToString
    public static class ReservationCarwashListDTO {
        private int bayNo;
        private List<ReservationCarwashDTO> reservationList;
        private PageMetaDTO page;

        public ReservationCarwashListDTO(List<Reservation> reservationList, int bayNo) {
            this(reservationList, bayNo, 0, Math.max(1, reservationList.size()), reservationList.size());
        }

        public ReservationCarwashListDTO(List<Reservation> reservationList, int bayNo, int page, int size, long totalElements) {
            this.bayNo = bayNo;
            this.reservationList = reservationList.stream().map(ReservationCarwashDTO::new).collect(Collectors.toList());
            this.page = new PageMetaDTO(page, size, totalElements);
        }
    }

    @Getter
    @Setter
    @ToString
    public static class UserInfoDTO {
        private Long id;
        private String name;
        private String email;

        public UserInfoDTO(Member member) {
            this.id = member.getId();
            this.name = member.getUsername();
            this.email = member.getEmail();
        }
    }
}
