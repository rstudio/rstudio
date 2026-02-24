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
#include "SessionAssistant.hpp"
#include "SessionNodeTools.hpp"

#include "chat/ChatConstants.hpp"
#include "chat/ChatTypes.hpp"
#include "chat/ChatLogging.hpp"
#include "chat/ChatInstallation.hpp"
#include "chat/ChatStaticFiles.hpp"

#include <algorithm>
#include <chrono>
#include <map>
#include <queue>
#include <set>
#include <vector>
#include <functional>

#include <boost/thread/mutex.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/asio.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/regex.hpp>

#include <core/Exec.hpp>
#include <core/FileInfo.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/URL.hpp>
#include <core/http/Util.hpp>
#include <core/LogOptions.hpp>
#include <core/system/Process.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <core/Version.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>
#include <r/RUtil.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/REventLoop.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionUrlPorts.hpp>
#include <session/SessionScopes.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>
#include <session/projects/SessionProjects.hpp>

#include <r/session/RBusy.hpp>

#include <boost/date_time/posix_time/posix_time.hpp>

#include "../SessionConsoleInput.hpp"
#include "../SessionDirs.hpp"
#include "environment/EnvironmentUtils.hpp"

#include "session-config.h"

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
int s_chatBackendRestartCount = 0;
boost::shared_ptr<core::system::ProcessOperations> s_chatBackendOps;

// Track expected shutdown to distinguish from crashes
bool s_expectedShutdown = false;

// Track whether session is closing (vs suspending/restarting)
static bool s_sessionClosing = false;

// ============================================================================
// Suspension blocking
// ============================================================================
static bool s_chatBusy = false;

// ============================================================================
// Resume tracking for chat UI
// ============================================================================
// Tracks whether the chat UI should receive a "resume" hint when it loads.
// Set to the resume-conversation flag on session resume, and unconditionally
// set to true once the UI has loaded (via chatNotifyUILoaded RPC). Resets
// naturally when the rsession process restarts (e.g., project switch).
static bool s_resumeChat = false;

// ============================================================================
// Focused document tracking for insertAtCursor
// ============================================================================
static std::string s_focusedDocumentId;
static json::Array s_focusedDocumentSelections;

// ============================================================================
// Posit Assistant version tracking for About dialog
// ============================================================================
static std::string s_positAssistantVersion;

// ============================================================================
// Peer capability negotiation
// ============================================================================
// Whether the peer sent a capabilities field during the handshake.
// If false, we assume full compatibility (return true for all methods).
static bool s_peerSentCapabilities = false;

// The set of JSON-RPC methods the peer advertised it can handle.
static std::set<std::string> s_peerCapabilities;

// ============================================================================
// Project-specific assistant options (for chat provider)
// ============================================================================
static projects::RProjectAssistantOptions s_chatProjectOptions;

// ============================================================================
// Error Messages
// ============================================================================
// NOTE: This must match EXECUTION_CANCELED_ERROR constant in
// @databot/core/errors/constants.ts: "Execution canceled by user"
static const char* const kExecutionCanceledError = "Execution canceled by user";

// ============================================================================
// Code Execution Queue Limits
// ============================================================================
// Limit queued execution requests to prevent unbounded growth when R is busy
static const size_t kMaxQueuedExecutions = 10;

// Maximum time (milliseconds) an execution can wait in queue before timing out
static const int kMaxQueueWaitMs = 60000;  // 60 seconds

// ============================================================================
// Peer capability query
// ============================================================================
// Returns true if the peer can handle the given JSON-RPC method.
// If the peer did not send capabilities, assumes full compatibility.
bool peerHasCapability(const std::string& method)
{
   if (!s_peerSentCapabilities)
      return true;
   return s_peerCapabilities.count(method) > 0;
}

// ============================================================================
// Feature availability helper
// ============================================================================
// Returns true if the Posit AI feature is enabled. This requires:
// 1. The allow-posit-assistant admin option (always true in open-source, configurable in Pro)
// 2. The posit-assistant-enabled session option
bool isPaiEnabled()
{
   return options().allowPositAssistant() && options().positAssistantEnabled();
}

// Returns true if the user has selected Posit AI as their assistant (for code completions)
bool isPaiSelected()
{
   return prefs::userPrefs().assistant() == kAssistantPosit;
}

// Returns the configured chat provider, checking:
// 1. Project-level chat provider setting (if set and not "default")
// 2. Global user preference
std::string getConfiguredChatProvider()
{
   // Check for project-level override
   std::string projectChatProvider = s_chatProjectOptions.chatProvider;
   if (!projectChatProvider.empty() && projectChatProvider != "default")
      return projectChatProvider;

   // Fall back to global preference
   return prefs::userPrefs().chatProvider();
}

// Returns true if chat provider is set to Posit AI (for Chat pane)
// Checks project-level setting first, then falls back to global preference
bool isChatProviderPosit()
{
   return getConfiguredChatProvider() == kChatProviderPosit;
}

// Returns true if the user wants Posit AI for either chat or completions
// Used to determine if install/update operations should be allowed
bool isPositAiWanted()
{
   return isChatProviderPosit() || isPaiSelected();
}

// Selective imports from chat modules to avoid namespace pollution
namespace chat_constants = rstudio::session::modules::chat::constants;
namespace chat_types = rstudio::session::modules::chat::types;
namespace chat_logging = rstudio::session::modules::chat::logging;
namespace chat_installation = rstudio::session::modules::chat::installation;
namespace chat_staticfiles = rstudio::session::modules::chat::staticfiles;

// Constants used throughout
using chat_constants::kProtocolVersion;
using chat_constants::kMaxQueueSize;
using chat_constants::kMaxBufferSize;
using chat_constants::kMaxDelay;
using chat_constants::kMaxRestartAttempts;
using chat_constants::kPositAiDirName;
using chat_constants::kServerScriptPath;

// Types used throughout
using chat_types::SemanticVersion;

// Logging functions used throughout
using chat_logging::chatLogLevel;
using chat_logging::setChatLogLevel;
using chat_logging::setBackendMinLogLevel;
using chat_logging::getBackendMinLogLevel;
using chat_logging::getLogLevelPriority;
using chat_logging::shouldLogBackendMessage;
using chat_logging::rs_chatSetLogLevel;

// Installation functions used throughout
using chat_installation::locatePositAiInstallation;
using chat_installation::verifyPositAiInstallation;
using chat_installation::getInstalledVersion;

// Static file handler (used once for URI registration)
using chat_staticfiles::handleAIChatRequest;

// Logging functions are now in chat/ChatLogging.hpp/.cpp

// ============================================================================
// Execution Tracking (for cancellation support)
// ============================================================================
// R is single-threaded, so only one execution can be active at a time,
// but we need to track canceled IDs to handle pre-cancellation of queued requests
boost::mutex s_executionTrackingMutex;
std::string s_currentTrackingId;  // Empty string when not executing
std::set<std::string> s_canceledTrackingIds;  // TrackingIds that have been canceled

// ============================================================================
// Streaming Output Notification Queue with Lifecycle Management
// ============================================================================
// Notifications are queued from onConsoleOutput (any thread) and drained
// from onBackgroundProcessing (main thread only) to ensure thread safety
//
// Lifecycle management prevents unbounded growth and stale notifications:
// - Track active executions to filter notifications for completed/canceled requests
// - Queue size limit prevents memory exhaustion from noisy executions
// - Automatic cleanup when weak_ptr expires (backend died)

struct PendingNotification
{
   std::string trackingId;
   std::string type;  // "stdout" or "stderr"
   std::string content;
   boost::weak_ptr<core::system::ProcessOperations> weakOps;

   PendingNotification(const std::string& id,
                       const std::string& t,
                       const std::string& c,
                       boost::weak_ptr<core::system::ProcessOperations> ops)
      : trackingId(id), type(t), content(c), weakOps(ops)
   {
   }
};

// Global state (all protected by mutex)
static boost::mutex s_notificationQueueMutex;
static std::queue<PendingNotification> s_notificationQueue;
static std::set<std::string> s_activeTrackingIds;  // Active executions

// ============================================================================
// Deferred Code Execution Queue
// ============================================================================
// Code execution requests are queued and processed outside the poll() callback
// context. This allows the poll() callback to return quickly, enabling
// subsequent poll() calls to read cancellation messages from the pipe while
// R code is executing.

struct PendingExecutionRequest
{
   boost::weak_ptr<core::system::ProcessOperations> weakOps;
   json::Value requestId;
   std::string code;
   std::string trackingId;
   bool captureOutput;
   bool capturePlot;
   int timeout;
   std::chrono::steady_clock::time_point queuedTime;

   PendingExecutionRequest(
      boost::weak_ptr<core::system::ProcessOperations> ops,
      const json::Value& reqId,
      const std::string& c,
      const std::string& tid,
      bool capture,
      bool plot,
      int to)
      : weakOps(ops), requestId(reqId), code(c), trackingId(tid),
        captureOutput(capture), capturePlot(plot), timeout(to),
        queuedTime(std::chrono::steady_clock::now())
   {
   }
};

static boost::mutex s_pendingExecutionMutex;
static std::queue<PendingExecutionRequest> s_pendingExecutionQueue;
static bool s_executionScheduled = false;

// Forward declaration
void processPendingExecution();

// ============================================================================
// Code Execution Context (for console output capture)
// ============================================================================
class ChatExecContext
{
public:
   ChatExecContext(bool captureOutput,
                   const std::string& trackingId,
                   boost::shared_ptr<core::system::ProcessOperations> ops)
      : captureOutput_(captureOutput),
        trackingId_(trackingId),
        weakOps_(ops),
        lastStdoutFlush_(std::chrono::steady_clock::now()),
        lastStderrFlush_(std::chrono::steady_clock::now())
   {
   }

