/*
 * SessionPackages.cpp
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

// boost requires that winsock2.h must be included before windows.h
#ifdef _WIN32
#include <winsock2.h>
#endif

#include "SessionPackages.hpp"

#include <boost/format.hpp>
#include <boost/bind/bind.hpp>
#include <boost/make_shared.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/Thread.hpp>

#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RInterface.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "SessionPackrat.hpp"
#include "SessionPPM.hpp"

#include "session-config.h"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace packages {

namespace {

// store the last user input
std::string s_lastInput;

// the current value of the 'repos' R option
r::sexp::PreservedSEXP s_reposSEXP;

class AvailablePackagesCache : public boost::noncopyable
{
public:

   static AvailablePackagesCache& get()
   {
      static AvailablePackagesCache instance;
      return instance;
   }

private:

   AvailablePackagesCache() = default;

public:

   void insert(const std::string& contribUrl,
               const std::vector<std::string>& availablePackages)
   {
      cache_[contribUrl] = availablePackages;
   }

   bool find(const std::string& contribUrl)
   {
      return cache_.find(contribUrl) != cache_.end();
   }


   bool lookup(const std::string& contribUrl,
               std::vector<std::string>* pAvailablePackages)
   {
      std::map<std::string, std::vector<std::string> >::const_iterator it =
                                                         cache_.find(contribUrl);
      if (it != cache_.end())
      {
         core::algorithm::append(pAvailablePackages, it->second);
         return true;
      }
      else
      {
         return false;
      }
   }

   void ensurePopulated(const std::string& contribUrl)
   {
      if (cache_.find(contribUrl) != cache_.end())
         return;

      // build code to execute
      boost::format fmt(
               "row.names(available.packages(contriburl = '%1%'))");
      std::string code = boost::str(fmt % contribUrl);

      // get the packages
      std::vector<std::string> packages;
      Error error = r::exec::evaluateString(code, &packages);
      if (error)
      {
         // log error if it wasn't merely a null return value
         if (error != r::errc::UnexpectedDataTypeError)
            LOG_ERROR(error);
         return;
      }

      // put them in the cache
      cache_[contribUrl] = packages;
   }

private:
   std::map<std::string, std::vector<std::string> > cache_;
};

void downloadAvailablePackages(const std::string& contribUrl,
                               std::vector<std::string>* pAvailablePackages)
{
   AvailablePackagesCache& cache = AvailablePackagesCache::get();
   cache.ensurePopulated(contribUrl);
   cache.lookup(contribUrl, pAvailablePackages);
}

void downloadAvailablePackages(const std::string& contribUrl)
{
   AvailablePackagesCache& cache = AvailablePackagesCache::get();
   cache.ensurePopulated(contribUrl);
}

// Populate the package cache for a particular URL
SEXP rs_downloadAvailablePackages(SEXP contribUrlSEXP)
{
   std::string contribUrl = r::sexp::asString(contribUrlSEXP);
   downloadAvailablePackages(contribUrl);
   return R_NilValue;
}

SEXP rs_getCachedAvailablePackages(SEXP contribUrlSEXP)
{
   r::sexp::Protect protect;
   std::string contribUrl = r::sexp::asString(contribUrlSEXP);
   AvailablePackagesCache& s_availablePackagesCache = AvailablePackagesCache::get();

   std::vector<std::string> availablePackages;
   if (s_availablePackagesCache.lookup(contribUrl, &availablePackages))
      return r::sexp::create(availablePackages, &protect);
   else
      return R_NilValue;
}

Error availablePackages(const core::json::JsonRpcRequest&,
                        core::json::JsonRpcResponse* pResponse)
{
   // get contrib urls
   std::vector<std::string> contribUrls;
   Error error = r::exec::evaluateString<std::vector<std::string> >(
                  "contrib.url(getOption('repos'), getOption('pkgType'))",
                  &contribUrls);
   if (error)
      return error;


   // download available packages
   std::vector<std::string> availablePackages;
   std::for_each(contribUrls.begin(),
                 contribUrls.end(),
                 boost::bind(&downloadAvailablePackages, _1, &availablePackages));

   // order and remove duplicates
   std::stable_sort(availablePackages.begin(), availablePackages.end());
   availablePackages.erase(std::unique(availablePackages.begin(), availablePackages.end()),
                           availablePackages.end());

   // return as json
   json::Array jsonResults;
   for (std::string& availablePackage : availablePackages)
      jsonResults.push_back(json::Value(availablePackage));
   pResponse->setResult(jsonResults);
   return Success();
}

Error getPackageStateJson(json::Object* pJson)
{
   using namespace module_context;

   Error error = Success();

   PackratContext packratContext = module_context::packratContext();
   core::json::Value renvContext = module_context::renvContextAsJson();

   json::Value packageListJson;
   r::sexp::Protect protect;
   SEXP packageListSEXP = R_NilValue;

   bool renvActive = renvContext.getObject()["active"].getBool();

   // determine the appropriate package listing method from the current
   // packrat mode status
   if (packratContext.modeOn)
   {
      FilePath projectDir = projects::projectContext().directory();
      error = r::exec::RFunction(".rs.listPackagesPackrat",
                                 string_utils::utf8ToSystem(
                                    projectDir.getAbsolutePath()))
              .call(&packageListSEXP, &protect);
   }
   else if (renvActive)
   {
      FilePath projectDir = projects::projectContext().directory();
      error = r::exec::RFunction(".rs.renv.listPackages")
            .addParam(string_utils::utf8ToSystem(projectDir.getAbsolutePath()))
            .call(&packageListSEXP, &protect);
   }
   else
   {
      error = r::exec::RFunction(".rs.listInstalledPackages")
              .call(&packageListSEXP, &protect);
   }

   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // return the generated package list and the Packrat context
   r::json::jsonValueFromObject(packageListSEXP, &packageListJson);

   (*pJson)["package_list"] = packageListJson;
   (*pJson)["packrat_context"] = packrat::contextAsJson(packratContext);
   (*pJson)["renv_context"] = renvContext;

   json::Object activeRepository;
   error = r::exec::RFunction(".rs.ppm.getActiveRepository").call(&activeRepository);
   if (error)
      LOG_ERROR(error);
   (*pJson)["active_repository"] = activeRepository;

   // NOTE: vulnerability data is intentionally not part of the package state.
   // It is fetched asynchronously (see ppm::refreshVulnerabilitiesAsync) so that
   // a slow or unreachable PPM can't block the package list -- and therefore IDE
   // startup -- and delivered separately via the kPackageVulnerabilitiesReady
   // event.

   return Success();
}

SEXP rs_enqueLoadedPackageUpdates(SEXP installCmdSEXP)
{
   std::string installCmd;
   if (installCmdSEXP != R_NilValue)
      installCmd = r::sexp::asString(installCmdSEXP);

   ClientEvent event(client_events::kLoadedPackageUpdates, installCmd);
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

SEXP rs_canInstallPackages()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(session::options().allowPackageInstallation(),
                          &rProtect);
}

SEXP rs_shouldRecordPackageSources()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(session::options().allowPackageSourceRecording(),
                          &rProtect);
}

SEXP rs_packageLibraryMutated()
{
   // broadcast event to server
   module_context::events().onPackageLibraryMutated();

   // broadcast event to client
   enquePackageStateChanged();

   return R_NilValue;
}

// Result of a background package-event scan. The background thread fills in
// 'pkgNames' and sets 'done' under the mutex; the main thread polls it via
// schedulePeriodicWork. We deliberately avoid executeOnMainThread() here, as
// scheduling work from a background thread mutates the scheduled-command list
// without synchronization.
struct PackageEventScan
{
   boost::mutex mutex;
   bool done = false;
   std::vector<std::string> pkgNames;
};

// Coalescing state for package-event scans; main thread only.
bool s_packageEventScanRunning = false;
bool s_packageEventScanPending = false;

void startPackageEventScan();

// Runs on a background thread: filesystem access only, no R.
void scanForInstalledPackages(std::vector<FilePath> libPaths,
                              boost::shared_ptr<PackageEventScan> pScan)
{
   std::vector<std::string> pkgNames;
   for (const FilePath& libPath : libPaths)
   {
      if (!libPath.exists())
         continue;

      std::vector<FilePath> children;
      Error error = libPath.getChildren(children);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }

      for (const FilePath& child : children)
      {
         if (child.isDirectory())
            pkgNames.push_back(child.getFilename());
      }
   }

   LOCK_MUTEX(pScan->mutex)
   {
      pScan->pkgNames = std::move(pkgNames);
      pScan->done = true;
   }
   END_LOCK_MUTEX
}

// Main-thread poll for scan completion; returns true to keep polling.
bool checkPackageEventScan(boost::shared_ptr<PackageEventScan> pScan)
{
   std::vector<std::string> pkgNames;

   LOCK_MUTEX(pScan->mutex)
   {
      if (!pScan->done)
         return true;

      // consume the result: background processing can run re-entrantly while
      // the R call below executes, and a second poll of this same scan should
      // treat it as still pending rather than register twice
      pScan->done = false;
      pkgNames = std::move(pScan->pkgNames);
   }
   END_LOCK_MUTEX

   Error error = r::exec::RFunction(".rs.registerPackageEventHooks", pkgNames).call();
   if (error)
      LOG_ERROR(error);

   s_packageEventScanRunning = false;

   // if another scan was requested while this one was in flight (e.g. an
   // install completed), run it now so newly-installed packages get hooked
   if (s_packageEventScanPending)
   {
      s_packageEventScanPending = false;
      startPackageEventScan();
   }

   return false;
}

void startPackageEventScan()
{
   s_packageEventScanRunning = true;

   // resolve library paths on the main thread; this calls into R
   std::vector<FilePath> libPaths = module_context::getLibPaths();

   auto pScan = boost::make_shared<PackageEventScan>();
   core::thread::safeLaunchThread(
            boost::bind(scanForInstalledPackages, libPaths, pScan));

   module_context::schedulePeriodicWork(
            boost::posix_time::milliseconds(100),
            boost::bind(checkPackageEventScan, pScan),
            false);
}

// Enumerating the library paths can be slow on remote filesystems, so it runs
// on a background thread; if a scan is already in flight, just request another
// pass once it completes (the running scan may have raced with the library
// mutation that triggered this call).
void updatePackageEventsAsync()
{
   if (s_packageEventScanRunning)
      s_packageEventScanPending = true;
   else
      startPackageEventScan();
}

SEXP rs_updatePackageEvents()
{
   updatePackageEventsAsync();
   return R_NilValue;
}

void detectLibPathsChanges()
{
   static std::vector<std::string> s_lastLibPaths;
   std::vector<std::string> libPaths;
   Error error = r::exec::RFunction("base:::.libPaths").call(&libPaths);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (s_lastLibPaths.empty())
   {
      s_lastLibPaths = libPaths;
   }
   else if (libPaths != s_lastLibPaths)
   {
      module_context::events().onLibPathsChanged(libPaths);
      enquePackageStateChanged();
      s_lastLibPaths = libPaths;
   }
}

void onConsoleInput(const std::string& input)
{
   // record last console input
   s_lastInput += input;
   
   // record prior repository
   SEXP reposSEXP = r::options::getOption("repos");
   s_reposSEXP.set(reposSEXP);
}

// A cheap pre-filter that just asks "does this input contain a function call?".
// It matches an identifier (the function name) followed by '(', which also
// covers namespace-qualified calls like 'pkg::fn(' since the 'fn(' portion
// matches. The character class includes '.' because R identifiers may contain
// it ('install.packages'). This intentionally accepts any call, not just
// package-management ones: it only decides whether to run the precise,
// namespace-resolving check in '.rs.isPackageManagementCall'. Keeping the
// syntactic gate dumb means the authoritative list of package-management
// functions lives in exactly one place (that R function) rather than being
// mirrored here.
boost::regex buildCallSyntaxRegex()
{
   return boost::regex("[\\w.]+\\s*\\(");
}

void onConsolePrompt(const std::string&)
{
   // skip if we're being invoked within a readline call
   bool isReadingUserInput = false;
   Error error = r::exec::RFunction(".rs.isReadingUserInput").call(&isReadingUserInput);
   if (error)
      LOG_ERROR(error);

   if (isReadingUserInput)
      return;

   // consume last input
   std::string lastInput = s_lastInput;
   s_lastInput.clear();

   // A cheap regex pre-filter skips call-free console input without touching R;
   // anything that contains a call is handed to the precise check, which parses
   // it and resolves each call's namespace. This keeps a false positive (e.g.
   // 'remove' in a comment, or base R's remove()/update()) from triggering an
   // expensive package-library scan -- see issue #17758.
   if (containsCallSyntax(lastInput))
   {
      bool isPackageCall = false;
      Error error = r::exec::RFunction(".rs.isPackageManagementCall", lastInput)
                       .call(&isPackageCall);
      if (error)
         LOG_ERROR(error);

      if (isPackageCall)
      {
         rs_packageLibraryMutated();
         return;
      }
   }

   // check and see if the 'repos' option has been mutated
   SEXP reposSEXP = r::options::getOption("repos");
   if (reposSEXP != s_reposSEXP.get())
   {
      enquePackageStateChanged();
      return;
   }
}

void onDetectChanges(module_context::ChangeSource source)
{
   // silently drop attempts to call this from non-main thread
   if (!core::thread::isMainThread())
      return;
   
   // check for libPaths changes if we're evaluating a change from the REPL at
   // the top-level (i.e. not while debugging, as we don't want to mutate any
   // state that might be under inspection)
   if (source == module_context::ChangeSourceREPL &&
       core::thread::isMainThread() &&
       r::exec::atTopLevelContext())
   {
      detectLibPathsChanges();
   }
}

void onInitComplete()
{
   s_reposSEXP.set(r::options::getOption("repos"));
}

void onDeferredInit(bool /* newSession */)
{
   // Ensure we have a writeable user library
   Error error = r::exec::RFunction(".rs.ensureWriteableUserLibrary").call();
   if (error)
      LOG_ERROR(error);

   // subscribe to package attach / detach events. this happens here (rather
   // than at module init) both to keep the scan off the startup critical path
   // and because the rs_* routines it relies upon are only registered with R
   // once the init sequence has completed
   updatePackageEventsAsync();

   // monitor libPaths for changes
   detectLibPathsChanges();
   module_context::events().onDetectChanges.connect(onDetectChanges);

   // kick off the initial (asynchronous) vulnerability check
   ppm::refreshVulnerabilitiesAsync();
}

