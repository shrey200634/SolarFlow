package com.solarFlow.solarFlow.project.repo;

import com.solarFlow.solarFlow.project.domain.ProjectStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectStatusHistoryRepo extends JpaRepository<ProjectStatusHistory, UUID> {

    List<ProjectStatusHistory> findByProjectIdOrderByChangedAtAsc(UUID projectId);
}