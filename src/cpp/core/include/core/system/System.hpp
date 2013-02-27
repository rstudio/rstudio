/*
 * System.hpp
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

#ifndef CORE_SYSTEM_SYSTEM_HPP
#define CORE_SYSTEM_SYSTEM_HPP


#if defined(_WIN32)
#include <windef.h>
typedef DWORD PidType;
#else  // UNIX
#include <sys/types.h>
typedef pid_t PidType;
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

#include <core/system/Types.hpp>

namespace core {

class FileInfo;
class FilePath;

namespace system {

enum LogLevel 
{
   kLogLevelError = 0,
   kLogLevelWarning = 1,
   kLogLevelInfo = 2,
   kLogLevelDebug = 3
};

// portable realPath
Error realPath(const FilePath& filePath, FilePath* pRealPath);

#ifndef _WIN32
Error closeAllFileDescriptors();
Error closeNonStdFileDescriptors();
void closeStdFileDescriptors();
void attachStdFileDescriptorsToDevNull();
void setStandardStreamsToDevNull();
Error realPath(const std::string& path, FilePath* pRealPath);
void addToSystemPath(const FilePath& path, bool prepend = false);


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
                       T *pResult = NULL)
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
   Error error = posixCall<T>(func, location, NULL);
   if (error)
      LOG_ERROR(error);
}

#endif

#ifdef _WIN32
bool isWin64();
bool isVistaOrLater();
Error makeFileHidden(const FilePath& path);
Error copyMetafileToClipboard(const FilePath& path);
void ensureLongPath(FilePath* pFilePath);

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

private:
   HANDLE* pHandle_;
   ErrorLocation location_;
};



#endif

void initHook();
// initialization (not thread safe, call from main thread at app startup)  
void initializeSystemLog(const std::string& programIdentity, int logLevel);
void initializeStderrLog(const std::string& programIdentity, int logLevel);
void initializeLog(const std::string& programIdentity,
                   int logLevel,
                   const FilePath& logDir);

Error setExitFunction(void (*exitFunction) (void));
   
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
   SigChld
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

// user info
std::string username();
FilePath userHomePath(std::string envOverride = std::string());
FilePath userSettingsPath(const FilePath& userHomeDirectory,
                          const std::string& appName);
bool currentUserIsPrivilleged(unsigned int minimumUserId);

// log
void log(LogLevel level, const std::string& message) ;

// filesystem
bool isHiddenFile(const FilePath& filePath) ;
bool isHiddenFile(const FileInfo& fileInfo) ;
   
// terminals
bool stderrIsTerminal();
bool stdoutIsTerminal();

// uuid
std::string generateUuid(bool includeDashes = true);
std::string generateShortenedUuid();

// executable and installation path

Error executablePath(int argc, char * const argv[],
                     FilePath* pExecutablePath);

Error installPath(const std::string& relativeToExecutable,
                  int argc, char * const argv[],
                  FilePath* pInstallationPath);

void fixupExecutablePath(FilePath* pExePath);

void abort();

Error terminateProcess(PidType pid);
   
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_SYSTEM_HPP