   ~ChatExecContext()
   {
      // Flush any remaining buffered output
      flushBuffers();
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

   void flushBuffers()
   {
      boost::mutex::scoped_lock lock(bufferMutex_);
      flushStdoutLocked();
      flushStderrLocked();
   }

   std::string getOutput() const
   {
      boost::mutex::scoped_lock lock(bufferMutex_);
      return outputBuffer_;
   }

   std::string getError() const
   {
      boost::mutex::scoped_lock lock(bufferMutex_);
      return errorBuffer_;
   }

private:
   void onConsoleOutput(module_context::ConsoleOutputType type,
                        const std::string& output)
   {
      if (!captureOutput_)
         return;

      boost::mutex::scoped_lock lock(bufferMutex_);

      if (type == module_context::ConsoleOutputNormal)
      {
         outputBuffer_ += output;
         stdoutBuffer_ += output;

         auto now = std::chrono::steady_clock::now();
         bool sizeThreshold = stdoutBuffer_.size() >= kMaxBufferSize;
         bool timeThreshold = (now - lastStdoutFlush_) >= kMaxDelay;

         if (sizeThreshold || timeThreshold)
            flushStdoutLocked();
      }
      else
      {
         errorBuffer_ += output;
         stderrBuffer_ += output;

         auto now = std::chrono::steady_clock::now();
         bool sizeThreshold = stderrBuffer_.size() >= kMaxBufferSize;
         bool timeThreshold = (now - lastStderrFlush_) >= kMaxDelay;

         if (sizeThreshold || timeThreshold)
            flushStderrLocked();
      }
   }

   void flushStdoutLocked()
   {
      if (!stdoutBuffer_.empty())
      {
         queueNotification(trackingId_, "stdout", stdoutBuffer_, weakOps_);
         stdoutBuffer_.clear();
         lastStdoutFlush_ = std::chrono::steady_clock::now();
      }
   }

   void flushStderrLocked()
   {
      if (!stderrBuffer_.empty())
      {
         queueNotification(trackingId_, "stderr", stderrBuffer_, weakOps_);
         stderrBuffer_.clear();
         lastStderrFlush_ = std::chrono::steady_clock::now();
      }
   }

   void queueNotification(const std::string& trackingId,
                          const std::string& type,
                          const std::string& content,
                          boost::weak_ptr<core::system::ProcessOperations> weakOps)
   {
      boost::mutex::scoped_lock lock(s_notificationQueueMutex);

      // Enforce queue size limit to prevent unbounded growth
      if (s_notificationQueue.size() >= kMaxQueueSize)
      {
         WLOG("Notification queue full (size={}), dropping oldest notification",
              s_notificationQueue.size());
         s_notificationQueue.pop();
      }

      s_notificationQueue.push(PendingNotification(trackingId, type, content, weakOps));
   }

   bool captureOutput_;
   std::string trackingId_;
   boost::weak_ptr<core::system::ProcessOperations> weakOps_;

   mutable boost::mutex bufferMutex_;
   std::string outputBuffer_;  // Complete accumulated output
   std::string errorBuffer_;   // Complete accumulated error
   std::string stdoutBuffer_;  // Pending stdout to flush
   std::string stderrBuffer_;  // Pending stderr to flush
   std::chrono::steady_clock::time_point lastStdoutFlush_;
   std::chrono::steady_clock::time_point lastStderrFlush_;

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
Error startChatBackend(bool resumeConversation = false);

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

// JSON-RPC 2.0 standard error codes
// https://www.jsonrpc.org/specification#error_object
constexpr int kJsonRpcServerError    = -32000;
constexpr int kJsonRpcMethodNotFound = -32601;
constexpr int kJsonRpcInvalidParams  = -32602;
constexpr int kJsonRpcInternalError  = -32603;

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

void sendStreamingOutput(core::system::ProcessOperations& ops,
                         const std::string& trackingId,
                         const std::string& type,
                         const std::string& content)
{
   if (!peerHasCapability("runtime/executionOutput"))
   {
      DLOG("Peer does not support runtime/executionOutput, skipping");
      return;
   }

   json::Object notification;
   notification["jsonrpc"] = "2.0";
   notification["method"] = "runtime/executionOutput";

   json::Object params;
   params["trackingId"] = trackingId;
   params["type"] = type;
   params["content"] = content;
   params["isIncremental"] = true;

   notification["params"] = params;

   std::string body = notification.write();
   std::string message = fmt::format(
       "Content-Length: {}\r\n\r\n{}",
       body.size(),
       body
   );

   if (chatLogLevel() >= 2)
   {
      DLOG("Sending streaming output: trackingId={}, type={}, length={}",
           trackingId, type, content.size());
   }

   // Best-effort send - don't abort execution on failure
   Error error = ops.writeToStdin(message, false);
   if (error)
   {
      ELOG("[sendStreamingOutput]: Failed to send streaming output: {} (error code: {})",
           error.getMessage(), error.getCode());
   }
}

void requestBackendShutdown(core::system::ProcessOperations& ops,
                            const std::string& reason,
                            int gracePeriodMs = 5000)
{
   if (!peerHasCapability("lifecycle/requestShutdown"))
   {
      DLOG("Peer does not support lifecycle/requestShutdown, skipping");
      return;
   }

   json::Object notification;
   notification["jsonrpc"] = "2.0";
   notification["method"] = "lifecycle/requestShutdown";

   json::Object params;
   params["reason"] = reason;
   params["gracePeriodMs"] = gracePeriodMs;

   notification["params"] = params;

   std::string body = notification.write();
   std::string message = fmt::format(
       "Content-Length: {}\r\n\r\n{}",
       body.size(),
       body
   );

   DLOG("Requesting graceful backend shutdown: reason={}, gracePeriodMs={}",
        reason, gracePeriodMs);

   // Best-effort send - don't block if it fails
   Error error = ops.writeToStdin(message, false);
   if (error)
   {
      WLOG("Failed to send shutdown request: {}", error.getMessage());
   }
}

void drainNotificationQueueForExecution(core::system::ProcessOperations& ops,
                                         const std::string& trackingId)
{
   std::vector<PendingNotification> toProcess;

   // Phase 1: Extract notifications to process (hold lock briefly)
   {
      boost::mutex::scoped_lock lock(s_notificationQueueMutex);

      std::queue<PendingNotification> remaining;
      while (!s_notificationQueue.empty())
      {
         PendingNotification notif = s_notificationQueue.front();
         s_notificationQueue.pop();

         if (notif.trackingId == trackingId)
         {
            toProcess.push_back(notif);
         }
         else
         {
            // Preserve notifications from concurrent executions
            remaining.push(notif);
         }
      }

      s_notificationQueue = remaining;
   }

   // Phase 2: Send notifications (no lock held, no race condition)
   for (const auto& notif : toProcess)
   {
      sendStreamingOutput(ops, notif.trackingId, notif.type, notif.content);
   }
}

void registerActiveExecution(const std::string& trackingId)
{
   boost::mutex::scoped_lock lock(s_notificationQueueMutex);
   s_activeTrackingIds.insert(trackingId);
}

void unregisterActiveExecution(const std::string& trackingId)
{
   boost::mutex::scoped_lock lock(s_notificationQueueMutex);
   s_activeTrackingIds.erase(trackingId);
}

void clearNotificationsForExecution(const std::string& trackingId)
{
   boost::mutex::scoped_lock lock(s_notificationQueueMutex);

   std::queue<PendingNotification> remaining;
   while (!s_notificationQueue.empty())
   {
      PendingNotification notif = s_notificationQueue.front();
      s_notificationQueue.pop();

      if (notif.trackingId != trackingId)
      {
         remaining.push(notif);
      }
      // Drop notifications for canceled trackingId
   }

   s_notificationQueue = remaining;
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

   // Get R workspace variables with smart filtering
   json::Array variablesArray;
   size_t totalVariableCount = 0;
   {
      using namespace rstudio::r::sexp;

      // Note: R_GlobalEnv objects don't need explicit protection as they're
      // already protected by the environment itself. rProtect is available
      // for any intermediate R operations.
      Protect rProtect;
      std::vector<Variable> vars;

      // List ALL variables (including hidden) to get accurate total count
      listEnvironment(R_GlobalEnv,
                      true,   // includeAll - get everything first
                      false,  // includeLastDotValue - we'll filter manually
                      &vars);

      totalVariableCount = vars.size();

      // Smart filtering: exclude functions and hidden variables
      std::vector<Variable> filteredVars;
      filteredVars.reserve(vars.size());

      for (const Variable& var : vars)
      {
         const std::string& name = var.first;
         SEXP varSEXP = var.second;

         // Skip hidden variables (starting with '.')
         if (!name.empty() && name[0] == '.')
            continue;

         // Skip functions (CLOSXP = user functions, SPECIALSXP/BUILTINSXP = built-ins)
         int objType = TYPEOF(varSEXP);
         if (objType == CLOSXP || objType == SPECIALSXP || objType == BUILTINSXP)
            continue;

         filteredVars.push_back(var);
      }

      // OPTIMIZATION: Extract sizes WITHOUT full JSON conversion
      // This is much faster than calling varToJson() on all variables
      struct VarWithSize {
         Variable var;
         int64_t size;
      };

      std::vector<VarWithSize> varsWithSize;
      varsWithSize.reserve(filteredVars.size());

      for (const Variable& var : filteredVars)
      {
         // Use .rs.estimatedObjectSize() which is optimized for:
         // - ALTREP objects (doesn't materialize)
         // - Large objects (returns estimate quickly)
         // - Edge cases (null pointers, etc.)
         SEXP sizeSEXP;
         Error error = r::exec::RFunction(".rs.estimatedObjectSize")
            .addParam(var.second)
            .call(&sizeSEXP, &rProtect);

         int64_t size = 0;
         if (!error && sizeSEXP != R_NilValue)
         {
            if (Rf_isInteger(sizeSEXP) && Rf_length(sizeSEXP) > 0)
            {
               int rSize = INTEGER(sizeSEXP)[0];
               // Check for NA and negative values
               if (rSize != NA_INTEGER && rSize >= 0)
                  size = static_cast<int64_t>(rSize);
            }
            else if (Rf_isReal(sizeSEXP) && Rf_length(sizeSEXP) > 0)
            {
               double rSize = REAL(sizeSEXP)[0];
               // Check for NA/NaN, negative values, and overflow
               if (!ISNA(rSize) && !ISNAN(rSize) && rSize >= 0.0 &&
                   rSize <= static_cast<double>(INT64_MAX))
                  size = static_cast<int64_t>(rSize);
            }
         }

         varsWithSize.push_back(VarWithSize{var, size});
      }

      // Sort by size (largest first)
      std::sort(varsWithSize.begin(),
                varsWithSize.end(),
                [](const VarWithSize& a, const VarWithSize& b)
                {
                   return a.size > b.size;
                });

      // Take top MAX_VARIABLES and convert to JSON
      // Limit to 100 to balance context richness with performance.
      // Most workspaces have <100 data objects; for larger workspaces,
      // showing the largest objects provides the most useful context.
      const size_t MAX_VARIABLES = 100;
      size_t numToInclude = std::min(varsWithSize.size(), MAX_VARIABLES);

      for (size_t i = 0; i < numToInclude; i++)
      {
         json::Value jsonVal = environment::varToJson(R_GlobalEnv, varsWithSize[i].var);

         // Skip variables that fail conversion (though this is rare)
         if (jsonVal.isNull() || !jsonVal.isObject())
         {
            DLOG("Failed to convert variable '{}' to JSON, skipping",
                 varsWithSize[i].var.first);
            continue;
         }

         // Transform to SessionVariable schema (name, type, displayName)
         // The backend expects simplified variables for context, not full details
         json::Object varObj = jsonVal.getObject();
         json::Object simplifiedVar;

         // Extract name (required) - fallback to variable name from pair if missing
         std::string name;
         if (varObj.hasMember("name") && varObj["name"].isString())
            name = varObj["name"].getString();
         else
            name = varsWithSize[i].var.first;
         simplifiedVar["name"] = name;

         // Extract type (required) - fallback to "unknown" if missing
         std::string type;
         if (varObj.hasMember("type") && varObj["type"].isString())
            type = varObj["type"].getString();
         else
            type = "unknown";
         simplifiedVar["type"] = type;

         // TODO: Reevaluate providing a richer displayName in the future.
         // For now, displayName is the same as name for simplicity.
         // Previous implementation attempted to extract description/value:
         //
         // // Extract description and use as displayName (required)
         // // Description contains human-readable summary like "data.frame: 32 obs. of 11 variables"
         // std::string displayName;
         // if (varObj.hasMember("description") && varObj["description"].isString())
         // {
         //    displayName = varObj["description"].getString();
         // }
         //
         // if (displayName.empty())
         // {
         //    // Fallback: use "type: value" format if no description
         //    std::string value;
         //    if (varObj.hasMember("value") && varObj["value"].isString())
         //       value = varObj["value"].getString();
         //
         //    if (value.empty())
         //       displayName = type;
         //    else
         //       displayName = type + ": " + value;
         // }
         simplifiedVar["displayName"] = name;

         variablesArray.push_back(std::move(simplifiedVar));
      }

      // Log filtering results
      if (filteredVars.size() > MAX_VARIABLES)
      {
         DLOG("Variable context: showing {} of {} variables (filtered from {} total)",
              variablesArray.getSize(), filteredVars.size(), totalVariableCount);
      }
      else if (filteredVars.size() < totalVariableCount)
      {
         DLOG("Variable context: showing {} variables (filtered from {} total)",
              variablesArray.getSize(), totalVariableCount);
      }
   }

   // Add metadata about filtering
   json::Object variablesMeta;
   variablesMeta["totalCount"] = static_cast<int>(totalVariableCount);
   variablesMeta["shownCount"] = static_cast<int>(variablesArray.getSize());
   variablesMeta["filtered"] = (variablesArray.getSize() < totalVariableCount);

   session["variables"] = variablesArray;
   session["variablesMeta"] = variablesMeta;

   // PlatformInfo object
   json::Object platformInfo;
   // TODO: Future work - check if active plots exist
   platformInfo["hasPlots"] = false;
   platformInfo["platformVersion"] = RSTUDIO_VERSION;
   platformInfo["currentDate"] = currentDate;

   // Main result
   json::Object result;

   // Enumerate open files from source database
   // This includes all documents currently open in the editor, both saved and unsaved
   json::Array openFilesArray;
   {
      std::vector<boost::shared_ptr<source_database::SourceDocument>> docs;
      Error error = source_database::list(&docs);

      if (error)
      {
         WLOG("Failed to enumerate open files for detailed context: {}", error.getSummary());
         // Continue with empty array rather than failing the entire request
      }
      else
      {
         DLOG("Building open files context from {} documents", docs.size());

         for (const auto& pDoc : docs)
         {
            // Defensive null check
            if (!pDoc)
            {
               WLOG("Encountered null document pointer in source database, skipping");
               continue;
            }

            json::Object fileObj;

            // Construct URI and fileName based on document type
            if (pDoc->isUntitled())
            {
               // Use untitled: scheme for unsaved documents (matches Positron/VSCode convention)
               fileObj["uri"] = fmt::format("untitled:{}", pDoc->id());

               // Use tempName property (matches UI display like "Untitled1").
               // This is guaranteed to be set when isUntitled() is true.
               fileObj["tempName"] = pDoc->getProperty("tempName");
            }
            else
            {
               std::string docPath = pDoc->path();
               if (docPath.empty())
               {
                  WLOG("Titled document {} has empty path, skipping", pDoc->id());
                  continue;
               }

               // Use file:/// scheme with absolute path for saved documents (RFC 8089)
               FilePath resolvedPath = module_context::resolveAliasedPath(docPath);
               std::string absPath = resolvedPath.getAbsolutePath();

               // Windows: file:///C:/Users/file.txt
               // Linux/macOS: file:///home/user/file.txt

#ifdef _WIN32
               // Normalize path separators on Windows (C:\path -> C:/path)
               std::replace(absPath.begin(), absPath.end(), '\\', '/');
               fileObj["uri"] = fmt::format("file:///{}", absPath);
#else
               fileObj["uri"] = fmt::format("file://{}", absPath);
#endif
            }

            // Document state flags
            fileObj["isModified"] = pDoc->dirty();
            fileObj["isUntitled"] = pDoc->isUntitled();

            // Visibility: documents with relativeOrder > 0 are visible tabs in the editor
            // relativeOrder == 0 means document is loaded but not visible in a tab
            fileObj["isVisible"] = (pDoc->relativeOrder() > 0);

            // Active: the document currently focused in the editor
            // s_focusedDocumentId is set by chat_doc_focused RPC from GWT client
            fileObj["isActiveEditor"] = (pDoc->id() == s_focusedDocumentId);

            // Selections for active editor
            if (pDoc->id() == s_focusedDocumentId && !s_focusedDocumentSelections.isEmpty())
            {
               fileObj["selections"] = s_focusedDocumentSelections;
            }
            else
            {
               fileObj["selections"] = json::Array();
            }

            openFilesArray.push_back(fileObj);
         }

         DLOG("Open files context: returning {} files", openFilesArray.getSize());
      }
   }
   result["openFiles"] = openFilesArray;
   result["session"] = session;
   result["platformInfo"] = platformInfo;

   sendJsonRpcResponse(ops, requestId, result);
}

// Forward declaration for executeCodeImpl
void executeCodeImpl(boost::shared_ptr<core::system::ProcessOperations> pOps,
                     const json::Value& requestId,
                     const std::string& code,
                     const std::string& trackingId,
                     bool captureOutput,
                     bool capturePlot,
                     int timeout);

// Process pending execution requests from the queue
void processPendingExecution()
{
   PendingExecutionRequest request(
      boost::weak_ptr<core::system::ProcessOperations>(),
      json::Value(), "", "", false, false, 0);

   {
      boost::mutex::scoped_lock lock(s_pendingExecutionMutex);
      s_executionScheduled = false;

      if (s_pendingExecutionQueue.empty())
         return;

      request = s_pendingExecutionQueue.front();
      s_pendingExecutionQueue.pop();
   }

   // Check for timeout - if request waited too long in queue, return error
   auto now = std::chrono::steady_clock::now();
   auto waitTimeMs = std::chrono::duration_cast<std::chrono::milliseconds>(
      now - request.queuedTime).count();

   if (waitTimeMs > kMaxQueueWaitMs)
   {
      WLOG("Execution request timed out after {}ms in queue for trackingId: {}",
           waitTimeMs, request.trackingId);

      // Send timeout error response if backend is still alive
      boost::shared_ptr<core::system::ProcessOperations> pOps = request.weakOps.lock();
      if (pOps)
      {
         sendJsonRpcError(*pOps, request.requestId, kJsonRpcServerError,
            "Execution timed out. The R console was busy for too long.");
      }

      // Schedule next request if queued
      {
         boost::mutex::scoped_lock lock(s_pendingExecutionMutex);
         if (!s_pendingExecutionQueue.empty() && !s_executionScheduled)
         {
            s_executionScheduled = true;
            module_context::scheduleDelayedWork(
               boost::posix_time::milliseconds(1),
               processPendingExecution,
               true);  // idleOnly=true - wait for R to be idle
         }
      }
      return;
   }

   // Signal that R is executing chat code
   console_input::setExecuting(true);
   ClientEvent busyEvent(client_events::kBusy, true);
   module_context::enqueClientEvent(busyEvent);

   // Execute the code (may take a long time - but we're outside poll callback!)
   executeCodeImpl(request.weakOps.lock(), request.requestId, request.code,
                   request.trackingId, request.captureOutput, request.capturePlot,
                   request.timeout);

   // Schedule next request if queued, or clear busy state
   {
      boost::mutex::scoped_lock lock(s_pendingExecutionMutex);
      if (!s_pendingExecutionQueue.empty() && !s_executionScheduled)
      {
         s_executionScheduled = true;
         module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(1),
            processPendingExecution,
            true);  // idleOnly=true - wait for R to be idle
      }
      else if (s_pendingExecutionQueue.empty())
      {
         lock.unlock();
         console_input::setExecuting(false);
         console_input::updateSessionExecuting();
         ClientEvent busyEvent(client_events::kBusy, false);
         module_context::enqueClientEvent(busyEvent);
         console_input::reissueLastConsolePrompt();
      }
   }
}

// Handle runtime/executeCode request by queueing for deferred execution
void handleExecuteCode(core::system::ProcessOperations& ops,
                       const json::Value& requestId,
                       const json::Object& params)
{
   DLOG("Handling runtime/executeCode request (queueing for deferred execution)");

   // Extract and validate parameters
   std::string language, code, trackingId;
   Error error = json::readObject(params, "language", language, "code", code, "trackingId", trackingId);

   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: " + error.getMessage());
      return;
   }

