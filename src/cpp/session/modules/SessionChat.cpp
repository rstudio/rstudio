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

#include <chrono>
#include <map>
#include <set>
#include <functional>

#include <boost/thread/mutex.hpp>

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

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionUrlPorts.hpp>
#include <session/SessionScopes.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "../SessionDirs.hpp"

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
// Suspension blocking
// ============================================================================
static bool s_chatBusy = false;


// ============================================================================
// Installation paths
// ============================================================================
const char* const kPositAiDirName = "ai";
const char* const kClientDirPath = "dist/client";
const char* const kServerScriptPath = "dist/server/main.js";
const char* const kIndexFileName = "index.html";

// ============================================================================
// Protocol Version (SUPPORTED_PROTOCOL_VERSION)
// ============================================================================
const char* const kProtocolVersion = "1.0";

// ============================================================================
// Logging
// ============================================================================
int s_chatLogLevel = 0;
std::string s_chatBackendMinLogLevel = "error"; // Default: show only error logs

int chatLogLevel()
{
   return s_chatLogLevel;
}

// ============================================================================
// Execution Tracking (for cancellation support)
// ============================================================================
// R is single-threaded, so only one execution can be active at a time,
// but we need to track cancelled IDs to handle pre-cancellation of queued requests
boost::mutex s_executionTrackingMutex;
std::string s_currentTrackingId;  // Empty string when not executing
std::set<std::string> s_cancelledTrackingIds;  // TrackingIds that have been cancelled

// ============================================================================
// Code Execution Context (for console output capture)
// ============================================================================
class ChatExecContext
{
public:
   ChatExecContext(bool captureOutput)
      : captureOutput_(captureOutput)
   {
   }

   ~ChatExecContext()
   {
      disconnect();
   }

   void connect()
   {
      connection_ = module_context::events().onConsoleOutput.connect(
          boost::bind(&ChatExecContext::onConsoleOutput, this, _1, _2));
   }

   void disconnect()
   {
      connection_.disconnect();
   }

   std::string getOutput() const { return outputBuffer_; }
   std::string getError() const { return errorBuffer_; }

private:
   void onConsoleOutput(module_context::ConsoleOutputType type,
                        const std::string& output)
   {
      if (!captureOutput_)
         return;

      if (type == module_context::ConsoleOutputNormal)
         outputBuffer_ += output;
      else
         errorBuffer_ += output;
   }

   bool captureOutput_;
   std::string outputBuffer_;
   std::string errorBuffer_;
   RSTUDIO_BOOST_CONNECTION connection_;
};

// Echo source code with prompts (like evaluate does)
void echoSourceCode(const std::string& code)
{
   std::vector<std::string> lines;
   boost::split(lines, code, boost::is_any_of("\n"));

   for (size_t i = 0; i < lines.size(); i++)
   {
      // Skip trailing empty line from trailing newline
      if (i == lines.size() - 1 && lines[i].empty())
         continue;

      std::string prompt = (i == 0) ? "> " : "+ ";
      module_context::consoleWriteOutput(prompt + lines[i] + "\n");
   }
}

