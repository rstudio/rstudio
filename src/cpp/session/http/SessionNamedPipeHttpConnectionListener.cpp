/*
 * SessionNamedPipeHttpConnectionListener.cpp
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

#include <session/SessionHttpConnectionListener.hpp>

#include <string>

#include <boost/utility.hpp>
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

#include <core/system/Environment.hpp>

#include <windows.h>

#include <session/SessionOptions.hpp>

#include "SessionHttpConnectionUtils.hpp"

// TODO: detailed review of named pipe connection listener code

// TODO: memory management tests
// TODO: security for local session only
// TODO: consider addding PIPE_REJECT_REMOTE_CLIENTS flag

// TODO: should we make the listener thread interruptable?


using namespace core ;

namespace session {

namespace {

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
      CHAR buff[256];
      DWORD bytesRead;

      while(TRUE)
      {
         // read from pipe
         BOOL result = ::ReadFile(hPipe_, buff, sizeof(buff), &bytesRead, NULL);

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
      : started_(false), pipeName_(pipeName), secret_(secret)
   {
   }


   virtual Error start()
   {
      core::thread::safeLaunchThread(
         boost::bind(&NamedPipeHttpConnectionListener::listenerThread,
                     this),
         &listenerThread_);

      started_ = true;

      return Success();
   }

   virtual void stop()
   {
      // don't stop if we never started
      if (!started_)
      {
         LOG_WARNING_MESSAGE("Stopping NamedPipeHttpConnectionListener "
                             "which wasn't started");
         return;
      }

      if (listenerThread_.joinable())
      {
         listenerThread_.interrupt();

         if (!listenerThread_.timed_join(boost::posix_time::seconds(3)))
         {
            LOG_WARNING_MESSAGE(
               "NamedPipeHttpConnectionListener didn't stop within 3 sec");
         }

         listenerThread_.detach();
      }
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
            // create pipe
            HANDLE hPipe = ::CreateNamedPipeA(pipeName_.c_str(),
                                              PIPE_ACCESS_DUPLEX,
                                              PIPE_TYPE_BYTE |
                                              PIPE_READMODE_BYTE |
                                              PIPE_WAIT,
                                              PIPE_UNLIMITED_INSTANCES,
                                              4096,
                                              4096,
                                              0,
                                              NULL);
            if (hPipe == INVALID_HANDLE_VALUE)
            {
               LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
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
            boost::shared_static_cast<HttpConnection>(ptrConnection);

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

private:
   bool started_;
   std::string pipeName_;
   std::string secret_;
   boost::thread listenerThread_ ;
   HttpConnectionQueue mainConnectionQueue_;
   HttpConnectionQueue eventsConnectionQueue_;
};



// pointer to global connection listener singleton
HttpConnectionListener* s_pHttpConnectionListener = NULL ;

}  // anonymouys namespace


void initializeHttpConnectionListener()
{
   std::string pipeName = core::system::getenv("RS_LOCAL_PEER");
   std::string secret = session::options().sharedSecret();
   s_pHttpConnectionListener = new NamedPipeHttpConnectionListener(pipeName,
                                                                   secret);
   Error error = s_pHttpConnectionListener->start();
   if (error)
      LOG_ERROR(error);
}

HttpConnectionListener& httpConnectionListener()
{
   return *s_pHttpConnectionListener;
}


} // namespace session
