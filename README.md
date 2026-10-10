# Smart Issue Tracking & Ticket Management System

[![GitHub Repository](https://img.shields.io/badge/GitHub-Rohit7270%2Fsmart--issue--tracker-blue?logo=github)](https://github.com/Rohit7270/smart-issue-tracker)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6-green.svg)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)

A clean, production-grade Spring Boot 3 educational backend application demonstrating best architectural practices for building enterprise RESTful microservices and monoliths.

---

## Architecture & Design Principles

The application is organized into a **clean layered architecture** with strict separation of concerns, explicit mapping, and constructor-based dependency injection.

### Layered Architecture Flow

```text
Request (HTTP JSON / Multipart)
       │
       ▼
   Controller               <-- REST API Layer (Thin, handles HTTP status & routing)
       │
       ▼
   Request DTO              <-- Input Validation Layer (@Valid, Bean Validation)
       │
       ▼
  FileStorageService        <-- Storage Abstraction (Handles disk I/O, file uploads)
       │
       ▼
     Service                <-- Business Logic & Workflow Rules Layer (@Transactional)
       │
       ▼
     Mapper                 <-- DTO <-> Entity Conversion Layer (Explicit mapping)
       │
       ▼
   Repository               <-- Persistence Layer (Spring Data JPA & Specifications)
       │
       ▼
  JPA / Hibernate           <-- ORM Engine & Entity Lifecycle Management
       │
       ▼
    Database                <-- Relational DB (PostgreSQL / H2)
       │
       ▼
  Response DTO              <-- API Response Envelope (No direct entity leaks)
       │
       ▼
REST Response (200 OK / 201 Created)
```

---

## Detailed Step-by-Step Request Flow Example: Update Issue Status

```text
[ Client Request: PATCH /api/v1/tickets/100/status ]
                         │
                         ▼
        ┌──────────────────────────────────┐
        │        TicketController          │ <-- Receives HTTP Request & UserDetails
        └──────────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │    UpdateTicketStatusRequest     │ <-- Validates request body (@NotNull statusId)
        └──────────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │          TicketService           │ <-- Enforces Business Rules:
        └──────────────────────────────────┘     1. Role permissions check
                         │                       2. State machine workflow validation
                         │                       3. Audit log creation
                         ▼
        ┌──────────────────────────────────┐
        │         StatusRepository         │ <-- Fetches target Status entity from DB
        └──────────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │         TicketRepository         │ <-- Persists updated Ticket entity
        └──────────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │           TicketMapper           │ <-- Maps updated Entity to TicketResponse DTO
        └──────────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────┐
        │     ResponseEntity<ApiResponse>  │ <-- Returns HTTP 200 OK JSON response
        └──────────────────────────────────┘
```

---

## Domain Model & Package Structure

### Core Domain Entities
- **`Ticket`**: The central issue record (title, description, category, priority, status, reporter, assignee, due date).
- **`User`**: User credentials, full name, account state (active/inactive), and assigned roles.
- **`Role`**: Role-based access control definitions (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER`).
- **`Category`**: Issue classification tags (`Bug`, `Feature Request`, `Task`, `IT Support`).
- **`Priority`**: Severity weights (`Low`, `Medium`, `High`, `Critical`).
- **`Status`**: Workflow state (`Open`, `In Progress`, `Resolved`, `Closed`).
- **`Assignment`**: Historical audit record of ticket delegation and assignment notes.
- **`Comment`**: Collaboration and discussion threads attached to issues.
- **`Attachment`**: File metadata and relative storage paths.
- **`ActivityLog`**: Unified audit trail of system events.

### Package Responsibilities
```text
com.enterprise.issuetracker/
├── controller/       # Thin REST API endpoints exposing HTTP endpoints & Swagger annotations
├── dto/              # API payload transfer objects with Bean Validation annotations (@NotBlank, @NotNull, @Size)
├── entity/           # JPA entity mappings with Hibernate constraints & audit timestamps
├── exception/        # Global exception handler (@RestControllerAdvice) and custom runtime exceptions
├── mapper/           # Explicit, readable mapper classes for Entity <-> DTO conversions
├── repository/       # Spring Data JPA repositories and JPA Criteria Specifications
├── security/         # Spring Security 6, JWT filter chain, BCrypt encoder, UserDetailsService
└── service/          # Concrete @Service classes (@Transactional business logic & FileStorageService)
```

---

## Issue Workflow & State Machine Rules

Tickets follow a defined status state machine:

```text
    ┌────────┐
    │  Open  │ ───► In Progress ───► Resolved ───► Closed
    └────────┘           │               │           │
        ▲                └──────►────────┘           │
        │                                            │
        └────────────────── Reopen ──────────────────┘
```

### Business Rules & Validations:
1. **Initial State**: Newly created tickets automatically default to the `Open` status.
2. **Reopening Closed Tickets**: A ticket in `Closed` status cannot directly jump to `In Progress` or `Resolved`. It must first be reopened to `Open`.
3. **Role Permissions**:
   - **`ROLE_USER` (Reporter)**: Can create tickets, edit open ticket details, add comments, upload attachments.
   - **`ROLE_USER` (Assignee)**: Can update ticket status (e.g. from Open to In Progress or Resolved).
   - **`ROLE_MANAGER`**: Can assign/reassign tickets to any developer, modify properties, update statuses, view management statistics.
   - **`ROLE_ADMIN`**: Complete system governance (user account activation/deactivation, role promotion, category management, deletion).

---

## REST API Documentation

### 1. Authentication & Account Management
| Method | Endpoint | Description | Permitted Roles |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user account | Public |
| `POST` | `/api/v1/auth/login` | Authenticate credentials & return JWT | Public |
| `POST` | `/api/v1/auth/refresh` | Refresh JWT access token | Public |
| `GET` | `/api/v1/admin/users` | List all users (paginated) | `ROLE_ADMIN` |
| `PATCH` | `/api/v1/admin/users/{id}/status` | Activate / Deactivate user login | `ROLE_ADMIN` |
| `PATCH` | `/api/v1/admin/users/{id}/roles` | Update user roles (e.g. promote to Manager) | `ROLE_ADMIN` |

### 2. Issue Lifecycle Management
| Method | Endpoint | Description | Permitted Roles |
|---|---|---|---|
| `POST` | `/api/v1/tickets` | Create a new issue | Authenticated Users |
| `GET` | `/api/v1/tickets` | Search & filter issues (paginated) | Authenticated Users |
| `GET` | `/api/v1/tickets/{id}` | Get ticket details by ID | Authenticated Users |
| `PUT` | `/api/v1/tickets/{id}` | Update ticket properties | Reporter, Manager, Admin |
| `PATCH` | `/api/v1/tickets/{id}/assign` | Assign or reassign ticket | `ROLE_MANAGER`, `ROLE_ADMIN` |
| `PATCH` | `/api/v1/tickets/{id}/status` | Update issue workflow status | Assignee, Manager, Admin |
| `GET` | `/api/v1/tickets/{id}/assignments` | View assignment delegation history | Authenticated Users |
| `DELETE` | `/api/v1/tickets/{id}` | Delete ticket | Reporter, `ROLE_ADMIN` |

### 3. Comments, Attachments & Analytics
| Method | Endpoint | Description | Permitted Roles |
|---|---|---|---|
| `POST` | `/api/v1/tickets/{id}/comments` | Add comment to ticket | Authenticated Users |
| `POST` | `/api/v1/tickets/{id}/attachments` | Upload file attachment | Authenticated Users |
| `GET` | `/api/v1/tickets/attachments/download/{file}` | Download stored file | Authenticated Users |
| `GET` | `/api/v1/dashboard/statistics` | Unified analytics dashboard | `ROLE_MANAGER`, `ROLE_ADMIN` |

---

## Seed Accounts for Quick Testing

All pre-seeded test accounts use the password **`password`**:

- **System Administrator** (`ROLE_ADMIN`): `admin` / `admin@tracker.com`
- **Project Manager** (`ROLE_MANAGER`): `manager` / `manager@tracker.com`
- **Software Engineer** (`ROLE_USER`): `developer` / `developer@tracker.com`

---

## Getting Started & Execution Guide

### Option 1: Run with Docker Compose (Recommended)
Launch the complete Spring Boot application and PostgreSQL container:
```bash
docker compose up --build -d
```
The application will be accessible at `http://localhost:8080/api/v1`.

### Option 2: Run Locally with H2 In-Memory Database (Development Profile)
Run with the `dev` profile:

* **Windows (PowerShell)**:
  ```powershell
  $env:SPRING_PROFILES_ACTIVE="dev"; .\mvnw.cmd spring-boot:run
  ```
* **Linux / macOS**:
  ```bash
  SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
  ```

### Option 3: Run Unit & Integration Tests
To execute the test suite (includes isolated Service Unit Tests with Mockito and Controller Integration Tests with MockMvc):

* **Windows (PowerShell)**:
  ```powershell
  .\mvnw.cmd test
  ```
* **Linux / macOS**:
  ```bash
  ./mvnw test
  ```

---

## Interactive OpenAPI / Swagger Documentation
When the server is running, explore and test the interactive API documentation at:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Specification**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
