/*
 * AsyncClient.hpp
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

#ifndef CORE_HTTP_ASYNC_CLIENT_HPP
#define CORE_HTTP_ASYNC_CLIENT_HPP

#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/asio/write.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/strand.hpp>
#include <boost/asio/bind_executor.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/read.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/system_timer.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/Thread.hpp>

#include <core/http/ChunkParser.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/ResponseParser.hpp>
#include <core/http/Socket.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/ConnectionRetryProfile.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>

#include <boost/optional.hpp>

// special version of unexpected exception handler which makes
// sure to call the user's ErrorHandler
#define CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION \
   catch(const std::exception& e) \
   { \
      handleUnexpectedError(std::string("Unexpected exception: ") + \
                            e.what(), ERROR_LOCATION);  \
   } \
   catch(...) \
   { \
      handleUnexpectedError("Unknown exception", ERROR_LOCATION); \
   }

namespace rstudio {
namespace core {
namespace http {

// handler for delivering response body pieces (bounded to at most 1MB each)
// as they become available, for both chunked-encoding and non-chunked bodies
typedef boost::function<bool(const http::Response&, const std::string&)> FixedBufferHandler;

typedef boost::function<void(const http::Response&)> ResponseHandler;
typedef boost::function<void(const core::Error&)> ErrorHandler;
typedef boost::function<void(void)> ConnectHandler;

class IAsyncClient : public Socket
{
public:
   virtual http::Request& request() = 0;
   virtual void setConnectionRetryProfile(
         const http::ConnectionRetryProfile& connectionRetryProfile) = 0;
   virtual void setRequestTimeout(
         const boost::posix_time::time_duration& requestTimeout) = 0;
   virtual void execute(const ResponseHandler& responseHandler,
                        const ErrorHandler& errorHandler,
                        const FixedBufferHandler& fixedBufferHandler = FixedBufferHandler()) = 0;
   virtual void setFixedBufferHandler(const FixedBufferHandler& fixedBufferHandler) = 0;

   // Opt in to streaming non-chunked (Content-Length/EOF-delimited) response
   // bodies piece-wise to the FixedBufferHandler instead of accumulating them in
   // memory and delivering via the ResponseHandler. Default is false, so a
   // wiring site that only calls setFixedBufferHandler() (as /s/ and the launcher do)
   // keeps its legacy behavior: non-chunked responses buffer and run the
   // ResponseHandler; chunked responses still stream. Only wiring sites that can
   // tolerate their non-chunked ResponseHandler side effects being skipped
   // (currently just the /p/ localhost path) should opt in. The client-facing
   // framing (Content-Length vs chunked) is chosen downstream by FixedBufferProxy from
   // the upstream response's Content-Length; AsyncClient only relays bytes.
   virtual void setStreamNonChunkedResponses(bool stream) = 0;

   // Optional predicate, evaluated once response headers are parsed, that forces
   // the full response body to be buffered in memory and delivered via the
   // ResponseHandler (legacy behavior) instead of streamed. Used for handlers
   // that must examine/rewrite the whole body (e.g. the SparkUI root-path link
   // fixup). Applies to both chunked and non-chunked responses (a Jetty/SparkUI
   // page can be chunked). When unset, streaming is governed solely by the
   // setStreamNonChunkedResponses flag and the chunked-encoding detection.
   virtual void setBufferPredicate(
      const boost::function<bool(const http::Response&)>& predicate) = 0;

   virtual void setConnectHandler(const ConnectHandler& connectHandler) = 0;
   virtual void resumeChunkProcessing() = 0;
   virtual void disableHandlers() = 0;
   virtual void close() = 0;
   virtual void setStrand(boost::asio::io_context::strand* pStrand) = 0;

   // Opt in to treating a `false` return from the FixedBufferHandler's completion
   // call (the empty chunk sent by closeAndRespond()) as "temporary
   // backpressure, resume me later" rather than discarding it. Only
   // FixedBufferProxy's contract works this way -- it declines under backpressure
   // while leaving the connection open, and calls resumeChunkProcessing()
   // once its outbound buffer drains. Other FixedBufferHandler consumers (e.g.
   // LauncherClient, sendMethodToSession's onChunk wrapper) return false on
   // the empty chunk to mean "stream is done" and close the connection
   // themselves, synchronously, within that same call -- they never call
   // resumeChunkProcessing(), so treating their "done" as a pause would leave
   // completion permanently unretried and disableHandlers() permanently
   // skipped. Default is false (discard the return value, matching every
   // consumer's existing behavior); FixedBufferProxy sets this to true when it
   // wires setFixedBufferHandler().
   virtual void setFixedBufferHandlerSupportsPause(bool supportsPause) = 0;
};

template <typename SocketService>
class AsyncClient :
   public boost::enable_shared_from_this<AsyncClient<SocketService> >,
   public IAsyncClient,
   boost::noncopyable
{
public:
   AsyncClient(boost::asio::io_context& ioContext,
               bool logToStderr = false)
      : chunkedEncoding_(false),
        ioContext_(ioContext),
        connectionRetryContext_(ioContext),
        logToStderr_(logToStderr),
        closed_(false),
        requestWritten_(false),
        defaultStrand_(ioContext)
   {
      // Make sure we read at least 8192 bytes from the socket at a time. The default ends up as 512.
      responseBuffer_.prepare(8192);
   }

   virtual ~AsyncClient()
   {
   }

   // populate the request before calling execute
   virtual http::Request& request() { return request_; }

   // set (optional) connection retry profile. must do this prior
   // to calling execute
   virtual void setConnectionRetryProfile(
         const http::ConnectionRetryProfile& connectionRetryProfile)
   {
      connectionRetryContext_.profile = connectionRetryProfile;
   }

   // Set an optional overall request deadline covering connect, handshake, and
   // response read. Must be set prior to calling execute. When left unset the
   // request has no deadline of its own -- the connect phase is still bounded
   // by the subclass's connection timeout, but a peer that completes the
   // handshake and then stalls would otherwise keep the request in flight
   // indefinitely (see rstudio#17807). A special or non-positive duration
   // disables the deadline.
   virtual void setRequestTimeout(
         const boost::posix_time::time_duration& requestTimeout)
   {
      requestTimeout_ = requestTimeout;
   }

   // Execute the async client
   // The responseHandler will be expected to handle any status that indicates http error, such as 400 or 500 series of codes
   // The errorHandler is called on a low level level error like failure to read or write
   virtual void execute(const ResponseHandler& responseHandler,
                        const ErrorHandler& errorHandler,
                        const FixedBufferHandler& fixedBufferHandler = FixedBufferHandler())
   {
      // set handlers
      responseHandler_ = responseHandler;
      errorHandler_ = errorHandler;
      if (fixedBufferHandler)
         fixedBufferHandler_ = fixedBufferHandler;

      // if the host header is not already set, make sure we stamp a default one
      // this is required by the http standard
      if (request_.host().empty())
         request_.setHost(getDefaultHostHeader());

      // arm the overall request deadline (no-op unless one was configured)
      armRequestDeadline();

      // connect and write request (implemented in a protocol
      // specific manner by subclassees)
      connectAndWriteRequest();
   }

   // if an embedder of this class calls close() on AsyncClient in it's
   // destructor (for more rigorous cleanup) then it's possible that the
   // onError handler will still be called as a result of the socket close.
   // the callback might then be interacting with a C++ object that has
   // already been deleted. for this case (which does occur in the
   // desktop::NetworkReply class) we provide a method that disables
   // any pending handlers
   virtual void disableHandlers()
   {
      // the request has settled (or is being torn down by an embedder); stop
      // the overall deadline so it can't fire a spurious timeout afterwards
      cancelRequestDeadline();

      responseHandler_ = ResponseHandler();
      errorHandler_ = ErrorHandler();
      fixedBufferHandler_ = FixedBufferHandler();
      connectHandler_ = ConnectHandler();
   }

   // satisfy lower-level http::Socket interface (used when the client
   // is upgraded to a websocket connection and no longer conforms to
   // the request/response protocol used by the class in the ordinary
   // course of business)

   virtual void asyncReadSome(boost::asio::mutable_buffer buffer,
                              Handler handler)
   {
      socket().async_read_some(buffer, boost::asio::bind_executor(*pStrand_, handler));
   }

   virtual void asyncWrite(
                     const boost::asio::const_buffer& buffer,
                     Handler handler)
   {
      boost::asio::async_write(socket(), buffer, boost::asio::bind_executor(*pStrand_, handler));
   }

   virtual void asyncWrite(
                     const std::vector<boost::asio::const_buffer>& buffers,
                     Handler handler)
   {
      boost::asio::async_write(socket(), buffers, boost::asio::bind_executor(*pStrand_, handler));
   }

   virtual void close()
   {
      // ensure the socket is only closed once - boost considers
      // multiple closes an error, and this can lead to a segfault
      LOCK_MUTEX(socketMutex_)
      {
         if (!closed_)
         {
            Error error = closeSocket(socket().lowest_layer());
            if (error && !core::http::isConnectionTerminatedError(error))
               logError(error);

            closed_ = true;
         }
      }
      END_LOCK_MUTEX
   }

   virtual void setFixedBufferHandler(const FixedBufferHandler& fixedBufferHandler)
   {
      fixedBufferHandler_ = fixedBufferHandler;
   }

   virtual void setStreamNonChunkedResponses(bool stream)
   {
      streamNonChunkedResponses_ = stream;
   }

   virtual void setBufferPredicate(
      const boost::function<bool(const http::Response&)>& predicate)
   {
      bufferPredicate_ = predicate;
   }

   virtual void setFixedBufferHandlerSupportsPause(bool supportsPause)
   {
      fixedBufferHandlerSupportsPause_ = supportsPause;
   }

   virtual void resumeChunkProcessing()
   {
      // A rejected completion signal is tracked separately from chunkState_
      // (see completionPending_'s declaration) and checked first: retrying it
      // re-enters closeAndRespond() itself rather than deliverChunks(), so
      // completion is never sent twice regardless of what chunkState_ holds.
      if (completionPending_)
      {
         auto self = AsyncClient<SocketService>::shared_from_this();
         boost::asio::post(ioContext_, [this, self]()
         {
            completionPending_ = false;
            closeAndRespond(); // may set completionPending_ again if still full
         });
         return;
      }

      if (!chunkState_)
      {
         // no saved chunk state so this was an errant call and we should not do anything
         return;
      }

      // deliver the chunks on the thread pool instead of directly from this method
      // so that it is not a re-entrant method (beneficial for clients if they are holding locks, etc)
      auto self = AsyncClient<SocketService>::shared_from_this();
      boost::asio::post(ioContext_, [this, self]()
      {
         // capture shared_ptr of this to keep instance alive while posting callback
         // to io service
         bool complete = chunkState_->complete;
         bool handled = deliverChunks(chunkState_->chunks, complete);

         if (handled)
         {
            if (!complete)
               readSomeContent();
            else
               closeAndRespond();
         }
      });
   }

   virtual void setConnectHandler(const ConnectHandler& connectHandler)
   {
      // if we are already connected, don't bother saving the connect handler
      // and just invoke it directly
      bool invokeConnectHandler = false;
      LOCK_MUTEX(socketMutex_)
      {
         if (!requestWritten_)
            connectHandler_ = connectHandler;
         else
            invokeConnectHandler = true;
      }
      END_LOCK_MUTEX

      if (invokeConnectHandler)
         connectHandler();
   }

   virtual void setStrand(boost::asio::io_context::strand* pStrand)
   {
      pStrand_ = pStrand;
   }

protected:

   boost::asio::io_context& ioContext() { return ioContext_; }

   virtual SocketService& socket() = 0;

   // Verification hook invoked by connecting subclasses after the connection
   // (and any TLS handshake) succeeds but BEFORE any request bytes are written.
   // Default: no verification. Overridden by LocalhostAsyncClient[Ssl] to enforce
   // destination-port ownership (rstudio-pro#11470). Return true to proceed with
   // the write; set *pError and return false to reject (fail closed).
   virtual bool verifyConnectedPeer(Error* /*pError*/) { return true; }

   void handleConnectionError(const Error& connectionError)
   {
      // retry if necessary, otherwise just forward the error to
      // customary error handling scheme

      Error otherError;
      if (!retryConnectionIfRequired(connectionError, &otherError))
      {
         if (otherError)
            handleError(otherError);
         else
            handleError(connectionError);
      }
   }

   // asynchronously write the request (called by subclasses after
   // they finish connecting)
   void writeRequest()
   {
      // specify closing of the connection after the request unless this is
      // an attempt to upgrade to websockets
      Header overrideHeader;
      if (!util::isWSUpgradeRequest(request_))
      {
         overrideHeader = Header::connectionClose();
      }

      // write
      boost::asio::async_write(
          socket(),
          request_.toBuffers(overrideHeader),
          boost::asio::bind_executor(*pStrand_, boost::bind(
               &AsyncClient<SocketService>::handleWrite,
               AsyncClient<SocketService>::shared_from_this(),
               boost::asio::placeholders::error))
      );
   }

   void handleError(const Error& error)
   {
      Error httpError = error;
      addErrorProperties(httpError);

      // check to see if the socket was closed purposefully
      // if so, we will ignore the error
      LOCK_MUTEX(socketMutex_)
      {
         if (closed_)
            return;
      }
      END_LOCK_MUTEX

      // close the socket
      close();

      // invoke error handler
      if (errorHandler_)
         errorHandler_(httpError);

      // free handlers to ensure they do not keep a strong reference to us
      // this will allow us to properly clean up in that case
      disableHandlers();
   }

   void handleErrorCode(const boost::system::error_code& ec,
                        const ErrorLocation& location)
   {
      handleError(Error(ec, location));
   }

   void handleUnexpectedError(const std::string& description,
                              const ErrorLocation& location)
   {
      Error error = systemError(boost::system::errc::state_not_recoverable,
                                description,
                                location);
      handleError(error);
   }

   virtual void addErrorProperties(Error& error)
   {
      std::string host = request_.host();
      if (!host.empty())
         error.addProperty("host", host);
      std::string uri = request_.uri();
      if (!uri.empty())
         error.addProperty("uri", uri);
   }
   
