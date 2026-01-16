# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.9 application using Java 21, configured with Maven. The project uses:
- **Spring Data JPA** for database access with PostgreSQL
- **Spring Web** for REST API endpoints
- **Lombok** for reducing boilerplate code
- **JUnit 5** for testing

Package structure: `com.example.zadanie`

## Build and Development Commands

### Building and Running
```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Package as JAR
./mvnw package
```

### Testing
```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ZadanieApplicationTests

# Run a specific test method
./mvnw test -Dtest=ZadanieApplicationTests#contextLoads
```

### Code Quality
```bash
# Compile and check for compilation errors
./mvnw compile

# Clean and compile
./mvnw clean compile
```

## Architecture and Structure

### Maven Configuration
- Java version: 21
- Spring Boot: 3.5.9
- Lombok annotation processing is configured in the Maven compiler plugin
- Lombok is excluded from the final JAR build

### Database Configuration
- PostgreSQL is configured as the runtime database
- Connection details should be specified in `src/main/resources/application.yaml`
- Currently only the application name is configured; database connection properties will need to be added when implementing data access features

### Application Entry Point
Main class: `com.example.zadanie.ZadanieApplication`

## PRP-Based Development Workflow

This project uses a PRP (Project Requirements & Planning) workflow for feature development via custom Claude Code skills:

1. **enhance-initial**: Enhance the `INITIAL.md` template with codebase-specific context
2. **generate-prp**: Research and create a comprehensive PRP in `PRPs/{feature-name}.md`
3. **execute-prp**: Implement the feature following the PRP with validation gates

### PRP Workflow Steps
1. Define feature requirements in `INITIAL.md`
2. Run `/enhance-initial` to add codebase context
3. Run `/generate-prp INITIAL.md` to create detailed implementation plan with research
4. Run `/execute-prp PRPs/{feature-name}.md` to implement and validate

### PRP Guidelines
- PRPs must include executable validation gates (compile, test commands)
- Include URLs to documentation and examples for AI agent research
- Reference existing code patterns and files from the codebase
- Score each PRP 1-10 for one-pass implementation confidence