Error getPackageState(const json::JsonRpcRequest& ,
                      json::JsonRpcResponse* pResponse)
{
   json::Object result;
   Error error = getPackageStateJson(&result);
   if (error)
      LOG_ERROR(error);
   else
      pResponse->setResult(result);

   // The package list returns immediately without vulnerability data, which
   // arrives later via kPackageVulnerabilitiesReady. This RPC is the only path
   // for a re-join/reconnect to an already-running session (where onDeferredInit
   // won't fire again) and for the user-facing Refresh command, so kick off a
   // refresh here too -- otherwise a reconnected client would show no badges
   // until some incidental package mutation happened to trigger one.
   ppm::refreshVulnerabilitiesAsync();

   return error;
}

} // anonymous namespace

bool containsCallSyntax(const std::string& input)
{
   static const boost::regex s_pattern = buildCallSyntaxRegex();
   return boost::regex_search(input, s_pattern);
}

void enquePackageStateChanged()
{
   json::Object pkgState;
   Error error = getPackageStateJson(&pkgState);
   if (error)
      LOG_ERROR(error);
   else
   {
      ClientEvent event(client_events::kPackageStateChanged, pkgState);
      module_context::enqueClientEvent(event);
   }

   // the package set or active repository may have changed; refresh
   // vulnerability data asynchronously to match
   ppm::refreshVulnerabilitiesAsync();
}

Error initialize()
{
   // register deferred init
   module_context::events().onInitComplete.connect(onInitComplete);
   module_context::events().onDeferredInit.connect(onDeferredInit);
   module_context::events().onConsoleInput.connect(onConsoleInput);
   module_context::events().onConsolePrompt.connect(onConsolePrompt);

   // register routines
   RS_REGISTER_CALL_METHOD(rs_enqueLoadedPackageUpdates);
   RS_REGISTER_CALL_METHOD(rs_canInstallPackages);
   RS_REGISTER_CALL_METHOD(rs_shouldRecordPackageSources);
   RS_REGISTER_CALL_METHOD(rs_packageLibraryMutated);
   RS_REGISTER_CALL_METHOD(rs_getCachedAvailablePackages);
   RS_REGISTER_CALL_METHOD(rs_downloadAvailablePackages);
   RS_REGISTER_CALL_METHOD(rs_updatePackageEvents);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPackages.R"))
      (bind(registerRpcMethod, "available_packages", availablePackages))
      (bind(registerRpcMethod, "get_package_state", getPackageState))
      (bind(r::exec::executeString, ".rs.packages.initialize()"));
   return initBlock.execute();
}


} // namespace packages
} // namespace modules
} // namespace session
} // namespace rstudio

