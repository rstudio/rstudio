/*
 * SessionChat.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "SessionChat.hpp"
#include "SessionNodeTools.hpp"

#include <map>
#include <functional>

#include <boost/algorithm/string.hpp>
#include <boost/asio.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/regex.hpp>

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/http/Util.hpp>
#include <core/system/Process.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionUrlPorts.hpp>

#include "session-config.h"

#define CHAT_LOG_IMPL(__LOGGER__, __FMT__, ...)                             \
   do                                                                       \
   {                                                                        \
      std::string __message__ = fmt::format(__FMT__, ##__VA_ARGS__);        \
      std::string __formatted__ =                                           \
          fmt::format("[{}]: {}", __func__, __message__);                   \
      __LOGGER__("chat", __formatted__);                                    \
      if (chatLogLevel() >= 1)                                              \
         std::cerr << __formatted__ << std::endl;                           \
   } while (0)

#define DLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_DEBUG_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_WARNING_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_ERROR_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define ILOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_INFO_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define TLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_TRACE_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)

// Use a default section of 'chat' for errors / warnings
#ifdef LOG_ERROR
# undef LOG_ERROR
# define LOG_ERROR(error) LOG_ERROR_NAMED("chat", error)
#endif

using namespace rstudio::core;
using namespace rstudio::core::system;
using namespace rstudio::session::module_context;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {

namespace {

// ============================================================================
// Process management
// ============================================================================
PidType s_chatBackendPid = -1;
int s_chatBackendPort = -1;
std::string s_chatBackendUrl;
int s_chatBackendRestartCount = 0;
const int kMaxRestartAttempts = 1;

// ============================================================================
// Installation paths
// ============================================================================
const char* const kPositAiDirName = "ai";
const char* const kClientDirPath = "dist/client";
const char* const kServerScriptPath = "dist/server/main.js";
const char* const kIndexFileName = "index.html";

// ============================================================================
// Logging
// ============================================================================
int s_chatLogLevel = 0;

int chatLogLevel()
{
   return s_chatLogLevel;
}

SEXP rs_chatSetLogLevel(SEXP logLevelSEXP)
{
   int logLevel = r::sexp::asInteger(logLevelSEXP);
   s_chatLogLevel = logLevel;
   return logLevelSEXP;
}

// ============================================================================
// JSON-RPC Message Handling
// ============================================================================

// Buffer for accumulating stdout data from backend
std::string s_backendOutputBuffer;

// Queue of parsed JSON-RPC messages from backend
std::vector<json::Value> s_pendingResponses;

// Regex for parsing Content-Length header (case-insensitive per HTTP spec)
boost::regex s_contentLengthRegex("Content-Length:\\s*(\\d+)", boost::regex::icase);

// Map of notification method names to handler functions
using NotificationHandler = std::function<void(const json::Object&)>;
std::map<std::string, NotificationHandler> s_notificationHandlers;

// ============================================================================
// Forward Declarations
// ============================================================================
Error startChatBackend();

// ============================================================================
// JSON-RPC Notification Handling
// ============================================================================

void registerNotificationHandler(const std::string& method, NotificationHandler handler)
{
   s_notificationHandlers[method] = handler;
}

void handleNotification(const std::string& method, const json::Object& params)
{
   auto it = s_notificationHandlers.find(method);
   if (it != s_notificationHandlers.end())
   {
      it->second(params);
   }
   else
   {
      WLOG("Unhandled notification method: {}", method);
   }
}

void processBackendMessage(const json::Value& message)
{
   if (!message.isObject())
   {
      WLOG("Invalid JSON-RPC message: not an object");
      return;
   }

   json::Object messageObj = message.getObject();

   // Check for method field (indicates notification or request)
   std::string method;
   Error error = json::readObject(messageObj, "method", method);
   if (!error)  // Success - method field is present
   {
      // This is a notification (no 'id' field) or request from backend
      json::Object params;
      json::readObject(messageObj, "params", params);

      // For now, we only handle notifications
      handleNotification(method, params);
   }
   else
   {
      // No method field - this would be a response to a request we sent (future implementation)
      if (chatLogLevel() >= 2)
         DLOG("Received response from backend (not yet handled)");
   }
}

void handleLoggerLog(const json::Object& params)
{
   std::string level;
   std::string message;

   if (json::readObject(params, "level", level))  // Returns error if missing
   {
      WLOG("logger/log notification missing 'level' field");
      return;
   }

   if (json::readObject(params, "message", message))  // Returns error if missing
   {
      WLOG("logger/log notification missing 'message' field");
      return;
   }

   // Map backend log levels to RStudio logging macros
   // Use "databot" prefix to distinguish backend logs
   std::string prefixedMessage = fmt::format("[databot] {}", message);

   if (level == "trace")
   {
      TLOG("{}", prefixedMessage);
   }
   else if (level == "debug")
   {
      DLOG("{}", prefixedMessage);
   }
   else if (level == "info")
   {
      ILOG("{}", prefixedMessage);
   }
   else if (level == "warn")
   {
      WLOG("{}", prefixedMessage);
   }
   else if (level == "error")
   {
      ELOG("{}", prefixedMessage);
   }
   else
   {
      DLOG("[databot] [{}] {}", level, message);
   }
}

void processPendingMessages()
{
   // Process all pending messages from the backend
   while (!s_pendingResponses.empty())
   {
      json::Value message = s_pendingResponses.front();
      s_pendingResponses.erase(s_pendingResponses.begin());

      processBackendMessage(message);
   }
}

void onBackgroundProcessing(bool isIdle)
{
   // Process pending messages from backend on main thread
   processPendingMessages();

   // Future: Add timeout handling for request/response pattern
   // (similar to SessionCopilot.cpp lines 1448-1462)
}

// ============================================================================
// Installation Detection
// ============================================================================

/**
 * Verify that an AI installation at the given path contains all required files.
 */