   if (language != "r")
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
         "Unsupported language: " + language + ". Only 'r' is currently supported.");
      return;
   }

   // Extract optional options
   json::Object options;
   json::readObject(params, "options", options);
   bool captureOutput = true, capturePlot = false;
   int timeout = 30000;
   json::readObject(options, "captureOutput", captureOutput);
   json::readObject(options, "capturePlot", capturePlot);
   json::readObject(options, "timeout", timeout);

   // Check if R console is busy and log for debugging
   bool rIsBusy = console_input::executing() || r::session::isBusy();
   if (rIsBusy)
   {
      DLOG("R console is busy, execution will be queued until idle for trackingId: {}", trackingId);
   }

   // Queue the request for deferred execution
   {
      boost::mutex::scoped_lock lock(s_pendingExecutionMutex);

      // Check queue limit to prevent unbounded growth when R is busy
      if (s_pendingExecutionQueue.size() >= kMaxQueuedExecutions)
      {
         WLOG("Execution queue full ({} pending), rejecting request for trackingId: {}",
              s_pendingExecutionQueue.size(), trackingId);

         // Release mutex before I/O operation
         lock.unlock();
         sendJsonRpcError(ops, requestId, kJsonRpcServerError,
            "R console is busy. Execution queue is full. Please wait for current operations to complete.");
         return;
      }

      s_pendingExecutionQueue.push(PendingExecutionRequest(
         ops.getWeakPtr(), requestId, code, trackingId,
         captureOutput, capturePlot, timeout));

      if (!s_executionScheduled)
      {
         s_executionScheduled = true;
         module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(1),
            processPendingExecution,
            true);  // idleOnly=true - wait for R to be idle
      }
   }

   DLOG("Queued execution for trackingId: {}", trackingId);
}

