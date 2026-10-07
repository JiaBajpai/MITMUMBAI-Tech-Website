# MIT TECH KERNEL Backend - Architecture Overview

## High-Level Architecture

```mermaid
graph TD
    subgraph "API Layer"
        CTRL[Controllers<br/>HTTP, Validation, Serialization]
        SEC[Security Filter Chain<br/>Authentication, Authorization]
    end

    subgraph "Module / Application Layer"
        AUTH[auth<br/>Identity, Login, Token Management]
        USER[user<br/>User Profile, Account Management]
        MEMBER[member<br/>Membership, Member Details]
        DOMAIN[domain<br/>Technical Domains, Metadata]
        EVENT[event<br/>Event Definitions, Configuration]
        REG[registration<br/>Event Registration, Workflows]
        FOUNDATION[foundation<br/>Foundation Program Logic]
        LEADERBOARD[leaderboard<br/>Rankings, Scoring, Read Models]
    end

    subgraph "Shared Infrastructure"
        CONFIG[config/<br/>Spring Configuration]
        COMMON[common/<br/>exception, response, util]
        SECURITY_CFG[security/<br/>Security Configuration]
    end

    subgraph "Persistence / Infrastructure Layer"
        JPA[Spring Data JPA / Hibernate]
        PG[(PostgreSQL)]
        FLYWAY[Flyway Migrations]
    end

    CTRL --> AUTH
    CTRL --> USER
    CTRL --> MEMBER
    CTRL --> DOMAIN
    CTRL --> EVENT
    CTRL --> REG
    CTRL --> FOUNDATION
    CTRL --> LEADERBOARD

    SEC --> AUTH
    SEC --> USER

    AUTH --> COMMON
    USER --> COMMON
    MEMBER --> COMMON
    DOMAIN --> COMMON
    EVENT --> COMMON
    REG --> COMMON
    FOUNDATION --> COMMON
    LEADERBOARD --> COMMON

    AUTH --> JPA
    USER --> JPA
    MEMBER --> JPA
    DOMAIN --> JPA
    EVENT --> JPA
    REG --> JPA
    FOUNDATION --> JPA
    LEADERBOARD --> JPA

    JPA --> PG
    FLYWAY --> PG

    CONFIG --> JPA
    CONFIG --> SECURITY_CFG
```

## Architectural Principles

1. **Modular Monolith**: Each business capability is isolated in its own module under `modules/`
2. **Layered Architecture**: Controllers → Services → Repositories → Entities
3. **Shared Infrastructure**: `config/`, `security/`, `common/` provide cross-cutting concerns
4. **Dependency Direction**: Modules depend on `common/`, never on each other directly
5. **Database per Module**: Each module owns its tables; cross-module queries via read models or explicit integration

## Package Structure

```
com.mittechkernel.backend
├── TechKernelApplication.java
├── config/                    # Spring @Configuration classes
├── security/                  # Security configuration, filters, handlers
├── common/
│   ├── exception/             # Global exception handling, base exceptions
│   ├── response/              # Standard API response wrappers
│   └── util/                  # Generic utilities (no business logic)
└── modules/
    ├── auth/                  # Authentication & session management
    ├── user/                  # User identity & account management
    ├── member/                # Membership & member-specific data
    ├── domain/                # Technical domains & metadata
    ├── event/                 # Event definitions & configuration
    ├── registration/          # Registration workflows & state
    ├── foundation/            # Foundation Program (separate scope)
    └── leaderboard/           # Rankings, scoring, read models
```

## Technology Stack

- **Java**: 25 (LTS)
- **Spring Boot**: 4.1.1
- **Build**: Maven
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security
- **API**: Spring Web MVC
- **Validation**: Jakarta Bean Validation
- **Monitoring**: Spring Boot Actuator + Micrometer
- **Testing**: JUnit 5 + Spring Boot Test
- **Containerization**: Docker / Docker Compose