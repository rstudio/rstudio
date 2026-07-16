# Phase 1: Cross-process coordination for Posit Assistant install/update/uninstall

## Context

Posit Assistant is installed per-user at `<xdg::userDataDir()>/pai/bin`, shared by every rsession that user runs (desktop multi-window on Mac/Windows/Linux; multiple server/Workbench sessions). Update (`performInstall` → `installPackage`, a remove/rename/unzip swap of `pai/bin`) and uninstall (recursive delete) are guarded only by a session-local mutex and stop only the invoking session's own node subprocesses (chat backend, NES agent). A concurrent session running PA gets a corrupted or vanishing install: sharing-violation failures on Windows, silent deleted-CWD/mixed-version breakage on macOS/Linux, ESTALE crashes on NFS-backed Workbench, and torn installs when two sessions install at once.

Phase 1 goal: serialize mutations across processes and refuse them while another session is using the install — converting silent corruption into clear, actionable errors. **Top requirement (user's explicit concern): a crashed/killed session must not leave stale state that produces false "in use by another session" warnings.**

Crash-robustness strategy: reuse `core::FileLock` and make in-use markers *held locks*, never marker files whose cleanup we own:
- Desktop (all platforms): `AdvisoryFileLock` (forced on Windows, default in desktop mode via `SessionMain.cpp:2375`) — fcntl/LockFileEx locks the **kernel auto-releases when the process dies**. False positives impossible.
- Server (Linux): `LinkBasedFileLock` — PID-in-file + `/proc` orphan check (same host: near-instant stale detection) + 20s heartbeat (already wired: `SessionHttpConnectionListenerImpl.hpp:112`) with 30s staleness timeout (load-balanced NFS worst case). Stale locks are auto-reclaimed on next `acquire()`.
- `FileLock::isLocked(path)` probes "held **and** non-stale" without acquiring. Existing template for enumerate-and-filter-stale: `SessionSourceDatabaseSupervisor.cpp:311-373`.

Decisions made with user: **refuse-only** (no force/override escape hatch); Phase 2 (side-by-side versioned installs) is out of scope.

## Design

Two lock kinds under `<userDataDir>/pai/locks/` (survives uninstall — uninstall deletes only `pai/bin` and `ai.prev`, `SessionChat.cpp:6007/:6030`):

1. **Mutation lock** `pai/locks/install.lock` — held by `performInstall` and `chatUninstallPositAssistant` for the whole mutation, acquired **before** stopping the session's own processes (fixes the current bug where `performInstall` kills its own backend at `SessionChat.cpp:5644` before it could fail). `FileLock::acquire()` is a non-blocking try-lock; contention classified via `FileLock::isNoLockAvailable()`.
2. **In-use locks** `pai/locks/sessions/<ownerId>.lock` — one per session, held while that session's chat backend OR NES agent is running from the user-data install. Acquired when the first component starts, released when the last stops (component flags, not refcounts).

Protocols (TOCTOU-safe two-flag ordering):
- **Mutator**: acquire `install.lock` (fail → "another session is updating") → enumerate `pai/locks/sessions/*.lock`, probe each with `isLocked()` **excluding own file by name** (POSIX fcntl hazard: probing a lock you hold releases it — confirmed by core's own `FileLockTests.cpp:222-223`) → any live lock → release `install.lock`, fail with "in use" message → files probing unlocked are opportunistically deleted (log-only on failure; file existence alone never means "in use").
- **Starter** (`startChatBackend` / `startAgent`): acquire own in-use lock first, then probe `install.lock` with `isLocked()`; if held, release own lock and fail the start with an "update in progress" error. Simultaneous racers each see the other and back off — no corruption either way.
- In-process re-entrancy (event-loop pumping inside stop-wait loops can dispatch RPCs mid-mutation) is guarded by plain bools, **never** by the file lock (fcntl doesn't conflict within one process). Concretely: `tryBeginMutation` fails while `mutationActive_` (nested mutation), and `acquireInUse` fails while `mutationActive_` (a re-entrant start must not launch from the dir being swapped). Own held components do **not** block `tryBeginMutation` — update/uninstall legitimately begin while this session's backend/agent are running and stop them immediately after taking the lock.
- **Single release point per component, generation-tagged**: `onBackendExit` (supervisor reap callback) is the only place that calls `releaseInUse(ChatBackend)`; `agent::onExit`/`onError` likewise for the agent. `terminateProcess()` merely signals — releasing at the stop call sites would drop the lock while the process may still be alive and holding its CWD. But a reap callback can also arrive **late**, after a stop path manually cleared `s_chatBackendPid` and a new backend already started; an unguarded release would unlock the new process and stomp its state. So `acquireInUse` returns a monotonically increasing token, the start path captures it in the process callbacks alongside the PID, and `releaseInUse(component, token)` no-ops unless the token matches the current holder — stale-generation callbacks (which should also skip the existing state-clearing) become harmless. When the rsession process itself exits (suspend, shutdown, crash) without a reap, the OS auto-releases advisory locks and `FileLock::cleanUp()` (SessionMain.cpp:1504) removes link-based files.

Locking only applies when PA runs from the user-data install; env-var (`RSTUDIO_POSIT_AI_PATH`) and system (`/etc/rstudio/ai`) installs are not mutation targets and skip all locking.

## New files

### `src/cpp/session/modules/chat/ChatInstallLock.{hpp,cpp}` + `ChatInstallLockTests.cpp`

Pure, dependency-injected class in `namespace ...::chat::install_lock` (no `module_context`/session includes — same testable pattern as `ChatInstallation.cpp`). Add the .cpp to `src/cpp/session/CMakeLists.txt` (~line 197); tests auto-globbed (`CMakeLists.txt:437`).

```cpp
class InstallLock : boost::noncopyable
{
public:
   InstallLock(const core::FilePath& locksDir, const std::string& ownerId);
   enum class Component { ChatBackend, NesAgent };

   // First component takes the file lock. Fails while mutationActive_ is set:
   // a re-entrant start during our own mutation must not launch from the dir
   // being swapped (updateInProgressElsewhere() intentionally ignores our own
   // install.lock). *pToken receives a monotonically increasing generation
   // token identifying this holder.
   core::Error acquireInUse(Component c, uint64_t* pToken);
   // No-op unless token matches the component's current holder — a late exit
   // callback from a terminated previous process must not release the lock a
   // restarted process now holds. Last live component releases the file lock.
   void releaseInUse(Component c, uint64_t token);
   bool inUseHeld() const;

   core::Error tryBeginMutation(std::string* pUserMessage); // install.lock + probe sessions/*
   void endMutation();
   bool mutationInProgress() const;         // in-process flag only — never self-probes
   bool updateInProgressElsewhere() const;  // isLocked(install.lock); false if we hold it

   // path accessors for tests
private:
   // FileLock instances created lazily via FileLock::createDefault() (never at
   // static init — FileLock ctor warns pre-initialize(); rsession initializes at
   // SessionMain.cpp:2375). ensureDirectory() before every acquire (advisory
   // acquire does NOT create parent dirs). bools: backendHeld_, agentHeld_,
   // mutationActive_. Main-thread-only (callbacksRequireMainThread on both
   // subprocesses) — document, no mutexes.
};
class MutationScope;  // RAII over tryBeginMutation/endMutation
```

Wiring: `install_lock::InstallLock& installLock();` declared in `SessionChat.hpp` (SessionAssistant.cpp already includes it — cross-module precedent: `assistant::stopAgentForUpdate()`), defined in SessionChat.cpp as a lazy function-local static with:
- `locksDir = xdg::userDataDir().completePath(kPositAiLocksDirName)`
- `ownerId = module_context::activeSession().id()`, **fallback `<hostname>-<pid>` when empty** (dev/automation-launched sessions have an empty launcher token → empty id; collision would cause spurious start failures).

### `ChatConstants.{hpp,cpp}` additions

`kPositAiLocksDirName = "pai/locks"`, session-locks subdir name, `install.lock` filename.

## Integration points — `SessionChat.cpp`

| Location | Change |
|---|---|
| `startChatBackend` (:5048) | After `locatePositAssistantInstallation()` (:5055), only if the path is the user-data install: `acquireInUse(ChatBackend, &token)`, then `updateInProgressElsewhere()` → if true release and return error with `description` = update-in-progress text. Capture the token (alongside the PID) in the process callbacks so `onExit` can identify its generation. Scope-guard release on later error returns (findNode :5071, missing script :5076, port :5082, `runProgram` fail :5236). Covers both callers (RPC :5294, `onResume` :6148). |
| `onBackendExit` (:4997) | `releaseInUse(ChatBackend, token)` next to `s_chatBackendPid = -1` (:5030). **Sole release point for this component** — the supervisor reap callback fires in every stop path (graceful, force-terminate, crash), even when a stop call site already set `s_chatBackendPid = -1` manually. The token no-ops a **late** reap callback from a previous process generation, so it cannot unlock a restarted backend; guard the existing state-clearing (:5030-5036) with the same generation check so a stale callback doesn't stomp the new process's state either. No release in `chatStopBackend` — `terminateProcess()` only signals, and the process may still be alive holding its CWD. |
| `performInstall` (:5601) | After :5642, **before** stopping own backend: `tryBeginMutation(&msg)`; on failure set `installStatus = Error` + `installMessage = msg` under `s_updateStateMutex` and return via continuation (mirrors download-failure pattern :5668-5682). On success hold `MutationScope` across all five exits (:5681, :5701, :5720, :5775, :5801). Then stop own processes **and confirm they are reaped before touching disk**: replace the bare `terminateProcess` (:5648) with uninstall's request+poll+terminate+reap pattern (:5949-5988), call `assistant::stopAgentForUpdate()` (already synchronous), and poll (event-pumping, bounded ~2s) until `inUseHeld()` is false — component flags are released only by generation-matched exit callbacks, so they are the authoritative "my processes are really dead" signal (the mutator's probe excludes its own lock, so this is on us). On timeout: abort the mutation with `installMessage` = "A Posit Assistant process is still shutting down. Please try again, or restart RStudio." |
| `chatUninstallPositAssistant` (:5890) | Keep not-installed early branch (:5902-5940) untouched (no disk mutation). Before the backend-stop block (:5943): `tryBeginMutation(&msg)`; on failure `pResponse->setError(systemError(errc::device_or_resource_busy, ERROR_LOCATION), json::Value(msg))` — existing client_info pattern (:5907-5913). `MutationScope` through the delete. Same own-process reap confirmation as `performInstall`: after the existing stop/term-wait loops, poll until `inUseHeld()` is false; on timeout, **abort instead of proceeding** (today :5985-5987 warns and proceeds with a live process — exactly the corruption this change prevents), with client_info message "Failed to stop Posit Assistant processes. Please restart RStudio and try again." (consistent with the existing failure text at :6019-6023). Also make `ai.prev` removal failure an uninstall failure: today :6030-6032 logs the error and still reports success, leaving an executable tree behind with no guaranteed later cleanup — route it through the same error path as the `pai/bin` delete failure (:6019-6027). |
| `onSuspend` (:6052) / `onShutdown` (:6154) | No manual release. If `onBackendExit` fires during the stop-wait polling, release happens there; otherwise the rsession process exits — advisory locks auto-release, `FileLock::cleanUp()` (SessionMain.cpp:1504) removes link-based files. Resume re-acquires via `startChatBackend` (:6148). |

## Integration points — `SessionAssistant.cpp`

| Location | Change |
|---|---|
| `startAgent` (:1178) | Only when effective assistant is Posit **and** agent path resolves under the user-data install (not `RSTUDIO_AGENT_PATH` override :507): `acquireInUse(NesAgent)`, probe `updateInProgressElsewhere()` → on conflict release, set `s_agentStartupError`, status `Stopped`, return Error (callers already tolerate failure and retry on demand :1510-1518). Release on later failure paths (:1319-1324, :1334-1341). |
| `agent::onExit` (:1135) | `releaseInUse(NesAgent, token)` with the token captured at `startAgent` (no-op for Copilot, which never acquires; no-op for stale generations if the agent was restarted). This funnel covers `stopAgent`/`stopAgentSync`/`stopAgentForUpdate`. If `stopAgentSync` times out with the agent alive, component correctly stays held. |
| `agent::onError` (:1126) | **Do not release or clear the PID here.** `onError` means a stream IO error — the process may still be alive, and the terminate-the-child behavior documented in `Process.hpp:337-339` applies only when *no* callback is set. Record `s_agentStartupError`, then call `operations.terminate()` on the `ProcessOperations&` the callback receives (`Process.hpp:340`) — unlike `terminateProcess(pid)`, it terminates the whole detached process group (`detachSession=true`), so agent descendants don't outlive the lock; the supervisor keeps polling and the generation-matched `onExit` then clears state and releases. This also fixes the pre-existing bug where `onError` cleared `s_agentPid` while the agent could still be running. |

## Error messages → existing UI channels (no GWT changes; all channels verified to render arbitrary strings)

| Scenario | Message | Channel |
|---|---|---|
| Install/update: mutation lock contention | "Another RStudio session is currently installing or updating Posit Assistant. Please wait for it to finish, then try again." | `installMessage` polled via `chat_get_update_status` → `PositAiInstallManager.pollUpdateStatus` |
| Install/update: sessions in use | "Posit Assistant is currently in use by another RStudio session. Close Posit Assistant in your other sessions and try again." + when `FileLock::getDefaultType() == LOCKTYPE_LINKBASED` (FileLock.hpp:93): " If another session ended unexpectedly, this may take up to 30 seconds to clear." | same |
| Uninstall (both kinds) | Same texts prefixed "Unable to uninstall Posit Assistant: " | client_info error → `ChatPresenter.java:627-642` |
| Backend start during mutation | "A Posit Assistant update is in progress in another RStudio session. Please try again in a moment." | error `description` → `chatStartBackend` `result["error"]` (:5305) → `ChatPresenter.chatBackendStartFailed` |
| Agent start during mutation | same text | log + `s_agentStartupError` (existing silent-retry semantics) |
| Windows open-handle failure during swap | unchanged | existing `ERROR_SHARING_VIOLATION` backstop (:5745-5752) stays |

## Edge cases handled

- **rsession crash**: desktop/Windows — kernel releases advisory lock instantly; leftover file probes unlocked, deleted by next mutator. Server same-host — `/proc` orphan check, near-instant. Load-balanced — 30s timeout (message hint covers it). **No unbounded false "in use".**
- **Backend/agent crash**: exit-callback funnels release.
- **Agent-only usage** (NES without chat): component flags hold/release correctly.
- **PA not installed**: start fails before lock activity; `ensureDirectory()` on first acquire.
- **Windows**: deleting a probe-unlocked session file may hit a sharing violation from a lingering handle — log-only (probe result is authoritative).
- **Re-entrancy**: `mutationActive_`/component bools guard nested RPC dispatch during stop-wait event pumping (:5345, :6094).

## Known limitations (document in code comments, accept for Phase 1)

- Desktop + server rsessions on one Linux machine mix lock types (advisory vs link-based) and can't see each other reliably — pre-existing core limitation; uniform type can be forced via `file-locks` conf.
- Advisory unlink-on-release inode race in core — same exposure as all existing advisory usage; both racers still probe in-use locks afterward.
- macOS `exitWithParent` is a no-op (`PosixChildProcess.cpp:817`): an rsession crash orphans the node child, but the lock still releases (held by rsession) — the orphan is unreachable/harmless; POSIX mutation proceeds fine.
- Windows `ChildProcess::terminate()` kills only the target process, not descendants — a pre-existing platform gap shared by every RStudio stop path, not introduced here (POSIX paths use process-group termination via `detachSession`). If a surviving descendant still holds handles under `pai\bin`, the mutation's rename/delete typically fails with the existing sharing-violation backstop message (:5745-5752) instead of corrupting. If the rename succeeds anyway and only the `ai.prev` cleanup fails, that failure is logged and the install still reports success (`installPackage` step 6) — deliberate: the new install is verified and functional, and both mutation entry points delete stale `ai.prev` on their next run, so the leftover is self-healing. True process-tree termination (job objects) is the complete fix but is new core infrastructure affecting all subprocess handling — deferred beyond Phase 1. The exposure is theoretical today: neither component spawns long-lived descendants (the NES agent spawns none; chat-backend children such as git invocations are short-lived).

## Testing

- **Unit** (`ChatInstallLockTests.cpp`, gtest; `FileLock::initialize()` in tests per `core/FileLockTests.cpp:84`): two `InstallLock` instances, distinct ownerIds, temp dir. Use `LOCKTYPE_LINKBASED` for cross-instance exclusion (advisory can't conflict in-process); `fork()` for advisory cross-process cases (POSIX-only, per `forkAndCheckLock`). Cases: mutation-vs-mutation exclusion; live in-use blocks mutation; own session excluded from probe; `tryBeginMutation` **succeeds** while this instance's own components are held (mutator stops its own processes after locking); nested `tryBeginMutation` fails; stale leftover file doesn't block and is deleted; component flags (backend+agent, release one keeps held); `acquireInUse` fails while own mutation is active (re-entrancy); **stale-token `releaseInUse` is a no-op** (acquire, release with old token after re-acquire → still held); probes never self-release (assert own lock still held after `updateInProgressElsewhere()`); locks-dir auto-creation; stale link-based reclaim after simulated crash. The `agent::onError` lock-retention behavior has no unit harness (there is no existing test scaffolding for agent process lifecycle); its guarantee reduces to the helper's token semantics — "component stays held until generation-matched release" — which the token tests cover directly, plus manual scenario (f)/(g) below.
- **Build/checks**: `cd build && cmake --build . --target all`; run `./build/src/cpp/rstudio-tests --scope rsession --filter '*InstallLock*'` (and existing chat test suites).
- **Manual verification** (two sessions; desktop = two project windows, server = two browser sessions): (a) A chatting, B updates → refused with in-use message; (b) A chatting, B uninstalls → refused; (c) A stops chat (or closes), B update/uninstall succeeds; (d) `kill -9` A's rsession mid-chat → B update succeeds immediately (desktop / same-host server); (e) A mid-update (throttle with slow network or automation override RPC `chat_set_update_check_override`), B opens chat → "update in progress" start refusal, retry after A finishes succeeds; (f) reap-timeout abort: `SIGSTOP` the session's own backend node process, then update and uninstall → both abort with the "still shutting down" / "restart RStudio" message and `pai/bin` is untouched (`SIGCONT` + retry then succeeds); (g) stop chat then immediately restart it, and while chatting have B attempt an update → refused (late exit callback of the old generation must not have released the new backend's lock); (h) `ai.prev` removal failure: obstruct `ai.prev` during uninstall (hold a file handle open in it on Windows, or `chmod 000` its parent-relative entry on POSIX) → uninstall reports failure (not success), then remove the obstruction and retry → succeeds. A two-context server-mode Playwright test is a feasible follow-up, not part of this change.

## Process notes

- Branch: `bugfix/pai-concurrent-session-locking` (never commit to main).
- Issue: [#18271](https://github.com/rstudio/rstudio/issues/18271) (filed 2026-07-16; references the existing Windows uninstall issue #17365). NEWS.md entry links #18271. PR body: "Addresses #18271." (manual multi-session verification required; other-session change notification is out of scope for Phase 1).
- Commit per logical change; monitor roborev after each commit and close the review.
