package team7.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import team7.service.ComplianceSchedulerService;
import team7.service.SchedulerRunResult;
import team7.service.SchedulerState;

@Controller
@RequestMapping("/admin")
public class AdminController extends ControllerSupport {

    private final ComplianceSchedulerService schedulerService;
    private final SchedulerState schedulerState;

    public AdminController(ComplianceSchedulerService schedulerService,
                            SchedulerState schedulerState) {
        this.schedulerService = schedulerService;
        this.schedulerState   = schedulerState;
    }

    @GetMapping("/scheduler")
    public String scheduler(Model model) {
        model.addAttribute("result",   schedulerState.getLastResult());
        model.addAttribute("hasRun",   schedulerState.hasRun());
        model.addAttribute("activeNav", "compliance");
        return "admin/scheduler";
    }

    @PostMapping("/run-check")
    public String runCheck(RedirectAttributes ra) {
        SchedulerRunResult result = schedulerService.checkDeadlines();
        ra.addFlashAttribute("runMsg",
                "Check complete: " + result.getAlertsSent() + " alerts sent, "
                + result.getOverdueCount() + " items flagged overdue.");
        return "redirect:/admin/scheduler";
    }
}
