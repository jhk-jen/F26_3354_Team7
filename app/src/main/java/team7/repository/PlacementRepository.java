package team7.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import team7.model.Placement;

public interface PlacementRepository extends JpaRepository<Placement, Long> {

    /** Active placement for a case (at most one). */
    Optional<Placement> findByCaseRecordIdAndActiveTrue(String caseId);

    /** All placements for a case, newest first. */
    List<Placement> findByCaseRecordIdOrderByStartDateDesc(String caseId);
}
