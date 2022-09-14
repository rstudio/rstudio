/*
 * LinuxResources.cpp
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

/**
 * LinuxMemoryProvider is an abstract class that provides memory usage stats on
 * Linux. As there are several ways of limiting and measuring memory usage on
 * Linux, we figure out the most appropriate one at runtime and create the 
 * corresponding provider (see getMemoryProvider factory)
 */
class LinuxMemoryProvider
{
public:

   virtual Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider) = 0;

   virtual Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider) = 0;

   Error getProcessMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
   {
      long size = 0;
      long resident = 0;
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

/**
 * MemInfoMemoryProvider is a class supplying Linux memory statistics using the
 * contents of the file /proc/meminfo.
 */
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

   Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
   {
      long availableKb = 0;
      Error error = readMemInfoKey("MemAvailable", &availableKb);
      if (error)
      {
         return error;
      }
      *pUsedKb = memTotal_ - availableKb;
      *pProvider = MemoryProviderLinuxProcMeminfo;
      return Success();
   }

   Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider)
   {
      *pTotalKb = memTotal_;
      *pProvider = MemoryProviderLinuxProcMeminfo;
      return Success();
   }

private:

   // Parses /proc/meminfo to look up specific memory stats.
   Error readMemInfoKey(const std::string& key, long* pValue)
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
         try
         {
            std::getline(*pMemStream, memLine);
         }
         catch(const std::exception& e)
         {
            Error error = systemError(boost::system::errc::io_error, 
                                      ERROR_LOCATION);
            error.addProperty("what", e.what());
            error.addProperty("path", "/proc/meminfo");
            return error;
         }

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

   long memTotal_;
};

/**
 * CGroupsMemoryProvider is a class supplying Linux memory statistics using the
 * contents of the virtual cgroups filesystem at /sys/fs/cgroup.
 */
class CGroupsMemoryProvider : public LinuxMemoryProvider
{
public:
   CGroupsMemoryProvider(const std::string& path)
   {
      // In some environments, scoped memory stats are available in e.g.,
      //
      // /sys/fs/cgroup/memory/user.slice/user-1000.slice/user@1000.service/memory/memory.usage_in_bytes
      // 
      // In others, we will need to read the memory stats at a system level,
      // e.g.,
      //
      // /sys/fs/cgroup/memory/memory.usage_in_bytes
      //
      // Check to see whether the path we read from the process's cgroup file
      // exists. If it does, we can read scoped memory stats.
      FilePath statPath("/sys/fs/cgroup/memory" + path);
      if (statPath.exists())
      {
         // Use scoped memory stats
         path_ = path;
      }
      else
      {
         // Use system memory stats
         path_ = "";
      }
   }

   Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
   {
      Error error = getCgroupMemoryStat("memory.usage_in_bytes", pUsedKb); 
      if (!error)
      {
         *pProvider = MemoryProviderLinuxCgroups;
      }
      return error;
   }

   Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider)
   {
      Error error = getCgroupMemoryStat("memory.limit_in_bytes", pTotalKb); 
      if (!error)
      {
         *pProvider = MemoryProviderLinuxCgroups;
      }
      return error;
   }

   // Gets a memory value from cgroup virtual file
   Error getCGroupMemoryValue(const std::string&key, std::string *pValue)
   {
      // Attempt to read the statistic from the file.
      FilePath statPath = FilePath("/sys/fs/cgroup/memory" + path_).completePath(key);
      std::string val;
      Error error = readStringFromFile(statPath, &val);
      if (error)
      {
         return error;
      }

      // Remove whitespace; these virtual files usually end with a newline
      val = string_utils::trimWhitespace(val);
      *pValue = val;

      return Success();
   }

private:
   // Gets a memory statistic from cgroup virtual file
   Error getCgroupMemoryStat(const std::string& key, long *pValue)
   {
      // Get the raw value from the file
      std::string val;
      Error error = getCGroupMemoryValue(key, &val);
      if (error)
      {
         return error;
      }

      // Attempt to convert the file's contents to a stat
      boost::optional<long> stat = safe_convert::stringTo<long>(val);
      if (!stat)
      {
         error = systemError(boost::system::errc::protocol_error, 
               "Could not read cgroup memory stat " 
               "'" + key + "'"
               " from " 
               "'" + val + "'",
               ERROR_LOCATION);
         return error;
      }

      // Convert to kb for return value
      *pValue = *stat / 1024;
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
      LOG_INFO_MESSAGE("No memory control group found in /proc/self/cgroup");
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

      if (!cgroup.empty())
      {
         // We got a cgroup. Does it have a limit?
         boost::shared_ptr<CGroupsMemoryProvider> provider = 
            boost::make_shared<CGroupsMemoryProvider>(cgroup);

         // Check memory limit
         std::string val;
         Error error = provider->getCGroupMemoryValue("memory.limit_in_bytes", &val);

         if (error)
         {
            LOG_ERROR(error);
         }
         else if (val != "9223372036854771712")
         {
            // The above is a special value indicating no memory limit (it is
            // roughly the maximum int64 value). If we don't find it, we can use
            // the cgroup to provide a memory limit.
            s_provider = provider;
         }
      }

      // Fall back on the default if we don't have a provider at this point.
      if (!s_provider)
      {
         s_provider.reset(new MemInfoMemoryProvider());
      }
   }
   END_LOCK_MUTEX;

   return s_provider;
}

} // anonymous namespace



Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider();
   if (provider)
   {
      return provider->getTotalMemoryUsed(pUsedKb, pProvider);
   }

   *pUsedKb = 0;
   *pProvider = MemoryProviderUnknown;
   return Success();
}

Error getProcessMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
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

Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider)
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

