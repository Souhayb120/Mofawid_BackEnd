# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Mofawid is the backend of a SaaS for Moroccan *commissaires de justice* (huissiers). It is multi-tenant: each tenant is one office.
The flow is: a **case** (`ref` like `2026/12`) gets **parties**. A **mission** is given to a collaborateur, who logs **delivery attempts**, **observations** and **attachments** in the field.
An **AI** (Gemini) then drafts the official **document** (PV). The document goes DRAFT/DRAFT_AI → AWAITING_APPROVAL → APPROVED, and an approved document is rendered as a PDF with PDFBox.

The React front end is a separate repo: `C:\Users\Souhayb\Desktop\Mofawid_Front_Test`. It uses Vite, proxies `/api` to `localhost:8080`, and CORS only allows `localhost:5173`.

## Commands

The project uses the Maven wrapper with Java 21 (Spring Boot 4.1, Hibernate 7).

```bash
./mvnw spring-boot:run                      # run (needs the env vars below)
./mvnw test                                 # all tests
./mvnw test -Dtest=CaseDeleteTest           # one test class
./mvnw test -Dtest=CaseDeleteTest#refusedWhenTheCaseHasAnApprovedDocument
./mvnw clean package -DskipTests            # build the jar
```

- **Configuration:** everything comes from environment variables (`application.properties` only has `${...}` placeholders). Copy `.env.example` to `.env`.
  - Needed: `PORT`, `DATA_SOURCE_URL`/`DATA_SOURCE_USERNAME`/`DB_PASSWORD`, `GEMINI_API_KEY`, `CHAT_MODEL`, `CHAT_MODEL_VERSION`, `JWT_SECRET`, `TRACKING_SECRET`, and `R2_ACCOUNT_ID`/`R2_ACCESS_KEY`/`R2_SECRET_KEY`/`R2_BUCKET` (Cloudflare R2 storage).
  - In a bash shell, load them with `set -a && . ./.env && set +a`. Never print or commit `.env` values.
- **Database:** MySQL on Aiven is the shared, real database. Ask before writing to it.
  - The schema is managed by Hibernate (`ddl-auto=update`). Flyway is on the classpath but `spring.flyway.enabled=false`.
  - UUID ids are stored as binary, so raw SQL needs `UUID_TO_BIN('...')`.
- **API docs:** Swagger UI is at `/swagger-ui.html`.
- **Tests:** the unit tests under `src/test/.../services/ServicesImpl/` use Mockito only and need no database. `MofawidApplicationTests` loads the full context, so it needs the env vars.

## Architecture

Package `com.example.mofawid` is split in layers:
- `controllers` → `services` (interfaces) → `services/ServicesImpl` → `repositories` (Spring Data JPA) → `models`.
- `DTO` holds request and response records, grouped by entity (for example `DTO/TenantDTO/TenantResposeDTO`; the misspelling "Respose" is in the existing names, so keep it). `mapper` holds the MapStruct mappers.

Cross-cutting rules you need to know before changing a service:

- **Tenant isolation is done by hand in every service.** The current user comes from `getCurrentUser()` (the security context). Their tenant id is used in every lookup, as in `caseRepo.findByIdAndTenantId(id, tenantId)`.
  - A new query on tenant data must be scoped the same way.
  - A record from another tenant should come back as "not found".
- **Roles:** SUPER_ADMIN, ADMIN, ASSISTANT and COLLABORATEUR, enforced with `@PreAuthorize("hasAnyRole(...)")` on the controller methods.
  - The front end mirrors this in `src/features/auth/permissions.ts`, so a role change usually needs an edit there too.
  - A collaborateur only sees their own missions.
- **Auth:** stateless JWT (`config/JwtUtil`, `JwtAuthFilter`, `SecurityConfig`).
  - Public routes: `/api/auth/**`, Swagger, and `/api/public/tracking/**` (the client tracking link, signed with `tracking.secret`).
  - `SuperAdminInitializer` creates the super admin at startup.
- **Errors:** services throw the typed exceptions in `exceptions/`: `ResourceNotFoundException` (404), `BadRequestException` (400), `ForbiddenException` and `UnauthorizedException`. `GlobalExceptionHandler` turns them into JSON responses. Don't return error codes by hand.
- **Nested resources:** missions own attempts and observations (`/api/missions/{missionId}/attempts`, `.../observations`). Cases own billing (`/api/cases/{caseId}/billing`), parties and documents.
- **Deletes:** the foreign keys have no cascade. To delete a case, `CaseServiceImpl.deleteCase` removes the children by hand, in this order:
  1. attachments;
  2. the observations and attempts of each mission, then the missions;
  3. documents, billing, tracking and parties;
  4. the case itself.

  The R2 files are removed after that, and a failure there is only logged. A case with an APPROVED document can't be deleted. Follow the same pattern for any other delete that has children.
- **AI documents:** `Prompt/DocumentPromptFactory` builds one prompt per document type from the case, party, attempt and observation data. `AiServiceImpl` sends it to Gemini through the Spring AI `ChatClient`.
  - There are 7 document types (`DocumentKind`), such as NOTIFICATION, NOTIFICATION_REFUS and NOTIFICATION_INFORMATIF. `DocumentServiceImpl` checks that the requested type matches the result of the last delivery attempt (`expectedKind`).
  - Observations can also be dictated: the audio is transcribed through the same AI service.
- **PDF:** `ReportPdfServiceImpl` uses PDFBox 3. It shapes Arabic text with ICU4J and the Amiri fonts in `resources/fonts`.
- **Files:** `StorageService` stores attachments in Cloudflare R2 through the AWS S3 SDK. The database only keeps the object key.

## Conventions

- The owner is a junior Spring developer. Write plain, simple code that matches the existing style: constructor injection, short `//` comments, no clever abstractions. Explain big changes before making them.
- Real huissier reports in `C:\Users\Souhayb\Desktop\hussier_rapports` contain personal data. Use them only to understand the document formats. Never use them in tests, demos or seed data; use invented data.
- `docs/` holds planning notes in French (for example `docs/Mofawid_Suivi_Procedures_Plan.md`).