void executeCodeImpl(boost::shared_ptr<core::system::ProcessOperations> pOps,
                     const json::Value& requestId,
                     const std::string& code,
                     const std::string& trackingId,
                     bool captureOutput,
                     bool capturePlot,
                     int timeout)
{
   if (!pOps)
   {
      WLOG("Chat backend died before execution could start for trackingId: {}", trackingId);
      return;
   }

   core::system::ProcessOperations& ops = *pOps;

   DLOG("Executing code for trackingId: {}", trackingId);

   // Track if execution was canceled (for response field)
   bool wasCanceled = false;

   // Check if this request was already canceled (pre-cancellation of queued request)
   // and register tracking ID for cancellation support
   {
      boost::mutex::scoped_lock lock(s_executionTrackingMutex);

      // Check if already canceled
      if (s_canceledTrackingIds.count(trackingId) > 0)
      {
         wasCanceled = true;
         s_canceledTrackingIds.erase(trackingId);

         // Return a response indicating cancellation
         json::Object result;
         result["output"] = "";
         result["error"] = kExecutionCanceledError;
         result["canceled"] = true;
         result["plots"] = json::Array();
         result["executionTime"] = 0;

         sendJsonRpcResponse(ops, requestId, result);
         // Note: No need to unregister here as we never registered (this check is before registration)
         return;
      }

      // Register as current execution
      s_currentTrackingId = trackingId;
   }

   // Register this execution as active to enable notification filtering
   registerActiveExecution(trackingId);

   // Record start time
   auto startTime = std::chrono::steady_clock::now();

   // Error variable for R execution
   Error error;

   // Create execution context with streaming support
   // Pass shared_ptr for safe access from background thread
   ChatExecContext execContext(captureOutput, trackingId, ops.shared_from_this());
   execContext.connect();

   // Record the current plot before execution so we can detect if
   // plotting actually occurred (to avoid capturing stale plots)
   r::sexp::Protect plotBeforeProtect;
   SEXP plotBeforeSEXP = R_NilValue;
   if (capturePlot)
   {
      Error plotBeforeError = r::exec::RFunction(".rs.chat.getRecordedPlot").call(
         &plotBeforeSEXP, &plotBeforeProtect);
      // If there's an error, plotBeforeSEXP will remain R_NilValue (which is fine)
      if (plotBeforeError)
      {
         LOG_DEBUG_MESSAGE("Failed to record plot before execution: " + plotBeforeError.getMessage());
      }
   }

   // Echo source code with prompts (like evaluate does)
   echoSourceCode(code);

   // Evaluate the code. For multi-line code, we parse it into separate expressions
   // and evaluate each one, mimicking REPL behavior where each line is evaluated
   // and visible results are auto-printed. This is necessary because wrapping
   // multi-statement code in withVisible({...}) causes only the last statement's
   // result to be captured, losing intermediate htmlwidgets or other visible results.
   //
   // IMPORTANT ASSUMPTIONS:
   // 1. This assumes complete, well-formed expressions. Incomplete expressions
   //    (e.g., multi-line function definitions without braces on first line) will
   //    fail to parse.
   // 2. Expression-by-expression evaluation matches R REPL behavior but differs
   //    from evaluate::evaluate() for some edge cases.
   // 3. Each expression is evaluated independently in the global environment.
   //
   // Examples:
   //   Supported:     x <- 1; y <- 2; x + y
   //   Supported:     f <- function() { x <- 1; x + 1 }
   //   NOT supported: f <- function()\n{\n  x <- 1\n}  (parse error)
   r::sexp::Protect protect;

   // Parse the code into expressions
   SEXP parsedSEXP = R_NilValue;
   error = r::exec::RFunction("parse")
      .addParam("text", code)
      .addParam("keep.source", false)
      .call(&parsedSEXP, &protect);

   if (error)
   {
      LOG_ERROR_MESSAGE("Failed to parse code: " + error.getMessage());
   }

   // Evaluate each expression with withVisible() to capture visibility
   // Declare these outside the block so they're available for error reporting
   int numExpressions = 0;
   int currentExpressionIndex = 0;
   std::string currentExpressionText;

   if (!error && parsedSEXP != R_NilValue && TYPEOF(parsedSEXP) == EXPRSXP)
   {
      // Add code to console history so it appears in History tab
      r::session::consoleHistory().add(code, false);

      // Fire events to notify modules that code is about to execute
      // (matches behavior of normal console input in SessionConsoleInput.cpp)
      module_context::events().onBeforeExecute();
      module_context::events().onConsoleInput(code);

      numExpressions = Rf_length(parsedSEXP);

      // Evaluate each expression
      for (int i = 0; i < numExpressions && !error; i++)
      {
         // Check for cancellation before each expression
         {
            boost::mutex::scoped_lock lock(s_executionTrackingMutex);
            if (s_canceledTrackingIds.count(trackingId) > 0)
            {
               wasCanceled = true;
               s_canceledTrackingIds.erase(trackingId);
               error = Error(boost::system::errc::operation_canceled,
                            kExecutionCanceledError, ERROR_LOCATION);
               break;
            }
         }

         SEXP exprSEXP = VECTOR_ELT(parsedSEXP, i);
         SEXP evalResultSEXP = R_NilValue;

         // Store current expression index for error reporting
         currentExpressionIndex = i;

         // Evaluate this expression using our safe wrapper that catches errors and interrupts.
         // The result will be either:
         //   - A condition object (inherits "interrupt" or "error")
         //   - A list with $value and $visible (successful evaluation)
         //
         // IMPORTANT: Wrap the expression in quote() to prevent R from evaluating it
         // during argument passing. Without this, R evaluates exprSEXP before passing
         // it to the function, which breaks visibility detection (all results become visible).
         // https://github.com/rstudio/rstudio/issues/17044
         SEXP quotedExpr = Rf_lang2(Rf_install("quote"), exprSEXP);
         protect.add(quotedExpr);

         Error callError = r::exec::RFunction(".rs.chat.safeEval")
            .addParam(quotedExpr)
            .call(&evalResultSEXP, &protect);

         // This should only fail if there's a problem calling the wrapper itself
         if (callError)
         {
            LOG_ERROR_MESSAGE("Failed to call .rs.chat.safeEval: " + callError.getMessage());
            error = callError;
            break;
         }

         if (evalResultSEXP == R_NilValue)
         {
            std::string errorMsg = ".rs.chat.safeEval returned NULL for expression " +
               std::to_string(i + 1) + " of " + std::to_string(numExpressions);
            LOG_ERROR_MESSAGE(errorMsg);
            error = Error(boost::system::errc::state_not_recoverable, errorMsg, ERROR_LOCATION);
            break;
         }

         // Check if the result is an interrupt or error condition
         // Treat both as regular errors: display them and stop executing further expressions,
         // but do not cancel the overall LLM/chat response.
         if (Rf_inherits(evalResultSEXP, "interrupt") || Rf_inherits(evalResultSEXP, "error"))
         {
            // Extract the error message using conditionMessage()
            std::string errorMsg = Rf_inherits(evalResultSEXP, "interrupt") 
               ? "Execution interrupted" 
               : "Error during evaluation";
            SEXP messageSEXP = R_NilValue;
            Error msgError = r::exec::RFunction("base:::conditionMessage")
               .addParam(evalResultSEXP)
               .call(&messageSEXP, &protect);

            if (!msgError && messageSEXP != R_NilValue &&
                TYPEOF(messageSEXP) == STRSXP && Rf_length(messageSEXP) > 0)
            {
               errorMsg = CHAR(STRING_ELT(messageSEXP, 0));
            }

            error = Error(boost::system::errc::state_not_recoverable, errorMsg, ERROR_LOCATION);
            break;
         }

         // Successful evaluation - result should be a list with $value and $visible
         if (TYPEOF(evalResultSEXP) != VECSXP || Rf_length(evalResultSEXP) < 2)
         {
            std::string errorMsg = ".rs.chat.safeEval returned unexpected result type for expression " +
               std::to_string(i + 1) + " of " + std::to_string(numExpressions);
            LOG_ERROR_MESSAGE(errorMsg);
            error = Error(boost::system::errc::state_not_recoverable, errorMsg, ERROR_LOCATION);
            break;
         }

         // Extract result and visibility
         SEXP exprResult = VECTOR_ELT(evalResultSEXP, 0);
         SEXP visibleSEXP = VECTOR_ELT(evalResultSEXP, 1);

         // Validate visibility flag
         if (visibleSEXP == R_NilValue || TYPEOF(visibleSEXP) != LGLSXP ||
             Rf_length(visibleSEXP) == 0)
         {
            std::string visibilityErrorMsg = ".rs.chat.safeEval returned invalid visibility flag for expression " +
               std::to_string(i + 1) + " of " + std::to_string(numExpressions);
            LOG_ERROR_MESSAGE(visibilityErrorMsg);
            error = Error(boost::system::errc::state_not_recoverable, visibilityErrorMsg, ERROR_LOCATION);
            break;
         }

         int visibleValue = Rf_asLogical(visibleSEXP);
         if (visibleValue == NA_LOGICAL)
         {
            LOG_ERROR_MESSAGE(".rs.chat.safeEval returned NA for visibility flag, treating as FALSE for expression " +
               std::to_string(i + 1) + " of " + std::to_string(numExpressions));
            visibleValue = FALSE;
         }

         bool exprVisible = (visibleValue == TRUE);

         // Print visible results immediately (mimics REPL)
         if (exprVisible)
         {
            Error printError = r::exec::RFunction("print")
               .addParam(exprResult)
               .call();

            if (printError)
            {
               // Print errors should be visible to users, not just logged
               std::string printErrorMsg = "Error printing result: " + printError.getMessage();
               LOG_ERROR_MESSAGE(printErrorMsg);

               // Also show to user via console (similar to how R handles print errors)
               std::string userMsg = "Warning: " + printErrorMsg + "\n";
               module_context::events().onConsoleOutput(
                  module_context::ConsoleOutputError, userMsg);
               module_context::consoleWriteError(userMsg);
            }
         }
      }
   }

   // Handle parse/runtime errors
   // We need to both:
   // 1. Fire the onConsoleOutput signal so our callback can capture it
   // 2. Write to console UI via consoleWriteError for user visibility
   if (error)
   {
      std::string errorMsg = error.getMessage();

      // Add expression context for multi-statement code
      if (numExpressions > 1)
      {
         std::string contextPrefix = "Error in expression " +
            std::to_string(currentExpressionIndex + 1) + " of " +
            std::to_string(numExpressions);

         // Try to deparse the failing expression (only on error, avoids S4 issues for successful code)
         // If deparse fails (e.g., S4 objects like ggplot2), we just show expression number without text
         if (currentExpressionIndex >= 0 && parsedSEXP != R_NilValue &&
             TYPEOF(parsedSEXP) == EXPRSXP && currentExpressionIndex < Rf_length(parsedSEXP))
         {
            SEXP failedExprSEXP = VECTOR_ELT(parsedSEXP, currentExpressionIndex);
            SEXP deparsedSEXP = R_NilValue;

            // Try to deparse - if it fails, that's okay, we'll just skip the text
            Error deparseError = r::exec::RFunction("deparse")
               .addParam(failedExprSEXP)
               .addParam("width.cutoff", 80)
               .call(&deparsedSEXP, &protect);

            // Only include expression text if deparse succeeded
            if (!deparseError && deparsedSEXP != R_NilValue &&
                TYPEOF(deparsedSEXP) == STRSXP && Rf_length(deparsedSEXP) > 0)
            {
               std::string exprText = r::sexp::asString(deparsedSEXP);
               // Truncate if multi-line
               size_t newlinePos = exprText.find('\n');
               if (newlinePos != std::string::npos)
                  exprText = exprText.substr(0, newlinePos) + " ...";

               if (!exprText.empty())
                  contextPrefix += " (" + exprText + ")";
            }
         }

         contextPrefix += ": ";

         // Strip existing "Error: " or "Error in " prefix to avoid duplication
         if (errorMsg.find("Error: ") == 0)
            errorMsg = errorMsg.substr(7);
         else if (errorMsg.find("Error in ") == 0)
            errorMsg = errorMsg.substr(9);

         errorMsg = contextPrefix + errorMsg;
      }
      else
      {
         // Single expression - ensure consistent "Error: " prefix
         if (errorMsg.find("Error: ") != 0 && errorMsg.find("Error in ") != 0)
            errorMsg = "Error: " + errorMsg;
      }

      std::string errorOutput = errorMsg + "\n";

      // Fire signal first so callback captures it
      module_context::events().onConsoleOutput(
         module_context::ConsoleOutputError, errorOutput);

      // Also write to console UI
      module_context::consoleWriteError(errorOutput);
   }

   // NOTE: We no longer need to print results here because we print each visible
   // expression immediately as we evaluate them in the loop above. This mimics
   // REPL behavior where each line's visible result is printed before the next
   // line is evaluated.

   // Handle plots if requested
   // NOTE: This only captures the final plot state. If code creates multiple plots
   // (e.g., plot(1); plot(2)), only the last one is captured. This is a known
   // limitation compared to the previous evaluate-based approach which used new_device.
   json::Array plotsArray;
   if (capturePlot)
   {
      // Use R helper for plot capture, passing the recorded plot from before
      // execution so it can detect if a NEW plot was created (vs. stale plot)
      r::sexp::Protect plotProtect;
      SEXP plotSEXP = R_NilValue;

      r::exec::RFunction captureFunc(".rs.chat.captureCurrentPlot");
      captureFunc.addParam("plotBefore", plotBeforeSEXP);
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

   // Flush any remaining buffered output
   // This queues final notifications to the global queue
   execContext.flushBuffers();

   // Synchronously drain THIS execution's notifications before final response
   // This guarantees correct ordering without sleeping or waiting for background processing
   // Handles concurrent executions correctly by only draining matching trackingId
   drainNotificationQueueForExecution(ops, trackingId);

   // Disconnect context
   execContext.disconnect();

   // Check if this execution was canceled (handles R interrupts that don't go through C++ checks)
   // Do this BEFORE clearing the tracking ID
   {
      boost::mutex::scoped_lock lock(s_executionTrackingMutex);
      if (s_canceledTrackingIds.count(trackingId) > 0)
      {
         wasCanceled = true;
      }
   }

   // Clear tracking ID - execution complete
   {
      boost::mutex::scoped_lock lock(s_executionTrackingMutex);
      s_currentTrackingId.clear();
      // Also clean up from canceled set in case cancel arrived during execution
      s_canceledTrackingIds.erase(trackingId);
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
   result["canceled"] = wasCanceled;
   result["plots"] = plotsArray;
   result["executionTime"] = executionTime;

   // Unregister this execution - notifications after this point are stale
   unregisterActiveExecution(trackingId);

   // Send successful response
   sendJsonRpcResponse(ops, requestId, result);

   // Fire change detection event to trigger environment refresh
   // Use ChangeSourceREPL if a plot was captured so the Plots pane gets activated
   module_context::ChangeSource changeSource = !plotsArray.isEmpty()
      ? module_context::ChangeSourceREPL
      : module_context::ChangeSourceRPC;
   module_context::events().onDetectChanges(changeSource);
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

   // Add to canceled set and check if currently executing
   bool shouldInterrupt = false;
   {
      boost::mutex::scoped_lock lock(s_executionTrackingMutex);

      // Always add to canceled set - handles pre-cancellation of queued requests
      s_canceledTrackingIds.insert(trackingId);

      // Check if this is currently executing
      shouldInterrupt = !s_currentTrackingId.empty() &&
                        s_currentTrackingId == trackingId;
   }

   // Clear any queued notifications for this canceled execution
   clearNotificationsForExecution(trackingId);

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

// ============================================================================
// Workspace File Content Handlers (for databot file operations)
// ============================================================================

// Convert internal document type to standard language ID
// Based on SessionAssistant.cpp:languageIdFromDocument()
std::string languageIdFromDocument(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // R files: r_source, r_markdown, quarto_markdown all map to "r"
   if (pDoc->isRMarkdownDocument() || pDoc->isRFile())
      return "r";

   // Special cases for Makefiles and Dockerfiles
   FilePath docPath(pDoc->path());
   std::string name = docPath.getFilename();
   std::string stem = docPath.getStem();
   if (name == "Makefile" || name == "makefile")
      return "makefile";
   else if (stem == "Dockerfile")
      return "dockerfile";

   // Default: lowercase the internal type (cpp → cpp, python → python, etc.)
   return boost::algorithm::to_lower_copy(pDoc->type());
}

// Map databot languageId values to RStudio document types
// Used by workspace/insertIntoNewFile to create documents with correct type
// NOTE: These values must match the constants in NewDocumentWithCodeEvent.java:
//   R_SCRIPT = "r_script", R_NOTEBOOK = "r_notebook", SQL = "sql"
// The GWT client defaults to R Markdown if the type doesn't match these constants.
std::string documentTypeFromLanguageId(const std::string& languageId)
{
   if (languageId == "r")
      return "r_script";  // Must match NewDocumentWithCodeEvent.R_SCRIPT
   else if (languageId == "python")
      return "python";
   else if (languageId == "sql")
      return "sql";
   else if (languageId == "markdown" || languageId == "rmd" || languageId == "rmarkdown")
      return "r_markdown";
   else if (languageId == "quarto" || languageId == "qmd")
      return "quarto_markdown";
   else if (languageId == "cpp" || languageId == "c++")
      return "cpp";
   else if (languageId == "javascript" || languageId == "js")
      return "js";
   else if (languageId == "shell" || languageId == "sh" || languageId == "bash")
      return "sh";
   else
      return "text";  // Default to plain text
}

// Convert file:// URI to file system path (cross-platform)
std::string uriToPath(const std::string& uri)
{
   std::string filePrefix("file://");
   if (uri.find(filePrefix) == 0)
   {
      // Windows uses file:/// for drive letters (e.g., file:///C:/path)
      // Unix/Mac use file:// (e.g., file:///path)
#ifdef _WIN32
      if (uri.find(filePrefix + "/") == 0)
         filePrefix = filePrefix + "/";
#endif

      std::string path = uri.substr(filePrefix.length());
      path = core::http::util::urlDecode(path);
      path = r::util::fixPath(path);  // Clean up double slashes

      return path;
   }

   // If no file:// prefix, assume it's already a path
   return uri;
}

// Find an open document by document ID
// Used for untitled documents which have no file path
boost::shared_ptr<source_database::SourceDocument> findOpenDocumentById(const std::string& docId)
{
   boost::shared_ptr<source_database::SourceDocument> pDoc(
      new source_database::SourceDocument());
   Error error = source_database::get(docId, pDoc);
   if (error)
   {
      WLOG("Failed to get document by ID {}: {}", docId, error.getMessage());
      return nullptr;
   }
   return pDoc;
}

// Find an open document by file path with proper alias/symlink handling
// Note: Untitled documents (never saved) cannot be matched by path
boost::shared_ptr<source_database::SourceDocument> findOpenDocument(const std::string& path)
{
   if (path.empty())
      return nullptr;  // Cannot match untitled documents by path

   // Method 1: Use source_database::getId() - fast, handles aliased paths
   // This converts the path to RStudio's internal aliased format and looks it up
   FilePath inputPath = module_context::resolveAliasedPath(path);
   std::string id;
   Error error = source_database::getId(inputPath, &id);
   if (!error)
   {
      boost::shared_ptr<source_database::SourceDocument> pDoc(
         new source_database::SourceDocument());
      error = source_database::get(id, pDoc);
      if (!error)
         return pDoc;
   }

   // Method 2: Fallback to path comparison
   // Handles: symlinks (when both exist), deleted files, path aliasing differences
   std::vector<boost::shared_ptr<source_database::SourceDocument>> docs;
   error = source_database::list(&docs);
   if (error)
   {
      WLOG("Failed to list source documents: {}", error.getMessage());
      return nullptr;
   }

   for (const auto& doc : docs)
   {
      if (doc->path().empty())
         continue;  // Skip untitled documents

      FilePath docPath = module_context::resolveAliasedPath(doc->path());

      // Try filesystem-aware comparison first (handles symlinks when both exist)
      if (inputPath.exists() && docPath.exists())
      {
         if (docPath.isEquivalentTo(inputPath))
            return doc;
      }
      else
      {
         // Fallback: string comparison of absolute paths
         // This works when file is open but deleted/moved on disk
         if (docPath.getAbsolutePath() == inputPath.getAbsolutePath())
            return doc;
      }
   }

   return nullptr;
}

// Resolve a URI to an open document and/or file path.
// For untitled: URIs, sets *pDoc and leaves *pPath empty.
// For file: URIs, sets *pPath and sets *pDoc if the file is open in the editor.
// Returns true on success; returns false and sends an error response on failure.
bool resolveDocumentUri(
   core::system::ProcessOperations& ops,
   const json::Value& requestId,
   const std::string& uri,
   boost::shared_ptr<source_database::SourceDocument>* pDoc,
   std::string* pPath)
{
   if (boost::starts_with(uri, "untitled:"))
   {
      std::string docId = uri.substr(9);  // Remove "untitled:" prefix
      *pDoc = findOpenDocumentById(docId);
      if (!*pDoc)
      {
         DLOG("Untitled document not found: {}", uri);
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Untitled document not found: " + uri);
         return false;
      }
      DLOG("Found untitled document by ID: {}", docId);
   }
   else
   {
      *pPath = uriToPath(uri);
      if (pPath->empty())
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid URI format: " + uri);
         return false;
      }
      *pDoc = findOpenDocument(*pPath);
   }
   return true;
}

// Send a save_document editor command to trigger a frontend save, which clears
// the dirty indicator and persists the buffer to disk.
void sendSaveDocumentCommand(const std::string& docId)
{
   json::Object eventData;
   eventData["type"] = "save_document";
   json::Object data;
   data["id"] = docId;
   eventData["data"] = data;
   ClientEvent event(client_events::kEditorCommand, eventData);
   module_context::enqueClientEvent(event);
}

// Save a dirty document's buffer contents to disk and update the source
// database, then notify the frontend to clear its dirty indicator via a
// "save_document" editor command. This mirrors the essential subset of
// SessionSource.cpp's saveDocumentCore for same-path saves.
Error saveDocumentToDisk(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   DLOG("Saving document to disk: {}", pDoc->path());

   std::string path = pDoc->path();
   FilePath fullDocPath = module_context::resolveAliasedPath(path);

   // Convert from UTF-8 to the document's encoding
   std::string encoded;
   Error error = r::util::iconvstr(pDoc->contents(),
                                   "UTF-8",
                                   pDoc->encoding(),
                                   false,
                                   &encoded);
   if (error)
   {
      // Retry with substitution on encoding failure
      error = r::util::iconvstr(pDoc->contents(),
                                "UTF-8",
                                pDoc->encoding(),
                                true,
                                &encoded);
      if (error)
         return error;

      module_context::consoleWriteError(
                       "Not all of the characters in " + path +
                       " could be encoded using " + pDoc->encoding() +
                       ". To save using a different encoding, choose \"File | "
                       "Save with Encoding...\" from the main menu.\n");
   }

   // Note whether the file existed prior to writing
   bool newFile = !fullDocPath.exists();

   // Write the encoded contents to disk
   error = core::writeStringToFile(fullDocPath, encoded,
                                   module_context::lineEndings(fullDocPath));
   if (error)
      return error;

   // Save the original UTF-8 contents before setPathAndContents replaces
   // them with what was read back from disk (which may differ after encoding
   // round-trip, e.g. substitution characters in non-UTF-8 files).
   std::string contents = pDoc->contents();

   // Update the source database entry's hash/timestamps
   error = pDoc->setPathAndContents(path);
   if (error)
      return error;

   // Mark the document as clean in the source database
   pDoc->setDirty(false);

   // Restore the original UTF-8 editor buffer so the source database stays
   // in sync with what the user sees in the editor (mirrors saveDocumentCore).
   pDoc->setContents(contents);

   // Enqueue file changed event if the directory isn't already monitored
   if (!module_context::isDirectoryMonitored(fullDocPath.getParent()))
   {
      using core::system::FileChangeEvent;
      FileChangeEvent changeEvent(newFile ? FileChangeEvent::FileAdded :
                                            FileChangeEvent::FileModified,
                                  FileInfo(fullDocPath));
      module_context::enqueFileChangedEvent(changeEvent);
   }

   // Notify other server modules of the file save
   module_context::events().onSourceEditorFileSaved(fullDocPath);

   // Persist to the source database and notify listeners
   error = source_database::put(pDoc);
   if (error)
      return error;

   source_database::events().onDocUpdated(pDoc);

   // Tell the frontend to save the document so it clears its dirty indicator.
   sendSaveDocumentCommand(pDoc->id());

   return Success();
}

// Read file content from editor buffer if open, otherwise from disk
// Handles aliased paths (~/file.R), symlinks, project-relative paths, and untitled documents
void handleReadFileContent(core::system::ProcessOperations& ops,
                           const json::Value& requestId,
                           const json::Object& params)
{
   DLOG("Handling workspace/readFileContent request");

   // Extract URI parameter
   std::string uri;
   Error error = json::readObject(params, "uri", uri);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: uri required");
      return;
   }

   boost::shared_ptr<source_database::SourceDocument> doc;
   std::string path;

   if (!resolveDocumentUri(ops, requestId, uri, &doc, &path))
      return;

   if (!doc)
   {
      // File not open in editor - read from disk
      std::string content;
      error = core::readStringFromFile(FilePath(path), &content);

      if (error)
      {
         if (error.getCode() == boost::system::errc::no_such_file_or_directory)
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "File not found: " + path);
         }
         else if (error.getCode() == boost::system::errc::permission_denied)
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError, "Permission denied: " + path);
         }
         else
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError, "Failed to read file: " + path);
         }
         return;
      }

      json::Object result;
      result["content"] = content;
      result["isModified"] = false;

      DLOG("Read file content from disk: {}", path);
      sendJsonRpcResponse(ops, requestId, result);
      return;
   }

   // For dirty file-backed documents, save to disk so the on-disk file matches
   // the editor buffer before we report the contents to the chat backend.
   if (!doc->isUntitled() && doc->dirty())
   {
      Error saveError = saveDocumentToDisk(doc);
      if (saveError)
      {
         WLOG("Failed to save dirty document '{}' before read: {}",
              doc->path(), saveError.getMessage());
         sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
            "Cannot read file: the document has unsaved changes that "
            "could not be saved first. Error: " + saveError.getMessage());
         return;
      }
   }

   // Common logic for open documents (both untitled and file-based)
   json::Object result;
   result["content"] = doc->contents();
   result["isModified"] = doc->dirty();
   result["languageId"] = languageIdFromDocument(doc);

   // Set fileName for display purposes
   if (doc->isUntitled())
   {
      // Use tempName property if available (matches UI display like "Untitled1")
      result["tempName"] = doc->getProperty("tempName");
   }

   DLOG("Read file content from editor buffer (modified: {})", doc->dirty());

   sendJsonRpcResponse(ops, requestId, result);
}

