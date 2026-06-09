# Manifest Download Throttling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Throttle the Posit Assistant `manifest.json` download to at most ~once per day (always fetching when nothing is installed or the installed protocol mismatches), and use a compatible installed version without error when a fetch fails.

**Architecture:** A new pure, unit-tested `chat::throttle` component owns the throttle decision and the persisted `manifest-check.json` record. `SessionChat.cpp` gates `startUpdateCheck` on that decision, reapplies a persisted "unsupported" block when skipping or on fetch failure, and bumps the persisted timestamp on every real fetch completion.

**Tech Stack:** C++ (Allman, 3-space indent), Boost, RStudio `shared_core` JSON/FilePath, Google Test.

**Design spec:** `docs/superpowers/specs/2026-06-09-manifest-download-throttle-design.md`

**Branch:** `feature/manifest-download-throttle` (already checked out).

---

## File Structure

- **Create** `src/cpp/session/modules/chat/ChatUpdateThrottle.hpp` — types (`ManifestCheckRecord`, `ResolvedBlock`, `SuccessOutcome`) and pure function declarations.
- **Create** `src/cpp/session/modules/chat/ChatUpdateThrottle.cpp` — implementations + record file I/O.
- **Create** `src/cpp/session/modules/chat/ChatUpdateThrottleTests.cpp` — Google Test unit tests (auto-globbed by CMake, no CMake edit needed).
- **Modify** `src/cpp/session/CMakeLists.txt` — add the new `.cpp` to the explicit `SESSION_SOURCE_FILES` list (the `.hpp` is glob-included via `*.h*`).
- **Modify** `src/cpp/session/modules/SessionChat.cpp` — include + `using` declarations; extract `drainPendingCompletions()`; rework `onUpdateCheckComplete`; add `force` parameter, `shouldFetchManifest`, `resolveWithoutManifestFetch`, and the throttle gate; update the three callers.

### Build / test commands (used throughout)

- Build (reconfigures automatically when `CMakeLists.txt` changes):
  `cd build && cmake --build . --target rsession`
  (If `build/` does not exist: `mkdir build && cd build && cmake -DCMAKE_EXPORT_COMPILE_COMMANDS=1 ..` — the dev configuration enables `RSTUDIO_UNIT_TESTS_ENABLED`.)
- Run the unit tests for this component (see project CLAUDE.md):
  `./rstudio-tests --scope rsession --filter "ChatUpdateThrottle*"`

---

## Task 1: `ChatUpdateThrottle` component + unit tests

**Files:**
- Create: `src/cpp/session/modules/chat/ChatUpdateThrottle.hpp`
- Create: `src/cpp/session/modules/chat/ChatUpdateThrottle.cpp`
- Test: `src/cpp/session/modules/chat/ChatUpdateThrottleTests.cpp`
- Modify: `src/cpp/session/CMakeLists.txt:185-191`

- [ ] **Step 1: Create the header**

Create `src/cpp/session/modules/chat/ChatUpdateThrottle.hpp`:

```cpp
/*
 * ChatUpdateThrottle.hpp
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

#ifndef SESSION_CHAT_UPDATE_THROTTLE_HPP
#define SESSION_CHAT_UPDATE_THROTTLE_HPP

#include <ctime>
#include <string>

#include <boost/optional.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace throttle {

// Default throttle window: 24 hours.
extern const int kManifestCheckThrottleSeconds;

// Persisted manifest-check record. The two bool flags store only MANIFEST-derived
// decisions; the local protocol.json mismatch is never persisted (it is recomputed
// at reapply time). installedVersion / rstudioProtocol record the context the flags
// were computed for, so a stale block is never applied to a different install.
struct ManifestCheckRecord
{
   std::time_t lastCheckTime = 0;
   std::string installedVersion;
   std::string rstudioProtocol;
   bool unsupportedInstalledVersion = false;
   bool unsupportedProtocol = false;
};

// Persisted unsupported flags resolved against the current install.
struct ResolvedBlock
{
   bool unsupportedInstalledVersion = false;
   bool unsupportedProtocol = false;
};

// The live flag (for s_updateState) and the record (to persist) for a successful
// manifest check, derived from the same inputs.
struct SuccessOutcome
{
   bool liveUnsupportedInstalledVersion = false;
   ManifestCheckRecord record;
};

// Path of the persisted record: <userDataDir>/pai/manifest-check.json.
core::FilePath manifestCheckStatePath();

// Read the record. Returns none on missing / unreadable / malformed file, or when
// the required lastCheckTime field is absent.
boost::optional<ManifestCheckRecord> readManifestCheckRecord(const core::FilePath& stateFile);

// Write the record (ensureDirectory on the parent first).
core::Error writeManifestCheckRecord(const core::FilePath& stateFile,
                                     const ManifestCheckRecord& record);

// Preserve a prior record's flags + context, bumping only lastCheckTime. Returns a
// default record (flags false, empty context) with `now` when prior is none.
ManifestCheckRecord bumpRecord(boost::optional<ManifestCheckRecord> prior,
                               std::time_t now);

// Pure: is a manifest fetch due now?
//   force || !installed || protocolMismatch || !lastCheckTime ||
//   (now - *lastCheckTime) >= throttleSeconds
bool manifestCheckDue(bool force,
                      bool installed,
                      bool protocolMismatch,
                      boost::optional<std::time_t> lastCheckTime,
                      std::time_t now,
                      int throttleSeconds);

// Pure: resolve which persisted flags still apply to the current install.
// unsupportedProtocol is kept when the stored RStudio protocol matches;
// unsupportedInstalledVersion is kept only when both the stored installed version
// and RStudio protocol match.
ResolvedBlock resolvePersistedBlock(const ManifestCheckRecord& record,
                                    const std::string& installedVersion,
                                    const std::string& rstudioProtocol);

// Pure: split a successful check's result into the live composite flag
// (versionUnsupported || protocolMismatch) and the manifest-only record.
SuccessOutcome buildSuccessOutcome(std::time_t now,
                                   const std::string& installedVersion,
                                   const std::string& rstudioProtocol,
                                   bool versionUnsupported,
                                   bool protocolMismatch,
                                   bool unsupportedProtocol);

} // namespace throttle
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_UPDATE_THROTTLE_HPP
```