// Map log level names to numeric priorities for filtering
// Returns priority (higher = more severe)
// Unknown levels return very high priority to ensure critical messages are never filtered
int getLogLevelPriority(const std::string& level)
{
   if (level == "trace") return 0;
   if (level == "debug") return 1;
   if (level == "info")  return 2;
   if (level == "warn")  return 3;
   if (level == "error") return 4;
   if (level == "fatal") return 5;
   return 999; // Unknown levels treated as highest priority (always show them)
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

// Regex for parsing Content-Length header (case-insensitive per HTTP spec)
// Matches only at start of line to avoid false matches in log output
boost::regex s_contentLengthRegex("(?:^|\r\n)Content-Length:\\s*(\\d+)", boost::regex::icase);

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

// ============================================================================
// JSON-RPC Request Handling
// ============================================================================

void sendJsonRpcResponse(core::system::ProcessOperations& ops,
                         const json::Value& id,
                         const json::Value& result)
{
   json::Object response;
   response["jsonrpc"] = "2.0";
   response["id"] = id;
   response["result"] = result;

   std::string body = response.write();
   std::string message = fmt::format(
       "Content-Length: {}\r\n\r\n{}",
       body.size(),
       body
   );

   if (chatLogLevel() >= 2)
   {
      DLOG("Sending JSON-RPC response: {}", body);
   }

   Error error = ops.writeToStdin(message, false);
   if (error)
   {
      ELOG("Failed to write JSON-RPC response to backend stdin: {}", error.getMessage());
   }
}

void sendJsonRpcError(core::system::ProcessOperations& ops,
                      const json::Value& id,
                      int code,
                      const std::string& message)
{
   json::Object errorObj;
   errorObj["code"] = code;
   errorObj["message"] = message;

   json::Object response;
   response["jsonrpc"] = "2.0";
   response["id"] = id;
   response["error"] = errorObj;

   std::string body = response.write();
   std::string frameMessage = fmt::format(
       "Content-Length: {}\r\n\r\n{}",
       body.size(),
       body
   );

   if (chatLogLevel() >= 2)
   {
      DLOG("Sending JSON-RPC error response: {}", body);
   }

   Error error = ops.writeToStdin(frameMessage, false);
   if (error)
   {
      ELOG("Failed to write JSON-RPC error response to backend stdin: {}", error.getMessage());
   }
}

void handleGetActiveSession(core::system::ProcessOperations& ops,
                            const json::Value& requestId)
{
   json::Object result;
   result["language"] = "R";
   result["version"] = module_context::rVersion();
   result["sessionId"] = module_context::activeSession().id();
   result["mode"] = "console";

   DLOG("Handling runtime/getActiveSession request");
   sendJsonRpcResponse(ops, requestId, result);
}

void handleGetDetailedContext(core::system::ProcessOperations& ops,
                               const json::Value& requestId)
{
   DLOG("Handling runtime/getDetailedContext request");

   // Get current time in human-readable format
   // Format: "Friday, October 3, 2025 at 10:32:34 PM CDT"
   std::time_t now = std::time(nullptr);
   std::tm* localTime = std::localtime(&now);
   char dateBuffer[128];
   std::strftime(dateBuffer, sizeof(dateBuffer), "%A, %B %d, %Y at %I:%M:%S %p %Z", localTime);
   std::string currentDate(dateBuffer);

   // Session object
   // TODO: Future work - detect and support Python sessions
   json::Object session;
   session["language"] = "R";
   session["version"] = module_context::rVersion();
   session["mode"] = "console";
   session["sessionId"] = module_context::activeSession().id();
   // TODO: Future work - return R workspace variables
   session["variables"] = json::Array();

   // PlatformInfo object
   json::Object platformInfo;
   // TODO: Future work - check if active plots exist
   platformInfo["hasPlots"] = false;
   platformInfo["platformVersion"] = RSTUDIO_VERSION;
   platformInfo["currentDate"] = currentDate;

   // Main result
   json::Object result;
   // TODO: Future work - return detailed file context
   result["openFiles"] = json::Array();
   result["session"] = session;
   result["platformInfo"] = platformInfo;

   sendJsonRpcResponse(ops, requestId, result);
}

void handleExecuteCode(core::system::ProcessOperations& ops,
                       const json::Value& requestId,
                       const json::Object& params)
{
   DLOG("Handling runtime/executeCode request");

   // Extract required parameters
   std::string language;
   std::string code;
   std::string trackingId;

   Error error = json::readObject(params,
      "language", language,
      "code", code,
      "trackingId", trackingId);

   if (error)
   {
      sendJsonRpcError(ops, requestId, -32602,
         "Invalid params: " + error.getMessage());
      return;
   }

   // Validate language (only R supported currently)
   if (language != "r")
   {
      sendJsonRpcError(ops, requestId, -32602,
         "Unsupported language: " + language + ". Only 'r' is currently supported.");
      return;
   }

   // Extract optional options object
   json::Object options;
   json::readObject(params, "options", options);  // Ignore error if missing

   bool captureOutput = true;
   bool capturePlot = false;
   int timeout = 30000;

   json::readObject(options, "captureOutput", captureOutput);
   json::readObject(options, "capturePlot", capturePlot);
   json::readObject(options, "timeout", timeout);

   // Check if this request was already cancelled (pre-cancellation of queued request)
   // and register tracking ID for cancellation support
   {
      boost::mutex::scoped_lock lock(s_executionTrackingMutex);

      // Check if already cancelled
      if (s_cancelledTrackingIds.count(trackingId) > 0)
      {
         s_cancelledTrackingIds.erase(trackingId);

         // Return a response indicating cancellation
         json::Object result;
         result["output"] = "";
         result["error"] = "Execution cancelled";
         result["plots"] = json::Array();
         result["executionTime"] = 0;

         sendJsonRpcResponse(ops, requestId, result);
         return;
      }

      // Register as current execution
      s_currentTrackingId = trackingId;
   }

   // Record start time
   auto startTime = std::chrono::steady_clock::now();

   // Create execution context to capture console output
   ChatExecContext execContext(captureOutput);
   execContext.connect();

   // Record the display list length before execution so we can detect if
   // plotting actually occurred (to avoid capturing stale plots)
   int displayListLengthBefore = 0;
   if (capturePlot)
   {
      r::sexp::Protect lenProtect;
      SEXP lenSEXP = R_NilValue;
      Error lenError = r::exec::RFunction(".rs.chat.getDisplayListLength").call(
         &lenSEXP, &lenProtect);
      if (!lenError && lenSEXP != R_NilValue && Rf_isInteger(lenSEXP) && Rf_length(lenSEXP) > 0)
         displayListLengthBefore = INTEGER(lenSEXP)[0];
   }

   // Echo source code with prompts (like evaluate does)
   echoSourceCode(code);

   // Evaluate the code using existing RExec infrastructure
   // evaluateString wraps in try(..., silent=TRUE) for error handling
   r::sexp::Protect protect;
   SEXP resultSEXP = R_NilValue;
   error = r::exec::evaluateString(code, R_GlobalEnv, &resultSEXP, &protect);

   // Handle parse/runtime errors from evaluateString.
   // evaluateString uses silent=TRUE, so errors don't automatically go to console.
   // We need to both:
   // 1. Fire the onConsoleOutput signal so our callback can capture it
   // 2. Write to console UI via consoleWriteError for user visibility
   if (error)
   {
      std::string errorMsg = error.getMessage();
      // Ensure consistent "Error: " prefix
      if (errorMsg.find("Error: ") != 0 && errorMsg.find("Error in ") != 0)
         errorMsg = "Error: " + errorMsg;

      std::string errorOutput = errorMsg + "\n";

      // Fire signal first so callback captures it
      module_context::events().onConsoleOutput(
         module_context::ConsoleOutputError, errorOutput);

      // Also write to console UI
      module_context::consoleWriteError(errorOutput);
   }

   // Handle plots if requested
   // NOTE: This only captures the final plot state. If code creates multiple plots
   // (e.g., plot(1); plot(2)), only the last one is captured. This is a known
   // limitation compared to the previous evaluate-based approach which used new_device.
   json::Array plotsArray;
   if (capturePlot)
   {
      // Use R helper for plot capture, passing the display list length from before
      // execution so it can detect if a NEW plot was created (vs. stale plot)
      r::sexp::Protect plotProtect;
      SEXP plotSEXP = R_NilValue;

      r::exec::RFunction captureFunc(".rs.chat.captureCurrentPlot");
      captureFunc.addParam("displayListLengthBefore", displayListLengthBefore);
      Error plotError = captureFunc.call(&plotSEXP, &plotProtect);

      if (!plotError && plotSEXP != R_NilValue && r::sexp::isList(plotSEXP))
      {
         // Extract plot data from R list
         SEXP plotNames = Rf_getAttrib(plotSEXP, R_NamesSymbol);
         if (plotNames != R_NilValue)
         {
            json::Object plotObj;
            int plotLen = Rf_length(plotSEXP);

            for (int j = 0; j < plotLen; j++)
            {
               if (j < Rf_length(plotNames))
               {
                  std::string name = CHAR(STRING_ELT(plotNames, j));
                  SEXP valueSEXP = VECTOR_ELT(plotSEXP, j);

                  if (name == "data" && Rf_isString(valueSEXP) && Rf_length(valueSEXP) > 0)
                  {
                     plotObj["data"] = std::string(CHAR(STRING_ELT(valueSEXP, 0)));
                  }
                  else if (name == "mimeType" && Rf_isString(valueSEXP) && Rf_length(valueSEXP) > 0)
                  {
                     plotObj["mimeType"] = std::string(CHAR(STRING_ELT(valueSEXP, 0)));
                  }
                  else if (name == "width" && Rf_isInteger(valueSEXP) && Rf_length(valueSEXP) > 0)
                  {
                     plotObj["width"] = INTEGER(valueSEXP)[0];
                  }
                  else if (name == "height" && Rf_isInteger(valueSEXP) && Rf_length(valueSEXP) > 0)
                  {
                     plotObj["height"] = INTEGER(valueSEXP)[0];
                  }
               }
            }

            if (plotObj.hasMember("data"))
               plotsArray.push_back(plotObj);
         }
      }
      // Don't fail on plot errors - just continue without plots
   }

   // Disconnect context
   execContext.disconnect();

   // Clear tracking ID - execution complete
   {
      boost::mutex::scoped_lock lock(s_executionTrackingMutex);
      s_currentTrackingId.clear();
      // Also clean up from cancelled set in case cancel arrived during execution
      s_cancelledTrackingIds.erase(trackingId);
   }

   // Calculate execution time in milliseconds
   auto endTime = std::chrono::steady_clock::now();
   int executionTime = static_cast<int>(
      std::chrono::duration_cast<std::chrono::milliseconds>(
         endTime - startTime).count());

   // Build response (same format as before)
   json::Object result;
   result["output"] = execContext.getOutput();
   result["error"] = execContext.getError();
   result["plots"] = plotsArray;
   result["executionTime"] = executionTime;

   // Send successful response
   sendJsonRpcResponse(ops, requestId, result);
}

void handleCancelExecution(const json::Object& params)
{
   DLOG("Handling runtime/cancelExecution notification");

   // Extract trackingId
   std::string trackingId;
   Error error = json::readObject(params, "trackingId", trackingId);

   if (error)
   {
      WLOG("runtime/cancelExecution: missing or invalid trackingId");
      return;
   }

   // Add to cancelled set and check if currently executing
   bool shouldInterrupt = false;
   {
      boost::mutex::scoped_lock lock(s_executionTrackingMutex);

      // Always add to cancelled set - handles pre-cancellation of queued requests
      s_cancelledTrackingIds.insert(trackingId);

      // Check if this is currently executing
      shouldInterrupt = !s_currentTrackingId.empty() &&
                        s_currentTrackingId == trackingId;
   }

   if (shouldInterrupt)
   {
      // Set the interrupt flag - this will cause R to throw an interrupt condition
      r::exec::setInterruptsPending(true);
      DLOG("Set interrupt pending for trackingId: {}", trackingId);
   }
   else
   {
      // Not currently executing - pre-cancellation recorded for when it starts
      DLOG("Recorded pre-cancellation for trackingId: {}", trackingId);
   }
}

void handleGetProtocolVersion(core::system::ProcessOperations& ops,
                               const json::Value& requestId,
                               const json::Object& params)
{
   DLOG("Handling protocol/getVersion request");

   // Extract client info for debugging (optional fields)
   std::string clientProtocolVersion;
   std::string clientVersion;

   json::readObject(params, "clientProtocolVersion", clientProtocolVersion);
   json::readObject(params, "clientVersion", clientVersion);

   DLOG("Client protocol version: {}, client version: {}",
        clientProtocolVersion.empty() ? "unknown" : clientProtocolVersion,
        clientVersion.empty() ? "unknown" : clientVersion);

   // Build response
   json::Object result;
   result["protocolVersion"] = kProtocolVersion;
   result["rstudioVersion"] = std::string(RSTUDIO_VERSION);

   sendJsonRpcResponse(ops, requestId, result);
}

void handleRequest(core::system::ProcessOperations& ops,
                   const std::string& method,
                   const json::Value& requestId,
                   const json::Object& params)
{
   if (method == "runtime/getActiveSession")
   {
      handleGetActiveSession(ops, requestId);
   }
   else if (method == "runtime/getDetailedContext")
   {
      handleGetDetailedContext(ops, requestId);
   }
   else if (method == "runtime/executeCode")
   {
      handleExecuteCode(ops, requestId, params);
   }
   else if (method == "protocol/getVersion")
   {
      handleGetProtocolVersion(ops, requestId, params);
   }
   else
   {
      // Unknown method - send JSON-RPC error response
      WLOG("Unknown JSON-RPC request method: {}", method);
      sendJsonRpcError(ops, requestId, -32601, "Method not found");
   }
}

// ============================================================================
// JSON-RPC Message Processing
// ============================================================================

void processBackendMessage(core::system::ProcessOperations& ops,
                           const json::Value& message)
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
      // This is a notification or request from backend
      json::Object params;
      json::readObject(messageObj, "params", params);

      // Check for 'id' field to distinguish request from notification
      // Per JSON-RPC 2.0 spec:
      // - Request: has 'id' field (value can be string, number, or null)
      // - Notification: 'id' field is absent entirely
      // The VALUE of id doesn't matter - only its presence/absence
      auto it = messageObj.find("id");
      if (it != messageObj.end())
      {
         // 'id' field EXISTS - this is a REQUEST (even if value is null)
         json::Value requestId = messageObj["id"];
         handleRequest(ops, method, requestId, params);
      }
      else
      {
         // 'id' field ABSENT - this is a NOTIFICATION
         handleNotification(method, params);
      }
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

   // Filter backend logs based on minimum level setting
   if (getLogLevelPriority(level) < getLogLevelPriority(s_chatBackendMinLogLevel))
   {
      // This log level is below the threshold, skip it
      return;
   }

   // Map backend log levels to RStudio logging macros
   // Use "ai" prefix and include level to distinguish backend logs
   std::string prefixedMessage = fmt::format("[ai] [{}] {}", level, message);

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
   else if (level == "fatal")
   {
      ELOG("{}", prefixedMessage);
   }
   else
   {
      // Unknown levels are treated as high priority (see getLogLevelPriority),
      // so log them as errors to ensure visibility
      ELOG("[ai] [{}] {}", level, message);
   }
}

