# `ui/previewUrl` JSON-RPC Method Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `ui/previewUrl` JSON-RPC method on rsession so Posit Assistant can open `http(s)` URLs in the RStudio Viewer pane (mirrors `positron.window.previewUrl`).

**Architecture:** Two pure scheme/height validators in `chat/ChatConstants.{hpp,cpp}` (unit tested via gtest); a new `handlePreviewUrl()` function in `SessionChat.cpp` that validates params, then delegates to the existing `module_context::viewer(url, height)`; capability advertisement in `rstudioCapabilities()`. No GWT changes needed -- `module_context::viewer()` already fires `kViewerNavigate` and handles server-mode port mapping.

**Tech Stack:** C++ (rsession), boost.algorithm, Google Test (gtest), JSON-RPC.

**Spec:** `docs/superpowers/specs/2026-05-04-ui-previewurl-rpc-design.md`.

**Branch:** `feature/ui-previewurl-rpc` (already created).

---

## File Structure

| File | Change | Responsibility |
|---|---|---|
| `src/cpp/session/modules/chat/ChatConstants.hpp` | Modify | Declare `isValidPreviewUrlScheme()` and `isValidPreviewUrlHeight()` |
| `src/cpp/session/modules/chat/ChatConstants.cpp` | Modify | Implement both helpers; add `"ui/previewUrl"` to capabilities |
| `src/cpp/session/modules/chat/ChatConstantsTests.cpp` | Modify | Add gtest cases for both helpers |
| `src/cpp/session/modules/SessionChat.cpp` | Modify | Add `handlePreviewUrl()` and wire into `handleRequest()` |
| `NEWS.md` | Modify | User-facing entry under `### New` |
| `docs/superpowers/specs/2026-05-04-ui-previewurl-rpc-design.md` | Untrack | Remove from git index before PR (file is in gitignored tree) |
| `docs/superpowers/plans/2026-05-04-ui-previewurl-rpc.md` | Untrack | Same as above |

---

## Task 1: Add `isValidPreviewUrlScheme()` helper (TDD)

**Files:**
- Modify: `src/cpp/session/modules/chat/ChatConstants.hpp`
- Modify: `src/cpp/session/modules/chat/ChatConstants.cpp`
- Test: `src/cpp/session/modules/chat/ChatConstantsTests.cpp`

- [ ] **Step 1: Write the failing tests**

Append to `src/cpp/session/modules/chat/ChatConstantsTests.cpp` (after the last `TEST(AssembleWebSocketPath, ...)` block, before any closing braces):

```cpp
// -- isValidPreviewUrlScheme -------------------------------------------------

TEST(IsValidPreviewUrlScheme, EmptyStringRejected)
{
   EXPECT_FALSE(isValidPreviewUrlScheme(""));
}

TEST(IsValidPreviewUrlScheme, HttpLocalhostAccepted)
{
   EXPECT_TRUE(isValidPreviewUrlScheme("http://localhost:4321"));
}

TEST(IsValidPreviewUrlScheme, HttpsExampleAccepted)
{
   EXPECT_TRUE(isValidPreviewUrlScheme("https://example.com"));
}

TEST(IsValidPreviewUrlScheme, UppercaseHttpRejected)
{
   // module_context::viewer() uses case-sensitive starts_with("http"), so
   // accepting uppercase here would silently route the URL to the file-path
   // branch.
   EXPECT_FALSE(isValidPreviewUrlScheme("HTTP://example.com"));
}

TEST(IsValidPreviewUrlScheme, MixedCaseHttpsRejected)
{
   EXPECT_FALSE(isValidPreviewUrlScheme("Https://example.com"));
}

TEST(IsValidPreviewUrlScheme, FileSchemeRejected)
{
   EXPECT_FALSE(isValidPreviewUrlScheme("file:///tmp/x.html"));
}

TEST(IsValidPreviewUrlScheme, JavascriptSchemeRejected)
{
   EXPECT_FALSE(isValidPreviewUrlScheme("javascript:alert(1)"));
}

TEST(IsValidPreviewUrlScheme, FtpSchemeRejected)
{
   EXPECT_FALSE(isValidPreviewUrlScheme("ftp://example.com"));
}

TEST(IsValidPreviewUrlScheme, ProtocolRelativeRejected)
{
   EXPECT_FALSE(isValidPreviewUrlScheme("//example.com"));
}

TEST(IsValidPreviewUrlScheme, NoSchemeRejected)
{
   EXPECT_FALSE(isValidPreviewUrlScheme("example.com"));
}
```