- [ ] **Step 2: Create a stub implementation (so the build links but tests fail)**

Create `src/cpp/session/modules/chat/ChatUpdateThrottle.cpp` with stub bodies that return defaults. This compiles and links so the tests run and fail on assertions (red):

```cpp
/*
 * ChatUpdateThrottle.cpp
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

#include "ChatUpdateThrottle.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace throttle {

const int kManifestCheckThrottleSeconds = 24 * 60 * 60;

core::FilePath manifestCheckStatePath() { return core::FilePath(); }

boost::optional<ManifestCheckRecord> readManifestCheckRecord(const core::FilePath&)
{
   return boost::none;
}

core::Error writeManifestCheckRecord(const core::FilePath&, const ManifestCheckRecord&)
{
   return Success();
}

ManifestCheckRecord bumpRecord(boost::optional<ManifestCheckRecord>, std::time_t)
{
   return ManifestCheckRecord();
}

bool manifestCheckDue(bool, bool, bool, boost::optional<std::time_t>, std::time_t, int)
{
   return false;
}

ResolvedBlock resolvePersistedBlock(const ManifestCheckRecord&,
                                    const std::string&,
                                    const std::string&)
{
   return ResolvedBlock();
}

SuccessOutcome buildSuccessOutcome(std::time_t, const std::string&, const std::string&,
                                   bool, bool, bool)
{
   return SuccessOutcome();
}

} // namespace throttle
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
```

- [ ] **Step 3: Register the new source in CMake**

In `src/cpp/session/CMakeLists.txt`, add the new `.cpp` to the explicit source list. Change:

```cmake
   modules/chat/ChatInstallation.cpp
   modules/chat/ChatIntegrity.cpp
   modules/chat/ChatStaticFiles.cpp
```

to:

```cmake
   modules/chat/ChatInstallation.cpp
   modules/chat/ChatIntegrity.cpp
   modules/chat/ChatUpdateThrottle.cpp
   modules/chat/ChatStaticFiles.cpp
```

(The header is picked up by the `*.h*` glob and the test by the `*Tests.cpp` glob; no further CMake edits.)

- [ ] **Step 4: Write the unit tests**

Create `src/cpp/session/modules/chat/ChatUpdateThrottleTests.cpp`:

