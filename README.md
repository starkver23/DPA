# Repository Contents

The main components of the repository are:

- **Frontend** — React and TypeScript implementation of the CodeClassroom visual modelling environment. This component provides the interactive modelling canvas, entity creation, relationship modelling, inheritance modelling, JDL/CDL editing, and application-generation interface.
- **Backend** — Spring Boot backend responsible for receiving visual models from the frontend and coordinating compilation and application generation.
- **Compiler** — Custom compiler pipeline responsible for processing CodeClassroom models through lexical analysis, parsing, semantic validation, inheritance resolution, metadata generation, and extended JDL generation.
- **JHipster Generator** — Modified JHipster generation functionality used to generate applications from the extended model representation and support inheritance-aware application generation.
- **Resources and Configuration** — Supporting templates, configuration files, and other resources required by the compiler and application-generation pipeline.

The repository therefore contains the implementation of the complete workflow from visual modelling through compilation and application generation.

---

# Running CodeClassroom

## Prerequisites

The following software is required to run the project:

- **Node.js** and **npm**
- **Java Development Kit (JDK)** 21
- **Maven**
- **Git**
- **JHipster** and its required dependencies

---

## Running the Frontend

1. Navigate to the frontend directory: cd frontend
2. Install the required dependencies: npm i
3. Start the development server: npm run dev

## The frontend can then be accessed through the local development address displayed by Vite (typically http://localhost:5173).

---

# Running the Backend

1. Navigate to the backend directory: cd backend
2. Start the Spring Boot application using Maven: mvn spring-boot:run

## The backend exposes the CodeClassroom REST API under the /api base path.

---

# Generating an Application

1. Open the visual modelling interface in your browser once both frontend and backend servers are running.
2. Build your software model using the CodeClassroom canvas.
3. Submit the completed model for compilation and application generation.

## The backend will process the model through the CodeClassroom compiler pipeline and invoke the modified JHipster generation process. Once generation completes successfully, the generated Spring Boot application is packaged as a .zip archive and returned to the browser for download.
