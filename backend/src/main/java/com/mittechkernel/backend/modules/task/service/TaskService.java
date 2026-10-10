package com.mittechkernel.backend.modules.task.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.task.dto.TaskCompletionRequest;
import com.mittechkernel.backend.modules.task.dto.TaskRequest;
import com.mittechkernel.backend.modules.task.dto.TaskResponse;
import com.mittechkernel.backend.modules.task.entity.Task;
import com.mittechkernel.backend.modules.task.repository.TaskRepository;
import com.mittechkernel.backend.modules.project.service.ProjectService;
import com.mittechkernel.backend.modules.project.service.ProjectGitHubRepositoryService;
import com.mittechkernel.backend.modules.project.entity.ProjectGitHubRepository;
import com.mittechkernel.backend.modules.project.repository.GitHubContributionRepository;
import com.mittechkernel.backend.modules.gamification.XpService;
import com.mittechkernel.backend.security.DomainAuthorizationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "COMPLETED", "VERIFIED");

    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;
    private final DomainAuthorizationService domainAuthorizationService;
    private final JdbcTemplate jdbcTemplate;
    private final ProjectService projectService;
    private final ProjectGitHubRepositoryService projectGitHubRepositoryService;
    private final GitHubContributionRepository contributionRepository;
    private final XpService xpService;

    public TaskService(TaskRepository taskRepository,
                      CurrentUserService currentUserService,
                      DomainAuthorizationService domainAuthorizationService,
                      JdbcTemplate jdbcTemplate,
                      ProjectService projectService,
                      ProjectGitHubRepositoryService projectGitHubRepositoryService,
                      GitHubContributionRepository contributionRepository,
                      XpService xpService) {
        this.taskRepository = taskRepository;
        this.currentUserService = currentUserService;
        this.domainAuthorizationService = domainAuthorizationService;
        this.jdbcTemplate = jdbcTemplate;
        this.projectService = projectService;
        this.projectGitHubRepositoryService = projectGitHubRepositoryService;
        this.contributionRepository = contributionRepository;
        this.xpService = xpService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Long sessionId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();

        if (currentUser.roles().contains("DOMAIN_LEAD")) {
            Set<Long> assignedDomains = getAssignedDomainIds(currentUser.id());
            return taskRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(task -> sessionDomainId(task.getSessionId()) != null)
                    .filter(task -> assignedDomains.contains(sessionDomainId(task.getSessionId())))
                    .map(TaskResponse::from)
                    .toList();
        }

        if (sessionId != null) {
            return taskRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                    .map(TaskResponse::from)
                    .toList();
        }

        return taskRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        Long sessionDomainId = sessionDomainId(task.getSessionId());
        if (currentUser.roles().contains("DOMAIN_LEAD") && !hasDomainAccess(currentUser.id(), sessionDomainId)) {
            throw new ForbiddenException("You do not have access to that task's domain");
        }

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        ensureTaskCreatorAllowed(currentUser, request.sessionId());

        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("Task title is required");
        }
        if (request.requirements() == null || request.requirements().isEmpty()) {
            throw new BadRequestException("At least one requirement is required");
        }

        Task task = new Task();
        task.setSessionId(request.sessionId());
        if (request.projectId() != null) {
            projectService.assertCanAssignTaskToProject(request.projectId(), request.sessionId());
            task.setProjectId(request.projectId());
        }
        task.setTitle(request.title().trim());
        task.setRequirements(request.requirements().stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new));
        task.setDeadline(request.deadline());
        task.setVerificationRequired(Boolean.TRUE.equals(request.verificationRequired()));
        if (request.assigneeId() != null) {
            validateAssigneeProgram(request.sessionId(), request.assigneeId());
        }
        task.setAssigneeId(request.assigneeId());
        String requestedStatus = normalizeStatus(request.status(), "OPEN");
        if (!"OPEN".equals(requestedStatus)) {
            throw new BadRequestException("New tasks must start in OPEN status");
        }
        task.setStatus("OPEN");

        Task savedTask = taskRepository.save(task);
        return TaskResponse.from(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        if (!canManageTask(currentUser, task)) {
            throw new ForbiddenException("You are not allowed to update this task");
        }

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.projectId() != null && !request.projectId().equals(task.getProjectId())) {
            projectService.assertCanAssignTaskToProject(request.projectId(), task.getSessionId());
            task.setProjectId(request.projectId());
        }
        if (request.requirements() != null) {
            task.setRequirements(request.requirements().stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new));
        }
        if (request.deadline() != null) {
            task.setDeadline(request.deadline());
        }
        if (request.verificationRequired() != null) {
            task.setVerificationRequired(request.verificationRequired());
        }
        if (request.assigneeId() != null) {
            validateAssigneeProgram(task.getSessionId(), request.assigneeId());
            task.setAssigneeId(request.assigneeId());
        }
        if (request.status() != null && !request.status().isBlank()) {
            throw new BadRequestException("Use the task completion and verification endpoints to change task status");
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse completeTask(Long id, TaskCompletionRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        if (isReadOnlyFaculty(currentUser)) {
            throw new ForbiddenException("Faculty accounts have read-only access");
        }
        if (!userProgram(currentUser.id()).equalsIgnoreCase(sessionProgram(task.getSessionId()))) {
            throw new ForbiddenException("Task and participant programs must match");
        }
        if ("FOUNDATION".equals(sessionProgram(task.getSessionId()))
                && (!currentUser.id().equals(task.getAssigneeId())
                    || !"FOUNDATION".equals(userProgram(currentUser.id())))) {
            throw new ForbiddenException("Only the assigned Foundation participant can complete this task");
        }

        if (!currentUser.id().equals(task.getAssigneeId()) && !isPrivileged(currentUser)) {
            throw new ForbiddenException("Only the assignee or a privileged role can complete this task");
        }
        if (request.repoUrl() == null || request.repoUrl().isBlank()) {
            throw new BadRequestException("repoUrl is required to complete the task");
        }
        if (Boolean.TRUE.equals(task.isVerificationRequired()) && (request.commitSha() == null || request.commitSha().isBlank())) {
            throw new BadRequestException("Commit SHA is required when task verification is enabled");
        }
        if (!"OPEN".equals(task.getStatus())) {
            throw new BadRequestException("Only OPEN tasks can be completed");
        }

        task.setStatus("COMPLETED");
        taskRepository.save(task);

        Long currentTaskId = task.getId();
        String repoUrl = request.repoUrl().trim();
        String commitSha = request.commitSha() == null ? null : request.commitSha().trim();
        String notes = request.notes() == null ? null : request.notes().trim();

        jdbcTemplate.update(
                "INSERT INTO task_completions (task_id, user_id, repo_url, commit_sha, notes, verified, verified_by, verified_at, created_at) VALUES (?, ?, ?, ?, ?, false, NULL, NULL, NOW())",
                currentTaskId,
                currentUser.id(),
                repoUrl,
                commitSha,
                notes
        );

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse verifyTask(Long id) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        Long sessionDomainId = sessionDomainId(task.getSessionId());
        if (!isPrivileged(currentUser) && (isReadOnlyFaculty(currentUser)
                || "FOUNDATION".equals(sessionProgram(task.getSessionId()))
                || !hasDomainAccess(currentUser.id(), sessionDomainId))) {
            throw new ForbiddenException("Only a privileged user or the session domain lead can verify this task");
        }
        if (!"COMPLETED".equals(task.getStatus())) {
            throw new BadRequestException("Only completed tasks can be verified");
        }

        if (task.isVerificationRequired()) {
            verifyGitHubBackedTask(task, currentUser);
            return TaskResponse.from(task);
        }

        task.setStatus("VERIFIED");
        taskRepository.save(task);

        List<Long> verifiedCompletionIds = jdbcTemplate.query(
                "UPDATE task_completions SET verified = true, verified_by = ?, verified_at = NOW() WHERE task_id = ? AND user_id = ? RETURNING id",
                (rs, rowNum) -> rs.getLong("id"),
                currentUser.id(),
                task.getId(),
                task.getAssigneeId()
        );
        taskRepository.saveAndFlush(task);
        verifiedCompletionIds.forEach(xpService::awardVerifiedTaskCompletion);

        return TaskResponse.from(task);
    }

    private void verifyGitHubBackedTask(Task task, CurrentUser verifier) {
        if (task.getProjectId() == null) {
            throw new BadRequestException("Verification-required task must be assigned to a project");
        }
        projectService.assertCanReviewProject(task.getProjectId());
        projectService.assertCanAssignTaskToProject(task.getProjectId(), task.getSessionId());
        ProjectGitHubRepository linked = projectGitHubRepositoryService
                .requireLinkedRepositoryWithProjectAccess(task.getProjectId());
        TaskCompletion completion = jdbcTemplate.query("""
                SELECT id, user_id, commit_sha FROM task_completions
                WHERE task_id = ? ORDER BY created_at DESC, id DESC LIMIT 1
        """, rs -> rs.next() ? new TaskCompletion(rs.getLong("id"), rs.getLong("user_id"), rs.getString("commit_sha")) : null,
                task.getId());
        if (completion == null || completion.commitSha() == null || !completion.commitSha().matches("[0-9a-f]{40}")) {
            throw new BadRequestException("A valid task completion commit SHA is required");
        }
        GitHubContributionRepository.ContributionForVerification contribution = contributionRepository
                .findByRepositoryAndSha(linked.getId(), completion.commitSha())
                .orElseThrow(() -> new BadRequestException("No persisted contribution matches the task completion SHA"));
        if (!task.getProjectId().equals(contribution.projectId())) {
            throw new BadRequestException("Contribution does not belong to the task project");
        }
        projectService.assertUserProgramMatchesProject(task.getProjectId(), completion.userId());
        if (!completion.userId().equals(contribution.userId())) {
            throw new ForbiddenException("Contribution author does not match the task completer");
        }
        if (contribution.verified()) {
            throw new BadRequestException("Contribution has already been verified");
        }
        if (contribution.userId() == null || contribution.githubAuthorId() == null
                || !contributionRepository.identityMatches(contribution.githubAuthorId(), contribution.userId())) {
            throw new BadRequestException("Contribution author is not linked to a Kernel user by GitHub ID");
        }
        if (!contributionRepository.isActiveMember(task.getProjectId(), contribution.userId())) {
            throw new ForbiddenException("Contribution author must be an active project member");
        }
        if (contributionRepository.markVerified(contribution.id(), verifier.id()) != 1) {
            throw new BadRequestException("Contribution has already been verified");
        }
        int completionUpdated = jdbcTemplate.update("""
                UPDATE task_completions SET verified = true, verified_by = ?, verified_at = now()
                WHERE task_id = ? AND user_id = ? AND commit_sha = ? AND verified = false
                """, verifier.id(), task.getId(), completion.userId(), completion.commitSha());
        if (completionUpdated != 1) {
            throw new BadRequestException("Task completion has already been verified");
        }
        task.setStatus("VERIFIED");
        taskRepository.saveAndFlush(task);
        xpService.awardVerifiedTaskCompletion(completion.id());
        xpService.awardVerifiedContribution(contribution.id());
    }

    private record TaskCompletion(Long id, Long userId, String commitSha) {}

    private void ensureTaskCreatorAllowed(CurrentUser currentUser, Long sessionId) {
        if (isPrivileged(currentUser)) {
            return;
        }

        if ("FOUNDATION".equals(sessionProgram(sessionId))) {
            throw new ForbiddenException("Only a privileged role can manage Foundation tasks");
        }

        Long sessionDomainId = sessionDomainId(sessionId);
        if (sessionDomainId == null) {
            throw new BadRequestException("Session not found");
        }
        if (!hasDomainAccess(currentUser.id(), sessionDomainId)) {
            throw new ForbiddenException("You do not have access to create tasks for this session");
        }
    }

    private boolean canManageTask(CurrentUser currentUser, Task task) {
        if (isPrivileged(currentUser)) {
            return true;
        }
        if (isReadOnlyFaculty(currentUser) || "FOUNDATION".equals(sessionProgram(task.getSessionId()))) {
            return false;
        }
        Long sessionDomainId = sessionDomainId(task.getSessionId());
        return hasDomainAccess(currentUser.id(), sessionDomainId);
    }

    private boolean isPrivileged(CurrentUser currentUser) {
        return currentUser.roles().contains("SUPER_ADMIN") || currentUser.roles().contains("CORE_MEMBER");
    }

    private boolean isReadOnlyFaculty(CurrentUser currentUser) {
        return currentUser.roles().contains("FACULTY") && !isPrivileged(currentUser);
    }

    private void validateAssigneeProgram(Long sessionId, Long assigneeId) {
        String program = sessionProgram(sessionId);
        if (program == null || !program.equalsIgnoreCase(userProgram(assigneeId))) {
            throw new BadRequestException("Task assignee must belong to the session program");
        }
    }

    private String sessionProgram(Long sessionId) {
        if (sessionId == null) return null;
        try {
            return jdbcTemplate.queryForObject("SELECT program FROM sessions WHERE id = ?", String.class, sessionId);
        } catch (Exception ex) {
            return null;
        }
    }

    private String userProgram(Long userId) {
        try {
            return jdbcTemplate.queryForObject("SELECT program FROM users WHERE id = ?", String.class, userId);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean hasDomainAccess(Long userId, Long domainId) {
        return domainId != null && domainAuthorizationService.hasDomainAccess(userId, domainId);
    }

    private Set<Long> getAssignedDomainIds(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT domain_id FROM domain_leads WHERE user_id = ? AND start_date <= CURRENT_DATE AND (end_date IS NULL OR end_date >= CURRENT_DATE)",
                Long.class,
                userId
        ).stream().collect(Collectors.toSet());
    }

    private Long sessionDomainId(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT domain_id FROM sessions WHERE id = ?",
                    Long.class,
                    sessionId
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new BadRequestException("Invalid task status: " + status);
        }
        return normalized;
    }

    private void validateTransition(String current, String next, CurrentUser currentUser, Task task) {
        if (current == null) {
            current = "OPEN";
        }
        boolean allowed = switch (current) {
            case "OPEN" -> "COMPLETED".equals(next);
            case "COMPLETED" -> "VERIFIED".equals(next);
            case "VERIFIED" -> "VERIFIED".equals(next);
            default -> false;
        };

        if (!allowed) {
            throw new BadRequestException("Invalid task status transition from " + current + " to " + next);
        }

        if ("VERIFIED".equals(next) && !isPrivileged(currentUser) && !hasDomainAccess(currentUser.id(), sessionDomainId(task.getSessionId()))) {
            throw new ForbiddenException("Only a privileged role or domain lead can verify this task");
        }
    }
}
