# ETERNA Changelog

## v1.0.1 (unreleased)

#### Bug fixes
- Catalog tree no longer shows AIPs at description level `item` or `file` [#604](https://github.com/ETERNA-earkiv/ETERNA/issues/604)
- Fixed flickering horizontal scrollbar on data table list pages (e.g. ingest process); the footer no longer scrolls horizontally with the table [#605](https://github.com/ETERNA-earkiv/ETERNA/issues/605) [#609](https://github.com/ETERNA-earkiv/ETERNA/pull/609)

## v1.0.0 (2026-06-15)

#### New features
- Added XSLT viewer for XML files with auto-discovery of co-located stylesheets, multi-stylesheet dropdown, and toggle to view raw XML [#468](https://github.com/ETERNA-earkiv/ETERNA/pull/468)
- Added web archive (WARC/WACZ) viewer using ReplayWeb.page v2.4.4 [#225](https://github.com/ETERNA-earkiv/ETERNA/pull/225)
- Added TIFF file preview with canvas-based rendering and multi-page navigation [#312](https://github.com/ETERNA-earkiv/ETERNA/pull/312)
- Added collapsible catalog tree drawer with level icons and clickable header [#306](https://github.com/ETERNA-earkiv/ETERNA/pull/306) [#430](https://github.com/ETERNA-earkiv/ETERNA/pull/430)
- Added ghost-node fallback in catalog tree for users without root-level permissions [#516](https://github.com/ETERNA-earkiv/ETERNA/pull/516)
- Added resizable columns to all GWT tables via drag handles [#266](https://github.com/ETERNA-earkiv/ETERNA/pull/266)
- Added configurable main menu item visibility via `roda-wui.properties` [#356](https://github.com/ETERNA-earkiv/ETERNA/pull/356)
- Added collapsible info-icon description panels on all main pages [#353](https://github.com/ETERNA-earkiv/ETERNA/pull/353)
- Added full job scheduling system: recurring cron and one-shot archive maintenance jobs, `SCHEDULED` template jobs, `JobSchedulerTask` polling every 60 s, new REST endpoints `POST/DELETE /api/v2/jobs/{id}/schedule` [#257](https://github.com/ETERNA-earkiv/ETERNA/pull/257)
- Added HumanizeDateCell respecting `ui.dateTime.format.UTC` configuration in all list views [#581](https://github.com/ETERNA-earkiv/ETERNA/pull/581)
- Added configurable trash support (`core.storage.filesystem.trash.enabled`) in FileStorageService and ScatteredFileStorageService [#573](https://github.com/ETERNA-earkiv/ETERNA/pull/573)
- Added COOP/COEP headers for cross-origin isolation [#539](https://github.com/ETERNA-earkiv/ETERNA/pull/539)
- Added Swedish localization for cron-expression-descriptor [#574](https://github.com/ETERNA-earkiv/ETERNA/pull/574)
- Added audit log entries on AIP view and XSLT request context in audit trail [#468](https://github.com/ETERNA-earkiv/ETERNA/pull/468)
- Added query routing from welcome page search bar to catalogue [#351](https://github.com/ETERNA-earkiv/ETERNA/pull/351)
- Added welcome page quick-actions configurable via `roda-wui.properties`, time-based greeting, and forum integration (Flarum API proxy showing latest threads) [#320](https://github.com/ETERNA-earkiv/ETERNA/pull/320)
- Added configurable CSV export and expanded the export module to support all entity types: RepresentationInformation, User/Groups, Ingest, and remaining types [#315](https://github.com/ETERNA-earkiv/ETERNA/pull/315) [#575](https://github.com/ETERNA-earkiv/ETERNA/pull/575)
- Added infrastructure support for office document conversion via unoserver (LibreOffice/unoconvert); includes dedicated `unoserver` Docker image and client installation in main image [#309](https://github.com/ETERNA-earkiv/ETERNA/pull/309)
- Added `deploys/full-service/` Docker Compose example for ETERNA 1.0 production-like deployments; includes unoserver, OpenLDAP container, Traefik reverse proxy, ACME TLS, and `ldap_data` named volume for persistence
- Added Swedish translations for set password screen [#536](https://github.com/ETERNA-earkiv/ETERNA/pull/536) and email templates [#583](https://github.com/ETERNA-earkiv/ETERNA/pull/583)
- Added plugin names, descriptions, and parameter labels translated to Swedish for all ingest, preservation, and support plugins [#311](https://github.com/ETERNA-earkiv/ETERNA/pull/311) [#314](https://github.com/ETERNA-earkiv/ETERNA/pull/314)
- Updated PDF redactor integration to `se.whitered.eterna:pdf-redactor` 1.1.0 with async save callbacks, user-defined version suffix (empty → timestamp), and mandatory redaction reason at save time (configurable via `ui.redaction.reason.mandatory`) [#238](https://github.com/ETERNA-earkiv/ETERNA/pull/238) [#319](https://github.com/ETERNA-earkiv/ETERNA/pull/319) [#321](https://github.com/ETERNA-earkiv/ETERNA/pull/321)

#### Improvements
- Replaced embedded ApacheDS with external OpenLDAP server via Spring LDAP integration
- Migrated all remaining GWT-RPC interface methods to REST API v2; removed GWT-RPC layer
- Catalog tree now refreshes automatically after AIP create, delete, move, and metadata updates, including node title sync
- Catalog tree auto-collapses at narrow viewports; fixed double scrollbars and separated tree/content scroll [#434](https://github.com/ETERNA-earkiv/ETERNA/pull/434) [#578](https://github.com/ETERNA-earkiv/ETERNA/pull/578)
- Statistics page AJAX calls are now context-path aware; chart title and layout issues resolved; removed reporting services panel [#547](https://github.com/ETERNA-earkiv/ETERNA/pull/547)
- Date/time formatting centralised through Humanize; disposal confirmation reports respect UTC configuration
- CSV export now uses UTF-8 BOM for correct Excel rendering of Swedish characters, with Swedish column headers and date formatting
- Scheduler hardened against race conditions; firing serialised per job ID; day-name expressions used for weekly schedules
- PREMIS user agent always re-indexed in Solr on create/update and on CAS login [#422](https://github.com/ETERNA-earkiv/ETERNA/pull/422)
- Representation file statistics updated atomically on file creation and deletion [#422](https://github.com/ETERNA-earkiv/ETERNA/pull/422)
- Permissions UI improved with grouped permissions, reusable panel layout, and select-all support [#213](https://github.com/ETERNA-earkiv/ETERNA/pull/213)
- Cache-Control headers added for static resources; StaticCacheFilter updated [#460](https://github.com/ETERNA-earkiv/ETERNA/pull/460)
- Favicon updated to WhiteRed logo
- Default ingest pipeline set as the pre-selected option on the ingest page
- RODATransactionManager disabled for large-scale ingest jobs (legacy storage mode); individual AIP failures are handled at plugin/job level instead of rolling back the entire batch [#290](https://github.com/ETERNA-earkiv/ETERNA/pull/290)
- External links in HTML widgets now open in a new tab with `rel="noopener noreferrer"` [#259](https://github.com/ETERNA-earkiv/ETERNA/pull/259)
- Branding updated: user-visible RODA references replaced with ETERNA in English and Swedish i18n files (UI strings, email subjects, CAS/Drop Folder texts, log facets), OpenAPI config, and license headers [#297](https://github.com/ETERNA-earkiv/ETERNA/pull/297) [#341](https://github.com/ETERNA-earkiv/ETERNA/pull/341) [#342](https://github.com/ETERNA-earkiv/ETERNA/pull/342)

#### Swedish terminology updates
- "Katalog" → "Arkivbestånd" throughout UI, HTML templates, and documentation [#439](https://github.com/ETERNA-earkiv/ETERNA/issues/439)
- "Logisk enhet" / "Förvaringsenhet" → "Strukturenhet" for intellectual entities/AIPs [#437](https://github.com/ETERNA-earkiv/ETERNA/issues/437) [#572](https://github.com/ETERNA-earkiv/ETERNA/pull/572)
- "Undernivå" → "Strukturenhet" [#436](https://github.com/ETERNA-earkiv/ETERNA/issues/436)
- "Ankomstkontroll" → "Leveranskontroll" [#443](https://github.com/ETERNA-earkiv/ETERNA/issues/443)
- "Bevara permanent" → "Bevara" in disposal schedule [#406](https://github.com/ETERNA-earkiv/ETERNA/issues/406)

#### Bug fixes
- Fixed destroyed AIPs appearing in catalog tree [#560](https://github.com/ETERNA-earkiv/ETERNA/pull/560)
- Fixed catalog tree not updating after AIP deletion [#470](https://github.com/ETERNA-earkiv/ETERNA/issues/470) [#530](https://github.com/ETERNA-earkiv/ETERNA/pull/530)
- Fixed ghost nodes showing expand arrow incorrectly; tree state preserved across navigation
- Fixed Solr commit not being guarded on success; commit errors now propagated correctly [#403](https://github.com/ETERNA-earkiv/ETERNA/issues/403) [#407](https://github.com/ETERNA-earkiv/ETERNA/issues/407)
- Fixed `transfer.update` permission could not be saved; role was mapped to `transfer.read` in `roda-roles.properties` [#261](https://github.com/ETERNA-earkiv/ETERNA/pull/261)
- Fixed navigation after removing a representation: user is now redirected to the parent AIP instead of staying on the deleted representation view [#421](https://github.com/ETERNA-earkiv/ETERNA/pull/421)
- Fixed missing AMDSEC element in UpdateOriginalMETS; element is now added automatically when absent [#248](https://github.com/ETERNA-earkiv/ETERNA/pull/248)
- Fixed file download endpoint UUID lookup by resolving file UUIDs to their full composite paths [#253](https://github.com/ETERNA-earkiv/ETERNA/pull/253)
- Fixed retention-period end date handling when the disposal deadline was missing [#459](https://github.com/ETERNA-earkiv/ETERNA/pull/459)
- Fixed NPE in InternalWebAuthFilter login redirect and various production log NPEs [#541](https://github.com/ETERNA-earkiv/ETERNA/pull/541) [#544](https://github.com/ETERNA-earkiv/ETERNA/pull/544)
- Fixed Docker startup syntax error in `docker-entrypoint.sh` caused by duplicate `fi`
- Fixed hardcoded `SPRING_DATASOURCE_URL` in Dockerfile ENV
- Fixed duplicate user creation showing incorrect error message in Swedish [#387](https://github.com/ETERNA-earkiv/ETERNA/issues/387) [#520](https://github.com/ETERNA-earkiv/ETERNA/pull/520)
- Fixed XSLT viewer rendering, caching, fallback to native text preview, and permission grants
- Fixed CSV export field name trimming, section headers, and export dialog placement
- Fixed Swedish i18n: date range search fields [#393](https://github.com/ETERNA-earkiv/ETERNA/issues/393), package label in ingest context [#442](https://github.com/ETERNA-earkiv/ETERNA/issues/442), sub-levels label [#435](https://github.com/ETERNA-earkiv/ETERNA/issues/435), catalog tree search field label [#398](https://github.com/ETERNA-earkiv/ETERNA/issues/398)
- Fixed additional Swedish i18n labels for disposal classes, disposal confirmations/catalog tree, edit permissions, representation cards, and log reason/loading messages [#587](https://github.com/ETERNA-earkiv/ETERNA/pull/587) [#577](https://github.com/ETERNA-earkiv/ETERNA/pull/577) [#524](https://github.com/ETERNA-earkiv/ETERNA/pull/524) [#523](https://github.com/ETERNA-earkiv/ETERNA/pull/523) [#411](https://github.com/ETERNA-earkiv/ETERNA/issues/411) [#412](https://github.com/ETERNA-earkiv/ETERNA/issues/412)
- Fixed ReplayWeb.page URL encoding, service worker scope, and iframe accessibility [#539](https://github.com/ETERNA-earkiv/ETERNA/pull/539)
- Fixed representations card layout and full-area click handling [#531](https://github.com/ETERNA-earkiv/ETERNA/issues/531) [#532](https://github.com/ETERNA-earkiv/ETERNA/pull/532)
- Fixed UI table overflow in job list and disposal confirmations layout [#582](https://github.com/ETERNA-earkiv/ETERNA/pull/582)
- Fixed highlight.js dark theme applied incorrectly; switched to light (github) theme
- Fixed statistics API calls migrated from v1 to v2 endpoints [#547](https://github.com/ETERNA-earkiv/ETERNA/pull/547)
- Fixed WhiteRed support portal URL [#473](https://github.com/ETERNA-earkiv/ETERNA/pull/473)
- Fixed `StringIndexOutOfBoundsException` in `ScatteredFSUtils` when AIP-IDs are shorter than the configured scatter interval; `isValidName` is now called before `getScatteredPath` in `listResourcesUnderContainer` and `getStoragePath`
- Fixed Solr schema fields that deviate from the Java model definition being silently ignored; `SchemaBuilder` now supports `replaceField()` and `SolrBootstrapUtils` calls it on mismatch — resolves `RepresentationInformation` reindex failures caused by `required=true` in Solr when the Java model has `required=false`
- Fixed `NoClassDefFoundError` for `OutputPropertiesFactory` during `RepresentationInformation` deserialization; caused by `xalan:xalan:2.7.3` shipping an empty POM that dropped `xalan:serializer` from the classpath — pinned `xalan:serializer:2.7.2` explicitly

#### Configuration changes

**`roda-core.properties`**
- **LDAP** — removed ApacheDS-specific settings (`core.ldap.backend`, `core.ldap.cacheSize`, `core.ldap.startServer`, `core.ldap.passwordDigestAlgorithm`); connection now configured via `core.ldap.url` and `core.ldap.port` pointing to an external OpenLDAP server (environment variable substitution: `${env:LDAP_SERVER_URL}`, `${env:LDAP_SERVER_PORT}`)
- **Orchestrator** — previously commented-out defaults are now active: `core.orchestrator.max_jobs_in_parallel=8`, `max_limited_jobs_in_parallel=2`, `nr_of_jobs_workers=8`, `nr_of_limited_jobs_workers=2`, `block_size=100`
- **Storage** — added `core.storage.filesystem.trash.enabled` (commented out, default `true`) and inline documentation for ScatteredFileStorageService folder-scattering configuration
- **Email** — `core.email.from`/`user` now uncommented; default protocol changed from `smtps` to `smtp`, default host/port changed to `127.0.0.1:1025` (Mailhog/dev); deploy config reads from `${env:SMTP_HOST}` / `${env:SMTP_PORT}`
- **Storage legacy mode** — added `core.storage.legacy.implementation.enabled=true` to keep large ingest jobs outside `RODATransactionManager` rollback semantics
- **External authentication/security** — added CAS plugin defaults, commented CAS group-mapping settings (`core.authorization.external.*`), and commented external security plugin examples (`core.plugins.external.security.*`) for CAS/Entra ID

**`roda-wui.properties`**
- Added `ui.sharedProperties.whitelist.configuration.prefix` entries for `ui.welcome`, `ui.mainmenu`, and `ui.export`
- Added `ui.sharedProperties.whitelist.configuration.property = ui.redaction.reason.mandatory`
- Added `ui.filter.security.csp.directives[] = worker-src 'self'` to global CSP
- Added path-scoped CSP configuration for the ReplayWeb.page WARC/WACZ viewer (`ui.filter.security.csp.replay.*`)
- Added new role `ui.role: representation.apply_xslt` for XSLT stylesheet application
- **Renamed** `ui.search.fields.Job.*` → `ui.search.fields.IndexedJob.*` (custom configs referencing the old key must be updated)
- Added `ui.icons.class.IndexedJob = fa fa-cog`
- Added `ui.lists.AuditLogs_triggeredLogs.*` search configuration

**`application.properties`** (Spring Boot)
- Added `spring.servlet.multipart.max-file-size=-1` and `max-request-size=-1` (unlimited upload size)
- Added `server.forward-headers-strategy=NATIVE` for reverse-proxy header forwarding
- Added `server.servlet.session.cookie.secure=true` (session cookies require HTTPS by default)
- Added OpenAPI/Swagger configuration (`springdoc.api-docs.path=/api/v2/openapi`, UI disabled by default)
- Added PostgreSQL datasource configuration with lazy startup (`spring.datasource.hikari.initialization-fail-timeout=-1`); application starts without a database connection
- Added `transactions.cleanup.interval.millis=3600000` and `jobs.scheduler.interval.millis=60000`
- Added Micrometer metrics and Prometheus endpoint exposure (`management.endpoints.web.exposure.include=health,info,metrics,prometheus`)

#### Infrastructure
- Docker build context corrected from repo root to `docker/` directory
- CI workflow updated for Docker image build, unoserver image build, and SNAPSHOT artifact publishing
- Dependencies upgraded: Spring Boot 3.4.10, Spring Core 6.2.18, Solr 9.10.1, Logback 1.5.34, Tomcat 10.1.55, PostgreSQL driver 42.7.11, commons-ip2 2.11.2, ReplayWeb.page 2.4.4, Handlebars 4.4.0
- Docker Compose: updated service images for ZooKeeper, Solr, ClamAV, and Siegfried; switched OpenLDAP image to `docker.io/bitnamilegacy/openldap:2.6` [#591](https://github.com/ETERNA-earkiv/ETERNA/pull/591)
- Docker Compose: added unoserver service with restart policy and healthcheck; added Swagger UI and Mailpit to distributed dev environments (not standalone); removed Swagger UI and Mailpit from standalone configuration and changed SMTP host to `mailserver` [#591](https://github.com/ETERNA-earkiv/ETERNA/pull/591)
- Upgraded unoserver from 3.3.2 to 3.7; added `apt upgrade` step for base image security patches, rebuilt font cache, set `FONTCONFIG_CACHE`, and created home directory and dconf cache for the unoserver user
- Added healthchecks for ZooKeeper, Solr, Siegfried, and OpenLDAP in both `docker-compose.yaml` and `full-service/docker-compose.yaml`; upgraded `eterna` `depends_on` conditions from `service_started` to `service_healthy` where healthchecks are defined
- Fixed ACME certificate persistent storage in full-service Docker Compose: updated path to `/var/lib/traefik/acme.json` so certificates survive container recreates

#### Security updates
- Fixed GHSA-wxr5-93ph-8wr9: upgraded `commons-beanutils` to 1.11.0
- Fixed GHSA-wrvw-hg22-4m67, GHSA-h4h5-3hr4-j3g2, GHSA-735f-pc8j-v9w8: upgraded `protobuf-java` to 3.25.9
- Fixed GHSA-mj4r-2hfc-f8p6: upgraded `netty` to 4.1.133.Final
- Fixed GHSA-337m-mw94-2v6g: added `commons-configuration2` 2.15.0
- Fixed GHSA-9339-86wc-4qgf: upgraded `xalan` to 2.7.3
- Fixed GHSA-cgp8-4m63-fhh5: upgraded `commons-net` to 3.9.0
- Fixed GHSA-xwmg-2g98-w7v9: upgraded `nimbus-jose-jwt` to 9.37.4
- Fixed GHSA-qw69-rqj8-6qw8, GHSA-q4rv-gq96-w7c5, GHSA-3gh6-v5v9-6v9j, GHSA-j26w-f9rq-mr2q, GHSA-p26g-97m4-6q7c, GHSA-58qw-p7qm-5rvh: pinned Jetty 9.x transitive dependencies to 9.4.57.v20241219 [#590](https://github.com/ETERNA-earkiv/ETERNA/pull/590)
- Fixed GHSA-7xrh-hqfc-g7qr, GHSA-crhr-qqj8-rpxc: upgraded ZooKeeper to 3.9.5
- Migrated BouncyCastle from `bcpkix/bcprov-jdk15on` to `jdk18on` in `cas-client-core`; upgraded to 1.84
- Upgraded `owasp-java-html-sanitizer` to 20260101.1

#### Documentation
- Added admin guide for date/time timezone configuration
- Added configurable export administration guide
- Added IDP group authorization administration guide
- Added PDF redaction user guide
- Added WARC/WACZ web archive viewer user guide
- Added XSLT viewer user guide
- Added Swedish "Common Terms" (Vanliga begrepp) glossary page
- Updated Swedish overview, FAQ, and pre-ingest documentation (PAIMAS-based delivery process)
- Updated help index with links to new guides


## v0.5.0 (2025-12-16)
#### Updates
- Added Swedish and English SVG credentials images
- Updated default admin password to 'eterna'
- Updated LDAP configuration for admin user with new password and details
- Updated documentation
- Change PluginManager to expect an ETERNA-plugin manifest entry, has a fallback to support RODA plugins
- Ensure parent directories exist when creating a new file in FileStorageService
- Refactored AbstractConvertPlugin to enhance file conversion process with new ConversionContext and ConversionResult classes, improved file processing logic, enhanced error handling, and consistent representation ID handling
- Fixed internationalization of DROPDOWN values in PluginParameters for external plugins
- Added a getCreationTime method to StorageService to be able to fetch a file or folders creation time
- Updates commons-ip dependency to fix problems with incorrect parsing of METS metadata types
- Fix problems in processing `Preservation Metadata` when ingesting E-ARK SIPs
- Fixed missing handling of `Descriptive Metadata` in ResourceParseUtils.convertResourceTo that caused ModelService#listDescriptiveMetadata to fail
- Fixed incorrect naming of PREMIS:FILE metadata when ingesting from E-ARK SIPs
- Added breadcrumb navigation to PDF redactor page to match the navigation pattern used by other file viewing pages
- Fixed missing version number in footer
- Added support for communicating with SOLR over TLS

## v0.4.2 (2025-10-14)
#### New features
- Added functionality to display user extra fields in the user details view
- Added new internationalization (i18n) keys for configurable user extra fields
- Added new Swedish translations for user extra fields
- Added new Swedish documentation for User Configuration detailing the configuration and management of User Extra Fields

#### Improvements
- Updated Developer's Guide to require Oracle Java 21
- Updated internal links in the Swedish footer to reflect restructured documentation paths
- Updated field type `datum_intervall` to `date_interval` in Swedish disposal documentation
- Updated link to Swedish metadata formats documentation in `Descriptive_Metadata_Types_sv_SE.md`
- Updated relative paths for images and links in EditDescriptiveMetadata.md and its Swedish version
- Corrected relative links to Disposal and Developers Guide documentation in FAQ
- Adjusted relative image paths in Statistics.md and its Swedish version to ensure images display correctly
- Corrected image paths in Disposal Policies documentation and its Swedish version
- Updated footer links to reflect updated documentation structure
- Updated contact email for reporting incidents in `CODE_OF_CONDUCT.md` to `info@whitered.se`

#### Cleanup
- Removed unused localized HTML files for various languages

## v0.4.1 (2025-10-07)
#### Improvments
- Updated footer links
- Categorized documention documents into category folders

## v0.4.0 (2025-09-22)
#### New features
* Added missing functionality to set the preservation state of converted representations to `PRESERVATION` in AbstractConvertPlugin
* Make indexing of preservation events optional, it is enabled by default
* Separated Plugins Certificate Status from `VERIFIED` into `VERIFIED` and `LICENSED` where `LICENSED` is equivalent to the previous `VERIFIED`status, which represents a plugin commercially licensed to a customer and `VERIFIED` means that is code signed and trusted 

#### Bug fixes
* Allowed img-src 'self' blob: in the Content-Security-Policy to fix printing in the PDF renderer and PDF redactor
* Updated paths to PDF redactor to point to version 1.0.1

#### Improvements
* Added White Red Plugin Certificate Authority Certificate to the plugin truststore

## v0.3.0 (2025-06-17)
#### Security
- Updated dependencies

#### New features
- Enabled EAD 3 support by default

#### Improvements
- Improved EAD 3 support
- Fixed tab titles
- Add missing translations
- Removed unused plugin market tab

#### Bug fixes
- Fixed NPE in MarketUtils.java

## v0.2.0 (2025-05-13)
#### New features
- Added PDF Redaction tool
- Grouped permission options into categories under Administration -> Users and groups
- ETERNAs Content-Security-Policy is now configurable
- ETERNAs Security related HTTP headers are now toggleable
- The "Secure"-flag of HTTP Cookies is now toggleable to allow full functionality in HTTP (without TLS) even if ETERNA is not hosted on localhost

#### Bug fixes
- Properly escape LDAP queries containing special characters
- Fixed broken height in the PDF Viewer

#### Other changes
- Removed unused link in main menu: Administration -> Monitoring
- Removed unused languages

## v0.1.0 (2024-11-27)
#### Security
- Fixed severe (CVSS v3.1 Base Score: 10.0) vulnerability in updateMyUser method
- Fixed severe (CVSS v3.1 Base Score: 9.1) vulnerability in SIP ingest

#### New features
- Added support for Facet Range queries
- Initial rebranding

## v0.0.1 (2024-09-16)
#### New features
- Implemented new highly configurable Scattered FS Storage Service to spread files and folders to multiple sub-folders
- Upgraded to CloudHttp2SolrClient and added support for Basic authentication to SOLR

&nbsp;

---

# RODA Changelog
## v6.0.0 (2025-07-29)
### :warning: Breaking Changes

- Due to various dependency changes in this release, it is strongly recommended to back up all data and configurations before performing the upgrade. After upgrading, a complete reindexing of all data is required to maintain system integrity and performance.
- The embedded ApacheDS has been replaced by an external LDAP server running in an OpenLDAP container. Due to this change, starting RODA will cause previously stored user data to be lost. We recommend backing up all user information before upgrading. A migration process to transfer existing user data will be provided as soon as possible.
- The legacy REST API (v1) has been fully removed. All external integrations must now use the new REST API (v2)

#### New features:
- Major Web UI redesign: The RODA interface has been completely reimagined to deliver a cleaner, more intuitive, and user-friendly experience. This overhaul touches nearly every aspect of the UI, streamlining workflows, improving accessibility, and aligning with modern design standards. [3330](https://github.com/keeps/roda/issues/3330)
- Introduced a transactional storage mechanism that stages most write operations before committing them to the main storage, enabling rollback in case of errors and improving data integrity and reliability. [102](https://github.com/keeps/roda/issues/102)[1224](https://github.com/keeps/roda/issues/1224)
- The user database service has been upgraded from embedded ApacheDS to an external LDAP server with Spring LDAP integration, enhancing security, performance, and maintainability. [3115](https://github.com/keeps/roda/issues/3115)
- Added support for manual override of file format identification via the Web UI, allowing users to correct misidentified formats when automatic detection fails. [3256](https://github.com/keeps/roda/issues/3256)
- File format identification warnings now generate risk incidents, visible in the file information panel, allowing users to assess and accept potential issues like format mismatches or multiple matches.  [3259](https://github.com/keeps/roda/issues/3259)
- Improved audit log presentation by grouping related REST-API calls under single user actions and allowing inspection of detailed calls, enhancing clarity and reducing noise in the Web UI. [3383](https://github.com/keeps/roda/issues/3383)
- Added support for advanced search over nested items using Solr block join queries, enabling more precise queries across hierarchical metadata structures via new filter parameters: ParentWhichFilterParameter and ChildOfFilterParameter [3322](https://github.com/keeps/roda/issues/3322)
- Added support for external user group mapping by allowing administrators to define mappings between CAS attributes and RODA groups through configuration. User group membership is now resolved dynamically at login based on the external attribute (e.g. memberOf) and assigned to corresponding RODA groups [3499](https://github.com/keeps/roda/pull/3499)

#### Changes:
- Migrated all GWT-RPC interface methods to REST API, reducing dependency on GWT and aligning with modern web architecture practices. [2060](https://github.com/keeps/roda/issues/2060)
- Removed the sourceObjects field from the JobCollections index to prevent Solr overload caused by large identifier lists, improving system scalability and stability. Adjusted interface components to retrieve object data from the model instead of the index as needed.  [3307](https://github.com/keeps/roda/issues/3307)
- Added welcome pages for languages other than English and Portuguese, improving user onboarding for a wider audience. [7c506370f](https://github.com/keeps/roda/commit/7c506370f22fd598ecaa48f5b26714ca4e3dbb8e)
- Reviewed and updated pre-ingest text [3412](https://github.com/keeps/roda/pull/3412)
- Improve support for E-ARK SIP administrative metadata (amdSec) [3380](https://github.com/keeps/roda/issues/3380)
- Added detailed prompts and outcome tracking for lifting disposal holds, including preservation event generation via ModelService. Replaced liftDisposalHoldBySelectedItems API calls with dissociateDisposalHold for disposal hold removal.  [3235](https://github.com/keeps/roda/pull/3235)
- Added indexing support for technical metadata to improve searchability and metadata management. [0723959e](https://github.com/keeps/roda/commit/0723959e45f137fee982d67450058fc8e757426a)

#### Security:
- Several dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---
## v5.7.6 (2025-06-02)
#### Security
- Updated dependency of jaxb for glassfish

---

To try this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).

---

## v5.7.5 (2025-05-19)
#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).

---

## v5.7.4 (2025-04-29)
#### Enhancements
-  Improve support for E-ARK SIP administrative metadata (amdSec) #3380

#### Bug fixes
-  NPE when editing a user via profile #3405

#### Security
-  Several major dependency upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.7.3 (2025-04-03)
#### Security
-  Fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.7.2 (2025-03-24)
#### Bugs

- Disposal confirmation cancel button message #3303

#### Enhancements

- Missing translations for disposal rules order panel #3312

#### Security
- Several major dependency upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.7.1 (2025-01-08)
#### Bug fixes

- Fix built-in plugin "AIP ancestor hierarchy fix"
- Deleting linked DIPs now longer increments objects processed (#3285)

#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.7.0 (2024-09-05)
#### Security
- Several dependency major upgrades to fix security vulnerabilities
- Improve HTTP headers security

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.7.0-beta1 (2024-06-21)
#### New features 

- Replace Akka with Apache Pekko

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.6.5 (2024-06-07)
#### Bug fixes

- Roda fails to resolve other metadata with folders #3219

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.6.4 (2024-06-06)
#### Bug fixes

- Roda fails to reindex due to problem with other metadata files #3218

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.6.3 (2024-05-23)
#### Bug fixes

- Revert webjars-locator functionality

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.6.2 (2024-05-22)
#### Bug fixes

- Base roda overwrites the configuration regarding user permissions in roda-config.properties #3189

#### Security
- Dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.6.1 (2024-05-03)
#### Bug fixes

- Custom E-ARK SIP representation type not being set when ingesting a E-ARK SIP #3139

#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.6.0 (2024-04-04)
#### New features 

- Auto refresh after the session expires

#### Enhancements 

- Update representation information links

#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.5.3 (2024-03-13)
#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.5.2 (2024-03-11)
#### Bug fixes
- Fixed other metadata download #3117


#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).

---

## v5.5.1 (2024-03-08)
#### Bug fixes
- Remove "opt-in" from roda-core.properties #3113
- Fix ns2 namespace in premis.xml when creating technical metadata  #3114 

#### Security
- Several dependency major upgrades to fix security vulnerabilities


---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).

---

## v5.5.0 (2024-03-04)
#### New features
-  Support for generic technical metadata creation and visualization #3097

#### Bug fixes
- Fixed unexpected behaviour when trying to create a new AIP #3110
- Fixed AIP permissions calculation using ModelService #3105 

#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.4.0 (2024-02-08)
#### New features
-  Technological platforms major upgrade, which largely improves overall security, maintanability and performance #3055
-  Adding support for the latest version of the [E-ARK SIP specification](https://dilcis.eu/specifications/sip) (version 2.1.0) #3046
-  Support [trusting the your own plugins](https://github.com/keeps/roda/blob/master/documentation/Plugin_signing.md) #3059

#### Enhancements
-  Added help text to Agents register page that was missing #2831 
-  Added close button to license popup #2975
-  Improved documentation about default permissions #3045
- Other small improvements #3063

#### Bug fixes
-  Fixed "Clear" button in search component that did not behave as expected #3062
-  Fixed the Event Register menu entry that did not match the title of page #2832
-  Fixed Date and time of last transfer resource refresh in RODA interface only updated when reloading the page #3038 
-  Fixed default permissions issue when reading admin user permissions from configuration #3066 

#### Security
- Several dependency major upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.3.1 (2024-01-11)
#### Bug fixes:
- Changed default permissions to old behaviour #3043

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.3.0 (2023-12-14)
#### Enhancement:
- Added tool tip to plugin license verification panel #2974
#### New features:
- Added permissions configuration for newly created AIPs #3032
#### Bug fixes:
- Unable to perform actions even having right permissions #2986
- Ingest jobs created in RODA 4 cannot be accessed on the interface of RODA 5 #3037
- Problem using index REST API without filter #2962
#### Security:
- Several dependency upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.2.5 (2023-12-06)
#### Bug fixes:

- Error sending ingestion failure notification via email #3023 

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).
---

## v5.2.4 (2023-11-10)
#### Enhancements:

- Update Swedish translation language

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).


---

## v5.2.3 (2023-11-10)
#### What's new:

- New German (Austrian) translation of the Web interface :austria: 

#### Bug fixes:

- Create folder access-keys when initializing RODA for the first time #2992
- Add default representation type when creating a preservation action job #2990
- Edit button for selecting parent does not work as expected #2988
- EAD 2002 dissemination crosswalk duplicates record group level #2987

#### Enhancements:

- Add title attribute to improve accessibility #2989

#### Security:

- Bump several dependencies

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).


---

## v5.2.2 (2023-10-04)
#### Bug fixes:
- Fixed FileID when it is encoded #2963
- Fixed API filter issue #2965

#### Security:
- Several dependency upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://www.roda-community.org/deploys/standalone/).


---

## v5.2.1 (2023-09-08)
#### Bug fixes:
- Listing RODA objects via REST-API is not showing any results #2935
- Preservation events page is not showing no events #2928
- REST API endpoint to retrieve the last transferred resource report does not show the reports #2929
- Problem with pre-filter not being reset when searching preservation events #2941

#### Security:
- Several dependency upgrades to fix security vulnerabilities

---

To try out this version, check the [install instructions](https://www.roda-community.org/deploys/standalone/).


---

## v5.2.0 (2023-07-28)
#### Enhancements:
- DIP must be deleted if it no longer contains any link with any entity. #2863
- Ingest job report could expose if SIP is update #2212

#### Bug fixes:
- Unexpected behaviour can cause index to be completely deleted #2921

#### Security:
- Several dependency upgrades to fix security vulnerabilities
- Remove python from Docker image

---

To try out this version, check the [install instructions](https://www.roda-community.org/deploys/standalone/).


---

## v5.1.0 (2023-06-20)
#### New features:

- Added property to differentiate environments #2676 
- Added link to RODA Marketplace in Menu #2722
- Added links to additional features #2723
- Added marketplace text to welcome page #2724
- Option to enable AIP locking when editing descriptive metadata #2672
- Preview functionality in disposal rules with AIPs affected by #2664 

#### Enhancements:
- Reduce indexed information of the entities that spend much of the index #2058
- Partial updates are not affecting the updatedOn field #2851
- Updated the banner #2725

#### Bug fixes:
- Minimal ingest plugin is using E-ARK SIP 1.0 as SIP format instead of E-ARK SIP 2.0.4 #2736 
- Could not resolve type id 'AndFiltersParameters' #2809
- Access token can only be created if RODA is instantiated as CENTRAL #2881
- Saved search for files associated to a representation information not working properly #2671 

#### Security:
- Remove xml-beans dependency #2726 

---

To try out this version, check the [install instructions](https://www.roda-community.org/deploys/standalone/).


---

## v4.5.6 (2023-05-04)
#### Bug fixes:

- Option to disable user registration on server-side #2840 

Install for demonstration:
```
docker pull ghcr.io/keeps/roda:v4.5.6
```
---

## v5.1.0-RC (2023-04-17)

---

## v4.5.5 (2023-03-16)
#### Dependencies upgrade:
- Bump commons-ip version from 2.3.0 to 2.3.2 


Install for demonstration:
```
docker pull ghcr.io/keeps/roda:v4.5.5
```
---

## v5.0.0 (2023-03-13)
### :warning: Breaking Changes
RODA  5.X will use Apache Solr 9 as indexing system. If you have an existing RODA implementation with Solr 8 you will need to [upgrade the Solr to version 9](https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-9.html) and then rebuild all indexes on RODA.

RODA 5.X docker now runs as the user roda (uid: 1000, gid: 1000) to improve security. This may affect you current implementation as it may lack enough permissions to access the storage. To fix, change the owner or permissions of the files and directories in the mapped volumes or binded folders. Alternatively, you can [change the RODA user uid](https://docs.docker.com/compose/compose-file/#user) in docker compose. 

---

#### New features:

- Distributed Digital Preservation #1933 #1934 #1935
- Added authentication via Access Token (for REST-API)
- Support binaries as a reference (shallow SIP/AIP) #786
- Adds list of all available plugins (see [RODA Marketplace](https://market.roda-community.org/)) #2323
- Supports verified plugins #2323
- New Swedish translation of the Web interface :sweden:
- Updates to Hungarian translation of the Web interface :hungary:

#### Changes:

- Upgraded from Java 8 to Java 17
- Upgraded from Apache Solr 8 to Apache Solr 9
- Upgraded from Apache Tomcat 8.5 to Apache Tomcat 9

#### Security:

- RODA docker now runs as roda (uid: 1000) instead of root
- (Applicational) Users can now have JWT access tokens to access the REST-API
- Option to restrict user web authentication to delegated (CAS) or JWT access tokens
- Several dependency upgrades to fix security vulnerabilities
- CVE-2016-1000027 (spring-web 5.3.24): RODA does not use the HTTPInvokerServiceExporter or RemoteInvocationSerializingExporter classes, therefore we are [NOT affected](https://github.com/spring-projects/spring-framework/issues/24434#issuecomment-744519525) by this vulnerability 
- CVE-2022-1471 (snake-yaml 1.33): RODA does not use [empty constructor](https://snyk.io/blog/unsafe-deserialization-snakeyaml-java-cve-2022-1471/) so we are NOT affected by this vulnerability.

---

We would like to thank the contributions of:
- [WhiteRed](https://www.whitered.se/) with the Swedish translation :sweden:
- Panka Dióssy from the [National Laboratory for Digital Heritage](https://dh-lab.hu/), with updates to the Hungarian translation :hungary:

---

To try out this version, check the [install instructions](https://github.com/keeps/roda/blob/master/deploys/standalone/README.md).


---

## v4.5.4 (2023-01-27)
#### Enhancements:

- Add metric per percentage of retries #2299

Install for demonstration:
```
docker pull keeps/roda:v4.5.4
```
---

## v4.5.3 (2023-01-25)
#### Bug fixes:

- Support very large queries to Solr (fix regression) #2311

#### Enhancements:

- Add icon to experimental plugin categories #2306

Install for demonstration:
```
docker pull keeps/roda:v4.5.3
```
---

## v4.5.2 (2023-01-19)
#### Bug fixes:

- Failsafe fallback policy misconfigured #2303

Install for demonstration:
```
docker pull keeps/roda:v4.5.2
```
---

## v4.5.1 (2023-01-16)
#### Enhancements:

- Refactor RetryPolicyBuilder #2296
- Improve log information during initialization process #2297
- Add metrics about retries (related to RetryPolicyBuilder) #2298

Install for demonstration:
```
docker pull keeps/roda:v4.5.1
```
