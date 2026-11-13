package team7.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import team7.model.CaseEvent;

public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    /** All events for a case, newest first. */
    List<CaseEvent> findByCaseRecordIdOrderByTimestampDesc(String caseId);
}
