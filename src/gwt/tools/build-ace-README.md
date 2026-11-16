# Updating Ace

RStudio maintains its Ace changes in
[rstudio/rstudio-ace](https://github.com/rstudio/rstudio-ace). The integration
branch is named after the release in `version/RELEASE` (`rstudio/parrot-balsam`
for 2026.12); each release's `rel-*` branch rebuilds from its own fork branch,
so main-bound changes never target the previous release's branch. The first
fork change for a release cuts the new branch from the previous one. The fork's
GitHub default branch is older and is not the target for these changes.

`sync-ace-commits` records the repository and exact commit used for the bundled
JavaScript. A moving release or feature branch is not sufficient to reproduce
the files in an RStudio checkout. When updating Ace:

1. Implement and test the source change in a dedicated Ace worktree, and open
   its PR against the integration branch.
2. Set `REPOSITORY` and `REVISION` in `sync-ace-commits` to the reviewed source.
   Prefer its integration-branch commit once merged. While the Ace PR is under
   review, an RStudio draft PR can pin its published head commit and link the
   dependency. Update the pin and rebuild if that source changes.
3. Run `src/gwt/tools/sync-ace-commits`. It checks out the pinned revision in
   `src/gwt/tools/rstudio-ace`, initializes submodules, installs dependencies,
   and runs `build-ace`. This resets that source checkout to the pin; use the
   dedicated-worktree build below while developing.
4. Inspect the generated diff, run the affected Ace and RStudio tests, and commit
   the pin with the generated bundles. Do not patch the bundles by hand.

To build a dedicated Ace worktree directly:

```sh
ACE_SOURCE_DIR=/absolute/path/to/ace-worktree src/gwt/tools/build-ace
```

Install that checkout's npm dependencies first. Outputs are always written into
the RStudio checkout containing `build-ace`, regardless of the caller's working
directory or where the Ace worktree resides. The build covers core, keybindings,
language tools, diff, and workers; unrelated outputs should remain unchanged.

The bounded-diff source revision includes the tokenizer fixes from
[Ace PR #13](https://github.com/rstudio/rstudio-ace/pull/13), which were already
shipped in RStudio but were not yet on the integration branch. Combining them
with `rstudio/montauk-daisy` (the 2026.11 branch) reproduced the previous core and diff bundles
byte-for-byte before the bounded-diff change. Keep those fixes when updating
the pin rather than reverting the shipped tokenizer behavior.
Review of that dependency also added a plain-data clone fallback for Node 16
and corrected sparse-array length comparisons. Those fixes regenerate the core
bundles alongside the diff bundles.

For the Git diff changes, run:

```sh
# From the Ace worktree:
node src/background_tokenizer_test.js
node src/ext/diff/diff_test.js
node src/ext/diff/bounded_refinement_test.js

# From RStudio (src/gwt/test/git_diff_test.html drives the real gutter and view):
cd src/gwt
ant testpage
```
