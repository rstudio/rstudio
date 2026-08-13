# Linux Desktop E2E engine configuration

The reusable workflow `.github/workflows/os-test-e2e-rstudio-desktop-linux.yml`
runs the Playwright E2E suite against RStudio Desktop on any supported Linux
target. Each target ("engine") is described by one JSON config file in this
directory plus, where needed, a small set of shell hooks under
`<script_dir>/`:

    .github/e2e-linux/
    ├── <distro>-<ver>-<arch>.json   # scalar knobs: runner, image, cache keys, ...
    └── <script_dir>/                # distro-specific setup hooks (script_dir key)
        ├── build-setup.sh    # after checkout, before the dependency install
        ├── e2e-setup.sh      # runtime packages, repos, sysctls, locale
        ├── install-r.sh      # replaces the rig install (r_install: "distro")
        └── post-r-setup.sh   # after R is installed, before R package installs

All hooks are optional; the workflow skips any that don't exist. Hooks that
need to persist environment variables for later steps write to `$GITHUB_ENV` /
`$GITHUB_PATH` as usual.

Every engine name ends in its architecture (`ubuntu-24-x86_64`,
`rocky-10-arm64`), so the arch is visible everywhere the name surfaces: the
dispatch picker, job names, and artifact names. The Playwright project label is
not one of those places -- it is built from `pw_label`, not from the engine
name (see below).
Hook directories are *not* arch-suffixed -- they're shared by whichever engines
need the same distro setup (`script_dir`).

## Adding a new engine

1. Copy the closest existing config (Ubuntu-family: `ubuntu-24-x86_64.json`,
   RHEL-family: `rocky-9-x86_64.json`) and adjust the knobs. Name it
   `<distro>-<ver>-<arch>.json`. Check the three build cache keys you inherit:
   `ubuntu-24-x86_64.json`'s keys carry only the architecture. All three are
   shared with the Debian x86_64 engine; only `tools_cache_key` and
   `rlibs_cache_key` are also shared with the Linux Server build (its GWT
   cache key is separate). Sharing is only correct for an engine that also
   builds bare on the Ubuntu 24 runner (see Architecture below).
2. Add setup hooks under a new script dir if the distro needs packages or
   quirks the shared dirs don't cover; otherwise point `script_dir` at an
   existing one (e.g. `fedora-43` and `fedora-44` share `fedora/`).
3. Wire the engine into the callers you want: the scheduled rotation
   (`os-test-e2e-rstudio-scheduled.yml` -- add it to the `ENGINES` array, which
   puts it in its platform/arch group's Sunday run (Linux x86_64, Linux arm64,
   or macOS+Windows), and optionally to a weekday slate), the PR run
   (`os-test-e2e-rstudio-pr.yml`), and/or the certification run
   (`.github/e2e-certification/matrix.json` -- see that directory's README;
   an engine absent from that file cannot be certified at all).
4. Add it to the `os` choice list in the workflow's `workflow_dispatch`
   inputs, so it can be run by hand.

## R versions

Which R an engine runs against is a caller's choice, not a property of the
engine config -- with one exception, which is the config's business:

- `r_install: "rig"` (12 of the 16 Linux Desktop engines documented here) honors
  the caller's `r_version` input, passing it to `rig add` / `rig default`
  verbatim.
- `r_install: "distro"` (the four Fedora engines) **ignores `r_version`
  entirely** and installs whatever R the distro's repos ship, via
  `fedora/install-r.sh`. Nothing warns about the dropped value, so a caller
  that pins a version for one of these engines gets a different R than it asked
  for. `.github/actions/os-e2e-deps` reads the installed R back and writes it to
  the job summary; reading it back is the only trustworthy *method*, because the
  requested version is not a record of anything.

Counts in this file refer to the 16 Linux Desktop engines it documents. The
rotation's `ENGINES` array counts 21, adding Linux Server, three macOS engines,
and Windows.

`os-e2e-deps` also folds the *resolved* R major.minor into the R-library and pak
cache keys, with version-scoped `restore-keys`. Two R versions on one engine
therefore get separate cache entries and cannot poison each other; two patch
levels within a minor series (4.5.1 and 4.5.3) share one entry, which is safe
because package ABI is stable within a series.

## Architecture

Architecture is not a second selection axis: every engine config is
single-arch, and the arch it runs on is spelled out in `tools_arch`, the runner
labels, all three build cache keys, `sccache_key_prefix`, `installer_artifact`,
`daily_platform_key`, and `e2e_deps_cache_scope`. Keeping the arch literal in
each of those (rather than templating it in) is what stops two engines of
different arch from ever sharing a cache entry or an artifact name. `pw_label`
follows the same convention on new engines, with older exceptions noted below.
(`daily_platform_key` is the exception to the "unique per engine" reading: it is
a lookup into the dailies manifest, not a cache or artifact name, so engines of
the same arch correctly share one -- both Fedora engines and `rocky-10-arm64`
all install `rhel10-arm64`, and both Debian x86_64 and Ubuntu 24 x86_64 install
`noble-amd64`.)

New engines spell `pw_label` as `linux-<distro><version>-<arch>`. Five older
values predate that convention and are deliberately left alone:

| Config | `pw_label` | Missing |
|---|---|---|
| `ubuntu-24-x86_64` | `linux` | version and arch |
| `fedora-44-x86_64` | `linux-fedora` | version and arch |
| `fedora-43-x86_64` | `linux-fedora43` | arch |
| `debian-13-arm64` | `linux-debian-arm64` | version |
| `rocky-10-arm64` | `linux-rocky-arm64` | version |

