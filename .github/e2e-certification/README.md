# E2E certification matrix

`matrix.json` is the certification scope: which environments RStudio is
certified on, and which version of R each one is certified against. It is read
by `.github/workflows/os-test-e2e-rstudio-certification.yml`, which dispatches
one E2E engine per selected cell, handing each its own R version.

This file exists so the certification scope is a reviewable document rather than
a value buried in workflow YAML. A change to what Posit certifies shows up as a
one-file diff.

## Why this is separate from the scheduled rotation

The scheduled rotation (`os-test-e2e-rstudio-scheduled.yml`) answers "did
anything regress this week" and passes one hardcoded R version to every engine.
Certification answers "which OS and R combinations does this build work on", so
R has to vary per environment. The two have different selection rules and
different reporting needs, which is why they are separate workflows.

The routing fields below (`platform`, `arch`, `os`, `name`, `url_key`) duplicate
the rotation's `ENGINES` array deliberately -- the two lists answer different
questions and change for different reasons. Unifying them is a possible
follow-up, not a bug.

## Schema

One entry per engine. All 21 engines are listed; `default` decides which of them
a plain dispatch runs.

| Field | Required | Meaning |
|---|---|---|
| `cell` | always | Selection name, matching the rotation's engine `key`. What you type into the `cells` and `r_overrides` dispatch inputs. |
| `platform` | always | `linux`, `macos`, or `windows`. Documentation only in this file -- nothing in the certification workflow reads it, which routes Linux cells by the presence of `os` instead. It is functional in the rotation's `ENGINES`, where it drives the Sunday grouping. |
| `arch` | always | `x86_64` or `arm64`. Passed through and asserted for Linux cells only: that workflow fails the run if it disagrees with the engine config's `tools_arch`. On the other five it is documentation. |
| `r_version` | always | The R version this cell certifies against, or the sentinel `distro` (see below). |
| `default` | always | `true` to include the cell in a plain dispatch; `false` to make it selectable by name only. |
| `os` | Linux Desktop only | Names the `.github/e2e-linux/<os>.json` engine config. Its presence is what marks a cell as Linux Desktop. |
| `name` | Linux Desktop only | Job label in the run's job list. |
| `url_key` | Linux Desktop only | Which `os-resolve-daily-urls` output holds this engine's installer URL. Must agree with the engine config's `daily_platform_key`, with `-` written as `_`. |
| `r_version_actual_note` | optional | Informational only. Records the observed R for a `distro` cell, with a date. Nothing reads it. |

The five non-Linux cells (`ubuntu24s`, `macos14`, `macos15`, `macos26`,
`windows2025`) need only the always-required fields: each has its own dedicated
job in the certification workflow, guarded on the selection, rather than being
built into a matrix.

## Where R comes from

Every `r_version` in `matrix.json` is an exact `X.Y.Z`, and the engines install
it from the RStudio Build Tools (`rstudio-buildtools`) S3 bucket rather than
resolving it at job time: `.github/actions/os-install-r-unix` and
`os-install-r-windows` download
`R/<version>/R-<version>-<platform>.<ext>` and install that. There is no
resolver behind the mirror, which is why aliases like `release` and `oldrel` are
not accepted here or in the engine workflows' `r_version` inputs.

Cells are the only place that names a version explicitly. Every other lane
passes nothing and takes `RSTUDIO_R_VERSION` from
`dependencies/tools/rstudio-tools.sh`, so the day-to-day pin lives in exactly
one line; a cell here overrides it deliberately, to certify against something
older.

Certifying against a version that has not been mirrored yet fails the engine at
its R install step. Mirror it first, from a checkout with AWS credentials:

    dependencies/tools/upload-r.sh 4.7.0

That copies the official CRAN and Posit r-builds installers for every platform
CI runs on; pass platform names to narrow it. Then pin the version -- in
`matrix.json` for a certification cell, or in the `r_version` default of the
engine workflow for everything else.

## The `distro` sentinel

The four Fedora engines set `r_install: "distro"` in their engine config, which
means they install R from Fedora's own repos and ignore the `r_version` input
entirely (see `.github/e2e-linux/fedora/install-r.sh`). Declaring `"r_version":
"distro"` records that honestly: the cell certifies against whatever R the distro
ships, which is a real certification target, just not a chosen one.

The workflow passes a real mirrored version to those engines rather than the
literal `distro`, since the value is unused -- and would break the download
outright if a config ever switched to `r_install: "mirror"`. What the run
actually used is read back from the installed R and reported, so the sentinel
never has to be trusted.

## Changing the matrix

