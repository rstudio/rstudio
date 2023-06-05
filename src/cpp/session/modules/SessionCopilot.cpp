/*
 * SessionCopilot.cpp
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

#include "SessionCopilot.hpp"

#include <boost/current_function.hpp>
#include <boost/range/adaptors.hpp>
#include <boost/range/algorithm/copy.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>
#include <core/http/Header.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Process.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <core/Thread.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>
#include <r/session/REventLoop.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

#define COPILOT_LOG_IMPL(__LOGGER__, __FMT__, ...)                             \
   do                                                                          \
   {                                                                           \
      std::string message = fmt::format(__FMT__, ##__VA_ARGS__);               \
      std::string formatted =                                                  \
          fmt::format("[{}]: {}", __func__, message);                          \
      __LOGGER__(formatted);                                                   \
      if (copilotLogLevel() > 0)                                               \
         std::cerr << formatted << std::endl;                                  \
   } while (0)

#define DLOG(__FMT__, ...) COPILOT_LOG_IMPL(LOG_DEBUG_MESSAGE,   __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...) COPILOT_LOG_IMPL(LOG_ERROR_MESSAGE,   __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...) COPILOT_LOG_IMPL(LOG_WARNING_MESSAGE, __FMT__, ##__VA_ARGS__)

#ifndef _WIN32
# define kNodeExe "node"
#else
# define kNodeExe "node.exe"
#endif

using namespace rstudio::core;
using namespace rstudio::core::system;

namespace rstudio {
namespace session {
namespace modules {
namespace copilot {

namespace {

class CopilotContinuation
{
public:

   // default ctor needed for compatibility with map
   CopilotContinuation()
   {
   }

   explicit CopilotContinuation(const json::JsonRpcFunctionContinuation& continuation)
      : continuation_(continuation),
        time_(boost::posix_time::second_clock::local_time())
   {
   }

   void invoke(json::Object resultJson)
   {
      resultJson["cancelled"] = false;

      json::JsonRpcResponse response;
      response.setResult(resultJson);

      if (continuation_)
         continuation_(Success(), &response);
   }

   void cancel()
   {
      json::Object resultJson;
      resultJson["cancelled"] = true;

      json::JsonRpcResponse response;
      response.setResult(resultJson);

      if (continuation_)
         continuation_(Success(), &response);
   }

   boost::posix_time::ptime time()
   {
      return time_;
   }

private:
   json::JsonRpcFunctionContinuation continuation_;
   boost::posix_time::ptime time_;
};

// The log level for Copilot-specific logs. Primarily for developer use.
int s_copilotLogLevel = 3;

// Whether Copilot is enabled.
bool s_copilotEnabled = false;

// The PID of the active Copilot agent process.
PidType s_agentPid = -1;

// A queue of pending requests, to be sent via the agent's stdin.
std::queue<std::string> s_pendingRequests;

// Metadata related to pending requests. Mainly used to map
// responses to their expected result types.
std::map<std::string, CopilotContinuation> s_pendingContinuations;

// A queue of pending responses, sent via the agent's stdout.
std::queue<std::string> s_pendingResponses;

// Whether we're about to shut down.
bool s_isSessionShuttingDown = false;

std::string uriFromDocumentId(const std::string& documentId)
{
   return fmt::format("rstudio-document://{}", documentId);
}

std::string languageIdFromDocument(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (pDoc->isRMarkdownDocument() || pDoc->isRFile())
      return "r";

   return boost::algorithm::to_lower_copy(pDoc->type());
}

template <typename F>
bool waitFor(F&& callback)
{
   for (int i = 0; i < 100; i++)
   {
      bool ready = callback();
      if (ready)
         return true;

      module_context::onBackgroundProcessing(false);
      r::session::event_loop::processEvents();
      boost::this_thread::sleep_for(boost::chrono::milliseconds(100));
   }

   return false;
}

Error findNode(FilePath* pNodePath,
               core::system::Options* pOptions)
{
   // Check for an admin-configured node path.
   FilePath nodePath = session::options().nodePath();
   if (!nodePath.isEmpty())
   {
      // Allow both directories containing a 'node' binary, and the path
      // to a 'node' binary directly.
      if (nodePath.isDirectory())
      {
         FilePath nodeExePath = nodePath.completeChildPath(kNodeExe);
         if (nodeExePath.exists())
         {
            *pNodePath = nodeExePath;
            return Success();
         }
         else
         {
            return Error(fileNotFoundError(nodeExePath, ERROR_LOCATION));
         }
      }
      else if (nodePath.isRegularFile())
      {
         *pNodePath = nodePath;
         return Success();
      }
      else
      {
         return Error(fileNotFoundError(nodePath, ERROR_LOCATION));
      }
   }

   // In Desktop builds of RStudio, we can re-use the version of node
   // bundled with Electron.
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      std::string desktopExePath = core::system::getenv("RSTUDIO_DESKTOP_EXE");
      if (!desktopExePath.empty() && FilePath(desktopExePath).exists())
      {
         *pNodePath = FilePath(desktopExePath);
         pOptions->push_back(std::make_pair("ELECTRON_RUN_AS_NODE", "1"));
         return Success();
      }
   }

   // Otherwise, use node from the PATH
   // TODO: We'll need to bundle a version of node with RStudio Server.
   // Quarto 1.4 will ship with 'deno', which will be able to run regular 'node' applications,
   // so maybe we can use that?
   return core::system::findProgramOnPath(kNodeExe, pNodePath);
}

int copilotLogLevel()
{
   return s_copilotLogLevel;
}

FilePath copilotAgentPath()
{
   // Check for configured copilot path.
   FilePath copilotPath = session::options().copilotPath();
   if (copilotPath.exists())
      return copilotPath.getParent();

   // Otherwise, use a default user location.
   return core::system::xdg::userCacheDir().completeChildPath("copilot/dist/agent.js");
}

bool isCopilotAgentInstalled()
{
   return copilotAgentPath().exists();
}

Error installCopilotAgent()
{
   return r::exec::RFunction(".rs.copilot.installCopilotAgent")
         .addParam(copilotAgentPath().getParent().getAbsolutePath())
         .call();
}

std::string createRequest(const std::string& method,
                          const std::string& id,
                          const json::Value& paramsJson)
{
   // Create the request body
   json::Object requestJson;
   requestJson["jsonrpc"] = "2.0";
   requestJson["method"] = method;
   requestJson["params"] = paramsJson;

   // Set id if available.
   // Requests without an id are 'notifications', and never receive a response.
   if (!id.empty())
      requestJson["id"] = id;

   // Convert to a JSON string
   std::string request = requestJson.write();

   // Convert into HTTP request with JSON payload in body
   return fmt::format("Content-Length: {}\r\n\r\n{}", request.size(), request);
}

void sendNotification(const std::string& method,
                      const json::Value& paramsJson)
{
   std::string request = createRequest(method, std::string(), paramsJson);
   s_pendingRequests.push(request);
}

void sendRequest(const std::string& method,
                 const std::string& requestId,
                 const json::Value& paramsJson,
                 const CopilotContinuation& continuation)
{
   // Add the continuation, which is executed in response to the request.
   s_pendingContinuations[requestId] = continuation;

   // Create and enqueue the request.
   std::string request = createRequest(method, requestId, paramsJson);
   s_pendingRequests.push(request);
}

namespace agent {

void onStarted(ProcessOperations& operations)
{
   // Record the PID of the agent.
   DLOG("Copilot agent has started [PID = {}]", operations.getPid());
   s_agentPid = operations.getPid();
}

bool onContinue(ProcessOperations& operations)
{
   // Send any pending requests over stdin.
   while (!s_pendingRequests.empty())
   {
      std::string request = s_pendingRequests.front();
      s_pendingRequests.pop();

      if (copilotLogLevel() > 2)
      {
         std::cerr << std::endl;
         std::cerr << "REQUEST" << std::endl;
         std::cerr << "----------------" << std::endl;
         std::cerr << request << std::endl;
         std::cerr << "----------------" << std::endl;
         std::cerr << std::endl << std::endl;
      }

      operations.writeToStdin(request, false);
   }

   return true;
}

void onStdout(ProcessOperations& operations, const std::string& stdOut)
{
   // Discard empty lines.
   static const boost::regex reWhitespace("^\\s*$");
   if (regex_utils::match(stdOut, reWhitespace))
      return;

   // Copilot responses will have the format
   //
   //    Content-Length: xyz
   //
   //    <body>
   //
   // Importantly, there is no character separating the body from the
   // next response, so we're forced to parse headers here.
   //
   // TODO: Does this need to be a state machine? Is it possible that we
   // receive some incomplete requests here?
   std::size_t startIndex = 0;

   while (startIndex < stdOut.length())
   {
      // Find the split.
      const char* splitText = "\r\n\r\n";
      const int splitSize = 4;

      auto splitIndex = stdOut.find(splitText, startIndex);
      if (splitIndex == std::string::npos)
      {
         ELOG("Internal error: parsing response failed.");
         break;
      }

      // Extract the header text.
      std::string headerText = string_utils::substring(stdOut, startIndex, splitIndex + splitSize);

      // Parse the headers.
      core::http::Headers headers;
      std::istringstream iss(headerText);
      core::http::parseHeaders(iss, &headers);

      // Check for content length.
      std::string contentLength = core::http::headerValue(headers, "Content-Length");
      if (contentLength.empty())
      {
         ELOG("Internal error: response contains no Content-Length header.");
         break;
      }

      // Consume that text.
      auto bodyStart = splitIndex + splitSize;
      auto bodyEnd = bodyStart + safe_convert::stringTo<int>(contentLength, 0);
      std::string bodyText = string_utils::substring(stdOut, bodyStart, bodyEnd);
      s_pendingResponses.push(bodyText);

      if (copilotLogLevel() > 2)
      {
         std::cerr << std::endl;
         std::cerr << "RESPONSE" << std::endl;
         std::cerr << "------------------" << std::endl;
         std::cerr << bodyText << std::endl;
         std::cerr << "------------------" << std::endl;
         std::cerr << std::endl << std::endl;
      }

      // Update the start index.
      startIndex = bodyEnd;
   }
}

void onStderr(ProcessOperations& operations, const std::string& stdErr)
{
   std::cerr << stdErr << std::endl;
}

void onExit(int status)
{
   s_agentPid = -1;

   if (s_isSessionShuttingDown)
      return;

   if (status != 0)
   {
      ELOG("Agent exited with status {}.", status);
   }
}

} // end namespace agent

void stopAgent()
{
   if (s_agentPid == -1)
   {
      DLOG("No agent running; nothing to do.");
      return;
   }

   Error error = core::system::terminateProcess(s_agentPid);
   if (error)
      LOG_ERROR(error);
}

bool startAgent()
{
   Error error;

   FilePath agentPath = copilotAgentPath();
   if (!agentPath.exists())
   {
      ELOG("Copilot agent not installed; cannot start agent.");
      return false;
   }

   // Create environment for agent process
   core::system::Options environment;
   core::system::environment(&environment);

   // For Desktop builds of RStudio, use the version of node embedded in Electron.
   FilePath nodePath;
   error = findNode(&nodePath, &environment);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   else if (!nodePath.exists())
   {
      LOG_ERROR(fileNotFoundError(ERROR_LOCATION));
      return false;
   }

   // Set up process callbacks
   core::system::ProcessCallbacks callbacks;
   callbacks.onStarted = &agent::onStarted;
   callbacks.onContinue = &agent::onContinue;
   callbacks.onStdout = &agent::onStdout;
   callbacks.onStderr = &agent::onStderr;
   callbacks.onExit = &agent::onExit;

   // Set up process options
   core::system::ProcessOptions options;
   options.environment = environment;
   options.allowParentSuspend = true;
   options.exitWithParent = true;
   options.callbacksRequireMainThread = true; // TODO: It'd be nice to drop this requirement!
   options.workingDir = agentPath.getParent();

   error = module_context::processSupervisor().runProgram(
            nodePath.getAbsolutePath(),
            { agentPath.getAbsolutePath() },
            options,
            callbacks);

   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // Wait for the process to start.
   //
   // TODO: This is kind of a hack. We should probably instead use something like a
   // status flag that tracks if the agent is stopped, starting, or already running.
   //
   // We include this because all requests will fail if we haven't yet called
   // initialized, so maybe the right approach is to have some sort of 'ensureInitialized'
   // method?
   waitFor([]() { return s_agentPid != -1; });
   if (s_agentPid == -1)
   {
      ELOG("Copilot agent failed to start.");
      return false;
   }

   // Send an initialize request to the agent.
   json::Object clientInfoJson;
   clientInfoJson["name"] = "RStudio";
   clientInfoJson["version"] = "1.0.0";

   json::Object paramsJson;
   paramsJson["processId"] = ::getpid();
   paramsJson["clientInfo"] = clientInfoJson;
   paramsJson["capabilities"] = json::Object();

   std::string requestId = core::system::generateUuid();
   sendRequest("initialize", requestId, paramsJson, CopilotContinuation());

   // Okay, we're ready to go.
   return true;
}

bool ensureAgentRunning()
{
   // bail if we haven't enabled copilot
   if (!s_copilotEnabled)
   {
      DLOG("Copilot is not enabled; not starting agent.");
      return false;
   }

   // bail if copilot is not allowed
   if (!session::options().allowCopilot())
   {
      DLOG("Copilot has been disabled by the administrator; not starting agent.");
      return false;
   }

   // bail if we're shutting down
   if (s_isSessionShuttingDown)
   {
      DLOG("Session is shutting down; not starting agent.");
      return false;
   }

   // TODO: Should we further validate the PID is actually associated
   // with a running Copilot process, or just handle that separately?
   if (s_agentPid != -1)
   {
      DLOG("Copilot is already running; nothing to do.");
      return true;
   }

   return startAgent();
}

void onDocAdded(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!ensureAgentRunning())
      return;

   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocumentId(pDoc->id());
   textDocumentJson["languageId"] = languageIdFromDocument(pDoc);
   textDocumentJson["version"] = 1;
   textDocumentJson["text"] = "";

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didOpen", paramsJson);
}

void onDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!ensureAgentRunning())
      return;

   // Synchronize document contents with Copilot
   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocumentId(pDoc->id());
   textDocumentJson["languageId"] = languageIdFromDocument(pDoc);
   textDocumentJson["version"] = 1;
   textDocumentJson["text"] = pDoc->contents();

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didOpen", paramsJson);
}

void onDocRemoved(const std::string& id, const std::string& path)
{
   if (!ensureAgentRunning())
      return;

   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocumentId(id);

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didClose", paramsJson);
}

void onBackgroundProcessing(bool isIdle)
{
   // extract requests that appear to have been dropped
   if (isIdle)
   {
      auto currentTime = boost::posix_time::second_clock::local_time();

      std::vector<std::string> keys;
      boost::copy(
               s_pendingContinuations | boost::adaptors::map_keys,
               std::back_inserter(keys));

      for (auto&& key : keys)
      {
         auto&& continuation = s_pendingContinuations[key];
         auto elapsedTime = currentTime - continuation.time();
         if (elapsedTime.seconds() > 10)
         {
            DLOG("Cancelling old continuation with id '{}'.", key);
            continuation.cancel();
            s_pendingContinuations.erase(key);
         }
      }
   }

   // process any pending requests
   while (!s_pendingResponses.empty())
   {
      // Try to parse this as JSON, and see if we received the response we expect.
      // The agent might be chatty inbetween a request and a response, so we can
      // drop any output not specifically for us.
      std::string response = s_pendingResponses.front();
      s_pendingResponses.pop();

      json::Object responseJson;
      Error error = responseJson.parse(response);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      // Check if this is a 'LogMessage' response. Should we log these in verbose mode?
      json::Value methodJson = responseJson["method"];
      if (methodJson.isString())
      {
         std::string method = methodJson.getString();
         if (method == "LogMessage")
            continue;
      }

      // Check the response id. This will be missing for notifications; we may receive
      // a flurry of progress notifications when requesting completions from Copilot.
      // We might want to handle these somehow. Perhaps hook them up to some status widget?
      json::Value requestIdJson = responseJson["id"];
      if (!requestIdJson.isString())
      {
         DLOG("Ignoring response with missing id.");
         continue;
      }

      // Check for a continuation handler, and invoke it if available.
      std::string requestId = requestIdJson.getString();
      if (s_pendingContinuations.count(requestId))
      {
         DLOG("Invoking continuation with id '{}'.", requestId);
         s_pendingContinuations[requestId].invoke(responseJson);
         s_pendingContinuations.erase(requestId);
      }
      else
      {
         WLOG("Received response with id '{}', but no continuation is registered for that response.", requestId);
      }
   }
}

void onPreferencesSaved()
{
   // Check for preference change
   bool oldEnabled = s_copilotEnabled;
   bool newEnabled = prefs::userPrefs().copilotEnabled();
   if (oldEnabled == newEnabled)
      return;

   // Update our preference
   s_copilotEnabled = newEnabled;

   // Start or stop the agent as appropriate
   if (s_copilotEnabled)
   {
      startAgent();
   }
   else
   {
      stopAgent();
   }
}

void onUserPrefsChanged(const std::string& layer,
                        const std::string& name)
{
   if (name == "copilot_enabled")
   {
      onPreferencesSaved();
   }
}

void onDeferredInit(bool newSession)
{
   source_database::events().onDocAdded.connect(onDocAdded);
   source_database::events().onDocUpdated.connect(onDocUpdated);
   source_database::events().onDocRemoved.connect(onDocRemoved);
}

void onShutdown(bool)
{
   // Note that we're about to shut down.
   s_isSessionShuttingDown = true;

   // Shut down the agent.
   stopAgent();

   // Unset the agent PID. It should already be shutting down,
   // but this will make sure we don't try to make any more
   // requests while we're shutting down.
   s_agentPid = -1;
}

SEXP rs_copilotSetLogLevel(SEXP logLevelSEXP)
{
   int logLevel = r::sexp::asInteger(logLevelSEXP);
   s_copilotLogLevel = logLevel;
   return logLevelSEXP;
}

Error copilotGenerateCompletions(const json::JsonRpcRequest& request,
                                 const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure copilot is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Read params
   std::string documentId;
   int cursorRow, cursorColumn;

   Error error = core::json::readParams(request.params, &documentId, &cursorRow, &cursorColumn);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // Build completion request
   json::Object positionJson;
   positionJson["line"] = cursorRow;
   positionJson["character"] = cursorColumn;

   json::Object docJson;
   docJson["position"] = positionJson;
   docJson["uri"] = uriFromDocumentId(documentId);
   docJson["version"] = 1;

   json::Object paramsJson;
   paramsJson["doc"] = docJson;

   // Send the request
   std::string requestId = core::system::generateUuid();
   sendRequest("getCompletions", requestId, paramsJson, CopilotContinuation(continuation));

   return Success();
}

Error copilotSignIn(const json::JsonRpcRequest& request,
                    const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure copilot is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Send sign in request
   std::string requestId = core::system::generateUuid();
   sendRequest("signInInitiate", requestId, json::Object(), CopilotContinuation(continuation));

   return Success();
}

Error copilotSignOut(const json::JsonRpcRequest& request,
                     const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure copilot is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Send sign out request
   std::string requestId = core::system::generateUuid();
   sendRequest("signOut", requestId, json::Object(), CopilotContinuation(continuation));
   return Success();
}

Error copilotStatus(const json::JsonRpcRequest& request,
                    const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure copilot is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Send sign out request
   std::string requestId = core::system::generateUuid();
   sendRequest("checkStatus", requestId, json::Object(), CopilotContinuation(continuation));
   return Success();
}

Error copilotVerifyInstalled(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   json::Object responseJson;
   responseJson["installed"] = isCopilotAgentInstalled();
   pResponse->setResult(responseJson);
   return Success();
}

Error copilotInstallAgent(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   Error installError = installCopilotAgent();

   json::Object responseJson;
   if (installError)
      responseJson["error"] = installError.asString();
   pResponse->setResult(responseJson);
   return Success();
}

} // end anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   s_copilotEnabled = prefs::userPrefs().copilotEnabled();

   events().onBackgroundProcessing.connect(onBackgroundProcessing);
   events().onPreferencesSaved.connect(onPreferencesSaved);
   events().onDeferredInit.connect(onDeferredInit);
   events().onShutdown.connect(onShutdown);

   // TODO: Do we need this _and_ the preferences saved callback?
   // This one seems required so that we see preference changes while
   // editting preferences within the Copilot prefs dialog, anyhow.
   prefs::userPrefs().onChanged.connect(onUserPrefsChanged);

   RS_REGISTER_CALL_METHOD(rs_copilotSetLogLevel);

   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(registerAsyncRpcMethod, "copilot_generate_completions", copilotGenerateCompletions))
         (bind(registerAsyncRpcMethod, "copilot_sign_in", copilotSignIn))
         (bind(registerAsyncRpcMethod, "copilot_sign_out", copilotSignOut))
         (bind(registerAsyncRpcMethod, "copilot_status", copilotStatus))
         (bind(sourceModuleRFile, "SessionCopilot.R"))
         (bind(registerRpcMethod, "copilot_verify_installed", copilotVerifyInstalled))
         (bind(registerRpcMethod, "copilot_install_agent", copilotInstallAgent))
         ;
   return initBlock.execute();

}

} // end namespace copilot
} // end namespace modules
} // end namespace session
} // end namespace rstudio
