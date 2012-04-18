/*
 * Win32OutputCapture.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <core/system/OutputCapture.hpp>

#include <windows.h>

#include <stdio.h>
#include <fcntl.h>


#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/BoostThread.hpp>
#include <core/BoostErrors.hpp>

namespace core {
namespace system {


namespace {

void standardStreamCaptureThread(
       HANDLE hReadPipe,
       const boost::function<void(const std::string&)>& outputHandler)
{
   try
   {
      while(true)
      {
         const int kBufferSize = 512;
         char buffer[kBufferSize];
         DWORD bytesRead = 0;
         if (::ReadFile(hReadPipe, &buffer, kBufferSize, &bytesRead, NULL))
         {
            if (bytesRead > 0)
               outputHandler(std::string(buffer, bytesRead));
         }
         else
         {
            // we don't expect errors to ever occur (since the standard
            // streams are never closed) so log any that do and continue
            LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}

Error ioError(const std::string& description, const ErrorLocation& location)
{
   boost::system::error_code ec(boost::system::errc::io_error,
                                boost::system::get_system_category());
   Error error(ec, location);
   error.addProperty("description", description);
   return error;
}

Error redirectToPipe(DWORD stdHandle,
                     FILE* fpStdFile,
                     HANDLE* phReadPipe)
{
   // create pipe
   HANDLE hWritePipe;
   if (!::CreatePipe(phReadPipe, &hWritePipe, NULL, 0))
      return systemError(::GetLastError(), ERROR_LOCATION);

   // reset win32 standard handle
   if (!::SetStdHandle(stdHandle, hWritePipe))
      return systemError(::GetLastError(), ERROR_LOCATION);

   // reset c runtime library handle
   int fd = ::_open_osfhandle((intptr_t)hWritePipe, _O_TEXT);
   if (fd == -1)
      return ioError("_open_osfhandle", ERROR_LOCATION);
   if (::_dup2(fd, _fileno(fpStdFile)) != 0)
      return systemError(errno, ERROR_LOCATION);

   // turn off buffering
   if (::setvbuf(fpStdFile, NULL, _IONBF, 0) != 0)
      return ioError("setvbuf", ERROR_LOCATION);

   // sync c++ std streams
   std::ios::sync_with_stdio();

   return Success();
}


} // anonymous namespace

Error captureStandardStreams(
            const boost::function<void(const std::string&)>& stdoutHandler,
            const boost::function<void(const std::string&)>& stderrHandler)
{
   try
   {
      // redirect stdout
      HANDLE hReadStdoutPipe = NULL;
      Error error = redirectToPipe(STD_OUTPUT_HANDLE, stdout, &hReadStdoutPipe);
      if (error)
         return error;

      // capture stdout
      boost::thread stdoutThread(boost::bind(standardStreamCaptureThread,
                                             hReadStdoutPipe,
                                             stdoutHandler));

      // optionally redirect stderror if handler was provided
      HANDLE hReadStderrPipe = NULL;
      if (stderrHandler)
      {
         // redirect stderr
         error = redirectToPipe(STD_ERROR_HANDLE, stderr, &hReadStderrPipe);
         if (error)
            return error;

         // capture stderr
         boost::thread stderrThread(boost::bind(standardStreamCaptureThread,
                                                hReadStderrPipe,
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

