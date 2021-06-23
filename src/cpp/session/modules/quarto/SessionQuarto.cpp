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

#include <string>

#include <yaml-cpp/yaml.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Exec.hpp>
#include <core/YamlUtil.hpp>
#include <core/FileSerializer.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/projects/SessionProjects.hpp>

#include <session/prefs/UserPrefs.hpp>

#include "SessionQuartoServe.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {

const char * const kQuartoXt = "quarto-document";

FilePath s_quartoPath;

core::FilePath quartoConfigFilePath(const FilePath& dirPath)
{
   FilePath quartoYml = dirPath.completePath("_quarto.yml");
   if (quartoYml.exists())
      return quartoYml;

   FilePath quartoYaml = dirPath.completePath("_quarto.yaml");
   if (quartoYaml.exists())
      return quartoYaml;

   return FilePath();
}

core::FilePath quartoProjectConfigFile(const core::FilePath& filePath)
{
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
            return FilePath();
      }

      // see if we have a config
      FilePath configFile = quartoConfigFilePath(targetPath);
      if (!configFile.isEmpty())
         return configFile;
   }

   return FilePath();
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
         else if (filePath.isWithin(projects::projectContext().directory()) && modules::quarto::projectIsQuarto())
         {
            return kQuartoXt;

         // file has a parent directory with a quarto config
         } else if (modules::quarto::isInstalled() && !quartoProjectConfigFile(filePath).isEmpty()) {
            return kQuartoXt;
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


void readQuartoProjectConfig(const FilePath& configFile, std::string* pType, std::string* pOutputDir)
{
   // read the config
   std::string configText;
   Error error = core::readStringFromFile(configFile, &configText);
   if (!error)
   {
      try
      {
         YAML::Node node = YAML::Load(configText);
         for (auto it = node.begin(); it != node.end(); ++it)
         {
            std::string key = it->first.as<std::string>();
            if (key == "project" && it->second.Type() == YAML::NodeType::Map)
            {
               for (auto projIt = it->second.begin(); projIt != it->second.end(); ++projIt)
               {
                  if (projIt->second.Type() == YAML::NodeType::Scalar)
                  {
                     std::string projKey = projIt->first.as<std::string>();
                     std::string projValue = projIt->second.Scalar();
                     if (projKey == "type")
                        *pType = projValue;
                     else if (projKey == "output-dir")
                        *pOutputDir = projValue;
                  }
               }
               // got the project config so break
               break;
            }
         }
      }
      CATCH_UNEXPECTED_EXCEPTION;
   }
   else
   {
      LOG_ERROR(error);
   }
}

}

namespace module_context {

bool onHandleRmdPreview(const core::FilePath& filePath)
{
   // don't do anyting if user prefs are set to no preview
   if (prefs::userPrefs().rmdViewerType() == kRmdViewerTypeNone)
      return false;

   // don't do anything if there is no quarto
   if (!modules::quarto::isInstalled())
      return false;

   // don't do anything if this isn't a quarto doc
   std::string extendedType;
   Error error = source_database::detectExtendedType(filePath, &extendedType);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   if (extendedType != kQuartoXt)
      return false;

   // if the current project is a site or book and this file is within it,
   // then initiate a preview (one might be already running)
   auto config = quartoConfig();
   if ((config.project_type == kQuartoProjectSite || config.project_type == kQuartoProjectBook) &&
       filePath.isWithin(module_context::resolveAliasedPath(config.project_dir)))
   {
      // preview the doc (but schedule it for later so we can get out of the onCompleted
      // handler this was called from -- launching a new process in the supervisor when
      // an old one is in the middle of executing onCompleted doesn't work
      module_context::scheduleDelayedWork(boost::posix_time::milliseconds(10),
                                          boost::bind( modules::quarto::serve::previewDoc, filePath),
                                          false);
      return true;
   }

   // if this file is within another quarto site or book project then no preview at all
   // (as it will more than likely be broken)
   FilePath configFile = quartoProjectConfigFile(filePath);
   if (!configFile.isEmpty())
   {
      std::string type, outputDir;
      readQuartoProjectConfig(configFile, &type, &outputDir);
      if (type == kQuartoProjectSite || type == kQuartoProjectBook)
         return true;
   }

   // continue with preview
   return false;
}


const char* const kQuartoProjectSite = "site";
const char* const kQuartoProjectBook = "book";


QuartoConfig quartoConfig(bool refresh)
{
   static module_context::QuartoConfig s_quartoConfig;

   if (refresh || s_quartoConfig.empty)
   {

      s_quartoConfig = QuartoConfig();
      s_quartoConfig.installed = modules::quarto::isInstalled(true);
      using namespace session::projects;
      const ProjectContext& context = projectContext();
      if (context.hasProject())
      {
         // look for a config file in the project directory
         FilePath configFile = quartoConfigFilePath(context.directory());

         // if we don't find one, then chase up the directory heirarchy until we find one
         if (!configFile.exists())
            configFile = quartoProjectConfigFile(context.directory());

         if (configFile.exists())
         {
            // confirm that it's a project
            s_quartoConfig.is_project = true;

            // record the project directory as an aliased path
            s_quartoConfig.project_dir = module_context::createAliasedPath(configFile.getParent());

            // read additional config from yaml
            readQuartoProjectConfig(configFile, &s_quartoConfig.project_type, &s_quartoConfig.project_output_dir);
         }
      }
   }
   return s_quartoConfig;
}


}

namespace modules {
namespace quarto {

bool isInstalled(bool refresh)
{
   if (refresh)
   {
      Error error = core::system::findProgramOnPath("quarto", &s_quartoPath);
      if (error)
         LOG_ERROR(error);
   }

   return !s_quartoPath.isEmpty();
}



json::Object quartoConfigJSON(bool refresh)
{
   module_context::QuartoConfig config = module_context::quartoConfig(refresh);
   json::Object quartoConfigJSON;
   quartoConfigJSON["installed"] = config.installed;
   quartoConfigJSON["is_project"] = config.is_project;
   quartoConfigJSON["project_type"] = config.project_type;
   quartoConfigJSON["project_output_dir"] = config.project_output_dir;
   return quartoConfigJSON;
}

bool projectIsQuarto()
{
   using namespace session::projects;
   const ProjectContext& context = projectContext();
   if (context.hasProject())
   {
      return module_context::quartoConfig().is_project;
   } else {
      return false;
   }
}

Error initialize()
{
   // initialize config at startup
   module_context::quartoConfig(true);

   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectQuartoSourceType);

   // additional initialization
   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "quarto_capabilities", quartoCapabilities))
     (boost::bind(quarto::serve::initialize))
   ;
   return initBlock.execute();
}

} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
