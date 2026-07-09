package com.jobs.infrastructure.web.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_profile")
public class ProfileEntity {

    @Id
    private String username;

    @Column(length = 2000)
    private String description;

    protected ProfileEntity() {
        // JPA
    }

    public ProfileEntity(String username, String description) {
        this.username = username;
        this.description = description;
    }

    public String getUsername() {
        return username;
    }

    public String getDescription() {
        return description;
    }
}
