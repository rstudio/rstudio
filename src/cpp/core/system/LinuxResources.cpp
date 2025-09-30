/*
 * LinuxResources.cpp
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

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/Algorithm.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/Thread.hpp>
#include <core/Truncating.hpp>
#include <core/system/Resources.hpp>
#include <core/system/PosixSystem.hpp>

#include <boost/regex.hpp>

#include <iostream>
#include <fstream>
#include <dirent.h>

#include <sys/sysinfo.h>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace system {
namespace {

// Parses /proc/meminfo to look up specific memory stats.
Error readProcFileKeys(const std::string& procPath, const std::vector<std::string>& keys, std::vector<long>* pValues)
{
   // /proc/meminfo and /proc/<pid>/status contains lines that look like this:
   //
   // MemTotal: 8124360 kB

   // Open the file and prepare to read it
   FilePath memInfoFile(procPath);
   std::shared_ptr<std::istream> pMemStream;
   Error error = memInfoFile.openForRead(pMemStream);
   if (error)
   {
      return error;
   }
   std::size_t numFound = 0;

   // Read one line at a time, looking for each key
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
         error.addProperty("path", procPath);
         return error;
      }

      for (std::size_t i = 0; i < keys.size(); i++)
      {
         const std::string& key = keys[i];
         if (string_utils::isPrefixOf(memLine, keys[i] + ":"))
         {
            // This is the key we're looking for; read the value from the remainder of the line.
            try
            {
               std::stringstream lineStream(string_utils::substring(
                        memLine, key.size() + 1));
               long nextValue;
               lineStream >> nextValue;
               (*pValues)[i] = nextValue;
               numFound++;
            }
            catch (...)
            {
               error = systemError(boost::system::errc::protocol_error,
                     "Could not read proc path value "
                     "'" + key + "'"
                     " from " + procPath + " line "
                     "'" + memLine + "'",
                     ERROR_LOCATION);
            }

            // We found the key
            break;
         }
      }
      if (error || numFound == keys.size())
         break;
   }
   if (error)
      return error;

   if (numFound != keys.size())
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Proc stat file: " + procPath + " missing value - found only: " + std::to_string(numFound) + " of: " +
                         std::to_string(keys.size()) + " keys",
                         ERROR_LOCATION);
   }
   return Success();
}

// Returns the RSS + swap for the specified process.
// The goal here is to choose values that are specific to a given process,
// that are using real resources on the system. Including swap so that processes
// that overflow main memory continue are measured based on their complete size.
long getProcessSize(PidType pid)
{
   std::string statmPath = "/proc/" + std::to_string(pid) + "/statm";

   std::vector<std::string> keys = {"VmRSS", "VmSwap"};
   std::vector<long> values = {0, 0};

   Error error = readProcFileKeys("/proc/" + std::to_string(pid) + "/status", keys, &values);
   if (error)
   {
      LOG_ERROR(error);
      return 0;
   }

   // Return the sum of rss and swap in kb
   return values[0] + values[1];
}

bool isChildOfParent(const std::map<PidType, PidType>& childToParentMap, PidType parentPid, PidType childPid)
{
   if (childPid == parentPid)
      return true;
   do
   {
      auto it = childToParentMap.find(childPid);
      if (it == childToParentMap.end())
         return false;
      PidType nextParent = it->second;
      if (nextParent == parentPid)
         return true;
      childPid = nextParent;
   } while (true);
}

long getProcessSizeOfChildren(PidType parentPid, UidType userId)
{
   DIR *pDir = nullptr;
   long childRssSize = 0;
   std::map<PidType, PidType> childToParentMap;

   try
   {
      // open the /proc directory
      pDir = ::opendir("/proc");
      if (pDir == nullptr)
         return 0;

      struct dirent *pDirEnt;
      while ( (pDirEnt = ::readdir(pDir)) )
      {
         if (pDirEnt->d_type != DT_DIR)
            continue;

         // If the name is not an int, it's not a process
         PidType pid = safe_convert::stringTo<PidType>(pDirEnt->d_name, -1);
         if (pid <= 0)
            continue;

         FilePath procDir = FilePath("/proc").completeChildPath(pDirEnt->d_name);

         // Check that the process is owned by the same user
         uid_t fileUid;
         Error error = procDir.getFileOwner(fileUid);
         if (error || fileUid != userId)
            continue;

         std::string statusPath = "/proc/" + std::string(pDirEnt->d_name) + "/status";
         std::ifstream statusFile(statusPath);
         if (statusFile.is_open())
         {
            std::string line;
            while (std::getline(statusFile, line)) {
               if (line.find("PPid:") == 0) {
                  std::istringstream iss(line.substr(5));
                  PidType ppid;
                  iss >> ppid;
                  // Sanity check that the child is not already a parent of this new parent
                  // to avoid cycles in the childToParentMap
                  if (!isChildOfParent(childToParentMap, pid, ppid))
                     childToParentMap[pid] = ppid;
                  break;
                }
            }
            statusFile.close();
         }
      }
      ::closedir(pDir);
      pDir = nullptr;
   }
   catch (...)
   {
      if (pDir != nullptr)
         ::closedir(pDir);
      return 0;
   }
   int numChildren = 0;
   for (const auto& entry : childToParentMap)
   {
      PidType pid = entry.first;
      PidType childParent = entry.second;
      if (childParent == parentPid || isChildOfParent(childToParentMap, parentPid, childParent))
      {
         childRssSize += getProcessSize(pid);
         numChildren++;
      }
   }
   return childRssSize;
}

/**
 * LinuxMemoryProvider is an abstract class that provides memory usage stats on
 * Linux. As there are several ways of limiting and measuring memory usage on
 * Linux, we figure out the most appropriate one at runtime and create the 
 * corresponding provider (see getMemoryProvider factory)
 */
