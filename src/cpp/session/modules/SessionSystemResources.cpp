/*
 * SessionSystemResources.cpp
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

#include "SessionSystemResources.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace system_resources {

core::Error getMemoryUsage(boost::shared_ptr<MemoryUsage> *pMemUsage)
{
    return core::Success();
}

core::Error initialize()
{
   return core::Success();
}

}  // namespace system_resources
}  // namespace modules
}  // namespace session
}  // namespace rstudio
