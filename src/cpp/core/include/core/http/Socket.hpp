/*
 * Socket.hpp
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

#ifndef CORE_HTTP_SOCKET_HPP
#define CORE_HTTP_SOCKET_HPP

#include <vector>

#include <boost/system/error_code.hpp>
#include <boost/function.hpp>

#include <boost/asio/buffer.hpp>

namespace rstudio {
namespace core {
namespace http {  

class Socket
{
public:
   typedef boost::function<void(const boost::system::error_code&, std::size_t)>
                                                         Handler;

   // An implementation of Handler that does nothing.   
   static void NullHandler(const boost::system::error_code& ec, std::size_t bytes_transferred)
   {   
   }

   // boost::bind two handlers to the first two arguments, and you've got
   // yourself a Handler that invokes each of those handler arguments in turn.
   static void joinHandlers(Handler a, Handler b, const boost::system::error_code& ec, std::size_t bytes_transferred)
   {
      a(ec, bytes_transferred);
      b(ec, bytes_transferred);
   }

public:
   virtual void asyncReadSome(boost::asio::mutable_buffers_1 buffers,
                              Handler handler) = 0;

   virtual void asyncWrite(
                     const boost::asio::const_buffers_1& buffer,
                     Handler handler) = 0;

   virtual void asyncWrite(
                     const std::vector<boost::asio::const_buffer>& buffers,
                     Handler handler) = 0;

   virtual void close() = 0;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SOCKET_HPP
