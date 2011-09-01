/*
 * PosixChildProcess.cpp
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

#include "ChildProcess.hpp"

#include <fcntl.h>
#include <signal.h>
#include <sys/wait.h>

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/system/ProcessArgs.hpp>

#include <core/PerformanceTimer.hpp>

#include "ChildProcess.hpp"

// TODO: add process handle to onContinue and onExit calls

// TODO: add the ability to run & terminate as a process group and
//       specify this option for ExecuteInterruptableChild (note
//       we should look into this for Windows as well)

// TODO: add the abilty to assume root

// TODO: replace custom rolled SessionPamAuth calls

namespace core {
namespace system {

namespace {

// pipe handle indexes
const int READ = 0;
const int WRITE = 1;
const std::size_t READ_ERR = -1;

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
   Impl() : pid(-1), fdStdin(-1), fdStdout(-1), fdStderr(-1) {}
   pid_t pid;
   int fdStdin;
   int fdStdout;
   int fdStderr;

   void closeAll(const ErrorLocation& location)
   {
      pid = -1;
      closeFD(&fdStdin, location);
      closeFD(&fdStdout, location);
      closeFD(&fdStderr, location);
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
                        const std::vector<std::string>& args)
{
   exe_ = exe;
   args_ = args;
}

void ChildProcess::init(const std::string& command)
{
   std::vector<std::string> args;
   args.push_back("-c");
   args.push_back(command);
   init("/bin/sh", args);
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


Error ChildProcess::terminate()
{
   // only send signal if the process is open
   if (pImpl_->pid == -1)
      return systemError(ESRCH, ERROR_LOCATION);

   // send signal
   if (::kill(pImpl_->pid, SIGTERM) == -1)
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}


Error ChildProcess::run()
{  
   // standard input
   int fdInput[2];
   Error error = posixCall<int>(boost::bind(::pipe, fdInput), ERROR_LOCATION);
   if (error)
      return error;

   // standard output
   int fdOutput[2];
   error = posixCall<int>(boost::bind(::pipe, fdOutput), ERROR_LOCATION);
   if (error)
   {
      closePipe(fdInput, ERROR_LOCATION);
      return error;
   }

   // standard error
   int fdError[2];
   error = posixCall<int>(boost::bind(::pipe, fdError), ERROR_LOCATION);
   if (error)
   {
      closePipe(fdInput, ERROR_LOCATION);
      closePipe(fdOutput, ERROR_LOCATION);
      return error;
   }

   // fork
   pid_t pid;
   error = posixCall<pid_t>(::fork, ERROR_LOCATION, &pid);
   if (error)
   {
      closePipe(fdInput, ERROR_LOCATION);
      closePipe(fdOutput, ERROR_LOCATION);
      closePipe(fdError, ERROR_LOCATION);
      return error;
   }

   // child
   else if (pid == 0)
   {
      // NOTE: within the child we want to make sure in all cases that
      // we call ::execv to execute the program. as a result if any
      // errors occur while we are setting up for the ::execv we log
      // and continue rather than calling ::exit (we do this to avoid
      // strange error conditions related to global c++ objects being
      // torn down in a non-standard sequence).

      // close unused pipes -- intentionally fail forward (see note above)
      closePipe(fdInput[WRITE], ERROR_LOCATION);
      closePipe(fdOutput[READ], ERROR_LOCATION);
      closePipe(fdError[READ], ERROR_LOCATION);

      // clear the child signal mask
      error = core::system::clearSignalMask();
      if (error)
      {
         LOG_ERROR(error);
         // intentionally fail forward (see note above)
      }

      // wire standard streams (intentionally fail forward)
      safePosixCall<int>(boost::bind(::dup2, fdInput[READ], STDIN_FILENO),
                         ERROR_LOCATION);
      safePosixCall<int>(boost::bind(::dup2, fdOutput[WRITE], STDOUT_FILENO),
                         ERROR_LOCATION);
      safePosixCall<int>(boost::bind(::dup2, fdError[WRITE], STDERR_FILENO),
                         ERROR_LOCATION);

      // close all open file descriptors other than std streams
      error = core::system::closeNonStdFileDescriptors();
      if (error)
      {
         LOG_ERROR(error);
         // intentionally fail forward (see note above)
      }

      // build args (on heap so they stay around after exec)
      // create set of args to pass (needs to include the cmd)
      std::vector<std::string> args;
      args.push_back(exe_);
      args.insert(args.end(), args_.begin(), args_.end());
      using core::system::ProcessArgs;

      ProcessArgs* pProcessArgs = new ProcessArgs(args);
      ::execv(exe_.c_str(), pProcessArgs->args()) ;

      // in the normal case control should never return from execv (it starts
      // anew at main of the process pointed to by path). therefore, if we get
      // here then there was an error
      LOG_ERROR(systemError(errno, ERROR_LOCATION)) ;
      ::exit(EXIT_FAILURE) ;
   }

   // parent
   else
   {
      // close unused pipes
      closePipe(fdInput[READ], ERROR_LOCATION);
      closePipe(fdOutput[WRITE], ERROR_LOCATION);
      closePipe(fdError[WRITE], ERROR_LOCATION);

      // record pid and pipe handles
      pImpl_->pid = pid;
      pImpl_->fdStdin = fdInput[WRITE];
      pImpl_->fdStdout = fdOutput[READ];
      pImpl_->fdStderr = fdError[READ];

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
   pid_t result = posixCall<pid_t>(
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
};

AsyncChildProcess::AsyncChildProcess(const std::string& exe,
                                     const std::vector<std::string>& args)
   : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(exe, args);
}

AsyncChildProcess::AsyncChildProcess(const std::string& command)
      : ChildProcess(), pAsyncImpl_(new AsyncImpl())
{
   init(command);
}

AsyncChildProcess::~AsyncChildProcess()
{
}

void AsyncChildProcess::poll()
{
   // call onStarted if we haven't yet
   if (!(pAsyncImpl_->calledOnStarted_))
   {
      // make sure the output pipes are setup for async reading
      setPipeNonBlocking(pImpl_->fdStdout);
      setPipeNonBlocking(pImpl_->fdStderr);

      if (callbacks_.onStarted)
         callbacks_.onStarted(*this);
      pAsyncImpl_->calledOnStarted_ = true;
   }

   // call onContinue
   if (callbacks_.onContinue)
   {
      if (!callbacks_.onContinue())
      {
         // terminate the proces
         Error error = terminate();
         if (error)
            LOG_ERROR(error);
      }
   }

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
            callbacks_.onStdout(*this, out);

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
            callbacks_.onStderr(*this, err);

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
   pid_t result = posixCall<pid_t>(
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

      // if this is an error that isn't ECHILD then log it (we never
      // expect this to occur as the only documented error codes are
      // EINTR and ECHILD, and EINTR is handled internally by posixCall)
      if (result == -1 && errno != ECHILD)
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
   }
}

bool AsyncChildProcess::exited()
{
   return pAsyncImpl_->exited_;
}

} // namespace system
} // namespace core

