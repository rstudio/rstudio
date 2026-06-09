# Posit Assistant manifest download throttling

## Overview

RStudio downloads the Posit Assistant manifest (`manifest.json`) to discover the
latest available package version, decide whether to offer an install / update /
downgrade, and detect unsupported installed versions. Today the fetch happens on
essentially every rsession startup where Posit AI is "wanted" (chat provider is
Posit, or the completions assistant preference is Posit). Because server/desktop
sessions restart frequently, this means the manifest is downloaded far more often
than necessary.

This change throttles the manifest download so it happens at most about once per
day, while still fetching immediately when there is a real need (nothing
installed yet, or a protocol-version mismatch). A failed fetch no longer blocks a
working installation, but a version a prior successful check flagged as
unsupported stays blocked until another successful check clears it.

The manifest download is owned entirely by the Chat module
(`SessionChat.cpp`). The NES / inline-completions module (`SessionAssistant.cpp`)
does not fetch the manifest; it shares the same `pai/bin` installation and reads
the Chat module's `isPositAssistantUnsupported()` state. So all changes here are
in the Chat module and a new helper component; there are no NES-specific or
frontend changes.

## Goals

- Download the manifest at most once per ~24 hours under normal conditions.
- Persist, across session restarts:
  - the time of the last *attempt* (so the throttle holds), and
  - the last *successful* check's blocking result and the install context it was
    computed for (so a known-unsupported install stays blocked without a fresh
    fetch).
- Always fetch immediately, ignoring the throttle, when:
  - Posit AI is not currently installed, or
  - the installed package's protocol version (from its `protocol.json`) does not
    match the protocol version RStudio expects (`kProtocolVersion`).
- When a fetch fails (or is throttled) but a compatible version is already
  installed (installed and no protocol mismatch), use the installed version
  without surfacing an error -- UNLESS a prior successful check flagged that same
  install as unsupported, in which case keep blocking it. Retry on the next
  throttle window (~24 hours later).
- Never throttle a user-initiated recheck (Retry) or an actual install.
- Only a successful manifest fetch may change (set or clear) the persisted
  unsupported determination.

## Non-goals

- No periodic in-session timer. A long-lived session that already has populated
  update state will not re-fetch past 24 hours until it restarts or the user
  forces a recheck. (Accepted tradeoff.)
- No change to the JSON-RPC payload shape (`buildUpdateStateResult`) or any
  frontend code. The persisted block reuses the existing
  `unsupportedInstalledVersion` / `unsupportedProtocol` flags, which the client
  already understands.

## Background: current flow

- `onDeferredInit` schedules `startUpdateCheck(true, ...)` ~1s after R goes idle,
  gated by `isPositAssistantWanted()`.
- `chatCheckForUpdates` (RPC) returns cached `s_updateState` immediately when it
  is populated, no recheck is forced, and no check is in flight; otherwise it
  joins/kicks `startUpdateCheck(false, ...)`.
- `chatInstallUpdate` (RPC) calls `performInstall` directly when state is
  populated, otherwise runs `startUpdateCheck(false, performInstall)` first
  because the install needs the manifest-derived `downloadUrl` / `sha256`.
- `startUpdateCheck` always calls `fetchManifestAsync`, which downloads via a
  `--vanilla` child R process and calls `onUpdateCheckComplete`.
- `onUpdateCheckComplete` computes `s_updateState`; on any fetch error it sets
  `manifestUnavailable = true`, which makes `isPositAssistantUnsupported()` true
  and triggers `stopAgentForUpdate()`.

## Design

### 1. New component: `chat/ChatUpdateThrottle.{hpp,cpp}` + `ChatUpdateThrottleTests.cpp`

Keeps the throttle decision and the persisted record pure and unit-testable,
alongside the existing `ChatIntegrity` / `ChatInstallation` components. Namespace
`rstudio::session::modules::chat::throttle`.

