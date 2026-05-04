# `ui/previewUrl` JSON-RPC Method

**Issue:** [rstudio/rstudio#17479](https://github.com/rstudio/rstudio/issues/17479)
**Status:** design approved, pending implementation
**Date:** 2026-05-04

## Background

Posit Assistant currently produces clickable links in chat output that open in
an external browser. For local URLs (e.g. a Shiny app at
`http://localhost:4321`), it would be more useful to open the link in
RStudio's Viewer pane, the same way `rstudioapi::viewer(url)` does.

Posit Assistant already has a JSON-RPC bridge to rsession (used today for
`ui/openDocument`, `ui/revealInFilesPane`, etc.). To support viewer-pane
previews, we need a new JSON-RPC method on the rsession side that mirrors
Positron's `positron.window.previewUrl(url)` extension API.

## Scope

**In scope:** the rsession-side handler, capability advertisement, and tests
for input validation.

**Out of scope (follow-up tasks):**

- Adding `previewUrl` to the `UIService` interface in
  `@assistant/core`'s `platform.ts`.
- Wiring `JsonRpcUIService` (in `@assistant/rstudio`) to call the new
  `ui/previewUrl` method.
- Capability detection on the assistant side (would check for
  `"ui/previewUrl"` in the rsession capabilities array).
- Any tool / UX in the assistant that decides when to use the viewer pane
  vs. an external browser.

The Posit Assistant codebase has no `previewUrl`, `openInViewer`, or
viewer-related methods today (verified by grepping
`@assistant/core`'s `UIService` interface and the `@assistant/rstudio`
JSON-RPC layer). This work establishes the rsession capability so the
follow-up assistant-side work has something to call.

## Design

### Method shape

```
Request:  ui/previewUrl
Params:   { "url": string, "height"?: integer }
Response: { "success": true } | JSON-RPC error
```

- `url` (required, string): must start with lowercase `http://` or
  `https://`. Other schemes (`file://`, `data:`, `javascript:`, `ftp://`,
  etc.) and uppercased variants (`HTTP://`) are rejected with
  `kJsonRpcInvalidParams`. Lowercase-only matches `module_context::viewer`,
  which uses `boost::algorithm::starts_with(url, "http")` and would
  otherwise route a `HTTP://...` URL to the file-path branch.
- `height` (optional, integer): matches the underlying
  `module_context::viewer(url, int)` contract -- `0` (or omitted) for no
  height change, `-1` for maximize, positive integers for pixel height.
  Anything else (non-integer, `< -1`) returns `kJsonRpcInvalidParams`.

The handler delegates to the existing `module_context::viewer(url, height)`,
which already handles server-mode port mapping (via
`url_ports::mapUrlPorts`) and fires the `kViewerNavigate` client event. No
new client-side or GWT changes are needed.

### URL validation

A pure helper, `isValidPreviewUrlScheme(const std::string& url)`, returns
true only when the URL begins with lowercase `http://` or `https://`. It
does not perform full URL parsing -- a malformed URL like `http://` would
pass validation and reach `module_context::viewer()`, which will simply
fail to load. Full URL parsing is not justified for the marginal benefit;
the viewer pane already runs arbitrary http content under R control, so
this is not a new attack surface.

Validation is case-sensitive on purpose. `module_context::viewer()` uses
case-sensitive `boost::algorithm::starts_with(url, "http")` to distinguish
URLs from file paths, so a URL like `HTTP://example.com` would be sent
down the file-path branch and silently fail. Rejecting the request up
front gives the caller a clear error instead. RFC 3986 schemes are
case-insensitive in principle, but real-world URLs are virtually always
lowercase; if a future caller needs case-insensitive acceptance, the right
fix is to normalize the scheme to lowercase before forwarding rather than
broaden this validator.

### Height validation

A second pure helper, `isValidPreviewUrlHeight(int height)`, returns true
when `height >= -1`. The handler reads the optional `height` field, type-
checks it as an integer using `json::Value::isInt()`, then runs the value
through this helper. Only integers in `{-1, 0, 1, 2, ...}` are accepted;
`-1` means maximize, `0` means no change, positive values are pixel
heights. The R-side `rstudioapi::viewer()` API additionally accepts the
string `"maximize"` as a synonym for `-1`, but the JSON-RPC method
intentionally stays close to the C++ `int` contract. Callers that prefer
the friendlier form can convert `"maximize"` to `-1` on their side.

### Capability advertisement

Add `"ui/previewUrl"` to `rstudioCapabilities()`. No sub-capability for
`height` is advertised -- `height` is supported from day one of this method,
so there is no version of rsession that has `ui/previewUrl` without it. The
sub-capability pattern in `ui/openDocument/line` exists because `line` was
added later than `ui/openDocument` itself.

### Protocol version

`kProtocolVersion` stays at `10.0`. Adding a new capability is a
backward-compatible additive change, and the recent
`ui/revealInFilesPane` / `ui/openDocument/line` additions (#17528) followed
the same pattern without bumping.

## Files changed

1. **`src/cpp/session/modules/chat/ChatConstants.hpp`**: declare the pure
   helpers `bool isValidPreviewUrlScheme(const std::string& url)` and
   `bool isValidPreviewUrlHeight(int height)`.
2. **`src/cpp/session/modules/chat/ChatConstants.cpp`**: implement the
   helpers (`isValidPreviewUrlScheme` uses case-sensitive
   `boost::algorithm::starts_with`; `isValidPreviewUrlHeight` returns
   `height >= -1`); add `"ui/previewUrl"` to `rstudioCapabilities()`.
3. **`src/cpp/session/modules/chat/ChatConstantsTests.cpp`**: add tests
   for both helpers.
   - `isValidPreviewUrlScheme`: empty, `http://localhost:4321`,
     `https://example.com`, `HTTP://example.com` (rejected, case
     sensitive), `Https://example.com` (rejected), `file:///tmp/x.html`,
     `javascript:alert(1)`, `ftp://example.com`, `//example.com`,
     `example.com`.
   - `isValidPreviewUrlHeight`: `-2` (rejected), `-1` (maximize), `0`
     (default), `1`, `1024`.
4. **`src/cpp/session/modules/SessionChat.cpp`**: add `handlePreviewUrl()`
   adjacent to `handleOpenDocument()` and `handleRevealInFilesPane()`; wire
   into the `handleRequest()` switch on `"ui/previewUrl"`.
5. **`NEWS.md`**: add entry referencing #17479.

## Handler sketch

```cpp
void handlePreviewUrl(core::system::ProcessOperations& ops,
                      const json::Value& requestId,
                      const json::Object& params)
{
   DLOG("Handling ui/previewUrl request");

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

   // Optional height: 0 = no change, -1 = maximize, positive = pixels.
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

   module_context::viewer(url, height);

   json::Object result;
   result["success"] = true;

   DLOG("Previewing url in viewer pane: {} (height={})", url, height);
   sendJsonRpcResponse(ops, requestId, result);
}
```

## Risks and tradeoffs

- **`http://` (not just `https://`):** required because the headline use
  case is local Shiny / Plumber servers on `http://localhost:PORT`. The
  Viewer pane already runs arbitrary http content under R control, so this
  is not a new attack surface.
- **Scheme-only validation:** a syntactically malformed URL passes
  validation and is forwarded to the viewer, which simply fails to load.
  Full URL parsing adds complexity for marginal benefit.
- **Forward compatibility:** if a future change wants to broaden the
  accepted schemes (to match `module_context::viewer()`'s temp-file
  behavior, for example), the helper can be relaxed without renaming the
  method.

## Verification plan

### Automated (C++ unit tests)

- `isValidPreviewUrlScheme()`: all scheme cases listed in "Files changed"
  above.
- `isValidPreviewUrlHeight()`: `-2`, `-1`, `0`, `1`, `1024`.

### Manual smoke tests

The handler itself is not unit-tested. Adjacent handlers
(`handleOpenDocument`, `handleRevealInFilesPane`) follow the same pattern
of inline `json::readObject` / `json::Value::isInt()` validation without
dedicated handler-level tests, and that machinery is exercised
identically in #17528. The cases below cover the wiring around the pure
helpers:

- **Happy path, desktop mode**: send `{"url": "http://localhost:4321"}`
  via JSON-RPC and confirm the Viewer pane navigates.
- **Happy path, server mode**: confirm `url_ports::mapUrlPorts` rewrites
  the URL and the Viewer pane loads the proxied URL.
- **Maximize**: send `{"url": "http://localhost:4321", "height": -1}`
  and confirm the pane maximizes.
- **Pixel height**: send `{"url": "http://localhost:4321", "height": 600}`
  and confirm the pane resizes.
- **Missing url**: send `{}` and confirm `kJsonRpcInvalidParams` with
  message "url required".
- **Wrong-type url**: send `{"url": 42}` and confirm
  `kJsonRpcInvalidParams` from `json::readObject`.
- **Rejected scheme**: send `{"url": "file:///tmp/x.html"}` and confirm
  `kJsonRpcInvalidParams` with the "only http and https" message.
- **Uppercase scheme**: send `{"url": "HTTP://example.com"}` and
  confirm rejection with the same scheme error (regression coverage for
  the case-sensitivity issue caught in review).
- **Non-integer height**: send `{"url": "...", "height": "tall"}` and
  confirm `kJsonRpcInvalidParams` with the "must be an integer" message.
- **Float height**: send `{"url": "...", "height": 5.5}` and confirm the
  same error (`json::Value::isInt()` returns false for floats).
- **Out-of-range height**: send `{"url": "...", "height": -5}` and
  confirm `kJsonRpcInvalidParams` with the "-1, 0, or positive integer"
  message.
