/*
 * SocketProxy.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// boost requires that winsock2.h must be included before windows.h
#ifdef _WIN32
#include <winsock2.h>
#endif

#ifndef _WIN32
#include <core/http/BoostAsioSsl.hpp>
#endif

#include <core/http/SocketProxy.hpp>
#include <core/http/Util.hpp>

#include <iostream>

#include <boost/asio/placeholders.hpp>
#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>

#include <core/http/SocketUtils.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
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
   // client and server reads can happen simultaneously on two threads; a race
   // condition during close can lead to the socket not getting properly
   // shut down. use a simple mutex to prevent the threads from simultaneously
   // writing to the socket state.
   RECURSIVE_LOCK_MUTEX(socketMutex_)
   {
      if (!e)
      {
         if (checkFunction_ && !checkFunction_())
         {
            close();
            return;
         }

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
   END_LOCK_MUTEX
}

void SocketProxy::handleServerRead(const boost::system::error_code& e,
                                   std::size_t bytesTransferred)
{
   RECURSIVE_LOCK_MUTEX(socketMutex_)
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
   END_LOCK_MUTEX
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
   if (!http::isConnectionTerminatedError(error) &&
       (e != boost::asio::error::operation_aborted) &&
       !util::isSslShutdownError(e))
   {
      LOG_ERROR(error);
   }

   close();
}

void SocketProxy::close()
{
   RECURSIVE_LOCK_MUTEX(socketMutex_)
   {
      if (closed_)
         return;
      ptrClient_->close();
      ptrServer_->close();
      closed_ = true;
   }
   END_LOCK_MUTEX

   if (closeFunction_)
   {
      closeFunction_();
   }
}

} // namespace http
} // namespace core
} // namespace rstudio