class LinuxMemoryProvider
{
public:

   virtual ~LinuxMemoryProvider()
   {
   }

   virtual Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider) = 0;

   virtual Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider) = 0;

   virtual Error getProcessMemoryLimit(long *pLimitKb, MemoryProvider *pProvider)
   {
      RLimitType softMemLimit, hardMemLimit;
      Error error = getResourceLimit(MemoryLimit, &softMemLimit, &hardMemLimit);
      if (error)
         return error;
      if (softMemLimit != RLIM_INFINITY)
      {
         *pLimitKb = softMemLimit/1024;
         *pProvider = MemoryProviderLinuxUlimit;
      }
      else if (hardMemLimit != RLIM_INFINITY)
      {
         *pLimitKb = hardMemLimit/1024;
         *pProvider = MemoryProviderLinuxUlimit;
      }
      else
      {
         *pLimitKb = 0;
         *pProvider = MemoryProviderUnknown;
      }
      return Success();
   }

   virtual Error getProcessMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
   {
      PidType pid = ::getpid();
      long resident = getProcessSize(pid);

      long childSizeKb = getProcessSizeOfChildren(pid, ::geteuid());

      *pUsedKb = resident + childSizeKb;
      *pProvider = MemoryProviderLinuxProcFs;

      return Success();
   }

   virtual Error getProcessCpuLimit(double *pNumCpus, MemoryProvider *pProvider)
   {
      *pNumCpus = 0.0;
      *pProvider = MemoryProviderUnknown;
      return Success();
   }

   virtual Error setProcessMemoryLimit(long memHighKb, long memMaxKb, MemoryProvider *pProvider)
   {
      *pProvider = MemoryProviderLinuxUlimit;
      return setResourceLimit(MemoryLimit, memHighKb * 1024);
   }

   virtual Error setProcessCpuLimit(double numCpus, MemoryProvider *pProvider)
   {
      // No way to set cpu limits in this config. Requires cgroups v2 and a non-shared cgroup
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
      std::vector<std::string> keys = {"MemAvailable", "SwapTotal", "SwapFree"};
      std::vector<long> values = {0, 0, 0};
      Error error = readProcFileKeys("/proc/meminfo", keys, &values);
      if (error)
      {
         return error;
      }
      long availableKb = values[0];
      long swapTotalKb = values[1];
      long swapFreeKb = values[2];
      // Want to compute the total memory used by processes. So this is
      // how much swap space is used plus the total memory minus the mem
      // available.
      *pUsedKb = memTotal_ + (swapTotalKb - swapFreeKb) - availableKb;
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
   Error readMemInfoKey(const std::string& key, long* pValue)
   {
      std::vector<std::string> keys(1);
      keys[0] = key;
      std::vector<long> values(1);
      Error error = readProcFileKeys("/proc/meminfo", keys, &values);
      if (!error)
         *pValue = values[0];
      return error;
   }

   long memTotal_;
};

