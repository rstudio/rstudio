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

// TODO: add some comments on semantics

// TODO: test with lots of concurrent subprocesses

// TODO: test on linux

// TODO: wrapper for single callback

// TODO: wrapper for synchronous invocation

// TODO: some type of protection against permanent hanging if a child
// doesn't exit? (perhaps calling exited during poll rather than close)
// this is sensible because a never-exiting process would result in
// only file handle and memory leakage rather than freezing all of rsession

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
//  - In server we need to reapChildren since we have session processes
//    which are exiting at random times (however we did have problems
//    with exit codes for PAM helper!). this simply may not be necessary
//    in session
//
//  - Furthermore, we may want to go with a more explicit method of
//    reaping the rsession processes so that we don't interfere with
//    the exit code handing for other scenarios.
//

#include <core/system/ChildProcess.hpp>

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

#include "ChildProcessImpl.hpp"

namespace core {
namespace system {

namespace {


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



struct ChildProcessImpl::Impl
{
   Impl()
      : finishedStdout_(false), finishedStderr_(false)
   {
   }

   redi::basic_pstreambuf<char>& rdbuf() { return *pstream_.rdbuf(); }

   redi::basic_pstream<char> pstream_;
   bool finishedStdout_;
   bool finishedStderr_;
};



ChildProcessImpl::ChildProcessImpl(const std::string& cmd,
                                     const std::vector<std::string>& args)
  : cmd_(cmd), args_(args), pImpl_(new Impl())
{
}

ChildProcessImpl::~ChildProcessImpl()
{
}


Error ChildProcessImpl::run()
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

Error ChildProcessImpl::writeToStdin(const std::string& input, bool eof)
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


void ChildProcessImpl::poll()
{
   // check for output
   try
   {
      // check stdout and fire event if we got output
      std::string out;
      readFromStream(pImpl_->pstream_.out(), &out);
      if (!out.empty() && callbacks_.onStdout)
         callbacks_.onStdout(*this, out);

      // check stram state
      Error error = checkStreamState(pImpl_->pstream_,
                                     &(pImpl_->finishedStdout_),
                                     pImpl_->finishedStderr_);
      if (error)
         reportError(error);

      // check stderr and fire event if we got output
      std::string err;
      readFromStream(pImpl_->pstream_.err(), &err);
      if (!err.empty() && callbacks_.onStderr)
         callbacks_.onStderr(*this, err);

      // check stream state
      error = checkStreamState(pImpl_->pstream_,
                               &(pImpl_->finishedStderr_),
                               pImpl_->finishedStdout_);
      if (error)
         reportError(error);

      // if both streams are finished then close the stream
      if (pImpl_->finishedStdout_ && pImpl_->finishedStderr_)
      {
         // close the stream
         pImpl_->pstream_.close();

         // check for and log errors
         if (pImpl_->pstream_.fail())
            reportIOError(ERROR_LOCATION);

         // fire exit event
         if (callbacks_.onExit)
            callbacks_.onExit(pImpl_->rdbuf().status());
      }

   }
   catch(const std::ios_base::failure& e)
   {
      // we don't expect this to ever happen (since we haven't set the
      // exception flag on our io objects)
      reportIOError(e.what(), ERROR_LOCATION);
   }
}

bool ChildProcessImpl::isRunning()
{
   return pImpl_->pstream_.is_open();
}

Error ChildProcessImpl::terminate()
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