`SessionChat.cpp` adds `using throttle::ManifestCheckRecord;`,
`using throttle::ResolvedBlock;`, and `using throttle::SuccessOutcome;`
alongside its existing selective `using chat_installation::...` declarations, so
the snippets below refer to these types unqualified while still calling the
`throttle::` free functions explicitly.

```cpp
// Default throttle window: 24 hours.
extern const int kManifestCheckThrottleSeconds;

// Persisted record. installedVersion / rstudioProtocol record the context the
// unsupported flags were computed against, so a stale block is never applied to
// a different install.
//
// IMPORTANT: the two bool flags store only MANIFEST-derived decisions, never the
// locally-checkable protocol.json mismatch. In the live state,
// s_updateState.unsupportedInstalledVersion is the composite
// `isVersionUnsupported(...) || hasProtocolMismatch(...)`, but only the
// `isVersionUnsupported(...)` term is persisted here. The mismatch term is
// recomputed locally at reapply time, and both reapply paths (throttled-skip and
// fetch-failure-with-compatible-install) only run when there is NO current
// mismatch -- so a block caused purely by a transient/stale protocol.json is
// never resurrected after the install is repaired.
struct ManifestCheckRecord
{
   std::time_t lastCheckTime = 0;
   std::string installedVersion;            // installed PAI version at last success
   std::string rstudioProtocol;             // RStudio kProtocolVersion at last success
   bool unsupportedInstalledVersion = false; // manifest version decision only
   bool unsupportedProtocol = false;         // manifest protocol decision only
};

// Persisted state file: <userDataDir>/pai/manifest-check.json
// (sibling of pai/bin, so a reinstall -- which replaces pai/bin -- never wipes it).
core::FilePath manifestCheckStatePath();

// Read the record. Returns none on missing / unreadable / malformed file
// (callers treat "none" as "never checked" -> fetch is due, no persisted block).
boost::optional<ManifestCheckRecord> readManifestCheckRecord(const core::FilePath& stateFile);

// Write the record. ensureDirectory() on the parent first.
core::Error writeManifestCheckRecord(const core::FilePath& stateFile,
                                     const ManifestCheckRecord& record);

// Preserve a prior record's block flags + context, bumping only lastCheckTime.
// Returns a default record (flags false, empty context) with `now` when prior is
// none. Used by finish()'s non-success fallback so every real attempt is recorded.
ManifestCheckRecord bumpRecord(boost::optional<ManifestCheckRecord> prior,
                               std::time_t now);

// Pure decision. Returns true (fetch is due) when:
//   force || !installed || protocolMismatch || !lastCheckTime ||
//   (now - *lastCheckTime) >= throttleSeconds
bool manifestCheckDue(bool force,
                      bool installed,
                      bool protocolMismatch,
                      boost::optional<std::time_t> lastCheckTime,
                      std::time_t now,
                      int throttleSeconds);

// The persisted unsupported flags, resolved against the current install.
struct ResolvedBlock
{
   bool unsupportedInstalledVersion = false;
   bool unsupportedProtocol = false;
};

// Pure: resolve which persisted flags still apply to the current install. The
// two flags are independent, because they were computed from different inputs:
//
//   - unsupportedProtocol is a manifest decision about RStudio's kProtocolVersion
//     and does NOT depend on the installed package version. It is kept whenever
//     the record's stored RStudio protocol still matches the current one.
//   - unsupportedInstalledVersion depends on the installed package version (vs
//     the manifest's minimum/blocklist). It is kept only when the stored
//     installed version AND RStudio protocol both still match.
//
// Any mismatch makes that flag stale, so it is dropped (returns false for it).
ResolvedBlock resolvePersistedBlock(const ManifestCheckRecord& record,
                                    const std::string& installedVersion,
                                    const std::string& rstudioProtocol);
```

Reference implementation:

