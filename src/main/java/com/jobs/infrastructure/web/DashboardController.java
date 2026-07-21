package com.jobs.infrastructure.web;

import com.jobs.application.SearchJobsForProfileUseCase;
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
    private final SearchJobsForProfileUseCase searchJobsForProfileUseCase;
    private final CompanyLoader companyLoader;

    public DashboardController(ProfileStore profileStore, SearchJobsForProfileUseCase searchJobsForProfileUseCase,
            CompanyLoader companyLoader) {
        this.profileStore = profileStore;
        this.searchJobsForProfileUseCase = searchJobsForProfileUseCase;
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
            return new BuscarResponse(List.of(), "Edite seu perfil antes de buscar vagas.");
        }

        List<Company> companies = companyLoader.load();
        var result = searchJobsForProfileUseCase.search(companies, new UserProfile(description));

        String aviso = result.filterExtracted() ? null
                : "Não foi possível aplicar os filtros do seu perfil (falha ao consultar a IA) — "
                        + "mostrando todas as vagas encontradas, sem filtro.";
        return new BuscarResponse(result.jobs(), aviso);
    }

    private String currentDescription(Principal principal) {
        UserProfile profile = profileStore.find(principal.getName());
        return profile != null ? profile.description() : "";
    }
}
