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

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "SessionPackrat.hpp"

#include "session-config.h"

using namespace core;

namespace session {
namespace modules { 
namespace packages {

namespace {

Error availablePackagesBegin(const core::json::JsonRpcRequest& request,
                             std::vector<std::string>* pContribUrls)
{
   return r::exec::evaluateString<std::vector<std::string> >(
         "contrib.url(getOption('repos'), getOption('pkgType'))",
         pContribUrls);
}

class AvailablePackagesCache
{
public:
   AvailablePackagesCache()
      : pMutex_(new boost::mutex())
   {
   }

   void insert(const std::string& contribUrl,
               const std::vector<std::string>& availablePackages)
   {
      LOCK_MUTEX(*pMutex_)
      {
         cache_[contribUrl] = availablePackages;
      }
      END_LOCK_MUTEX
   }


   bool lookup(const std::string& contribUrl,
               std::vector<std::string>* pAvailablePackages)
   {
      LOCK_MUTEX(*pMutex_)
      {
         std::map<std::string, std::vector<std::string> >::const_iterator it =
                                                         cache_.find(contribUrl);
         if (it != cache_.end())
         {
            *pAvailablePackages = it->second;
            return true;
         }
         else
         {
            return false;
         }
      }
      END_LOCK_MUTEX

      // keep compiler happy
      return false;
   }

private:
   // make mutex heap based to avoid boost mutex assertions when
   // it is destructucted in a multicore forked child
   boost::mutex* pMutex_;
   std::map<std::string, std::vector<std::string> > cache_;
};

void downloadAvailablePackages(const std::string& contribUrl,
                               std::vector<std::string>* pAvailablePackages)
{
   // cache available packages to minimize http round trips
   static AvailablePackagesCache s_availablePackagesCache;

   // check cache first
   std::vector<std::string> availablePackages;
   if (s_availablePackagesCache.lookup(contribUrl, &availablePackages))
   {
      std::copy(availablePackages.begin(),
                availablePackages.end(),
                std::back_inserter(*pAvailablePackages));
      return;
   }

   http::URL url(contribUrl + "/PACKAGES");
   http::Request pkgRequest;
   pkgRequest.setMethod("GET");
   pkgRequest.setHost(url.hostname());
   pkgRequest.setUri(url.path());
   pkgRequest.setHeader("Accept", "*/*");
   pkgRequest.setHeader("Connection", "close");
   http::Response pkgResponse;

   Error error = http::sendRequest(url.hostname(),
                                   safe_convert::numberToString(url.port()),
                                   pkgRequest,
                                   &pkgResponse);

   // we don't log errors or bad http status codes because we expect these
   // requests will fail frequently due to either being offline or unable to
   // navigate a proxy server
   if (!error && (pkgResponse.statusCode() == 200))
   {
      std::string body = pkgResponse.body();
      boost::regex re("^Package:\\s*([^\\s]+?)\\s*$");

      boost::sregex_iterator matchBegin(body.begin(), body.end(), re);
      boost::sregex_iterator matchEnd;
      std::vector<std::string> results;
      for (; matchBegin != matchEnd; matchBegin++)
      {
         // copy to temporary list for insertion into cache
         results.push_back((*matchBegin)[1]);

         // append to out param
         pAvailablePackages->push_back((*matchBegin)[1]);
      }

      // add to cache
      s_availablePackagesCache.insert(contribUrl, results);
   }
}

Error availablePackagesEnd(const core::json::JsonRpcRequest& request,
                           const std::vector<std::string>& contribUrls,
                           core::json::JsonRpcResponse* pResponse)
{
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
         enquePackageStateChanged();
         s_lastLibPaths = libPaths;
      }
   }
   else
   {
      LOG_ERROR(error);
   }
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


void initializeRStudioPackages(bool newSession)
{
#ifdef RSTUDIO_UNVERSIONED_BUILD
   bool force = true;
#else
   bool force = false;
#endif
   
   if (newSession || (options().programMode() == kSessionProgramModeServer))
   {
      std::string libDir = core::string_utils::utf8ToSystem(
                              options().sessionLibraryPath().absolutePath());
      std::string pkgSrcDir = core::string_utils::utf8ToSystem(
                              options().sessionPackagesPath().absolutePath());
      std::string rsVersion = RSTUDIO_VERSION;
      Error error = r::exec::RFunction(".rs.initializeRStudioPackages",
                                                                  libDir,
                                                                  pkgSrcDir,
                                                                  rsVersion,
                                                                  force)
                                                                       .call();
      if (error)
         LOG_ERROR(error);
   }
}

void onDeferredInit(bool newSession)
{
   // initialize rstudio packages
   initializeRStudioPackages(newSession);

   // monitor libPaths for changes
   detectLibPathsChanges();
   module_context::events().onDetectChanges.connect(onDetectChanges);
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

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcAsyncCoupleMethod<std::vector<std::string> >,
            "available_packages",
            availablePackagesBegin,
            availablePackagesEnd))
      (bind(sourceModuleRFile, "SessionPackages.R"))
      (bind(registerRpcMethod, "get_package_state", getPackageState))
      (bind(r::exec::executeString, ".rs.packages.initialize()"));
   return initBlock.execute();
}


} // namespace packages
} // namespace modules
} // namesapce session

