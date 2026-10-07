# MIT TECH KERNEL Backend - Testing Conventions

## Test Pyramid

```
        /\
       /  \     E2E / Contract Tests (Few)
      /----\
     /      \   Integration Tests (Some)
    /--------\
   /          \  Unit Tests (Many)
  /____________\
```

### Distribution Target

| Test Type | Target % | Speed | Scope |
|-----------|----------|-------|-------|
| Unit | 70% | < 100ms | Single class, mocked dependencies |
| Integration | 20% | < 5s | Module + DB + Spring context |
| Contract/E2E | 10% | < 30s | Full HTTP stack, multiple modules |

---

## Unit Tests

### Location
```
src/test/java/com/mittechkernel/backend/modules/<module>/
```

### Naming
- Test class: `<ClassUnderTest>Test` (e.g., `UserServiceTest`)
- Test method: `should<ExpectedBehavior>When<Condition>()` 
  - e.g., `shouldReturnUserWhenEmailExists()`
  - e.g., `shouldThrowExceptionWhenEmailAlreadyRegistered()`

### Structure (AAA Pattern)

```java
@Test
void shouldCreateUserWhenValidRequest() {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(...);
    User savedUser = User.builder().id(1L).email("test@example.com").build();
    when(userRepository.save(any())).thenReturn(savedUser);
    when(userMapper.toEntity(request)).thenReturn(savedUser);
    when(userMapper.toResponse(savedUser)).thenReturn(expectedResponse);

    // Act
    UserResponse response = userService.createUser(request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.email()).isEqualTo("test@example.com");
    verify(userRepository).save(any());
}
```

### Rules
- Test ONE public method per test method
- Mock ALL external dependencies (repositories, other services, external APIs)
- Use `@ExtendWith(MockitoExtension.class)`
- Use `@Mock` for dependencies, `@InjectMocks` for class under test
- Prefer `assertThat()` from AssertJ
- No Spring context loading (`@SpringBootTest` forbidden)
- No database, no network, no file I/O
- Target: < 100ms per test

### What to Test
- Business logic in services
- Validation logic
- Mapping logic (mapper tests)
- Utility functions
- Exception translation
- State transitions

### What NOT to Test
- Getters/setters/records
- JPA repository methods (Spring Data handles)
- Spring framework code
- Configuration classes

---

## Service Tests (Integration Tests - Module Level)

### Location
```
src/test/java/com/mittechkernel/backend/modules/<module>/service/
```

### Naming
- Test class: `<ServiceName>IntegrationTest`

### Annotations
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("tech_kernel_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configure(DataSourceProperties properties) {
        properties.setUrl(postgres.getJdbcUrl());
        properties.setUsername(postgres.getUsername());
        properties.setPassword(postgres.getPassword());
    }
}
```

### Rules
- Real Spring context (but test profile)
- Real database (Testcontainers PostgreSQL)
- Flyway migrations run automatically
- `@Transactional` + rollback after each test
- Test the full service layer with real repositories
- Mock ONLY external systems (email, SMS, external APIs)
- Target: < 5s per test class

### What to Test
- Repository queries (custom @Query methods)
- Entity relationships and cascading
- Transaction boundary behavior
- Database constraints (unique, FK, check)
- Service orchestration with real dependencies
- Flyway migration compatibility

---

## Repository Tests

### Location
```
src/test/java/com/mittechkernel/backend/modules/<module>/repository/
```

### Naming
- Test class: `<EntityName>RepositoryTest`

### Annotations
```java
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = ...;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository repository;
}
```

### Rules
- `@DataJpaTest` loads only JPA configuration
- Testcontainers for real PostgreSQL
- Test custom query methods, not Spring Data built-ins
- Use `TestEntityManager` for test data setup
- Target: < 2s per test class

---

## Controller Tests (API Tests)

### Location
```
src/test/java/com/mittechkernel/backend/modules/<module>/controller/
```

### Naming
- Test class: `<ControllerName>ControllerTest`

### Annotations
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @WithMockUser(roles = "SUPER_ADMIN")
    @Test
    void shouldCreateUserWhenValidRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest(...);
        
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }
}
```

