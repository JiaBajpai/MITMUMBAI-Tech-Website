# MIT TECH KERNEL Backend - RBAC Conceptual Model

## Roles

The system defines five system-wide roles. Roles are assigned to users via the `user` module (`UserRole` entity). A user can have multiple roles.

---

### SUPER_ADMIN

**Scope**: Global (entire system)

**Responsibilities**:
- Full system access
- User management (create, deactivate, delete any user)
- Role assignment for any user
- Domain creation and domain lead assignment
- System configuration (feature flags, global settings)
- Audit log access
- Database migration approval (via Flyway)
- Security policy management

**Permissions**: All permissions (`*`)

**Assignment**: Only by another SUPER_ADMIN (initial SUPER_ADMIN via database seed)

---

### CORE_MEMBER

**Scope**: Global (club operations)

**Responsibilities**:
- Member application review (approve/reject)
- Event creation and management (all domains)
- Event registration oversight
- Foundation Program coordination
- Leaderboard management (manual adjustments, configuration)
- Announcements and communications
- Club-level analytics and reporting

**Permissions**:
- `member:application:review`
- `event:create`, `event:update`, `event:delete`, `event:publish`, `event:cancel`
- `registration:override`, `registration:view_all`
- `foundation:coordinate`, `foundation:manage_mentors`
- `leaderboard:configure`, `leaderboard:adjust_scores`
- `analytics:view_club_reports`

**Assignment**: By SUPER_ADMIN

---

### FACULTY

**Scope**: Global (advisory/oversight)

**Responsibilities**:
- View-only access to club analytics and reports
- Event approval for official events
- Foundation Program advisory (view progress, provide feedback)
- Student mentorship (assigned students)
- Domain advisory (assigned domains)

**Permissions**:
- `analytics:view_reports`
- `event:approve_official`
- `foundation:view_progress`, `foundation:provide_feedback`
- `user:view_assigned_students`
- `domain:view_assigned`

**Assignment**: By SUPER_ADMIN or CORE_MEMBER

---

### DOMAIN_LEAD

**Scope**: Scoped to **one or more assigned technical domains**

**Critical**: DOMAIN_LEAD is **NOT** a globally privileged role. All permissions are restricted to the domains explicitly assigned to the user via `DomainLeadAssignment` in the `domain` module.

**Responsibilities** (within assigned domains only):
- Domain-specific event creation and management
- Domain member management (view, add, remove)
- Domain-specific registration oversight
- Domain leaderboard viewing
- Domain analytics and reporting
- Domain settings configuration
- Mentor assignment for Foundation participants in their domain

**Permissions** (domain-scoped):
- `domain:{domainId}:event:create`, `domain:{domainId}:event:update`, `domain:{domainId}:event:delete`
- `domain:{domainId}:member:view`, `domain:{domainId}:member:manage`
- `domain:{domainId}:registration:view`, `domain:{domainId}:registration:override`
- `domain:{domainId}:leaderboard:view`
- `domain:{domainId}:analytics:view`
- `domain:{domainId}:settings:update`
- `domain:{domainId}:foundation:assign_mentors`

**Assignment**: By SUPER_ADMIN or CORE_MEMBER via `domain` module (`DomainLeadAssignment` with start/end dates)

**Revocation**: Automatic on end date, or manual by SUPER_ADMIN/CORE_MEMBER

---

### STUDENT

**Scope**: Global (base role for all authenticated users)

**Responsibilities**:
- View public events and register
- View own registrations and attendance
- View own profile and update preferences
- Apply for membership
- Participate in Foundation Program (if enrolled)
- View public leaderboards
- Submit feedback

**Permissions**:
- `event:view_public`, `event:register_self`
- `registration:view_own`, `registration:cancel_own`
- `profile:view_own`, `profile:update_own`
- `member:apply`
- `foundation:participate` (if enrolled)
- `leaderboard:view_public`
- `feedback:submit`

**Assignment**: Automatic on user registration (default role)

---

## Role Hierarchy (for Spring Security)

```
SUPER_ADMIN > CORE_MEMBER > FACULTY > DOMAIN_LEAD > STUDENT
```

**Note**: This hierarchy is for **global permission inheritance only**. DOMAIN_LEAD permissions are **domain-scoped** and do not inherit global permissions from FACULTY/CORE_MEMBER. The hierarchy ensures SUPER_ADMIN can do everything CORE_MEMBER can, etc.

---

## Permission Model

Permissions follow the format: `{resource}:{action}` or `{scope}:{resource}:{action}`