// Write content to editor buffer if open, otherwise to disk
// Handles aliased paths (~/file.R), symlinks, project-relative paths, and untitled documents
void handleWriteFileContent(core::system::ProcessOperations& ops,
                            const json::Value& requestId,
                            const json::Object& params)
{
   DLOG("Handling workspace/writeFileContent request");

   // Extract parameters
   std::string uri;
   std::string content;
   Error error = json::readObject(params, "uri", uri);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: uri required");
      return;
   }

   error = json::readObject(params, "content", content);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: content required");
      return;
   }

   boost::shared_ptr<source_database::SourceDocument> doc;
   std::string path;

   if (!resolveDocumentUri(ops, requestId, uri, &doc, &path))
      return;

   json::Object result;

   if (doc)
   {
      // Compute replace range from OLD content (before mutation)
      std::string oldContent = doc->contents();
      std::vector<std::string> oldLines;
      boost::split(oldLines, oldContent, boost::is_any_of("\n"));
      int lastRow = static_cast<int>(oldLines.size()) - 1;
      int lastCol = oldLines.empty()
         ? 0
         : static_cast<int>(oldLines.back().length());

      // Build replace_ranges event (shared by both paths)
      json::Object replaceData;
      replaceData["id"] = doc->id();

      json::Array ranges;
      json::Array range;
      range.push_back(0);
      range.push_back(0);
      range.push_back(lastRow);
      range.push_back(lastCol);
      ranges.push_back(range);
      replaceData["ranges"] = ranges;

      json::Array text;
      text.push_back(content);
      replaceData["text"] = text;

      json::Object replaceEventData;
      replaceEventData["type"] = "replace_ranges";
      replaceEventData["data"] = replaceData;

      ClientEvent replaceEvent(
         client_events::kEditorCommand, replaceEventData);

      if (!doc->isUntitled())
      {
         // File-backed document: update buffer, sync to disk,
         // and clear dirty flag via saveDocumentToDisk.
         doc->setContents(content);

         // Enqueue replace_ranges first so the editor buffer
         // updates before the save_document command arrives.
         module_context::enqueClientEvent(replaceEvent);

         Error saveErr = saveDocumentToDisk(doc);
         if (saveErr)
         {
            // Editor already has new content; persist as dirty
            // so the source database matches what the user sees.
            doc->setDirty(true);
            Error putErr = source_database::put(doc);
            if (putErr)
            {
               LOG_ERROR(putErr);
               sendJsonRpcError(
                  ops, requestId, kJsonRpcInternalError,
                  "Failed to save to disk: " + saveErr.getMessage() +
                     "; failed to update source database: " +
                     putErr.getMessage());
               return;
            }
            source_database::events().onDocUpdated(doc);
            sendJsonRpcError(
               ops, requestId, kJsonRpcInternalError,
               "Failed to save to disk: " +
                  saveErr.getMessage());
            return;
         }
      }
      else
      {
         // Untitled document: no backing file to write.
         doc->setContents(content);
         doc->setDirty(true);

         Error putErr = source_database::put(doc);
         if (putErr)
         {
            LOG_ERROR(putErr);
            sendJsonRpcError(
               ops, requestId, kJsonRpcInternalError,
               "Failed to update source database: " +
                  putErr.getMessage());
            return;
         }

         source_database::events().onDocUpdated(doc);
         module_context::enqueClientEvent(replaceEvent);
      }

      result["success"] = true;
      if (doc->isUntitled())
         result["tempName"] = doc->getProperty("tempName");

      DLOG("Sent editor command to replace document content");
   }
   else
   {
      // File not open - write to disk (only for file:// URIs)
      if (path.empty())
      {
         // This shouldn't happen (untitled docs are always open or error earlier)
         sendJsonRpcError(ops, requestId, kJsonRpcInternalError, "Cannot write to disk: not a file URI");
         return;
      }

      error = core::writeStringToFile(FilePath(path), content);

      if (error)
      {
         if (error.getCode() == boost::system::errc::permission_denied)
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError, "Permission denied: " + path);
         }
         else if (error.getCode() == boost::system::errc::no_such_file_or_directory)
         {
            // Parent directory doesn't exist
            FilePath filePath(path);
            std::string parentDir = filePath.getParent().getAbsolutePath();
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
                           "Directory does not exist: " + parentDir);
         }
         else
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError, "Failed to write file: " + path);
         }
         return;
      }

      result["success"] = true;

      DLOG("Wrote file content to disk: {}", path);
   }

   sendJsonRpcResponse(ops, requestId, result);
}

// Find all non-overlapping byte offsets of 'needle' in 'haystack'.
// Returns offsets in ascending order.
std::vector<std::size_t> findAllOccurrences(
   const std::string& haystack,
   const std::string& needle)
{
   std::vector<std::size_t> offsets;
   std::size_t pos = 0;
   while (pos < haystack.size())
   {
      pos = haystack.find(needle, pos);
      if (pos == std::string::npos)
         break;
      offsets.push_back(pos);
      pos += needle.size();
   }
   return offsets;
}

// Convert a byte offset within LF-normalized UTF-8 content to a 0-based
// (row, col) pair. Column is measured in Unicode code points (matching
// Ace editor's character-based positioning) rather than bytes.
std::pair<int, int> offsetToRowCol(
   const std::string& content,
   std::size_t offset)
{
   int row = 0;
   std::size_t lineStart = 0;
   for (std::size_t i = 0; i < offset && i < content.size(); ++i)
   {
      if (content[i] == '\n')
      {
         ++row;
         lineStart = i + 1;
      }
   }
   std::string linePrefix = content.substr(lineStart, offset - lineStart);
   size_t col = 0;
   Error error = string_utils::utf8Distance(
      linePrefix.begin(), linePrefix.end(), &col);
   if (error)
   {
      // Fall back to byte length for invalid UTF-8
      col = linePrefix.size();
   }
   return std::make_pair(row, static_cast<int>(col));
}

// Edit content in editor buffer if open, otherwise on disk.
// Performs targeted search-and-replace with undo support.
void handleEditFileContent(core::system::ProcessOperations& ops,
                           const json::Value& requestId,
                           const json::Object& params)
{
   DLOG("Handling workspace/editFileContent request");

   // Extract required parameters
   std::string uri;
   Error error = json::readObject(params, "uri", uri);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                       "Invalid params: uri required");
      return;
   }

   std::string oldString;
   error = json::readObject(params, "oldString", oldString);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                       "Invalid params: oldString required");
      return;
   }

   std::string newString;
   error = json::readObject(params, "newString", newString);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                       "Invalid params: newString required");
      return;
   }

   if (oldString.empty())
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                       "Invalid params: oldString must not be empty");
      return;
   }

   // Optional replaceAll (default false)
   bool replaceAll = false;
   json::readObject(params, "replaceAll", replaceAll);

   // Resolve the URI to an open document and/or a file path
   boost::shared_ptr<source_database::SourceDocument> doc;
   std::string path;

   if (!resolveDocumentUri(ops, requestId, uri, &doc, &path))
      return;

   // Label used in error messages
   std::string label = path.empty() ? uri : path;

   json::Object result;

   if (doc)
   {
      // Document is open in the editor
      std::string content = doc->contents();

      std::vector<std::size_t> offsets =
         findAllOccurrences(content, oldString);

      if (offsets.empty())
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                          "oldString not found in file: " + label);
         return;
      }

      if (offsets.size() > 1 && !replaceAll)
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
            "oldString is ambiguous: found " +
            std::to_string(offsets.size()) +
            " occurrences. Set replaceAll to true or provide "
            "a more specific string.");
         return;
      }

      int occurrences = static_cast<int>(offsets.size());

      // Build replacement ranges (for the editor event)
      json::Array ranges;
      for (std::size_t i = 0; i < offsets.size(); ++i)
      {
         auto start = offsetToRowCol(content, offsets[i]);
         auto end = offsetToRowCol(
            content, offsets[i] + oldString.size());

         json::Array range;
         range.push_back(start.first);
         range.push_back(start.second);
         range.push_back(end.first);
         range.push_back(end.second);
         ranges.push_back(range);
      }

      // Build new content by replacing in reverse offset order
      // so earlier offsets stay valid
      std::string newContent = content;
      for (int i = static_cast<int>(offsets.size()) - 1; i >= 0; --i)
      {
         newContent.replace(
            offsets[i], oldString.size(), newString);
      }

      // Build the replace_ranges editor event
      json::Object replaceData;
      replaceData["id"] = doc->id();
      replaceData["ranges"] = ranges;

      json::Array text;
      text.push_back(newString);
      replaceData["text"] = text;

      json::Object replaceEventData;
      replaceEventData["type"] = "replace_ranges";
      replaceEventData["data"] = replaceData;

      ClientEvent replaceEvent(
         client_events::kEditorCommand, replaceEventData);

      if (!doc->isUntitled())
      {
         // File-backed document: update buffer, sync to disk,
         // and clear dirty flag via saveDocumentToDisk.
         doc->setContents(newContent);

         // Enqueue replace_ranges first so the editor buffer
         // updates before the save_document command arrives.
         module_context::enqueClientEvent(replaceEvent);

         Error saveErr = saveDocumentToDisk(doc);
         if (saveErr)
         {
            // Editor already has new content; persist as dirty
            // so the source database matches what the user sees.
            doc->setDirty(true);
            Error putErr = source_database::put(doc);
            if (putErr)
            {
               LOG_ERROR(putErr);
               sendJsonRpcError(
                  ops, requestId, kJsonRpcInternalError,
                  "Failed to save to disk: " +
                     saveErr.getMessage() +
                     "; failed to update source database: " +
                     putErr.getMessage());
               return;
            }
            source_database::events().onDocUpdated(doc);
            sendJsonRpcError(
               ops, requestId, kJsonRpcInternalError,
               "Failed to save to disk: " +
                  saveErr.getMessage());
            return;
         }
      }
      else
      {
         // Untitled document: no backing file to write.
         doc->setContents(newContent);
         doc->setDirty(true);

         Error putErr = source_database::put(doc);
         if (putErr)
         {
            LOG_ERROR(putErr);
            sendJsonRpcError(
               ops, requestId, kJsonRpcInternalError,
               "Failed to update source database: " +
                  putErr.getMessage());
            return;
         }

         source_database::events().onDocUpdated(doc);
         module_context::enqueClientEvent(replaceEvent);
      }

      result["success"] = true;
      result["occurrences"] = occurrences;
      if (doc->isUntitled())
         result["tempName"] = doc->getProperty("tempName");

      DLOG("Edited document content ({} occurrences replaced)", occurrences);
   }
   else
   {
      // File not open in editor - edit on disk
      if (path.empty())
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
                          "Cannot edit: not a file URI");
         return;
      }

      std::string content;
      error = core::readStringFromFile(
         FilePath(path), &content,
         string_utils::LineEndingPosix);

      if (error)
      {
         if (error.getCode() ==
             boost::system::errc::no_such_file_or_directory)
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                             "File not found: " + path);
         }
         else if (error.getCode() ==
                  boost::system::errc::permission_denied)
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
                             "Permission denied: " + path);
         }
         else
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
                             "Failed to read file: " + path);
         }
         return;
      }

      std::vector<std::size_t> offsets =
         findAllOccurrences(content, oldString);

      if (offsets.empty())
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
                          "oldString not found in file: " + path);
         return;
      }

      if (offsets.size() > 1 && !replaceAll)
      {
         sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams,
            "oldString is ambiguous: found " +
            std::to_string(offsets.size()) +
            " occurrences. Set replaceAll to true or provide "
            "a more specific string.");
         return;
      }

      int occurrences = static_cast<int>(offsets.size());

      // Build new content by replacing in reverse offset order
      std::string newContent = content;
      for (int i = static_cast<int>(offsets.size()) - 1; i >= 0; --i)
      {
         newContent.replace(
            offsets[i], oldString.size(), newString);
      }

      // Write back with original line ending style
      error = core::writeStringToFile(
         FilePath(path), newContent,
         module_context::lineEndings(FilePath(path)));

      if (error)
      {
         if (error.getCode() ==
             boost::system::errc::permission_denied)
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
                             "Permission denied: " + path);
         }
         else if (error.getCode() ==
                  boost::system::errc::no_such_file_or_directory)
         {
            FilePath filePath(path);
            std::string parentDir =
               filePath.getParent().getAbsolutePath();
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
                             "Directory does not exist: " + parentDir);
         }
         else
         {
            sendJsonRpcError(ops, requestId, kJsonRpcInternalError,
                             "Failed to write file: " + path);
         }
         return;
      }

      result["success"] = true;
      result["occurrences"] = occurrences;

      DLOG("Edited file on disk ({} occurrences replaced): {}",
           occurrences, path);
   }

   sendJsonRpcResponse(ops, requestId, result);
}

void handleInsertIntoNewFile(core::system::ProcessOperations& ops,
                             const json::Value& requestId,
                             const json::Object& params)
{
   DLOG("Handling workspace/insertIntoNewFile request");

   // Extract parameters
   std::string languageId;
   std::string content;

   Error error = json::readObject(params, "languageId", languageId);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: languageId required");
      return;
   }

   error = json::readObject(params, "content", content);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: content required");
      return;
   }

   // Map languageId to RStudio document type
   std::string documentType = documentTypeFromLanguageId(languageId);

   // Build event data for kNewDocumentWithCode
   json::Object eventData;
   eventData["type"] = documentType;
   eventData["code"] = content;
   eventData["row"] = 0;      // Position cursor at start
   eventData["column"] = 0;
   eventData["execute"] = false;  // Don't auto-execute

   // Fire client event to create new document
   ClientEvent event(client_events::kNewDocumentWithCode, eventData);
   module_context::enqueClientEvent(event);

   // Return success
   json::Object result;
   result["success"] = true;

   DLOG("Created new {} document with {} bytes of content", documentType, content.size());
   sendJsonRpcResponse(ops, requestId, result);
}

void handleInsertAtCursor(core::system::ProcessOperations& ops,
                          const json::Value& requestId,
                          const json::Object& params)
{
   DLOG("Handling workspace/insertAtCursor request");

   // Extract content parameter
   std::string content;
   Error error = json::readObject(params, "content", content);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: content required");
      return;
   }

   // Check if we have a focused document
   if (s_focusedDocumentId.empty())
   {
      DLOG("No focused document for insertAtCursor");
      json::Object result;
      result["success"] = false;
      sendJsonRpcResponse(ops, requestId, result);
      return;
   }

   // Verify the document still exists in the source database
   auto pDoc = boost::make_shared<source_database::SourceDocument>();
   error = source_database::get(s_focusedDocumentId, pDoc);
   if (error)
   {
      DLOG("Focused document {} no longer exists", s_focusedDocumentId);
      // Clear the stale ID
      s_focusedDocumentId.clear();
      json::Object result;
      result["success"] = false;
      sendJsonRpcResponse(ops, requestId, result);
      return;
   }

   // Validate document is a text-editable type (not a data viewer, profiler, etc.)
   // Non-text document types don't have text editors that support cursor insertion.
   // These types use non-text EditingTargets on the GWT side (e.g., DataEditingTarget,
   // ProfilerEditingTarget) which don't have DocDisplay/insertCode support.
   // The type IDs match the FileType.getTypeId() values from GWT FileType classes.
   std::string docType = pDoc->type();
   if (docType == "r_dataframe" ||     // DataFrameType - data viewer (View())
       docType == "r_prof" ||           // ProfilerType - profiler results
       docType == "object_explorer" ||  // ObjectExplorerFileType - object explorer
       docType == "urlcontent" ||       // UrlContentType - URL content viewer
       docType == "r_code_browser")     // CodeBrowserType - read-only code browser
   {
      DLOG("Document {} is a non-text type ({}), cannot insert at cursor",
           s_focusedDocumentId, docType);
      json::Object result;
      result["success"] = false;
      sendJsonRpcResponse(ops, requestId, result);
      return;
   }

   // Build editor command event data for insert_at_cursor
   json::Object eventData;
   eventData["type"] = "insert_at_cursor";

   json::Object data;
   data["id"] = s_focusedDocumentId;
   data["content"] = content;
   eventData["data"] = data;

   // Fire client event to insert at cursor
   ClientEvent event(client_events::kEditorCommand, eventData);
   module_context::enqueClientEvent(event);

   // Return success
   json::Object result;
   result["success"] = true;

   DLOG("Sent insert_at_cursor command for document {} with {} bytes",
        s_focusedDocumentId, content.size());
   sendJsonRpcResponse(ops, requestId, result);
}