void handleSetBusyStatus(const json::Object& params)
{
   bool busy = false;
   Error error = json::readObject(params, "busy", busy);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // Update our tracking flag
   s_chatBusy = busy;

   // Log for debugging (using chat-specific logging infrastructure)
   DLOG("Chat backend busy status: {}", busy ? "busy" : "idle");
}

// Check if a logger/log notification should be shown in raw JSON-RPC format
// Returns true if the message should be logged in raw form
// Verbosity levels:
//   CHAT_LOG_LEVEL=2: Show all JSON except logger/log (formatted version is enough)
//   CHAT_LOG_LEVEL=3+: Show all JSON including logger/log (for debugging logging itself)
bool shouldLogBackendMessage(const std::string& messageBody)
{
   // Quick parse to check if this is a logger/log notification
   json::Value message;
   if (message.parse(messageBody))
      return true; // Parse error, show it

   if (!message.isObject())
      return true; // Not an object, show it

   json::Object obj = message.getObject();

   // Check if it's a logger/log notification
   std::string method;
   if (json::readObject(obj, "method", method) || method != "logger/log")
      return true; // Not a logger/log notification, always show it at level 2+

   // It's a logger/log notification
   // At level 2: hide raw JSON (formatted version will be shown by handleLoggerLog)
   // At level 3+: show raw JSON (for debugging the logging mechanism itself)
   if (chatLogLevel() >= 3)
   {
      // Level 3+: apply backend level filter and show if it passes
      json::Object params;
      if (json::readObject(obj, "params", params))
         return true; // No params, show it

      std::string level;
      if (json::readObject(params, "level", level))
         return true; // No level field, show it

      return getLogLevelPriority(level) >= getLogLevelPriority(s_chatBackendMinLogLevel);
   }

   // Level 2: hide logger/log raw JSON (user will see formatted version)
   return false;
}

