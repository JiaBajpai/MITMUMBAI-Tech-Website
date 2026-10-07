# MIT TECH KERNEL Backend - Dependency Rules

## Core Principle

**Modules are independent deployable units.** They communicate only through well-defined contracts. Direct dependencies between business modules are forbidden.

---

## Allowed Dependencies

### 1. Business Modules → Shared Infrastructure (Always Allowed)

| Module | Can Depend On |
|--------|---------------|
| All modules | `common.exception` |
| All modules | `common.response` |
| All modules | `common.util` |
| All modules | `config` (Spring config only) |
| All modules | `security` (Spring Security config only) |

**Rule**: Business modules may use shared infrastructure freely. Shared infrastructure MUST NOT depend on business modules.

---

### 2. Business Modules → Other Business Modules (Read-Only, Query Services Only)

A module may **read** data from another module ONLY through a **query service interface** defined by the target module.

| Consumer Module | Can Query (Read-Only) | Via |
|-----------------|----------------------|-----|
| `member` | `user` | `UserQueryService` |
| `domain` | `user` | `UserQueryService` |
| `event` | `domain` | `DomainQueryService` |
| `registration` | `event` | `EventQueryService` |
| `registration` | `user` | `UserQueryService` |
| `foundation` | `user` | `UserQueryService` |
| `foundation` | `event` | `EventQueryService` (optional) |
| `leaderboard` | `user` | `UserQueryService` |
| `leaderboard` | `event` | `EventQueryService` |
| `leaderboard` | `registration` | `RegistrationQueryService` |
| `leaderboard` | `foundation` | `FoundationQueryService` |
| `leaderboard` | `domain` | `DomainQueryService` |

**Rules**:
- ONLY query services (`*QueryService`) may be used
- NEVER command services (`*Service` with mutating operations)
- NEVER inject another module's repository directly
- NEVER inject another module's entity directly
- DTOs returned by query services are the contract

---

### 3. Shared Infrastructure → Business Modules (NEVER)

| Package | Must NOT Depend On |
|---------|-------------------|
| `common.exception` | Any business module |
| `common.response` | Any business module |
| `common.util` | Any business module |
| `config` | Any business module |
| `security` | Any business module |

**Rule**: Shared infrastructure is generic. If it needs business logic, that logic belongs in a business module.

---

## Forbidden Dependencies

| Pattern | Reason |
|---------|--------|
| `moduleA` → `moduleB` (direct) | Creates tight coupling, prevents independent evolution |
| `moduleA` → `moduleB` repository | Leaks persistence details |
| `moduleA` → `moduleB` entity | Leaks persistence model |
| `moduleA` → `moduleB` command service | Violates module autonomy |
| `common/*` → `modules/*` | Makes shared code business-specific |
| Circular: `moduleA` ↔ `moduleB` | Impossible to build/test independently |
| `security` → `modules/*` (except `auth`, `user` via interfaces) | Security config should be generic |

---

## Cross-Module Communication Patterns

### Pattern 1: Query Service (Preferred for Reads)

```java
// In domain module - defines the contract
public interface DomainQueryService {
    DomainResponse findById(Long domainId);
    List<DomainResponse> findByLeadId(Long userId);
}

// In domain module - implements the contract
@Service
@RequiredArgsConstructor
public class DomainQueryServiceImpl implements DomainQueryService {
    private final DomainRepository repository;
    private final DomainMapper mapper;
    // implementation
}

// In event module - consumes via interface
@Service
@RequiredArgsConstructor
public class EventService {
    private final DomainQueryService domainQueryService; // interface only
    
    public EventResponse createEvent(CreateEventRequest request) {
        DomainResponse domain = domainQueryService.findById(request.domainId());
        // validate domain exists, user is lead, etc.
    }
}
```

### Pattern 2: Domain Events (Future - for Async)

```java
// Publish from source module
@Component
@RequiredArgsConstructor
public class EventPublisher {
    private final ApplicationEventPublisher publisher;
    
    public void publishRegistrationConfirmed(RegistrationConfirmedEvent event) {
        publisher.publishEvent(event);
    }
}

// Consume in target module
@Component
@RequiredArgsConstructor
public class RegistrationEventListener {
    private final LeaderboardService leaderboardService;
    
    @EventListener
    public void handle(RegistrationConfirmedEvent event) {
        leaderboardService.recordAttendance(event.registrationId());
    }
}
```

### Pattern 3: Explicit Integration Service (For Complex Workflows)

Create a dedicated integration service in the **consuming** module that orchestrates calls to multiple query services.

