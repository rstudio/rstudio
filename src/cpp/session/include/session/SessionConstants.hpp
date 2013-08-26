/*
 * SessionConstants.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SESSION_CONSTANTS_HPP
#define SESSION_CONSTANTS_HPP

#define kEventsPending                    "ep"

#define kRStudioUserIdentity              "RSTUDIO_USER_IDENTITY"
#define kRStudioLimitRpcClientUid         "RSTUDIO_LIMIT_RPC_CLIENT_UID"
#define kRSessionPortNumber               "RSTUDIO_SESSION_PORT"

#define kProgramModeSessionOption         "program-mode"
#define kSessionProgramModeDesktop        "desktop"
#define kSessionProgramModeServer         "server"

#define kUserIdentitySessionOption        "user-identity"
#define kUserIdentitySessionOptionShort   "u"

#define kProjectSessionOption             "project"
#define kProjectSessionOptionShort        "p"
#define kProjectSessionDelimiter          ":::"

#define kVerifyInstallationSessionOption  "verify-installation"

#define kTimeoutSessionOption             "session-timeout-minutes"

// NOTE: literal versions of these are depended upon by the desktop/rsinverse
// project so they should be updated there as well if they are changed
#define kLocalUriLocationPrefix           "/rsession-local/"
#define kPostbackUriScope                 "postback/"
#define kPostbackExitCodeHeader           "X-Postback-ExitCode"

// These constants are here so that the HttpConnectionListener::checkForAbort
// method can write the next session project (so that aborts don't require
// a full IDE reload)
#define kProjectsSettings                  "projects_settings"
#define kNextSessionProject                "next-session-project"

#endif // SESSION_CONSTANTS_HPP

