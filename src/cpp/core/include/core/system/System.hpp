/*
 * System.hpp
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

#ifndef CORE_SYSTEM_SYSTEM_HPP
#define CORE_SYSTEM_SYSTEM_HPP

#include <string>
#include <vector>
#include <map>
#include <iosfwd>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>

#include <core/Error.hpp>

namespace core {

class FilePath ;

namespace system {

enum LogLevel 
{
   kLogLevelError = 0,
   kLogLevelWarning = 1,
   kLogLevelInfo = 2,
   kLogLevelDebug = 3
};

#ifndef _WIN32
Error realPath(const std::string& path, FilePath* pRealPath);
void addToSystemPath(const FilePath& path, bool prepend = false);
#endif

#ifdef _WIN32
bool isWin64();
#endif

void initHook();
// initialization (not thread safe, call from main thread at app startup)  
void initializeSystemLog(const std::string& programIdentity, int logLevel);
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
   SigPipe
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

   
// environment
std::string getenv(const std::string& name);
void setenv(const std::string& name, const std::string& value);   
void unsetenv(const std::string& name);

// user info
std::string username();
FilePath userHomePath(const std::string& envOverride = std::string());
FilePath userSettingsPath(const FilePath& userHomeDirectory,
                          const std::string& appName);
bool currentUserIsPrivilleged(unsigned int minimumUserId);
   
typedef std::pair<std::string,std::string> Option;
typedef std::vector<Option> Options;

Error executeInterruptableChildProcess(
           std::string path,
           Options args,
           int checkContinueIntervalMs,
           const boost::function<bool()>& checkContinueFunction);

Error captureCommand(const std::string& command, std::string* pOutput);


// log
void log(LogLevel level, const std::string& message) ;

// filesystem
bool isHiddenFile(const FilePath& filePath) ;
   
// terminals
bool stderrIsTerminal();
bool stdoutIsTerminal();

// uuid
std::string generateUuid(bool includeDashes = true);

// installation path
Error installPath(const std::string& relativeToExecutable,
                  int argc, char * const argv[],
                  FilePath* pInstallationPath);

void fixupExecutablePath(FilePath* pExePath);

void abort();
   
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_SYSTEM_HPP

