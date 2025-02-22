/*
 * ServerSessionManager.cpp
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

#include <core/CrashHandler.hpp>

#include <server/session/ServerSessionManager.hpp>

#include <boost/format.hpp>

#include <shared_core/SafeConvert.hpp>

#include <core/SocketRpc.hpp>
#include <core/system/System.hpp>
#include <core/system/Process.hpp>
#include <core/system/PosixUser.hpp>
#include <core/system/Environment.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionConstants.hpp>

#include <monitor/MonitorClient.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerPaths.hpp>
#include <server/ServerErrorCategory.hpp>
#include <server/auth/ServerValidateUser.hpp>

#include "../ServerREnvironment.hpp"
#include "../ServerMetrics.hpp"
#include "server-config.h"


using namespace rstudio::core;
using namespace boost::placeholders;

#if defined(__SANITIZE_THREAD__)
# define HAS_THREAD_SANITIZER
#endif

#if defined(__has_feature)
# if __has_feature(thread_sanitizer)
#  define HAS_THREAD_SANITIZER
# endif
#endif

namespace rstudio {
namespace server {

namespace {

static std::string s_launcherToken;

void readRequestArgs(const core::http::Request& request, core::system::Options *pArgs)
{
   // we only do this when establishing new sessions via client_init
   if (!boost::algorithm::ends_with(request.uri(), "client_init"))
      return;

   // parse the request (okay if it fails, none of the below is critical)
   json::JsonRpcRequest clientInit;
   Error error = json::parseJsonRpcRequest(request.body(), &clientInit);
   if (error)
      return;
   
   // read parameters from the request if present
   int restoreWorkspace = -1;
   json::getOptionalParam<int>(clientInit.kwparams, "restore_workspace", -1, &restoreWorkspace);
   if (restoreWorkspace != -1)
      pArgs->push_back(std::make_pair("--r-restore-workspace", 
               safe_convert::numberToString(restoreWorkspace)));
   int runRprofile = -1;
   json::getOptionalParam<int>(clientInit.kwparams, "run_rprofile", -1, &runRprofile);
   if (runRprofile != -1)
      pArgs->push_back(std::make_pair("--r-run-rprofile", 
            safe_convert::numberToString(runRprofile)));
}

core::system::ProcessConfig sessionProcessConfig(
         r_util::SessionContext context,
         const core::system::Options& extraArgs = core::system::Options(),
         const core::system::Options& extraEnvironment = core::system::Options(),
         bool requestIsSecure = false,
         std::string openFile = "")
{
   // prepare command line arguments
   server::Options& options = server::options();
   core::system::Options args;

   // check for options-specified config file and add to command
   // line if specified
   std::string rsessionConfigFile(options.rsessionConfigFile());
   if (!rsessionConfigFile.empty())
      args.push_back(std::make_pair("--config-file", rsessionConfigFile));

   // pass the user-identity
   args.push_back(std::make_pair("-" kUserIdentitySessionOptionShort,
                                 context.username));

   // pass the project if specified
   if (!context.scope.project().empty())
   {
      args.push_back(std::make_pair("-" kProjectSessionOptionShort,
                                    context.scope.project()));
   }

   // pass the scope id if specified
   if (!context.scope.id().empty())
   {
      args.push_back(std::make_pair("-" kScopeSessionOptionShort,
                                    context.scope.id()));
   }

   // ensure cookies are marked secure if applicable
   bool useSecureCookies = options.authCookiesForceSecure() ||
                           options.getOverlayOption("ssl-enabled") == "1" ||
                           requestIsSecure;
   args.push_back(std::make_pair("--" kUseSecureCookiesSessionOption, 
         useSecureCookies ? "1" : "0"));

   args.push_back(std::make_pair("--" kRootPathSessionOption,
                                 options.wwwRootPath()));
   args.push_back(std::make_pair("--" kSameSiteSessionOption,
                                 safe_convert::numberToString(static_cast<int>(options.wwwSameSite()))));

   args.push_back({ "--" kSessionUseFileStorage, options.sessionUseFileStorage() ? "1" : "0"});

   // create launch token if we haven't already
   if (s_launcherToken.empty())
      s_launcherToken = core::system::generateShortenedUuid();
   args.push_back(std::make_pair("--launcher-token", s_launcherToken));
   
   // allow session timeout to be overridden via environment variable
   std::string timeout = core::system::getenv("RSTUDIO_SESSION_TIMEOUT");
   if (!timeout.empty())
      args.push_back(std::make_pair("--" kTimeoutSessionOption, timeout));

   if (!openFile.empty() && context.scope.workbench() == kWorkbenchRStudio) {
      // currently only support for rstudio.
      args.push_back(std::make_pair("--open-files", openFile));
   }

   // pass our uid to instruct rsession to limit rpc clients to us and itself
   core::system::Options environment;
   environment.insert(environment.end(), extraEnvironment.begin(), extraEnvironment.end());
   uid_t uid = core::system::user::currentUserIdentity().userId;
   environment.push_back(std::make_pair(
                           kRStudioLimitRpcClientUid,
                           safe_convert::numberToString(uid)));

   // set session scope project if we have one
   if (!context.scope.project().empty())
   {
      environment.push_back(std::make_pair(
                              kRStudioSessionScopeProject,
                              context.scope.project()));
   }

   // set session scope id if we have one
   if (!context.scope.id().empty())
   {
      environment.push_back(std::make_pair(
                              kRStudioSessionScopeId,
                              context.scope.id()));
   }

   sessionProcessConfigOverlay(&args, &environment);

   // pass extra params
   std::copy(extraArgs.begin(), extraArgs.end(), std::back_inserter(args));

   // append R environment variables
   r_util::RVersion rVersion = r_environment::rVersion();
   core::system::Options rEnvVars = rVersion.environment();
   environment.insert(environment.end(), rEnvVars.begin(), rEnvVars.end());
   
   // mark this as the system default R version
   core::system::setenv(&environment,
                        kRStudioDefaultRVersion,
                        rVersion.number());
   core::system::setenv(&environment,
                        kRStudioDefaultRVersionHome,
                        rVersion.homeDir().getAbsolutePath());

   // forward the auth options
   core::system::setenv(&environment,
                        kRStudioRequiredUserGroup,
                        options.authRequiredUserGroup());
   core::system::setenv(&environment,
                        kRStudioMinimumUserId,
                        safe_convert::numberToString(
                                 options.authMinimumUserId()));
   
   // add monitor shared secret
   environment.push_back(std::make_pair(kMonitorSharedSecretEnvVar,
                                        options.monitorSharedSecret()));

   // stamp the version number of the rserver process that is launching this session
   // the session should log an error if its version does not match, as that is
   // likely an unsupported configuration
   environment.push_back({kRStudioVersion, RSTUDIO_VERSION});

   // forward over crash handler environment if we have it (used for development mode)
   if (!core::system::getenv(kCrashHandlerEnvVar).empty())
      environment.push_back({kCrashHandlerEnvVar, core::system::getenv(kCrashHandlerEnvVar)});

   if (!core::system::getenv(kCrashpadHandlerEnvVar).empty())
      environment.push_back({kCrashpadHandlerEnvVar, core::system::getenv(kCrashpadHandlerEnvVar)});


   // forward path for session temp dir (used for local stream path)
   environment.push_back(
         std::make_pair(kSessionTmpDirEnvVar, sessionTmpDir().getAbsolutePath()));

   // Set RPC socket path and secret
   environment.push_back({kServerRpcSocketPathEnvVar, serverRpcSocketPath().getAbsolutePath()});
   environment.push_back({kServerRpcSecretEnvVar, core::socket_rpc::secret()});
   
   // if we're running automation, forward the flag
   if (server::options().runAutomation())
   {
      // only trigger the automation run with the first rsession that's launched
      static bool s_runAutomation = true;
      args.push_back(std::make_pair("--run-automation", s_runAutomation ? "1" : "0"));
      s_runAutomation = false;
      
      // make sure automation tests run with unique blank slate for prefs
      std::string uniqueId = core::system::generateUuid();
      FilePath rootPath = FilePath("/tmp/rstudio-automation").completePath(uniqueId);
      Error error = rootPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);
      
      environment.push_back({ "RSTUDIO_CONFIG_HOME", rootPath.completeChildPath("config-home").getAbsolutePath() });
      environment.push_back({ "RSTUDIO_CONFIG_DIR",  rootPath.completeChildPath("config-dir").getAbsolutePath()  });
      environment.push_back({ "RSTUDIO_DATA_HOME",   rootPath.completeChildPath("data-home").getAbsolutePath()   });
      
      // forward project root, so development automation tests can be discovered
      std::string projectRoot = core::system::getenv("RSTUDIO_PROJECT_ROOT");
      if (!projectRoot.empty())
         environment.push_back({ "RSTUDIO_AUTOMATION_ROOT", projectRoot });
      
      // forward filter and markers if available
      std::string filter = server::options().automationFilter();
      if (!filter.empty())
         environment.push_back({ "RSTUDIO_AUTOMATION_FILTER", filter });
      
      std::string markers = server::options().automationMarkers();
      if (!markers.empty())
         environment.push_back({ "RSTUDIO_AUTOMATION_MARKERS", markers });
   }

   // build the config object and return it
   core::system::ProcessConfig config;
   config.args = args;
   config.environment = environment;
   config.stdStreamBehavior = core::system::StdStreamInherit;
   return config;
}

void onProcessExit(const std::string& username, PidType pid)
{
}

} // anonymous namespace

SessionManager& sessionManager()
{
   static SessionManager instance;
   return instance;
}

SessionManager::SessionManager()
{
   // set default session launcher
   sessionLaunchFunction_ = boost::bind(&SessionManager::launchAndTrackSession,
                                           this, _1, _2);
}

Error SessionManager::launchSession(boost::asio::io_context& ioContext,
                                    const r_util::SessionContext& context,
                                    const http::Request& request,
                                    bool &launched,
                                    const core::system::Options environment,
                                    const http::ResponseHandler& onLaunch,
                                    const http::ErrorHandler& onError,
                                    const std::string& openFile)
{
   int numRemoved = 0;
   using namespace boost::posix_time;
   LOCK_MUTEX(launchesMutex_)
   {
      // check whether we already have a launch pending
      LaunchMap::const_iterator pos = pendingLaunches_.find(context);
      if (pos != pendingLaunches_.end())
      {
         // if the launch is less than one minute old then return success
         if ( (pos->second + boost::posix_time::minutes(1))
               > microsec_clock::universal_time() )
         {
            LOG_DEBUG_MESSAGE("Found existing recent launch < 1 min for: " + context.username + " id: " + context.scope.id());

            launched = false;
            return Success();
         }
         // otherwise erase it from pending launches and then
         // re-launch (immediately below)
         else
         {
            // This is expected when load balancing since rpc requests that remove pendingLaunches may happen on another server
            LOG_INFO_MESSAGE("Found pending previous session launch for "
                             "user " + context.username +" (aborting wait) for: " + context.scope.id());

            pendingLaunches_.erase(context);
         }
      }

      pendingLaunches_[context] =  microsec_clock::universal_time();

      numRemoved = cleanStalePendingLaunches();
   }
   END_LOCK_MUTEX

   if (numRemoved > 0)
      LOG_DEBUG_MESSAGE("Found " + std::to_string(numRemoved) + " sessions launched, but not connected to from this server in 3 minutes");

   std::string processName = context.scope.isWorkspaces() ? "Homepage (rworkspaces)" : context.scope.workbench();
   LOG_DEBUG_MESSAGE("Launching " + processName + " session for: " + context.username + " id: " + context.scope.id());

   // translate querystring arguments into extra session args 
   core::system::Options args;
   readRequestArgs(request, &args);

   // determine launch options
   r_util::SessionLaunchProfile profile;
   profile.context = context;
   profile.executablePath = server::options().rsessionPath();
   profile.config = sessionProcessConfig(context, args, environment, request.isSecure(), openFile);

   // pass the profile to any filters we have
   for (SessionLaunchProfileFilter f : sessionLaunchProfileFilters_)
   {
      f(&profile);
   }

   // launch the session
   Error error = sessionLaunchFunction_(ioContext, profile, request, onLaunch, onError);
   if (error)
   {
      removePendingLaunch(context, false, "error during launch: " + error.asString());
      return error;
   }

   launched = true;
   return Success();
}

namespace {

boost::mutex s_configFilterMutex;
core::system::ProcessConfigFilter s_processConfigFilter;

} // anonymous namespace


void setProcessConfigFilter(const core::system::ProcessConfigFilter& filter)
{
   LOCK_MUTEX(s_configFilterMutex)
   {
      s_processConfigFilter = filter;
   }
   END_LOCK_MUTEX
}

// default session launcher -- does the launch then tracks the pid
// for later reaping
Error SessionManager::launchAndTrackSession(
                           boost::asio::io_context&,
                           const core::r_util::SessionLaunchProfile& profile)
{
   // if we are root then assume the identity of the user
   using namespace rstudio::core::system;
   std::string runAsUser = realUserIsRoot() ? profile.context.username : "";

   core::system::ProcessConfigFilter configFilter;
   LOCK_MUTEX(s_configFilterMutex)
   {
      configFilter = s_processConfigFilter;
   }
   END_LOCK_MUTEX
         
   // retrieve profile config
   auto config = profile.config;
   
   // on macOS, we need to forward DYLD_INSERT_LIBRARIES
#if __APPLE__
   auto rVersion = server::r_environment::rVersion();
   auto rLibPath = rVersion.homeDir().completeChildPath("lib/libR.dylib");

   core::system::setenv(
            &config.environment,
            "DYLD_INSERT_LIBRARIES",
            rLibPath.getAbsolutePath());
#endif

#ifdef HAS_THREAD_SANITIZER
   // the thread sanitizer will cause the session to hang if output
   // if written to stdout or stderr, so redirect to a file instead
   std::string tsanOptions = core::system::getenv("TSAN_OPTIONS");
   if (tsanOptions.empty())
   {
      LOG_INFO_MESSAGE("Thread sanitizer is enabled. Reports will be logged to /tmp/rsession.tsan.log.");
      tsanOptions = "log_path=/tmp/rsession.tsan.log";
   }

   core::system::setenv(
       &config.environment,
       "TSAN_OPTIONS",
       tsanOptions);
#endif


   // launch the session
   PidType pid = 0;
   Error error = launchChildProcess(profile.executablePath,
                                    runAsUser,
                                    config,
                                    configFilter,
                                    &pid);
   if (error)
   {
      error.addProperty("description", "Error launching session process");
      error.addProperty("user", runAsUser);
      error.addProperty("executablePath", profile.executablePath);
      return error;
   }

   LOG_DEBUG_MESSAGE("Launched session process for user: " + runAsUser + ": " + profile.executablePath +
                     " pid: " + safe_convert::numberToString(pid));
   metrics::sessionLaunch(metrics::kEditorRStudio);

   // track it for subsequent reaping
   processTracker_.addProcess(pid, boost::bind(onProcessExit,
                                               profile.context.username,
                                               pid));

   // return success
   return Success();
}

void SessionManager::setSessionLaunchFunction(
                           const SessionLaunchFunction& launchFunction)
{
   sessionLaunchFunction_ = launchFunction;
}

void SessionManager::addSessionLaunchProfileFilter(
                              const SessionLaunchProfileFilter& filter)
{
   sessionLaunchProfileFilters_.push_back(filter);
}

void SessionManager::removePendingLaunch(const r_util::SessionContext& context, const bool success, const std::string& errorMsg)
{
   bool removed = false;
   boost::posix_time::ptime startTime;
   LOCK_MUTEX(launchesMutex_)
   {
      LaunchMap::const_iterator it = pendingLaunches_.find(context);
      if (it != pendingLaunches_.cend())
      {
         removed = true;
         startTime = it->second;
         pendingLaunches_.erase(context);
      }
   }
   END_LOCK_MUTEX

   if (removed)
   {
      boost::posix_time::time_duration startDuration = boost::posix_time::microsec_clock::universal_time() - startTime;
      std::string progName = context.scope.isWorkspaces() ? "Homepage (rworkspaces)" : context.scope.workbench() + " session(" + context.scope.id() + ")";
      if (success)
      {
         if (!context.scope.isWorkspaces())
            metrics::sessionStartConnect(context.scope.workbench(), context.username, startDuration);

         LOG_DEBUG_MESSAGE(progName + " started and connection made by: " + context.username +
                           " in " + std::to_string(startDuration.total_seconds()) + "." +
                                    std::to_string(startDuration.total_milliseconds() % 1000) + "s");
      }
      else if (!errorMsg.empty())
         LOG_ERROR_MESSAGE(context.scope.workbench() + " session start failed for: " + context.username + ":" + context.scope.id() +
                           " in " + std::to_string(startDuration.total_seconds()) + "." +
                                    std::to_string(startDuration.total_milliseconds() % 1000) + "s error: " + errorMsg);
   }
}

void SessionManager::removePendingSessionLaunch(const std::string& username, const std::string& sessionId, const bool success, const std::string& errorMsg)
{
   bool removed = false;
   boost::posix_time::ptime startTime;
   LOCK_MUTEX(launchesMutex_)
   {
      auto it = pendingLaunches_.cbegin();
      while (it != pendingLaunches_.cend())
      {
         if (it->first.username == username && it->first.scope.id() == sessionId)
         {
            removed = true;
            startTime = it->second;
            pendingLaunches_.erase(it++);
            break;
         }
         else
            ++it;
      }
   }
   END_LOCK_MUTEX

   if (removed)
   {
      boost::posix_time::time_duration startDuration = boost::posix_time::microsec_clock::universal_time() - startTime;
      if (success)
         LOG_DEBUG_MESSAGE("Session started and connection made by: " + username + ":" + sessionId +
                           " in " + std::to_string(startDuration.total_seconds()) + "." +
                                    std::to_string(startDuration.total_milliseconds() % 1000) + "s");
      else
         LOG_ERROR_MESSAGE("Session start failed for: " + username + ":" + sessionId +
                           " in " + std::to_string(startDuration.total_seconds()) + "." +
                                    std::to_string(startDuration.total_milliseconds() % 1000) + "s error: " + errorMsg);
   }
}

// Caller should have launchesMutex_. Removes any pendingLaunches that were recorded but where the
// session never started, or rpcs ended up being handled by another rserver node in the cluster
int SessionManager::cleanStalePendingLaunches()
{
   auto it = pendingLaunches_.cbegin();
   auto now = boost::posix_time::microsec_clock::universal_time();
   int numRemoved = 0;
   while (it != pendingLaunches_.cend())
   {
      if (now > (it->second + boost::posix_time::minutes(3)))
      {
         pendingLaunches_.erase(it++);
         numRemoved++;
      }
      else
         ++it;
   }
   return numRemoved;
}

void SessionManager::notifySIGCHLD()
{
   processTracker_.notifySIGCHILD();
}

r_util::SessionLaunchProfile createSessionLaunchProfile(const r_util::SessionContext& context,
                                                        const core::system::Options& extraArgs)
{
   r_util::SessionLaunchProfile profile;
   profile.context = context;
   profile.executablePath = server::options().rsessionPath();
   profile.config = sessionProcessConfig(context, extraArgs);

   // pass the profile to any filters we have
   for (const SessionManager::SessionLaunchProfileFilter& f : sessionManager().getSessionLaunchProfileFilters())
   {
      f(&profile);
   }

   return profile;
}

// helper function for verify-installation
Error launchSession(const r_util::SessionContext& context,
                    const core::system::Options& extraArgs,
                    PidType* pPid)
{
   // launch the session
   // we use a modified configured home directory to provide a reliable temp dir that can be written to
   // as the server (service) user most likely does not have a home directory configured
   std::string username = context.username;
   std::string rsessionPath = server::options().rsessionPath();
   std::string runAsUser = core::system::realUserIsRoot() ? username : "";
   core::system::ProcessConfig config = sessionProcessConfig(context,
                                                             extraArgs);

   FilePath tmpDir;
   Error error = FilePath::tempFilePath(tmpDir);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      error = tmpDir.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
      }
      else
      {
         FilePath userHome = tmpDir;
         FilePath xdgConfigHome = tmpDir.completeChildPath(".config");
         core::system::setenv(&config.environment, "HOME", userHome.getAbsolutePath());
         core::system::setenv(&config.environment, "XDG_CONFIG_HOME", xdgConfigHome.getAbsolutePath());
      }
   }

   *pPid = -1;
   return core::system::launchChildProcess(rsessionPath,
                                           runAsUser,
                                           config,
                                           core::system::ProcessConfigFilter(),
                                           pPid);
}

} // namespace server
} // namespace rstudio
