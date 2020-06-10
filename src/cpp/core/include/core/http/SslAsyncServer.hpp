/*
 * SslAsyncServer.hpp
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

#ifndef CORE_HTTP_SSL_ASYNC_SERVER_HPP
#define CORE_HTTP_SSL_ASYNC_SERVER_HPP

#include <boost/asio/io_service.hpp>
#include <boost/asio/ssl.hpp>

#include <shared_core/FilePath.hpp>
#include <core/http/AsyncServerImpl.hpp>
#include <core/http/TcpIpSocketUtils.hpp>

namespace rstudio {
namespace core {
namespace http {

class SslAsyncServer : public AsyncServerImpl<boost::asio::ip::tcp>
{
public:

   SslAsyncServer(const std::string& serverName,
                  const std::string& baseUri = std::string(),
                  bool disableOriginCheck = true,
                  const std::vector<boost::regex>& allowedOrigins = std::vector<boost::regex>(),
                  const Headers& additionalHeaders = Headers())
      : AsyncServerImpl(serverName, baseUri, disableOriginCheck, allowedOrigins, additionalHeaders)
   {
   }
   
   Error init(const std::string& address,
              const std::string& port,
              const FilePath& certFile,
              const FilePath& keyFile)
   {
      if (!certFile.exists())
      {
         return systemError(boost::system::errc::no_such_file_or_directory,
                            "Certificate file does not exist",
                            ERROR_LOCATION);
      }

      if (!keyFile.exists())
      {
         return systemError(boost::system::errc::no_such_file_or_directory,
                            "Key file does not exist",
                            ERROR_LOCATION);
      }

      boost::shared_ptr<boost::asio::ssl::context> context(
               new boost::asio::ssl::context(boost::asio::ssl::context::sslv23));
      context->set_options(boost::asio::ssl::context::default_workarounds |
                           boost::asio::ssl::context::no_sslv2 |
                           boost::asio::ssl::context::single_dh_use);

      boost::system::error_code ec;
      context->use_certificate_chain_file(certFile.getAbsolutePath(), ec);
      if (ec)
         return Error(ec, ERROR_LOCATION);

      context->use_private_key_file(keyFile.getAbsolutePath(), boost::asio::ssl::context::pem, ec);
      if (ec)
         return Error(ec, ERROR_LOCATION);

      setSslContext(context);

      return initTcpIpAcceptor(acceptorService(), address, port);
   }
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SSL_ASYNC_SERVER_HPP



