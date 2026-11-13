package team7.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;

import team7.model.User;
import team7.repository.UserRepository;

/**
 * Base controller that injects the current user's displayName and role
 * into the model for every request — consumed by the layout header fragment.
 */
public abstract class ControllerSupport {

    @Autowired
    protected UserRepository userRepo;

    @ModelAttribute
    public void addUserAttributes(Authentication auth,
                                   org.springframework.ui.Model model) {
        if (auth == null) return;
        User user = userRepo.findByUsername(auth.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("username",    user.getUsername());
            model.addAttribute("displayName", user.getDisplayName());
            model.addAttribute("role",        capitalize(user.getRole().name()));
        }
    }

    protected String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
