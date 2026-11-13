package team7.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import team7.model.Case;

public interface CaseRepository extends JpaRepository<Case, String> {

    /** All cases assigned to a caseworker (by username). */
    List<Case> findByAssignedCaseworkerUsername(String username);
}
