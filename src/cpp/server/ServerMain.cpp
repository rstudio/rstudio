/*
 * ServerMain.cpp
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


#include <pthread.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>

#include <core/CrashHandler.hpp>
#include <core/FileLock.hpp>
#include <core/Log.hpp>
#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>
#include <core/SocketRpc.hpp>
#include <core/json/JsonRpc.hpp>

#include <core/text/TemplateFilter.hpp>

#include <core/system/PosixChildProcess.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/User.hpp>
#include <core/system/encryption/Encryption.hpp>

#include <core/http/URL.hpp>
#include <core/http/AsyncUriHandler.hpp>
#include <core/http/TcpIpAsyncServer.hpp>
#include <core/http/ProxyUtils.hpp>

#include <core/gwt/GwtLogHandler.hpp>
#include <core/gwt/GwtFileHandler.hpp>

#include <server_core/SecureKeyFile.hpp>
#include <server_core/ServerDatabase.hpp>
#include <server_core/http/SecureCookie.hpp>

#include <monitor/MonitorClient.hpp>

#include <session/SessionConstants.hpp>


#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerValidateUser.hpp>
#include <server/auth/ServerSecureUriHandler.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>
#include <server/ServerScheduler.hpp>
#include <server/ServerProcessSupervisor.hpp>
#include <server/ServerPaths.hpp>

#include <server/session/ServerSessionProxy.hpp>
#include <server/session/ServerSessionManager.hpp>
#include <server/session/ServerSessionMetadataRpc.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>
#include <shared_core/system/encryption/EncryptionConfiguration.hpp>

#include "server-config.h"
#include "ServerAddins.hpp"
#include "ServerBrowser.hpp"
#include "ServerEval.hpp"
#include "ServerEnvVars.hpp"
#include "ServerInit.hpp"
#include "ServerMeta.hpp"
#include "ServerOffline.hpp"
#include "ServerPAMAuth.hpp"
#include "ServerREnvironment.hpp"
#include "ServerXdgVars.hpp"
#include "ServerLogVars.hpp"

#if defined(__linux__)
# define kOpenProgram "/usr/bin/xdg-open"
#elif defined(__APPLE__)
# define kOpenProgram "/usr/bin/open"
#elif defined(_WIN32)
# define kOpenProgram "start"
#endif

using namespace rstudio;
using namespace rstudio::core;
using namespace rstudio::server;
using namespace boost::placeholders;

// forward-declare overlay methods
namespace rstudio {
namespace server {
namespace overlay {

Error initialize();
Error startup();
bool reloadConfiguration();
void startShutdown();
std::set<std::string> interruptProcs();
void shutdown();
bool requireLocalR();

} // namespace overlay
} // namespace server
} // namespace rstudio

namespace {

std::string s_rpcSecret;

const char * const kProgramIdentity = "rserver";

const int kMinDesiredOpenFiles = 4096;
const int kMaxDesiredOpenFiles = 8192;
   
bool mainPageFilter(const core::http::Request& request,
                    core::http::Response* pResponse)
{
   return server::eval::expirationFilter(request, pResponse) &&
          server::browser::supportedBrowserFilter(request, pResponse) &&
          auth::handler::mainPageFilter(request, pResponse);
}

http::UriHandlerFunction blockingFileHandler()
{
   Options& options = server::options();

   // determine initJs (none for now)
   std::string initJs;

   // return file
   return gwt::fileHandlerFunction(options.wwwLocalPath(),
                                   "/",
                                   mainPageFilter,
                                   initJs,
                                   options.gwtPrefix(),
                                   options.wwwUseEmulatedStack(),
                                   "", // no server homepage in open source
                                   options.wwwFrameOrigin());
}

//
// some fancy footwork is required to take the standand blocking file handler
// and make it work within a secure async context.
//
auth::SecureAsyncUriHandlerFunction secureAsyncFileHandler()
{
   // create a functor which can adapt a synchronous file handler into
   // an asynchronous handler
   class FileRequestHandler {
   public:
      static void handleRequest(
            const http::UriHandlerFunction& fileHandlerFunction,
            boost::shared_ptr<http::AsyncConnection> pConnection)
      {
         fileHandlerFunction(pConnection->request(), &(pConnection->response()));
         pConnection->writeResponse();
      }
   };

   // use this functor to generate an async uri handler function from the
   // stock blockingFileHandler (defined above)
   http::AsyncUriHandlerFunction asyncFileHandler =
      boost::bind(FileRequestHandler::handleRequest, blockingFileHandler(), _1);


   // finally, adapt this to be a secure async uri handler by binding out the
   // first parameter (username, which the gwt file handler knows nothing of)
   return boost::bind(asyncFileHandler, _2);
}

// http server
boost::shared_ptr<http::AsyncServer> s_pHttpServer;

Error httpServerInit()
{
   http::Headers additionalHeaders;
   for (const std::string& headerStr : options().serverAddHeaders())
   {
      size_t pos = headerStr.find(':');
      if (pos == std::string::npos)
      {
         LOG_WARNING_MESSAGE("Invalid header " + headerStr +
                             " will be skipped and not be written to outgoing requests");
         continue;
      }

      additionalHeaders.emplace_back(string_utils::trimWhitespace(headerStr.substr(0, pos)),
                                     string_utils::trimWhitespace(headerStr.substr(pos+1)));
   }

   s_pHttpServer.reset(server::httpServerCreate(additionalHeaders));

   s_pHttpServer->addStreamingUriPrefix("/events/get_events");

   // set server options
   s_pHttpServer->setAbortOnResourceError(true);
   s_pHttpServer->setScheduledCommandInterval(
                                    boost::posix_time::milliseconds(500));

   // initialize
   return rstudio::server::httpServerInit(s_pHttpServer.get());
}

void pageNotFoundHandler(const http::Request& request,
                         http::Response* pResponse)
{
   std::ostringstream os;
   std::map<std::string, std::string> vars;
   vars["request_uri"] = string_utils::jsLiteralEscape(request.uri());
   vars["base_uri"] = string_utils::jsLiteralEscape(request.baseUri(core::http::BaseUriUse::External));

   FilePath notFoundTemplate = FilePath(options().wwwLocalPath()).completeChildPath("404.htm");
   core::Error err = core::text::renderTemplate(notFoundTemplate, vars, os);

   if (err)
   {
      // if we cannot display the 404 page log the error
      // note: this should never happen in a proper deployment
      LOG_ERROR(err);
   }
   else
   {
      std::string body = os.str();
      pResponse->setContentType("text/html");
      pResponse->setBodyUnencoded(body);
   }

   // set 404 status even if there was an error showing the proper not found page
   pResponse->setStatusCode(core::http::status::NotFound);
}

void rootPathRequestFilter(
            boost::asio::io_service& ioService,
            http::Request* pRequest,
            http::RequestFilterContinuation continuation)
{
   // for all requests, be sure to inject the configured root path
   // this way proxied requests will redirect correctly and cookies
   // will have the correct path
   pRequest->setRootPath(options().wwwRootPath());
   continuation(boost::shared_ptr<http::Response>());
}

void httpServerAddHandlers()
{
   // establish json-rpc handlers
   using namespace server::auth;
   using namespace server::session_proxy;
   uri_handlers::add("/rpc", secureAsyncJsonRpcHandlerEx(proxyRpcRequest));
   uri_handlers::add("/events", secureAsyncJsonRpcHandler(proxyEventsRequest));

   // establish content handlers
   uri_handlers::add("/graphics", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::addUploadHandler("/upload", secureAsyncUploadHandler(proxyUploadRequest));
   uri_handlers::add("/export", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/source", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/content", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/diff", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/file_show", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/view_pdf", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/agreement", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/presentation", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/pdf_js", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/mathjax", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/connections", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/theme", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/fonts", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/python", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/tutorial", secureAsyncHttpHandler(proxyContentRequest));
   uri_handlers::add("/quarto", secureAsyncHttpHandler(proxyContentRequest));

   // content handlers which might be accessed outside the context of the
   // workbench get secure + authentication when required
   uri_handlers::add("/help", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/files", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/custom", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/session", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/docs", secureAsyncHttpHandler(secureAsyncFileHandler(), true));
   uri_handlers::add("/html_preview", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/rmd_output", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/grid_data", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/grid_resource", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/chunk_output", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/profiles", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/rmd_data", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/profiler_resource", secureAsyncHttpHandler(proxyContentRequest, true));
   uri_handlers::add("/dictionaries", secureAsyncHttpHandler(proxyContentRequest, true));

   // proxy localhost if requested
   if (server::options().wwwProxyLocalhost())
   {
      uri_handlers::addProxyHandler("/p/", secureAsyncHttpHandler(
                                       boost::bind(proxyLocalhostRequest, false, _1, _2), true));
      uri_handlers::addProxyHandler("/p6/", secureAsyncHttpHandler(
                                       boost::bind(proxyLocalhostRequest, true, _1, _2), true));
   }

   // establish logging handler
   uri_handlers::addBlocking("/log", secureJsonRpcHandler(gwt::handleLogRequest));

   // establish meta
   uri_handlers::addBlocking("/meta", secureJsonRpcHandler(meta::handleMetaRequest));

   // establish progress handler
   FilePath wwwPath(server::options().wwwLocalPath());
   FilePath progressPagePath = wwwPath.completePath("progress.htm");
   uri_handlers::addBlocking("/progress",
                               secureHttpHandler(boost::bind(
                               core::text::handleSecureTemplateRequest,
                               _1, progressPagePath, _2, _3)));

   // establish browser unsupported handler
   using namespace server::browser;
   uri_handlers::addBlocking(kBrowserUnsupported,
                             handleBrowserUnsupportedRequest);

   // restrct access to templates directory
   uri_handlers::addBlocking("/templates", pageNotFoundHandler);

   // initialize gwt symbol maps
   gwt::initializeSymbolMaps(server::options().wwwSymbolMapsPath());

   // add default handler for gwt app
   uri_handlers::setBlockingDefault(blockingFileHandler());
}

bool reloadLoggingConfiguration()
{
   LOG_INFO_MESSAGE("Reloading logging configuration...");

   Error error = core::system::reinitLog();
   if (error)
   {
      LOG_ERROR_MESSAGE("Failed to reload logging configuration");
      LOG_ERROR(error);
   }
   else
   {
      LOG_INFO_MESSAGE("Successfully reloaded logging configuration");
   }

   return !static_cast<bool>(error);
}

bool reloadEnvConfiguration()
{
   LOG_INFO_MESSAGE("Reloading environment configuration...");
   Error error = env_vars::initialize();
   if (error)
   {
      LOG_ERROR_MESSAGE("Failed to reload environment configuration");
      LOG_ERROR(error);
   }
   else
   {
      LOG_INFO_MESSAGE("Successfully reloaded environment configuration");
   }

   return !static_cast<bool>(error);
}

void reloadConfiguration()
{
   bool success = reloadLoggingConfiguration();
   success = reloadEnvConfiguration() && success;
   success = overlay::reloadConfiguration() && success;

   if (success)
   {
      LOG_INFO_MESSAGE("Successfully reloaded all configuration");
   }
   else
   {
      LOG_ERROR_MESSAGE("Configuration reload unsuccessful");
   }
}

// bogus SIGCHLD handler (never called)
void handleSIGCHLD(int)
{
}

// wait for and handle signals
Error waitForSignals()
{
   // setup bogus handler for SIGCHLD (if we don't do this then
   // we can't successfully block/wait for the signal). This also
   // allows us to specify SA_NOCLDSTOP
   struct sigaction sa;
   ::memset(&sa, 0, sizeof sa);
   sa.sa_handler = handleSIGCHLD;
   sigemptyset(&sa.sa_mask);
   sa.sa_flags = SA_NOCLDSTOP;
   int result = ::sigaction(SIGCHLD, &sa, nullptr);
   if (result != 0)
      return systemError(errno, ERROR_LOCATION);

   // block signals that we want to sigwait on
   sigset_t wait_mask;
   sigemptyset(&wait_mask);
   sigaddset(&wait_mask, SIGCHLD);
   sigaddset(&wait_mask, SIGINT);
   sigaddset(&wait_mask, SIGQUIT);
   sigaddset(&wait_mask, SIGTERM);
   sigaddset(&wait_mask, SIGHUP);

   result = ::pthread_sigmask(SIG_BLOCK, &wait_mask, nullptr);
   if (result != 0)
      return systemError(result, ERROR_LOCATION);

   // wait for child exits
   for (;;)
   {
      // perform wait
      int sig = 0;
      int result = ::sigwait(&wait_mask, &sig);
      if (result != 0)
         return systemError(result, ERROR_LOCATION);

      // SIGCHLD
      if (sig == SIGCHLD)
      {
         sessionManager().notifySIGCHLD();
      }

      // SIGINT, SIGQUIT, SIGTERM
      else if (sig == SIGINT || sig == SIGQUIT || sig == SIGTERM)
      {
         LOG_DEBUG_MESSAGE("Received termination signal: " + std::to_string(sig));

         Error error;

         // Stop serving requests first so we aren't trying to do that during shutdown
         s_pHttpServer->stop();

         overlay::startShutdown();

         // send SIGTERM signal to specific Workbench child processes
         // this way user processes will not receive the signal until it traverses the tree
         std::set<std::string> interruptProcs = {
            "rserver-launcher",
            "rserver-monitor",
            "rsession",
            "rstudio-launcher"
         };
         std::set<std::string> overlayProcs = overlay::interruptProcs();
         interruptProcs.insert(overlayProcs.begin(), overlayProcs.end());
         
         // get list of child processes
         std::vector<system::ProcessInfo> procInfos;
         error = core::system::getChildProcesses(&procInfos, false);
         if (error)
         {
            LOG_ERROR(error);
            break;
         }
         
         // pull out pids matching our process names
         std::vector<pid_t> pids;
         for (auto&& procInfo : procInfos)
            if (interruptProcs.count(procInfo.exe))
            {
               LOG_DEBUG_MESSAGE("Sending SIGTERM to child: " + procInfo.exe + " (" + std::to_string(procInfo.pid) + ")");
               pids.push_back(procInfo.pid);
            }
         
         // signal those processes
         error = core::system::sendSignalToSpecifiedChildProcesses(pids, SIGTERM);
         if (error)
         {
            std::string message = "Error occurred while notifying child processes of ";
            message += strsignal(sig);
            error.addProperty("description", message);
            LOG_ERROR(error);
         }
         else
         {
            std::string message = "Successfully notified children of ";
            message += strsignal(sig);
            LOG_INFO_MESSAGE(message);
         }

         // allow the child processes to try and shut down
         boost::thread waitThread([=]()
         {
            while (true)
            {
               int status = 0;
               if (::waitpid(-1, &status, 0) == -1)
                  break;
            }
         });
         
         waitThread.timed_join(boost::chrono::seconds(60));
         
         // notify user if there seem to still be some processes around
         int status = 0;
         if (::waitpid(-1, &status, WNOHANG) == 0)
         {
            std::vector<system::ProcessInfo> stuckProcs;
            std::string stuckProcInfo;
            error = core::system::getChildProcesses(&stuckProcs, false);
            if (!error)
            {
               for (auto&& stuckProc : stuckProcs)
               {
                  if (!stuckProcInfo.empty())
                     stuckProcInfo = stuckProcInfo + ", ";
                  stuckProcInfo = stuckProcInfo + stuckProc.exe + " (" + std::to_string(stuckProc.pid) + ") - " + stuckProc.state;
               }
            }
            LOG_WARNING_MESSAGE("Continuing with shutdown despite remaining child processes: " + stuckProcInfo);
         }
         
         // call overlay shutdown
         overlay::shutdown();

         // clear the signal mask
         error = core::system::clearSignalMask();
         if (error)
            LOG_ERROR(error);

         // reset the signal to its default
         struct sigaction sa;
         ::memset(&sa, 0, sizeof sa);
         sa.sa_handler = SIG_DFL;
         int result = ::sigaction(sig, &sa, nullptr);
         if (result != 0)
            LOG_ERROR(systemError(result, ERROR_LOCATION));

         // re-raise the signal
         ::kill(::getpid(), sig);
      }

      // SIGHUP
      else if (sig == SIGHUP)
      {
         reloadConfiguration();

         // forward signal to specific child processes
         // this will allow them to also reload their configuration / logging if applicable
         // care is taken not to send errant SIGHUP signals to processes we don't control
         std::set<std::string> reloadableProcs = {
            "rsession",
            "rserver-launcher",
            "rstudio-launcher",
            "rworkspaces",
            "rserver-monitor",
         };

         Error error = core::system::sendSignalToSpecifiedChildProcesses(reloadableProcs, SIGHUP);
         if (error)
         {
            error.addProperty("description", "Error occurred while notifying child processes of SIGHUP");
            LOG_ERROR(error);
         }
         else
            LOG_INFO_MESSAGE("Successfully notified children of SIGHUP");
      }

      // Unexpected signal
      else
      {
         LOG_WARNING_MESSAGE("Unexpected signal returned from sigwait: " +
                             safe_convert::numberToString(sig));
      }
   }

   // keep compiler happy (we never get here)
   return Success();
}

} // anonymous namespace

// provide global access to handlers
namespace rstudio {
namespace server {
namespace uri_handlers {

void add(const std::string& prefix,
         const http::AsyncUriHandlerFunction& handler)
{
   s_pHttpServer->addHandler(prefix, handler);
}

void addUploadHandler(const std::string& prefix,
         const http::AsyncUriUploadHandlerFunction& handler)
{
   s_pHttpServer->addUploadHandler(prefix, handler);
}

void addProxyHandler(const std::string& prefix,
                     const http::AsyncUriHandlerFunction& handler)
{
   s_pHttpServer->addProxyHandler(prefix, handler);
}

void addBlocking(const std::string& prefix,
                 const http::UriHandlerFunction& handler)
{
   s_pHttpServer->addBlockingHandler(prefix, handler);
}

void setDefault(const http::AsyncUriHandlerFunction& handler)
{
   s_pHttpServer->setDefaultHandler(handler);
}

// set blocking default handler
void setBlockingDefault(const http::UriHandlerFunction& handler)
{
  s_pHttpServer->setBlockingDefaultHandler(handler);
}

void setRequestFilter(const core::http::RequestFilter& filter)
{
   s_pHttpServer->setRequestFilter(filter);
}

void setResponseFilter(const core::http::ResponseFilter& filter)
{
   s_pHttpServer->setResponseFilter(filter);
}


} // namespace uri_handlers

boost::shared_ptr<http::AsyncServer> server()
{
   return s_pHttpServer;
}

namespace scheduler {

void addCommand(boost::shared_ptr<ScheduledCommand> pCmd)
{
   s_pHttpServer->addScheduledCommand(pCmd);
}

} // namespace scheduler
} // namespace server
} // namespace rstudio

int main(int argc, char * const argv[]) 
{
   try
   {
      // read environment variables from config file; we have to do this before initializing logging
      // so that logging environment variables like RS_LOG_LEVEL stored in this file will be
      // respected when logging is initialized (below).
      //
      // note that we can't emit any logs or errors while reading this config file since logging
      // isn't initialized yet, so we suppress logging in this step
      env_vars::readEnvConfigFile(false /* suppress logs */);

      Error error = core::system::initializeLog(kProgramIdentity, core::log::LogLevel::WARN, false);
      if (error)
      {
         core::log::writeError(error, std::cerr);
         return EXIT_FAILURE;
      }

      // ignore SIGPIPE (don't log error because we should never call
      // syslog prior to daemonizing)
      core::system::ignoreSignal(core::system::SigPipe);
      
