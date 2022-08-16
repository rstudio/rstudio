/*
 * ServerPAMAuthOverlay.hpp
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

#ifndef SERVER_PAM_AUTH_OVERLAY_HPP
#define SERVER_PAM_AUTH_OVERLAY_HPP

#include <string>

namespace rstudio {
namespace server {
namespace pam_auth {
namespace overlay {

bool canSetSignInCookies();
void onUserPasswordAvailable(const std::string& username,
                         const std::string& password);
void onUserPasswordUnavailable(const std::string& username,
                           bool signedOut = false);

} // namespace overlay
} // namespace pam_auth
} // namespace server
} // namespace rstudio

#endif // SERVER_PAM_AUTH_OVERLAY_HPP
