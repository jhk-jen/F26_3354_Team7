package team7.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import team7.model.Visit;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    /** Visits for a case, sorted by scheduled date descending. */
    List<Visit> findByCaseRecordIdOrderByScheduledDateDesc(String caseId);

    /** Visits assigned to cases owned by a caseworker username. */
    List<Visit> findByCaseRecordAssignedCaseworkerUsernameOrderByScheduledDateAsc(String username);
}
