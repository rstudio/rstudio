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
| `platform` | always | `linux`, `macos`, or `windows`. |
| `arch` | always | `x86_64` or `arm64`. Asserted downstream: the Linux workflow fails the run if it disagrees with the engine config's `tools_arch`. |
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

## The `distro` sentinel

The four Fedora engines set `r_install: "distro"` in their engine config, which
means they install R from Fedora's own repos and ignore the `r_version` input
entirely (see `.github/e2e-linux/fedora/install-r.sh`). Declaring `"r_version":
"distro"` records that honestly: the cell certifies against whatever R the distro
ships, which is a real certification target, just not a chosen one.

The workflow passes `release` to those engines rather than the literal `distro`,
since the value is unused -- and would break `rig add` outright if a config ever
switched to `r_install: "rig"`. What the run actually used is read back from the
installed R and reported, so the sentinel never has to be trusted.

## Changing the matrix

- **Change what an environment certifies against:** edit its `r_version`.
- **Add or drop an environment from the default run:** flip its `default`.
- **Certify a one-off combination without committing anything:** use the
  workflow's `cells` and `r_overrides` dispatch inputs.
- **Add a new engine:** add an entry here once the engine exists (see
  `.github/e2e-linux/README.md`). An engine absent from this file cannot be
  certified even by name.

Both dispatch inputs validate every name against this file and fail the run on
an unknown one, before any runner is provisioned.

## What a run reports

The workflow's summary job writes a table of environment, R requested, R actually
used, and result. "R actually used" is read back from the installed R, never
echoed from the request, so a `distro` cell shows the concrete version `dnf`
supplied and a cell whose R silently failed to change shows what really ran.

Two routes get that value to the table, because one alone can't cover both
shapes:

- The five non-Linux engines each return it as a `workflow_call` output.
- The Linux Desktop cells run as a single matrix call, and a matrix job collapses
  to one output value, so each uploads a `r-version-linux-desktop-<os>` artifact
  that the summary job downloads and attributes per cell. A cell that never
  reached the R install has no artifact and reads `_not reached_`.

The R version also appears in the Playwright HTML report's metadata, so it
travels with the uploaded report artifact, and on every E2E job's own summary
page (written by `.github/actions/os-e2e-deps`, for all callers, not just this
one).
