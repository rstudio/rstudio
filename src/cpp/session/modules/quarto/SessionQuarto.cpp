/*
 * SessionQuarto.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include "SessionQuarto.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Exec.hpp>
#include <core/YamlUtil.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/projects/SessionProjects.hpp>

#include <string>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace quarto {
namespace {

const char * const kQuartoXt = "quarto-document";

FilePath s_quartoPath;

bool pathHasQuartoConfig(const FilePath& filePath)
{
   return filePath.completePath("_quarto.yml").exists() ||
          filePath.completePath("_quarto.yaml").exists();
}

std::string onDetectQuartoSourceType(
      boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   // short circuit everything if this is already marked as a quarto markdown doc
   if (pDoc->type() == "quarto_markdown")
   {
      return kQuartoXt;
   }

   if (!pDoc->path().empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
      if (filePath.getExtensionLowerCase() == ".qmd")
      {
         return kQuartoXt;
      }
      else if (filePath.getExtensionLowerCase() == ".rmd" ||
          filePath.getExtensionLowerCase() == ".md")
      {
         // if we have a format: or knit: quarto render then it's a quarto document
         std::string yamlHeader = yaml::extractYamlHeader(pDoc->contents());
         static const boost::regex reOutput("output:\\s*");
         static const boost::regex reFormat("format:\\s*");
         static const boost::regex reJupyter("jupyter:\\s*");
         static const boost::regex reKnitQuarto("knit:\\s*quarto\\s+render");
         // format: without output:
         if (regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reFormat) &&
             !regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reOutput))
         {
            return kQuartoXt;
         }
         // knit: quarto render
         else if (regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reKnitQuarto))
         {
            return kQuartoXt;
         }
         // jupyter:
         else if (filePath.getExtensionLowerCase() == ".md" &&
                  regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reJupyter))
         {
            return kQuartoXt;
         }
         // project has quarto config in build target dir
         else if (filePath.isWithin(projects::projectContext().directory()) && projectIsQuarto())
         {
            return kQuartoXt;

         // file has a parent directory with a quarto config
         } else if (isInstalled()) {

            // list all paths up to root from home dir
            // if we hit the anchor path, or any parent directory of that path
            FilePath anchorPath = module_context::userHomePath();
            std::vector<FilePath> anchorPaths;
            for (; anchorPath.exists(); anchorPath = anchorPath.getParent())
               anchorPaths.push_back(anchorPath);

            // scan through parents
            for (FilePath targetPath = filePath.getParent(); targetPath.exists(); targetPath = targetPath.getParent())
            {
               // bail if we've hit our anchor
               for (const FilePath& anchorPath : anchorPaths)
               {
                  if (targetPath == anchorPath)
                     return "";
               }

               // see if we have a config
               if (pathHasQuartoConfig(targetPath))
               {
                  return kQuartoXt;
               }
            }
         }
      }
   }

   // quarto type not detected
   return "";
}

core::system::ProcessOptions quartoOptions()
{
   core::system::ProcessOptions options;
#ifdef _WIN32
   options.createNewConsole = true;
#else
   options.terminateChildren = true;
#endif
   return options;
}

Error runQuarto(const std::vector<std::string>& args, const std::string& input, core::system::ProcessResult* pResult)
{
   return core::system::runProgram(
      string_utils::utf8ToSystem(s_quartoPath.getAbsolutePath()),
      args,
      input,
      quartoOptions(),
      pResult
   );
}


bool quartoCaptureOutput(const std::vector<std::string>& args,
                         const std::string& input,
                         std::string* pOutput,
                         json::JsonRpcResponse* pResponse)
{
   // run pandoc
   core::system::ProcessResult result;
   Error error = runQuarto(args, input, &result);
   if (error)
   {
      json::setErrorResponse(error, pResponse);
      return false;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      json::setProcessErrorResponse(result, ERROR_LOCATION, pResponse);
      return false;
   }
   else
   {
      *pOutput = result.stdOut;
      return true;
   }
}

Error quartoCapabilities(const json::JsonRpcRequest&,
                         json::JsonRpcResponse* pResponse)
{
   std::string output;
   if (quartoCaptureOutput({"capabilities"}, "", &output, pResponse))
   {
      json::Object jsonCapabilities;
      if (json::parseJsonForResponse(output, &jsonCapabilities, pResponse))
         pResponse->setResult(jsonCapabilities);
   }

   return Success();
}


} // anonymous namespace

bool isInstalled(bool refresh)
{
   if (refresh)
   {
      s_quartoPath = module_context::findProgram("quarto");
   }

   return !s_quartoPath.isEmpty();
}

json::Object quartoConfig(bool refresh)
{
   json::Object jsonConfig;
   jsonConfig["installed"] = isInstalled(refresh);
   return jsonConfig;
}


bool projectIsQuarto()
{
   using namespace session::projects;
   const ProjectContext& context = projectContext();
   if (context.hasProject())
   {
      return pathHasQuartoConfig(context.directory());
   } else {
      return false;
   }
}

Error initialize()
{
   // update status at startup
   isInstalled(true);

   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectQuartoSourceType);

   // register rpc functions

   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "quarto_capabilities", quartoCapabilities))
   ;
   return initBlock.execute();
}

} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