### Rules
- Use `MockMvc` for full HTTP stack testing
- `@WithMockUser` or `@WithUserDetails` for authentication
- Test request/response serialization
- Test validation error responses
- Test security (401, 403)
- Test pagination, sorting, filtering
- Mock external services, use real repositories
- Target: < 3s per test class

---

## Security Tests

### Location
```
src/test/java/com/mittechkernel/backend/security/
```

### What to Test
- Authentication filter chain
- JWT token validation
- Role-based access (`@PreAuthorize`)
- Domain-scoped access (`DOMAIN_LEAD` permissions)
- CSRF protection (if enabled)
- CORS configuration
- Password encoding

### Example
```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenInsufficientRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                .with(user("student").roles("STUDENT")))
            .andExpect(status().isForbidden());
    }
}
```

---

## Test Data Management

### Test Fixtures / Builders

```java
// In test-support module or module's test package
public class UserTestData {
    
    public static User userWithId(Long id) {
        return User.builder()
            .id(id)
            .email("user" + id + "@example.com")
            .fullName("Test User " + id)
            .studentId("2021" + String.format("%06d", id))
            .roles(Set.of(Role.STUDENT))
            .active(true)
            .build();
    }
    
    public static CreateUserRequest validCreateRequest() {
        return new CreateUserRequest(
            "new@example.com",
            "New User",
            "2021000001",
            Role.STUDENT
        );
    }
}
```

### Testcontainers Configuration

Shared in `src/test/java/com/mittechkernel/backend/TestcontainersConfiguration.java`:

```java
@TestConfiguration
public class TestcontainersConfiguration {
    
    @Bean
    @ServiceConnection
    static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tech_kernel_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Reuse across test classes
    }
}
```

---

## Test Profiles

### `application-test.yml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    enabled: true
  sql:
    init:
      mode: never

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.springframework.test: DEBUG
```

---

## Running Tests

### All Tests
```bash
mvn test
```

### Unit Tests Only (Fast)
```bash
mvn test -Dtest="*Test" -DfailIfNoTests=false
```

### Integration Tests Only
```bash
mvn test -Dtest="*IntegrationTest,*RepositoryTest,*ControllerTest"
```

### Specific Module
```bash
mvn test -pl :tech-kernel-backend -Dtest="com.mittechkernel.backend.modules.user.*"
```

### With Coverage
```bash
mvn verify jacoco:report
```

---

## Test Naming Summary

| Test Type | Class Name | Method Name |
|-----------|------------|-------------|
| Unit | `<Class>Test` | `should<Behavior>When<Condition>()` |
| Integration (Service) | `<Service>IntegrationTest` | `should<Behavior>When<Condition>()` |
| Repository | `<Entity>RepositoryTest` | `should<Behavior>When<Condition>()` |
| Controller | `<Controller>ControllerTest` | `should<Behavior>When<Condition>()` |
| Security | `<Feature>SecurityTest` | `should<Behavior>When<Condition>()` |
| Architecture | `ArchitectureTests` | `modulesDoNotDependOnEachOther()` |

---

## Minimal Example Structure

```
src/test/java/com/mittechkernel/backend/
├── common/
│   └── util/
│       └── JwtUtilTest.java                 # Unit test
├── modules/
│   ├── user/
│   │   ├── service/
│   │   │   ├── UserServiceTest.java         # Unit test
│   │   │   └── UserServiceIntegrationTest.java
│   │   ├── repository/
│   │   │   └── UserRepositoryTest.java
│   │   └── controller/
│   │       └── UserControllerTest.java
│   ├── auth/
│   │   └── service/
│   │       ├── TokenServiceTest.java
│   │       └── AuthenticationServiceIntegrationTest.java
│   └── ... (other modules)
├── security/
│   └── SecurityConfigurationTest.java
└── ArchitectureTests.java                    # ArchUnit tests
```

---

## CI/CD Integration

GitHub Actions / GitLab CI should run:
1. `mvn test` (unit + integration)
2. `mvn verify` (with coverage)
3. ArchUnit tests (fail build on violations)
4. Coverage threshold check (e.g., 70% line coverage on services)

---

## Test Dependencies

```xml
<!-- In pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <scope>test</scope>
</dependency>
```