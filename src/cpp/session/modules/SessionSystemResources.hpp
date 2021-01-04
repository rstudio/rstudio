/*
 * SessionSystemResources.hpp
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
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

class MemoryStat {
public:
    int kb;
    core::system::MemoryProvider provider;
    core::json::Object toJson();
};

class MemoryUsage {
public:
    MemoryStat total;
    MemoryStat used;
    MemoryStat process;
    core::json::Object toJson();
};

core::Error getMemoryUsage(boost::shared_ptr<MemoryUsage> *pMemUsage);

core::Error initialize();

}  // namespace system_resources
}  // namespace modules
}  // namespace session
}  // namespace rstudio

#endif
