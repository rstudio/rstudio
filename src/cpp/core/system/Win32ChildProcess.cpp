/*
 * Win32ChildProcess.cpp
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

#include "ChildProcess.hpp"
#include "ChildProcessSubprocPoll.hpp"
#include "Win32Pty.hpp"

#include <iostream>

#include <windows.h>
#include <Shlwapi.h>

#include <boost/foreach.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/system/System.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>

#include "CriticalSection.hpp"

namespace rstudio {
namespace core {
namespace system {

namespace {

const boost::posix_time::milliseconds kResetRecentDelay =
                                         boost::posix_time::milliseconds(1000);
const boost::posix_time::milliseconds kCheckSubprocDelay =
                                         boost::posix_time::milliseconds(200);

std::string findOnPath(const std::string& exe,
                       const std::string& appendExt = "")
{
   // make sure it has the specified extension
   std::string resolvedExe = exe;
   if (!appendExt.empty() &&
       !boost::algorithm::ends_with(resolvedExe, appendExt))
   {
      resolvedExe += appendExt;
   }

   // do the search
   std::vector<TCHAR> exeBuffer(MAX_PATH*4);
   exeBuffer.insert(exeBuffer.begin(), resolvedExe.begin(), resolvedExe.end());
   exeBuffer.push_back('\0');
   if (::PathFindOnPath(&(exeBuffer[0]), NULL))
   {
      return std::string(&(exeBuffer[0]));
   }
   else
   {
      return std::string();
   }
}

// resolve the passed command and arguments to the form required for a
// call to CreateProcess (do path lookup if necessary and invoke the
// command within a command processor if it is a batch file)
void resolveCommand(std::string* pExecutable, std::vector<std::string>* pArgs)
{
   // if this is a root path or it exists then leave it as is
   if (!FilePath::isRootPath(*pExecutable) && !FilePath::exists(*pExecutable))
   {
      // try to find it on the path as a .exe
      std::string exePath = findOnPath(*pExecutable, ".exe");
      if (!exePath.empty())
      {
         *pExecutable = exePath;
      }
      else
      {
         // try to find it on the path as a cmd
         std::string cmdPath = findOnPath(*pExecutable, ".cmd");
         if (!cmdPath.empty())
         {
            // set the pCmd to cmd.exe
            std::string cmdExePath = findOnPath("cmd.exe");
            if (!cmdExePath.empty())
            {
               // set to cmd.exe
               *pExecutable = cmdExePath;

               // manipulate args to have cmd.exe invoke the batch file
               pArgs->insert(pArgs->begin(), cmdPath);
               pArgs->insert(pArgs->begin(), "/C");

            }
         }
      }
   }
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
        closeProcess_(&hProcess, ERROR_LOCATION),
        pid(static_cast<PidType>(-1)),
        ctrlC(0x03)
   {
   }

   HANDLE hStdInWrite;
   HANDLE hStdOutRead;
   HANDLE hStdErrRead;
   HANDLE hProcess;
   PidType pid;
   WinPty pty;
   char ctrlC;

private:
   CloseHandleOnExitScope closeStdIn_;
   CloseHandleOnExitScope closeStdOut_;
   CloseHandleOnExitScope closeStdErr_;
   CloseHandleOnExitScope closeProcess_;
};


ChildProcess::ChildProcess()
  : pImpl_(new Impl())
{
}


void ChildProcess::init(const std::string& exe,
                        const std::vector<std::string>& args,
                        const ProcessOptions& options)
{
   exe_ = exe;
   args_ = args;
   options_ = options;
   resolveCommand(&exe_, &args_);

   if (!options.stdOutFile.empty() || !options.stdErrFile.empty())
   {
      LOG_ERROR_MESSAGE(
               "stdOutFile/stdErrFile options cannot be used with runProgram");
   }
}

void ChildProcess::init(const std::string& command,
                        const ProcessOptions& options)
{
   exe_ = findOnPath("cmd.exe");
   args_.push_back("/S");
   args_.push_back("/C");
   args_.push_back("\"" + command + "\"");
   options_ = options;
}

// initialize for an interactive terminal
void ChildProcess::init(const ProcessOptions& options)
{
   options_ = options;
   exe_ = options_.shellPath.absolutePathNative();
   args_ = options_.args;
}

ChildProcess::~ChildProcess()
{
}

Error ChildProcess::writeToStdin(const std::string& input, bool eof)
{
   // write synchronously to the pipe
   if (!input.empty())
   {
      if (options().pseudoterminal)
      {
         Error error = WinPty::writeToPty(pImpl_->hStdInWrite, input);
         if (error)
            return error;
      }
      else
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
   }

   // close pipe if requested
   if (eof)
      return closeHandle(&pImpl_->hStdInWrite, ERROR_LOCATION);
   else
      return Success();
}

Error ChildProcess::ptySetSize(int cols, int rows)
{
   // verify we are dealing with a pseudoterminal
   if (!options().pseudoterminal)
      return systemError(boost::system::errc::not_supported, ERROR_LOCATION);

   return pImpl_->pty.setSize(cols, rows);
}

Error ChildProcess::ptyInterrupt()
{
   // verify we are dealing with a pseudoterminal
   if (!options().pseudoterminal)
      return systemError(boost::system::errc::not_supported, ERROR_LOCATION);

   return pImpl_->pty.interrupt();
}

Error ChildProcess::terminate()
{
   // terminate with exit code 15 (15 is SIGTERM on posix)
   if (!::TerminateProcess(pImpl_->hProcess, 15))
      return systemError(::GetLastError(), ERROR_LOCATION);
   else
      return Success();
}

bool ChildProcess::hasSubprocess() const
{
   // base class doesn't support subprocess-checking; override to implement
   return true;
}

bool ChildProcess::hasRecentOutput() const
{
   // base class doesn't support output activity detection; override to implement
   return true;
}

namespace {

Error openFile(const FilePath& file, bool inheritable, HANDLE* phFile)
{
   HANDLE hFile = ::CreateFileW(file.absolutePathW().c_str(),
                                GENERIC_WRITE,
                                0,
                                NULL,
                                CREATE_ALWAYS,
                                FILE_ATTRIBUTE_NORMAL,
                                NULL);
   if (hFile == INVALID_HANDLE_VALUE)
      return systemError(::GetLastError(), ERROR_LOCATION);

   if (inheritable)
   {
      if (!::SetHandleInformation(hFile,
                                  HANDLE_FLAG_INHERIT,
                                  HANDLE_FLAG_INHERIT))
      {
         Error err = systemError(::GetLastError(), ERROR_LOCATION);
         ::CloseHandle(hFile);
         return err;
      }
   }

   *phFile = hFile;

   return Success();
}

} // namespace

Error ChildProcess::run()
{   
   Error error;

   // NOTE: if the run method is called from multiple threads in single app
   // concurrently then a race condition can cause handles to get incorrectly
   // directed. the workaround suggested by microsoft is to wrap the process
   // creation code in a critical section. see this article for details:
   //   http://support.microsoft.com/kb/315939
   static CriticalSection s_runCriticalSection;
   CriticalSection::Scope csScope(s_runCriticalSection);

   // pseudoterminal mode: use winpty to emulate Posix pseudoterminal
   if (options_.pseudoterminal)
   {
      error = pImpl_->pty.start(exe_, args_, options_,
                               &pImpl_->hStdInWrite,
                               &pImpl_->hStdOutRead,
                               &pImpl_->hStdErrRead,
                               &pImpl_->hProcess);
      if (!error)
      {
         pImpl_->pid = ::GetProcessId(pImpl_->hProcess);
      }
      return error;
   }

   // Standard input pipe
   HANDLE hStdInRead;
   if (!::CreatePipe(&hStdInRead, &pImpl_->hStdInWrite, NULL, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   CloseHandleOnExitScope closeStdIn(&hStdInRead, ERROR_LOCATION);
   if (!::SetHandleInformation(hStdInRead,
                               HANDLE_FLAG_INHERIT,
                               HANDLE_FLAG_INHERIT))
      return systemError(::GetLastError(), ERROR_LOCATION);

   // Standard output pipe
   HANDLE hStdOutWrite;
   if (!::CreatePipe(&pImpl_->hStdOutRead, &hStdOutWrite, NULL, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   CloseHandleOnExitScope closeStdOut(&hStdOutWrite, ERROR_LOCATION);
   if (!::SetHandleInformation(hStdOutWrite,
                               HANDLE_FLAG_INHERIT,
                               HANDLE_FLAG_INHERIT) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // Standard error pipe
   HANDLE hStdErrWrite;
   if (!::CreatePipe(&pImpl_->hStdErrRead, &hStdErrWrite, NULL, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);
   CloseHandleOnExitScope closeStdErr(&hStdErrWrite, ERROR_LOCATION);
   if (!::SetHandleInformation(hStdErrWrite,
                               HANDLE_FLAG_INHERIT,
                               HANDLE_FLAG_INHERIT) )
      return systemError(::GetLastError(), ERROR_LOCATION);

   // populate startup info
   STARTUPINFO si = { sizeof(STARTUPINFO) };
   si.dwFlags |= STARTF_USESTDHANDLES;
   si.hStdOutput = hStdOutWrite;
   si.hStdError = options_.redirectStdErrToStdOut ? hStdOutWrite
                                                  : hStdErrWrite;
   si.hStdInput = hStdInRead;

   HANDLE hStdOutWriteFile = INVALID_HANDLE_VALUE;
   if (!options_.stdOutFile.empty())
   {
      error = openFile(options_.stdOutFile, true, &hStdOutWriteFile);
      if (error)
         return error;
      si.hStdOutput = hStdOutWriteFile;
   }
   CloseHandleOnExitScope closeStdOutFile(&hStdOutWriteFile, ERROR_LOCATION);

   HANDLE hStdErrWriteFile = INVALID_HANDLE_VALUE;
   if (!options_.stdErrFile.empty())
   {
      error = openFile(options_.stdErrFile, true, &hStdErrWriteFile);
      if (error)
         return error;
      si.hStdOutput = hStdErrWriteFile;
   }
   CloseHandleOnExitScope closeStdErrFile(&hStdErrWriteFile, ERROR_LOCATION);

   // build command line
   std::vector<TCHAR> cmdLine;

   bool exeQuot = std::string::npos != exe_.find(' ')
         && std::string::npos == exe_.find('"');
   if (exeQuot)
      cmdLine.push_back('"');
   std::copy(exe_.begin(), exe_.end(), std::back_inserter(cmdLine));
   if (exeQuot)
      cmdLine.push_back('"');

   BOOST_FOREACH(std::string& arg, args_)
   {
      cmdLine.push_back(' ');

      // This is kind of gross. Ideally we would be more deterministic
      // than this.
      bool quot = std::string::npos != arg.find(' ')
            && std::string::npos == arg.find('"');

      if (quot)
         cmdLine.push_back('"');
      std::copy(arg.begin(), arg.end(), std::back_inserter(cmdLine));
      if (quot)
         cmdLine.push_back('"');
   }
   cmdLine.push_back('\0');

   // specify custom environment if requested
   DWORD dwFlags = 0;
   LPVOID lpEnv = NULL;
   std::vector<wchar_t> envBlock;
   if (options_.environment)
   {
      const Options& env = options_.environment.get();
      BOOST_FOREACH(const Option& envVar, env)
      {
         std::wstring key = string_utils::utf8ToWide(envVar.first);
         std::wstring value = string_utils::utf8ToWide(envVar.second);
         std::copy(key.begin(), key.end(), std::back_inserter(envBlock));
         envBlock.push_back(L'=');
         std::copy(value.begin(), value.end(), std::back_inserter(envBlock));
         envBlock.push_back(L'\0');
      }
      envBlock.push_back(L'\0');

      dwFlags |= CREATE_UNICODE_ENVIRONMENT;
      lpEnv = &envBlock[0];
   }

   if (options_.createNewConsole)
   {
      dwFlags |= CREATE_NEW_CONSOLE;
      si.dwFlags |= STARTF_USESHOWWINDOW;
      si.wShowWindow = SW_HIDE;
   }
   else if (options_.detachProcess)
   {
      dwFlags |= DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP;
      si.dwFlags |= STARTF_USESHOWWINDOW;
      si.wShowWindow = SW_HIDE;
   }

   if (options_.breakawayFromJob)
      dwFlags |= CREATE_BREAKAWAY_FROM_JOB;

   std::string workingDir;
   if (!options_.workingDir.empty())
   {
      workingDir = string_utils::utf8ToSystem(
            options_.workingDir.absolutePathNative());
   }

   // Start the child process.
   PROCESS_INFORMATION pi;
   ::ZeroMemory( &pi, sizeof(PROCESS_INFORMATION));
   BOOL success = ::CreateProcess(
     exe_.c_str(),    // Process
     &(cmdLine[0]),   // Command line
     NULL,            // Process handle not inheritable
     NULL,            // Thread handle not inheritable
     TRUE,            // Set handle inheritance to TRUE
     dwFlags,         // Creation flags
     lpEnv,           // Environment block
                      // Use parent's starting directory
     workingDir.empty() ? NULL : workingDir.c_str(),
     &si,             // Pointer to STARTUPINFO structure
     &pi );   // Pointer to PROCESS_INFORMATION structure

   if (!success)
      return systemError(::GetLastError(), ERROR_LOCATION);

   // close thread handle on exit
   CloseHandleOnExitScope closeThread(&pi.hThread, ERROR_LOCATION);

   // save handle to process
   pImpl_->hProcess = pi.hProcess;
   pImpl_->pid = ::GetProcessId(pImpl_->hProcess);

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
      : calledOnStarted_(false),
        exited_(false)
   {
   }

   bool calledOnStarted_;
   bool exited_;
   boost::scoped_ptr<ChildProcessSubprocPoll> pSubprocPoll_;
 };

AsyncChildProcess::AsyncChildProcess(const std::string& exe,
                                     const std::vector<std::string>& args,
                                     const ProcessOptions& options)
   : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(exe, args, options);
}

AsyncChildProcess::AsyncChildProcess(const std::string& command,
                                     const ProcessOptions& options)
   : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(command, options);
}

AsyncChildProcess::AsyncChildProcess(const ProcessOptions& options)
      : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(options);
}

AsyncChildProcess::~AsyncChildProcess()
{
}


Error AsyncChildProcess::terminate()
{
   return ChildProcess::terminate();
}

bool AsyncChildProcess::hasSubprocess() const
{
   if (pAsyncImpl_->pSubprocPoll_)
      return pAsyncImpl_->pSubprocPoll_->hasSubprocess();
   else
      return true;
}

bool AsyncChildProcess::hasRecentOutput() const
{
   if (pAsyncImpl_->pSubprocPoll_)
      return pAsyncImpl_->pSubprocPoll_->hasRecentOutput();
   else
      return true;
}

void AsyncChildProcess::poll()
{
   // call onStarted if we haven't yet
   if (!(pAsyncImpl_->calledOnStarted_))
   {
      // setup for subprocess polling
      pAsyncImpl_->pSubprocPoll_.reset(new ChildProcessSubprocPoll(
         pImpl_->pid,
         kResetRecentDelay, kCheckSubprocDelay,
         options().reportHasSubprocs ? core::system::hasSubprocesses : NULL));

      if (callbacks_.onStarted)
         callbacks_.onStarted(*this);
      pAsyncImpl_->calledOnStarted_ = true;
   }

   // call onContinue
   if (callbacks_.onContinue)
   {
      if (!callbacks_.onContinue(*this))
      {
         // terminate the proces
         Error error = terminate();
         if (error)
            LOG_ERROR(error);
      }
   }

   bool hasRecentOutput = false;

   // check stdout
   std::string stdOut;
   Error error = WinPty::readFromPty(pImpl_->hStdOutRead, &stdOut);
   if (error)
      reportError(error);
   if (!stdOut.empty() && callbacks_.onStdout)
      callbacks_.onStdout(*this, stdOut);

   // check stderr
   // when using winpty, hStdErrRead is optional
   if (pImpl_->hStdErrRead)
   {
      std::string stdErr;
      error = WinPty::readFromPty(pImpl_->hStdErrRead, &stdErr);
      if (error)
         reportError(error);
      if (!stdErr.empty() && callbacks_.onStderr)
      {
         hasRecentOutput = true;
         callbacks_.onStderr(*this, stdErr);
      }
   }

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

      // set exited_ flag so that our exited function always
      // returns the right value
      pAsyncImpl_->exited_ = true;
      pAsyncImpl_->pSubprocPoll_->stop();
   }

   // Perform optional periodic operations
   if (pAsyncImpl_->pSubprocPoll_->poll(hasRecentOutput))
   {
      if (callbacks_.onHasSubprocs)
      {
         callbacks_.onHasSubprocs(hasSubprocess());
      }
   }
}

bool AsyncChildProcess::exited()
{
   return pImpl_->hProcess == NULL;
}

} // namespace system
} // namespace core
} // namespace rstudio


