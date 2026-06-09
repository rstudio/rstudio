# Trust the OS Certificate Store for AI Assistant Connections — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an opt-in `assistant_use_system_ca` preference that makes the Copilot agent and Posit Assistant chat backend trust the OS certificate store, by appending `--use-system-ca` to `NODE_OPTIONS` at launch.

**Architecture:** A new boolean user pref drives a guarded environment tweak at the two Node launch sites. The tweak is gated on the resolved Node version (`>= 22.17.0`, since older Node rejects the flag in `NODE_OPTIONS` and would fail to start). The pure string/version helpers live in the shared `node_tools` namespace and are unit-tested; the launch-site wiring and the preferences-pane checkbox are verified by build.

**Tech Stack:** C++ (session modules, Google Test), JSON schema + `generate-prefs.R` code generation, GWT/Java (preferences pane), Quarto docs.

**Issue:** [#17892](https://github.com/rstudio/rstudio/issues/17892). **Branch:** `feature/assistant-system-ca` (already created). **Spec:** `docs/superpowers/specs/2026-06-09-assistant-system-ca-design.md`.

---

## File Structure

- `src/cpp/session/modules/SessionNodeTools.hpp` / `.cpp` — add `appendNodeOption`, `parseNodeVersion`, `nodeSupportsSystemCa` to the existing shared `node_tools` namespace.
- `src/cpp/session/modules/SessionNodeToolsTests.cpp` — **new**; unit tests for the two pure helpers (auto-discovered by the `*Tests.cpp` glob).
- `src/cpp/session/resources/schema/user-prefs-schema.json` — add the `assistant_use_system_ca` boolean. Drives generated C++/Java accessors.
- generated (do not hand-edit): `src/cpp/session/include/session/prefs/UserPrefValues.hpp`, `src/cpp/session/prefs/UserPrefValues.cpp`, `src/gwt/src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessor.java`, `src/gwt/src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessorConstants.java`.
- `src/cpp/session/modules/SessionAssistant.cpp` — guarded `NODE_OPTIONS` injection in `startAgent`.
- `src/cpp/session/modules/SessionChat.cpp` — guarded `NODE_OPTIONS` injection in `startChatBackend`.
- `src/gwt/src/org/rstudio/studio/client/workbench/prefs/views/AssistantPreferencesPane.java` — checkbox + session-restart-on-change.
- `docs/user/rstudio/ide/guide/tools/copilot.qmd` — document the setting.
- `NEWS.md` — `### New` entry.
- `.gitignore` + spec/plan docs — **Task 8** reverts the temporary tracking before the PR.

## Build / test commands (this machine)

- C++ build: `cd build && ninja`
- C++ tests (rsession scope; needs R_HOME or it runs nothing): `cd build && R_HOME=C:/opt/R/R-4.6.0 ./src/cpp/rstudio-tests.bat rsession`
  - The new tests report under the `SessionNodeToolsTest` suite. If the runner forwards gtest flags, `... rsession --gtest_filter=SessionNodeToolsTest.*` narrows the run; otherwise run the full rsession scope and read the `SessionNodeToolsTest` lines.
- Java compile check: `cd src/gwt && ant javac`
- Pref regeneration: `Rscript scripts/generate-prefs.R`

No command is added to `Commands.cmd.xml`, so no `.MD5` update is required.

---

### Task 1: node_tools helpers + unit tests (TDD)

**Files:**
- Create: `src/cpp/session/modules/SessionNodeToolsTests.cpp`
- Modify: `src/cpp/session/modules/SessionNodeTools.hpp`
- Modify: `src/cpp/session/modules/SessionNodeTools.cpp`

- [ ] **Step 1: Write the failing tests**

Create `src/cpp/session/modules/SessionNodeToolsTests.cpp`:

```cpp
/*
 * SessionNodeToolsTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionNodeTools.hpp"

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace modules {
namespace node_tools {
namespace tests {

TEST(SessionNodeToolsTest, AppendNodeOptionEmpty)
{
   EXPECT_EQ(appendNodeOption("", "--use-system-ca"), "--use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionPreservesExisting)
{
   EXPECT_EQ(appendNodeOption("--max-old-space-size=4096", "--use-system-ca"),
             "--max-old-space-size=4096 --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentSingle)
{
   EXPECT_EQ(appendNodeOption("--use-system-ca", "--use-system-ca"),
             "--use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentFirstAndMiddle)
{
   EXPECT_EQ(appendNodeOption("--use-system-ca --enable-source-maps", "--use-system-ca"),
             "--use-system-ca --enable-source-maps");
   EXPECT_EQ(appendNodeOption("--a --use-system-ca --b", "--use-system-ca"),
             "--a --use-system-ca --b");
}

TEST(SessionNodeToolsTest, AppendNodeOptionPreservesQuotedWhitespace)
{
   // Quoted values with intentional internal whitespace must not be rewritten.
   EXPECT_EQ(appendNodeOption("--require \"/my  app/x.js\"", "--use-system-ca"),
             "--require \"/my  app/x.js\" --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionMatchesWholeTokenOnly)
{
   // A longer option that merely starts with the flag is not a match.
   EXPECT_EQ(appendNodeOption("--use-system-cafoo", "--use-system-ca"),
             "--use-system-cafoo --use-system-ca");
}

TEST(SessionNodeToolsTest, ParseNodeVersionTypical)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v22.22.2\n", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 22);
}

TEST(SessionNodeToolsTest, ParseNodeVersionBoundary)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v22.17.0", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 17);
}

TEST(SessionNodeToolsTest, ParseNodeVersionBelowGate)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v20.19.4", &major, &minor));
   EXPECT_EQ(major, 20);
   EXPECT_EQ(minor, 19);
}

TEST(SessionNodeToolsTest, ParseNodeVersionMalformed)
{
   int major = 0, minor = 0;
   EXPECT_FALSE(parseNodeVersion("", &major, &minor));
   EXPECT_FALSE(parseNodeVersion("not-a-version", &major, &minor));
}

} // namespace tests
} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio
```

- [ ] **Step 2: Build and confirm the tests fail to compile**

Run: `cd build && ninja`
Expected: FAIL — `appendNodeOption` / `parseNodeVersion` are not declared in `node_tools`.

- [ ] **Step 3: Declare the helpers in the header**

In `src/cpp/session/modules/SessionNodeTools.hpp`, after the `findNode` declaration (before `} // namespace node_tools`):

```cpp
/**
 * Append a Node.js option to an existing NODE_OPTIONS value.
 *
 * Preserves the caller's value verbatim and avoids adding a duplicate flag; the
 * option is appended after a single space when absent.
 *
 * @param existingOptions The current NODE_OPTIONS value (may be empty).
 * @param option The flag to append, e.g. "--use-system-ca".
 * @return The merged NODE_OPTIONS value.
 */
std::string appendNodeOption(const std::string& existingOptions,
                             const std::string& option);

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

- [ ] **Step 4: Implement the helpers**

In `src/cpp/session/modules/SessionNodeTools.cpp`, add to the include block (after the existing `#include <core/system/System.hpp>`):

```cpp
#include <core/system/Process.hpp>

#include <boost/algorithm/string/trim.hpp>

#include <cctype>
#include <cstdio>
```

Then add the implementations before `} // namespace node_tools` (after `findNode`):

```cpp
namespace {

// Whether `token` appears in `options` as a whole, whitespace-delimited token
// (so "--use-system-ca" does not match inside "--use-system-cafoo").
bool containsOptionToken(const std::string& options, const std::string& token)
{
   std::string::size_type pos = 0;
   while ((pos = options.find(token, pos)) != std::string::npos)
   {
      bool atStart = (pos == 0) ||
         std::isspace(static_cast<unsigned char>(options[pos - 1]));
      std::string::size_type end = pos + token.size();
      bool atEnd = (end == options.size()) ||
         std::isspace(static_cast<unsigned char>(options[end]));
      if (atStart && atEnd)
         return true;
      pos = end;
   }
   return false;
}

} // anonymous namespace

std::string appendNodeOption(const std::string& existingOptions,
                             const std::string& option)
{
   // Preserve the caller's NODE_OPTIONS verbatim (quoted values may contain
   // intentional whitespace); only append our flag when it is not already
   // present as a whole token. No tokenize/rejoin, so nothing is rewritten.
   if (containsOptionToken(existingOptions, option))
      return existingOptions;
   if (existingOptions.empty())
      return option;
   return existingOptions + " " + option;
}

bool parseNodeVersion(const std::string& versionOutput, int* pMajor, int* pMinor)
{
   std::string trimmed = boost::algorithm::trim_copy(versionOutput);
   if (!trimmed.empty() && (trimmed[0] == 'v' || trimmed[0] == 'V'))
      trimmed = trimmed.substr(1);

   int major = 0, minor = 0;
   if (std::sscanf(trimmed.c_str(), "%d.%d", &major, &minor) != 2)
      return false;

   *pMajor = major;
   *pMinor = minor;
   return true;
}

bool nodeSupportsSystemCa(const core::FilePath& nodePath)
{
   core::system::ProcessOptions options;
   core::system::ProcessResult result;
   Error error = core::system::runProgram(nodePath.getAbsolutePath(),
                                          { "--version" },
                                          options,
                                          &result);
   if (error)
   {
      WLOG("Could not determine node version at '{}': {}",
           nodePath.getAbsolutePath(), error.getMessage());
      return false;
   }

   int major = 0, minor = 0;
   if (!parseNodeVersion(result.stdOut, &major, &minor))
   {
      WLOG("Could not parse node version from output '{}'.", result.stdOut);
      return false;
   }

   // --use-system-ca via NODE_OPTIONS requires Node >= 22.17.0.
   bool supported = (major > 22) || (major == 22 && minor >= 17);
   if (!supported)
   {
      WLOG("Node {}.{} at '{}' does not support --use-system-ca via NODE_OPTIONS "
           "(requires 22.17.0+); the system certificate store will not be trusted.",
           major, minor, nodePath.getAbsolutePath());
   }

   return supported;
}
```

- [ ] **Step 5: Build and run the tests**

Run: `cd build && ninja && R_HOME=C:/opt/R/R-4.6.0 ./src/cpp/rstudio-tests.bat rsession`
Expected: PASS — all `SessionNodeToolsTest` cases green.

- [ ] **Step 6: Commit**

```bash
git add src/cpp/session/modules/SessionNodeTools.hpp \
        src/cpp/session/modules/SessionNodeTools.cpp \
        src/cpp/session/modules/SessionNodeToolsTests.cpp
git commit -m "Add node_tools NODE_OPTIONS and version helpers (#17892)"
```

---

### Task 2: Add the preference and regenerate accessors

**Files:**
- Modify: `src/cpp/session/resources/schema/user-prefs-schema.json:1889`
- Regenerated: `UserPrefValues.hpp/.cpp`, `UserPrefsAccessor.java`, `UserPrefsAccessorConstants.java`

- [ ] **Step 1: Add the schema entry**

In `src/cpp/session/resources/schema/user-prefs-schema.json`, insert this block immediately after the `assistant_toolbar_button_visible` entry's closing `},` (line 1889) and before `"posit_assistant_test_manifest"`:

```json
        "assistant_use_system_ca": {
            "type": "boolean",
            "default": false,
            "title": "Use the system certificate store (restart required)",
            "description": "When enabled, the AI assistant agents trust the operating system certificate store (e.g. the Windows Certificate Store or macOS Keychain) in addition to Node.js's built-in certificate authorities. Useful behind a TLS-inspecting proxy. Restart the R session for the change to take effect."
        },
```

- [ ] **Step 2: Regenerate accessors**

Run: `Rscript scripts/generate-prefs.R`
Expected: no errors; `git status` shows modifications to `UserPrefValues.hpp`, `UserPrefValues.cpp`, `UserPrefsAccessor.java`, `UserPrefsAccessorConstants.java`.

- [ ] **Step 3: Verify the generated accessors exist**

Run: `grep -rn "assistantUseSystemCa\|kAssistantUseSystemCa\|ASSISTANT_USE_SYSTEM_CA" src/cpp/session/include/session/prefs/UserPrefValues.hpp src/gwt/src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessor.java`
Expected: `bool assistantUseSystemCa();`, `#define kAssistantUseSystemCa "assistant_use_system_ca"` (C++), and `public PrefValue<Boolean> assistantUseSystemCa()` plus the `ASSISTANT_USE_SYSTEM_CA` constant (Java).

- [ ] **Step 4: Build to confirm generated C++ compiles**

Run: `cd build && ninja`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/cpp/session/resources/schema/user-prefs-schema.json \
        src/cpp/session/include/session/prefs/UserPrefValues.hpp \
        src/cpp/session/prefs/UserPrefValues.cpp \
        src/gwt/src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessor.java \
        src/gwt/src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessorConstants.java
git commit -m "Add assistant_use_system_ca preference (#17892)"
```

---

### Task 3: Wire the Copilot agent launch (SessionAssistant.cpp)

**Files:**
- Modify: `src/cpp/session/modules/SessionAssistant.cpp` (in `startAgent`, just after the node binary is resolved/validated, ~line 1230)

- [ ] **Step 1: Add the guarded injection**

In `src/cpp/session/modules/SessionAssistant.cpp::startAgent`, find:

```cpp
   DLOG("Using node.js at '{}'.", nodePath.getAbsolutePath());

   // Set up process callbacks
```

Insert the block between those two lines so it reads:

```cpp
   DLOG("Using node.js at '{}'.", nodePath.getAbsolutePath());

   // Trust the OS certificate store when the user has opted in. Additive to
   // NODE_EXTRA_CA_CERTS; preserves any NODE_OPTIONS already in the environment.
   // Guarded on the resolved node version: NODE_OPTIONS=--use-system-ca is
   // rejected by node < 22.17.0, which would otherwise break agent startup.
   if (prefs::userPrefs().assistantUseSystemCa() &&
       node_tools::nodeSupportsSystemCa(nodePath))
   {
      std::string nodeOptions = core::system::getenv(environment, "NODE_OPTIONS");
      core::system::setenv(&environment, "NODE_OPTIONS",
                           node_tools::appendNodeOption(nodeOptions, "--use-system-ca"));
   }

   // Set up process callbacks
```

This sits before the `options.environment = environment;` assignments in all
three launch branches (direct, copilot-helper, posit-helper), so every branch
picks up the modified `environment`. No new includes are needed
(`SessionNodeTools.hpp`, `core/system/Process.hpp`, `session/prefs/UserPrefs.hpp`
are already included).

- [ ] **Step 2: Build**

Run: `cd build && ninja`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/cpp/session/modules/SessionAssistant.cpp
git commit -m "Apply system-CA NODE_OPTIONS to Copilot agent (#17892)"
```

---

### Task 4: Wire the chat backend launch (SessionChat.cpp)

**Files:**
- Modify: `src/cpp/session/modules/SessionChat.cpp` (in `startChatBackend`, in the environment block, just after the `NODE_USE_ENV_PROXY` line, ~line 4801)

- [ ] **Step 1: Add the guarded injection**

In `src/cpp/session/modules/SessionChat.cpp::startChatBackend`, find:

```cpp
   core::system::setenv(&environment, "NODE_USE_ENV_PROXY", "1");

   // Pass per-session auth token for WebSocket authentication
```

Insert the block between those lines so it reads:

```cpp
   core::system::setenv(&environment, "NODE_USE_ENV_PROXY", "1");

   // Trust the OS certificate store when the user has opted in. Additive to
   // NODE_EXTRA_CA_CERTS; preserves any NODE_OPTIONS already in the environment.
   // Guarded on the resolved node version: NODE_OPTIONS=--use-system-ca is
   // rejected by node < 22.17.0, which would otherwise break backend startup.
   if (prefs::userPrefs().assistantUseSystemCa() &&
       node_tools::nodeSupportsSystemCa(nodePath))
   {
      std::string nodeOptions = core::system::getenv(environment, "NODE_OPTIONS");
      core::system::setenv(&environment, "NODE_OPTIONS",
                           node_tools::appendNodeOption(nodeOptions, "--use-system-ca"));
   }

   // Pass per-session auth token for WebSocket authentication
```

`nodePath` is already resolved earlier in `startChatBackend` (via `findNode`,
~line 4689) and is in scope. `SessionNodeTools.hpp` and `session/prefs/UserPrefs.hpp`
are already included — no new includes.

- [ ] **Step 2: Build**

Run: `cd build && ninja`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/cpp/session/modules/SessionChat.cpp
git commit -m "Apply system-CA NODE_OPTIONS to chat backend (#17892)"
```

---

### Task 5: Add the preferences-pane checkbox (AssistantPreferencesPane.java)

**Files:**
- Modify: `src/gwt/src/org/rstudio/studio/client/workbench/prefs/views/AssistantPreferencesPane.java`

- [ ] **Step 1: Declare the field and initial-value tracker**

In the `// UI` field block (near `cbAssistantToolbarButtonVisible_`, ~line 1439), add:

```java
   private final CheckBox cbAssistantUseSystemCa_;
```

In the `// State` field block (near `assistantStartupError_`, ~line 1412), add:

```java
   private boolean initialUseSystemCa_;
```

- [ ] **Step 2: Construct the checkbox**

In the constructor, next to the other `checkboxPref` creations (after `cbAssistantToolbarButtonVisible_ = checkboxPref(...)`, ~line 224), add:

```java
      cbAssistantUseSystemCa_ = checkboxPref(prefs_.assistantUseSystemCa(), true);
```

- [ ] **Step 3: Add the checkbox to the layout**

In `initDisplay()`, find `add(cbAssistantToolbarButtonVisible_);` (~line 353) and add the new checkbox directly after it:

```java
      add(cbAssistantToolbarButtonVisible_);
      add(cbAssistantUseSystemCa_);
```

- [ ] **Step 4: Capture the initial value**

At the top of `initialize(UserPrefs prefs)` (~line 1231, before the migration block), add:

```java
      initialUseSystemCa_ = prefs.assistantUseSystemCa().getValue();
```

- [ ] **Step 5: Request a session restart on change in onApply**

Replace the current tail of `onApply` (`return super.onApply(prefs);`, ~line 108) with:

```java
      RestartRequirement restartRequirement = super.onApply(prefs);

      // The system-CA checkbox value is applied by the base class (checkboxPref).
      // The agents read NODE_OPTIONS only at launch, so a change requires an R
      // session restart to take effect.
      if (cbAssistantUseSystemCa_.getValue() != initialUseSystemCa_)
      {
         initialUseSystemCa_ = cbAssistantUseSystemCa_.getValue();
         restartRequirement.setSessionRestartRequired(true);
      }

      return restartRequirement;
```

- [ ] **Step 6: Compile-check the Java**

Run: `cd src/gwt && ant javac`
Expected: BUILD SUCCESSFUL, no compile errors.

- [ ] **Step 7: Commit**

```bash
git add src/gwt/src/org/rstudio/studio/client/workbench/prefs/views/AssistantPreferencesPane.java
git commit -m "Add system-CA checkbox to Assistant preferences (#17892)"
```

---

### Task 6: Document the setting (copilot.qmd)

**Files:**
- Modify: `docs/user/rstudio/ide/guide/tools/copilot.qmd:71`

- [ ] **Step 1: Add the Desktop/system-store guidance**

In `docs/user/rstudio/ide/guide/tools/copilot.qmd`, find the paragraph ending the certificates section (line 71):

```
in your `/etc/rstudio/rsession.conf`. Note that this is functionally equivalent to setting the `NODE_EXTRA_CA_CERTS` environment variable for the GitHub Copilot agent process -- see https://nodejs.org/api/cli.html#node_extra_ca_certsfile for more details.
```

Insert the following two paragraphs immediately after that line (before `### External Services Used by GitHub Copilot`):

```
On RStudio Desktop, where `rsession.conf` is not used, enable **Tools > Global Options > Assistant > "Use the system certificate store"** instead. When enabled, RStudio trusts the operating system certificate store (the Windows Certificate Store or macOS Keychain) in addition to Node.js's built-in certificate authorities, which is usually all that is needed behind a TLS-inspecting proxy whose root CA the machine already trusts. The setting applies to both the GitHub Copilot agent and the Posit Assistant. Restart the R session for the change to take effect.

This option is additive: it supplements `NODE_EXTRA_CA_CERTS` and `copilot-ssl-certificates-file` rather than replacing them. It requires the bundled Node.js (or any configured Node.js 22.17.0 or newer); with an older configured Node.js the option is ignored.
```

- [ ] **Step 2: Commit**

```bash
git add docs/user/rstudio/ide/guide/tools/copilot.qmd
git commit -m "Document system certificate store option (#17892)"
```

---

### Task 7: Add the NEWS.md entry

**Files:**
- Modify: `NEWS.md:9` (end of the `### New` list)

- [ ] **Step 1: Add the entry**

In `NEWS.md`, add as the last bullet of the `### New` section (after line 8, before the blank line preceding `### Fixed`):

```
- ([#17892](https://github.com/rstudio/rstudio/issues/17892)): Added a "Use the system certificate store" option (Tools > Global Options > Assistant) so the GitHub Copilot agent and Posit Assistant trust the operating system certificate store (Windows Certificate Store, macOS Keychain) when connecting -- useful behind a TLS-inspecting proxy on RStudio Desktop.
```

- [ ] **Step 2: Commit**

```bash
git add NEWS.md
git commit -m "Add NEWS entry for system certificate store option (#17892)"
```

---

### Task 8: Pre-PR cleanup (run only when ready to open the PR)

**Files:**
- Modify: `.gitignore`
- Remove from the branch: `docs/superpowers/specs/2026-06-09-assistant-system-ca-design.md`, `docs/superpowers/plans/2026-06-09-assistant-system-ca.md`

> Do this last, after all implementation commits and their roborev reviews are addressed. The spec/plan were committed only so roborev could review them.

- [ ] **Step 1: Restore the .gitignore entry**

In `.gitignore`, replace the temporary comment block:

```
# TEMPORARY: un-ignored so design spec/plan docs can be committed for roborev
# review. MUST be restored (and docs/superpowers removed) before opening the PR.
# docs/superpowers
```

with the original single line:

```
docs/superpowers
```

- [ ] **Step 2: Remove the tracked design docs**

```bash
git rm docs/superpowers/specs/2026-06-09-assistant-system-ca-design.md \
       docs/superpowers/plans/2026-06-09-assistant-system-ca.md
```

- [ ] **Step 3: Verify the docs are untracked and re-ignored**

Run: `git status --porcelain docs/superpowers; git check-ignore docs/superpowers/x`
Expected: the two files show as deleted/staged; `git check-ignore` prints `docs/superpowers/x` (the path is ignored again).

- [ ] **Step 4: Commit**

```bash
git add .gitignore
git commit -m "Restore docs/superpowers gitignore and drop design docs (#17892)"
```

- [ ] **Step 5: Update the project memory**

Mark `project-assistant-system-ca` memory as cleanup-done so it is not flagged again.

---

## Self-Review

**Spec coverage:**
- Pref schema -> Task 2. Generated accessors -> Task 2. Merge helper + version helpers -> Task 1. Both launch sites -> Tasks 3, 4. Pane checkbox + session-restart -> Task 5. Unit tests -> Task 1. Docs -> Task 6. NEWS -> Task 7. Version guard (the spec's key addition) -> Task 1 (`nodeSupportsSystemCa`/`parseNodeVersion`) + Tasks 3/4 (gate). gitignore/doc cleanup obligation -> Task 8. All spec sections map to a task.
- Spec correction noted: the spec said "SessionChat.cpp adds the include" — it already includes `SessionNodeTools.hpp` and `UserPrefs.hpp`, so Task 4 adds no include. (Harmless discrepancy; plan is authoritative.)

**Placeholder scan:** No TBD/TODO; every code step shows complete code; every command lists expected output.

**Type/name consistency:** `appendNodeOption(const std::string&, const std::string&)`, `parseNodeVersion(const std::string&, int*, int*)`, `nodeSupportsSystemCa(const core::FilePath&)`, `prefs::userPrefs().assistantUseSystemCa()`, `prefs_.assistantUseSystemCa()`, `cbAssistantUseSystemCa_`, `initialUseSystemCa_` — used identically across Tasks 1-5. Gate threshold (`>= 22.17.0`) matches the schema title's "restart required" wording and the docs note.