/**
 * CGroupsMemoryProvider is a class supplying Linux memory statistics using the
 * contents of the virtual cgroups filesystem at /sys/fs/cgroup.
 */
class CGroupsMemoryProvider : public MemInfoMemoryProvider
{
public:
   CGroupsMemoryProvider(const std::string& path) : isV2_(false)
   {
      // In some cgroup v1 environments with pam_systemd, user scoped memory stats are available in e.g.,
      //
      // /sys/fs/cgroup/memory/user.slice/user-1000.slice/user@1000.service/memory/memory.usage_in_bytes
      //
      // In others, it will be a shared group that we can't use:
      //
      // either:

      // /sys/fs/cgroup/memory/system.slice/launcher.service
      //
      // or:
      //
      // /sys/fs/cgroup/memory/memory.usage_in_bytes
      //
      // For those using cgroups v2 using pam_systemd, stats are under the same unified
      // hierarchy, e.g.
      //
      // /sys/fs/cgroup/user.slice/user-1000.slice/user@1000.service/memory.max
      //
      // With local launcher cgroups, the path is:
      // /sys/fs/cgroup/system.slice/rstudio-launcher.service/jobs/MlxEEgj0TeFsfIwX9ojxWg==
      //
      // Check to see whether the path we read from the process's cgroup file
      // exists. If it does, we can read scoped memory stats.
      if (FilePath("/sys/fs/cgroup/memory" + path).exists())
      {
         // Use scoped memory stats
         path_ = FilePath("/sys/fs/cgroup/memory" + path);
      }
      else if (FilePath("/sys/fs/cgroup" + path).exists())
      {
         // Use cgroups v2 stats
         path_ = FilePath("/sys/fs/cgroup" + path);
         isV2_ = true;
      }
      else
      {
         // Container runtimes using cgroups v1 do not use namespacing, so the
         // cgroup path entries in /proc/self/cgroup will not exist. When this
         // occurs, we default to /sys/fs/cgroup/memory, which is the mount path
         // of the v1 memory cgroup most container runtimes use.
         path_ = FilePath("/sys/fs/cgroup/memory");
      }
   }

   // Overriding the base classes's process memory method that accumulates RSS size of all children since this is way more efficient.
   // Should be roughly the same, as long as this cgroup is scoped to the session process (as is true in all cases now)
   //
   // If we do add user and group based cgroups, we'll need to identify which mode is used and return the cgroup
   // values in the appropriate category.
   virtual Error getProcessMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
   {
      Error error;
      if (!isV2_)
      {
         error = getCgroupMemoryStat("memory.usage_in_bytes", pUsedKb);
      }
      else
      {
         error = getCgroupMemoryStat("memory.current", pUsedKb);
      }
      if (!error)
      {
         *pProvider = MemoryProviderLinuxCgroups;
      }
      else
      {
         // Even if cgroups is enabled, the memory controller for cgroups may not be installed.
         // Fall back on looking at process memmory from ps
         return LinuxMemoryProvider::getProcessMemoryUsed(pUsedKb, pProvider);
      }

      return error;
   }

   // Returns a value of 0 if there's no limit
   virtual Error getProcessMemoryLimit(long *pTotalKb, MemoryProvider *pProvider)
   {
      long linuxLimitKb;
      MemoryProvider linuxMemProvider;
      bool hasLinuxLimit = false;

      // Do we have a ulimit based limit?
      Error error = LinuxMemoryProvider::getProcessMemoryLimit(&linuxLimitKb, &linuxMemProvider);
      if (!error && linuxLimitKb != 0)
      {
         hasLinuxLimit = true;
      }

      error = getCgroupsMemoryLimit(pTotalKb, pProvider);

      if (!error)
      {
	 // pick the smaller of the two limits
         if (hasLinuxLimit && (*pTotalKb == LONG_MAX || *pTotalKb > linuxLimitKb))
         {
            *pProvider = linuxMemProvider;
            *pTotalKb = linuxLimitKb;
         }
         else
         {
            if (*pTotalKb == LONG_MAX)
               *pTotalKb = 0;
         }
      }
      else if (hasLinuxLimit)
      {
         *pProvider = linuxMemProvider;
         *pTotalKb = linuxLimitKb;
      }
      else // no limit
      {
         *pProvider = MemoryProviderUnknown;
         *pTotalKb = 0;
      }

      return Success();
   }

