package team7.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import team7.model.Case;
import team7.model.CaseEvent;
import team7.model.Child;
import team7.model.FosterHome;
import team7.model.Placement;
import team7.model.Role;
import team7.model.User;
import team7.repository.CaseEventRepository;
import team7.repository.CaseRepository;
import team7.repository.DeadlineRepository;
import team7.repository.FosterHomeRepository;
import team7.repository.PlacementRepository;
import team7.repository.UserRepository;
import team7.repository.VisitRepository;

@ExtendWith(MockitoExtension.class)
class PlacementServiceTest {

    @Mock CaseRepository       caseRepo;
    @Mock PlacementRepository  placementRepo;
    @Mock FosterHomeRepository homeRepo;
    @Mock CaseEventRepository  eventRepo;
    @Mock UserRepository       userRepo;
    @Mock VisitRepository      visitRepo;
    @Mock DeadlineRepository   deadlineRepo;

    @InjectMocks PlacementService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User actor() {
        return new User("m.garcia", "hash", "Maria Garcia", Role.CASEWORKER);
    }

    private Case openCase() {
        Child child = new Child("2231", "Noah K.");
        return new Case("C-1088", child, actor(), "Open");
    }

    private FosterHome availableHome(int capacity, int occupancy) {
        return new FosterHome("Johnson Home", "Richardson, TX", capacity, occupancy);
    }

    private FosterHome fullHome() {
        return new FosterHome("Rivera Family Home", "Garland, TX", 4, 4);
    }

