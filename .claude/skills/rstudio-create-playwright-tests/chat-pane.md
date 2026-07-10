# Chat pane / Posit Assistant patterns

Read this when working on tests in `tests/panes/posit-assistant-chat/` or
anything that intercepts rsession RPCs via `page.route()` in the Electron
CDP context.

## RPC interception (page.route in Electron CDP)

- **Verify the contract before mocking.** Field names in a casual prompt may
  not match the real RPC. Check `actions/<feature>.actions.ts` (the wrapper
  that uses the response) and `src/cpp/session/modules/SessionXxx.cpp` (the
  handler) to confirm the actual field names. A sibling test that already
  mocks the same RPC is also a good reference.
- **Regex, not glob.** `page.route(/\/rpc\/method_name/, ...)` handles query
  strings; `**/rpc/method_name` may not.
- **Never `route.fetch()` in Electron CDP** -- it returns empty bodies from
  rsession. Use `route.fulfill()` directly with the JSON-RPC envelope:

  ```typescript
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ result: mockPayload }),
  });
  ```

- RPC URL pattern: `http://127.0.0.1:<port>/rpc/<method_name>`.

## Chat-pane specifics

- **The `chatCheckForUpdates` cache.** The frontend fires it once at startup;
  toggling the chat provider preference does NOT re-trigger it. To force a
  fresh check, post `retry-manifest` *from inside the chat iframe*:

  ```typescript
  await chatPane.frame.locator('body').evaluate(() => {
    window.parent.postMessage('retry-manifest', '*');
  });
  ```

  ChatPresenter filters by `event.source`, so `window.postMessage(...)` from
  the main page is silently ignored.

- **A running PAI backend overwrites blocking pages.** `loadUrl()` reloads
  `ai-chat/index.html` on top of GWT's blocking-HTML write. For blocking-state
  tests, inject the HTML directly via `page.evaluate()` after verifying the
  RPC interception fired.

- **The Options dialog has its own install/update flow** that bypasses RPC
  interception. When you need to control the RPC response, write the
  preference directly via the `window.rstudio` bridge:

  ```typescript
  import { setPref } from '@utils/commands';
  await setPref(page, 'chat_provider', 'posit');
  ```