- [ ] **Step 2: Run tests to verify they fail to compile**

The C++ unit tests for rsession are linked into the `rsession` binary itself (via `file(GLOB_RECURSE SESSION_TEST_FILES "*Tests.cpp")` in `src/cpp/session/CMakeLists.txt`), so the failing-test signal at this stage is a build failure.

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | tail -30
```

Expected: Compile error -- `isValidPreviewUrlScheme` is not declared in `rstudio::session::modules::chat::constants`.

- [ ] **Step 3: Declare the helper in `ChatConstants.hpp`**

Edit `src/cpp/session/modules/chat/ChatConstants.hpp`. Inside the `constants` namespace (e.g. directly after the `assembleWebSocketPath` declaration block), append:

```cpp
// ============================================================================
// ui/previewUrl validators
// ============================================================================

// Returns true when `url` begins with lowercase "http://" or "https://".
// Validation is case-sensitive on purpose: module_context::viewer() uses
// case-sensitive starts_with(url, "http") to distinguish URLs from file
// paths, so an uppercase scheme would silently route the URL to the
// file-path branch.
bool isValidPreviewUrlScheme(const std::string& url);
```

- [ ] **Step 4: Implement the helper in `ChatConstants.cpp`**

Edit `src/cpp/session/modules/chat/ChatConstants.cpp`. Add the include at the top, after `#include "ChatConstants.hpp"`:

```cpp
#include <boost/algorithm/string/predicate.hpp>
```

Then add the implementation inside the `constants` namespace, after `assembleWebSocketPath`:

```cpp
bool isValidPreviewUrlScheme(const std::string& url)
{
   // Case-sensitive on purpose -- see header comment.
   return boost::algorithm::starts_with(url, "http://") ||
          boost::algorithm::starts_with(url, "https://");
}
```

- [ ] **Step 5: Run the tests to verify they pass**

