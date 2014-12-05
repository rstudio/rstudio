/*
 * PosixSystem.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


namespace rscore {
   class Error;
}

namespace rscore {
namespace system {

namespace user {
   struct User;
}

// daemonize the process
rscore::Error daemonize();

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

rscore::Error getResourceLimit(ResourceLimit resourcelimit,
                             RLimitType* pSoft,
                             RLimitType* pHard);

rscore::Error setResourceLimit(ResourceLimit resourceLimit, RLimitType limit);

rscore::Error setResourceLimit(ResourceLimit resourceLimit,
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

rscore::Error systemInformation(SysInfo* pSysInfo);

rscore::Error pidof(const std::string& process, std::vector<PidType>* pPids);

struct ProcessInfo
{
   ProcessInfo() : pid(0) {}
   PidType pid;
   std::string username;
};

rscore::Error processInfo(const std::string& process,
                        std::vector<ProcessInfo>* pInfo);

std::ostream& operator<<(std::ostream& os, const ProcessInfo& info);

struct IpAddress
{
   std::string name;
   std::string addr;
};

rscore::Error ipAddresses(std::vector<IpAddress>* pAddresses);

// core dump restriction
rscore::Error restrictCoreDumps();
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
   rscore::system::Options args;
   rscore::system::Options environment;
   std::string stdInput;
   StdStreamBehavior stdStreamBehavior;
   ProcessLimits limits;
};

rscore::Error waitForProcessExit(PidType processId);

rscore::Error launchChildProcess(std::string path,
                               std::string runAsUser,
                               ProcessConfig config,
                               PidType* pProcessId ) ;

bool isUserNotFoundError(const rscore::Error& error);

rscore::Error userBelongsToGroup(const user::User& user,
                               const std::string& groupName,
                               bool* pBelongs);

// query priv state
bool realUserIsRoot();
bool effectiveUserIsRoot();

// privillege management (not thread safe, call from main thread at app startup
// or just after fork() prior to exec() for new processes)
rscore::Error temporarilyDropPriv(const std::string& newUsername);
rscore::Error permanentlyDropPriv(const std::string& newUsername);
rscore::Error restorePriv();


} // namespace system
} // namespace rscore

#endif // CORE_SYSTEM_POSIX_SYSTEM_HPP

