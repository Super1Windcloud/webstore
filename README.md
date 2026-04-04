# WebStore

[![Java](https://img.shields.io/badge/Java-17-000?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
[![Spotless](https://img.shields.io/badge/code%20style-Spotless-1f6feb?style=flat-square)](https://github.com/diffplug/spotless)
[![SpotBugs](https://img.shields.io/badge/static%20analysis-SpotBugs-f28c28?style=flat-square)](https://spotbugs.github.io/)

An open-source web app store for self-hosted services.

`WebStore` is a Spring Boot based project intended to provide a simple, browser-first experience for discovering, organizing, and launching self-hosted web applications from a single place.

The long-term goal is similar to products such as Runtipi: make app deployment and daily management easier for individuals, homelabs, and small teams. The difference is that this repository is currently at an early stage and is being built in public.

## Preview

The project is currently in bootstrap status.

What exists today:

- Spring Boot 4 application skeleton
- Thymeleaf + Spring MVC foundation
- Maven wrapper for reproducible builds
- `spotless` formatting checks
- `spotbugs` static analysis checks

What is planned next:

- App catalog pages
- App detail pages
- Install and update workflows
- Runtime status dashboard
- App metadata management
- Search, tags, categories, and recommendations

## Why WebStore

Managing self-hosted apps often means dealing with scattered compose files, manual reverse proxy setup, inconsistent environment variables, and weak visibility into what is actually running.

WebStore aims to provide:

- A clean app store style UI for browsing available apps
- A unified metadata model for app definitions
- A simple admin workflow for install, upgrade, remove, and inspect
- A backend foundation that can later integrate with Docker or other runtime engines
- A codebase that is small enough to understand and extend

## Tech Stack

- Java 17
- Spring Boot 4
- Spring MVC
- Thymeleaf
- Maven
- Spotless
- SpotBugs

## Getting Started

### Requirements

- Java 17+
- Maven 3.9+ or the included Maven Wrapper

### Run locally

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

By default the app starts on:

```text
http://localhost:8080
```

### Run with Docker Compose

The repository now includes a minimal platform stack for local container startup:

- `webstore` Spring Boot application
- `postgres` for persistent platform data
- `traefik` as the reverse proxy

Start only the non-Spring backend services and run `webstore` locally:

```bash
docker compose up -d
```

On Windows:

```powershell
docker compose up -d
```

Then start the Spring Boot app locally with PostgreSQL from Docker:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/webstore"
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"
$env:SPRING_DATASOURCE_USERNAME="webstore"
$env:SPRING_DATASOURCE_PASSWORD="webstore"
$env:SPRING_H2_CONSOLE_ENABLED="false"
$env:WEBSTORE_SECURITY_JWT_SECRET="change-this-jwt-secret-before-production-use"
.\mvnw.cmd spring-boot:run
```

In this mode:

- `postgres` runs in Docker and is exposed on `localhost:5432`
- `traefik` runs in Docker and forwards `http://localhost:8088` to your local Spring Boot app on port `8080`
- `webstore` itself does not need to run in Docker

Start the full containerized stack when needed:

```bash
docker compose --profile app up -d --build
```

On Windows:

```powershell
docker compose --profile app up -d --build
```

Then open:

```text
http://localhost:8088
```

Traefik dashboard:

```text
http://localhost:8081
```

Notes:

- `postgres` and `traefik` start by default with `docker compose up -d`.
- `webstore` is under the `app` profile.
- The Traefik local-development route points to `http://host.docker.internal:8080`.
- Traefik is mapped to host port `8088` by default to avoid conflicts on port `80`.
- The default PostgreSQL and JWT credentials in `docker-compose.yml` are for local development only.
- Local PostgreSQL data is stored in `./docker-data/postgres`.

## Quality Checks

Run formatting checks:

```bash
./mvnw spotless:check
```

Apply formatting automatically:

```bash
./mvnw spotless:apply
```

Run static analysis:

```bash
./mvnw spotbugs:check
```

Run the full verification pipeline:

```bash
./mvnw verify
```

On Windows, replace `./mvnw` with `.\mvnw.cmd`.

## Project Structure

```text
src/
  main/
    java/
      org/superwindcloud/webstore/
    resources/
  test/
    java/
      org/superwindcloud/webstore/
pom.xml
README.md
```

## Roadmap

- Build the first storefront homepage
- Add application domain models and repository structure
- Design app manifest format
- Implement catalog, search, and detail pages
- Introduce installation orchestration
- Add authentication and admin permissions
- Add container runtime integration
- Add CI pipeline and release process

## Contributing

Contributions are welcome, especially in these areas:

- Product design for the app store experience
- Backend architecture and domain modeling
- Frontend templates and UI components
- App manifest and packaging design
- Deployment workflow integration
- Testing and documentation

Suggested workflow:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run formatting and checks
5. Open a pull request

## Current Status

This repository is not yet a production-ready app store.

If you are looking for a mature self-hosted app marketplace today, projects such as Runtipi are more complete. If you want to help shape a Java/Spring based alternative from the beginning, this project is intended for that path.

## Inspiration

This project is inspired by the open-source self-hosting ecosystem, especially the product direction of Runtipi:

- Runtipi repository: https://github.com/runtipi/runtipi
- Runtipi app store: https://github.com/runtipi/runtipi-appstore

## License

Add a license file before public release. Until then, all rights remain reserved by the repository owner unless stated otherwise.
