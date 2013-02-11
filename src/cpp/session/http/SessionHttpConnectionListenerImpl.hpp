/*
 * SessionHttpConnectionListenerImpl.hpp
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

#ifndef SESSION_HTTP_CONNECTION_LISTENER_IMPL_HPP
#define SESSION_HTTP_CONNECTION_LISTENER_IMPL_HPP

#include <queue>

#include <boost/shared_ptr.hpp>

#include <boost/utility.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/BoostThread.hpp>
#include <core/FilePath.hpp>
#include <core/Error.hpp>
#include <core/BoostErrors.hpp>
#include <core/system/System.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/http/SocketAcceptorService.hpp>

#include <core/FileSerializer.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionConstants.hpp>

#include <session/SessionHttpConnection.hpp>
#include <session/SessionHttpConnectionQueue.hpp>
#include <session/SessionHttpConnectionListener.hpp>

#include "SessionHttpConnectionImpl.hpp"


namespace session {

namespace {

bool isShutdownError(const boost::system::error_code& ec)
{
   // for windows check explicitly for is not a socket error
#ifdef _WIN32
   if (ec.value() == WSAENOTSOCK)
      return true;
#endif

   //  - operation cancelled (happens while shutting down the server)
   //  - invalid argument (happens if socket is closed before we
   //    can actually peform the handleAccept)
   //  - bad file descriptor (simillar to above)
   return (ec == boost::asio::error::operation_aborted ||
           ec == boost::asio::error::invalid_argument ||
           ec == boost::system::errc::bad_file_descriptor);
}

}


template <typename ProtocolType>
class HttpConnectionListenerImpl : public HttpConnectionListener,
                                   boost::noncopyable
{  
protected:
   HttpConnectionListenerImpl() : started_(false) {}

   // COPYING: boost::noncopyable
   
public:
   virtual core::Error start()
   {
      // cleanup any existing networking state
      core::Error error = cleanup();
      if (error)
         return error ;

      // initialize acceptor
      error = initializeAcceptor(&acceptorService_);
      if (error)
         return error;

      // accept next connection (asynchronously)
      acceptNextConnection();

      // block all signals for launch of listener thread (will cause it
      // to never receive signals)
      core::system::SignalBlocker signalBlocker;
      error = signalBlocker.blockAll();
      if (error)
         return error ;

      // launch the listener thread
      try
      {
         using boost::bind;
         boost::thread listenerThread(bind(&boost::asio::io_service::run,
                                           &(acceptorService_.ioService())));
         listenerThread_ = listenerThread.move();

         // set started flag
         started_ = true;

         return core::Success();
      }
      catch(const boost::thread_resource_error& e)
      {
         return core::Error(boost::thread_error::ec_from_exception(e),
                            ERROR_LOCATION);
      }
   }

   virtual void stop()
   {
      // don't stop if we never started
      if (!started_)
      {
         LOG_WARNING_MESSAGE("Stopping HttpConnectionListener "
                             "which wasn't started");
         return;
      }

      // close acceptor
      boost::system::error_code ec ;
      acceptorService_.closeAcceptor(ec);
      if (ec)
         LOG_ERROR(core::Error(ec, ERROR_LOCATION));

      // stop the server
      ioService().stop();

      // join the thread and wait for it complete
      if (listenerThread_.joinable())
      {
         if (!listenerThread_.timed_join(boost::posix_time::seconds(3)))
         {
            LOG_WARNING_MESSAGE(
                  "HttpConnectionListener didn't stop within 3 sec");
         }

         listenerThread_.detach();
      }

      // allow subclass specific cleanup
      core::Error error = cleanup();
      if (error)
         LOG_ERROR(error);
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

protected:

   virtual bool authenticate(boost::shared_ptr<HttpConnection>)
   {
      return true;
   }

private:
   // required subclass hooks
   virtual core::Error initializeAcceptor(
             core::http::SocketAcceptorService<ProtocolType>* pAcceptor) = 0;

   virtual bool validateConnection(
      boost::shared_ptr<HttpConnectionImpl<ProtocolType> > ptrConnection) = 0;

   virtual core::Error cleanup() = 0 ;

private:
   boost::asio::io_service& ioService() { return acceptorService_.ioService(); }

   void acceptNextConnection()
   {
      // create the connection
      ptrNextConnection_.reset( new HttpConnectionImpl<ProtocolType>(
            ioService(),
            boost::bind(
                 &HttpConnectionListenerImpl<ProtocolType>::enqueConnection,
                 this,
                 _1))
      );

      // wait for next connection
      acceptorService_.asyncAccept(
         ptrNextConnection_->socket(),
         boost::bind(&HttpConnectionListenerImpl<ProtocolType>::handleAccept,
                     this,
                     boost::asio::placeholders::error)
      );
   }


   void handleAccept(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // validate the connection
            if (validateConnection(ptrNextConnection_))
            {
               // start reading from the connection
               ptrNextConnection_->startReading();
            }
            else
            {
               // invalid client: close the connection immediately
               ptrNextConnection_->close();
            }
         }
         else
         {
            // for errors, log and continue,but don't log errors caused
            // by normal course of socket shutdown
            if (!isShutdownError(ec))
               LOG_ERROR(core::Error(ec, ERROR_LOCATION)) ;
         }
      }
      catch(const boost::system::system_error& e)
      {
         LOG_ERROR_MESSAGE(std::string("Unexpected exception: ") + e.what());
      }
      CATCH_UNEXPECTED_EXCEPTION

      // ALWAYS accept next connection
      try
      {
         acceptNextConnection() ;
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   void enqueConnection(
         boost::shared_ptr<HttpConnectionImpl<ProtocolType> > ptrConnection)
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
      if (checkForAbort(ptrHttpConnection))
         return;

      // place the connection on the correct queue
      if (isGetEvents(ptrHttpConnection))
         eventsConnectionQueue_.enqueConnection(ptrHttpConnection);
      else
         mainConnectionQueue_.enqueConnection(ptrHttpConnection);
   }

   static bool isMethod(boost::shared_ptr<HttpConnection> ptrConnection,
                        const std::string& method)
   {
      return boost::algorithm::ends_with(ptrConnection->request().uri(),
                                         "rpc/" + method);
   }

   static bool isGetEvents(boost::shared_ptr<HttpConnection> ptrConnection)
   {
      return boost::algorithm::ends_with(ptrConnection->request().uri(),
                                         "events/get_events");
   }

   static void handleAbortNextProjParam(
                  boost::shared_ptr<HttpConnection> ptrConnection)
   {
      std::string nextProj;
      core::json::JsonRpcRequest jsonRpcRequest;
      core::Error error = core::json::parseJsonRpcRequest(
                                            ptrConnection->request().body(),
                                            &jsonRpcRequest);
      if (!error)
      {
         error = core::json::readParam(jsonRpcRequest.params, 0, &nextProj);
         if (error)
            LOG_ERROR(error);

         if (!nextProj.empty())
         {
            // NOTE: this must be synchronized with the implementation of
            // ProjectContext::setNextSessionProject -- we do this using
            // constants rather than code so that this code (which runs in
            // a background thread) don't call into the projects module (which
            // is designed to be foreground and single-threaded)
            core::FilePath userScratch = session::options().userScratchPath();
            core::FilePath settings = userScratch.complete(kProjectsSettings);
            error = settings.ensureDirectory();
            if (error)
               LOG_ERROR(error);
            core::FilePath writePath = settings.complete(kNextSessionProject);
            core::Error error = core::writeStringToFile(writePath, nextProj);
            if (error)
               LOG_ERROR(error);
         }
      }
      else
      {
         LOG_ERROR(error);
      }
   }

   bool checkForAbort(
                  boost::shared_ptr<HttpConnection> ptrConnection)
   {
      if (isMethod(ptrConnection, "abort"))
      {
         // respond and log (try/catch so we are ALWAYS guaranteed to abort)
         try
         {
            // handle the nextProj param if it's specified
            handleAbortNextProjParam(ptrConnection);

            // respond
            ptrConnection->sendJsonRpcResponse();

            // log
            LOG_WARNING_MESSAGE("Abort requested");
         }
         catch(...)
         {
         }

         // cleanup (if we don't do this then the user may be locked out of
         // future requests). note that this should occur in the normal
         // course of a graceful shutdown but we do it here anyway just
         // to be paranoid
         try
         {
            cleanup();
         }
         catch(...)
         {
         }

         // abort
         ::abort();
         return true;
      }
      else
      {
         return false;
      }
   }

private:

   // acceptor service (includes io service)
   core::http::SocketAcceptorService<ProtocolType> acceptorService_;

   // next connection
   boost::shared_ptr<HttpConnectionImpl<ProtocolType> > ptrNextConnection_;

   // connection queues
   HttpConnectionQueue mainConnectionQueue_;
   HttpConnectionQueue eventsConnectionQueue_;

   // listener thread
   boost::thread listenerThread_ ;

   // flag indicating we've started
   bool started_;
};

} // namespace session

#endif // SESSION_HTTP_CONNECTION_LISTENER_IMPL_HPP

