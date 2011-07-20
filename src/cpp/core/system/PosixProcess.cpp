/*
 * PosixProcess.cpp
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

// TODO: consider whether onError should default to log and terminate

// TODO: test on linux

// TODO: wrapper for synchronous invocation

// TODO: address semantics of SIGCHLD:
//
//  - We've been calling reapChildren in SessionMain.cpp. this prevents
//    us from getting exit codes in the ChildProcessSupervisor! it appears
//    as if the semantics of system is to block SIGCHLD during execution
//    so we get away with it, however some other cases we might not be
//    (see pg. 345 of Advanced Unix Programming for more on this)
//
//  - Are the popen issues we've seen on the mac a result of the fact
//    that popen is not ignoring SIGCHLD on the mac? -- experiment
//
//  - Are the sweave/texi2dvi exit code issues we saw actually still
//    present if we do away with reapChildren? (we moved away from
//    ignoreChildExits to reapChildren however it may have been enough
//    to simply get rid of ignoreChildExits
//
//  - On the Mac R_system (in sysutils.c) uses a special cocoa run process
//    impl if useAqua is defined. Could it be that this implementation does
//    not block child signals which is why we couldn't get texi2dvi
//    exit codes?
//
//  - In server we need to reapChildren since we have session processes
//    which are exiting at random times (however we did have problems
//    with exit codes for PAM helper!). this simply may not be necessary
//    in session
//
//  - Furthermore, we may want to go with a more explicit method of
//    reaping the rsession processes so that we don't interfere with
//    the exit code handing for other scenarios.
//
// TODO: once we resolve the signal delivery issues could this
//       actually run on a background thread?


// PStreams 0.7.0
//
// The implementation of PosixChildProcess uses an embedded copy of the
// PStreams posix process control library. Links to the PStreams docs:
//
// http://pstreams.sourceforge.net/
// http://pstreams.sourceforge.net/doc/
// http://pstreams.sourceforge.net/faq.html
//
#include "pstreams-0.7.0/pstream.h"

#include <iostream>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include "ChildProcess.hpp"

namespace core {
namespace system {

namespace {

void reportError(const Error& error)
{
   LOG_ERROR(error);
}

void reportIOError(const char* what, const ErrorLocation& location)
{
   Error error = systemError(boost::system::errc::io_error, location);
   if (what != NULL)
      error.addProperty("what", what);
   reportError(error);
}

void reportIOError(const ErrorLocation& location)
{
   reportIOError(NULL, location);
}

void readFromStream(redi::basic_pstream<char>& stream, std::string* pOutput)
{
   char ch;
   while (stream.readsome(&ch, 1) > 0)
      pOutput->append(1, ch);
}

Error checkStreamState(redi::basic_pstream<char>& stream,
                       bool *pFinished,
                       bool otherFinished)
{
   // defualt to no error
   Error error;

   // check for eof or error
   if (stream.eof())
   {
      *pFinished = true;
   }
   else if (stream.fail())
   {
      error = systemError(boost::system::errc::io_error, ERROR_LOCATION);
   }

   // clear any error bit if stderr is still pending
   if (!stream.good() && !otherFinished)
      stream.clear();

   // return error status
   return error;
}

} // anonymous namespace



struct ChildProcess::Impl
{
   Impl()
      : calledOnStarted_(false),
        finishedStdout_(false),
        finishedStderr_(false),
        exited_(false)
   {
   }

   redi::basic_pstreambuf<char>& rdbuf() { return *pstream_.rdbuf(); }

   redi::basic_pstream<char> pstream_;
   bool calledOnStarted_;
   bool finishedStdout_;
   bool finishedStderr_;
   bool exited_;
};



ChildProcess::ChildProcess(const std::string& cmd,
                           const std::vector<std::string>& args)
  : cmd_(cmd), args_(args), pImpl_(new Impl())
{
}

ChildProcess::~ChildProcess()
{
}


Error ChildProcess::run(const ProcessCallbacks& callbacks)
{
   // create set of args to pass (needs to include the cmd)
   std::vector<std::string> args;
   args.push_back(cmd_);
   args.insert(args.end(), args_.begin(), args_.end());

   // open the process
   pImpl_->pstream_.open(cmd_, args, redi::pstreambuf::pstdin |
                                     redi::pstreambuf::pstdout |
                                     redi::pstreambuf::pstderr);

   // return success if we are running
   if (pImpl_->pstream_.is_open())
   {
      callbacks_ = callbacks;
      return Success();
   }
   else
   {
      return systemError(pImpl_->rdbuf().error(), ERROR_LOCATION);
   }
}

Error ChildProcess::writeToStdin(const std::string& input, bool eof)
{
   try
   {
      // write and check for error
      pImpl_->pstream_ << input;
      if (pImpl_->pstream_.fail())
         return systemError(boost::system::errc::io_error, ERROR_LOCATION);

      // write eof if requested
      if (eof)
      {
         pImpl_->rdbuf().peof();

         if (pImpl_->pstream_.fail())
            return systemError(boost::system::errc::io_error, ERROR_LOCATION);
      }

      return Success();
   }
   catch(const std::ios_base::failure& e)
   {
      // we don't expect this to ever happen (since we haven't set the
      // exception flag on our io objects)
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      return error;
   }
}


void ChildProcess::poll()
{
   // check for output
   try
   {
      // call onStarted if we haven't yet
      if (!(pImpl_->calledOnStarted_))
      {
         if (callbacks_.onStarted)
            callbacks_.onStarted(*this);
         pImpl_->calledOnStarted_ = true;
      }

      // check stdout and fire event if we got output
      if (!pImpl_->finishedStdout_)
      {
         std::string out;
         readFromStream(pImpl_->pstream_.out(), &out);
         if (!out.empty() && callbacks_.onStdout)
            callbacks_.onStdout(*this, out);

         // check stream state
         Error error = checkStreamState(pImpl_->pstream_,
                                        &(pImpl_->finishedStdout_),
                                        pImpl_->finishedStderr_);
         if (error)
            reportError(error);
      }

      // check stderr and fire event if we got output
      if (!pImpl_->finishedStderr_)
      {
         std::string err;
         readFromStream(pImpl_->pstream_.err(), &err);
         if (!err.empty() && callbacks_.onStderr)
            callbacks_.onStderr(*this, err);

         // check stream state
         Error error = checkStreamState(pImpl_->pstream_,
                                        &(pImpl_->finishedStderr_),
                                        pImpl_->finishedStdout_);
         if (error)
            reportError(error);
      }

      // if both streams are finished then check for exited
      if (pImpl_->finishedStdout_ && pImpl_->finishedStderr_)
      {
         // Attempt to reap child. Note that this method specifies WNOHANG
         // so we don't block forever waiting for a process the exit. We may
         // not be able to reap the child due to EINTR, in that case we'll
         // just call exited again at the next polling interval. We may not
         // be able to reap the child due to ECHILD (reaped by a global
         // handler) in which case we'll allow the exit sequence to proceed
         // and simply pass -1 as the exit status. If we fail to reap the
         // the child for any other reason (there aren't actually any other
         // failure codes defined for waitpid) then we'll simply continue
         // calling exited in every polling interval (and this leak this
         // object). Note that we don't anticipate this ever occuring based
         // on our understanding of waitpid, PStreams, etc.
         if (pImpl_->rdbuf().exited() || (pImpl_->rdbuf().error() == ECHILD))
         {
            // close the stream (this won't block because we have
            // already established that the child is not running)
            pImpl_->pstream_.close();

            // fire exit event
            if (callbacks_.onExit)
            {
               // recover exit code
               int status = pImpl_->rdbuf().status();
               if (WIFEXITED(status))
                  status = WEXITSTATUS(status);

               // call onExit
               callbacks_.onExit(status);
            }

            // set exited_ flag so that our exited function always
            // returns the right value (even if we haven't been able
            // to successfully wait/reap the child)
            pImpl_->exited_ = true;
         }
      }
   }
   catch(const std::ios_base::failure& e)
   {
      // we don't expect this to ever happen (since we haven't set the
      // exception flag on our io objects)
      reportIOError(e.what(), ERROR_LOCATION);
   }
}

bool ChildProcess::exited()
{
   return pImpl_->exited_;
}

Error ChildProcess::terminate()
{
   // only send signal if the process is open
   if (!pImpl_->pstream_.is_open())
      return systemError(ESRCH, ERROR_LOCATION);

   // send signal
   if (pImpl_->rdbuf().kill(SIGTERM) != NULL)
      return Success();
   else
      return systemError(pImpl_->rdbuf().error(), ERROR_LOCATION);
}

} // namespace system
} // namespace core