`tests::run()` in `src/cpp/tests/cpp/tests/TestRunner.hpp` initializes gtest with dummy argv (it ignores the harness's command-line args), so `rstudio-tests --scope rsession` runs every rsession test. Filter the output instead.

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | tail -10 && \
  ./src/cpp/rstudio-tests --scope rsession 2>&1 | grep -E 'IsValidPreviewUrlScheme|FAILED'
```

Expected: 10 lines for `IsValidPreviewUrlScheme.*` (all passing) and no `FAILED` lines from any test.

- [ ] **Step 6: Commit**

```bash
cd /Users/gary/chat/rstudio && git add src/cpp/session/modules/chat/ChatConstants.hpp \
  src/cpp/session/modules/chat/ChatConstants.cpp \
  src/cpp/session/modules/chat/ChatConstantsTests.cpp && \
git commit -m "Add isValidPreviewUrlScheme helper for ui/previewUrl

Pure validator that accepts only lowercase http:// and https:// URLs.
Rejects uppercase variants, file://, javascript:, ftp://, etc. Case-
sensitive matching matches module_context::viewer()'s downstream
starts_with() check, so we don't route uppercased URLs into the
file-path branch.

Addresses #17479."
```

---

## Task 2: Add `isValidPreviewUrlHeight()` helper (TDD)

**Files:**
- Modify: `src/cpp/session/modules/chat/ChatConstants.hpp`
- Modify: `src/cpp/session/modules/chat/ChatConstants.cpp`
- Test: `src/cpp/session/modules/chat/ChatConstantsTests.cpp`

- [ ] **Step 1: Write the failing tests**

Append to `src/cpp/session/modules/chat/ChatConstantsTests.cpp` after the `IsValidPreviewUrlScheme` tests:

```cpp
// -- isValidPreviewUrlHeight -------------------------------------------------

TEST(IsValidPreviewUrlHeight, MinusTwoRejected)
{
   EXPECT_FALSE(isValidPreviewUrlHeight(-2));
}

TEST(IsValidPreviewUrlHeight, MinusOneAccepted)
{
   // -1 is the maximize sentinel.
   EXPECT_TRUE(isValidPreviewUrlHeight(-1));
}

TEST(IsValidPreviewUrlHeight, ZeroAccepted)
{
   // 0 means "no height change".
   EXPECT_TRUE(isValidPreviewUrlHeight(0));
}

TEST(IsValidPreviewUrlHeight, OneAccepted)
{
   EXPECT_TRUE(isValidPreviewUrlHeight(1));
}

TEST(IsValidPreviewUrlHeight, TypicalPixelHeightAccepted)
{
   EXPECT_TRUE(isValidPreviewUrlHeight(1024));
}
```

- [ ] **Step 2: Run tests to verify they fail to compile**

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | tail -10
```

Expected: Compile error -- `isValidPreviewUrlHeight` is not declared.

- [ ] **Step 3: Declare the helper in `ChatConstants.hpp`**

Edit `src/cpp/session/modules/chat/ChatConstants.hpp`. Add directly after the `isValidPreviewUrlScheme` declaration:

```cpp
// Returns true when `height` is a valid value for ui/previewUrl:
// -1 (maximize), 0 (no change), or any positive integer (pixel height).
// Anything < -1 is rejected.
bool isValidPreviewUrlHeight(int height);
```

- [ ] **Step 4: Implement the helper in `ChatConstants.cpp`**

Add the implementation directly after `isValidPreviewUrlScheme`:

```cpp
bool isValidPreviewUrlHeight(int height)
{
   return height >= -1;
}
```

- [ ] **Step 5: Run the tests to verify they pass**

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | tail -10 && \
  ./src/cpp/rstudio-tests --scope rsession 2>&1 | grep -E 'IsValidPreviewUrlHeight|FAILED'
```

Expected: 5 lines for `IsValidPreviewUrlHeight.*` (all passing) and no `FAILED` lines.

- [ ] **Step 6: Commit**

```bash
cd /Users/gary/chat/rstudio && git add src/cpp/session/modules/chat/ChatConstants.hpp \
  src/cpp/session/modules/chat/ChatConstants.cpp \
  src/cpp/session/modules/chat/ChatConstantsTests.cpp && \
git commit -m "Add isValidPreviewUrlHeight helper for ui/previewUrl

Pure validator for the optional height parameter. Accepts -1 (maximize),
0 (no height change), and any positive integer (pixel height). Rejects
anything < -1.

Addresses #17479."
```

---

## Task 3: Advertise `ui/previewUrl` capability

**Files:**
- Modify: `src/cpp/session/modules/chat/ChatConstants.cpp`

- [ ] **Step 1: Add the capability**

Edit `src/cpp/session/modules/chat/ChatConstants.cpp`. Locate `rstudioCapabilities()` and append `"ui/previewUrl"` to the static initializer list, after `"ui/revealInFilesPane"`:

```cpp
static const std::vector<std::string> s_capabilities = {
   "runtime/getActiveSession",
   "runtime/getDetailedContext",
   "runtime/executeCode",
   "runtime/getConsoleContent",
   "workspace/readFileContent",
   "workspace/writeFileContent",
   "workspace/editFileContent",
   "workspace/insertIntoNewFile",
   "workspace/insertAtCursor",
   "ui/openDocument",
   "ui/openDocument/line",
   "ui/revealInFilesPane",
   "ui/previewUrl",
};
```

- [ ] **Step 2: Verify the file still compiles**

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | tail -10
```

Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
cd /Users/gary/chat/rstudio && git add src/cpp/session/modules/chat/ChatConstants.cpp && \
git commit -m "Advertise ui/previewUrl in rsession capabilities

Lets Posit Assistant detect that this rsession build supports the new
viewer-pane URL preview method.

Addresses #17479."
```

---

## Task 4: Add `handlePreviewUrl()` to SessionChat.cpp

**Files:**
- Modify: `src/cpp/session/modules/SessionChat.cpp`

- [ ] **Step 1: Add the handler function**

Edit `src/cpp/session/modules/SessionChat.cpp`. Locate `handleRevealInFilesPane()` (around line 2971). Insert the new handler immediately after it (i.e. before `handleGetProtocolVersion()`):

```cpp
void handlePreviewUrl(core::system::ProcessOperations& ops,
                      const json::Value& requestId,
                      const json::Object& params)
{
   DLOG("Handling ui/previewUrl request");

   // Extract url parameter
   std::string url;
   Error error = json::readObject(params, "url", url);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                       "Invalid params: url required");
      return;
   }

   if (!chat::constants::isValidPreviewUrlScheme(url))
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                       "Invalid url: only http and https schemes are supported");
      return;
   }

   // Optional height parameter: 0 = no change, -1 = maximize, positive = pixels.
   int height = 0;
   auto heightIt = params.find("height");
   if (heightIt != params.end())
   {
      const json::Value& heightValue = (*heightIt).getValue();
      if (!heightValue.isInt())
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                          "Invalid params: height must be an integer");
         return;
      }
      if (!chat::constants::isValidPreviewUrlHeight(heightValue.getInt()))
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                          "Invalid params: height must be -1, 0, or a positive integer");
         return;
      }
      height = heightValue.getInt();
   }

   // Navigate the Viewer pane. module_context::viewer() handles server-mode
   // port mapping and fires kViewerNavigate.
   module_context::viewer(url, height);

   // Return success
   json::Object result;
   result["success"] = true;

   // Log the URL with query/fragment redacted -- URLs from assistant tool
   // calls can include access tokens or other secrets in the query string.
   auto queryPos = url.find_first_of("?#");
   std::string urlForLog = (queryPos == std::string::npos)
                              ? url
                              : url.substr(0, queryPos) + "...";
   DLOG("Previewing url in viewer pane: {} (height={})", urlForLog, height);
   sendJsonRpcResponse(ops, requestId, result);
}
```

- [ ] **Step 2: Wire the handler into `handleRequest()`**

In the same file, locate the `handleRequest()` switch (around line 3118). Add a new branch directly after the `"ui/revealInFilesPane"` branch:

```cpp
   else if (method == "ui/revealInFilesPane")
   {
      handleRevealInFilesPane(ops, requestId, params);
   }
   else if (method == "ui/previewUrl")
   {
      handlePreviewUrl(ops, requestId, params);
   }
   else
   {
      // Unknown method - send JSON-RPC error response
```

- [ ] **Step 3: Build to verify**

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | tail -20
```

Expected: Build succeeds with no warnings introduced by the new code.

- [ ] **Step 4: Commit**

```bash
cd /Users/gary/chat/rstudio && git add src/cpp/session/modules/SessionChat.cpp && \
git commit -m "Add ui/previewUrl JSON-RPC handler

handlePreviewUrl() validates the url (lowercase http/https only) and
optional height (>= -1) using the pure helpers in ChatConstants, then
delegates to module_context::viewer(), which already handles server-mode
port mapping and fires the kViewerNavigate client event.

Addresses #17479."
```

---

## Task 5: NEWS.md entry

**Files:**
- Modify: `NEWS.md`

- [ ] **Step 1: Add the entry**

Edit `NEWS.md`. Under the `### New` section (top of the current release notes), insert a new entry. Place it adjacent to the other Posit Assistant entries (#17477 / #17478):

```markdown
- ([#17479](https://github.com/rstudio/rstudio/issues/17479)): Posit Assistant: added the `ui/previewUrl` JSON-RPC method, which navigates the Viewer pane to an `http(s)` URL (e.g., a local Shiny app); supports an optional `height` parameter that mirrors `rstudioapi::viewer()` (`-1` for maximize, `0` for no change, positive for pixels).
```

- [ ] **Step 2: Commit**

```bash
cd /Users/gary/chat/rstudio && git add NEWS.md && \
git commit -m "NEWS: ui/previewUrl JSON-RPC method (#17479)"
```

---

## Task 6: Manual smoke tests

This task is verification, not new code. Run through the cases below and confirm each behaves as expected. If any case fails, return to the relevant task above and fix.

**Files:** none

- [ ] **Step 1: Build and launch a debug rsession**

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | tail -10
```

Then launch RStudio Desktop (or rserver) against this build. The smoke tests below send JSON-RPC requests through Posit Assistant's WebSocket bridge; they're easiest to drive through the in-development assistant by manually issuing a request (e.g., from a debugger or an `npm run` test harness in `~/chat/assistant`).

- [ ] **Step 2: Happy path -- desktop, no height**

Send: `{ "jsonrpc": "2.0", "id": 1, "method": "ui/previewUrl", "params": { "url": "http://localhost:4321" } }`

Expected: response `{ "success": true }`; Viewer pane navigates to `http://localhost:4321` (will show a connection-refused page unless something is actually listening, which is fine).

- [ ] **Step 3: Happy path -- server, port mapping**

Repeat Step 2 against an rserver build. Confirm the `s_currentUrl` event fired on the GWT side has the URL rewritten by `url_ports::mapUrlPorts` (e.g., `/p/<id>/...`).

- [ ] **Step 4: Maximize**

Send: `{ "url": "http://localhost:4321", "height": -1 }`

Expected: success; Viewer pane maximizes.

- [ ] **Step 5: Pixel height**

Send: `{ "url": "http://localhost:4321", "height": 600 }`

Expected: success; Viewer pane resizes to 600px.

- [ ] **Step 6: Missing url**

Send: `{ "method": "ui/previewUrl", "params": {} }`

Expected: JSON-RPC error, code `-32602`, message `"Invalid params: url required"`.

- [ ] **Step 7: Wrong-type url (number)**

Send: `{ "url": 42 }`

Expected: JSON-RPC error, code `-32602` (from `json::readObject`).

- [ ] **Step 8: Rejected scheme**

Send: `{ "url": "file:///tmp/x.html" }`

Expected: JSON-RPC error, code `-32602`, message `"Invalid url: only http and https schemes are supported"`.

- [ ] **Step 9: Uppercase scheme (regression for review finding)**

Send: `{ "url": "HTTP://example.com" }`

Expected: same scheme error as Step 8 (NOT routed through the file-path branch of `module_context::viewer`).

- [ ] **Step 10: Non-integer height (string)**

Send: `{ "url": "http://localhost:4321", "height": "tall" }`

Expected: JSON-RPC error, code `-32602`, message `"Invalid params: height must be an integer"`.

- [ ] **Step 11: Float height**

Send: `{ "url": "http://localhost:4321", "height": 5.5 }`

Expected: same "must be an integer" error (`json::Value::isInt()` returns false for floats).

- [ ] **Step 12: Out-of-range height**

Send: `{ "url": "http://localhost:4321", "height": -5 }`

Expected: JSON-RPC error, code `-32602`, message `"Invalid params: height must be -1, 0, or a positive integer"`.

- [ ] **Step 13: Capability advertised**

Trigger a `protocol/getVersion` exchange and confirm the response's `capabilities` array contains `"ui/previewUrl"`.

---

## Task 7: Pre-PR cleanup -- untrack spec and plan docs

The spec and plan files live under the gitignored `docs/superpowers/` tree and were force-added on this branch so roborev could review them. Remove them from the git index (but keep the files locally) before opening the PR.

**Files:**
- Modify (untrack): `docs/superpowers/specs/2026-05-04-ui-previewurl-rpc-design.md`
- Modify (untrack): `docs/superpowers/plans/2026-05-04-ui-previewurl-rpc.md`

- [ ] **Step 1: Remove from git index, keep files on disk**

```bash
cd /Users/gary/chat/rstudio && \
git rm --cached \
  docs/superpowers/specs/2026-05-04-ui-previewurl-rpc-design.md \
  docs/superpowers/plans/2026-05-04-ui-previewurl-rpc.md
```

- [ ] **Step 2: Confirm files still exist locally and are now ignored**

```bash
ls docs/superpowers/specs/2026-05-04-ui-previewurl-rpc-design.md \
   docs/superpowers/plans/2026-05-04-ui-previewurl-rpc.md && \
git status --ignored | grep superpowers
```

Expected: both files exist on disk; `git status --ignored` lists them under `Ignored files`.

- [ ] **Step 3: Commit the removal**

```bash
cd /Users/gary/chat/rstudio && git commit -m "Drop spec and plan docs from feature branch tracking

These planning docs live under the gitignored docs/superpowers/ tree;
they were force-added on this branch for roborev review and are removed
before opening the PR."
```

---

## Task 8: Final verification before PR

- [ ] **Step 1: Run the full rsession test scope**

```bash
cd /Users/gary/chat/rstudio/build && \
  ./src/cpp/rstudio-tests --scope rsession 2>&1 | tee /tmp/previewurl-tests.log | tail -30 && \
  grep -E 'IsValidPreviewUrl|AssembleWebSocketPath' /tmp/previewurl-tests.log | head -40 && \
  grep -E 'FAILED' /tmp/previewurl-tests.log || echo 'NO FAILED LINES'
```

Expected: 27 test result lines for the chat module (10 `IsValidPreviewUrlScheme` + 5 `IsValidPreviewUrlHeight` + 12 `AssembleWebSocketPath`), and the final command prints `NO FAILED LINES`.

- [ ] **Step 2: Confirm no warnings introduced**

```bash
cd /Users/gary/chat/rstudio/build && cmake --build . --target rsession 2>&1 | grep -i 'warning\|error' | head
```

Expected: no new warnings or errors attributable to the diff.

- [ ] **Step 3: Diff review**

```bash
cd /Users/gary/chat/rstudio && git diff main...HEAD --stat
```

Confirm the only files touched are:
- `src/cpp/session/modules/chat/ChatConstants.hpp`
- `src/cpp/session/modules/chat/ChatConstants.cpp`
- `src/cpp/session/modules/chat/ChatConstantsTests.cpp`
- `src/cpp/session/modules/SessionChat.cpp`
- `NEWS.md`

If any other files appear, investigate before opening the PR.
