/*
 * NamedPipeProtocol.cpp
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

#include <core/http/NamedPipeProtocol.hpp>

namespace rstudio {
namespace core {
namespace http {


// specialization of closeSocket for stream handle lowest level
template<> Error closeSocket(
              boost::asio::windows::stream_handle::lowest_layer_type& socket)
{
   if (socket.is_open())
   {
      boost::system::error_code ec;
      socket.close(ec);
      if (ec)
        return Error(ec, ERROR_LOCATION);
   }

   return Success();
}

// specialization of closeSocket for stream handles
template<> Error closeSocket(boost::asio::windows::stream_handle& socket)
{
   // delegate to lowest_layer (it's the same object)
   return closeSocket(socket.lowest_layer());
}


   
} // namespace http
} // namespace core
} // namespace rstudio

