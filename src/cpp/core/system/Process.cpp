/*
 * Process.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/Process.hpp>

#include <iostream>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Scope.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/BoostThread.hpp>

#include <core/PerformanceTimer.hpp>

#include "ChildProcess.hpp"

namespace core {
namespace system {


Error runProgram(const std::string& executable,
                 const std::vector<std::string>& args,
                 const std::string& input,
                 const ProcessOptions& options,
                 ProcessResult* pResult)
{
   SyncChildProcess child(executable, args, options);
   return child.run(input, pResult);
}

Error runCommand(const std::string& command,
                 const ProcessOptions& options,
                 ProcessResult* pResult)
{
   return runCommand(command, "", options, pResult);
}

Error runCommand(const std::string& command,
                 const std::string& input,
                 const ProcessOptions& options,
                 ProcessResult* pResult)
{
   SyncChildProcess child(command, options);
   return child.run(input, pResult);
}


struct ProcessSupervisor::Impl
{
   Impl() : isPolling(false) {}
   bool isPolling;
   std::vector<boost::shared_ptr<AsyncChildProcess> > children;
};

ProcessSupervisor::ProcessSupervisor()
   : pImpl_(new Impl())
{
}

ProcessSupervisor::~ProcessSupervisor()
{
}

namespace {

Error runChild(boost::shared_ptr<AsyncChildProcess> pChild,
               std::vector<boost::shared_ptr<AsyncChildProcess> >* pChildren,
               const ProcessCallbacks& callbacks)
{
   // run the child
   Error error = pChild->run(callbacks);
   if (error)
      return error;

   // add to the list of children
   pChildren->push_back(pChild);

   // success
   return Success();
}

} // anonymous namespace

Error ProcessSupervisor::runProgram(const std::string& executable,
                                    const std::vector<std::string>& args,
                                    const ProcessOptions& options,
                                    const ProcessCallbacks& callbacks)
{
   // create the child
   boost::shared_ptr<AsyncChildProcess> pChild(
                                 new AsyncChildProcess(executable,
                                                       args,
                                                       options));

   // run the child
   return runChild(pChild, &(pImpl_->children), callbacks);
}

Error ProcessSupervisor::runCommand(const std::string& command,
                                    const ProcessOptions& options,
                                    const ProcessCallbacks& callbacks)
{
   // create the child
   boost::shared_ptr<AsyncChildProcess> pChild(
                                 new AsyncChildProcess(command, options));

   // run the child
   return runChild(pChild, &(pImpl_->children), callbacks);
}

namespace {

// class which implements all of the callbacks
struct ChildCallbacks
{
   ChildCallbacks(const std::string& input,
                  const boost::function<void(const ProcessResult&)>& onCompleted,
                  const boost::function<void(const Error&)>& onErrored)
      : input(input), onCompleted(onCompleted), onErrored(onErrored)
   {
   }

   void onStarted(ProcessOperations& operations)
   {
      if (!input.empty())
      {
         Error error = operations.writeToStdin(input, true);
         if (error)
         {
            LOG_ERROR(error);

            error = operations.terminate();
            if (error)
               LOG_ERROR(error);
         }
      }
   }

   void onStdout(ProcessOperations&, const std::string& output)
   {
      stdOut.append(output);
   }

   void onStderr(ProcessOperations&, const std::string& output)
   {
      stdErr.append(output);
   }

   void onError(ProcessOperations&, const Error& error)
   {
      onErrored(error);
   }

   void onConsoleOutputSnapshot(ProcessOperations&,
                                const std::vector<char>& output)
   {
      consoleOutputSnapshot = output;
   }

   void onExit(int exitStatus)
   {
      ProcessResult result;
      result.exitStatus = exitStatus;
      result.stdOut = stdOut;
      result.stdErr = stdErr;
      onCompleted(result);
   }

   std::string input;
   std::string stdOut;
   std::string stdErr;
   std::vector<char> consoleOutputSnapshot;
   boost::function<void(const ProcessResult&)> onCompleted;
   boost::function<void(const Error&)> onErrored;
};

} // anonymous namespace


ProcessCallbacks createProcessCallbacks(
               const std::string& input,
               const boost::function<void(const ProcessResult&)>& onCompleted,
               const boost::function<void(const Error&)>& onError)
{
   // create a shared_ptr to the ChildCallbacks. it will stay alive
   // as long as one of its members is referenced in a bind context
   boost::shared_ptr<ChildCallbacks> pCC(new ChildCallbacks(input,
                                                            onCompleted,
                                                            onError));

   // bind in the callbacks
   using boost::bind;
   ProcessCallbacks cb;
   cb.onStarted = bind(&ChildCallbacks::onStarted, pCC, _1);
   cb.onStdout = bind(&ChildCallbacks::onStdout, pCC, _1, _2);
   cb.onStderr = bind(&ChildCallbacks::onStderr, pCC, _1, _2);
   cb.onConsoleOutputSnapshot =
         bind(&ChildCallbacks::onConsoleOutputSnapshot, pCC, _1, _2);
   cb.onExit = bind(&ChildCallbacks::onExit, pCC, _1);
   cb.onError = bind(&ChildCallbacks::onError, pCC, _1, _2);

   // return it
   return cb;
}


Error ProcessSupervisor::runProgram(
            const std::string& executable,
            const std::vector<std::string>& args,
            const std::string& input,
            const ProcessOptions& options,
            const boost::function<void(const ProcessResult&)>& onCompleted)
{
   // create proces callbacks
   ProcessCallbacks cb = createProcessCallbacks(
            input, onCompleted, boost::function<void(const Error&)>());

   // run the child
   return runProgram(executable, args, options, cb);
}

Error ProcessSupervisor::runCommand(
             const std::string& command,
             const ProcessOptions& options,
             const boost::function<void(const ProcessResult&)>& onCompleted)
{
   return runCommand(command, "", options, onCompleted);
}

Error ProcessSupervisor::runCommand(
             const std::string& command,
             const std::string& input,
             const ProcessOptions& options,
             const boost::function<void(const ProcessResult&)>& onCompleted)
{
   // create proces callbacks
   ProcessCallbacks cb = createProcessCallbacks(input, onCompleted);

   // run the child
   return runCommand(command, options, cb);
}



bool ProcessSupervisor::hasRunningChildren()
{
   return !pImpl_->children.empty();
}

bool ProcessSupervisor::poll()
{
   // bail immediately if we have no children
   if (!hasRunningChildren())
      return false;

   // never allow re-entrancy (could occur if one of the output
   // handlers called from poll executes a waitForMethod which
   // results in additional polling during idle/wait time)
   if (pImpl_->isPolling)
      return true;

   // set isPolling then clear it on exit
   pImpl_->isPolling = true;
   scope::SetOnExit<bool> setOnExit(&pImpl_->isPolling, false);

   // call poll on all of our children
   std::for_each(pImpl_->children.begin(),
                 pImpl_->children.end(),
                 boost::bind(&AsyncChildProcess::poll, _1));

   // remove any children who have exited from our list
   pImpl_->children.erase(std::remove_if(
                             pImpl_->children.begin(),
                             pImpl_->children.end(),
                             boost::bind(&AsyncChildProcess::exited, _1)),
                          pImpl_->children.end());

   // return status
   return hasRunningChildren();
}

void ProcessSupervisor::terminateAll()
{
   // call terminate on all of our children
   BOOST_FOREACH(boost::shared_ptr<AsyncChildProcess> pChild,
                 pImpl_->children)
   {
      Error error = pChild->terminate();
      if (error)
         LOG_ERROR(error);
   }
}

bool ProcessSupervisor::wait(
      const boost::posix_time::time_duration& pollingInterval,
      const boost::posix_time::time_duration& maxWait)
{
   boost::posix_time::ptime timeoutTime(boost::posix_time::not_a_date_time);
   if (!maxWait.is_not_a_date_time())
      timeoutTime = boost::get_system_time() + maxWait;

   while (poll())
   {
      // wait the specified polling interval
      boost::this_thread::sleep(pollingInterval);

      // check for timeout if appropriate
      if (!timeoutTime.is_not_a_date_time())
      {
         if (boost::get_system_time() > timeoutTime)
            return false;
      }
   }

   return true;
}

} // namespace system
} // namespace core


