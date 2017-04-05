/*
 * ChildProcess.hpp
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

#ifndef CORE_SYSTEM_CHILD_PROCESS_HPP
#define CORE_SYSTEM_CHILD_PROCESS_HPP

#include <core/system/Process.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

namespace rstudio {
namespace core {

class ErrorLocation;

namespace system {

// Base class for child processes
class ChildProcess : boost::noncopyable, public ProcessOperations
{
protected:
   ChildProcess();

   // separate init from construction so subclassees can use custom
   // processing to calculate exe and args (e.g. lookup paths or
   // invoke within a command processor)
   void init(const std::string& exe,
             const std::vector<std::string>& args,
             const ProcessOptions& options);

   // init from a command (platform specific)
   void init(const std::string& command,
             const ProcessOptions& options);

   // init for an interactive terminal
   void init(const ProcessOptions& options);

public:
   virtual ~ChildProcess();

public:
   // write (synchronously) to std input
   virtual Error writeToStdin(const std::string& input, bool eof);

   // set the size of the pseudoterminal
   virtual Error ptySetSize(int cols, int rows);

   // interrupt the pseudoterminal
   virtual Error ptyInterrupt();

   // terminate the process
   virtual Error terminate();

   // Does this process have any subprocesses? True if there are
   // subprocesses, if it hasn't been checked yet, or if the process
   // isn't configured to check for subprocesses.
   virtual bool hasSubprocess() const;

   // Has this process generated any recent output?
   virtual bool hasRecentOutput() const;

protected:
   Error run();

   const ProcessOptions& options() const { return options_; }

protected:
   // platform specific impl
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;

private:
   // command and args
   std::string exe_;
   std::vector<std::string> args_;
   ProcessOptions options_;
};


// Child process which can be run synchronously
class SyncChildProcess : public ChildProcess
{
public:
   SyncChildProcess(const std::string& exe,
                    const std::vector<std::string>& args,
                    const ProcessOptions& options)
      : ChildProcess()
   {
      init(exe, args, options);
      if (!options.stdOutFile.empty() || !options.stdErrFile.empty())
      {
         LOG_ERROR_MESSAGE(
                  "stdOutFile/stdErrFile options cannot be used with runProgram");
      }
   }

   SyncChildProcess(const std::string& command,
                    const ProcessOptions& options)
      : ChildProcess()
   {
      init(command, options);
   }

   Error run(const std::string& input, ProcessResult* pResult)
   {
      // sync child processes don't support pseudoterminal mode
      if (options().pseudoterminal)
      {
         return systemError(boost::system::errc::not_supported,
                            ERROR_LOCATION);
      }

      // run the process
      Error error = ChildProcess::run();
      if (error)
         return error;

      // write input
      if (!input.empty())
      {
         error = writeToStdin(input, true);
         if (error)
         {
            Error terminateError = terminate();
            if (terminateError)
               LOG_ERROR(terminateError);
         }
      }

      // bail if we aren't waiting for results
      if (pResult == NULL)
         return Success();

      // read standard out if we didn't have a previous problem
      if (!error)
         error = readStdOut(&(pResult->stdOut));

      // read standard error if we didn't have a previous problem
      if (!error)
         error = readStdErr(&(pResult->stdErr));

      // wait on exit and get exit status. note we always need to do this
      // even if we called terminate due to an earlier error (so we always
      // reap the child)
      Error waitError = waitForExit(&(pResult->exitStatus));
      if (waitError)
      {
         if (!error)
            error = waitError;
         else
            LOG_ERROR(waitError);
      }

      // return error status
      return error;
   }

private:
   Error readStdOut(std::string* pOutput);
   Error readStdErr(std::string* pOutput);
   Error waitForExit(int* pExitStatus);
};


// Child process which can be run asynchronously
class AsyncChildProcess : public ChildProcess
{
public:
   AsyncChildProcess(const std::string& exe,
                     const std::vector<std::string>& args,
                     const ProcessOptions& options);
   AsyncChildProcess(const std::string& command,
                     const ProcessOptions& options);
   AsyncChildProcess(const ProcessOptions& options);
   virtual ~AsyncChildProcess();

   // run process asynchronously
   Error run(const ProcessCallbacks& callbacks)
   {
      Error error = ChildProcess::run();
      if (!error)
      {
         callbacks_ = callbacks;
         return Success();
      }
      else
      {
         return error;
      }
   }

   // poll for input and exit status
   void poll();

   // has it exited?
   bool exited();

   // override of terminate (allow special handling for unix pty termination)
   virtual Error terminate();

   virtual bool hasSubprocess() const;
   virtual bool hasRecentOutput() const;

private:

   void reportError(const Error& error)
   {
      if (callbacks_.onError)
      {
         callbacks_.onError(*this, error);
      }
      else
      {
         LOG_ERROR(error);
         Error termError = terminate();
         if (termError)
            LOG_ERROR(termError);
      }
   }

   void reportIOError(const char* what, const ErrorLocation& location)
   {
      Error error = systemError(boost::system::errc::io_error, location);
      if (what != NULL)
         error.addProperty("what", what);
      reportError(error);
   }

private:
   // callbacks
   ProcessCallbacks callbacks_;

   // platform specific impl
   struct AsyncImpl;
   boost::scoped_ptr<AsyncImpl> pAsyncImpl_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_CHILD_PROCESS_HPP
