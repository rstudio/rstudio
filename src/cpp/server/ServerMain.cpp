/*
 * ServerMain.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <pthread.h>
#include <signal.h>

#include <core/CrashHandler.hpp>
#include <core/FileLock.hpp>
#include <core/Log.hpp>
#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>

#include <core/text/TemplateFilter.hpp>

#include <core/system/PosixSystem.hpp>
#include <core/system/Crypto.hpp>

#include <core/http/URL.hpp>
#include <core/http/AsyncUriHandler.hpp>
#include <server_core/http/SecureCookie.hpp>
#include <core/http/TcpIpAsyncServer.hpp>

#include <core/gwt/GwtLogHandler.hpp>
#include <core/gwt/GwtFileHandler.hpp>

#include <server_core/ServerDatabase.hpp>

#include <monitor/MonitorClient.hpp>

#include <session/SessionConstants.hpp>


#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerValidateUser.hpp>
#include <server/auth/ServerSecureUriHandler.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>
#include <server/ServerScheduler.hpp>
#include <server/ServerSessionProxy.hpp>
#include <server/ServerSessionManager.hpp>
#include <server/ServerProcessSupervisor.hpp>
#include <server/ServerPaths.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>

#include "ServerAddins.hpp"
#include "ServerBrowser.hpp"
#include "ServerEval.hpp"
#include "ServerInit.hpp"
#include "ServerMeta.hpp"
#include "ServerOffline.hpp"
#include "ServerPAMAuth.hpp"
#include "ServerREnvironment.hpp"

using namespace rstudio;
using namespace rstudio::core;
using namespace rstudio::server;

// forward-declare overlay methods
namespace rstudio {
namespace server {
namespace overlay {

Error initialize();
Error startup();
bool reloadConfiguration();
void shutdown();
bool requireLocalR();

} // namespace overlay
} // namespace server
} // namespace rstudio

namespace {

const char * const kProgramIdentity = "rserver";
   
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

Error initLog()
{
   return core::system::initializeSystemLog(kProgramIdentity, core::log::LogLevel::WARN, false);
}

bool reloadLoggingConfiguration()
{
   LOG_INFO_MESSAGE("Reloading logging configuration...");

   Error error = initLog();
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

void reloadConfiguration()
{
   bool success = reloadLoggingConfiguration();
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
   for(;;)
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
         //
         // Here is where we can perform server cleanup e.g.
         // closing pam sessions
         //

         // call overlay shutdown
         overlay::shutdown();

         // clear the signal mask
         Error error = core::system::clearSignalMask();
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
      Error error = initLog();
      if (error)
      {
         core::log::writeError(error, std::cerr);
         return EXIT_FAILURE;
      }

      // ignore SIGPIPE (don't log error because we should never call
      // syslog prior to daemonizing)
      core::system::ignoreSignal(core::system::SigPipe);

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
      if (options.serverDaemonize())
      {
         Error error = core::system::daemonize(options.serverPidFile());
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         error = core::system::ignoreTerminalSignals();
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         // set file creation mask to 022 (might have inherted 0 from init)
         if (options.serverSetUmask())
            setUMask(core::system::OthersNoWriteMask);
      }

      // increase the number of open files allowed (need more files
      // so we can supports lots of concurrent connectins)
      if (core::system::realUserIsRoot())
      {
         Error error = setResourceLimit(core::system::FilesLimit, 4096);
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);
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
                 boost::ends_with(file.getFilename(), "-d.pid")))
               return false;

            return true;
         };

         system::User serverUserObj;
         error = system::User::getUserFromIdentifier(options.serverUser(), serverUserObj);
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

      // initialize File Lock
      FileLock::initialize();

      // initialize crypto utils
      core::system::crypto::initialize();

      // initialize secure cookie module
      error = core::http::secure_cookie::initialize(options.secureCookieKeyFile());
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

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
            monitor::client().createLogDestination(core::log::LogLevel::WARN, kProgramIdentity));
      }

      // overlay may replace this
      if (server::options().wwwRootPath() != kRequestDefaultRootPath) 
      {
         // inject the path prefix as the root path for all requests
         uri_handlers::setRequestFilter(rootPathRequestFilter);
      }

      // call overlay initialize
      error = overlay::initialize();
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

      // give up root privilige if requested
      std::string runAsUser = options.serverUser();
      if (!runAsUser.empty())
      {
         // drop root priv
         error = core::system::temporarilyDropPriv(runAsUser);
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);
      }

      // run special verify installation mode if requested
      if (options.verifyInstallation())
      {
         Error error = session_proxy::runVerifyInstallationSession();
         if (error)
            return core::system::exitFailure(error, ERROR_LOCATION);

         return EXIT_SUCCESS;
      }

      // catch unhandled exceptions
      error = core::crash_handler::initialize();
      if (error)
         LOG_ERROR(error);

      // call overlay startup
      error = overlay::startup();
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

      // add http server not found handler
      s_pHttpServer->setNotFoundHandler(pageNotFoundHandler);

      // run http server
      error = s_pHttpServer->run(options.wwwThreadPoolSize());
      if (error)
         return core::system::exitFailure(error, ERROR_LOCATION);

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
