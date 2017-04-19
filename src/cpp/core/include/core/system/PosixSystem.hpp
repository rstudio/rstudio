/*
 * PosixSystem.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef CORE_SYSTEM_POSIX_SYSTEM_HPP
#define CORE_SYSTEM_POSIX_SYSTEM_HPP

#include <core/system/System.hpp>

#include <core/system/PosixSched.hpp>

// typedefs (in case we need indirection on these for porting)
#include <sys/resource.h>
typedef pid_t PidType;
typedef rlim_t RLimitType;


namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace core {
namespace system {

namespace user {
   struct User;
}

// daemonize the process
core::Error daemonize();

// umask
// file creation masks and file modes
enum UMask
{
   OthersNoWriteMask,    // S_IWGRP | S_IWOTH
   OthersNoneMask        // S_IWGRP | S_IRWXO
};
void setUMask(UMask mask);


// resource limits
enum ResourceLimit
{
   MemoryLimit,
   FilesLimit,
   UserProcessesLimit,
   StackLimit,
   CoreLimit,
   MemlockLimit,
   CpuLimit,
   NiceLimit
};

bool resourceIsUnlimited(RLimitType limitValue);

core::Error getResourceLimit(ResourceLimit resourcelimit,
                             RLimitType* pSoft,
                             RLimitType* pHard);

core::Error setResourceLimit(ResourceLimit resourceLimit, RLimitType limit);

core::Error setResourceLimit(ResourceLimit resourceLimit,
                             RLimitType soft,
                             RLimitType hard);


struct SysInfo
{
   SysInfo() : cores(0), load1(0), load5(0), load15(0) {}
   unsigned cores;
   double load1;
   double load5;
   double load15;
};

core::Error systemInformation(SysInfo* pSysInfo);

core::Error pidof(const std::string& process, std::vector<PidType>* pPids);

struct ProcessInfo
{
   ProcessInfo() : pid(0) {}
   PidType pid;
   std::string username;
};

core::Error processInfo(const std::string& process,
                        std::vector<ProcessInfo>* pInfo);

std::ostream& operator<<(std::ostream& os, const ProcessInfo& info);

struct IpAddress
{
   std::string name;
   std::string addr;
};

core::Error ipAddresses(std::vector<IpAddress>* pAddresses);

// core dump restriction
core::Error restrictCoreDumps();
void printCoreDumpable(const std::string& context);

// launching child processes

enum StdStreamBehavior
{
   StdStreamClose = 0,
   StdStreamDevNull = 1,
   StdStreamInherit = 2
};

struct ProcessLimits
{
   ProcessLimits()
     : priority(0),
       memoryLimitBytes(0),
       stackLimitBytes(0),
       userProcessesLimit(0),
       cpuLimit(0),
       niceLimit(0),
       filesLimit(0)
   {
   }

   CpuAffinity cpuAffinity;
   int priority;
   RLimitType memoryLimitBytes;
   RLimitType stackLimitBytes;
   RLimitType userProcessesLimit;
   RLimitType cpuLimit;
   RLimitType niceLimit;
   RLimitType filesLimit;
};

void setProcessLimits(ProcessLimits limits);


struct ProcessConfig
{
   ProcessConfig()
      : stdStreamBehavior(StdStreamInherit)
   {
   }
   core::system::Options args;
   core::system::Options environment;
   std::string stdInput;
   StdStreamBehavior stdStreamBehavior;
   ProcessLimits limits;
};

core::Error waitForProcessExit(PidType processId);

// filter to call after the setuid has occurred (i.e. after
// the user's home directory has become visible)
typedef boost::function<void(const user::User&, ProcessConfig*)>
                                                   ProcessConfigFilter;

core::Error launchChildProcess(std::string path,
                               std::string runAsUser,
                               ProcessConfig config,
                               ProcessConfigFilter configFilter,
                               PidType* pProcessId ) ;

bool isUserNotFoundError(const core::Error& error);

core::Error userBelongsToGroup(const user::User& user,
                               const std::string& groupName,
                               bool* pBelongs);

// query priv state
bool realUserIsRoot();
bool effectiveUserIsRoot();

// privilege management (not thread safe, call from main thread at app startup
// or just after fork() prior to exec() for new processes)
core::Error temporarilyDropPriv(const std::string& newUsername);
core::Error permanentlyDropPriv(const std::string& newUsername);
core::Error restorePriv();

#ifdef __APPLE__
// Detect subprocesses via Mac-only BSD-ish APIs
bool hasSubprocessesMac(PidType pid);
#endif // __APPLE__

// Detect subprocesses via shelling out to pgrep, kinda expensive but used
// as last-resort on non-Mac Posix system without procfs.
bool hasSubprocessesViaPgrep(PidType pid);

// Detect subprocesses via procfs; returns false no subprocesses, true if
// subprocesses or unable to determine if there are subprocesses
bool hasSubprocessesViaProcFs(PidType pid, core::FilePath procFsPath);

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_POSIX_SYSTEM_HPP

