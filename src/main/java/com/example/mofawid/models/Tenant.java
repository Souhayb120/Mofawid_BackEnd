package com.example.mofawid.models;




import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Data
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String status = "active";

    private String adress;
    private String phone;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}