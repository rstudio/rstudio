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

// TODO: test locating of executables

// TODO: note on PeekNamedPipe blocking in multithreaded app with
// other blocking call to ReadFile active

// TODO: consider a peek, read, sleep loop to avoid global read block

// TODO: kb article on race condition in creating processes

namespace core {
namespace system {

namespace {

// close a handle then set it to NULL (so we can call this function
// repeatedly without failure or other side effects)
Error closeHandle(HANDLE* pHandle, const ErrorLocation& location)
{
   if (*pHandle != NULL)
   {
      BOOL result = ::CloseHandle(*pHandle);
      *pHandle = NULL;

      if (!result)
         return systemError(::GetLastError(), location);
      else
         return Success();
   }
   else
   {
      return Success();
   }
}

class CloseHandleOnExitScope
{
public:
   CloseHandleOnExitScope(HANDLE* pHandle, const ErrorLocation& location)
      : pHandle_(pHandle), location_(location)
   {
   }

   virtual ~CloseHandleOnExitScope()
   {
      try
      {
         Error error = closeHandle(pHandle_, location_);
         if (error)
            LOG_ERROR(error);
      }
      catch(...)
      {
      }
   }

private:
   HANDLE* pHandle_;
   ErrorLocation location_;
};


Error readPipeAvailableBytes(HANDLE hPipe, std::string* pOutput)
{
   // check for available bytes
   DWORD dwAvail = 0;
   if (!::PeekNamedPipe(hPipe, NULL, 0, NULL, &dwAvail, NULL))
   {
      if (::GetLastError() == ERROR_BROKEN_PIPE)
         return Success();
      else
         return systemError(::GetLastError(), ERROR_LOCATION);
   }

   // no data available
   if (dwAvail == 0)
      return Success();

   // read data which is available
   DWORD nBytesRead;
   std::vector<CHAR> buffer(dwAvail, 0);
   if (!::ReadFile(hPipe, &(buffer[0]), dwAvail, &nBytesRead, NULL))
      return systemError(::GetLastError(), ERROR_LOCATION);

   // append to output
   pOutput->append(&(buffer[0]), nBytesRead);

   // success
   return Success();
}

Error readPipeUntilDone(HANDLE hPipe, std::string* pOutput)
{
   CHAR buff[256];
   DWORD nBytesRead;

   while(TRUE)
   {
      // read from pipe
      BOOL result = ::ReadFile(hPipe, buff, sizeof(buff), &nBytesRead, NULL);

      // end of file
      if (nBytesRead == 0)
         break;

      // pipe broken
      else if (!result && (::GetLastError() == ERROR_BROKEN_PIPE))
         break;

      // unexpected error
      else if (!result)
         return systemError(::GetLastError(), ERROR_LOCATION);

      // got input, append it
      else
         pOutput->append(buff, nBytesRead);
   }

   return Success();
}

} // anonymous namespace

struct ChildProcess::Impl
{
   Impl()
      : hStdInWrite(NULL),
        hStdOutRead(NULL),
        hStdErrRead(NULL),
        hProcess(NULL),
        closeStdIn_(&hStdInWrite, ERROR_LOCATION),
        closeStdOut_(&hStdOutRead, ERROR_LOCATION),
        closeStdErr_(&hStdErrRead, ERROR_LOCATION),
        closeProcess_(&hProcess, ERROR_LOCATION)
   {
   }

   HANDLE hStdInWrite;
   HANDLE hStdOutRead;
   HANDLE hStdErrRead;
   HANDLE hProcess;

private:
   CloseHandleOnExitScope closeStdIn_;
   CloseHandleOnExitScope closeStdOut_;
   CloseHandleOnExitScope closeStdErr_;
   CloseHandleOnExitScope closeProcess_;
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
   if (!input.empty())
   {
      DWORD dwWritten;
      BOOL bSuccess = ::WriteFile(pImpl_->hStdInWrite,
                                  input.data(),
                                  input.length(),
                                  &dwWritten,
                                  NULL);
      if (!bSuccess)
         return systemError(::GetLastError(), ERROR_LOCATION);
   }