bool verifyPositAiInstallation(const FilePath& positAiPath)
{
   if (!positAiPath.exists())
      return false;

   FilePath clientDir = positAiPath.completeChildPath(kClientDirPath);
   FilePath serverScript = positAiPath.completeChildPath(kServerScriptPath);
   FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);

   return clientDir.exists() && serverScript.exists() && indexHtml.exists();
}

FilePath locatePositAiInstallation()
{
   // 1. Check environment variable override (for development/testing)
   std::string rstudioPositAiPath = core::system::getenv("RSTUDIO_POSIT_AI_PATH");
   if (!rstudioPositAiPath.empty())
   {
      FilePath positAiPath(rstudioPositAiPath);
      if (verifyPositAiInstallation(positAiPath))
      {
         DLOG("Using AI installation from RSTUDIO_POSIT_AI_PATH: {}", positAiPath.getAbsolutePath());
         return positAiPath;
      }
      else
      {
         WLOG("RSTUDIO_POSIT_AI_PATH set but installation invalid: {}", rstudioPositAiPath);
      }
   }

   // 2. Check user data directory (XDG-based, platform-appropriate)
   // Linux: ~/.local/share/rstudio/ai
   // macOS: ~/Library/Application Support/RStudio/ai
   // Windows: %LOCALAPPDATA%/RStudio/ai
   FilePath userPositAiPath = xdg::userDataDir().completePath(kPositAiDirName);
   if (verifyPositAiInstallation(userPositAiPath))
   {
      DLOG("Using user-level AI installation: {}", userPositAiPath.getAbsolutePath());
      return userPositAiPath;
   }

   // 3. Check system-wide installation (XDG config directory)
   // Linux: /etc/rstudio/ai
   // Windows: C:/ProgramData/RStudio/ai
   FilePath systemPositAiPath = xdg::systemConfigDir().completePath(kPositAiDirName);
   if (verifyPositAiInstallation(systemPositAiPath))
   {
      DLOG("Using system-wide AI installation: {}", systemPositAiPath.getAbsolutePath());
      return systemPositAiPath;
   }

   DLOG("No valid AI installation found. Checked locations:");
   if (!rstudioPositAiPath.empty())
      DLOG("  - RSTUDIO_POSIT_AI_PATH: {}", rstudioPositAiPath);
   DLOG("  - User data dir: {}", userPositAiPath.getAbsolutePath());
   DLOG("  - System config dir: {}", systemPositAiPath.getAbsolutePath());

   return FilePath(); // Not found
}

// ============================================================================
// Port Allocation
// ============================================================================

Error allocatePort(int* pPort)
{
   // Use bind to port 0 and let OS assign
   using boost::asio::ip::tcp;
   boost::asio::io_context ioContext;
   tcp::acceptor acceptor(ioContext);

   tcp::endpoint endpoint(tcp::v4(), 0); // Port 0 = OS assigns
   boost::system::error_code ec;
   acceptor.open(endpoint.protocol(), ec);
   if (ec)
      return systemError(ec, ERROR_LOCATION);

   acceptor.bind(endpoint, ec);
   if (ec)
      return systemError(ec, ERROR_LOCATION);

   *pPort = acceptor.local_endpoint().port();
   acceptor.close();

   return Success();
}

