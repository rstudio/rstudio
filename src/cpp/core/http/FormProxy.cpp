/*
 * FormProxy.cpp
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

#include <core/http/FormProxy.hpp>
#include <core/http/Util.hpp>

namespace rstudio {
namespace core {
namespace http {

FormProxy::FormProxy(const boost::shared_ptr<AsyncConnection>& pUpstreamConnection,
                     const boost::shared_ptr<IAsyncClient>& pDownstreamConnection,
                     uint64_t maxBufferSize) :
   pUpstreamConnection_(pUpstreamConnection),
   pDownstreamConnection_(pDownstreamConnection),
   maxBufferSize_(maxBufferSize),
   currentBufferSize_(0),
   connectedDownstream_(false),
   bufferFull_(false),
   downstreamClosed_(false)
{
}

void FormProxy::initialize()
{
   pDownstreamConnection_->setConnectHandler(boost::bind(&FormProxy::onDownstreamConnected,
                                                         shared_from_this()),
                                             boost::bind(&FormProxy::onDownstreamClosed,
                                                         shared_from_this()));
}

void FormProxy::onDownstreamClosed()
{
   // The downstream connection settled before we could register our connect
   // handler: it either errored, or the session returned its final response
   // without reading the whole request body (a 401/403/413/redirect on an
   // upload is entirely legitimate HTTP). Either way there is nowhere left to
   // write form data to, and waiting for a connect notification that can no
   // longer arrive would buffer up to maxBufferSize_, pause parsing, and hang
   // the upload until the client times out.
   //
   // Deliberately do NOT close pUpstreamConnection_ here. Whoever settled the
   // downstream connection already owns finishing it -- the error path has run
   // the client's ErrorHandler, and the ordinary path has handed the response
   // to FixedBufferProxy, which closes both connections once it has written it
   // (see FixedBufferProxy::closeConnections). Closing it from here would race
   // that write and turn a clean 413 into a connection reset. Instead drop what
   // we have buffered and accept-and-discard the rest, which keeps the upstream
   // connection parsing so it can be closed normally; the drain is bounded by
   // that in-flight response, which is already complete.
   LOCK_MUTEX(mutex_)
   {
      downstreamClosed_ = true;

      std::queue<std::string> discarded;
      writeBuffer_.swap(discarded);
      currentBufferSize_ = 0;

      if (bufferFull_)
      {
         // we had told the connection to stop reading; let it finish
         bufferFull_ = false;
         pUpstreamConnection_->continueParsing();
      }
   }
   END_LOCK_MUTEX
}

void FormProxy::onDownstreamConnected()
{
   LOCK_MUTEX(mutex_)
   {
      connectedDownstream_ = true;
      writeData();
   }
   END_LOCK_MUTEX
}

bool FormProxy::queueData(const std::string& data)
{
   LOCK_MUTEX(mutex_)
   {
      if (downstreamClosed_)
      {
         // nowhere to write it -- discard, but keep accepting so the upstream
         // connection drains and closes rather than stalling (onDownstreamClosed)
         return true;
      }

      if (currentBufferSize_ + data.size() > maxBufferSize_)
      {
         bufferFull_ = true;

         // we are temporarily out of space and cannot buffer any more form data
         // until more data is written to the outgoing (client) connection
         // signal to connection to stop reading new data, and redeliver this form data
         // when we have space for it
         return false;
      }

      // queue the data
      currentBufferSize_ += data.size();
      writeBuffer_.push(data);

      if (writeBuffer_.size() == 1 && connectedDownstream_)
      {
         // we're the only data in the buffer, so we need to initiate a write
         writeData();
      }
   }
   END_LOCK_MUTEX

   return true;
}

void FormProxy::writeData()
{
   if (writeBuffer_.empty())
   {
      if (bufferFull_)
      {
         // we previously hit a full buffer condition
         // inform the connection that we are ready to continue parsing data
         bufferFull_ = false;
         pUpstreamConnection_->continueParsing();
      }

      return;
   }

   const std::string& data = writeBuffer_.front();

   boost::asio::const_buffer buffer(data.c_str(), data.size());
   pDownstreamConnection_->asyncWrite(buffer,
                                      boost::bind(&FormProxy::onDataWrote,
                                                  shared_from_this(),
                                                  boost::asio::placeholders::error));
}

void FormProxy::onDataWrote(const boost::system::error_code& ec)
{
   if (handleError(ec))
      return;

   LOCK_MUTEX(mutex_)
   {
      currentBufferSize_ -= writeBuffer_.front().size();
      writeBuffer_.pop();

      // keep writing any queued chunks until we're empty
      writeData();
   }
   END_LOCK_MUTEX
}

bool FormProxy::handleError(const boost::system::error_code& ec)
{
   if (ec)
   {
      Error error(ec, ERROR_LOCATION);

      if (!http::isConnectionTerminatedError(error))
         LOG_ERROR(error);

      // close both connections to stop all data transfer
      pDownstreamConnection_->close();
      pUpstreamConnection_->close();
      return true;
   }

   return false;
}

} // namespace http
} // namespace core
} // namespace rstudio
