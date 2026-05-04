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

- `url` (required, string): must start with `http://` or `https://` (case
  insensitive). Other schemes (`file://`, `data:`, `javascript:`, `ftp://`,
  etc.) are rejected with `kJsonRpcInvalidParams`.
- `height` (optional, integer): matches the underlying
  `module_context::viewer(url, int)` contract — `0` (or omitted) for no
  height change, `-1` for maximize, positive integers for pixel height.
  Anything else (non-integer, `< -1`) returns `kJsonRpcInvalidParams`.

The handler delegates to the existing `module_context::viewer(url, height)`,
which already handles server-mode port mapping (via
`url_ports::mapUrlPorts`) and fires the `kViewerNavigate` client event. No
new client-side or GWT changes are needed.

### URL validation

A pure helper, `isValidPreviewUrlScheme(const std::string& url)`, returns
true only when the URL begins with `http://` or `https://` (case
insensitive). It does not perform full URL parsing — a malformed URL like
`http://` would pass validation and reach `module_context::viewer()`, which
will simply fail to load. Full URL parsing is not justified for the marginal
benefit; the viewer pane already runs arbitrary http content under R
control, so this is not a new attack surface.

### Height validation

Only integers in `{-1, 0, 1, 2, ...}` are accepted. The R-side
`rstudioapi::viewer()` API additionally accepts the string `"maximize"` as a
synonym for `-1`, but the JSON-RPC method intentionally stays close to the
C++ `int` contract. Callers that prefer the friendlier form can convert
`"maximize"` to `-1` on their side.

### Capability advertisement

Add `"ui/previewUrl"` to `rstudioCapabilities()`. No sub-capability for
`height` is advertised — `height` is supported from day one of this method,
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
   helper `bool isValidPreviewUrlScheme(const std::string& url)`.
2. **`src/cpp/session/modules/chat/ChatConstants.cpp`**: implement the
   helper using `boost::algorithm::istarts_with`; add `"ui/previewUrl"` to
   `rstudioCapabilities()`.
3. **`src/cpp/session/modules/chat/ChatConstantsTests.cpp`**: add tests for
   `isValidPreviewUrlScheme` covering empty, `http://localhost:4321`,
   `https://example.com`, `HTTP://example.com` (case insensitivity),
   `file:///tmp/x.html`, `javascript:alert(1)`, `ftp://example.com`,
   `//example.com`, `example.com`.
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
      if (!heightValue.isInt() || heightValue.getInt() < -1)
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

- C++ unit tests for `isValidPreviewUrlScheme()` (covers all branches in
  the validator).
- Manual desktop-mode smoke test: in a debug build, send a JSON-RPC
  request with `{"url": "http://localhost:4321"}` to rsession and confirm
  the Viewer pane navigates.
- Manual server-mode smoke test: confirm port mapping kicks in and the
  Viewer pane loads the proxied URL.
