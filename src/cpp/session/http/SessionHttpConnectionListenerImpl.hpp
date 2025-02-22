/*
 * SessionHttpConnectionListenerImpl.hpp
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

#ifndef SESSION_HTTP_CONNECTION_LISTENER_IMPL_HPP
#define SESSION_HTTP_CONNECTION_LISTENER_IMPL_HPP

#include <queue>

#include <boost/shared_ptr.hpp>

#include <boost/utility.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Macros.hpp>
#include <core/BoostThread.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileLock.hpp>
#include <shared_core/Error.hpp>
#include <core/BoostErrors.hpp>
#include <core/system/System.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/http/SocketAcceptorService.hpp>

#include <core/FileSerializer.hpp>

#include <core/StringUtils.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionConstants.hpp>

#include <session/SessionHttpConnection.hpp>
#include <session/SessionHttpConnectionQueue.hpp>
#include <session/SessionHttpConnectionListener.hpp>

#include "SessionHttpConnectionImpl.hpp"
#include "../SessionUriHandlers.hpp"
#include "../SessionHttpMethods.hpp"
#include "../SessionRpc.hpp"
#include "../SessionInit.hpp"

using namespace boost::placeholders;

namespace rstudio {
namespace session {

class UploadVisitor : public boost::static_visitor<core::http::UriAsyncUploadHandlerFunction>
{
public:
   core::http::UriAsyncUploadHandlerFunction
   operator()(const core::http::UriAsyncHandlerFunction& func) const
   {
      // return empty function to signify that the func is not an upload handler
      return core::http::UriAsyncUploadHandlerFunction();
   }

   core::http::UriAsyncUploadHandlerFunction
   operator()(const core::http::UriAsyncUploadHandlerFunction& func) const
   {
      // return the func itself so it can be invoked
      return func;
   }
};

template <typename ProtocolType>
class HttpConnectionListenerImpl : public HttpConnectionListener,
                                   boost::noncopyable
{  
protected:
   HttpConnectionListenerImpl() : started_(false) {}

   void setSslContext(boost::shared_ptr<boost::asio::ssl::context> context)
   {
      sslContext_ = context;
   }

   // COPYING: boost::noncopyable
   
public:
   virtual core::Error start()
   {
      // cleanup any existing networking state
      core::Error error = cleanup();
      if (error)
         return error;

      // initialize acceptor
      error = initializeAcceptor(&acceptorService_);
      if (error)
         return error;

      // accept next connection (asynchronously)
      acceptNextConnection();
      
      // refresh locks
      core::FileLock::refreshPeriodically(acceptorService_.ioContext());

      // block all signals for launch of listener thread (will cause it
      // to never receive signals)
      core::system::SignalBlocker signalBlocker;
      error = signalBlocker.blockAll();
      if (error)
         return error;

      // launch the listener thread
      try
      {
         using boost::bind;
         boost::thread listenerThread(bind(&boost::asio::io_context::run,
                                           &(acceptorService_.ioContext())));
         listenerThread_ = MOVE_THREAD(listenerThread);

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
      boost::system::error_code ec;
      acceptorService_.closeAcceptor(ec);
      if (ec)
         LOG_ERROR(core::Error(ec, ERROR_LOCATION));

      // stop the server
      ioContext().stop();

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

   virtual bool eventsActive()
   {
      return eventsActive_;
   }

protected:

   virtual bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection)
   {
      // ensure request signature is valid
      if (!http_methods::verifyRequestSignature(ptrConnection->request()))
         return false;
      return true;
   }

private:
   // required subclass hooks
   virtual core::Error initializeAcceptor(
             core::http::SocketAcceptorService<ProtocolType>* pAcceptor) = 0;

   virtual bool validateConnection(
      boost::shared_ptr<HttpConnectionImpl<ProtocolType> > ptrConnection) = 0;

   virtual core::Error cleanup() = 0;

private:
   boost::asio::io_context& ioContext() { return acceptorService_.ioContext(); }

   void acceptNextConnection()
   {
      // create the connection
      ptrNextConnection_.reset( new HttpConnectionImpl<ProtocolType>(
            ioContext(),
            sslContext_,
            boost::bind(
                 &HttpConnectionListenerImpl<ProtocolType>::onHeadersParsed,
                 this,
                 _1),
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
            if (!core::isShutdownError(ec))
               LOG_ERROR(core::Error(ec, ERROR_LOCATION));
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
         acceptNextConnection();
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   void onHeadersParsed(boost::shared_ptr<HttpConnectionImpl<ProtocolType> > ptrConnection)
   {
      // convert to cannonical HttpConnection
      boost::shared_ptr<HttpConnection> ptrHttpConnection =
            boost::static_pointer_cast<HttpConnection>(ptrConnection);

      // check if request handler is an upload handler
      const core::http::Request& request = ptrConnection->request();
      std::string uri = request.uri();
      boost::optional<core::http::UriAsyncHandlerFunctionVariant> uriHandler =
        uri_handlers::handlers().handlerFor(uri);

      if (uriHandler)
      {
         core::http::UriAsyncUploadHandlerFunction func =
               boost::apply_visitor(UploadVisitor(), uriHandler.get());

         if (func)
            ptrConnection->setUploadHandler(func);
      }
   }

   // NOTE: this logic is duplicated btw here and NamedPipeConnectionListener

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
      if (connection::checkForAbort(
             ptrHttpConnection,
             boost::bind(&HttpConnectionListenerImpl<ProtocolType>::cleanup,
                         this)))
      {
         return;
      }

      // check for a suspend_session. done here as well as in foreground to
      // allow clients without the requisite client-id and/or version header
      // to also initiate a suspend (e.g. an admin/supervisor process)
      if (connection::checkForSuspend(ptrHttpConnection))
         return;
      
      if (connection::checkForInterrupt(ptrHttpConnection))
         return;

      // place the connection on the correct queue
      if (connection::isGetEvents(ptrHttpConnection))
      {
         eventsActive_ = true;
         eventsConnectionQueue_.enqueConnection(ptrHttpConnection);
      }
      else
      {
         // Turn off async rpc for the client upon seeing ClientInit until the first get_events arrives
         // since the client is not listening at first
         if (connection::isMethod(ptrHttpConnection, kClientInit))
         {
            eventsActive_ = false;
         }
         if (options().handleOfflineEnabled() && options().handleOfflineTimeoutMs() == 0 &&
             rpc::isOfflineableRequest(ptrHttpConnection) && init::isSessionInitializedAndRestored())
         {
            // TODO: handleOffline - should these be put into a separate queue and run in a dedicated thread?
            if (http_methods::protocolDebugEnabled())
            {
               std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
               std::chrono::duration<double> beforeTime = now - ptrConnection->receivedTime();
               LOG_DEBUG_MESSAGE("- Handle immediate:  " + ptrConnection->request().uri() +
                                 " after: " + core::string_utils::formatDouble(beforeTime.count(), 2));
               http_methods::handleConnection(ptrConnection, http_methods::ForegroundConnection);
               std::chrono::duration<double> afterTime = std::chrono::steady_clock::now() - now;
               LOG_DEBUG_MESSAGE("--- complete:        " + ptrConnection->request().uri() +
                                 " in: " + core::string_utils::formatDouble(afterTime.count(), 2));
            }
            else
               http_methods::handleConnection(ptrConnection, http_methods::ForegroundConnection);
            return;
         }
         if (options().asyncRpcEnabled() &&
             options().asyncRpcTimeoutMs() == 0 &&
             http_methods::isAsyncJsonRpcRequest(ptrHttpConnection) && 
             eventsActive_)
         {
            if (http_methods::protocolDebugEnabled())
            {
               std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
               std::chrono::duration<double> beforeTime = now - ptrConnection->receivedTime();
               LOG_DEBUG_MESSAGE("Async imm reply:     " + ptrConnection->request().uri() +
                                 " after: " + core::string_utils::formatDouble(beforeTime.count(), 2));
            }
            boost::shared_ptr<HttpConnection> asyncConnection = http_methods::handleAsyncRpc(ptrHttpConnection);
            if (asyncConnection)
               mainConnectionQueue_.enqueConnection(asyncConnection);
            // else: error in handling the rpc request, nothing left to do
         }
         else
         {
            mainConnectionQueue_.enqueConnection(ptrHttpConnection);
         }
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
   boost::thread listenerThread_;

   // flag indicating we've started
   std::atomic<bool> started_;

   // Set when the first get_events request is received - ensure async rpc not used until client is ready to listen
   std::atomic<bool> eventsActive_;

   boost::shared_ptr<boost::asio::ssl::context> sslContext_;
};

} // namespace session
} // namespace rstudio

#endif // SESSION_HTTP_CONNECTION_LISTENER_IMPL_HPP

