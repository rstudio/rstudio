/*
 * FormProxy.hpp
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

   bool queueData(const std::string& formData);
   void initialize();

private:
   constexpr static uint64_t defaultMaxBufferSize = 1024*1024*1.5; // 1.5 MB

   void writeData();
   void onDownstreamConnected();
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
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_FORM_PROXY_HPP
