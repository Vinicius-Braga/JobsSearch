package com.jobs.infrastructure.web;

import com.jobs.infrastructure.web.persistence.AccountEntity;
import com.jobs.infrastructure.web.persistence.AccountJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountJpaRepository repository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAccountAndRedirectsToLoginWhenUsernameIsAvailable() throws Exception {
        when(repository.existsById("novo.usuario")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash");

        mockMvc.perform(post("/cadastro")
                        .param("username", "novo.usuario")
                        .param("password", "senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?cadastro=ok"));

        verify(repository).save(any(AccountEntity.class));
    }

    @Test
    void rejectsDuplicateUsername() throws Exception {
        when(repository.existsById("existente")).thenReturn(true);

        mockMvc.perform(post("/cadastro")
                        .param("username", "existente")
                        .param("password", "senha123"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeExists("erro"));

        verify(repository, never()).save(any(AccountEntity.class));
    }

    @Test
    void rejectsBlankPasswordWithoutTouchingRepository() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .param("username", "usuario")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeExists("erro"));

        verify(repository, never()).existsById(anyString());
        verify(repository, never()).save(any(AccountEntity.class));
    }
}
