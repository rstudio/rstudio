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

#include <session/SessionModuleContext.hpp>

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

SEXP rs_enqueLoadedPackageUpdates(SEXP installCmdSEXP)
{
   std::string installCmd;
   if (installCmdSEXP != R_NilValue)
      installCmd = r::sexp::asString(installCmdSEXP);

   ClientEvent event(client_events::kLoadedPackageUpdates, installCmd);
   module_context::enqueClientEvent(event);

   return R_NilValue;
}

void initializeRStudioPackages(bool newSession)
{
#ifdef RSTUDIO_UNVERSIONED_BUILD
   bool force = true;
#else
   bool force = false;
#endif
   
   if (newSession)
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

} // anonymous namespace

Error initialize()
{
   // register deferred init
   module_context::events().onDeferredInit.connect(initializeRStudioPackages);

   // register routines
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_enqueLoadedPackageUpdates" ;
   methodDef.fun = (DL_FUNC) rs_enqueLoadedPackageUpdates ;
   methodDef.numArgs = 1;
   r::routines::addCallMethod(methodDef);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcAsyncCoupleMethod<std::vector<std::string> >,
            "available_packages",
            availablePackagesBegin,
            availablePackagesEnd))
      (bind(sourceModuleRFile, "SessionPackages.R"))
      (bind(r::exec::executeString, ".rs.packages.initialize()"));
   return initBlock.execute();
}


} // namespace packages
} // namespace modules
} // namesapce session

