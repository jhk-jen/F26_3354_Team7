package team7.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import team7.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    /** Unread alerts for a recipient. */
    List<Alert> findByRecipientUsernameAndReadFalseOrderBySentAtDesc(String username);

    /** All alerts for a recipient, newest first. */
    List<Alert> findByRecipientUsernameOrderBySentAtDesc(String username);

    /** Check if an alert was already sent for this deadline at this daysBeforeDue. */
    boolean existsByDeadlineIdAndDaysBeforeDue(Long deadlineId, int daysBeforeDue);
}