- **Change what an environment certifies against:** edit its `r_version`.
- **Add or drop an environment from the default run:** flip its `default`.
- **Certify a one-off combination without committing anything:** use the
  workflow's `cells` and `r_overrides` dispatch inputs.
- **Add a new engine:** add an entry here once the engine exists (see
  `.github/e2e-linux/README.md`). An engine absent from this file cannot be
  certified even by name.

Both dispatch inputs validate every name against this file and fail the run on an
unknown one, before any *engine* runner is provisioned -- the validation itself
runs in the `pick` job. The file's own invariants are checked there too: a
duplicate `cell` or `os`, and a `url_key` the resolve action doesn't emit, both
fail the run rather than surfacing later as a collided artifact name or a silently
empty installer URL.

## Pinning a version

`version` takes a build exactly as shown on dailies.rstudio.com. Two things to
know:

- **The format matters.** `2026.08.0+187` and `2026.08.0-daily+186` are both real
  forms; some platforms' filenames carry the `daily` token and some don't, and the
  resolver swaps the requested string into each platform's own URL. If a pin
  fails, check the string against the manifest before assuming the build is gone.
- **Only the most recent dailies survive.** Older builds are removed from the
  download host, so a pin from more than a few days back fails the HEAD check for
  every platform. That failure is loud and lands in `resolve`, before any engine
  starts.

The resolver attempts all 13 platform URLs regardless of which cells are
selected, but resolution is best-effort: platform builds fail independently, so a
platform nobody asked about warns rather than failing the run. What fails the run
is a *selected* cell whose URL came back empty, which the `resolve` job's "Verify
a pinned version resolved for every selected cell" step checks before any engine
starts. So a Linux-only run is not held up by a missing Windows build, and a pin
that resolved for nothing still fails loudly.

## What a run reports

The summary job writes a table of environment, R requested, R actually used, and
result. "R actually used" is read back from the installed R, never echoed from the
request, so a `distro` cell shows the concrete version `dnf` supplied and a cell
whose R silently failed to change shows what really ran.

Two routes carry the per-cell facts, because one alone can't cover both shapes:

- The five non-Linux engines each return the version as a `workflow_call` output,
  and their result is their own job's result.
- The Linux Desktop cells run as a single matrix call, and a matrix job collapses
  to one output value for the whole matrix -- so each uploads two artifacts,
  `r-version-linux-desktop-<os>` holding the R it installed and
  `r-outcome-linux-desktop-<os>` holding its own result, which the summary
  attributes per cell. This is what stops one failing cell from being reported
  against every other cell in the matrix. Two artifacts from two jobs, because the
  version is known inside a shard (where R is installed) while the result is only
  known downstream of the whole shard matrix: a cell whose second shard failed has
  failed, and no value available inside shard 1 can say so.

The two columns answer different questions, and the Result column is the one to
trust. It is also not quite the same fact for every cell: for a Linux cell it is
the *test* result (the shard matrix's own outcome), while for the five job-backed
cells it is the whole workflow call, build and report-merge included. So a Linux
cell whose tests passed but whose merge job failed reads `success` on a red run.

A Linux cell with no usable result artifact is the exception, and it says so in
the cell rather than looking measured. The workflow falls back to the matrix-wide
result and appends why -- either `per-cell results unavailable`, when nothing
arrived and the summary is the likely reason, or `this cell reported no result`,
when other cells did arrive and this one did not. The distinction is drawn by
counting the artifacts that arrived, not from the download step's status: a
download whose pattern matches nothing reports success, so an expired artifact on
a re-run and a skipped merge job both look like a clean fetch.

Four distinguishable gaps in the "R actually used" column:

- `_not reached_` -- no evidence the cell installed R. On a Linux cell that means
  other cells' version artifacts arrived and this one's did not: normally because
  it never got that far, but also if shard 1 specifically died ahead of the R
  install while another shard ran, since only shard 1 uploads it. On one of the
  five job-backed cells it means the job was skipped or cancelled, so no shard ran.
- `_summary could not fetch_` -- the version artifact download itself errored. The
  cells may well have been fine. Unlike the Result column above, this label is not
  given when the download merely found nothing: no version artifact at all is a
  plausible real outcome, since a cell that dies after setup but before the R
  install uploads a result and no version, so that case stays `_not reached_`.
- `_empty_` -- a Linux cell's version artifact exists but holds nothing.
- `_shard output collapsed_` -- one of the five job-backed cells ran and returned
  no version. Usually its output collapsed across its shard matrix, in which case
  the cell may have run the entire suite. A build failure, or a failure ahead of
  the R install, reads the same way here; the label errs toward not claiming a
  working cell never reached R.

The installed R version also appears on every E2E job's own summary page, written
by `.github/actions/os-e2e-deps` for all callers rather than just this one.
