# Smart Issue Tracker

Smart Issue Tracking & Ticket Management System built with Spring Boot, Spring Security, and Spring Data JPA.

---

## Features

- **JWT Authentication**: Secure endpoints with stateless JWT Access and Refresh Tokens.
- **Role-Based Access Control**: Different roles (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_USER`) to regulate operations.
- **Ticket Management**: Create, assign, categorize, and prioritize issues with support for status updates.
- **Interactive Documentation**: Beautiful interactive API docs with Swagger UI.
- **Flexible Profiles**: Run with local PostgreSQL or light in-memory H2 database for testing and demonstration.

---

## Prerequisites

- **Java Development Kit (JDK)**: version 17 or higher.
- **Maven**: Bundled inside the project root (`./maven/apache-maven-3.9.6`).

---

## Getting Started

### Run the Application (In-Memory Database)
To run the application locally without requiring a PostgreSQL database instance, use the in-memory H2 profile:

#### On Windows (PowerShell)
```powershell
$env:SPRING_PROFILES_ACTIVE="dev"; .\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

#### On Linux / macOS
```bash
SPRING_PROFILES_ACTIVE=dev ./maven/apache-maven-3.9.6/bin/mvn spring-boot:run
```

Once started, the application will be hosted at `http://localhost:8080/api/v1`.

### Interactive API Documentation (Swagger UI)
To explore, test, and interact with the endpoints:
- **Swagger URL**: [http://localhost:8080/api/v1/swagger-ui/index.html](http://localhost:8080/api/v1/swagger-ui/index.html)

---

## Seed Accounts & Testing
The `dev` and `test` environments are pre-seeded with the following roles and accounts. All accounts share the password **`password`**:

- **System Administrator** (`ROLE_ADMIN`):
  - Username: `admin`
  - Email: `admin@tracker.com`
- **Project Manager** (`ROLE_MANAGER`):
  - Username: `manager`
  - Email: `manager@tracker.com`
- **Software Engineer** (`ROLE_USER`):
  - Username: `developer`
  - Email: `developer@tracker.com`

To make authenticated requests:
1. Hit the `/api/v1/auth/login` endpoint with one of the pre-seeded users.
2. Copy the `accessToken` from the response.
3. Click the **Authorize** lock button in Swagger UI, paste the token, and authorize.

---

## Running Automated Tests
To execute the unit and integration test suite:

```powershell
.\maven\apache-maven-3.9.6\bin\mvn.cmd test
```