```cpp
ResolvedBlock resolvePersistedBlock(const ManifestCheckRecord& record,
                                    const std::string& installedVersion,
                                    const std::string& rstudioProtocol)
{
   bool protoMatch = (record.rstudioProtocol == rstudioProtocol);
   bool versionMatch = (record.installedVersion == installedVersion);

   ResolvedBlock out;
   out.unsupportedProtocol = record.unsupportedProtocol && protoMatch;
   out.unsupportedInstalledVersion =
      record.unsupportedInstalledVersion && versionMatch && protoMatch;
   return out;
}
```

The success path must do two related-but-distinct things from the same manifest
result: set the *live* `s_updateState.unsupportedInstalledVersion` (a composite of
the manifest version decision OR a current local protocol mismatch) and build the
*persisted* record (manifest version decision ONLY). To remove all selection
logic from the impure call site -- which is where "persist the live composite by
mistake" would otherwise hide -- both are produced by a single pure function from
raw inputs:

```cpp
struct SuccessOutcome
{
   bool liveUnsupportedInstalledVersion = false;   // -> s_updateState
   ManifestCheckRecord record;                     // -> persist verbatim
};

// `now` / `installedVersion` / `rstudioProtocol` are the current context;
// versionUnsupported = isVersionUnsupported(installed, manifest) (manifest-only);
// protocolMismatch   = hasProtocolMismatch(installed) (local);
// unsupportedProtocol = isProtocolUnsupported(manifest) (manifest-only).
SuccessOutcome buildSuccessOutcome(std::time_t now,
                                   const std::string& installedVersion,
                                   const std::string& rstudioProtocol,
                                   bool versionUnsupported,
                                   bool protocolMismatch,
                                   bool unsupportedProtocol)
{
   SuccessOutcome out;
   out.liveUnsupportedInstalledVersion = versionUnsupported || protocolMismatch;
   out.record.lastCheckTime = now;
   out.record.installedVersion = installedVersion;
   out.record.rstudioProtocol = rstudioProtocol;
   out.record.unsupportedInstalledVersion = versionUnsupported;   // manifest-only
   out.record.unsupportedProtocol = unsupportedProtocol;          // manifest-only
   return out;
}
```

Because this single function decides both the live flag and every persisted
field, the call site becomes a mechanical copy of `outcome.liveUnsupportedInstalledVersion`
into `s_updateState` and `outcome.record` into the record-to-write -- no
selection, nothing to get wrong. That mechanical copy is the irreducible impure
boundary (writing to the file-static `s_updateState` and the on-disk record);
this is the convergence point for testability, and further pure decomposition
would not reduce risk.

Passing the state-file path, `now`, and times as parameters is what lets the
tests avoid the filesystem and the wall clock. `lastCheckTime` is stored as
integer epoch seconds for unambiguous comparison.

### 2. Gate in `startUpdateCheck` (SessionChat.cpp)

Add a `force` flag:

```cpp
void startUpdateCheck(bool isStartup, bool force, boost::function<void()> onComplete);
```

After the existing single-flight guard sets `s_checkInProgress = true`:

```cpp
if (!shouldFetchManifest(force))
{
   resolveWithoutManifestFetch();   // no network, no record write
   return;
}
fetchManifestAsync(&onUpdateCheckComplete);
```

`shouldFetchManifest` is the thin impure composer:

```cpp
bool shouldFetchManifest(bool force)
{
   std::string installed = getInstalledVersion();   // "" when not installed
   bool isInstalled = !installed.empty();
   bool mismatch = isInstalled && hasProtocolMismatch(installed);
   boost::optional<throttle::ManifestCheckRecord> record =
      throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
   boost::optional<std::time_t> last;
   if (record) last = record->lastCheckTime;
   return throttle::manifestCheckDue(
      force, isInstalled, mismatch, last,
      std::time(nullptr), throttle::kManifestCheckThrottleSeconds);
}
```