```cpp
/*
 * ChatUpdateThrottleTests.cpp
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

#include "ChatUpdateThrottle.hpp"

#include <ctime>

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::throttle;

namespace {

const std::time_t kNow = 1000000000;
const int kDay = 24 * 60 * 60;

ManifestCheckRecord makeRecord(std::time_t t,
                               const std::string& ver,
                               const std::string& proto,
                               bool unsupVer,
                               bool unsupProto)
{
   ManifestCheckRecord r;
   r.lastCheckTime = t;
   r.installedVersion = ver;
   r.rstudioProtocol = proto;
   r.unsupportedInstalledVersion = unsupVer;
   r.unsupportedProtocol = unsupProto;
   return r;
}

} // anonymous namespace

// ---- manifestCheckDue ----

TEST(ChatUpdateThrottle, DueWhenForced)
{
   EXPECT_TRUE(manifestCheckDue(true, true, false, kNow, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenNotInstalled)
{
   EXPECT_TRUE(manifestCheckDue(false, false, false, kNow, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenProtocolMismatch)
{
   EXPECT_TRUE(manifestCheckDue(false, true, true, kNow, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenNoPriorCheck)
{
   EXPECT_TRUE(manifestCheckDue(false, true, false, boost::none, kNow, kDay));
}

TEST(ChatUpdateThrottle, NotDueWithinWindow)
{
   std::time_t last = kNow - (kDay - 1);
   EXPECT_FALSE(manifestCheckDue(false, true, false, last, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueAtWindowBoundary)
{
   std::time_t last = kNow - kDay;
   EXPECT_TRUE(manifestCheckDue(false, true, false, last, kNow, kDay));
}

TEST(ChatUpdateThrottle, DuePastWindow)
{
   std::time_t last = kNow - (kDay + 100);
   EXPECT_TRUE(manifestCheckDue(false, true, false, last, kNow, kDay));
}

// ---- resolvePersistedBlock ----

TEST(ChatUpdateThrottle, ResolveBothPreservedWhenContextMatches)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", true, true);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.3", "10.0");
   EXPECT_TRUE(b.unsupportedInstalledVersion);
   EXPECT_TRUE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, ResolveVersionBlockDroppedOnVersionChange)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", true, true);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.4", "10.0");
   EXPECT_FALSE(b.unsupportedInstalledVersion);
   EXPECT_TRUE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, ResolveBothDroppedOnProtocolChange)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", true, true);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.3", "11.0");
   EXPECT_FALSE(b.unsupportedInstalledVersion);
   EXPECT_FALSE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, ResolveFalseFlagsStayFalse)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", false, false);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.3", "10.0");
   EXPECT_FALSE(b.unsupportedInstalledVersion);
   EXPECT_FALSE(b.unsupportedProtocol);
}

// ---- buildSuccessOutcome ----

TEST(ChatUpdateThrottle, OutcomePersistsManifestOnlyNotComposite)
{
   // versionUnsupported=false but protocolMismatch=true: live blocked, persisted NOT.
   SuccessOutcome o = buildSuccessOutcome(kNow, "1.2.3", "10.0", false, true, false);
   EXPECT_TRUE(o.liveUnsupportedInstalledVersion);
   EXPECT_FALSE(o.record.unsupportedInstalledVersion);
}

TEST(ChatUpdateThrottle, OutcomeVersionUnsupportedPersists)
{
   SuccessOutcome o = buildSuccessOutcome(kNow, "1.2.3", "10.0", true, false, false);
   EXPECT_TRUE(o.liveUnsupportedInstalledVersion);
   EXPECT_TRUE(o.record.unsupportedInstalledVersion);
}

TEST(ChatUpdateThrottle, OutcomeCarriesContext)
{
   SuccessOutcome o = buildSuccessOutcome(kNow, "1.2.3", "10.0", false, false, true);
   EXPECT_FALSE(o.liveUnsupportedInstalledVersion);
   EXPECT_EQ(o.record.lastCheckTime, kNow);
   EXPECT_EQ(o.record.installedVersion, "1.2.3");
   EXPECT_EQ(o.record.rstudioProtocol, "10.0");
   EXPECT_TRUE(o.record.unsupportedProtocol);
}

// ---- bumpRecord ----

TEST(ChatUpdateThrottle, BumpPreservesPriorAndBumpsTime)
{
   ManifestCheckRecord prior = makeRecord(kNow - kDay, "1.2.3", "10.0", true, false);
   ManifestCheckRecord b = bumpRecord(prior, kNow);
   EXPECT_EQ(b.lastCheckTime, kNow);
   EXPECT_EQ(b.installedVersion, "1.2.3");
   EXPECT_EQ(b.rstudioProtocol, "10.0");
   EXPECT_TRUE(b.unsupportedInstalledVersion);
   EXPECT_FALSE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, BumpWithNoPriorReturnsDefaultWithTime)
{
   ManifestCheckRecord b = bumpRecord(boost::none, kNow);
   EXPECT_EQ(b.lastCheckTime, kNow);
   EXPECT_TRUE(b.installedVersion.empty());
   EXPECT_FALSE(b.unsupportedInstalledVersion);
}

// ---- read / write round-trip ----

TEST(ChatUpdateThrottle, RecordRoundTrip)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   ManifestCheckRecord in = makeRecord(kNow, "1.2.3", "10.0", true, false);
   ASSERT_FALSE(writeManifestCheckRecord(tmp, in));

   boost::optional<ManifestCheckRecord> out = readManifestCheckRecord(tmp);
   ASSERT_TRUE(out);
   EXPECT_EQ(out->lastCheckTime, kNow);
   EXPECT_EQ(out->installedVersion, "1.2.3");
   EXPECT_EQ(out->rstudioProtocol, "10.0");
   EXPECT_TRUE(out->unsupportedInstalledVersion);
   EXPECT_FALSE(out->unsupportedProtocol);
   tmp.removeIfExists();
}

TEST(ChatUpdateThrottle, ReadMissingFileReturnsNone)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   tmp.removeIfExists();
   EXPECT_FALSE(readManifestCheckRecord(tmp));
}

TEST(ChatUpdateThrottle, ReadMalformedReturnsNone)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   ASSERT_FALSE(writeStringToFile(tmp, "not json"));
   EXPECT_FALSE(readManifestCheckRecord(tmp));
   tmp.removeIfExists();
}

TEST(ChatUpdateThrottle, StatePathHasExpectedNameAndParent)
{
   FilePath path = manifestCheckStatePath();
   EXPECT_EQ(path.getFilename(), "manifest-check.json");
   EXPECT_EQ(path.getParent().getFilename(), "pai");
}
```