// ============================================================================
// WebSocket URL Construction
// ============================================================================

std::string buildWebSocketUrl(int port)
{
#ifdef RSTUDIO_SERVER
   if (options().programMode() == kSessionProgramModeServer)
   {
      // Build localhost URL
      std::string localhostUrl = "http://127.0.0.1:" +
                                 boost::lexical_cast<std::string>(port);

      DLOG("Building WebSocket URL for port {}, localhost URL: {}", port, localhostUrl);

      // Transform to portmapped path (returns relative path like "p/58fab3e4" or "/p/58fab3e4")
      std::string portmappedPath = url_ports::mapUrlPorts(localhostUrl);

      // Ensure portmapped path starts with /
      if (!portmappedPath.empty() && portmappedPath[0] != '/')
         portmappedPath = "/" + portmappedPath;

      // Remove trailing slash from portmapped path if present
      if (!portmappedPath.empty() && portmappedPath[portmappedPath.length() - 1] == '/')
         portmappedPath = portmappedPath.substr(0, portmappedPath.length() - 1);

      DLOG("Normalized portmapped path: {}", portmappedPath);

      // Get base URL from session
      std::string baseUrl = persistentState().activeClientUrl();
      DLOG("Base URL: {}", baseUrl);

      // Parse base URL to get scheme and host
      http::URL parsedBase(baseUrl);

      // Determine WebSocket scheme based on HTTP scheme
      std::string wsScheme = (parsedBase.protocol() == "https") ? "wss" : "ws";

      // Build complete WebSocket URL as string with /ai-chat base path
      // The proxy will route {portmappedPath}/ai-chat/ws to http://127.0.0.1:{port}/ai-chat/ws
      std::string wsUrl = wsScheme + "://" + parsedBase.host() + portmappedPath + "/ai-chat";
      DLOG("Final WebSocket URL: {}", wsUrl);

      return wsUrl;
   }
#endif

   // Desktop mode: include /ai-chat base path for DatabotServer routing
   // The client will append /ws to get ws://127.0.0.1:{port}/ai-chat/ws
   std::string desktopUrl = "ws://127.0.0.1:" + boost::lexical_cast<std::string>(port) + "/ai-chat";
   DLOG("Desktop WebSocket URL: {}", desktopUrl);
   return desktopUrl;
}

// ============================================================================
// Process Management
// ============================================================================

void onBackendStdout(core::system::ProcessOperations& ops, const std::string& output)
{
   // Append new output to buffer
   s_backendOutputBuffer.append(output);

   // Process all complete messages in the buffer
   while (true)
   {
      // Find the end of headers (blank line: \r\n\r\n) first
      // This ensures we only parse Content-Length from a valid header block
      size_t headerEnd = s_backendOutputBuffer.find("\r\n\r\n");
      if (headerEnd == std::string::npos)
      {
         // Headers not complete yet
         break;
      }

      // Extract header block (everything before \r\n\r\n)
      std::string headerBlock = s_backendOutputBuffer.substr(0, headerEnd);

      // Look for Content-Length header within this header block only
      // This prevents matching stray "Content-Length:" text in non-protocol stdout
      boost::smatch matches;
      if (!boost::regex_search(headerBlock, matches, s_contentLengthRegex))
      {
         // No Content-Length in this header block - malformed JSON-RPC message
         WLOG("JSON-RPC message missing Content-Length header, skipping malformed message");
         // Skip past this malformed message (discard up to and including \r\n\r\n)
         s_backendOutputBuffer = s_backendOutputBuffer.substr(headerEnd + 4);
         continue;
      }

      // Extract content length
      std::string lengthStr = matches[1].str();
      int contentLength = safe_convert::stringTo<int>(lengthStr, -1);
      if (contentLength <= 0)
      {
         WLOG("Invalid Content-Length value in backend message: {}", lengthStr);
         // Skip past this malformed message
         s_backendOutputBuffer = s_backendOutputBuffer.substr(headerEnd + 4);
         continue;
      }

      // Calculate where the body starts and ends
      size_t bodyStart = headerEnd + 4; // Skip past \r\n\r\n
      size_t bodyEnd = bodyStart + contentLength;

      // Check if we have the complete body
      if (s_backendOutputBuffer.size() < bodyEnd)
      {
         // Body not complete yet
         break;
      }

      // Extract the message body (IMPORTANT: byte-based, not character-based)
      std::string messageBody = s_backendOutputBuffer.substr(bodyStart, contentLength);

      // Verbose logging
      if (chatLogLevel() >= 2)
      {
         DLOG("Received message from backend: {}", messageBody);
      }

      // Parse JSON
      json::Value messageValue;
      if (messageValue.parse(messageBody))
      {
         WLOG("Failed to parse JSON from backend: {}", messageBody);
      }
      else
      {
         // Queue the parsed message for processing
         s_pendingResponses.push_back(messageValue);
      }

      // Remove the processed message from buffer
      s_backendOutputBuffer = s_backendOutputBuffer.substr(bodyEnd);
   }

   // Messages will be processed by onBackgroundProcessing on the main thread
}

