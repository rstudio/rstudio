/*
 * PosixSystem.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/System.hpp>

#include <stdio.h>

#include <iostream>
#include <boost/algorithm/string.hpp>

#include <signal.h>
#include <fcntl.h>
#include <syslog.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <unistd.h>

#include <uuid/uuid.h>

#ifdef __APPLE__
#include <mach-o/dyld.h>
#endif

#include <boost/thread.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string/replace.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileLogWriter.hpp>
#include <core/Exec.hpp>
#include <core/SyslogLogWriter.hpp>

#include <core/system/ProcessArgs.hpp>

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
         
      default:
         return -1;
   }
}

} // anonymous namespace

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
LogWriter* s_pLogWriter = NULL;
} // anonymous namespace
     
void initializeSystemLog(const std::string& programIdentity, int logLevel)
{
   if (s_pLogWriter)
      delete s_pLogWriter;

   s_pLogWriter = new SyslogLogWriter(programIdentity, logLevel);
}

void initializeLog(const std::string& programIdentity,
                   int logLevel,
                   const FilePath& logDir)
{
   if (s_pLogWriter)
      delete s_pLogWriter;

   s_pLogWriter = new FileLogWriter(programIdentity, logLevel, logDir);
}

void log(LogLevel logLevel, const std::string& message)
{
   if (s_pLogWriter)
      s_pLogWriter->log(logLevel, message);
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
   
std::string getenv(const std::string& name)
{
   char * value = ::getenv(name.c_str());
   if (value)
      return std::string(value);
   else
      return std::string();
}
   
void setenv(const std::string& name, const std::string& value)
{
   ::setenv(name.c_str(), value.c_str(), 1);
}

void unsetenv(const std::string& name)
{
	::unsetenv(name.c_str());
}

std::string username()
{
   return system::getenv("USER");
}

FilePath userHomePath(const std::string& envOverride)
{
   // use environment override if specified
   if (!envOverride.empty())
   {
      std::string envHomePath = system::getenv(envOverride);
      if (!envHomePath.empty())
      {
         FilePath userHomePath(envHomePath);
         if (userHomePath.exists())
            return userHomePath;
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

Error closeNonStdFileDescriptors()
{
   return closeFileDescriptorsFrom(STDERR_FILENO+1);
}

} // anonymous namespace


Error executeInterruptableChildProcess(
           std::string path,
           Options args,
           int checkContinueIntervalMs,
           const boost::function<bool()>& checkContinueFunction)
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

      // clear the signal mask so the child process can handle whatever
      // signals it wishes to
      Error error = clearSignalMask();
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
      
      // build process args
      std::vector<std::string> argVector;
      argVector.push_back(path);
      for (Options::const_iterator it = args.begin();
           it != args.end();
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
      ::execv(path.c_str(), pProcessArgs->args()) ;
      
      // in the normal case control should never return from execv (it starts 
      // anew at main of the process pointed to by path). therefore, if we get
      // here then there was an error
      LOG_ERROR(systemError(errno, ERROR_LOCATION)) ; 
      ::exit(EXIT_FAILURE) ;
   } 

   else // parent
   {
      while (true)
      {
         // sleep waiting for the next check
         using boost::posix_time::milliseconds;
         boost::this_thread::sleep(milliseconds(checkContinueIntervalMs));

         // check whether we should continue waiting
         if (!checkContinueFunction())
         {
            // kill the child process group
            int result = ::kill(-pid, SIGTERM);
            if (result == -1 && errno != ECHILD && errno != ESRCH)
               return systemError(errno, ERROR_LOCATION);
            else
               return Success();
         }

         // call waitpid
         int stat;
         int result = ::waitpid (pid, &stat, WNOHANG);
         if (result == pid)
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
   }
   
   return Success() ;
}   


Error runCommand(const std::string& command)
{
   int rc = ::system(command.c_str());
   if (rc == -1 && errno != ECHILD)
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}

Error captureCommand(const std::string& command, std::string* pOutput)
{
   // start process
   FILE* fp = ::popen(command.c_str(), "r");
   if (fp == NULL)
      return systemError(errno, ERROR_LOCATION);

   // collect output
   const int kBuffSize = 1024;
   char buffer[kBuffSize];
   while (::fgets(buffer, kBuffSize, fp) != NULL)
      *pOutput += buffer;

   // check if an error terminated our output
   Error error ;
   if (::ferror(fp))
      error = systemError(boost::system::errc::io_error, ERROR_LOCATION);

   // close file
   if (::pclose(fp) == -1)
   {
      // ECHILD expected if process was reaped elsewhere by wait or waitpid
      if (errno != ECHILD)
      {
         // log existing error before overwriting it
         if (error)
            LOG_ERROR(error);

         error = systemError(errno, ERROR_LOCATION);
      }
   }

   // return status
   return error;
}

bool isHiddenFile(const FilePath& filePath) 
{
   std::string filename = filePath.filename() ;
   return (!filename.empty() && (filename[0] == '.')) ;
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

namespace {

Error installPath(const std::string& executablePath,
                  const std::string& relativeToExecutable,
                  FilePath* pInstallPath)
 {
    FilePath exeFilePath;
    Error error = realPath(executablePath, &exeFilePath);
    if (error)
       return error;

    // fully resolve installation path relative to executable
    FilePath installPath = exeFilePath.parent().complete(relativeToExecutable);
    return realPath(installPath.absolutePath(), pInstallPath);
 }

} // anonymous namespace

// installation path
Error installPath(const std::string& relativeToExecutable,
                  int argc, char * const argv[],
                  FilePath* pInstallPath)
{
#if defined(__APPLE__)

   // get path to current executable
   uint32_t buffSize = 2048;
   std::vector<char> buffer(buffSize);
   if (_NSGetExecutablePath(&(buffer[0]), &buffSize) == -1)
   {
      buffer.resize(buffSize);
      _NSGetExecutablePath(&(buffer[0]), &buffSize);
   }

   // calculate install path
   return installPath(&(buffer[0]), relativeToExecutable, pInstallPath);

#elif defined(HAVE_PROCSELF)

   // calcluate install path
   return installPath("/proc/self/exe", relativeToExecutable, pInstallPath);

#else

   // Note that this technique will NOT work if the executable was located
   // via a search of the PATH. To make this fallback fully robust we would
   // need to also search the PATH for the exe name in argv[0]
   //

   // use argv[0] and initial path
   FilePath initialPath = FilePath::initialPath();
   std::string exePath = initialPath.complete(argv[0]).absolutePath();

   // calculate install path
   return installPath(exePath, relativeToExecutable, pInstallPath);

#endif


}

void fixupExecutablePath(FilePath* pExePath)
{
   // do nothing on posix
}

void abort()
{
	::abort();
}


} // namespace system
} // namespace core