- [ ] **Step 5: Build and run the tests — expect RED**

Run: `cd build && cmake --build . --target rsession`
Then: `./rstudio-tests --scope rsession --filter "ChatUpdateThrottle*"`
Expected: the suite builds and runs, with multiple FAILURES (stub returns defaults — e.g. `DueWhenForced` fails because the stub returns `false`, `RecordRoundTrip` fails because the stub read returns none, `StatePathHasExpectedNameAndParent` fails because the stub path is empty).

- [ ] **Step 6: Replace the stub bodies with real implementations**

Replace the body of `src/cpp/session/modules/chat/ChatUpdateThrottle.cpp` (keep the copyright header) with:

```cpp
#include "ChatUpdateThrottle.hpp"

#include <string>

#include <shared_core/json/Json.hpp>

#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/Xdg.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace throttle {

const int kManifestCheckThrottleSeconds = 24 * 60 * 60;

core::FilePath manifestCheckStatePath()
{
   return core::system::xdg::userDataDir()
      .completePath("pai")
      .completeChildPath("manifest-check.json");
}

boost::optional<ManifestCheckRecord> readManifestCheckRecord(const core::FilePath& stateFile)
{
   if (!stateFile.exists())
      return boost::none;

   std::string content;
   Error error = core::readStringFromFile(stateFile, &content);
   if (error)
      return boost::none;

   json::Value value;
   if (value.parse(content))
      return boost::none;
   if (!value.isObject())
      return boost::none;

   json::Object obj = value.getObject();

   // lastCheckTime is required, stored as a decimal-seconds string for portability.
   std::string lastCheckStr;
   if (json::readObject(obj, "lastCheckTime", lastCheckStr))
      return boost::none;

   ManifestCheckRecord record;
   record.lastCheckTime = static_cast<std::time_t>(
      core::safe_convert::stringTo<long long>(lastCheckStr, 0));

   // Context + flags are optional: a missing key leaves the struct default in place
   // (the boost::optional<T> readObject overload reports absence as Success/none).
   boost::optional<std::string> installedVersion;
   boost::optional<std::string> rstudioProtocol;
   boost::optional<bool> unsupportedInstalledVersion;
   boost::optional<bool> unsupportedProtocol;
   json::readObject(obj, "installedVersion", installedVersion);
   json::readObject(obj, "rstudioProtocol", rstudioProtocol);
   json::readObject(obj, "unsupportedInstalledVersion", unsupportedInstalledVersion);
   json::readObject(obj, "unsupportedProtocol", unsupportedProtocol);
   if (installedVersion)
      record.installedVersion = *installedVersion;
   if (rstudioProtocol)
      record.rstudioProtocol = *rstudioProtocol;
   if (unsupportedInstalledVersion)
      record.unsupportedInstalledVersion = *unsupportedInstalledVersion;
   if (unsupportedProtocol)
      record.unsupportedProtocol = *unsupportedProtocol;

   return record;
}

core::Error writeManifestCheckRecord(const core::FilePath& stateFile,
                                     const ManifestCheckRecord& record)
{
   Error error = stateFile.getParent().ensureDirectory();
   if (error)
      return error;

   json::Object obj;
   obj["lastCheckTime"] = std::to_string(static_cast<long long>(record.lastCheckTime));
   obj["installedVersion"] = record.installedVersion;
   obj["rstudioProtocol"] = record.rstudioProtocol;
   obj["unsupportedInstalledVersion"] = record.unsupportedInstalledVersion;
   obj["unsupportedProtocol"] = record.unsupportedProtocol;
   return core::writeStringToFile(stateFile, obj.write());
}

ManifestCheckRecord bumpRecord(boost::optional<ManifestCheckRecord> prior,
                               std::time_t now)
{
   ManifestCheckRecord out = prior.value_or(ManifestCheckRecord());
   out.lastCheckTime = now;
   return out;
}

bool manifestCheckDue(bool force,
                      bool installed,
                      bool protocolMismatch,
                      boost::optional<std::time_t> lastCheckTime,
                      std::time_t now,
                      int throttleSeconds)
{
   if (force || !installed || protocolMismatch)
      return true;
   if (!lastCheckTime)
      return true;
   std::time_t elapsed = now - *lastCheckTime;
   return elapsed >= static_cast<std::time_t>(throttleSeconds);
}

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
   out.record.unsupportedInstalledVersion = versionUnsupported;
   out.record.unsupportedProtocol = unsupportedProtocol;
   return out;
}

} // namespace throttle
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
```

