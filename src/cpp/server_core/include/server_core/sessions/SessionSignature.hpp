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

} // namespace sessions
} // namespace server_core
} // namespace rstudio

#endif // SERVER_CORE_SESSION_SIGNATURE_HPP
