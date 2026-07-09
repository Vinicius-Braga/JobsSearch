package com.jobs.infrastructure.web;

import com.jobs.infrastructure.web.persistence.AccountEntity;
import com.jobs.infrastructure.web.persistence.AccountJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AccountController {

    private final AccountJpaRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(AccountJpaRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/cadastro")
    public String form() {
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String create(@RequestParam String username, @RequestParam String password, Model model) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("erro", "Preencha usuário e senha.");
            return "cadastro";
        }
        if (repository.existsById(username)) {
            model.addAttribute("erro", "Esse usuário já existe.");
            return "cadastro";
        }

        repository.save(new AccountEntity(username, passwordEncoder.encode(password)));
        return "redirect:/login?cadastro=ok";
    }
}
