    package com.example.mofawid.models;


    import com.example.mofawid.Enums.Role;
    import jakarta.persistence.*;
    import lombok.Data;
    import org.springframework.security.core.GrantedAuthority;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    import org.springframework.security.core.userdetails.UserDetails;

    import java.time.Instant;
    import java.util.Collection;
    import java.util.List;
    import java.util.UUID;

    @Entity
    @Table(name = "users")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    @Data
    public class User implements UserDetails {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "tenant_id", nullable = false)
        private Tenant tenant;

        @Column(nullable = false, unique = true)
        private String email;

        @Column(nullable = false)
        private String username;

        @Column(nullable = false)
        private String password;

        private String nom;
        private String prenom;
        private String phone;

        @Column(nullable = false)
        private Boolean active = true;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private Role role;

        @Column(nullable = false, updatable = false)
        private Instant createdAt = Instant.now();

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.name()
                    )
            );
        }
    }