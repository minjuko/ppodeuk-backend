package bdbe.bdbd.repository.bay;

import bdbe.bdbd.model.bay.Bay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface BayJPARepository extends JpaRepository<Bay, Long> {
    Bay findFirstBy();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bay b WHERE b.id = :bayId")
    Optional<Bay> findByIdForUpdate(@Param("bayId") Long bayId);

    @Query("SELECT b.id FROM Bay b WHERE b.carwash.id = :carwashId")
    List<Long> findIdsByCarwashId(@Param("carwashId") Long carwashId);
    List<Bay> findByCarwashId(Long carwashId);
    @Query("SELECT b FROM Bay b WHERE b.carwash.id = :carwashId AND b.status = 1")
    List<Bay> findByCarwashIdAndStatus(Long carwashId);

}