- [ ] **Step 7: Build and run the tests — expect GREEN**

Run: `cd build && cmake --build . --target rsession`
Then: `./rstudio-tests --scope rsession --filter "ChatUpdateThrottle*"`
Expected: all `ChatUpdateThrottle.*` tests PASS.

- [ ] **Step 8: Commit**

```bash
git add src/cpp/session/modules/chat/ChatUpdateThrottle.hpp \
        src/cpp/session/modules/chat/ChatUpdateThrottle.cpp \
        src/cpp/session/modules/chat/ChatUpdateThrottleTests.cpp \
        src/cpp/session/CMakeLists.txt
git commit -m "Add ChatUpdateThrottle component for manifest-check throttling"
```

---

## Task 2: Wire-in groundwork — include, `using` decls, `drainPendingCompletions()`

This is a behavior-preserving refactor: include the new header, add `using` declarations, and extract the completion-draining tail of `onUpdateCheckComplete`'s `finish()` into a reusable helper.

**Files:**
- Modify: `src/cpp/session/modules/SessionChat.cpp`

- [ ] **Step 1: Add the header include**

In `src/cpp/session/modules/SessionChat.cpp`, change:

```cpp
#include "chat/ChatIntegrity.hpp"
#include "chat/ChatStaticFiles.hpp"
```

to:

```cpp
#include "chat/ChatIntegrity.hpp"
#include "chat/ChatStaticFiles.hpp"
#include "chat/ChatUpdateThrottle.hpp"
```

- [ ] **Step 2: Add the `<ctime>` standard include**

Change:

```cpp
#include <atomic>
#include <chrono>
#include <map>
```

to:

```cpp
#include <atomic>
#include <chrono>
#include <ctime>
#include <map>
```

- [ ] **Step 3: Add `using` declarations for the throttle types**

After the installation `using` block, change:

```cpp
using chat_installation::getInstalledVersion;
using chat_installation::getInstalledProtocolVersion;
```

to:

```cpp
using chat_installation::getInstalledVersion;
using chat_installation::getInstalledProtocolVersion;

// Update throttle types used throughout
using throttle::ManifestCheckRecord;
using throttle::ResolvedBlock;
using throttle::SuccessOutcome;
```

- [ ] **Step 4: Define `drainPendingCompletions()` near the single-flight state**

Immediately after the `std::vector<boost::function<void()>> s_pendingCompletions;` declaration (in the single-flight state block), insert:

```cpp

// Reset single-flight state and run queued completions. Swap first so a completion
// that kicks a fresh check sees a clean queue. Main-thread only.
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

- [ ] **Step 5: Use `drainPendingCompletions()` inside `finish()`**

In `onUpdateCheckComplete`, change the tail of the `finish` lambda from:

```cpp
      // Reset single-flight state and run queued completions. Swap first so a
      // completion that kicks a fresh check sees a clean queue.
      s_checkInProgress = false;
      s_checkIncludesStartup = false;
      std::vector<boost::function<void()>> completions;
      completions.swap(s_pendingCompletions);
      for (boost::function<void()>& completion : completions)
         completion();
   };
```

to:

```cpp
      drainPendingCompletions();
   };
```

- [ ] **Step 6: Build to verify it compiles (no behavior change)**

Run: `cd build && cmake --build . --target rsession`
Expected: builds cleanly. Then run the existing chat tests to confirm no regression:
`./rstudio-tests --scope rsession --filter "Chat*"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/cpp/session/modules/SessionChat.cpp
git commit -m "Extract drainPendingCompletions and include update throttle"
```

---

## Task 3: Rework `onUpdateCheckComplete` to persist the record and reapply blocks

**Files:**
- Modify: `src/cpp/session/modules/SessionChat.cpp`

- [ ] **Step 1: Add `recordToWrite` to the computed locals**

In `onUpdateCheckComplete`, change:

```cpp
   std::string newVersion;
   std::string downloadUrl;
   std::string expectedSha256;
   bool showVersionWarning = false;
