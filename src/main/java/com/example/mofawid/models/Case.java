package com.example.mofawid.models;

import com.example.mofawid.Enums.CaseStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cases")
@Data
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ref;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus caseStatus = CaseStatus.QUALIFIED;

    @Column(nullable = false)
    private String clientName;

    private String clientIce;

    @Column(nullable = false)
    private String address;

    private String phone;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}