/*
 * SessionShiny.cpp
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

#include "SessionShiny.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>

#include <r/RExec.hpp>

#include <session/SessionRUtil.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace shiny {

namespace {

void onPackageLoaded(const std::string& pkgname)
{
   // we need an up to date version of shiny when running in server mode
   // to get the websocket protocol/path and port randomizing changes
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      if (pkgname == "shiny")
      {
         if (!module_context::isPackageVersionInstalled("shiny", "0.8"))
         {
            module_context::consoleWriteError("\nWARNING: To run Shiny "
              "applications with RStudio you need to install the "
              "latest version of the Shiny package from CRAN (version 0.8 "
              "or higher is required).\n\n");
         }
      }
   }
}



bool isShinyAppDir(const FilePath& filePath)
{
   bool hasServer = filePath.childPath("server.R").exists() ||
                    filePath.childPath("server.r").exists();
   if (hasServer)
   {
      bool hasUI = filePath.childPath("ui.R").exists() ||
                   filePath.childPath("ui.r").exists() ||
                   filePath.childPath("www").exists();

      return hasUI;
   }
   else
   {
      return false;
   }
}

std::string onDetectShinySourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   const char * const kShinyType = "shiny";

   if (!pDoc->path().empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      if (isShinyDocument(filePath, pDoc->contents()))
         return kShinyType;
   }

   return std::string();
}

Error getShinyCapabilities(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   json::Object capsJson;
   capsJson["installed"] = module_context::isPackageInstalled("shiny");
   pResponse->setResult(capsJson);

   return Success();
}

} // anonymous namespace

bool isShinyDocument(const FilePath& filePath,
                     const std::string& contents)
{
   static const boost::regex reRuntimeShiny("runtime:\\s*shiny");
   
   // Check for 'runtime: shiny' in a YAML header.
   std::string yamlHeader = r_utils::extractYamlHeader(contents);
   if (boost::regex_search(yamlHeader.begin(), yamlHeader.end(), reRuntimeShiny))
      return true;
   
   std::string filename = filePath.filename();

   if (boost::algorithm::iequals(filename, "ui.r") &&
       boost::algorithm::icontains(contents, "shinyUI"))
   {
      return true;
   }
   else if (boost::algorithm::iequals(filename, "server.r") &&
            boost::algorithm::icontains(contents, "shinyServer"))
   {
      return true;
   }
   else if (boost::algorithm::iequals(filename, "app.r") && 
            boost::algorithm::icontains(contents, "shinyApp"))
   {
      return true;
   }
   else if ((boost::algorithm::iequals(filename, "global.r") ||
             boost::algorithm::iequals(filename, "ui.r") ||
             boost::algorithm::iequals(filename, "server.r")) &&
            isShinyAppDir(filePath.parent()))
   {
      return true;
   }

   return false;
}

bool isShinyDocument(const FilePath& filePath)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   
   return isShinyDocument(filePath, contents);
}

Error initialize()
{
   using namespace module_context;
   events().onPackageLoaded.connect(onPackageLoaded);

   events().onDetectSourceExtendedType.connect(onDetectShinySourceType);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(registerRpcMethod, "get_shiny_capabilities", getShinyCapabilities));

   return initBlock.execute();
}


} // namespace crypto
} // namespace modules
} // namesapce session
} // namespace rstudio