void onBackendStderr(core::system::ProcessOperations& ops, const std::string& output)
{
   WLOG("Chat backend stderr: {}", output);
}

void onBackendExit(int exitCode)
{
   WLOG("Chat backend exited with code: {}", exitCode);

   s_chatBackendPid = -1;
   s_chatBackendPort = -1;
   s_chatBackendUrl.clear();

   // Clear JSON-RPC state
   s_backendOutputBuffer.clear();
   s_pendingResponses.clear();

   // Auto-restart once
   if (s_chatBackendRestartCount < kMaxRestartAttempts)
   {
      s_chatBackendRestartCount++;
      DLOG("Attempting to restart chat backend (attempt {})", s_chatBackendRestartCount);

      Error error = startChatBackend();
      if (error)
         LOG_ERROR(error);
   }
}

Error startChatBackend()
{
   // Check if already running
   if (s_chatBackendPid != -1)
      return Success();

   // Locate installation
   FilePath positAiPath = locatePositAiInstallation();
   if (positAiPath.isEmpty())
   {
      std::string userPath = xdg::userDataDir().completePath(kPositAiDirName).getAbsolutePath();
      std::string systemPath = xdg::systemConfigDir().completePath(kPositAiDirName).getAbsolutePath();
      std::string errorMsg = fmt::format(
         "Posit AI installation not found. Install to: {} (user) or {} (system)",
         userPath, systemPath);
      return systemError(boost::system::errc::no_such_file_or_directory,
                        errorMsg,
                        ERROR_LOCATION);
   }

   // Find Node.js
   FilePath nodePath;
   Error error = node_tools::findNode(&nodePath, "rstudio.positAi.nodeBinaryPath");
   if (error)
      return error;

   // Locate backend script
   FilePath serverScript = positAiPath.completeChildPath(kServerScriptPath);
   if (!serverScript.exists())
      return systemError(boost::system::errc::no_such_file_or_directory,
                        "Backend script not found: " + serverScript.getAbsolutePath(),
                        ERROR_LOCATION);

   // Allocate a free port
   error = allocatePort(&s_chatBackendPort);
   if (error)
      return error;

   DLOG("Allocated port {} for chat backend", s_chatBackendPort);

   // Build WebSocket URL BEFORE launching (so it's ready when needed)
   s_chatBackendUrl = buildWebSocketUrl(s_chatBackendPort);

   // Build command arguments
   // Use relative path so Node.js resolves modules from working directory
   std::vector<std::string> args;
   args.push_back(kServerScriptPath);
   args.push_back("-h");
   args.push_back("127.0.0.1");  // Explicitly bind to IPv4 to match client connection
   args.push_back("-p");
   args.push_back(boost::lexical_cast<std::string>(s_chatBackendPort));
   args.push_back("--json"); // Enable JSON-RPC mode

   // Set up environment
   core::system::Options environment;
   core::system::environment(&environment);

   // Set up callbacks
   core::system::ProcessCallbacks callbacks;
   callbacks.onStarted = [](core::system::ProcessOperations& ops) {
      s_chatBackendPid = ops.getPid();
      DLOG("Chat backend started with PID: {}", s_chatBackendPid);
   };
   callbacks.onStdout = onBackendStdout;
   callbacks.onStderr = onBackendStderr;
   callbacks.onExit = onBackendExit;

   // Process options
   core::system::ProcessOptions processOpts;
   processOpts.allowParentSuspend = true;
   processOpts.exitWithParent = true;
   processOpts.callbacksRequireMainThread = true;
   processOpts.reportHasSubprocs = false;
#ifndef _WIN32
   processOpts.detachSession = true;
#else
   processOpts.detachProcess = true;
#endif
   processOpts.workingDir = positAiPath;
   processOpts.environment = environment;

   // Log execution details
   std::string argsStr = boost::algorithm::join(args, " ");
   DLOG("Launching chat backend: nodePath={}, args=[{}], workingDir={}, exitWithParent={}",
        nodePath.getAbsolutePath(),
        argsStr,
        processOpts.workingDir.getAbsolutePath(),
        processOpts.exitWithParent);

   // Launch via ProcessSupervisor
   error = processSupervisor().runProgram(
       nodePath.getAbsolutePath(),
       args,
       processOpts,
       callbacks);

   if (error)
   {
      LOG_ERROR(error);
      s_chatBackendPort = -1;
      s_chatBackendUrl.clear();
      return error;
   }

   return Success();
}

