/*
 * SessionNamedPipeHttpConnectionListener.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <session/SessionHttpConnectionListener.hpp>

// Necessary to avoid compile error on Win x64
#include <winsock2.h>

#include <string>

#include <boost/utility.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/asio/buffer.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Thread.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/RequestParser.hpp>
#include <core/http/SocketUtils.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <windows.h>
#include <sddl.h>

// Vista+
#ifndef PIPE_REJECT_REMOTE_CLIENTS
#define PIPE_REJECT_REMOTE_CLIENTS 0x00000008
#endif

// Mingw doesn't have this declaration
#include <session/SessionOptions.hpp>

#include "SessionHttpConnectionUtils.hpp"

using namespace rstudio::core ;

#define kReadBufferSize 4096

namespace rstudio {
namespace session {

class NamedPipeHttpConnection : public HttpConnection,
                                boost::noncopyable
{
public:
   explicit NamedPipeHttpConnection(HANDLE hPipe)
      : hPipe_(hPipe)
   {
   }

   virtual ~NamedPipeHttpConnection()
   {
      try
      {
         close();
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   bool readRequest()
   {
      core::http::RequestParser parser;
      CHAR buff[kReadBufferSize];
      DWORD bytesRead;

      while(TRUE)
      {
         // read from pipe
         BOOL result = ::ReadFile(hPipe_, buff, kReadBufferSize, &bytesRead, NULL);

         // check for error
         if (!result)
         {
            Error error = systemError(::GetLastError(), ERROR_LOCATION);
            if (!core::http::isConnectionTerminatedError(error))
               LOG_ERROR(error);

            close();

            return false;
         }

         // end of file - we should never get this far (request parser
         // should signal that we have the full request bfore we get here)
         else if (bytesRead == 0)
         {
            LOG_WARNING_MESSAGE("ReadFile returned 0 bytes");

            core::http::Response response;
            response.setStatusCode(core::http::status::BadRequest);
            sendResponse(response);

            return false;
         }

         // got input
         else
         {
            // parse next chunk
            http::RequestParser::status status = parser.parse(
                                                   request_,
                                                   buff,
                                                   buff + bytesRead);

            // error - return bad request
            if (status == core::http::RequestParser::error)
            {
               core::http::Response response;
               response.setStatusCode(core::http::status::BadRequest);
               sendResponse(response);

               return false;
            }

            // incomplete -- keep reading
            else if (status == core::http::RequestParser::incomplete)
            {
               continue;
            }

            // got valid request -- handle it
            else
            {
               requestId_ = request_.headerValue("X-RS-RID");
               return true;
            }
         }
      }

      // keep compiler happy (we should never get here
      return false;
   }

   virtual const core::http::Request& request() { return request_; }

   virtual void sendResponse(const core::http::Response &response)
   {
      // get the buffers
      std::vector<boost::asio::const_buffer> buffers =response.toBuffers(
                                        core::http::Header::connectionClose());

      // write them
      DWORD bytesWritten;
      for (std::size_t i=0; i<buffers.size(); i++)
      {
         DWORD bytesToWrite = boost::asio::buffer_size(buffers[i]);
         BOOL success = ::WriteFile(
                  hPipe_,
                  boost::asio::buffer_cast<const unsigned char*>(buffers[i]),
                  bytesToWrite,
                  &bytesWritten,
                  NULL);

         if (!success || (bytesWritten != bytesToWrite))
         {
            // establish error
            Error error = systemError(::GetLastError(), ERROR_LOCATION);
            error.addProperty("request-uri", request_.uri());

            // log the error if it wasn't connection terminated
            if (!core::http::isConnectionTerminatedError(error))
               LOG_ERROR(error);

            // close and terminate
            close();
            break;
         }
      }
   }

   // close (occurs automatically after writeResponse, here in case it
   // need to be closed in other circumstances
   virtual void close()
   {
      if (hPipe_ != INVALID_HANDLE_VALUE)
      {
         if (!::FlushFileBuffers(hPipe_))
            LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));

         if (!::DisconnectNamedPipe(hPipe_))
            LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));

         if (!::CloseHandle(hPipe_))
            LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));

         hPipe_ = INVALID_HANDLE_VALUE;
     }
   }

   // other useful introspection methods
   virtual std::string requestId() const { return requestId_; }


private:
   HANDLE hPipe_;
   core::http::Request request_;
   std::string requestId_;
};


class NamedPipeHttpConnectionListener : public HttpConnectionListener,
                                        boost::noncopyable
{
public:
   explicit NamedPipeHttpConnectionListener(const std::string& pipeName,
                                            const std::string& secret)
      : pipeName_(pipeName), secret_(secret)
   {
   }


   virtual Error start()
   {
      core::thread::safeLaunchThread(
         boost::bind(&NamedPipeHttpConnectionListener::listenerThread,
                     this));

      return Success();
   }

   virtual void stop()
   {
      // we don't support stop because it is never called in desktop mode

   }

   // connection queues
   virtual HttpConnectionQueue& mainConnectionQueue()
   {
      return mainConnectionQueue_;
   }

   virtual HttpConnectionQueue& eventsConnectionQueue()
   {
      return eventsConnectionQueue_;
   }


private:
   void listenerThread()
   {
      try
      {
         while (true)
         {
            // create security attributes
            PSECURITY_ATTRIBUTES pSA = NULL;
            SECURITY_ATTRIBUTES sa;
            ZeroMemory(&sa, sizeof(sa));
            sa.nLength = sizeof(sa);
            sa.lpSecurityDescriptor = NULL;
            sa.bInheritHandle = FALSE;

            // get login session only descriptor -- proceed without one
            // if we fail since we don't have 100% assurance this will
            // work in all configurations and the world ends if we don't
            // proceed with creating the pipe
            sa.lpSecurityDescriptor = pipeServerSecurityDescriptor();
            if (sa.lpSecurityDescriptor)
               pSA = &sa;

            // set pipe mode, specify rejection of remote clients if >= vista
            DWORD dwPipeMode = PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT;
            if (core::system::isVistaOrLater())
                dwPipeMode |= PIPE_REJECT_REMOTE_CLIENTS;

            // create pipe
            HANDLE hPipe = ::CreateNamedPipeA(pipeName_.c_str(),
                                              PIPE_ACCESS_DUPLEX,
                                              dwPipeMode,
                                              PIPE_UNLIMITED_INSTANCES,
                                              kReadBufferSize,
                                              kReadBufferSize,
                                              0,
                                              pSA);
            DWORD lastError = ::GetLastError(); // capture err before LocalFree

            // free security descriptor if we used one
            if (pSA)
               ::LocalFree(pSA->lpSecurityDescriptor);

            // check for error
            if (hPipe == INVALID_HANDLE_VALUE)
            {
               LOG_ERROR(systemError(lastError, ERROR_LOCATION));
               continue;
            }

            // attempt to connect
            BOOL connected = ::ConnectNamedPipe(hPipe, NULL) ?
                   TRUE : (::GetLastError() == ERROR_PIPE_CONNECTED);

            if (connected)
            {
               // create connection
               boost::shared_ptr<NamedPipeHttpConnection> ptrPipeConnection(
                                         new NamedPipeHttpConnection(hPipe));

               // if we can successfully read a request then enque it
               if (ptrPipeConnection->readRequest())
                  enqueConnection(ptrPipeConnection);
            }
            else
            {
               LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
            }
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   // NOTE: this logic is duplicated btw here and HttpConnectionListenerImpl

   void enqueConnection(
                  boost::shared_ptr<NamedPipeHttpConnection> ptrConnection)
   {
      // convert to cannonical HttpConnection
      boost::shared_ptr<HttpConnection> ptrHttpConnection =
            boost::static_pointer_cast<HttpConnection>(ptrConnection);

      if (!authenticate(ptrHttpConnection))
      {
         core::http::Response response;
         response.setStatusCode(403);
         response.setStatusMessage("Forbidden");
         ptrConnection->sendResponse(response);
         return;
      }

      // check for the special rpc/abort endpoint and abort if requested
      // we do this in the background listener thread so it can always
      // be processed even if the foreground thread is deadlocked or otherwise
      // unresponsive
      if (connection::checkForAbort(
             ptrHttpConnection,
             boost::bind(&NamedPipeHttpConnectionListener::cleanup,
                         this)))
      {
         return;
      }

      // check for a suspend_session. done here as well as in foreground to
      // allow clients without the requisite client-id and/or version header
      // to also initiate a suspend (e.g. an admin/supervisor process)
      if (connection::checkForSuspend(ptrHttpConnection))
         return;

      // place the connection on the correct queue
      if (connection::isGetEvents(ptrHttpConnection))
         eventsConnectionQueue_.enqueConnection(ptrHttpConnection);
      else
         mainConnectionQueue_.enqueConnection(ptrHttpConnection);
   }

   virtual bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection)
   {
      return connection::authenticate(ptrConnection, secret_);
   }

   core::Error cleanup()
   {
      return Success();
   }

   static LPVOID pipeServerSecurityDescriptor()
   {
      // NOTE: if this doesn't work for whatever reason we could consider
      // falling back to this: "D:(D;;GA;;;AN)(A;;GA;;;AU)"
      // (which would be deny access to anonymous users and grant access
      // to authenticated users)

      std::string securityDescriptor;
      Error error = logonSessionOnlyDescriptor(&securityDescriptor);
      if (error)
      {
         LOG_ERROR(error);
         return NULL;
      }

      ULONG sdSize;
      LPVOID pSA;
      if (::ConvertStringSecurityDescriptorToSecurityDescriptorA(
          securityDescriptor.c_str(),
          SDDL_REVISION_1,
          &pSA,
          &sdSize))
      {
         return pSA;
      }
      else
      {
         LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
         return NULL;
      }
   }

   static core::Error logonSessionOnlyDescriptor(std::string* pDescriptor)
   {
      // token for current process
      HANDLE hToken = NULL;
      if (!OpenProcessToken(::GetCurrentProcess(), TOKEN_QUERY, &hToken))
         return systemError(::GetLastError(), ERROR_LOCATION);
      core::system::CloseHandleOnExitScope tokenScope(&hToken, ERROR_LOCATION);

      // size of token groups structure (note that we expect the error
      // since we pass NULL for the token information buffer)
      DWORD tgSize = 0;
      BOOL res = ::GetTokenInformation(hToken, TokenGroups, NULL, 0, &tgSize);
      if (res != FALSE && ::GetLastError() != ERROR_INSUFFICIENT_BUFFER)
         return systemError(::GetLastError(), ERROR_LOCATION);

      // get the token groups structure
      std::vector<char> tg(tgSize);
      TOKEN_GROUPS* pTG = reinterpret_cast<TOKEN_GROUPS*>(&tg[0]);
      if (!::GetTokenInformation(hToken, TokenGroups, pTG, tgSize, &tgSize))
         return systemError(::GetLastError(), ERROR_LOCATION);

      // find login sid
      SID* pSid = NULL;
      for (DWORD i = 0; i < pTG->GroupCount ; ++i)
      {
         if ((pTG->Groups[i].Attributes & SE_GROUP_LOGON_ID)
                                                   == SE_GROUP_LOGON_ID)
         {
           pSid = reinterpret_cast<SID*>(pTG->Groups[i].Sid);
           break;
         }
      }

      // ensure we found it
      if (pSid == NULL)
      {
         return systemError(boost::system::windows_error::file_not_found,
                            "Failed to find SE_GROUP_LOGON_ID",
                            ERROR_LOCATION);
      }

      // convert to a string
      char* pSidString = NULL;
      if (!::ConvertSidToStringSid(pSid, &pSidString))
         return systemError(::GetLastError(), ERROR_LOCATION);

      // format string for caller
      boost::format fmt("D:(A;OICI;GA;;;%1%)");
      *pDescriptor = boost::str(fmt % pSidString);

      // free sid string
      ::LocalFree(pSidString);

      // return success
      return Success();
   }


private:
   std::string pipeName_;
   std::string secret_;
   HttpConnectionQueue mainConnectionQueue_;
   HttpConnectionQueue eventsConnectionQueue_;
};

} // namespace session
} // namespace rstudio
