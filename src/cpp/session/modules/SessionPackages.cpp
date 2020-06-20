/*
 * SessionPackages.cpp
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

// boost requires that winsock2.h must be included before windows.h
#ifdef _WIN32
#include <winsock2.h>
#endif

#include "SessionPackages.hpp"

#include <boost/bind.hpp>
#include <boost/regex.hpp>
#include <boost/format.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>
#include <r/RInterface.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "SessionPackrat.hpp"

#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {
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
   std::unique(availablePackages.begin(), availablePackages.end());

   // return as json
   json::Array jsonResults;
   for (size_t i = 0; i < availablePackages.size(); i++)
      jsonResults.push_back(json::Value(availablePackages.at(i)));
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
   SEXP packageList;

   bool renvActive = renvContext.getObject()["active"].getBool();

   // determine the appropriate package listing method from the current
   // packrat mode status
   if (packratContext.modeOn)
   {
      FilePath projectDir = projects::projectContext().directory();
      error = r::exec::RFunction(".rs.listPackagesPackrat",
                                 string_utils::utf8ToSystem(
                                    projectDir.getAbsolutePath()))
              .call(&packageList, &protect);
   }
   else if (renvActive)
   {
      FilePath projectDir = projects::projectContext().directory();
      error = r::exec::RFunction(".rs.renv.listPackages")
            .addParam(string_utils::utf8ToSystem(projectDir.getAbsolutePath()))
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
      (*pJson)["packrat_context"] = packrat::contextAsJson(packratContext);
      (*pJson)["renv_context"] = renvContext;
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

SEXP rs_packageLibraryMutated()
{
   // broadcast event to server
   module_context::events().onPackageLibraryMutated();

   // broadcast event to client
   enquePackageStateChanged();

   return R_NilValue;
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
   return error;
}

} // anonymous namespace

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
}

Error initialize()
{
   // register deferred init
   module_context::events().onDeferredInit.connect(onDeferredInit);
   module_context::events().onConsoleInput.connect(onConsoleInput);
   module_context::events().onConsolePrompt.connect(onConsolePrompt);

   // register routines
   RS_REGISTER_CALL_METHOD(rs_enqueLoadedPackageUpdates);
   RS_REGISTER_CALL_METHOD(rs_canInstallPackages);
   RS_REGISTER_CALL_METHOD(rs_packageLibraryMutated);
   RS_REGISTER_CALL_METHOD(rs_getCachedAvailablePackages);
   RS_REGISTER_CALL_METHOD(rs_downloadAvailablePackages);

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

