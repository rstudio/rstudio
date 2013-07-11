/*
 * PosixSystem.cpp
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

#include <core/system/PosixSystem.hpp>

#include <stdio.h>

#include <iostream>
#include <vector>

#include <boost/algorithm/string.hpp>

#include <signal.h>
#include <fcntl.h>
#include <syslog.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <unistd.h>
#include <pwd.h>
#include <grp.h>

#include <uuid/uuid.h>

#ifdef __APPLE__
#include <mach-o/dyld.h>
#endif

#include <boost/thread.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/range.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/split.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/FileLogWriter.hpp>
#include <core/Exec.hpp>
#include <core/SyslogLogWriter.hpp>
#include <core/StderrLogWriter.hpp>
#include <core/StringUtils.hpp>
#include <core/SafeConvert.hpp>

#include <core/system/ProcessArgs.hpp>
#include <core/system/Environment.hpp>
#include <core/system/PosixUser.hpp>

#include "config.h"

namespace core {
namespace system {

namespace {

Error ignoreSig(int signal)
{
   struct sigaction sa;
   ::memset(&sa, 0, sizeof sa);
   sa.sa_handler = SIG_IGN;
   int result = ::sigaction(signal, &sa, NULL);
   if (result != 0)
   {
      Error error = systemError(result, ERROR_LOCATION);
      error.addProperty("signal", signal);
      return error;
   }
   else
   {
      return Success();
   }
}


   
int signalForType(SignalType type)
{
   switch(type)
   {
      case SigInt:
         return SIGINT;
         
      case SigHup:
         return SIGHUP;

      case SigAbrt:
         return SIGABRT;
         
      case SigSegv:
         return SIGSEGV;
         
      case SigIll:
         return SIGILL;
         
      case SigUsr1:
         return SIGUSR1;
         
      case SigUsr2:
         return SIGUSR2;

      case SigPipe:
         return SIGPIPE;

      case SigChld:
         return SIGCHLD;
         
      default:
         return -1;
   }
}

} // anonymous namespace

Error realPath(const FilePath& filePath, FilePath* pRealPath)
{
   std::string path = string_utils::utf8ToSystem(filePath.absolutePath());

   char buffer[PATH_MAX*2];
   char* realPath = ::realpath(path.c_str(), buffer);
   if (realPath == NULL)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }

  *pRealPath = FilePath(string_utils::systemToUtf8(realPath));
  return Success();
}

Error realPath(const std::string& path, FilePath* pRealPath)
{
   char buffer[PATH_MAX*2];
   char* realPath = ::realpath(path.c_str(), buffer);
   if (realPath == NULL)
      return systemError(errno, ERROR_LOCATION);
  *pRealPath = FilePath(realPath);
   return Success();
}

void addToSystemPath(const FilePath& path, bool prepend)
{
   std::string systemPath = system::getenv("PATH");
   if (prepend)
      systemPath = path.absolutePath() + ":" + systemPath;
   else
      systemPath = systemPath + ":" + path.absolutePath();
   system::setenv("PATH", systemPath);
}


namespace {

// main log writer
LogWriter* s_pLogWriter = NULL;

// additional log writers
std::vector<boost::shared_ptr<LogWriter> > s_logWriters;

} // anonymous namespace
     
void initHook()
{
}

void initializeSystemLog(const std::string& programIdentity, int logLevel)
{
   if (s_pLogWriter)
      delete s_pLogWriter;

   s_pLogWriter = new SyslogLogWriter(programIdentity, logLevel);
}

void initializeStderrLog(const std::string& programIdentity, int logLevel)
{
   if (s_pLogWriter)
      delete s_pLogWriter;

   s_pLogWriter = new StderrLogWriter(programIdentity, logLevel);
}

void initializeLog(const std::string& programIdentity,
                   int logLevel,
                   const FilePath& logDir)
{
   if (s_pLogWriter)
      delete s_pLogWriter;

   s_pLogWriter = new FileLogWriter(programIdentity, logLevel, logDir);
}

void addLogWriter(boost::shared_ptr<core::LogWriter> pLogWriter)
{
   s_logWriters.push_back(pLogWriter);
}

void log(LogLevel logLevel, const std::string& message)
{
   if (s_pLogWriter)
      s_pLogWriter->log(logLevel, message);

   std::for_each(s_logWriters.begin(),
                 s_logWriters.end(),
                 boost::bind(&LogWriter::log, _1, logLevel, message));
}
   
Error ignoreTerminalSignals()
{
   ExecBlock ignoreBlock ;
   ignoreBlock.addFunctions()
      (boost::bind(ignoreSig, SIGHUP))
      (boost::bind(ignoreSig, SIGTSTP))
      (boost::bind(ignoreSig, SIGTTOU))
      (boost::bind(ignoreSig, SIGTTIN));
   return ignoreBlock.execute();
}
      
Error ignoreChildExits()
{
#if defined(HAVE_SA_NOCLDWAIT) // POSIX compliant
   struct sigaction reapchildren;
   ::memset( &reapchildren, 0, sizeof reapchildren );
   reapchildren.sa_flags = SA_NOCLDWAIT;
   int result = ::sigaction( SIGCHLD, &reapchildren, 0);
   if (result != 0)
      return systemError(result, ERROR_LOCATION);
#else // other systems
   if (::signal(SIGCHLD, SIG_IGN) == SIG_ERR)
      return systemError(errno, ERROR_LOCATION);
#endif // NO_CLDWAIT   
   
   return Success();
}
   
namespace {

void handleSIGCHLD(int sig)
{
   int stat;
   while (::waitpid (-1, &stat, WNOHANG) > 0)
   {
   }
}
   
}
   
Error reapChildren()
{
   // setup signal handler
   struct sigaction sa;
   ::memset(&sa, 0, sizeof sa);
   sa.sa_handler = handleSIGCHLD;
   sigemptyset(&sa.sa_mask);
   sa.sa_flags = SA_RESTART;
   
   // install it
   int result = ::sigaction(SIGCHLD, &sa, NULL);
   if (result != 0)
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}
   

   
// SignalBlocker -- block signals in a scope
//
// NOTE: boost has a portable signal_blocker class however we don't use it b/c:
//    1) it is in the detail namespace (which isn't a public api)
//    2) it doesn't check or fail on errors
//
// NOTE: blocking signals in threads and then handling them on either the
// main thread or a dedicated thread will only work properly on Linux kernel
// v2.6 or higher (because it supports the Native POSIX Thread Library and
// thus delivers signals to multi-threaded programs in a POSIX compliant way)

struct SignalBlocker::Impl
{
   Impl() : blocked(false) {}
   bool blocked;
   sigset_t oldMask;
   
   Error block(sigset_t* pBlockMask)
   {
      // install mask
      int result = ::pthread_sigmask(SIG_BLOCK, pBlockMask, &oldMask);
      if (result != 0)
         return systemError(result, ERROR_LOCATION);
      
      // set restore bit and return success
      blocked = true; 
      return Success();
   }
};
   
SignalBlocker::SignalBlocker()
   : pImpl_(new Impl())
{
}
   
   
Error SignalBlocker::block(SignalType signal)
{
   // get signal
   int sig = signalForType(signal);
   if (sig < 0)
      return systemError(EINVAL, ERROR_LOCATION);
   
   // create a mask for blocking the signal
   sigset_t blockMask;
   sigemptyset(&blockMask);
   sigaddset(&blockMask, sig);
   
   // block
   return pImpl_->block(&blockMask);
}

Error SignalBlocker::blockAll()
{
   // create mask to block all signals
   sigset_t blockMask;
   sigfillset(&blockMask);
   
   // block
   return pImpl_->block(&blockMask);
}
      
SignalBlocker::~SignalBlocker()
{
   try
   {
      if (pImpl_->blocked)
      {
         // restore old mask
         int result = ::pthread_sigmask(SIG_SETMASK, &(pImpl_->oldMask), NULL);
         if (result != 0)
            LOG_ERROR(systemError(result, ERROR_LOCATION));
      }
   }
   catch(...)
   {
   }
}
   
Error clearSignalMask()
{
   sigset_t blockNoneMask;
   sigemptyset(&blockNoneMask);
   int result = ::pthread_sigmask(SIG_SETMASK, &blockNoneMask, NULL);
   if (result != 0)
      return systemError(result, ERROR_LOCATION);
   else
      return Success();
}
   
Error handleSignal(SignalType signal, void (*handler)(int))
{
   int sig = signalForType(signal);
   if (sig < 0)
      return systemError(EINVAL, ERROR_LOCATION);
   
   ::signal(sig, handler);
   
   return Success();
}
   
core::Error ignoreSignal(SignalType signal)
{
   int sig = signalForType(signal);
   if (sig < 0)
      return systemError(EINVAL, ERROR_LOCATION);
   
   return ignoreSig(sig);
}   


Error useDefaultSignalHandler(SignalType signal)
{
   int sig = signalForType(signal);
   if (sig < 0)      return systemError(EINVAL, ERROR_LOCATION);
   
   struct sigaction sa;
   ::memset(&sa, 0, sizeof sa);
   sa.sa_handler = SIG_DFL;
   int result = ::sigaction(sig, &sa, NULL);
   if (result != 0)
   {
      Error error = systemError(result, ERROR_LOCATION);
      return error;
   }
   else
   {
      return Success();
   }
}

void sendSignalToSelf(SignalType signal)
{
   ::kill(::getpid(), signalForType(signal));
}

std::string username()
{
   return system::getenv("USER");
}

FilePath userHomePath(std::string envOverride)
{
   using namespace boost::algorithm;

   // use environment override if specified
   if (!envOverride.empty())
   {
      for (split_iterator<std::string::iterator> it =
           make_split_iterator(envOverride, first_finder("|", is_iequal()));
           it != split_iterator<std::string::iterator>();
           ++it)
      {
         std::string envHomePath = system::getenv(boost::copy_range<std::string>(*it));
         if (!envHomePath.empty())
         {
            FilePath userHomePath(envHomePath);
            if (userHomePath.exists())
               return userHomePath;
         }
      }
   }

   // otherwise use standard unix HOME
   return FilePath(system::getenv("HOME"));
}

FilePath userSettingsPath(const FilePath& userHomeDirectory,
                          const std::string& appName)
{
   std::string lower = appName;
   boost::to_lower(lower);

   FilePath path = userHomeDirectory.childPath("." + lower);
   Error error = path.ensureDirectory();
   if (error)
   {
      LOG_ERROR(error);
   }
   return path;
}

bool currentUserIsPrivilleged(unsigned int minimumUserId)
{
   return ::geteuid() < minimumUserId;
}

namespace {

// NOTE: this function is duplicated between here and core::system
// Did this to prevent the "system" interface from allowing Posix
// constructs with Win32 no-ops to creep in (since this is used on
// Posix for forking and has no purpose on Win32)

Error closeFileDescriptorsFrom(int fdStart)
{
   // There is no fully reliable and cross-platform way to do this, see:
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


} // anonymous namespace

Error closeAllFileDescriptors()
{
   return closeFileDescriptorsFrom(0);
}

Error closeNonStdFileDescriptors()
{
   return closeFileDescriptorsFrom(STDERR_FILENO+1);
}

void closeStdFileDescriptors()
{
   ::close(STDIN_FILENO);
   ::close(STDOUT_FILENO);
   ::close(STDERR_FILENO);
}


void attachStdFileDescriptorsToDevNull()
{
   int fd0 = ::open("/dev/null", O_RDWR);
   if (fd0 == -1)
      LOG_ERROR(systemError(errno, ERROR_LOCATION));

   int fd1 = ::dup(fd0);
   if (fd1 == -1)
       LOG_ERROR(systemError(errno, ERROR_LOCATION));

   int fd2 = ::dup(fd0);
   if (fd2 == -1)
      LOG_ERROR(systemError(errno, ERROR_LOCATION));
}

void setStandardStreamsToDevNull()
{
   core::system::closeStdFileDescriptors();
   core::system::attachStdFileDescriptorsToDevNull();
   std::ios::sync_with_stdio();
}


bool isHiddenFile(const FilePath& filePath) 
{
   std::string filename = filePath.filename() ;
   return (!filename.empty() && (filename[0] == '.')) ;
}  

bool isHiddenFile(const FileInfo& fileInfo)
{
   return isHiddenFile(FilePath(fileInfo.absolutePath()));
}

bool isReadOnly(const FilePath& filePath)
{
   if (::access(filePath.absolutePath().c_str(), W_OK) == -1)
   {
      if (errno == EACCES)
      {
         return true;
      }
      else
      {
         Error error = systemError(errno, ERROR_LOCATION);
         error.addProperty("path", filePath);
         LOG_ERROR(error);
         return false;
      }
   }
   else
   {
      return false;
   }
}
   
bool stderrIsTerminal()
{
   return ::isatty(STDERR_FILENO) == 1;
}
      
bool stdoutIsTerminal()
{
   return ::isatty(STDOUT_FILENO) == 1;
}

std::string generateUuid(bool includeDashes)
{
   // generaate the uuid and convert it to a strting
   uuid_t uuid ;
   ::uuid_generate_random(uuid);
   char uuidBuffer[40];
   ::uuid_unparse_lower(uuid, uuidBuffer);
   std::string uuidStr(uuidBuffer);

   // remove dashes if requested
   if (!includeDashes)
      boost::algorithm::replace_all(uuidStr, "-", "");

   return uuidStr;
}

PidType currentProcessId()
{
   return ::getpid();
}

Error executablePath(int argc, char * const argv[],
                     FilePath* pExecutablePath)
{
   std::string executablePath;

#if defined(__APPLE__)

   // get path to current executable
   uint32_t buffSize = 2048;
   std::vector<char> buffer(buffSize);
   if (_NSGetExecutablePath(&(buffer[0]), &buffSize) == -1)
   {
      buffer.resize(buffSize);
      _NSGetExecutablePath(&(buffer[0]), &buffSize);
   }

   // set it
   executablePath = std::string(&(buffer[0]));


#elif defined(HAVE_PROCSELF)

   executablePath = std::string("/proc/self/exe");

#else

   // Note that this technique will NOT work if the executable was located
   // via a search of the PATH. To make this fallback fully robust we would
   // need to also search the PATH for the exe name in argv[0]
   //

   // use argv[0] and initial path
   FilePath initialPath = FilePath::initialPath();
   executablePath = initialPath.complete(argv[0]).absolutePath();

#endif

   // return realPath of executable path
   return realPath(executablePath, pExecutablePath);
}

// installation path
Error installPath(const std::string& relativeToExecutable,
                  int argc, char * const argv[],
                  FilePath* pInstallPath)
{
   // get executable path
   FilePath executablePath;
   Error error = system::executablePath(argc, argv, &executablePath);
   if (error)
      return error;

   // fully resolve installation path relative to executable
   FilePath installPath = executablePath.parent().complete(relativeToExecutable);
   return realPath(installPath.absolutePath(), pInstallPath);
}

void fixupExecutablePath(FilePath* pExePath)
{
   // do nothing on posix
}

void abort()
{
	::abort();
}

Error terminateProcess(PidType pid)
{
   if (::kill(pid, SIGTERM))
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}


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
                        core::system::Options* pVars,
                        bool evenIfEmpty = false)
{
   std::string value = core::system::getenv(name);
   if (!value.empty() || evenIfEmpty)
      core::system::setenv(pVars, name, value);
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
                           safe_convert::numberToString(result));
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
      user::User user;
      error = user::currentUser(&user);
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
      core::system::Options env;
      copyEnvironmentVar("PATH", &env);
      copyEnvironmentVar("MANPATH", &env);
      copyEnvironmentVar("LANG", &env);
      core::system::setenv(&env, "USER", user.username);
      core::system::setenv(&env, "LOGNAME", user.username);
      core::system::setenv(&env, "HOME", user.homeDirectory);
      copyEnvironmentVar("SHELL", &env);

      // add custom environment vars (overriding as necessary)
      for (core::system::Options::const_iterator it = config.environment.begin();
           it != config.environment.end();
           ++it)
      {
         core::system::setenv(&env, it->first, it->second);
      }

      // format as ProcessArgs expects
      boost::format fmt("%1%=%2%");
      std::vector<std::string> envVars;
      for(core::system::Options::const_iterator it = env.begin();
           it != env.end();
           ++it)
      {
         envVars.push_back(boost::str(fmt % it->first % it->second));
      }

      // create environment args  (allocate on heap so memory stays around
      // after we exec (some systems including OSX seem to require this)
      core::system::ProcessArgs* pEnvironment = new core::system::ProcessArgs(
                                                                       envVars);

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

Error userBelongsToGroup(const user::User& user,
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
         result = EACCES;
      Error error = systemError(result, ERROR_LOCATION);
      error.addProperty("group-name", groupName);
      return error;
   }

   *pBelongs = false; // default to not found

   // see if the group id matches the user's group id
   if (user.groupId == grp.gr_gid)
   {
      *pBelongs = true;
   }
   // else scan the list of member names for this user
   else
   {
      char** pUsers = grp.gr_mem;
      while (*pUsers)
      {
         const char* pUser = *(pUsers++);
         if (user.username.compare(pUser) == 0)
         {
            *pBelongs = true;
            break;
         }
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
   user::User user;
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
   user::User user;
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
   user::User user;
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
   user::User user;
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
} // namespace core

