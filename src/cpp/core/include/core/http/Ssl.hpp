/*
 * Ssl.hpp                                                                                                                                                                                                                                                   .hpp
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

#ifndef CORE_HTTP_SSL_HPP
#define CORE_HTTP_SSL_HPP

#include <boost/asio/ip/tcp.hpp>

#include "BoostAsioSsl.hpp"

namespace rstudio {
namespace core {
namespace http {
namespace ssl {

void initializeSslContext(boost::asio::ssl::context* pContext,
                          bool verify,
                          const std::string& certificateAuthority = std::string());

void initializeSslStream(boost::asio::ssl::stream<boost::asio::ip::tcp::socket>* pSslStream,
                         const std::string& host);

} // namespace ssl
} // namespace http
} // namespace core 
} // namespace rstudio

#endif // CORE_HTTP_SSL_HPP
