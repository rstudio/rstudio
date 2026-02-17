# Testing Proxy Support for Posit Assistant

Verifies that the `NODE_USE_ENV_PROXY=1` change in `SessionChat.cpp` causes
the chat backend's `fetch()` calls to route through `HTTP_PROXY`/`HTTPS_PROXY`.


## 1. Quick smoke test (no proxy software needed)

Set the proxy to a port where nothing is listening. If `fetch()` respects the
proxy, it fails with a connection-refused error; if it ignores the proxy, it
connects directly.

Add to `~/.Renviron`:

```
https_proxy=http://127.0.0.1:9999
```

Restart RStudio, open Posit Assistant, and attempt to sign in. The chat backend
stderr logs should show a connection error referencing `127.0.0.1:9999`,
confirming `fetch()` is routing through the proxy.

To confirm it's specifically `NODE_USE_ENV_PROXY` causing this, temporarily
comment out the `setenv` line in `SessionChat.cpp`, rebuild, and repeat -- the
sign-in should bypass the broken proxy and attempt a direct connection instead.

Remove the `~/.Renviron` line when done.


## 2. Full positive test with mitmproxy

Verifies end-to-end: sign-in and chat complete through a real proxy.

### Setup

```bash
brew install mitmproxy
```

Run it once to generate its CA certificate, then start it:

```bash
mitmdump -p 8888
```

The CA cert is created at `~/.mitmproxy/mitmproxy-ca-cert.pem` on first run.
Node.js needs to trust it for HTTPS interception. Add both lines to
`~/.Renviron`:

```
https_proxy=http://127.0.0.1:8888
NODE_EXTRA_CA_CERTS=~/.mitmproxy/mitmproxy-ca-cert.pem
```

`NODE_EXTRA_CA_CERTS` is already picked up by the chat backend --
`SessionAssistant.cpp` passes it through to the Node.js environment.

### Verify

Restart RStudio, open the chat pane, and sign in. In the `mitmdump` terminal
you should see CONNECT requests to `login.posit.cloud` and then
`gateway.posit.ai` once you send a message.


## 3. Regression check (no proxy)

Remove both lines from `~/.Renviron`, restart RStudio, and verify Posit
Assistant works normally with no proxy configured. `NODE_USE_ENV_PROXY=1`
should have no effect when no proxy env vars are set.
