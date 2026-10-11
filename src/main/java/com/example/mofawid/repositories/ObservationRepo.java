package com.example.mofawid.repositories;

import com.example.mofawid.models.Observation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObservationRepo
        extends JpaRepository<Observation, UUID> {

    Page<Observation> findAllByMissionId(
            UUID missionId,
            Pageable pageable
    );

    List<Observation> findAllByMissionId(UUID missionId);
}