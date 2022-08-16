/*
 * SocketProxy.hpp
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

#ifndef CORE_HTTP_SOCKET_PROXY_HPP
#define CORE_HTTP_SOCKET_PROXY_HPP

#include <string>

#include <boost/array.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Thread.hpp>
#include <shared_core/Error.hpp>
#include <core/http/Socket.hpp>

namespace rstudio {
namespace core {
namespace http {

class SocketProxy : public boost::enable_shared_from_this<SocketProxy>
{
public:
   static void create(boost::shared_ptr<core::http::Socket> ptrClient,
                      boost::shared_ptr<core::http::Socket> ptrServer,
                      boost::function<bool()> checkFunction = 0,
                      boost::function<void()> closeFunction = 0)
   {
      boost::shared_ptr<SocketProxy> pProxy(new SocketProxy(ptrClient,
                                                            ptrServer,
                                                            checkFunction,
                                                            closeFunction));
      pProxy->readClient();
      pProxy->readServer();
   }

private:
   SocketProxy(boost::shared_ptr<core::http::Socket> ptrClient,
               boost::shared_ptr<core::http::Socket> ptrServer,
               boost::function<bool()> checkFunction,
               boost::function<void()> closeFunction)
      : ptrClient_(ptrClient), ptrServer_(ptrServer),
        checkFunction_(checkFunction), closeFunction_(closeFunction), closed_(false)
   {
   }

   void readClient();
   void readServer();

   void handleClientRead(const boost::system::error_code& e,
                         std::size_t bytesTransferred);
   void handleServerRead(const boost::system::error_code& e,
                         std::size_t bytesTransferred);
   void handleClientWrite(const boost::system::error_code& e,
                          std::size_t bytesTransferred);
   void handleServerWrite(const boost::system::error_code& e,
                          std::size_t bytesTransferred);
   void handleError(const boost::system::error_code& e,
                    const core::ErrorLocation& location);

   void close();

private:
   boost::shared_ptr<core::http::Socket> ptrClient_;
   boost::shared_ptr<core::http::Socket> ptrServer_;
   boost::array<char, 8192> clientBuffer_;
   boost::array<char, 8192> serverBuffer_;
   boost::recursive_mutex socketMutex_;
   boost::function<bool()> checkFunction_;
   boost::function<void()> closeFunction_;
   bool closed_ = false;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SOCKET_PROXY_HPP

