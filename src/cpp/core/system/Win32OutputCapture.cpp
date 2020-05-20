/*
 * Win32OutputCapture.cpp
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
       HANDLE hReadPipe,
       HANDLE hOrigStdHandle,
       const boost::function<void(const std::string&)>& outputHandler)
{
   try
   {
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

               if (hOrigStdHandle)
               {
                  bool result = false;
                  LOCK_MUTEX(s_mutex)
                  {
                     result = ::WriteFile(hOrigStdHandle, buffer, bytesRead, nullptr, nullptr);
                  }
                  END_LOCK_MUTEX

                  if (!result)
                  {
                     LOG_ERROR(LAST_SYSTEM_ERROR());
                  }
               }
            }
         }
         else
         {
            // we don't expect errors to ever occur (since the standard
            // streams are never closed) so log any that do and continue
            LOG_ERROR(LAST_SYSTEM_ERROR());
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

