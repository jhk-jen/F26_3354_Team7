package team7.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests: every nav URL returns HTTP 2xx/3xx (never 404)
 * for all three roles, and an unknown URL shows our custom error page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NavControllerTest {

    @Autowired MockMvc mockMvc;

    // ── Caseworker ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Caseworker nav")
    class CaseworkerNav {

        @Test @DisplayName("/my-work → redirect")
        void myWork() throws Exception {
            mockMvc.perform(get("/my-work").with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().is3xxRedirection());
        }

        @Test @DisplayName("/placements → redirect to /caseworker/placements")
        void placements() throws Exception {
            mockMvc.perform(get("/placements").with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrl("/caseworker/placements"));
        }

        @Test @DisplayName("/cases → redirect to /caseworker/cases")
        void cases() throws Exception {
            mockMvc.perform(get("/cases").with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrl("/caseworker/cases"));
        }

        @Test @DisplayName("/compliance → redirect to /caseworker/compliance")
        void compliance() throws Exception {
            mockMvc.perform(get("/compliance").with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrl("/caseworker/compliance"));
        }
    }

    // ── Supervisor ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Supervisor nav")
    class SupervisorNav {

        @Test @DisplayName("/my-work → redirect")
        void myWork() throws Exception {
            mockMvc.perform(get("/my-work").with(user("k.brown").roles("SUPERVISOR")))
                   .andExpect(status().is3xxRedirection());
        }

        @Test @DisplayName("/placements → not-available page (200)")
        void placements() throws Exception {
            mockMvc.perform(get("/placements").with(user("k.brown").roles("SUPERVISOR")))
                   .andExpect(status().isOk())
                   .andExpect(content().string(org.hamcrest.Matchers.containsString("isn't available")));
        }

        @Test @DisplayName("/cases → not-available page (200)")
        void cases() throws Exception {
            mockMvc.perform(get("/cases").with(user("k.brown").roles("SUPERVISOR")))
                   .andExpect(status().isOk())
                   .andExpect(content().string(org.hamcrest.Matchers.containsString("isn't available")));
        }

        @Test @DisplayName("/compliance → redirect to /supervisor/dashboard")
        void compliance() throws Exception {
            mockMvc.perform(get("/compliance").with(user("k.brown").roles("SUPERVISOR")))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrl("/supervisor/dashboard"));
        }
    }

    // ── Administrator ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Administrator nav")
    class AdminNav {

        @Test @DisplayName("/my-work → redirect")
        void myWork() throws Exception {
            mockMvc.perform(get("/my-work").with(user("admin").roles("ADMINISTRATOR")))
                   .andExpect(status().is3xxRedirection());
        }

        @Test @DisplayName("/placements → not-available page (200)")
        void placements() throws Exception {
            mockMvc.perform(get("/placements").with(user("admin").roles("ADMINISTRATOR")))
                   .andExpect(status().isOk())
                   .andExpect(content().string(org.hamcrest.Matchers.containsString("isn't available")));
        }

        @Test @DisplayName("/cases → not-available page (200)")
        void cases() throws Exception {
            mockMvc.perform(get("/cases").with(user("admin").roles("ADMINISTRATOR")))
                   .andExpect(status().isOk())
                   .andExpect(content().string(org.hamcrest.Matchers.containsString("isn't available")));
        }

        @Test @DisplayName("/compliance → redirect to /admin/scheduler")
        void compliance() throws Exception {
            mockMvc.perform(get("/compliance").with(user("admin").roles("ADMINISTRATOR")))
                   .andExpect(status().is3xxRedirection())
                   .andExpect(redirectedUrl("/admin/scheduler"));
        }
    }

    // ── FR-2: Caseworker ownership (access control) ───────────────────────────

    @Nested
    @DisplayName("FR-2 Caseworker ownership")
    class CaseworkerOwnership {

        @Test @DisplayName("own case detail → 200")
        void ownCaseDetail() throws Exception {
            // C-1088 is assigned to m.garcia in DataLoader
            mockMvc.perform(get("/caseworker/cases/C-1088")
                            .with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().isOk());
        }

        @Test @DisplayName("other's case detail → 403")
        void otherCaseDetail() throws Exception {
            // C-1066 is assigned to d.lee, not m.garcia
            mockMvc.perform(get("/caseworker/cases/C-1066")
                            .with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().isForbidden());
        }

        @Test @DisplayName("other's case timeline → 403")
        void otherCaseTimeline() throws Exception {
            mockMvc.perform(get("/caseworker/cases/C-1066/timeline")
                            .with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().isForbidden());
        }

        @Test @DisplayName("other's end-placement form → 403")
        void otherCaseEndPlacement() throws Exception {
            mockMvc.perform(get("/caseworker/cases/C-1066/end-placement")
                            .with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().isForbidden());
        }

        @Test @DisplayName("other's new-placement form → 403")
        void otherCaseNewPlacement() throws Exception {
            mockMvc.perform(get("/caseworker/cases/C-1066/new-placement")
                            .with(user("m.garcia").roles("CASEWORKER")))
                   .andExpect(status().isForbidden());
        }
    }

    // ── Custom error page ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown URL → 404 (no Whitelabel)")
    void unknownUrl_returnsCustom404() throws Exception {
        // Spring Boot 3.x returns 404 with empty body for unknown URLs in MockMvc;
        // the important check is that Spring's Whitelabel Error Page is NOT served.
        mockMvc.perform(get("/this-page-does-not-exist")
                        .with(user("m.garcia").roles("CASEWORKER")))
               .andExpect(status().isNotFound())
               .andExpect(content().string(org.hamcrest.Matchers.not(
                       org.hamcrest.Matchers.containsString("Whitelabel Error Page"))));
    }
}
