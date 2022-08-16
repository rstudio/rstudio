/*
 * SessionCpp.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
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

#include "SessionCpp.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/r_util/RPackageInfo.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include <string>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace cpp {

namespace {

std::string cppProjectStyleImpl() 
{
   projects::ProjectContext& context = projects::projectContext();
   
   // ----- not in a project or not in a package: give up
   if (!context.hasProject() || !context.isPackageProject())
   {
      return "";
   }

   // ----- Look for Rcpp or cpp11 in LinkingTo
   const core::r_util::RPackageInfo& info = context.packageInfo();
   const std::string& linkingTo = info.linkingTo();
   
   if (boost::algorithm::contains(linkingTo, "cpp11")) 
   {
      return "cpp11";
   }
   
   if (boost::algorithm::contains(linkingTo, "Rcpp")) 
   {
      return "Rcpp";
   }

   // ---- check if cpp11 was vendored
   FilePath cpp11Header = context.directory().completePath("inst/include/cpp11.hpp");
   if (cpp11Header.exists())
   {
      return "cpp11";
   }

   // ---- give up
   return "";
}

Error cppProjectStyle(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(cppProjectStyleImpl());
   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionCpp.R"))
      (bind(registerRpcMethod, "cpp_project_style", cppProjectStyle))
   ;
   return initBlock.execute();
}

} // namespace cpp
} // namespace modules
} // namespace session
} // namespace rstudio
