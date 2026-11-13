package team7.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import team7.model.Deadline;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    /** All open (not met) deadlines, for the scheduler. */
    List<Deadline> findByMetFalse();

    /** Open deadlines for a specific case. */
    List<Deadline> findByCaseRecordIdAndMetFalse(String caseId);

    /** Deadlines assigned to a specific caseworker. */
    List<Deadline> findByAssignedToUsername(String username);

    /** Open (not met) deadlines assigned to a specific caseworker. */
    List<Deadline> findByAssignedToUsernameAndMetFalse(String username);
}