#ifdef __APPLE__
      // warn if the rstudio pam profile does not exist
      // (note that this only effects macOS development configurations)
      FilePath pamProfilePath("/etc/pam.d/rstudio");
      if (!pamProfilePath.exists())
      {
         std::cerr << "WARNING: /etc/pam.d/rstudio does not exist; authentication may fail!" << std::endl;
         std::cerr << "Run 'sudo cp /etc/pam.d/cups /etc/pam.d/rstudio' to set a default PAM profile for RStudio." << std::endl;
      }
#endif

      // read program options 
      std::ostringstream osWarnings;
      Options& options = server::options();
      ProgramStatus status = options.read(argc, argv, osWarnings);
      std::string optionsWarnings = osWarnings.str();
      if ( status.exit() )
      {
         if (!optionsWarnings.empty())
            program_options::reportWarnings(optionsWarnings, ERROR_LOCATION);

         return status.exitCode();
      }
      
      // daemonize if requested
      if (options.serverDaemonize() && options.dbCommand().empty())
      {
         Error error = core::system::daemonize(options.serverPidFile());
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         error = core::system::ignoreTerminalSignals();
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         // Refresh the loggers after successful daemonize to clear out old FDs
         core::log::refreshAllLogDestinations();

         // set file creation mask to 022 (might have inherted 0 from init)
         if (options.serverSetUmask())
            setUMask(core::system::OthersNoWriteMask);
      }

      // increase the number of open files allowed (need more files
      // so we can supports lots of concurrent connectins)
      if (core::system::realUserIsRoot())
      {
         RLimitType soft, hard;
         Error error = core::system::getResourceLimit(core::system::FilesLimit, &soft, &hard);
         if (error)
         {
            LOG_WARNING_MESSAGE("Error trying to get system open files limits - using system defaults: " + error.asString());
         }
         else if (soft < kMaxDesiredOpenFiles && hard > kMinDesiredOpenFiles)
         {
            RLimitType newLimit;
            if (hard < kMaxDesiredOpenFiles)
               newLimit = hard;
            else
               newLimit = kMaxDesiredOpenFiles;
            Error error = setResourceLimit(core::system::FilesLimit, newLimit);
            if (error)
               LOG_WARNING_MESSAGE("Unable to increase open files limit to: " + std::to_string(newLimit) +
                                   " error: " + error.asString() + " with system limits soft: " +
                                   std::to_string(soft) + " hard: " + std::to_string(hard));
            else
               LOG_DEBUG_MESSAGE("Increasing soft open files limit from: " + std::to_string(soft) +
                                 " to " + std::to_string(newLimit) + " with hard limit: " + std::to_string(hard));
         }
         else
         {
            LOG_DEBUG_MESSAGE("Using system defined open files limits: " + std::to_string(soft));
         }
      }

      // set working directory
      error = FilePath(options.serverWorkingDir()).makeCurrentPath();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize server data directory
      FilePath serverDataDir = options.serverDataDir();
      error = serverDataDir.ensureDirectory();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      boost::optional<system::User> serverUser;
      if (core::system::effectiveUserIsRoot())
      {
         auto shouldChown = [&](int depth, const FilePath& file)
         {
            // don't chown user sockets - they belong to the user
            if (depth == 3 &&
                boost::ends_with(file.getParent().getParent().getFilename(), "-ds"))
               return false;

            if (depth == 1 &&
                (boost::ends_with(file.getFilename(), "-d") ||
                 boost::ends_with(file.getFilename(), "-d.pid") ||
                 boost::ends_with(file.getFilename(), "-d.ctx")))
               return false;

            return true;
         };

         system::User serverUserObj;
         error = system::getUserFromUsername(options.serverUser(), serverUserObj);
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         serverUser = serverUserObj;

         error = serverDataDir.changeOwnership(serverUserObj, true, shouldChown);
         if (error)
         {
            error.addProperty("description",
                              "Could not change owner for path " + serverDataDir.getAbsolutePath() +
                                 ". Is root squash enabled?");
            LOG_ERROR(error);
         }
      }

      // ensure permissions - the folder needs to be readable and writeable
      // by all users of the system, and the sticky bit must be set to ensure
      // that users do not delete each others' sockets
      struct stat st;
      if (::stat(serverDataDir.getAbsolutePath().c_str(), &st) == -1)
      {
         Error error = systemError(errno,
                                   "Could not determine permissions on specified 'server-data-dir' "
                                      "directory (" + serverDataDir.getAbsolutePath() + ")",
                                   ERROR_LOCATION);
         return core::system::exitFailure(error, ERROR_LOCATION);
      }

      unsigned desiredMode = S_IRWXU | S_IRWXG | S_IRWXO | S_ISVTX;
      if ((st.st_mode & desiredMode) != desiredMode)
      {
         // permissions aren't correct - attempt to fix them
         Error error = serverDataDir.changeFileMode(core::FileMode::ALL_READ_WRITE_EXECUTE, true);
         if (error)
         {
            LOG_ERROR_MESSAGE("Could not change permissions for specified 'server-data-dir' - "
                                 "the directory (" + serverDataDir.getAbsolutePath() + ") must be "
                                 "writeable by all users and have the sticky bit set");
            return core::system::exitFailure(error, ERROR_LOCATION);
         }
      }

      // export important environment variables
      core::system::setenv(kServerDataDirEnvVar, serverDataDir.getAbsolutePath());
      core::system::setenv(kSessionTmpDirEnvVar, sessionTmpDir().getAbsolutePath());
      core::system::setenv(kServerRpcSocketPathEnvVar, serverRpcSocketPath().getAbsolutePath());

      // Log HTTP Proxy variables
      const auto httpProxyVar = http::proxyUtils().httpProxyUrl();
      if (httpProxyVar)
         LOG_INFO_MESSAGE("Using HTTP Proxy: " + httpProxyVar.value().absoluteURL());
      const auto httpsProxyVar = http::proxyUtils().httpsProxyUrl();
      if (httpsProxyVar)
         LOG_INFO_MESSAGE("Using HTTPS Proxy: " + httpsProxyVar.value().absoluteURL());
      const auto& noProxyRules = http::proxyUtils().noProxyRules();
      if (!noProxyRules.empty()) 
      {
         std::string noProxyStr;
         for (const auto& rule : noProxyRules)
            noProxyStr += rule->toString() + ",";
         noProxyStr.pop_back();
         LOG_INFO_MESSAGE("No Proxy Rules: " + noProxyStr);
      }

      // initialize File Lock
      FileLock::initialize();

      // initialize crypto utils
      core::system::crypto::encryption::initialize();
      core::system::crypto::initialize();
      LOG_INFO_MESSAGE("Encryption versions set to max: " + std::to_string(system::crypto::getMaximumEncryptionVersion()) + \
                       ", min: " + std::to_string(system::crypto::getMinimumEncryptionVersion()));

      // initialize secure cookie module
      error = core::http::secure_cookie::initialize(options.secureCookieKeyFile());
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      if (!options.dbCommand().empty())
      {
         Error error = server_core::database::execute(options.databaseConfigFile(), serverUser, options.dbCommand());
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         return EXIT_SUCCESS;
      }

      // initialize database connectivity
      error = server_core::database::initialize(options.databaseConfigFile(), true, serverUser);
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize the session proxy
      error = session_proxy::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize http server
      error = httpServerInit();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize the process supervisor (needs to happen post http server
      // init for access to the scheduled command list)
      error = process_supervisor::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize monitor (needs to happen post http server init for access
      // to the server's io service)
      monitor::initializeMonitorClient(monitorSocketPath().getAbsolutePath(),
                                       server::options().monitorSharedSecret(),
                                       s_pHttpServer->ioService());

      if (!options.verifyInstallation())
      {
         // add a monitor log writer
         core::log::addLogDestination(
            monitor::client().createLogDestination(core::system::generateShortenedUuid(), core::log::LogLevel::WARN, kProgramIdentity));
      }

      // initialize XDG var insertion
      error = xdg_vars::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize log var insertion
      error = log_vars::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize environment variables
      error = env_vars::initialize();
      if (error)
      {
         // error loading env vars is non-fatal
         LOG_ERROR(error);
      }

      // overlay may replace this
      if (server::options().wwwRootPath() != kRequestDefaultRootPath) 
      {
         // inject the path prefix as the root path for all requests
         uri_handlers::setRequestFilter(rootPathRequestFilter);
      }
      // initialize socket rpc so we can send distributed events
      error = key_file::readSecureKeyFile("session-rpc-key", &s_rpcSecret);
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      error = core::socket_rpc::initializeSecret(s_rpcSecret);
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // initialize the session rpc handler
      error = session_rpc::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // call overlay initialize
      error = overlay::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      error = session_metadata::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // detect R environment variables (calls R (and this forks) so must
      // happen after daemonize so that upstart script can correctly track us
      std::string errMsg;
      bool detected = r_environment::initialize(&errMsg);
      if (!detected && overlay::requireLocalR())
      {
         program_options::reportError(errMsg, ERROR_LOCATION);
         return EXIT_FAILURE;
      }

      // initialize base authorization routines
      error = auth::handler::initialize();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // add handlers and initialize addins (offline has distinct behavior)
      if (server::options().serverOffline())
      {
         offline::httpServerAddHandlers();
      }
      else
      {
         // add handlers
         httpServerAddHandlers();

         // initialize addins
         error = addins::initialize();
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         // initialize pam auth if we don't already have an auth handler
         if (!auth::handler::isRegistered())
         {
            error = pam_auth::initialize();
            if (error)
               return core::system::exitFailure(error, ERROR_LOCATION);
         }
      }

      // give up root privilege if requested and running as root
      if (core::system::realUserIsRoot())
      {
         std::string runAsUser = options.serverUser();
         if (!runAsUser.empty())
         {
            // drop root priv
            error = core::system::temporarilyDropPriv(runAsUser, true);
            if (error)
               return core::system::exitFailure(error, ERROR_LOCATION);
         }
      }

      // run special verify installation mode if requested
      if (options.verifyInstallation())
      {
         Error error = session_proxy::runVerifyInstallationSession();
         if (error)
         {
            std::cerr << "Verify Installation Failed: " << error << std::endl;
            return core::system::exitFailure(error, ERROR_LOCATION);
         }

         return EXIT_SUCCESS;
      }
      
      // catch unhandled exceptions
      error = core::crash_handler::initialize();
      if (error)
         LOG_ERROR(error);

      server::session_rpc::addHandler(
         "/server_version", 
         [](const std::string&, boost::shared_ptr<core::http::AsyncConnection> pConnection)
         {
            json::Object obj;
            obj["version"] = RSTUDIO_VERSION;

            json::JsonRpcResponse response;
            response.setResult(obj);

            json::setJsonRpcResponse(response, &(pConnection->response()));
            pConnection->writeResponse();
         });

      // call overlay startup
      error = overlay::startup();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // Start the session RPC
      error = server::session_rpc::startup();
      if (error)
         LOG_ERROR(error);

      // add http server not found handler
      s_pHttpServer->setNotFoundHandler(pageNotFoundHandler);

      // run http server
      error = s_pHttpServer->run(options.wwwThreadPoolSize());
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // if we're running automation, open a browser instance and
      // navigate to the RStudio Server instance URL to initiate
      // the automated tests
      if (options.runAutomation())
      {
         std::string address = options.wwwAddress();
         if (address == "0.0.0.0")
         {
            address = "localhost";
         }
         
         std::string port = options.wwwPort();
         if (port.empty())
         {
            Error error = systemError(boost::system::errc::protocol_error, ERROR_LOCATION);
            LOG_ERROR(error);
            return EXIT_FAILURE;
         }
         
         std::string url = fmt::format("http://{}:{}", address, port);
         core::system::ProcessOptions options;
         core::system::ProcessCallbacks callbacks;
         Error error = server::process_supervisor::runProgram(
                  kOpenProgram,
                  { url },
                  options,
                  callbacks);
         if (error)
            LOG_ERROR(error);
      }
      
      // wait for signals
      error = waitForSignals();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // NOTE: we never get here because waitForSignals waits forever
      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE;
}
