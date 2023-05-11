/*
 * HeaderCookieConstants.hpp
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

#ifndef CORE_HTTP_HEADER_CONSTANTS_HPP
#define CORE_HTTP_HEADER_CONSTANTS_HPP

// NOTE: All cookie and header constants should be defined in this file. When making changes to this
// file, keep cookies and headers grouped together and alphabetized within each section. Be sure to
// document all changes in the Header and Cookie Dictionary at
// docs/server/access_and_security/header_cookie_dictionary.qmd

// Cookie Constants ================================================================================
constexpr const char* kCSRFTokenCookie = "rs-csrf-token";
constexpr const char* kLegacyCookieSuffix = "-legacy";
constexpr const char* kPersistAuthCookie = "persist-auth";
constexpr const char* kPortTokenCookie = "port-token";
constexpr const char* kUserIdCookie = "user-id";
constexpr const char* kUserListCookie = "user-list-id";

// NOTE: Remove block when Ghost Orchid 2021.09 is not supported ===================================
constexpr const char* kOldCSRFTokenCookie = "csrf-token";
constexpr const char* kOldCSRFTokenHeader = "X-CSRF-Token";
// =================================================================================================

// Header Constants ================================================================================
constexpr const char* kCSRFTokenHeader = "X-RS-CSRF-Token";
constexpr const char* kPostbackExitCodeHeader = "X-Postback-ExitCode";
constexpr const char* kRStudioRpcCookieHeader = "X-RS-Session-Server-RPC-Cookie";
constexpr const char* kRStudioRpcRefreshAuthCreds = "X-RStudio-Refresh-Auth-Creds";
constexpr const char* kRStudioSessionRequiredHeader = "X-RStudio-Session-Required";
constexpr const char* kServerRpcSecretHeader = "X-RS-Session-Server-RPC-Secret";
constexpr const char* kSessionOriginalUri = "X-RStudio-Session-Original-Uri";
constexpr const char* kVirtualPathHeader = "X-RStudio-Virtual-Path";

#endif // SERVER_CORE_HTTP_HEADER_CONSTANTS_HPP
