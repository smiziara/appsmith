# AGENTS.md

## Cursor Cloud specific instructions

### Architecture overview

Appsmith is a low-code platform with three main services:

| Service | Path | Tech | Port |
|---------|------|------|------|
| Client (frontend) | `app/client` | React/TS, Yarn 3.5.1 workspaces | 3000 (dev), proxied via nginx on 443 |
| Server (backend) | `app/server` | Java 17, Spring Boot 3.3, Maven | 8080 |
| RTS (real-time) | `app/client/packages/rts` | Node.js 20.11.1, Express | 8091 |

Infrastructure dependencies: **MongoDB** (with replica set `rs0`) on port 27017, **Redis** on port 6379 (both via Docker).

### Running services

See `contributions/ClientSetup.md` and `contributions/ServerSetup.md` for full docs. Key caveats:

- **Java 17 is required.** The build will fail with any other major version. Ensure `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` and `$JAVA_HOME/bin` is on `PATH`.
- **RTS must be started before the Java server.** The backend performs an RTS health check on startup.
- **Nginx HTTPS proxy** requires `sudo` when binding to ports 80/443. Run `sudo nginx -c /workspace/app/client/nginx/nginx.dev.conf` after generating the config via `start-https.sh`.
- **`/appsmith-stacks/configuration/docker.env`** must exist (even if empty) for the admin setup endpoint to work. Create it with `sudo mkdir -p /appsmith-stacks/configuration && sudo touch /appsmith-stacks/configuration/docker.env && sudo chown -R $USER:$USER /appsmith-stacks`.
- **MongoDB** must run with `--replSet rs0` and be initialized with `rs.initiate()`.

### Starting services (order matters)

1. Docker containers: `docker start appsmith-mongodb appsmith-redis`
2. RTS: `cd app/client/packages/rts && source .env && node --require source-map-support/register dist/bundle/server.js`
3. Server: `cd app/server/dist && source ../. env && java -jar server-*.jar`
4. Nginx: `sudo nginx -c /workspace/app/client/nginx/nginx.dev.conf`
5. Client: `cd app/client && yarn start`

### Lint / Test / Build commands

| Scope | Command | Notes |
|-------|---------|-------|
| Client lint | `cd app/client && yarn lint` | ESLint; warnings are expected |
| Client unit tests | `cd app/client && yarn run test:unit` | Jest; 466 suites, ~20 min |
| Client prettier | `cd app/client && yarn prettier` | |
| Server spotless | `cd app/server && mvn spotless:check` | Java formatting |
| Server tests | `cd app/server && mvn clean package` | Requires Docker for testcontainers |
| Server build | `cd app/server && mvn package -DskipTests && mkdir -p dist/plugins && cp appsmith-server/target/server-*.jar dist/` | |
| RTS build | `cd app/client/packages/rts && ./build.sh` | |

### Gotchas

- `yarn install` in `app/client` uses Yarn 3.5.1 via corepack. Make sure `corepack enable` has been run.
- The client dev server (`yarn start`) takes ~3 minutes to compile on first run.
- The server `build.sh` checks for Java 17 in Maven's output and exits if a different version is found.
- `start-https.sh` has an interactive prompt when ports 80/443 are in use; kill the conflicting process before running non-interactively.
- `fs.inotify.max_user_watches` should be set to 524288 on Linux for the client dev server to work.
