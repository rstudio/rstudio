/*
 * Resources.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#ifndef CORE_SYSTEM_RESOURCES_HPP
#define CORE_SYSTEM_RESOURCES_HPP

#include <string>
#include <sys/types.h>

namespace rstudio {
namespace core {

class Error;

namespace system {

// Enumeration of possible sources of memory information. When computing usage
// information, we indicate where the information comes from so we can provide
// detail in the UI.
enum MemoryProvider {
   // Source of stat unknown
   MemoryProviderUnknown = 0,

   // Native MacOS memory provider
   MemoryProviderMacOS,

   // Native Windows memory provider
   MemoryProviderWindows,

   // Linux provider based on cgroups; only available when process is inside a
   // cgroup
   MemoryProviderLinuxCgroups,
   
   // Linux provider based on ulimit; only useful if process has been given a
   // memory cap via setrlimit(1) and friends
   MemoryProviderLinuxUlimit,

   // Linux provider serving process memory information from the /proc virtual
   // filesystem
   MemoryProviderLinuxProcFs,

   // Linux provider serving system-wide memory stats from /proc/meminfo
   MemoryProviderLinuxProcMeminfo
};


// Returns the amount of memory used by the *current* system process (i.e. the one that calls this
// function). Note that this will not be the memory used by the main R session if called from a
// forked/child process.
Error getProcessMemoryUsed(long *pUsedKb, MemoryProvider *pProvider);

// Returns the total amount of memory in use.
Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider);

// Returns the total amount of available memory. This may be a physical constraint (e.g., physical
// RAM) or a virtual one (e.g., a cgroup-imposed limit)
Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider);

// Returns 0 if there's no limit. cgroups memory limits if enabled, or ulimit -m
Error getProcessMemoryLimit(long *pTotalKb, MemoryProvider *pProvider);

// Returns 0.0 if the cpu is unlimited, otherwise, the number of cpus with fractional allocation (e.g. 0.5)
Error getProcessCpuLimit(double *pNumCpus, MemoryProvider *pProvider);

#ifdef __linux__

// Selects how session and total memory usage are computed and reported. See the
// rsession.conf "memory-usage-mode" option for the meaning of each mode.
enum MemoryUsageMode {
   // Auto-detect: resolves to MemoryUsageModeContainer when running inside a
   // container and MemoryUsageModeDefault otherwise. This is the default.
   MemoryUsageModeAuto = 0,

   // Preserve the historical behavior: report the node's physical/cgroup RAM as
   // the total and rely on node free memory (plus a grace period) for aborts.
   MemoryUsageModeDefault,

   // Report the session's cgroup memory limit as the total so the session is
   // aborted when it reaches its own limit rather than when the node runs low.
   MemoryUsageModeContainer,

   // Force memory usage to be read from the cgroup.
   MemoryUsageModeCgroup,

   // Force memory usage to be read from the node's /proc/meminfo. Useful when
   // cgroup memory includes file cache that does not reflect actual session use.
   MemoryUsageModeMemInfo
};

// Sets the mode used to compute and report memory usage. Call once at startup,
// before the memory providers are first used.
void setMemoryUsageMode(MemoryUsageMode mode);

// Sets the memory limit. Must have privileges and provide the uid of the ultimate process owner
Error setProcessMemoryLimit(long memHighKb, long memMaxKb, uid_t uid, MemoryProvider *pProvider);

// Sets the cpu limit. Must have privileges and provide the uid of the ultimate process owner
Error setProcessCpuLimit(double numCpus, uid_t uid, MemoryProvider *pProvider);

#endif

// Returns the RSS + swap for the current process.
// The goal here is to choose values that are specific to a given process,
// that are using real resources on the system. Including swap so that processes
// that overflow main memory continue are measured based on their complete size.
long getProcessSize();

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_RESOURCES_HPP
