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
if (isInstalled && !mismatch)
{
   // Compatible install -- reapply persisted block (if any) instead of clearing.
   boost::optional<ManifestCheckRecord> record =
      throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
   if (record)
   {
      ResolvedBlock b =
         throttle::resolvePersistedBlock(*record, installedVersion, kProtocolVersion);
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
finish();   // fetchSucceeded == false
return;
```

The success path is unchanged in how it computes the *live* flags, but it now
also captures the manifest-only version decision separately so the persisted
record excludes the local protocol-mismatch term:

```cpp
bool versionUnsupported = isVersionUnsupported(installedVersion, unsupportedInfo);
bool protocolMismatch   = hasProtocolMismatch(installedVersion);
// live composite (unchanged behavior for s_updateState):
unsupportedInstalledVersion = versionUnsupported || protocolMismatch;
// `versionUnsupported` (manifest-only) is what gets persisted -- see section 5.
```

`versionUnsupported` is declared (defaulting to `false`) among the block of
computed locals at the top of `onUpdateCheckComplete`, alongside
`unsupportedInstalledVersion` etc., so the by-reference `finish()` lambda can read
it on both the success and failure exit paths.

### 5. Record persistence in `finish()`

`finish()` records the attempt on every real fetch completion. On success it
persists the manifest-only decisions (`versionUnsupported`, not the live
composite `unsupportedInstalledVersion`):

```cpp
ManifestCheckRecord record;
std::time_t now = std::time(nullptr);
if (fetchSucceeded)
{
   // Authoritative result -- overwrite context + manifest-only flags.
   record.lastCheckTime = now;
   record.installedVersion = installedVersion;
   record.rstudioProtocol = kProtocolVersion;
   record.unsupportedInstalledVersion = versionUnsupported;   // manifest-only
   record.unsupportedProtocol = unsupportedProtocol;          // manifest-only
}
else
{
   // Failure learns nothing about support status: preserve the prior block,
   // bump only the timestamp. (Only a success may set/clear the block.)
   boost::optional<ManifestCheckRecord> prior =
      throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
   record = prior.value_or(ManifestCheckRecord{});
   record.lastCheckTime = now;
}
throttle::writeManifestCheckRecord(throttle::manifestCheckStatePath(), record);
```

This write happens inside `finish()` (a real attempt completed), never on the
throttled-skip path.

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
   |--- success ---> compute flags fresh (authoritative)
   |--- failure + compatible install ---> reapply persisted block (else no error)
   |--- failure + (not installed | mismatch) ---> manifestUnavailable -> block
   v
finish(): write record (success: overwrite flags; failure: preserve flags,
          bump timestamp) -> stopAgentForUpdate if blocked -> drainPendingCompletions
```

## Error handling

- Unreadable / missing / malformed state file: `readManifestCheckRecord` returns
  none -> treated as "never checked" (fetch is due) and "no persisted block".
  Fail-open toward fetching, which is safe.
- `writeManifestCheckRecord` failure: logged (WLOG), not fatal. Worst case the
  next startup fetches again (no throttle benefit that one time).
- Fetch failure semantics covered in section 4.

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
- `readManifestCheckRecord` / `writeManifestCheckRecord` round-trip against a
  temp file; missing file -> none; malformed JSON -> none; valid -> expected
  fields (including the boolean flags and the context strings).

### Behavior paths (throttled-skip and fetch-failure)

`resolveWithoutManifestFetch` and the `onUpdateCheckComplete` failure branch are
deliberately thin wrappers around `resolvePersistedBlock` and the record
read/write helpers, so the substantive reapply/preserve logic is fully covered by
the pure tests above. The wrappers themselves only (a) clear the update fields,
(b) copy the resolved flags into `s_updateState`, and (c) on failure, bump the
timestamp while preserving the stored flags. Full SessionChat-level integration
tests of these wrappers would require new seams around the file-static
`s_updateState`, the filesystem-backed `getInstalledVersion` /
`hasProtocolMismatch`, and `enqueClientEvent`; that refactor is out of scope for
this change. The risk is mitigated by keeping the wrappers trivial and the
decision pure -- this tradeoff is called out explicitly so the implementer keeps
the wrappers thin rather than reintroducing logic into them.

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
