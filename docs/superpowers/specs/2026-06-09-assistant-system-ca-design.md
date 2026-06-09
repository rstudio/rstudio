# Trust the OS certificate store for AI assistant connections

- **Issue:** [#17892](https://github.com/rstudio/rstudio/issues/17892)
- **Date:** 2026-06-09
- **Branch:** `feature/assistant-system-ca`
- **Milestone:** Blue Plumbago

## Problem

The GitHub Copilot agent and the Posit Assistant chat backend are Node.js
processes. Their network requests go through Node's TLS stack, not
curl/OpenSSL, so the environment variables R users reach for
(`CURL_CA_BUNDLE`, `SSL_CERT_FILE`, ...) have no effect on them.

Behind a TLS-inspecting corporate proxy, the agent needs the proxy's root CA
to connect. Today the only options are the server settings
`copilot-ssl-certificates-file` / `posit-assistant-ssl-certificates-file` in
`rsession.conf`, or setting `NODE_EXTRA_CA_CERTS` to a PEM file by hand in
`~/.Renviron`. Both are awkward on RStudio Desktop on Windows: `rsession.conf`
is not used on Desktop, and corporate CAs usually live in the Windows
Certificate Store rather than as a PEM file, so `NODE_EXTRA_CA_CERTS` means
exporting the cert and tracking an absolute path.

## Solution

Add an opt-in user preference `assistant_use_system_ca` (default `false`),
surfaced as a checkbox in *Tools -> Global Options -> Assistant*. When enabled,
RStudio appends `--use-system-ca` to `NODE_OPTIONS` for both the Copilot agent
and the Posit Assistant chat backend, so Node trusts the OS certificate store
(Windows Certificate Store, macOS Keychain) in addition to its built-in CAs.

The change is additive: it stacks with `NODE_EXTRA_CA_CERTS` and the existing
`*-ssl-certificates-file` server options, so existing setups keep working.

`--use-system-ca` is available in the bundled Node (22.22.2; the flag landed in
22.15.0). Using `NODE_OPTIONS` rather than a CLI argument means it also covers
the enterprise helper-script launch paths and any child Node processes the
agents spawn, with no extra plumbing. `NODE_OPTIONS=--use-system-ca` is
accepted on Node 22.17.0+, which the bundled runtime satisfies.

### Node version compatibility (guard)

The resolved Node binary is not always the bundled one. `node_tools::findNode`
honors the `rstudio.copilot.nodeBinaryPath` / `rstudio.positAi.nodeBinaryPath`
R options, the admin `node-path` session option, and finally a `PATH` lookup.
Node **older than 22.17.0 rejects `NODE_OPTIONS=--use-system-ca` and exits on
startup** rather than ignoring it, so blindly injecting the flag could make the
agent fail to start when an older Node is configured -- a worse outcome than the
CA simply not being trusted.

To prevent that, the flag is injected only after confirming the resolved Node is
compatible. When the pref is enabled (and only then -- zero cost when off), the
launch site probes `<nodePath> --version`, parses the major/minor, and appends
the flag only when the version is >= 22.17.0. Otherwise it logs a clear warning
(naming the path and version) and starts the agent without the flag, so an old
configured Node degrades to "CA not trusted" instead of "agent won't start".
The bundled Node 22.22.2 always passes.

## How the setting takes effect (restart semantics)

The agents read `NODE_OPTIONS` only when launched, and both are child processes
of the **rsession** process. Therefore:

- A browser reload (`uiReloadRequired`) is **not** sufficient: it only
  reconnects the client to the same, still-running rsession process, so the
  agents keep their original environment.
- The rsession process must be relaunched so it re-runs `startAgent()` /
  `startChatBackend()` and recomputes the environment from the pref.

"Restart R Session" does exactly that, and it is a genuine *process* restart,
not an in-process R reinit:

`Commands.restartR()` -> `suspend_for_restart` RPC -> `doSuspendForRestart`
(`SessionMain.cpp`) -> `r::session::suspendForRestart` -> `suspend(...)` ->
`RCleanUp(SA_NOSAVE, status, FALSE)`, which terminates the rsession process.
The supervisor (rserver on Server, the session launcher on Desktop) then starts
a fresh rsession that resumes from saved state; its `initialize()` / `onResume`
relaunch the Copilot agent and chat backend, reading the new pref. Stored
Copilot credentials mean no re-authentication is needed.

This path is identical on Desktop and Server. The chat backend already
registers a suspend handler (`SessionChat.cpp` `addSuspendHandler` -> `onSuspend`
records `chat_suspended`; `onResume` calls `startChatBackend`), and the Copilot
agent is relaunched by the fresh process's `synchronize()` in `initialize()`.

Consequently the pane signals `restartRequirement.setSessionRestartRequired(true)`.
`PreferencesDialogBase.handleRestart` routes a lone `sessionRestartRequired` to
`restartSession()` -> `commands.restartR().execute()`, so toggling the box and
clicking OK offers to restart the R session and the change applies with no
manual app restart and no Server-specific caveat.

## Components and changes

### 1. Preference schema (declarative source of truth)

`src/cpp/session/resources/schema/user-prefs-schema.json`, alongside the other
`assistant_*` entries:

```json
"assistant_use_system_ca": {
    "type": "boolean",
    "default": false,
    "title": "Use the system certificate store (restart required)",
    "description": "When enabled, the AI assistant agents trust the operating system certificate store (e.g. the Windows Certificate Store or macOS Keychain) in addition to Node.js's built-in certificate authorities. Useful behind a TLS-inspecting proxy. Restart the R session for the change to take effect."
}
```

### 2. Regenerate accessors (generated; not hand-edited)

Run `Rscript scripts/generate-prefs.R`. This regenerates:

- C++ (`UserPrefValues.hpp/.cpp`): constant `kAssistantUseSystemCa` and
  accessor `bool assistantUseSystemCa()`, reachable as
  `prefs::userPrefs().assistantUseSystemCa()`.
- GWT (`UserPrefsAccessor.java`, `UserPrefsAccessorConstants.java`):
  `PrefValue<Boolean> assistantUseSystemCa()`, constant
  `ASSISTANT_USE_SYSTEM_CA`, and the `readUserPrefs` / `getAllPreferences`
  entries.

### 3. Testable merge helper

Add to the shared `node_tools` namespace (used by both launch sites):

`src/cpp/session/modules/SessionNodeTools.hpp`:

```cpp
/**
 * Append a Node.js option to an existing NODE_OPTIONS value.
 *
 * Preserves any options the caller already set (e.g. via .Renviron) and avoids
 * adding a duplicate flag. Tokens are separated by single spaces.
 *
 * @param existingOptions The current NODE_OPTIONS value (may be empty).
 * @param option The flag to append, e.g. "--use-system-ca".
 * @return The merged NODE_OPTIONS value.
 */
std::string appendNodeOption(const std::string& existingOptions,
                             const std::string& option);
```

`src/cpp/session/modules/SessionNodeTools.cpp`: split `existingOptions` on
whitespace; if `option` is already present as a token, return the original
value unchanged (trimmed); otherwise append `option` after a single separating
space. Empty input returns `option`. Pure function, no I/O.

Add two more helpers to the same header for the version guard:

```cpp
/**
 * Parse the major and minor version from `node --version` output
 * (e.g. "v22.22.2\n" -> 22, 22).
 *
 * @param versionOutput Raw stdout from `node --version`.
 * @param pMajor Output major version.
 * @param pMinor Output minor version.
 * @return true if a version was parsed, false otherwise.
 */
bool parseNodeVersion(const std::string& versionOutput, int* pMajor, int* pMinor);

/**
 * Whether the Node binary at nodePath supports `--use-system-ca` via
 * NODE_OPTIONS (Node >= 22.17.0). Runs `<nodePath> --version` and parses the
 * result. Returns false (with a logged warning) if the version cannot be
 * determined, so callers fail safe by not injecting the flag.
 */
bool nodeSupportsSystemCa(const core::FilePath& nodePath);
```

`parseNodeVersion` is pure (trims a leading `v`, parses `major.minor`) and
unit-tested. `nodeSupportsSystemCa` runs the binary via
`core::system::runProgram`, feeds stdout to `parseNodeVersion`, and returns
`major > 22 || (major == 22 && minor >= 17)`.

### 4. Both launch sites

In `SessionAssistant.cpp::startAgent` and `SessionChat.cpp::startChatBackend`,
once the resolved `nodePath` is available (the version guard needs it):

```cpp
// Trust the OS certificate store when the user has opted in. Additive to
// NODE_EXTRA_CA_CERTS; preserves any NODE_OPTIONS already in the environment.
// Guarded on the resolved Node version: NODE_OPTIONS=--use-system-ca is
// rejected by Node < 22.17.0, which would otherwise break agent startup.
if (prefs::userPrefs().assistantUseSystemCa() &&
    node_tools::nodeSupportsSystemCa(nodePath))
{
   std::string nodeOptions = core::system::getenv(environment, "NODE_OPTIONS");
   core::system::setenv(&environment, "NODE_OPTIONS",
                        node_tools::appendNodeOption(nodeOptions, "--use-system-ca"));
}
```

Placement differs because of where `nodePath` is resolved:

- `SessionChat.cpp`: `findNode` runs (line ~4689) before the environment block,
  so the snippet goes in that block after the `NODE_USE_ENV_PROXY` line.
- `SessionAssistant.cpp`: `findNode` runs *after* the `NODE_USE_ENV_PROXY` line
  (~line 1217), and the environment is later assigned into `options.environment`
  separately for the direct-launch and helper-script branches. Add the snippet
  after `nodePath` is resolved and validated, but before those `options.environment`
  assignments, so every launch branch picks up the modified `environment`.

Both sites already build `environment` via `core::system::environment(&environment)`,
so an inherited `NODE_OPTIONS` (e.g. from `.Renviron`) is present and preserved.
`SessionAssistant.cpp` includes the `node_tools` header already (`findNode`);
`SessionChat.cpp` adds the include. No `onUserPrefsChanged` wiring is added --
the restart-required model keeps this simple.

### 5. Preferences pane

`AssistantPreferencesPane.java`:

- Field: `private CheckBox cbAssistantUseSystemCa_;`
- Construct:
  `cbAssistantUseSystemCa_ = checkboxPref(prefs_.assistantUseSystemCa(), true);`
- Placement: always visible in the static layout (not a per-assistant swappable
  panel), since it governs both Copilot and the chat backend independent of the
  selected assistant. Add it in `initDisplay()` in the shared top region of the
  pane (after `add(cbAssistantToolbarButtonVisible_);`).
- Track the initial value for change detection: capture
  `initialUseSystemCa_ = prefs.assistantUseSystemCa().getValue();` in
  `initialize(UserPrefs prefs)` and set the checkbox value from it.
- In `onApply`, write the value and request a session restart only when it
  changed:

```java
boolean useSystemCa = cbAssistantUseSystemCa_.getValue();
prefs.assistantUseSystemCa().setGlobalValue(useSystemCa);
if (useSystemCa != initialUseSystemCa_)
{
   initialUseSystemCa_ = useSystemCa;
   restartRequirement.setSessionRestartRequired(true);
}
```

(`onApply` currently returns `super.onApply(prefs)` directly; refactor it to
hold the `RestartRequirement` in a local, apply the above, and return it -- the
same shape as `AccessibilityPreferencesPane.onApply`.)

### 6. Tests

`src/cpp/session/modules/SessionNodeToolsTests.cpp` (auto-discovered by the
`*Tests.cpp` glob in `src/cpp/session/CMakeLists.txt`; no CMake edit).

`node_tools::appendNodeOption`:

- empty existing value -> returns the flag alone
- existing unrelated options -> flag appended, originals preserved, single-space
  separated
- flag already present -> returned unchanged (idempotent), including when it is
  the first, middle, or last token
- existing value with extra/leading/trailing whitespace -> normalized to
  single-space-separated tokens

`node_tools::parseNodeVersion` (drives the >= 22.17.0 gate):

- `"v22.22.2\n"` -> 22, 22 (leading `v` and trailing newline tolerated)
- exactly `"v22.17.0"` -> 22, 17 (boundary that must pass the gate)
- `"v22.16.1"` / `"v20.19.4"` -> parsed, but below the gate (must not inject)
- malformed / empty output -> returns false (caller fails safe, skips the flag)

Run via the `rsession` scope. `nodeSupportsSystemCa` (subprocess) and end-to-end
TLS/proxy trust are verified manually on Windows (behind a TLS-inspecting proxy
with the proxy CA installed in the Windows Certificate Store).

### 7. Documentation

`docs/user/rstudio/ide/guide/tools/copilot.qmd`: document the new setting as the
recommended way to trust custom / proxy CAs on Desktop (especially Windows),
alongside the existing `NODE_EXTRA_CA_CERTS` and `*-ssl-certificates-file`
guidance. Note it trusts the OS certificate store and requires an R session
restart.

### 8. NEWS.md

Add under `### New`:

```
- ([#17892](https://github.com/rstudio/rstudio/issues/17892)): Added an option to trust the operating system certificate store for AI assistant connections (Tools > Global Options > Assistant > "Use the system certificate store").
```

## File change summary

| File | Change |
| --- | --- |
| `src/cpp/session/resources/schema/user-prefs-schema.json` | Add `assistant_use_system_ca` boolean |
| generated: `UserPrefValues.hpp/.cpp`, `UserPrefsAccessor.java`, `UserPrefsAccessorConstants.java` | Regenerated via `generate-prefs.R` |
| `src/cpp/session/modules/SessionNodeTools.hpp/.cpp` | Add `appendNodeOption` |
| `src/cpp/session/modules/SessionAssistant.cpp` | Append `--use-system-ca` to `NODE_OPTIONS` when pref on |
| `src/cpp/session/modules/SessionChat.cpp` | Same; add `node_tools` include |
| `src/cpp/session/modules/SessionNodeToolsTests.cpp` | New unit tests |
| `src/gwt/.../prefs/views/AssistantPreferencesPane.java` | Add checkbox + session-restart-on-change |
| `docs/user/rstudio/ide/guide/tools/copilot.qmd` | Document the setting |
| `NEWS.md` | `### New` entry for #17892 |

## Non-goals

- No auto-restart of the agents on toggle; the setting is restart-required by
  design (matches the issue and the `posit_assistant_test_manifest` precedent).
- No change to the existing `NODE_EXTRA_CA_CERTS` / `*-ssl-certificates-file`
  mechanisms; this is purely additive.
- No new server/session CLI option.
- No project-level override; this is a global (machine) concern.

## Conventions

- Branch: `feature/assistant-system-ca`.
- Commits: imperative subject, one logical change per commit; use
  "Addresses #17892".
- PR: "Addresses #17892." (unit tests included, but end-to-end behavior needs
  manual Windows verification), milestone Blue Plumbago.