   // Returns LONG_MAX if there is no limit set
   Error getCgroupsMemoryLimit(long *pTotalKb, MemoryProvider *pProvider)
   {
      Error error;

      if (!isV2_)
      {
         error = getCgroupMemoryStat("memory.limit_in_bytes", pTotalKb);
      }
      else
      {
         // cgroups v2 furnishes both a soft and a hard memory limit; we need to
         // check them both.
         error = getCgroupMemoryStat("memory.high", pTotalKb);
         if (error)
         {
           return error;
         }
         if (*pTotalKb == LONG_MAX) // check the hard limit
         {
            error = getCgroupMemoryStat("memory.max", pTotalKb);
         }
      }
      if (!error)
      {
         *pProvider = MemoryProviderLinuxCgroups;
      }
      return error;
   }

   virtual Error getProcessCpuLimit(double *pNumCpus, MemoryProvider *pProvider)
   {
      if (!isV2_)
      {
         *pNumCpus = 0.0;
         *pProvider = MemoryProviderUnknown;
      }
      else
      {
         Error error = getCgroupV2NumCpus(pNumCpus, pProvider);
         if (error)
         {
            *pNumCpus = 0.0;
            *pProvider = MemoryProviderUnknown;
         }
         else
         {
            *pProvider = MemoryProviderLinuxCgroups;
         }
      }
      return Success();
   }

   virtual Error setProcessMemoryLimit(long highMemKb, long maxMemKb, MemoryProvider *pProvider)
   {
      Error error;
      *pProvider = MemoryProviderLinuxCgroups;
      if (!isV2_)
         error = setCgroupMemoryStat("memory.limit_in_bytes", maxMemKb * 1024);
      else
      {
         error = setCgroupMemoryStat("memory.high", highMemKb * 1024);
         if (!error)
            error = setCgroupMemoryStat("memory.max", maxMemKb * 1024);
      }
      return error;
   }