### Global Permissions
- `user:manage` - Create/update/deactivate users
- `user:role:assign` - Assign roles to users
- `domain:create` - Create new domains
- `domain:lead:assign` - Assign domain leads
- `member:application:review` - Review member applications
- `event:create`, `event:update`, `event:delete`, `event:publish`, `event:cancel`
- `registration:override` - Override registration limits/waitlist
- `registration:view_all` - View all registrations
- `foundation:coordinate` - Manage Foundation Program
- `foundation:manage_mentors` - Assign mentors
- `leaderboard:configure` - Configure scoring rules
- `leaderboard:adjust_scores` - Manual score adjustments
- `analytics:view_club_reports` - Club-level reports
- `analytics:view_reports` - General reports
- `event:approve_official` - Approve official events

### Domain-Scoped Permissions (DOMAIN_LEAD)
All prefixed with `domain:{domainId}:`
- `event:create`, `event:update`, `event:delete`
- `member:view`, `member:manage`
- `registration:view`, `registration:override`
- `leaderboard:view`
- `analytics:view`
- `settings:update`
- `foundation:assign_mentors`

### Base Permissions (STUDENT)
- `event:view_public`
- `event:register_self`
- `registration:view_own`
- `registration:cancel_own`
- `profile:view_own`
- `profile:update_own`
- `member:apply`
- `foundation:participate`
- `leaderboard:view_public`
- `feedback:submit`

---

## Foundation Program ≠ RBAC Role

**Critical Architectural Decision**: The Foundation Program is a **separate program scope**, not an RBAC role.

### Why?
- A user can be a `STUDENT` AND a Foundation participant simultaneously
- A `DOMAIN_LEAD` can also be a Foundation mentor
- A `CORE_MEMBER` can coordinate the Foundation Program
- Foundation has its own internal roles: `PARTICIPANT`, `MENTOR`, `COORDINATOR`
- These internal roles are managed within the `foundation` module, not the RBAC system

### Implementation Implications
- Foundation enrollment tracked in `foundation.FoundationEnrollment`
- Foundation internal roles stored in `foundation.FoundationRole` (enum: PARTICIPANT, MENTOR, COORDINATOR)
- Authorization for Foundation actions checks `FoundationEnrollment` + `FoundationRole`, NOT Spring Security `@PreAuthorize` with RBAC roles
- Spring Security RBAC roles control **system-level** access; Foundation internal roles control **program-level** access

---

## Authorization Implementation Strategy

### Method-Level Security (Spring Security)
```java
@PreAuthorize("hasRole('SUPER_ADMIN')")
@PreAuthorize("hasRole('CORE_MEMBER')")
@PreAuthorize("hasRole('DOMAIN_LEAD') and @domainSecurityService.hasDomainAccess(#domainId)")
```

### Domain-Scoped Access
- Custom `DomainSecurityService` with `hasDomainAccess(userId, domainId)` 
- Checks `DomainLeadAssignment` with valid date range
- Used in `@PreAuthorize` SpEL expressions

### Program-Scoped Access (Foundation)
- Custom `FoundationSecurityService` with `hasRole(enrollmentId, FoundationRole)`
- Checks `FoundationEnrollment` + `FoundationMentorship` for mentor role
- **Not** expressed via Spring Security annotations on controllers
- Implemented as service-layer checks for finer control

### Permission Evaluation Order
1. Global role check (Spring Security)
2. Domain scope check (if DOMAIN_LEAD)
3. Program scope check (if Foundation action)
4. Resource ownership check (user owns the resource)

---

## Role Assignment Workflow

1. **SUPER_ADMIN**: Created via database seed / Flyway migration
2. **CORE_MEMBER**: Assigned by SUPER_ADMIN via admin UI or API
3. **FACULTY**: Assigned by SUPER_ADMIN or CORE_MEMBER
4. **DOMAIN_LEAD**: Assigned via `domain` module API (`DomainLeadService.assignLead()`)
   - Creates `DomainLeadAssignment` with `domainId`, `userId`, `startDate`, `endDate`
   - `user` module adds `DOMAIN_LEAD` role to `UserRole`
   - Automatic revocation via scheduled job checking `endDate`
5. **STUDENT**: Default on user registration

---

## Audit Requirements

All role assignments and revocations must be audited:
- `RoleAssignmentAudit` entity: `userId`, `role`, `assignedBy`, `assignedAt`, `revokedAt`, `reason`
- `DomainLeadAssignmentAudit` entity: `domainId`, `userId`, `assignedBy`, `startDate`, `endDate`, `revokedAt`
- Immutable audit log (append-only)