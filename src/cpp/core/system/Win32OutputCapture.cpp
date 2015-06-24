/*
 * Win32OutputCapture.cpp
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

#include <windows.h>

#include <stdio.h>
#include <fcntl.h>

#include <boost/lexical_cast.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/BoostThread.hpp>
#include <core/BoostErrors.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace {

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
                     int* pReadFd)
{
   HANDLE hWritePipe;

   // Create pipe--this returns two file descriptors corresponding to the
   // read and write ends of the pipe, respectively. Note that we formerly used
   // CreatePipe here; for reasons that are unclear, we couldn't reassign
   // the descriptor (i.e. the _dup2 call below) for pipe handles created
   // this way when more than one user has RStudio open (see case 4230).
   int pdfs[2];
   if (!::_pipe(pdfs, 4096, O_TEXT))
      return systemError(errno, ERROR_LOCATION);

   // reset win32 standard handle
   hWritePipe = reinterpret_cast<HANDLE>(::_get_osfhandle(pdfs[1]));
   if (hWritePipe == INVALID_HANDLE_VALUE)
      return systemError(errno, ERROR_LOCATION);
   if (!::SetStdHandle(stdHandle, hWritePipe))
      return systemError(::GetLastError(), ERROR_LOCATION);

   // reassign the standard output/error file descriptor to the write end of
   // the pipe
   if (::_dup2(pdfs[1], _fileno(fpStdFile)) != 0)
      return systemError(errno, ERROR_LOCATION);

   // turn off buffering
   if (::setvbuf(fpStdFile, NULL, _IONBF, 0) != 0)
      return ioError("setvbuf", ERROR_LOCATION);

   // sync c++ std streams
   std::ios::sync_with_stdio();

   // return read descriptor
   *pReadFd = pdfs[0];
   return Success();
}

void standardStreamCaptureThread(
       int outputHandle,
       FILE* outputStream,
       int readFd,
       const boost::function<void(const std::string&)>& outputHandler)
{
   const int kBufferSize = 512;
   const int kMaxRetries = 16;
   char buffer[kBufferSize];
   int redirectTries = 0;
   try
   {
      while(true)
      {
         // read from the descriptor; this descriptor is attached to a pipe,
         // and this _read call blocks until we have some bytes or until the
         // descriptor is closed
         int bytesRead = ::_read(readFd, &buffer, kBufferSize);
         if (bytesRead > 0)
         {
            // if we got some bytes, invoke the output handler
            if (bytesRead > kBufferSize)
            {
               // this should never happen, but just to be safe, don't try to
               // alloc a std::string if we get more bytes than we asked for
               LOG_WARNING_MESSAGE("Requested " +
                  boost::lexical_cast<std::string>(kBufferSize) + " bytes, "
                  "but received " +
                  boost::lexical_cast<std::string>(bytesRead) + "bytes; "
                  "ignoring");
            }
            else
            {
               outputHandler(std::string(buffer, bytesRead));
            }
         }
         else if (bytesRead == 0)
         {
            // reading 0 bytes indicates that we've reached EOF, so we can
            // quit capturing (we don't expect this to happen)
            LOG_WARNING_MESSAGE("Reached end of input on standard stream");
            break;
         }
         else if (bytesRead < 0)
         {
            // if we've already used up all our retries, just stop trying
            // (we don't want to spin and fill the logs with errors)
            if (redirectTries++ >= kMaxRetries)
            {
               LOG_ERROR_MESSAGE("Could not redirect standard stream after " +
                  boost::lexical_cast<std::string>(redirectTries) + " tries; "
                  "giving up.");
               break;
            }

            // if we hit an error, attempt to recover by closing the pipe
            // we were reading from ...
            if (!_close(readFd))
            {
               LOG_ERROR(systemError(errno, ERROR_LOCATION));

               // wait just a few ms to yield to other threads/processes before
               // trying again
               boost::this_thread::sleep(boost::posix_time::milliseconds(10));
               continue;
            }
            // ... and establishing a new redirection pipe in its place.
            Error error = redirectToPipe(outputHandle, outputStream, &readFd);
            if (error)
            {
               LOG_ERROR(error);
               boost::this_thread::sleep(boost::posix_time::milliseconds(10));
               continue;
            }
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}

} // anonymous namespace

Error captureStandardStreams(
            const boost::function<void(const std::string&)>& stdoutHandler,
            const boost::function<void(const std::string&)>& stderrHandler)
{
   try
   {
      // redirect stdout
      int stdoutFd = 0;
      Error error = redirectToPipe(STD_OUTPUT_HANDLE, stdout, &stdoutFd);
      if (error)
         return error;

      // capture stdout
      boost::thread stdoutThread(boost::bind(standardStreamCaptureThread,
                                             STD_OUTPUT_HANDLE,
                                             stdout,
                                             stdoutFd,
                                             stdoutHandler));

      // optionally redirect stderror if handler was provided
      int stderrFd = 0;
      if (stderrHandler)
      {
         // redirect stderr
         error = redirectToPipe(STD_ERROR_HANDLE, stderr, &stderrFd);
         if (error)
            return error;

         // capture stderr
         boost::thread stderrThread(boost::bind(standardStreamCaptureThread,
                                                STD_ERROR_HANDLE,
                                                stderr,
                                                stderrFd,
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
