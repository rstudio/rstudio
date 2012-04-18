/*
 * SessionConstants.hpp
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

#define kVerifyInstallationSessionOption  "verify-installation"
#define kVerifyInstallationHomeDir        "/tmp/rstudio-verify-installation"

#define kLocalUriLocationPrefix           "/rsession-local/"
#define kPostbackUriScope                 "postback/"

#define kPostbackExitCodeHeader           "X-Postback-ExitCode"

#endif // SESSION_CONSTANTS_HPP

