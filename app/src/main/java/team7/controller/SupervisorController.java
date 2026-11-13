package team7.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import team7.model.Role;
import team7.model.User;
import team7.service.CaseworkerStat;
import team7.service.ComplianceSchedulerService;
import team7.service.OverdueItem;
import team7.service.SchedulerState;

@Controller
@RequestMapping("/supervisor")
public class SupervisorController extends ControllerSupport {

    private final ComplianceSchedulerService complianceService;
    private final SchedulerState             schedulerState;

    public SupervisorController(ComplianceSchedulerService complianceService,
                                  SchedulerState schedulerState) {
        this.complianceService = complianceService;
        this.schedulerState    = schedulerState;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String caseworker,
                             Model model) {
        List<User> caseworkers = userRepo.findByRole(Role.CASEWORKER);
        model.addAttribute("caseworkers", caseworkers);
        model.addAttribute("lastRun", schedulerState.getLastResult());
        model.addAttribute("selectedCaseworker", caseworker != null ? caseworker : "");

        if (caseworker != null && !caseworker.isBlank()) {
            // Filtered view
            User cw = userRepo.findByUsername(caseworker).orElse(null);
            if (cw != null) {
                double rate = complianceService.calculateOnTrackRate(caseworker);
                List<OverdueItem> overdueItems = complianceService.filterByCaseworker(caseworker);

                // Get this caseworker's stat entry for case count
                List<CaseworkerStat> allStats = complianceService.getAllCaseworkerStats();
                CaseworkerStat cwStat = allStats.stream()
                        .filter(s -> s.getCaseworker().getUsername().equals(caseworker))
                        .findFirst().orElse(null);

                long missedVisits = overdueItems.stream()
                        .filter(i -> "visit".equals(i.getType()) && "Missed".equals(i.getTagLabel())).count();

                String rateClass, rateLabel;
                if (rate >= 90)      { rateClass = "ok";     rateLabel = "On target (90%+)"; }
                else if (rate >= 70) { rateClass = "warn";   rateLabel = "Needs attention (70–89%)"; }
                else                 { rateClass = "danger"; rateLabel = "Below target (<70%)"; }

                model.addAttribute("filtered", true);
                model.addAttribute("filteredCaseworker", cw);
                model.addAttribute("onTrackRate", String.format("%.1f", rate));
                model.addAttribute("onTrackRateRaw", rate);
                model.addAttribute("onTrackRateClass", rateClass);
                model.addAttribute("onTrackRateLabel", rateLabel);
                model.addAttribute("overdueItems", overdueItems);
                model.addAttribute("overdueCount", overdueItems.size());
                model.addAttribute("cwStat", cwStat);
                model.addAttribute("missedVisits", missedVisits);
            } else {
                return "redirect:/supervisor/dashboard";
            }
        } else {
            // Overall view
            Map<String, Object> stats = complianceService.getOverallStats();
            List<CaseworkerStat> cwStats = complianceService.getAllCaseworkerStats();
            model.addAttribute("filtered", false);
            model.addAttribute("stats", stats);
            model.addAttribute("cwStats", cwStats);
        }

        model.addAttribute("activeNav", "compliance");
        return "supervisor/dashboard";
    }
}