void onBackgroundProcessing(bool isIdle)
{
   // Messages are now processed immediately in onBackendStdout while we have
   // a valid ProcessOperations reference. No deferred processing needed.

   // Future: Add timeout handling for request/response pattern if needed
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
// Update Management
// ============================================================================

// Structure to hold update check state
struct UpdateState
{
   bool updateAvailable;
   std::string currentVersion;
   std::string newVersion;
   std::string downloadUrl;
   std::string errorMessage;

   // Installation status tracking
   enum class Status
   {
      Idle,
      Downloading,
      Installing,
      Complete,
      Error
   };
   Status installStatus;
   std::string installMessage;

   UpdateState()
      : updateAvailable(false),
        installStatus(Status::Idle)
   {
   }
};

// Global update state
UpdateState s_updateState;
boost::mutex s_updateStateMutex;

// Validate that a URL uses HTTPS protocol
bool isHttpsUrl(const std::string& url)
{
   return boost::starts_with(url, "https://");
}

// Download manifest from URL specified in pai_download_uri preference
Error downloadManifest(json::Object* pManifest)
{
   if (!pManifest)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   // Get download URI from preference
   std::string downloadUri = prefs::userPrefs().paiDownloadUri();

   // Trim whitespace from the URL
   boost::algorithm::trim(downloadUri);

   if (downloadUri.empty())
   {
      return systemError(boost::system::errc::operation_not_permitted,
                        "pai_download_uri preference not set",
                        ERROR_LOCATION);
   }

   // Validate HTTPS
   if (!isHttpsUrl(downloadUri))
   {
      WLOG("Manifest download URL must use HTTPS, rejecting: {}", downloadUri);
      return systemError(boost::system::errc::protocol_error,
                        "Manifest URL must use HTTPS protocol",
                        ERROR_LOCATION);
   }

   DLOG("Downloading manifest from: {}", downloadUri);

   // Create temp file for download
   FilePath tempFile = module_context::tempFile("manifest", "json");

   // Use R's download.file() function with timeout protection
   r::exec::RFunction downloadFunc("download.file");
   downloadFunc.addParam("url", downloadUri);
   downloadFunc.addParam("destfile", tempFile.getAbsolutePath());
   downloadFunc.addParam("quiet", true);
   downloadFunc.addParam("method", "libcurl");  // Use libcurl for HTTPS support
   downloadFunc.addParam("timeout", 30);  // 30 second timeout

   Error error = downloadFunc.call();
   if (error)
   {
      WLOG("Failed to download manifest: {}", error.getMessage());
      return error;
   }

   // Read and parse JSON
   std::string manifestContent;
   error = core::readStringFromFile(tempFile, &manifestContent);
   if (error)
   {
      WLOG("Failed to read manifest file: {}", error.getMessage());
      return error;
   }

   // Parse JSON
   json::Value manifestValue;
   if (manifestValue.parse(manifestContent))
   {
      WLOG("Failed to parse manifest JSON");
      return systemError(boost::system::errc::protocol_error,
                        "Invalid JSON in manifest",
                        ERROR_LOCATION);
   }

   if (!manifestValue.isObject())
   {
      return systemError(boost::system::errc::protocol_error,
                        "Manifest must be a JSON object",
                        ERROR_LOCATION);
   }

   *pManifest = manifestValue.getObject();
   DLOG("Successfully downloaded and parsed manifest");

   return Success();
}

// Parse manifest to get package info for current protocol version
Error getPackageInfoFromManifest(
    const json::Object& manifest,
    const std::string& protocolVersion,
    std::string* pPackageVersion,
    std::string* pDownloadUrl)
{
   if (!pPackageVersion || !pDownloadUrl)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   // Get "versions" object
   json::Object versions;
   Error error = json::readObject(manifest, "versions", versions);
   if (error)
   {
      WLOG("Manifest missing 'versions' field");
      return error;
   }

   // Look up our protocol version
   auto it = versions.find(protocolVersion);
   if (it == versions.end())
   {
      WLOG("Manifest does not contain entry for protocol version: {}", protocolVersion);
      return systemError(boost::system::errc::protocol_not_supported,
                        "Protocol version not found in manifest",
                        ERROR_LOCATION);
   }

   // Access the value for this protocol version
   json::Object::Member member = *it;
   json::Value versionValue = member.getValue();

   if (!versionValue.isObject())
   {
      return systemError(boost::system::errc::protocol_error,
                        "Protocol version entry must be an object",
                        ERROR_LOCATION);
   }

   json::Object versionInfo = versionValue.getObject();

   // Extract version and url
   std::string version;
   std::string url;
   error = json::readObject(versionInfo, "version", version);
   if (error)
   {
      WLOG("Version info missing 'version' field");
      return error;
   }

   error = json::readObject(versionInfo, "url", url);
   if (error)
   {
      WLOG("Version info missing 'url' field");
      return error;
   }

   // Validate download URL is HTTPS
   if (!isHttpsUrl(url))
   {
      WLOG("Package download URL must use HTTPS, rejecting: {}", url);
      return systemError(boost::system::errc::protocol_error,
                        "Package download URL must use HTTPS protocol",
                        ERROR_LOCATION);
   }

   *pPackageVersion = version;
   *pDownloadUrl = url;

   DLOG("Found package info: version={}, url={}", version, url);

   return Success();
}

// Read installed version from package.json in ai directory
std::string getInstalledVersion()
{
   FilePath positAiPath = locatePositAiInstallation();
   if (positAiPath.isEmpty())
      return "";

   FilePath packageJson = positAiPath.completeChildPath("package.json");
   if (!packageJson.exists())
   {
      WLOG("package.json not found in AI installation");
      return "";
   }

   // Read and parse package.json
   std::string content;
   Error error = core::readStringFromFile(packageJson, &content);
   if (error)
   {
      WLOG("Failed to read package.json: {}", error.getMessage());
      return "";
   }

   json::Value packageValue;
   if (packageValue.parse(content))
   {
      WLOG("Failed to parse package.json");
      return "";
   }

   if (!packageValue.isObject())
   {
      WLOG("package.json is not a JSON object");
      return "";
   }

   json::Object packageObj = packageValue.getObject();
   std::string version;
   error = json::readObject(packageObj, "version", version);
   if (error)
   {
      WLOG("package.json missing 'version' field");
      return "";
   }

   DLOG("Installed version: {}", version);
   return version;
}

// Parse semantic version string into components
struct SemanticVersion
{
   int major;
   int minor;
   int patch;

   SemanticVersion() : major(0), minor(0), patch(0) {}

   bool parse(const std::string& versionStr)
   {
      // Match format: major.minor.patch (with optional "v" prefix)
      std::string cleanVersion = versionStr;
      if (!cleanVersion.empty() && cleanVersion[0] == 'v')
         cleanVersion = cleanVersion.substr(1);

      // Split on dots
      std::vector<std::string> parts;
      boost::split(parts, cleanVersion, boost::is_any_of("."));

      if (parts.size() < 1)
         return false;

      // Parse major (required)
      major = safe_convert::stringTo<int>(parts[0], -1);
      if (major < 0)
         return false;

      // Parse minor (optional, default to 0)
      if (parts.size() >= 2)
      {
         minor = safe_convert::stringTo<int>(parts[1], -1);
         if (minor < 0)
            return false;
      }

      // Parse patch (optional, default to 0)
      if (parts.size() >= 3)
      {
         patch = safe_convert::stringTo<int>(parts[2], -1);
         if (patch < 0)
            return false;
      }

      return true;
   }

   bool operator>(const SemanticVersion& other) const
   {
      if (major != other.major)
         return major > other.major;
      if (minor != other.minor)
         return minor > other.minor;
      return patch > other.patch;
   }
};

// Compare semantic versions (returns true if available > installed)
bool isNewerVersionAvailable(
    const std::string& installedVersion,
    const std::string& availableVersion)
{
   SemanticVersion installed, available;

   if (!installed.parse(installedVersion))
   {
      WLOG("Failed to parse installed version: {}", installedVersion);
      return false;
   }

   if (!available.parse(availableVersion))
   {
      WLOG("Failed to parse available version: {}", availableVersion);
      return false;
   }

   return available > installed;
}

// Download package to temp directory
Error downloadPackage(const std::string& url, const FilePath& destPath)
{
   if (!isHttpsUrl(url))
   {
      WLOG("Package download URL must use HTTPS, rejecting: {}", url);
      return systemError(boost::system::errc::protocol_error,
                        "Package download URL must use HTTPS protocol",
                        ERROR_LOCATION);
   }

   DLOG("Downloading package from: {} to: {}", url, destPath.getAbsolutePath());

   // Use R's download.file() function with timeout protection
   r::exec::RFunction downloadFunc("download.file");
   downloadFunc.addParam("url", url);
   downloadFunc.addParam("destfile", destPath.getAbsolutePath());
   downloadFunc.addParam("quiet", false);  // Show progress for user feedback
   downloadFunc.addParam("method", "libcurl");
   downloadFunc.addParam("mode", "wb");  // Binary mode for zip files
   downloadFunc.addParam("timeout", 60);  // 60 second timeout for larger package

   Error error = downloadFunc.call();
   if (error)
   {
      WLOG("Failed to download package: {}", error.getMessage());
      return error;
   }

   // Verify file exists and has content
   if (!destPath.exists() || destPath.getSize() == 0)
   {
      return systemError(boost::system::errc::io_error,
                        "Downloaded file is empty or missing",
                        ERROR_LOCATION);
   }

   DLOG("Successfully downloaded package ({} bytes)", destPath.getSize());
   return Success();
}

// Install package (backup, extract, cleanup)
Error installPackage(const FilePath& packagePath)
{
   FilePath userDataDir = xdg::userDataDir();
   FilePath aiDir = userDataDir.completePath(kPositAiDirName);
   FilePath aiPrevDir = userDataDir.completePath("ai.prev");

   DLOG("Installing package from: {}", packagePath.getAbsolutePath());

   // Step 1: Remove old backup if it exists
   if (aiPrevDir.exists())
   {
      DLOG("Removing old backup directory: {}", aiPrevDir.getAbsolutePath());
      Error error = aiPrevDir.removeIfExists();
      if (error)
      {
         WLOG("Failed to remove old backup: {}", error.getMessage());
         return error;
      }
   }

   // Step 2: Backup current installation if it exists
   if (aiDir.exists())
   {
      DLOG("Backing up current installation to: {}", aiPrevDir.getAbsolutePath());
      Error error = aiDir.move(aiPrevDir);
      if (error)
      {
         WLOG("Failed to backup current installation: {}", error.getMessage());
         return error;
      }
   }

   // Step 3: Create new ai directory
   Error error = aiDir.ensureDirectory();
   if (error)
   {
      WLOG("Failed to create ai directory: {}", error.getMessage());
      // Try to restore backup
      if (aiPrevDir.exists())
      {
         aiPrevDir.move(aiDir);
      }
      return error;
   }

   // Step 4: Extract package using R's unzip()
   DLOG("Extracting package to: {}", aiDir.getAbsolutePath());
   r::exec::RFunction unzipFunc("unzip");
   unzipFunc.addParam("zipfile", packagePath.getAbsolutePath());
   unzipFunc.addParam("exdir", aiDir.getAbsolutePath());

   error = unzipFunc.call();
   if (error)
   {
      WLOG("Failed to extract package: {}", error.getMessage());
      // Clean up partial extraction
      Error cleanupError = aiDir.removeIfExists();
      if (cleanupError)
      {
         ELOG("Failed to clean up failed extraction directory: {}", cleanupError.getMessage());
      }
      // Restore backup
      if (aiPrevDir.exists())
      {
         Error restoreError = aiPrevDir.move(aiDir);
         if (restoreError)
         {
            ELOG("Failed to restore backup after extraction failure: {}", restoreError.getMessage());
         }
      }
      return error;
   }

   // Step 5: Verify installation
   if (!verifyPositAiInstallation(aiDir))
   {
      WLOG("Extracted package failed verification");
      // Clean up invalid extraction
      Error cleanupError = aiDir.removeIfExists();
      if (cleanupError)
      {
         ELOG("Failed to clean up invalid extraction directory: {}", cleanupError.getMessage());
      }
      // Restore backup
      if (aiPrevDir.exists())
      {
         Error restoreError = aiPrevDir.move(aiDir);
         if (restoreError)
         {
            ELOG("Failed to restore backup after verification failure: {}", restoreError.getMessage());
         }
      }
      return systemError(boost::system::errc::io_error,
                        "Extracted package is incomplete or invalid",
                        ERROR_LOCATION);
   }

   // Step 6: Success - remove backup
   if (aiPrevDir.exists())
   {
      DLOG("Installation successful, removing backup");
      Error backupCleanup = aiPrevDir.removeIfExists();
      if (backupCleanup)
      {
         WLOG("Failed to remove backup directory after successful install: {}", backupCleanup.getMessage());
         // Don't fail the installation for this - backup will be cleaned up next time
      }
   }

   DLOG("Package installation complete");
   return Success();
}

// Called during session initialization to check for updates
Error checkForUpdatesOnStartup()
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   // Eligibility check: require both preferences
   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      DLOG("Update check skipped: pai preferences not configured");
      return Success();
   }

   DLOG("Checking for updates on startup");

   // Get installed version
   std::string installedVersion = getInstalledVersion();
   if (installedVersion.empty())
   {
      DLOG("No installation found, checking for initial install");
      installedVersion = "0.0.0";
   }

   s_updateState.currentVersion = installedVersion;

   // Download manifest (silent failure)
   json::Object manifest;
   Error error = downloadManifest(&manifest);
   if (error)
   {
      WLOG("Failed to download manifest: {}", error.getMessage());
      // Silent failure - don't block feature usage
      return Success();
   }

   // Get package info for our protocol version
   std::string packageVersion;
   std::string downloadUrl;
   error = getPackageInfoFromManifest(manifest, kProtocolVersion, &packageVersion, &downloadUrl);
   if (error)
   {
      WLOG("Failed to parse manifest: {}", error.getMessage());
      // Silent failure
      return Success();
   }

   // Compare versions
   if (isNewerVersionAvailable(installedVersion, packageVersion))
   {
      DLOG("Update available: {} -> {}", installedVersion, packageVersion);
      s_updateState.updateAvailable = true;
      s_updateState.newVersion = packageVersion;
      s_updateState.downloadUrl = downloadUrl;
   }
   else
   {
      DLOG("No update available (installed: {}, available: {})", installedVersion, packageVersion);
      s_updateState.updateAvailable = false;
   }

   return Success();
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

      // Determine WebSocket scheme and extract URL without scheme
      // This preserves the full path including any session prefix (e.g., /s/abc123/)
      std::string wsScheme;
      std::string urlWithoutScheme;

      if (baseUrl.find("https://") == 0)
      {
         wsScheme = "wss://";
         urlWithoutScheme = baseUrl.substr(8); // Remove "https://"
      }
      else if (baseUrl.find("http://") == 0)
      {
         wsScheme = "ws://";
         urlWithoutScheme = baseUrl.substr(7); // Remove "http://"
      }
      else
      {
         // Fallback - shouldn't happen
         WLOG("Unexpected base URL format: {}", baseUrl);
         wsScheme = "ws://";
         urlWithoutScheme = baseUrl;
      }

      // Remove trailing slash from URL if present (portmappedPath will add one)
      if (!urlWithoutScheme.empty() && urlWithoutScheme.back() == '/')
         urlWithoutScheme = urlWithoutScheme.substr(0, urlWithoutScheme.length() - 1);

      // Build complete WebSocket URL preserving any session path from baseUrl
      // Example: wss://hostname:8787/s/abc123/p/58fab3e4/ai-chat
      // The proxy will route this to http://127.0.0.1:{port}/ai-chat/ws
      std::string wsUrl = wsScheme + urlWithoutScheme + portmappedPath + "/ai-chat";
      DLOG("Final WebSocket URL: {}", wsUrl);

      return wsUrl;
   }
