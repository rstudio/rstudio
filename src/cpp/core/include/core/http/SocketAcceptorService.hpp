/*
 * SocketAcceptorService.hpp
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

#ifndef CORE_HTTP_SOCKET_ACCEPTOR_SERVICE_HPP
#define CORE_HTTP_SOCKET_ACCEPTOR_SERVICE_HPP

#include <string>

#include <boost/function.hpp>
#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>

#include <boost/asio/io_context.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>


namespace rstudio {
namespace core {
namespace http {

typedef boost::function<void(const boost::system::error_code& ec)> 
                                                               AcceptHandler;

template <typename ProtocolType>
class SocketAcceptorService : boost::noncopyable
{
public:
   SocketAcceptorService()
      : pInternalIOService_(new boost::asio::io_context()),
        ioContext_(*pInternalIOService_),
        acceptor_(ioContext_)
   {
   }
   
   explicit SocketAcceptorService(boost::asio::io_context& ioContext)
      : ioContext_(ioContext),
        acceptor_(ioContext_)
   {
   }

   virtual ~SocketAcceptorService()
   {
      try
      {
         if (acceptor_.is_open())
         {
            boost::system::error_code ec;
            closeAcceptor(ec);
            if (ec && (ec.value() != boost::system::errc::bad_file_descriptor))
               LOG_ERROR(Error(ec, ERROR_LOCATION));
         }
      }
      catch(...)
      {
      }
   }
   
   // COPYING: boost::noncopyable

public:

   boost::asio::io_context& ioContext()
   {
      return ioContext_;
   }
   
   typename ProtocolType::acceptor& acceptor()
   {
      return acceptor_;
   }

   void asyncAccept(typename ProtocolType::socket& socket, 
                    AcceptHandler acceptHandler) 
   {
      acceptor_.async_accept(socket, acceptHandler);
   }
   
   void closeAcceptor(boost::system::error_code& ec)
   {
      acceptor_.close(ec);
   }
   
private:
   boost::scoped_ptr<boost::asio::io_context> pInternalIOService_;
   boost::asio::io_context& ioContext_;
   typename ProtocolType::acceptor acceptor_;
};


} // namespace http
} // namespace core 
} // namespace rstudio

#endif // CORE_HTTP_SOCKET_ACCEPTOR_SERVICE_HPP
