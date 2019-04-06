/*
 * System.hpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#ifndef CORE_SYSTEM_SYSTEM_HPP
#define CORE_SYSTEM_SYSTEM_HPP


#if defined(_WIN32)
#include <windows.h>
typedef DWORD PidType;
#else  // UNIX
#include <sys/types.h>
#include <sys/resource.h>
typedef pid_t PidType;
typedef uid_t UidType;
#endif

#include <string>
#include <vector>
#include <map>
#include <iosfwd>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/system/Types.hpp>

namespace rstudio {
namespace core {

class FileInfo;

namespace system {

enum LogLevel 
{
   kLogLevelDebug = 0,
   kLogLevelInfo = 1,
   kLogLevelWarning = 2,
   kLogLevelError = 3
};

// portable realPath
Error realPath(const FilePath& filePath, FilePath* pRealPath);
Error realPath(const std::string& path, FilePath* pRealPath);
bool realPathsEqual(const FilePath& a, const FilePath& b);

void addToSystemPath(const FilePath& path, bool prepend = false);

#ifndef _WIN32
Error closeAllFileDescriptors();
Error closeNonStdFileDescriptors();

// retrieves a list of file descriptors opened by the specified child process
// and sends each fd to the child via pipe so that the child can signal-safely
// close the fds
Error closeChildFileDescriptorsFrom(pid_t childPid, int pipeFd, uint32_t fdStart);

// gets the list of open fds for the current process
Error getOpenFds(std::vector<uint32_t>* pFds);

// gets the list of open fds for the specified process
Error getOpenFds(pid_t pid, std::vector<uint32_t>* pFds);

namespace signal_safe {

// thread and signal-safe version of closeNonStdFileDescriptors()
void closeNonStdFileDescriptors(rlim_t fdLimit);

// close file descriptors given to us by our parent process
// must be paired with a call to closeChildFileDescriptorsFrom in the parent
void closeFileDescriptorsFromParent(int pipeFd, uint32_t fdStart, rlim_t fdLimit);

int clearSignalMask();

} // namespace signal_safe

void closeStdFileDescriptors();
void attachStdFileDescriptorsToDevNull();
void setStandardStreamsToDevNull();

// Handles EINTR retrying. Only for use with functions that return -1 on
// error and set errno.
template <typename T>
T posixCall(const boost::function<T()>& func)
{
   const T ERR = -1;

   T result;
   while (true)
   {
      result = func();

      if (result == ERR && errno == EINTR)
         continue;
      else
         break;
   }

   return result;
}

// Handles EINTR retrying and error construction (also optionally returns
// the result as an out parameter). Only for use with functions that return
// -1 on error and set errno.
template <typename T>
Error posixCall(const boost::function<T()>& func,
                       const ErrorLocation& location,
                       T *pResult = nullptr)
{
   const T ERR = -1;

   // make the call
   T result = posixCall<T>(func);

   // set out param (if requested)
   if (pResult)
      *pResult = result;

   // return status
   if (result == ERR)
      return systemError(errno, location);
   else
      return Success();
}

// Handles EINTR retrying and error logging. Only for use with functions
// that return -1 on error and set errno.
template <typename T>
void safePosixCall(const boost::function<T()>& func,
                          const ErrorLocation& location)
{
   Error error = posixCall<T>(func, location, nullptr);
   if (error)
      LOG_ERROR(error);
}

#endif

#ifdef _WIN32

// Is 64-bit Windows?
bool isWin64();

// Is calling process 64-bit?
bool isCurrentProcessWin64();

bool isWin7OrLater();
Error makeFileHidden(const FilePath& path);
Error copyMetafileToClipboard(const FilePath& path);
void ensureLongPath(FilePath* pFilePath);
Error expandEnvironmentVariables(std::string value, std::string* pResult);
FilePath expandComSpec();

// close a handle then set it to NULL (so we can call this function
// repeatedly without failure or other side effects)
Error closeHandle(HANDLE* pHandle, const ErrorLocation& location);

class CloseHandleOnExitScope : boost::noncopyable
{
public:
   CloseHandleOnExitScope(HANDLE* pHandle, const ErrorLocation& location)
      : pHandle_(pHandle), location_(location)
   {
   }

   virtual ~CloseHandleOnExitScope();
   void detach() { pHandle_ = nullptr; }
private:
   HANDLE* pHandle_;
   ErrorLocation location_;
};

// set $HOME to $USERPROFILE
void setHomeToUserProfile(core::system::Options* pChildEnv);

// Folder for per-machine configuration data
FilePath systemSettingsPath(const std::string& appName, bool create);

#endif // WIN32

void initHook();

// initialization
Error initializeSystemLog(const std::string& programIdentity,
                          int logLevel,
                          bool enableConfigReload = true);

Error initializeStderrLog(const std::string& programIdentity,
                          int logLevel,
                          bool enableConfigReload = true);

Error initializeLog(const std::string& programIdentity,
                    int logLevel,
                    const FilePath& logDir,
                    bool enableConfigReload = true);

void initializeLogConfigReload();

// common initialization functions - do not invoke directly
Error initLog();
Error reinitLog();

// exit
int exitFailure(const Error& error, const ErrorLocation& loggedFromLocation);
int exitFailure(const std::string& errMsg,
                const ErrorLocation& loggedFromLocation);
   
// signals 
   
// ignore selected signals
Error ignoreTerminalSignals(); 
Error ignoreChildExits();
   
// reap children (better way to handle child exits than ignoreChildExits
// because it doesn't prevent system from determining exit codes)
Error reapChildren();

 
enum SignalType 
{
   SigInt,
   SigHup,
   SigAbrt,
   SigSegv,
   SigIll,
   SigUsr1,
   SigUsr2,
   SigPipe,
   SigChld,
   SigTerm
};


   
// block all signals for a given scope
class SignalBlocker : boost::noncopyable
{
public:
   SignalBlocker();
   virtual ~SignalBlocker();
   // COPYING: boost::noncopyable
   
   Error block(SignalType signal);
   Error blockAll();
   
private:
   struct Impl;
   boost::shared_ptr<Impl> pImpl_;
};
   
core::Error clearSignalMask();

core::Error handleSignal(SignalType signal, void (*handler)(int));
core::Error ignoreSignal(SignalType signal);   
core::Error useDefaultSignalHandler(SignalType signal);

void sendSignalToSelf(SignalType signal);

// user info
std::string username();
FilePath userHomePath(std::string envOverride = std::string());
FilePath userSettingsPath(const FilePath& userHomeDirectory,
                          const std::string& appName,
                          bool ensureDirectory = true /* create directory */);