#endif

   // Desktop mode: include /ai-chat base path for AIServer routing
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
   // NOTE: We process messages immediately here (not deferred) because:
   // 1. callbacksRequireMainThread=true means we're already on the main thread
   // 2. We need the valid 'ops' reference to send responses to requests
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

      // Verbose logging (filtered by backend log level for logger/log notifications)
      if (chatLogLevel() >= 2 && shouldLogBackendMessage(messageBody))
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
         // Process message immediately while ops is valid
         processBackendMessage(ops, messageValue);
      }

      // Remove the processed message from buffer
      s_backendOutputBuffer = s_backendOutputBuffer.substr(bodyEnd);
   }
}

void onBackendStderr(core::system::ProcessOperations& ops, const std::string& output)
{
   WLOG("Chat backend stderr: {}", output);
}

void onBackendExit(int exitCode)
{
   WLOG("Chat backend exited with code: {}", exitCode);

   // Clear chat backend busy state to prevent stuck suspension blocking
   if (s_chatBusy)
   {
      s_chatBusy = false;
      DLOG("Cleared chat backend busy state on process exit");
   }

   // Clear state
   s_chatBackendPid = -1;
   s_chatBackendPort = -1;
   s_chatBackendUrl.clear();
   s_backendOutputBuffer.clear();
   s_chatBackendRestartCount = kMaxRestartAttempts;

   // Notify client of unexpected backend exit
   json::Object exitData;
   exitData["exit_code"] = exitCode;
   exitData["crashed"] = true;
   module_context::enqueClientEvent(ClientEvent(
      client_events::kChatBackendExit,
      exitData
   ));
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

   // Add workspace path argument
   FilePath workspacePath = dirs::getInitialWorkingDirectory();
   args.push_back("--workspace");
   args.push_back(workspacePath.getAbsolutePath());

   // Create storage base path: {XDG_DATA_HOME}/ai-data/
   FilePath storagePath = xdg::userDataDir().completePath("ai-data");
   error = storagePath.ensureDirectory();
   if (error)
      return(error);

   args.push_back("--storage");
   args.push_back(storagePath.getAbsolutePath());

   // Generate a persistent ID for this workspace directory
   std::string workspacePathStr = workspacePath.getAbsolutePath();
   std::string workspaceId = session::projectToProjectId(
       module_context::userScratchPath(),
       FilePath(),  // No shared storage - use per-user workspace IDs
       workspacePathStr
   ).id();

   args.push_back("--workspace-id");
   args.push_back(workspaceId);

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
      // Use multiple headers to ensure cache is disabled across all browsers and proxies
      pResponse->setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
      pResponse->setHeader("Pragma", "no-cache");  // HTTP/1.0 compatibility
      pResponse->setHeader("Expires", "0");        // Proxy cache control
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

Error chatCheckForUpdates(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   // Guard: Require both preferences to be set
   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      // Return empty/negative response - don't reveal feature exists
      json::Object result;
      result["updateAvailable"] = false;
      pResponse->setResult(result);
      return Success();
   }

   // Return cached startup check result
   json::Object result;
   result["updateAvailable"] = s_updateState.updateAvailable;
   result["currentVersion"] = s_updateState.currentVersion;
   result["newVersion"] = s_updateState.newVersion;
   result["downloadUrl"] = s_updateState.downloadUrl;
   result["isInitialInstall"] = (s_updateState.currentVersion == "0.0.0");

   pResponse->setResult(result);
   return Success();
}

