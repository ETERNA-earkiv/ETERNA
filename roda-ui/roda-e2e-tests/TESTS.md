# ETERNA E2E Test Suite — `roda-e2e-tests`

This document describes every test in the end-to-end (E2E) browser test suite: what it does,
why it exists, and what it would catch if it failed.

---

## Table of Contents

1. [Overview](#overview)
2. [Technology Choices](#technology-choices)
3. [How to Run](#how-to-run)
4. [Infrastructure — `BaseTest`](#infrastructure--basetest)
5. [Page Objects](#page-objects)
   - [LoginPage](#loginpage)
   - [BrowsePage](#browsepage)
   - [SearchPage](#searchpage)
   - [IngestPage](#ingestpage)
   - [PlanningPage](#planningpage)
   - [ProcessPage](#processpage)
   - [AdminPage](#adminpage)
6. [Test Classes and Methods](#test-classes-and-methods)
   - [LoginTest](#logintest-2-tests)
   - [BrowseAIPTest](#browseaiptest-1-test)
   - [SearchTest](#searchtest-2-tests)
   - [IngestTransferTest](#ingesttransfertest-3-tests)
   - [ProcessTest](#processtest-1-test)
   - [RiskManagementTest](#riskmanagementtest-2-tests)
   - [AdminTest](#admintest-2-tests)
7. [GWT-Specific Quirks](#gwt-specific-quirks)
8. [Failure Artifacts](#failure-artifacts)
9. [Configuration Reference](#configuration-reference)

---

## Overview

The E2E suite verifies that ETERNA's key user flows work correctly in a real browser against
a running application instance. It does **not** mock the backend; it exercises the full stack
from GWT JavaScript through the Jersey REST API to Solr and the storage layer.

**Total tests: 13** across 7 test classes, covering:

| Area | Tests |
|---|---|
| Authentication | Login, logout, wrong-password rejection |
| Browse | AIP catalogue renders, search bar present |
| Search | Search page loads, wildcard query returns results panel |
| Ingest transfer | Transfer page loads, upload button renders, file upload appears in list |
| Process / Jobs | Jobs list page renders |
| Risk management | Planning page and risk register render |
| Administration | User management page renders, admin user visible in list |

---

## Technology Choices

### Playwright for Java (1.49.0)

Playwright was chosen over extending the existing `SeleniumUtils.java` utility class for
three reasons:

1. **GWT async handling.** The GWT Single-Page Application triggers asynchronous RPC calls
   after every navigation. `SeleniumUtils` works around this with 10-second `Thread.sleep()`
   calls between steps, which is both slow and fragile. Playwright's `waitFor*` API blocks
   precisely until a named element becomes visible or a URL condition is met.

2. **Zero driver management.** Playwright bundles its own browser binary and ships it during
   `mvn verify`, eliminating ChromeDriver/browser version mismatch failures.

3. **Built-in diagnostics.** Playwright records a Chromium Trace (`.zip`) and full-page
   screenshot for every failing test with no additional boilerplate.

### TestNG 7.x

TestNG was chosen because it matches the existing `roda-core-tests` test runner. The suite
descriptor `testng-e2e.xml` mirrors the structure of `roda-core/roda-core-tests/testng.xml`.

### maven-failsafe-plugin

Tests run in the `integration-test` Maven lifecycle phase (not `test`) via `failsafe`, which
correctly reports failures as build failures only at the `verify` phase. This allows the app
to be started externally before the suite runs and torn down after it finishes.

---

## How to Run

### Prerequisites

The application must be running before the suite starts. The suite performs a health-check
(`GET /api/openapi.json → HTTP 200`) before any test and aborts immediately with a clear
error message if the app is not reachable.

```bash
# 1. Start infrastructure (Solr, ZooKeeper, Siegfried, ClamAV)
mkdir -p $HOME/.roda/data/storage
docker compose -f deploys/standalone/docker-compose-dev.yaml up -d

# 2. Build and install core
mvn install -Pcore -DskipTests

# 3. Start the application (keep this running in a separate terminal)
mvn -pl roda-ui/roda-wui -am spring-boot:run -Pdebug-main
```

### First-time: install browser binary

```bash
mvn -pl roda-ui/roda-e2e-tests exec:java \
    -Dexec.mainClass=com.microsoft.playwright.CLI \
    -Dexec.args="install firefox"
```

### Run the suite

```bash
# Headless (default — CI-friendly)
mvn verify -Pe2e -DskipTests=false

# Headed — watch the browser while tests run
mvn verify -Pe2e -DskipTests=false -Dplaywright.headless=false

# Against a non-default host
mvn verify -Pe2e -DskipTests=false -Deterna.base.url=http://staging:8080

# Single test class
mvn verify -Pe2e -DskipTests=false -Dit.test=LoginTest

# Firefox explicitly (default in pom.xml; override to chromium if needed)
mvn verify -Pe2e -DskipTests=false -Dplaywright.browser=chromium
```

---

## Infrastructure — `BaseTest`

**File:** `src/test/java/org/roda/wui/e2e/base/BaseTest.java`

All test classes extend `BaseTest`, which owns the browser lifecycle and provides shared
helpers. It is abstract and annotated `@Test(groups = {"e2e"})` so subclass methods inherit
the group without repeating it.

### `@BeforeSuite launchBrowser()`

Runs once before any test in the suite.

1. Creates a `Playwright` instance and launches the browser specified by the
   `playwright.browser` system property (`firefox` by default).
2. Performs a **health check**: issues `GET /api/openapi.json` and asserts HTTP 200.
   If the app is not running the entire suite fails immediately with a message that
   includes the exact Maven command needed to start it.
3. Creates the output directories:
   - `target/e2e-screenshots/` — full-page PNGs for failing tests
   - `target/e2e-traces/` — Playwright Trace `.zip` files for failing tests
   - `target/e2e-videos/` — screen recordings for all tests

**Why this matters:** A missing app would cause every test to fail with a cryptic timeout
error. The health-check converts that into a single, actionable failure at the very start.

### `@AfterSuite closeBrowser()`

Closes the `Browser` and `Playwright` instances to release native resources.

### `@BeforeMethod createContext()`

Runs before each test method.

1. Creates a new `BrowserContext` with video recording enabled to `target/e2e-videos/`.
2. Starts Playwright tracing with screenshot and DOM snapshot capture.
3. Creates a fresh `Page` inside the context.

Each test gets an isolated browser context (no shared cookies, storage, or login state).

### `@AfterMethod closeContext(ITestResult result)`

Runs after each test method.

- **On failure:** saves the Playwright trace as `target/e2e-traces/<Class>_<method>.zip`
  and a full-page screenshot as `target/e2e-screenshots/<Class>_<method>.png`.
- **On success:** stops tracing without saving (discards captured data).
- Always closes the `BrowserContext`.

### `loginAsAdmin()` helper

A protected method available to all test classes. It:

1. Navigates to `/#login`.
2. Waits for network idle.
3. Fills the username field (`.fieldTextBox` — first match, which is the username TextBox
   in `Login.ui.xml`).
4. Fills the password field (`input[type=password]`).
5. Clicks the login button (`.login-button`).
6. Waits for the URL to leave `#login` (GWT redirects to `#welcome` on success).
7. Waits for network idle.

**Why `waitForURL` instead of `waitForLoadState`:** GWT's `Login.doLogin()` fires an async
RPC call; `NETWORKIDLE` can return while the page is still rendering the post-login state
and the URL might still contain `#login`. Waiting for the URL change ensures the login RPC
has completed and GWT has navigated away.

---

## Page Objects

The Page Object Model is used to isolate locators from test logic. Each page class
encapsulates the selectors and wait strategies for one section of the GWT SPA, so a
selector change only needs to be fixed in one place.

### LoginPage

**File:** `src/test/java/org/roda/wui/e2e/pages/LoginPage.java`
**GWT source:** `roda-wui/src/main/java/org/roda/wui/client/main/Login.java`

| Method | What it does |
|---|---|
| `navigate()` | Navigates to `/#login`, waits for network idle |
| `login(user, password)` | Fills credentials, clicks `.login-button`, waits for URL to leave `#login` |
| `isLoggedIn()` | Navigates to `/#login`, waits up to 5 s for `.loginPanel` `nth(1)` (the `loggedInPanel`) to become **VISIBLE** |
| `isLoginFormVisible()` | Navigates to `/#login`, waits up to 5 s for `.loginPanel` `first()` (the login form panel) to become **VISIBLE** |
| `logout()` | Navigates to `/#login`, clicks the logout link (`.login-link` filtered by text "logout") |

**Locator rationale:**

- `Login.ui.xml` declares both `loginPanel` and `loggedInPanel` as `FlowPanel` elements with
  style name `loginPanel`. GWT adds the GWT class name as the CSS class. The two panels
  are distinguished by index: `.loginPanel.first()` is the unauthenticated form;
  `.loginPanel.nth(1)` is the authenticated panel.
- Both `isLoggedIn()` and `isLoginFormVisible()` call `navigate()` before querying, because
  GWT's `Login.resolve()` calls `UserLogin.getAuthenticatedUser()` asynchronously. The panel
  visibility is set only after the async callback fires. Navigating fresh and calling
  `waitFor(VISIBLE)` is the only reliable way to observe the resolved state.

### BrowsePage

**File:** `src/test/java/org/roda/wui/e2e/pages/BrowsePage.java`

| Method | What it does |
|---|---|
| `navigate()` | Navigates to `/#browse`, waits for network idle |
| `isLoaded()` | Checks `.browse` or `.contentFlowPanel` is present |
| `hasSearchBar()` | Checks `input[type=text]` is present |
| `getAIPRowCount()` | Returns count of `table tr` elements |
| `clickFirstAIP()` | Clicks the second `table tr` (first is the header row) |

### SearchPage

**File:** `src/test/java/org/roda/wui/e2e/pages/SearchPage.java`

| Method | What it does |
|---|---|
| `navigate()` | Navigates to `/#search`, waits for network idle |
| `isLoaded()` | Checks `.search` or `input[type=text]` is present |
| `search(term)` | Fills the first text input, presses Enter, waits for network idle |
| `hasResults()` | Checks `.searchResults`, `.emptyPanel`, or `table` is present |

### IngestPage

**File:** `src/test/java/org/roda/wui/e2e/pages/IngestPage.java`
**GWT source:** `roda-wui/src/main/java/org/roda/wui/client/ingest/transfer/TransferUpload.java`

| Method | What it does |
|---|---|
| `navigateToTransfer()` | Navigates to `/#ingest/transfer`, waits for network idle |
| `isTransferPageLoaded()` | Checks `.wui-ingest-transfer` or `.contentFlowPanel` is present |
| `hasUploadButton()` | Checks `.fa-upload` is present |
| `uploadFile(filePath)` | Navigates to `/#ingest/transfer/upload`, waits for `#drop`, sets file on `input[name='upl']` |
| `isFileInList(filename)` | Checks `#upload-list` or any `table` contains the filename text |

**Locator rationale for `hasUploadButton()`:**

GWT's `ActionButton.java` maps the logical button identifier `"btn-upload"` to a Font
Awesome icon class `"fa fa-upload"` rendered on an `<i>` element inside an
`.actionable-button` container. There is no CSS class `btn-upload` on the button element
itself. The selector `.fa-upload` detects the icon regardless of whether the surrounding
button is visible.

**Upload implementation rationale:**

`TransferUpload.updateUploadForm()` renders a `<form id='upload'>` containing a `<div
id='drop'>` drop-zone and a `<input type='file' name='upl'>` file input. jQuery Fileupload
styles the drop-zone as the visible target and hides the native `<input>`. Playwright's
`setInputFiles()` bypasses visibility checks, so no `force` flag is needed. The file input
name comes from `RodaConstants.API_PARAM_UPLOAD`, which equals `"upl"`.

Clicking the upload toolbar button was avoided because `ActionButton` wraps the icon in an
`.actionable-button` container that may have `visibility: hidden` when the button is
conditionally disabled. Navigating directly to `/#ingest/transfer/upload` is both more
reliable and exactly equivalent to what the user does when clicking the button.

### PlanningPage

**File:** `src/test/java/org/roda/wui/e2e/pages/PlanningPage.java`
**GWT source:** `RiskRegister.RESOLVER.getHistoryToken()` → `"riskregister"`

| Method | What it does |
|---|---|
| `navigate()` | Navigates to `/#planning/riskregister`, waits for network idle |
| `isLoaded()` | Waits up to 10 s for `.wui-risk-register` to appear |
| `navigateToRisks()` | Navigates to `/#planning/riskregister`, waits for network idle |
| `isRiskRegisterLoaded()` | Waits up to 10 s for `.wui-risk-register` to appear |
| `clickNewRisk()` | Clicks `.btn-plus` or `button:has(.fa-plus)`, waits for network idle |
| `isNewRiskFormVisible()` | Checks `input[type=text]` is present |

**Locator rationale:**

`/#planning` alone renders an empty HTML placeholder. The risk register is at
`/#planning/riskregister` (from `RiskRegister.RESOLVER.getHistoryToken()`). The widget
renders with CSS class `.wui-risk-register`. Using `waitFor` on this specific class, rather
than a generic `table` or `.emptyPanel`, avoids false positives from other pages.

### ProcessPage

**File:** `src/test/java/org/roda/wui/e2e/pages/ProcessPage.java`

| Method | What it does |
|---|---|
| `navigate()` | Navigates to `/#process`, waits for network idle |
| `isLoaded()` | Checks `.contentFlowPanel`, `table`, or `.emptyPanel` is present |

### AdminPage

**File:** `src/test/java/org/roda/wui/e2e/pages/AdminPage.java`

| Method | What it does |
|---|---|
| `navigate()` | Navigates to `/#administration/user`, waits for network idle |
| `isLoaded()` | Waits up to 10 s for `.wui-management-user` to appear |
| `navigateToUsers()` | Navigates to `/#administration/user`, waits for network idle |
| `isAdminUserVisible()` | Checks `table` contains text "admin" |
| `clickNewUser()` | Clicks `.btn-plus` or `button:has(.fa-plus)`, waits for network idle |
| `isNewUserFormVisible()` | Checks `input[type=text]` is present |

**Locator rationale:**

`/#administration` renders an empty placeholder. User management is at
`/#administration/user` (from `MemberManagement.RESOLVER.getHistoryToken()`). The widget
has CSS class `.wui-management-user`, used in `isLoaded()` with `waitFor` to handle GWT's
async rendering.

---

## Test Classes and Methods

### LoginTest (2 tests)

**File:** `src/test/java/org/roda/wui/e2e/tests/LoginTest.java`

#### `loginAndLogout`

**What it does:**
1. Opens `/#login`.
2. Asserts the login form (`.loginPanel.first()`) is visible.
3. Fills credentials and submits.
4. Asserts the logged-in panel (`.loginPanel.nth(1)`) is visible.
5. Clicks the logout link.
6. Asserts the login form is visible again.

**Why it exists:**
Authentication is a prerequisite for every other feature in ETERNA. This test verifies the
complete login/logout cycle end-to-end: that the GWT `UserLogin` singleton successfully
authenticates via the backend RPC, that the UI transitions to the authenticated state, and
that logging out reverts the UI to the guest state.

**What it would catch:**
- Backend authentication endpoint broken or returning the wrong response
- GWT `Login.resolve()` or `UserLogin.logout()` not updating the UI correctly
- CSS class renames in `Login.ui.xml` or `Login.java`
- Spring Security session management issues
- The logout redirect landing on an unexpected page

#### `loginWithWrongPasswordShowsError`

**What it does:**
1. Opens `/#login`.
2. Attempts login with the correct username but an incorrect password.
3. Asserts the login form is still visible (user was not redirected).
4. Asserts `isLoggedIn()` returns false.

**Why it exists:**
Verifying that failed authentication does not grant access is a security-critical behaviour.
This test ensures that an `AuthenticationDeniedException` from the backend is correctly
surfaced in the UI (staying on `#login`) rather than granting access.

**What it would catch:**
- Backend incorrectly returning a success response for wrong credentials
- GWT's error handling in `doLogin()` not keeping the user on `#login`
- Session fixation: a previous authenticated session leaking into the new context

---

### BrowseAIPTest (1 test)

**File:** `src/test/java/org/roda/wui/e2e/tests/BrowseAIPTest.java`

#### `browsePageLoads`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#browse`.
3. Asserts the browse container (`.browse` or `.contentFlowPanel`) is rendered.
4. Asserts a search input (`input[type=text]`) is present.

**Why it exists:**
The AIP (Archival Information Package) catalogue is the central feature of any digital
preservation repository. This test confirms that an authenticated user can reach the browse
section and that the GWT widget and its search bar have been rendered — catching startup
failures, permission errors, or Solr unavailability that would leave the catalogue empty or
absent.

**What it would catch:**
- `BrowseAIP` widget failing to mount (GWT compilation or runtime error)
- Solr index unavailable, causing the browse query to throw instead of returning empty results
- RBAC / permission check for `BROWSE_AIP` failing for the admin user
- GWT routing error preventing `/#browse` from resolving to the correct widget

---

### SearchTest (2 tests)

**File:** `src/test/java/org/roda/wui/e2e/tests/SearchTest.java`

#### `searchPageLoads`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#search`.
3. Asserts the search container (`.search` or a text input) is present.

**Why it exists:**
Confirms that the search section renders at all. A failure here indicates a routing or
widget-mount problem independent of whether any content is indexed.

#### `searchReturnsResults`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#search`.
3. Types `*` (wildcard — matches all documents) into the search field and presses Enter.
4. Asserts that a results container (`.searchResults`, `.emptyPanel`, or `table`) appears.

**Why it exists:**
A results panel (even an empty one) proves the full search round-trip works: GWT fires the
search RPC, the backend executes a Solr query, and the response is rendered. The wildcard
`*` avoids dependence on specific indexed content — the test passes whether the index is
empty or populated.

**What it would catch:**
- Solr cloud unreachable or misconfigured, causing search RPC to throw
- GWT search widget not rendering the results container after a query
- REST endpoint `/api/v1/aips` returning a non-200 status for a wildcard query
- Regression in `Search.java` that prevents the results panel from mounting

---

### IngestTransferTest (3 tests)

**File:** `src/test/java/org/roda/wui/e2e/tests/IngestTransferTest.java`

#### `ingestTransferPageLoads`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#ingest/transfer`.
3. Asserts the transfer page (`.wui-ingest-transfer` or `.contentFlowPanel`) is rendered.

**Why it exists:**
The ingest transfer workspace is the entry point for all SIP ingestion. This smoke test
confirms the section is accessible to an admin user and that the GWT widget mounts correctly.

#### `uploadButtonIsVisible`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#ingest/transfer`.
3. Asserts the Font Awesome upload icon (`.fa-upload`) is present on the page.

**Why it exists:**
The upload action is the primary action in the ingest transfer workspace. This test verifies
that the `ActionButton` for upload is rendered and its icon is in the DOM, indicating the
toolbar has loaded correctly.

**Locator note:** GWT's `ActionButton.java` translates the logical identifier `"btn-upload"`
into a Font Awesome icon class `"fa fa-upload"` on an `<i>` element. There is no CSS class
`btn-upload` on the button element. The `.fa-upload` selector is correct.

#### `uploadSIP`

**What it does:**
1. Logs in as admin.
2. Resolves the classpath resource `fixtures/sample.zip` (a minimal E-ARK SIP).
3. Navigates to `/#ingest/transfer`.
4. Calls `IngestPage.uploadFile()`, which:
   a. Navigates directly to `/#ingest/transfer/upload` (the upload form route).
   b. Waits up to 10 s for `#drop` (the jQuery Fileupload drop-zone container) to appear.
   c. Calls `setInputFiles()` on `input[name='upl']` (the native file input hidden by jQuery).
   d. Waits for network idle (upload HTTP POST completes).
5. Asserts `sample.zip` appears in `#upload-list` or a `table`.

**Why it exists:**
SIP upload is the first step in every preservation workflow. This test verifies the upload
form renders, the jQuery Fileupload plugin is initialised (the drop-zone appears), the file
POST reaches the backend REST endpoint (`POST /api/v1/transfers`), and the filename is
reflected in the upload list.

**What it would catch:**
- `TransferUpload.updateUploadForm()` not rendering the `#drop` container
- `RodaConstants.API_PARAM_UPLOAD` changing from `"upl"` to something else
- REST endpoint for transfer upload returning an error
- File not appearing in the index after upload (Solr ingest latency too high, requiring a
  longer `waitForLoadState` or explicit poll)
- RBAC: admin lacking `INGEST_TRANSFER_UPLOAD` permission

---

### ProcessTest (1 test)

**File:** `src/test/java/org/roda/wui/e2e/tests/ProcessTest.java`

#### `processPageLoads`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#process`.
3. Asserts the process section (`.contentFlowPanel`, `table`, or `.emptyPanel`) is rendered.

**Why it exists:**
The process/jobs section shows running and completed preservation actions (ingest jobs,
format identification, fixity checks, etc.). This smoke test confirms the section is
accessible and the widget mounts, catching permission errors or GWT routing regressions.

**What it would catch:**
- `Jobs` widget failing to mount
- Admin user lacking `PROCESS_LIST` permission
- Solr job index unavailable, preventing the jobs list query

---

### RiskManagementTest (2 tests)

**File:** `src/test/java/org/roda/wui/e2e/tests/RiskManagementTest.java`

#### `planningPageLoads`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#planning/riskregister`.
3. Asserts `.wui-risk-register` is rendered (waits up to 10 s).

**Why it exists:**
The planning section contains the risk register, which is an OAIS-required component for
proactive preservation management. This test confirms the section is accessible and the GWT
widget mounts correctly.

**Route note:** `/#planning` alone renders an empty HTML placeholder; the risk register
widget lives at `/#planning/riskregister` (from `RiskRegister.RESOLVER.getHistoryToken()` =
`"riskregister"`). The `waitFor` on `.wui-risk-register` is used because generic selectors
like `table` or `.emptyPanel` also appear on other pages and would produce false positives.

#### `riskRegisterLoads`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#planning/riskregister`.
3. Asserts `.wui-risk-register` is rendered (waits up to 10 s).

**Why it exists:**
A redundant but explicit confirmation that `navigateToRisks()` and `isRiskRegisterLoaded()`
work independently of `navigate()` / `isLoaded()`. This enables isolated diagnosis if one
method fails without the other.

**What both risk tests would catch:**
- `RiskRegister` widget failing to mount (GWT compilation or runtime error)
- Admin lacking `RISK_LIST` permission
- Solr risk index unavailable
- History token change: if `RiskRegister.RESOLVER.getHistoryToken()` is renamed, the
  navigation would land on the wrong page and `.wui-risk-register` would not appear

---

### AdminTest (2 tests)

**File:** `src/test/java/org/roda/wui/e2e/tests/AdminTest.java`

#### `administrationPageLoads`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#administration/user`.
3. Asserts `.wui-management-user` is rendered (waits up to 10 s).

**Why it exists:**
The administration section manages users, groups, and system configuration. Confirming it
loads verifies that the admin user has the required superuser permissions and that the GWT
user management widget mounts correctly.

**Route note:** `/#administration` alone renders an empty placeholder; the user management
widget is at `/#administration/user` (from `MemberManagement.RESOLVER.getHistoryToken()` =
`"user"`).

#### `usersListShowsAdminUser`

**What it does:**
1. Logs in as admin.
2. Navigates to `/#administration/user`.
3. Asserts the word "admin" appears in a `table` row.

**Why it exists:**
The admin user is created during first startup from `ldap/users.ldif`. This test confirms
that:
- The user management REST endpoint returns the user list.
- The GWT table renders at least the built-in admin user.
- The admin account is active and not accidentally deleted by a migration.

**What it would catch:**
- REST endpoint `/api/v1/users` returning an empty list or an error
- The admin user being deleted or deactivated during a migration
- `MemberManagement` widget not rendering the user table
- RBAC: admin user lacking `USER_LIST` permission

---

## GWT-Specific Quirks

GWT compiles Java to JavaScript and implements its own history/routing mechanism
(`History.addValueChangeHandler`). Several of its behaviours require non-obvious test
strategies.

### 1. Async auth — `waitFor(VISIBLE)` not `isVisible()`

`UserLogin.getAuthenticatedUser()` is an asynchronous GWT-RPC call. The `Login` widget
fires this call in `resolve()` and sets panel visibility only inside the callback. By the
time Playwright's `waitForLoadState(NETWORKIDLE)` returns, the async callback may not yet
have fired. All panel-visibility checks therefore use `waitFor(VISIBLE, timeout=5s)` to
block until the callback has completed.

### 2. Same-hash navigation is a no-op

Navigating to the same GWT hash token (`page.navigate("#login")` when already on `#login`)
does not trigger `History.fireCurrentHistoryState()` in older GWT versions. This means
`Login.resolve()` is not called again and `loggedInPanel` / `loginPanel` visibility is not
updated. All page objects that check state on `#login` call `navigate()` unconditionally to
force a cross-hash round-trip.

### 3. Logout causes a full-page reload

`UserLogin.logout()` calls `Window.open("/logout?hash=...", "_self")`, which causes a
complete browser navigation to the Spring Security logout endpoint and back to the app.
After logout the app lands on `/#welcome`, not `/#login`. `LoginPage.isLoginFormVisible()`
therefore calls `navigate()` (which goes to `/#login`) before looking for the login panel,
rather than assuming the current URL.

### 4. Upload form is rendered by jQuery Fileupload, not native HTML

`TransferUpload.updateUploadForm()` injects a jQuery Fileupload widget via `innerHTML`.
jQuery Fileupload hides the native `<input type=file>` and shows a styled drop-zone. The
correct Playwright approach is `setInputFiles()` on the hidden native input, which bypasses
visibility constraints. Do not attempt to `click()` the drop-zone or the styled button, as
that triggers a native OS file picker that Playwright cannot interact with.

### 5. Section roots render empty placeholders

Several GWT sections (`/#planning`, `/#administration`) render empty HTML nodes at the root
and only populate content at sub-paths. All page objects navigate directly to the
content-bearing sub-path (e.g., `/#planning/riskregister`, `/#administration/user`).

---

## Failure Artifacts

When a test fails, the following files are saved automatically:

| Artifact | Location | Contents |
|---|---|---|
| Playwright Trace | `target/e2e-traces/<Class>_<method>.zip` | Full browser trace with DOM snapshots, network log, and screenshots at each step |
| Screenshot | `target/e2e-screenshots/<Class>_<method>.png` | Full-page screenshot at the moment of failure |
| Video | `target/e2e-videos/*.webm` | Screen recording of the entire test (saved for all tests) |

To view a trace:

```bash
# Using Playwright CLI
java -cp "$(mvn -pl roda-ui/roda-e2e-tests -q -DforceStdout dependency:build-classpath)" \
    com.microsoft.playwright.CLI show-trace target/e2e-traces/LoginTest_loginAndLogout.zip
```

---

## Configuration Reference

All values are overridable with `-D<property>=<value>` on the `mvn verify` command line.

| System Property | Default | Description |
|---|---|---|
| `eterna.base.url` | `http://localhost:8080` | Base URL of the running ETERNA application |
| `eterna.admin.user` | `admin` | Username of the admin account used in tests |
| `eterna.admin.password` | `eterna` | Password of the admin account (from `roda-test.properties`) |
| `playwright.headless` | `true` | Set to `false` to watch the browser during test runs |
| `playwright.browser` | `firefox` | Browser engine: `firefox`, `chromium`, or `webkit` |

The default browser is **Firefox**, which avoids the Chromium sandbox permission issues
common on Fedora/RHEL developer workstations. Override with `-Dplaywright.browser=chromium`
on Ubuntu/Debian/macOS.
