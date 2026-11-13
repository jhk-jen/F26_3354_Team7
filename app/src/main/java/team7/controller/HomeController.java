package team7.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController extends ControllerSupport {

    /** Redirect to role-appropriate home page. */
    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        for (var a : auth.getAuthorities()) {
            String r = a.getAuthority();
            if ("ROLE_CASEWORKER".equals(r))     return "redirect:/caseworker/home";
            if ("ROLE_ADMINISTRATOR".equals(r))   return "redirect:/admin/scheduler";
            if ("ROLE_SUPERVISOR".equals(r))      return "redirect:/supervisor/dashboard";
        }
        return "home";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }
}
