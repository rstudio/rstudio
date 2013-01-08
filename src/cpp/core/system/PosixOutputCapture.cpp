/*
 * PosixOutputCapture.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/system/OutputCapture.hpp>

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>

#include <iostream>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/BoostThread.hpp>
#include <core/BoostErrors.hpp>

#include <core/system/System.hpp>

namespace core {
namespace system {

namespace {

void readFromPipe(
      int pipeFd,
      const boost::function<void(const std::string&)>& outputFunction)
{
   const int kBufferSize = 512;
   char buffer[kBufferSize];
   int bytesRead = 0;
   while ( (bytesRead = ::read(pipeFd, buffer, kBufferSize)) > 0 )
   {
      std::string output(buffer, bytesRead);
      outputFunction(output);
   }

   // log unexpected errors
   if (bytesRead == -1)
   {
      if (errno != EAGAIN && errno != EINTR)
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
   }
}

void standardStreamCaptureThread(
       int stdoutFd,
       const boost::function<void(const std::string&)>& stdoutHandler,
       int stderrFd,
       const boost::function<void(const std::string&)>& stderrHandler)
{
   try
   {
      while(true)
      {
         // create fd set
         fd_set fds;
         FD_ZERO(&fds);
         FD_SET(stdoutFd, &fds);
         if (stderrFd != -1)
            FD_SET(stderrFd, &fds);

         // wait
         int highFd = std::max(stdoutFd, stderrFd);
         int result = ::select(highFd+1, &fds, NULL, NULL, NULL);
         if (result != -1)
         {
            if (FD_ISSET(stdoutFd, &fds))
               readFromPipe(stdoutFd, stdoutHandler);

            if (stderrFd != -1)
            {
               if (FD_ISSET(stderrFd, &fds))
                  readFromPipe(stderrFd, stderrHandler);
            }
         }
         else if (errno != EINTR)
         {
            LOG_ERROR(systemError(errno, ERROR_LOCATION));
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}


Error redirectToPipe(int fd, int* pPipeReadFd)
{
   // create pipe
   int pipe[2];
   if (::pipe(pipe) == -1)
      return systemError(errno, ERROR_LOCATION);

   // redirect
   if (::dup2(pipe[1], fd) == -1)
      return systemError(errno, ERROR_LOCATION);

   // set read fds to non-blocking
   if (::fcntl(pipe[0],
               F_SETFL,
               ::fcntl(pipe[0], F_GETFL) | O_NONBLOCK) == -1)
   {
      return systemError(errno, ERROR_LOCATION);
   }

   // return read fds
   *pPipeReadFd = pipe[0];
   return Success();
}

} // anonymous namespace

Error captureStandardStreams(
            const boost::function<void(const std::string&)>& stdoutHandler,
            const boost::function<void(const std::string&)>& stderrHandler)
{

   // redirect stdout
   int stdoutReadPipe = 0;
   Error error = redirectToPipe(STDOUT_FILENO, &stdoutReadPipe);
   if (error)
      return error;

   // set stdout to use unbuffered io
   ::setvbuf(stdout, NULL, _IONBF, 0);

   // optionally oredirect stderror if handler was provided
   int stderrReadPipe = -1;
   if (stderrHandler)
   {
      error = redirectToPipe(STDERR_FILENO, &stderrReadPipe);
      if (error)
         return error;

      // set stderr to use unbuffered io
      ::setvbuf(stderr, NULL, _IONBF, 0);
   }

   // sync c++ iostreams
   std::ios::sync_with_stdio();

   // launch the monitor thread
   try
   {
      // block all signals for launch of background thread (will cause it
      // to never receive signals)
      core::system::SignalBlocker signalBlocker;
      Error error = signalBlocker.blockAll();
      if (error)
         LOG_ERROR(error);

      boost::thread t(boost::bind(standardStreamCaptureThread,
                                     stdoutReadPipe,
                                     stdoutHandler,
                                     stderrReadPipe,
                                     stderrHandler));

      return Success();
   }
   catch(const boost::thread_resource_error& e)
   {
      return Error(boost::thread_error::ec_from_exception(e), ERROR_LOCATION);
   }
}

} // namespace system
} // namespace core

