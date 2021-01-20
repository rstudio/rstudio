/*
 * LinuxResources.cpp
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

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/Resources.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>

#include <sys/sysinfo.h>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace system {
namespace {

class LinuxMemoryProvider
{
public:
   virtual Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)        = 0;
   virtual Error getProcessMemoryUsed(int *pUsedKb, MemoryProvider *pProvider) = 0;
   virtual Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider)      = 0;
};

class MemInfoMemoryProvider : public LinuxMemoryProvider
{
public:
   MemInfoMemoryProvider(): memTotal_(0)
   {
      // Read total memory only once (it doesn't change)
      Error error = readMemInfoKey("MemTotal", &memTotal_);
      if (error)
      {
         LOG_ERROR(error);
      }
   }

   Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
   {
      int availableKb = 0;
      Error error = readMemInfoKey("MemAvailable", &availableKb);
      if (error)
      {
         return error;
      }
      *pUsedKb = memTotal_ - availableKb;
      *pProvider = MemoryProviderLinuxProcMeminfo;
      return Success();
   }

   Error getProcessMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
   {
      // TODO: get memory used by process
      *pProvider = MemoryProviderLinuxProcMeminfo;
      return Success();
   }

   Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider)
   {
      *pTotalKb = memTotal_;
      *pProvider = MemoryProviderLinuxProcMeminfo;
      return Success();
   }

private:
   Error readMemInfoKey(const std::string& key, int* pValue)
   {
      // TODO: parse meminfo
      return Success();
   }

   int memTotal_;
};

class CGroupsMemoryProvider : public LinuxMemoryProvider
{
   Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
   {
      // TODO: get memory used from cgroups file
      return Success();
   }

   Error getProcessMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
   {
      // TODO: get memory used by process
      return Success();
   }

   Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider)
   {
      // TODO: get memory total from cgroups file
      return Success();
   }
};

// Factory for memory provider instantiation
boost::shared_ptr<LinuxMemoryProvider> getMemoryProvider()
{
   static boost::mutex s_mutex;
   static boost::shared_ptr<LinuxMemoryProvider> s_provider;

   // Return memory provider if we have one
   if (s_provider)
   {
      return s_provider;
   }

   // Otherwise, try to create one
   LOCK_MUTEX(s_mutex)
   {
      // Check /sys/fs for virtual cgroups filesystem.
      FilePath cgroup("/sys/fs/cgroup/memory");
      if (cgroup.exists())
      {
         s_provider.reset(new CGroupsMemoryProvider());
      }
      else
      {
         s_provider.reset(new MemInfoMemoryProvider());
      }
   }
   END_LOCK_MUTEX;

   return s_provider;
}

} // anonymous namespace



Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
{
    return Success();
}

Error getProcessMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
{
    return Success();
}

Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider)
{
    return Success();
}

} // namespace system
} // namespace core
} // namespace rstudio

