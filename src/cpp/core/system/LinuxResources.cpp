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
#include <shared_core/SafeConvert.hpp>

#include <core/system/Resources.hpp>

#include <core/Log.hpp>
#include <core/Thread.hpp>
#include <core/StringUtils.hpp>
#include <core/Algorithm.hpp>
#include <core/FileSerializer.hpp>

#include <iostream>
#include <fstream>

#include <sys/sysinfo.h>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace system {
namespace {

class LinuxMemoryProvider
{
public:

   virtual Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider) = 0;

   virtual Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider) = 0;

   Error getProcessMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
   {
      int size = 0;
      int resident = 0;
      try 
      {
         std::ifstream statm("/proc/self/statm");
         statm >> size >> resident;
         statm.close();
      }
      catch (...)
      {
         Error error = systemError(boost::system::errc::no_such_file_or_directory, 
               "Could not read process memory stats from /proc/self/statm", 
               ERROR_LOCATION);
         return error;
      }

      long pageKib = ::sysconf(_SC_PAGE_SIZE) / 1024;
      *pUsedKb = resident * pageKib;
      *pProvider = MemoryProviderLinuxProcFs;
      return Success();
   }
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

   Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider)
   {
      *pTotalKb = memTotal_;
      *pProvider = MemoryProviderLinuxProcMeminfo;
      return Success();
   }

private:

   // Parses /proc/meminfo to look up specific memory stats.
   Error readMemInfoKey(const std::string& key, int* pValue)
   {
      // /proc/meminfo contains lines that look like this:
      //
      // MemTotal: 8124360 kB
      
      // Open the file and prepare to read it
      FilePath memInfoFile("/proc/meminfo");
      std::shared_ptr<std::istream> pMemStream;
      Error error = memInfoFile.openForRead(pMemStream);
      if (error)
      {
         return error;
      }

      // Read one line at a time, looking for the key
      while (!pMemStream->eof())
      {
         std::string memLine;
         std::getline(*pMemStream, memLine);

         if (string_utils::isPrefixOf(memLine, key + ":"))
         {
            // This is the key we're looking for; read the value from the remainder of the line.
            try
            {
               std::stringstream lineStream(string_utils::substring(
                        memLine, key.size() + 1));
               lineStream >> *pValue;
            }
            catch (...)
            {
               error = systemError(boost::system::errc::protocol_error, 
                     "Could not read memory stat " 
                     "'" + key + "'"
                     " from /proc/meminfo line " 
                     "'" + memLine + "'",
                     ERROR_LOCATION);
            }

            // We found the key
            break;
         }
      }

      return error;
   }

   int memTotal_;
};

class CGroupsMemoryProvider : public LinuxMemoryProvider
{
public:
   CGroupsMemoryProvider(const std::string& path):
      path_(path)
   {
   }

   Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
   {
      Error error = getCgroupMemoryStat("memory.usage_in_bytes", pUsedKb); 
      if (!error)
      {
         *pProvider = MemoryProviderLinuxCgroups;
      }
      return error;
   }

   Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider)
   {
      Error error = getCgroupMemoryStat("memory.max_usage_in_bytes", pTotalKb); 
      if (!error)
      {
         *pProvider = MemoryProviderLinuxCgroups;
      }
      return error;
   }

private:
   // Gets a memory statistic from cgroup virtual file
   Error getCgroupMemoryStat(const std::string& key, int *pValue)
   {
      // Attempt to read the statistic from the file.
      FilePath statPath = FilePath("/sys/fs/cgroup/memory" + path_).completePath(key);
      std::string val;
      Error error = readStringFromFile(statPath, &val);
      if (error)
      {
         return error;
      }

      // Attempt to convert the file's entire contents to a stat
      boost::optional<long> stat = safe_convert::stringTo<long>(
            string_utils::trimWhitespace(val));
      if (!stat)
      {
         error = systemError(boost::system::errc::protocol_error, 
               "Could not read cgroup memory stat " 
               "'" + key + "'"
               " from " + statPath.getAbsolutePath() + " value " 
               "'" + val + "'",
               ERROR_LOCATION);
         return error;
      }

      // Convert to kb for return value
      *pValue = static_cast<int>(*stat / 1024);
      return Success();
   }

   std::string path_;
};

// Returns the memory cgroup path.
std::string getMemoryCgroup()
{
   FilePath cgroup("/proc/self/cgroup");
   if (!cgroup.exists())
   {
      // No problem, likely no cgroup support on this host. We'll use a different method to check
      // memory usage.
      return std::string();
   }

   std::shared_ptr<std::istream> pCgroupStream;
   Error error = cgroup.openForRead(pCgroupStream);
   if (error)
   {
      // Can't read cgroup file (unexpected)
      LOG_ERROR(error);
      return std::string();
   }

   // Loop through lines in cgroup file. We're trying to figure out where our memory entry is.
   // It looks like this:
   // 9:memory:/user.slice/user-1000.slice/user@1000.service
   try
   {
      while (!pCgroupStream->eof())
      {
         std::string line;
         std::getline(*pCgroupStream, line);
         std::vector<std::string> entries = core::algorithm::split(line, ":");

         // We expect 3 entries; from the example above they'd be:
         // 0. "9"
         // 1. "memory"
         // 2. "/user.slice/user-1000.slice/user@1000.service"
         if (entries.size() != 3)
         {
            continue;
         }

         // We are only interested in the "memory" entry.
         if (entries[1] == "memory")
         {
            // Return the memory entry.
            return entries[2];
         }
      }

      // If we got this far, we hit the end of the file without finding a memory entry.
      LOG_WARNING_MESSAGE("No memory control group found in /proc/self/cgroup");
   }
   catch (...)
   {
      LOG_WARNING_MESSAGE("Could not parse /proc/self/cgroup, will not use cgroup memory statistics");
   }

   return std::string();
}

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
      std::string cgroup = getMemoryCgroup();
      if (cgroup.empty())
      {
         s_provider.reset(new MemInfoMemoryProvider());
      }
      else
      {
         s_provider.reset(new CGroupsMemoryProvider(cgroup));
      }
   }
   END_LOCK_MUTEX;

   return s_provider;
}

} // anonymous namespace



Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider();
   if (provider)
   {
      return provider->getMemoryUsed(pUsedKb, pProvider);
   }

   *pUsedKb = 0;
   *pProvider = MemoryProviderUnknown;
   return Success();
}

Error getProcessMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider();
   if (provider)
   {
      return provider->getProcessMemoryUsed(pUsedKb, pProvider);
   }

   *pUsedKb = 0;
   *pProvider = MemoryProviderUnknown;
   return Success();
}

Error getTotalMemory(int *pTotalKb, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider();
   if (provider)
   {
      return provider->getTotalMemory(pTotalKb, pProvider);
   }

   *pTotalKb = 0;
   *pProvider = MemoryProviderUnknown;
   return Success();
}

} // namespace system
} // namespace core
} // namespace rstudio

