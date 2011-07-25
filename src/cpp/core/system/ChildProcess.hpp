/*
 * ChildProcess.hpp
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

#ifndef CORE_SYSTEM_CHILD_PROCESS_HPP
#define CORE_SYSTEM_CHILD_PROCESS_HPP

#include <core/system/Process.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

namespace core {

class ErrorLocation;

namespace system {

// Base class for child processes
class ChildProcess : boost::noncopyable, public ProcessOperations
{
protected:
   ChildProcess(const std::string& exe, const std::vector<std::string>& args);

public:
   virtual ~ChildProcess();

public:
   // write (synchronously) to std input
   virtual Error writeToStdin(const std::string& input, bool eof);

   // terminate the process
   virtual Error terminate();

protected:
   Error run();

protected:
   // platform specific impl
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;

private:
   // command and args
   std::string exe_;
   std::vector<std::string> args_;
};


// Child process which can be run synchronously
class SyncChildProcess : public ChildProcess
{
public:
   SyncChildProcess(const std::string& exe, const std::vector<std::string>& args)
      : ChildProcess(exe, args)
   {
   }

   Error run(const std::string& input, ProcessResult* pResult)
   {
      // run the process
      Error error = ChildProcess::run();
      if (error)
         return error;

      // write input
      error = writeToStdin(input, true);
      if (error)
      {
         Error terminateError = terminate();
         if (terminateError)
            LOG_ERROR(terminateError);
      }

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
   AsyncChildProcess(const std::string& exe, const std::vector<std::string>& args);
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

private:

   void reportError(const Error& error)
   {
      if (callbacks_.onError)
      {
         callbacks_.onError(error);
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

#endif // CORE_SYSTEM_CHILD_PROCESS_HPP
