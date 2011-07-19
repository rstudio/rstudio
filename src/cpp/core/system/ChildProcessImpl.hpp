/*
 * ChildProcessImpl.hpp
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

namespace core {

class Error;
class ErrorLocation;

namespace system {
     
class ChildProcessImpl : boost::noncopyable, public ChildProcess
{
public:
   ChildProcessImpl(const std::string& cmd,
                    const std::vector<std::string>& args);
   virtual ~ChildProcessImpl();

   // run process
   Error run();

   // set callbacks
   void setCallbacks(const ProcessCallbacks& callbacks);

   // check running status
   bool isRunning();

   // write (synchronously) to std input
   virtual Error writeToStdin(const std::string& input, bool eof);

   // terminate the process
   virtual Error terminate();

   // poll for input and exit status
   void poll();

// private helpers
private:
   void reportError(const Error& error);
   void reportIOError(const char* what, const ErrorLocation& location);
   void reportIOError(const ErrorLocation& location);

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

