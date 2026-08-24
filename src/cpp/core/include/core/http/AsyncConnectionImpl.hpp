/*
 * AsyncConnectionImpl.hpp
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

#ifndef CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP
#define CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP

#include <atomic>

#include <boost/array.hpp>
#include <boost/optional.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/date_time/posix_time/ptime.hpp>

#include <boost/asio/write.hpp>
#include <boost/asio/error.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/asio/post.hpp>
#include <boost/asio/ssl.hpp>
#include <boost/asio/ip/tcp.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>

#include <core/http/AsyncUriHandler.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/StreamWriter.hpp>
#include <core/http/RequestParser.hpp>
#include <core/http/AsyncConnection.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace http {

typedef boost::function<void(const boost::system::error_code&, std::size_t)> ReadHandler;
typedef boost::function<void(const boost::system::error_code&, bool)> WriteHandler;

class ISocketOperations
{
public:
   virtual void asyncReadSome(const boost::asio::mutable_buffer& buffers, ReadHandler handler) = 0;
   virtual void asyncWrite(const boost::asio::mutable_buffer& buffers, Socket::Handler handler) = 0;
   virtual void asyncWrite(const boost::asio::const_buffer& buffers, Socket::Handler handler) = 0;
   virtual void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers, Socket::Handler handler) = 0;
};

template <typename StreamType>
class SocketOperations : public ISocketOperations
{
public:
   SocketOperations(const boost::shared_ptr<StreamType>& stream,
                    boost::asio::io_context::strand& strand) :
                          stream_(stream),
                          strand_(strand)
   {
   }

   virtual ~SocketOperations()
   {
   }

   virtual void asyncReadSome(const boost::asio::mutable_buffer& buffers, ReadHandler handler)
   {
      stream_->async_read_some(buffers, boost::asio::bind_executor(strand_, handler));
   }

   virtual void asyncWrite(const boost::asio::mutable_buffer& buffers, Socket::Handler handler)
   {
      boost::asio::async_write(*stream_, buffers, boost::asio::bind_executor(strand_, handler));
   }

   virtual void asyncWrite(const boost::asio::const_buffer& buffer, Socket::Handler handler)
   {
      boost::asio::async_write(*stream_, buffer, boost::asio::bind_executor(strand_, handler));
   }

   virtual void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers, Socket::Handler handler)
   {
      boost::asio::async_write(*stream_, buffers, boost::asio::bind_executor(strand_, handler));
   }

private:
   boost::shared_ptr<StreamType> stream_;
   boost::asio::io_context::strand& strand_;
};

template <typename SocketType>
class AsyncConnectionImpl :
   public AsyncConnection,
   public boost::enable_shared_from_this<AsyncConnectionImpl<SocketType> >,
   boost::noncopyable
{
public:
   typedef boost::function<void(
         boost::shared_ptr<AsyncConnectionImpl<SocketType> >,
         http::Request*)> Handler;

    typedef boost::function<void(
         boost::weak_ptr<AsyncConnectionImpl<SocketType>>, bool, bool)> ClosedHandler;

   typedef boost::function<bool(
         boost::shared_ptr<AsyncConnectionImpl<SocketType> >,
         http::Request*)> HeadersParsedHandler;

public:
   AsyncConnectionImpl(boost::asio::io_context& ioContext,
                       boost::shared_ptr<boost::asio::ssl::context> sslContext,
                       long requestSequence,
                       const HeadersParsedHandler& onHeadersParsed,
                       const Handler& onRequestParsed,
                       const ClosedHandler& onClosed,
                       const RequestFilter& requestFilter = RequestFilter(),
                       const ResponseFilter& responseFilter = ResponseFilter())
      : ioContext_(ioContext),
        onHeadersParsed_(onHeadersParsed),
        onRequestParsed_(onRequestParsed),
        onClosed_(onClosed),
        requestFilter_(requestFilter),
        responseFilter_(responseFilter),
        closed_(false),
        requestSequence_(requestSequence),
        bytesTransferred_(0),
        strand_(ioContext)
        
   {
      if (sslContext)
      {
         sslStream_.reset(new boost::asio::ssl::stream<SocketType>(ioContext, *sslContext));

         // get socket and store it in a separate shared pointer
         // the owner is the SSL stream pointer - this ensures we don't double delete
         socket_.reset(sslStream_, &sslStream_->next_layer());

         socketOperations_.reset(new SocketOperations<boost::asio::ssl::stream<SocketType> >(sslStream_, strand_));
      }
      else
      {
         socket_.reset(new SocketType(ioContext));
         socketOperations_.reset(new SocketOperations<SocketType>(socket_, strand_));
      }
      request_.setRequestSequence(requestSequence);
   }

   virtual ~AsyncConnectionImpl()
   {
      try
      {
         close(true);
      }
      catch(...)
      {
      }
   }

   SocketType& socket()
   {
      return *socket_;
   }

   void startReading()
   {
      startTime_ = boost::posix_time::microsec_clock::universal_time();
      request_.setStartTime(startTime_);

      if (sslStream_)
      {
         // begin ssl handshake
         sslStream_->async_handshake(boost::asio::ssl::stream_base::server,
                                     boost::bind(&AsyncConnectionImpl<SocketType>::handleHandshake,
                                                 AsyncConnectionImpl<SocketType>::shared_from_this(),
                                                 boost::asio::placeholders::error));
      }
      else
      {
         readSome();
      }
   }

   virtual boost::asio::io_context& ioContext()
   {
      return ioContext_;
   }

   virtual const http::Request& request() const
   {
      return request_;
   }

   virtual http::Response& response()
   {
      return response_;
   }

   virtual void writeResponse(bool close = true, Socket::Handler handler = Socket::NullHandler)
   {
      if (!claimResponse(handler))
         return;

      writeResponseImpl(close, handler);
   }

   virtual void writeResponse(const http::Response& response,
                              bool close = true,
                              const Headers& additionalHeaders = Headers(),
                              Socket::Handler handler = Socket::NullHandler)
   {
      // Claim before mutating response_, not just before the socket write: if
      // a response (or only its headers) has already started, an in-flight
      // asyncWrite may still hold buffers pointing into response_'s current
      // backing storage -- assign()ing over it here would corrupt those
      // buffers mid-write, not just send a logically-duplicate response.
      if (!claimResponse(handler))
         return;

      response_.assign(response, additionalHeaders);

      // straight to the impl: we already hold the claim, and claiming a second
      // time through the public overload would now fail against ourselves.
      writeResponseImpl(close, handler);
   }

   // Writes the headers already staged in response().
   //
   // Prefer the overload below where the response can be handed over instead:
   // this one cannot protect a caller that populates response() itself, since
   // the claim is only reached after that mutation has happened. See
   // AsyncConnection::response()'s threading note.
   virtual void writeResponseHeaders(Socket::Handler handler)
   {
      // Same claim-before-mutating-response_ rationale as writeResponse()/
      // writeError(): an in-flight asyncWrite of a prior
      // writeResponseHeaders() call may still hold buffers pointing into
      // response_'s current backing storage.
      if (!claimResponse(handler))
         return;

      writeResponseHeadersImpl(handler);
   }

   // Assemble-and-write in one claimed step: the caller hands over the response
   // to send rather than staging it in response() first, so nothing mutates
   // response_'s storage until the claim has been won.
   //
   // That ordering is the whole point. A previous winner's asyncWrite holds
   // buffers that point straight into response_'s member strings, and it is
   // still in flight after the call that started it returned -- running on
   // strand_ does not wait for it, a strand only serializes the handlers that
   // start such writes. So a caller that assigns first and asks second can free
   // bytes asio is mid-read even in the case where it never writes anything at
   // all because its claim fails.
   virtual void writeResponseHeaders(const http::Response& response,
                                     Socket::Handler handler)
   {
      if (!claimResponse(handler))
         return;

      response_.assign(response);
      writeResponseHeadersImpl(handler);
   }

   virtual void writeError(const Error& error)
   {
      // Same claim-before-mutating-response_ rationale as the writeResponse()
      // overload above: setError() below must not run once a response has
      // already started.
      if (!claimResponse(Socket::NullHandler))
         return;

      response_.setError(error);
      writeResponseImpl(true, Socket::NullHandler);
   }

   // satisfy lower-level http::Socket interface (used when the connection
   // is upgraded to a websocket connection and no longer conforms to the
   // request/response protocol used by the class in the ordinary course
   // of business)

   virtual void asyncReadSome(boost::asio::mutable_buffer buffer,
                              Socket::Handler handler)
   {
      socketOperations_->asyncReadSome(buffer, handler);
   }

   virtual void asyncWrite(
                     const std::vector<boost::asio::const_buffer>& buffers,
                     Socket::Handler handler)
   {
      socketOperations_->asyncWrite(buffers, handler);
   }

   virtual void asyncWrite(
                     const boost::asio::const_buffer& buffer,
                     Socket::Handler handler)
   {
      socketOperations_->asyncWrite(buffer, handler);
   }

   virtual void close()
   {
      close(false);
   }

   virtual void close(bool fromDestructor)
   {
      if (fromDestructor && !closed_)
      {
         LOG_DEBUG_MESSAGE("Closing connection without an explicit response for URI: " + request().debugInfo());
      }

      // ensure the socket is only closed once - boost considers
      // multiple closes an error, and this can lead to a segfault
      ClosedHandler closedHandler;
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         if (!closed_)
         {
            Error error = closeSocket(*socket_);
            if (error && !core::http::isConnectionTerminatedError(error))
               logConnectionError(error);

            closed_ = true;
            closedHandler = onClosed_;

            // cleanup any associated data with the connection
            connectionData_.clear();
         }
      }
      END_LOCK_MUTEX;

      // notify that we have closed the connection
      // we do this after giving up the mutex to prevent potential deadlock
      if (closedHandler)
         closedHandler(AsyncConnectionImpl<SocketType>::weak_from_this(), fromDestructor, requestParsed_);
   }

   virtual boost::asio::io_context::strand& getStrand()
   {
      return strand_;
   }

   void setUploadHandler(const AsyncUriUploadHandlerFunction& handler)
   {
      FormHandler formHandler = boost::bind(handler,
                                            AsyncConnectionImpl<SocketType>::shared_from_this(),
                                            _1,
                                            _2);

      requestParser_.setFormHandler(formHandler);
   }

   virtual void continueParsing()
   {
      // continue parsing by reinvoking the read handler
      // with the amount of bytes that were read last time it was called
      // this is posted to be invoked asynchronously
      // so callers are not reentrantly locked
      //
      // Posted through strand_ rather than bare onto the io_context: every
      // other invocation of handleRead() arrives on it (readSome() binds its
      // completion to it), and a resumed parse is not a passive operation --
      // it can decide to write a response of its own (RequestParser::error ->
      // writeResponse(BadRequest), or callHandler()). Our caller is a body
      // writer running somewhere else entirely -- FormProxy resumes us from
      // the *downstream* connection's handlers -- so without the strand the
      // resumed parse runs concurrently with this connection's own reads,
      // writes and response state.
      boost::asio::post(
               strand_,
               boost::bind(
                  &AsyncConnectionImpl<SocketType>::handleRead,
                  AsyncConnectionImpl<SocketType>::shared_from_this(),
                  boost::system::error_code(),
                  bytesTransferred_));
   }

   virtual void setData(const boost::any& data)
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         connectionData_ = data;
      }
      END_LOCK_MUTEX
   }

   virtual boost::any getData()
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         return connectionData_;
      }
      END_LOCK_MUTEX

      return boost::any();
   }

   bool closed() const
   {
      return closed_;
   }

   bool requestParsed() const
   {
      return requestParsed_;
   }

   bool sendingResponse() const
   {
      return sendingResponse_;
   }

   boost::posix_time::ptime startTime() const
   {
      return startTime_;
   }

   long requestSequence() const
   {
      return requestSequence_;
   }

   void setUsername(const std::string& username)
   {
      request_.setUsername(username);
   }

   const std::string& username() const
   {
      return request().username();
   }

   void setHandlerPrefix(const std::string& handlerPrefix)
   {
      request_.setHandlerPrefix(handlerPrefix);
   }

   const std::string& handlerPrefix() const
   {
      return request().handlerPrefix();
   }
   
private:

   // Claims the exclusive right to write this connection's one response, and
   // reports whether the caller won it. Losers are e.g. an upstream error
   // handler calling writeResponse()/writeError() after a streaming proxy
   // (FixedBufferProxy) has already flushed headers/body directly via
   // writeResponseHeaders()/asyncWrite().
   //
   // A single atomic exchange, not a read-then-set of a plain bool, because
   // the parties that race for it are not all on the same execution context:
   // a streaming proxy writes from the *upstream* AsyncClient's completion
   // handlers, which share this connection's strand only if the caller asked
   // them to (proxyLocalhostRequest does; proxyRequest does not), while this
   // connection's own read path can decide to write a response of its own at
   // the same moment -- a request-parse error turning into
   // writeResponse(BadRequest), for instance.
   //
   // What the claim cannot arbitrate is a caller that mutates response()
   // directly through the accessor and only then asks us to write it -- the
   // claim only sees the call, which comes after the mutation. That is why
   // response()'s declaration in AsyncConnection requires such callers to keep
   // the populate-then-write pair on strand_ as one unit, the way
   // FixedBufferProxy::writeHeaders() does.
   bool claimResponse(const Socket::Handler& handler)
   {
      if (!sendingResponse_.exchange(true))
         return true;

      LOG_ERROR_MESSAGE("Attempt to write a response after one was already started; ignoring");

      // Settle the loser's handler rather than dropping it. Returning silently
      // strands whatever the caller meant to do in that completion: for
      // FixedBufferProxy's header write it leaves clientWriteInProgress_
      // latched true forever, so queued chunks never flush, backpressure never
      // resumes, and both sockets leak with no timeout to reap them. An error
      // lets the caller tear itself down instead -- specifically
      // already_started, which FixedBufferProxy::handleError() distinguishes
      // from a transport failure so that it detaches from its upstream without
      // closing this connection. We don't close() it here either: the winner
      // owns it and its write may still be in flight, so closing would
      // truncate or reset the response the claim exists to protect.
      //
      // Posted rather than invoked inline, both because asio's contract is that
      // a completion handler never runs inside the initiating call and because
      // callers here hold locks across it -- FixedBufferProxy::queueChunk()
      // calls us with its mutex_ held, and the handler's error path would
      // re-enter that mutex.
      if (handler)
      {
         boost::asio::post(
                  strand_,
                  boost::bind(handler,
                              boost::system::error_code(boost::asio::error::already_started),
                              std::size_t(0)));
      }

      return false;
   }

   // The body of writeResponseHeaders(), minus the claim -- shared by both of
   // its overloads, for the same reason writeResponseImpl() below is.
   void writeResponseHeadersImpl(Socket::Handler handler)
   {
      // Send our own HTTP-version, not a proxied upstream's -- same RFC 7230 2.6
      // rationale as writeResponse() above, and it matters more here: this is
      // the path FixedBufferProxy streams through, where the framing headers
      // that follow are chosen for an HTTP/1.1 client.
      response_.setHttpVersion(1, 1);

      if (!response_.containsHeader("Date"))
         response_.setHeader("Date", util::httpDate());

      // kept for parity with the older writeResponse() above, which has set
      // this unconditionally on every response for a long time. Remove any
      // upstream-supplied instance(s) first -- setHeader() only replaces the
      // first match, so a duplicated (or differently-cased) upstream header
      // would otherwise still reach the client alongside ours.
      response_.removeHeader("X-Content-Type-Options");
      response_.setHeader("X-Content-Type-Options", "nosniff");

      // call the response filter if we have one -- in the same position (the
      // last mutation before the write) as writeResponse() above, so a filter
      // that deliberately overrides Date or nosniff wins identically on both
      // paths.
      //
      // This is not an optional hook that only some deployments install:
      // AsyncServerImpl::acceptNextConnection() hands every connection its
      // connectionResponseFilter, which stamps "Server" and every
      // server-add-header header -- documented in server-options.json as
      // applying to *all* responses from RStudio Server, and in practice
      // carrying HSTS / X-Frame-Options. Skipping it here dropped those from
      // every response streamed through this path (in pro, also multi-session
      // Location/Refresh rewriting), which is exactly the arbitrary-user-app
      // content served over /p/.
      //
      // Note the caller has already chosen this response's framing headers by
      // the time we get here (see FixedBufferProxy::queueChunk), so a filter
      // that set Content-Length/Transfer-Encoding/Connection would override
      // them. That is deliberately not defended against, for two reasons.
      // First, writeResponse() allows exactly the same overrides (the filter
      // runs there before the response is serialized, and its Content-Length
      // default only applies when none is present), so restoring framing here
      // would make the two paths disagree -- which is the class of bug this
      // call is fixing. Second, the damage is bounded to a single malformed
      // response rather than cross-request desync: every terminal path in
      // FixedBufferProxy closes the client connection, as does
      // writeResponse()'s default close=true, so a client misled by a bogus
      // length hits EOF instead of parsing a following response out of
      // leftover bytes. Both halves of that parity are pinned by
      // AsyncConnectionImplTests' *LetsTheFilterOverrideFramingHeaders* pair.
      if (responseFilter_)
         responseFilter_(originalRequest_, &response_);

      // write only the header buffers
      socketOperations_->asyncWrite(response_.headerBuffers(), handler);
   }

   // The body of writeResponse(), minus the claim -- so the entry points that
   // must claim *before* mutating response_ (assign(), setError()) can reach
   // it without claiming a second time and failing against themselves.
   void writeResponseImpl(bool close, Socket::Handler handler)
   {
      // RFC 7230 2.6: "Intermediaries that process HTTP messages (i.e., all
      // intermediaries other than those acting as tunnels) MUST send their own
      // HTTP-version in forwarded messages." Responses this server generates
      // itself already carry 1.1 (http::Message's constructor), but a proxied
      // one does not: Response::assign() copies httpVersionMajor_/Minor_ off
      // the upstream response, so an HTTP/1.0 backend's version would otherwise
      // reach the client on our HTTP/1.1 connection. That is not cosmetic once
      // this hop makes its own framing decisions -- a status line reading
      // HTTP/1.0 tells the client not to expect chunked framing or a persistent
      // connection, whatever headers follow it.
      response_.setHttpVersion(1, 1);

      // add extra response headers
      if (!response_.containsHeader("Date"))
         response_.setHeader("Date", util::httpDate());
      if (close)
         response_.setHeader("Connection", "close");
      response_.setHeader("X-Content-Type-Options", "nosniff");

      // call the response filter if we have one
      if (responseFilter_)
         responseFilter_(originalRequest_, &response_);

      if (response_.isStreamResponse())
      {
         boost::shared_ptr<core::http::StreamWriter<SocketType> > pWriter(
                  new core::http::StreamWriter<SocketType>(
                     socket(), // using socket(), not *socket in case of SSL connection
                     response_,
                     boost::bind(&AsyncConnectionImpl<SocketType>::onStreamComplete,
                                 AsyncConnectionImpl<SocketType>::shared_from_this(),
                                 close,
                                 handler),
                     boost::bind(&AsyncConnectionImpl<SocketType>::handleStreamError,
                                 AsyncConnectionImpl<SocketType>::shared_from_this(),
                                 close,
                                 handler,
                                 _1)));

         pWriter->write();
         return;
      }
      else
      {
         // make sure that if no body and content-length were specified,
         // and the status code is not 1xx or 204,
         // we send 0 for Content-Length
         // otherwise, this response will be invalid
         if ((response_.body().empty() && response_.headerValue("Content-Length").empty()) &&
             (response_.statusCode() < 100 || response_.statusCode() > 199) &&
             response_.statusCode() != 204)
         {
             response_.setContentLength(0);
         }

         // After asyncWrite completes, we want to first do our cleanup
         // (this->handleWrite()) and then call the caller's handler
         Socket::Handler handlers = boost::bind(
            &Socket::joinHandlers,
            static_cast<Socket::Handler>(boost::bind(
               &AsyncConnectionImpl<SocketType>::handleWrite,
               AsyncConnectionImpl<SocketType>::shared_from_this(),
               boost::asio::placeholders::error,
               close)),
            handler,
            _1,
            _2
         );

         // write
         socketOperations_->asyncWrite(response_.toBuffers(), handlers);
      }
   }

   void logConnectionError(Error error)
   {
      error.addProperty("request", request().debugInfo());
      LOG_ERROR(error);
   }

   void handleRead(const boost::system::error_code& e,
                   std::size_t bytesTransferred)
   {
      try
      {
         if (!e)
         {
            bytesTransferred_ = bytesTransferred;

            // we must synchronize access to the RequestParser, because while returning
            // from a suspending form handler, we could be told to resume processing
            // before the request parser properly saves its temporary state, causing all sorts of havoc
            RequestParser::status status = RequestParser::error;
            RECURSIVE_LOCK_MUTEX(mutex_)
            {
               // parse next chunk
               status = requestParser_.parse(request_,
                                             buffer_.data(),
                                             buffer_.data() + bytesTransferred);
            }
            END_LOCK_MUTEX

            // error - return bad request
            if (status == RequestParser::error)
            {
               response_.setStatusCode(http::status::BadRequest);
               writeResponse();
            }
            
            // incomplete -- keep reading
            else if (status == RequestParser::incomplete)
            {
               readSome();
            }

            // headers parsed - body parsing has not yet begun
            else if (status == RequestParser::headers_parsed)
            {
               // record the original request
               originalRequest_.assign(request_);

               // call the request filter if we have one
               if (requestFilter_)
               {
                  // call the filter (passing a continuation to be invoked
                  // once the filter is completed)
                  requestFilter_(
                     ioContext(),
                     &request_,
                     boost::bind(
                        &AsyncConnectionImpl<SocketType>::requestFilterContinuation,
                        AsyncConnectionImpl<SocketType>::shared_from_this(),
                        _1, e, bytesTransferred
                     ));
               }
               else
               {
                  if (!callHeadersParsedHandler())
                  {
                     writeResponse();
                     return;
                  }

                  // we need to resume body parsing by recalling the parse
                  // method and providing the exact same buffer to continue
                  // from where we left off
                  handleRead(e, bytesTransferred);
               }

               return;
            }
            
            // pause - save current state
            else if (status == RequestParser::pause)
            {
               // we will need to reinvoke this method with the same
               // buffer when told to resume, so for now simply return
               // to keep our internal state the same
               return;
            }

            // form complete - do nothing since the form handler
            // has been invoked by the request parser as appropriate
            else if (status == RequestParser::form_complete)
            {
               return;
            }

            // got valid request -- handle it 
            else
            {  
               callHandler();
            }
         }
         else // error reading
         {
            // log the error if it wasn't connection terminated
            Error error(e, ERROR_LOCATION);
            if (!isConnectionTerminatedError(error))
               logConnectionError(error);
            
            // close the socket
            close();
            
            //
            // no more async operations are initiated here so the shared_ptr to 
            // this connection no more references and is automatically destroyed
            //
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   
   void requestFilterContinuation(boost::shared_ptr<http::Response> response,
                                  const boost::system::error_code& e,
                                  std::size_t bytesTransferred)
   {
      if (response)
      {
         response_.assign(*response);
         writeResponse();
      }
      else
      {
         if (!callHeadersParsedHandler())
         {
            writeResponse();
            return;
         }

         // we need to resume body parsing by recalling the parse
         // method and providing the exact same buffer to continue
         // from where we left off
         handleRead(e, bytesTransferred);
      }
   }

   bool callHeadersParsedHandler()
   {
      return onHeadersParsed_(AsyncConnectionImpl<SocketType>::shared_from_this(),
                              &request_);
   }

   void callHandler()
   {
      requestParsed_ = true;
      onRequestParsed_(AsyncConnectionImpl<SocketType>::shared_from_this(),
                       &request_);
   }

   void handleWrite(const boost::system::error_code& e, bool closeSocket)
   {
      try
      {
         if (e)
         {
            // log the error if it wasn't connection terminated
            Error error(e, ERROR_LOCATION);
            if (!http::isConnectionTerminatedError(error) &&
                !http::isWrongProtocolTypeError((error)))
            {
               logConnectionError(error);
            }
         }
         
         // close the socket, if requested or if error
         if (e || closeSocket)
         {
            close();
         }

         //
         // no more async operations are initiated here so the shared_ptr to 
         // this connection no more references and is automatically destroyed
         //
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   
   void readSome()
   {
      socketOperations_->asyncReadSome(boost::asio::buffer(buffer_),
                                       boost::bind(&AsyncConnectionImpl<SocketType>::handleRead,
                                                   AsyncConnectionImpl<SocketType>::shared_from_this(),
                                                   boost::asio::placeholders::error,
                                                   boost::asio::placeholders::bytes_transferred));
   }

   void handleHandshake(const boost::system::error_code& ec)
   {
      if (ec)
      {
         Error error(ec, ERROR_LOCATION);
         if (!core::http::isConnectionTerminatedError(error))
            logConnectionError(error);

         return;
      }

      // ssl stream established - start reading
      readSome();
   }

   void onStreamComplete(bool close, Socket::Handler handler)
   {
      if (close)
         this->close();
      // -1 isn't the correct number of bytes written, but we don't have access
      // to that information at this point.
      handler(boost::system::error_code(), -1);
   }

   void handleStreamError(bool close, Socket::Handler handler, const Error& error)
   {
      if (!core::http::isConnectionTerminatedError(error))
         logConnectionError(error);
      if (close)
         this->close();
      // jcheng: boost::system::generic_category() isn't correct, but I don't
      // know how to get an error_code category out of an Error object.
      // -1 isn't the correct number of bytes written, but we don't have access
      // to that information at this point.
      handler(boost::system::error_code(error.getCode(), boost::system::generic_category()), -1);
   }

private:
   boost::asio::io_context& ioContext_;

   // optional ssl stream
   // not used if the connection is not ssl enabled
   boost::shared_ptr<boost::asio::ssl::stream<SocketType> > sslStream_;

   // underlying socket
   boost::shared_ptr<SocketType> socket_;

   // socket wrapper to forward calls to an SSL stream or a raw socket
   // depending on whether or not SSL is enabled
   boost::shared_ptr<ISocketOperations> socketOperations_;

   HeadersParsedHandler onHeadersParsed_;
   Handler onRequestParsed_;
   ClosedHandler onClosed_;
   FormHandler formHandler_;
   RequestFilter requestFilter_;
   ResponseFilter responseFilter_;
   boost::array<char, 8192> buffer_;
   RequestParser requestParser_;
   Request originalRequest_;
   http::Request request_;
   http::Response response_;

   boost::recursive_mutex mutex_;
   bool closed_ = false;
   bool requestParsed_ = false;
   // atomic: claimResponse() arbitrates writers that may be on different
   // strands (see there), and sendingResponse() is read from the server's
   // connection-info reporting on yet another thread
   std::atomic<bool> sendingResponse_{false};
   boost::posix_time::ptime startTime_;
   int requestSequence_;

   size_t bytesTransferred_;

   boost::any connectionData_;

protected:
   boost::asio::io_context::strand strand_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP


