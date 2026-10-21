# E2E shard counts

`shards.json` declares, per platform, how many Playwright shards the e2e
suite splits into. The `os-e2e-shards` composite action reads it and
republishes the value as job outputs, so a workflow declares its shard
count exactly once -- here -- and derives everything else from it: the
`strategy.matrix` (via `fromJSON`), the `--shard=N/M` argument (via
`strategy.job-total`), and the shard count printed in the PR comment.

    .github/e2e/shards.json            # { "<platform>": <count>, ... }
    .github/actions/os-e2e-shards      # reads it; outputs count + matrix

A workflow's `strategy.matrix` cannot read a file or an `env` value, which
is why the count has to pass through a small job's outputs.

Today only the macOS workflows (`os-test-e2e-rstudio-desktop-os-macos-*.yml`)
read from here; the Windows and Linux workflows still declare their matrix
inline.

## Choosing a count

Playwright shards by spec file, not by duration, so shards stay somewhat
unbalanced: a shard that lands the heavy file clusters (Posit Assistant
chat, data viewer) runs longest. Per-shard setup on macOS is about four
minutes (checkout, R, RStudio.app, test deps), against a test step that
ran 79 and 101 minutes at 2 shards, so extra shards convert nearly 1:1
into wall-clock savings until the per-shard test time approaches the
setup overhead. The org's enterprise plan allows 50 concurrent macOS
jobs, so runner concurrency is not the limit.
