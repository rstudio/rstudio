/*
 * ProcessSupervisor.cpp
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

#include <core/system/Process.hpp>

#include <iostream>

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/PerformanceTimer.hpp>

#include "ChildProcess.hpp"

namespace core {
namespace system {


Error runProcess(const std::string& command,
                 const std::vector<std::string>& args,
                 const std::string& input,
                 ProcessResult* pResult)
{
   SyncChildProcess child(command, args);
   return child.run(input, pResult);
}


struct ProcessSupervisor::Impl
{
   std::vector<boost::shared_ptr<AsyncChildProcess> > children;
};

ProcessSupervisor::ProcessSupervisor()
   : pImpl_(new Impl())
{
}

ProcessSupervisor::~ProcessSupervisor()
{
}

Error ProcessSupervisor::runAsync(const std::string& cmd,
                                  const std::vector<std::string>& args,
                                  const ProcessCallbacks& callbacks)
{
   // create the child
   boost::shared_ptr<AsyncChildProcess> pChild(new AsyncChildProcess(cmd, args));

   // run the child
   Error error = pChild->run(callbacks);
   if (error)
      return error;

   // add to our list of children
   pImpl_->children.push_back(pChild);

   // success
   return Success();
}

namespace {

// class which implements all of the callbacks
struct ChildCallbacks
{
   ChildCallbacks(const std::string& input,
                  const boost::function<void(const ProcessResult&)>& onCompleted)
      : input(input), onCompleted(onCompleted)
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
   boost::function<void(const ProcessResult&)> onCompleted;
};


} // anonymous namespace


Error ProcessSupervisor::runAsync(
                  const std::string& command,
                  const std::vector<std::string>& args,
                  const std::string& input,
                  const boost::function<void(const ProcessResult&)>& onCompleted)
{
   // create a shared_ptr to the ChildCallbacks. it will stay alive
   // as long as one of its members is referenced in a bind context
   boost::shared_ptr<ChildCallbacks> pCC(new ChildCallbacks(input, onCompleted));

   // bind in the callbacks
   using boost::bind;
   ProcessCallbacks cb;
   cb.onStarted = bind(&ChildCallbacks::onStarted, pCC, _1);
   cb.onStdout = bind(&ChildCallbacks::onStdout, pCC, _1, _2);
   cb.onStderr = bind(&ChildCallbacks::onStderr, pCC, _1, _2);
   cb.onExit = bind(&ChildCallbacks::onExit, pCC, _1);

   // run the child
   return runAsync(command, args, cb);
}


bool ProcessSupervisor::poll()
{
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
   return !pImpl_->children.empty();
}


} // namespace system
} // namespace core


