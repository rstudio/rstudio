
/*
 * SessionConstants.hpp
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

#ifndef SESSION_CONSTANTS_HPP
#define SESSION_CONSTANTS_HPP

#define kEventsPending                    "ep"

#define kRStudioUserIdentity              "RSTUDIO_USER_IDENTITY"
#define kRStudioSystemUserIdentity        "X-RStudioSystemUserIdentity"
#define kRStudioUserIdentityDisplay       "X-RStudioUserIdentity"
#define kRStudioLimitRpcClientUid         "RSTUDIO_LIMIT_RPC_CLIENT_UID"
#define kRSessionPortNumber               "RSTUDIO_SESSION_PORT"
#define kRSessionStandalonePortNumber     "RSTUDIO_STANDALONE_PORT"
#define kRStudioSessionStream             "RSTUDIO_SESSION_STREAM"
#define kRStudioMultiSession              "RSTUDIO_MULTI_SESSION"
#define kRStudioSessionScopeProject       "RSTUDIO_SESSION_SCOPE_PROJECT"
#define kRStudioSessionScopeId            "RSTUDIO_SESSION_SCOPE_ID"
#define kRStudioSessionRoute              "RSTUDIO_SESSION_ROUTE"
#define kRStudioRequiredUserGroup         "RSTUDIO_REQUIRED_USER_GROUP"
#define kRStudioMinimumUserId             "RSTUDIO_MINIMUM_USER_ID"
#define kRStudioSigningKey                "RSTUDIO_SIGNING_KEY"
#define kRStudioVersion                   "RSTUDIO_VERSION"
#define kRSessionRsaPublicKey             "RSTUDIO_SESSION_RSA_PUBLIC_KEY"
#define kRSessionRsaPrivateKey            "RSTUDIO_SESSION_RSA_PRIVATE_KEY"
#define kRStudioSessionUserLicenseSoftLimitReached "RSTUDIO_SESSION_USER_LICENSE_SOFT_LIMIT_REACHED"

#define kRStudioDefaultRVersion           "RSTUDIO_DEFAULT_R_VERSION"
#define kRStudioDefaultRVersionHome       "RSTUDIO_DEFAULT_R_VERSION_HOME"

#define kRStudioUserHomePage              "RSTUDIO_USER_HOME_PAGE"

#define kSessionUrlEnvVar                 "RS_SESSION_URL"
#define kServerUrlEnvVar                  "RS_SERVER_URL"
#define kPortTokenEnvVar                  "RS_PORT_TOKEN"

#define kSessionSslCertEnvVar             "RS_SESSION_SSL_CERT"
#define kSessionSslCertKeyEnvVar          "RS_SESSION_SSL_CERT_KEY"
#define kSessionSslCertPathEnvVar         "RS_SESSION_SSL_CERT_PATH"
#define kSessionSslCertKeyPathEnvVar      "RS_SESSION_SSL_CERT_KEY_PATH"

#define kProgramModeSessionOption         "program-mode"
#define kRStudioProgramMode               "RSTUDIO_PROGRAM_MODE"
#define kSessionProgramModeDesktop        "desktop"
#define kSessionProgramModeServer         "server"

#define kShowUserIdentitySessionOption    "show-user-identity"
#define kUserIdentitySessionOption        "user-identity"
#define kUserIdentitySessionOptionShort   "u"

#define kProjectSessionOption             "project"
#define kProjectSessionOptionShort        "p"

#define kScopeSessionOption               "scope"
#define kScopeSessionOptionShort          "s"

#define kVerifyInstallationSessionOption  "verify-installation"

#define kRunAutomationSessionOption       "run-automation"
#define kAutomationAgentSessionOption     "automation-agent"

#define kRunTestsSessionOption            "run-tests"
#define kRunScriptSessionOption           "run-script"

#define kVersionSessionOption             "version"

#define kLimitSessionOption               "session-limit"
#define kTimeoutSessionOption             "session-timeout-minutes"
#define kTimeoutSuspendSessionOption      "session-timeout-suspend"
#define kDisconnectedTimeoutSessionOption "session-disconnected-timeout-minutes"
#define kSessionEphemeralEnvVars          "session-ephemeral-env-vars"
#define kVerifySignaturesSessionOption    "verify-signatures"
#define kStandaloneSessionOption          "standalone"
#define kWwwAddressSessionOption          "www-address"
#define kWwwPortSessionOption             "www-port"
#define kWwwResusePorts                   "www-reuse-ports"
#define kSessionSslCertOption             "cert"
#define kSessionSslCertKeyOption          "cert-key"
#define kTerminalPortOption               "terminal-port"
#define kSessionSuspendOnIncompleteStatement "session-suspend-on-incomplete-statement"
#define kSessionAsyncRpcEnabled           "session-async-rpc-enabled"
#define kSessionAsyncRpcTimeoutMs         "session-async-rpc-timeout-ms"
#define kSessionHandleOfflineEnabled      "session-handle-offline-enabled"
#define kSessionHandleOfflineTimeoutMs    "session-handle-offline-timeout-ms"
#define kSessionUseFileStorage            "session-use-file-storage"

#define kLauncherSessionOption            "launcher-session"

#define kWebSocketPingInterval            "websocket-ping-seconds"
#define kWebSocketConnectTimeout          "websocket-connect-timeout"
#define kWebSocketLogLevel                "websocket-log-level"
#define kWebSocketHandshakeTimeout        "websocket-handshake-timeout"

#define kPackageOutputInPackageFolder     "package-output-to-package-folder"

#define kRootPathSessionOption            "session-root-path"
#define kUseSecureCookiesSessionOption    "session-use-secure-cookies"
#define kSameSiteSessionOption            "session-same-site"

// NOTE: literal versions of these are depended upon by the desktop/rsinverse
// project so they should be updated there as well if they are changed
#define kLocalUriLocationPrefix           "/rsession-local/"
#define kPostbackUriScope                 "postback/"

#define kMonitoredPath      "monitored"
#define kListsPath          "lists"

// legacy, stores only the project file path (no custom name)
#define kProjectMruList     "project_mru"

// stores project file path and optional name separated by kProjectNameSepChar
#define kProjectNameMruList "project_name_mru" 
#define kProjectNameSepChar '\t' // must match PROJECT_NAME_SEP in GWT code

#define kServerHomeSetting     "showUserHomePage"
#define kServerHomeAlways      "always"
#define kServerHomeNever       "never"
#define kServerHomeSessions    "sessions"

#define kReuseSessionsForProjectLinksSettings "reuseSessionsForProjectLinks"

#define kRStudioNoTransformRedirect "X-RStudio-NoTransformRedirect"

#define kSessionProxyDefaultPort   "8789"
#define kRStudioSessionProxyPort   "X-RStudio-Session-Proxy-Port"

#define kSessionUserLicenseSoftLimitReached  "session-user-license-soft-limit-reached"

#define kRestoreWorkspaceNo       0
#define kRestoreWorkspaceYes      1
#define kRestoreWorkspaceDefault  2

#define kRunRprofileNo       0
#define kRunRprofileYes      1
#define kRunRprofileDefault  2

#define kSessionTmpDirEnvVar       "RS_SESSION_TMP_DIR"
#define kSessionTmpDir             "rstudio-rsession"

#ifdef QUARTO_ENABLED
# define kDefaultPandocPath        "bin/quarto/bin/tools"
#else
# define kDefaultPandocPath        "bin/pandoc"
#endif

#define kDefaultNodePath           "bin/node"
#define kDefaultQuartoPath         "bin/quarto"
#define kDefaultCopilotPath        "bin/copilot-language-server"
#define kDefaultRsclangPath        "bin/rsclang"

#ifdef _WIN32
# define kDefaultPostbackPath       "bin/rpostback.exe"
#else
# define kDefaultPostbackPath       "bin/rpostback"
#endif

// json rpc methods we handle (the rest are delegated to the HttpServer)
const char * const kClientInit = "client_init";
const char * const kEditCompleted = "edit_completed";
const char * const kChooseFileCompleted = "choose_file_completed";
const char * const kLocatorCompleted = "locator_completed";
const char * const kUserPromptCompleted = "user_prompt_completed";
const char * const kAdminNotificationAcknowledged = "admin_notification_acknowledged";
const char * const kHandleUnsavedChangesCompleted = "handle_unsaved_changes_completed";
const char * const kQuitSession = "quit_session";
const char * const kSuspendSession = "suspend_session";
const char * const kInterrupt = "interrupt";
const char * const kConsoleInput = "console_input";
const char * const kSuspendForRestart = "suspend_for_restart";
const char * const kAuthStatus = "auth_status";
const char * const kRStudioAPIShowDialogMethod = "rstudio_api_show_dialog";
const char * const kPing = "ping";

// session exit codes - note max value supported by Linux is 255
#define SESSION_EXIT_CODE_OFFSET              200
#define SESSION_EXIT_SCOPE_INVALID_SESSION    SESSION_EXIT_CODE_OFFSET + 1
#define SESSION_EXIT_SCOPE_INVALID_PROJECT    SESSION_EXIT_CODE_OFFSET + 2
#define SESSION_EXIT_SCOPE_MISSING_PROJECT    SESSION_EXIT_CODE_OFFSET + 3
#define SESSION_EXIT_INVALID_RPC_CONFIG       SESSION_EXIT_CODE_OFFSET + 4
#define SESSION_EXIT_EXCEEDED_MEMORY_LIMIT    SESSION_EXIT_CODE_OFFSET + 5

#endif // SESSION_CONSTANTS_HPP