`resolveWithoutManifestFetch` (throttled-skip; reached only when installed and no
protocol mismatch) writes `s_updateState` from the installed version with the
update fields cleared (`updateAvailable=false`, `isDowngrade=false`,
`noCompatibleVersion=false`, `manifestUnavailable=false`, empty `newVersion` /
`downloadUrl` / `expectedSha256`). For the unsupported flags it reapplies the
persisted block using the same guarded form as the failure branch:

```cpp
unsupportedInstalledVersion = false;
unsupportedProtocol = false;
boost::optional<ManifestCheckRecord> record =
   throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
if (record)
{
   ResolvedBlock b =
      throttle::resolvePersistedBlock(*record, installedVersion, kProtocolVersion);
   unsupportedInstalledVersion = b.unsupportedInstalledVersion;
   unsupportedProtocol = b.unsupportedProtocol;
}
```

Then call `isPositAssistantUnsupported()` and `stopAgentForUpdate()` if blocked
(mirroring `finish()`), and drain the pending completions via
`drainPendingCompletions()`. It does not write the record (no attempt was made).

### 3. Caller updates

- `onDeferredInit`: `startUpdateCheck(true, /*force*/ false, {})`.
- `chatCheckForUpdates`: pass the existing `forceRecheck` through as `force`.
- `chatInstallUpdate`: `startUpdateCheck(false, /*force*/ true, performInstall)`.

### 4. `onUpdateCheckComplete` changes

Track whether the fetch succeeded with a captured `bool fetchSucceeded = false;`
set true on the success path before `finish()`.

Fetch-failure branch (`if (fetchError)`):

```cpp
bool isInstalled = (installedVersion != "0.0.0");
bool mismatch = hasProtocolMismatch(installedVersion);
boost::optional<ManifestCheckRecord> prior =
   throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
if (isInstalled && !mismatch)
{
   // Compatible install -- reapply persisted block (if any) instead of clearing.
   if (prior)
   {
      ResolvedBlock b =
         throttle::resolvePersistedBlock(*prior, installedVersion, kProtocolVersion);
      unsupportedInstalledVersion = b.unsupportedInstalledVersion;
      unsupportedProtocol = b.unsupportedProtocol;
   }
   // manifestUnavailable stays false; surface no error.
}
else
{
   manifestUnavailable = true;          // not installed, or protocol mismatch
   errorMessage = fetchError.getMessage();
}
finish();   // fetchSucceeded == false; finish() handles the preserve-and-bump
return;
```

This branch leaves `recordToWrite` unset; the preserve-and-bump is centralized in
`finish()` (section 5) so it also covers the other non-success exits.

The success path computes the raw inputs and delegates the live-flag / persisted-
record split to `buildSuccessOutcome`, then sets `fetchSucceeded = true`:

```cpp
bool versionUnsupported = isVersionUnsupported(installedVersion, unsupportedInfo);
bool protocolMismatch   = hasProtocolMismatch(installedVersion);
SuccessOutcome outcome = throttle::buildSuccessOutcome(
   std::time(nullptr), installedVersion, kProtocolVersion,
   versionUnsupported, protocolMismatch, unsupportedProtocol);
unsupportedInstalledVersion = outcome.liveUnsupportedInstalledVersion;
recordToWrite = outcome.record;   // persisted verbatim in finish()
fetchSucceeded = true;
```

`recordToWrite` is a `boost::optional<ManifestCheckRecord>` declared (defaulting
to none) among the block of computed locals at the top of `onUpdateCheckComplete`,
so the by-reference `finish()` lambda can read it. Only the success path sets it
(from `buildSuccessOutcome`); every non-success exit leaves it unset and relies on
`finish()`'s fallback.

### 5. Record persistence in `finish()`

`onUpdateCheckComplete` reaches `finish()` from several exits: the success path
(`recordToWrite` staged), the fetch-error branch, and the early manifest-
processing failures (malformed `getUnsupportedInfo`, `getPackageInfoFromManifest`
error). All of those are real fetch attempts and must bump the timestamp, or a
bad manifest would bypass the throttle and re-download on every startup. So
`finish()` writes the staged success record when present, and otherwise falls
back to a preserve-and-bump (read prior, keep its block flags, bump only the
timestamp -- only a success may set/clear the persisted block):

