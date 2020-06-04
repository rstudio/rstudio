/*
 * PosixOutputCapture.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <shared_core/Error.hpp>
#include <core/BoostThread.hpp>
#include <core/BoostErrors.hpp>

#include <core/system/System.hpp>

#include <boost/bind.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

void readFromPipe(
      int pipeFd,
      const boost::function<void(const std::string&)>& outputFunction)
{
   const int kBufferSize = 40960;
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
       int dupStdoutFd,
       const boost::function<void(const std::string&)>& stdoutHandler,
       int stderrFd,
       int dupStderrFd,
       const boost::function<void(const std::string&)>& stderrHandler)
{
   boost::function<void(const std::string&)> outHandler = stdoutHandler;
   boost::function<void(const std::string&)> errHandler = stderrHandler;

   auto wrapHandler =
    [=](const boost::function<void(const std::string&)>& handler,
        int dupFd,
        const std::string& output)
    {
       handler(output);
       if (::write(dupFd, output.c_str(), output.size()) == -1)
       {
          if (errno != EAGAIN && errno != EINTR)
             LOG_ERROR(systemError(errno, ERROR_LOCATION));
       }
    };

   if (dupStdoutFd != -1)
      outHandler = boost::bind<void>(wrapHandler, stdoutHandler, dupStdoutFd, _1);

   if (dupStderrFd != -1)
      errHandler = boost::bind<void>(wrapHandler, stderrHandler, dupStderrFd, _1);

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
         int result = ::select(highFd+1, &fds, nullptr, nullptr, nullptr);
         if (result != -1)
         {
            if (stdoutFd != -1)
            {
               if (FD_ISSET(stdoutFd, &fds))
                  readFromPipe(stdoutFd, outHandler);
            }

            if (stderrFd != -1)
            {
               if (FD_ISSET(stderrFd, &fds))
                  readFromPipe(stderrFd, errHandler);
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

Error redirectToPipe(int fd,
                     int *pPipeReadFd,
                     int *pOriginalDup)
{
   int newFd = ::dup(fd);
   if (newFd == -1)
      return systemError(errno, ERROR_LOCATION);

   Error error = redirectToPipe(fd, pPipeReadFd);
   if (error)
      return error;

   *pOriginalDup = newFd;
   return Success();
}

} // anonymous namespace

Error captureStandardStreams(
            const boost::function<void(const std::string&)>& stdoutHandler,
            const boost::function<void(const std::string&)>& stderrHandler,
            bool forwardOutputToOriginalDescriptors)
{
   if (!stdoutHandler && !stderrHandler)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "At least one of stdoutHandler and stderrHandler must be set",
                         ERROR_LOCATION);
   }

   Error error;
   int stdoutReadPipe = -1;
   int stderrReadPipe = -1;
   int dupStdoutFd = -1;
   int dupStderrFd = -1;

   // redirect stdout if handler was provided
   if (stdoutHandler)
   {
      if (!forwardOutputToOriginalDescriptors)
         error = redirectToPipe(STDOUT_FILENO, &stdoutReadPipe);
      else
         error = redirectToPipe(STDOUT_FILENO, &stdoutReadPipe, &dupStdoutFd);

      if (error)
         return error;

      // set stdout to use unbuffered io
      ::setvbuf(stdout, nullptr, _IONBF, 0);
   }

   // redirect stderror if handler was provided
   if (stderrHandler)
   {
      if (!forwardOutputToOriginalDescriptors)
         error = redirectToPipe(STDERR_FILENO, &stderrReadPipe);
      else
         error = redirectToPipe(STDERR_FILENO, &stderrReadPipe, &dupStderrFd);

      if (error)
         return error;

      // set stderr to use unbuffered io
      ::setvbuf(stderr, nullptr, _IONBF, 0);
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
                                     dupStdoutFd,
                                     stdoutHandler,
                                     stderrReadPipe,
                                     dupStderrFd,
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
} // namespace rstudio

