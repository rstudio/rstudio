/*
 * SessionProfiler.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include "SessionProfiler.hpp"

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RUtil.hpp>
#include <r/ROptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace profiler {

namespace {

#define kProfilesCacheDir "profiles-cache"
#define kProfilesUrlPath "profiles"

#define kProfilerResource "profiler_resource"
#define kProfilerResourceLocation "/" kProfilerResource "/"

std::string profilesCacheDir() 
{
   return module_context::scopedScratchPath()
       .completeChildPath(kProfilesCacheDir)
       .getAbsolutePath();
}

SEXP rs_profilesPath()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(profilesCacheDir(), &rProtect);
}

} // anonymous namespace

void handleProfilerResReq(const http::Request& request,
                          http::Response* pResponse)
{
   std::string resourceName = http::util::pathAfterPrefix(request, "/" kProfilesUrlPath "/");

   core::FilePath profilesPath = core::FilePath(profilesCacheDir());
   core::FilePath profileResource = profilesPath.completeChildPath(resourceName);

   // cache indefinitely (the cache dir is ephemeral)
   pResponse->setIndefiniteCacheableFile(profileResource, request);
}

void handleProfilerResourceResReq(const http::Request& request,
                            http::Response* pResponse)
{
   std::string path("profiler/");
   path.append(http::util::pathAfterPrefix(request, kProfilerResourceLocation));

   core::FilePath profilerResource = options().rResourcesPath().completeChildPath(path);
   pResponse->setCacheableFile(profilerResource, request);
}

void clearRProfile(const std::string& path, const std::string& htmlLocalPath)
{
   Error error = r::exec::RFunction(".rs.rpc.clear_profile")
         .addUtf8Param(path)
         .addUtf8Param(htmlLocalPath)
         .call();

   if (error)
   {
      LOG_ERROR(error);
   }
}

void onDocPendingRemove(
        boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // check to see if there is cached data
   std::string path = pDoc->getProperty("path");
   std::string htmlLocalPath = pDoc->getProperty("htmlLocalPath");
   if (htmlLocalPath.empty() && path.empty())
      return;

   module_context::executeOnMainThread(boost::bind(clearRProfile, path, htmlLocalPath));
}

Error initialize()
{
   ExecBlock initBlock;
   
   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);

   RS_REGISTER_CALL_METHOD(rs_profilesPath);

   // set up profiles path (be careful to mark as UTF-8)
   r::sexp::Protect protect;
   SEXP cacheDir = r::sexp::createUtf8(profilesCacheDir(), &protect);
   Error error = r::exec::RFunction(".rs.setOptionDefault")
               .addParam("profvis.prof_output")
               .addParam(cacheDir)
               .call();

   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "SessionProfiler.R"))
      (boost::bind(module_context::registerUriHandler, "/" kProfilesUrlPath "/", handleProfilerResReq))
      (boost::bind(module_context::registerUriHandler, kProfilerResourceLocation, handleProfilerResourceResReq));

   return initBlock.execute();
}

} // namespace profiler
} // namespace modules
} // namespace session
} // namespace rstudio