```cpp
ManifestCheckRecord record = recordToWrite
   ? *recordToWrite
   : throttle::bumpRecord(
        throttle::readManifestCheckRecord(throttle::manifestCheckStatePath()),
        std::time(nullptr));
throttle::writeManifestCheckRecord(throttle::manifestCheckStatePath(), record);
```

The fallback's record construction is a pure helper so it is unit-tested rather
than living inline in `finish()`:

```cpp
// Preserve the prior record's manifest block flags and context, bumping only the
// timestamp. Returns a default record (empty context, flags false) with the new
// timestamp when there is no prior record.
ManifestCheckRecord bumpRecord(boost::optional<ManifestCheckRecord> prior,
                               std::time_t now)
{
   ManifestCheckRecord out = prior.value_or(ManifestCheckRecord{});
   out.lastCheckTime = now;
   return out;
}
```

Because every `finish()` path writes (staged success record or bumped fallback),
each real attempt bumps the timestamp. The throttled-skip path does not call
`finish()`, so it never writes (no attempt was made).

### 6. Shared helper

Factor the existing completion-draining tail of `finish()` into:

```cpp
void drainPendingCompletions()
{
   s_checkInProgress = false;
   s_checkIncludesStartup = false;
   std::vector<boost::function<void()>> completions;
   completions.swap(s_pendingCompletions);
   for (boost::function<void()>& completion : completions)
      completion();
}
```

Used by both `onUpdateCheckComplete::finish()` and `resolveWithoutManifestFetch()`.

## Data flow

```
startup / pane-open / install / retry
        |
        v
startUpdateCheck(isStartup, force, onComplete)
        |  single-flight guard
        v
shouldFetchManifest(force)
   |                          |
 due                       not due (installed + no mismatch + within window)
   v                          v
fetchManifestAsync       resolveWithoutManifestFetch
   |                          |  installed version; update fields cleared;
   |                          |  reapply persisted block iff context matches
   |                          v
   |                     stopAgentForUpdate if blocked; drainPendingCompletions
   v
onUpdateCheckComplete
   |--- success ---> buildSuccessOutcome (live flag + staged record)
   |--- failure + compatible install ---> reapply persisted block (else no error)
   |--- failure + (not installed | mismatch) ---> manifestUnavailable -> block
   |--- malformed manifest / no-compatible-version ---> block / leave running
   v
finish(): write staged success record, else fallback preserve-and-bump (every
          exit bumps the timestamp) -> stopAgentForUpdate if blocked ->
          drainPendingCompletions
```

## Error handling

- Unreadable / missing / malformed state file: `readManifestCheckRecord` returns
  none -> treated as "never checked" (fetch is due) and "no persisted block".
  Fail-open toward fetching, which is safe.
- `writeManifestCheckRecord` failure: logged (WLOG), not fatal. Worst case the
  next startup fetches again (no throttle benefit that one time).
