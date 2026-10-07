# MIT TECH KERNEL Backend - Implementation Guidelines

## Engineering Principles

### SOLID Principles

| Principle | Application |
|-----------|-------------|
| **Single Responsibility** | Each class has one reason to change. Services orchestrate; repositories persist; controllers handle HTTP. |
| **Open/Closed** | Extend via new classes, not modification. Use interfaces for extension points. |
| **Liskov Substitution** | Subtypes must be substitutable for their base types. Query service interfaces enable this. |
| **Interface Segregation** | Small, focused interfaces (`UserQueryService`, not `UserService`). Clients depend only on what they use. |
| **Dependency Inversion** | High-level modules (services) depend on abstractions (interfaces), not concretions (repositories). |

### Separation of Concerns

```
Controller (HTTP) → Service (Business) → Repository (Persistence) → Entity (Data)
                    ↑
              Mapper (DTO ↔ Entity)
```

Each layer has a distinct responsibility. No layer skips another.

### Modular Design

- Modules are independently developable, testable, deployable
- Clear module boundaries via query service interfaces
- Shared code in `common/` is generic, not business-specific

### Dependency Inversion

```
Service → QueryService Interface ← Other Module's Implementation
Service → Repository Interface ← JPA Repository Implementation
```

High-level policy (services) depends on abstractions. Details (JPA, external APIs) implement abstractions.

### Constructor Injection

```java
// CORRECT
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final UserQueryService userQueryService; // interface
}

// FORBIDDEN - Field Injection
@Service
public class BadService {
    @Autowired
    private UserRepository repository;
}

// FORBIDDEN - Setter Injection
@Service
public class BadService {
    private UserRepository repository;
    @Autowired
    public void setRepository(UserRepository repository) {
        this.repository = repository;
    }
}
```

### Explicit Boundaries

- Package structure reflects module boundaries
- `module-info.java` (future) enforces encapsulation
- ArchUnit tests verify boundaries at build time

### Minimal Coupling

- Modules communicate via DTOs, not entities
- No shared database tables between modules
- No direct service-to-service calls for commands
- Async via domain events (future)

### High Cohesion

- Related functionality grouped in same module
- Single aggregate root per repository
- Entities and their repositories in same package

### Clear Naming

| Element | Convention |
|---------|------------|
| Packages | lowercase, singular (`user`, not `users`) |
| Classes | PascalCase, noun (`UserService`) |
| Interfaces | PascalCase, noun (`UserQueryService`) |
| Methods | camelCase, verb (`findById`, `createUser`) |
| Fields | camelCase, noun (`userRepository`) |
| Constants | UPPER_SNAKE_CASE |
| Enums | PascalCase, singular (`Role`, `RegistrationStatus`) |
| DTOs | PascalCase + suffix (`UserResponse`, `CreateUserRequest`) |

---

## Layered Responsibilities

### Controller

**Package**: `modules/<module>/controller/`

**Responsibilities**:
- HTTP request/response handling
- Request validation (`@Valid`)
- Path variable / query parameter binding
- Response status codes
- Calling service layer
- No business logic

**Forbidden**:
- Business logic
- Direct repository access
- Entity manipulation
- Transaction management

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserQueryService userQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
    }
}
```

### Service (Command)

**Package**: `modules/<module>/service/`

**Responsibilities**:
- Business logic / orchestration
- Transaction management (`@Transactional`)
- Validation of business rules
- Coordination between repositories
- Publishing domain events
- Calling other modules' **query** services only

**Forbidden**:
- HTTP concerns (request/response objects)
- Direct SQL/JPA queries
- Presentation logic

```java
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final DomainQueryService domainQueryService; // interface only

    public UserResponse createUser(CreateUserRequest request) {
        // Business validation
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateUserException("Email already registered");
        }
        
        // Orchestration
        User user = mapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(Role.STUDENT));
        
        User saved = repository.save(user);
        return mapper.toResponse(saved);
    }
}
```

### Query Service (Read)

**Package**: `modules/<module>/service/` (same as command service, or separate)

**Responsibilities**:
- Read-only data access
- Projections / DTOs for API
- No transaction (or read-only)
- Exposed as interface for other modules

```java
public interface UserQueryService {
    UserResponse findById(Long id);
    UserResponse findByEmail(String email);
    List<UserResponse> findByRole(Role role);
    PageResponse<UserListItemResponse> findAll(Pageable pageable);
}

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository repository;
    private final UserMapper mapper;
    // implementation
}
```

### Repository

**Package**: `modules/<module>/repository/`

**Responsibilities**:
- Data access abstraction
- Custom query methods (`@Query`)
- No business logic
- Returns entities, not DTOs

**Forbidden**:
- Business logic
- HTTP concerns
- DTO mapping
- Calling other repositories

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByStudentId(String studentId);
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.active = true AND u.roles IN :roles")
    List<User> findActiveByRoles(@Param("roles") Set<Role> roles);
}
```

### Entity

**Package**: `modules/<module>/entity/`

**Responsibilities**:
- Persistence model
- JPA annotations
- Encapsulated fields (private + getters/setters or record-like)
- Equals/hashCode based on ID
- No business logic (or minimal: factory methods)

