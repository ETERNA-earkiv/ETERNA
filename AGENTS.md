# AGENTS.md

This file provides guidance for AI coding agents working in this repository.

## Project Overview

**ETERNA** is an open-source digital preservation repository (E-Archive) implementing the OAIS reference model (ISO 14721:2012). It is a multi-module Maven project with:

- **Language**: Java 21
- **Build system**: Apache Maven (multi-module)
- **UI framework**: GWT (Google Web Toolkit) 2.13.0, compiled to JavaScript
- **Backend**: Spring Boot 4.0.3 serving a Jakarta JAX-RS REST API (Jersey 3.1.6)
- **Search/index**: Apache Solr 9 + Apache ZooKeeper
- **Actor model**: Apache Pekko 1.1.4
- **Deployment target**: WAR on Apache Tomcat 10.1 (JRE 21)
- **Key external services**: Apache Solr, Siegfried (format identification), ClamAV (antivirus)

### Module Layout

```
roda-common/
  roda-common-data/       # ETERNA model/data objects
  roda-common-utils/      # Base utility classes
roda-core/
  roda-core/              # Business logic: model, index, storage, migration
  roda-core-tests/        # TestNG test suite
roda-ui/
  roda-wui/               # GWT web UI + REST API → produces the .war
dev/
  codeserver/             # GWT code server for hot-reload during development
deploys/
  standalone/             # Docker Compose files (prod and dev)
docker/                   # Dockerfile and entrypoint
scripts/                  # Release, changelog, and version-check utilities
code-style/               # Eclipse formatter, Checkstyle, and cleanup configs
```

## Build Commands

```bash
# Full build (all modules, skip tests — fastest)
mvn clean package -DskipTests

# Full build with tests
mvn clean package

# Build and install core modules only (no UI)
mvn install -Pcore -DskipTests
```

The WAR artifact is produced at `roda-ui/roda-wui/target/roda-wui-<VERSION>.war`.

> **Note**: Downloading Maven dependencies requires a GitHub Personal Access Token configured in `~/.m2/settings.xml` for GitHub Packages access. See the [GitHub Packages Maven documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token).

## Running Tests

Tests use **TestNG 7.9.0** and live in `roda-core/roda-core-tests/`. The suite is defined in `roda-core/roda-core-tests/testng.xml`.

```bash
# Run all tests
mvn clean test

# Run only the CI subset (group "travis")
mvn test -Dtestng.groups=travis

# Run tests with JaCoCo coverage (as used in the release CI)
mvn -Dtestng.groups="travis" -Denforcer.skip=true clean \
    org.jacoco:jacoco-maven-plugin:prepare-agent install
```

Integration tests that require Solr Cloud need these environment variables set:

```bash
export RODA_CORE_SOLR_TYPE=CLOUD
export RODA_CORE_SOLR_CLOUD_URLS=localhost:2181
```

## Linting and Formatting

All Java source files must be formatted with the project's Eclipse formatter and must carry the project license header.

```bash
# Apply Eclipse code formatter to all Java files
mvn formatter:format

# Validate formatting without modifying files
mvn formatter:validate

# Apply license headers to all Java files
mvn license:format

# Check license headers (fails if any are missing)
mvn license:check
```

Configuration files:
- Formatter rules: `code-style/eclipse_formatter.xml`
- Checkstyle rules: `code-style/checkstyle.xml`
- License header template: `LICENSE_HEADER.txt`

When editing Java files, always run `mvn formatter:format` and `mvn license:check` before committing.

### IDE Setup (IntelliJ IDEA)

1. Install the "Adapter for Eclipse Code Formatter" plugin.
2. Configure it to use `code-style/eclipse_formatter.xml`.
3. Set import order: `java;javax;org;com;`
4. Set class/static import thresholds to 9999 (prevents star imports).

## Development Workflow

### Starting the Dev Environment

```bash
# 1. Start infrastructure dependencies (Solr, ZooKeeper, Siegfried, ClamAV)
mkdir -p $HOME/.roda/data/storage
docker compose -f deploys/standalone/docker-compose-dev.yaml up -d

# 2. Build and install core modules
mvn install -Pcore -DskipTests

# 3. Run the WUI via Spring Boot (http://localhost:8080)
mvn -pl roda-ui/roda-wui -am spring-boot:run -Pdebug-main
```

### GWT Hot-Reload (first time only — compile GWT and copy RPC files)

```bash
mvn -pl roda-ui/roda-wui -am gwt:compile -Pdebug-main -Dscope.gwt-dev=compile
./roda-ui/roda-wui/copy_gwt_rpc.sh
```

Then run the GWT code server in a separate terminal:

```bash
mvn -f dev/codeserver gwt:codeserver -DrodaPath=$(pwd)
```

Open `http://127.0.0.1:9876`, add the bookmarks, then open ETERNA at `http://localhost:8080` and click "Dev Mode On".

### Maven Build Profiles

| Profile | Modules | Typical use |
|---|---|---|
| *(default)* | all | Full build |
| `core` | roda-common + roda-core + roda-core-tests | Build/install core artifacts only |
| `wui` | core + roda-wui | Build UI |
| `debug-main` | roda-wui | Spring Boot dev run with GWT code server |

## Security and Dependency Checks

Run these before releasing or after adding/upgrading dependencies:

```bash
# Check for known CVEs in dependencies
mvn com.redhat.victims.maven:security-versions:check

# Check for available minor/patch version updates
./scripts/check_versions.sh MINOR

# Check for all updates including major version bumps
./scripts/check_versions.sh MAJOR
```

## Key Conventions

- **Package namespace**: Core classes live under `org.roda.core.*`; data/model classes under `org.roda.core.data.*`.
- **No star imports**: Keep import thresholds at 9999 as described above.
- **License header required**: Every `.java` file must start with the license block from `LICENSE_HEADER.txt`.
- **Signed commits**: Commit signing is expected for contributors (see the GitHub docs on [signing commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits)).
- **Branch model**: Work on feature branches; `dev` is the integration branch; releases are triggered by version tags (`v*`).

## CI / GitHub Actions

| Workflow | Trigger | What it does |
|---|---|---|
| `dev-amd64.yml` / `dev-arm64.yml` | Push to `dev` | Builds (no tests) and pushes Docker image to GHCR |
| `release.yml` | Push of `v*` tag | Runs test suite, deploys Maven artifacts to GitHub Packages, builds multi-arch Docker image, creates a GitHub draft release |

## Useful Scripts

| Script | Purpose |
|---|---|
| `scripts/release.sh <VERSION>` | Applies license headers, sets Maven version, commits, tags, and pushes to trigger CI release |
| `scripts/prepare_next_version.sh <VERSION>` | Bumps to the next SNAPSHOT version after a release |
| `scripts/update_changelog.sh <VERSION>` | Updates `CHANGELOG.md` after a release |
| `scripts/check_versions.sh [MINOR\|MAJOR]` | Reports available dependency/plugin updates |
| `scripts/createSolrCollections.sh` | Creates Solr collections for a manual Solr setup |

## Further Reading

- [`DEV_NOTES.md`](DEV_NOTES.md) — setup, debug, and release workflow details
- [`documentation/development/Developers_Guide.md`](documentation/development/Developers_Guide.md) — full developer guide
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution guidelines
- [`deploys/standalone/README.md`](deploys/standalone/README.md) — production deployment instructions
