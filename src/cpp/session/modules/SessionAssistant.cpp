/*
 * SessionAssistant.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "SessionAssistant.hpp"
#include "SessionNodeTools.hpp"

#include <atomic>

#include <boost/current_function.hpp>
#include <boost/range/adaptors.hpp>
#include <boost/range/algorithm/copy.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/collection/Position.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/Header.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/StringUtils.hpp>
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

#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionLSP.hpp"

#include "session-config.h"

#define _LOG_IMPL(__LOGGER__, __FMT__, ...)                             \
   do                                                                   \
   {                                                                    \
      std::string __message__ = fmt::format(__FMT__, ##__VA_ARGS__);    \
      std::string __formatted__ =                                       \
          fmt::format("[{}]: {}", __func__, __message__);               \
      __LOGGER__("assistant", __formatted__);                           \
      if (assistantLogLevel() >= 1)                                     \
         std::cerr << __formatted__ << std::endl;                       \
   } while (0)

#define DLOG(__FMT__, ...) _LOG_IMPL(LOG_DEBUG_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...) _LOG_IMPL(LOG_WARNING_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...) _LOG_IMPL(LOG_ERROR_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)

// Use a default section of 'assistant' for errors / warnings
#ifdef LOG_ERROR
# undef LOG_ERROR
# define LOG_ERROR(error) LOG_ERROR_NAMED("assistant", error)
#endif

#define kAssistantDefaultDocumentVersion (0)
#define kMaxIndexingFileSize (1048576)

// completion was triggered explicitly by a user gesture
#define kAssistantCompletionTriggerUserInvoked (1)

// completion was triggered automatically while editing
#define kAssistantCompletionTriggerAutomatic (2)

using namespace rstudio::core;
using namespace rstudio::core::system;

namespace rstudio {
namespace session {
namespace modules {
namespace assistant {

namespace {

struct AssistantRequest
{
   std::string method;
   std::string id;
   json::Value params;
};

class AssistantContinuation
{
public:

   explicit AssistantContinuation(const json::JsonRpcFunctionContinuation& continuation)
      : continuation_(continuation),
        time_(boost::posix_time::second_clock::local_time())
   {
   }
   
   // default ctor needed for compatibility with map
   AssistantContinuation()
      : AssistantContinuation(json::JsonRpcFunctionContinuation())
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

enum class AgentNotRunningReason {
   Unknown,
   NotInstalled,
   DisabledByAdministrator,
   DisabledViaProjectPreferences,
   DisabledViaGlobalPreferences,
   LaunchError,
};

// keep in sync with constants in AssistantStatusChangedEvent.java
enum class AgentRuntimeStatus {
   Unknown,
   Preparing,
   Starting,
   Running,
   Stopping,
   Stopped,
};

// The log level for agent-specific logs. Primarily for developer use.
int s_assistantLogLevel = 0;

// Whether the agent is enabled.
bool s_assistantEnabled = false;

// Whether the agent has been allowed to index project files.
bool s_assistantIndexingEnabled = false;

// Have we checked the config files at least once
bool s_assistantInitialized = false;

// The PID of the active agent process.
PidType s_agentPid = -1;

// Error output (if any) that was written during startup.
std::string s_agentStartupError;

// The current status of the agent, mainly around if it's enabled
// (and why or why not).
AgentNotRunningReason s_agentNotRunningReason = AgentNotRunningReason::Unknown;

// The current runtime status of the agent process.
AgentRuntimeStatus s_agentRuntimeStatus = AgentRuntimeStatus::Unknown;

void setAgentRuntimeStatus(AgentRuntimeStatus status)
{
   if (s_agentRuntimeStatus != status)
   {
      s_agentRuntimeStatus = status;

      // notify client of agent status change
      json::Object dataJson;
      dataJson["status"] = static_cast<int>(status);
      ClientEvent event(client_events::kAssistantStatusChanged, dataJson);
      module_context::enqueClientEvent(event);
   }
}

// Whether or not we've handled the Assistant 'initialized' notification.
// Primarily done to allow proper sequencing of Assistant callbacks.
bool s_agentInitialized = false;

// A queue of pending requests, to be sent via the agent's stdin.
std::vector<AssistantRequest> s_pendingRequests;

// Metadata related to pending requests. Mainly used to map
// responses to their expected result types.
std::map<std::string, AssistantContinuation> s_pendingContinuations;

// A queue of pending responses, sent via the agent's stdout.
std::queue<std::string> s_pendingResponses;

// Whether we're about to shut down.
bool s_isSessionShuttingDown = false;

// Project-specific assistant options.
projects::RProjectAssistantOptions s_assistantProjectOptions;

// A queue of pending files to be indexed.
std::vector<FileInfo> s_indexQueue;
std::size_t s_indexBatchSize = 200;

// Next Edit Suggestions (NES) retry configuration.
// When NES returns no suggestions, we retry at progressively further positions.
constexpr int kNesMaxRetries = 3;
constexpr int kNesRetryLineOffset = 4;

// Sequence number for NES requests. Incremented on each new request to
// allow cancellation of in-flight requests when a new request arrives.
std::atomic<int> s_nesRequestSequence{0};

int assistantLogLevel()
{
   return s_assistantLogLevel;
}

bool isIndexableFile(const FilePath& documentPath)
{
   // Don't index hidden files.
   if (documentPath.isHidden())
      return false;
   
   // Don't index R files which might contain secrets.
   std::string name = documentPath.getFilename();
   if (name == "Renviron.site")
      return false;
   
   // Don't index files within hidden folders (like .ssh)
   std::string path = documentPath.getAbsolutePath();
   if (path.find("/.") != std::string::npos)
      return false;
   
   // Don't index .Rproj files
   std::string ext = documentPath.getExtensionLowerCase();
   if (ext == ".rproj")
      return false;

   // Handle Dockerfile with any extension (including none, the most common)
   std::string stem = documentPath.getStem();
   if (stem == "Dockerfile")
      return true;

   // Handle extensionless Makefile / makefile
   if (name == "Makefile" || name == "makefile")
      return true;

   // TODO: Do we want to also allow indexing of 'data' file types?
   // We previously used module_context::isTextFile(), but because this
   // relies on invoking /usr/bin/file, this can be dreadfully slow if
   // the project contains a large number of files without a known type.
   std::string languageId = lsp::languageIdFromExtension(ext);
   return !languageId.empty();
}

bool isIndexableDocument(const boost::shared_ptr<source_database::SourceDocument>& pDoc)
{
   // Don't index binary files.
   if (pDoc->contents().find('\0') != std::string::npos)
      return false;
   
   // Our source database uses non-standard names for certain R file types,
   // so explicitly check for those and allow them to be indexed.
   if (pDoc->isRFile() || pDoc->isRMarkdownDocument())
      return true;
   
   // Allow indexing of Untitled documents if they have a known type.
   if (pDoc->isUntitled())
   {
      std::string type = pDoc->type();
      std::string ext = lsp::extensionFromLanguageId(type);
      return !ext.empty();
   }
   
   // Otherwise, check for known files / extensions.
   FilePath docPath(pDoc->path());
   return isIndexableFile(docPath);
}

FilePath assistantLanguageServerPath()
{
   FilePath assistantPath;
   
   std::string rstudioAgentPath = core::system::getenv("RSTUDIO_AGENT_PATH");
   if (!rstudioAgentPath.empty() && FilePath::exists(rstudioAgentPath))
      return FilePath(rstudioAgentPath);

   std::string rstudioAssistant = core::system::getenv("RSTUDIO_COPILOT_JS_FOLDER");
   if (!rstudioAssistant.empty())
   {
      if (FilePath::exists(rstudioAssistant) && FilePath(rstudioAssistant).isDirectory())
      {
         assistantPath = FilePath(rstudioAssistant);
      }
   }
   
   if (assistantPath.isEmpty())
   {
      // TODO: Should we support separate paths for Posit Assistant vs Copilot here?
      assistantPath = session::options().copilotPath();
      if (!assistantPath.exists() || !assistantPath.isDirectory())
      {
         ELOG("Assistant Language Server path '{}' does not exist or is not a directory.", assistantPath.getAbsolutePath());
         return FilePath();
      }
   }

   auto suffix = "language-server.js";
   FilePath candidatePath = assistantPath.completePath(suffix);
   if (candidatePath.exists())
   {
      DLOG("Assistant Language Server '{}' found at '{}'.", suffix, candidatePath.getAbsolutePath());
      return candidatePath;
   }

   ELOG("Assistant Language Server '{}' not found at '{}'.", suffix, candidatePath.getAbsolutePath());
   return FilePath();
}

bool isAgentInstalled()
{
   return assistantLanguageServerPath().exists();
}

bool isAssistantEnabled()
{
   // Check administrator option
   // TODO: Add option for 'assistantEnabled' vs. 'assistantEnabled'?
   if (!session::options().copilotEnabled())
   {
      s_agentNotRunningReason = AgentNotRunningReason::DisabledByAdministrator;
      return false;
   }

   // Check whether the agent is where it should be
   if (!isAgentInstalled())
   {
      s_agentNotRunningReason = AgentNotRunningReason::NotInstalled;
      return false;
   }

   // Check project option
   // TODO: Handle possible values of 'assistant' here
   std::string assistant = s_assistantProjectOptions.assistant;

   // Check user preference
   // TODO: Copilot vs. Assistant options here
   if (!prefs::userPrefs().copilotEnabled())
   {
      s_agentNotRunningReason = AgentNotRunningReason::DisabledViaGlobalPreferences;
      return false;
   }
   
   return true;
}

bool isAssistantIndexingEnabled()
{
   // Check project option
   // TODO: Use 'assistant' member on options value
   /// switch (s_assistantProjectOptions.assistantIndexingEnabled)
   /// {
   /// case r_util::YesValue: return true;
   /// case r_util::NoValue: return false;
   /// default: {}
   /// }

   // Check user preference
   // TODO: Copilot vs. Assistant options here
   return prefs::userPrefs().copilotIndexingEnabled();
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

void sendNotification(const std::string& method,
                      const json::Value& paramsJson)
{
   s_pendingRequests.push_back({ method, "", paramsJson });
}

std::string formatRequest(const AssistantRequest& request)
{
   // Create the request body
   json::Object requestJson;
   requestJson["jsonrpc"] = "2.0";
   requestJson["method"] = request.method;
   requestJson["params"] = request.params;

   // Set id if available.
   // Requests without an id are 'notifications', and never receive a response.
   std::string id = request.id;
   if (!id.empty())
      requestJson["id"] = id;

   // Convert to a JSON string
   std::string requestBody = requestJson.write();

   // Convert into HTTP request with JSON payload in body
   return fmt::format("Content-Length: {}\r\n\r\n{}", requestBody.size(), requestBody);
}


void sendRequest(const std::string& method,
                 const std::string& requestId,
                 const json::Value& paramsJson,
                 const AssistantContinuation& continuation)
{
   DLOG("Enqueuing request '{}' with id '{}'.", method, requestId);
   
   // Add the continuation, which is executed in response to the request.
   s_pendingContinuations[requestId] = continuation;

   // Create and enqueue the request.
   s_pendingRequests.push_back({ method, requestId, paramsJson });
}

// Should only be used for debugging, as this will block the R session
// while the request is being serviced.
json::Object sendSynchronousRequest(const std::string& method,
                                   const std::string& requestId,
                                   const json::Value& paramsJson)
{
   json::Object result;
   bool responseReceived = false;
   
   auto continuation = [&](const Error& error, json::JsonRpcResponse* pResponse)
   {
      responseReceived = true;
      if (error == Success() && pResponse->result().isObject())
         result = pResponse->result().getObject();
   };
   
   sendRequest(method, requestId, paramsJson, AssistantContinuation(continuation));
   waitFor([&]() { return responseReceived; });
   
   return result;
}

void setCopilotConfiguration()
{
   json::Object paramsJson;
   json::Object settingsJson;
   
   // network proxy settings 
   r::sexp::Protect protect;
   
   // check strict ssl flag
   bool proxyStrictSsl = session::options().copilotProxyStrictSsl();
   
   // allow override from envvar
   std::string proxyStrictSslOverride = core::system::getenv("COPILOT_PROXY_STRICT_SSL");
   if (!proxyStrictSslOverride.empty())
      proxyStrictSsl = core::string_utils::isTruthy(proxyStrictSslOverride, true);
   
   // check for server-configured proxy URL
   std::string proxyUrl = session::options().copilotProxyUrl();
   
   // allow override from envvar
   std::string proxyUrlOverride = core::system::getenv("COPILOT_PROXY_URL");
   if (!proxyUrlOverride.empty())
      proxyUrl = proxyUrlOverride;
   
   // if the network proxy isn't set, try reading a fallback from R
   if (proxyUrl.empty())
   {
      SEXP networkProxySEXP = R_NilValue;
      Error error = r::exec::RFunction(".rs.copilot.networkProxy").call(&networkProxySEXP, &protect);
      if (error)
         LOG_ERROR(error);
      proxyUrl = networkProxySEXP == R_NilValue ? "" : r::sexp::asString(networkProxySEXP);
   }

   // if we now have a network proxy definition, log it
   if (!proxyUrl.empty())
   {
      SEXP networkProxySEXP = R_NilValue;

      // parse the URL into its associated components for logging
      Error error = r::exec::RFunction(".rs.copilot.parseNetworkProxyUrl")
            .addUtf8Param(proxyUrl)
            .call(&networkProxySEXP, &protect);
      if (error)
      {
         proxyUrl.clear();
         LOG_ERROR(error);
      }
      if (networkProxySEXP != R_NilValue)
      {
         json::Value networkProxy;
         Error error = r::json::jsonValueFromObject(networkProxySEXP, &networkProxy);
         if (error)
            LOG_ERROR(error);
         if (networkProxy.isObject())
         {
            json::Object networkProxyJson = networkProxy.getObject();

            if (s_assistantLogLevel > 0)
            {
               json::Object networkProxyClone = networkProxyJson.clone().getObject();
               if (networkProxyClone.hasMember("user"))
                  networkProxyClone["user"] = "<user>";
               if (networkProxyClone.hasMember("pass"))
                  networkProxyClone["pass"] = "<pass>";
               DLOG("Using network proxy: {}", networkProxyClone.writeFormatted());
            }
         }
      }
   }
   
   // if we have a network proxy, add it to the configuration
   if (!proxyUrl.empty())
   {
      // check for proxy Kerberos service principal setting
      std::string kerberosPrincipal = session::options().copilotProxyKerberosPrincipal();

      // allow override from envvar
      std::string kerberosPrincipalOverride = core::system::getenv("COPILOT_PROXY_KERBEROS_SERVICE_PRINCIPAL");
      if (!kerberosPrincipalOverride.empty())
         kerberosPrincipal = kerberosPrincipalOverride;

      // if the principal isn't set, try reading a fallback from R
      if (kerberosPrincipal.empty())
      {
         SEXP kerberosPrincipalSEXP = R_NilValue;
         Error error = r::exec::RFunction(".rs.copilot.proxyKerberosServicePrincipal").call(&kerberosPrincipalSEXP, &protect);
         if (error)
            LOG_ERROR(error);
         kerberosPrincipal = kerberosPrincipalSEXP == R_NilValue ? "" : r::sexp::asString(kerberosPrincipalSEXP);
      }
      
      json::Object httpParamsJson;
      httpParamsJson["proxy"] = proxyUrl;
      httpParamsJson["proxyStrictSSL"] = proxyStrictSsl;
      if (!kerberosPrincipal.empty())
         httpParamsJson["proxyKerberosServicePrincipal"] = kerberosPrincipal;
      settingsJson["http"] = httpParamsJson;
   }
   
   // check for authentication provider
   std::string authProviderUrl = session::options().copilotAuthProvider();
   
   // allow override from envvar
   std::string authProviderUrlOverride = core::system::getenv("COPILOT_AUTH_PROVIDER");
   if (!authProviderUrlOverride.empty())
      authProviderUrl = authProviderUrlOverride;
   
   if (authProviderUrl.empty())
   {
      // check for authentication provider configuration from R
      SEXP authProviderSEXP = R_NilValue;
      Error error = r::exec::RFunction(".rs.copilot.authProvider").call(&authProviderSEXP, &protect); 
      if (error)
         LOG_ERROR(error);
      authProviderUrl = authProviderSEXP == R_NilValue ? "" : r::sexp::asString(authProviderSEXP);
   }
   
   if (!authProviderUrl.empty())
   {
      json::Object githubEnterpriseJson;
      githubEnterpriseJson["uri"] = authProviderUrl;
      
      DLOG("Using github-enterprise authentication provider: {}", githubEnterpriseJson.writeFormatted());
      settingsJson["github-enterprise"] = githubEnterpriseJson;
   }
  
   paramsJson["settings"] = settingsJson;
   std::string requestId = core::system::generateUuid();
   sendNotification("workspace/didChangeConfiguration", paramsJson);
}

namespace agent {

void onStarted(ProcessOperations& operations)
{
   // Record the PID of the agent.
   // TODO: Log current agent type as well (from prefs)
   DLOG("Agent has started [PID = {}]", operations.getPid());
   s_agentPid = operations.getPid();
   setAgentRuntimeStatus(AgentRuntimeStatus::Starting);
}

bool onContinue(ProcessOperations& operations)
{
   if (s_isSessionShuttingDown)
      return false;

   auto debugCallback = [](const std::string& htmlRequest)
   {
      if (assistantLogLevel() >= 2)
      {
         std::cerr << std::endl;
         std::cerr << "REQUEST" << std::endl;
         std::cerr << "----------------" << std::endl;
         std::cerr << htmlRequest << std::endl;
         std::cerr << "----------------" << std::endl;
         std::cerr << std::endl << std::endl;
      }
   };
   
   if (s_agentInitialized)
   {
      for (auto&& request : s_pendingRequests)
      {
         std::string htmlRequest = formatRequest(request);
         debugCallback(htmlRequest);
         operations.writeToStdin(htmlRequest, false);
      }
      s_pendingRequests.clear();
   }
   else
   {
      // use expel_if to only process requests related to initialization
      core::algorithm::expel_if(s_pendingRequests, [&](const AssistantRequest& request)
      {
         bool isInitMethod =
               request.method == "initialize" ||
               request.method == "initialized" ||
               request.method == "workspace/didChangeConfiguration";
         
         if (!isInitMethod)
            return false;
         
         if (request.method == "initialized")
            s_agentInitialized = true;
         
         std::string htmlRequest = formatRequest(request);
         debugCallback(htmlRequest);
         operations.writeToStdin(htmlRequest, false);
         return true;
      });
   }

   return true;
}

void onStdout(ProcessOperations& operations, const std::string& stdOut)
{
   if (s_isSessionShuttingDown)
      return;

   // Discard empty lines.
   static const boost::regex reWhitespace("^\\s*$");
   if (regex_utils::match(stdOut, reWhitespace))
      return;

   // Responses will have the format
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
         
         if (assistantLogLevel() >= 2)
         {
            std::cerr << std::endl;
            std::cerr << "RESPONSE" << std::endl;
            std::cerr << "------------------" << std::endl;
            std::cerr << stdOut << std::endl;
            std::cerr << "------------------" << std::endl;
            std::cerr << std::endl << std::endl;
         }

         break;
      }

      // Consume that text.
      auto bodyStart = splitIndex + splitSize;
      auto bodyEnd = bodyStart + safe_convert::stringTo<int>(contentLength, 0);
      std::string bodyText = string_utils::substring(stdOut, bodyStart, bodyEnd);
      s_pendingResponses.push(bodyText);

      if (assistantLogLevel() >= 2)
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
   
   // Note that the agent is now ready.
   setAgentRuntimeStatus(AgentRuntimeStatus::Running);
}

void onStderr(ProcessOperations& operations, const std::string& stdErr)
{
   if (s_isSessionShuttingDown)
      return;

   LOG_ERROR_MESSAGE_NAMED("assistant", stdErr);
   if (assistantLogLevel() >= 1)
      std::cerr << stdErr << std::endl;
 
   // If we get output from stderr while the agent is starting, that means
   // something went wrong and we're about to shut down.
   switch (s_agentRuntimeStatus)
   {
   
   case AgentRuntimeStatus::Starting:
   case AgentRuntimeStatus::Stopping:
   {
      s_agentStartupError += stdErr;
      setAgentRuntimeStatus(AgentRuntimeStatus::Stopping);
      break;
   }
 
   // TODO: Is there anything reasonable we can do with errors here?
   default: {}
      
   }

}

void onError(ProcessOperations& operations, const Error& error)
{
   s_agentPid = -1;
   setAgentRuntimeStatus(AgentRuntimeStatus::Stopped);
}

void onExit(int status)
{
   s_agentPid = -1;
   setAgentRuntimeStatus(AgentRuntimeStatus::Stopped);
}

} // end namespace agent


void stopAgent()
{
   if (s_agentPid == -1)
   {
      setAgentRuntimeStatus(AgentRuntimeStatus::Stopped);
      return;
   }

   Error error = core::system::terminateProcess(s_agentPid);
   if (error)
      LOG_ERROR(error);
}

bool stopAgentSync()
{
   if (s_agentPid == -1)
      return true;

   stopAgent();
   return waitFor([]() { return s_agentPid == -1; });
}

Error startAgent()
{
   if (s_agentRuntimeStatus != AgentRuntimeStatus::Unknown &&
       s_agentRuntimeStatus != AgentRuntimeStatus::Stopped)
   {
      return Success();
   }

   setAgentRuntimeStatus(AgentRuntimeStatus::Preparing);

   Error error;

   // Create environment for agent process
   core::system::Options environment;
   core::system::environment(&environment);
   
   // Set NODE_EXTRA_CA_CERTS if a custom certificates file is provided.
   // TODO: Copilot vs. Assistant option here?
   std::string certificatesFile = session::options().copilotSslCertificatesFile();
   if (!certificatesFile.empty())
      environment.push_back(std::make_pair("NODE_EXTRA_CA_CERTS", certificatesFile));

   // Find node.js
   // TODO: Should we support a custom node path here?
   FilePath nodePath;
   error = node_tools::findNode(&nodePath, "rstudio.copilot.nodeBinaryPath");
   if (error)
      return error;

   if (!nodePath.exists())
      return fileNotFoundError("node", ERROR_LOCATION);

   // Set up process callbacks
   core::system::ProcessCallbacks callbacks;
   callbacks.onStarted = &agent::onStarted;
   callbacks.onContinue = &agent::onContinue;
   callbacks.onStdout = &agent::onStdout;
   callbacks.onStderr = &agent::onStderr;
   callbacks.onError = &agent::onError;
   callbacks.onExit = &agent::onExit;

   // Set up process options
   core::system::ProcessOptions options;
   options.allowParentSuspend = true;
   options.exitWithParent = true;
   options.callbacksRequireMainThread = true; // TODO: It'd be nice to drop this requirement!
   options.reportHasSubprocs = false;
   
#ifndef _WIN32
   options.detachSession = true;
#else
   options.detachProcess = true;
#endif

   // Check for and run a custom assistant helper script if provided.
   // TODO: assistantHelper vs. copilotHelper?
   FilePath assistantHelper = session::options().copilotHelper();
   if (!assistantHelper.isEmpty())
   {
      if (!assistantHelper.exists())
         return fileNotFoundError(assistantHelper, ERROR_LOCATION);
      
      // TODO: Update these environment variables.
      FilePath assistantPath = assistantLanguageServerPath();
      environment.push_back(std::make_pair("RSTUDIO_NODE_PATH", nodePath.getAbsolutePath()));
      environment.push_back(std::make_pair("RSTUDIO_COPILOT_PATH", assistantPath.getAbsolutePath()));
      options.environment = environment;
      error = module_context::processSupervisor().runProgram(
               assistantHelper.getAbsolutePath(),
               {},
               options,
               callbacks);
   }
   else
   {
      FilePath assistantPath = assistantLanguageServerPath();
      if (!assistantPath.exists())
         return fileNotFoundError(assistantPath, ERROR_LOCATION);

      options.workingDir = assistantPath.getParent();
      options.environment = environment;

      std::vector<std::string> args;
      args.push_back(assistantPath.getAbsolutePath());
      args.push_back("--stdio");
      error = module_context::processSupervisor().runProgram(
               nodePath.getAbsolutePath(),
               args,
               options,
               callbacks);
   }
   
   if (error)
   {
      setAgentRuntimeStatus(AgentRuntimeStatus::Unknown);
      return error;
   }
   

   // Wait for the process to start.
   //
   // We include this because all requests will fail if we haven't yet called
   // initialized, so maybe the right approach is to have some sort of 'ensureInitialized'
   // method?
   s_agentStartupError = std::string();
   waitFor([]() { return s_agentPid != -1; });
   if (s_agentPid == -1)
   {
      s_agentRuntimeStatus = AgentRuntimeStatus::Unknown;
      return Error(boost::system::errc::no_such_process, ERROR_LOCATION);
   }
   
   // Send an initialize request to the agent.
   json::Object clientInfoJson;
   clientInfoJson["name"] = "RStudio";
   clientInfoJson["version"] = RSTUDIO_VERSION;
   
   json::Object initializationOptionsJson;
   initializationOptionsJson["editorInfo"] = clientInfoJson;
   initializationOptionsJson["editorPluginInfo"] = clientInfoJson;
   
   json::Object paramsJson;
   paramsJson["processId"] = ::getpid();
   paramsJson["locale"] = prefs::userPrefs().uiLanguage();
   paramsJson["initializationOptions"] = initializationOptionsJson;

   // TODO: Does this really need to be a preference? Seems reasonable to just always include the project as a workspace URI.
   std::string workspaceFolderURI = lsp::uriFromDocumentPath(projects::projectContext().directory());

   json::Object workspaceJson;
   workspaceJson["workspaceFolders"] = !workspaceFolderURI.empty();

   json::Object capabilitiesJson;
   capabilitiesJson["workspace"] = workspaceJson;
   paramsJson["capabilities"] = capabilitiesJson;

   if (!workspaceFolderURI.empty())
   {
      json::Object workspaceFolderJson;
      workspaceFolderJson["uri"] = workspaceFolderURI;
      json::Array workspaceFoldersJsonArray;
      workspaceFoldersJsonArray.push_back(workspaceFolderJson);
      paramsJson["workspaceFolders"] = workspaceFoldersJsonArray;
   }
   
   // set up continuation after we've finished initializing
   auto initializedCallback = [=](const Error& error, json::JsonRpcResponse* pResponse)
   {
      if (error)
      {
         s_agentRuntimeStatus = AgentRuntimeStatus::Unknown;
         LOG_ERROR(error);
         return;
      }
      
      // newer versions of Assistant require an 'initialized' notification, which is
      // then used as a signal that they should start the agent process
      sendNotification("initialized", json::Object());

      // TODO: Posit AI?
      setCopilotConfiguration();
   };
   
   std::string requestId = core::system::generateUuid();
   sendRequest("initialize", requestId, paramsJson, AssistantContinuation(initializedCallback));

   // Okay, we're ready to go.
   return Success();
}

bool ensureAgentRunning(Error* pAgentLaunchError = nullptr)
{
   // TODO: Should we further validate the PID is actually associated
   // with a running Assistant process, or just handle that separately?
   if (s_agentPid != -1)
   {
      //DLOG("Assistant is already running; nothing to do.");
      return true;
   }

   // bail if we're shutting down
   if (s_isSessionShuttingDown)
      return false;

   // bail if we haven't enabled assistant
   if (!s_assistantEnabled)
   {
      if (!s_assistantInitialized)
      {
         DLOG("Assistant is not enabled; not starting agent.");
         s_assistantInitialized = true;
      }
      return false;
   }

   // bail if we're not on the main thread; we make use of R when attempting
   // to start R so we cannot safely start on a child thread
   if (!thread::isMainThread())
      return false;
   
   // preflight checks passed; try to start the agent
   Error error = startAgent();
   if (error)
      LOG_ERROR(error);
   
   if (pAgentLaunchError)
      *pAgentLaunchError = error;
   
   s_assistantInitialized = true;
   return error == Success();
}

void docOpened(const std::string& uri,
               int64_t version,
               const std::string& languageId,
               const std::string& contents)
{
   lsp::DidOpenTextDocumentParams params = {
      .textDocument = {
         .uri        = uri,
         .languageId = languageId,
         .version    = version,
         .text       = contents
      }
   };

   sendNotification("textDocument/didOpen", lsp::toJson(params));
}

void docClosed(const std::string& uri)
{
   lsp::DidCloseTextDocumentParams params = {
      .textDocument = {
         .uri = uri,
      }
   };

   sendNotification("textDocument/didClose", lsp::toJson(params));
}

// Send a textDocument/didChange notification with diff
void didChangeIncremental(const std::string& uri,
                          int version,
                          const lsp::Range& range,
                          const std::string& text)
{
   lsp::TextDocumentContentChangeEvent event = {
      .range = range,
      .text = text,
   };

   lsp::DidChangeTextDocumentParams params = {
      .textDocument = {
         .uri = uri,
         .version = version,
      },
      .contentChanges = {
         {
            .range = range,
            .text = text,
         }
      }
   };

   sendNotification("textDocument/didChange", lsp::toJson(params));
}

namespace file_monitor {

namespace {

void indexFile(const core::FileInfo& info)
{
   // Don't index overly-large files
   if (info.size() >= kMaxIndexingFileSize)
      return;
   
   // Verify this file has an indexable type
   FilePath documentPath = module_context::resolveAliasedPath(info.absolutePath());
   if (!isIndexableFile(documentPath))
      return;
   
   std::string ext = documentPath.getExtensionLowerCase();
   std::string languageId = lsp::languageIdFromExtension(ext);
   if (languageId.empty())
      return;
      
   std::string contents;
   Error error = core::readStringFromFile(documentPath, &contents);
   if (error)
      return;
   
   DLOG("Indexing document: {}", info.absolutePath());
   
   std::string uri = lsp::uriFromDocumentPath(documentPath);
   auto version = lsp::documentVersionFromUri(uri);
   docOpened(
      uri,
      version,
      languageId,
      contents);
}

} // end anonymous namespace

void onMonitoringEnabled(const tree<core::FileInfo>& tree)
{
   if (s_assistantIndexingEnabled)
      for (auto&& file : tree)
         s_indexQueue.push_back(file);
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   if (s_assistantIndexingEnabled)
      for (auto&& event : events)
         s_indexQueue.push_back(event.fileInfo());
}

void onMonitoringDisabled()
{
}

} // end namespace file_monitor

void didOpen(lsp::DidOpenTextDocumentParams params)
{
   if (!ensureAgentRunning())
      return;
   
   boost::shared_ptr<source_database::SourceDocument> pDoc(new source_database::SourceDocument);
   Error error = lsp::sourceDocumentFromUri(params.textDocument.uri, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (!isIndexableDocument(pDoc))
      return;

   docOpened(
      params.textDocument.uri,
      params.textDocument.version,
      params.textDocument.languageId,
      params.textDocument.text);
}

void didChange(lsp::DidChangeTextDocumentParams params)
{
   if (!ensureAgentRunning())
      return;

   boost::shared_ptr<source_database::SourceDocument> pDoc(new source_database::SourceDocument);
   Error error = lsp::sourceDocumentFromUri(params.textDocument.uri, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (!isIndexableDocument(pDoc))
      return;

   for (auto&& contentChange : params.contentChanges)
   {
      didChangeIncremental(
         params.textDocument.uri,
         params.textDocument.version,
         contentChange.range,
         contentChange.text);
   }
}

void didClose(lsp::DidCloseTextDocumentParams params)
{
   if (!ensureAgentRunning())
      return;

   boost::shared_ptr<source_database::SourceDocument> pDoc(new source_database::SourceDocument);
   Error error = lsp::sourceDocumentFromUri(params.textDocument.uri, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (!isIndexableDocument(pDoc))
      return;

   docClosed(params.textDocument.uri);
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

      // Handle various notifications
      json::Value methodJson = responseJson["method"];
      std::string method;
      if (methodJson.isString())
      {
         method = methodJson.getString();
         if (method == "LogMessage" || method == "window/logMessage")
         {
            json::Value paramsJson = responseJson["params"];
            if (paramsJson.isObject())
            {
               json::Object params = paramsJson.getObject();
               json::Value messageJson = params["message"];
               if (messageJson.isString())
               {
                  DLOG("logMessage: {}", messageJson.getString());
               }
            }
            continue;
         }
         else if (method == "window/showMessageRequest")
         {
            std::string message = "undetermined";
            std::string type = "Error"; // assume the worst

            json::Value paramsJson = responseJson["params"];
            if (paramsJson.isObject())
            {
               json::Object params = paramsJson.getObject();
               json::Value messageJson = params["message"];
               json::Value typeJson = params["type"];

               if (messageJson.isString())
               {
                  message = messageJson.getString();
               }
               if (typeJson.isInt())
               {
                  int typeInt = typeJson.getInt();
                  if (typeInt == 1)
                     type = "Error";
                  else if (typeInt == 2)
                     type = "Warning";
                  else if (typeInt == 3)
                     type = "Info";
                  else if (typeInt == 4)
                     type = "Log";
                  else if (typeInt == 5)
                     type = "Debug";
               }
            }

            if (boost::iequals(type, "Error") ||
                boost::iequals(type, "Warning") ||
                boost::iequals(type, "Info"))
            {
               // TODO: Copilot vs. Assistant preference here?
               if (prefs::userPrefs().copilotShowMessages())
                  module_context::showErrorMessage("Copilot", message);
               else
                  ELOG("showMessageRequest ({}): '{}'", type, message);
            }
            else // Log, Debug, or unknown
            {
               DLOG("showMessageRequest ({}): '{}'", type, message);
            }
            continue;
         }
         else if (method == "didChangeStatus")
         {
            json::Value paramsJson = responseJson["params"];
            if (paramsJson.isObject())
            {
               std::string message;
               std::string kind;

               json::Object params = paramsJson.getObject();
               json::Value messageJson = params["message"];
               if (messageJson.isString())
                  message = messageJson.getString();

               json::Value kindJson = params["kind"];
               if (kindJson.isString())
                  kind = kindJson.getString();

                // Log at debug level to avoid spamming the logs with things such as
                // "You are not signed into GitHub."
                // https://github.com/rstudio/rstudio/issues/15910
                DLOG("didChangeStatus: '{}: {}'", kind, message);
            }
            continue;
         }
      }

      // Check the response id. This will be missing for notifications; we may receive
      // a flurry of progress notifications when requesting completions from Assistant.
      // We might want to handle these somehow. Perhaps hook them up to some status widget?
      json::Value requestIdJson = responseJson["id"];
      if (!requestIdJson.isString())
      {
         if (method.empty())
         {
            DLOG("Ignoring response with missing id.");
            continue;
         }
         else
         {
            DLOG("Ignoring notification, method='{}'.", method);
         }
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
   
   // index files in the queue
   if (isIdle && !s_indexQueue.empty())
   {
      auto start = boost::chrono::steady_clock::now();
      while (true)
      {
         // run on batch of files
         auto n = std::min(s_indexQueue.size(), std::size_t(s_indexBatchSize));
         for (std::size_t i = 0; i < n; i++)
            file_monitor::indexFile(s_indexQueue[i]);

         // remove those files
         s_indexQueue.erase(s_indexQueue.begin(), s_indexQueue.begin() + n);
         if (s_indexQueue.empty())
            break;
         
         // check for finished
         auto now = boost::chrono::steady_clock::now();
         auto elapsed = boost::chrono::duration_cast<boost::chrono::milliseconds>(now - start);
         if (elapsed.count() >= 100)
            break;
      }
   }
   
}

// TODO: Can we remove this?
bool subscribeToFileMonitor()
{
   session::projects::FileMonitorCallbacks callbacks;
   callbacks.onMonitoringEnabled = file_monitor::onMonitoringEnabled;
   callbacks.onFilesChanged = file_monitor::onFilesChanged;
   callbacks.onMonitoringDisabled = file_monitor::onMonitoringDisabled;
   projects::projectContext().subscribeToFileMonitor("Posit Assistant", callbacks);
   return true;
}

void synchronize()
{
   // Bail if we're shutting down.
   if (s_isSessionShuttingDown)
      return;

   // Update flags
   s_assistantEnabled = isAssistantEnabled();
   s_assistantIndexingEnabled = s_assistantEnabled && isAssistantIndexingEnabled();
   
   // Subscribe to file monitor if enabled
   if (s_assistantIndexingEnabled)
   {
      static bool once = subscribeToFileMonitor();
      (void) once;
   }
   
   // Start or stop the agent as appropriate
   if (s_assistantEnabled)
   {
      startAgent();
   }
   else
   {
      stopAgent();
   }
   
}

void onPreferencesSaved()
{
   synchronize();
}

void onProjectOptionsUpdated()
{
   // Update internal cache of project options
   Error error = projects::projectContext().readAssistantOptions(&s_assistantProjectOptions);
   if (error)
      LOG_ERROR(error);
   
   // Synchronize other flags
   synchronize();
}

void onUserPrefsChanged(const std::string& layer,
                        const std::string& name)
{
   // TODO: Check if posit assistant enabled?
   if (name == kCopilotEnabled)
   {
      synchronize();
   }
}

void onDeferredInit(bool newSession)
{
   lsp::events().didOpen.connect(didOpen);
   lsp::events().didChange.connect(didChange);
   lsp::events().didClose.connect(didClose);
}

void onShutdown(bool)
{
   // Note that we're about to shut down.
   s_isSessionShuttingDown = true;

   // Shut down the agent.
   stopAgentSync();
}

// Primarily intended for debugging / exploration.
SEXP rs_assistantSendRequest(SEXP methodSEXP, SEXP paramsSEXP)
{
   std::string method = r::sexp::asString(methodSEXP);
   
   json::Object paramsJson;
   if (r::sexp::length(paramsSEXP) != 0)
   {
      Error error = r::json::jsonValueFromObject(paramsSEXP, &paramsJson);
      if (error)
      {
         LOG_ERROR(error);
         return R_NilValue;
      }
   }
   
   std::string requestId = core::system::generateUuid();
   json::Object responseJson = sendSynchronousRequest(method, requestId, paramsJson);
   
   r::sexp::Protect protect;
   return r::sexp::create(responseJson, &protect);
}

SEXP rs_assistantSetLogLevel(SEXP logLevelSEXP)
{
   int logLevel = r::sexp::asInteger(logLevelSEXP);
   s_assistantLogLevel = logLevel;
   return logLevelSEXP;
}

std::string assistantVersion()
{
   std::string requestId = core::system::generateUuid();
   json::Object responseJson = sendSynchronousRequest("getVersion", requestId, json::Object());
   
   std::string version;
   if (responseJson.hasMember("result"))
   {
      json::Object resultJson = responseJson["result"].getObject();
      if (resultJson.hasMember("version"))
      {
         json::Value versionJson = resultJson["version"];
         if (versionJson.isString())
         {
            version = versionJson.getString();
         }
      }
   }
   
   return version;
}

SEXP rs_assistantVersion()
{
   if (!isAssistantEnabled())
      return R_NilValue;
   
   std::string version = assistantVersion();
   r::sexp::Protect protect;
   return r::sexp::create(version, &protect);
}

SEXP rs_assistantStopAgent()
{
   // stop the agent
   bool stopped = stopAgentSync();
 
   // return status
   return Rf_ScalarLogical(stopped);
}

Error assistantVerifyInstalled(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   json::Object responseJson;
   pResponse->setResult(isAgentInstalled());
   return Success();
}

Error assistantDiagnostics(const json::JsonRpcRequest& request,
                           const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }
   
   std::string requestId = core::system::generateUuid();
   sendRequest("debug/diagnostics", requestId, json::Object(), AssistantContinuation(continuation));
   
   return Success();
}

Error assistantGenerateCompletions(const json::JsonRpcRequest& request,
                                   const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Read params
   std::string documentId;
   std::string documentPath;
   bool isUntitled;
   bool autoInvoked;
   int cursorRow, cursorColumn;

   Error error = core::json::readParams(
            request.params,
            &documentId,
            &documentPath,
            &isUntitled,
            &autoInvoked,
            &cursorRow,
            &cursorColumn);
   
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // Resolve source document from id
   auto pDoc = boost::make_shared<source_database::SourceDocument>();

   error = source_database::get(documentId, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // Disallow completion request in hidden files, since this might trigger
   // the agent to attempt to read the contents of that file
   if (!isIndexableDocument(pDoc))
   {
      json::Object resultJson;
      resultJson["enabled"] = false;
      
      json::JsonRpcResponse response;
      response.setResult(resultJson);
      
      continuation(Success(), &response);
      return Success();
   }

   // Build completion request
   json::Object positionJson;
   positionJson["line"] = cursorRow;
   positionJson["character"] = cursorColumn;

   json::Object docJson;
   auto uri = lsp::uriFromDocument(pDoc);
   docJson["uri"] = uri;
   docJson["version"] = lsp::documentVersionFromUri(uri);

   json::Object contextJson;
   contextJson["triggerKind"] = autoInvoked ? 
         kAssistantCompletionTriggerAutomatic : kAssistantCompletionTriggerUserInvoked;

   json::Object paramsJson;
   paramsJson["textDocument"] = docJson;
   paramsJson["position"] = positionJson;
   paramsJson["context"] = contextJson;

   // Send the request
   std::string requestId = core::system::generateUuid();
   sendRequest("textDocument/inlineCompletion", requestId, paramsJson, AssistantContinuation(continuation));

   return Success();
}

// Structure to hold state for NES retry logic
struct NesRetryState
{
   json::JsonRpcFunctionContinuation continuation;
   boost::shared_ptr<source_database::SourceDocument> pDoc;
   std::string uri;
   int documentVersion;
   int originalRow;    // The original cursor row (doesn't change)
   int currentRow;     // The current row being queried (changes with retries)
   int column;
   int retryCount;
   int sequenceNumber;
   int documentLineCount;
};

// Helper function to check if a NES response contains edit suggestions
bool nesResponseHasEdits(json::Object responseJson)
{
   if (!responseJson.hasMember("result"))
      return false;

   json::Value resultValue = responseJson["result"];
   if (!resultValue.isObject())
      return false;

   json::Object result = resultValue.getObject();
   if (!result.hasMember("edits"))
      return false;

   json::Value editsValue = result["edits"];
   if (!editsValue.isArray())
      return false;

   json::Array edits = editsValue.getArray();
   return !edits.isEmpty();
}

// Forward declaration
void sendNesRequestWithRetry(boost::shared_ptr<NesRetryState> state);

// Callback handler for NES requests that implements retry logic
class NesContinuation
{
public:
   explicit NesContinuation(boost::shared_ptr<NesRetryState> state)
      : state_(state)
   {
   }

   void invoke(json::Object responseJson)
   {
      // Check if this request has been cancelled (a newer request has started)
      if (s_nesRequestSequence.load() != state_->sequenceNumber)
      {
         DLOG("NES request {} cancelled (current sequence: {})",
              state_->sequenceNumber, s_nesRequestSequence.load());

         // Return a cancelled response
         json::Object cancelledResult;
         cancelledResult["cancelled"] = true;

         json::JsonRpcResponse response;
         response.setResult(cancelledResult);

         if (state_->continuation)
            state_->continuation(Success(), &response);
         return;
      }

      // Check if we got edit suggestions
      if (nesResponseHasEdits(responseJson))
      {
         DLOG("NES request {} got suggestions on attempt {}",
              state_->sequenceNumber, state_->retryCount + 1);

         // We have results - pass them to the original continuation
         responseJson["cancelled"] = false;

         json::JsonRpcResponse response;
         response.setResult(responseJson);

         if (state_->continuation)
            state_->continuation(Success(), &response);
         return;
      }

      // No suggestions - should we retry?
      if (state_->retryCount < kNesMaxRetries)
      {
         // Calculate new position for retry (linear offset from original position)
         int newRow = state_->originalRow + ((state_->retryCount + 1) * kNesRetryLineOffset);

         // Don't retry if we'd go past the end of the document
         if (newRow >= state_->documentLineCount)
         {
            DLOG("NES request {} no more retries (would exceed document length: {} >= {})",
                 state_->sequenceNumber, newRow, state_->documentLineCount);

            // Return empty result
            responseJson["cancelled"] = false;

            json::JsonRpcResponse response;
            response.setResult(responseJson);

            if (state_->continuation)
               state_->continuation(Success(), &response);
            return;
         }

         DLOG("NES request {} retrying at row {} (attempt {})",
              state_->sequenceNumber, newRow, state_->retryCount + 2);

         // Update state for retry
         state_->retryCount++;
         state_->currentRow = newRow;

         // Send another request
         sendNesRequestWithRetry(state_);
         return;
      }

      DLOG("NES request {} exhausted retries", state_->sequenceNumber);

      // No more retries - return the (empty) result
      responseJson["cancelled"] = false;

      json::JsonRpcResponse response;
      response.setResult(responseJson);

      if (state_->continuation)
         state_->continuation(Success(), &response);
   }

   void cancel()
   {
      json::Object resultJson;
      resultJson["cancelled"] = true;

      json::JsonRpcResponse response;
      response.setResult(resultJson);

      if (state_->continuation)
         state_->continuation(Success(), &response);
   }

   boost::posix_time::ptime time()
   {
      return time_;
   }

private:
   boost::shared_ptr<NesRetryState> state_;
   boost::posix_time::ptime time_ = boost::posix_time::second_clock::local_time();
};

// Send a NES request with retry support
void sendNesRequestWithRetry(boost::shared_ptr<NesRetryState> state)
{
   // Check for cancellation before sending
   if (s_nesRequestSequence.load() != state->sequenceNumber)
   {
      DLOG("NES request {} cancelled before sending", state->sequenceNumber);

      json::Object cancelledResult;
      cancelledResult["cancelled"] = true;

      json::JsonRpcResponse response;
      response.setResult(cancelledResult);

      if (state->continuation)
         state->continuation(Success(), &response);
      return;
   }

   // Build the request
   json::Object positionJson;
   positionJson["line"] = state->currentRow;
   positionJson["character"] = state->column;

   json::Object docJson;
   docJson["uri"] = state->uri;
   docJson["version"] = state->documentVersion;

   json::Object contextJson;
   contextJson["triggerKind"] = kAssistantCompletionTriggerAutomatic;

   json::Object paramsJson;
   paramsJson["textDocument"] = docJson;
   paramsJson["position"] = positionJson;
   paramsJson["context"] = contextJson;

   // Create the continuation that will handle retries
   NesContinuation nesContinuation(state);

   // Wrap in a AssistantContinuation-compatible lambda
   auto continuation = [nesContinuation](const Error& error, json::JsonRpcResponse* pResponse) mutable
   {
      if (error)
      {
         nesContinuation.cancel();
         return;
      }

      if (pResponse && pResponse->result().isObject())
      {
         nesContinuation.invoke(pResponse->result().getObject());
      }
      else
      {
         // No valid response - treat as empty
         json::Object emptyResponse;
         nesContinuation.invoke(emptyResponse);
      }
   };

   // Send the request
   std::string requestId = core::system::generateUuid();
   sendRequest("textDocument/copilotInlineEdit", requestId, paramsJson, AssistantContinuation(continuation));
}

Error assistantNextEditSuggestions(const json::JsonRpcRequest& request,
                                   const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Read params
   std::string documentId;
   std::string documentPath;
   bool isUntitled;
   int cursorRow, cursorColumn;

   Error error = core::json::readParams(
            request.params,
            &documentId,
            &documentPath,
            &isUntitled,
            &cursorRow,
            &cursorColumn);

   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // Resolve source document from id
   auto pDoc = boost::make_shared<source_database::SourceDocument>();
   error = source_database::get(documentId, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // Disallow completion request in hidden files, since this might trigger
   // the agent to attempt to read the contents of that file
   if (!isIndexableDocument(pDoc))
   {
      json::Object resultJson;
      resultJson["enabled"] = false;

      json::JsonRpcResponse response;
      response.setResult(resultJson);

      continuation(Success(), &response);
      return Success();
   }

   // Increment the sequence number to cancel any in-flight requests
   int sequenceNumber = ++s_nesRequestSequence;

   DLOG("Starting NES request {} at row {}", sequenceNumber, cursorRow);

   // Get document line count for bounds checking during retries
   std::string contents = pDoc->contents();
   int lineCount = 1;
   for (char c : contents)
   {
      if (c == '\n')
         lineCount++;
   }

   // Create the retry state
   auto state = boost::make_shared<NesRetryState>();
   state->continuation = continuation;
   state->pDoc = pDoc;
   state->uri = lsp::uriFromDocument(pDoc);
   state->documentVersion = lsp::documentVersionFromUri(state->uri);
   state->originalRow = cursorRow;
   state->currentRow = cursorRow;
   state->column = cursorColumn;
   state->retryCount = 0;
   state->sequenceNumber = sequenceNumber;
   state->documentLineCount = lineCount;

   // Send the initial request with retry support
   sendNesRequestWithRetry(state);

   return Success();
}


Error assistantSignIn(const json::JsonRpcRequest& request,
                      const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Send sign in request
   std::string requestId = core::system::generateUuid();
   sendRequest("signInInitiate", requestId, json::Object(), AssistantContinuation(continuation));

   return Success();
}

Error assistantSignOut(const json::JsonRpcRequest& request,
                       const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }

   // Send sign out request
   std::string requestId = core::system::generateUuid();
   sendRequest("signOut", requestId, json::Object(), AssistantContinuation(continuation));
   return Success();
}

Error assistantStatus(const json::JsonRpcRequest& request,
                      const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure assistant is running
   Error launchError;
   if (!ensureAgentRunning(&launchError))
   {
      json::JsonRpcResponse response;
      json::Object resultJson;
      
      if (launchError)
      {
         json::Object errorJson;
         launchError.writeJson(&errorJson);
         resultJson["reason"] = static_cast<int>(AgentNotRunningReason::LaunchError);
         resultJson["error"] = errorJson;
         resultJson["output"] = s_agentStartupError;
      }
      else
      {
         resultJson["reason"] = static_cast<int>(s_agentNotRunningReason);
      }
      
      response.setResult(resultJson);
      continuation(Success(), &response);
      return Success();
   }

   // Send status request
   std::string requestId = core::system::generateUuid();
   sendRequest("checkStatus", requestId, json::Object(), AssistantContinuation(continuation));
   return Success();
}

Error assistantDocFocused(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      // nothing to do if we can't connect to the agent
      return Success();
   }

   // Read params
   std::string documentId;
   Error error = core::json::readParams(request.params, &documentId);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // Resolve source document from id
   auto pDoc = boost::make_shared<source_database::SourceDocument>();
   error = source_database::get(documentId, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // If document is NOT indexable we tell the agent that no file has focus via an empty request.
   // This is to prevent the agent from attempting to read the contents of the file.
   json::Object paramsJson;
   if (isIndexableDocument(pDoc))
   {
      lsp::DidFocusTextDocumentParams params = {
         .textDocument = {
            .uri = lsp::uriFromDocument(pDoc),
         }
      };
      
      paramsJson = lsp::toJson(params);
   }

   sendNotification("textDocument/didFocus", paramsJson);
   return Success();
}

Error assistantDidShowCompletion(const json::JsonRpcRequest& request,
                                 json::JsonRpcResponse* pResponse)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      // nothing to do if we can't connect to the agent
      return Success();
   }

   // Read params
   json::Object completionJson;
   Error error = core::json::readParams(request.params, &completionJson);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   json::Object paramsJson;
   paramsJson["item"] = completionJson;
   sendNotification("textDocument/didShowCompletion", paramsJson);
   return Success();
}

Error assistantDidAcceptCompletion(const json::JsonRpcRequest& request,
                                   json::JsonRpcResponse* pResponse)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      // nothing to do if we can't connect to the agent
      return Success();
   }

   // Read params
   json::Object completionCommandJson;
   Error error = core::json::readParams(request.params, &completionCommandJson);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   sendNotification("workspace/executeCommand", completionCommandJson);
   return Success();
}

Error assistantDidAcceptPartialCompletion(const json::JsonRpcRequest& request,
                                          json::JsonRpcResponse* pResponse)
{
   // Make sure assistant is running
   if (!ensureAgentRunning())
   {
      // nothing to do if we can't connect to the agent
      return Success();
   }

   // Read params
   json::Object partialCompletionJson;
   int acceptedLength;
   Error error = core::json::readParams(request.params, &partialCompletionJson, &acceptedLength);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   json::Object paramsJson;
   paramsJson["item"] = partialCompletionJson;
   paramsJson["acceptedLength"] = acceptedLength;
   sendNotification("textDocument/didPartiallyAcceptCompletion", paramsJson);
   return Success();
}

Error assistantRegisterOpenFiles(const json::JsonRpcRequest& request,
                                 json::JsonRpcResponse* pResponse)
{
   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   for (const json::Value& val : paths)
      s_indexQueue.push_back(FileInfo(FilePath(val.getString())));

   return Success();
}

} // end anonymous namespace


Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // Read default log level
   // TODO: Also PAI_LOG_LEVEL?
   std::string assistantLogLevel = core::system::getenv("COPILOT_LOG_LEVEL");
   if (!assistantLogLevel.empty())
      s_assistantLogLevel = safe_convert::stringTo<int>(assistantLogLevel, 0);
   
   // Read project options
   if (projects::projectContext().hasProject())
   {
      Error error = projects::projectContext().readAssistantOptions(&s_assistantProjectOptions);
      if (error)
         LOG_ERROR(error);
   }
    
   // Synchronize user + project preferences with internal caches
   synchronize();

   events().onBackgroundProcessing.connect(onBackgroundProcessing);
   events().onPreferencesSaved.connect(onPreferencesSaved);
   events().onProjectOptionsUpdated.connect(onProjectOptionsUpdated);
   events().onDeferredInit.connect(onDeferredInit);
   events().onShutdown.connect(onShutdown);

   // TODO: Do we need this _and_ the preferences saved callback?
   // This one seems required so that we see preference changes while
   // editting preferences within the Assistant prefs dialog, anyhow.
   prefs::userPrefs().onChanged.connect(onUserPrefsChanged);

   RS_REGISTER_CALL_METHOD(rs_assistantSendRequest);
   RS_REGISTER_CALL_METHOD(rs_assistantSetLogLevel);
   RS_REGISTER_CALL_METHOD(rs_assistantVersion);
   RS_REGISTER_CALL_METHOD(rs_assistantStopAgent);

   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(registerAsyncRpcMethod, "assistant_diagnostics", assistantDiagnostics))
         (bind(registerAsyncRpcMethod, "assistant_generate_completions", assistantGenerateCompletions))
         (bind(registerAsyncRpcMethod, "assistant_next_edit_suggestions", assistantNextEditSuggestions))
         (bind(registerAsyncRpcMethod, "assistant_sign_in", assistantSignIn))
         (bind(registerAsyncRpcMethod, "assistant_sign_out", assistantSignOut))
         (bind(registerAsyncRpcMethod, "assistant_status", assistantStatus))
         (bind(registerRpcMethod, "assistant_verify_installed", assistantVerifyInstalled))
         (bind(registerRpcMethod, "assistant_doc_focused", assistantDocFocused))
         (bind(registerRpcMethod, "assistant_did_show_completion", assistantDidShowCompletion))
         (bind(registerRpcMethod, "assistant_did_accept_completion", assistantDidAcceptCompletion))
         (bind(registerRpcMethod, "assistant_did_accept_partial_completion", assistantDidAcceptPartialCompletion))
         (bind(registerRpcMethod, "assistant_register_open_files", assistantRegisterOpenFiles))
         (bind(sourceModuleRFile, "SessionAssistant.R"))
         (bind(sourceModuleRFile, "SessionCopilot.R"))
         ;
   return initBlock.execute();

}

} // end namespace assistant
} // end namespace modules
} // end namespace session
} // end namespace rstudio
