# Full-service deployment example

This is a full-service ETERNA deployment example including a Traefik reverse proxy with TLS termination, ZooKeeper, Solr in cloud mode, ClamAV, Siegfried, OpenLDAP, and Unoserver. Optional services (CAS SSO, Swagger UI, SFTP drop folder) are included as commented-out blocks in the compose file.

This example is intended as a starting point. Every production environment should be adapted to its specific requirements: data volume, infrastructure, availability needs, and integrations. For professional support see [WhiteRed ETERNA](https://www.whitered.se/eterna).

## Prerequisites

- Linux (Windows and macOS are not supported for production use)
- Docker and Docker Compose: [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/)
- A DNS A record pointing your hostname to this server
- Port 80 and 443 open on the server firewall

## Configuration files

The compose file expects a `./config/` directory with site-specific files. Create the layout before starting services:

```sh
mkdir -p ./data/storage ./data/log
mkdir -p ./integration/drop ./integration/transfer ./integration/reports
mkdir -p ./config/theme ./config/ldap ./config/plugins ./config/traefik
```

Minimum required files:

| Path | Description |
|------|-------------|
| `config/roda-core.properties` | ETERNA core configuration |
| `config/roda-wui.properties` | ETERNA web UI configuration |
| `config/ldap/pbkdf2.ldif` | PBKDF2 schema extension for OpenLDAP |
| `config/ldap/groups.ldif` | Initial LDAP groups |
| `config/ldap/users.ldif` | Initial LDAP users |
| `config/clamd.conf` | ClamAV client configuration |
| `config/traefik/config.yaml` | Traefik static/dynamic configuration |

The `config/theme/` and `config/plugins/` directories may be empty if you do not have custom themes or plugins.

## Quick start

```sh
# 1. Copy and edit the environment file
cp .env.example .env
$EDITOR .env   # set ETERNA_HOST, secrets, SMTP, and paths

# 2. Prepare data and config directories (see above)

# 3. Start all core services
docker compose up -d

# 4. Follow logs during first startup (may take a few minutes)
docker compose logs -f --tail=100
```

ETERNA will be available at `https://${ETERNA_HOST}` once healthy.
Default credentials: **admin / eterna** (change immediately after first login).

## TLS certificates

The default compose file expects you to manage TLS certificates manually. Place your certificate and key where Traefik can find them and reference them in `config/traefik/config.yaml`.

To use Let's Encrypt / ACME instead, uncomment the `certresolver` lines in `docker-compose.yaml` and set `ACME_EMAIL` in your `.env`.

## Enabling optional services

### CAS SSO

1. Create `config/cas/` with your CAS configuration files (`cas.properties`, `services/service-1.json`, `thekeystore`).
2. Uncomment the `cas` service block in `docker-compose.yaml`.
3. In the `eterna` service environment, uncomment the `RODA_SERVICES_CAS_ACTIVE` and `CAS_*` variables.

### Swagger UI / API docs

Uncomment the `docs-api` service block in `docker-compose.yaml`. The API documentation will be available at `https://${ETERNA_HOST}/api-docs/`.

### SFTP drop folder

1. Create `config/sftp/sftp-users.conf` following the [atmoz/sftp](https://github.com/atmoz/sftp) format.
2. Uncomment the `sftp` service block in `docker-compose.yaml`.
3. Set `RODA_SERVICES_DROPFOLDER_ACTIVE=true` in the `eterna` service environment.
4. Set `SFTP_USERNAME` in your `.env`.

## Stopping services

```sh
docker compose down
```

Data in `./data/` and `./integration/` is preserved on the host. Named volumes (Solr, ZooKeeper, ClamAV, Siegfried, OpenLDAP) are preserved in Docker volume storage. To remove volumes as well:

```sh
docker compose down -v
```

## Need help?

Community issues and discussions: [https://github.com/ETERNA-earkiv/ETERNA](https://github.com/ETERNA-earkiv/ETERNA)

Professional support (installation, maintenance, training, migration): [WhiteRed ETERNA](https://www.whitered.se/eterna)
