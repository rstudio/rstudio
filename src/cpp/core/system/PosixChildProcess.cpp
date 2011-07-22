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

#include "ChildProcess.hpp"

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
#include <sstream>

#include <boost/iostreams/copy.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include "ChildProcess.hpp"

namespace core {
namespace system {

namespace {

Error readStream(redi::basic_pstream<char>& stream, std::string* pStr)
{
   try
   {
      // set exception mask (required for consistent reporting of errors since
      // boost::iostreams::copy throws)
      stream.exceptions(std::istream::failbit | std::istream::badbit);

      // copy file to string stream
      std::ostringstream ostr;
      boost::iostreams::copy(stream, ostr);
      *pStr = ostr.str();

      // reset exception mask
      stream.exceptions(std::istream::goodbit);

      // return success
      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      return error;
   }
}

void asyncReadStream(redi::basic_pstream<char>& stream, std::string* pOutput)
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

   // clear any error bit if the other stream isn't finished yet
   if (!stream.good() && !otherFinished)
      stream.clear();

   // return error status
   return error;
}


int resolveExitStatus(int status)
{
   return WIFEXITED(status) ? WEXITSTATUS(status) : status;
}

} // anonymous namespace



struct ChildProcess::Impl
{
   redi::basic_pstreambuf<char>& rdbuf() { return *pstream_.rdbuf(); }
   redi::basic_pstream<char> pstream_;
};


ChildProcess::ChildProcess(const std::string& cmd,
                           const std::vector<std::string>& args)
  : pImpl_(new Impl()), cmd_(cmd), args_(args)
{
}

ChildProcess::~ChildProcess()
{
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


Error ChildProcess::run()
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
      return Success();
   }
   else
   {
      return systemError(pImpl_->rdbuf().error(), ERROR_LOCATION);
   }
}


Error SyncChildProcess::readStdOut(std::string* pOutput)
{
   return readStream(pImpl_->pstream_.out(), pOutput);
}

Error SyncChildProcess::readStdErr(std::string* pOutput)
{
   return readStream(pImpl_->pstream_.err(), pOutput);
}

Error SyncChildProcess::waitForExit(int* pExitStatus)
{
   pImpl_->pstream_.close();
   *pExitStatus = resolveExitStatus(pImpl_->rdbuf().status());
   return Success();
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

AsyncChildProcess::AsyncChildProcess(const std::string& cmd,
                                     const std::vector<std::string>& args)
   : ChildProcess(cmd, args), pAsyncImpl_(new AsyncImpl())
{
}

AsyncChildProcess::~AsyncChildProcess()
{
}

void AsyncChildProcess::poll()
{
   // check for output
   try
   {
      // call onStarted if we haven't yet
      if (!(pAsyncImpl_->calledOnStarted_))
      {
         if (callbacks_.onStarted)
            callbacks_.onStarted(*this);
         pAsyncImpl_->calledOnStarted_ = true;
      }

      // call onRunning
      if (callbacks_.onRunning)
         callbacks_.onRunning(*this);

      // check stdout and fire event if we got output
      if (!pAsyncImpl_->finishedStdout_)
      {
         std::string out;
         asyncReadStream(pImpl_->pstream_.out(), &out);
         if (!out.empty() && callbacks_.onStdout)
            callbacks_.onStdout(*this, out);

         // check stream state
         Error error = checkStreamState(pImpl_->pstream_,
                                        &(pAsyncImpl_->finishedStdout_),
                                        pAsyncImpl_->finishedStderr_);
         if (error)
            reportError(error);
      }

      // check stderr and fire event if we got output
      if (!pAsyncImpl_->finishedStderr_)
      {
         std::string err;
         asyncReadStream(pImpl_->pstream_.err(), &err);
         if (!err.empty() && callbacks_.onStderr)
            callbacks_.onStderr(*this, err);

         // check stream state
         Error error = checkStreamState(pImpl_->pstream_,
                                        &(pAsyncImpl_->finishedStderr_),
                                        pAsyncImpl_->finishedStdout_);
         if (error)
            reportError(error);
      }

      // if both streams are finished then check for exited
      if (pAsyncImpl_->finishedStdout_ && pAsyncImpl_->finishedStderr_)
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
               // resolve exit status
               int status = resolveExitStatus(pImpl_->rdbuf().status());

               // call onExit
               callbacks_.onExit(status);
            }

            // set exited_ flag so that our exited function always
            // returns the right value (even if we haven't been able
            // to successfully wait/reap the child)
            pAsyncImpl_->exited_ = true;
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

bool AsyncChildProcess::exited()
{
   return pAsyncImpl_->exited_;
}



} // namespace system
} // namespace core

