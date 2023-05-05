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
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>
#include <r/session/REventLoop.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

#define DBG if (true)

using namespace rstudio::core;
using namespace rstudio::core::system;

namespace rstudio {
namespace session {
namespace modules {
namespace copilot {

namespace {

// Whether Copilot is enabled.
bool s_copilotEnabled = false;

// The PID of the active Copilot agent process.
PidType s_agentPid = -1;

// A queue of pending requests, to be sent via the agent's stdin.
std::queue<std::string> s_pendingRequests;

// A queue of pending responses, sent via the agent's stdout.
std::queue<std::string> s_pendingResponses;


std::string uriFromDocumentId(const std::string& documentId)
{
   return fmt::format("rstudio-document://{}", documentId);
}

template <typename F>
bool waitFor(F&& callback,
             int growthFactor = 2,
             int initialWaitMs = 10,
             int maxTries = 100)
{
   int waitMs = initialWaitMs;

   for (int i = 0; i < maxTries; i++)
   {
      bool ready = callback();
      if (ready)
         return true;

      r::session::event_loop::processEvents();
      ::usleep(waitMs * 1000);

      waitMs = waitMs * growthFactor;
   }

   return false;
}

FilePath copilotAgentDirectory()
{
   return core::system::xdg::userCacheDir().completeChildPath("copilot/dist");
}

FilePath copilotAgentPath()
{
   return copilotAgentDirectory().completeChildPath("agent.js");
}

bool isCopilotAgentInstalled()
{
   return copilotAgentPath().exists();
}

bool installCopilotAgent()
{
   bool didInstall = false;
   Error error = r::exec::RFunction(".rs.copilot.installCopilotAgent")
         .addParam(copilotAgentDirectory().getAbsolutePath())
         .call(&didInstall);

   if (error)
      LOG_ERROR(error);

   return didInstall;
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

   // Set id if available. (Requests without an id are 'notifications')
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

json::Value sendRequest(const std::string& method,
                        const std::string& requestId,
                        const json::Value& paramsJson)
{
   // Create and enqueue the request.
   std::string request = createRequest(method, requestId, paramsJson);
   s_pendingRequests.push(request);

   // Start waiting until we get a response.
   while (true)
   {
      bool ready = waitFor([] () { return !s_pendingResponses.empty();  });
      if (!ready)
      {
         ELOGF("[copilot] no response received for request with id '{}'", requestId);
         break;
      }

      // Try to parse this as JSON, and see if we received the response we expect.
      // The agent might be chatty inbetween a request and a response, so we can
      // drop any output not specifically for us.
      std::string response = s_pendingResponses.front();
      s_pendingResponses.pop();

      json::Object object;
      Error error = object.parse(response);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      // Check if this is a 'LogMessage' response. Should we log these in verbose mode?
      json::Value methodJson = object["method"];
      if (methodJson.isString())
      {
         std::string method = methodJson.getString();
         if (method == "LogMessage")
            continue;
      }

      // Check the response id. This will be missing for notifications; we may receive
      // a flurry of progress notifications when requesting completions from Copilot.
      // We might want to handle these somehow.
      json::Value idJson = object["id"];
      if (!idJson.isString())
      {
         ELOGF("[copilot] ignoring response with missing id");
         continue;
      }

      std::string responseId = idJson.getString();
      if (requestId != responseId)
      {
         ELOGF("[copilot] ignoring unexpected response with id '{}'", responseId);
         continue;
      }

      // We got our response; return it
      return object;
   }

   ELOGF("[copilot] unexpected error while waiting for response from request with id '{}'", requestId);
   return json::Value();

}

namespace agent {

void onStarted(ProcessOperations& operations)
{
   // Record the PID of the agent.
   s_agentPid = operations.getPid();
}

bool onContinue(ProcessOperations& operations)
{
   // Send any pending requests over stdin.
   while (!s_pendingRequests.empty())
   {
      std::string request = s_pendingRequests.front();
      s_pendingRequests.pop();

      DBG
      {
         std::cerr << "----------------" << std::endl;
         std::cerr << "Sending request:" << std::endl;
         std::cerr << request << std::endl;
         std::cerr << "----------------" << std::endl;
         std::cerr << std::endl;
      }

      operations.writeToStdin(request, false);
   }

   return true;
}

void onStdout(ProcessOperations& operations, const std::string& stdOut)
{
   // Copilot responses will have the format
   //
   //    Content-Length: xyz
   //
   //    <body>
   //
   // Importantly, there is no character separating the body from the
   // next response, so we're forced to parse headers here.
   std::size_t startIndex = 0;

   while (startIndex < stdOut.length())
   {
      // Find the split.
      auto splitIndex = stdOut.find("\r\n\r\n", startIndex);
      if (splitIndex == std::string::npos)
      {
         ELOGF("[copilot] internal error: parsing response failed");
         break;
      }

      // Extract the header text.
      std::string headerText = string_utils::substring(stdOut, startIndex, splitIndex + 4);

      // Parse the headers.
      core::http::Headers headers;
      std::istringstream iss(headerText);
      core::http::parseHeaders(iss, &headers);

      // Check for content length.
      std::string contentLength = core::http::headerValue(headers, "Content-Length");
      if (contentLength.empty())
      {
         ELOGF("[copilot] internal error: response contains no Content-Length header");
         break;
      }

      // Consume that text.
      auto bodyStart = splitIndex + 4;
      auto bodyEnd = bodyStart + safe_convert::stringTo<int>(contentLength, 0);
      std::string bodyText = string_utils::substring(stdOut, bodyStart, bodyEnd);
      s_pendingResponses.push(bodyText);

      DBG
      {
         std::cerr << "------------------" << std::endl;
         std::cerr << "Received response:" << std::endl;
         std::cerr << bodyText << std::endl;
         std::cerr << "------------------" << std::endl;
         std::cerr << std::endl;
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

   if (status != 0)
   {
      ELOGF("[copilot] agent exited with status {}", status);
   }
}

} // end namespace agent

void stopAgent()
{
   if (s_agentPid != -1)
   {
      Error error = core::system::terminateProcess(s_agentPid);
      if (error)
         LOG_ERROR(error);

      s_agentPid = -1;
   }
}

bool startAgent()
{
   Error error;

   // TODO: Bundle node? Ask user to provide path to node executable?
   // With desktop builds, we could consider using ELECTRON_RUN_AS_NODE=1 <RStudio>
   FilePath nodePath;
   error = core::system::findProgramOnPath("node", &nodePath);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // TODO: Prompt user to download agent, and then use that path.
   FilePath agentPath = copilotAgentPath();
   if (!agentPath.exists())
      return false;

   // Set up process callbacks
   core::system::ProcessCallbacks callbacks;
   callbacks.onStarted = &agent::onStarted;
   callbacks.onContinue = &agent::onContinue;
   callbacks.onStdout = &agent::onStdout;
   callbacks.onStderr = &agent::onStderr;
   callbacks.onExit = &agent::onExit;

   core::system::ProcessOptions options;
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
   waitFor([]() { return s_agentPid != -1; });

   // Send an initialize request to the agent.
   json::Object clientInfoJson;
   clientInfoJson["name"] = "RStudio";
   clientInfoJson["version"] = "1.0.0";

   json::Object paramsJson;
   paramsJson["processId"] = ::getpid();
   paramsJson["clientInfo"] = clientInfoJson;
   paramsJson["capabilities"] = json::Object();

   std::string requestId = core::system::generateUuid();
   sendRequest("initialize", requestId, paramsJson);

   // Okay, we're ready to go.
   return true;
}

void ensureAgentRunning()
{
   if (s_agentPid == -1)
   {
      startAgent();
   }
}

void onDocAdded(const std::string& id)
{
   if (!s_copilotEnabled)
      return;

   ensureAgentRunning();

   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocumentId(id);
   textDocumentJson["languageId"] = "r";
   textDocumentJson["version"] = 1;
   textDocumentJson["text"] = "";

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didOpen", paramsJson);
}

void onDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!s_copilotEnabled)
      return;

   ensureAgentRunning();

   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocumentId(pDoc->id());
   textDocumentJson["languageId"] = "r";
   textDocumentJson["version"] = 1;
   textDocumentJson["text"] = pDoc->contents();

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didOpen", paramsJson);
}

void onDocRemoved(const std::string& id, const std::string& path)
{
   if (!s_copilotEnabled)
      return;

   ensureAgentRunning();

   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocumentId(id);

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didClose", paramsJson);
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

void onDeferredInit(bool newSession)
{
   source_database::events().onDocAdded.connect(onDocAdded);
   source_database::events().onDocUpdated.connect(onDocUpdated);
   source_database::events().onDocRemoved.connect(onDocRemoved);
}

SEXP rs_copilotStartAgent()
{
   // If we have a running agent, try to stop it now.
   stopAgent();

   // Start the agent.
   bool ready = startAgent();
   if (!ready)
      Rf_error("[copilot] agent failed to start");

   // Wait for a PID to be known
   waitFor([]() { return s_agentPid != -1; });

   r::sexp::Protect protect;
   return r::sexp::create(s_agentPid, &protect);
}

SEXP rs_copilotAgentRunning()
{
   r::sexp::Protect protect;
   return r::sexp::create(s_agentPid != -1, &protect);
}

SEXP rs_copilotSendRequest(SEXP methodSEXP,
                           SEXP idSEXP,
                           SEXP paramsSEXP)
{
   std::string method = r::sexp::asString(methodSEXP);
   std::string id = r::sexp::asString(idSEXP);
   std::string paramsText = r::sexp::asString(paramsSEXP);

   json::Object paramsJson;
   Error error = paramsJson.parse(paramsText);
   if (error)
   {
      error.addProperty("params", paramsText);
      LOG_ERROR(error);
      return R_NilValue;
   }

   if (id.empty())
   {
      sendNotification(method, paramsJson);
      return R_NilValue;
   }

   std::string requestId = core::system::generateUuid();
   json::Value responseJson = sendRequest(method, id, paramsJson);

   r::sexp::Protect protect;
   return r::sexp::create(responseJson, &protect);
}

SEXP rs_copilotAgentPid()
{
   r::sexp::Protect protect;
   return r::sexp::create(s_agentPid, &protect);
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
   json::Object responseJson;
   responseJson["installed"] = installCopilotAgent();
   pResponse->setResult(responseJson);
   return Success();
}

} // end anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   s_copilotEnabled = prefs::userPrefs().copilotEnabled();

   events().onPreferencesSaved.connect(onPreferencesSaved);
   events().onDeferredInit.connect(onDeferredInit);

   RS_REGISTER_CALL_METHOD(rs_copilotStartAgent);
   RS_REGISTER_CALL_METHOD(rs_copilotAgentRunning);
   RS_REGISTER_CALL_METHOD(rs_copilotSendRequest);
   RS_REGISTER_CALL_METHOD(rs_copilotAgentPid);

   ExecBlock initBlock;
   initBlock.addFunctions()
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