void handleOpenDocument(core::system::ProcessOperations& ops,
                        const json::Value& requestId,
                        const json::Object& params)
{
   DLOG("Handling ui/openDocument request");

   // Extract path parameter
   std::string path;
   Error error = json::readObject(params, "path", path);
   if (error)
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid params: path required");
      return;
   }

   // Convert URI to path if needed (handles file:// URIs)
   std::string filePath = uriToPath(path);

   if (filePath.empty())
   {
      sendJsonRpcError(ops, requestId, kJsonRpcInvalidParams, "Invalid path: " + path);
      return;
   }

   // Resolve aliased paths (e.g., ~/file.R)
   FilePath resolvedPath = module_context::resolveAliasedPath(filePath);

   // Open the file in the editor
   module_context::editFile(resolvedPath);

   // Return success
   json::Object result;
   result["success"] = true;

   DLOG("Opened document: {}", resolvedPath.getAbsolutePath());
   sendJsonRpcResponse(ops, requestId, result);
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

   // Store the client version for later retrieval via chat_get_version RPC
   s_positAssistantVersion = clientVersion.empty() ? "unknown" : clientVersion;

   // Read optional capabilities from the peer
   json::Array peerCaps;
   Error error = json::readObject(params, "capabilities", peerCaps);
   if (!error && peerCaps.getSize() > 0)
   {
      s_peerSentCapabilities = true;
      s_peerCapabilities.clear();
      for (const json::Value& cap : peerCaps)
      {
         if (cap.isString())
            s_peerCapabilities.insert(cap.getString());
      }
      DLOG("Peer sent {} capabilities", s_peerCapabilities.size());
   }
   else
   {
      // Peer did not send capabilities -- assume full compatibility
      s_peerSentCapabilities = false;
      s_peerCapabilities.clear();
      DLOG("Peer did not send capabilities, assuming full compatibility");
   }

   // Build response with RStudio's own capabilities
   json::Object result;
   result["protocolVersion"] = kProtocolVersion;
   result["rstudioVersion"] = std::string(RSTUDIO_VERSION);

   const auto& caps = chat_constants::rstudioCapabilities();
   json::Array capsArray;
   for (const std::string& cap : caps)
   {
      capsArray.push_back(cap);
   }
   result["capabilities"] = capsArray;

   sendJsonRpcResponse(ops, requestId, result);
}