// ============================================================================
// Static File Serving
// ============================================================================

std::string getContentType(const std::string& extension)
{
   static std::map<std::string, std::string> contentTypes = {
      {".html", "text/html; charset=utf-8"},
      {".js", "application/javascript; charset=utf-8"},
      {".mjs", "application/javascript; charset=utf-8"},
      {".css", "text/css; charset=utf-8"},
      {".json", "application/json; charset=utf-8"},
      {".svg", "image/svg+xml"},
      {".png", "image/png"},
      {".jpg", "image/jpeg"},
      {".jpeg", "image/jpeg"},
      {".gif", "image/gif"},
      {".ico", "image/x-icon"},
      {".woff", "font/woff"},
      {".woff2", "font/woff2"},
      {".ttf", "font/ttf"},
      {".eot", "application/vnd.ms-fontobject"}
   };

   auto it = contentTypes.find(extension);
   if (it != contentTypes.end())
      return it->second;

   return "application/octet-stream";
}

Error validateAndResolvePath(const FilePath& clientRoot,
                             const std::string& requestPath,
                             FilePath* pResolvedPath)
{
   // Remove query string and fragment
   std::string cleanPath = requestPath;
   size_t queryPos = cleanPath.find('?');
   if (queryPos != std::string::npos)
      cleanPath = cleanPath.substr(0, queryPos);

   size_t fragmentPos = cleanPath.find('#');
   if (fragmentPos != std::string::npos)
      cleanPath = cleanPath.substr(0, fragmentPos);

   // URL decode
   cleanPath = http::util::urlDecode(cleanPath);

   // Build full path
   FilePath resolved = clientRoot.completeChildPath(cleanPath);

   // CRITICAL: Canonicalize to resolve symlinks and ".." before security check
   Error error = core::system::realPath(resolved, &resolved);
   if (error)
      return error;

   // Security: Ensure resolved path is within clientRoot
   std::string resolvedStr = resolved.getAbsolutePath();
   std::string rootStr = clientRoot.getAbsolutePath();

   if (!boost::starts_with(resolvedStr, rootStr))
   {
      return systemError(boost::system::errc::permission_denied,
                        "Path traversal attempt detected",
                        ERROR_LOCATION);
   }

   *pResolvedPath = resolved;
   return Success();
}

Error handleAIChatRequest(const http::Request& request,
                          http::Response* pResponse)
{
   // Locate installation
   FilePath positAiPath = locatePositAiInstallation();
   if (positAiPath.isEmpty())
   {
      pResponse->setStatusCode(http::status::NotFound);
      pResponse->setBody("Posit AI not installed.");
      return Success();
   }

   FilePath clientRoot = positAiPath.completeChildPath(kClientDirPath);

   // Parse requested path from URI
   // URI format: /ai-chat/<path>
   std::string uri = request.uri();
   size_t pos = uri.find("/ai-chat/");
   if (pos == std::string::npos)
   {
      pResponse->setStatusCode(http::status::BadRequest);
      return Success();
   }

   std::string requestPath = uri.substr(pos + 9); // Length of "/ai-chat/"

   // Default to index.html
   if (requestPath.empty() || requestPath == "/")
      requestPath = kIndexFileName;

   // Validate and resolve path
   FilePath resolvedPath;
   Error error = validateAndResolvePath(clientRoot, requestPath, &resolvedPath);
   if (error)
   {
      pResponse->setStatusCode(http::status::Forbidden);
      return Success();
   }

   // Check if file exists
   if (!resolvedPath.exists())
   {
      pResponse->setStatusCode(http::status::NotFound);
      return Success();
   }

   // Read file BYTE-FOR-BYTE (no modifications)
   std::string content;
   error = core::readStringFromFile(resolvedPath, &content);
   if (error)
   {
      pResponse->setStatusCode(http::status::InternalServerError);
      return error;
   }

   // Set content type
   std::string extension = resolvedPath.getExtension();
   pResponse->setContentType(getContentType(extension));

   // Set caching headers
   if (boost::ends_with(requestPath, kIndexFileName) ||
       boost::ends_with(requestPath, ".js") ||
       boost::ends_with(requestPath, ".css"))
   {
      // Don't cache HTML, JS, or CSS files to avoid stale cache issues during development
      pResponse->setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
   }
   else if (requestPath.find(".") != std::string::npos)
   {
      // Cache other assets like images, fonts, etc.
      pResponse->setHeader("Cache-Control", "public, max-age=31536000");
   }

   pResponse->setStatusCode(http::status::Ok);
   pResponse->setBody(content);

   return Success();
}

