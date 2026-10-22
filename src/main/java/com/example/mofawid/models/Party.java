package com.example.mofawid.models;

import com.example.mofawid.Enums.PartyRole;
import com.example.mofawid.Enums.PartyType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "parties")
@Data
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType type;

    // name of the person or of the company
    @Column(nullable = false)
    private String fullName;

    // بطاقة التعريف الوطنية (person)
    private String cin;

    // السجل التجاري (company)
    private String rc;

    // ex: "في شخص مديرها العام" (company)
    private String legalRepresentative;

    private String address;

    private String phone;

    // صفة, ex: "مكري", "أجير"
    private String capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
