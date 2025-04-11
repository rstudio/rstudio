/*
 * SessionCopilot.cpp
 *
 * Copyright (C) 2023 by Posit Software, PBC
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
#include <core/FileSerializer.hpp>
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

#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

#include "session-config.h"

#define COPILOT_LOG_IMPL(__LOGGER__, __FMT__, ...)                             \
   do                                                                          \
   {                                                                           \
      std::string __message__ = fmt::format(__FMT__, ##__VA_ARGS__);               \
      std::string __formatted__ =                                                  \
          fmt::format("[{}]: {}", __func__, __message__);                          \
      __LOGGER__("copilot", __formatted__);                                        \
      if (copilotLogLevel() >= 1)                                              \
         std::cerr << __formatted__ << std::endl;                                  \
   } while (0)

#define DLOG(__FMT__, ...) COPILOT_LOG_IMPL(LOG_DEBUG_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...) COPILOT_LOG_IMPL(LOG_WARNING_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...) COPILOT_LOG_IMPL(LOG_ERROR_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)

// Use a default section of 'copilot' for errors / warnings
#ifdef LOG_ERROR
# undef LOG_ERROR
# define LOG_ERROR(error) LOG_ERROR_NAMED("copilot", error)
#endif

#define kCopilotDefaultDocumentVersion (0)
#define kMaxIndexingFileSize (1048576)

// completion was triggered explicitly by a user gesture
#define kCopilotCompletionTriggerUserInvoked (1)

// completion was triggered automatically while editing
#define kCopilotCompletionTriggerAutomatic (2)

using namespace rstudio::core;
using namespace rstudio::core::system;

namespace rstudio {
namespace session {
namespace modules {
namespace copilot {

namespace {

// https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocumentItem
std::map<std::string, std::string> s_extToLanguageIdMap = {
   { ".abap",  "abap" },
   { ".bash",  "shellscript" },
   { ".bat",   "bat" },
   { ".bib",   "bibtex" },
   { ".c",     "c" },
   { ".cc",    "cpp" },
   { ".clj",   "clojure"},
   { ".coffee","coffeescript" },
   { ".cpp",   "cpp" },
   { ".cs",    "csharp" },
   { ".css",   "css" },
   { ".dart",  "dart" },
   { ".diff",  "diff" },
// { "",       "dockerfile" }, (special handling due to lack of extension)
   { ".erl",   "erlang" },
   { ".etx",   "tex" },
   { ".ex",    "elixir" },
   { ".fs",    "fsharp" },
   { ".go",    "go" },
   { ".groovy","groovy" },
   { ".h",     "c" },
   { ".hbs",   "handlebars" },
   { ".hpp",   "cpp" },
   { ".html",  "html" },
   { ".ini",   "ini" },
   { ".jade",  "jade" },
   { ".java",  "java" },
   { ".js",    "javascript" },
   { ".jsx",   "javascriptreact" },
   { ".json",  "json" },
   { ".less",  "less" },
   { ".lua",   "lua" },
   { ".m",     "objective-c" },
   { ".md",    "markdown" },
   { ".mjs",   "javascript" },
   { ".ps",    "powershell" },
   { ".mk",    "makefile" }, // (special handling for extensionless "makefile" / "Makefile")
   { ".mm",    "objective-cpp" },
   { ".php",   "php" },
   { ".pl",    "perl" },
   { ".pl6",   "perl6" },
   { ".pug",   "jade" },
   { ".py",    "python" },
   { ".qmd",   "quarto" },
   { ".r",     "r" },
   { ".razor", "razor" },
   { ".rb",    "ruby" },
   { ".rmd",   "r" },
   { ".rnb",   "r" },
   { ".rnw",   "r" },
   { ".rs",    "rust" },
   { ".sass",  "sass" },
   { ".sc",    "scala" },
   { ".scala", "scala" },
   { ".scss",  "scss" },
   { ".sh",    "shellscript" },
   { ".shader","shaderlab" },
   { ".sql",   "sql" },
   { ".swift", "swift" },
   { ".tex",   "latex" },
   { ".toml",  "toml" },
   { ".ts",    "typescript" },
   { ".tsx",   "typescriptreact" },
   { ".vb",    "vb" },
   { ".xml",   "xml" },
   { ".xsl",   "xsl" },
   { ".yml",   "yaml" },
};

std::map<std::string, std::string> makeLanguageIdToExtMap()
{
   std::map<std::string, std::string> map;
   for (auto&& entry : s_extToLanguageIdMap)
      map[entry.second] = entry.first;
   return map;
}

std::map<std::string, std::string>& languageIdToExtMap()
{
   static auto instance = makeLanguageIdToExtMap();
   return instance;
}

struct CopilotRequest
{
   std::string method;
   std::string id;
   json::Value params;
};

class CopilotContinuation
{
public:

   explicit CopilotContinuation(const json::JsonRpcFunctionContinuation& continuation)
      : continuation_(continuation),
        time_(boost::posix_time::second_clock::local_time())
   {
   }
   
   // default ctor needed for compatibility with map
   CopilotContinuation()
      : CopilotContinuation(json::JsonRpcFunctionContinuation())
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

enum class CopilotAgentNotRunningReason {
   Unknown,
   NotInstalled,
   DisabledByAdministrator,
   DisabledViaProjectPreferences,
   DisabledViaGlobalPreferences,
   LaunchError,
};

enum class CopilotAgentRuntimeStatus {
   Unknown,
   Preparing,
   Starting,
   Running,
   Stopping,
   Stopped,
};

// The log level for Copilot-specific logs. Primarily for developer use.
int s_copilotLogLevel = 0;

// Whether Copilot is enabled.
bool s_copilotEnabled = false;

// Whether Copilot has been allowed to index project files.
bool s_copilotIndexingEnabled = false;

// Have we checked the config files at least once
bool s_copilotInitialized = false;

// The PID of the active Copilot agent process.
PidType s_agentPid = -1;

// Error output (if any) that was written during startup.
std::string s_agentStartupError;

// The current status of the Copilot agent, mainly around if it's enabled
// (and why or why not).
CopilotAgentNotRunningReason s_agentNotRunningReason = CopilotAgentNotRunningReason::Unknown;

// The current runtime status of the Copilot agent process.
CopilotAgentRuntimeStatus s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Unknown;

// Whether or not we've handled the Copilot 'initialized' notification.
// Primarily done to allow proper sequencing of Copilot callbacks.
bool s_agentInitialized = false;

// A queue of pending requests, to be sent via the agent's stdin.
std::vector<CopilotRequest> s_pendingRequests;

// Metadata related to pending requests. Mainly used to map
// responses to their expected result types.
std::map<std::string, CopilotContinuation> s_pendingContinuations;

// A queue of pending responses, sent via the agent's stdout.
std::queue<std::string> s_pendingResponses;

// Whether we're about to shut down.
bool s_isSessionShuttingDown = false;

// Project-specific Copilot options.
projects::RProjectCopilotOptions s_copilotProjectOptions;

// A queue of pending files to be indexed.
std::vector<FileInfo> s_indexQueue;
std::size_t s_indexBatchSize = 200;

int copilotLogLevel()
{
   return s_copilotLogLevel;
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
   return s_extToLanguageIdMap.count(ext);
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
      return languageIdToExtMap().count(type);
   }
   
   // Otherwise, check for known files / extensions.
   FilePath docPath(pDoc->path());
   return isIndexableFile(docPath);
}

FilePath copilotLanguageServerPath()
{
   FilePath copilotPath;
   
   // first check RSTUDIO_COPILOT_FOLDER environment variable; if used, must point at
   // the folder containing the copilot-language-server executable, but not include the
   // executable itself
   std::string rstudioCopilot = core::system::getenv("RSTUDIO_COPILOT_FOLDER");
   if (!rstudioCopilot.empty())
   {
      if (FilePath::exists(rstudioCopilot) && FilePath(rstudioCopilot).isDirectory())
      {
         copilotPath = FilePath(rstudioCopilot);
      }
   }
   
   if (copilotPath.isEmpty())
   {
      copilotPath = session::options().copilotPath();
      if (!copilotPath.exists() || !copilotPath.isDirectory())
      {
         ELOG("Copilot Language Server path '{}' does not exist or is not a directory.", copilotPath.getAbsolutePath());
         return FilePath();
      }
   }

#if defined(_WIN32)
   auto suffix = "copilot-language-server.exe";
#elif defined(__APPLE__)
   auto suffix = "copilot-language-server";
   if (!isAppleSilicon())
      suffix = "copilot-language-server-x64";
#else // Linux
   auto suffix = "copilot-language-server";
#endif
   FilePath candidatePath = copilotPath.completePath(suffix);
   if (candidatePath.exists())
   {
      return candidatePath;
   }

   ELOG("Copilot Language Server executable not found at '{}'.", candidatePath.getAbsolutePath());
   return FilePath();
}

bool isCopilotEnabled()
{
#ifdef COPILOT_ENABLED

   // Check administrator option
   if (!session::options().copilotEnabled())
   {
      s_agentNotRunningReason = CopilotAgentNotRunningReason::DisabledByAdministrator;
      return false;
   }
   
   // Check project option
   switch (s_copilotProjectOptions.copilotEnabled)
   {
   
   case r_util::YesValue:
   {
      return true;
   }
      
   case r_util::NoValue:
   {
      s_agentNotRunningReason = CopilotAgentNotRunningReason::DisabledViaProjectPreferences;
      return false;
   }
      
   default: {}
      
   }

   // Check user preference
   if (!prefs::userPrefs().copilotEnabled())
   {
      s_agentNotRunningReason = CopilotAgentNotRunningReason::DisabledViaGlobalPreferences;
      return false;
   }
   
   return true;

#else // RStudio built without Copilot support
   s_agentNotRunningReason = CopilotAgentNotRunningReason::DisabledByAdministrator;
   return false;
#endif
}

bool isCopilotIndexingEnabled()
{
   // Check project option
   switch (s_copilotProjectOptions.copilotIndexingEnabled)
   {
   case r_util::YesValue: return true;
   case r_util::NoValue: return false;
   default: {}
   }

   // Check user preference
   return prefs::userPrefs().copilotIndexingEnabled();
}

std::string uriFromDocumentPath(const std::string& path)
{
   return fmt::format("file://{}", path);
}


std::string uriFromDocumentId(const std::string& id)
{
   return fmt::format("rstudio-document://{}", id);
}

std::string uriFromDocumentImpl(const std::string& id,
                                const std::string& path,
                                bool isUntitled)
{
   FilePath resolvedPath = module_context::resolveAliasedPath(path);
   return isUntitled ? uriFromDocumentId(id) : uriFromDocumentPath(resolvedPath.getAbsolutePath());
   
}
std::string uriFromDocument(const boost::shared_ptr<source_database::SourceDocument>& pDoc)
{
   return uriFromDocumentImpl(pDoc->id(), pDoc->path(), pDoc->isUntitled());
}

std::string languageIdFromDocument(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (pDoc->isRMarkdownDocument() || pDoc->isRFile())
      return "r";

   FilePath docPath(pDoc->path());
   std::string name = docPath.getFilename();
   std::string stem = docPath.getStem();
   if (name == "Makefile" || name == "makefile")
      return "makefile";
   else if (stem == "Dockerfile")
      return "dockerfile";
   
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

void sendNotification(const std::string& method,
                      const json::Value& paramsJson)
{
   s_pendingRequests.push_back({ method, "", paramsJson });
}

std::string formatRequest(const CopilotRequest& request)
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
                 const CopilotContinuation& continuation)
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
   
   sendRequest(method, requestId, paramsJson, CopilotContinuation(continuation));
   waitFor([&]() { return responseReceived; });
   
   return result;
}

void setConfiguration()
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

            if (s_copilotLogLevel > 0)
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
   DLOG("Copilot agent has started [PID = {}]", operations.getPid());
   s_agentPid = operations.getPid();
   s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Starting;
}

bool onContinue(ProcessOperations& operations)
{
   auto debugCallback = [](const std::string& htmlRequest)
   {
      if (copilotLogLevel() >= 2)
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
      // use expel_if to only process requests related to Copilot initialization
      core::algorithm::expel_if(s_pendingRequests, [&](const CopilotRequest& request)
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
         
         if (copilotLogLevel() >= 2)
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

      if (copilotLogLevel() >= 2)
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
   s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Running;
}

void onStderr(ProcessOperations& operations, const std::string& stdErr)
{
   LOG_ERROR_MESSAGE_NAMED("copilot", stdErr);
   if (copilotLogLevel() >= 1)
      std::cerr << stdErr << std::endl;
 
   // If we get output from stderr while the agent is starting, that means
   // something went wrong and we're about to shut down.
   switch (s_agentRuntimeStatus)
   {
   
   case CopilotAgentRuntimeStatus::Starting:
   case CopilotAgentRuntimeStatus::Stopping:
   {
      s_agentStartupError += stdErr;
      s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Stopping;
      break;
   }
 
   // TODO: Is there anything reasonable we can do with errors here?
   default: {}
      
   }

}

void onError(ProcessOperations& operations, const Error& error)
{
   s_agentPid = -1;
   s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Stopped;
}

void onExit(int status)
{
   s_agentPid = -1;
   s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Stopped;
}

} // end namespace agent

void stopAgent()
{
   if (s_agentPid == -1)
   {
      //DLOG("No agent running; nothing to do.");
      s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Stopped;
      return;
   }

   Error error = core::system::terminateProcess(s_agentPid);
   if (error)
      LOG_ERROR(error);
}

Error startAgent()
{
   if (s_agentRuntimeStatus != CopilotAgentRuntimeStatus::Unknown &&
       s_agentRuntimeStatus != CopilotAgentRuntimeStatus::Stopped)
   {
      return Success();
   }

   s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Preparing;

   Error error;

   // Create environment for agent process
   core::system::Options environment;
   core::system::environment(&environment);
   
   // Set NODE_EXTRA_CA_CERTS if a custom certificates file is provided.
   std::string certificatesFile = session::options().copilotSslCertificatesFile();
   if (!certificatesFile.empty())
      environment.push_back(std::make_pair("NODE_EXTRA_CA_CERTS", certificatesFile));

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

   // Run the Copilot Language Server. If RStudio has been configured with a custom
   // Copilot script, use that; otherwise, just run the Copilot Language Server directly.
   FilePath copilotHelper = session::options().copilotHelper();
   if (!copilotHelper.isEmpty())
   {
      if (!copilotHelper.exists())
         return fileNotFoundError(copilotHelper, ERROR_LOCATION);
      
      FilePath copilotPath = copilotLanguageServerPath();
      environment.push_back(std::make_pair("RSTUDIO_COPILOT_PATH", copilotPath.getAbsolutePath()));
      options.environment = environment;
      error = module_context::processSupervisor().runProgram(
               copilotHelper.getAbsolutePath(),
               {},
               options,
               callbacks);
   }
   else
   {
      FilePath copilotPath = copilotLanguageServerPath();
      if (!copilotPath.exists())
         return fileNotFoundError(copilotPath, ERROR_LOCATION);

      options.workingDir = copilotPath.getParent();
      options.environment = environment;

      std::vector<std::string> args;
      args.push_back("--stdio");
      error = module_context::processSupervisor().runProgram(
               copilotPath.getAbsolutePath(),
               args,
               options,
               callbacks);
   }
   
   if (error)
   {
      s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Unknown;
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
      s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Unknown;
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

   std::string workspaceFolderURI;
   if (prefs::userPrefs().copilotProjectWorkspace() && projects::projectContext().hasProject())
   {
      workspaceFolderURI = uriFromDocumentPath(projects::projectContext().directory().getAbsolutePath());
   }

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
         s_agentRuntimeStatus = CopilotAgentRuntimeStatus::Unknown;
         LOG_ERROR(error);
         return;
      }
      
      // newer versions of Copilot require an 'initialized' notification, which is
      // then used as a signal that they should start the agent process
      sendNotification("initialized", json::Object());
      setConfiguration();
   };
   
   std::string requestId = core::system::generateUuid();
   sendRequest("initialize", requestId, paramsJson, CopilotContinuation(initializedCallback));

   // Okay, we're ready to go.
   return Success();
}

bool ensureAgentRunning(Error* pAgentLaunchError = nullptr)
{
   // TODO: Should we further validate the PID is actually associated
   // with a running Copilot process, or just handle that separately?
   if (s_agentPid != -1)
   {
      //DLOG("Copilot is already running; nothing to do.");
      return true;
   }

   // bail if we haven't enabled copilot
   if (!s_copilotEnabled)
   {
      if (!s_copilotInitialized)
      {
         DLOG("Copilot is not enabled; not starting agent.");
         s_copilotInitialized = true;
      }
      return false;
   }

   // bail if we're shutting down
   if (s_isSessionShuttingDown)
   {
      if (!s_copilotInitialized)
      {
         DLOG("Session is shutting down; not starting agent.");
         s_copilotInitialized = true;
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
   
   s_copilotInitialized = true;
   return error == Success();
}

std::string contentsFromDocument(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   std::string contents = pDoc->contents();
   
   // for SQL documents, remove a 'preview' header to avoid confusing Copilot
   // into producing R completions in a SQL context
   // https://github.com/rstudio/rstudio/issues/13432
   if (pDoc->type() == kSourceDocumentTypeSQL)
   {
      boost::regex rePreview("(?:#+|[-]{2,})\\s*[!]preview[^\n]+\n");
      contents = boost::regex_replace(contents, rePreview, "\n");
   }
   
   return contents;
}

void onDocAdded(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!ensureAgentRunning())
      return;
   
   if (!isIndexableDocument(pDoc))
      return;
   
   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocument(pDoc);
   textDocumentJson["languageId"] = languageIdFromDocument(pDoc);
   textDocumentJson["version"] = kCopilotDefaultDocumentVersion;
   textDocumentJson["text"] = contentsFromDocument(pDoc);

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didOpen", paramsJson);
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
   
   std::string languageId;
   std::string ext = documentPath.getExtensionLowerCase();
   if (s_extToLanguageIdMap.count(ext))
      languageId = s_extToLanguageIdMap[ext];
      
   std::string contents;
   Error error = core::readStringFromFile(documentPath, &contents);
   if (error)
      return;
   
   DLOG("Indexing document: {}", info.absolutePath());
   
   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocumentPath(documentPath.getAbsolutePath());
   textDocumentJson["languageId"] = languageId;
   textDocumentJson["version"] = kCopilotDefaultDocumentVersion;
   textDocumentJson["text"] = contents;

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didOpen", paramsJson);
}

} // end anonymous namespace

void onMonitoringEnabled(const tree<core::FileInfo>& tree)
{
   if (s_copilotIndexingEnabled)
      for (auto&& file : tree)
         s_indexQueue.push_back(file);
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   if (s_copilotIndexingEnabled)
      for (auto&& event : events)
         s_indexQueue.push_back(event.fileInfo());
}

void onMonitoringDisabled()
{
}

} // end namespace file_monitor

void onDocUpdated(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!ensureAgentRunning())
      return;

   if (!isIndexableDocument(pDoc))
      return;
   
   // Synchronize document contents with Copilot
   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocument(pDoc);
   textDocumentJson["languageId"] = languageIdFromDocument(pDoc);
   textDocumentJson["version"] = kCopilotDefaultDocumentVersion;
   textDocumentJson["text"] = contentsFromDocument(pDoc);

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didOpen", paramsJson);
}

void onDocRemoved(boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   if (!ensureAgentRunning())
      return;

   json::Object textDocumentJson;
   textDocumentJson["uri"] = uriFromDocument(pDoc);

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
               if (prefs::userPrefs().copilotShowMessages())
                  module_context::showErrorMessage("GitHub Copilot", message);
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

                if (boost::iequals(kind, "Error"))
                  ELOG("didChangeStatus: '{}'", message);
                else if (boost::iequals(kind, "Warning") || boost::iequals(kind, "Inactive"))
                  WLOG("didChangeStatus: '{}'", message);
            }
            continue;
         }
      }

      // Check the response id. This will be missing for notifications; we may receive
      // a flurry of progress notifications when requesting completions from Copilot.
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


bool subscribeToFileMonitor()
{
   session::projects::FileMonitorCallbacks callbacks;
   callbacks.onMonitoringEnabled = file_monitor::onMonitoringEnabled;
   callbacks.onFilesChanged = file_monitor::onFilesChanged;
   callbacks.onMonitoringDisabled = file_monitor::onMonitoringDisabled;
   projects::projectContext().subscribeToFileMonitor("Copilot indexing", callbacks);
   return true;
}

void synchronize()
{
   // Update flags
   s_copilotEnabled = isCopilotEnabled();
   s_copilotIndexingEnabled = s_copilotEnabled && isCopilotIndexingEnabled();
   
   // Subscribe to file monitor if enabled
   if (s_copilotIndexingEnabled)
   {
      static bool once = subscribeToFileMonitor();
      (void) once;
   }
   
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

void onPreferencesSaved()
{
   synchronize();
}

void onProjectOptionsUpdated()
{
   // Update internal cache of project options
   Error error = projects::projectContext().readCopilotOptions(&s_copilotProjectOptions);
   if (error)
      LOG_ERROR(error);
   
   // Synchronize other flags
   synchronize();
}

void onUserPrefsChanged(const std::string& layer,
                        const std::string& name)
{
   if (name == kCopilotEnabled)
   {
      synchronize();
   }
}

void onDeferredInit(bool newSession)
{
   source_database::events().onDocAdded.connect(onDocAdded);
   source_database::events().onDocUpdated.connect(onDocUpdated);
   source_database::events().onDocPendingRemove.connect(onDocRemoved);
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

// Primarily intended for debugging / exploration.
SEXP rs_copilotSendRequest(SEXP methodSEXP, SEXP paramsSEXP)
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

SEXP rs_copilotSetLogLevel(SEXP logLevelSEXP)
{
   int logLevel = r::sexp::asInteger(logLevelSEXP);
   s_copilotLogLevel = logLevel;
   return logLevelSEXP;
}

std::string copilotVersion()
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

SEXP rs_copilotVersion()
{
   if (!isCopilotEnabled())
      return R_NilValue;
   
   std::string version = copilotVersion();
   r::sexp::Protect protect;
   return r::sexp::create(version, &protect);
}

SEXP rs_copilotStopAgent()
{
   // stop the copilot agent
   stopAgent();
   
   // wait until the process is gone
   bool stopped = waitFor([]() { return s_agentPid == -1; });
 
   // return status
   return Rf_ScalarLogical(stopped);
}

Error copilotDiagnostics(const json::JsonRpcRequest& request,
                         const json::JsonRpcFunctionContinuation& continuation)
{
   // Make sure copilot is running
   if (!ensureAgentRunning())
   {
      json::JsonRpcResponse response;
      continuation(Success(), &response);
      return Success();
   }
   
   std::string requestId = core::system::generateUuid();
   sendRequest("debug/diagnostics", requestId, json::Object(), CopilotContinuation(continuation));
   
   return Success();
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
   // the copilot agent to attempt to read the contents of that file
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
   docJson["uri"] = uriFromDocument(pDoc);
   docJson["version"] = kCopilotDefaultDocumentVersion;

   json::Object contextJson;
   contextJson["triggerKind"] = autoInvoked ? 
         kCopilotCompletionTriggerAutomatic : kCopilotCompletionTriggerUserInvoked;

   json::Object paramsJson;
   paramsJson["textDocument"] = docJson;
   paramsJson["position"] = positionJson;
   paramsJson["context"] = contextJson;

   // Send the request
   std::string requestId = core::system::generateUuid();
   sendRequest("textDocument/inlineCompletion", requestId, paramsJson, CopilotContinuation(continuation));

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
   Error launchError;
   if (!ensureAgentRunning(&launchError))
   {
      json::JsonRpcResponse response;
      
      json::Object resultJson;
      
      if (launchError)
      {
         json::Object errorJson;
         launchError.writeJson(&errorJson);
         resultJson["reason"] = static_cast<int>(CopilotAgentNotRunningReason::LaunchError);
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
   sendRequest("checkStatus", requestId, json::Object(), CopilotContinuation(continuation));
   return Success();
}

Error copilotDocFocused(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // Make sure copilot is running
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
   
   // If document is NOT indexable we tell Copilot that no file has focus via an empty request.
   // This is to prevent Copilot from attempting to read the contents of the file.
   json::Object textDocumentJson;
   if (isIndexableDocument(pDoc))
   {
      textDocumentJson["uri"] = uriFromDocument(pDoc);
   }

   json::Object paramsJson;
   paramsJson["textDocument"] = textDocumentJson;

   sendNotification("textDocument/didFocus", paramsJson);
   return Success();
}

Error copilotDidShowCompletion(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   // Make sure copilot is running
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

} // end anonymous namespace


Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // Read default log level
   std::string copilotLogLevel = core::system::getenv("COPILOT_LOG_LEVEL");
   if (!copilotLogLevel.empty())
      s_copilotLogLevel = safe_convert::stringTo<int>(copilotLogLevel, 0);
   
   // Read project options
   if (projects::projectContext().hasProject())
   {
      Error error = projects::projectContext().readCopilotOptions(&s_copilotProjectOptions);
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
   // editting preferences within the Copilot prefs dialog, anyhow.
   prefs::userPrefs().onChanged.connect(onUserPrefsChanged);

   RS_REGISTER_CALL_METHOD(rs_copilotSendRequest);
   RS_REGISTER_CALL_METHOD(rs_copilotSetLogLevel);
   RS_REGISTER_CALL_METHOD(rs_copilotVersion);
   RS_REGISTER_CALL_METHOD(rs_copilotStopAgent);

   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(registerAsyncRpcMethod, "copilot_diagnostics", copilotDiagnostics))
         (bind(registerAsyncRpcMethod, "copilot_generate_completions", copilotGenerateCompletions))
         (bind(registerAsyncRpcMethod, "copilot_sign_in", copilotSignIn))
         (bind(registerAsyncRpcMethod, "copilot_sign_out", copilotSignOut))
         (bind(registerAsyncRpcMethod, "copilot_status", copilotStatus))
         (bind(registerRpcMethod, "copilot_doc_focused", copilotDocFocused))
         (bind(registerRpcMethod, "copilot_did_show_completion", copilotDidShowCompletion))
         (bind(sourceModuleRFile, "SessionCopilot.R"))
         ;
   return initBlock.execute();

}

} // end namespace copilot
} // end namespace modules
} // end namespace session
} // end namespace rstudio