```java
// In registration module
@Service
@RequiredArgsConstructor
public class RegistrationIntegrationService {
    private final EventQueryService eventQueryService;
    private final UserQueryService userQueryService;
    
    public RegistrationValidationResult validateRegistration(RegistrationRequest request) {
        EventResponse event = eventQueryService.findById(request.eventId());
        UserResponse user = userQueryService.findById(request.userId());
        // cross-module validation logic
    }
}
```

---

## Shared DTOs / Contracts

### Where to Put Shared DTOs

**Option 1: In the owning module's `dto` or `contract` package** (Preferred)
```
modules/user/
├── dto/
│   ├── UserResponse.java
│   └── UserProfileResponse.java
└── contract/
    └── UserQueryService.java
```

**Option 2: In `common.response` for truly generic wrappers**
```
common/response/
├── ApiResponse.java
├── PageResponse.java
└── TokenResponse.java
```

**Option 3: Dedicated `contracts` module (if many consumers)**
```
contracts/
├── user/
│   ├── UserResponse.java
│   └── UserQueryService.java
├── event/
│   ├── EventResponse.java
│   └── EventQueryService.java
```

**Decision**: Start with **Option 1** (in owning module). Extract to Option 3 only when multiple modules need the same contract and versioning becomes an issue.

### Contract Rules
- Contract DTOs are **immutable** (records or final classes with getters only)
- Contract DTOs **never** expose JPA entities
- Contract DTOs **never** contain business logic
- Contract interfaces (`*QueryService`) are the **only** allowed dependency entry point

---

## Enforcing Dependency Rules

### 1. ArchUnit Tests (Mandatory)

Add ArchUnit tests to verify architecture at build time:

```java
@AnalyzeClasses(packages = "com.mittechkernel.backend")
class ArchitectureTests {

    @ArchTest
    static final ArchRule modules_do_not_depend_on_each_other =
        noClasses()
            .that().resideInAPackage("..modules..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..modules.auth..", "..modules.user..", "..modules.member..", 
                               "..modules.domain..", "..modules.session..", "..modules.registration..",
                               "..modules.foundation..", "..modules.leaderboard..")
            .because("Business modules must not depend on each other directly. " +
                     "Use query service interfaces for cross-module reads.");

    @ArchTest
    static final ArchRule common_does_not_depend_on_modules =
        noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat()
            .resideInAPackage("..modules..")
            .because("Shared infrastructure must not depend on business modules.");

    @ArchTest
    static final ArchRule modules_only_use_query_services =
        noClasses()
            .that().resideInAPackage("..modules..")
            .should().dependOnClassesThat()
            .resideInAPackage("..modules..Repository")
            .because("Modules must not access other modules' repositories directly.");

    @ArchTest
    static final ArchRule controllers_only_in_api_layer =
        classes()
            .that().resideInAPackage("..modules..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..modules..")
            .because("Controllers must stay in their module.");
}
```

### 2. Maven Enforcer Plugin (Optional)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>enforce-architecture</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <bannedDependencies>
                        <excludes>
                            <exclude>com.mittechkernel.backend:tech-kernel-backend:modules:*</exclude>
                        </excludes>
                    </bannedDependencies>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3. Package-Private / Module-Private (Future: Java Modules)

When migrating to Java Modules (`module-info.java`):
- Export only `*QueryService` interfaces and contract DTOs
- Keep `*Service` (command), `*Repository`, `*Entity` package-private or non-exported

---

## Dependency Direction Summary

```
                    ┌─────────────────┐
                    │   Controllers   │  (HTTP layer - one per module)
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  *Service       │  (Command/Orchestration - module private)
                    │  *QueryService  │  (Read contract - EXPORTED)
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
       │ *Repository │ │ *Repository │ │ *Repository │
       │  (Module A) │ │  (Module B) │ │  (Module C) │
       └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
              │               │               │
              ▼               ▼               ▼
       ┌─────────────────────────────────────────┐
       │           Persistence (JPA/Hibernate)   │
       └─────────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL    │
                    └─────────────────┘

Shared Infrastructure (common/, config/, security/) → Used by ALL modules
                                                          ↑
                                                   NO reverse deps
```

---

## Adding a New Module

1. Create package under `modules/newmodule/`
2. Define `NewModuleQueryService` interface (contract)
3. Implement `NewModuleQueryServiceImpl` (internal)
4. Define contract DTOs in `dto/` or `contract/`
5. Add ArchUnit test to verify no forbidden dependencies
6. Document in `modules.md`
7. Other modules depend ONLY on `NewModuleQueryService` interface