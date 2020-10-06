/*
 * AuthCookies.hpp
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

#ifndef AUTH_COOKIES_HPP
#define AUTH_COOKIES_HPP

#define kUserIdCookie      "user-id"
#define kUserListCookie    "user-list-id"
#define kPersistAuthCookie "persist-auth"

#define kSameSiteOmitOption ""
#define kSameSiteNoneOption "none"
#define kSameSiteLaxOption  "lax"
// Strict is **too strict** and may confuse customers. Better not have it unless explicitly requested.
// If we were to have this option, selecting it would cause the browser to omit existing cookies when:
// - Visiting from email links to RStudio, what would **always** require a new sign-in.
// - Visiting links on other sites to RStudio, what would **always** require a new sign-in.
// That's because `SameSite=Strict` requires that even the `referal` header points to the same site.
//#define kSameSiteStrictOption  "strict"

#endif // AUTH_COOKIES_HPP