// ============================================================================
// RPC Methods
// ============================================================================

Error chatVerifyInstalled(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   FilePath installation = locatePositAiInstallation();
   bool installed = !installation.isEmpty();
   pResponse->setResult(installed);
   return Success();
}

Error chatStartBackend(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   Error error = startChatBackend();

   json::Object result;
   result["success"] = !error;
   if (error)
      result["error"] = error.getMessage();

   pResponse->setResult(result);
   return Success();
}

Error chatGetBackendUrl(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   json::Object result;
   result["url"] = s_chatBackendUrl;
   result["port"] = s_chatBackendPort;
   result["ready"] = (s_chatBackendPid != -1 && !s_chatBackendUrl.empty());

   pResponse->setResult(result);
   return Success();
}

Error chatGetBackendStatus(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   json::Object result;

   FilePath installation = locatePositAiInstallation();
   if (installation.isEmpty())
   {
      result["status"] = "not_installed";
      result["error"] = "Posit AI not installed.";
   }
   else if (s_chatBackendPid == -1)
   {
      result["status"] = "stopped";
   }
   else if (s_chatBackendUrl.empty())
   {
      result["status"] = "starting";
   }
   else
   {
      result["status"] = "ready";
      result["url"] = s_chatBackendUrl;
   }

   pResponse->setResult(result);
   return Success();
}

// ============================================================================
// Module Lifecycle
// ============================================================================

void onShutdown(bool terminatedNormally)
{
   // Terminate backend process
   if (s_chatBackendPid != -1)
   {
      DLOG("Terminating chat backend process");
      Error error = core::system::terminateProcess(s_chatBackendPid);
      if (error)
         LOG_ERROR(error);
      s_chatBackendPid = -1;
   }

   // Clear port and URL
   s_chatBackendPort = -1;
   s_chatBackendUrl.clear();

   // Clear JSON-RPC state
   s_backendOutputBuffer.clear();
   s_pendingResponses.clear();
}

} // end anonymous namespace

// ============================================================================
// Module Initialization
// ============================================================================

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // Read default log level
   std::string chatLogLevelStr = core::system::getenv("CHAT_LOG_LEVEL");
   if (!chatLogLevelStr.empty())
      s_chatLogLevel = safe_convert::stringTo<int>(chatLogLevelStr, 0);

   RS_REGISTER_CALL_METHOD(rs_chatSetLogLevel);

   // Register JSON-RPC notification handlers
   registerNotificationHandler("logger/log", handleLoggerLog);

   // Register event handlers
   events().onBackgroundProcessing.connect(onBackgroundProcessing);
   events().onShutdown.connect(onShutdown);

   // Register RPC methods
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "chat_verify_installed", chatVerifyInstalled))
      (bind(registerRpcMethod, "chat_start_backend", chatStartBackend))
      (bind(registerRpcMethod, "chat_get_backend_url", chatGetBackendUrl))
      (bind(registerRpcMethod, "chat_get_backend_status", chatGetBackendStatus))
      (bind(registerUriHandler, "/ai-chat", handleAIChatRequest))
      (bind(sourceModuleRFile, "SessionChat.R"))
      ;

   Error error = initBlock.execute();
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   DLOG("SessionChat module initialized successfully, URI handler registered for /ai-chat");
   return Success();
}

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio
