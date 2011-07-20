/*
 * Process.hpp
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


#ifndef CORE_SYSTEM_PROCESS_HPP
#define CORE_SYSTEM_PROCESS_HPP

#include <vector>

#include <boost/utility.hpp>
#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>

namespace core {

class Error;

namespace system {

struct ProcessResult
{
   ProcessResult() : exitStatus(-1) {}

   // Standard output from process
   std::string stdOut;

   // Standard error from process
   std::string stdErr;

   // Process exit status (exit code if the child was successfully reaped
   // otherwise -1)
   int exitStatus;
};

class ProcessOperations
{
public:
   // Write (synchronously) to standard input
   virtual Error writeToStdin(const std::string& input, bool eof) = 0;

   // Terminate the process (SIGTERM)
   virtual Error terminate() = 0;
};

struct ProcessCallbacks
{
   // Called after the process begins running (note: is called during
   // the first call to poll and therefore after runAsync returns). Can
   // be used for writing initial standard input to the child
   boost::function<void(ProcessOperations&)> onStarted;

   // Streaming callback for standard output
   boost::function<void(ProcessOperations&, const std::string&)> onStdout;

   // Streaming callback for standard error
   boost::function<void(ProcessOperations&, const std::string&)> onStderr;

   // Called after the process has exited. Note that if the child cannot
   // be sucessfully reaped (e.g. if there is a global SIGCHLD handler
   // which automatically reaps all children) then exitStatus is -1
   boost::function<void(int)> onExit;
};


// Class for running processes asynchronously. Any number of processes
// can be run by calling runAsync and their results will be delivered
// using the provided callbacks.
class ProcessSupervisor : boost::noncopyable
{
public:
   ProcessSupervisor();
   virtual ~ProcessSupervisor();

   // Run a child asynchronously, invoking callbacks as the process starts,
   // produces output, and exits. Output callbacks are streamed/interleaved,
   // but note that output is collected at a polling interval so it is
   // possible that e.g. two writes to standard output which had an
   // intervening write to standard input might still be concatenated.
   Error runAsync(
            const std::string& command,
            const std::vector<std::string>& args,
            const ProcessCallbacks& callbacks);

   // Run a child asynchronously, invoking the onCompleted callback when
   // the process exits. Note that if input is provided then then the
   // standard input stream is closed (so EOF is sent) after the input
   // is written (if you want more customized input handling then you
   // can use the more granular runAsync call above)
   Error runAsync(
            const std::string& command,
            const std::vector<std::string>& args,
            const std::string& input,
            const boost::function<void(const ProcessResult&)>& onCompleted);


   // Poll for child (output and exit) events. returns true if there
   // are still children being supervised after the poll
   bool poll();

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace system
} // namespace core

#endif // CORE_SYSTEM_PROCESS_HPP
