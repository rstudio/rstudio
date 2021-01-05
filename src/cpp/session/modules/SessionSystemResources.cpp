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

#include <boost/make_shared.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace system_resources {

json::Object MemoryStat::toJson()
{
   json::Object stat;
   stat["kb"] = kb;
   stat["provider"] = static_cast<int>(provider);
   return stat;
}

json::Object MemoryUsage::toJson()
{
   json::Object usage;
   usage["total"] = total.toJson();
   usage["used"] = used.toJson();
   usage["process"] = process.toJson();
   return usage;
}

Error getMemoryUsage(boost::shared_ptr<MemoryUsage> *pMemUsage)
{
   boost::shared_ptr<MemoryUsage> pStats = boost::make_shared<MemoryUsage>();

   Error error;
   int kb;
   core::system::MemoryProvider provider;

   error = core::system::getTotalMemory(&kb, &provider);
   if (error)
      return error;
   pStats->total = MemoryStat(kb, provider);

   error = core::system::getMemoryUsed(&kb, &provider);
   if (error)
      return error;
   pStats->used = MemoryStat(kb, provider);

   error = core::system::getProcessMemoryUsed(&kb, &provider);
   if (error)
      return error;
   pStats->process = MemoryStat(kb, provider);

   *pMemUsage = pStats;
   return core::Success();
}

Error initialize()
{
   return Success();
}

}  // namespace system_resources
}  // namespace modules
}  // namespace session
}  // namespace rstudio