- Fetch failure semantics covered in section 4.
- `manifestUnavailable` is transient state, never persisted. It is recomputed on
  each real attempt and only the manifest-derived block flags are stored. Two
  consequences, both intentional:
  - A download failure (or, per existing behavior, a malformed manifest) sets
    `manifestUnavailable` and blocks only within the session that observed it; a
    restart within the throttle window throttle-skips and uses the installed
    version (consistent with "just use the compatible install").
  - This change scopes the "use the installed version without an error" handling
    to download *failures* (the user's stated case). The pre-existing
    malformed-manifest / no-compatible-version exits keep their current
    within-session behavior (block / leave-running respectively) but now also bump
    the throttle timestamp via `finish()`'s fallback. Broadening "just use it" to
    malformed manifests is out of scope.

## Testing

`ChatUpdateThrottleTests.cpp` (Google Test, rsession scope):

- `manifestCheckDue`:
  - `force=true` -> due regardless of other inputs.
  - not installed -> due.
  - protocol mismatch -> due.
  - no prior check (`lastCheckTime` = none) -> due.
  - within window (`now - last < throttle`) -> not due.
  - exactly at / past window -> due.
- `resolvePersistedBlock` (the core of both the throttled-skip and the
  fetch-failure reapply, so this is where the regression risk concentrates):
  - installedVersion + protocol both match -> both flags preserved as stored.
  - installedVersion differs, protocol matches -> `unsupportedInstalledVersion`
    dropped, `unsupportedProtocol` preserved (the independence fix).
  - protocol differs -> both flags dropped.
  - record has both flags false -> both stay false regardless of match.
- `buildSuccessOutcome` (pins the live-vs-persisted contract AND the record
  construction, so a regression that writes the live composite into the record is
  caught -- the call site has no selection logic left to mis-wire):
  - `versionUnsupported=false, protocolMismatch=true` ->
    `liveUnsupportedInstalledVersion=true` but
    `record.unsupportedInstalledVersion=false` (the manifest-only persistence
    guarantee -- the key regression guard).
  - `versionUnsupported=true, protocolMismatch=false` -> live true,
    `record.unsupportedInstalledVersion=true`.
  - `versionUnsupported=false, protocolMismatch=false` -> live false,
    `record.unsupportedInstalledVersion=false`.
  - `record` carries through `lastCheckTime`, `installedVersion`,
    `rstudioProtocol`, and `unsupportedProtocol` from the inputs.
- `bumpRecord` (the non-success fallback used for fetch-error, malformed-manifest,
  and no-compatible-version exits):
  - prior present -> all block flags + context preserved, `lastCheckTime` set to
    `now`.
  - prior none -> default record (flags false, empty context), `lastCheckTime` set
    to `now`.
- `readManifestCheckRecord` / `writeManifestCheckRecord` round-trip against a
  temp file; missing file -> none; malformed JSON -> none; valid -> expected
  fields (including the boolean flags and the context strings).

### Behavior paths (throttled-skip and fetch-failure)

`resolveWithoutManifestFetch`, the `onUpdateCheckComplete` failure branch, and
the success/fallback persistence are deliberately thin wrappers around the pure
helpers (`resolvePersistedBlock`, `buildSuccessOutcome`, `bumpRecord`) and the
record read/write helpers, so all record *construction* -- which flags reapply,
the live-vs-persisted split, and the preserve-and-bump -- is covered by the pure
tests above. What remains in the wrappers is one routing branch in `finish()`
(`recordToWrite` staged vs. `bumpRecord` fallback) plus the irreducible impure
boundary: reading the prior record, assigning pure results into the file-static
`s_updateState`, and writing the record to disk. That routing + I/O is what a full
SessionChat-level integration test would cover, and it would require new seams
around `s_updateState`, the filesystem-backed `getInstalledVersion` /
`hasProtocolMismatch`, and `enqueClientEvent`; that refactor is out of scope. This
is the convergence point: every decision and record-construction step is pure and
tested, leaving only mechanical routing + I/O in the wrappers -- the implementer
must keep them mechanical rather than reintroducing logic.

No new frontend tests; payload shape and client behavior are unchanged.

## Files touched

- `src/cpp/session/modules/chat/ChatUpdateThrottle.hpp` (new)
- `src/cpp/session/modules/chat/ChatUpdateThrottle.cpp` (new)
- `src/cpp/session/modules/chat/ChatUpdateThrottleTests.cpp` (new)
- `src/cpp/session/modules/SessionChat.cpp` (gate, callers, failure handling,
  record persistence, shared helper)
- `src/cpp/session/CMakeLists.txt` (register new source + test files, if not
  globbed)
- `NEWS.md` (entry under the appropriate section, if user-facing per the repo's
  NEWS rules)
