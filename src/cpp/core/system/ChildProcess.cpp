/*
 * ChildProcess.cpp
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

#include <core/system/ChildProcess.hpp>

#include <iostream>

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include "ChildProcessImpl.hpp"

namespace core {
namespace system {

void ChildProcessImpl::setCallbacks(const ProcessCallbacks& callbacks)
{
   callbacks_ = callbacks;
}

void ChildProcessImpl::reportError(const Error& error)
{
   if (callbacks_.onError)
      callbacks_.onError(error);
   else
      LOG_ERROR(error);
}

void ChildProcessImpl::reportIOError(const char* what,
                                      const ErrorLocation& location)
{
   Error error = systemError(boost::system::errc::io_error, location);
   if (what != NULL)
      error.addProperty("what", what);
   reportError(error);
}

void ChildProcessImpl::reportIOError(const ErrorLocation& location)
{
   reportIOError(NULL, location);
}


namespace {

void childPoll(boost::shared_ptr<ChildProcessImpl> pChild)
{
   pChild->poll();
}

bool childNotRunning(boost::shared_ptr<ChildProcessImpl> pChild)
{
   return !pChild->isRunning();
}

} // anonymous namespace

struct ChildProcessSupervisor::Impl
{
   std::vector<boost::shared_ptr<ChildProcessImpl> > children;
};

ChildProcessSupervisor::ChildProcessSupervisor()
   : pImpl_(new Impl())
{
}

ChildProcessSupervisor::~ChildProcessSupervisor()
{
}

Error ChildProcessSupervisor::runChild(const std::string& command,
                                       const std::vector<std::string>& args,
                                       const ProcessInput& input,
                                       const ProcessCallbacks& callbacks)
{
   // create the child
   boost::shared_ptr<ChildProcessImpl> pChild(new ChildProcessImpl(command,
                                                                   args));
   // run the child
   Error error = pChild->run();
   if (error)
      return error;

   // send input if requested
   if (!input.data.empty())
   {
      error = pChild->writeToStdin(input.data, input.eof);
      if (error)
      {
         Error terminateError = pChild->terminate();
         if (terminateError)
            LOG_ERROR(error);

         return error;
      }
   }

   // connect callbacks
   pChild->setCallbacks(callbacks);

   // add to our list of children
   pImpl_->children.push_back(pChild);

   // success
   return Success();
}


bool ChildProcessSupervisor::poll()
{
   // call poll on all of our children
   std::for_each(pImpl_->children.begin(), pImpl_->children.end(), childPoll);

   // remove any children who have exited from our list
   pImpl_->children.erase(std::remove_if(pImpl_->children.begin(),
                                         pImpl_->children.end(),
                                         childNotRunning),
                          pImpl_->children.end());

   // return status
   return !pImpl_->children.empty();
}


} // namespace system
} // namespace core


