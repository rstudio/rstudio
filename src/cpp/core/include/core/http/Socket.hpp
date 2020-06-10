/*
 * Socket.hpp
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

public:
   virtual void asyncReadSome(boost::asio::mutable_buffers_1 buffers,
                              Handler handler) = 0;

   virtual void asyncWrite(
                     const boost::asio::const_buffers_1& buffer,
                     Handler Handler) = 0;

   virtual void asyncWrite(
                     const std::vector<boost::asio::const_buffer>& buffers,
                     Handler Handler) = 0;

   virtual void close() = 0;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SOCKET_HPP
