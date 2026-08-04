/*
 * Win32OutputCapture.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/OutputCapture.hpp>

#include <windows.h>
#include <io.h>

#include <stdio.h>
#include <fcntl.h>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/Thread.hpp>

#ifndef STDOUT_FILENO
# define STDOUT_FILENO 1
#endif

#ifndef STDERR_FILENO
# define STDERR_FILENO 2
#endif

namespace rstudio {
namespace core {
namespace system {

namespace {

boost::mutex s_mutex;

void standardStreamCaptureThread(
       const std::string& streamName,
       HANDLE hReadPipe,
       HANDLE hOrigStdHandle,
       const boost::function<void(const std::string&)>& outputHandler)
{
   try
   {
      bool skipEchoWrite = false;
      bool loggedReadError = false;

      while(true)
      {
         const int kBufferSize = 512;
         char buffer[kBufferSize];
         DWORD bytesRead = 0;
         if (::ReadFile(hReadPipe, &buffer, kBufferSize, &bytesRead, nullptr))
         {
            if (bytesRead > 0)
            {
               outputHandler(std::string(buffer, bytesRead));

               if (hOrigStdHandle && !skipEchoWrite)
               {
                  DWORD writeError = ERROR_SUCCESS;
                  LOCK_MUTEX(s_mutex)
                  {
                     // the byte-count out-param is required for synchronous
                     // handles; a short write is deliberately ignored, as the
                     // echo is best-effort
                     DWORD bytesWritten = 0;
                     if (!::WriteFile(hOrigStdHandle, buffer, bytesRead, &bytesWritten, nullptr))
                        writeError = ::GetLastError();
                  }
                  END_LOCK_MUTEX

                  if (writeError != ERROR_SUCCESS)
                  {
                     // deliberately give up on the echo after the first failure and
                     // log only once: a stderr log destination writes to the
                     // redirected stderr, so a logged error is read back by the
                     // stderr capture thread, and logging every failure spins
                     // forever. the same option gates both this echo and stderr
                     // logging, so that feedback loop is guaranteed, not a rare
                     // configuration (#18414). unlike the Posix twin (#18398),
                     // no error code is treated as transient here: the console
                     // handle is opened once at startup and never reopened, so
                     // the failures targeted here are permanent.
                     skipEchoWrite = true;

                     LOG_ERROR(systemError(
                                  writeError,
                                  streamName + " echo to the attached console failed. Output will no longer be echoed to the console.",
                                  ERROR_LOCATION));
                  }
               }
            }
         }
         else
         {
            // capture the error code before any other call can clobber it
            DWORD readError = ::GetLastError();

            // a broken pipe or invalid handle can never recover, and both imply
            // the pipe no longer accepts writes, so exit instead of polling
            // forever; logging here is bounded because the thread exits
            if (readError == ERROR_BROKEN_PIPE || readError == ERROR_INVALID_HANDLE)
            {
               LOG_ERROR(systemError(
                            readError,
                            streamName + " capture pipe is no longer readable. Output capture stopped.",
                            ERROR_LOCATION));
               return;
            }

            // we don't expect other read errors here (the write end of the pipe
            // is our own stdout / stderr and is never closed), so log only the
            // first -- with a stderr log destination, every logged error lands
            // back in a capture pipe -- and retry at a modest interval rather
            // than spinning (#18414)
            if (!loggedReadError)
            {
               loggedReadError = true;
               LOG_ERROR(systemError(
                            readError,
                            streamName + " capture pipe read failed.",
                            ERROR_LOCATION));
            }

            ::Sleep(100);
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}

Error ioError(const std::string& description, const ErrorLocation& location)
{
   boost::system::error_code ec(boost::system::errc::io_error,
                                boost::system::system_category());
   Error error(ec, location);
   error.addProperty("description", description);
   return error;
}

Error redirectToPipe(DWORD stdHandle,
                     int stdFd,
                     FILE* fpStdFile,
                     HANDLE* phReadPipe)
{
   // create pipe
   HANDLE hWritePipe;
   if (!::CreatePipe(phReadPipe, &hWritePipe, nullptr, 0))
   {
      return LAST_SYSTEM_ERROR();
   }

   // reset win32 standard handle
   if (!::SetStdHandle(stdHandle, hWritePipe))
   {
      return LAST_SYSTEM_ERROR();
   }

   // reset c runtime library handle
   int fd = ::_open_osfhandle((intptr_t)hWritePipe, _O_TEXT);
   if (fd == -1)
      return ioError("_open_osfhandle", ERROR_LOCATION);

   int stdDupFd = ::_fileno(fpStdFile);
   if (stdDupFd == -2)
   {
      // -2 is a special value that indicates stdout/stderr isn't associated
      // with an output stream. The below fixes an issue in which, for Windows
      // users other than the first, QProcess doesn't start the R session in
      // such a way that it receives stdout/stderr streams (see case 4230); in
      // this situation, we allocate a new stream for the descriptor so
      // stdout/stderr will go somewhere.
      FILE* newStdHandle = ::_fdopen(stdFd, "w");
      if (newStdHandle == nullptr)
         return systemError(errno, ERROR_LOCATION);
      *fpStdFile = *newStdHandle;
      stdDupFd = stdFd;
   }

   if (::_dup2(fd, stdDupFd))
      return systemError(errno, ERROR_LOCATION);

   // turn off buffering
   if (::setvbuf(fpStdFile, nullptr, _IONBF, 0) != 0)
      return ioError("setvbuf", ERROR_LOCATION);

   // sync c++ std streams
   std::ios::sync_with_stdio();

   return Success();
}

Error redirectToPipe(DWORD stdHandle,
                     int stdFd,
                     FILE* fpStdFile,
                     HANDLE* phReadPipe,
                     HANDLE* hOrigStdHandle)
{
   Error error = redirectToPipe(stdHandle, stdFd, fpStdFile, phReadPipe);
   if (error)
      return error;

   HANDLE hOrigHandle = ::CreateFile("CONOUT$",
                                     GENERIC_WRITE,
                                     FILE_SHARE_WRITE,
                                     NULL,
                                     OPEN_EXISTING,
                                     FILE_ATTRIBUTE_NORMAL,
                                     NULL);

   if (hOrigHandle == INVALID_HANDLE_VALUE)
   {
      // if for some reason we cannot open a handle to the console we simply log the error instead
      // of bubling it up - this way, we can continue to redirect output to the pipe but not forward
      // it to the console, as an error would indicate that the console is not available
      LOG_ERROR(LAST_SYSTEM_ERROR());
   }
   else
   {
      *hOrigStdHandle = hOrigHandle;
   }

   return Success();
}


} // anonymous namespace

Error captureStandardStreams(
            const boost::function<void(const std::string&)>& stdoutHandler,
            const boost::function<void(const std::string&)>& stderrHandler,
            bool forwardOutputToOriginalDescriptors)
{
   try
   {
      if (!stdoutHandler && !stderrHandler)
      {
         return systemError(boost::system::errc::invalid_argument,
                            "At least one of stdoutHandler and stderrHandler must be set",
                            ERROR_LOCATION);
      }

      Error error;
      HANDLE hReadStdoutPipe = nullptr;
      HANDLE hReadStderrPipe = nullptr;
      HANDLE hOrigStdoutHandle = nullptr;
      HANDLE hOrigStderrHandle = nullptr;

      // redirect stdout if handler was provided
      if (stdoutHandler)
      {
         if (!forwardOutputToOriginalDescriptors)
         {
            error = redirectToPipe(STD_OUTPUT_HANDLE,
                                   STDOUT_FILENO,
                                   stdout,
                                   &hReadStdoutPipe);
         }
         else
         {
            error = redirectToPipe(STD_OUTPUT_HANDLE,
                                   STDOUT_FILENO,
                                   stdout,
                                   &hReadStdoutPipe,
                                   &hOrigStdoutHandle);
         }

         if (error)
            return error;

         // capture stdout
         boost::thread stdoutThread(boost::bind(standardStreamCaptureThread,
                                                std::string("Stdout"),
                                                hReadStdoutPipe,
                                                hOrigStdoutHandle,
                                                stdoutHandler));
      }

      // redirect stderror if handler was provided
      if (stderrHandler)
      {
         if (!forwardOutputToOriginalDescriptors)
         {
            error = redirectToPipe(STD_ERROR_HANDLE,
                                   STDERR_FILENO,
                                   stderr,
                                   &hReadStderrPipe);
         }
         else
         {
            error = redirectToPipe(STD_ERROR_HANDLE,
                                   STDERR_FILENO,
                                   stderr,
                                   &hReadStderrPipe,
                                   &hOrigStderrHandle);
         }

         if (error)
            return error;

         // capture stderr
         boost::thread stderrThread(boost::bind(standardStreamCaptureThread,
                                                std::string("Stderr"),
                                                hReadStderrPipe,
                                                hOrigStderrHandle,
                                                stderrHandler));
      }

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

