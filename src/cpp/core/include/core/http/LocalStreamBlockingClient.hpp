/*
 * LocalStreamBlockingClient.hpp
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

#ifndef CORE_HTTP_LOCAL_STREAM_BLOCKING_CLIENT_HPP
#define CORE_HTTP_LOCAL_STREAM_BLOCKING_CLIENT_HPP


#include <core/http/BlockingClient.hpp>

#include <core/http/LocalStreamAsyncClient.hpp>

namespace rstudio {
namespace core {
namespace http {  

inline Error sendRequest(const FilePath& localStreamPath,
                         const http::Request& request,
                         http::Response* pResponse)
{
   // create client
   boost::asio::io_service ioService;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(
         new LocalStreamAsyncClient(ioService, localStreamPath, true));

   // execute blocking request
   return sendRequest<boost::asio::local::stream_protocol::socket>(ioService,
                                                                   pClient,
                                                                   request,
                                                                   pResponse);
}
   
} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_LOCAL_STREAM_BLOCKING_CLIENT_HPP
