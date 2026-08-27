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
#include <boost/asio/buffers_iterator.hpp>
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

   // Register the notification delivered once the request has been written to a
   // successfully connected socket.
   //
   // downstreamClosedHandler is the counterpart for the case where there will
   // never be such a notification, because the client settled -- errored,
   // completed, or was torn down by its proxy. A settle is either flag: the
   // paths above run disableHandlers(), but a bare close() (an embedder
   // tearing down, FormProxy's own error path) marks only closed_, and after
   // one of those handleError() returns early on closed_ without ever reaching
   // disableHandlers(). Both are terminal and neither is ever cleared, so both
   // count here. It covers both sides of that race:
   //
   //  - settled before this call: reported synchronously, here.
   //  - registered before the request was written, then settled before
   //    handleWrite() could deliver the connect: reported by whichever of
   //    close()/disableHandlers() detaches the stored handler first, posted to
   //    the strand. Detaching is what makes the pair report exactly once.
   //  - registered after the request was written, so this call owns delivery:
   //    dispatched to the strand and decided there, so a settle racing the
   //    registration is reported as closed rather than as a connect the
   //    caller would act on by writing to a dead socket.
   //
   // Either is reachable whenever registration is deferred past execute(), as
   // ServerSessionProxy's upload path deliberately does (it posts its
   // ClientHandler to the io_context so the caller's ordering is preserved),
   // and on rserver's multi-threaded io_context the two can land on different
   // threads.
   //
   // Callers that gate their own progress on the connect notification (see
   // FormProxy) must supply it, or they will wait for a notification that
   // cannot arrive. Exactly one of the two handlers is ever delivered; neither
   // is silently dropped. It may be an empty function for callers with nothing
   // to unwind.
   virtual void setConnectHandler(const ConnectHandler& connectHandler,
                                  const ConnectHandler& downstreamClosedHandler) = 0;
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

typedef boost::asio::buffers_iterator<boost::asio::streambuf::const_buffers_type>
   ResponseBufferIterator;

