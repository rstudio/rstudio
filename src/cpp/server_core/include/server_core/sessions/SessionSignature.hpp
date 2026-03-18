/*
 * SessionSignature.hpp
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

#ifndef SERVER_CORE_SESSION_SIGNATURE_HPP
#define SERVER_CORE_SESSION_SIGNATURE_HPP

#include <shared_core/Error.hpp>
#include <core/http/Request.hpp>

#define kRStudioMessageSignature          "X-RS-Message-Signature"

namespace rstudio {
namespace server_core {
namespace sessions {

core::Error signRequest(const std::string& rsaPrivateKey,
                        core::http::Request& request,
                        bool includeUsername = true);

core::Error verifyRequestSignature(const std::string& rsaPublicKey,
                                   const core::http::Request& request,
                                   bool includeUsername = true);

core::Error verifyRequestSignature(const std::string& rsaPublicKey,
                                   const std::string& expectedUser,
                                   const core::http::Request& request,
                                   bool includeUsername = true);

// Signs the X-RS-CSRF-Bypass header value and sets X-RS-CSRF-Bypass-Sig on the request.
// No-op if X-RS-CSRF-Bypass is not present.
core::Error signCSRFBypass(const std::string& rsaPrivateKey,
                           core::http::Request& request);

// Verifies the X-RS-CSRF-Bypass signature. Returns Success and sets pBypassInfo
// if valid. Returns an error if the header is missing or the signature is invalid.
core::Error verifyCSRFBypass(const std::string& rsaPublicKey,
                             const core::http::Request& request,
                             std::string* pBypassInfo);

} // namespace sessions
} // namespace server_core
} // namespace rstudio

#endif // SERVER_CORE_SESSION_SIGNATURE_HPP
