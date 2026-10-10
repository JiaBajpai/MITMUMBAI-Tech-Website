package com.mittechkernel.backend.modules.project.service;

import com.mittechkernel.backend.common.exception.BadRequestException;
import com.mittechkernel.backend.common.exception.ForbiddenException;
import com.mittechkernel.backend.common.exception.ResourceNotFoundException;
import com.mittechkernel.backend.common.security.CurrentUser;
import com.mittechkernel.backend.common.security.CurrentUserService;
import com.mittechkernel.backend.modules.project.dto.ProjectMemberResponse;
import com.mittechkernel.backend.modules.project.dto.ProjectRequest;
import com.mittechkernel.backend.modules.project.dto.ProjectResponse;
import com.mittechkernel.backend.modules.project.dto.ProjectReviewRequest;
import com.mittechkernel.backend.modules.project.entity.Project;
import com.mittechkernel.backend.modules.project.entity.ProjectMember;
import com.mittechkernel.backend.modules.project.repository.ProjectMemberRepository;
import com.mittechkernel.backend.modules.project.repository.ProjectRepository;
import com.mittechkernel.backend.security.DomainAuthorizationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Set<String> VALID_PROGRAMS = Set.of("TECHNICAL", "FOUNDATION");
    private static final Set<String> VALID_STATUSES = Set.of(
            "PROPOSED", "UNDER_REVIEW", "CHANGES_REQUESTED", "APPROVED",
            "ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED", "REJECTED"
    );
    private static final Set<String> VALID_REVIEW_DECISIONS = Set.of("APPROVE", "REJECT", "CHANGES_REQUESTED", "UNDER_REVIEW");

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CurrentUserService currentUserService;
    private final DomainAuthorizationService domainAuthorizationService;
    private final JdbcTemplate jdbcTemplate;

    public ProjectService(ProjectRepository projectRepository,
                         ProjectMemberRepository projectMemberRepository,
                         CurrentUserService currentUserService,
                         DomainAuthorizationService domainAuthorizationService,
                         JdbcTemplate jdbcTemplate) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.currentUserService = currentUserService;
        this.domainAuthorizationService = domainAuthorizationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(Long domainId, String status, String program, String query) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        String normalizedStatus = normalizeStatus(status, false);
        String normalizedProgram = normalizeProgram(program, false);

        List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc();

        if (isDomainLead(currentUser)) {
            Set<Long> assignedDomainIds = getAssignedDomainIds(currentUser.id());
            if (assignedDomainIds.isEmpty()) {
                return List.of();
            }
            projects = projects.stream()
                    .filter(project -> assignedDomainIds.contains(project.getDomainId()))
                    .toList();
        }

        if (domainId != null) {
            if (isDomainLead(currentUser) && !hasDomainAccess(currentUser.id(), domainId)) {
                throw new ForbiddenException("You do not have access to that project domain");
            }
            projects = projects.stream()
                    .filter(project -> project.getDomainId().equals(domainId))
                    .toList();
        }

        if (normalizedStatus != null) {
            projects = projects.stream()
                    .filter(project -> normalizedStatus.equalsIgnoreCase(project.getStatus()))
                    .toList();
        }

        if (normalizedProgram != null) {
            projects = projects.stream()
                    .filter(project -> normalizedProgram.equalsIgnoreCase(project.getProgram()))
                    .toList();
        }

        if (query != null && !query.isBlank()) {
            String search = query.trim().toLowerCase(Locale.ROOT);
            projects = projects.stream()
                    .filter(project ->
                            project.getName() != null && project.getName().toLowerCase(Locale.ROOT).contains(search)
                                    || project.getProblem() != null && project.getProblem().toLowerCase(Locale.ROOT).contains(search)
                                    || project.getSolution() != null && project.getSolution().toLowerCase(Locale.ROOT).contains(search)
                    )
                    .toList();
        }

        return projects.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long id) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (isDomainLead(currentUser) && !hasDomainAccess(currentUser.id(), project.getDomainId())) {
            throw new ForbiddenException("You do not have access to that project's domain");
        }

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        CurrentUser currentUser = currentUserService.getCurrentUser();

        if (isDomainLead(currentUser) && !hasDomainAccess(currentUser.id(), project.getDomainId())) {
            throw new ForbiddenException("You do not have access to that project's domain");
        }

        return projectMemberRepository.findByProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(ProjectMemberResponse::from)
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        if (request == null) {
            throw new BadRequestException("Project payload is required");
        }

        CurrentUser currentUser = currentUserService.getCurrentUser();
        validateProjectRequest(request);

        if (isDomainLead(currentUser) && !hasDomainAccess(currentUser.id(), request.domainId())) {
            throw new ForbiddenException("You do not have access to create projects in that domain");
        }

        Project project = new Project();
        project.setName(request.name().trim());
        project.setDomainId(request.domainId());
        project.setProgram(normalizeProgram(request.program(), false));
        if (project.getProgram() == null) {
            project.setProgram("TECHNICAL");
        }
        assertProgramAlignment(currentUser.id(), project.getProgram());
        project.setProblem(request.problem().trim());
        project.setSolution(request.solution().trim());
        project.setTechnologies(normalizeArray(request.technologies()));
        project.setRequiredSkills(normalizeArray(request.requiredSkills()));
        project.setExpectedMembers(request.expectedMembers());
        project.setOutcome(request.outcome() == null ? null : request.outcome().trim());
        project.setStatus("PROPOSED");
        project.setCreatedBy(currentUser.id());
        project.setApprovedBy(null);
        project.setReviewComment(null);

        Project savedProject = projectRepository.save(project);

        ProjectMember owner = new ProjectMember();
        owner.setProjectId(savedProject.getId());
        owner.setUserId(currentUser.id());
        owner.setRole("OWNER");
        owner.setStatus("ACTIVE");
        owner.setReviewedBy(currentUser.id());
        projectMemberRepository.save(owner);

        return toResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        if (request == null) {
            throw new BadRequestException("Project payload is required");
        }

        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (!canManageProject(currentUser, project)) {
            throw new ForbiddenException("You do not have permission to update this project");
        }

        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.domainId() != null) {
            if (isDomainLead(currentUser) && !hasDomainAccess(currentUser.id(), request.domainId())) {
                throw new ForbiddenException("You do not have access to move the project into that domain");
            }
            project.setDomainId(request.domainId());
        }
        if (request.program() != null && !request.program().isBlank()) {
            String nextProgram = normalizeProgram(request.program(), true);
            assertProjectProgramAlignment(project, nextProgram);
            project.setProgram(nextProgram);
        }
        if (request.problem() != null && !request.problem().isBlank()) {
            project.setProblem(request.problem().trim());
        }
        if (request.solution() != null && !request.solution().isBlank()) {
            project.setSolution(request.solution().trim());
        }
        if (request.technologies() != null) {
            project.setTechnologies(normalizeArray(request.technologies()));
        }
        if (request.requiredSkills() != null) {
            project.setRequiredSkills(normalizeArray(request.requiredSkills()));
        }
        if (request.expectedMembers() != null) {
            if (request.expectedMembers() <= 0) {
                throw new BadRequestException("expectedMembers must be greater than zero");
            }
            project.setExpectedMembers(request.expectedMembers());
        }
        if (request.outcome() != null) {
            project.setOutcome(request.outcome().trim());
        }
        if (request.status() != null && !request.status().isBlank()) {
            String nextStatus = normalizeStatus(request.status(), true);
            validateStatusTransition(project.getStatus(), nextStatus);
            boolean reviewerDecision = Set.of("APPROVED", "CHANGES_REQUESTED", "REJECTED").contains(nextStatus)
                    || ("ACTIVE".equals(nextStatus) && !"PAUSED".equals(project.getStatus()));
            if (reviewerDecision && !canReviewProject(currentUser, project)) {
                throw new ForbiddenException("Project approval and review decisions require a project reviewer");
            }
            project.setStatus(nextStatus);
        }
        if (request.reviewComment() != null) {
            project.setReviewComment(request.reviewComment().trim());
        }

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse reviewProject(Long id, ProjectReviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Review payload is required");
        }

        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (!canReviewProject(currentUser, project)) {
            throw new ForbiddenException("You do not have authority to review this project");
        }

        String decision = request.decision().trim().toUpperCase(Locale.ROOT);
        if (!VALID_REVIEW_DECISIONS.contains(decision)) {
            throw new BadRequestException("Invalid project review decision: " + request.decision());
        }

        String nextStatus = switch (decision) {
            case "APPROVE" -> "ACTIVE";
            case "REJECT" -> "REJECTED";
            case "CHANGES_REQUESTED" -> "CHANGES_REQUESTED";
            case "UNDER_REVIEW" -> "UNDER_REVIEW";
            default -> throw new BadRequestException("Unsupported review decision: " + request.decision());
        };

        validateStatusTransition(project.getStatus(), nextStatus);
        project.setStatus(nextStatus);
        project.setApprovedBy(currentUser.id());
        if (request.comment() != null && !request.comment().isBlank()) {
            project.setReviewComment(request.comment().trim());
        }

        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectMemberResponse requestMembership(Long projectId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        assertProgramAlignment(currentUser.id(), project.getProgram());

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUser.id())) {
            throw new BadRequestException("You are already a member of this project");
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(currentUser.id());
        member.setRole("MEMBER");
        member.setStatus("REQUESTED");
        member.setReviewedBy(null);

        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    @Transactional
    public ProjectMemberResponse approveMembership(Long projectId, Long userId) {
        return reviewMembership(projectId, userId, "ACTIVE");
    }

    @Transactional
    public ProjectMemberResponse rejectMembership(Long projectId, Long userId) {
        return reviewMembership(projectId, userId, "REJECTED");
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (!canManageMembership(currentUser, project)) {
            throw new ForbiddenException("You do not have permission to manage project membership");
        }
        if (currentUser.id().equals(userId)) {
            throw new BadRequestException("Use leaveProject to leave the project");
        }
        if (!userExists(userId)) {
            throw new BadRequestException("User not found: " + userId);
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project membership", userId));

        if ("OWNER".equalsIgnoreCase(member.getRole()) || userId.equals(project.getCreatedBy())) {
            throw new BadRequestException("Project owner cannot be removed from the project");
        }

        projectMemberRepository.delete(member);
    }

    @Transactional
    public void leaveProject(Long projectId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Project membership", currentUser.id()));

        if ("OWNER".equalsIgnoreCase(membership.getRole()) || currentUser.id().equals(project.getCreatedBy())) {
            throw new BadRequestException("Project owner cannot leave the project");
        }

        projectMemberRepository.delete(membership);
    }

    private void validateProjectRequest(ProjectRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Project name is required");
        }
        if (request.domainId() == null) {
            throw new BadRequestException("domainId is required");
        }
        if (request.problem() == null || request.problem().isBlank()) {
            throw new BadRequestException("problem is required");
        }
        if (request.solution() == null || request.solution().isBlank()) {
            throw new BadRequestException("solution is required");
        }
        if (request.expectedMembers() == null || request.expectedMembers() <= 0) {
            throw new BadRequestException("expectedMembers must be greater than zero");
        }
    }

    private boolean canManageProject(CurrentUser currentUser, Project project) {
        if (isPrivileged(currentUser)) {
            return true;
        }
        if (currentUser.id().equals(project.getCreatedBy())) {
            return true;
        }
        if (isDomainLead(currentUser)) {
            return hasDomainAccess(currentUser.id(), project.getDomainId());
        }
        return false;
    }

    private boolean canReviewProject(CurrentUser currentUser, Project project) {
        if (isPrivileged(currentUser)) {
            return true;
        }
        return isDomainLead(currentUser) && hasDomainAccess(currentUser.id(), project.getDomainId());
    }

    @Transactional(readOnly = true)
    public void assertCanReviewProject(Long projectId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        if (!canReviewProject(currentUser, project)) {
            throw new ForbiddenException("You do not have permission to verify contributions for this project");
        }
    }

    @Transactional(readOnly = true)
    public void assertUserProgramMatchesProject(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        assertProgramAlignment(userId, project.getProgram());
    }

    @Transactional(readOnly = true)
    public void assertCanAssignTaskToProject(Long projectId, Long sessionId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        if (!canManageProject(currentUser, project)) {
            throw new ForbiddenException("You do not have permission to assign tasks to this project");
        }
        SessionScope session = jdbcTemplate.query("SELECT domain_id, program FROM sessions WHERE id = ?", rs ->
                rs.next() ? new SessionScope(rs.getLong("domain_id"), rs.getString("program")) : null, sessionId);
        if (session == null) {
            throw new BadRequestException("Task session not found");
        }
        if (!session.domainId().equals(project.getDomainId())) {
            throw new BadRequestException("Task session and project must belong to the same domain");
        }
        if (!project.getProgram().equalsIgnoreCase(session.program())) {
            throw new BadRequestException("Task session and project must belong to the same program");
        }
    }

    private record SessionScope(Long domainId, String program) {}

    private boolean canManageMembership(CurrentUser currentUser, Project project) {
        return canManageProject(currentUser, project);
    }

    private void assertProjectProgramAlignment(Project project, String program) {
        assertProgramAlignment(project.getCreatedBy(), program);
        Boolean hasMisalignedMembers = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM project_members pm JOIN users u ON u.id = pm.user_id WHERE pm.project_id = ? AND u.program <> ?)",
                Boolean.class,
                project.getId(),
                program
        );
        if (Boolean.TRUE.equals(hasMisalignedMembers)) {
            throw new BadRequestException("Project program cannot change while it has members from another program");
        }
    }

    private void assertProgramAlignment(Long userId, String projectProgram) {
        String userProgram = jdbcTemplate.queryForObject(
                "SELECT program FROM users WHERE id = ?",
                String.class,
                userId
        );
        if (!projectProgram.equalsIgnoreCase(userProgram)) {
            throw new ForbiddenException("User and project programs must match");
        }
    }

    private ProjectMemberResponse reviewMembership(Long projectId, Long userId, String targetStatus) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (currentUser.id().equals(userId)) {
            throw new ForbiddenException("You cannot approve or reject your own project membership request");
        }
        if (!canManageMembership(currentUser, project)) {
            throw new ForbiddenException("You do not have permission to approve project membership");
        }
        if (!userExists(userId)) {
            throw new BadRequestException("User not found: " + userId);
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project membership", userId));

        validateMembershipTransition(member.getStatus(), targetStatus);
        if ("ACTIVE".equals(targetStatus)) {
            assertProgramAlignment(userId, project.getProgram());
        }
        member.setStatus(targetStatus);
        member.setReviewedBy(currentUser.id());

        return ProjectMemberResponse.from(projectMemberRepository.save(member));
    }

    private void validateMembershipTransition(String currentStatus, String nextStatus) {
        String normalizedCurrent = currentStatus == null ? "REQUESTED" : currentStatus.trim().toUpperCase(Locale.ROOT);
        String normalizedNext = nextStatus == null ? "" : nextStatus.trim().toUpperCase(Locale.ROOT);

        Set<String> validTransitions = switch (normalizedCurrent) {
            case "REQUESTED" -> Set.of("ACTIVE", "REJECTED");
            case "ACTIVE" -> Set.of();
            case "REJECTED" -> Set.of();
            default -> Set.of();
        };

        if (!validTransitions.contains(normalizedNext)) {
            throw new BadRequestException("Invalid membership status transition from " + normalizedCurrent + " to " + normalizedNext);
        }
    }

    private boolean isPrivileged(CurrentUser currentUser) {
        return currentUser.roles().contains("SUPER_ADMIN") || currentUser.roles().contains("CORE_MEMBER");
    }

    private boolean isDomainLead(CurrentUser currentUser) {
        return currentUser.roles().contains("DOMAIN_LEAD");
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

    private String normalizeProgram(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BadRequestException("program is required");
            }
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!VALID_PROGRAMS.contains(normalized)) {
            throw new BadRequestException("Invalid program: " + value);
        }
        return normalized;
    }

    private String normalizeStatus(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new BadRequestException("status is required");
            }
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new BadRequestException("Invalid project status: " + value);
        }
        return normalized;
    }

    private void validateStatusTransition(String currentStatus, String nextStatus) {
        if (currentStatus == null) {
            currentStatus = "PROPOSED";
        }
        Set<String> validTransitions = switch (currentStatus) {
            case "PROPOSED" -> Set.of("UNDER_REVIEW", "REJECTED", "ACTIVE");
            case "UNDER_REVIEW" -> Set.of("APPROVED", "CHANGES_REQUESTED", "REJECTED", "ACTIVE");
            case "CHANGES_REQUESTED" -> Set.of("UNDER_REVIEW", "APPROVED", "REJECTED", "ACTIVE");
            case "APPROVED" -> Set.of("ACTIVE");
            case "ACTIVE" -> Set.of("PAUSED", "COMPLETED", "ARCHIVED");
            case "PAUSED" -> Set.of("ACTIVE", "COMPLETED", "ARCHIVED");
            case "COMPLETED" -> Set.of("ARCHIVED");
            case "ARCHIVED" -> Set.of();
            case "REJECTED" -> Set.of();
            default -> Set.of();
        };

        if (!validTransitions.contains(nextStatus)) {
            throw new BadRequestException("Invalid project status transition from " + currentStatus + " to " + nextStatus);
        }
    }

    private String[] normalizeArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new String[0];
        }
        return values.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(item -> item.trim())
                .toArray(String[]::new);
    }

    private boolean userExists(Long userId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE id = ?)",
                Boolean.class,
                userId
        );
        return Boolean.TRUE.equals(exists);
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.from(project, projectMemberRepository.findByProjectIdOrderByCreatedAtAsc(project.getId()));
    }
}
