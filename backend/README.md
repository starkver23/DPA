# CodeClassroom Backend Foundation

This is the Spring Boot backend foundation for **CodeClassroom**, an MSc dissertation project designed to bridge the gap between JDL-based database modeling and structured Java code generation.

## Technology Stack
- **Java:** Version 21 (Supports up to Java 25+)
- **Spring Boot:** 3.5.0
- **Build System:** Maven 3.9+
- **Database:** In-memory H2 Database
- **Persistence:** Spring Data JPA + Hibernate
- **Utilities:** Lombok & Spring Validation

---

## REST Endpoints
All API requests are exposed under the `/api` routing prefix:

* **GET `/api/health`:** Checks health status of the backend application.
  - **Success Response (200 OK):**
    ```json
    {
        "status": "UP",
        "application": "CodeClassroom Backend"
    }
    ```

---

## Package Architecture

```text
uk.ac.bham.codeclassroom
├── CodeClassroomApplication.java  # Main Application Entry Point
├── config                         # Global CORS, Security, and App configs
├── controller                     # Rest Controllers (e.g., HealthController)
├── service                        # Business Service Interfaces & Implementations
├── repository                     # Spring Data JPA Repository Interfaces
├── model                          # JPA Entities and Database Models
├── exception                      # Custom Exceptions and Global Handlers
├── generator                      # Custom Generation Engine packages
│   ├── parser                     # JDL / DSL Parsers
│   ├── validation                 # Semantic check & AST validator
│   ├── engine                     # Code & Boilerplate rendering core
│   ├── archive                    # ZIP rendering and archiving helpers
│   └── ast                        # Abstract Syntax Tree structures
└── util                           # Cross-cutting utility classes
```

---

## TODO: Maven Wrapper Setup

> **Note:** The current development container environment does not have a local Maven (`mvn`) distribution globally installed.

Please generate the Maven Wrapper files locally on your machine where `mvn` is installed, or within an IDE (e.g., IntelliJ, VS Code) by running:

```bash
# In the backend/ directory, run:
mvn wrapper:wrapper
```

This will automatically re-create the standard Maven Wrapper files (`mvnw`, `mvnw.cmd`, `.mvn/`) with your local Maven distribution's version.

---

## Local Development & Startup

Once the Maven wrapper or a local Maven installation is configured, run the following:

### Running the Application
```bash
mvn spring-boot:run
```

The application will start on **`http://localhost:8080`**.

### Database Console
An in-memory H2 database is configured. You can access the console UI at:
- **URL:** `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:mem:codeclassroom`
- **Username:** `sa`
- **Password:** *(empty)*
