/*
 * BlockingClient.hpp
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

#ifndef CORE_HTTP_BLOCKING_CLIENT_HPP
#define CORE_HTTP_BLOCKING_CLIENT_HPP

#include <boost/function.hpp>


#include <shared_core/FilePath.hpp>

#include <core/http/AsyncClient.hpp>

namespace rstudio {
namespace core {
namespace http {

namespace {

void responseHandler(const http::Response& response,
                     http::Response* pTargetResponse)
{
   pTargetResponse->assign(response);
}

void errorHandler(const Error& error, Error* pTargetError)
{
   *pTargetError = error;
}

}

template <typename SocketService>
Error sendRequest(boost::asio::io_service& ioService,
                  boost::shared_ptr<AsyncClient<SocketService> > pClient,
                  const http::Request& request,
                  http::Response* pResponse)
{
   // assign request
   pClient->request().assign(request);

   // start execution
   Error error;
   pClient->execute(boost::bind(responseHandler, _1, pResponse),
                    boost::bind(errorHandler, _1, &error));

   // run the io service
   boost::system::error_code ec;
   ioService.run(ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);

   // return error status
   return error;
}

   

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_LOCAL_STREAM_BLOCKING_CLIENT_HPP
