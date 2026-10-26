/*
 * Ssl.hpp                                                                                                                                                                                                                                                   .hpp
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

#ifndef CORE_HTTP_SSL_HPP
#define CORE_HTTP_SSL_HPP

#include <string>
#include <vector>

#include <boost/asio/ip/tcp.hpp>

#include <shared_core/Error.hpp>

#include "BoostAsioSsl.hpp"

namespace rstudio {
namespace core {

class FilePath;

namespace http {
namespace ssl {

void validateSelfSignedCertificate(const FilePath& certPath);

struct ProtocolSupport
{
   std::vector<std::string> usable;
   std::vector<std::string> unusable;
};

// Sorts each protocol named in protocolList into usable or unusable, by
// running an in-memory TLS handshake that is pinned to that protocol. The
// list uses the syntax of the nginx ssl_protocols directive: SSLv2, SSLv3,
// TLSv1, TLSv1.1, TLSv1.2, and TLSv1.3, separated by spaces. Other names are
// skipped.
//
// The server side of the handshake uses the system OpenSSL library and
// configuration, the given certificate and key, and cipherList for TLS 1.2
// and earlier. The client side accepts any cipher, so only the server side
// decides the result.
//
// Returns an error if the certificate or key cannot be loaded, or if
// cipherList is not valid.
Error checkProtocolSupport(const std::string& protocolList,
                           const FilePath& certificatePath,
                           const FilePath& keyPath,
                           const std::string& cipherList,
                           ProtocolSupport* pSupport);

bool initializeSslContext(boost::asio::ssl::context* pContext,
                          bool verify,
                          const std::string& certificateAuthority = std::string());

void initializeSslStream(boost::asio::ssl::stream<boost::asio::ip::tcp::socket>* pSslStream,
                         const std::string& host);

} // namespace ssl
} // namespace http
} // namespace core 
} // namespace rstudio

#endif // CORE_HTTP_SSL_HPP