```

to:

```cpp
   std::string newVersion;
   std::string downloadUrl;
   std::string expectedSha256;
   // Staged on the success path (authoritative record from buildSuccessOutcome).
   // Left unset on every other exit, so finish() falls back to preserve-and-bump.
   boost::optional<ManifestCheckRecord> recordToWrite;
   bool showVersionWarning = false;
```

- [ ] **Step 2: Persist the record inside `finish()`**

In the `finish` lambda, change:

```cpp
      if (showVersionWarning)
         showRStudioVersionWarning(recommendedVersion, downloadPageUrl);

      drainPendingCompletions();
   };
```

to:

```cpp
      if (showVersionWarning)
         showRStudioVersionWarning(recommendedVersion, downloadPageUrl);

      // Persist the attempt. The success path stages an authoritative record;
      // every other exit leaves it unset and we preserve-and-bump (only a success
      // may set or clear the persisted block). Every real attempt bumps the
      // timestamp, so a bad manifest cannot bypass the throttle.
      ManifestCheckRecord record = recordToWrite
         ? *recordToWrite
         : throttle::bumpRecord(
              throttle::readManifestCheckRecord(throttle::manifestCheckStatePath()),
              std::time(nullptr));
      Error writeError = throttle::writeManifestCheckRecord(
         throttle::manifestCheckStatePath(), record);
      if (writeError)
         WLOG("Failed to persist manifest-check record: {}", writeError.getMessage());

      drainPendingCompletions();
   };
```

- [ ] **Step 3: Reapply the persisted block on a recoverable fetch failure**

Change the fetch-error branch from:

```cpp
   if (fetchError)
   {
      WLOG("Failed to download manifest: {}", fetchError.getMessage());
      manifestUnavailable = true;
      errorMessage = fetchError.getMessage();
      finish();
      return;
   }
```

to:

```cpp
   if (fetchError)
   {
      WLOG("Failed to download manifest: {}", fetchError.getMessage());

      bool isInstalled = (installedVersion != "0.0.0");
      bool protocolMismatch = hasProtocolMismatch(installedVersion);
      if (isInstalled && !protocolMismatch)
      {
         // Compatible install present: use it, surface no error. Reapply any
         // persisted (manifest-only) unsupported block so a known-bad version
         // stays blocked across the failure.
         boost::optional<ManifestCheckRecord> prior =
            throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
         if (prior)
         {
            ResolvedBlock block = throttle::resolvePersistedBlock(
               *prior, installedVersion, kProtocolVersion);
            unsupportedInstalledVersion = block.unsupportedInstalledVersion;
            unsupportedProtocol = block.unsupportedProtocol;
         }
      }
      else
      {
         // Not installed, or protocol mismatch: cannot proceed -> block.
         manifestUnavailable = true;
         errorMessage = fetchError.getMessage();
      }

      // recordToWrite stays unset -> finish() preserve-and-bumps the timestamp.
      finish();
      return;
   }
```

- [ ] **Step 4: Stage the authoritative record on the success path**

Change the unsupported-flag computation from:

```cpp
   // Protocol mismatch (file I/O, no lock held).
   bool protocolMismatch = hasProtocolMismatch(installedVersion);
   unsupportedProtocol = isProtocolUnsupported(unsupportedInfo);
   unsupportedInstalledVersion =
      isVersionUnsupported(installedVersion, unsupportedInfo) || protocolMismatch;
   DLOG("Unsupported check: protocol={}, installedVersion={}",
        unsupportedProtocol, unsupportedInstalledVersion);
```

to:

```cpp
   // Protocol mismatch (file I/O, no lock held).
   bool protocolMismatch = hasProtocolMismatch(installedVersion);
   unsupportedProtocol = isProtocolUnsupported(unsupportedInfo);
   bool versionUnsupported = isVersionUnsupported(installedVersion, unsupportedInfo);

   // Split the live composite (for s_updateState) from the manifest-only record
   // (persisted). The local protocol mismatch contributes to the live flag but is
   // never persisted -- it is recomputed locally, and the reapply paths run only
   // when there is no current mismatch. The manifest fetch succeeded, so this is
   // the authoritative result for every remaining exit (including no-compatible-
   // version): stage it now.
   SuccessOutcome outcome = throttle::buildSuccessOutcome(
      std::time(nullptr), installedVersion, kProtocolVersion,
      versionUnsupported, protocolMismatch, unsupportedProtocol);
   unsupportedInstalledVersion = outcome.liveUnsupportedInstalledVersion;
   recordToWrite = outcome.record;
   DLOG("Unsupported check: protocol={}, installedVersion={}",
        unsupportedProtocol, unsupportedInstalledVersion);
