/*
 * PosixSystem.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <server/util/system/System.hpp>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <pwd.h>
#include <grp.h>

#include <vector>

#include <boost/lexical_cast.hpp>

#include <core/system/ProcessArgs.hpp>
#include <core/system/Environment.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <server/util/system/User.hpp>

#include "config.h"

using namespace core;

namespace server {
namespace util {
namespace system {

namespace {

// NOTE: this function is duplicated between here and core::system
// Did this to prevent the "system" interface from allowing Posix
// constructs with Win32 no-ops to creep in (since this is used on
// Posix for forking and has no purpose on Win32)

Error closeFileDescriptorsFrom(int fdStart)
{
   // There is no fully reliable and cross-platform way to do this
   //
   // Various potential mechanisms include:
   //
   //  - closefrom
   //  - fcntl(0, F_MAXFD)
   //  - sysconf(_SC_OPEN_MAX)
   //  - getrlimit(RLIMIT_NOFILE, &rl)
   //  - gettdtablesize
   //  - read from /proc/self/fd, /proc/<pid>/fd, or /dev/fd
   //
   // Note that the above functions may return either -1 or MAX_INT, in
   // which case substituting/truncating to an appropriate number (1024?)
   // is still required

   // get limit
   struct rlimit rl;
   if (::getrlimit(RLIMIT_NOFILE, &rl) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (rl.rlim_max == RLIM_INFINITY)
      rl.rlim_max = 1024; // default on linux

   // close file descriptors
   for (int i=fdStart; i< (int)rl.rlim_max; i++)
   {
      if (::close(i) < 0 && errno != EBADF)
         return systemError(errno, ERROR_LOCATION);
   }

   return Success();
}

Error closeNonStdFileDescriptors()
{
   return closeFileDescriptorsFrom(STDERR_FILENO+1);
}

const int kNotFoundError = EACCES;

} // anonymouys namespace


Error daemonize()
{
   // fork
   pid_t pid = ::fork();
   if (pid < 0)
      return systemError(errno, ERROR_LOCATION); // fork error
   else if (pid > 0)
      ::exit(EXIT_SUCCESS); // parent exits

   // obtain a new process group
   ::setsid();

   // close all file descriptors
   Error error = closeFileDescriptorsFrom(0);
   if (error)
      return error;

   // attach file descriptors 0, 1, and 2 to /dev/null
   core::system::attachStdFileDescriptorsToDevNull();

   // note: ignoring of terminal signals are handled by an optional
   // separate call (ignoreTerminalSignals)

   return Success();
}

void setUMask(UMask mask)
{
   switch (mask)
   {
      case OthersNoWriteMask:
         ::umask(S_IWGRP | S_IWOTH);
         break;

      case OthersNoneMask:
         ::umask(S_IWGRP | S_IRWXO);
         break;

      default:
         BOOST_ASSERT(false);
         break;
   }
}

namespace {

Error osResourceLimit(ResourceLimit limit, int* pLimit)
{
   switch(limit)
   {
      case MemoryLimit:
         *pLimit = RLIMIT_AS;
         break;
      case FilesLimit:
         *pLimit = RLIMIT_NOFILE;
         break;
      case UserProcessesLimit:
         *pLimit = RLIMIT_NPROC;
         break;
      case StackLimit:
         *pLimit = RLIMIT_STACK;
         break;
      default:
         *pLimit = -1;
         break;
   }

   if (*pLimit == -1)
      return systemError(EINVAL, ERROR_LOCATION);
   else
      return Success();
}

} // anonumous namespace

bool resourceIsUnlimited(RLimitType resourceValue)
{
   return resourceValue == RLIM_INFINITY;
}

Error getResourceLimit(ResourceLimit resourceLimit,
                       RLimitType* pSoft,
                       RLimitType* pHard)
{
   // determine resource
   int resource;
   Error error = osResourceLimit(resourceLimit, &resource);
   if (error)
      return error;

   // get limits
   struct rlimit rl;
   if (::getrlimit(resource, &rl) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set out params and return success
   *pSoft = rl.rlim_cur;
   *pHard = rl.rlim_max;
   return Success();
}

Error setResourceLimit(ResourceLimit resourceLimit, RLimitType limit)
{
   return setResourceLimit(resourceLimit, limit, limit);
}

Error setResourceLimit(ResourceLimit resourceLimit,
                       RLimitType soft,
                       RLimitType hard)
{
   // determine resource
   int resource;
   Error error = osResourceLimit(resourceLimit, &resource);
   if (error)
      return error;

   // set limit
   struct rlimit rl;
   rl.rlim_cur = soft;
   rl.rlim_max = hard;
   if (::setrlimit(resource, &rl) == -1)
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}


namespace {

Error setProcessLimits(RLimitType memoryLimitBytes,
                       RLimitType stackLimitBytes,
                       RLimitType userProcessesLimit)
{
   // set memory limit
   Error memoryError;
   if (memoryLimitBytes != 0)
      memoryError = setResourceLimit(MemoryLimit, memoryLimitBytes);

   Error stackError;
   if (stackLimitBytes != 0)
      stackError = setResourceLimit(StackLimit, stackLimitBytes);

   // user processes limit
   Error processesError;
   if (userProcessesLimit != 0)
      processesError = setResourceLimit(UserProcessesLimit, userProcessesLimit);

   // if both had errors then log one and return the other
   if (memoryError)
   {
      if (stackError)
         LOG_ERROR(stackError);

      if (processesError)
         LOG_ERROR(processesError);

      return memoryError;
   }
   else if (stackError)
   {
      if (processesError)
         LOG_ERROR(processesError);

      return stackError;
   }
   else if (processesError)
   {
      return processesError;
   }
   else
   {
      return Success();
   }
}

void copyEnvironmentVar(const std::string& name,
                        std::vector<std::string>* pVars,
                        bool evenIfEmpty = false)
{
   std::string value = core::system::getenv(name);
   if (!value.empty() || evenIfEmpty)
      pVars->push_back(name + "=" + value);
}

}


Error waitForProcessExit(PidType processId)
{
   while (true)
   {
      // call waitpid
      int stat;
      int result = ::waitpid (processId, &stat, 0);
      if (result == processId)
      {
         // process "status changed" (i.e. exited, core dumped, terminated
         // by a signal, or stopped). in all cases we stop waiting
         return Success();
      }
      else if (result == 0)
      {
         // no status change, keep waiting...
         continue;
      }
      else if (result < 0)
      {
         if (errno == EINTR)
         {
            // system call interrupted, keep waiting...
            continue;
         }
         else if (errno == ECHILD)
         {
            // the child was already reaped (probably by a global handler)
            return Success();
         }
         else
         {
            // some other unexpected error
            return systemError(errno, ERROR_LOCATION);
         }
      }
      else // a totally unexpected return from waitpid
      {
         Error error = systemError(
                           boost::system::errc::state_not_recoverable,
                           ERROR_LOCATION);
         error.addProperty("result",
                           boost::lexical_cast<std::string>(result));
         return error;
      }
   }

   // keep compiler happy (we can't reach this code)
   return Success();
}

Error launchChildProcess(std::string path,
                         std::string runAsUser,
                         ProcessConfig config,
                         PidType* pProcessId)
{
   pid_t pid = ::fork();

   // error
   if (pid < 0)
   {
      Error error = systemError(errno, ERROR_LOCATION) ;
      error.addProperty("commmand", path) ;
      return error ;
   }

   // child
   else if (pid == 0)
   {
      // obtain a new process group (using our own process id) so our
      // lifetime isn't tied to our parent's lifetime
      if (::setpgid(0,0) == -1)
      {
         Error error = systemError(errno, ERROR_LOCATION);
         LOG_ERROR(error);
         ::exit(EXIT_FAILURE);
      }

      // change user here if requested
      if (!runAsUser.empty())
      {
         // restore root
         Error error = restorePriv();
         if (error)
         {
            LOG_ERROR(error);
            ::exit(EXIT_FAILURE);
         }

         // set limits
         error = setProcessLimits(config.memoryLimitBytes,
                                  config.stackLimitBytes,
                                  config.userProcessesLimit);
         if (error)
            LOG_ERROR(error);

         // switch user
         error = permanentlyDropPriv(runAsUser);
         if (error)
         {
            LOG_ERROR(error);
            ::exit(EXIT_FAILURE);
         }
      }

      // clear the signal mask so the child process can handle whatever
      // signals it wishes to
      Error error = core::system::clearSignalMask();
      if (error)
      {
         LOG_ERROR(error);
         ::exit(EXIT_FAILURE);
      }

      // get current user (before closing file handles since 
      // we might be using a PAM module that has open FDs...)
      util::system::user::User user;
      error = util::system::user::currentUser(&user);
      if (error)
      {
         LOG_ERROR(error);
         ::exit(EXIT_FAILURE);
      }

      // close all open file descriptors other than std streams
      error = closeNonStdFileDescriptors();
      if (error)
      {
         LOG_ERROR(error);
         ::exit(EXIT_FAILURE);
      }

      // handle std streams
      switch(config.stdStreamBehavior)
      {
         case StdStreamClose:
            core::system::closeStdFileDescriptors();
            break;

         case StdStreamDevNull:
            core::system::closeStdFileDescriptors();
            core::system::attachStdFileDescriptorsToDevNull();
            break;

         case StdStreamInherit:
         default:
            // do nothing to inherit the streams
            break;
      }

      // setup environment
      std::vector<std::string> env;
      copyEnvironmentVar("PATH", &env);
      copyEnvironmentVar("MANPATH", &env);
      copyEnvironmentVar("LANG", &env);
      env.push_back("USER=" + user.username);
      env.push_back("LOGNAME=" + user.username);
      env.push_back("HOME=" + user.homeDirectory);
      copyEnvironmentVar("SHELL", &env);

      // add custom environment vars
      for (core::system::Options::const_iterator it = config.environment.begin();
           it != config.environment.end();
           ++it)
      {
         env.push_back(it->first + "=" + it->second);
      }

      // create environment args  (allocate on heap so memory stays around
      // after we exec (some systems including OSX seem to require this)
      core::system::ProcessArgs* pEnvironment = new core::system::ProcessArgs(
                                                                         env);

      // build process args
      std::vector<std::string> argVector;
      argVector.push_back(path);
      for (core::system::Options::const_iterator it = config.args.begin();
           it != config.args.end();
           ++it)
      {
         argVector.push_back(it->first);
         argVector.push_back(it->second);
      }

      // allocate ProcessArgs on heap so memory stays around after we exec
      // (some systems including OSX seem to require this)
      core::system::ProcessArgs* pProcessArgs = new core::system::ProcessArgs(
                                                                  argVector);

      // execute child
      ::execve(path.c_str(), pProcessArgs->args(), pEnvironment->args());

      // in the normal case control should never return from execv (it starts
      // anew at main of the process pointed to by path). therefore, if we get
      // here then there was an error
      error = systemError(errno, ERROR_LOCATION);
      error.addProperty("child-path", path);
      LOG_ERROR(error) ;
      ::exit(EXIT_FAILURE) ;
   }

   // parent
   if (pProcessId)
      *pProcessId = pid ;

   return Success() ;
}



bool isUserNotFoundError(const Error& error)
{
   return error.code() == boost::system::errc::permission_denied;
}

Error userBelongsToGroup(const std::string& username,
                         const std::string& groupName,
                         bool* pBelongs)
{
   struct group grp;
   struct group* ptrGrp = &grp;
   struct group* tempPtrGrp ;

   // get the estimated size of the groups data
   int buffSize = ::sysconf(_SC_GETGR_R_SIZE_MAX);
   if (buffSize == -1)
      buffSize = 4096; // some systems return -1, be conservative!

   // call until we pass a buffer large enough for the data
   std::vector<char> buffer;
   int result = 0;
   do
   {
      // double the size of the suggested/previous buffer
      buffSize *= 2;
      buffer.reserve(buffSize);

      // attempt the read
      result = ::getgrnam_r(groupName.c_str(),
                            ptrGrp,
                            &(buffer[0]),
                            buffSize,
                            &tempPtrGrp);

   } while (result == ERANGE);

   // check for no group data
   if (tempPtrGrp == NULL)
   {
      if (result == 0) // will happen if group is simply not found
         result = kNotFoundError;
      Error error = systemError(result, ERROR_LOCATION);
      error.addProperty("group-name", groupName);
      return error;
   }

   // scan the list of member names for this user
   *pBelongs = false; // default to not found
   char** pUsers = grp.gr_mem;
   while (*pUsers)
   {
      const char* pUser = *(pUsers++);
      if (username.compare(pUser) == 0)
      {
         *pBelongs = true;
         break;
      }
   }

   return Success();
}



/////////////////////////////////////////////////////////////////////
//
// setuid privilege manipulation
//

bool realUserIsRoot()
{
   return ::getuid() == 0;
}

bool effectiveUserIsRoot()
{
   return ::geteuid() == 0;
}

// privilege manipulation for systems that support setresuid/getresuid
#if defined(HAVE_SETRESUID)

Error temporarilyDropPriv(const std::string& newUsername)
{
   // clear error state
   errno = 0;

   // get user info
   util::system::user::User user;
   Error error = userFromUsername(newUsername, &user);
   if (error)
      return error;

   // init supplemental group list
   // NOTE: if porting to CYGWIN may need to call getgroups/setgroups
   // after initgroups -- more research required to confirm
   if (::initgroups(user.username.c_str(), user.groupId) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group and verify
   if (::setresgid(-1, user.groupId, ::getegid()) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (::getegid() != user.groupId)
      return systemError(EACCES, ERROR_LOCATION);

   // set user and verify
   if (::setresuid(-1, user.userId, ::geteuid()) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (::geteuid() != user.userId)
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}

Error permanentlyDropPriv(const std::string& newUsername)
{
   // clear error state
   errno = 0;

   // get user info
   util::system::user::User user;
   Error error = userFromUsername(newUsername, &user);
   if (error)
      return error;

   // supplemental group list
   if (::initgroups(user.username.c_str(), user.groupId) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group
   if (::setresgid(user.groupId, user.groupId, user.groupId) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   gid_t rgid, egid, sgid;
   if (::getresgid(&rgid, &egid, &sgid) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (rgid != user.groupId || egid != user.groupId || sgid != user.groupId)
      return systemError(EACCES, ERROR_LOCATION);

   // set user
   if (::setresuid(user.userId, user.userId, user.userId) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   uid_t ruid, euid, suid;
   if (::getresuid(&ruid, &euid, &suid) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (ruid != user.userId || euid != user.userId || suid != user.userId)
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}

Error restorePriv()
{
   // reset error state
   errno = 0;

   // set user
   uid_t ruid, euid, suid;
   if (::getresuid(&ruid, &euid, &suid) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (::setresuid(-1, suid, -1) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::geteuid() != suid)
      return systemError(EACCES, ERROR_LOCATION);

   // get saved user info to use in group calls
   struct passwd* pPrivPasswd = ::getpwuid(suid);
   if (pPrivPasswd == NULL)
      return systemError(errno, ERROR_LOCATION);

   // supplemental groups
   if (::initgroups(pPrivPasswd->pw_name, pPrivPasswd->pw_gid) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group
   gid_t rgid, egid, sgid;
   if (::getresgid(&rgid, &egid, &sgid) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (::setresgid(-1, sgid, -1) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::getegid() != sgid)
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}

// privilege manipulation for systems that don't support setresuid/getresuid
#else

namespace {
   uid_t s_privUid ;
}

Error temporarilyDropPriv(const std::string& newUsername)
{
   // clear error state
   errno = 0;

   // get user info
   util::system::user::User user;
   Error error = userFromUsername(newUsername, &user);
   if (error)
      return error;

   // init supplemental group list
   if (::initgroups(user.username.c_str(), user.groupId) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group

   // save old EGUID
   gid_t oldEGUID = ::getegid();

   // copy EGUID to SGID
   if (::setregid(::getgid(), oldEGUID) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set new EGID
   if (::setegid(user.groupId) < 0)
      return systemError(errno, ERROR_LOCATION);

   // verify
   if (::getegid() != user.groupId)
      return systemError(EACCES, ERROR_LOCATION);


   // set user

   // save old EUID
   uid_t oldEUID = ::geteuid();

   // copy EUID to SUID
   if (::setreuid(::getuid(), oldEUID) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set new EUID
   if (::seteuid(user.userId) < 0)
      return systemError(errno, ERROR_LOCATION);

   // verify
   if (::geteuid() != user.userId)
      return systemError(EACCES, ERROR_LOCATION);

   // save privilleged user id
   s_privUid = oldEUID;

   // success
   return Success();
}


Error permanentlyDropPriv(const std::string& newUsername)
{
   // clear error state
   errno = 0;

   // get user info
   util::system::user::User user;
   Error error = userFromUsername(newUsername, &user);
   if (error)
      return error;

   // supplemental group list
   if (::initgroups(user.username.c_str(), user.groupId) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group
   if (::setregid(user.groupId, user.groupId) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::getgid() != user.groupId || ::getegid() != user.groupId)
      return systemError(EACCES, ERROR_LOCATION);

   // set user
   if (::setreuid(user.userId, user.userId) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::getuid() != user.userId || ::geteuid() != user.userId)
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}


Error restorePriv()
{
   // reset error state
   errno = 0;

   // set effective user to saved privid
   if (::seteuid(s_privUid) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::geteuid() != s_privUid)
      return systemError(EACCES, ERROR_LOCATION);

   // get user info to use in group calls
   struct passwd* pPrivPasswd = ::getpwuid(s_privUid);
   if (pPrivPasswd == NULL)
      return systemError(errno, ERROR_LOCATION);

   // supplemental groups
   if (::initgroups(pPrivPasswd->pw_name, pPrivPasswd->pw_gid) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set effective group
   if (::setegid(pPrivPasswd->pw_gid) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::getegid() != pPrivPasswd->pw_gid)
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}

#endif

} // namespace system
} // namespace util
} // namespace server

