# ETERNA Changelog
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
