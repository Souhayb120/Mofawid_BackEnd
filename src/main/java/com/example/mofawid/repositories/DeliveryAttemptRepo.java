package com.example.mofawid.repositories;

import com.example.mofawid.models.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepo extends JpaRepository<DeliveryAttempt, UUID> {

    // the visits in the order they happened
    List<DeliveryAttempt> findAllByMissionIdOrderByAttemptedAtAsc(UUID missionId);
}
