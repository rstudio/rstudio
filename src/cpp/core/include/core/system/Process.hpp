/*
 * Process.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#ifndef CORE_SYSTEM_PROCESS_HPP
#define CORE_SYSTEM_PROCESS_HPP

#include <vector>

#include <boost/enable_shared_from_this.hpp>
#include <boost/optional.hpp>
#include <boost/utility.hpp>
#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/date_time/posix_time/posix_time_types.hpp>

#include <core/system/Types.hpp>
#include <core/FilePath.hpp>

namespace rstudio {
namespace core {

class Error;

namespace system {

extern const char* const kSmartTerm;
extern const char* const kDumbTerm;

////////////////////////////////////////////////////////////////////////////////
//
// Run child process synchronously
//
//

// Struct for specifying pseudoterminal options
struct Pseudoterminal
{
   Pseudoterminal(
#ifdef _WIN32
         const FilePath& winptyPath,
         bool plainText,
         bool conerr,
#endif
         int cols, int rows)
      :
#ifdef _WIN32
        winptyPath(winptyPath),
        plainText(plainText),
        conerr(conerr),
#endif
        cols(cols), rows(rows)
   {
   }
#ifdef _WIN32
   FilePath winptyPath;
   bool plainText;
   bool conerr;
#endif
   int cols;
   int rows;
};

// Struct for specifying process options
struct ProcessOptions
{
   ProcessOptions()
#ifdef _WIN32
      : terminateChildren(false),
        smartTerminal(false),
        detachProcess(false),
        createNewConsole(false),
        breakawayFromJob(false),
        cols(80),
        rows(25),
        redirectStdErrToStdOut(false),
        reportHasSubprocs(false)
#else
      : terminateChildren(false),
        smartTerminal(false),
        detachSession(false),
        cols(80),
        rows(25),
        redirectStdErrToStdOut(false),
        reportHasSubprocs(false)
#endif
   {
   }

   // environment variables to set for the child process
   // if you want to simply merge in some additional environment
   // variables you can use the helper functions in Environment.hpp
   // to derive the desired environment
   boost::optional<Options> environment;

   // terminate should also terminate all children owned by the process
   // NOTE: currently only supported on posix -- in the posix case this
   // results in a call to ::setpgid(0,0) to create a new process group
   // and the specification of -pid to kill so as to kill the child and
   // all of its subprocesses
   // NOTE: to support the same behavior on Win32 we'll need to use
   // CreateJobObject/CREATE_BREAKAWAY_FROM_JOB to get the same effect
   bool terminateChildren;

   // Use kSmartTerm as terminal type and disable canonical line-by-line
   // I/O processing
   bool smartTerminal;
   
#ifndef _WIN32
   // Calls ::setsid after fork for POSIX (no effect on Windows)
   bool detachSession;
#endif

   // attach the child process to pseudoterminal pipes
   boost::optional<Pseudoterminal> pseudoterminal;

   // pseudoterminal size
   int cols;
   int rows;
   
#ifdef _WIN32
   // Creates the process with DETACHED_PROCESS
   bool detachProcess;

   // Creates the process with CREATE_NEW_CONSOLE but with the console hidden
   bool createNewConsole;

   // create the process with CREATE_BREAKAWAY_FROM_JOB
   bool breakawayFromJob;
#endif

   // interactive terminal shell
   FilePath shellPath;

   // interactive terminal shell arguments
   std::vector<std::string> args;

   bool redirectStdErrToStdOut;

   // Periodically report if process has any child processes
   bool reportHasSubprocs;

   // If not empty, these two provide paths that stdout and stderr
   // (respectively) should be redirected to. Note that this ONLY works
   // if you use runCommand, not runProgram, as we use the shell to do
   // the redirection.
   core::FilePath stdOutFile;
   core::FilePath stdErrFile;

   // function to run within the child process immediately after the fork
   // NOTE: only supported on posix as there is no fork on Win32
   boost::function<void()> onAfterFork;

   core::FilePath workingDir;
};

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


// Run a program synchronously. Note that if executable is not an absolute
// path then runProgram will duplicate the actions of the shell in searching
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
Error runProgram(const std::string& executable,
                 const std::vector<std::string>& args,
                 const std::string& input,
                 const ProcessOptions& options,
                 ProcessResult* pResult);

// Run a command synchronously. The command will be passed to and executed
// by a command shell (/bin/sh on posix, cmd.exe on windows).
//
Error runCommand(const std::string& command,
                 const ProcessOptions& options,
                 ProcessResult* pResult);

Error runCommand(const std::string& command,
                 const std::string& input,
                 const ProcessOptions& options,
                 ProcessResult* pResult);


////////////////////////////////////////////////////////////////////////////////
//
// ProcessSupervisor -- run child processes asynchronously
//
// Any number of processes can be run by calling runProgram or runCommand and
// their results will be delivered using the provided callbacks. Note that
// the poll() method must be called periodically (e.g. during standard event
// pumping / idle time) in  order to check for output & status of children.
//
// If you want to pair a call to runProgam or runCommand with an object which
// will live for the lifetime of the child process you should create a
// shared_ptr to that object and then bind the applicable members to the
// callback function(s) -- the bind will keep the shared_ptr alive (see the
// implementation of the single-callback version of runProgram for an example)
//
//

// Operations that can be performed from within ProcessCallbacks
class ProcessOperations : public boost::enable_shared_from_this<ProcessOperations>
{
public:
   virtual ~ProcessOperations() {}

   // Write (synchronously) to standard input
   virtual Error writeToStdin(const std::string& input, bool eof) = 0;

   // Operations which apply to Pseudoterminals (only available
   // if ProcessOptions::pseudoterminal is specified)
   virtual Error ptySetSize(int cols, int rows) = 0;
   virtual Error ptyInterrupt() = 0;

   // Terminate the process (SIGTERM)
   virtual Error terminate() = 0;

   boost::weak_ptr<ProcessOperations> getWeakPtr()
   {
      return weak_from_this();
   }
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
   // first call to onStarted). If it returns false then the child process
   // is terminated.
   boost::function<bool(ProcessOperations&)> onContinue;

   // Streaming callback for standard output
   boost::function<void(ProcessOperations&, const std::string&)> onStdout;

   // Streaming callback for standard error
   boost::function<void(ProcessOperations&, const std::string&)> onStderr;

   boost::function<void(ProcessOperations&, const std::vector<char>&)>
                                                  onConsoleOutputSnapshot;

   // Called if an IO error occurs while reading from standard streams. The
   // default behavior if no callback is specified is to log and then terminate
   // the child (which will result in onExit being called w/ exitStatus == 15)
   boost::function<void(ProcessOperations&,const Error&)> onError;

   // Called after the process has exited. Passes exitStatus (see ProcessResult
   // comment above for potential values)
   boost::function<void(int)> onExit;

   // Called periodically to report if this process has subprocesses
   boost::function<void(bool)> onHasSubprocs;
};

ProcessCallbacks createProcessCallbacks(
               const std::string& input,
               const boost::function<void(const ProcessResult&)>& onCompleted,
               const boost::function<void(const Error&)>& onError=
                                  boost::function<void(const core::Error&)>());

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
   // comment on runProgram above for the semantics of the "executable"
   // argument.
   Error runProgram(const std::string& executable,
                    const std::vector<std::string>& args,
                    const ProcessOptions& options,
                    const ProcessCallbacks& callbacks);

   // Run a command asynchronously (same as above but uses a command shell
   // rather than running the executable directly)
   Error runCommand(const std::string& command,
                    const ProcessOptions& options,
                    const ProcessCallbacks& callbacks);

   // Run an interactive terminal asynchronously (same as above but uses
   // platform-specific implementation to determine what to execute).
   Error runTerminal(const ProcessOptions& options,
                     const ProcessCallbacks& callbacks);

   // Run a child asynchronously, invoking the completed callback when the
   // process exits. Note that if input is provided then then the standard
   // input stream is closed (so EOF is sent) after the input is written.
   // Note also that the standard error handler (log and terminate) is also
   // used. If you want more customized behavior then you can use the more
   // granular runProgram call above. See comment on runProgram above for the
   // semantics of the "command" argument.
   Error runProgram(
            const std::string& executable,
            const std::vector<std::string>& args,
            const std::string& input,
            const ProcessOptions& options,
            const boost::function<void(const ProcessResult&)>& onCompleted);

   // Run a command asynchronously (same as above but uses a command shell
   // rather than running the executable directly)
   Error runCommand(
            const std::string& command,
            const ProcessOptions& options,
            const boost::function<void(const ProcessResult&)>& onCompleted);

   Error runCommand(
            const std::string& command,
            const std::string& input,
            const ProcessOptions& options,
            const boost::function<void(const ProcessResult&)>& onCompleted);


   // Check whether any children are currently running
   bool hasRunningChildren();

   // Check whether any children consider themselves active; non-active
   // processes may be terminated without warning.
   bool hasActiveChildren();

   // Poll for child (output and exit) events. returns true if there
   // are still children being supervised after the poll
   bool poll();

   // Terminate all running children
   void terminateAll();

   // Wait for all children to exit. Returns false if the operation timed out
   bool wait(
      const boost::posix_time::time_duration& pollingInterval =
         boost::posix_time::milliseconds(100),
      const boost::posix_time::time_duration& maxWait =
         boost::posix_time::time_duration(boost::posix_time::not_a_date_time));

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_PROCESS_HPP