// Locates the end of a response's header block within the bytes read so far,
// for use as an async_read_until() match condition.
//
// The obvious delimiter -- "\r\n\r\n" -- is wrong here, because
// ResponseParser::parseStatusLine() has already consumed the status line
// *including* its CRLF. A response carrying no header fields at all therefore
// leaves only "\r\n" in the buffer, which "\r\n\r\n" cannot match: the search
// runs off the end of that response and swallows however many bytes follow it,
// so the fields of the *next* response get attached to this one. Both
// "HTTP/1.1 204 No Content\r\n\r\n" and a bare "HTTP/1.1 100 Continue\r\n\r\n"
// are exactly that shape.
//
// Scanning line by line and stopping at the first empty line finds the
// terminator whether or not any fields precede it.
inline std::pair<ResponseBufferIterator, bool> findHeaderBlockEnd(
   ResponseBufferIterator begin,
   ResponseBufferIterator end)
{
   ResponseBufferIterator lineStart = begin;

   for (ResponseBufferIterator it = begin; it != end; ++it)
   {
      if (*it != '\n')
         continue;

      ResponseBufferIterator next = it;
      ++next;

      // the line ending here is the terminator when it holds nothing but an
      // optional CR (tolerating a bare LF the way the rest of this parsing
      // path does)
      std::size_t lineLength = std::distance(lineStart, it);
      if (lineLength == 0 || (lineLength == 1 && *lineStart == '\r'))
         return std::make_pair(next, true);

      lineStart = next;
   }

   // No terminator yet. Resume from the start of the incomplete trailing line
   // rather than from `end`: async_read_until remembers this position and
   // passes it as `begin` next time, and this scan is only correct when it
   // starts on a line boundary.
   return std::make_pair(lineStart, false);
}

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
      // Copy the handlers before taking the lock, because copying is the
      // fallible part of installing them: a boost::function copy allocates
      // whenever its target is too large for the small-object buffer. Copying
      // under the lock -- interleaved with the assignments, as this used to do
      // -- meant a failure left the client half-configured and LOCK_MUTEX
      // stepped over it, so the request below still went out with (say) a
      // response handler installed and no error handler behind it, and the
      // exchange could complete with nobody left to notify. Copying first
      // keeps the request from starting on failure; if an embedder has already
      // installed a handler (FixedBufferProxy does), detach it before the
      // exception reaches the caller so a pre-existing reference cycle is not
      // left with nothing in motion to break it.
      ResponseHandler newResponseHandler;
      ErrorHandler newErrorHandler;
      FixedBufferHandler newFixedBufferHandler;
      try
      {
         newResponseHandler = responseHandler;
         newErrorHandler = errorHandler;
         newFixedBufferHandler = fixedBufferHandler;
      }
      catch (...)
      {
         disableHandlers();
         throw;
      }

      // Commit them under socketMutex_, like every other access to these
      // members (see disableHandlers()). Once the lock is acquired, only
      // non-throwing swaps remain; each swap leaves the value it displaced in
      // the local, which destructs only once the lock is released. That
      // matters: destroying a displaced handler can release the last reference
      // to a consumer whose teardown calls back into close()/disableHandlers(),
      // which take this same (non-recursive) mutex.
      {
         // Unlike LOCK_MUTEX, let a lock-acquisition failure reach the caller.
         // Falling through here would start the request with the copied
         // handlers still in these locals and the members unchanged.
         boost::lock_guard<boost::mutex> lock(socketMutex_);
         responseHandler_.swap(newResponseHandler);
         errorHandler_.swap(newErrorHandler);

         // tested before the swap below, so this is still the incoming value
         if (newFixedBufferHandler)
            fixedBufferHandler_.swap(newFixedBufferHandler);
      }

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
   //
   // Contract: once this returns, no NEW handler invocation can be admitted
   // -- every invocation site copies the handler out under socketMutex_
   // (the same lock this method holds to mark handlersDisabled_ and detach
   // the members), so the check-then-copy admission step is mutually
   // exclusive with disabling. An invocation admitted concurrently on
   // another thread before the detach may still complete after this
   // returns; it operates on its own copy of the handler, which is
   // memory-safe for consumers kept alive by the handler's own bound
   // shared_ptr (FixedBufferProxy -- for which a late-completing invocation
   // simply lands on already-closed connections and no-ops into its
   // idempotent close path). The stronger "nothing is executing after this
   // returns" guarantee holds only when this is called on the client's
   // strand or from within a handler invocation itself, since all
   // invocations run strand-serialized. That covers the internal callers
   // (strand-bound continuations), /p/ (both connections share one strand,
   // see ServerSessionProxy.cpp), and single-io-thread embedders
   // (desktop::NetworkReply) -- but NOT /s/, where FixedBufferProxy's
   // write-completion path calls this from the downstream connection's own
   // strand; there, only the weaker (still memory-safe) guarantee applies.
   //
   // We deliberately do NOT close that residual window by blocking here
   // until in-flight invocations drain: the in-flight callback
   // (FixedBufferProxy::queueChunk) can itself be blocked acquiring
   // FixedBufferProxy's mutex_, which the disabling caller
   // (closeConnections(), called with mutex_ held) already owns -- a
   // wait-based scheme deadlocks exactly in the /s/ scenario it would exist
   // to serve. Likewise handlers must never be invoked while holding
   // socketMutex_: FixedBufferProxy's close paths run with its mutex_ held
   // and call close()/disableHandlers() (which take socketMutex_), so
   // invoking under socketMutex_ would invert that lock order.
   virtual void disableHandlers()
   {
      // the request has settled (or is being torn down by an embedder); stop
      // the overall deadline so it can't fire a spurious timeout afterwards
      cancelRequestDeadline();

      // Detach all handlers synchronously, under socketMutex_. Swap into
      // locals so the old values are destroyed after the lock is released (a
      // destructor releasing the last reference to a consumer must not run
      // under our lock -- the consumer's teardown may call back into
      // close()/disableHandlers()).
      ResponseHandler oldResponseHandler;
      ErrorHandler oldErrorHandler;
      FixedBufferHandler oldFixedBufferHandler;
      ConnectHandler oldConnectHandler;
      ConnectHandler oldDownstreamClosedHandler;
      bool reportDownstreamClosed = false;
      LOCK_MUTEX(socketMutex_)
      {
         handlersDisabled_ = true;
         oldResponseHandler.swap(responseHandler_);
         oldErrorHandler.swap(errorHandler_);
         oldFixedBufferHandler.swap(fixedBufferHandler_);
         oldConnectHandler.swap(connectHandler_);
         oldDownstreamClosedHandler.swap(downstreamClosedHandler_);

         // Are we detaching a connect notification that was stored but never
         // delivered? handleWrite() copies connectHandler_ out rather than
         // clearing it, so a non-empty handler alone doesn't say; what
         // distinguishes the two is requestWritten_, which handleWrite() sets
         // in the same critical section it reports the connect from. Still
         // false here means the connect never happened, so setConnectHandler()
         // stored a notification that this call is about to make unreachable
         // -- exactly the drop the downstream-closed handler exists to report.
         // (Idempotent on a second call: connectHandler_ is empty by then.)
         reportDownstreamClosed = oldConnectHandler && !requestWritten_;
      }
      END_LOCK_MUTEX

      // Post rather than invoke. We may have been called with a consumer's own
      // lock held (FixedBufferProxy::closeConnections() holds its mutex_) or
      // from inside a handler invocation, so calling out from here directly
      // would invert the lock order this method's declaration warns about.
      // Posting defers the notification until the disabling caller has
      // unwound, and binding the client's strand keeps it serialized with
      // every other handler invocation. The bound handler holds the consumer's
      // shared_ptr, so it stays alive until this runs.
      if (reportDownstreamClosed && oldDownstreamClosedHandler)
      {
         boost::asio::post(ioContext_,
                           boost::asio::bind_executor(*pStrand_, oldDownstreamClosedHandler));
      }

      // The old values now destruct inline, breaking any reference cycle
      // (e.g. fixedBufferHandler_'s bound shared_ptr<FixedBufferProxy>)
      // immediately and without depending on the io_context still running.
      // This is safe even when we are called from *within* the very handler
      // being detached: the invocation site invoked its own stack copy of
      // the handler (copied out under socketMutex_, see the contract
      // comment above), and that copy keeps the consumer alive until the
      // invocation returns and the call site's scope ends -- dropping our
      // reference here therefore never destroys a consumer mid-call.
      //
      // The one exception is a posted downstream-closed notification, whose
      // copy above deliberately outlives this scope: that notification is only
      // reachable before the request was written, so no FixedBufferHandler
      // cycle exists yet to keep alive.
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

   // Also reports a stored-but-undeliverable connect notification, for the same
   // reason disableHandlers() does: close() is a settle in its own right, and
   // on some paths the only one. It is public API, and callers that close a
   // client without disabling its handlers do exist -- FormProxy's own error
   // path, the launcher/session clients that close a streaming client to stop
   // it, and an embedder closing in its destructor (the case disableHandlers()
   // was added for in the first place). Nothing then detaches a connect handler
   // stored before the request was written: the aborted connect/write
   // completion returns early from handleError() on closed_ and never reaches
   // the disableHandlers() at the end of that function. A caller gated on the
   // notification would wait forever -- FormProxy buffers to maxBufferSize_,
   // pauses parsing, and hangs the upload. Reporting here is what makes
   // setConnectHandler()'s "exactly one of the two is delivered" hold for a
   // bare close() as well.
   virtual void close()
   {
      // displaced values destroyed after unlock, see execute()
      ConnectHandler oldConnectHandler;
      ConnectHandler oldDownstreamClosedHandler;
      bool reportDownstreamClosed = false;

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

         // Same test, and the same reasoning, as disableHandlers(): a non-empty
         // connectHandler_ alone doesn't mean one is still pending, because
         // handleWrite() invokes its own copy without clearing the member;
         // requestWritten_ -- set in that same critical section -- is what
         // discriminates a connect that was delivered from one that never
         // happened. Detaching here is also what keeps the pair idempotent: the
         // ordinary close()-then-disableHandlers() teardown finds the handler
         // gone and so cannot report a second time.
         reportDownstreamClosed = connectHandler_ && !requestWritten_;
         if (reportDownstreamClosed)
         {
            oldConnectHandler.swap(connectHandler_);
            oldDownstreamClosedHandler.swap(downstreamClosedHandler_);
         }
      }
      END_LOCK_MUTEX

      // Posted, not invoked, exactly as in disableHandlers(): we may be called
      // with a consumer's own lock held (FixedBufferProxy::closeConnections()
      // holds its mutex_), so calling out from here would invert the lock order
      // disableHandlers()'s declaration warns about. Binding the strand keeps
      // the notification serialized with every other handler invocation.
      if (reportDownstreamClosed && oldDownstreamClosedHandler)
      {
         boost::asio::post(ioContext_,
                           boost::asio::bind_executor(*pStrand_, oldDownstreamClosedHandler));
      }
   }

   virtual void setFixedBufferHandler(const FixedBufferHandler& fixedBufferHandler)
   {
      // Copied before the lock, for the reason execute() gives: detaching the
      // installed handler and then failing to put the new one in its place
      // left the client with no fixed buffer handler at all, which silently
      // flips a streaming consumer into accumulating into response_ -- a body
      // it never reads. Taking the fallible copy here means a copy failure
      // changes nothing; lock acquisition below is allowed to propagate for
      // the same all-or-nothing guarantee.
      FixedBufferHandler newFixedBufferHandler = fixedBufferHandler;

      // the swap leaves the displaced value in the local, to be destroyed
      // after releasing the lock -- see execute() for why destroying it under
      // the lock could deadlock
      {
         // Propagate lock-acquisition failure so the caller cannot mistake an
         // unchanged handler for a successful install.
         boost::lock_guard<boost::mutex> lock(socketMutex_);
         fixedBufferHandler_.swap(newFixedBufferHandler);
      }
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
      // Post unconditionally to the strand, and do ALL state inspection
      // (completionPending_, chunkState_) and mutation inside that handler --
      // not just the mutation, as an earlier version of this fix did. Every
      // other continuation in this class is strand-bound (see
      // asyncReadSome()/asyncWrite() above), and rserver's io_context runs a
      // thread pool, so reading completionPending_/chunkState_ here, on
      // whatever thread calls resumeChunkProcessing(), to decide whether/how
      // to post, would race strand-bound writes to those same members from a
      // different thread. resumeChunkProcessing() itself is called from the
      // downstream connection's own completion context
      // (FixedBufferProxy::writeChunk()), which for /p/ shares one strand
      // with this connection (see ServerSessionProxy.cpp), but for /s/
      // (which never calls setStrand()) that call arrives on whatever
      // context the downstream side runs on.
      auto self = AsyncClient<SocketService>::shared_from_this();
      boost::asio::post(ioContext_, boost::asio::bind_executor(*pStrand_, [this, self]()
      {
         // A rejected completion signal is tracked separately from
         // chunkState_ (see completionPending_'s declaration) and checked
         // first: retrying it re-enters closeAndRespond() itself rather than
         // deliverChunks(), so completion is never sent twice regardless of
         // what chunkState_ holds.
         if (completionPending_)
         {
            completionPending_ = false;
            closeAndRespond(); // may set completionPending_ again if still full
            return;
         }

         if (!chunkState_)
         {
            // no saved chunk state so this was an errant call and we should not do anything
            return;
         }

         bool complete = chunkState_->complete;
         bool handled = deliverChunks(chunkState_->chunks, complete);

         if (handled)
         {
            if (!complete)
               readSomeContent();
            else
               closeAndRespond();
         }
      }));
   }

   virtual void setConnectHandler(const ConnectHandler& connectHandler,
                                  const ConnectHandler& downstreamClosedHandler)
   {
      // if we are already connected, don't bother saving the connect handler
      // and just invoke it directly
      bool deferConnectNotification = false;
      bool invokeDownstreamClosedHandler = false;

      // Copied before the lock, for the reason execute() gives, and here the
      // two copies are a pair: storing the connect notification while failing
      // to store the downstream-closed handler that reports its loss left a
      // caller with no way to be told either way, which is the hang
      // setConnectHandler()'s contract exists to prevent. Each swap below
      // leaves the value it displaced in the local, to destroy after unlock.
      ConnectHandler newConnectHandler = connectHandler;
      ConnectHandler newDownstreamClosedHandler = downstreamClosedHandler;
      {
         // As above, lock failure is an install failure and must propagate.
         boost::lock_guard<boost::mutex> lock(socketMutex_);
         // Whether we have settled has to be read under the same lock
         // close()/disableHandlers() set these with -- see disableHandlers()'s
         // declaration -- so that the decision below can't be made against a
         // stale answer.
         //
         // Both flags, for the same reason deliverLateConnectNotification()
         // checks both: disableHandlers() marks handlersDisabled_, but a bare
         // close() (an embedder tearing down, FormProxy's own error path)
         // marks only closed_. Checking handlersDisabled_ alone sent a bare-
         // closed client down the !requestWritten_ branch below, which stores a
         // handler that only disableHandlers() ever reports -- and after a bare
         // close() the aborted connect/write completion returns early from
         // handleError() on closed_ without reaching disableHandlers(), so
         // nothing reported it at all.
         if (handlersDisabled_ || closed_)
         {
            // We have settled already, so connectHandler can never run: there
            // is no connect left to report, and disableHandlers() would detach
            // it again immediately even if we stored it. Report that instead of
            // dropping the notification -- see the declaration for why a caller
            // that never hears either way can hang.
            invokeDownstreamClosedHandler = true;
         }
         else if (!requestWritten_)
         {
            // store both: disableHandlers() may yet detach the connect
            // notification before handleWrite() can deliver it, and reports
            // that through the stored downstream-closed handler
            connectHandler_.swap(newConnectHandler);
            downstreamClosedHandler_.swap(newDownstreamClosedHandler);
         }
         else
         {
            // handleWrite() has already been and gone, so it will never
            // deliver this one; we have to. Deferred rather than invoked
            // below -- see there for why.
            deferConnectNotification = true;
         }
      }

      // Dispatch the late connect notification through the strand instead of
      // invoking it here. Deciding under the lock above and calling out after
      // it is a decision another thread's close()/disableHandlers() can
      // invalidate in between, and the caller acts on a connect notification
      // by writing: FormProxy would set connectedDownstream_, write to a
      // socket that has since closed, and take its error path -- which closes
      // the upstream connection too, resetting whatever response
      // FixedBufferProxy was mid-write of. Re-reading the state on the strand
      // at delivery time reports that settle as closed instead, which is the
      // answer the caller can act on safely. (The settled branch above stays
      // synchronous: neither flag is ever cleared, so it cannot go stale.)
      if (deferConnectNotification)
      {
         boost::shared_ptr<AsyncClient<SocketService>> self =
            AsyncClient<SocketService>::shared_from_this();
         boost::asio::post(
            ioContext_,
            boost::asio::bind_executor(
               *pStrand_,
               [self,
                connectHandler = std::move(newConnectHandler),
                downstreamClosedHandler = std::move(newDownstreamClosedHandler)]()
               {
                  self->deliverLateConnectNotification(connectHandler, downstreamClosedHandler);
               }));
      }
      else if (invokeDownstreamClosedHandler && newDownstreamClosedHandler)
      {
         // outside the lock: handlers must never be invoked while holding
         // socketMutex_ (see disableHandlers()'s declaration for the lock order)
         newDownstreamClosedHandler();
      }
   }

   // Deliver a connect notification registered after the request was already
   // written, from the strand. Which of the two handlers is correct is decided
   // here rather than at registration time so that a settle racing the
   // registration is reported as closed rather than as a connect the caller
   // would act on by writing to a dead socket.
   //
   // Both flags matter: disableHandlers() marks handlersDisabled_, but a bare
   // close() (an embedder tearing down, a deadline firing) only marks closed_,
   // and a connect notification is equally untrue in that case.
   void deliverLateConnectNotification(const ConnectHandler& connectHandler,
                                       const ConnectHandler& downstreamClosedHandler)
   {
      bool settled = false;
      LOCK_MUTEX(socketMutex_)
      {
         settled = handlersDisabled_ || closed_;
      }
      END_LOCK_MUTEX

      if (!settled)
      {
         if (connectHandler)
            connectHandler();
      }
      else if (downstreamClosedHandler)
      {
         downstreamClosedHandler();
      }
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

   void handleError(const Error& error, bool settleEvenIfClosed = false)
   {
      Error httpError = error;
      addErrorProperties(httpError);

      // check to see if the socket was closed purposefully
      // if so, we will ordinarily ignore the error. A handler snapshot failure
      // is different: it must still detach the handlers even if a previous
      // close() has already marked the socket closed, or a FixedBufferProxy
      // cycle can survive with no completion left to break it.
      bool alreadyClosed = false;
      LOCK_MUTEX(socketMutex_)
      {
         alreadyClosed = closed_;
      }
      END_LOCK_MUTEX

      if (alreadyClosed && !settleEvenIfClosed)
         return;

      // close the socket
      if (!alreadyClosed)
         close();

      // invoke error handler -- copy it out under socketMutex_ (checking
      // handlersDisabled_ in the same critical section disableHandlers()
      // uses to set it and detach the handler) so a concurrent
      // disableHandlers() call can't race between this check and the
      // invocation below. Actual invocation happens outside the lock, same
      // as handleWrite()'s connectHandler_ pattern. This copy is fallible for
      // the same reason as every other handler snapshot; if it fails, log the
      // original error and still detach everything rather than treating the
      // empty local as "no error handler installed."
      ErrorHandler handler;
      SnapshotStatus status = snapshotHandlers(
         [&]()
         {
            if (handlersDisabled_)
               return false;

            handler = errorHandler_;
            return true;
         });

      if (status == SnapshotStatus::Failed)
         logError(httpError);
      else if (status == SnapshotStatus::Ready && handler)
         handler(httpError);

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

   void handleHandlerSnapshotError(const char* description,
                                   const ErrorLocation& location) noexcept
   {
      try
      {
         Error error = systemError(boost::system::errc::state_not_recoverable,
                                   description,
                                   location);
         handleError(error, true);
      }
      catch (...)
      {
         // This path starts with a failed boost::function copy, usually a
         // bad_alloc. Building or reporting the synthetic Error can therefore
         // fail too. Never let that second failure bypass settlement or escape
         // an unguarded posted continuation such as resumeChunkProcessing().
         // Attempt each operation independently: close() can throw while
         // posting a pending connect notification, but disableHandlers() must
         // still get its chance to break any handler reference cycle.
         try
         {
            close();
         }
         catch (...)
         {
         }

         try
         {
            disableHandlers();
         }
         catch (...)
         {
         }
      }
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

   // Outcome of copying handler callbacks out of their members under
   // socketMutex_, so they can be invoked once the lock is released.
   enum class SnapshotStatus
   {
      Ready,    // the copies completed and are safe to act on
      Disabled, // disableHandlers() has already detached the handlers
      Failed    // the snapshot threw; the copies are unusable
   };

   // Take one such snapshot. `snapshot` runs holding socketMutex_ and returns
   // false when it finds the handlers already disabled, so each site keeps its
   // own disabled test in the same critical section as its own copies -- the
   // property disableHandlers()'s contract comment depends on.
   //
   // Unlike LOCK_MUTEX, a throw here is reported rather than logged and stepped
   // over. Copying a boost::function allocates whenever its target is too large
   // for the small-object buffer, so a snapshot can fail under exactly the
   // memory pressure streaming exists to relieve -- and LOCK_MUTEX's
   // fall-through would leave the locals empty, which every site below reads as
   // "no handler installed" and acts on: a body piece quietly appended to
   // response_ instead of handed to the streaming consumer, a completion or a
   // connect notification never delivered. (On the lock-acquisition path it
   // also left the sites' `disabled` flag indeterminate.) Failed sends the
   // caller to its error path instead, where the client settles and the error
   // handler reports it when that handler can itself be copied; otherwise the
   // failure is logged and cleanup still completes.
   //
   // No message is composed from the exception here: the one it exists to
   // survive is std::bad_alloc, so doing so could throw again on the way out,
   // and which site failed -- which the caller names -- is the more useful half
   // of the diagnosis anyway.
   template <typename F>
   SnapshotStatus snapshotHandlers(const F& snapshot)
   {
      try
      {
         boost::lock_guard<boost::mutex> lock(socketMutex_);
         return snapshot() ? SnapshotStatus::Ready : SnapshotStatus::Disabled;
      }
      catch (...)
      {
         return SnapshotStatus::Failed;
      }
   }

   void handleWrite(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // invoke connect handler if we have one -- checked under the
            // same lock disableHandlers() uses, see its declaration.
            //
            // The copy comes before requestWritten_ is set, and that ordering
            // is load-bearing: close() and disableHandlers() read the flag as
            // "handleWrite() has been and gone, so the connect notification was
            // reported" (see disableHandlers()). A copy that fails can never
            // report it, so leaving the flag false on that path is what routes
            // the still-pending notification to the downstream-closed handler
            // -- the answer a caller gated on the connect needs to hear.
            ConnectHandler handler;
            SnapshotStatus status = snapshotHandlers(
               [&]()
               {
                  bool disabled = handlersDisabled_;
                  if (!disabled)
                     handler = connectHandler_;

                  requestWritten_ = true;
                  return !disabled;
               });

            if (status == SnapshotStatus::Failed)
            {
               // Take the error path rather than reading the empty local as
               // "no connect handler installed" and reading on: a caller that
               // hears neither notification buffers until it times out (see
               // setConnectHandler()). close(), from within handleError(),
               // reports the pending notification; the error handler reports
               // the failure itself.
               handleHandlerSnapshotError("Failed to copy connect handler", ERROR_LOCATION);
               return;
            }

            // actual invocation should be outside of lock to prevent recursive
            // lock acquisitions. Disabled leaves `handler` empty, as before:
            // there is nothing left to report.
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
               // initiate async read of the headers -- see findHeaderBlockEnd()
               // for why this cannot just look for "\r\n\r\n"
               boost::asio::async_read_until(
                 socket(),
                 responseBuffer_,
                 findHeaderBlockEnd,
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
   // Reads fixedBufferHandler_ under socketMutex_ (like every access to the
   // handler members, see disableHandlers()); must not be called while
   // already holding that lock.
   bool useFixedBufferHandler() const
   {
      LOCK_MUTEX(socketMutex_)
      {
         return fixedBufferHandler_ && !bufferFullResponse_;
      }
      END_LOCK_MUTEX
      return false;
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

   // True for an interim (informational) response -- one the sender will follow
   // with a further response on this same connection.
   //
   // 101 is deliberately excluded. Despite its 1xx status it is the final HTTP
   // response on the connection: what follows is the upgraded protocol, not
   // another HTTP response, so skipping it would discard the handshake and
   // then try to parse websocket frames as a status line. (The localhost proxy
   // routes 101 to the buffered websocket-upgrade path via its buffer
   // predicate.)
   static bool isInterimResponse(const http::Response& response)
   {
      int statusCode = response.statusCode();
      return statusCode >= 100 &&
             statusCode < 200 &&
             statusCode != http::status::SwitchingProtocols;
   }

   void handleReadHeaders(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // parse headers
            ResponseParser::parseHeaders(&responseBuffer_, &response_);

            // RFC 7231 6.2: "A client MUST be able to parse one or more 1xx
            // responses received prior to a final response, even if the client
            // does not expect one." A 1xx is interim: it carries no body (RFC
            // 7230 3.3.3 rule 1) and another response follows it on the same
            // connection. Treating it as the final response, as this used to,
            // made the real response arrive as the interim one's body -- the
            // caller saw a 100/103 status with the literal bytes
            // "HTTP/1.1 200 OK\r\n..." behind it, and the actual response was
            // never delivered at all.
            //
            // Reachable: nothing in this tree strips a client's
            // "Expect: 100-continue" before the request is proxied on, and 103
            // Early Hints is emitted unprompted by a growing number of servers.
            if (isInterimResponse(response_))
            {
               // Bounded so an upstream that only ever emits interim responses
               // fails rather than looping for as long as it cares to send them.
               if (++interimResponseCount_ > maxInterimResponses)
               {
                  handleError(systemError(boost::system::errc::protocol_error,
                                          "Too many interim (1xx) responses from upstream",
                                          ERROR_LOCATION));
                  return;
               }

               // Discard it and go back for the next response's status line.
               // A 1xx has no body, so the bytes immediately after its header
               // block start the next response -- async_read_until finds those
               // already sitting in responseBuffer_ rather than waiting on the
               // socket for them. (It always posts its completion handler, so
               // looping this way does not nest stack frames.)
               response_.reset();

               boost::asio::async_read_until(
                 socket(),
                 responseBuffer_,
                 "\r\n",
                 boost::asio::bind_executor(*pStrand_,
                        boost::bind(&AsyncClient<SocketService>::handleReadStatusLine,
                                    AsyncClient<SocketService>::shared_from_this(),
                                    boost::asio::placeholders::error)));
               return;
            }

            // decide, from headers alone, whether a downstream handler needs the
            // entire body buffered (legacy path) or whether we can stream it.
            bufferFullResponse_ = bufferPredicate_ && bufferPredicate_(response_);

            // Decide chunked-ness from the parsed transfer-coding list, not
            // from a string compare against the raw field. The compare missed
            // "Chunked" and "gzip, chunked" -- both chunk-framed on the wire --
            // leaving the body read as if unencoded while a downstream consumer
            // (FixedBufferProxy) looking at the same header reached its own,
            // different conclusion. Two disagreeing answers to "is this body
            // chunked" is what let an already-chunked body be chunked a second
            // time, which RFC 7230 3.3.1 forbids outright.
            util::TransferEncoding transferEncoding =
               util::parseTransferEncoding(response_.headers());

            // A body still transfer-encoded after we have undone all we can --
            // gzip we cannot decode, chunked applied before another coding, or
            // chunked applied twice -- cannot be handed on as a decoded
            // payload, and silently relabelling it as one corrupts the
            // response. Fail closed instead. parseTransferEncoding() owns this
            // verdict so no caller has to reassemble it (and get it subtly
            // different).
            if (!transferEncoding.isDecodable)
            {
               handleError(systemError(boost::system::errc::protocol_error,
                                       "Unsupported Transfer-Encoding in response: " +
                                          response_.headerValue(kTransferEncoding),
                                       ERROR_LOCATION));
               return;
            }

            // if this is chunked encoding, start processing chunks
            //
            // Note this is deliberately not conditioned on Content-Length being
            // absent: RFC 7230 3.3.3 rule 3 says that when a message carries
            // both, the Transfer-Encoding wins and the Content-Length must not
            // be used for framing (and must be removed before forwarding, which
            // FixedBufferProxy's chunked branch does). Preferring Content-Length
            // there read a chunk-framed body as if it were raw bytes.
            if (transferEncoding.chunkedIsFinal)
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

         // Copy the handler out under socketMutex_, checking
         // handlersDisabled_ in the same critical section disableHandlers()
         // uses -- see that method's contract comment for why admission
         // must be locked, why the mode decision below must come from this
         // same copy (an unlocked useFixedBufferHandler() call here would
         // both race disableHandlers()'s detach and, once detached, silently
         // flip a streaming consumer into the accumulate-into-response_
         // branch mid-body), and why this local copy also serves as the
         // lifetime holder that keeps the consumer alive if it re-entrantly
         // calls disableHandlers() from within the invocation.
         FixedBufferHandler handler;
         SnapshotStatus status = snapshotHandlers(
            [&]()
            {
               if (handlersDisabled_)
                  return false;

               handler = fixedBufferHandler_;
               return true;
            });

         if (status == SnapshotStatus::Disabled)
         {
            // Handlers were disabled (e.g. the downstream connection already
            // errored out and broke its reference cycle via
            // disableHandlers()) while chunks were still in flight -- stop
            // processing entirely.
            return false;
         }
         else if (status == SnapshotStatus::Failed)
         {
            // An empty local here is indistinguishable from an absent handler,
            // and the else branch below would act on it: this piece would be
            // appended to response_ -- which nothing in streaming mode ever
            // reads -- and the consumer would go on to be told the body was
            // complete without it. Fail the request instead.
            handleHandlerSnapshotError("Failed to copy fixed buffer handler", ERROR_LOCATION);
            return false;
         }

         if (handler && !bufferFullResponse_) // useFixedBufferHandler(), from the copy
         {
            bool keepGoing = handler(response_, *chunk);

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
      // Copy both handlers out under socketMutex_, checking
      // handlersDisabled_ in the same critical section disableHandlers()
      // uses to set it and detach them -- see that method's contract comment
      // for why admission must be locked, and why the mode decision below is
      // computed from these same copies rather than re-reading the members
      // through useFixedBufferHandler(). Neither branch below checks closed_
      // on its own. The local copies also serve as the lifetime holders that
      // keep the consumer alive if it re-entrantly calls disableHandlers()
      // from within the invocation.
      //
      // Taken before the close below so the ordinary case can report through
      // the open client's error path. The failure path below also settles an
      // already-closed client: completionPending_ retries necessarily re-enter
      // here after the first attempt closed the socket.
      ResponseHandler responseHandler;
      FixedBufferHandler fixedBufferHandler;
      SnapshotStatus status = snapshotHandlers(
         [&]()
         {
            if (handlersDisabled_)
               return false;

            responseHandler = responseHandler_;
            fixedBufferHandler = fixedBufferHandler_;
            return true;
         });

      if (status == SnapshotStatus::Failed)
      {
         // This is the last word on the request, so an empty local here costs
         // more than anywhere else: whichever handler failed to copy, the
         // branches below would read as absent and settle silently -- either
         // handing a streamed response to the whole-body handler with an empty
         // response_ body, or completing with no notification delivered at all.
         handleHandlerSnapshotError("Failed to copy response handlers", ERROR_LOCATION);
         return;
      }

      if (!keepConnectionAlive())
         close();

      if (status == SnapshotStatus::Disabled)
      {
         disableHandlers(); // idempotent
         return;
      }

      // useFixedBufferHandler(), computed from the copy
      bool useFixedBuffer = fixedBufferHandler && !bufferFullResponse_;

      if (responseHandler && !(useFixedBuffer && (chunkedEncoding_ || streamResponse_)))
      {
         responseHandler(response_);
      }
      else if (useFixedBuffer)
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
            if (!fixedBufferHandler(response_, ""))
            {
               completionPending_ = true;
               return; // resumeChunkProcessing() will retry via closeAndRespond()
            }
         }
         else
         {
            fixedBufferHandler(response_, "");
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

   // How many interim (1xx) responses to skip before treating the upstream as
   // broken. Generous next to real usage -- a server sends at most a couple of
   // 103 Early Hints, or one 100 Continue -- while still bounding the work an
   // upstream can make us do before it commits to a final response.
   static constexpr int maxInterimResponses = 8;

   boost::asio::io_context& ioContext_;
   ConnectionRetryContext connectionRetryContext_;
   bool logToStderr_;
   ResponseHandler responseHandler_;
   ErrorHandler errorHandler_;

   // Protected by socketMutex_ (like closed_ below), not a bare atomic: every
   // site that invokes responseHandler_, errorHandler_, fixedBufferHandler_,
   // or connectHandler_ must check this and copy out the handler it's about
   // to call under the SAME lock disableHandlers() uses to set this and
   // detach the handlers, or a thread could observe this as false and still
   // be mid-call into a handler that another thread's disableHandlers() call
   // is concurrently detaching. See disableHandlers()'s declaration for the
   // full contract, including why the invocation-site copies double as the
   // lifetime holders that make its synchronous detach safe.
   bool handlersDisabled_ = false;

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
   int interimResponseCount_ = 0;        // 1xx responses skipped so far (see handleReadHeaders)
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

   // mutable so const readers (useFixedBufferHandler()) can lock it
   mutable boost::mutex socketMutex_;
   bool closed_;

   bool requestWritten_;
   ConnectHandler connectHandler_;

   // Companion to connectHandler_, stored alongside it and reported by
   // disableHandlers() when it detaches a connect notification that was never
   // delivered. See setConnectHandler()'s declaration.
   ConnectHandler downstreamClosedHandler_;

   boost::asio::io_context::strand defaultStrand_;

protected:
   boost::asio::io_context::strand* pStrand_ = &defaultStrand_;

};
   

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_CLIENT_HPP
