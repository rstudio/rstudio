/*
 * ServerPAMAuth.hpp
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

#ifndef SERVER_PAM_AUTH_HPP
#define SERVER_PAM_AUTH_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace server {
namespace pam_auth {
   
bool pamLogin(const std::string& username, const std::string& password);

core::Error initialize();

namespace overlay {

core::Error initialize();

} // namespace overlay
} // namespace pam_auth
} // namespace server
} // namespace rstudio

#endif // SERVER_PAM_AUTH_HPP
