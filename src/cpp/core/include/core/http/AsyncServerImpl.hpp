/*
 * AsyncServerImpl.hpp
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

#ifndef CORE_HTTP_ASYNC_SERVER_IMPL_HPP
#define CORE_HTTP_ASYNC_SERVER_IMPL_HPP

#include <vector>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/variant/static_visitor.hpp>

#include <boost/asio/io_service.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/asio/deadline_timer.hpp>

#include <core/BoostThread.hpp>
#include <shared_core/Error.hpp>
#include <core/BoostErrors.hpp>
#include <core/Log.hpp>
#include <core/ScheduledCommand.hpp>
#include <core/system/System.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/AsyncServer.hpp>
#include <core/http/AsyncConnectionImpl.hpp>
#include <core/http/AsyncUriHandler.hpp>
#include <core/http/Util.hpp>
#include <core/http/UriHandler.hpp>
#include <core/http/URL.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/SocketAcceptorService.hpp>


namespace rstudio {
namespace core {
namespace http {

class UploadVisitor : public boost::static_visitor<AsyncUriUploadHandlerFunction>
{
public:
   AsyncUriUploadHandlerFunction operator()(const AsyncUriHandlerFunction& func) const
   {
      // return empty function to signify that the func is not an upload handler
      return AsyncUriUploadHandlerFunction();
   }

   AsyncUriUploadHandlerFunction operator()(const AsyncUriUploadHandlerFunction& func) const
   {
      // return the func itself so it can be invoked
      return func;
   }
};

template <typename ProtocolType>
class AsyncServerImpl : public AsyncServer, boost::noncopyable
{
public:
   AsyncServerImpl(const std::string& serverName,
                   const std::string& baseUri = std::string(),
                   bool disableOriginCheck = true,
                   const std::vector<boost::regex>& allowedOrigins = std::vector<boost::regex>(),
                   const Headers& additionalResponseHeaders = Headers())
      : acceptorService_(),
        abortOnResourceError_(false),
        serverName_(serverName),
        baseUri_(baseUri),
        originCheckDisabled_(disableOriginCheck),
        allowedOrigins_(allowedOrigins),
        additionalResponseHeaders_(additionalResponseHeaders),
        scheduledCommandInterval_(boost::posix_time::seconds(3)),
        scheduledCommandTimer_(acceptorService_.ioService()),
        running_(false)
   {
   }
   
   virtual ~AsyncServerImpl()
   {
   }

   virtual boost::asio::io_service& ioService()
   {
      return acceptorService_.ioService();
   }
   
   virtual void setAbortOnResourceError(bool abortOnResourceError)
   {
      BOOST_ASSERT(!running_);
      abortOnResourceError_ = abortOnResourceError;
   }

   virtual void addProxyHandler(const std::string& prefix,
                                const AsyncUriHandlerFunction& handler)
   {
      BOOST_ASSERT(!running_);
      uriHandlers_.add(AsyncUriHandler(baseUri_ + prefix, handler, true));
   }
   
   virtual void addHandler(const std::string& prefix,
                           const AsyncUriHandlerFunction& handler)
   {
      BOOST_ASSERT(!running_);
      uriHandlers_.add(AsyncUriHandler(baseUri_ + prefix, handler));
   }

   virtual void addUploadHandler(const std::string& prefix,
                                 const AsyncUriUploadHandlerFunction& handler)
   {
      BOOST_ASSERT(!running_);
      uriHandlers_.add(AsyncUriHandler(baseUri_ + prefix, handler));
   }

   virtual void addBlockingHandler(const std::string& prefix,
                                   const UriHandlerFunction& handler)
   {
      BOOST_ASSERT(!running_);
      addHandler(prefix,
                 boost::bind(handleAsyncConnectionSynchronously, handler, _1));
   }

   virtual void setDefaultHandler(const AsyncUriHandlerFunction& handler)
   {
      BOOST_ASSERT(!running_);
      defaultHandler_ = handler;
   }

   virtual void setBlockingDefaultHandler(const UriHandlerFunction& handler)
   {
      BOOST_ASSERT(!running_);
      setDefaultHandler(boost::bind(handleAsyncConnectionSynchronously,
                                    handler,
                                    _1));
   }

   virtual void setScheduledCommandInterval(
                                   boost::posix_time::time_duration interval)
   {
      BOOST_ASSERT(!running_);
      scheduledCommandInterval_ = interval;
   }

   virtual void addScheduledCommand(boost::shared_ptr<ScheduledCommand> pCmd)
   {
      BOOST_ASSERT(!running_);
      scheduledCommands_.push_back(pCmd);
   }

   virtual void setRequestFilter(RequestFilter requestFilter)
   {
      BOOST_ASSERT(!running_);
      requestFilter_ = requestFilter;
   }

   virtual void setResponseFilter(ResponseFilter responseFilter)
   {
      BOOST_ASSERT(!running_);
      responseFilter_ = responseFilter;
   }

   virtual Error runSingleThreaded()
   {

      // update state
      running_ = true;

      // get ready for next connection
      acceptNextConnection();

      // initialize scheduled command timer
      waitForScheduledCommandTimer();


      // run
      runServiceThread();


      return Success();
   }

   virtual Error run(std::size_t threadPoolSize = 1)
   {
      if (running_)
         return Success();

      try
      {
         // update state
         running_ = true;

         // get ready for next connection
         acceptNextConnection();

         // initialize scheduled command timer
         waitForScheduledCommandTimer();
         
         // block all signals for the creation of the thread pool
         // (prevents signals from occurring on any of the handler threads)
         core::system::SignalBlocker signalBlocker;
         Error error = signalBlocker.blockAll();
         if (error)
            return error;
      
         // create the threads
         for (std::size_t i=0; i < threadPoolSize; ++i)
         {
            // run the thread
            boost::shared_ptr<boost::thread> pThread(new boost::thread(
                              &AsyncServerImpl<ProtocolType>::runServiceThread,
                              this));
            
            // add to list of threads
            threads_.push_back(pThread);
         }
      }
      catch(const boost::thread_resource_error& e)
      {
         return Error(boost::thread_error::ec_from_exception(e),
                      ERROR_LOCATION);
      }
      
      return Success();
   }
   
   virtual void stop()
   {
      // close acceptor so we free up the main port immediately
      boost::system::error_code closeEc;
      acceptorService_.closeAcceptor(closeEc);
      if (closeEc)
         LOG_ERROR(Error(closeEc, ERROR_LOCATION));
      
      // stop the server 
      acceptorService_.ioService().stop();

      std::set<boost::weak_ptr<AsyncConnectionImpl<typename ProtocolType::socket> >> connections;
      boost::shared_ptr<AsyncConnectionImpl<typename ProtocolType::socket>> pendingConnection;
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         running_ = false;
         connections = connections_;
         pendingConnection = ptrNextConnection_;
      }
      END_LOCK_MUTEX

      // gracefully stop all open connections to ensure they are freed
      // before our io service (socket acceptor) is freed - if this is
      // not gauranteed, boost will crash when attempting to free socket objects
      //
      // note that we create a copy of the connections list here because as connections
      // are closed, they will remove themselves from the list
      //
      // we do this outside of the lock to prevent potential deadlock
      // since the connection mutex and the server mutex are intertwined here
      for (const auto& connection : connections)
      {
         if (auto instance = connection.lock())
            instance->close();
      }

      // the list should be empty now, but clear it to make sure
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         connections_.clear();
      }
      END_LOCK_MUTEX

      // ensure we "close" the empty connection that is always created to handle the next incoming connection
      // if we do not specifically close it here, it will attempt to close itself when no shared_ptr to
      // it is held by this server object, causing a bad_weak_ptr exception to be thrown
      ptrNextConnection_->close();
   }
   
   virtual void waitUntilStopped()
   {
      // wait until all of the threads in the pool exit
      for (std::size_t i=0; i < threads_.size(); ++i)
         threads_[i]->join();
   }
   
   virtual bool isRunning()
   {
      return running_;
   }

   void setNotFoundHandler(const NotFoundHandler& handler)
   {
      notFoundHandler_ = handler;
   }

   virtual typename ProtocolType::acceptor::endpoint_type localEndpoint()
   {
      return acceptorService_.acceptor().local_endpoint();
   }
   
private:

   void runServiceThread()
   {
      try
      {
         boost::system::error_code ec;
         acceptorService_.ioService().run(ec);
         if (ec)
            LOG_ERROR(Error(ec, ERROR_LOCATION));
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   void addConnection(const boost::weak_ptr<AsyncConnectionImpl<typename ProtocolType::socket>>& connection)
   {
      // add connection to our map
      // note that we only hold a weak_ptr to the connection so that it can go out of scope on its own
      // if we didn't allow this, unused (finished) connections may never close
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         connections_.insert(connection);
      }
      END_LOCK_MUTEX
   }

   void onConnectionClosed(const boost::weak_ptr<AsyncConnectionImpl<typename ProtocolType::socket>>& connection)
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         connections_.erase(connection);
      }
      END_LOCK_MUTEX
   }

   void acceptNextConnection()
   {
      ptrNextConnection_.reset(
               new AsyncConnectionImpl<typename ProtocolType::socket> (

         // controlling io_service
         acceptorService_.ioService(),

         // optional ssl context - only used for SSL connections
         sslContext_,

         // headers parsed handler
         boost::bind(&AsyncServerImpl<ProtocolType>::onHeadersParsed,
                     this, _1, _2),

         // connection handler
         boost::bind(&AsyncServerImpl<ProtocolType>::handleConnection,
                     this, _1, _2),

         // close handler
         boost::bind(&AsyncServerImpl<ProtocolType>::onConnectionClosed,
                     this, _1),

         // request filter
         boost::bind(&AsyncServerImpl<ProtocolType>::connectionRequestFilter,
                     this, _1, _2, _3),

         // response filter
         boost::bind(&AsyncServerImpl<ProtocolType>::connectionResponseFilter,
                     this, _1, _2)
      ));

      // wait for next connection
      acceptorService_.asyncAccept(
         ptrNextConnection_->socket(),
         boost::bind(&AsyncServerImpl<ProtocolType>::handleAccept,
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
            boost::weak_ptr<AsyncConnectionImpl<typename ProtocolType::socket>> weak(ptrNextConnection_);
            addConnection(weak);
            ptrNextConnection_->startReading();
         }
         else
         {
            // for errors, log and continue (but don't log operation aborted
            // or bad file descriptor since it happens in the ordinary course
            // of shutting down the server)
            if (ec != boost::asio::error::operation_aborted &&
                ec != boost::asio::error::bad_descriptor)
            {
               // log the error
               LOG_ERROR(Error(ec, ERROR_LOCATION));
               
               // check for resource exhaustion
               checkForResourceExhaustion(ec, ERROR_LOCATION);
            }
         }
      }
      catch(const boost::system::system_error& e)
      {
         // always log
         LOG_ERROR_MESSAGE(std::string("Unexpected exception: ") + e.what());
         
         // check for resource exhaustion
         checkForResourceExhaustion(e.code(), ERROR_LOCATION);
      }
      CATCH_UNEXPECTED_EXCEPTION

      // ALWAYS accept next connection
      try
      {
         acceptNextConnection();
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   
   bool onHeadersParsed(
         boost::shared_ptr<AsyncConnectionImpl<typename ProtocolType::socket> > pConnection,
         http::Request* pRequest)
   {
      try
      {
         // call subclass
         onRequest(&(pConnection->socket()), pRequest);

         // convert to cannonical HttpConnection
         boost::shared_ptr<AsyncConnection> pAsyncConnection =
             boost::static_pointer_cast<AsyncConnection>(pConnection);

         // set the default 404 not found handler on the response if we have one
         if (notFoundHandler_)
            pAsyncConnection->response().setNotFoundHandler(notFoundHandler_);

         std::string uri = pRequest->uri();
         AsyncUriHandler handler = uriHandlers_.handlerFor(uri);
         boost::optional<AsyncUriHandlerFunctionVariant> handlerFunc = handler.function();

         if (!handler.isProxyHandler())
         {
            // check to ensure the request is for a supported method
            // proxy handlers do not perform this checking as we
            // flow all traffic like a proxy
            const std::string& method = pRequest->method();
            if (method != "GET" &&
                method != "POST" &&
                method != "HEAD" &&
                method != "PUT" &&
                method != "OPTIONS")
            {
               // invalid method - fail out
               LOG_ERROR_MESSAGE("Invalid method " + method + " requested for uri: " + pRequest->uri());
               pConnection->response().setStatusCode(http::status::MethodNotAllowed);
               return false;
            }

            if (!originCheckDisabled_)
            {
               // cross-origin check: ensure that the Origin or Referer headers match the target origin
               // this is a basic security precaution recommended here:
               // https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
               std::string originator = pRequest->headerValue("Origin");
               if (originator.empty())
                  originator = pRequest->headerValue("Referer");

               // get the actual host from the originator as it is possible for it to be a full URL
               originator = URL(originator).host();

               // get the host header, which indicates the destination for the request
               // we check for proxy values first as any reverse proxies will modify the host header
               // **Always use proxied URI:** the path may be a little off but the host here is always
               // correct and that's what we need to use to confirm a cross-origin violation.
               std::string host = URL(pRequest->proxiedUri()).host();

               if (!originator.empty() && originator != host)
               {
                  // origin does not match destination, but that might be okay
                  // we will not reject the request if the originator matches an allowed origin
                  bool originMatches = false;
                  for (const boost::regex& re : allowedOrigins_)
                  {
                     boost::smatch match;
                     if (boost::regex_match(originator, re))
                     {
                        originMatches = true;
                        break;
                     }
                  }

                  if (!originMatches)
                  {
                     LOG_ERROR_MESSAGE("Rejecting request with mismatched originator " + originator + " - "
                                       "expected: " + host + " for URI " + pRequest->uri());
                     pConnection->response().setStatusCode(http::status::BadRequest);
                     return false;
                  }
               }
            }
         }

         if (handlerFunc)
         {
            // determine if this is an upload handler
            // if it is, we need to deliver the body in pieces instead of
            // invoking one callback when the entire message has been parsed
            AsyncUriUploadHandlerFunction func = boost::apply_visitor(UploadVisitor(), handlerFunc.get());
            if (func)
               pConnection->setUploadHandler(func);
         }

         return true;
      }
      catch(const boost::system::system_error& e)
      {
         // always log
         LOG_ERROR_MESSAGE(std::string("Unexpected exception: ") + e.what());

         // check for resource exhaustion
         checkForResourceExhaustion(e.code(), ERROR_LOCATION);
      }
      CATCH_UNEXPECTED_EXCEPTION

      return false;
   }

   void handleConnection(
         boost::shared_ptr<AsyncConnectionImpl<typename ProtocolType::socket> > pConnection,
         http::Request* pRequest)
   {
      try
      {
         // convert to cannonical HttpConnection
         boost::shared_ptr<AsyncConnection> pAsyncConnection =
             boost::static_pointer_cast<AsyncConnection>(pConnection);

         // call the appropriate handler to generate a response
         std::string uri = pRequest->uri();
         AsyncUriHandler handler = uriHandlers_.handlerFor(uri);
         boost::optional<AsyncUriHandlerFunctionVariant> handlerFunc = handler.function();

         // if no handler was assigned but we have a default, use it instead
         if (!handlerFunc && defaultHandler_)
            handlerFunc = defaultHandler_;

         // call handler if we have one
         if (handlerFunc)
         {
            visitHandler(handlerFunc.get(), pAsyncConnection);
         }
         else
         {
            // log error
            LOG_ERROR_MESSAGE("Handler not found for uri: " + pRequest->uri());
            
            // return 404 not found
            sendNotFoundError(pConnection);
         }
      }
      catch(const boost::system::system_error& e)
      {
         // always log
         LOG_ERROR_MESSAGE(std::string("Unexpected exception: ") + e.what());
         
         // check for resource exhaustion
         checkForResourceExhaustion(e.code(), ERROR_LOCATION);
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   void connectionRequestFilter(
            boost::asio::io_service& ioService,
            http::Request* pRequest,
            http::RequestFilterContinuation continuation)
   {
      if (requestFilter_)
         requestFilter_(ioService, pRequest, continuation);
      else
         continuation(boost::shared_ptr<http::Response>());
   }

   void connectionResponseFilter(const http::Request& originalRequest,
                                 http::Response* pResponse)
   {
      // set server header (evade ref-counting to defend against
      // non-threadsafe std::string implementations)
      pResponse->setHeader("Server", std::string(serverName_.c_str()));

      // set additional headers
      for (const Header& header : additionalResponseHeaders_)
      {
         pResponse->setHeader(header);
      }

      if (responseFilter_)
         responseFilter_(originalRequest, pResponse);
   }

   void waitForScheduledCommandTimer()
   {
      // set expiration time for 3 seconds from now
      boost::system::error_code ec;
      scheduledCommandTimer_.expires_from_now(scheduledCommandInterval_, ec);

      // attempt to schedule timer (should always succeed but
      // include error check to be paranoid/robust)
      if (!ec)
      {
         scheduledCommandTimer_.async_wait(boost::bind(
               &AsyncServerImpl<ProtocolType>::handleScheduledCommandTimer,
               this,
               boost::asio::placeholders::error));
      }
      else
      {
         // unexpected error setting timer. log it
         LOG_ERROR(Error(ec, ERROR_LOCATION));
      }
   }

   void handleScheduledCommandTimer(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // execute all commands
            std::for_each(scheduledCommands_.begin(),
                          scheduledCommands_.end(),
                          boost::bind(&ScheduledCommand::execute, _1));

            // remove any commands which are finished
            scheduledCommands_.erase(
                 std::remove_if(scheduledCommands_.begin(),
                                scheduledCommands_.end(),
                                boost::bind(&ScheduledCommand::finished, _1)),
                 scheduledCommands_.end());

           // wait for the timer again
           waitForScheduledCommandTimer();

         }
         else
         {
            if (ec != boost::system::errc::operation_canceled)
               LOG_ERROR(Error(ec, ERROR_LOCATION));
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

protected: 
   SocketAcceptorService<ProtocolType>& acceptorService()
   {
      return acceptorService_;
   }

   void setSslContext(boost::shared_ptr<boost::asio::ssl::context> context)
   {
      // sets ssl context, enabling the usage of ssl for incoming connections
      sslContext_ = context;
   }
   
private:

   virtual void onRequest(typename ProtocolType::socket* pSocket,
                          http::Request* pRequest)
   {
      
   }
   
   void maybeAbortServer(const std::string& message, 
                         const core::ErrorLocation& location)
   {
      if (abortOnResourceError_)
      {
         core::log::logErrorMessage("(ABORTING SERVER): " + message, location);
         ::abort();
      }
      else
      {
         core::log::logWarningMessage(
                  "Resource exhaustion error occurred (continuing to run)",
                  location);
      }
   }
   
   void checkForResourceExhaustion(const boost::system::error_code& ec,
                                   const core::ErrorLocation& location)
   {
      if ( ec.category() == boost::system::system_category() &&
          (ec.value() == boost::system::errc::too_many_files_open ||
           ec.value() == boost::system::errc::not_enough_memory) )
      {
         // our process has run out of memory or file handles. in this 
         // case the only way future requests can be serviced is if we 
         // abort and allow upstart to respawn us
         maybeAbortServer("Resource exhaustion", location);
      }
   }

   static void handleAsyncConnectionSynchronously(
                        const UriHandlerFunction& uriHandlerFunction,
                        boost::shared_ptr<AsyncConnection> pConnection)
   {
      uriHandlerFunction(pConnection->request(), &(pConnection->response()));
      pConnection->writeResponse();
   }

   void sendNotFoundError(const boost::shared_ptr<AsyncConnectionImpl<typename ProtocolType::socket> >&
                             pConnection)
   {
      if (notFoundHandler_)
      {
         notFoundHandler_(pConnection->request(), &pConnection->response());
         return;
      }

      pConnection->response().setStatusCode(http::status::NotFound);
   }

private:
   boost::recursive_mutex mutex_;
   SocketAcceptorService<ProtocolType> acceptorService_;
   bool abortOnResourceError_;
   std::string serverName_;
   std::string baseUri_;
   bool originCheckDisabled_;
   std::vector<boost::regex> allowedOrigins_;
   Headers additionalResponseHeaders_;
   boost::shared_ptr<boost::asio::ssl::context> sslContext_;
   boost::shared_ptr<AsyncConnectionImpl<typename ProtocolType::socket> > ptrNextConnection_;
   std::set<boost::weak_ptr<AsyncConnectionImpl<typename ProtocolType::socket> >> connections_;
   AsyncUriHandlers uriHandlers_;
   AsyncUriHandlerFunction defaultHandler_;
   std::vector<boost::shared_ptr<boost::thread> > threads_;
   boost::posix_time::time_duration scheduledCommandInterval_;
   boost::asio::deadline_timer scheduledCommandTimer_;
   std::vector<boost::shared_ptr<ScheduledCommand> > scheduledCommands_;
   RequestFilter requestFilter_;
   ResponseFilter responseFilter_;
   NotFoundHandler notFoundHandler_;
   bool running_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_SERVER_IMPL_HPP


