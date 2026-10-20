# MindFocus QA suite (Java, Gradle)

A separate Gradle project with black-box tests that talk to the **running** system:
REST API tests (REST Assured) and browser tests (Selenium WebDriver), both on JUnit 5 with Allure reports.
The white-box tests (unit tests and Spring integration tests) are in `backend/src/test`.

## Test pyramid

```
             /\            Browser tests (Selenium, JUnit 5)      qa/.../e2e      login, protected route, live dashboard
            /  \
           /----\          API tests (REST Assured, JUnit 5)      qa/.../api      black-box, against the running server
          /      \
         /--------\        Integration tests (Spring + MockMvc    backend/src/test/*IT
        /          \       + Testcontainers PostgreSQL)
       /------------\      Unit tests (JUnit 5, Mockito)          backend/src/test/*Test
```

## Run

Start the system first (`docker compose up -d`, backend, and for browser tests also the frontend). Then:

```bash
cd qa
gradle smokeTest    # the quickest check that the system works (tests tagged smoke)
gradle apiTest      # REST API tests, backend must be running
gradle e2eTest      # browser tests, backend and frontend must be running, Chrome installed
gradle test         # everything
```

Report (install the Allure command line tool once, for example `brew install allure`):

```bash
allure serve build/allure-results
```

Environment variables: `API_URL` (default http://localhost:8080), `BASE_URL` (default http://localhost:4200),
`HEADLESS=0` to watch the browser. Java 21 is needed. Selenium Manager downloads the Chrome driver on the first run,
so that run needs internet access.

If you renamed the `localStorage` keys in the frontend `AuthService`, change them in `BrowserTest.openLoggedIn`.

## Layout

```
qa/
├── build.gradle                 JUnit 5, REST Assured, Selenium, Allure, tag-based test tasks
└── src/test/java/com/mindfocus/qa/
    ├── support/                 Settings, Api (request builders), Users (registers a new user), Dates
    ├── api/                     AuthApiTest, OwnershipApiTest, CrudContractApiTest, HabitApiTest, AnalyticsApiTest
    └── e2e/                     BrowserTest (base), page objects, ScreenshotOnFailure, UiFlowsTest
```

## Why these tests

| Risk | Impact | Where it is tested |
|---|---|---|
| One user reads or changes another user's diary | Critical (privacy) | `OwnershipApiTest` (every resource, 404 not 403, mass assignment); `OwnershipApiIT` in the backend |
| Login and token handling | Critical | `AuthApiTest`; `JwtServiceTest` and `AuthApiIT` in the backend |
| Reminders not sent, sent twice or lost when mail is down | High | `ReminderIT` in the backend |
| Wrong numbers on the dashboard | High | `AnalyticsApiTest`, `UiFlowsTest` (live counter); `AnalyticsServiceTest` in the backend |
| Wrong goal percentage | Medium | `CrudContractApiTest`; `GoalProgressTest` in the backend |
| Wrong habit streak | Medium | `HabitApiTest` |
| Bad input crashes the server | Medium | `CrudContractApiTest`, `HabitApiTest`; `ValidationApiIT` in the backend |
| UI does not match the API | Medium | `UiFlowsTest` |

## Techniques used

- Boundary value analysis: password 7/8/100/101 characters, step weight 0/1/100/101, `days` 0/1/90/999.
- Equivalence classes and data-driven tests with `@ParameterizedTest` and `@MethodSource`.
- Negative and security testing: missing and forged tokens, other users' ids, mass assignment, malformed input.
- State and idempotency: logging the same day twice, deleting a habit with logs.
- Independent tests: each test registers its own user with a unique email, so there is no shared state and no order dependency.
- Tags (`api`, `e2e`, `smoke`) and one Gradle task per suite.
- Page Object pattern, explicit waits (never `sleep`), Allure steps, severities, features and stories, and a screenshot on failure.
- Every REST call is attached to the Allure report with its request and response.

## Defects found while designing the tests

| ID | Severity | Summary | Expected | Found |
|---|---|---|---|---|
| DEF-1 | Major | Streak after a slip today | A slip today resets the current streak to 0 | Yesterday's streak was still counted |
| DEF-2 | Major | Habit log without a date | 400 Bad Request | Reached the database, 500 error |
| DEF-3 | Minor | Goal step validation | Weight 1 to 100 and a title are enforced for steps | Any weight or empty title accepted |

The tests for these are marked `[DEF-n]` and fail until the fixes in `PATCHES.md` are applied.

## Known gaps

- Completion timestamps use the server's default time zone while analytics uses `app.zone`. They are equal in Poland.
- No load, accessibility or cross-browser tests yet.
- Web push delivery is not tested; it needs a real browser push service.
- The Angular code has no component tests yet.
