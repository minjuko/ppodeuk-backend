package bdbe.bdbd.repository.reservation;

import bdbe.bdbd.model.reservation.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface ReservationJPARepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.bay b JOIN FETCH b.carwash WHERE r.member.id = :memberId AND r.isDeleted = false")
    List<Reservation> findFirstByMemberIdWithJoinFetch(@Param("memberId") Long memberId, Pageable pageable);

    List<Reservation> findByBayIdInAndIsDeletedFalse(List<Long> bayIds); // bay id 리스트로 관련된 모든 reservation 찾기

    @Query("select r from Reservation r " +
            "join fetch r.member m " +
            "join fetch r.bay b " +
            "join fetch b.carwash c " +
            "where b.id = :bayId and r.isDeleted = false " +
            "and FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) " +
            "and FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) " +
            "order by r.startTime DESC")
    List<Reservation> findByBay_IdWithJoinsAndIsDeletedFalseAndMonthOrderByStartTimeDesc(@Param("bayId") Long bayId, @Param("selectedDate") LocalDate selectedDate);

    List<Reservation> findByBay_IdAndIsDeletedFalse(Long bayId);

    List<Reservation> findByMemberIdAndIsDeletedFalse(Long memberId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.bay b JOIN FETCH b.carwash WHERE r.member.id = :memberId AND r.isDeleted = false AND r.endTime < CURRENT_TIMESTAMP ORDER BY r.endTime DESC")
    List<Reservation> findByMemberIdJoinFetch(@Param("memberId") Long memberId, Pageable pageable);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.bay b JOIN FETCH b.carwash WHERE r.member.id = :memberId AND r.isDeleted = false AND r.endTime < :selectedAt ORDER BY r.endTime DESC")
    List<Reservation> findByMemberIdJoinFetchBefore(@Param("memberId") Long memberId, @Param("selectedAt") java.time.LocalDateTime selectedAt, Pageable pageable);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.bay b JOIN FETCH b.carwash c JOIN FETCH r.member u WHERE c.id IN :carwashIds AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND FUNCTION('DATE', r.startTime) <= :selectedDate AND r.isDeleted = false ORDER BY r.startTime DESC")
    List<Reservation> findAllByCarwash_IdInOrderByStartTimeDesc(@Param("carwashIds") List<Long> carwashIds, @Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.bay b JOIN FETCH b.carwash c JOIN FETCH r.member u WHERE c.id IN :carwashIds AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.startTime <= :selectedAt AND r.isDeleted = false ORDER BY r.startTime DESC")
    List<Reservation> findAllByCarwash_IdInOrderByStartTimeDescBefore(@Param("carwashIds") List<Long> carwashIds, @Param("selectedDate") LocalDate selectedDate, @Param("selectedAt") java.time.LocalDateTime selectedAt);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.bay b JOIN FETCH b.carwash c WHERE c.id = :carwashId AND FUNCTION('DATE', r.startTime) = :today AND r.isDeleted = false ORDER BY r.startTime DESC")
    List<Reservation> findTodaysReservationsByCarwashId(@Param("carwashId") Long carwashId, @Param("today") Date today);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.bay.carwash.id IN :carwashIds AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.isDeleted = false")
    Long findMonthlyReservationCountByCarwashIdsAndDate(@Param("carwashIds") List<Long> carwashIds, @Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.bay.carwash.id = :carwashId AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.isDeleted = false")
    Long findMonthlyReservationCountByCarwashIdAndDate(@Param("carwashId") Long carwashId, @Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.bay.id = :bayId AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.isDeleted = false")
    Long findMonthlyReservationCountByBayIdAndDate(@Param("bayId") Long bayId, @Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT COALESCE(SUM(r.price), 0) FROM Reservation r WHERE r.bay.carwash.id IN :carwashIds AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.isDeleted = false")
    Long findTotalRevenueByCarwashIdsAndDate(@Param("carwashIds") List<Long> carwashIds, @Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT r FROM Reservation r WHERE r.bay.carwash.id IN :carwashIds AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.isDeleted = false")
    List<Reservation> findTotalByCarwashIdsAndDate(@Param("carwashIds") List<Long> carwashIds, @Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT COALESCE(SUM(r.price), 0) FROM Reservation r WHERE r.bay.carwash.id = :carwashId AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.isDeleted = false")
    Long findTotalRevenueByCarwashIdAndDate(@Param("carwashId") Long carwashId, @Param("selectedDate") LocalDate selectedDate);

    @Query("SELECT COALESCE(SUM(r.price), 0) FROM Reservation r WHERE r.bay.id = :bayId AND FUNCTION('YEAR', r.startTime) = FUNCTION('YEAR', :selectedDate) AND FUNCTION('MONTH', r.startTime) = FUNCTION('MONTH', :selectedDate) AND r.isDeleted = false")
    Long findTotalRevenueByBayIdAndDate(@Param("bayId") Long bayId, @Param("selectedDate") LocalDate selectedDate);
}
