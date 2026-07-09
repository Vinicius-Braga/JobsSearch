package com.jobs.infrastructure.web;

import com.jobs.application.SearchAndScoreJobsUseCase;
import com.jobs.application.SearchOutcome;
import com.jobs.application.port.CompanyLoader;
import com.jobs.application.port.ProfileStore;
import com.jobs.domain.Company;
import com.jobs.domain.JobFilter;
import com.jobs.domain.UserProfile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.security.Principal;
import java.util.List;

@Controller
public class DashboardController {

    private final ProfileStore profileStore;
    private final SearchAndScoreJobsUseCase searchAndScoreJobsUseCase;
    private final CompanyLoader companyLoader;

    public DashboardController(ProfileStore profileStore, SearchAndScoreJobsUseCase searchAndScoreJobsUseCase,
            CompanyLoader companyLoader) {
        this.profileStore = profileStore;
        this.searchAndScoreJobsUseCase = searchAndScoreJobsUseCase;
        this.companyLoader = companyLoader;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        model.addAttribute("profile", currentDescription(principal));
        return "dashboard";
    }

    @GetMapping("/perfil/editar")
    public String editProfile(Model model, Principal principal) {
        model.addAttribute("description", currentDescription(principal));
        return "perfil";
    }

    @PostMapping("/perfil")
    public String saveProfile(@RequestParam String description, Principal principal) {
        profileStore.save(principal.getName(), new UserProfile(description));
        return "redirect:/";
    }

    @PostMapping("/buscar")
    public String search(Model model, Principal principal) throws Exception {
        String description = currentDescription(principal);
        model.addAttribute("profile", description);

        if (description == null || description.isBlank()) {
            model.addAttribute("erro", "Edite seu perfil antes de buscar vagas.");
            return "dashboard";
        }

        List<Company> companies = companyLoader.load();
        JobFilter filter = JobFilter.load(Path.of("filtro.txt"));
        SearchOutcome outcome = searchAndScoreJobsUseCase.search(companies, filter, new UserProfile(description));

        model.addAttribute("results", outcome.scored());
        if (outcome.hasUnscoredMatches()) {
            model.addAttribute("aviso", (outcome.matchedCount() - outcome.scored().size())
                    + " vaga(s) bateram no filtro mas não puderam ser pontuadas pela IA — verifique se o "
                    + "ANTHROPIC_API_KEY está configurado corretamente no .env.");
        }
        return "dashboard";
    }

    private String currentDescription(Principal principal) {
        UserProfile profile = profileStore.find(principal.getName());
        return profile != null ? profile.description() : "";
    }
}
