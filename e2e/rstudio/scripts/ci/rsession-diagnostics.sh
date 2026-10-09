#!/usr/bin/env bash
#
# Dump the state of every running rsession, and optionally kill them.
#
# Used by the Linux Server e2e workflow in two places:
#
#  - as PW_RSTUDIO_SERVER_RECOVER_CMD (with --kill): when a sign-in times out
#    waiting for the IDE console, the harness runs this before retrying, so a
#    wedged session is recorded and replaced instead of failing every later
#    spec in the shard with the same login timeout (run 37965911829).
#  - from the failure step (without --kill): whatever is still alive at the
#    end of a failed shard gets the same dump into the logs artifact.
#
# Backtraces come from gdb, which needs ptrace rights over the target: run
# as root (sudo) on a runner where the sessions belong to another account.
# The packaged rsession is stripped; its frames are named only when the
# build's rsession.debug sits next to the binary (the workflow installs it).
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
pids=$(pgrep -x rsession || true)
if [ -z "$pids" ]; then
  echo "[rsession-diagnostics] no rsession processes" | tee -a "$out/summary.txt"
  exit 0
fi

for pid in $pids; do
  file="$out/rsession-$pid-$stamp.txt"
  echo "[rsession-diagnostics] dumping pid $pid to $file"
  {
    echo "=== ps ==="
    ps -o pid,ppid,user,etime,stat,pcpu,rss,wchan:32,cmd -p "$pid"
    echo
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
