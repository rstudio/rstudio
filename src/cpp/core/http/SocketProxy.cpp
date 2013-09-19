/*
 * SocketProxy.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/http/SocketProxy.hpp>

#include <iostream>

#include <boost/bind.hpp>

#include <boost/asio/placeholders.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/http/SocketUtils.hpp>

using namespace core;

namespace core {
namespace http {

void SocketProxy::readClient()
{
   ptrClient_->asyncReadSome(
        boost::asio::buffer(clientBuffer_),
         boost::bind(
            &SocketProxy::handleClientRead,
            SocketProxy::shared_from_this(),
            boost::asio::placeholders::error,
            boost::asio::placeholders::bytes_transferred));
}

void SocketProxy::readServer()
{
   ptrServer_->asyncReadSome(
        boost::asio::buffer(serverBuffer_),
         boost::bind(
            &SocketProxy::handleServerRead,
            SocketProxy::shared_from_this(),
            boost::asio::placeholders::error,
            boost::asio::placeholders::bytes_transferred));
}

void SocketProxy::handleClientRead(const boost::system::error_code& e,
                                   std::size_t bytesTransferred)
{
   if (!e)
   {
      std::vector<boost::asio::const_buffer> buffers;
      buffers.push_back(boost::asio::buffer(clientBuffer_.data(),
                                            bytesTransferred));
      ptrServer_->asyncWrite(buffers,
                             boost::bind(
                                &SocketProxy::handleServerWrite,
                                SocketProxy::shared_from_this(),
                                boost::asio::placeholders::error,
                                boost::asio::placeholders::bytes_transferred));
   }
   else
   {
      handleError(e, ERROR_LOCATION);
   }
}

void SocketProxy::handleServerRead(const boost::system::error_code& e,
                                   std::size_t bytesTransferred)
{
   if (!e)
   {
      std::vector<boost::asio::const_buffer> buffers;
      buffers.push_back(boost::asio::buffer(serverBuffer_.data(),
                                            bytesTransferred));
      ptrClient_->asyncWrite(buffers,
                             boost::bind(
                                &SocketProxy::handleClientWrite,
                                SocketProxy::shared_from_this(),
                                boost::asio::placeholders::error,
                                boost::asio::placeholders::bytes_transferred));
   }
   else
   {
      handleError(e, ERROR_LOCATION);
   }
}

void SocketProxy::handleClientWrite(const boost::system::error_code& e,
                                    std::size_t bytesTransferred)
{
   if (!e)
   {
      readServer();
   }
   else
   {
      handleError(e, ERROR_LOCATION);
   }
}

void SocketProxy::handleServerWrite(const boost::system::error_code& e,
                                    std::size_t bytesTransferred)
{
   if (!e)
   {
      readClient();
   }
   else
   {
      handleError(e, ERROR_LOCATION);
   }
}

void SocketProxy::handleError(const boost::system::error_code& e,
                              const core::ErrorLocation& location)
{
   // log the error if it wasn't connection terminated
   Error error(e, location);
   if (!http::isConnectionTerminatedError(error))
      LOG_ERROR(error);

   close();
}

void SocketProxy::close()
{
   ptrClient_->close();
   ptrServer_->close();
}

} // namespace http
} // namespace core
