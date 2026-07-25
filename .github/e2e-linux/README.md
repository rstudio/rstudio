# Linux Desktop E2E engine configuration

The reusable workflow `.github/workflows/os-test-e2e-rstudio-desktop-linux.yml`
runs the Playwright E2E suite against RStudio Desktop on any supported Linux
target. Each target ("engine") is described by one JSON config file in this
directory plus, where needed, a small set of shell hooks under
`<script_dir>/`:

    .github/e2e-linux/
    ├── <os>.json          # scalar knobs: runner, image, cache keys, slugs, ...
    └── <script_dir>/      # distro-specific setup hooks (script_dir key)
        ├── build-setup.sh    # after checkout, before the dependency install
        ├── e2e-setup.sh      # runtime packages, repos, sysctls, locale
        ├── install-r.sh      # replaces the rig install (r_install: "distro")
        └── post-r-setup.sh   # after R is installed, before R package installs

All hooks are optional; the workflow skips any that don't exist. Hooks that
need to persist environment variables for later steps write to `$GITHUB_ENV` /
`$GITHUB_PATH` as usual.

## Adding a new engine

1. Copy the closest existing `<os>.json` (Ubuntu-family: `ubuntu-24.json`,
   RHEL-family: `rocky-9.json`) and adjust the knobs.
2. Add setup hooks under a new script dir if the distro needs packages or
   quirks the shared dirs don't cover; otherwise point `script_dir` at an
   existing one (e.g. `fedora-43` and `fedora-44` share `fedora/`).
3. Wire the engine into the callers you want: the scheduled rotation
   (`os-test-e2e-rstudio-scheduled.yml` -- add it to the engine table and a
   weekday slate) and/or the PR run (`os-test-e2e-rstudio-pr.yml`).

## Config keys

| Key | Meaning |
| --- | --- |
| `pretty_name`, `emoji`, `runner_note` | PR-comment presentation only |
| `package_format` / `package_ext` | `DEB`/`deb` or `RPM`/`rpm`; selects the make-electron-package target and build dir |
| `package_manager` | `apt` or `dnf`; drives container bootstrap and installer installs |
| `script_dir` | directory under `.github/e2e-linux/` holding this engine's hooks |
| `build_runner`, `build_image`, `build_container_options` | build job placement; empty image = run directly on the runner |
| `build_bootstrap_packages` | packages installed in the container before checkout (git, tar, ...) |
| `build_timeout_minutes` | build job timeout (engines with no seeded cache build fully cold) |
| `tools_arch` | RSTUDIO_TOOLS_ROOT subdirectory (`x86_64` / `arm64`) |
| `dependency_script` | script under `dependencies/linux` to run |
| `tools_cache_key`, `rlibs_cache_key`, `gwt_cache_key` | Actions cache key prefixes; per-distro/arch isolation, and the ubuntu-24 keys are deliberately shared with the Linux Server build and its cache seed |
| `sccache_key_prefix` | S3 key prefix for the shared sccache bucket |
| `installer_artifact` | name of the built installer artifact passed from build to e2e |
| `daily_platform_key` | key under `products.electron.platforms` in the dailies manifest |
| `e2e_runner`, `e2e_image`, `e2e_container_options`, `e2e_bootstrap_packages` | e2e job placement (same semantics as the build_ variants) |
| `r_install` | `rig` (default path) or `distro` (runs `install-r.sh`) |
| `run_as_user` | non-root user the test run drops to via setpriv (container engines); empty = run as the step user |
| `display_server` | `xvfb` or `cage` (RHEL 10 dropped X.Org, so Xwayland-under-Cage provides the display) |
| `e2e_deps_cache_scope` | os-e2e-deps cache isolation scope (R libraries are ABI-bound to the distro/arch) |
| `preinstall_r_packages` | `true` to pre-install the harness's full REQUIRED_PACKAGES set (engines without PPM binary coverage) and point globalSetup at that library |
| `heartbeat_timeout_seconds` | PW heartbeat idle ceiling; raised where R packages may source-compile at test time |
| `pw_label` | suffix for PW_PROJECT_LABEL / SHARD_NAME (report + dashboard continuity; do not rename casually) |
