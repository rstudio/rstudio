/*
 * SessionPackages.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

// boost requires that winsock2.h must be included before windows.h
#ifdef _WIN32
#include <winsock2.h>
#endif

#include "SessionPackages.hpp"

#include <boost/bind.hpp>
#include <boost/regex.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/http/URL.hpp>
#include <core/http/TcpIpBlockingClient.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>
#include <r/RInterface.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionAsyncRProcess.hpp>

#include "SessionPackrat.hpp"

#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

namespace {

bool isSecure(const std::string& url)
{
   return boost::algorithm::starts_with(url, "https");
}

void insecureReposURLWarning(const std::string& url,
                             const std::string& extraMsg = "")
{
   std::string msg =
         "Your CRAN mirror is set to \"" + url + "\" which "
         "has an insecure (non-HTTPS) URL.";
   
   if (!extraMsg.empty())
      msg += " " + extraMsg;
   
   Error error = r::exec::RFunction(".rs.insecureReposWarning", msg).call();
   if (error)
      LOG_ERROR(error);
}

void insecureDownloadWarning(const std::string& msg)
{
   Error error = r::exec::RFunction(".rs.insecureDownloadWarning", msg).call();
   if (error)
      LOG_ERROR(error);
}


void unableToSecureConnectionWarning(const std::string& url)
{
   boost::format fmt(
      "You are configured to use the CRAN mirror at %1%. This mirror "
      "supports secure (HTTPS) downloads however your system is unable to "
      "communicate securely with the server (possibly due to out of date "
      "certificate files on your system). Falling back to using insecure "
      "URL for this mirror."
   );

   insecureDownloadWarning(boost::str(fmt % url));
}

bool isCRANReposFromSettings()
{
   bool fromSettings = true;
   Error error = r::exec::RFunction(".rs.isCRANReposFromSettings").call(
                                                              &fromSettings);
   if (error)
      LOG_ERROR(error);
   return fromSettings;
}


class CRANMirrorHttpsUpgrade : public async_r::AsyncRProcess
{
public:
   static void attemptUpgrade()
   {
      // get the URL currently in settings. if it's https already then bail
      CRANMirror mirror = userSettings().cranMirror();
      if (isSecure(mirror.url))
         return;

      // modify to be secure
      mirror.url = boost::algorithm::replace_first_copy(mirror.url,
                                                        "http://",
                                                        "https://");

      // build the command
      std::string cmd("{ " + module_context::CRANDownloadOptions() + "; ");
      cmd += "tmp <- tempfile(); ";
      cmd += "download.file(paste(contrib.url('" + mirror.url +
              "'), '/PACKAGES.gz', sep = ''), destfile = tmp); ";
      cmd += "cat(readLines(tmp)); ";
      cmd += "} ";

      // kickoff the process
      boost::shared_ptr<CRANMirrorHttpsUpgrade> pUpgrade(
                                    new CRANMirrorHttpsUpgrade(mirror));
      pUpgrade->start(cmd.c_str(), FilePath(), async_r::R_PROCESS_VANILLA);
   }

   virtual void onStdout(const std::string& output)
   {
      output_ += output;
   }

   virtual void onCompleted(int exitStatus)
   {
      if ((exitStatus == EXIT_SUCCESS) && checkOutputForSuccess())
      {
         userSettings().setCRANMirror(secureMirror_);
      }
      else
      {
         std::string url = userSettings().cranMirror().url;
         if (isKnownSecureMirror(url))
            unableToSecureConnectionWarning(secureMirror_.url);
         else
            insecureReposURLWarning(url);
      }
   }

private:
   bool checkOutputForSuccess()
   {
      return boost::algorithm::contains(output_, "Package: Matrix");
   }

   bool isKnownSecureMirror(const std::string& url)
   {
      std::vector<std::string> mirrors;
      mirrors.push_back("http://cran.rstudio.com/");
      return std::find(mirrors.begin(), mirrors.end(), url) != mirrors.end();
   }

private:
   explicit CRANMirrorHttpsUpgrade(const CRANMirror& secureMirror)
      : secureMirror_(secureMirror)
   {
   }
   std::string output_;
   CRANMirror secureMirror_;
};


void revertCRANMirrorToHTTP()
{
   CRANMirror mirror = userSettings().cranMirror();
   boost::algorithm::replace_first(mirror.url, "https://", "http://");
   userSettings().setCRANMirror(mirror);
}

} // anonymous namespace

void reconcileSecureDownloadConfiguration()
{
   // secure downloads enabled
   if (userSettings().securePackageDownload())
   {
      // ensure we have a secure download method
      Error error = r::exec::RFunction(".rs.initSecureDownload").call();
      if (error)
         LOG_ERROR(error);

      // if we couldn't get one then a suitable warning has been printed,
      // revert any https mirror and exit
      if (!module_context::haveSecureDownloadFileMethod())
      {
         revertCRANMirrorToHTTP();
         return;
      }

      // if the current repository is secure then don't bother (it may
      // be secure via the setting or by the user setting it explicitly
      // within .Rprofile)
      std::string reposURL = module_context::CRANReposURL();
      if (isSecure(reposURL))
         return;

      // if there is a global repository set and it's inscure then warn
      // (in this case the global repository is always overriding the user
      // provided repository so it only makes sense to check/verify the
      // global repository)
      std::string globalRepos = session::options().rCRANRepos();
      if (!globalRepos.empty() && !isSecure(globalRepos))
      {
         insecureReposURLWarning(globalRepos,
            "Please report this to your server administrator."
         );
      }

      // if the repository was set in R profile then we also need to
      // just warn and bail
      else if (!isCRANReposFromSettings())
      {
         insecureReposURLWarning(reposURL,
            "The repository was likely specified in .Rprofile or "
            "Rprofile.site so if you wish to change it you may need "
            "to edit one of those files.");
      }

      // let's see if we can automatically update the user's CRAN repos to
      // an HTTPS connection
      else
      {
         CRANMirrorHttpsUpgrade::attemptUpgrade();
      }
   }

   // secure downloads not enabled -- back out any https url
   else
   {
      revertCRANMirrorToHTTP();
   }
}

} // namespace module_context

namespace modules { 
namespace packages {

namespace {

class AvailablePackagesCache : public boost::noncopyable
{
public:
   
   static AvailablePackagesCache& get()
   {
      static AvailablePackagesCache instance;
      return instance;
   }
   
private:
   
   AvailablePackagesCache()
   {
   }
   
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
         // log error if it wasn't merly a null return value
         if (error.code() != r::errc::UnexpectedDataTypeError)
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
   std::unique(availablePackages.begin(), availablePackages.end());

   // return as json
   json::Array jsonResults;
   for (size_t i = 0; i < availablePackages.size(); i++)
      jsonResults.push_back(availablePackages.at(i));
   pResponse->setResult(jsonResults);
   return Success();
}

Error getPackageStateJson(json::Object* pJson, bool useCachedPackratActions)
{
   Error error = Success();
   module_context::PackratContext context = module_context::packratContext();
   json::Value packageListJson;
   r::sexp::Protect protect;
   SEXP packageList;

   // determine the appropriate package listing method from the current 
   // packrat mode status
   if (context.modeOn)
   {
      FilePath projectDir = projects::projectContext().directory();
      error = r::exec::RFunction(".rs.listPackagesPackrat", 
                                 string_utils::utf8ToSystem(
                                    projectDir.absolutePath()))
              .call(&packageList, &protect);
   }
   else
   {
      error = r::exec::RFunction(".rs.listInstalledPackages")
              .call(&packageList, &protect);
   }

   if (!error)
   {
      // return the generated package list and the Packrat context
      r::json::jsonValueFromObject(packageList, &packageListJson);
      (*pJson)["package_list"] = packageListJson;
      (*pJson)["packrat_context"] = packrat::contextAsJson(context);
      if (context.modeOn)
         packrat::annotatePendingActions(pJson, useCachedPackratActions);
   }

   return error;
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

void rs_packageLibraryMutated()
{
   // broadcast event to server
   module_context::events().onPackageLibraryMutated();

   // broadcast event to client
   enquePackageStateChanged();
}

void detectLibPathsChanges()
{
   static std::vector<std::string> s_lastLibPaths;
   std::vector<std::string> libPaths;
   Error error = r::exec::RFunction("base:::.libPaths").call(&libPaths);
   if (!error)
   {
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
   else
   {
      LOG_ERROR(error);
   }
}

// if the last input had an install_github then fire event
std::string s_lastInput;
void onConsoleInput(const std::string& input)
{
   s_lastInput = input;
}
void onConsolePrompt(const std::string&)
{
   if (boost::algorithm::contains(s_lastInput, "install_github("))
      rs_packageLibraryMutated();
}

void onDetectChanges(module_context::ChangeSource source)
{
   // check for libPaths changes if we're evaluating a change from the REPL at
   // the top-level (i.e. not while debugging, as we don't want to mutate any
   // state that might be under inspection)
   if (source == module_context::ChangeSourceREPL && 
       r::exec::atTopLevelContext())
      detectLibPathsChanges();
}

void onDeferredInit(bool newSession)
{
   // Ensure we have a writeable user library
   Error error = r::exec::RFunction(".rs.ensureWriteableUserLibrary").call();
   if (error)
      LOG_ERROR(error);

   // monitor libPaths for changes
   detectLibPathsChanges();
   module_context::events().onDetectChanges.connect(onDetectChanges);

   // ensure we have a secure connection to CRAN
   module_context::reconcileSecureDownloadConfiguration();
}

Error getPackageState(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   json::Object result;
   // in Packrat mode a manual refresh will bypass any cached state and do work
   // to get the current state (even if it's expensive)
   bool manualCheck = false;
   Error error = json::readParams(request.params, &manualCheck);
   if (error)
      return error;
   error = getPackageStateJson(&result, !manualCheck);
   if (error) 
      LOG_ERROR(error);
   else
      pResponse->setResult(result);
   return error;
}

} // anonymous namespace

void enquePackageStateChanged()
{
   json::Object pkgState;
   Error error = getPackageStateJson(&pkgState, true);
   if (error)
      LOG_ERROR(error);
   else
   {
      ClientEvent event(client_events::kPackageStateChanged, pkgState);
      module_context::enqueClientEvent(event);
   }
}

Error initialize()
{
   // register deferred init
   module_context::events().onDeferredInit.connect(onDeferredInit);
   module_context::events().onConsoleInput.connect(onConsoleInput);
   module_context::events().onConsolePrompt.connect(onConsolePrompt);

   // register routines
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_enqueLoadedPackageUpdates" ;
   methodDef.fun = (DL_FUNC) rs_enqueLoadedPackageUpdates ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   R_CallMethodDef methodDef2 ;
   methodDef2.name = "rs_canInstallPackages" ;
   methodDef2.fun = (DL_FUNC) rs_canInstallPackages ;
   methodDef2.numArgs = 0;
   r::routines::addCallMethod(methodDef2);

   R_CallMethodDef methodDef3 ;
   methodDef3.name = "rs_packageLibraryMutated" ;
   methodDef3.fun = (DL_FUNC) rs_packageLibraryMutated ;
   methodDef3.numArgs = 0;
   r::routines::addCallMethod(methodDef3);
   
   r::routines::registerCallMethod(
            "rs_getCachedAvailablePackages",
            (DL_FUNC) rs_getCachedAvailablePackages,
            1);
   
   r::routines::registerCallMethod(
            "rs_downloadAvailablePackages",
            (DL_FUNC) rs_downloadAvailablePackages,
            1);
   
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
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