    // ── endPlacement tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("endPlacement: blank reason → IllegalArgumentException")
    void endPlacement_blankReason_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> service.endPlacement("C-1088", LocalDate.now(), "  ", "m.garcia"),
            "Blank reason should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("endPlacement: empty string reason → IllegalArgumentException")
    void endPlacement_emptyReason_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> service.endPlacement("C-1088", LocalDate.now(), "", "m.garcia"));
    }

    @Test
    @DisplayName("endPlacement: valid reason → decrements FosterHome occupancy")
    void endPlacement_validReason_decrementsOccupancy() {
        // Given
        FosterHome home = new FosterHome("Patel Home", "Irving, TX", 4, 3);
        Placement active = new Placement(openCase(), home, LocalDate.now().minusDays(100));
        active.setActive(true);

        when(placementRepo.findByCaseRecordIdAndActiveTrue("C-1088"))
                .thenReturn(Optional.of(active));
        when(userRepo.findByUsername("m.garcia")).thenReturn(Optional.of(actor()));
        when(placementRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(homeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        service.endPlacement("C-1088", LocalDate.now(),
                "Foster family relocating out of state", "m.garcia");

        // Then
        assertEquals(2, home.getCurrentOccupancy(),
                "Occupancy should decrease from 3 to 2");
        assertFalse(active.isActive(), "Placement should be marked inactive");
    }

    @Test
    @DisplayName("endPlacement: valid reason → logs CaseEvent with correct description")
    void endPlacement_validReason_logsCaseEvent() {
        // Given
        FosterHome home = new FosterHome("Patel Home", "Irving, TX", 4, 3);
        Placement active = new Placement(openCase(), home, LocalDate.now().minusDays(100));
        active.setActive(true);

        when(placementRepo.findByCaseRecordIdAndActiveTrue("C-1088"))
                .thenReturn(Optional.of(active));
        when(userRepo.findByUsername("m.garcia")).thenReturn(Optional.of(actor()));
        when(placementRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(homeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<CaseEvent> captor = ArgumentCaptor.forClass(CaseEvent.class);
        when(eventRepo.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        service.endPlacement("C-1088", LocalDate.now(),
                "Foster family relocating out of state", "m.garcia");

        // Then
        CaseEvent logged = captor.getValue();
        assertTrue(logged.getDescription().contains("Placement ended"),
                "Event description should mention 'Placement ended'");
        assertTrue(logged.getDescription().contains("Patel Home"),
                "Event description should include home name");
        assertEquals("M. Garcia", logged.getActor(),
                "Actor should be short name 'M. Garcia'");
    }

    // ── createPlacement tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("createPlacement: full home → returns rejected result with message")
    void createPlacement_fullHome_returnsRejection() {
        // Given
        FosterHome full = fullHome();
        when(placementRepo.findByCaseRecordIdAndActiveTrue("C-1088"))
                .thenReturn(Optional.empty());
        when(caseRepo.findById("C-1088")).thenReturn(Optional.of(openCase()));
        when(homeRepo.findById(1L)).thenReturn(Optional.of(full));

        // When
        PlacementResult result = service.createPlacement("C-1088", 1L, LocalDate.now(), "m.garcia");

        // Then
        assertFalse(result.isSuccess(), "Result should not be success for a full home");
        assertEquals("Rivera Family Home", result.getRejectedHome().getName());
        assertTrue(result.getMessage().contains("maximum capacity"),
                "Rejection message should mention 'maximum capacity'");
        assertTrue(result.getMessage().contains("4 of 4"),
                "Rejection message should include occupancy numbers");
    }

    @Test
    @DisplayName("createPlacement: available home → increments occupancy")
    void createPlacement_availableHome_incrementsOccupancy() {
        // Given
        FosterHome home = availableHome(4, 2);
        Case c = openCase();

        when(placementRepo.findByCaseRecordIdAndActiveTrue("C-1088"))
                .thenReturn(Optional.empty());
        when(caseRepo.findById("C-1088")).thenReturn(Optional.of(c));
        when(homeRepo.findById(1L)).thenReturn(Optional.of(home));
        when(userRepo.findByUsername("m.garcia")).thenReturn(Optional.of(actor()));
        when(placementRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(homeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(eventRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        PlacementResult result = service.createPlacement("C-1088", 1L, LocalDate.now(), "m.garcia");

        // Then
        assertTrue(result.isSuccess(), "Result should be success");
        assertEquals(3, home.getCurrentOccupancy(),
                "Occupancy should increase from 2 to 3");
    }

    @Test
    @DisplayName("createPlacement: available home → logs CaseEvent 'Placement started'")
    void createPlacement_availableHome_logsCaseEvent() {
        // Given
        FosterHome home = availableHome(4, 2);
        Case c = openCase();

        when(placementRepo.findByCaseRecordIdAndActiveTrue("C-1088"))
                .thenReturn(Optional.empty());
        when(caseRepo.findById("C-1088")).thenReturn(Optional.of(c));
        when(homeRepo.findById(1L)).thenReturn(Optional.of(home));
        when(userRepo.findByUsername("m.garcia")).thenReturn(Optional.of(actor()));
        when(placementRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(homeRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<CaseEvent> captor = ArgumentCaptor.forClass(CaseEvent.class);
        when(eventRepo.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        service.createPlacement("C-1088", 1L, LocalDate.now(), "m.garcia");

        // Then
        CaseEvent logged = captor.getValue();
        assertTrue(logged.getDescription().contains("Placement started"),
                "Event should say 'Placement started'");
        assertTrue(logged.getDescription().contains("Johnson Home"),
                "Event should include home name");
    }

    @Test
    @DisplayName("createPlacement: existing active placement → IllegalStateException")
    void createPlacement_existingActivePlacement_throws() {
        // Given: case already has an active placement
        FosterHome home = availableHome(4, 2);
        Placement existing = new Placement(openCase(), home, LocalDate.now().minusDays(10));
        when(placementRepo.findByCaseRecordIdAndActiveTrue("C-1088"))
                .thenReturn(Optional.of(existing));

        // When/Then
        assertThrows(IllegalStateException.class,
            () -> service.createPlacement("C-1088", 1L, LocalDate.now(), "m.garcia"),
            "Should throw when case already has an active placement");
    }
}