Error chatInstallUpdate(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   // Guard: Require both preferences to be set
   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      return systemError(boost::system::errc::operation_not_permitted,
                        "Feature not enabled",
                        ERROR_LOCATION);
   }

   // Check if update is available
   if (!s_updateState.updateAvailable)
   {
      return systemError(boost::system::errc::operation_not_permitted,
                        "No update available",
                        ERROR_LOCATION);
   }

   // Check if already in progress
   if (s_updateState.installStatus != UpdateState::Status::Idle &&
       s_updateState.installStatus != UpdateState::Status::Complete &&
       s_updateState.installStatus != UpdateState::Status::Error)
   {
      return systemError(boost::system::errc::operation_in_progress,
                        "Update already in progress",
                        ERROR_LOCATION);
   }

   // Start update process (async would be better, but doing sync for simplicity)
   s_updateState.installStatus = UpdateState::Status::Downloading;
   s_updateState.installMessage = "Downloading update...";

   // Unlock mutex during download/install to allow status queries
   lock.unlock();

   // Stop backend if running
   if (s_chatBackendPid != -1)
   {
      DLOG("Stopping backend for update");
      Error error = core::system::terminateProcess(s_chatBackendPid);
      if (error)
      {
         WLOG("Failed to stop backend: {}", error.getMessage());
      }
      s_chatBackendPid = -1;
      s_chatBackendPort = -1;
      s_chatBackendUrl.clear();
   }

   // Download package
   FilePath tempPackage = module_context::tempFile("pai-update", "zip");
   Error error = downloadPackage(s_updateState.downloadUrl, tempPackage);

   if (error)
   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);
      s_updateState.installStatus = UpdateState::Status::Error;
      s_updateState.installMessage = "Download failed: " + error.getMessage();

      // Clean up temp file
      Error cleanupError = tempPackage.removeIfExists();
      if (cleanupError)
         WLOG("Failed to remove temp package after download failure: {}", cleanupError.getMessage());

      return error;
   }

   // Install package
   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);
      s_updateState.installStatus = UpdateState::Status::Installing;
      s_updateState.installMessage = "Installing update...";
   }

   error = installPackage(tempPackage);

   // Always clean up temp file (do this before error handling)
   Error cleanupError = tempPackage.removeIfExists();
   if (cleanupError)
   {
      WLOG("Failed to remove temp package: {}", cleanupError.getMessage());
   }

   if (error)
   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);
      s_updateState.installStatus = UpdateState::Status::Error;
      s_updateState.installMessage = "Installation failed: " + error.getMessage();

      // Note: installPackage() already handles backup restoration internally,
      // so we don't need to call restoreFromBackup() here again.
      // Just verify backup was restored and clean up if needed.
      FilePath userDataDir = xdg::userDataDir();
      FilePath aiPrevDir = userDataDir.completePath("ai.prev");

      // Defensive cleanup: remove orphaned backup if it exists
      if (aiPrevDir.exists())
      {
         Error prevCleanup = aiPrevDir.removeIfExists();
         if (prevCleanup)
            WLOG("Failed to clean up backup directory after failed install: {}", prevCleanup.getMessage());
      }

      return error;
   }

   // Success - ensure backup is cleaned up
   {
      boost::mutex::scoped_lock lock2(s_updateStateMutex);

      // Defensive cleanup: ensure ai.prev is removed
      FilePath userDataDir = xdg::userDataDir();
      FilePath aiPrevDir = userDataDir.completePath("ai.prev");
      if (aiPrevDir.exists())
      {
         WLOG("Backup directory still exists after successful install, cleaning up");
         Error prevCleanup = aiPrevDir.removeIfExists();
         if (prevCleanup)
            WLOG("Failed to clean up backup directory: {}", prevCleanup.getMessage());
      }

      s_updateState.installStatus = UpdateState::Status::Complete;
      s_updateState.installMessage = "Update complete";
      s_updateState.updateAvailable = false;  // Clear update flag
      s_updateState.currentVersion = s_updateState.newVersion;
   }

   pResponse->setResult(json::Value());
   return Success();
}

