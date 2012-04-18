/*
 * SocketAcceptorService.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

#include <boost/asio/io_service.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>


namespace core {
namespace http {

typedef boost::function<void(const boost::system::error_code& ec)> 
                                                               AcceptHandler ;

template <typename ProtocolType>
class SocketAcceptorService : boost::noncopyable
{
public:
   SocketAcceptorService()
      : acceptor_(ioService_)
   {
   }
   
   virtual ~SocketAcceptorService()
   {
      try
      {
         if (acceptor_.is_open())
         {
            boost::system::error_code ec ;
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

   boost::asio::io_service& ioService()
   {
      return ioService_ ;
   }
   
   typename ProtocolType::acceptor& acceptor()
   {
      return acceptor_;
   }
    
   Error accept(typename ProtocolType::socket& socket)
   {
      boost::system::error_code ec ;
      acceptor_.accept(socket, ec);
      if (ec)
         return Error(ec, ERROR_LOCATION);
      else
         return Success();
   }
   
   void asyncAccept(typename ProtocolType::socket& socket, 
                    AcceptHandler acceptHandler) 
   {
      acceptor_.async_accept(socket, acceptHandler);
   }
   
   void cancelPendingAccept(boost::system::error_code& ec) 
   {
      acceptor_.cancel(ec) ;
   }
   
   void closeAcceptor(boost::system::error_code& ec)
   {
      acceptor_.close(ec);
   }
   
private:
   boost::asio::io_service ioService_;
   typename ProtocolType::acceptor acceptor_;
};


} // namespace http
} // namespace core 

#endif // CORE_HTTP_SOCKET_ACCEPTOR_SERVICE_HPP