   // close pipe if requested
   if (eof)
      return closeHandle(&pImpl_->hStdInWrite, ERROR_LOCATION);
   else
      return Success();
}


Error ChildProcess::terminate()
{
   // terminate with exit code 15 (15 is SIGTERM on posix)
   if (!::TerminateProcess(pImpl_->hProcess, 15))
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
   HANDLE hStdInRead;
   if (!::CreatePipe(&hStdInRead, &pImpl_->hStdInWrite, &sa, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   CloseHandleOnExitScope closeStdIn(&hStdInRead, ERROR_LOCATION);
   if (!::SetHandleInformation(pImpl_->hStdInWrite, HANDLE_FLAG_INHERIT, 0) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // Standard output pipe
   HANDLE hStdOutWrite;
   if (!::CreatePipe(&pImpl_->hStdOutRead, &hStdOutWrite, &sa, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   CloseHandleOnExitScope closeStdOut(&hStdOutWrite, ERROR_LOCATION);
   if (!::SetHandleInformation(pImpl_->hStdOutRead, HANDLE_FLAG_INHERIT, 0) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // Standard error pipe
   HANDLE hStdErrWrite;
   if (!::CreatePipe(&pImpl_->hStdErrRead, &hStdErrWrite, &sa, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   CloseHandleOnExitScope closeStdErr(&hStdErrWrite, ERROR_LOCATION);
   if (!::SetHandleInformation(pImpl_->hStdErrRead, HANDLE_FLAG_INHERIT, 0) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // populate startup info
   STARTUPINFO si;
   ZeroMemory(&si,sizeof(STARTUPINFO));
   si.cb = sizeof(STARTUPINFO);
   si.dwFlags |= STARTF_USESTDHANDLES;
   si.hStdOutput = hStdOutWrite;
   si.hStdError = hStdErrWrite;
   si.hStdInput = hStdInRead;

   // build command line
   std::vector<TCHAR> cmdLine;
   BOOST_FOREACH(std::string& arg, args_)
   {
      cmdLine.push_back(' ');
      std::copy(arg.begin(), arg.end(), std::back_inserter(cmdLine));
   }
   cmdLine.push_back('\0');

   // Start the child process.
   PROCESS_INFORMATION pi;
   ::ZeroMemory( &pi, sizeof(PROCESS_INFORMATION));
   BOOL success = ::CreateProcess(
     cmd_.c_str(),    // Process
     &(cmdLine[0]),   // Command line
     NULL,            // Process handle not inheritable
     NULL,            // Thread handle not inheritable
     TRUE,            // Set handle inheritance to TRUE
     0,               // No creation flags
     NULL,            // Use parent's environment block
     NULL,            // Use parent's starting directory
     &si,             // Pointer to STARTUPINFO structure
     &pi );   // Pointer to PROCESS_INFORMATION structure

   if (!success)
      return systemError(::GetLastError(), ERROR_LOCATION);

   // close thread handle on exit
   CloseHandleOnExitScope closeThread(&pi.hThread, ERROR_LOCATION);

   // save handle to process
   pImpl_->hProcess = pi.hProcess;

   // success
   return Success();
}


Error SyncChildProcess::readStdOut(std::string* pOutput)
{  
   return readPipeUntilDone(pImpl_->hStdOutRead, pOutput);
}

Error SyncChildProcess::readStdErr(std::string* pOutput)
{
  return readPipeUntilDone(pImpl_->hStdErrRead, pOutput);
}

Error SyncChildProcess::waitForExit(int* pExitStatus)
{
   // wait
   DWORD result = ::WaitForSingleObject(pImpl_->hProcess, INFINITE);

   // check for error
   if (result != WAIT_OBJECT_0)
   {
      if (result == WAIT_FAILED)
      {
         return systemError(::GetLastError(), ERROR_LOCATION);
      }
      else
      {
         Error error = systemError(boost::system::errc::result_out_of_range,
                                   ERROR_LOCATION);
         error.addProperty("result", result);
         return error;
      }
   }
   else
   {
      // get exit code
      DWORD dwStatus;
      if (!::GetExitCodeProcess(pImpl_->hProcess, &dwStatus))
         return systemError(::GetLastError(), ERROR_LOCATION);

      *pExitStatus = dwStatus;
      return Success();
   }
}

struct AsyncChildProcess::AsyncImpl
{
   AsyncImpl()
      : calledOnStarted_(false)
   {
   }

   bool calledOnStarted_;
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
   // call onStarted if we haven't yet
   if (!(pAsyncImpl_->calledOnStarted_))
   {
      if (callbacks_.onStarted)
         callbacks_.onStarted(*this);
      pAsyncImpl_->calledOnStarted_ = true;
   }

   // call onRunning
   if (callbacks_.onRunning)
      callbacks_.onRunning(*this);

   // check stdout
   std::string stdOut;
   Error error = readPipeAvailableBytes(pImpl_->hStdOutRead, &stdOut);
   if (error)
      reportError(error);
   if (!stdOut.empty() && callbacks_.onStdout)
      callbacks_.onStdout(*this, stdOut);

   // check stderr
   std::string stdErr;
   error = readPipeAvailableBytes(pImpl_->hStdErrRead, &stdErr);
   if (error)
      reportError(error);
   if (!stdErr.empty() && callbacks_.onStderr)
      callbacks_.onStderr(*this, stdErr);

   // check for process exit
   DWORD result = ::WaitForSingleObject(pImpl_->hProcess, 0);

   // check for process exit (or error waiting)
   if (result != WAIT_TIMEOUT)
   {
      // try to get exit status
      int exitStatus = -1;

      // normal wait for process state
      if (result == WAIT_OBJECT_0)
      {
         // get the exit status
         DWORD dwStatus;
         if (!::GetExitCodeProcess(pImpl_->hProcess, &dwStatus))
            LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
         exitStatus = dwStatus;
      }

      // error state, return -1 and try to log a meaningful error
      else
      {
         Error error;
         if (result == WAIT_FAILED)
         {
            error = systemError(::GetLastError(), ERROR_LOCATION);
         }
         else
         {
            error = systemError(boost::system::errc::result_out_of_range,
                                ERROR_LOCATION);
            error.addProperty("result", result);
         }
         LOG_ERROR(error);
      }

      // close the process handle
      Error error = closeHandle(&pImpl_->hProcess, ERROR_LOCATION);
      if (error)
         LOG_ERROR(error);

      // call onExit
      if (callbacks_.onExit)
         callbacks_.onExit(exitStatus);
   }
}

bool AsyncChildProcess::exited()
{
   return pImpl_->hProcess == NULL;
}

} // namespace system
} // namespace core


