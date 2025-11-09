package com.app.thinknshare.user.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private UUID id = UuidCreator.getTimeOrdered();

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;
    
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;
}
