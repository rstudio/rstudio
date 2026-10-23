#!/usr/bin/env bash
#
# Dump the state of every running rsession, and optionally kill them.
#
# Used by the e2e harness in three places:
#
#  - from the per-test liveness probe (utils/session-liveness.ts): when a
#    session stops answering the ping RPC, the test attaches this script's
#    output as rsession-stacks.txt before failing fast.
#  - as PW_RSTUDIO_SERVER_RECOVER_CMD (with --kill): when a sign-in times out
#    waiting for the IDE console, the harness runs this before retrying, so a
#    wedged session is recorded and replaced instead of failing every later
#    spec in the shard with the same login timeout (run 37965911829).
#  - from the failure step (without --kill): whatever is still alive at the
#    end of a failed shard gets the same dump into the logs artifact. A
#    session wedged by code running on the idle event loop (#19236) looks
#    idle from the outside; the backtrace is what names the culprit.
#
# Linux backtraces come from gdb, which needs ptrace rights over the target:
# run as root (sudo) on a runner where the sessions belong to another account
# or where ptrace_scope restricts attaching. The packaged rsession is
# stripped; its frames are named only when the build's rsession.debug sits
# next to the binary (the Linux Server workflow installs it). macOS uses
# `sample`, which works unprivileged on the caller's own processes and names
# frames from the binary's own symbols.
#
# Usage: rsession-diagnostics.sh [--kill] <output-dir>

set -u

kill_sessions=0
if [ "${1:-}" = "--kill" ]; then
  kill_sessions=1
  shift
fi
out=${1:?usage: rsession-diagnostics.sh [--kill] <output-dir>}
mkdir -p "$out"

stamp=$(date -u +%Y%m%dT%H%M%SZ)
# Packaged RStudio on an arm64 Mac launches the session as rsession-arm64
# (session-launcher.ts), so a plain -x rsession finds nothing on those runners.
pids=$(pgrep -x 'rsession(-arm64)?' || true)
if [ -z "$pids" ]; then
  echo "[rsession-diagnostics] no rsession processes" | tee -a "$out/summary.txt"
  exit 0
fi

dump_linux() {
  local pid=$1
  echo "=== threads (tid state utime stime wchan) ==="
  for task in /proc/"$pid"/task/*; do
    tid=$(basename "$task")
    # A session or thread can exit after enumeration. Skip failed reads
    # before expanding variables that may be unset (or left from a prior task).
    if ! read -r _ _ state _ _ _ _ _ _ _ _ _ _ utime stime _ < "$task/stat"; then
      echo "(could not read $task/stat; skipping thread)"
      continue
    fi
    echo "$tid $state $utime $stime $(cat "$task/wchan" 2>/dev/null)"
  done
  echo
  echo "=== children ==="
  ps -o pid,ppid,stat,etime,cmd --ppid "$pid" || true
  echo
  echo "=== gdb: thread apply all bt ==="
  if command -v gdb >/dev/null; then
    # A debugger stuck attaching or unwinding must not block recovery or
    # the workflow's final artifact upload. Escalate if it ignores SIGTERM.
    timeout --kill-after=5s 30s \
      gdb -batch -p "$pid" -ex 'set pagination off' -ex 'thread apply all bt' 2>&1 \
      || echo "(gdb failed or timed out; continuing)"
  else
    echo "(gdb not installed)"
  fi
}

dump_macos() {
  local pid=$1
  echo "=== threads ==="
  ps -M -p "$pid" || true
  echo
  echo "=== children ==="
  pgrep -lP "$pid" || true
  echo
  echo "=== sample: call graph of every thread ==="
  if command -v sample >/dev/null; then
    # One second of sampling is plenty for a parked thread, and keeps a
    # failure step short. sample writes its report to stdout without -file.
    sample "$pid" 1 2>&1 || echo "(sample failed; continuing)"
  else
    echo "(sample not available)"
  fi
}

for pid in $pids; do
  file="$out/rsession-$pid-$stamp.txt"
  echo "[rsession-diagnostics] dumping pid $pid to $file"
  {
    echo "=== ps ==="
    if [ "$(uname -s)" = "Darwin" ]; then
      ps -o pid,ppid,user,etime,stat,pcpu,rss,command -p "$pid"
    else
      ps -o pid,ppid,user,etime,stat,pcpu,rss,wchan:32,cmd -p "$pid"
    fi
    echo
    if [ "$(uname -s)" = "Darwin" ]; then
      dump_macos "$pid"
    else
      dump_linux "$pid"
    fi
  } > "$file" 2>&1
  echo "$stamp pid $pid dumped to $(basename "$file")" >> "$out/summary.txt"
done

if [ "$kill_sessions" = 1 ]; then
  for pid in $pids; do
    echo "[rsession-diagnostics] killing pid $pid"
    kill -KILL "$pid" 2>/dev/null || true
    echo "$stamp pid $pid killed" >> "$out/summary.txt"
  done
fi
