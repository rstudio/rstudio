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

// typedefs (in case we need indirection on these for porting)
#include <sys/resource.h>
typedef pid_t PidType;
typedef rlim_t RLimitType;


namespace core {
   class Error;
}

namespace core {
namespace system {

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
   StackLimit
};

bool resourceIsUnlimited(RLimitType limitValue);

core::Error getResourceLimit(ResourceLimit resourcelimit,
                             RLimitType* pSoft,
                             RLimitType* pHard);

core::Error setResourceLimit(ResourceLimit resourceLimit, RLimitType limit);

core::Error setResourceLimit(ResourceLimit resourceLimit,
                             RLimitType soft,
                             RLimitType hard);

// launching child processes

enum StdStreamBehavior
{
   StdStreamClose = 0,
   StdStreamDevNull = 1,
   StdStreamInherit = 2
};

struct ProcessConfig
{
   ProcessConfig()
      : stdStreamBehavior(StdStreamInherit),
        memoryLimitBytes(0),
        stackLimitBytes(0),
        userProcessesLimit(0)
   {
   }

   core::system::Options args;
   core::system::Options environment;
   StdStreamBehavior stdStreamBehavior;
   RLimitType memoryLimitBytes;
   RLimitType stackLimitBytes;
   RLimitType userProcessesLimit;
};

core::Error waitForProcessExit(PidType processId);

core::Error launchChildProcess(std::string path,
                               std::string runAsUser,
                               ProcessConfig config,
                               PidType* pProcessId ) ;

bool isUserNotFoundError(const core::Error& error);

core::Error userBelongsToGroup(const std::string& username,
                               const std::string& groupName,
                               bool* pBelongs);

// query priv state
bool realUserIsRoot();
bool effectiveUserIsRoot();

// privillege management (not thread safe, call from main thread at app startup
// or just after fork() prior to exec() for new processes)
core::Error temporarilyDropPriv(const std::string& newUsername);
core::Error permanentlyDropPriv(const std::string& newUsername);
core::Error restorePriv();


} // namespace system
} // namespace core

#endif // CORE_SYSTEM_POSIX_SYSTEM_HPP

