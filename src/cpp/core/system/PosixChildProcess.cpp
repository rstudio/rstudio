/*
 * PosixChildProcess.cpp
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

#include "ChildProcessSubprocPoll.hpp"

#include <fcntl.h>
#include <signal.h>
#include <unistd.h>

#ifdef __APPLE__
#include <util.h>
#include <sys/ttycom.h>
#include <sys/ioctl.h>
#else
#include <pty.h>
#include <asm/ioctls.h>
#endif

#include <sys/wait.h>
#include <sys/types.h>

#include <boost/asio.hpp>
#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/PosixChildProcess.hpp>
#include <core/system/System.hpp>
#include <core/system/ProcessArgs.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/Thread.hpp>

#include <core/PerformanceTimer.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

// pipe handle indexes
const int READ = 0;
const int WRITE = 1;
const std::size_t READ_ERR = -1;

// how long we keep "saw activity" state at true even if we haven't seen
// new activity
const boost::posix_time::milliseconds kResetRecentDelay =
                                         boost::posix_time::milliseconds(1000);

// how often we update "has subprocesses" flag
const boost::posix_time::milliseconds kCheckSubprocDelay =
                                         boost::posix_time::milliseconds(200);

// how often we query and store current working directory of subprocess
const boost::posix_time::milliseconds kCheckCwdDelay =
                                         boost::posix_time::milliseconds(2000);

int resolveExitStatus(int status)
{
   return WIFEXITED(status) ? WEXITSTATUS(status) : status;
}

void setPipeNonBlocking(int pipeFd)
{
   int flags = ::fcntl(pipeFd, F_GETFL);
   if ( (flags != -1) && !(flags & O_NONBLOCK) )
     ::fcntl(pipeFd, F_SETFL, flags | O_NONBLOCK);
}

void closePipe(int pipeFd, const ErrorLocation& location)
{
   safePosixCall<int>(boost::bind(::close, pipeFd), location);
}


void closePipe(int* pipeDescriptors, const ErrorLocation& location)
{
   closePipe(pipeDescriptors[READ], location);
   closePipe(pipeDescriptors[WRITE], location);
}

Error readPipe(int pipeFd, std::string* pOutput, bool *pEOF = NULL)
{
   // default to not eof
   if (pEOF)
      *pEOF = false;

   // setup and read into buffer
   const std::size_t kBufferSize = 512;
   char buffer[kBufferSize];
   std::size_t bytesRead = posixCall<std::size_t>(
                     boost::bind(::read, pipeFd, buffer, kBufferSize));
   while (true)
   {
      // check for error
      if (bytesRead == READ_ERR)
      {
         if (errno == EAGAIN) // carve-out for O_NONBLOCK pipes
            return Success();

         // on linux slave terminals return EIO rather than bytesRead == 0
         // to indicate end of file
         else if ((errno == EIO) && ::isatty(pipeFd))
         {
            if (pEOF)
               *pEOF = true;

            return Success();
         }
         else
            return systemError(errno, ERROR_LOCATION);
      }

      // check for eof
      else if (bytesRead == 0)
      {
         if (pEOF)
            *pEOF = true;

         return Success();
      }

      // append to output
      pOutput->append(buffer, bytesRead);

      // read more bytes
      bytesRead = posixCall<std::size_t>(
                        boost::bind(::read, pipeFd, buffer, kBufferSize));
   }

   // keep compiler happy
   return Success();
}

} // anonymous namespace



struct ChildProcess::Impl
{
   Impl() :
      pid(-1),
      fdStdin(-1),
      fdStdout(-1),
      fdStderr(-1),
      fdMaster(-1),
      ctrlC(0x03)
   {
   }

   PidType pid;
   int fdStdin;
   int fdStdout;
   int fdStderr;

   // pty related
   int fdMaster;
   char ctrlC;

   void init(PidType pid, int fdStdin, int fdStdout, int fdStderr)
   {
      this->pid = pid;
      this->fdStdin = fdStdin;
      this->fdStdout = fdStdout;
      this->fdStderr = fdStderr;
      this->fdMaster = -1;
   }

   void init(PidType pid, int fdMaster)
   {
      this->pid = pid;
      this->fdStdin = fdMaster;
      this->fdStdout = fdMaster;
      this->fdStderr = -1;
      this->fdMaster = fdMaster;
   }

   void closeAll(const ErrorLocation &location)
   {
      closeAll(true, location);
   }

   void closeAll(bool clearPid, const ErrorLocation& location)
   {
      if (clearPid)
         pid = -1;

      if (fdMaster != -1)
      {
         closeFD(&fdMaster, location);
         fdStdin = -1;
         fdStdout = -1;
      }
      else
      {
         closeFD(&fdStdin, location);
         closeFD(&fdStdout, location);
         closeFD(&fdStderr, location);
      }
   }

   void closeFD(int* pFD, const ErrorLocation& location)
   {
      if (*pFD >= 0)
      {
         closePipe(*pFD, location);
         *pFD = -1;
      }
   }
};


ChildProcess::ChildProcess()
  : pImpl_(new Impl())
{
}

void ChildProcess::init(const std::string& exe,
                        const std::vector<std::string>& args,
                        const ProcessOptions& options)
{
   exe_ = exe;
   args_ = args;
   options_ = options;
}

void ChildProcess::init(const std::string& command,
                        const ProcessOptions& options)
{
   std::vector<std::string> args;
   args.push_back("-c");

   std::string realCommand = command;
   if (!options.stdOutFile.empty())
      realCommand += " > " + shell_utils::escape(options.stdOutFile);
   if (!options.stdErrFile.empty())
      realCommand += " 2> " + shell_utils::escape(options.stdErrFile);
   args.push_back(realCommand);

   init("/bin/sh", args, options);
}

// Initialize for an interactive terminal
void ChildProcess::init(const ProcessOptions& options)
{
   if (!options.stdOutFile.empty() || !options.stdErrFile.empty())
   {
      LOG_ERROR_MESSAGE(
               "stdOutFile/stdErrFile options cannot be used with interactive terminal");
   }

   options_ = options;
   exe_ = options_.shellPath.absolutePath();
   args_ = options_.args;
}

ChildProcess::~ChildProcess()
{
}

Error ChildProcess::writeToStdin(const std::string& input, bool eof)
{
   std::size_t written;
   Error error = posixCall<std::size_t>(boost::bind(::write,
                                                   pImpl_->fdStdin,
                                                   input.c_str(),
                                                   input.length()),
                                        ERROR_LOCATION,
                                        &written);
   if (error)
      return error;

   // close if requested
   if (eof)
      pImpl_->closeFD(&pImpl_->fdStdin, ERROR_LOCATION);

   // check for correct bytes written
   if (written != static_cast<std::size_t>(input.length()))
       return systemError(boost::system::errc::io_error, ERROR_LOCATION);

   // return success
   return Success();
}

Error ChildProcess::ptySetSize(int cols, int rows)
{
   // verify we are dealing with a pseudoterminal
   if (!options().pseudoterminal)
      return systemError(boost::system::errc::not_supported, ERROR_LOCATION);

   // define winsize structure
   struct winsize winp;
   winp.ws_col = cols;
   winp.ws_row = rows;
   winp.ws_xpixel = 0;
   winp.ws_ypixel = 0;

   // set it
   int res = ::ioctl(pImpl_->fdMaster, TIOCSWINSZ, &winp);
   if (res == -1)
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}

Error ChildProcess::ptyInterrupt()
{
   // verify we are dealing with a pseudoterminal
   if (!options().pseudoterminal)
      return systemError(boost::system::errc::not_supported, ERROR_LOCATION);

   // write control-c to the slave
   return posixCall<int>(boost::bind(::write,
                                       pImpl_->fdMaster,
                                       &pImpl_->ctrlC,
                                       sizeof(pImpl_->ctrlC)),
                         ERROR_LOCATION);
}

PidType ChildProcess::getPid()
{
   return pImpl_->pid;
}

Error ChildProcess::terminate()
{
   // only send signal if the process is open
   if (pImpl_->pid == -1)
      return systemError(ESRCH, ERROR_LOCATION);

   // special code path for pseudoterminal
   if (options_.pseudoterminal)
   {
#ifndef __APPLE__
      // On Linux only do this if dealing with a Terminal-pane process.
      // This is to reduce scope of this change for 1.1
      // TODO: review post 1.1
      if (options().smartTerminal)
      {
#endif
         // you need to close all of the terminal handles to get
         // bash to quit, however some other processes (like svn+ssh
         // require the signal)
         pImpl_->closeAll(false, ERROR_LOCATION);

#ifndef __APPLE__
      }
#endif

      if (::killpg(::getpgid(pImpl_->pid), SIGTERM) == -1)
      {
         if (errno == EPERM) // see note below on carve out for EPERM
            return Success();
         else
            return systemError(errno, ERROR_LOCATION);
      }
      else
         return Success();
   }
   else
   {
      // determine target pid (kill just this pid or pid + children)
      PidType pid = pImpl_->pid;
      if (options_.detachSession || options_.terminateChildren)
      {
         pid = -pid;
      }

      // send signal
      if (::kill(pid, SIGTERM) == -1)
      {
         // when killing an entire process group EPERM can be returned if even
         // a single one of the subprocesses couldn't be killed. in this case
         // the signal is still delivered and other subprocesses may have been
         // killed so we don't log an error
         if (pid < 0 && errno == EPERM)
            return Success();
         else if (errno == ESRCH)
            return Success();
         else
            return systemError(errno, ERROR_LOCATION);
      }
      else
         return Success();
   }
}

bool ChildProcess::hasNonWhitelistSubprocess() const
{
   // base class doesn't support subprocess-checking; override to implement
   return true;
}

bool ChildProcess::hasWhitelistSubprocess() const
{
   // base class doesn't support subprocess-checking; override to implement
   return false;
}

core::FilePath ChildProcess::getCwd() const
{
   // base class doesn't support cwd-tracking; override to implement
   return FilePath();
}

bool ChildProcess::hasRecentOutput() const
{
   // base class doesn't support output tracking; override to implement
   return true;
}

Error ChildProcess::run()
{  
   // declarations
   PidType pid = 0;
   int fdInput[2] = {0,0};
   int fdOutput[2] = {0,0};
   int fdError[2] = {0,0};
   int fdMaster = 0;

   // pseudoterminal mode: fork using the special forkpty call
   if (options_.pseudoterminal)
   {
      char* nullName = NULL;
      struct termios* nullTermp = NULL;
      struct winsize winSize;
      winSize.ws_col = options_.pseudoterminal.get().cols;
      winSize.ws_row = options_.pseudoterminal.get().rows;
      winSize.ws_xpixel = 0;
      winSize.ws_ypixel = 0;
      Error error = posixCall<PidType>(
         boost::bind(::forkpty, &fdMaster, nullName, nullTermp, &winSize),
         ERROR_LOCATION,
         &pid);
      if (error)
         return error;
   }

   // standard mode: use conventional fork + stream redirection
   else
   {
      // standard input
      Error error = posixCall<int>(boost::bind(::pipe, fdInput), ERROR_LOCATION);
      if (error)
         return error;

      // standard output
      error = posixCall<int>(boost::bind(::pipe, fdOutput), ERROR_LOCATION);
      if (error)
      {
         closePipe(fdInput, ERROR_LOCATION);
         return error;
      }

      // standard error
      error = posixCall<int>(boost::bind(::pipe, fdError), ERROR_LOCATION);
      if (error)
      {
         closePipe(fdInput, ERROR_LOCATION);
         closePipe(fdOutput, ERROR_LOCATION);
         return error;
      }

      // fork
      error = posixCall<PidType>(::fork, ERROR_LOCATION, &pid);
      if (error)
      {
         closePipe(fdInput, ERROR_LOCATION);
         closePipe(fdOutput, ERROR_LOCATION);
         closePipe(fdError, ERROR_LOCATION);
         return error;
      }
   }

   // child
   if (pid == 0)
   {
      // NOTE: within the child we want to make sure in all cases that
      // we call ::execv to execute the program. as a result if any
      // errors occur while we are setting up for the ::execv we log
      // and continue rather than calling ::exit (we do this to avoid
      // strange error conditions related to global c++ objects being
      // torn down in a non-standard sequence).

      // check for an onAfterFork function
       if (options_.onAfterFork)
          options_.onAfterFork();

      // if we didn't create a pseudoterminal then check the detachSession
      // and terminateChildren options to see whether we need to setsid
      // or setpgid(0,0). we skip the check for pseudoterminals because
      // forkpty calls setsid internally
      if (!options_.pseudoterminal)
      {
         // If options.detachSession is requested then separate.
         if (options_.detachSession)
         {
            if (::setsid() == -1)
            {
               LOG_ERROR(systemError(errno, ERROR_LOCATION));
               // intentionally fail forward (see note above)
            }
         }
         else if (options_.terminateChildren)
         {
            // No need to call ::setpgid(0,0) if ::setsid() was already called

            // if options.terminateChildren is requested then obtain a new
            // process group (using our own process id). this enables terminate
            // to specify -pid to kill which will kill this process and all of
            // its children. note that another side-effect is that this process
            // will not automatically die with its parent, so the parent
            // may want to kill all children from the processSupervisor on exit
            if (::setpgid(0,0) == -1)
            {
               LOG_ERROR(systemError(errno, ERROR_LOCATION));
               // intentionally fail forward (see note above)
            }
         }
      }

      // clear the child signal mask
      Error error = core::system::clearSignalMask();
      if (error)
      {
         LOG_ERROR(error);
         // intentionally fail forward (see note above)
      }  

      // pseudoterminal mode: file descriptor work is already handled
      // by forkpty, all we need to do is configure terminal behavior
      if (options_.pseudoterminal)
      {
         // get current attributes
         struct termios termp;
         Error error = posixCall<int>(
            boost::bind(::tcgetattr, STDIN_FILENO, &termp),
            ERROR_LOCATION);
         if (!error)
         {
            if (!options_.smartTerminal)
            {
               // Specify raw mode; not doing this for terminal (versus dumb
               // shell) because on Linux, it broke things like "passwd"
               // command's ability to collect passwords).
               ::cfmakeraw(&termp);
            }
            else
            {
               // for smart terminals we need to echo back the user input
               termp.c_lflag |= ECHO;
               termp.c_oflag |= OPOST|ONLCR;

               // Turn off XON/XOFF flow control so Ctrl+S can be used by
               // the shell command-line editing instead of suspending output.
               termp.c_iflag &= ~(IXON|IXOFF);
            }

            // Don't ignore signals -- this is done
            // so we can send Ctrl-C for interrupts
            termp.c_lflag |= ISIG;

            // set attribs
            safePosixCall<int>(
                  boost::bind(::tcsetattr, STDIN_FILENO, TCSANOW, &termp),
                  ERROR_LOCATION);

            // save the VINTR character
            pImpl_->ctrlC = termp.c_cc[VINTR];
         }
         else
         {
            LOG_ERROR(error);
         }
      }

      // standard mode: close/redirect pipes
      else
      {
         // close unused pipes -- intentionally fail forward (see note above)
         closePipe(fdInput[WRITE], ERROR_LOCATION);
         closePipe(fdOutput[READ], ERROR_LOCATION);
         closePipe(fdError[READ], ERROR_LOCATION);

         // wire standard streams (intentionally fail forward)
         safePosixCall<int>(boost::bind(::dup2, fdInput[READ], STDIN_FILENO),
                            ERROR_LOCATION);
         safePosixCall<int>(boost::bind(::dup2, fdOutput[WRITE], STDOUT_FILENO),
                            ERROR_LOCATION);
         safePosixCall<int>(
               boost::bind(::dup2,
                           options_.redirectStdErrToStdOut ? fdOutput[WRITE]
                                                           : fdError[WRITE],
                           STDERR_FILENO),
               ERROR_LOCATION);
      }

      // close all open file descriptors other than std streams
      error = core::system::closeNonStdFileDescriptors();
      if (error)
      {
         LOG_ERROR(error);
         // intentionally fail forward (see note above)
      }

      if (!options_.workingDir.empty())
      {
         if (::chdir(options_.workingDir.absolutePath().c_str()))
         {
            std::string message = "Error changing directory: '";
            message += options_.workingDir.absolutePath().c_str();
            message += "'";
            LOG_ERROR(systemError(errno, message.c_str(), ERROR_LOCATION));
         }
      }

      // build args (on heap so they stay around after exec)
      // create set of args to pass (needs to include the cmd)
      std::vector<std::string> args;
      args.push_back(exe_);
      args.insert(args.end(), args_.begin(), args_.end());
      using core::system::ProcessArgs;
      ProcessArgs* pProcessArgs = new ProcessArgs(args);

      if (options_.environment)
      {
         // build env (on heap, see comment above)
         std::vector<std::string> env;
         const Options& options = options_.environment.get();
         for (Options::const_iterator
                  it = options.begin(); it != options.end(); ++it)
         {
            env.push_back(it->first + "=" + it->second);
         }
         ProcessArgs* pEnvironment = new ProcessArgs(env);

         // execute
         ::execve(exe_.c_str(), pProcessArgs->args(), pEnvironment->args());
      }
      else
      {
         // execute
         ::execv(exe_.c_str(), pProcessArgs->args()) ;
      }

      // in the normal case control should never return from execv (it starts
      // anew at main of the process pointed to by path). therefore, if we get
      // here then there was an error
      error = systemError(errno, ERROR_LOCATION);
      error.addProperty("exe", exe_);
      LOG_ERROR(error);
      ::exit(EXIT_FAILURE);
   }

   // parent
   else
   {
      // pseudoterminal mode: wire input/output streams to fdMaster
      // returned from forkpty
      if (options_.pseudoterminal)
      {
         // record masterFd as our handles
         pImpl_->init(pid, fdMaster);
      }

      // standard mode: close unused pipes & wire streams to approprite fds
      else
      {
         // close unused pipes
         closePipe(fdInput[READ], ERROR_LOCATION);
         closePipe(fdOutput[WRITE], ERROR_LOCATION);
         closePipe(fdError[WRITE], ERROR_LOCATION);

         // record pipe handles
         pImpl_->init(pid, fdInput[WRITE], fdOutput[READ], fdError[READ]);
      }

      return Success();
   }

   // keep compiler happy
   return Success();
}

Error SyncChildProcess::readStdOut(std::string* pOutput)
{
   return readPipe(pImpl_->fdStdout, pOutput);
}

Error SyncChildProcess::readStdErr(std::string* pOutput)
{
   return readPipe(pImpl_->fdStderr, pOutput);
}

Error SyncChildProcess::waitForExit(int* pExitStatus)
{
   // blocking wait for exit
   int status;
   PidType result = posixCall<PidType>(
      boost::bind(::waitpid, pImpl_->pid, &status, 0));

   // always close all of the pipes
   pImpl_->closeAll(ERROR_LOCATION);

   // check result
   if (result == -1)
   {
      *pExitStatus = -1;

      if (errno == ECHILD) // carve out for child already reaped
         return Success();
      else
         return systemError(errno, ERROR_LOCATION);
   }
   else
   {
      *pExitStatus = resolveExitStatus(status);
      return Success();
   }
}

struct AsyncChildProcess::AsyncImpl
{
   AsyncImpl()
      : calledOnStarted_(false),
        finishedStdout_(false),
        finishedStderr_(false),
        exited_(false)
   {
   }
   bool calledOnStarted_;
   bool finishedStdout_;
   bool finishedStderr_;
   bool exited_;
   boost::scoped_ptr<ChildProcessSubprocPoll> pSubprocPoll_;
};

AsyncChildProcess::AsyncChildProcess(const std::string& exe,
                                     const std::vector<std::string>& args,
                                     const ProcessOptions& options)
   : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(exe, args, options);
   if (!options.stdOutFile.empty() || !options.stdErrFile.empty())
   {
      LOG_WARNING_MESSAGE(
               "stdOutFile/stdErrFile options cannot be used with runProgram");
   }
}

AsyncChildProcess::AsyncChildProcess(const std::string& command,
                                     const ProcessOptions& options)
      : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(command, options);
}

AsyncChildProcess::AsyncChildProcess(const ProcessOptions& options)
      : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(options);
}

AsyncChildProcess::~AsyncChildProcess()
{
}

Error AsyncChildProcess::terminate()
{
#ifndef __APPLE__
   // On Linux only do this if dealing with a Terminal-pane process.
   // This is to reduce scope of this change for 1.1
   // TODO: review post 1.1
   if (options().smartTerminal)
   {
#endif
      if (options().pseudoterminal)
      {
         pAsyncImpl_->finishedStderr_ = true;
         pAsyncImpl_->finishedStdout_ = true;
      }
#ifndef __APPLE__
   }
#endif
   return ChildProcess::terminate();
}

bool AsyncChildProcess::hasNonWhitelistSubprocess() const
{
   if (pAsyncImpl_->pSubprocPoll_)
      return pAsyncImpl_->pSubprocPoll_->hasNonWhitelistSubprocess();
   else
      return true;
}

bool AsyncChildProcess::hasWhitelistSubprocess() const
{
   if (pAsyncImpl_->pSubprocPoll_)
      return pAsyncImpl_->pSubprocPoll_->hasWhitelistSubprocess();
   else
      return false;
}

core::FilePath AsyncChildProcess::getCwd() const
{
   if (pAsyncImpl_->pSubprocPoll_)
      return pAsyncImpl_->pSubprocPoll_->getCwd();
   else
      return FilePath();
}

bool AsyncChildProcess::hasRecentOutput() const
{
   if (pAsyncImpl_->pSubprocPoll_)
      return pAsyncImpl_->pSubprocPoll_->hasRecentOutput();
   else
      return true;
}

void AsyncChildProcess::poll()
{
   // call onStarted if we haven't yet
   if (!(pAsyncImpl_->calledOnStarted_))
   {
      // make sure the output pipes are setup for async reading
      setPipeNonBlocking(pImpl_->fdStdout);

      // if we are providing a pseudoterminal then stderr is disabled
      // so mark it finished. otherwise, configure it for non-blocking io
      if (options().pseudoterminal)
         pAsyncImpl_->finishedStderr_ = true;
      else
         setPipeNonBlocking(pImpl_->fdStderr);         

      // setup for subprocess polling
      pAsyncImpl_->pSubprocPoll_.reset(new ChildProcessSubprocPoll(
         pImpl_->pid,
         kResetRecentDelay, kCheckSubprocDelay, kCheckCwdDelay,
         options().reportHasSubprocs ? core::system::getSubprocesses : NULL,
         options().subprocWhitelist,
         options().trackCwd ? core::system::currentWorkingDir : NULL));

      if (callbacks_.onStarted)
         callbacks_.onStarted(*this);
      pAsyncImpl_->calledOnStarted_ = true;
   }
   // call onContinue
   if (callbacks_.onContinue)
   {
      if (!callbacks_.onContinue(*this))
      {
         // terminate the proces
         Error error = terminate();
         if (error)
            LOG_ERROR(error);
      }
   }

   bool hasRecentOutput = false;

   // check stdout and fire event if we got output
   if (!pAsyncImpl_->finishedStdout_)
   {
      bool eof;
      std::string out;
      Error error = readPipe(pImpl_->fdStdout, &out, &eof);
      if (error)
      {
         reportError(error);
      }
      else
      {
         if (!out.empty() && callbacks_.onStdout)
         {
            hasRecentOutput = true;
            callbacks_.onStdout(*this, out);
         }

         if (eof)
           pAsyncImpl_->finishedStdout_ = true;
      }
   }

   // check stderr and fire event if we got output
   if (!pAsyncImpl_->finishedStderr_)
   {
      bool eof;
      std::string err;
      Error error = readPipe(pImpl_->fdStderr, &err, &eof);

      if (error)
      {
         reportError(error);
      }
      else
      {
         if (!err.empty() && callbacks_.onStderr)
         {
            hasRecentOutput = true;
            callbacks_.onStderr(*this, err);
         }

         if (eof)
           pAsyncImpl_->finishedStderr_ = true;
      }
   }

   // Check for exited. Note that this method specifies WNOHANG
   // so we don't block forever waiting for a process the exit. We may
   // not be able to reap the child due to an error (typically ECHILD,
   // which occurs if the child was reaped by a global handler) in which
   // case we'll allow the exit sequence to proceed and simply pass -1 as
   // the exit status.
   int status;
   PidType result = posixCall<PidType>(
            boost::bind(::waitpid, pImpl_->pid, &status, WNOHANG));

   // either a normal exit or an error while waiting
   if (result != 0)
   {
      // close all of our pipes
      pImpl_->closeAll(ERROR_LOCATION);

      // fire exit event
      if (callbacks_.onExit)
      {
         // resolve exit status
         if (result > 0)
            status = resolveExitStatus(status);
         else
            status = -1;

         // call onExit
         callbacks_.onExit(status);
      }

      // set exited_ flag so that our exited function always
      // returns the right value
      pAsyncImpl_->exited_ = true;
      pAsyncImpl_->pSubprocPoll_->stop();

      // if this is an error that isn't ECHILD then log it (we never
      // expect this to occur as the only documented error codes are
      // EINTR and ECHILD, and EINTR is handled internally by posixCall)
      if (result == -1 && errno != ECHILD && errno != ENOENT)
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
   }

   // Perform optional periodic operations
   if (pAsyncImpl_->pSubprocPoll_->poll(hasRecentOutput))
   {
      if (callbacks_.onHasSubprocs)
      {
         callbacks_.onHasSubprocs(hasNonWhitelistSubprocess(),
                                  hasWhitelistSubprocess());
      }
      if (callbacks_.reportCwd)
      {
         callbacks_.reportCwd(getCwd());
      }
   }
}

bool AsyncChildProcess::exited()
{
   return pAsyncImpl_->exited_;
}

struct AsioAsyncChildProcess::Impl : public boost::enable_shared_from_this<AsioAsyncChildProcess::Impl>
{
   Impl(AsioAsyncChildProcess* parent,
        boost::asio::io_service& ioService) :
      parent_(parent), ioService_(ioService), stdOutDescriptor_(ioService_),
      stdErrDescriptor_(ioService_), stdInDescriptor_(ioService_), exited_(false),
      exitCode_(0), writing_(false)
   {
   }

   void attachDescriptors()
   {
      stdOutDescriptor_.assign(parent_->pImpl_->fdStdout);
      stdErrDescriptor_.assign(parent_->pImpl_->fdStderr);
      stdInDescriptor_.assign(parent_->pImpl_->fdStdin);
   }

   void beginReadStdOut()
   {
      return stdOutDescriptor_.async_read_some(boost::asio::buffer(stdOutBuff_, 1024),
                                               boost::bind(&Impl::stdOutCallback,
                                                           boost::weak_ptr<Impl>(shared_from_this()),
                                                           boost::asio::placeholders::error,
                                                           _2));
   }

   void beginReadStdErr()
   {
      return stdErrDescriptor_.async_read_some(boost::asio::buffer(stdErrBuff_, 1024),
                                               boost::bind(&Impl::stdErrCallback,
                                                           boost::weak_ptr<Impl>(shared_from_this()),
                                                           boost::asio::placeholders::error,
                                                           _2));
   }

   void beginWriteStdIn()
   {
      LOCK_MUTEX(mutex_)
      {
         if (exited_) return;

         if (writeBuffer_.empty()) return;

         std::pair<std::string, bool>& writeData = writeBuffer_.front();
         std::string& input = writeData.first;
         bool eof = writeData.second;

         stdInDescriptor_.async_write_some(boost::asio::buffer(input, input.size()),
                                           boost::bind(&Impl::writeCallback,
                                                       boost::weak_ptr<Impl>(shared_from_this()),
                                                       boost::asio::placeholders::error,
                                                       eof));
      }
      END_LOCK_MUTEX
   }

   static void stdOutCallback(const boost::weak_ptr<Impl>& instance,
                              boost::system::error_code ec, size_t bytesRead)
   {
      // check to make sure the instance is still alive
      // if not, we just ignore this callback as it is stale, with no object alive to service it
      if (boost::shared_ptr<Impl> impl = instance.lock())
      {
         // invoke the actual method on the shared ptr
         impl->onReadStdOut(ec, bytesRead);
      }
   }

   static void stdErrCallback(const boost::weak_ptr<Impl>& instance,
                              boost::system::error_code ec, size_t bytesRead)
   {
      // check to make sure the instance is still alive
      // if not, we just ignore this callback as it is stale, with no object alive to service it
      if (boost::shared_ptr<Impl> impl = instance.lock())
      {
         // invoke the actual method on the shared ptr
         impl->onReadStdErr(ec, bytesRead);
      }
   }

   void onReadStdOut(boost::system::error_code ec, size_t bytesRead)
   {
      if (ec)
         return handleError(ec);

      std::string out(stdOutBuff_, bytesRead);
      if (callbacks_.onStdout)
         callbacks_.onStdout(*(static_cast<ProcessOperations*>(parent_)), out);

      // continue reading stdout
      beginReadStdOut();
   }

   void onReadStdErr(boost::system::error_code ec, size_t bytesRead)
   {
      if (ec)
         return handleError(ec);

      std::string out(stdErrBuff_, bytesRead);
      if (callbacks_.onStderr)
         callbacks_.onStderr(*(static_cast<ProcessOperations*>(parent_)), out);

      // continue reading stdout
      beginReadStdErr();
   }

   static void writeCallback(const boost::weak_ptr<Impl>& instance, boost::system::error_code ec, bool eof)
   {
      // check to make sure the instance is still alive
      // if not, we just ignore this callback as it is stale, with no object alive to service it
      if (boost::shared_ptr<Impl> impl = instance.lock())
      {
         // invoke the actual method on the shared ptr
         impl->onWriteStdIn(ec, eof);
      }
   }

   void onWriteStdIn(boost::system::error_code ec, bool eof)
   {
      LOCK_MUTEX(mutex_)
      {
         if (exited_) return;

         // free the message that was just written
         // we do this here because the message has to exist in memory
         // for the entire duration of the asynchronous write
         writeBuffer_.pop();
      }
      END_LOCK_MUTEX

      if (ec)
         return handleError(ec);

      if (eof)
      {
         // close std in stream and stop writing
         parent_->pImpl_->closeFD(&parent_->pImpl_->fdStdin, ERROR_LOCATION);
      }
      else
      {
         // continue writing until our write queue is drained
         beginWriteStdIn();
      }
   }

   // writes need synchronization because we could have an outstanding write while a request
   // comes in to write more data. async io requires that we only have one outstanding write
   // operation to avoid interleaving of message content
   void writeInput(const std::string& input, bool eof)
   {
      bool beginWriting = false;

      LOCK_MUTEX(mutex_)
      {
         if (exited_) return;

         beginWriting = writeBuffer_.empty();
         writeBuffer_.push(std::make_pair(input, eof));
      }
      END_LOCK_MUTEX

      if (beginWriting)
         beginWriteStdIn();
   }

   void handleError(boost::system::error_code ec)
   {
      // if we were aborted, we did this ourselves so do not propagate the error
      if (ec == boost::asio::error::operation_aborted) return;

      // check to see if the process exited
      // if not, invoke error callback
      if (!checkExited())
      {
         Error error = systemError(ec.value(), ec.message(), ERROR_LOCATION);
         if (callbacks_.onError)
         {
            // copy members before posting the error to the thread pool
            // we do this because this instance could be destroyed while in the
            // io_service queue, and so we want to copy these variables
            // to ensure we don't try to access any members
            boost::function<void(void)> handler = boost::bind(&Impl::invokeErrorHandler,
                                                              boost::weak_ptr<Impl>(shared_from_this()),
                                                              error);
            ioService_.post(handler);
         }
         else
            LOG_ERROR(error);

         // if we had an unexpected closure of the stream but no exit, terminate
         parent_->terminate();
      }
   }

   static void invokeErrorHandler(boost::weak_ptr<Impl> weak,
                                  const Error& error)
   {
      // check to make sure the instance is still alive
      // if not, we just ignore this callback as it is stale, with no object alive to service it
      if (boost::shared_ptr<Impl> impl = weak.lock())
      {
         // invoke the actual callback on the shared ptr
         impl->callbacks_.onError(*(static_cast<ProcessOperations*>(impl->parent_)), error);
      }
   }

   bool exited()
   {
      LOCK_MUTEX(mutex_)
      {
         return exited_;
      }
      END_LOCK_MUTEX

      // keep compiler happy
      return exited_;
   }

   void setExited()
   {
      LOCK_MUTEX(mutex_)
      {
         exited_ = true;
      }
      END_LOCK_MUTEX
   }

   bool checkExited()
   {
      bool transitioned = false;

      LOCK_MUTEX(mutex_)
      {
         if (exited_) return exited_;

         // perform a non-blocking wait on the pid to determine if we exited
         int status = 0;
         PidType result = posixCall<PidType>(boost::bind(::waitpid, parent_->pImpl_->pid, &status, WNOHANG));

         if (result != 0)
         {
            exited_ = true;
            transitioned = true;

            cleanup();

            if (result > 0)
               exitCode_ = resolveExitStatus(status);
            else
               exitCode_ = -1;
         }
      }
      END_LOCK_MUTEX

      // invoke the exit callback on the thread pool
      // we do that instead of immediately to ensure a clean exit
      if (transitioned && callbacks_.onExit)
      {
         // copy the function before posting the error to the thread pool
         // we do this because this instance could be destroyed while in the
         // io_service queue, and so we want to copy these variables
         // to ensure we don't try to access any members
         boost::function<void(void)> handler = boost::bind(callbacks_.onExit, exitCode_);
         ioService_.post(handler);
      }

      return exited_;
   }

   void cleanup()
   {
      // stop asynchronous operations
      stdOutDescriptor_.cancel();
      stdErrDescriptor_.cancel();
      stdInDescriptor_.cancel();

      // close all pipes
      parent_->pImpl_->closeAll(ERROR_LOCATION);
   }

   AsioAsyncChildProcess* parent_;
   boost::asio::io_service& ioService_;
   boost::asio::posix::stream_descriptor stdOutDescriptor_;
   boost::asio::posix::stream_descriptor stdErrDescriptor_;
   boost::asio::posix::stream_descriptor stdInDescriptor_;
   bool exited_;
   int exitCode_;

   ProcessCallbacks callbacks_;

   char stdOutBuff_[1024];
   char stdErrBuff_[1024];

   bool writing_;
   std::queue<std::pair<std::string, bool> > writeBuffer_;

   boost::mutex mutex_;
};

AsioAsyncChildProcess::AsioAsyncChildProcess(boost::asio::io_service& ioService,
                                             const std::string& exe,
                                             const std::vector<std::string>& args,
                                             const ProcessOptions& options) :
   AsyncChildProcess(exe, args, options),
   pAsioImpl_(new Impl(const_cast<AsioAsyncChildProcess*>(this), ioService))
{
}

AsioAsyncChildProcess::AsioAsyncChildProcess(boost::asio::io_service& ioService,
                                             const std::string& command,
                                             const ProcessOptions& options) :
   AsyncChildProcess(command, options),
   pAsioImpl_(new Impl(const_cast<AsioAsyncChildProcess*>(this), ioService))
{
}

AsioAsyncChildProcess::~AsioAsyncChildProcess()
{
}

Error AsioAsyncChildProcess::run(const ProcessCallbacks& callbacks)
{
   pAsioImpl_->callbacks_ = callbacks;

   Error error = ChildProcess::run();
   if (error)
      return error;

   pAsioImpl_->attachDescriptors();
   pAsioImpl_->beginReadStdOut();
   pAsioImpl_->beginReadStdErr();

   return Success();
}

void AsioAsyncChildProcess::asyncWriteToStdin(const std::string& input, bool eof)
{
   return pAsioImpl_->writeInput(input, eof);
}

bool AsioAsyncChildProcess::exited()
{
   return pAsioImpl_->exited();
}

Error AsioAsyncChildProcess::terminate()
{
   if (exited()) return Success();

   Error error = AsyncChildProcess::terminate();
   if (!error)
      pAsioImpl_->checkExited();

   return error;
}

bool AsioAsyncChildProcess::hasNonWhitelistSubprocess() const
{
   return AsyncChildProcess::hasNonWhitelistSubprocess();
}

bool AsioAsyncChildProcess::hasWhitelistSubprocess() const
{
   return AsyncChildProcess::hasWhitelistSubprocess();
}

core::FilePath AsioAsyncChildProcess::getCwd() const
{
   // not relevant
   return FilePath();
}

bool AsioAsyncChildProcess::hasRecentOutput() const
{
   // not relevant
   return false;
}

} // namespace system
} // namespace core
} // namespace rstudio