void handleGetConsoleContent(core::system::ProcessOperations& ops,
                             const json::Value& requestId,
                             const json::Object& params)
{
   DLOG("Handling runtime/getConsoleContent request");

   // Extract parameters with defaults
   int limit = 50;
   int offset = 0;
   bool fromBottom = true;
   int maxChars = 8000;

   json::readObject(params, "limit", limit);
   json::readObject(params, "offset", offset);
   json::readObject(params, "fromBottom", fromBottom);
   json::readObject(params, "maxChars", maxChars);

   // Get console lines from the console actions buffer
   std::vector<std::string> lines =
       r::session::consoleActions().getConsoleLines(limit, offset, fromBottom, maxChars);

   // Build response
   json::Array linesArray;
   for (const std::string& line : lines)
   {
      linesArray.push_back(line);
   }

   json::Object result;
   result["lines"] = linesArray;

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
   else if (method == "runtime/getConsoleContent")
   {
      handleGetConsoleContent(ops, requestId, params);
   }
   else if (method == "protocol/getVersion")
   {
      handleGetProtocolVersion(ops, requestId, params);
   }
   else if (method == "workspace/readFileContent")
   {
      handleReadFileContent(ops, requestId, params);
   }
   else if (method == "workspace/writeFileContent")
   {
      handleWriteFileContent(ops, requestId, params);
   }
   else if (method == "workspace/editFileContent")
   {
      handleEditFileContent(ops, requestId, params);
   }
   else if (method == "workspace/insertIntoNewFile")
   {
      handleInsertIntoNewFile(ops, requestId, params);
   }
   else if (method == "workspace/insertAtCursor")
   {
      handleInsertAtCursor(ops, requestId, params);
   }
   else if (method == "ui/openDocument")
   {
      handleOpenDocument(ops, requestId, params);
   }
   else
   {
      // Unknown method - send JSON-RPC error response
      WLOG("Unknown JSON-RPC request method: {}", method);
      sendJsonRpcError(ops, requestId, kJsonRpcMethodNotFound, "Method not found");
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
   if (getLogLevelPriority(level) < getLogLevelPriority(getBackendMinLogLevel()))
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

void handleUIShowMessage(const json::Object& params)
{
   std::string type;
   std::string message;

   if (json::readObject(params, "type", type))
   {
      WLOG("ui/showMessage notification missing 'type' field");
      return;
   }

   if (json::readObject(params, "message", message))
   {
      WLOG("ui/showMessage notification missing 'message' field");
      return;
   }

   // Map type string to MessageDisplay constants (MSG_INFO=1, MSG_WARNING=2, MSG_ERROR=3)
   int messageType;
   if (type == "info")
      messageType = 1;  // MSG_INFO
   else if (type == "warning")
      messageType = 2;  // MSG_WARNING
   else if (type == "error")
      messageType = 3;  // MSG_ERROR
   else
   {
      WLOG("ui/showMessage: unknown type '{}'", type);
      return;
   }

   // Show dialog with hardcoded caption per spec
   module_context::showMessage(messageType, "Posit Assistant", message);
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

void onProjectOptionsUpdated()
{
   // Update internal cache of project options
   if (projects::projectContext().hasProject())
   {
      Error error = projects::projectContext().readAssistantOptions(&s_chatProjectOptions);
      if (error)
         LOG_ERROR(error);
   }
}

void onBackgroundProcessing(bool isIdle)
{
   std::vector<PendingNotification> toProcess;

   // Phase 1: Extract notifications to process (hold lock briefly)
   {
      boost::mutex::scoped_lock lock(s_notificationQueueMutex);

      while (!s_notificationQueue.empty())
      {
         PendingNotification notif = s_notificationQueue.front();
         s_notificationQueue.pop();

         // Only send notifications for active executions
         if (s_activeTrackingIds.find(notif.trackingId) != s_activeTrackingIds.end())
         {
            toProcess.push_back(notif);
         }
         // Notifications for inactive executions are dropped (completed/canceled)
      }
   }

   // Phase 2: Send notifications (no lock held, no race condition)
   for (const auto& notif : toProcess)
   {
      // Check if ops still valid (backend may have died)
      auto ops = notif.weakOps.lock();
      if (ops)
      {
         sendStreamingOutput(*ops, notif.trackingId, notif.type, notif.content);
      }
      // If ops expired, notification is dropped (backend dead)
   }
}

// ============================================================================
// Update Management
// ============================================================================

// Structure to hold update check state
struct UpdateState
{
   bool updateAvailable;
   bool noCompatibleVersion;
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
        noCompatibleVersion(false),
        installStatus(Status::Idle)
   {
   }
};

// Global update state
UpdateState s_updateState;
boost::mutex s_updateStateMutex;

// Check if we should skip the update check due to throttling
// Returns true if we should skip (recently checked with same RStudio version)
//
// NOTE: During development this delay is causing confusion. For now we will record
// the update check time but skip the throttling so updates are checked every time.
bool shouldSkipUpdateCheck()
{
   json::Object positAssistantState = prefs::userState().positAssistant();

   // Check if RStudio version has changed since last check
   auto versionIt = positAssistantState.find(kPositAssistantRstudioVersionChecked);
   if (versionIt == positAssistantState.end())
   {
      DLOG("No previous RStudio version recorded, will check for updates");
      return false;
   }

   std::string lastVersion = (*versionIt).getValue().getString();
   if (lastVersion.empty() || lastVersion != std::string(RSTUDIO_VERSION))
   {
      DLOG("RStudio version changed ({} -> {}), will check for updates",
           lastVersion, RSTUDIO_VERSION);
      return false;
   }

   // Check timestamp of last update check
   auto timestampIt = positAssistantState.find(kPositAssistantLastUpdateCheck);
   if (timestampIt == positAssistantState.end())
   {
      DLOG("No previous update check timestamp, will check for updates");
      return false;
   }

   std::string lastCheckStr = (*timestampIt).getValue().getString();
   if (lastCheckStr.empty())
   {
      DLOG("Empty update check timestamp, will check for updates");
      return false;
   }

   return false;  // Temporarily disable throttling during development

   // FUTURE: Re-enable throttling after testing
   // Parse the timestamp and check if an hour has passed
   // try
   // {
   //    boost::posix_time::ptime lastCheck =
   //       boost::posix_time::from_iso_string(lastCheckStr);
   //    boost::posix_time::ptime now =
   //       boost::posix_time::second_clock::universal_time();

   //    boost::posix_time::time_duration elapsed = now - lastCheck;

   //    // Handle clock skew: if elapsed time is negative, the stored timestamp
   //    // is in the future. Don't skip in this case.
   //    if (elapsed.is_negative())
   //    {
   //       DLOG("Update check timestamp is in the future (clock skew?), will check for updates");
   //       return false;
   //    }

   //    // Skip if less than 10 minutes has passed
   //    if (elapsed.total_seconds() < 600)
   //    {
   //       DLOG("Update check throttled: only {} minutes since last check",
   //            elapsed.total_seconds() / 60);
   //       return true;
   //    }

   //    DLOG("Over 10 minutes since last update check, will check for updates");
   //    return false;
   // }
   // catch (const std::exception& e)
   // {
   //    WLOG("Failed to parse update check timestamp '{}': {}",
   //         lastCheckStr, e.what());
   //    return false;
   // }
}

// Save the update check state (timestamp and RStudio version)
void saveUpdateCheckState()
{
   json::Object positAssistantState = prefs::userState().positAssistant();

   // Store current UTC time as ISO string
   boost::posix_time::ptime now =
      boost::posix_time::second_clock::universal_time();
   positAssistantState[kPositAssistantLastUpdateCheck] =
      boost::posix_time::to_iso_string(now);

   // Store current RStudio version
   positAssistantState[kPositAssistantRstudioVersionChecked] =
      std::string(RSTUDIO_VERSION);

   Error error = prefs::userState().setPositAssistant(positAssistantState);
   if (error)
   {
      WLOG("Failed to save update check state: {}", error.getMessage());
   }
   else
   {
      DLOG("Saved update check state: timestamp={}, version={}",
           boost::posix_time::to_iso_string(now), RSTUDIO_VERSION);
   }
}

// Validate that a URL uses HTTPS protocol
bool isHttpsUrl(const std::string& url)
{
   return boost::starts_with(url, "https://");
}

Error downloadManifest(json::Object* pManifest)
{
   if (!pManifest)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

#ifndef NDEBUG
   // DEBUG builds only: Support local manifest file for testing
   std::string debugManifestPath = core::system::getenv("RSTUDIO_CHAT_DEBUG_MANIFEST");
   if (!debugManifestPath.empty())
   {
      // Validate path ends with manifest.json
      if (!boost::algorithm::ends_with(debugManifestPath, "manifest.json"))
      {
         WLOG("RSTUDIO_CHAT_DEBUG_MANIFEST must end with 'manifest.json', ignoring: {}",
              debugManifestPath);
      }
      else
      {
         FilePath debugManifestFile(debugManifestPath);
         if (debugManifestFile.exists())
         {
            DLOG("DEBUG: Using local manifest file: {}", debugManifestPath);

            // Read and parse JSON from local file
            std::string manifestContent;
            Error error = core::readStringFromFile(debugManifestFile, &manifestContent);
            if (error)
            {
               WLOG("Failed to read debug manifest file: {}", error.getMessage());
               return error;
            }

            // Parse JSON
            json::Value manifestValue;
            if (manifestValue.parse(manifestContent))
            {
               WLOG("Failed to parse debug manifest JSON");
               return systemError(boost::system::errc::protocol_error,
                                 "Invalid JSON in debug manifest",
                                 ERROR_LOCATION);
            }

            if (!manifestValue.isObject())
            {
               return systemError(boost::system::errc::protocol_error,
                                 "Debug manifest must be a JSON object",
                                 ERROR_LOCATION);
            }

            *pManifest = manifestValue.getObject();
            DLOG("Successfully loaded and parsed debug manifest");
            return Success();
         }
         else
         {
            WLOG("RSTUDIO_CHAT_DEBUG_MANIFEST file does not exist, ignoring: {}", debugManifestPath);
         }
      }
   }
#endif

   // Get download URI via redirector
   std::string downloadUri = "https://www.rstudio.org/links/posit-assistant-manifest";

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
// Selects the highest minor version that matches the major version
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

   // Parse RStudio's protocol version to get major version
   SemanticVersion rstudioProtocol;
   if (!rstudioProtocol.parse(protocolVersion))
   {
      WLOG("Failed to parse RStudio protocol version: {}", protocolVersion);
      return systemError(boost::system::errc::invalid_argument,
                        "Invalid protocol version format",
                        ERROR_LOCATION);
   }

   DLOG("Looking for compatible protocols with major version {}", rstudioProtocol.major);

   // Find all compatible protocol versions (matching major version)
   // and select the highest minor version
   SemanticVersion bestProtocol;
   std::string bestPackageVersion;
   std::string bestDownloadUrl;
   bool foundCompatible = false;

   for (const auto& entry : versions)
   {
      std::string manifestProtocol = entry.getName();

      // Parse this manifest protocol version
      SemanticVersion manifestProtocolVer;
      if (!manifestProtocolVer.parse(manifestProtocol))
      {
         WLOG("Skipping manifest entry with invalid protocol version: {}", manifestProtocol);
         continue;
      }

      // Check if major version matches
      if (manifestProtocolVer.major != rstudioProtocol.major)
      {
         DLOG("Skipping protocol {} (major version mismatch)", manifestProtocol);
         continue;
      }

      // Extract version info for this protocol
      json::Value versionValue = entry.getValue();
      if (!versionValue.isObject())
      {
         WLOG("Skipping protocol {} (value is not an object)", manifestProtocol);
         continue;
      }

      json::Object versionInfo = versionValue.getObject();

      std::string packageVersion;
      std::string downloadUrl;

      error = json::readObject(versionInfo, "version", packageVersion);
      if (error)
      {
         WLOG("Skipping protocol {} (missing 'version' field)", manifestProtocol);
         continue;
      }

      error = json::readObject(versionInfo, "url", downloadUrl);
      if (error)
      {
         WLOG("Skipping protocol {} (missing 'url' field)", manifestProtocol);
         continue;
      }

      // Validate download URL is HTTPS
      if (!isHttpsUrl(downloadUrl))
      {
         WLOG("Skipping protocol {} (non-HTTPS URL: {})", manifestProtocol, downloadUrl);
         continue;
      }

      // Check if this is the best (highest) protocol version so far
      if (!foundCompatible || manifestProtocolVer > bestProtocol)
      {
         bestProtocol = manifestProtocolVer;
         bestPackageVersion = packageVersion;
         bestDownloadUrl = downloadUrl;
         foundCompatible = true;
         DLOG("Found compatible protocol {}.{} with package version {}",
              manifestProtocolVer.major, manifestProtocolVer.minor, packageVersion);
      }
   }

   if (!foundCompatible)
   {
      WLOG("No compatible protocol found in manifest for major version {}", rstudioProtocol.major);
      return systemError(boost::system::errc::protocol_not_supported,
                        "No compatible protocol version found in manifest",
                        ERROR_LOCATION);
   }

   *pPackageVersion = bestPackageVersion;
   *pDownloadUrl = bestDownloadUrl;

   DLOG("Selected best compatible protocol {}.{}: package version={}, url={}",
        bestProtocol.major, bestProtocol.minor, bestPackageVersion, bestDownloadUrl);

   return Success();
}

// Extract recommended RStudio version from manifest
// Returns Success() and populates output params if field is present and valid
// Returns error if field is missing or invalid (caller should handle gracefully)
Error getRecommendedRStudioVersion(
    const json::Object& manifest,
    std::string* pVersion,
    std::string* pUrl)
{
   if (!pVersion || !pUrl)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   // Look for "recommendedRStudioVersion" object
   json::Object versionObj;
   Error error = json::readObject(manifest, "recommendedRStudioVersion", versionObj);
   if (error)
   {
      // Field not present - this is expected for older manifests
      return error;
   }

   // Extract "version" and "url" fields
   std::string version, url;
   error = json::readObject(versionObj, "version", version, "url", url);
   if (error)
   {
      WLOG("recommendedRStudioVersion missing required fields: {}", error.getMessage());
      return error;
   }

   // Validate URL is HTTPS
   if (!isHttpsUrl(url))
   {
      WLOG("Rejecting recommendedRStudioVersion with non-HTTPS URL: {}", url);
      return systemError(boost::system::errc::protocol_error,
                        "recommendedRStudioVersion URL must use HTTPS",
                        ERROR_LOCATION);
   }

   *pVersion = version;
   *pUrl = url;

   DLOG("Found recommended RStudio version: {} at {}", version, url);
   return Success();
}

// Show warning bar about outdated RStudio version
void showRStudioVersionWarning(
    const std::string& recommendedVersion,
    const std::string& downloadUrl)
{
   json::Object msgJson;
   msgJson["severe"] = false;
   boost::format fmt(
      "A newer version of RStudio (%1%) is recommended for Posit AI. "
      "<a href=\"%2%\" target=\"_blank\" rel=\"noopener noreferrer\">Download the update</a>"
   );
   msgJson["message"] = boost::str(fmt %
      string_utils::htmlEscape(recommendedVersion, true) %
      string_utils::htmlEscape(downloadUrl, true));
   ClientEvent event(client_events::kShowWarningBar, msgJson);
   module_context::enqueClientEvent(event);
}

// Compare semantic versions and determine if installation is needed
// Returns true if versions differ, enabling both upgrades (available > installed)
// and downgrades (available < installed) when RStudio version changes
// Returns false if versions are identical or if either version fails to parse
bool shouldInstallVersion(
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

   return available != installed;
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

// Performs the actual update check logic (must be called with mutex NOT held)
// This populates s_updateState with current version info and available updates
void doUpdateCheck()
{
   DLOG("Performing update check");

   // Get installed version
   std::string installedVersion = getInstalledVersion();
   if (installedVersion.empty())
   {
      DLOG("No installation found, checking for initial install");
      installedVersion = "0.0.0";
   }

   {
      boost::mutex::scoped_lock lock(s_updateStateMutex);
      s_updateState.currentVersion = installedVersion;
   }

   // Download manifest (silent failure)
   json::Object manifest;
   Error error = downloadManifest(&manifest);
   if (error)
   {
      WLOG("Failed to download manifest: {}", error.getMessage());
      return;
   }

   // Get package info for our protocol version
   std::string packageVersion;
   std::string downloadUrl;
   error = getPackageInfoFromManifest(manifest, kProtocolVersion, &packageVersion, &downloadUrl);
   if (error)
   {
      WLOG("Failed to get package info from manifest: {}", error.getMessage());

      // Check if this is specifically a "protocol not found" error
      if (error.getCode() == boost::system::errc::protocol_not_supported)
      {
         boost::mutex::scoped_lock lock(s_updateStateMutex);
         // Only block if there's no installed version to fall back on
         if (installedVersion == "0.0.0")
         {
            s_updateState.noCompatibleVersion = true;
         }
         s_updateState.updateAvailable = false;
      }
      return;
   }

   // Compare versions - offer install if versions differ (upgrade or downgrade)
   boost::mutex::scoped_lock lock(s_updateStateMutex);
   if (shouldInstallVersion(installedVersion, packageVersion))
   {
      DLOG("Update available: {} -> {}", installedVersion, packageVersion);
      s_updateState.updateAvailable = true;
      s_updateState.newVersion = packageVersion;
      s_updateState.downloadUrl = downloadUrl;
   }
   else
   {
      DLOG("No update needed (installed: {}, available: {})", installedVersion, packageVersion);
      s_updateState.updateAvailable = false;
   }
}

// Called during session initialization to check for updates
Error checkForUpdatesOnStartup()
{
   if (!isPositAiWanted())
   {
      DLOG("Update check skipped: posit not selected for chat or assistant");
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

   // Check if we should skip due to throttling
   // Skip check if:
   // - Posit Assistant IS installed (version != "0.0.0")
   // - AND RStudio version hasn't changed
   // - AND less than 10 minutes since last check
   if (installedVersion != "0.0.0" && shouldSkipUpdateCheck())
   {
      DLOG("Update check skipped: throttled (checked within last 10 minutes)");
      return Success();
   }

   // Record that we attempted an update check (prevents hammering server on failures)
   saveUpdateCheckState();

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
      WLOG("Failed to get package info from manifest: {}", error.getMessage());
      WLOG("Error code: {}, Expected: {}", error.getCode(),
           static_cast<int>(boost::system::errc::protocol_not_supported));

      // Check if this is specifically a "protocol not found" error
      if (error.getCode() == boost::system::errc::protocol_not_supported)
      {
         // Protocol version not in manifest - only block if there's no installed
         // version to fall back on
         if (installedVersion == "0.0.0")
         {
            s_updateState.noCompatibleVersion = true;
         }
         s_updateState.updateAvailable = false;
      }

      // For other errors (network, parsing, etc), do silent failure as before
      return Success();
   }

   // Compare versions - offer install if versions differ (upgrade or downgrade)
   if (shouldInstallVersion(installedVersion, packageVersion))
   {
      // Determine if this is an upgrade or downgrade
      SemanticVersion installed, available;
      bool isDowngrade = false;

      // These parses should always succeed since shouldInstallVersion validated them
      if (installed.parse(installedVersion) && available.parse(packageVersion))
      {
         isDowngrade = (available < installed);
         DLOG("{} available: {} -> {}",
              isDowngrade ? "Downgrade" : "Update",
              installedVersion, packageVersion);
      }
      else
      {
         // Defensive: this shouldn't happen, but handle gracefully
         WLOG("Version re-parsing failed unexpectedly: {} -> {}",
              installedVersion, packageVersion);
         DLOG("Update available: {} -> {}", installedVersion, packageVersion);
      }

      s_updateState.updateAvailable = true;
      s_updateState.newVersion = packageVersion;
      s_updateState.downloadUrl = downloadUrl;
   }
   else
   {
      DLOG("No update needed (installed: {}, available: {})", installedVersion, packageVersion);
      s_updateState.updateAvailable = false;
   }

   // Check for recommended RStudio version
   std::string recommendedVersion, downloadPageUrl;
   Error versionError = getRecommendedRStudioVersion(manifest, &recommendedVersion, &downloadPageUrl);
   if (!versionError)
   {
      core::Version current(RSTUDIO_VERSION);
      core::Version recommended(recommendedVersion);

      // Validate version parsed successfully (non-empty with at least major version)
      if (recommended.empty())
      {
         WLOG("Failed to parse recommended RStudio version: {}", recommendedVersion);
      }
      else
      {
         std::string versionStr(RSTUDIO_VERSION);
         bool isDailyBuild = versionStr.find("-daily") != std::string::npos;
         bool isHourlyBuild = versionStr.find("-hourly") != std::string::npos;
         bool isPrereleaseBuild = isDailyBuild || isHourlyBuild;
         bool forceCheck = !core::system::getenv("RSTUDIO_FORCE_DEV_UPDATE_CHECK").empty();

         DLOG("RStudio version check: current={}, recommended={}, isPrerelease={}",
              RSTUDIO_VERSION, recommendedVersion, isPrereleaseBuild);

         // Skip if Posit Assistant not installed (user hasn't completed beta signup)
         if (installedVersion == "0.0.0")
         {
            DLOG("  Skipping version warning (Posit Assistant not installed)");
         }
         // Only check for prerelease builds (daily/hourly) unless overridden
         else if (!isPrereleaseBuild && !forceCheck)
         {
            DLOG("  Skipping version warning (release build)");
         }
         else if (current < recommended)
         {
            DLOG("  Showing version warning");
            showRStudioVersionWarning(recommendedVersion, downloadPageUrl);
         }
         else
         {
            DLOG("  No warning needed (version is current or newer)");
         }
      }
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

// Returns either:
// - Server mode: relative path (e.g., "/p/58fab3e4/ai-chat")
// - Desktop mode: absolute URL (e.g., "ws://127.0.0.1:1234/ai-chat")
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

      // Return RELATIVE path (client will construct absolute URL from window.location)
      // This ensures the WebSocket connection uses the same hostname as the page,
      // guaranteeing the port-token cookie is sent with the upgrade request.
      // Without this fix, accessing RStudio via IP address would fail because the
      // cookie domain wouldn't match the WebSocket URL's hostname.
      std::string wsPath = portmappedPath + "/ai-chat";
      DLOG("Server WebSocket path (relative): {}", wsPath);
      return wsPath;
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
   ILOG("Chat backend exited with code: {}", exitCode);

   // Clear chat backend busy state to prevent stuck suspension blocking
   if (s_chatBusy)
   {
      s_chatBusy = false;
      DLOG("Cleared chat backend busy state on process exit");
   }

   // Determine if this was an expected exit based on shutdown state
   bool cleanExit = (exitCode == 0);

   // Classify exit as crash or expected shutdown:
   // - Expected shutdown with exit code 0 = NOT a crash (silent)
   // - Unexpected exit with exit code 0 = IS a crash (backend died unexpectedly)
   // - Any non-zero exit code = IS a crash (error, version mismatch, etc.)
   bool crashed = !s_expectedShutdown || !cleanExit;

   if (s_expectedShutdown)
   {
      DLOG("Backend exited as expected: exitCode={}", exitCode);
   }
   else
   {
      WLOG("Backend exited unexpectedly: exitCode={}", exitCode);
   }

   // Clear expected shutdown state for next exit
   s_expectedShutdown = false;

   // Clear state
   s_chatBackendPid = -1;
   s_chatBackendPort = -1;
   s_backendOutputBuffer.clear();
   s_chatBackendOps.reset();
   s_peerSentCapabilities = false;
   s_peerCapabilities.clear();
   s_chatBackendRestartCount = kMaxRestartAttempts;

   // Notify client of backend exit (with correct crashed flag)
   json::Object exitData;
   exitData["exit_code"] = exitCode;
   exitData["crashed"] = crashed;
   module_context::enqueClientEvent(ClientEvent(
      client_events::kChatBackendExit,
      exitData
   ));
}

Error startChatBackend(bool resumeConversation)
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

   // Build command arguments
   // Use relative path so Node.js resolves modules from working directory
   std::vector<std::string> args;
   args.push_back(kServerScriptPath);
   args.push_back("-h");
   args.push_back("127.0.0.1");  // Explicitly bind to IPv4 to match client connection
   args.push_back("-p");
   args.push_back(boost::lexical_cast<std::string>(s_chatBackendPort));
   args.push_back("--json"); // Enable JSON-RPC mode
   args.push_back("--logger-type=file"); // Log to file instead of using rstudio logging
   args.push_back("--log-dir=" + log::LogOptions::defaultLogDirectory().getAbsolutePath());

   // Add workspace path argument
   FilePath workspacePath = dirs::getInitialWorkingDirectory();
   args.push_back("--workspace");
   args.push_back(workspacePath.getAbsolutePath());

   // Create storage base path: {XDG_DATA_HOME}/pai/
   FilePath storagePath = xdg::userDataDir().completePath("pai");
   error = storagePath.ensureDirectory();
   if (error)
      return(error);

   args.push_back("--storage");
   args.push_back(storagePath.getAbsolutePath());

   // Pass config file path (config is in pai/, but working dir is pai/bin/)
   FilePath configPath = storagePath.completePath("paconfig.json");
   args.push_back("--config");
   args.push_back(configPath.getAbsolutePath());

   // Generate a persistent ID for this workspace directory
   std::string workspacePathStr = workspacePath.getAbsolutePath();
   std::string workspaceId = session::projectToProjectId(
       module_context::userScratchPath(),
       FilePath(),  // No shared storage - use per-user workspace IDs
       workspacePathStr
   ).id();

   args.push_back("--workspace-id");
   args.push_back(workspaceId);

   // Add resume-conversation flag if resuming after suspend/restart
   if (resumeConversation)
   {
      args.push_back("--resume-conversation");
      DLOG("Passing --resume-conversation to backend");
   }

   // Set up environment
   core::system::Options environment;
   core::system::environment(&environment);

   // Enable Node.js proxy support for fetch().
   // When HTTP_PROXY / HTTPS_PROXY env vars are set (e.g. via ~/.Renviron),
   // this tells Node.js 22.21+ to route fetch() through the proxy.
   // See: https://github.com/nodejs/node/pull/57165
   core::system::setenv(&environment, "NODE_USE_ENV_PROXY", "1");

#ifdef _WIN32
   // On Windows, R sets HOME to the user's Documents directory rather than
   // %USERPROFILE%. Correct it so child processes (e.g. git) find their
   // expected config files.
   core::system::setHomeToUserProfile(&environment);
#endif

   // Set up callbacks
   core::system::ProcessCallbacks callbacks;
   callbacks.onStarted = [](core::system::ProcessOperations& ops) {
      s_chatBackendPid = ops.getPid();
      s_chatBackendOps = ops.shared_from_this();
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
      return error;
   }

   return Success();
}

// ============================================================================
// RPC Methods
// ============================================================================

Error chatDocFocused(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string documentId;
   json::Array selections;

   // Try new signature with selections first
   Error error = core::json::readParams(request.params, &documentId, &selections);
   if (error)
   {
      // Fall back to old signature without selections
      error = core::json::readParams(request.params, &documentId);
      if (error)
         return error;
      selections = json::Array();
   }

   s_focusedDocumentId = documentId;
   s_focusedDocumentSelections = selections;
   return Success();
}

Error chatVerifyInstalled(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   FilePath installation = locatePositAiInstallation();
   bool installed = !installation.isEmpty();

   json::Object result;
   result["installed"] = installed;
   if (installed)
   {
      result["version"] = getInstalledVersion();
   }
   pResponse->setResult(result);
   return Success();
}

Error chatStartBackend(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   Error error = startChatBackend(false);

   json::Object result;
   result["success"] = !error;
   if (error)
      result["error"] = error.getMessage();

   pResponse->setResult(result);
   return Success();
}

Error chatStopBackend(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   json::Object result;

   // Check if backend is running
   if (s_chatBackendPid == -1)
   {
      result["success"] = true;
      result["message"] = "Backend not running";
      pResponse->setResult(result);
      return Success();
   }

   // Capture shared_ptr atomically to avoid race conditions
   auto backendOps = s_chatBackendOps;

   // Request graceful shutdown if we have ProcessOperations
   if (backendOps)
   {
      const int SHUTDOWN_GRACE_PERIOD_MS = 1000;

      // Mark this as an expected shutdown to prevent crash notification
      s_expectedShutdown = true;

      requestBackendShutdown(*backendOps, "preference_change", SHUTDOWN_GRACE_PERIOD_MS);
      DLOG("Sent graceful shutdown request for preference change, waiting up to {}ms", SHUTDOWN_GRACE_PERIOD_MS);

      // Poll for backend exit with short intervals
      const int POLL_INTERVAL_MS = 50;
      int elapsed = 0;

      while (s_chatBackendPid != -1 && elapsed < SHUTDOWN_GRACE_PERIOD_MS)
      {
         module_context::onBackgroundProcessing(false);
         r::session::event_loop::processEvents();
         boost::this_thread::sleep(boost::posix_time::milliseconds(POLL_INTERVAL_MS));
         elapsed += POLL_INTERVAL_MS;
      }

      if (s_chatBackendPid != -1)
      {
         DLOG("Backend did not exit within grace period, force terminating");
      }
      else
      {
         DLOG("Backend exited gracefully after {}ms", elapsed);
      }
   }

   // Release ProcessOperations reference before force termination
   s_chatBackendOps.reset();

   // Force terminate if still running after grace period
   if (s_chatBackendPid != -1)
   {
      Error error = core::system::terminateProcess(s_chatBackendPid);
      if (error)
         LOG_ERROR(error);
   }

   s_chatBackendPid = -1;

   // Clear port
   s_chatBackendPort = -1;

   // Clear busy state
   s_chatBusy = false;
   s_backendOutputBuffer.clear();

   result["success"] = true;
   result["message"] = "Backend stopped";
   pResponse->setResult(result);
   return Success();
}

Error chatGetBackendUrl(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   json::Object result;

   // Rebuild URL on-demand to use current port token (which changes on page reload)
   std::string url;
   if (s_chatBackendPid != -1 && s_chatBackendPort != -1)
   {
      url = buildWebSocketUrl(s_chatBackendPort);
   }

   result["url"] = url;
   result["port"] = s_chatBackendPort;
   result["ready"] = (s_chatBackendPid != -1 && !url.empty());

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
   else if (s_chatBackendPort == -1)
   {
      result["status"] = "starting";
   }
   else
   {
      // Rebuild URL on-demand to use current port token (which changes on page reload)
      std::string url = buildWebSocketUrl(s_chatBackendPort);
      result["status"] = "ready";
      result["url"] = url;
      result["resume_chat"] = s_resumeChat;
   }

   pResponse->setResult(result);
   return Success();
}

Error chatNotifyUILoaded(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   DLOG("Chat UI loaded notification received, setting s_resumeChat = true");
   s_resumeChat = true;
   pResponse->setResult(json::Value());
   return Success();
}

Error chatGetVersion(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   // Return empty string if version not yet received (backend never started)
   // Return "unknown" if backend started but didn't provide version
   // Otherwise return the actual version string
   pResponse->setResult(s_positAssistantVersion);
   return Success();
}

Error chatCheckForUpdates(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // Perform on-demand update check if state hasn't been populated yet.
   // This happens when user selects Posit AI in Preferences before the pref is saved.
   // We allow the check regardless of isPositAiWanted() since checking for available
   // updates doesn't require the preference - only actual installation does.
   {
      boost::mutex::scoped_lock lock(s_updateStateMutex);
      if (s_updateState.currentVersion.empty())
      {
         DLOG("Update state not populated, performing on-demand check");
         lock.unlock();
         doUpdateCheck();
      }
   }

   boost::mutex::scoped_lock lock(s_updateStateMutex);

   // Return cached/computed check result
   json::Object result;
   result["updateAvailable"] = s_updateState.updateAvailable;
   result["noCompatibleVersion"] = s_updateState.noCompatibleVersion;
   result["currentVersion"] = s_updateState.currentVersion;
   result["newVersion"] = s_updateState.newVersion;
   result["downloadUrl"] = s_updateState.downloadUrl;
   result["isInitialInstall"] = (s_updateState.currentVersion == "0.0.0");

   DLOG("chatCheckForUpdates returning: updateAvailable={}, noCompatibleVersion={}",
        s_updateState.updateAvailable, s_updateState.noCompatibleVersion);

   pResponse->setResult(result);
   return Success();
}

Error chatInstallUpdate(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   if (!isPositAiWanted())
   {
      return systemError(boost::system::errc::operation_not_permitted,
                        "Posit AI not selected for chat or assistant",
                        ERROR_LOCATION);
   }

   // Check if we need to perform an update check first
   // This happens when the user selects Posit AI after session startup
   {
      boost::mutex::scoped_lock lock(s_updateStateMutex);
      if (s_updateState.currentVersion.empty())
      {
         DLOG("Update state not populated, performing on-demand check");
         lock.unlock();
         doUpdateCheck();
      }
   }

   boost::mutex::scoped_lock lock(s_updateStateMutex);

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
   }

   // Stop assistant agent (language server) if running - it also uses pai/bin/
   DLOG("Stopping assistant agent for update");
   if (!assistant::stopAgentForUpdate())
   {
      WLOG("Timeout waiting for assistant agent to stop");
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

      pResponse->setResult(json::Value());
      return Success();
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

#ifdef _WIN32
      if (error.getCode() == ERROR_ACCESS_DENIED ||
          error.getCode() == ERROR_SHARING_VIOLATION)
      {
         s_updateState.installMessage =
            "Unable to install update (access denied). "
            "Please close all other instances of RStudio and try again.";
      }
      else
#endif
      {
         s_updateState.installMessage = "Installation failed: " + error.getMessage();
      }

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

      pResponse->setResult(json::Value());
      return Success();
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

   if (!isPositAiWanted())
   {
      // Return idle status - posit not selected for chat or assistant
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
   DLOG("Session suspension starting - requesting graceful backend shutdown");

   // Persist whether backend was running before suspension
   bool wasRunning = (s_chatBackendPid != -1);
   pSettings->set("chat_suspended", wasRunning);

   // Persist whether to resume conversation
   // Resume if backend was running AND not closing (i.e., suspend or restart)
   bool shouldResumeConversation = wasRunning && !s_sessionClosing;
   pSettings->set("chat_resume_conversation", shouldResumeConversation);

   DLOG("Chat resume on next start: {}", shouldResumeConversation);

   // Reset flag for next cycle
   s_sessionClosing = false;

   // Request graceful shutdown and wait for backend to exit
   if (wasRunning)
   {
      // Capture shared_ptr atomically to avoid race conditions
      auto backendOps = s_chatBackendOps;

      // Request graceful shutdown if we have ProcessOperations
      if (backendOps)
      {
         // Use shorter grace period for suspend (0.5s instead of 1s)
         // to be more responsive while still allowing cleanup
         const int SUSPEND_GRACE_PERIOD_MS = 500;

         // Mark this as an expected shutdown to prevent crash notification
         s_expectedShutdown = true;

         requestBackendShutdown(*backendOps, "suspend", SUSPEND_GRACE_PERIOD_MS);
         DLOG("Sent graceful shutdown request, waiting up to {}ms", SUSPEND_GRACE_PERIOD_MS);

         // Poll for backend exit with short intervals
         // This allows event processing while still waiting for graceful shutdown
         const int POLL_INTERVAL_MS = 50;
         int elapsed = 0;

         while (s_chatBackendPid != -1 && elapsed < SUSPEND_GRACE_PERIOD_MS)
         {
            module_context::onBackgroundProcessing(false);
            r::session::event_loop::processEvents();
            boost::this_thread::sleep(boost::posix_time::milliseconds(POLL_INTERVAL_MS));
            elapsed += POLL_INTERVAL_MS;
         }

         if (s_chatBackendPid != -1)
         {
            DLOG("Backend did not exit within grace period, force terminating");
         }
         else
         {
            DLOG("Backend exited gracefully after {}ms", elapsed);
         }
      }

      // Release ProcessOperations reference before force termination
      s_chatBackendOps.reset();

      // Force terminate if still running after grace period
      // Note: s_expectedShutdown is already set, so onBackendExit() will
      // correctly treat this as an expected shutdown (not a crash)
      if (s_chatBackendPid != -1)
      {
         Error error = core::system::terminateProcess(s_chatBackendPid);
         if (error)
            LOG_ERROR(error);
      }

      // Clear state (rsession is exiting anyway)
      s_chatBackendPid = -1;
      s_chatBackendPort = -1;
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
   bool resumeConversation = settings.getBool("chat_resume_conversation", false);

   if (wasSuspended)
   {
      s_resumeChat = resumeConversation;
      DLOG("Restarting chat backend (resume conversation: {})", resumeConversation);

      Error error = startChatBackend(resumeConversation);
      if (error)
         LOG_ERROR(error);
   }
}

void onShutdown(bool terminatedNormally)
{
   // Clear busy state and JSON-RPC state early
   s_chatBusy = false;
   s_backendOutputBuffer.clear();

   // Request graceful shutdown and wait for backend to exit
   if (s_chatBackendPid != -1)
   {
      // Capture shared_ptr atomically to avoid race conditions
      auto backendOps = s_chatBackendOps;

      // Request graceful shutdown if we have ProcessOperations
      if (backendOps)
      {
         const int SHUTDOWN_GRACE_PERIOD_MS = 1000;

         // Mark this as an expected shutdown to prevent crash notification
         s_expectedShutdown = true;

         requestBackendShutdown(*backendOps, "close", SHUTDOWN_GRACE_PERIOD_MS);
         DLOG("Sent graceful shutdown request, waiting up to {}ms", SHUTDOWN_GRACE_PERIOD_MS);

         // Poll for backend exit with short intervals
         // This allows event processing while still waiting for graceful shutdown
         const int POLL_INTERVAL_MS = 50;
         int elapsed = 0;

         while (s_chatBackendPid != -1 && elapsed < SHUTDOWN_GRACE_PERIOD_MS)
         {
            module_context::onBackgroundProcessing(false);
            r::session::event_loop::processEvents();
            boost::this_thread::sleep(boost::posix_time::milliseconds(POLL_INTERVAL_MS));
            elapsed += POLL_INTERVAL_MS;
         }

         if (s_chatBackendPid != -1)
         {
            DLOG("Backend did not exit within grace period, force terminating");
         }
         else
         {
            DLOG("Backend exited gracefully after {}ms", elapsed);
         }
      }

      // Release ProcessOperations reference before force termination
      s_chatBackendOps.reset();

      // Force terminate if still running after grace period
      // Note: s_expectedShutdown is already set, so onBackendExit() will
      // correctly treat this as an expected shutdown (not a crash)
      if (s_chatBackendPid != -1)
      {
         Error error = core::system::terminateProcess(s_chatBackendPid);
         if (error)
            LOG_ERROR(error);
      }

      s_chatBackendPid = -1;
   }

   // Clear port
   s_chatBackendPort = -1;

   // Clear notification queue
   {
      boost::mutex::scoped_lock lock(s_notificationQueueMutex);
      while (!s_notificationQueue.empty())
         s_notificationQueue.pop();
      s_activeTrackingIds.clear();
   }
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
      setChatLogLevel(safe_convert::stringTo<int>(chatLogLevelStr, 0));

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
         setBackendMinLogLevel(backendMinLevel);
      }
      else
      {
         WLOG("Invalid CHAT_BACKEND_MIN_LEVEL value '{}', using default 'error'", backendMinLevel);
      }
   }

   // Read project options (must be done before validation to get accurate state)
   if (projects::projectContext().hasProject())
   {
      Error error = projects::projectContext().readAssistantOptions(&s_chatProjectOptions);
      if (error)
         LOG_ERROR(error);
   }

   // Validate assistant preference consistency
   // If user has Posit AI selected but PAI is no longer available, reset to "none"
   if (isPaiSelected() && !isPaiEnabled())
   {
      prefs::userPrefs().setAssistant(kAssistantNone);
   }

   // Validate chat provider preference consistency
   // If user has Posit selected as chat provider but PAI is no longer available, reset to "none"
   if (isChatProviderPosit() && !isPaiEnabled())
   {
      prefs::userPrefs().setChatProvider(kChatProviderNone);
   }

   RS_REGISTER_CALL_METHOD(rs_chatSetLogLevel);

   // Register JSON-RPC notification handlers
   registerNotificationHandler("logger/log", handleLoggerLog);
   registerNotificationHandler("ui/showMessage", handleUIShowMessage);
   registerNotificationHandler("chat/setBusyStatus", handleSetBusyStatus);
   registerNotificationHandler("runtime/cancelExecution", handleCancelExecution);

   // Register event handlers
   events().onBackgroundProcessing.connect(onBackgroundProcessing);
   events().onShutdown.connect(onShutdown);
   events().onProjectOptionsUpdated.connect(onProjectOptionsUpdated);

   // Register handler to detect session close (vs suspend/restart)
   events().onQuit.connect([]() {
      s_sessionClosing = true;
      DLOG("Session closing - backend will NOT resume conversation on next start");
   });

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
      (bind(registerRpcMethod, "chat_stop_backend", chatStopBackend))
      (bind(registerRpcMethod, "chat_get_backend_url", chatGetBackendUrl))
      (bind(registerRpcMethod, "chat_get_backend_status", chatGetBackendStatus))
      (bind(registerRpcMethod, "chat_get_version", chatGetVersion))
      (bind(registerRpcMethod, "chat_check_for_updates", chatCheckForUpdates))
      (bind(registerRpcMethod, "chat_install_update", chatInstallUpdate))
      (bind(registerRpcMethod, "chat_get_update_status", chatGetUpdateStatus))
      (bind(registerRpcMethod, "chat_doc_focused", chatDocFocused))
      (bind(registerRpcMethod, "chat_notify_ui_loaded", chatNotifyUILoaded))
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
