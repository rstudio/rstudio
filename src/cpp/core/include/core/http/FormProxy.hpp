/*
 * FormProxy.hpp
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

#ifndef CORE_HTTP_FORM_PROXY_HPP
#define CORE_HTTP_FORM_PROXY_HPP

#include <boost/enable_shared_from_this.hpp>
#include <boost/noncopyable.hpp>

#include <core/http/AsyncConnection.hpp>
#include <core/http/AsyncClient.hpp>

namespace rstudio {
namespace core {
namespace http {

class FormProxy : public boost::enable_shared_from_this<FormProxy>,
                  boost::noncopyable
{
public:
   FormProxy(const boost::shared_ptr<AsyncConnection>& pUpstreamConnection,
             const boost::shared_ptr<IAsyncClient>& pDownstreamCOnnection,
             uint64_t maxBufferSize = defaultMaxBufferSize);

   // Queue form data for the downstream connection. A final call marked
   // complete that leaves the body short of its Content-Length is the upstream
   // connection saying its client went away (RequestParser::abortForm), so
   // the downstream connection is closed, and the session stops waiting for
   // the rest.
   bool queueData(const std::string& formData, bool complete);
   void initialize();

private:
   constexpr static uint64_t defaultMaxBufferSize = 1'572'864; // 1.5 MB

   void abort();
   void writeData();
   void onDownstreamConnected();
   void onDownstreamClosed();
   void onDataWrote(const boost::system::error_code& ec);
   bool handleError(const boost::system::error_code& ec);

   boost::shared_ptr<AsyncConnection> pUpstreamConnection_;
   boost::shared_ptr<IAsyncClient> pDownstreamConnection_;
   uint64_t maxBufferSize_;

   boost::mutex mutex_;
   std::queue<std::string> writeBuffer_;
   uint64_t currentBufferSize_;
   bool connectedDownstream_;
   bool bufferFull_;

   // Set when the downstream connection settled before we could register our
   // connect handler; see onDownstreamClosed(). From then on form data is
   // accepted and discarded rather than buffered, so that the upstream
   // connection is never left paused waiting on a writer that cannot exist.
   bool downstreamClosed_;

   // form data accepted from the upstream connection so far
   uint64_t bytesReceived_;

   // set once abort() has closed the downstream connection, whose in-flight
   // write then fails with an error that is ours, not the session's
   bool aborted_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_FORM_PROXY_HPP
