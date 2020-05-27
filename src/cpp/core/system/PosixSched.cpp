/*
 * PosixSched.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/system/PosixSched.hpp>

#include <algorithm>
#include <sched.h>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {

int cpuCount()
{
   return sysconf(_SC_NPROCESSORS_ONLN);
}

CpuAffinity emptyCpuAffinity()
{
   return std::vector<bool>(cpuCount(), false);
}

bool isCpuAffinityEmpty(const CpuAffinity& cpus)
{
   return std::count(cpus.begin(), cpus.end(), true) == 0;
}

Error getCpuAffinity(CpuAffinity* pCpus)
{
#ifndef __APPLE__
   cpu_set_t cs;
   CPU_ZERO(&cs);
   if (::sched_getaffinity(0, sizeof(cs), &cs) == -1)
      return systemError(errno, ERROR_LOCATION);

   pCpus->clear();
   int cpus = cpuCount();
   for (int i = 0; i<cpus; i++)
   {
      if (CPU_ISSET(i, &cs))
         pCpus->push_back(true);
      else
         pCpus->push_back(false);
   }

   return Success();
#else
   return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
#endif
}


Error setCpuAffinity(const CpuAffinity& cpus)
{
#ifndef __APPLE__
   cpu_set_t cs;
   CPU_ZERO(&cs);

   for (std::size_t i=0; i<cpus.size(); i++)
   {
      if (cpus[i])
         CPU_SET(i, &cs);
      else
         CPU_CLR(i, &cs);
   }

   if (::sched_setaffinity(0, sizeof(cs), &cs) == -1)
      return systemError(errno, ERROR_LOCATION);

   return Success();
#else
   return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
#endif
}

} // namespace system
} // namespace core
} // namespace rstudio