private:

   virtual void connectAndWriteRequest() = 0;
   virtual std::string getDefaultHostHeader() = 0;

   // A hook for LocalStreamAsyncClient connections to retry on permission denied errors that show up intermittently
   virtual bool recentConnectionError(const Error& connectionError)
   {
      return false;
   }

   bool retryConnectionIfRequired(const Error& connectionError,
                                  Error* pOtherError)
   {
      // retry if this is a connection unavailable error and the
      // caller has provided a connection retry profile
      if ((http::isConnectionUnavailableError(connectionError) || recentConnectionError(connectionError)) &&
          !connectionRetryContext_.profile.empty())
      {
         // if this is our first retry then set our stop trying time
         bool firstAttempt = false;
         if (connectionRetryContext_.stopTryingTime.is_not_a_date_time())
         {
            connectionRetryContext_.stopTryingTime =
                  boost::posix_time::microsec_clock::universal_time() +
                  connectionRetryContext_.profile.maxWait;

            firstAttempt = true;
         }

         // call recovery function if we have it
         if (connectionRetryContext_.profile.recoveryFunction)
         {
            Error error = connectionRetryContext_.profile
                                   .recoveryFunction(request_, firstAttempt);
            if (error)
            {
               *pOtherError = error;
               return false;
            }
         }

         // if we aren't already past the maximum wait time then
         // wait the appropriate interval and attempt connection again
         if (boost::posix_time::microsec_clock::universal_time() <
             connectionRetryContext_.stopTryingTime)
         {
            return scheduleRetry(); // continuation
         }
         else // otherwise we've waited long enough, bail and
              // perform normal error handling
         {
            return false;
         }
      }
      else // not an error subject to retrying or no retry profile provided
      {
         return false;
      }
   }


   bool scheduleRetry()
   {
      // set expiration
      try
      {
         auto interval = std::chrono::milliseconds(
            connectionRetryContext_.profile.retryInterval.total_milliseconds());

         connectionRetryContext_.retryTimer.expires_after(interval);
         connectionRetryContext_.retryTimer.async_wait(
            boost::asio::bind_executor(
               *pStrand_,
               boost::bind(
                  &AsyncClient<SocketService>::handleConnectionRetryTimer,
                  AsyncClient<SocketService>::shared_from_this(),
                  boost::asio::placeholders::error)));
         return true;
      }
      catch (boost::system::system_error& e)
      {
         log::logError(Error(e.code(), ERROR_LOCATION));
         return false;
      }
      catch (std::exception& e)
      {
         log::logErrorMessage(
            fmt::format("Unexpected exception: {}", e.what()),
            ERROR_LOCATION);
         return false;
      }
      catch (...)
      {
         log::logErrorMessage(
            "Unexpected exception: <no information available>",
            ERROR_LOCATION);
         return false;
      }
   }

   void handleConnectionRetryTimer(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            connectAndWriteRequest();
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void handleWrite(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // invoke connect handler if we have one
            ConnectHandler handler;
            LOCK_MUTEX(socketMutex_)
            {
               requestWritten_ = true;
               if (connectHandler_)
                  handler = connectHandler_;
            }
            END_LOCK_MUTEX

            // actual invocation should be outside of lock to prevent recursive lock acquisitions
            if (handler)
               handler();

            // initiate async read of the first line of the response
            boost::asio::async_read_until(
              socket(),
              responseBuffer_,
              "\r\n",
              boost::asio::bind_executor(*pStrand_,
                                         boost::bind(&AsyncClient<SocketService>::handleReadStatusLine,
                                                     AsyncClient<SocketService>::shared_from_this(),
                                                     boost::asio::placeholders::error)));
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void handleReadStatusLine(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // parase status line
            Error error = ResponseParser::parseStatusLine(&responseBuffer_,
                                                          &response_);
            if (error)
            {
               handleError(error);
            }
            else
            {
               // initiate async read of the headers
               boost::asio::async_read_until(
                 socket(),
                 responseBuffer_,
                 "\r\n\r\n",
                 boost::asio::bind_executor(*pStrand_,
                        boost::bind(&AsyncClient<SocketService>::handleReadHeaders,
                                    AsyncClient<SocketService>::shared_from_this(),
                                    boost::asio::placeholders::error)));
            }
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void readSomeContent()
   {
      // provide a hook for subclasses to force termination of
      // content reads (this is needed for named pipes on windows,
      // where the client disconnecting from the server is part
      // of the normal pipe shutdown sequence). without this
      // the subsequent call to handleReadContent will perform
      // the close and respond when it gets a shutdown error (as
      // a result of the server shutting down)
      if (stopReadingAndRespond())
      {
         closeAndRespond();
         return;
      }

      boost::asio::async_read(
         socket(),
         responseBuffer_,
         boost::asio::transfer_at_least(1),
         boost::asio::bind_executor(*pStrand_,
              boost::bind(&AsyncClient<SocketService>::handleReadContent,
                          AsyncClient<SocketService>::shared_from_this(),
                          boost::asio::placeholders::error)));
   }

   virtual bool stopReadingAndRespond()
   {
      return false;
   }

   virtual bool keepConnectionAlive()
   {
      return false;
   }

   // A non-chunked response that carries a Content-Length is fully received
   // once the accumulated body reaches that length. Detecting this lets us
   // stop reading immediately instead of depending on the server to close the
   // connection (EOF) to signal the end of the body. Some servers and proxies
   // keep the socket open even when the client asked for "Connection: close",
   // which would otherwise stall the read until the caller's timeout fires and
   // the fully-received body gets discarded (see rstudio#17807). When the
   // response is chunked, or declares no Content-Length at all, we must still
   // read until EOF.
   //
   // Note: a malformed response carrying both Transfer-Encoding: chunked and a
   // non-zero Content-Length is not treated as chunked here (chunkedEncoding_ is
   // only set when Content-Length is absent or zero, see handleReadHeaders), so
   // we will honor its Content-Length. RFC 7230 says Transfer-Encoding wins and
   // must be ignored in that case; we don't, but this only affects a malformed
   // peer and matches the pre-existing chunked-detection behavior.
   bool responseBodyComplete() const
   {
      if (chunkedEncoding_)
         return false;

      // only treat the body as Content-Length-delimited when the header is
      // present and parses to a valid non-negative integer. An absent header
      // means the body is delimited by connection close, and a malformed value
      // should likewise fall back to reading until EOF rather than
      // short-circuiting on a possibly-incomplete body. (contentLength()
      // returns 0 for both an absent and an unparseable header, so we parse it
      // explicitly here.)
      boost::optional<uintmax_t> contentLength =
         safe_convert::stringTo<uintmax_t>(response_.headerValue("Content-Length"));
      if (!contentLength)
         return false;

      return response_.body().size() >= *contentLength;
   }

   // True when body data should be handed to the FixedBufferHandler (streamed to the
   // browser) rather than accumulated into response_ for the ResponseHandler.
   bool useFixedBufferHandler() const
   {
      return fixedBufferHandler_ && !bufferFullResponse_;
   }

   // Streaming analog of responseBodyComplete(): in streaming mode response_.body()
   // is never populated, so we compare the count of bytes handed to the fixed
   // buffer handler against the declared Content-Length instead. Returns false when there
   // is no valid Content-Length header (EOF-delimited body): completion is then
   // signalled by the upstream connection close in handleReadContent's EOF branch.
   // Mirrors responseBodyComplete()'s explicit parse so an absent/malformed value
   // falls back to read-until-EOF rather than short-circuiting.
   bool streamedBodyComplete() const
   {
      boost::optional<uintmax_t> contentLength =
         safe_convert::stringTo<uintmax_t>(response_.headerValue("Content-Length"));
      if (!contentLength)
         return false;
      return contentLengthStreamed_ >= *contentLength;
   }

   // Deliver the raw bytes currently in responseBuffer_ to the fixed buffer handler as
   // one or more <=1MB pieces, reusing the chunked-path delivery/backpressure.
   // The non-chunked analog of processChunks(): the bytes are the decoded body
   // only -- FixedBufferProxy decides the client-facing framing. Detects completion via
   // the byte counter (not response_.body(), which streaming never fills), passes
   // the computed `complete` through deliverChunks() into chunkState_, and drives
   // read-more / close-and-respond itself.
   void deliverContentAsChunk()
   {
      auto buffer = responseBuffer_.data();
      std::size_t n = buffer.size();
      auto piece = boost::make_shared<std::string>(
         static_cast<const char*>(buffer.data()), n);
      responseBuffer_.consume(responseBuffer_.size());

      // account the bytes we're relaying so we can detect Content-Length
      // completion without accumulating the body in memory.
      contentLengthStreamed_ += n;
      bool complete = streamedBodyComplete();

      std::deque<boost::shared_ptr<std::string>> chunks;
      if (n > 0)
         chunks.push_back(piece); // never deliver an empty piece mid-stream:
                                  // FixedBufferProxy treats an empty chunk as the final
                                  // completion signal (sent via closeAndRespond).
      breakChunks(chunks);

      // deliverChunks stashes chunkState_ (carrying this same `complete`) and
      // returns false if the consumer paused; resumeChunkProcessing() then routes
      // the resume to closeAndRespond() when complete, or readSomeContent()
      // otherwise -- so a pause on the final piece still completes correctly.
      bool chunksHandled = deliverChunks(chunks, complete);

      if (!chunksHandled)
         return; // paused; resumeChunkProcessing() will continue

      if (complete)
         closeAndRespond();  // sends fixedBufferHandler_(response_, "") completion signal
      else
         readSomeContent();
   }

   void handleReadHeaders(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // parse headers
            ResponseParser::parseHeaders(&responseBuffer_, &response_);

            // decide, from headers alone, whether a downstream handler needs the
            // entire body buffered (legacy path) or whether we can stream it.
            bufferFullResponse_ = bufferPredicate_ && bufferPredicate_(response_);

            // if this is chunked encoding, start processing chunks
            if (response_.headerValue(kTransferEncoding) == kChunkedTransferEncoding &&
                response_.contentLength() == 0)
            {
               chunkedEncoding_ = true;

               // we have some chunk data to process
               if (responseBuffer_.size() > 0)
               {
                  processChunks();
                  return;
               }
               else
               {
                  // no chunk data yet - keep reading
                  readSomeContent();
                  return;
               }
            }

            // a non-chunked body streams piece-wise to the fixed buffer handler only if
            // this wiring site opted in AND no handler needs the whole body
            // buffered. Sites that did not opt in keep buffering non-chunked
            // responses (legacy responseHandler_ path) -- see Step 3 / Background.
            streamResponse_ = streamNonChunkedResponses_ &&
                              useFixedBufferHandler() && !chunkedEncoding_;

            // Streaming path: hand the decoded body to the fixed buffer handler and let
            // deliverContentAsChunk() drive completion/read-more (analog of the
            // chunked branch's processChunks()/readSomeContent() above). Do NOT
            // fall through to responseBodyComplete() below -- it counts
            // response_.body(), which streaming never populates.
            if (streamResponse_)
            {
               deliverContentAsChunk(); // handles empty buffer, completion, backpressure
               return;
            }

            // append any lefover buffer contents to the body
            if (responseBuffer_.size() > 0)
               ResponseParser::appendToBody(&responseBuffer_, &response_);

            // a Content-Length-delimited body may have arrived in full along
            // with the headers; if so respond now rather than reading further
            if (responseBodyComplete())
            {
               closeAndRespond();
               return;
            }

            // start reading content
            readSomeContent();
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void handleReadContent(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // if we are parsing chunked encoding, process this data
            // as chunk data
            if (chunkedEncoding_)
            {
               processChunks();
               return;
            }

            if (streamResponse_)
            {
               // deliver this read piece-wise to the fixed buffer handler.
               // deliverContentAsChunk() tracks bytes-vs-Content-Length for its
               // own completion, closes/responds or reads more as appropriate,
               // and pauses (saving chunkState_) under backpressure -- mirroring
               // the processChunks() delegation used for chunked encoding above.
               deliverContentAsChunk();
               return;
            }

            // copy content
            ResponseParser::appendToBody(&responseBuffer_, &response_);

            // stop once a Content-Length-delimited body is fully received,
            // rather than waiting for the connection to close
            if (responseBodyComplete())
            {
               closeAndRespond();
               return;
            }

            // continue reading content
            readSomeContent();
         }
         else if (ec == boost::asio::error::eof ||
                  isShutdownError(ec))
         {
            closeAndRespond();
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void breakChunks(std::deque<boost::shared_ptr<std::string>>& chunks)
   {
      std::deque<boost::shared_ptr<std::string>> newChunks;

      for (const boost::shared_ptr<std::string>& chunk : chunks)
      {
         if (chunk->size() > maxChunkSize)
         {
            for (std::size_t offset = 0, n = chunk->size();
                 offset < n;
                 offset += maxChunkSize)
            {
               std::string chunkPiece = chunk->substr(offset, maxChunkSize);
               newChunks.push_back(boost::make_shared<std::string>(std::move(chunkPiece)));
            }
         }
         else
         {
            newChunks.push_back(chunk);
         }
      }

      chunks = newChunks;
   }

   void processChunks()
   {
      if (!chunkParser_)
      {
         // lazy init the parser - this is done because the vast majority of responses
         // are NOT chunked encoding
         chunkParser_.reset(new ChunkParser());
      }

      // get the underlying bytes from the response buffer
      auto buffer = responseBuffer_.data();

      // parse the bytes into chunks
      std::deque<boost::shared_ptr<std::string>> chunks;
      bool complete = chunkParser_->parse(static_cast<const char*>(buffer.data()), buffer.size(), &chunks);

      // break up any enormous chunks into more manageable pieces ensure we
      // do not hit any buffering limits preventing us from forwarding the chunk
      breakChunks(chunks);

      bool chunksHandled = deliverChunks(chunks, complete);

      if (!complete)
      {
         // more chunks to come - keep reading

         // we must explicitly consume the underlying buffer to ensure that
         // subsequent reads are read into the beginning of the buffer and not the end
         responseBuffer_.consume(responseBuffer_.size());

         if (chunksHandled)
            readSomeContent();
      }
      else
      {
         // no more chunks
         if (chunksHandled)
            closeAndRespond();
      }
   }

   bool deliverChunks(std::deque<boost::shared_ptr<std::string> >& chunks,
                      bool complete)
   {
      for (auto iter = chunks.begin(); iter != chunks.end();)
      {
         boost::shared_ptr<std::string> chunk = *iter;

         if (useFixedBufferHandler())
         {
            bool keepGoing = fixedBufferHandler_(response_, *chunk);

            if (!keepGoing)
            {
               // callback signaled to us to stop reading data for the moment
               // (perhaps the receiving buffer was full)
               // save this chunk state for later, and stop reading from the connection
               // until we are signaled to do resume
               chunkState_ = boost::make_shared<ChunkState>(chunks, complete);
               return false;
            }
            else
            {
               // consumer successfully processed this chunk, so we can delete it now
               iter = chunks.erase(iter);
            }
         }
         else
         {
            // no fixed buffer handler supplied, so caller expects to receive all chunks
            // in one shot when the request finishes - simply append chunk to final response
            ResponseParser::appendToBody(*chunk, &response_);

            ++iter;
         }
      }

      return true;
   }

   virtual bool isShutdownError(const boost::system::error_code& ec)
   {
      return false;
   }

   void closeAndRespond()
   {
      if (!keepConnectionAlive())
         close();

      if (responseHandler_ && !(useFixedBufferHandler() && (chunkedEncoding_ || streamResponse_)))
      {
         responseHandler_(response_);
      }
      else if (useFixedBufferHandler())
      {
         // The empty chunk is the completion signal for the fixed buffer handler,
         // and -- like any other chunk delivery -- the consumer (FixedBufferProxy)
         // may decline it under backpressure (e.g. its outbound buffer is
         // exactly full at the moment the body finishes). Unlike every other
         // chunk delivery, this call bypasses deliverChunks()/chunkState_, so
         // its return value must be checked explicitly: silently discarding a
         // "please pause" here would leave completion permanently unsent (see
         // completionPending_), since FixedBufferProxy's writeChunk() prioritizes a
         // pending buffer-full condition over its "received final, nothing
         // left to write" close.
         //
         // Only do this when fixedBufferHandlerSupportsPause_ is set (FixedBufferProxy's
         // contract) -- see that flag's declaration for why a `false` return
         // here is overloaded across FixedBufferHandler consumers, and why treating
         // every consumer's `false` as a pause would break the others.
         if (fixedBufferHandlerSupportsPause_)
         {
            if (!fixedBufferHandler_(response_, ""))
            {
               completionPending_ = true;
               return; // resumeChunkProcessing() will retry via closeAndRespond()
            }
         }
         else
         {
            fixedBufferHandler_(response_, "");
         }
      }

      // free handlers in case they keep a strong reference to us
      // this will allow us to properly clean up in that case
      disableHandlers();
   }

   // Start the overall request deadline, if one was configured via
   // setRequestTimeout. The timer holds a shared_ptr to us (via the bound
   // handler) so we stay alive until it fires or its cancellation is delivered.
   void armRequestDeadline()
   {
      if (requestTimeout_.is_special() || requestTimeout_.total_milliseconds() <= 0)
         return;

      pRequestDeadlineTimer_.reset(new boost::asio::system_timer(
         ioContext_,
         std::chrono::milliseconds(requestTimeout_.total_milliseconds())));

      pRequestDeadlineTimer_->async_wait(
         boost::asio::bind_executor(
            *pStrand_,
            boost::bind(&AsyncClient<SocketService>::handleRequestDeadline,
                        AsyncClient<SocketService>::shared_from_this(),
                        boost::asio::placeholders::error)));
   }

   void cancelRequestDeadline()
   {
      if (!pRequestDeadlineTimer_)
         return;

      // The timer is otherwise only touched by its own strand-bound async_wait
      // completion, and a Boost.Asio timer is not safe to access concurrently
      // from another thread. disableHandlers() (our caller) may run off the
      // io_context -- e.g. from an embedder's destructor -- so hop onto the
      // strand to cancel rather than racing the io_context thread. Capture a
      // shared_ptr to keep both this and the timer alive until the cancel runs;
      // if the io_context has already stopped the cancel simply never runs (and
      // neither would the deadline). The try/catch guards against cancel
      // throwing on the strand thread.
      boost::shared_ptr<AsyncClient<SocketService> > self =
         AsyncClient<SocketService>::shared_from_this();
      boost::shared_ptr<boost::asio::system_timer> pTimer = pRequestDeadlineTimer_;

      boost::asio::post(
         ioContext_,
         boost::asio::bind_executor(*pStrand_, [self, pTimer]() {
            try
            {
               pTimer->cancel();
            }
            catch (const boost::system::system_error& e)
            {
               LOG_DEBUG_MESSAGE(std::string("error cancelling AsyncClient request deadline: ") +
                                 e.what());
            }
         }));
   }

   void handleRequestDeadline(const boost::system::error_code& ec)
   {
      // a normal completion cancels the timer, delivering operation_aborted;
      // nothing actually timed out in that case
      if (ec == boost::asio::error::operation_aborted)
         return;

      // close the socket and surface a timeout. if the request already settled
      // (e.g. a late deadline that lost the race with cancellation), handleError()
      // drives the single-settle bookkeeping via the mutex-protected closed_ flag,
      // so this becomes a no-op and the subsequently-aborted read or write
      // completion stays quiet
      handleError(systemError(boost::system::errc::timed_out, ERROR_LOCATION));
   }

   void logError(const Error& error) const
   {
      if (logToStderr_)
      {
         std::cerr << error << std::endl;
      }
      else
      {
         LOG_ERROR(error);
      }
   }

private:
   struct ConnectionRetryContext
   {
      ConnectionRetryContext(boost::asio::io_context& ioContext)
         : stopTryingTime(boost::posix_time::not_a_date_time),
           retryTimer(ioContext)
      {
      }

      http::ConnectionRetryProfile profile;
      boost::posix_time::ptime stopTryingTime;
      boost::asio::system_timer retryTimer;
   };

   struct ChunkState
   {
      ChunkState(const std::deque<boost::shared_ptr<std::string> >& chunks,
                 bool complete) :
         chunks(chunks),
         complete(complete)
      {
      }

      std::deque<boost::shared_ptr<std::string> > chunks;
      bool complete;
   };

protected:
   http::Response response_;
   bool chunkedEncoding_;

private:
   static constexpr std::size_t maxChunkSize = 1'048'576; // 1MB

   boost::asio::io_context& ioContext_;
   ConnectionRetryContext connectionRetryContext_;
   bool logToStderr_;
   ResponseHandler responseHandler_;
   ErrorHandler errorHandler_;
   http::Request request_;
   boost::asio::streambuf responseBuffer_;
   boost::shared_ptr<ChunkParser> chunkParser_;
   FixedBufferHandler fixedBufferHandler_;
   boost::function<bool(const http::Response&)> bufferPredicate_;
   bool streamNonChunkedResponses_ = false; // opt-in, set by wiring site
   bool fixedBufferHandlerSupportsPause_ = false; // opt-in, set by FixedBufferProxy::proxy()
   bool bufferFullResponse_ = false; // decided at header time
   bool streamResponse_ = false;     // the final streaming decision for this
                                     // response, computed once at header time
                                     // from streamNonChunkedResponses_,
                                     // bufferFullResponse_ (the predicate
                                     // result), and whether the body is
                                     // already chunked (always streamed)
   uintmax_t contentLengthStreamed_ = 0; // bytes handed to fixedBufferHandler_ so far
                                         // (response_.body() stays empty when
                                         // streaming, so we count completion here)

   boost::shared_ptr<ChunkState> chunkState_;

   // True when closeAndRespond()'s completion signal (fixedBufferHandler_(response_,
   // "")) was declined by the fixed buffer handler (e.g. FixedBufferProxy is buffer-full at
   // the exact moment the body finishes) and has not yet been redelivered.
   // The completion signal is the one chunk delivery that does not flow
   // through deliverChunks()/chunkState_ (see closeAndRespond()), so without
   // this flag a genuine "please pause" here would be silently dropped:
   // nothing would remember that completion still needs to be sent, and
   // FixedBufferProxy's writeChunk() prioritizes a pending buffer-full condition
   // over its "received final, nothing left to write" close -- so both
   // connections would stay open forever. resumeChunkProcessing() checks this
   // before chunkState_ and retries closeAndRespond() itself, keeping the
   // retry logic separate from real-content redelivery so the completion
   // signal is never sent twice.
   //
   // Only ever set when fixedBufferHandlerSupportsPause_ is true (see its
   // declaration): a `false` return from the completion call is overloaded
   // across FixedBufferHandler consumers, and only FixedBufferProxy's contract treats it
   // as "pause, resume me later." Gating on that flag -- rather than trying
   // to infer intent from connection state -- keeps this correct without
   // affecting any other consumer's existing behavior.
   bool completionPending_ = false;

   // optional overall request deadline (connect + handshake + read); unset by
   // default, configured via setRequestTimeout and armed in execute()
   boost::posix_time::time_duration requestTimeout_ = boost::posix_time::pos_infin;
   boost::shared_ptr<boost::asio::system_timer> pRequestDeadlineTimer_;

   boost::mutex socketMutex_;
   bool closed_;

   bool requestWritten_;
   ConnectHandler connectHandler_;

   boost::asio::io_context::strand defaultStrand_;

protected:
   boost::asio::io_context::strand* pStrand_ = &defaultStrand_;

};
   

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_CLIENT_HPP


