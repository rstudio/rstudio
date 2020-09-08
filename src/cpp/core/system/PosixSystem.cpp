/*
 * PosixSystem.cpp
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

#include <core/system/PosixSystem.hpp>

#include <stdio.h>

#include <iostream>
#include <string>
#include <vector>

#include <boost/algorithm/string.hpp>
#include <boost/range/as_array.hpp>
#include <boost/bind.hpp>

#include <signal.h>
#include <fcntl.h>
#include <syslog.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <unistd.h>
#include <pwd.h>
#include <grp.h>
#include <sys/types.h>
#include <sys/socket.h>

#include <uuid/uuid.h>

#include <shared_core/system/PosixSystem.hpp>

#ifdef __APPLE__
#include <mach-o/dyld.h>
#include <sys/proc_info.h>
#include <libproc.h>
#include <gsl/gsl>
#endif

#ifndef __APPLE__
#include <sys/prctl.h>
#include <sys/sysinfo.h>
#include <linux/kernel.h>
#include <dirent.h>
#endif

#include <boost/thread.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/range.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/split.hpp>

#include <core/RegexUtils.hpp>
#include <core/Algorithm.hpp>
#include <core/DateTime.hpp>
#include <core/FileInfo.hpp>

#include <core/FileSerializer.hpp>
#include <core/Exec.hpp>
#include <core/LogOptions.hpp>
#include <core/StringUtils.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>
#include <core/Thread.hpp>

#include <core/system/ProcessArgs.hpp>
#include <core/system/Environment.hpp>
#include <core/system/FileScanner.hpp>
#include <core/system/PosixUser.hpp>
#include <core/system/PosixGroup.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>


#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/system/User.hpp>

#include "config.h"

namespace rstudio {
namespace core {
namespace system {

namespace {
   
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

      case SigTerm:
         return SIGTERM;
         
      default:
         return -1;
   }
}

} // anonymous namespace

Error realPath(const FilePath& filePath, FilePath* pRealPath)
{
   std::string path = string_utils::utf8ToSystem(filePath.getAbsolutePath());

   char buffer[PATH_MAX*2];
   char* realPath = ::realpath(path.c_str(), buffer);
   if (realPath == nullptr)
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
   if (realPath == nullptr)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", path);
      return error;
   }
  *pRealPath = FilePath(realPath);
   return Success();
}
     
void initHook()
{
}

// statics defined in System.cpp
extern boost::shared_ptr<log::LogOptions> s_logOptions;
extern boost::recursive_mutex s_loggingMutex;
extern std::string s_programIdentity;

Error initializeSystemLog(const std::string& programIdentity,
                          log::LogLevel logLevel,
                          bool enableConfigReload)
{
   RECURSIVE_LOCK_MUTEX(s_loggingMutex)
   {
      // create default syslog logger options
      log::SysLogOptions options;
      s_logOptions.reset(new log::LogOptions(programIdentity, logLevel, log::LoggerType::kSysLog, options));
      s_programIdentity = programIdentity;

      Error error = initLog();
      if (error)
         return error;
   }
   END_LOCK_MUTEX

   if (enableConfigReload)
      initializeLogConfigReload();

   return Success();
}

namespace {

void logConfigReloadThreadFunc(sigset_t waitMask)
{
   for(;;)
   {
      // wait for SIGHUP
      int sig = 0;
      int result = ::sigwait(&waitMask, &sig);
      if (result != 0)
         return;

      if (sig == SIGHUP)
      {
         LOG_INFO_MESSAGE("Reloading logging configuration...");

         Error error = reinitLog();
         if (error)
         {
            LOG_ERROR(error);
            LOG_ERROR_MESSAGE("Failed to reload logging configuration");
         }
         else
         {
            LOG_INFO_MESSAGE("Successfully reloaded logging configuration");
         }
      }
   }
}

} // anonymous namespace

void initializeLogConfigReload()
{
   // block the SIGHUP signal
   sigset_t waitMask;
   sigemptyset(&waitMask);
   sigaddset(&waitMask, SIGHUP);

   int result = ::pthread_sigmask(SIG_BLOCK, &waitMask, nullptr);
   if (result != 0)
      return;

   // start a thread to handle the SIGHUP signal
   boost::thread thread(boost::bind(logConfigReloadThreadFunc, waitMask));
}
   
Error ignoreTerminalSignals()
{
   ExecBlock ignoreBlock;
   ignoreBlock.addFunctions()
      (boost::bind(posix::ignoreSignal, SIGHUP))
      (boost::bind(posix::ignoreSignal, SIGTSTP))
      (boost::bind(posix::ignoreSignal, SIGTTOU))
      (boost::bind(posix::ignoreSignal, SIGTTIN));
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
   int result = ::sigaction(SIGCHLD, &sa, nullptr);
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
         int result = ::pthread_sigmask(SIG_SETMASK, &(pImpl_->oldMask), nullptr);
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
   int result = signal_safe::clearSignalMask();
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
   
   return posix::ignoreSignal(sig);
}   


Error useDefaultSignalHandler(SignalType signal)
{
   int sig = signalForType(signal);
   if (sig < 0)      return systemError(EINVAL, ERROR_LOCATION);
   
   struct sigaction sa;
   ::memset(&sa, 0, sizeof sa);
   sa.sa_handler = SIG_DFL;
   int result = ::sigaction(sig, &sa, nullptr);
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

unsigned int effectiveUserId()
{
   return ::geteuid();
}

FilePath userHomePath(std::string envOverride)
{
   return User::getUserHomePath(envOverride);
}

FilePath userSettingsPath(const FilePath& userHomeDirectory,
                          const std::string& appName,
                          bool ensureDirectory)
{
   std::string lower = appName;
   boost::to_lower(lower);

   FilePath path = userHomeDirectory.completeChildPath("." + lower);
   if (ensureDirectory)
   {
      Error error = path.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
      }
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

#if !defined(__APPLE__) && !defined(HAVE_PROCSELF)
// worst case scenario - close all file descriptors possible
// this can be EXTREMELY slow when max fd is set to a high value
// note: this conditional should actually never be true
// as all linux systems have /proc/self - this is perserved in the codebase
// as a remainder of what was being done in the recent past
Error closeFileDescriptorsFrom(int fdStart)
{
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
#else
// read the file descriptors from a virtual directory listing,
// iterate over them and close - much faster than the above method
Error closeFileDescriptorsFrom(int fdStart)
{
   std::vector<unsigned int> fds;
   Error error = getOpenFds(&fds);
   if (error)
      return error;

   for (int fd : fds)
   {
      if (fd >= fdStart)
      {
         if (::close(fd) < 0 && errno != EBADF)
            return systemError(errno, ERROR_LOCATION);
      }
   }

   return Success();
}
#endif

} // anonymous namespace

Error getOpenFds(std::vector<uint32_t>* pFds)
{
   return getOpenFds(getpid(), pFds);
}

#ifndef __APPLE__
Error getOpenFds(pid_t pid, std::vector<uint32_t>* pFds)
{
   std::string pidStr = safe_convert::numberToString(pid);
   boost::format fmt("/proc/%1%/fd");
   FilePath filePath(boost::str(fmt % pidStr));

   // note: we use a FileScanner to list the pids instead of using boost
   // (FilePath class), because there is a bug in boost filesystem where
   // directory iterators can segfault under heavy load while reading the /proc filesystem
   // there aren't many details on this, but see https://svn.boost.org/trac10/ticket/10450
   core::system::FileScannerOptions options;
   options.recursive = false;

   tree<FileInfo> subDirs;
   Error error = core::system::scanFiles(core::toFileInfo(filePath), options, &subDirs);
   if (error)
      return error;

   for (const FileInfo& info : subDirs)
   {
      FilePath path(info.absolutePath());

      boost::optional<uint32_t> fd = safe_convert::stringTo<uint32_t>(path.getFilename());
      if (fd)
      {
         pFds->push_back(fd.get());
      }
   }

   return Success();
}

#else
Error getOpenFds(pid_t pid, std::vector<uint32_t> *pFds)
{
   // get size of the buffer needed to hold the list of fds
   int bufferSize = proc_pidinfo(pid, PROC_PIDLISTFDS, 0, 0, 0);
   if (bufferSize == -1)
      return systemError(errno, ERROR_LOCATION);

   // get the list of open fds
   struct proc_fdinfo* procFdInfo = static_cast<struct proc_fdinfo*>(malloc(bufferSize));
   if (!procFdInfo)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   int filledSize = proc_pidinfo(pid, PROC_PIDLISTFDS, 0, procFdInfo, bufferSize);
   int numFds = filledSize / PROC_PIDLISTFD_SIZE;

   for (int i = 0; i < numFds; ++i)
   {
      pFds->push_back(procFdInfo[i].proc_fd);
   }

   free(procFdInfo);
   return Success();
}
#endif

Error closeAllFileDescriptors()
{
   return closeFileDescriptorsFrom(0);
}

Error closeNonStdFileDescriptors()
{
   return closeFileDescriptorsFrom(STDERR_FILENO+1);
}

Error closeChildFileDescriptorsFrom(pid_t childPid, int pipeFd, uint32_t fdStart)
{
   std::size_t written;

   std::vector<uint32_t> fds;
   Error error = getOpenFds(childPid, &fds);
   if (!error)
   {
      for (uint32_t fd : fds)
      {
         error = posix::posixCall<std::size_t>(
            boost::bind(
               ::write,
               pipeFd,
               &fd,
               4),
            ERROR_LOCATION,
            &written);

         if (error)
         {
            return error;
         }
      }
   }
   else
   {
      // we simply log the error instead of returning it because this is generally benign and can
      // happen in certain normal scenarios, such as if /proc/x/fd is only readable by root
      // (if core dumps are turned off)
      core::log::logErrorAsDebug(error);
   }

   // write message close (-1) even if we failed to retrieve pids above
   // this prevents the child from being stuck in limbo or interpreting its
   // actual stdin as fds
   int close = -1;
   Error closeError = posix::posixCall<std::size_t>(
      boost::bind(
         ::write,
         pipeFd,
         &close,
         4),
      ERROR_LOCATION,
      &written);

   if (closeError)
   {
      return error;
   }

   return closeError;
}

namespace signal_safe {

namespace {

void safeClose(uint32_t fd)
{
   while (::close(fd) == -1)
   {
      // keep trying the close operation if it was interrupted
      // otherwise, the file descriptor is not open, so return
      if (errno != EINTR)
         break;
   }
}

// worst case scenario - close all file descriptors possible
// this can be EXTREMELY slow when max fd is set to a high value
void closeFileDescriptorsFromSafe(uint32_t fdStart, rlim_t fdLimit)
{
   // safe function is best effort - swallow all errors
   // this is necessary when invoked in a signal handler or
   // during a fork in multithreaded processes to prevent hangs

   if (fdLimit == RLIM_INFINITY)
      fdLimit = 1024; // default on linux

   // close file descriptors
   for (uint32_t i = fdStart; i < fdLimit; ++i)
   {
      safeClose(i);
   }
}

} // anonymous namespace

void closeNonStdFileDescriptors(rlim_t fdLimit)
{
   closeFileDescriptorsFromSafe(STDERR_FILENO+1, fdLimit);
}

void closeFileDescriptorsFromParent(int pipeFd, uint32_t fdStart, rlim_t fdLimit)
{
   // read fds that we own from parent process pipe until we've read them all
   // the parent must give us this list because we cannot fetch it ourselves
   // in a signal-safe way, but we can read from the pipe safely
   bool error = false;
   bool fdsRead = false;

   int32_t buff;
   while (true)
   {
      ssize_t bytesRead = ::read(pipeFd, &buff, 4);

      // check for error
      if (bytesRead == -1 || bytesRead == 0)
      {
         if (errno != EINTR &&
             errno != EAGAIN)
         {
            error = true;
            break;
         }

         continue;
      }

      // determine which fd was just read from the parent
      if (buff == -1)
      {
         break; // indicates no more fds are open by the process
      }

      fdsRead = true;
      uint32_t fd = static_cast<uint32_t>(buff);

      // close the reported fd if it is in range
      if (fd >= fdStart && fd < fdLimit && buff != pipeFd)
         safeClose(fd);
   }

   // if no descriptors could be read from the parent for whatever reason,
   // or there was an error reading from the pipe,
   // fall back to the slow close method detailed above
   if (error || !fdsRead)
   {
      closeFileDescriptorsFromSafe(fdStart, pipeFd);
      closeFileDescriptorsFromSafe(pipeFd + 1, fdLimit);
   }
}

int permanentlyDropPriv(UidType newUid)
{
   return ::setuid(newUid);
}

int restoreRoot()
{
   return ::setuid(0);
}

int clearSignalMask()
{
   sigset_t blockNoneMask;
   sigemptyset(&blockNoneMask);
   return ::pthread_sigmask(SIG_SETMASK, &blockNoneMask, nullptr);
}

} // namespace signal_safe

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
   std::string filename = filePath.getFilename();
   return (!filename.empty() && (filename[0] == '.'));
}  

bool isHiddenFile(const FileInfo& fileInfo)
{
   return isHiddenFile(FilePath(fileInfo.absolutePath()));
}

bool isReadOnly(const FilePath& filePath)
{
   if (::access(filePath.getAbsolutePath().c_str(), W_OK) == -1)
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
   uuid_t uuid;
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
   return executablePath(argv[0], pExecutablePath);
}

Error executablePath(const char * argv0,
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
   executablePath = initialPath.completePath(argv0).getAbsolutePath();

#endif

   // return realPath of executable path
   return realPath(executablePath, pExecutablePath);
}

// installation path
Error installPath(const std::string& relativeToExecutable,
                  const char * argv0,
                  FilePath* pInstallPath)
{
   // get executable path
   FilePath executablePath;
   Error error = system::executablePath(argv0, &executablePath);
   if (error)
      return error;

   // fully resolve installation path relative to executable
   FilePath installPath = executablePath.getParent().completePath(relativeToExecutable);
   return realPath(installPath.getAbsolutePath(), pInstallPath);
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
   return killProcess(pid, SIGTERM);
}

Error killProcess(PidType pid, int signal)
{
   if (::kill(pid, signal))
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}

std::vector<SubprocInfo> getSubprocessesViaPgrep(PidType pid)
{
   std::vector<SubprocInfo> subprocs;

   // pgrep -P ppid -l returns 0 if there are matches, non-zero
   // otherwise; output is one line per direct child process,
   // for example:
   //
   // 23432 sleep
   // 23433 mycommand
   shell_utils::ShellCommand cmd("pgrep");
   cmd << "-P" << pid << "-l";

   core::system::ProcessOptions options;
   options.detachSession = true;

   core::system::ProcessResult result;
   Error error = runCommand(shell_utils::sendStdErrToStdOut(cmd),
                            options,
                            &result);
   if (error)
   {
      LOG_ERROR(error);
      return subprocs;
   }

   if (result.exitStatus == 0)
   {
      boost::regex re("^(\\d+)\\s+(.+)");
      std::vector<std::string> lines = algorithm::split(result.stdOut, "\n");
      for (const std::string& line : lines)
      {
         if (boost::algorithm::trim_copy(line).empty())
         {
            continue;
         }

         boost::smatch matches;
         if (regex_utils::match(line, matches, re))
         {
            SubprocInfo info;
            info.pid = safe_convert::stringTo<PidType>(matches[1], -1);
            if (info.pid != -1)
            {
               info.exe = matches[2];
               subprocs.push_back(info);
            }
         }
         else
         {
            std::string msg = "Unrecognized output from pgrep -l: '";
            msg += line;
            msg += "'";
            LOG_ERROR_MESSAGE(msg);
         }
      }
   }
   return subprocs;
}

#ifdef __APPLE__ // Mac-specific subprocess detection

std::vector<SubprocInfo> getSubprocessesMac(PidType pid)
{
   std::vector<SubprocInfo> subprocs;

   int result = proc_listchildpids(pid, nullptr, 0);
   if (result > 0)
   {
      std::vector<PidType> buffer(result, 0);
      result = proc_listchildpids(pid, &buffer[0], buffer.size() * sizeof(PidType));
      for (int i = 0; i < result; i++)
      {
         PidType childPid = buffer[i];
         if (childPid == 0)
            continue;

         SubprocInfo info;
         info.pid = childPid;

         // Try to get exe
         std::string path(PROC_PIDPATHINFO_MAXSIZE, '\0');
         proc_pidpath(childPid, &path[0], path.length());
         path.resize(strlen(&path[0]));
         if (!path.empty())
         {
            core::FilePath exePath(path);
            info.exe = exePath.getFilename();
         }
         subprocs.push_back(info);
      }
   }
   return subprocs;
}

#else

std::vector<SubprocInfo> getSubprocessesViaProcFs(PidType pid)
{
   std::vector<SubprocInfo> subprocs;

   core::FilePath procFsPath("/proc");
   if (!procFsPath.exists())
   {
      return getSubprocessesViaPgrep(pid);
   }

   // We iterate all /proc/###/stat files, where ### is a process id.
   //
   // The parent pid is the fourth field (whitespace separated) in the
   // single-line of the stat file. The first field is an int, second field
   // is a string enclosed in parenthesis (...), the third is a single
   // character, and the fourth is the parent pid (int). There are numerous
   // fields after that, all ints of varying sizes.
   //
   // The trick is that the third field can contain arbitrary text,
   // including whitespace and more parenthesis, inside its surrounding
   // parenthesis. The safe way to parse this is to search the file
   // in reverse for the closing parenthesis, then seek forward until we
   // reach the first integer character.
   //
   // An example:
   //    4075 (My )(great Program) S 4074 ....

   std::vector<FilePath> children;
   Error error = procFsPath.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return subprocs;
   }

   for (const FilePath& child : children)
   {
      // only interested in the numeric directories (pid)
      std::string filename = child.getFilename();
      bool isNumber = true;
      for (std::string::const_iterator k = filename.begin(); k != filename.end(); ++k)
      {
         if (!isdigit(*k))
         {
            isNumber = false;
            break;
         }
      }

      if (!isNumber)
         continue;

      // load the stat file
      std::string contents;
      FilePath statFile(child.completePath("stat"));
      Error error = rstudio::core::readStringFromFile(statFile, &contents);
      if (error)
      {
         continue;
      }

      size_t closingParen = contents.find_last_of(')');
      if (closingParen == std::string::npos)
      {
         LOG_ERROR_MESSAGE("no closing parenthesis");
         continue;
      }

      size_t i = contents.find_first_of("0123456789", closingParen);
      if (i == std::string::npos)
      {
         LOG_ERROR_MESSAGE("no integer after closing parenthesis");
         continue;
      }

      size_t j = contents.find_first_not_of("0123456789", i);
      if (j == std::string::npos)
      {
         LOG_ERROR_MESSAGE("no non-int after first int");
         continue;
      }

      size_t ppidLen = j - i;
      PidType ppid = safe_convert::stringTo<PidType>(contents.substr(i, ppidLen), -1);
      if (ppid == -1)
      {
         LOG_ERROR_MESSAGE("unrecognized parent process id");
         continue;
      }
      if (ppid != pid)
      {
         continue;
      }

      size_t openParen = contents.find_first_of('(');
      if (openParen == std::string::npos)
      {
         LOG_ERROR_MESSAGE("no opening parenthesis");
         continue;
      }
      if (openParen < 2) // at a minimum, "# (foo)"
      {
         LOG_ERROR_MESSAGE("no pid before exe name");
         continue;
      }
      if (closingParen < openParen)
      {
         LOG_ERROR_MESSAGE("closing paren before open paren");
         continue;
      }

      SubprocInfo info;
      info.exe = contents.substr(openParen + 1, closingParen - openParen - 1);
      info.pid = safe_convert::stringTo<PidType>(contents.substr(0, openParen - 1), -1);
      if (info.pid == -1)
      {
         LOG_ERROR_MESSAGE("unrecognized child process id");
         continue;
      }
      subprocs.push_back(info);
   }

   return subprocs;
}
#endif // !__APPLE__

std::vector<SubprocInfo> getSubprocesses(PidType pid)
{
#ifdef __APPLE__
   return getSubprocessesMac(pid);
#else // Linux
   return getSubprocessesViaProcFs(pid);
#endif
}

#ifdef __APPLE__

FilePath currentWorkingDirMac(PidType pid)
{
   struct proc_vnodepathinfo info;

   int size = ::proc_pidinfo(
            pid, PROC_PIDVNODEPATHINFO, 0,
            &info, PROC_PIDVNODEPATHINFO_SIZE);

   // check for explicit failure
   if (size <= 0)
   {
      LOG_ERROR(systemError(errno, ERROR_LOCATION));
      return FilePath();
   }

   // check for failure to write all required bytes
   if (size < gsl::narrow_cast<int>(PROC_PIDVNODEPATHINFO_SIZE))
   {
      using namespace boost::system::errc;
      LOG_ERROR(systemError(not_enough_memory, ERROR_LOCATION));
      return FilePath();
   }

   // ok, we can return the path
   return FilePath(info.pvi_cdir.vip_path);
}

#endif // __APPLE__


#ifndef __APPLE__

// NOTE: disabled on macOS; prefer using 'currentWorkingDirMac()'
FilePath currentWorkingDirViaLsof(PidType pid)
{
   // lsof -a -p PID -d cwd -Fn
   //
   shell_utils::ShellCommand cmd("lsof");
   cmd << shell_utils::EscapeFilesOnly;
   cmd << "-a";
   cmd << "-p" << pid;
   cmd << "-d cwd";
   cmd << "-Fn";

   core::system::ProcessOptions options;
   core::system::ProcessResult result;
   Error error = runCommand(cmd, options, &result);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   if (result.exitStatus != 0)
      return FilePath();

   // lsof outputs multiple lines, which varies by platform. We want the one
   // starting with lowercase 'n', after that is the current working directory.
   size_t pos = 0;
   while (pos != std::string::npos && pos < result.stdOut.length())
   {
      if (result.stdOut.at(pos) == 'n')
      {
         pos++;
         size_t finalPos = result.stdOut.find_first_of('\n', pos);
         if (finalPos != std::string::npos)
            return FilePath(result.stdOut.substr(pos, finalPos - pos));
         else
            return FilePath(result.stdOut.substr(pos));
      }

      // next line
      pos = result.stdOut.find_first_of('\n', pos);
      pos++;
   }

   return FilePath();
}

FilePath currentWorkingDirViaProcFs(PidType pid)
{
   core::FilePath procFsPath("/proc");
   if (!procFsPath.exists())
   {
      return currentWorkingDirViaLsof(pid);
   }

   std::string procId = safe_convert::numberToString(pid);
   if (procId.empty())
      return FilePath();

   // /proc/PID/cwd is a symbolic link to the process' current working directory
   FilePath pidPath = procFsPath.completePath(procId).completePath("cwd");
   if (pidPath.isSymlink())
      return pidPath.resolveSymlink();
   else
      return FilePath();
}
#endif // !__APPLE__

FilePath currentWorkingDir(PidType pid)
{
#ifdef __APPLE__
   return currentWorkingDirMac(pid);
#else
   return currentWorkingDirViaProcFs(pid);
#endif
}

namespace {

Error writePidFile(const FilePath& pidFile, PidType pid)
{
   return core::writeStringToFile(pidFile,
                                  safe_convert::numberToString(pid),
                                  string_utils::LineEndingPosix);
}

} // anonymous namespace

Error daemonize(const std::string& pidFile)
{
   bool writePid = !pidFile.empty();

   if (writePid)
   {
      // ensure that we will be able to write the daemon's pid to the pidfile by first
      // writing our own pid - if this fails, we need to bail out before daemonizing
      Error error = writePidFile(FilePath(pidFile), ::getpid());
      if (error)
         return error;
   }

   // fork
   PidType pid = ::fork();
   if (pid < 0)
   {
      return systemError(errno, ERROR_LOCATION); // fork error
   }
   else if (pid > 0)
   {
      int ret = EXIT_SUCCESS;

      if (writePid)
      {
         Error error = writePidFile(FilePath(pidFile), pid);
         if (error)
         {
            LOG_ERROR(error);
            ret = EXIT_FAILURE;
         }
      }

      ::exit(ret); // parent exits
   }

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
      case CoreLimit:
         *pLimit = RLIMIT_CORE;
         break;
      case MemlockLimit:
         *pLimit = RLIMIT_MEMLOCK;
         break;
      case CpuLimit:
         *pLimit = RLIMIT_CPU;
         break;
#ifndef __APPLE__
      case NiceLimit:
         *pLimit = RLIMIT_NICE;
         break;
#endif
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

Error systemInformation(SysInfo* pSysInfo)
{
   pSysInfo->cores = boost::thread::hardware_concurrency();

#ifndef __APPLE__
   struct sysinfo info;
   if (::sysinfo(&info) == -1)
      return systemError(errno, ERROR_LOCATION);

   pSysInfo->load1 = info.loads[0] / (float)(1 << SI_LOAD_SHIFT);
   pSysInfo->load5 = info.loads[1] / (float)(1 << SI_LOAD_SHIFT);
   pSysInfo->load15 = info.loads[2] / (float)(1 << SI_LOAD_SHIFT);
#else
   double loads[3];
   if (::getloadavg(loads, 3) == -1)
      return systemError(errno, ERROR_LOCATION);
   pSysInfo->load1 = loads[0];
   pSysInfo->load5 = loads[1];
   pSysInfo->load15 = loads[2];
#endif
   return Success();
}

namespace  {

void toPids(const std::vector<std::string>& lines, std::vector<PidType>* pPids)
{
   for (const std::string& line : lines)
   {
      PidType pid = safe_convert::stringTo<PidType>(line, -1);
      if (pid != -1)
         pPids->push_back(pid);
   }
}

} // anonymous namespace

#ifndef __APPLE__
core::Error pidof(const std::string& process, std::vector<PidType>* pPids)
{
   // use pidof to capture pids
   std::string cmd = "pidof " + process;
   core::system::ProcessResult result;
   Error error = core::system::runCommand(cmd,
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
      return error;

   // parse into pids
   std::vector<std::string> pids;
   boost::algorithm::split(pids,
                           result.stdOut,
                           boost::algorithm::is_space());

   toPids(pids, pPids);
   return Success();
}

Error processInfo(const std::string& process,
                  std::vector<ProcessInfo>* pInfo,
                  bool suppressErrors,
                  ProcessFilter filter)
{
   // clear the existing process info
   pInfo->clear();

   // declear directory iterator
   DIR *pDir = nullptr;

   try
   {
      // open the /proc directory
      pDir = ::opendir("/proc");
      if (pDir == nullptr)
         return systemError(errno, ERROR_LOCATION);

      struct dirent *pDirent;
      while ( (pDirent = ::readdir(pDir)) )
      {
         // confirm this is a process directory
         PidType pid = safe_convert::stringTo<PidType>(pDirent->d_name, -1);
         if (pid == -1)
            continue;

         ProcessInfo info;
         Error error = processInfo(safe_convert::stringTo<pid_t>(pDirent->d_name, -1), &info);
         if (error)
         {
            // only log the error if we were not told otherwise
            // in the vast majority of cases, these errors indicate
            // transient process issues (like processes exiting) or
            // not having access to privileged processes and are benign
            // and not worthy of logging
            if (!suppressErrors)
               LOG_ERROR(error);

            continue;
         }

         // check if this is the process we are filtering on
         if (process.empty() || info.exe == process)
         {
            if (!filter || filter(info))
            {
               pInfo->push_back(info);
            }
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   if (pDir != nullptr)
      ::closedir(pDir);

   return Success();
}

Error processInfo(pid_t pid, ProcessInfo* pInfo)
{
   std::string pidStr = safe_convert::numberToString(pid);

   // confirm the cmdline file exists for this pid
   boost::format fmt("/proc/%1%/cmdline");
   FilePath cmdlineFile = FilePath(boost::str(fmt % pidStr));
   if (!cmdlineFile.exists())
      return systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);

   // read the cmdline
   std::string cmdline;
   Error error = core::readStringFromFile(cmdlineFile, &cmdline);
   if (error)
      return error;
   boost::algorithm::trim(cmdline);

   // confirm we have a command line
   if (cmdline.empty())
      return systemError(boost::system::errc::protocol_error, ERROR_LOCATION);

   std::vector<std::string> commandVector;
   boost::algorithm::split(commandVector, cmdline, boost::is_any_of(boost::as_array("\0")));
   if (commandVector.size() == 0)
      return systemError(boost::system::errc::protocol_error, ERROR_LOCATION);
   cmdline = commandVector.front();

   // remove the first element from the command vector (the actual command)
   // and simply keep the arguments as the command is stored in its own variable
   commandVector.erase(commandVector.begin());

   // confirm the stat file exists for this pid
   boost::format statFmt("/proc/%1%/stat");
   FilePath statFile = FilePath(boost::str(statFmt % pidStr));
   if (!statFile.exists())
      return systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);

   // stat the file to determine it's owner
   struct stat st;
   if (::stat(cmdlineFile.getAbsolutePath().c_str(), &st) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", cmdlineFile);
      return error;
   }

   // get the username
   core::system::User user;
   error = User::getUserFromIdentifier(st.st_uid, user);
   if (error)
      return error;

   // read the stat fields for other relevant process info
   std::string statStr;
   error = core::readStringFromFile(statFile, &statStr);
   if (error)
      return error;

   std::vector<std::string> statFields;
   boost::algorithm::split(statFields, statStr,
                           boost::is_any_of(" "),
                           boost::algorithm::token_compress_on);

   if (statFields.size() < 5)
   {
      return systemError(boost::system::errc::protocol_error,
                         "Expected at least 5 stat fields but read: " +
                            safe_convert::numberToString<size_t>(statFields.size()),
                         ERROR_LOCATION);
   }

   // get the process state
   std::string state = statFields[2];

   // get process parent id
   std::string ppidStr = statFields[3];
   pid_t ppid = safe_convert::stringTo<pid_t>(ppidStr, -1);

   // get process group id
   std::string pgrpStr = statFields[4];
   pid_t pgrp = safe_convert::stringTo<pid_t>(pgrpStr, -1);

   // set process info fields
   pInfo->pid = pid;
   pInfo->ppid = ppid;
   pInfo->pgrp = pgrp;
   pInfo->username = user.getUsername();
   pInfo->exe = FilePath(cmdline).getFilename();
   pInfo->state = state;
   pInfo->arguments = commandVector;

   return Success();
}

bool isProcessRunning(pid_t pid)
{
   // the posix standard way of checking if a process
   // is running is to send the 0 signal to it
   // requires root privilege if process is owned by another user
   int result = kill(pid, 0);
   return result == 0;
}

namespace {

Error readStatFields(const FilePath& statFilePath,
                     std::size_t numRequiredFields,
                     std::vector<std::string>* pFields)
{
   if (!statFilePath.exists())
      return core::fileNotFoundError(statFilePath, ERROR_LOCATION);

   std::string str;
   Error error = core::readStringFromFile(statFilePath, &str);
   if (error)
      return error;

   boost::algorithm::split(*pFields, str,
                           boost::is_any_of(" "),
                           boost::algorithm::token_compress_on);
   if (pFields->size() < numRequiredFields)
   {
      Error error = systemError(boost::system::errc::protocol_error,
                                ERROR_LOCATION);
      error.addProperty("stat-fields", str);
      return error;
   }

   return Success();
}

} // anonymous namespace

Error ProcessInfo::creationTime(boost::posix_time::ptime* pCreationTime) const
{
   // get clock ticks (bail if we can't)
   double clockTicks = ::sysconf(_SC_CLK_TCK);
   if (clockTicks == -1)
      return systemError(errno, ERROR_LOCATION);

   // get boot time
   double bootTime = 0.0;
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(FilePath("/proc/stat"), &lines);
   if (error)
      return error;
   for (const std::string& line : lines)
   {
      if (boost::algorithm::starts_with(line, "btime"))
      {
         std::vector<std::string> fields;
         boost::algorithm::split(fields,
                                 line,
                                 boost::algorithm::is_any_of(" \t"),
                                 boost::algorithm::token_compress_on);
         if (fields.size() > 1)
         {
            bootTime = safe_convert::stringTo<double>(fields[1], 0);
            break;
         }
      }
   }
   if (bootTime == 0.0)
   {
      return systemError(boost::system::errc::protocol_error,
                         "Unable to find btime in /proc/stat",
                         ERROR_LOCATION);
   }


   // read the stat fields
   boost::format fmt("/proc/%1%");
   std::string dir = boost::str(fmt % pid);
   FilePath procDir(dir);
   std::vector<std::string> fields;
   error = readStatFields(procDir.completeChildPath("stat"), 22, &fields);
   if (error)
      return error;

   // get the creation time and return success
   double startTicks = safe_convert::stringTo<double>(fields[21], 0);
   double startSecs = (startTicks / clockTicks) + bootTime;
   *pCreationTime = date_time::timeFromSecondsSinceEpoch(startSecs);
   return Success();
}

#else
core::Error pidof(const std::string& process, std::vector<PidType>* pPids)
{
   // use ps to capture pids
   std::string cmd = "ps acx | awk \"{if (\\$5==\\\"" +
                      process + "\\\") print \\$1}\"";
   core::system::ProcessResult result;
   Error error = core::system::runCommand(cmd,
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
      return error;

   // parse into pids
   std::vector<std::string> lines;
   boost::algorithm::split(lines,
                           result.stdOut,
                           boost::algorithm::is_any_of("\n"));
   toPids(lines, pPids);
   return Success();
}

Error processInfo(const std::string& process, std::vector<ProcessInfo>* pInfo, bool suppressErrors, ProcessFilter filter)
{
   // use ps to capture process info
   // output format
   // USER:PID:PPID:PGID:::STATE:::PROCNAME:ARG1:ARG2:...:ARGN
   // we use a colon as the separator as it is not a valid path character in OSX
   std::string cmd = process.empty() ? "ps axj | awk '{OFS=\":\"; $5=\"\"; $6=\"\"; $8=\"\"; $9=\"\"; print}'"
                                     : "ps axj | awk '{OFS=\":\"; if ($10==\"" +
                                        process + "\"){ $5=\"\"; $6=\"\"; $8=\"\"; $9=\"\"; print} }'";

   core::system::ProcessResult result;
   Error error = core::system::runCommand(cmd,
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
      return error;

   // parse into ProcessInfo
   std::vector<std::string> lines;
   boost::algorithm::split(lines,
                           result.stdOut,
                           boost::algorithm::is_any_of("\n"));

   for (const std::string& line : lines)
   {
      if (line.empty()) continue;
       
      std::vector<std::string> lineInfo;
      boost::algorithm::split(lineInfo,
                              line,
                              boost::algorithm::is_any_of(":"));

      if (lineInfo.size() < 10)
      {
         LOG_WARNING_MESSAGE("Exepcted 10 items from ps output but received: " + safe_convert::numberToString<size_t>(lineInfo.size()));
         continue;
      }

      ProcessInfo procInfo;
      procInfo.username = lineInfo[0];
      procInfo.pid = safe_convert::stringTo<pid_t>(lineInfo[1], 0);
      procInfo.ppid = safe_convert::stringTo<pid_t>(lineInfo[2], 0);
      procInfo.pgrp = safe_convert::stringTo<pid_t>(lineInfo[3], 0);
      procInfo.state = lineInfo[6];

      // parse process name and arguments
      procInfo.exe = lineInfo[9];
      if (lineInfo.size() > 10)
         procInfo.arguments = std::vector<std::string>(lineInfo.begin() + 10, lineInfo.end());

      // check to see if this process info passes the filter criteria
      if (!filter || filter(procInfo))
      {
         pInfo->push_back(procInfo);
      }
   }

   return Success();
}
#endif

std::ostream& operator<<(std::ostream& os, const ProcessInfo& info)
{
   os << info.pid << " - " << info.username;
   return os;
}

Error ipAddresses(std::vector<posix::IpAddress>* pAddresses,
                  bool includeIPv6)
{
    return posix::getIpAddresses(*pAddresses, includeIPv6);
}


Error restrictCoreDumps()
{
   // set allowed size of core dumps to 0 bytes
   Error error = setResourceLimit(core::system::CoreLimit, 0);
   if (error)
      return error;

   // no ptrace core dumps permitted
#ifndef __APPLE__
   int res = ::prctl(PR_SET_DUMPABLE, 0);
   if (res == -1)
      return systemError(errno, ERROR_LOCATION);
#endif

   return Success();
}

Error enableCoreDumps()
{
   return posix::enableCoreDumps();
}

void printCoreDumpable(const std::string& context)
{
   std::ostringstream ostr;

   ostr << "Core Dumpable (" << context << ")" << std::endl;

   // ulimit
   RLimitType rLimitSoft = 0, rLimitHard = 0;
   Error error = getResourceLimit(core::system::CoreLimit,
                                  &rLimitSoft, &rLimitHard);
   if (error)
      LOG_ERROR(error);

   ostr << "  soft limit: " << rLimitSoft << std::endl;
   ostr << "  hard limit: " << rLimitHard << std::endl;

   // ptrace
#ifndef __APPLE__
   int dumpable = ::prctl(PR_GET_DUMPABLE, nullptr, nullptr, nullptr, nullptr);
   if (dumpable == -1)
      LOG_ERROR(systemError(errno, ERROR_LOCATION));
   ostr << "  pr_get_dumpable: " << dumpable << std::endl;
#endif

   std::cerr << ostr.str();
}

void setProcessLimits(ProcessLimits limits)
{
   // memory limit
   if (limits.memoryLimitBytes != 0)
   {
      Error error = setResourceLimit(MemoryLimit, limits.memoryLimitBytes);
      if (error)
         LOG_ERROR(error);
   }

   // stack limit
   if (limits.stackLimitBytes != 0)
   {
      Error error = setResourceLimit(StackLimit, limits.stackLimitBytes);
      if (error)
         LOG_ERROR(error);
   }

   // user processes limit
   if (limits.userProcessesLimit != 0)
   {
      Error error = setResourceLimit(UserProcessesLimit,
                                     limits.userProcessesLimit);
      if (error)
         LOG_ERROR(error);
   }

   // cpu limit
   if (limits.cpuLimit != 0)
   {
      Error error = setResourceLimit(CpuLimit, limits.cpuLimit);
      if (error)
         LOG_ERROR(error);
   }

   // nice limit
   if (limits.niceLimit != 0)
   {
      Error error = setResourceLimit(NiceLimit, limits.niceLimit);
      if (error)
         LOG_ERROR(error);
   }

   // files limit
   if (limits.filesLimit != 0)
   {
      Error error = setResourceLimit(FilesLimit, limits.filesLimit);
      if (error)
         LOG_ERROR(error);
   }

   // priority
   if (limits.priority != 0)
   {
      if (::setpriority(PRIO_PROCESS, 0, limits.priority) == -1)
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
   }

   // cpu affinity
#ifndef __APPLE__
   if (!isCpuAffinityEmpty(limits.cpuAffinity))
   {
      Error error = setCpuAffinity(limits.cpuAffinity);
      if (error)
         LOG_ERROR(error);
   }
#endif
}



namespace {


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
                         ProcessConfigFilter configFilter,
                         PidType* pProcessId)
{
   PidType pid = ::fork();

   // error
   if (pid < 0)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("commmand", path);
      return error;
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

      Error error = runProcess(path, runAsUser, config, configFilter);
      if (error)
      {
         LOG_ERROR(error);
         ::exit(EXIT_FAILURE);
      }
   }

   // parent
   if (pProcessId)
      *pProcessId = pid;

   return Success();
}

Error runProcess(const std::string& path,
                 const std::string& runAsUser,
                 ProcessConfig& config,
                 ProcessConfigFilter configFilter)
{
   // change user here if requested
   if (!runAsUser.empty())
   {
      // restore root
      Error error = restorePriv();
      if (error)
         return error;

      // set limits
      setProcessLimits(config.limits);

      // switch user
      error = permanentlyDropPriv(runAsUser);
      if (error)
         return error;
   }
   else
   {
      // set limits - calls may fail if attempting to set greater than max allowed values
      // since the user is potentially unprivileged
      setProcessLimits(config.limits);
   }

   // clear the signal mask so the child process can handle whatever
   // signals it wishes to
   Error error = core::system::clearSignalMask();
   if (error)
      return error;

   // get current user (before closing file handles since
   // we might be using a PAM module that has open FDs...)
   User user;
   error = User::getCurrentUser(user);
   if (error)
      return error;

   // close all open file descriptors other than std streams
   error = closeNonStdFileDescriptors();
   if (error)
      return error;

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
   core::system::setenv(&env, "USER", user.getUsername());
   core::system::setenv(&env, "LOGNAME", user.getUsername());
   core::system::setenv(&env, "HOME", user.getHomePath().getAbsolutePath());
   copyEnvironmentVar("SHELL", &env);

   // apply config filter if we have one
   if (configFilter)
      configFilter(user, &config);

   // add custom environment vars (overriding as necessary)
   for (core::system::Options::const_iterator it = config.environment.begin();
        it != config.environment.end();
        ++it)
   {
      core::system::setenv(&env, it->first, it->second);
   }

   // NOTE: this implemenentation ignores the config.stdInput field (that
   // was put in for another consumer)

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
      if (!it->second.empty())
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
   return error;
}

Error getChildProcesses(std::vector<ProcessInfo> *pOutProcesses)
{
   return getChildProcesses(::getpid(), pOutProcesses);
}

Error getChildProcesses(pid_t pid,
                        std::vector<ProcessInfo> *pOutProcesses)
{
   if (!pOutProcesses)
      return systemError(EINVAL, ERROR_LOCATION);

   // get all processes
   std::vector<ProcessInfo> processes;
   Error error = processInfo("", &processes, true);
   if (error)
      return error;

   // build a process tree of the processes
   ProcessTreeT tree;
   createProcessTree(processes, &tree);

   // return just the children of the specified process
   ProcessTreeT::const_iterator iter = tree.find(pid);
   if (iter == tree.end())
      return Success();

   const boost::shared_ptr<ProcessTreeNode>& rootNode = iter->second;
   getChildren(rootNode, pOutProcesses);

   return Success();
}

Error terminateChildProcesses()
{
   return terminateChildProcesses(SIGTERM);
}

Error terminateChildProcesses(int signal)
{
   return terminateChildProcesses(::getpid(), signal);
}

Error terminateChildProcesses(pid_t pid,
                              int signal)
{
   std::vector<ProcessInfo> childProcesses;
   Error error = getChildProcesses(pid, &childProcesses);
   if (error)
      return error;

   for (const ProcessInfo& process : childProcesses)
   {
      if (::kill(process.pid, signal) != 0)
      {
         // On the Mac some child processes exist as a side effect of the
         // call to getChildProcesses (sh, ps, awk). This results in a
         // regular stream of error messages at the dev console, so we
         // suppress that particular error to prevent bogus error states.
         Error error = systemError(errno, ERROR_LOCATION);
#ifdef __APPLE__
         if (error != systemError(boost::system::errc::no_such_process, ErrorLocation()))
#endif
            LOG_ERROR(error);
      }
   }

   // the actual kill is best effort
   // so return success regardless
   return Success();
}

bool isUserNotFoundError(const Error& error)
{
   return error == systemError(boost::system::errc::permission_denied, ErrorLocation());
}

Error userBelongsToGroup(const User& user,
                         const std::string& groupName,
                         bool* pBelongs)
{
   *pBelongs = false; // default to not found
   group::Group group;
   Error error = group::groupFromName(groupName, &group);
   if (error)
      return error;

   // see if the group id matches the user's group id
   if (user.getGroupId() == group.groupId)
   {
      *pBelongs = true;
   }
   // else scan the list of member names for this user
   else
   {
      *pBelongs = std::find(group.members.begin(),
                            group.members.end(),
                            user.getUsername()) != group.members.end();
   }

   return Success();
}



/////////////////////////////////////////////////////////////////////
//
// setuid privilege manipulation
//

bool realUserIsRoot()
{
   return posix::realUserIsRoot();
}

bool effectiveUserIsRoot()
{
   return ::geteuid() == 0;
}

Error temporarilyDropPriv(const std::string& newUsername)
{
   // clear error state
   errno = 0;

   // get user info
   User user;
   Error error = User::getUserFromIdentifier(newUsername, user);
   if (error)
      return error;

   return posix::temporarilyDropPrivileges(user);
}

Error restorePriv()
{
   return posix::restorePrivileges();
}

// privilege manipulation for systems that support setresuid/getresuid
#if defined(HAVE_SETRESUID)

Error permanentlyDropPriv(const std::string& newUsername)
{
   // clear error state
   errno = 0;

   // get user info
   User user;
   Error error = User::getUserFromIdentifier(newUsername, user);
   if (error)
      return error;

   // supplemental group list
   if (::initgroups(user.getUsername().c_str(), user.getGroupId()) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group
   if (::setresgid(user.getGroupId(), user.getGroupId(), user.getGroupId()) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   gid_t rgid, egid, sgid;
   if (::getresgid(&rgid, &egid, &sgid) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (rgid != user.getGroupId() || egid != user.getGroupId() || sgid != user.getGroupId())
      return systemError(EACCES, ERROR_LOCATION);

   // set user
   if (::setresuid(user.getUserId(), user.getUserId(), user.getUserId()) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   uid_t ruid, euid, suid;
   if (::getresuid(&ruid, &euid, &suid) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (ruid != user.getUserId() || euid != user.getUserId() || suid != user.getUserId())
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}

// privilege manipulation for systems that don't support setresuid/getresuid
#else

namespace {
   uid_t s_privUid;
}

Error permanentlyDropPriv(const std::string& newUsername)
{
   // clear error state
   errno = 0;

   // get user info
   User user;
   Error error = User::getUserFromIdentifier(newUsername, user);
   if (error)
      return error;

   // supplemental group list
   if (::initgroups(user.getUsername().c_str(), user.getGroupId()) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group
   if (::setregid(user.getGroupId(), user.getGroupId()) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::getgid() != user.getGroupId() || ::getegid() != user.getGroupId())
      return systemError(EACCES, ERROR_LOCATION);

   // set user
   if (::setreuid(user.getUserId(), user.getUserId()) < 0)
      return systemError(errno, ERROR_LOCATION);
   // verify
   if (::getuid() != user.getUserId() || ::geteuid() != user.getUserId())
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}

#endif

Error restoreRoot()
{
   return posix::restoreRoot();
}

} // namespace system
} // namespace core
} // namespace rstudio

