package com.solarFlow.solarFlow.project.service;

import com.solarFlow.solarFlow.auth.security.AuthUser;
import com.solarFlow.solarFlow.lead.service.LeadService;
import com.solarFlow.solarFlow.project.controller.CreateProjectRequestDTO;
import com.solarFlow.solarFlow.project.controller.ProjectResponseDTO;
import com.solarFlow.solarFlow.project.controller.ProjectStatusRequestDTO;
import com.solarFlow.solarFlow.project.controller.PublicProjectStatusDTO;
import com.solarFlow.solarFlow.project.domain.Project;
import com.solarFlow.solarFlow.project.domain.ProjectStatus;
import com.solarFlow.solarFlow.project.domain.ProjectStatusHistory;
import com.solarFlow.solarFlow.project.repo.ProjectRepo;
import com.solarFlow.solarFlow.project.repo.ProjectStatusHistoryRepo;
import com.solarFlow.solarFlow.quotation.domain.Quotation;
import com.solarFlow.solarFlow.quotation.domain.QuotationStatus;
import com.solarFlow.solarFlow.quotation.dto.QuotationSnapshot;
import com.solarFlow.solarFlow.quotation.repo.QuotationRepo;
import com.solarFlow.solarFlow.user.domain.Role;
import com.solarFlow.solarFlow.user.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String STATUS_LINK_PREFIX = "/api/v1/status/";

    private final ProjectRepo projects;
    private final ProjectStatusHistoryRepo history;
    private final QuotationRepo quotations;
    private final LeadService leadService;
    private final UserRepo users;
    private final JsonMapper jsonMapper;

    // ---------------- create ----------------

    @Transactional
    public ProjectResponseDTO create(CreateProjectRequestDTO request, AuthUser actor) {

        Quotation quotation = quotations.findById(request.quotationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quotation not found"));

        // Staff can access any lead; this also keeps the rule consistent if roles change later.
        leadService.findAccessible(quotation.getLead().getId(), actor);

        if (quotation.getStatus() != QuotationStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only an accepted quotation can become a project");
        }

        if (projects.existsByQuotationId(quotation.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A project already exists for this quotation");
        }

        BigDecimal netCost = readSnapshot(quotation).pricing().netSystemCost();

        if (request.advanceAmount().compareTo(netCost) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Advance amount cannot exceed the net system cost of " + netCost);
        }

        Project project = Project.builder()
                .quotation(quotation)
                .lead(quotation.getLead())
                .status(ProjectStatus.ADVANCE_PAID)
                .advanceAmount(request.advanceAmount())
                .statusToken(newToken())
                .build();

        try {
            project = projects.saveAndFlush(project);
        } catch (DataIntegrityViolationException e) {
            // Two staff members creating the same project at the same moment.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A project already exists for this quotation");
        }

        writeHistory(project, null, ProjectStatus.ADVANCE_PAID, "Project created with advance payment", actor);

        log.info("Project {} created from quotation {}", project.getId(), quotation.getId());
        return toDto(project, actor);
    }

    // ---------------- status change ----------------

    @Transactional
    public ProjectResponseDTO changeStatus(UUID projectId, ProjectStatusRequestDTO request, AuthUser actor) {

        Project project = projects.findById(projectId)
                .orElseThrow(ProjectService::notFound);

        ProjectStatus from = project.getStatus();
        ProjectStatus to = request.status();

        if (!from.canMoveTo(to)) {
            ProjectStatus allowed = from.next();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    allowed == null
                            ? "Project is already complete (" + from + ")"
                            : "Cannot move from " + from + " to " + to + ". Next allowed stage is " + allowed);
        }

        project.setStatus(to);

        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        writeHistory(project, from, to, note, actor);

        log.info("Project {} moved {} -> {}", project.getId(), from, to);
        return toDto(project, actor);
    }

    // ---------------- reads ----------------

    @Transactional(readOnly = true)
    public ProjectResponseDTO get(UUID projectId, AuthUser actor) {
        Project project = projects.findById(projectId)
                .orElseThrow(ProjectService::notFound);
        requireAccess(project, actor);
        return toDto(project, actor);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> list(AuthUser actor) {
        List<Project> found = actor.role() == Role.HOMEOWNER
                ? projects.findByLeadOwnerIdOrderByCreatedAtDesc(actor.id())
                : projects.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100));
        return found.stream().map(p -> toDto(p, actor)).toList();
    }

    /** Public, token-secured view. Returns only stage information. */
    @Transactional(readOnly = true)
    public PublicProjectStatusDTO publicStatus(String token) {

        Project project = projects.findByStatusToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status link not found"));

        List<PublicProjectStatusDTO.Stage> timeline = history
                .findByProjectIdOrderByChangedAtAsc(project.getId()).stream()
                .map(h -> new PublicProjectStatusDTO.Stage(h.getToStatus(), h.getChangedAt()))
                .toList();

        return new PublicProjectStatusDTO(
                project.getStatus(),
                project.getQuotation().getSelectedCapacityKw(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                timeline);
    }

    // ---------------- helpers ----------------

    private void requireAccess(Project project, AuthUser actor) {
        try {
            leadService.findAccessible(project.getLead().getId(), actor);
        } catch (ResponseStatusException e) {
            // Same 404 as a missing project, so ids are not revealed.
            throw notFound();
        }
    }

    private void writeHistory(Project project, ProjectStatus from, ProjectStatus to, String note, AuthUser actor) {
        history.save(ProjectStatusHistory.builder()
                .project(project)
                .fromStatus(from)
                .toStatus(to)
                .note(note)
                .changedBy(users.getReferenceById(actor.id()))
                .build());
    }

    private ProjectResponseDTO toDto(Project project, AuthUser actor) {

        boolean staff = actor.role() != Role.HOMEOWNER;

        List<ProjectResponseDTO.HistoryEntry> entries = history
                .findByProjectIdOrderByChangedAtAsc(project.getId()).stream()
                .map(h -> new ProjectResponseDTO.HistoryEntry(
                        h.getFromStatus(),
                        h.getToStatus(),
                        staff ? h.getNote() : null,     // staff notes stay internal
                        h.getChangedAt()))
                .toList();

        Quotation quotation = project.getQuotation();

        return new ProjectResponseDTO(
                project.getId(),
                quotation.getId(),
                project.getLead().getId(),
                project.getStatus(),
                quotation.getSelectedCapacityKw(),
                readSnapshot(quotation).pricing().netSystemCost(),
                project.getAdvanceAmount(),
                STATUS_LINK_PREFIX + project.getStatusToken(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                entries);
    }

    private QuotationSnapshot readSnapshot(Quotation quotation) {
        try {
            return jsonMapper.readValue(quotation.getSnapshotJson(), QuotationSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read quotation snapshot", e);
        }
    }

    /** 32 random bytes from SecureRandom, URL-safe: 43 characters, not guessable. */
    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
    }
}