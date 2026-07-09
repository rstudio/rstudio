## RStudio 2026.08.0 "Yellow Yarrow" Release Notes

### New
- ([#18122](https://github.com/rstudio/rstudio/issues/18122)): Add `rserver --setup-db` (and the `rstudio-server setup-db` admin subcommand) to create Workbench's PostgreSQL database, service user, and PostgreSQL 15+-safe grants without hand-running SQL. Prompts for the master database host, port, username, and password (the password can also come from `--master-password-file <path>` or the `RSERVER_SETUP_DB_MASTER_PASSWORD` environment variable), then writes the resulting connection settings into `database.conf`. Safe to re-run against an already-set-up database: if the service user already exists, the existing `database.conf`/credentials file is left untouched rather than overwritten with a newly generated password. `--print-only` writes the credentials to a standalone file instead of `database.conf`, and `--show-password` prints the generated password to stdout; both are off by default. In Posit Workbench builds, `--setup-db` also creates the configured audit database and service user, reusing the same master connection so the master password is only prompted for once per run.

### Fixed

### Dependencies
- Ace 1.43.5
- Copilot Language Server 1.509.1
- Electron 41.9.0
- Node.js 22.22.2 (copilot, Posit Assistant)
- Quarto 1.9.38
- xterm.js 6.0.0
