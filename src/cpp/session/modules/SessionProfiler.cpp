/*
 * SessionProfiler.cpp
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


#include "SessionProfiler.hpp"

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

#include <core/Error.hpp>
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

std::string profilesCacheDir() 
{
   return module_context::scopedScratchPath().childPath(kProfilesCacheDir)
      .absolutePath();
}

SEXP rs_profilesPath()
{
   r::sexp::Protect rProtect;
   std::string cacheDir = core::string_utils::utf8ToSystem(profilesCacheDir());
   return r::sexp::create(cacheDir, &rProtect);
}

} // anonymous namespace

void handleProfilerResReq(const http::Request& request,
                            http::Response* pResponse)
{
   std::string resourceName = http::util::pathAfterPrefix(request, "/" kProfilesUrlPath "/");

   core::FilePath profilesPath = core::FilePath(profilesCacheDir());
   core::FilePath profileResource = profilesPath.childPath(resourceName);

   pResponse->setCacheableFile(profileResource, request);
}

void onDocPendingRemove(
        boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // check to see if there is cached data
   std::string path = pDoc->getProperty("path");
   std::string htmlLocalPath = pDoc->getProperty("htmlLocalPath");
   if (htmlLocalPath.empty() && path.empty())
      return;

   r::exec::RFunction rFunction(".rs.rpc.clear_profile");
   rFunction.addParam(path);
   rFunction.addParam(htmlLocalPath);

   Error error = rFunction.call();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
}

Error initialize()
{  
   ExecBlock initBlock ;
   
   source_database::events().onDocPendingRemove.connect(onDocPendingRemove);

   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "SessionProfiler.R"))
      (boost::bind(module_context::registerUriHandler, "/" kProfilesUrlPath "/", handleProfilerResReq));

   RS_REGISTER_CALL_METHOD(rs_profilesPath, 0);

   return initBlock.execute();
}

} // namespace profiler
} // namespace modules
} // namespace session
} // namespace rstudio

