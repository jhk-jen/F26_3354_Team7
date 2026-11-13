package team7.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Routes the four shared nav URLs to the correct role-specific destination.
 * Brand link and "My Work" both resolve via /my-work → /home.
 */
@Controller
public class NavController extends ControllerSupport {

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }

    @GetMapping("/my-work")
    public String myWork() {
        return "redirect:/home";
    }

    @GetMapping("/placements")
    public String placements(Authentication auth, Model model) {
        if (hasRole(auth, "CASEWORKER")) return "redirect:/caseworker/placements";
        model.addAttribute("activeNav", "placements");
        return "not-available";
    }

    @GetMapping("/cases")
    public String cases(Authentication auth, Model model) {
        if (hasRole(auth, "CASEWORKER")) return "redirect:/caseworker/cases";
        model.addAttribute("activeNav", "cases");
        return "not-available";
    }

    @GetMapping("/compliance")
    public String compliance(Authentication auth) {
        if (hasRole(auth, "CASEWORKER"))   return "redirect:/caseworker/compliance";
        if (hasRole(auth, "SUPERVISOR"))    return "redirect:/supervisor/dashboard";
        return "redirect:/admin/scheduler";
    }
}
