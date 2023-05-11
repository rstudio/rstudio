/*
 * SessionSystemResources.hpp
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


#ifndef SESSION_SYSTEM_RESOURCES_HPP
#define SESSION_SYSTEM_RESOURCES_HPP

#include <string>

#include <core/system/Resources.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace system_resources {

/**
 * MemoryStat represents a single memory usage statistic; a value in KiB and
 * the source (provider) of that value.
 */
class MemoryStat {
public:
   MemoryStat(): 
       kb(0), provider(core::system::MemoryProviderUnknown)
       {}
   MemoryStat(long kbIn, core::system::MemoryProvider providerIn): 
       kb(kbIn), provider(providerIn)
       {}
   core::json::Object toJson();

   long kb;
   core::system::MemoryProvider provider;
};

/**
 * MemoryUsage represents system-level memory usage.
 */
class MemoryUsage {
public:
   MemoryUsage() {}

   core::json::Object toJson();

   MemoryStat total;    // Total system memory
   MemoryStat used;     // System memory currently in use
   MemoryStat process;  // Memory used by the current process
};

// Get information on current memory usage
core::Error getMemoryUsage(boost::shared_ptr<MemoryUsage> *pMemUsage);

core::Error initialize();

void emitMemoryChangedEvent();

}  // namespace system_resources
}  // namespace modules
}  // namespace session
}  // namespace rstudio

#endif
