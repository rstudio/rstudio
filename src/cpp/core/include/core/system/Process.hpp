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

////////////////////////////////////////////////////////////////////////////////
//
// Run child process synchronously
//
//

// Struct for returning output and exit status from a process
struct ProcessResult
{
   ProcessResult() : exitStatus(-1) {}

   // Standard output from process
   std::string stdOut;

   // Standard error from process
   std::string stdErr;

   // Process exit status. Potential values:
   //   0   - successful execution
   //   1   - application defined failure code (1, 2, 3, etc.)
   //  15   - process killed by terminate()
   //  -1   - unable to determine exit status
   int exitStatus;
};


// Run a process synchronously. Note that if executable is not an absolute
// path then runProcess will duplicate the actions of the shell in searching
// for an executable to run. Some platform specific notes:
//
//  - Posix: The executable path is not executed by /bin/sh, rather it is
//    executed directly by ::execvp. This means that shell metacharacters
//    (e.g. stream redirection, piping, etc.) are not supported in the
//    command string.
//
//  - Win32: The search for the executable path includes auto-appending .exe
//    and .cmd (in that order) for the path search and invoking cmd.exe if
//    the target is a batch (.cmd) file.
//
// Note that it would be straightforward to execute using /bin/sh on
// Posix (and thus support shell metacharacters, etc.) however these
// would be non-portable constructs so we leave those semantics out of
// this portable API layer. If desired, this behavior can manually achieved
// on Poix by passing "/bin/sh" as the executable and including -c as the
// first value in args.
//
Error runProcess(const std::string& executable,
                 const std::vector<std::string>& args,
                 const std::string& input,
                 ProcessResult* pResult);


////////////////////////////////////////////////////////////////////////////////
//
// ProcessSupervisor -- run child processes asynchronously
//
// Any number of processes can be run by calling runAsync and their results will
// be delivered using the provided callbacks. Note that the poll() method must
// be called periodically (e.g. during standard event pumping / idle time) in
// order to check for output & status of managed children.
//
// If you want to pair a call to runAsync with an object which will live for the
// lifetime of the child process you should create a shared_ptr to that object
// and then bind the applicable members to the callback function(s) -- the bind
// will keep the shared_ptr alive (see the implementation of the single-callback
// version of runAsync for an example)
//
//

// Operations that can be performed from within ProcessCallbacks
class ProcessOperations
{
public:
   // Write (synchronously) to standard input
   virtual Error writeToStdin(const std::string& input, bool eof) = 0;

   // Terminate the process (SIGTERM)
   virtual Error terminate() = 0;
};

// Callbacks for reporting various states and streaming output (note that
// all callbacks are optional)
struct ProcessCallbacks
{
   // Called after the process begins running (note: is called during
   // the first call to poll and therefore after runAsync returns). Can
   // be used for writing initial standard input to the child
   boost::function<void(ProcessOperations&)> onStarted;

   // Called periodically (at whatever interval poll is called) during the
   // lifetime of the child process (will not be called until after the
   // first call to onStarted)
   boost::function<void(ProcessOperations&)> onRunning;

   // Streaming callback for standard output
   boost::function<void(ProcessOperations&, const std::string&)> onStdout;

   // Streaming callback for standard error
   boost::function<void(ProcessOperations&, const std::string&)> onStderr;

   // Called if an IO error occurs while reading from standard streams. The
   // default behavior if no callback is specified is to log and then terminate
   // the child (which will result in onExit being called w/ exitStatus == 15)
   boost::function<void(const Error&)> onError;

   // Called after the process has exited. Passes exitStatus (see ProcessResult
   // comment above for potential values)
   boost::function<void(int)> onExit;
};


// Process supervisor
class ProcessSupervisor : boost::noncopyable
{
public:
   ProcessSupervisor();
   virtual ~ProcessSupervisor();

   // Run a child asynchronously, invoking callbacks as the process starts,
   // produces output, and exits. Output callbacks are streamed/interleaved,
   // but note that output is collected at a polling interval so it is
   // possible that e.g. two writes to standard output which had an
   // intervening write to standard input might still be concatenated. See
   // comment on runProcess above for the semantics of the "executable"
   // argument.
   Error runAsync(const std::string& executable,
                  const std::vector<std::string>& args,
                  const ProcessCallbacks& callbacks);

   // Run a child asynchronously, invoking the completed callback when the
   // process exits. Note that if input is provided then then the standard
   // input stream is closed (so EOF is sent) after the input is written.
   // Note also that the standard error handler (log and terminate) is also
   // used. If you want more customized behavior then you can use the more
   // granular runAsync call above. See comment on runProcess above for the
   // semantics of the "command" argument.
   Error runAsync(const std::string& executable,
                  const std::vector<std::string>& args,
                  const std::string& input,
                  const boost::function<void(const ProcessResult&)>& completed);


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
