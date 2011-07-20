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

namespace core {

class Error;
class ErrorLocation;

namespace system {
     
class ChildProcess : boost::noncopyable, public ProcessOperations
{
public:
   ChildProcess(const std::string& cmd, const std::vector<std::string>& args);
   virtual ~ChildProcess();

   // run process
   Error run(const ProcessCallbacks& callbacks);

   // has it exited?
   bool exited();

   // write (synchronously) to std input
   virtual Error writeToStdin(const std::string& input, bool eof);

   // terminate the process
   virtual Error terminate();

   // poll for input and exit status
   void poll();

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

