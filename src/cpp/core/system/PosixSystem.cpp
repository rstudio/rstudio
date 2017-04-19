/*
 * PosixSystem.cpp
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

#include <core/system/PosixSystem.hpp>

#include <stdio.h>

#include <iostream>
#include <vector>

#include <boost/foreach.hpp>
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
#include <sys/types.h>
#include <ifaddrs.h>
#include <sys/socket.h>
#include <netdb.h>

#include <uuid/uuid.h>

#ifdef __APPLE__
#include <mach-o/dyld.h>
#include <sys/proc_info.h>
#include <libproc.h>
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
#include <core/FileSerializer.hpp>

#include <core/system/ProcessArgs.hpp>
#include <core/system/Environment.hpp>
#include <core/system/PosixUser.hpp>
#include <core/system/PosixGroup.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include "config.h"

namespace rstudio {
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

      case SigTerm:
         return SIGTERM;
         
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
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", path);
      return error;
   }
  *pRealPath = FilePath(realPath);
   return Success();
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

void setLogToStderr(bool logToStderr)
{
   if (s_pLogWriter)
      s_pLogWriter->setLogToStderr(logToStderr);
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

unsigned int effectiveUserId()
{
   return ::geteuid();
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
   executablePath = initialPath.complete(argv0).absolutePath();

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

bool hasSubprocessesViaPgrep(PidType pid)
{
   // pgrep -P ppid returns 0 if there are matches, non-zero
   // otherwise
   shell_utils::ShellCommand cmd("pgrep");
   cmd << "-P" << pid;

   core::system::ProcessOptions options;
   options.detachSession = true;

   core::system::ProcessResult result;
   Error error = runCommand(shell_utils::sendStdErrToStdOut(cmd),
                            options,
                            &result);
   if (error)
   {
      // err on the side of assuming child processes, so we don't kill
      // a job unintentionally
      LOG_ERROR(error);
      return true;
   }
   return result.exitStatus == 0;
}

#ifdef __APPLE__ // Mac-specific subprocess detection
bool hasSubprocessesMac(PidType pid)
{
   int result = proc_listchildpids(pid, NULL, 0);
   if (result > 0)
   {
      // have fetch details to get accurate result
      std::vector<int> buffer;
      buffer.reserve(result);
      result = proc_listchildpids(pid, &buffer[0], buffer.capacity() * sizeof(int));
   }
   return result > 0;
}
#endif

bool hasSubprocessesViaProcFs(PidType pid, core::FilePath procFsPath)
{
   if (!procFsPath.exists())
   {
      return true; // err on the side of assuming child processes exist
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
   Error error = procFsPath.children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return true; // err on the side of assuming child processes exist
   }

   BOOST_FOREACH(const FilePath& child, children)
   {
      // only interested in the numeric directories (pid)
      std::string filename = child.filename();
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
      FilePath statFile(child.complete("stat"));
      Error error = rstudio::core::readStringFromFile(statFile, &contents);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      size_t i = contents.find_last_of(')');
      if (i == std::string::npos)
      {
         LOG_ERROR_MESSAGE("no closing parenthesis");
         continue;
      }

      i = contents.find_first_of("0123456789", i);
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

      // extremely unexpected/unlikely, but make sure we don't have some funky
      // super-large integer here that could cause lexical_cast to throw
      size_t ppidLen = j - i;
      if (ppidLen > 9)
      {
         LOG_ERROR_MESSAGE("stat file ppid too large");
         continue;
      }
      int ppid = boost::lexical_cast<int>(contents.substr(i, ppidLen));
      if (ppid == pid)
         return true;
   }
   return false;
}

bool hasSubprocesses(PidType pid)
{
#ifndef __APPLE__
   core::FilePath procFsPath("/proc");
   if (!procFsPath.exists())
   {
      return hasSubprocessesViaPgrep(pid);
   }
   return hasSubprocessesViaProcFs(pid, procFsPath);

#else
   return hasSubprocessesMac(pid);
#endif
}

Error daemonize()
{
   // fork
   PidType pid = ::fork();
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
   BOOST_FOREACH(const std::string& line, lines)
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

Error processInfo(const std::string& process, std::vector<ProcessInfo>* pInfo)
{
   // clear the existing process info
   pInfo->clear();

   // declear directory iterator
   DIR *pDir = NULL;

   try
   {
      // open the /proc directory
      pDir = ::opendir("/proc");
      if (pDir == NULL)
         return systemError(errno, ERROR_LOCATION);

      struct dirent *pDirent;
      while ( (pDirent = ::readdir(pDir)) )
      {
         // confirm this is a process directory
         PidType pid = safe_convert::stringTo<PidType>(pDirent->d_name, -1);
         if (pid == -1)
            continue;

         // confirm the cmdline file exists for this pid
         boost::format fmt("/proc/%1%/cmdline");
         FilePath cmdlineFile = FilePath(boost::str(fmt % pDirent->d_name));
         if (!cmdlineFile.exists())
            continue;

         // read the cmdline
         std::string cmdline;
         Error error = core::readStringFromFile(cmdlineFile, &cmdline);
         if (error)
         {
            LOG_ERROR(error);
            continue;
         }
         boost::algorithm::trim(cmdline);

         // confirm we have a command line
         if (cmdline.empty())
            continue;

         // just keep first part of the command line (the rest represent the
         // program arguments)
         size_t pos = cmdline.find('\0');
         if (pos != std::string::npos)
            cmdline = cmdline.substr(0, pos);

         // check if this is the process we are filtering on
         if (FilePath(cmdline).filename() == process)
         {
            // stat the file to determine it's owner
            struct stat st;
            if (::stat(cmdlineFile.absolutePath().c_str(), &st) == -1)
            {
               Error error = systemError(errno, ERROR_LOCATION);
               error.addProperty("path", cmdlineFile);
               LOG_ERROR(error);
               continue;
            }

            // get the username
            core::system::user::User user;
            Error error = core::system::user::userFromId(st.st_uid, &user);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }

            // add a process info
            ProcessInfo pi;
            pi.pid = pid;
            pi.username = user.username;
            pInfo->push_back(pi);
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   if (pDir != NULL)
      ::closedir(pDir);

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

Error processInfo(const std::string& process, std::vector<ProcessInfo>* pInfo)
{
   std::vector<PidType> pids;
   Error error = pidof(process, &pids);
   if (error)
      return error;

   pInfo->clear();
   BOOST_FOREACH(PidType pid, pids)
   {
      ProcessInfo pi;
      pi.pid = pid;
      pInfo->push_back(pi);
   }

   return Success();
}
#endif

std::ostream& operator<<(std::ostream& os, const ProcessInfo& info)
{
   os << info.pid << " - " << info.username;
   return os;
}

Error ipAddresses(std::vector<IpAddress>* pAddresses)
{
   // get addrs
   struct ifaddrs* pAddrs;
   if (::getifaddrs(&pAddrs) == -1)
      return systemError(errno, ERROR_LOCATION);

   // iterate through the linked list
   for (struct ifaddrs* pAddr = pAddrs; pAddr != NULL; pAddr = pAddr->ifa_next)
   {
      if (pAddr->ifa_addr == NULL)
         continue;

      if (pAddr->ifa_addr->sa_family != AF_INET)
         continue;

      char host[NI_MAXHOST];
      if (::getnameinfo(pAddr->ifa_addr,
                        sizeof(struct sockaddr_in),
                        host, NI_MAXHOST,
                        NULL, 0, NI_NUMERICHOST) != 0)
      {
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
         continue;
      }

      struct IpAddress addr;
      addr.name = pAddr->ifa_name;
      addr.addr = host;
      pAddresses->push_back(addr);
   }

   // free them and return success
   ::freeifaddrs(pAddrs);
   return Success();
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

void printCoreDumpable(const std::string& context)
{
   std::ostringstream ostr;

   ostr << "Core Dumpable (" << context << ")" << std::endl;

   // ulimit
   RLimitType rLimitSoft, rLimitHard;
   Error error = getResourceLimit(core::system::CoreLimit,
                                  &rLimitSoft, &rLimitHard);
   if (error)
      LOG_ERROR(error);

   ostr << "  soft limit: " << rLimitSoft << std::endl;
   ostr << "  hard limit: " << rLimitHard << std::endl;

   // ptrace
#ifndef __APPLE__
   int dumpable = ::prctl(PR_GET_DUMPABLE, NULL, NULL, NULL, NULL);
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
         setProcessLimits(config.limits);

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
   *pBelongs = false; // default to not found
   group::Group group;
   Error error = group::groupFromName(groupName, &group);
   if (error)
      return error;

   // see if the group id matches the user's group id
   if (user.groupId == group.groupId)
   {
      *pBelongs = true;
   }
   // else scan the list of member names for this user
   else
   {
      *pBelongs = std::find(group.members.begin(),
                            group.members.end(),
                            user.username) != group.members.end();
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
} // namespace rstudio