The label is the only per-engine discriminator that reaches the Test Insights
dashboard, so renaming one breaks the continuity of its series there -- which is
why the inconsistency with their newer siblings is a deliberate freeze rather
than an oversight, and why normalising them belongs in a change of its own.

To run an existing distro on a second architecture, add a *new* engine and make
sure nothing it names can collide with the engine of the other architecture (the
list of what must stay distinct is below); the only workflow change is adding it
to the `os` choice list for hand dispatches. `ubuntu-24-arm64.json` shows the
runner side of it: same bare-runner shape as `ubuntu-24-x86_64.json`, on
`ubuntu-24.04-arm`. Its cache keys are not the model to copy, though -- the three
build cache keys carry only the architecture (`rstudio-tools-linux-arm64`), with
no distro or version, and are shared with `debian-13-arm64` deliberately, for the
reason two paragraphs down. `rocky-9-arm64.json` is what to copy when the engine
needs its own pool: every key is scoped to distro, version and arch
(`rstudio-tools-linux-rocky9-arm64`).

Because every name ends in its arch, no engine name is a prefix of another --
which matters beyond readability: the merge job collects shard blobs with the
glob `playwright-blob-report-linux-desktop-<os>-*`, so a bare `ubuntu-24` would
also match `ubuntu-24-arm64`'s blobs and silently merge foreign results into the
x86_64 report.

Each Debian engine shares `tools_cache_key` / `rlibs_cache_key` /
`gwt_cache_key` with the Ubuntu 24 engine of the same arch -- `debian-13-arm64`
with `ubuntu-24-arm64`, `debian-13-x86_64` with `ubuntu-24-x86_64` -- which is
not an oversight: those engines also *build* bare on the Ubuntu 24 runner with
`install-dependencies-noble` (only their tests run in a container), so the
cached artifacts are ABI-identical and the new engine starts warm. On x86_64
the `tools_cache_key` / `rlibs_cache_key` pool is three deep, because those
two `ubuntu-24-x86_64` keys are also shared with the Linux Server build and
its cache seed -- `gwt_cache_key` is not; Server keys its GWT cache
separately (see the config-keys table).
What must stay arch-distinct is anything that would
otherwise collide with the engine of the other architecture:
`sccache_key_prefix`,
`installer_artifact`, `pw_label`, and especially `e2e_deps_cache_scope` -- an
empty scope means `runner.os` alone (`Linux`), so any two engines that left it
empty would hand each other ABI-incompatible R libraries. Every engine sets an
explicit scope; this matters more now that the cached library carries the full
compiled REQUIRED_PACKAGES set, not just pak's DESCRIPTION deps.

Every engine config is wired into the scheduled rotation, and the `ENGINES`
array in `os-test-e2e-rstudio-scheduled.yml` is the authoritative list of what
runs on a schedule. It names all 21 engines, each tagged with the platform and
architecture it runs on, and three Sunday crons split the fleet into Linux
x86_64, Linux arm64, and macOS+Windows (any arch) -- so adding a config to
`ENGINES` is what puts it on a schedule at all. The weekday slates reach only
a subset, so consult `ENGINES` rather than assuming an engine runs on a given
day because a config exists.

Callers pass an `arch` input naming the architecture they expect the engine to
run on; the workflow compares it against the config's `tools_arch` and fails
the run on a mismatch. It's an assertion, not a selector -- `auto` (the
dispatch default) accepts whatever the config declares. The point is that
re-pointing an engine at a different runner can't silently leave the
arch-bearing cache keys behind.

`arch` is a required `workflow_call` input, and an empty value is an error
rather than a skip: `required: true` alone only rejects an absent key, so a
caller passing an expression that resolves to nothing (a matrix entry with no
`arch`) would otherwise disable the assertion without saying so. Pass `auto` to
opt out on purpose.

The setup job also enforces the two conventions the rest of the workflow relies
on: the engine name must end in `-<tools_arch>` (see the blob-glob note above),
and every config value must be a single-line string, since they are republished
as `key=value` job outputs.

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
| `tools_cache_key`, `rlibs_cache_key`, `gwt_cache_key` | Actions cache key prefixes; per-distro/arch isolation. The ubuntu-24-x86_64 `tools_cache_key` and `rlibs_cache_key` are deliberately shared with the Linux Server build and its cache seed; `gwt_cache_key` is not -- Server keys its GWT cache separately |
| `sccache_key_prefix` | S3 key prefix for the shared sccache bucket |
| `installer_artifact` | name of the built installer artifact passed from build to e2e |
| `daily_platform_key` | key under `products.electron.platforms` in the dailies manifest |
| `e2e_runner`, `e2e_image`, `e2e_container_options`, `e2e_bootstrap_packages` | e2e job placement (same semantics as the build_ variants) |
| `r_install` | `rig` (default path) or `distro` (runs `install-r.sh`) |
| `run_as_user` | non-root user the test run drops to via setpriv (container engines); empty = run as the step user |
| `display_server` | `xvfb` or `cage` (RHEL 10 dropped X.Org, so Xwayland-under-Cage provides the display) |
| `e2e_deps_cache_scope` | os-e2e-deps cache isolation scope (R libraries are ABI-bound to the distro/arch) |
| `preinstall_r_packages` | `true` to pre-install the harness's full REQUIRED_PACKAGES set into the cached R library and point globalSetup at it. Every engine sets it: an engine that leaves it empty pays globalSetup's install into an uncached library on every run |
| `heartbeat_timeout_seconds` | PW heartbeat idle ceiling; raised where R packages may source-compile at test time |
| `pw_label` | suffix for PW_PROJECT_LABEL / SHARD_NAME (report + dashboard continuity; do not rename casually) |
