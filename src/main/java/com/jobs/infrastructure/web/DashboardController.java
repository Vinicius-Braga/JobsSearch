package com.jobs.infrastructure.web;

import com.jobs.application.SearchJobsForProfileUseCase;
import com.jobs.application.port.CompanyLoader;
import com.jobs.application.port.ProfileStore;
import com.jobs.application.port.SubscriptionStore;
import com.jobs.domain.ClassifiedJob;
import com.jobs.domain.Company;
import com.jobs.domain.Plan;
import com.jobs.domain.UserProfile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Controller
public class DashboardController {

    // Plano gratuito: só mostra as N primeiras vagas (resto vira upsell) e 1 busca/dia.
    private static final int FREE_VISIBLE_RESULTS = 3;
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String PLUS_PRICE = "R$5";

    private final ProfileStore profileStore;
    private final SearchJobsForProfileUseCase searchJobsForProfileUseCase;
    private final CompanyLoader companyLoader;
    private final SubscriptionStore subscriptionStore;

    public DashboardController(ProfileStore profileStore, SearchJobsForProfileUseCase searchJobsForProfileUseCase,
            CompanyLoader companyLoader, SubscriptionStore subscriptionStore) {
        this.profileStore = profileStore;
        this.searchJobsForProfileUseCase = searchJobsForProfileUseCase;
        this.companyLoader = companyLoader;
        this.subscriptionStore = subscriptionStore;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        String username = principal.getName();
        Plan plan = subscriptionStore.getPlan(username);

        model.addAttribute("profile", currentDescription(principal));
        model.addAttribute("plan", plan.name());
        model.addAttribute("planPrice", PLUS_PRICE);
        model.addAttribute("limitReached", plan == Plan.FREE && alreadySearchedToday(username));
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
        String username = principal.getName();
        String description = currentDescription(principal);
        if (description == null || description.isBlank()) {
            return new BuscarResponse(List.of(), 0, "Edite seu perfil antes de buscar vagas.");
        }

        Plan plan = subscriptionStore.getPlan(username);
        if (plan == Plan.FREE && alreadySearchedToday(username)) {
            return new BuscarResponse(List.of(), 0,
                    "Você já usou sua busca de hoje no plano Free. Assine o PLUS pra buscar sem limites.");
        }

        List<Company> companies = companyLoader.load();
        List<ClassifiedJob> results = searchJobsForProfileUseCase.search(companies, new UserProfile(description));
        subscriptionStore.recordSearchNow(username);

        if (plan == Plan.FREE && results.size() > FREE_VISIBLE_RESULTS) {
            List<ClassifiedJob> visible = results.subList(0, FREE_VISIBLE_RESULTS);
            int lockedCount = results.size() - FREE_VISIBLE_RESULTS;
            return new BuscarResponse(visible, lockedCount, null);
        }

        return new BuscarResponse(results, 0, null);
    }

    private boolean alreadySearchedToday(String username) {
        Instant lastSearchAt = subscriptionStore.getLastSearchAt(username);
        if (lastSearchAt == null) {
            return false;
        }
        return LocalDate.ofInstant(lastSearchAt, ZONE).equals(LocalDate.now(ZONE));
    }

    private String currentDescription(Principal principal) {
        UserProfile profile = profileStore.find(principal.getName());
        return profile != null ? profile.description() : "";
    }
}
