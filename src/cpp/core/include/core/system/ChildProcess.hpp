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

#include <vector>

#include <boost/utility.hpp>
#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>

namespace core {

class Error;

namespace system {

class ChildProcess
{
public:
   // write (synchronously) to std input
   virtual Error writeToStdin(const std::string& input, bool eof) = 0;

   // terminate the process
   virtual Error terminate() = 0;
};

struct ProcessInput
{
   ProcessInput(const std::string& data = "", bool eof = true)
      : data(data), eof(eof)
   {
   }

   std::string data;
   bool eof;
};

struct ProcessCallbacks
{
   boost::function<void(ChildProcess&, const std::string&)> onStdout;
   boost::function<void(ChildProcess&, const std::string&)> onStderr;
   boost::function<void(int status)> onExit;
   boost::function<void(const Error&)> onError;
};

class ChildProcessSupervisor : boost::noncopyable
{
public:
   ChildProcessSupervisor();
   virtual ~ChildProcessSupervisor();

   // run the child
   Error runChild(const std::string& command,
                  const std::vector<std::string>& args,
                  const ProcessInput& input,
                  const ProcessCallbacks& callbacks);

   // poll for child (output and exit) events. returns true if there
   // are still children being supervised after the poll
   bool poll();

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace system
} // namespace core

#endif // CORE_SYSTEM_CHILD_PROCESS_HPP