```

- [ ] **Step 5: Build to verify it compiles**

Run: `cd build && cmake --build . --target rsession`
Expected: builds cleanly. Then `./rstudio-tests --scope rsession --filter "Chat*"`
Expected: PASS (pure throttle tests still green; this task changes only `SessionChat.cpp` wiring, which has no dedicated unit tests — see the spec's testing note).

- [ ] **Step 6: Commit**

```bash
git add src/cpp/session/modules/SessionChat.cpp
git commit -m "Persist manifest-check record and reapply blocks on failure"
```

---

## Task 4: Add the throttle gate (`force` param, `shouldFetchManifest`, `resolveWithoutManifestFetch`)

**Files:**
- Modify: `src/cpp/session/modules/SessionChat.cpp`

- [ ] **Step 1: Update the forward declaration**

Change:

```cpp
void startUpdateCheck(bool isStartup, boost::function<void()> onComplete);
void onUpdateCheckComplete(const Error& fetchError, const json::Object& manifest);
```

to:

```cpp
void startUpdateCheck(bool isStartup, bool force, boost::function<void()> onComplete);
void onUpdateCheckComplete(const Error& fetchError, const json::Object& manifest);
```

- [ ] **Step 2: Add `shouldFetchManifest` and `resolveWithoutManifestFetch` before `startUpdateCheck`**

`startUpdateCheck` is defined just after `onUpdateCheckComplete`. Immediately before the `// Begin (or join) a manifest update check.` comment that precedes `startUpdateCheck`, insert:

```cpp
// Decide whether an automatic check must actually fetch the manifest. Always
// fetches when nothing is installed or the installed protocol mismatches; otherwise
// throttles to once per kManifestCheckThrottleSeconds. `force` (Retry / install)
// always fetches. Main-thread only (reads the filesystem).
bool shouldFetchManifest(bool force)
{
   std::string installed = getInstalledVersion();   // "" when not installed
   bool isInstalled = !installed.empty();
   bool mismatch = isInstalled && hasProtocolMismatch(installed);

   boost::optional<ManifestCheckRecord> record =
      throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
   boost::optional<std::time_t> last;
   if (record)
      last = record->lastCheckTime;

   return throttle::manifestCheckDue(
      force, isInstalled, mismatch, last,
      std::time(nullptr), throttle::kManifestCheckThrottleSeconds);
}

// Resolve the update state from the installed version without fetching the
// manifest (throttled skip). Reached only when a compatible version is installed
// (installed + no protocol mismatch), so the installed version is current and
// usable. Reapplies any persisted (manifest-only) unsupported block, then drains
// the single-flight queue. Does NOT write the record -- no attempt was made.
void resolveWithoutManifestFetch()
{
   std::string installedVersion = getInstalledVersion();

   bool unsupportedInstalledVersion = false;
   bool unsupportedProtocol = false;
   boost::optional<ManifestCheckRecord> record =
      throttle::readManifestCheckRecord(throttle::manifestCheckStatePath());
   if (record)
   {
      ResolvedBlock block = throttle::resolvePersistedBlock(
         *record, installedVersion, kProtocolVersion);
      unsupportedInstalledVersion = block.unsupportedInstalledVersion;
      unsupportedProtocol = block.unsupportedProtocol;
   }

   {
      boost::mutex::scoped_lock lock(s_updateStateMutex);
      s_updateState.currentVersion = installedVersion;
      s_updateState.manifestUnavailable = false;
      s_updateState.errorMessage = "";
      s_updateState.unsupportedProtocol = unsupportedProtocol;
      s_updateState.unsupportedInstalledVersion = unsupportedInstalledVersion;
      s_updateState.noCompatibleVersion = false;
      s_updateState.updateAvailable = false;
      s_updateState.isDowngrade = false;
      s_updateState.newVersion = "";
      s_updateState.downloadUrl = "";
      s_updateState.expectedSha256 = "";
   }

   if (isPositAssistantUnsupported())
      assistant::stopAgentForUpdate();

   drainPendingCompletions();
}
```

- [ ] **Step 3: Add the gate to `startUpdateCheck`**

Change:

```cpp
void startUpdateCheck(bool isStartup, boost::function<void()> onComplete)
{
   if (onComplete)
      s_pendingCompletions.push_back(onComplete);
   if (isStartup)
      s_checkIncludesStartup = true;

   if (s_checkInProgress)
      return;

   s_checkInProgress = true;
   fetchManifestAsync(&onUpdateCheckComplete);
}
```

to:

```cpp
void startUpdateCheck(bool isStartup, bool force, boost::function<void()> onComplete)
{
   if (onComplete)
      s_pendingCompletions.push_back(onComplete);
   if (isStartup)
      s_checkIncludesStartup = true;

   if (s_checkInProgress)
      return;

   s_checkInProgress = true;

   if (!shouldFetchManifest(force))
   {
      DLOG("Manifest check throttled; using installed version without fetching");
      resolveWithoutManifestFetch();
      return;
   }

   fetchManifestAsync(&onUpdateCheckComplete);
}
```

