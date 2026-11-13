package team7.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import team7.model.Alert;
import team7.model.Case;
import team7.model.CaseEvent;
import team7.model.Deadline;
import team7.model.FosterHome;
import team7.model.Placement;
import team7.model.Visit;
import team7.service.CaseView;
import team7.service.ComplianceSchedulerService;
import team7.service.PlacementResult;
import team7.service.PlacementService;

@Controller
@RequestMapping("/caseworker")
public class CaseworkerController extends ControllerSupport {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final PlacementService placementService;
    private final ComplianceSchedulerService complianceService;

    public CaseworkerController(PlacementService placementService,
                                  ComplianceSchedulerService complianceService) {
        this.placementService = placementService;
        this.complianceService = complianceService;
    }

    // ── Screen 2: My cases ────────────────────────────────────────────────────

    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        String username = auth.getName();
        List<CaseView> cases = placementService.getCaseViews(username);
        List<Alert> alerts = complianceService.getAlertsForUser(username);
        List<Deadline> overdueDeadlines = complianceService.getOverdueDeadlinesForUser(username);
        List<Visit> visits = placementService.getVisitsForCaseworker(username);
        model.addAttribute("cases", cases);
        model.addAttribute("alerts", alerts);
        model.addAttribute("overdueDeadlines", overdueDeadlines);
        model.addAttribute("visits", visits);
        model.addAttribute("activeNav", "work");
        return "caseworker/home";
    }

    // ── Screen 3: Case placement view ─────────────────────────────────────────

    @GetMapping("/cases/{id}")
    public String caseDetail(@PathVariable String id,
                              Authentication auth, Model model) {
        Case c = placementService.getCase(id);
        requireOwner(c, auth);
        Placement current = placementService.getCurrentPlacement(id).orElse(null);
        model.addAttribute("caseRecord", c);
        model.addAttribute("currentPlacement", current);
        model.addAttribute("activeNav", "cases");
        return "caseworker/case";
    }

    // ── Screens 4–6: End placement ────────────────────────────────────────────

    @GetMapping("/cases/{id}/end-placement")
    public String endPlacementForm(@PathVariable String id, Authentication auth, Model model) {
        Case c = placementService.getCase(id);
        requireOwner(c, auth);
        Placement current = placementService.getCurrentPlacement(id).orElse(null);
        model.addAttribute("caseRecord", c);
        model.addAttribute("currentPlacement", current);
        model.addAttribute("endDate", LocalDate.now().format(FMT));
        model.addAttribute("reason", "");
        model.addAttribute("activeNav", "placements");
        return "caseworker/end-placement";
    }

    @PostMapping("/cases/{id}/end-placement")
    public String endPlacementSubmit(@PathVariable String id,
                                      @RequestParam(defaultValue = "") String reason,
                                      Authentication auth,
                                      Model model,
                                      RedirectAttributes ra) {
        Case c = placementService.getCase(id);
        requireOwner(c, auth);
        if (reason.isBlank()) {
            // Re-show form with inline error (no redirect — POST with error)
            Placement current = placementService.getCurrentPlacement(id).orElse(null);
            model.addAttribute("caseRecord", c);
            model.addAttribute("currentPlacement", current);
            model.addAttribute("endDate", LocalDate.now().format(FMT));
            model.addAttribute("reason", reason);
            model.addAttribute("reasonError", "Enter a reason for the placement change.");
            model.addAttribute("activeNav", "placements");
            return "caseworker/end-placement";
        }

        // Capture home name before the placement is deactivated
        Placement current = placementService.getCurrentPlacement(id).orElse(null);
        String homeName = current != null ? current.getFosterHome().getName() : "?";
        String endDateStr = LocalDate.now().format(FMT);

        placementService.endPlacement(id, LocalDate.now(), reason, auth.getName());

        ra.addFlashAttribute("successMsg",
                "Placement at " + homeName + " ended on " + endDateStr
                + ". Event logged to the case timeline.");
        return "redirect:/caseworker/cases/" + id;
    }

    // ── Screens 7–9: New placement ────────────────────────────────────────────

    @GetMapping("/cases/{id}/new-placement")
    public String newPlacementForm(@PathVariable String id, Authentication auth, Model model) {
        Case c = placementService.getCase(id);
        requireOwner(c, auth);
        List<FosterHome> homes = placementService.getAllHomes();
        model.addAttribute("caseRecord", c);
        model.addAttribute("homes", homes);
        model.addAttribute("startDate", LocalDate.now().format(FMT));
        model.addAttribute("activeNav", "placements");
        return "caseworker/new-placement";
    }

    @PostMapping("/cases/{id}/new-placement")
    public String newPlacementSubmit(@PathVariable String id,
                                      @RequestParam Long homeId,
                                      Authentication auth,
                                      RedirectAttributes ra) {
        requireOwner(placementService.getCase(id), auth);
        PlacementResult result = placementService.createPlacement(
                id, homeId, LocalDate.now(), auth.getName());

        if (result.isSuccess()) {
            ra.addFlashAttribute("savedMsg", result.getMessage());
            return "redirect:/caseworker/cases/" + id + "/timeline";
        } else {
            ra.addFlashAttribute("rejectedMsg", result.getMessage());
            ra.addFlashAttribute("rejectedHomeId", result.getRejectedHome().getId());
            return "redirect:/caseworker/cases/" + id + "/new-placement";
        }
    }

    // ── Screen 10: Case timeline ──────────────────────────────────────────────

    @GetMapping("/cases/{id}/timeline")
    public String timeline(@PathVariable String id, Authentication auth, Model model) {
        Case c = placementService.getCase(id);
        requireOwner(c, auth);
        Placement current = placementService.getCurrentPlacement(id).orElse(null);
        List<CaseEvent> events = placementService.getTimeline(id);
        model.addAttribute("caseRecord", c);
        model.addAttribute("currentPlacement", current);
        model.addAttribute("events", events);
        model.addAttribute("activeNav", "cases");
        return "caseworker/timeline";
    }

    // ── Nav: Placements tab ───────────────────────────────────────────────────

    @GetMapping("/placements")
    public String placements(Authentication auth, Model model) {
        String username = auth.getName();
        List<CaseView> cases = placementService.getCaseViews(username);
        java.util.Map<String, Placement> placements = placementService.getCurrentPlacementsMap(username);
        model.addAttribute("cases", cases);
        model.addAttribute("placements", placements);
        model.addAttribute("activeNav", "placements");
        return "caseworker/placements";
    }

    // ── Nav: Cases tab ────────────────────────────────────────────────────────

    @GetMapping("/cases")
    public String casesList(Authentication auth, Model model) {
        List<CaseView> cases = placementService.getCaseViews(auth.getName());
        model.addAttribute("cases", cases);
        model.addAttribute("activeNav", "cases");
        return "caseworker/cases";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** FR-2: a caseworker may only view/modify their own cases. */
    private void requireOwner(Case c, Authentication auth) {
        if (!c.getAssignedCaseworker().getUsername().equals(auth.getName())) {
            throw new AccessDeniedException("Not your case: " + c.getId());
        }
    }

    // ── Nav: Compliance tab ───────────────────────────────────────────────────

    @GetMapping("/compliance")
    public String compliance(Authentication auth, Model model) {
        String username = auth.getName();
        List<Alert> alerts = complianceService.getAlertsForUser(username);
        List<Deadline> overdueDeadlines = complianceService.getOverdueDeadlinesForUser(username);
        List<Visit> visits = placementService.getVisitsForCaseworker(username);
        model.addAttribute("alerts", alerts);
        model.addAttribute("overdueDeadlines", overdueDeadlines);
        model.addAttribute("visits", visits);
        model.addAttribute("activeNav", "compliance");
        return "caseworker/compliance";
    }
}