unsigned int effectiveUserId();
bool effectiveUserIsRoot();
bool currentUserIsPrivilleged(unsigned int minimumUserId);

// log
void log(LogLevel level,
         const char* message,
         const std::string&logSection = std::string());

void log(LogLevel level,
         const std::string& message,
         const std::string& logSection = std::string());

void log(LogLevel level,
         const boost::function<std::string()>& action,
         const std::string& logSection = std::string());

const char* logLevelToStr(LogLevel level);

LogLevel lowestLogLevel();

// filesystem
bool isHiddenFile(const FilePath& filePath) ;
bool isHiddenFile(const FileInfo& fileInfo) ;
bool isReadOnly(const FilePath& filePath);
   
// terminals
bool stderrIsTerminal();
bool stdoutIsTerminal();

// uuid
std::string generateUuid(bool includeDashes = true);
std::string generateShortenedUuid();

// process info

PidType currentProcessId();
std::string currentProcessPidStr();

Error executablePath(int argc, const char * argv[],
                     FilePath* pExecutablePath);

Error executablePath(const char * argv0,
                     FilePath* pExecutablePath);


Error installPath(const std::string& relativeToExecutable,
                  const char * argv0,
                  FilePath* pInstallationPath);

void fixupExecutablePath(FilePath* pExePath);

void abort();

Error terminateProcess(PidType pid);

struct SubprocInfo
{
   PidType pid;
   std::string exe;
};

// Return list of child processes, by executable filename and pid
std::vector<SubprocInfo> getSubprocesses(PidType pid);

// Get current-working directory of a process; returns empty FilePath
// if unable to determine cwd
FilePath currentWorkingDir(PidType pid);

Error terminateChildProcesses();
   
} // namespace system
} // namespace core 
} // namespace rstudio

#endif // CORE_SYSTEM_SYSTEM_HPP