Error chatGetUpdateStatus(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   boost::mutex::scoped_lock lock(s_updateStateMutex);

   // Guard: Require both preferences to be set
   if (!prefs::userPrefs().pai() || prefs::userPrefs().paiDownloadUri().empty())
   {
      // Return idle status - don't reveal feature exists
      json::Object result;
      result["status"] = "idle";
      pResponse->setResult(result);
      return Success();
   }

   json::Object result;

   // Map status enum to string
   switch (s_updateState.installStatus)
   {
      case UpdateState::Status::Idle:
         result["status"] = "idle";
         break;
      case UpdateState::Status::Downloading:
         result["status"] = "downloading";
         break;
      case UpdateState::Status::Installing:
         result["status"] = "installing";
         break;
      case UpdateState::Status::Complete:
         result["status"] = "complete";
         break;
      case UpdateState::Status::Error:
         result["status"] = "error";
         break;
   }

   result["message"] = s_updateState.installMessage;
   if (s_updateState.installStatus == UpdateState::Status::Error)
   {
      result["error"] = s_updateState.errorMessage;
   }

   pResponse->setResult(result);
   return Success();
}

// ============================================================================
// Module Lifecycle
// ============================================================================

void onSuspend(const r::session::RSuspendOptions& options, Settings* pSettings)
{
   DLOG("Session suspension starting - terminating chat backend");

   // Persist whether backend was running before suspension
   bool wasRunning = (s_chatBackendPid != -1);
   pSettings->set("chat_suspended", wasRunning);

   // Terminate backend if running
   if (wasRunning)
   {
      Error error = core::system::terminateProcess(s_chatBackendPid);
      if (error)
         LOG_ERROR(error);

      // Clear state (rsession is exiting anyway)
      s_chatBackendPid = -1;
      s_chatBackendPort = -1;
      s_chatBackendUrl.clear();
   }

   // Clear busy state and JSON-RPC buffer
   s_chatBusy = false;
   s_backendOutputBuffer.clear();
}

