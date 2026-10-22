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

    // ===== letterhead of the reports (رأسية المحضر) =====

    // ex: عبد الغني أشرع
    private String huissierNameAr;

    // ex: ACHRAY ABDELGHANI
    private String huissierNameFr;

    // ex: الدار البيضاء -> "مفوض قضائي بدائرة استئناف الدار البيضاء"
    private String appealCourtAr;

    // ex: Casablanca -> "Huissier de justice près la Cour d'appel de Casablanca"
    private String appealCourtFr;

    // ex: المحاكم الابتدائية بالدار البيضاء
    private String firstInstanceCourtAr;

    // the address in french (the arabic one is "adress")
    private String addressFr;

    private String email;

    private String city;

    // first register number to use (مرجعنا) when the office comes from a paper register
    private Integer startNumber;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}