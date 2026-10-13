package com.example.mofawid.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_trackings")
@Data
public class CaseTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "case_id",
            nullable = false,
            unique = true
    )
    private Case caseEntity;

    @Column(
            nullable = false,
            unique = true,
            length = 64
    )
    private String codeHash;

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt = Instant.now();
}