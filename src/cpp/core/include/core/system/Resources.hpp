/*
 * Resources.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_RESOURCES_HPP
