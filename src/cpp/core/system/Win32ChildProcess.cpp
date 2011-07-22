/*
 * Win32ChildProcess.cpp
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

#include "ChildProcess.hpp"

#include <windows.h>

#include <boost/foreach.hpp>

namespace core {
namespace system {

namespace {

// close a handle then set it to NULL (so we can call this function
// repeatedly without failure or other side effects)
Error safeCloseHandle(HANDLE* pHandle)
{
   Error error;

   if (*pHandle != NULL)
   {
      if (!::CloseHandle(*pHandle))
         error = systemError(::GetLastError(), ERROR_LOCATION);

      *pHandle = NULL;
   }

   return error;
}

} // anonymous namespace

struct ChildProcess::Impl
{
   Impl()
      : hStdInRead(NULL),
        hStdInWrite(NULL),
        hStdOutRead(NULL),
        hStdOutWrite(NULL),
        hStdErrRead(NULL),
        hStdErrWrite(NULL)
   {
      ::ZeroMemory( &pi, sizeof(PROCESS_INFORMATION));
   }

   virtual ~Impl()
   {
      try
      {
         safeCloseHandle(&hStdInRead);
         safeCloseHandle(&hStdInWrite);
         safeCloseHandle(&hStdOutRead);
         safeCloseHandle(&hStdOutWrite);
         safeCloseHandle(&hStdErrRead);
         safeCloseHandle(&hStdErrWrite);
         safeCloseHandle(&pi.hProcess);
         safeCloseHandle(&pi.hThread);
      }
      catch(...)
      {
      }
   }

   HANDLE hStdInRead;
   HANDLE hStdInWrite;
   HANDLE hStdOutRead;
   HANDLE hStdOutWrite;
   HANDLE hStdErrRead;
   HANDLE hStdErrWrite;
   PROCESS_INFORMATION pi;
};


ChildProcess::ChildProcess(const std::string& cmd,
                           const std::vector<std::string>& args)
  : pImpl_(new Impl()), cmd_(cmd), args_(args)
{
}

ChildProcess::~ChildProcess()
{
}

Error ChildProcess::writeToStdin(const std::string& input, bool eof)
{
   // write synchronously to the pipe
   DWORD dwWritten;
   BOOL bSuccess = ::WriteFile(pImpl_->hStdInWrite,
                               input.data(),
                               input.length(),
                               &dwWritten,
                               NULL);
   if (!bSuccess)
      return systemError(::GetLastError(), ERROR_LOCATION);

   // close pipe if requested
   Error error;
   if (eof)
      error = safeCloseHandle(&pImpl_->hStdInWrite);

   return error;
}


Error ChildProcess::terminate()
{
   if (!::TerminateProcess(pImpl_->pi.hProcess, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   else
      return Success();
}


Error ChildProcess::run()
{
   SECURITY_ATTRIBUTES sa;
   sa.nLength = sizeof(SECURITY_ATTRIBUTES);
   sa.bInheritHandle = TRUE;
   sa.lpSecurityDescriptor = NULL;

   // Standard input pipe
   if (!::CreatePipe(&pImpl_->hStdInRead, &pImpl_->hStdInWrite, &sa, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   if (!::SetHandleInformation(pImpl_->hStdInWrite, HANDLE_FLAG_INHERIT, 0) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // Standard output pipe
   if (!::CreatePipe(&pImpl_->hStdOutRead, &pImpl_->hStdOutWrite, &sa, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   if (!::SetHandleInformation(pImpl_->hStdOutRead, HANDLE_FLAG_INHERIT, 0) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // Standard error pipe
   if (!::CreatePipe(&pImpl_->hStdErrRead, &pImpl_->hStdErrWrite, &sa, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   if (!::SetHandleInformation(pImpl_->hStdErrRead, HANDLE_FLAG_INHERIT, 0) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // populate startup info
   STARTUPINFO si;
   ZeroMemory(&si,sizeof(STARTUPINFO));
   si.cb = sizeof(STARTUPINFO);
   si.dwFlags |= STARTF_USESTDHANDLES;
   si.hStdOutput = pImpl_->hStdOutWrite;
   si.hStdError = pImpl_->hStdErrWrite;
   si.hStdInput = pImpl_->hStdInRead;

   // build command line
   std::vector<TCHAR> cmdLine;
   BOOST_FOREACH(std::string& arg, args_)
   {
      cmdLine.push_back(' ');
      std::copy(arg.begin(), arg.end(), std::back_inserter(cmdLine));
   }
   cmdLine.push_back('\0');

   // Start the child process.
   BOOL success = ::CreateProcess(
     cmd_.c_str(),    // Process
     &(cmdLine[0]),   // Command line
     NULL,            // Process handle not inheritable
     NULL,            // Thread handle not inheritable
     TRUE,            // Set handle inheritance to TRUE
     CREATE_NO_WINDOW,// No console widnow
     NULL,            // Use parent's environment block
     NULL,            // Use parent's starting directory
     &si,             // Pointer to STARTUPINFO structure
     &pImpl_->pi );   // Pointer to PROCESS_INFORMATION structure

   if (!success)
      return systemError(::GetLastError(), ERROR_LOCATION);
   else
      return Success();
}


Error SyncChildProcess::readStdOut(std::string* pOutput)
{
   return Success();
}

Error SyncChildProcess::readStdErr(std::string* pOutput)
{
  return Success();
}

void SyncChildProcess::waitForExit(int* pExitStatus)
{

}

struct AsyncChildProcess::AsyncImpl
{
};

AsyncChildProcess::AsyncChildProcess(const std::string& cmd,
                                     const std::vector<std::string>& args)
   : ChildProcess(cmd, args), pAsyncImpl_(new AsyncImpl())
{
}

AsyncChildProcess::~AsyncChildProcess()
{
}

void AsyncChildProcess::poll()
{

}

bool AsyncChildProcess::exited()
{
   return true;
}

} // namespace system
} // namespace core


