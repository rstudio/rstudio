/*
 * PosixSystem.hpp
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

#ifndef CORE_SYSTEM_POSIX_SYSTEM_HPP
#define CORE_SYSTEM_POSIX_SYSTEM_HPP

#include <boost/date_time.hpp>

#include <core/system/PosixSched.hpp>
#include <core/system/System.hpp>

#include <shared_core/system/PosixSystem.hpp>

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
    class User;

// daemonize the process
core::Error daemonize(const std::string& pidFile = std::string());

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

typedef boost::function<bool (const ProcessInfo&)> ProcessFilter;

// get process by process name, or all processes if process name is empty
// optionally allows supressing of errors - recommended in most cases
// as such errors are generally transient and benign
core::Error processInfo(const std::string& process,
                        std::vector<ProcessInfo>* pInfo,
                        bool suppressErrors = true,
                        ProcessFilter filter = ProcessFilter());

// get process info for the specific process specified by pid
core::Error processInfo(pid_t pid, ProcessInfo* pInfo);

bool isProcessRunning(pid_t pid);

std::ostream& operator<<(std::ostream& os, const ProcessInfo& info);


core::Error ipAddresses(std::vector<posix::IpAddress>* pAddresses, bool includeIPv6 = false);

// core dump restriction
core::Error restrictCoreDumps();
core::Error enableCoreDumps();
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
typedef boost::function<void(const User&, ProcessConfig*)>
                                                   ProcessConfigFilter;

core::Error launchChildProcess(std::string path,
                               std::string runAsUser,
                               ProcessConfig config,
                               ProcessConfigFilter configFilter,
                               PidType* pProcessId );

// runs a process, replacing the current process's image with that of the target
// note, this does not create a child process, but replaces the currently running one
Error runProcess(const std::string& path,
                 const std::string& runAsUser,
                 ProcessConfig& config,
                 ProcessConfigFilter configFilter);

// get this processes' child processes
Error getChildProcesses(std::vector<rstudio::core::system::ProcessInfo> *pOutProcesses);


// get the child processes of the specified process
Error getChildProcesses(pid_t pid,
                        std::vector<rstudio::core::system::ProcessInfo> *pOutProcesses);

// kill a process with a specific sign
Error killProcess(pid_t pid, int signal);

// no-signal version specified in System.hpp
// but on posix we can send any signal we want
// so we provide this function here
Error terminateChildProcesses(int signal);

// terminate child processes of the specified process
Error terminateChildProcesses(pid_t pid,
                              int signal);

bool isUserNotFoundError(const core::Error& error);

core::Error userBelongsToGroup(const User& user,
                               const std::string& groupName,
                               bool* pBelongs);

// query priv state
bool realUserIsRoot();

// privilege management - not thread safe
// call from main thread at app startup or just after fork() prior to exec() for new processes
// do not call after a fork in a multithreaded process, as this can cause deadlock!
core::Error temporarilyDropPriv(const std::string& newUsername);
core::Error permanentlyDropPriv(const std::string& newUsername);
core::Error restorePriv();

// restoreRoot should be used to set the effective ID back to root (0) before using
// the other privilege-modifying methods above - this is necessary because they maintain
// state of the original effective user, and in most cases that should be root
core::Error restoreRoot();

namespace signal_safe {

// signal-safe version of privilege drop
int permanentlyDropPriv(UidType newUid);

// signal-safe restore root priv
int restoreRoot();

} // namespace signal_safe

#ifdef __APPLE__
// Detect subprocesses via Mac-only BSD-ish APIs
std::vector<SubprocInfo> getSubprocessesMac(PidType pid);
#endif // __APPLE__

// Detect subprocesses via shelling out to pgrep, kinda expensive but used
// as last-resort on non-Mac Posix system without procfs.
std::vector<SubprocInfo> getSubprocessesViaPgrep(PidType pid);

// Detect subprocesses via procfs; returns false no subprocesses, true if
// subprocesses or unable to determine if there are subprocesses
#ifndef __APPLE__
std::vector<SubprocInfo> getSubprocessesViaProcFs(PidType pid);
#endif // !__APPLE__

#ifdef __APPLE__
// Detect current working directory via Mac-only APIs.
// Note that this will only work reliably for child processes.
FilePath currentWorkingDirMac(PidType pid);
#endif

#ifndef __APPLE__
// Determine current working directory of a given process by shelling out
// to lsof; used on systems without procfs.
FilePath currentWorkingDirViaLsof(PidType pid);

// Determine current working directory of a given process via procfs; returns
// empty FilePath if unable to determine.
FilePath currentWorkingDirViaProcFs(PidType pid);
#endif // !__APPLE__

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_POSIX_SYSTEM_HPP
