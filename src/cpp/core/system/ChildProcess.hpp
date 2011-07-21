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

#include <core/system/Process.hpp>

#include <core/Error.hpp>

namespace core {

class ErrorLocation;

namespace system {
     
class ChildProcess : boost::noncopyable, public ProcessOperations
{
public:
   ChildProcess(const std::string& cmd, const std::vector<std::string>& args);
   virtual ~ChildProcess();

   // run process synchronously
   Error run(const std::string& input, ProcessResult* pResult);

   // run process asynchronously
   Error runAsync(const ProcessCallbacks& callbacks);

   // poll for input and exit status
   void poll();

   // has it exited?
   bool exited();

   // write (synchronously) to std input
   virtual Error writeToStdin(const std::string& input, bool eof);

   // terminate the process (note that the implementation of this function
   // must never call reportError since it in turn calls terminate)
   virtual Error terminate();

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
   // command and args
   std::string cmd_;
   std::vector<std::string> args_;

   // callbacks
   ProcessCallbacks callbacks_;

   // platform specific impl
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};


} // namespace system
} // namespace core

