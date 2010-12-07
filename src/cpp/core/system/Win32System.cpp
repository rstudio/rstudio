/*
 * Win32System.cpp
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
#include <stdlib.h>
#include <io.h>

#include <iostream>
#include <sstream>
#include <vector>
#include <algorithm>

#include <windows.h>
#include <shlobj.h>

#include <boost/foreach.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Log.hpp>
#include <core/LogWriter.hpp>
#include <core/Error.hpp>
#include <core/FileLogWriter.hpp>
#include <core/FilePath.hpp>
#include <core/DateTime.hpp>
#include <core/StringUtils.hpp>

namespace core {
namespace system {

namespace {
LogWriter* s_pLogWriter = NULL;
}

void initializeSystemLog(const std::string& programIdentity, int logLevel)
{
}

void initializeLog(const std::string& programIdentity, int logLevel, const FilePath& settingsDir)
{
   if (s_pLogWriter)
      delete s_pLogWriter;

   s_pLogWriter = new FileLogWriter(programIdentity, logLevel, settingsDir);
}

void log(LogLevel logLevel, const std::string& message)
{
   if (s_pLogWriter)
      s_pLogWriter->log(logLevel, message);
}

bool isWin64()
{
   return !getenv("PROCESSOR_ARCHITEW6432").empty()
         || getenv("PROCESSOR_ARCHITECTURE") == "AMD64";
}

std::string getenv(const std::string& name)
{
   // get the variable
   DWORD nSize = 256;
   std::vector<TCHAR> buffer(nSize);
   DWORD result = ::GetEnvironmentVariable(name.c_str(), &(buffer[0]), nSize);
   if (result == 0) // not found
   {
      return std::string();
   }
   if (result > nSize) // not enough space in buffer
   {
      nSize = result;
      buffer.resize(nSize);
      result = ::GetEnvironmentVariable(name.c_str(), &(buffer[0]), nSize);
      if (result == 0 || result > nSize)
         return std::string(); // VERY unexpected failure case
   }

   // return it
   return std::string(&(buffer[0]));
}

void setenv(const std::string& name, const std::string& value)
{
   ::SetEnvironmentVariable(name.c_str(), value.c_str());
}

void unsetenv(const std::string& name)
{
   ::SetEnvironmentVariable(name.c_str(), NULL);
}

std::string username()
{
   return system::getenv("USERNAME");
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

   // query for My Documents directory
   const DWORD SHGFP_TYPE_CURRENT = 0;
   TCHAR homePath[MAX_PATH];
   HRESULT hr = ::SHGetFolderPath(NULL,
                                  CSIDL_PERSONAL,
                                  NULL,
                                  SHGFP_TYPE_CURRENT,
                                  homePath);
   if (SUCCEEDED(hr))
   {
      return FilePath(homePath);
   }
   else
   {
      LOG_ERROR_MESSAGE("Unable to retreive user home path. HRESULT:  " +
                        boost::lexical_cast<std::string>(hr));
      return FilePath();
   }
}

FilePath userSettingsPath(const FilePath& userHomeDirectory,
                          const std::string& appName)
{
   char path[MAX_PATH];
   HRESULT hr = ::SHGetFolderPathAndSubDir(
         NULL,
         CSIDL_LOCAL_APPDATA | CSIDL_FLAG_CREATE,
         NULL,
         SHGFP_TYPE_CURRENT,
         appName.c_str(),
         path);

   if (hr != S_OK)
   {
      LOG_ERROR_MESSAGE("Unable to retreive user home path. HRESULT:  " +
                        boost::lexical_cast<std::string>(hr));
      return FilePath();
   }

   return FilePath(path);
}

bool currentUserIsPrivilleged(unsigned int minimumUserId)
{
   return false;
}

Error executeInterruptableChildProcess(
           std::string path,
           Options args,
           int checkContinueIntervalMs,
           const boost::function<bool()>& checkContinueFunction)
{
   // see: http://msdn.microsoft.com/en-us/library/ms682512(VS.85).aspx

   // build command line
   std::vector<TCHAR> cmdLine;
   BOOST_FOREACH(Option& arg, args)
   {
      cmdLine.push_back(' ');
      std::copy(arg.first.begin(),
                arg.first.end(),
                std::back_inserter(cmdLine));
      cmdLine.push_back(' ');
      std::copy(arg.second.begin(),
                arg.second.end(),
                std::back_inserter(cmdLine));
   }
   cmdLine.push_back('\0');

   // setup structures
   STARTUPINFO si;
   ::ZeroMemory( &si, sizeof(si) );
   si.cb = sizeof(si);
   PROCESS_INFORMATION pi;
   ::ZeroMemory( &pi, sizeof(pi) );

   // Start the child process.
   BOOL success = ::CreateProcess(
     path.c_str(),    // Process
     &(cmdLine[0]),   // Command line
     NULL,            // Process handle not inheritable
     NULL,            // Thread handle not inheritable
     FALSE,           // Set handle inheritance to FALSE
     0,               // No creation flags
     NULL,            // Use parent's environment block
     NULL,            // Use parent's starting directory
     &si,             // Pointer to STARTUPINFO structure
     &pi );           // Pointer to PROCESS_INFORMATION structure

   if (!success)
      return systemError(::GetLastError(), ERROR_LOCATION);

   // Wait until child process exits
   Error error;
   while(true)
   {
      // wait specified interval
      DWORD result = ::WaitForSingleObject(pi.hProcess,checkContinueIntervalMs);

      // process still running
      if (result == WAIT_TIMEOUT)
      {
         // check for whether we should continue
         if (!checkContinueFunction())
         {
            // force abend
            if (!::TerminateProcess(pi.hProcess, 0))
               error = systemError(::GetLastError(), ERROR_LOCATION);

            break;
         }
         else
         {
            // keep waiting
            continue;
         }
      }

      // process terminated normally
      else if (result == WAIT_OBJECT_0)
      {
         break;
      }

      // unexpected error
      else if (result == WAIT_FAILED)
      {
         error = systemError(::GetLastError(), ERROR_LOCATION);
         break;
      }

      // unexpected condition
      else
      {
         using namespace boost::system;
         error_code ec(errc::result_out_of_range, get_system_category());
         error = Error(ec, ERROR_LOCATION);
         error.addProperty("result", result);
         break;
      }
   }

   // Close process and thread handles.
   ::CloseHandle( pi.hProcess );
   ::CloseHandle( pi.hThread );

   // return status
   return error;
}

Error captureCommand(const std::string& command, std::string* pOutput)
{
   // WIN32 popen docs:
   // http://msdn.microsoft.com/en-us/library/96ayss4b(VS.80).aspx

   // NOTE: note that popen only works from win32 console applications!

   // start process
   FILE* fp = ::_popen(command.c_str(), "r");
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
   if (::_pclose(fp) == -1)
   {
      // log existing error before overwriting it
      if (error)
         LOG_ERROR(error);

      error = systemError(errno, ERROR_LOCATION);
   }

   // return status
   return error;
}

bool isHiddenFile(const FilePath& filePath)
{
   DWORD attribs = ::GetFileAttributes(filePath.absolutePath().c_str());
   if (attribs == INVALID_FILE_ATTRIBUTES)
      return false;
   else if (attribs & FILE_ATTRIBUTE_HIDDEN)
      return true;
   else
      return false;
}

bool stderrIsTerminal()
{
   return _isatty(_fileno(stderr));
}

bool stdoutIsTerminal()
{
   return _isatty(_fileno(stdout));
}

// uuid
std::string generateUuid(bool includeDashes)
{
   // create the uuid
   UUID uuid = {0};
   ::UuidCreate(&uuid);
   PUCHAR pChar = NULL;
   ::UuidToStringA(&uuid, &pChar);
   std::string uuidStr((char*)pChar);
   ::RpcStringFreeA(&pChar);

   // remove dashes if requested
   if (!includeDashes)
      boost::algorithm::replace_all(uuidStr, "-", "");

   // return
   return uuidStr;
}

// installation path
Error installPath(const std::string& relativeToExecutable,
                  int argc, char * const argv[],
                  FilePath* pInstallationPath)
{
   // get full executable path
   FilePath exePath(_pgmptr);

   // resolve to install path using given relative path
   if (relativeToExecutable == "..") // common case
     *pInstallationPath = exePath.parent().parent();
   else
     *pInstallationPath = exePath.parent().complete(relativeToExecutable);

   return Success();
}

void fixupExecutablePath(FilePath* pExePath)
{
   if (pExePath->extension().empty())
     *pExePath = pExePath->parent().complete(pExePath->filename() + ".exe");
}

void abort()
{
   ::exit(1);
}

 
////////////////////////////////////////////////////////////////////////////
//
//  No signals on Win32 so all of these are no-ops
//
//


Error ignoreTerminalSignals()
{
   return Success();
}
      
Error ignoreChildExits()
{
   return Success();
}
     
Error reapChildren()
{
   return Success();
}
   
struct SignalBlocker::Impl
{
};
   
SignalBlocker::SignalBlocker()
   : pImpl_(new Impl())
{
}
   
   
Error SignalBlocker::block(SignalType signal)
{
   return Success();
}

Error SignalBlocker::blockAll()
{
   return Success();
}
      
SignalBlocker::~SignalBlocker()
{
   try
   {
   }
   catch(...)
   {
   }
}
   
Error clearSignalMask()
{
   return Success();
}
   
Error handleSignal(SignalType signal, void (*handler)(int))
{
  return Success();
}
   
core::Error ignoreSignal(SignalType signal)
{
   return Success();
}   


Error useDefaultSignalHandler(SignalType signal)
{
   return Success();
}
} // namespace system
} // namespace core

