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
working installation.

The manifest download is owned entirely by the Chat module
(`SessionChat.cpp`). The NES / inline-completions module (`SessionAssistant.cpp`)
does not fetch the manifest; it shares the same `pai/bin` installation and reads
the Chat module's `isPositAssistantUnsupported()` state. So all changes here are
in the Chat module and a new helper component; there are no NES-specific or
frontend changes.

## Goals

- Download the manifest at most once per ~24 hours under normal conditions.
- Persist the time of the last *attempt* so the throttle holds across session
  restarts.
- Always fetch immediately, ignoring the throttle, when:
  - Posit AI is not currently installed, or
  - the installed package's protocol version (from its `protocol.json`) does not
    match the protocol version RStudio expects (`kProtocolVersion`).
- When a fetch fails but a compatible version is already installed (installed and
  no protocol mismatch), use the installed version without surfacing an error,
  and retry on the next throttle window (~24 hours later).
- Never throttle a user-initiated recheck (Retry) or an actual install.

## Non-goals

- No periodic in-session timer. A long-lived session that already has populated
  update state will not re-fetch past 24 hours until it restarts or the user
  forces a recheck. (Accepted tradeoff.)
- No persistence of the *result* of a check. Within the 24-hour window a version
  that became unsupported (below the manifest minimum) upstream keeps running
  until the next daily fetch detects it. Protocol mismatch is locally checkable
  and always forces an immediate fetch, so a protocol-incompatible install is
  never silently used. (Accepted tradeoff.)
- No change to the JSON-RPC payload shape (`buildUpdateStateResult`) or any
  frontend code.

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

Keeps the throttle decision pure and unit-testable, alongside the existing
`ChatIntegrity` / `ChatInstallation` components. Namespace
`rstudio::session::modules::chat::throttle`.

```cpp
// Default throttle window: 24 hours.
extern const int kManifestCheckThrottleSeconds;

// Persisted state file: <userDataDir>/pai/manifest-check.json
// (sibling of pai/bin, so a reinstall -- which replaces pai/bin -- never wipes it).
core::FilePath manifestCheckStatePath();

// Read the last-attempt time. Returns none on missing / unreadable / malformed
// file (callers treat "none" as "never checked" -> fetch is due).
boost::optional<std::time_t> readLastCheckTime(const core::FilePath& stateFile);

// Write the last-attempt time. ensureDirectory() on the parent first.
// Format: { "lastCheckTime": <epoch-seconds> }
core::Error writeLastCheckTime(const core::FilePath& stateFile, std::time_t when);

// Pure decision. Returns true (fetch is due) when:
//   force || !installed || protocolMismatch || !lastCheck ||
//   (now - *lastCheck) >= throttleSeconds
bool manifestCheckDue(bool force,
                      bool installed,
                      bool protocolMismatch,
                      boost::optional<std::time_t> lastCheck,
                      std::time_t now,
                      int throttleSeconds);
```

Passing the state-file path, `now`, and `lastCheck` as parameters is what lets
the tests avoid the filesystem and the wall clock. The timestamp is stored as
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
   resolveWithoutManifestFetch();   // no network, no timestamp write
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
   boost::optional<std::time_t> last =
      throttle::readLastCheckTime(throttle::manifestCheckStatePath());
   return throttle::manifestCheckDue(
      force, isInstalled, mismatch, last,
      std::time(nullptr), throttle::kManifestCheckThrottleSeconds);
}
```

`resolveWithoutManifestFetch` writes `s_updateState` from the installed version
with every blocking flag cleared (`updateAvailable=false`, `isDowngrade=false`,
`noCompatibleVersion=false`, `unsupportedInstalledVersion=false`,
`unsupportedProtocol=false`, `manifestUnavailable=false`, empty `newVersion` /
`downloadUrl` / `expectedSha256`, `errorMessage` cleared), then drains the
pending completions via the shared `drainPendingCompletions()` helper. It does
not write the timestamp (no attempt was made).

### 3. Caller updates

- `onDeferredInit`: `startUpdateCheck(true, /*force*/ false, {})`.
- `chatCheckForUpdates`: pass the existing `forceRecheck` through as `force`.
- `chatInstallUpdate`: `startUpdateCheck(false, /*force*/ true, performInstall)`.

### 4. Fetch-failure handling in `onUpdateCheckComplete`

Replace the unconditional `manifestUnavailable = true` in the `if (fetchError)`
branch with:

```cpp
bool isInstalled = (installedVersion != "0.0.0");
bool mismatch = hasProtocolMismatch(installedVersion);
if (isInstalled && !mismatch)
{
   // Compatible version already installed -- use it, surface no error.
   // All blocking flags stay false; currentVersion stays = installedVersion.
}
else
{
   manifestUnavailable = true;          // not installed, or protocol mismatch
   errorMessage = fetchError.getMessage();
}
finish();
```

`finish()` records the attempt timestamp
(`throttle::writeLastCheckTime(throttle::manifestCheckStatePath(), std::time(nullptr))`)
so a failed attempt also resets the 24-hour window. The timestamp is written on
every real fetch completion (success or failure), and never on the
throttled-skip path.

### 5. Shared helper

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
   |                         |
 due                      not due
   v                         v
fetchManifestAsync     resolveWithoutManifestFetch
   |                         |  (installed version, flags cleared)
   v                         v
onUpdateCheckComplete    drainPendingCompletions
   |  (writes timestamp)
   |--- success ---> compute update state
   |--- failure + compatible install ---> use installed, no error
   |--- failure + (not installed | mismatch) ---> manifestUnavailable -> block
   v
finish() -> writeLastCheckTime + drainPendingCompletions
```

## Error handling

- Unreadable / missing / malformed state file: `readLastCheckTime` returns none
  -> treated as "never checked" -> fetch is due. Fail-open toward fetching, which
  is safe.
- `writeLastCheckTime` failure: logged (WLOG), not fatal. Worst case the next
  startup fetches again (no throttle benefit that one time).
- Fetch failure semantics covered in section 4.

## Testing

`ChatUpdateThrottleTests.cpp` (Google Test, rsession scope):

- `manifestCheckDue`:
  - `force=true` -> due regardless of other inputs.
  - not installed -> due.
  - protocol mismatch -> due.
  - no prior check (`lastCheck` = none) -> due.
  - within window (`now - last < throttle`) -> not due.
  - exactly at / past window -> due.
- `readLastCheckTime` / `writeLastCheckTime` round-trip against a temp file;
  missing file -> none; malformed JSON -> none; valid -> expected value.

No new frontend tests; payload shape and client behavior are unchanged.

## Files touched

- `src/cpp/session/modules/chat/ChatUpdateThrottle.hpp` (new)
- `src/cpp/session/modules/chat/ChatUpdateThrottle.cpp` (new)
- `src/cpp/session/modules/chat/ChatUpdateThrottleTests.cpp` (new)
- `src/cpp/session/modules/SessionChat.cpp` (gate, callers, failure handling,
  shared helper)
- `src/cpp/session/CMakeLists.txt` (register new source + test files, if not
  globbed)
- `NEWS.md` (entry under the appropriate section, if user-facing per the repo's
  NEWS rules)