void onResume(const Settings& settings)
{
   DLOG("Session resuming");

   // Check if we were suspended with chat backend running
   bool wasSuspended = settings.getBool("chat_suspended", false);

   if (wasSuspended)
   {
      DLOG("Restarting chat backend after resume");

      Error error = startChatBackend();
      if (error)
         LOG_ERROR(error);
   }
}

void onShutdown(bool terminatedNormally)
{
   // Clear busy state
   if (s_chatBusy)
   {
      s_chatBusy = false;
   }

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
}

} // end anonymous namespace

// ============================================================================
// Public API
// ============================================================================

bool isSuspendable()
{
   // Session can suspend if chat backend is NOT busy
   return !s_chatBusy;
}

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

   // Read backend minimum log level filter
   std::string backendMinLevel = core::system::getenv("CHAT_BACKEND_MIN_LEVEL");
   if (!backendMinLevel.empty())
   {
      // Convert to lowercase for case-insensitive matching
      boost::algorithm::to_lower(backendMinLevel);

      // Validate it's a known level, otherwise keep default
      if (backendMinLevel == "trace" || backendMinLevel == "debug" ||
          backendMinLevel == "info" || backendMinLevel == "warn" ||
          backendMinLevel == "error" || backendMinLevel == "fatal")
      {
         s_chatBackendMinLogLevel = backendMinLevel;
      }
      else
      {
         WLOG("Invalid CHAT_BACKEND_MIN_LEVEL value '{}', using default 'error'", backendMinLevel);
      }
   }

   RS_REGISTER_CALL_METHOD(rs_chatSetLogLevel);

   // Register JSON-RPC notification handlers
   registerNotificationHandler("logger/log", handleLoggerLog);
   registerNotificationHandler("chat/setBusyStatus", handleSetBusyStatus);
   registerNotificationHandler("runtime/cancelExecution", handleCancelExecution);

   // Register event handlers
   events().onBackgroundProcessing.connect(onBackgroundProcessing);
   events().onShutdown.connect(onShutdown);

   // Register suspend/resume handlers
   addSuspendHandler(SuspendHandler(
      boost::bind(onSuspend, _1, _2),
      onResume
   ));

   // Register RPC methods
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "chat_verify_installed", chatVerifyInstalled))
      (bind(registerRpcMethod, "chat_start_backend", chatStartBackend))
      (bind(registerRpcMethod, "chat_get_backend_url", chatGetBackendUrl))
      (bind(registerRpcMethod, "chat_get_backend_status", chatGetBackendStatus))
      (bind(registerRpcMethod, "chat_check_for_updates", chatCheckForUpdates))
      (bind(registerRpcMethod, "chat_install_update", chatInstallUpdate))
      (bind(registerRpcMethod, "chat_get_update_status", chatGetUpdateStatus))
      (bind(registerUriHandler, "/ai-chat", handleAIChatRequest))
      (bind(sourceModuleRFile, "SessionChat.R"))
      ;

   Error error = initBlock.execute();
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // Check for updates on startup (async, won't block initialization)
   error = checkForUpdatesOnStartup();
   if (error)
   {
      // Log but don't fail initialization
      WLOG("Update check failed: {}", error.getMessage());
   }

   DLOG("SessionChat module initialized successfully, URI handler registered for /ai-chat");
   return Success();
}

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio
