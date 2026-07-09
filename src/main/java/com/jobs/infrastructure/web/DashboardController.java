package com.jobs.infrastructure.web;

import com.jobs.application.SearchAndScoreJobsUseCase;
import com.jobs.application.SearchOutcome;
import com.jobs.application.port.CompanyLoader;
import com.jobs.application.port.ProfileStore;
import com.jobs.domain.Company;
import com.jobs.domain.UserProfile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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

    // Chamado via fetch() pelo radar no dashboard — busca roda em background, sem recarregar a página.
    @PostMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BuscarResponse buscar(Principal principal) throws Exception {
        String description = currentDescription(principal);
        if (description == null || description.isBlank()) {
            return new BuscarResponse(0, List.of(), "Edite seu perfil antes de buscar vagas.");
        }

        List<Company> companies = companyLoader.load();
        SearchOutcome outcome = searchAndScoreJobsUseCase.search(companies, new UserProfile(description));

        String aviso = null;
        if (outcome.hasUnscoredMatches()) {
            aviso = (outcome.matchedCount() - outcome.scored().size())
                    + " vaga(s) bateram no filtro mas não puderam ser pontuadas pela IA — verifique se o "
                    + "ANTHROPIC_API_KEY está configurado corretamente no .env.";
        }
        return new BuscarResponse(outcome.matchedCount(), outcome.scored(), aviso);
    }

    private String currentDescription(Principal principal) {
        UserProfile profile = profileStore.find(principal.getName());
        return profile != null ? profile.description() : "";
    }
}