   virtual Error setProcessCpuLimit(double numCpus, MemoryProvider *pProvider)
   {
      *pProvider = MemoryProviderLinuxCgroups;

      if (isV2_)
      {
         int period = getCgroupV2CpuPeriod(); // 100ms
         // cpu.max = 'quota period'
         Error error = setCgroupCpuStat("cpu.max", std::to_string(static_cast<int>(numCpus * period)) + " " + std::to_string(period));
         if (error)
            LOG_DEBUG_MESSAGE("Error setting cpu.max: " + error.asString());
      }
      // else pam_systemd does not provision cpu cgroups on v1 so can't set them here

      return Success();
   }

private:
   // Gets a memory statistic from cgroup virtual file
   Error getCgroupMemoryStat(const std::string& key, long *pValue)
   {
      // Attempt to read the statistic from the file.
      FilePath statPath = path_.completePath(key);
      std::string val;
      Error error = readStringFromFile(statPath, &val);
      if (error)
      {
         return error;
      }

      // Remove whitespace; these virtual files usually end with a newline
      val = string_utils::trimWhitespace(val);

      if (val == "9223372036854771712" || val == "max")
      {
         // The number above is a special value indicating no memory limit (it
         // is roughly the maximum int64 value), which is used for cgroups v1.
         // Under cgroups v2, the value will be the literal string "max"
         // instead.
         *pValue = LONG_MAX;
         return Success();
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

   Error setCgroupMemoryStat(const std::string& key, long val)
   {
      FilePath statPath = path_.completePath(key);
      Error error = writeStringToFile(statPath, std::to_string(val));
      if (error)
         return error;
      return Success();
   }

   // Gets the cpu.max value - returning 0.0 if no limit.
   Error getCgroupV2NumCpus(double *pNumCpus, MemoryProvider *pProvider)
   {
      // Attempt to read the statistic from the file.
      FilePath statPath = path_.completePath("cpu.max");
      std::string cpuMaxValue;
      Error error = readStringFromFile(statPath, &cpuMaxValue);
      if (error)
      {
         return error;
      }

      // Remove whitespace; these virtual files usually end with a newline
      std::istringstream iss(cpuMaxValue);
      std::string limitStr, periodStr;
      if (!(iss >> limitStr >> periodStr))
      {
         return systemError(boost::system::errc::invalid_argument, "Failed to parse cpu.max: " + cpuMaxValue, ERROR_LOCATION);
      }
      if (limitStr == "max")
      {
         *pNumCpus = 0.0;
      }
      else
      {
         int limitInt = safe_convert::stringTo<int>(limitStr, 0);
         int periodInt = safe_convert::stringTo<int>(periodStr, 0);
         if (limitInt == 0 || periodInt == 0)
         {
            LOG_DEBUG_MESSAGE("Failed to convert cpu.max: " + limitStr + " " + periodStr);
            *pNumCpus = 0.0;
            *pProvider = MemoryProviderUnknown;
            return Success();
         }
         *pNumCpus = static_cast<double>(limitInt) / periodInt;
      }

      *pProvider = MemoryProviderLinuxCgroups;

      return Success();
   }

   Error setCgroupCpuStat(const std::string& key, std::string val)
   {
      FilePath statPath = path_.completePath(key);
      Error error = writeStringToFile(statPath, val);
      if (error)
      {
         error.addProperty("description", "Failed to set cgroup cpu stat: " + key + " to: " +
                           val + " path: " + statPath.getAbsolutePath());
         return error;
      }
      return Success();
   }

   // The system is configured with a specific time period so use that when computing cpu limits.
   int getCgroupV2CpuPeriod()
   {
      std::string cpuMaxValue;
      int defaultPeriod = 100000;
      FilePath cpuMaxFile = path_.completePath("cpu.max");

      core::Error error = core::readStringFromFile(cpuMaxFile, &cpuMaxValue);
      if (error)
      {
         LOG_DEBUG_MESSAGE("Failed to read cpu.max: " + error.asString());
         return defaultPeriod;
      }

      std::istringstream iss(cpuMaxValue);
      std::string limit, period;
      if (!(iss >> limit >> period))
      {
         LOG_DEBUG_MESSAGE("Failed to parse cpu.max: " + cpuMaxValue);
         return defaultPeriod;
      }
      return safe_convert::stringTo<int>(period, defaultPeriod);
   }

   FilePath path_;
   bool isV2_;
};

bool isSharedCgroup(const std::string& path, uid_t uid)
{
   boost::regex systemRegex(R"(^/system\.slice/[^/]+\.service$)");
   boost::regex userRegex(R"(^/user\.slice/user-(\d+)\.slice/.*$)");
   boost::regex launcherPattern(R"(^/system\.slice/rstudio-launcher\.service/jobs/.*$)");

   // don't use cgroups for: /system.slice/launcher.service (shared by all users)
   if (boost::regex_match(path, systemRegex))
   {
      return true;
   }

   boost::smatch match;
   // use cgroups for: /user.slice/user-<uid>.slice if uid is the current user
   // Did pam_systemd step that creates this run for the session user or the system user?
   if (boost::regex_match(path, match, userRegex))
   {
      uid_t cgroupUid = static_cast<uid_t>(std::stoul(match[1]));
      return cgroupUid != uid;
   }

   // Use it if it's created from the job launcher
   if (boost::regex_match(path, launcherPattern))
   {
      return false;
   }

   // Kubernetes pod - this is the cgroup
   if (path == "0://")
      return false;

   LOG_DEBUG_MESSAGE("Unrecognized cgroup pattern, assuming a shared group and using /proc/meminfo for memory usage: " + path);

   // By default, assume it's a shared cgroup and don't use it
   return true;
}

// Returns the memory cgroup path.
std::string getMemoryCgroup(uid_t uid)
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
   //
   // When using cgroups v2, it looks instead like this:
   // 0::/user.slice/user-1000.slice/user@1000.service
   try
   {
      std::string pattern = R"(^/system\.slice/[^/]+\.service$)";
      boost::regex systemRegex(pattern);

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

         // cgroups v2; everything is under one hierarchy.
         if (entries[0] == "0")
         {
            if (isSharedCgroup(entries[2], uid))
               return std::string();
            return entries[2];
         }

         // We are only interested in the "memory" entry.
         if (entries[1] == "memory")
         {
            if (isSharedCgroup(entries[2], uid))
               return std::string();

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
boost::shared_ptr<LinuxMemoryProvider> getMemoryProvider(bool writeLimit, uid_t uid)
{
   static boost::mutex s_mutex;
   static boost::shared_ptr<LinuxMemoryProvider> s_readLimitProvider;
   static boost::shared_ptr<LinuxMemoryProvider> s_writeLimitProvider;

   // Return memory provider if we have one
   if (writeLimit && s_writeLimitProvider)
   {
      return s_writeLimitProvider;
   }
   else if (!writeLimit && s_readLimitProvider)
   {
      return s_readLimitProvider;
   }

   // Otherwise, try to create one
   LOCK_MUTEX(s_mutex)
   {
      std::string cgroup = getMemoryCgroup(uid);

      if (!cgroup.empty())
      {
         // We got a cgroup. Does it have a limit?
         boost::shared_ptr<CGroupsMemoryProvider> provider = 
            boost::make_shared<CGroupsMemoryProvider>(cgroup);

         // Check memory limit
         long totalKb;
         MemoryProvider tmpProvider;
         Error error = provider->getTotalMemory(&totalKb, &tmpProvider);

         if (error)
         {
            LOG_ERROR(error);
         }
         else if (totalKb != LONG_MAX) // i.e. no limitation via cgroups
         {
            s_readLimitProvider = provider;
         }
         s_writeLimitProvider = provider;
      }

      // Fall back on the default if we don't have a provider at this point.
      if (!s_readLimitProvider)
      {
         s_readLimitProvider.reset(new MemInfoMemoryProvider());
      }
      if (!s_writeLimitProvider)
      {
         s_writeLimitProvider.reset(new MemInfoMemoryProvider());
      }
   }
   END_LOCK_MUTEX;

   return writeLimit ? s_writeLimitProvider : s_readLimitProvider;
}

} // anonymous namespace



Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider(false, ::geteuid());
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
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider(false, ::geteuid());
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
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider(false, ::geteuid());
   if (provider)
   {
      return provider->getTotalMemory(pTotalKb, pProvider);
   }

   *pTotalKb = 0;
   *pProvider = MemoryProviderUnknown;
   return Success();
}

Error getProcessMemoryLimit(long *pTotalKb, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider(false, ::geteuid());
   if (provider)
   {
      return provider->getProcessMemoryLimit(pTotalKb, pProvider);
   }

   *pTotalKb = 0;
   *pProvider = MemoryProviderUnknown;
   return Success();
}

Error getProcessCpuLimit(double *pNumCpus, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider(false, ::geteuid());
   if (provider)
   {
      return provider->getProcessCpuLimit(pNumCpus, pProvider);
   }

   *pNumCpus = 0;
   *pProvider = MemoryProviderUnknown;
   return Success();
}

Error setProcessMemoryLimit(long highMemKb, long maxMemKb, uid_t uid, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider(true, uid);
   if (provider)
   {
      return provider->setProcessMemoryLimit(highMemKb, maxMemKb, pProvider);
   }

   *pProvider = MemoryProviderUnknown;
   return systemError(boost::system::errc::not_supported,
                      "Process memory limit not supported",
                      ERROR_LOCATION);
}

Error setProcessCpuLimit(double numCpus, uid_t uid, MemoryProvider *pProvider)
{
   boost::shared_ptr<LinuxMemoryProvider> provider = getMemoryProvider(true, uid);
   if (provider)
   {
      return provider->setProcessCpuLimit(numCpus, pProvider);
   }
   *pProvider = MemoryProviderUnknown;
   return systemError(boost::system::errc::not_supported,
                      "Process cpu limit not supported",
                      ERROR_LOCATION);
}

long getProcessSize()
{
   return getProcessSize(::getpid());
}

} // namespace system
} // namespace core
} // namespace rstudio