**Forbidden**:
- HTTP/JSON annotations (`@JsonProperty`, etc.)
- Business logic beyond simple validation
- References to services/repositories

```java
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(nullable = false, length = 100)
    private String fullName;
    
    @Column(nullable = false, unique = true, length = 20)
    private String studentId;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.of(Role.STUDENT);
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
    
    // Factory method
    public static User create(String email, String fullName, String studentId, String encodedPassword) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setStudentId(studentId);
        user.setPasswordHash(encodedPassword);
        return user;
    }
}
```

### DTO

**Package**: `modules/<module>/dto/` or `modules/<module>/contract/`

**Responsibilities**:
- API contract (request/response)
- Validation annotations
- Immutable (records preferred)
- No JPA annotations
- No business logic

```java
// Request
public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 2, max = 100) String fullName,
    @NotBlank @Pattern(regexp = "^[0-9]{10}$") String studentId,
    @NotNull Role defaultRole
) {}

// Response
public record UserResponse(
    Long id,
    String email,
    String fullName,
    String studentId,
    Set<Role> roles,
    Boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Mapper

**Package**: `modules/<module>/mapper/`

**Responsibilities**:
- Entity ↔ DTO conversion
- No business logic
- Use MapStruct or manual implementation

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(CreateUserRequest request);
    UserResponse toResponse(User entity);
    UserListItemResponse toListItemResponse(User entity);
    List<UserResponse> toResponseList(List<User> entities);
}
```

### Exception

**Package**: `modules/<module>/exception/` (module-specific) or `common/exception/` (shared)

**Responsibilities**:
- Domain-specific exceptions
- Extend `RuntimeException` (unchecked)
- Carry error code for API response

```java
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(Long id) {
        super("USER_NOT_FOUND", "User with id " + id + " not found", id);
    }
}
```

### Configuration

**Package**: `config/` (shared) or `modules/<module>/config/` (module-specific)

**Responsibilities**:
- Spring `@Configuration` classes
- Bean definitions
- Property binding (`@ConfigurationProperties`)
- No business logic

---

## Configuration Through Environment Variables

All environment-specific configuration via environment variables with sensible defaults in `application.yml`:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5433/tech_kernel}
    username: ${DATABASE_USERNAME:tech_kernel}
    password: ${DATABASE_PASSWORD:changeme}
  jwt:
    secret: ${JWT_SECRET:dev-secret-change-in-production}
    expiration: ${JWT_EXPIRATION:3600}

app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

**Never** hardcode:
- Database passwords
- JWT secrets
- API keys
- Third-party credentials

---

## Avoid

| Anti-Pattern | Why | Alternative |
|--------------|-----|-------------|
| Premature abstractions | YAGNI, adds complexity | Start concrete, extract when needed |
| Unnecessary interfaces | One implementation = no interface needed | Use interface only for cross-module contracts or multiple implementations |
| God classes | Violates SRP, hard to test | Split into focused services |
| God services | Too many responsibilities | Decompose by use case |
| Static global state | Hard to test, hidden dependencies | Dependency injection |
| Field injection | Immutable dependencies impossible, testing hard | Constructor injection |
| Duplicated business logic | Inconsistency, bugs | Extract to shared service/util |
| Leaking entities via API | Couples API to DB schema | Always use DTOs |
| Business logic in controllers | Untestable, violates layering | Move to service |
| Business logic in repositories | Untestable, violates layering | Move to service |

---

## Code Style

### Imports
- Explicit imports (no wildcards)
- Static imports for assertions (`assertThat`, `isEqualTo`)

### Formatting
- Google Java Format (or project default)
- 4-space indentation
- Max line length: 120 chars

### Annotations
- `@RequiredArgsConstructor` on all Spring beans
- `@Transactional` on service methods (read-only for queries)
- `@Valid` on request DTOs in controllers
- `@NonNull` / `@Nullable` for clarity

### Records vs Classes
- Use **records** for DTOs, immutable data carriers
- Use **classes** for entities, services, mutable state

### Null Safety
- Avoid null returns (use `Optional`, empty collections)
- Annotate parameters with `@NonNull` / `@Nullable`
- Fail fast with meaningful exceptions

---

## Git Conventions

### Branch Naming
- `feature/<module>-<description>` - New functionality
- `fix/<module>-<description>` - Bug fixes
- `docs/<description>` - Documentation
- `refactor/<module>-<description>` - Refactoring
- `test/<module>-<description>` - Test improvements

### Commit Messages
```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`

Examples:
```
feat(user): add user profile update endpoint

docs(architecture): add module dependency rules

fix(registration): handle waitlist promotion race condition

refactor(common): extract ApiResponse wrapper
```

---

## Definition of Done

Before merging any implementation:
- [ ] All tests pass (`mvn test`)
- [ ] Code compiles (`mvn compile`)
- [ ] ArchUnit tests pass (no architecture violations)
- [ ] Coverage meets threshold (>70% on services)
- [ ] No hardcoded secrets
- [ ] Documentation updated if API changed
- [ ] Flyway migration added for schema changes
- [ ] Code review approved