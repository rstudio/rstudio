/*
 * ServerPAMAuth.hpp
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

#ifndef SERVER_PAM_AUTH_HPP
#define SERVER_PAM_AUTH_HPP

namespace core {
   class Error;
}

namespace server {
namespace pam_auth {
   
core::Error initialize();

} // namespace pam_auth
} // namespace server

#endif // SERVER_PAM_AUTH_HPP

