# E2E shard counts

`shards.json` declares, per platform, how many Playwright shards the e2e
suite splits into. The `os-e2e-shards` composite action reads it and
republishes the value as job outputs, so each workflow declares its shard
count exactly once -- here -- and derives everything else from it: the
`strategy.matrix` (via `fromJSON`), the `--shard=N/M` argument (via
`strategy.job-total`), and the shard count printed in the PR comment.

    .github/e2e/shards.json                           # { "<platform>": <count>, ... }
    .github/actions/os-e2e-shards                     # reads it; outputs count + matrix
    .github/workflows/os-test-e2e-rstudio-shards.yml  # reusable job around the action

A workflow's `strategy.matrix` cannot read a file or an `env` value, which
is why the count has to pass through a small job's outputs. Most engines
get that job by calling the reusable workflow (a `shards` job with
`uses: ./.github/workflows/os-test-e2e-rstudio-shards.yml`), so the
checkout pin, sparse paths and timeout live in one place. Linux Desktop
already has a `setup` job that reads engine config, so it runs the action
directly.

| key             | workflows                                        | read by                                |
|-----------------|--------------------------------------------------|----------------------------------------|
| `macos`         | `os-test-e2e-rstudio-desktop-os-macos-*.yml`     | a `shards` job (the reusable workflow) |
| `windows`       | `os-test-e2e-rstudio-desktop-os-windows-*.yml`   | a `shards` job (the reusable workflow) |
| `linux-desktop` | `os-test-e2e-rstudio-desktop-linux.yml`          | the existing `setup` job (the action)  |
| `linux-server`  | `os-test-e2e-rstudio-server-os-ubuntu-24.yml`    | a `shards` job (the reusable workflow) |

The Linux Server accessibility run (`a11y: true`) ignores the count and
runs its handful of state scans in one shard.

## Choosing a count

Playwright shards by spec file, not by duration, so shards stay somewhat
unbalanced: a shard that lands the heavy file clusters (Posit Assistant
chat, data viewer) runs longest, and bumping the count alone does not
split such a cluster. Per-shard setup (checkout, R, RStudio, test deps) is
about four to five minutes on every platform, so extra shards convert
nearly 1:1 into wall-clock savings until the per-shard test time
approaches that overhead.

- macOS: 6. At 2 shards the test step ran 79 and 101 minutes against a
  120 minute job timeout. The org's enterprise plan allows 50 concurrent
  macOS jobs, so runner concurrency is not the limit.
- Windows: 6. At 4 shards the slowest shard ran ~30 minutes of tests
  against ~22 for its siblings and was the long pole of the PR-triggered
  run; at 6 most shards run 15 to 25 minutes of tests, with the shard
  that lands the heavy cluster at 30 to 40.
- Linux Desktop and Linux Server: 4. At 2 shards the slower shard ran
  roughly 50 to 78 minutes of tests on both platforms (Linux Server's
  second shard was 67 to 76 minutes against ~45 for its sibling), so a
  runner with a slow setup phase pushed a shard past the 120 minute job
  timeout with the tests themselves still passing. Four is also the
  floor for any platform: no shard should sit close enough to the
  timeout that a bad runner hour fails the job.