- [ ] **Step 4: Update the startup caller (`onDeferredInit`)**

Change:

```cpp
         []() { startUpdateCheck(true, boost::function<void()>()); },
```

to:

```cpp
         []() { startUpdateCheck(true, false, boost::function<void()>()); },
```

- [ ] **Step 5: Update the `chatCheckForUpdates` caller (pass `forceRecheck` through)**

Change:

```cpp
   startUpdateCheck(false, boost::bind(resolveWithUpdateState, cont));
```

to:

```cpp
   startUpdateCheck(false, forceRecheck, boost::bind(resolveWithUpdateState, cont));
```

- [ ] **Step 6: Update the `chatInstallUpdate` caller (force a fresh fetch)**

Change:

```cpp
      startUpdateCheck(false, boost::bind(performInstall, cont));
```

to:

```cpp
      startUpdateCheck(false, true, boost::bind(performInstall, cont));
```

- [ ] **Step 7: Build and run the chat tests**

Run: `cd build && cmake --build . --target rsession`
Expected: builds cleanly with no warnings.
Then: `./rstudio-tests --scope rsession --filter "Chat*"`
Expected: PASS.

- [ ] **Step 8: Manual smoke check (optional but recommended)**

With Posit Assistant installed and selected, start a session and confirm in the rsession log that the first startup fetches the manifest (`Fetching manifest from: ...`), then restart within 24h and confirm the throttled-skip log line (`Manifest check throttled; using installed version without fetching`) and that the chat pane still works. Confirm `<userDataDir>/pai/manifest-check.json` exists with a `lastCheckTime`. Clicking Retry in the pane should still fetch (force path).

- [ ] **Step 9: Commit**

```bash
git add src/cpp/session/modules/SessionChat.cpp
git commit -m "Throttle Posit Assistant manifest downloads to once per day"
```

---

## Documentation / NEWS

No `NEWS.md` entry is added. Per the repo's NEWS rules, entries are not added for changes to unreleased features still under active development; Posit Assistant and its manifest-driven install/update flow are in that category. If the maintainers consider this user-facing, add under `### Fixed` or `### New` at PR time.

## Out of scope (pre-PR, handled on request)

Per the working agreement, the temporary `.gitignore` change (un-ignoring `docs/superpowers`) is restored and the `docs/superpowers` design + plan files are deleted **only when preparing the PR** — not as part of this implementation.

---

## Self-Review

**Spec coverage:**
- "Download at most once/24h; persist last attempt" → `manifestCheckDue` + `lastCheckTime` (Task 1) gated in `startUpdateCheck` (Task 4). ✓
- "Always fetch when not installed or protocol mismatch" → `manifestCheckDue` force conditions + `shouldFetchManifest` (Tasks 1, 4). ✓
- "Persist last-known unsupported block + context; only a success clears it" → `ManifestCheckRecord` + `buildSuccessOutcome` (success stages) + `finish()` fallback preserves on non-success (Tasks 1, 3). ✓
- "Manifest-only persistence (exclude local protocol mismatch)" → `buildSuccessOutcome` persists `versionUnsupported` only (Tasks 1, 3 Step 4). ✓
- "Independent reapply of the two flags" → `resolvePersistedBlock` (Task 1). ✓
- "Fetch failure with compatible install uses it without error, reapplying a known block" → fetch-error branch (Task 3 Step 3). ✓
- "Every real attempt bumps the timestamp" → `finish()` staged-or-fallback write (Task 3 Step 2). ✓
- "Throttled skip uses installed version, no record write" → `resolveWithoutManifestFetch` (Task 4 Step 2). ✓
- "Never throttle Retry or install" → `forceRecheck` passthrough + `force=true` for install (Task 4 Steps 5-6). ✓
- Tests for all pure helpers incl. the manifest-only regression case → Task 1 Step 4. ✓

**Placeholder scan:** No TBD/TODO; every code step shows complete code; every command shows expected output. ✓

**Type consistency:** `ManifestCheckRecord`, `ResolvedBlock`, `SuccessOutcome`, `manifestCheckDue`, `resolvePersistedBlock`, `buildSuccessOutcome`, `bumpRecord`, `readManifestCheckRecord`, `writeManifestCheckRecord`, `manifestCheckStatePath`, `kManifestCheckThrottleSeconds`, `shouldFetchManifest`, `resolveWithoutManifestFetch`, `drainPendingCompletions` are used with identical signatures across the header (Task 1), the SessionChat helpers (Tasks 2-4), and the tests. `startUpdateCheck(bool, bool, boost::function<void()>)` is updated consistently at the forward declaration, definition, and all three call sites. ✓
