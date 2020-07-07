/*
 * AsyncClient.hpp
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

#ifndef CORE_HTTP_ASYNC_CLIENT_HPP
#define CORE_HTTP_ASYNC_CLIENT_HPP

#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/asio/write.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/read.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/deadline_timer.hpp>

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

// chunked handler for reading chunked encoding chunks
// ONLY used for responses that return chunked encoding
typedef boost::function<bool(const http::Response&, const std::string&)> ChunkHandler;

typedef boost::function<void(const http::Response&)> ResponseHandler;
typedef boost::function<void(const core::Error&)> ErrorHandler;
typedef boost::function<void(void)> ConnectHandler;

class IAsyncClient : public Socket
{
public:
   virtual http::Request& request() = 0;
   virtual void setConnectionRetryProfile(
         const http::ConnectionRetryProfile& connectionRetryProfile) = 0;
   virtual void execute(const ResponseHandler& responseHandler,
                        const ErrorHandler& errorHandler,
                        const ChunkHandler& chunkHandler = ChunkHandler()) = 0;
   virtual void setChunkHandler(const ChunkHandler& chunkHandler) = 0;
   virtual void setConnectHandler(const ConnectHandler& connectHandler) = 0;
   virtual void resumeChunkProcessing() = 0;
   virtual void disableHandlers() = 0;
   virtual void close() = 0;
};

template <typename SocketService>
class AsyncClient :
   public boost::enable_shared_from_this<AsyncClient<SocketService> >,
   public IAsyncClient,
   boost::noncopyable
{
public:
   AsyncClient(boost::asio::io_service& ioService,
               bool logToStderr = false)
      : chunkedEncoding_(false),
        ioService_(ioService),
        connectionRetryContext_(ioService),
        logToStderr_(logToStderr),
        closed_(false),
        requestWritten_(false)
   {
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

   // execute the async client
   virtual void execute(const ResponseHandler& responseHandler,
                        const ErrorHandler& errorHandler,
                        const ChunkHandler& chunkHandler = ChunkHandler())
   {
      // set handlers
      responseHandler_ = responseHandler;
      errorHandler_ = errorHandler;
      if (chunkHandler)
         chunkHandler_ = chunkHandler;

      // if the host header is not already set, make sure we stamp a default one
      // this is required by the http standard
      if (request_.host().empty())
         request_.setHost(getDefaultHostHeader());

      // connect and write request (implmented in a protocol
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
      responseHandler_ = ResponseHandler();
      errorHandler_ = ErrorHandler();
      chunkHandler_ = ChunkHandler();
      connectHandler_ = ConnectHandler();
   }

   // satisfy lower-level http::Socket interface (used when the client
   // is upgraded to a websocket connection and no longer conforms to
   // the request/response protocol used by the class in the ordinary
   // course of business)

   virtual void asyncReadSome(boost::asio::mutable_buffers_1 buffer,
                              Handler handler)
   {
      socket().async_read_some(buffer, handler);
   }

   virtual void asyncWrite(
                     const boost::asio::const_buffers_1& buffer,
                     Handler handler)
   {
      boost::asio::async_write(socket(), buffer, handler);
   }

   virtual void asyncWrite(
                     const std::vector<boost::asio::const_buffer>& buffers,
                     Handler handler)
   {
      boost::asio::async_write(socket(), buffers, handler);
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

   virtual void setChunkHandler(const ChunkHandler& chunkHandler)
   {
      chunkHandler_ = chunkHandler;
   }

   virtual void resumeChunkProcessing()
   {
      if (!chunkState_)
      {
         // no saved chunk state so this was an errant call and we should not do anything
         return;
      }

      // deliver the chunks on the thread pool instead of directly from this method
      // so that it is not a re-entrant method (beneficial for clients if they are holding locks, etc)
      boost::shared_ptr<AsyncClient<SocketService> > sharedThis =
            AsyncClient<SocketService>::shared_from_this();

      ioService_.post([=]()
      {
         bool complete = chunkState_->complete;

         // capture shared_ptr of this to keep instance alive while posting callback
         // to io service
         bool handled = sharedThis->deliverChunks(chunkState_->chunks, complete);

         if (handled)
         {
            if (!complete)
               sharedThis->readSomeContent();
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

protected:

   boost::asio::io_service& ioService() { return ioService_; }

   virtual SocketService& socket() = 0;

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
          boost::bind(
               &AsyncClient<SocketService>::handleWrite,
               AsyncClient<SocketService>::shared_from_this(),
               boost::asio::placeholders::error)
      );
   }

   void handleError(const Error& error)
   {
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
         errorHandler_(error);

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
   
private:

   virtual void connectAndWriteRequest() = 0;
   virtual std::string getDefaultHostHeader() = 0;

   bool retryConnectionIfRequired(const Error& connectionError,
                                  Error* pOtherError)
   {
      // retry if this is a connection unavailable error and the
      // caller has provided a connection retry profile
      if (http::isConnectionUnavailableError(connectionError) &&
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

         // if we aren't alrady past the maximum wait time then
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
      boost::system::error_code ec;
      connectionRetryContext_.retryTimer.expires_from_now(
                  connectionRetryContext_.profile.retryInterval,
                  ec);

      // attempt to schedule retry timer (should always succeed but
      // include error check to be paranoid/robust)
      if (!ec)
      {
         connectionRetryContext_.retryTimer.async_wait(boost::bind(
               &AsyncClient<SocketService>::handleConnectionRetryTimer,
               AsyncClient<SocketService>::shared_from_this(),
               boost::asio::placeholders::error));

         return true;
      }
      else
      {
         logError(Error(ec, ERROR_LOCATION));
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
              boost::bind(&AsyncClient<SocketService>::handleReadStatusLine,
                          AsyncClient<SocketService>::shared_from_this(),
                          boost::asio::placeholders::error));
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
                 boost::bind(&AsyncClient<SocketService>::handleReadHeaders,
                             AsyncClient<SocketService>::shared_from_this(),
                             boost::asio::placeholders::error));
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
         boost::bind(&AsyncClient<SocketService>::handleReadContent,
                     AsyncClient<SocketService>::shared_from_this(),
                     boost::asio::placeholders::error));
   }

   virtual bool stopReadingAndRespond()
   {
      return false;
   }

   virtual bool keepConnectionAlive()
   {
      return false;
   }

   void handleReadHeaders(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // parse headers
            ResponseParser::parseHeaders(&responseBuffer_, &response_);

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

            // append any lefover buffer contents to the body
            if (responseBuffer_.size() > 0)
               ResponseParser::appendToBody(&responseBuffer_, &response_);

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

            // copy content
            ResponseParser::appendToBody(&responseBuffer_, &response_);

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
            // break the chunk into more reasonable partial chunks
            size_t numChunks = static_cast<size_t>(ceil(static_cast<double>(chunk->size()) / maxChunkSize));
            size_t newChunkSize = static_cast<size_t>(static_cast<double>(chunk->size()) / numChunks);
            for (size_t i = 0; i < numChunks; ++i)
            {
               std::string chunkPiece = chunk->substr(i * newChunkSize, newChunkSize);
               newChunks.push_back(boost::make_shared<std::string>(std::move(chunkPiece)));
            }
         }
         else
            newChunks.push_back(chunk);
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
      const char* bufferPtr = boost::asio::buffer_cast<const char*>(responseBuffer_.data());

      // parse the bytes into chunks
      std::deque<boost::shared_ptr<std::string> > chunks;
      bool complete = chunkParser_->parse(bufferPtr, responseBuffer_.size(), &chunks);

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

         if (chunkHandler_)
         {
            bool keepGoing = chunkHandler_(response_, *chunk);

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
            // no chunk handler supplied, so caller expects to receive all chunks
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

      if (responseHandler_ && (!chunkedEncoding_ || !chunkHandler_))
         responseHandler_(response_);
      else if (chunkHandler_)
         chunkHandler_(response_, ""); // completion of chunks signified by empty chunk

      // free handlers in case they keep a strong reference to us
      // this will allow us to properly clean up in that case
      disableHandlers();
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
      ConnectionRetryContext(boost::asio::io_service& ioService)
         : stopTryingTime(boost::posix_time::not_a_date_time),
           retryTimer(ioService)
      {
      }

      http::ConnectionRetryProfile profile;
      boost::posix_time::ptime stopTryingTime;
      boost::asio::deadline_timer retryTimer;
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
   static constexpr double maxChunkSize = 1024.0*1024.0; // 1MB

   boost::asio::io_service& ioService_;
   ConnectionRetryContext connectionRetryContext_;
   bool logToStderr_;
   ResponseHandler responseHandler_;
   ErrorHandler errorHandler_;
   http::Request request_;
   boost::asio::streambuf responseBuffer_;
   boost::shared_ptr<ChunkParser> chunkParser_;
   ChunkHandler chunkHandler_;

   boost::shared_ptr<ChunkState> chunkState_;

   boost::mutex socketMutex_;
   bool closed_;

   bool requestWritten_;
   ConnectHandler connectHandler_;
};
   

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_CLIENT_HPP


