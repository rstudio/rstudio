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
#include <shared_core/json/Json.hpp>

#include <r/RExec.hpp>

#include <core/Exec.hpp>
#include <core/Version.hpp>
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
         static const boost::regex reOutput("(^|\\n)output:\\s*");
         static const boost::regex reFormat("(^|\\n)format:\\s*");
         static const boost::regex reJupyter("(^|\\n)jupyter:\\s*");
         static const boost::regex reKnitQuarto("(^|\\n)knit:\\s*quarto\\s+render");
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

// Given a path to a Quarto file (usually .qmd), attempt to extract its metadata as a JSON object
Error quartoMetadata(const std::string& path,
                     json::Object *pResultObject)
{
   // Run quarto and retrieve metadata
   std::string output;
   core::system::ProcessResult result;
   Error error = runQuarto({"metadata", path, "--json"}, "", &result);
   if (error)
   {
      return error;
   }

   // Parse JSON result
   return pResultObject->parse(result.stdOut);
}

void readQuartoProjectConfig(const FilePath& configFile,
                             std::string* pType,
                             std::string* pOutputDir,
                             std::vector<std::string>* pFormats)
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
            }
            else if (key == "format")
            {
               if (it->second.Type() == YAML::NodeType::Scalar)
               {
                  pFormats->push_back(it->second.as<std::string>());
               }
               else if (it->second.Type() == YAML::NodeType::Map)
               {
                  for (auto formatIt = it->second.begin(); formatIt != it->second.end(); ++formatIt)
                  {
                     pFormats->push_back(formatIt->first.as<std::string>());
                  }
               }
            }
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   else
   {
      LOG_ERROR(error);
   }
}

Error getQmdPublishDetails(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   using namespace module_context;
   
   std::string target;
   Error error = json::readParams(request.params, &target);
   if (error)
   {
       return error;
   }

   FilePath qmdPath = module_context::resolveAliasedPath(target);

   // Ask Quarto to get the metadata for the file
   json::Object metadata;
   error = quartoMetadata(qmdPath.getAbsolutePath(), &metadata);
   if (error)
   {
       return error;
   }
   auto format = (*metadata.begin()).getValue().getObject();

   json::Object result;

   auto formatMeta = format.find("metadata");

   // Establish output vars
   std::string title = qmdPath.getStem();
   bool isShinyQmd = false;
   bool selfContained = false;
   std::string outputFile;

   // If we were able to get the format's metadata, read it
   if (formatMeta != format.end())
   {
      json::Object formatMetadata = (*formatMeta).getValue().getObject();

      // Attempt to read file title; use stem as a fallback
      json::readObject(formatMetadata, "title", title);

      // Attempt to read file title; use stem as a fallback
      std::string runtime;
      error = json::readObject(formatMetadata, "runtime", runtime);
      if (!error)
      {
         isShinyQmd = runtime == "shinyrmd" || runtime == "shiny_prerendered";
      }
      if (!isShinyQmd)
      {
         json::Value serverJson;
         error = json::readObject(formatMetadata, "server", serverJson);
         if (!error)
         {
            if (serverJson.isString() && serverJson.getString() == "shiny")
            {
               isShinyQmd = true;
            }
            else if (serverJson.isObject())
            {
               std::string type;
               error = json::readObject(serverJson.getObject(), "type", type);
               if (type == "shiny")
               {
                  isShinyQmd = true;
               }
            }
         }
      }
   }


   // If we were able to get the format's pandoc parameters, read them as well
   auto pandoc = format.find("pandoc");
   if (pandoc != format.end())
   {
       json::Object pandocMetadata = (*pandoc).getValue().getObject();
       json::readObject(pandocMetadata, "self-contained", selfContained);

       std::string pandocOutput;
       json::readObject(pandocMetadata, "output-file", pandocOutput);
       auto outputFilePath = qmdPath.getParent().completeChildPath(pandocOutput);
       if (outputFilePath.exists())
       {
           outputFile = outputFilePath.getAbsolutePath();
       }
   }


   // Look up configuration for this Quarto project, if this file is part of a Quarto book or
   // website.
   std::string websiteDir, websiteOutputDir;
   FilePath quartoConfig = quartoProjectConfigFile(qmdPath);
   if (!quartoConfig.isEmpty())
   {
       std::string type, outputDir;
       std::vector<std::string> formats;
       readQuartoProjectConfig(quartoConfig,
                               &type,
                               &outputDir,
                               &formats);
       if (type == kQuartoProjectBook || type == kQuartoProjectSite)
       {
          FilePath configPath = quartoConfig.getParent();
          websiteDir = configPath.getAbsolutePath();
          // Infer output directory 
          if (outputDir.empty())
          {
              if (type == kQuartoProjectBook)
              {
                  outputDir = "_book";
              }
              else
              {
                  outputDir = "_site";
              }
          }
          websiteOutputDir = configPath.completeChildPath(outputDir).getAbsolutePath();
       }
   }

   // Attempt to determine whether or not the user has an active publishing account; used on the
   // client to trigger an account setup step if necessary
   r::sexp::Protect protect;
   SEXP sexpHasAccount;
   bool hasAccount = true;
   error = r::exec::RFunction(".rs.hasConnectAccount").call(&sexpHasAccount, &protect);
   if (error)
   {
      LOG_WARNING_MESSAGE("Couldn't determine whether Connect account is present");
      LOG_ERROR(error);
   }
   else
   {
       hasAccount = r::sexp::asLogical(sexpHasAccount);
   }


   // Build result object
   result["is_self_contained"] = selfContained;
   result["title"] = title;
   result["is_shiny_qmd"] = isShinyQmd;
   result["website_dir"] = websiteDir;
   result["website_output_dir"] = websiteOutputDir;
   result["has_connect_account"] = hasAccount;
   result["output_file"] = outputFile;

   pResponse->setResult(result);

   return Success();
}

}

namespace module_context {

bool handleQuartoPreview(const core::FilePath& sourceFile,
                         const core::FilePath& outputFile,
                         const std::string& renderOutput,
                         bool validateExtendedType)
{
   // don't do anyting if user prefs are set to no preview
   if (prefs::userPrefs().rmdViewerType() == kRmdViewerTypeNone)
      return false;

   // don't do anything if there is no quarto
   if (!modules::quarto::isInstalled())
      return false;

   // don't do anything if this isn't a quarto doc
   if (validateExtendedType)
   {
      std::string extendedType;
      Error error = source_database::detectExtendedType(sourceFile, &extendedType);
      if (error)
         return false;
      if (extendedType != kQuartoXt)
         return false;
   }

   // if the current project is a site or book and this file is within it,
   // then initiate a preview (one might be already running)
   auto config = quartoConfig();
   if ((config.project_type == kQuartoProjectSite || config.project_type == kQuartoProjectBook) &&
       sourceFile.isWithin(module_context::resolveAliasedPath(config.project_dir)))
   {
      // preview the doc (but schedule it for later so we can get out of the onCompleted
      // handler this was called from -- launching a new process in the supervisor when
      // an old one is in the middle of executing onCompleted doesn't work
      module_context::scheduleDelayedWork(boost::posix_time::milliseconds(10),
                                          boost::bind(modules::quarto::serve::previewDoc,
                                                      renderOutput, outputFile),
                                          false);
      return true;
   }

   // if this file is within another quarto site or book project then no preview at all
   // (as it will more than likely be broken)
   FilePath configFile = quartoProjectConfigFile(sourceFile);
   if (!configFile.isEmpty())
   {
      std::string type, outputDir;
      std::vector<std::string> formats;
      readQuartoProjectConfig(configFile, &type, &outputDir, &formats);
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

   if (refresh)
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
            readQuartoProjectConfig(configFile,
                                    &s_quartoConfig.project_type,
                                    &s_quartoConfig.project_output_dir,
                                    &s_quartoConfig.project_formats);

            // provide default output dirs
            if (s_quartoConfig.project_output_dir.length() == 0)
            {
               if (s_quartoConfig.project_type == kQuartoProjectSite)
                  s_quartoConfig.project_output_dir = "_site";
               else if (s_quartoConfig.project_type == kQuartoProjectBook)
                  s_quartoConfig.project_output_dir = "_book";
            }
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
      // reset
      s_quartoPath = FilePath();

      // see if quarto is on the path
      FilePath quartoPath;
      Error error = core::system::findProgramOnPath("quarto", &quartoPath);
      if (!error)
      {
         // convert to real path
         error = core::system::realPath(quartoPath, &quartoPath);
         if (!error)
         {
            // read version file -- if it doesn't exist we are running the dev
            // version so are free to proceed
            FilePath versionFile = quartoPath
               .getParent()
               .getParent()
               .completeChildPath("share")
               .completeChildPath("version");
            if (versionFile.exists())
            {
               std::string contents;
               error = core::readStringFromFile(versionFile, &contents);
               if (!error)
               {
                  const Version kQuartoRequiredVersion("0.1.319");
                  boost::algorithm::trim(contents);
                  Version quartoVersion(contents);
                  if (quartoVersion >= kQuartoRequiredVersion)
                  {
                     s_quartoPath = quartoPath;
                  }
                  else
                  {
                     // enque a warning
                     const char * const kUpdateURL = "https://github.com/quarto-dev/quarto-cli/releases/latest";
                     json::Object msgJson;
                     msgJson["severe"] = false;
                     boost::format fmt(
                       "Quarto CLI version %1% is installed, however RStudio requires version %2%. "
                       "Please update to the latest version at <a href=\"%3%\" target=\"_blank\">%3%</a>"
                     );
                     msgJson["message"] = boost::str(fmt %
                                                     std::string(quartoVersion) %
                                                     std::string(kQuartoRequiredVersion) %
                                                     kUpdateURL);
                     ClientEvent event(client_events::kShowWarningBar, msgJson);
                     module_context::enqueClientEvent(event);
                  }
               }
               else
               {
                  LOG_ERROR(error);
               }
            }
            // no version file means dev version, so we are okay
            else
            {
               s_quartoPath = quartoPath;
            }
         }
         else
         {
            LOG_ERROR(error);
         }
      }
      else
      {
         LOG_ERROR(error);
      }
   }

   return !s_quartoPath.isEmpty();
}



json::Object quartoConfigJSON(bool refresh)
{
   module_context::QuartoConfig config = module_context::quartoConfig(refresh);
   json::Object quartoConfigJSON;
   quartoConfigJSON["installed"] = config.installed;
   quartoConfigJSON["is_project"] = config.is_project;
   quartoConfigJSON["project_dir"] = config.project_dir;
   quartoConfigJSON["project_type"] = config.project_type;
   quartoConfigJSON["project_output_dir"] = config.project_output_dir;
   quartoConfigJSON["project_formats"] = json::toJsonArray(config.project_formats);
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
     (boost::bind(module_context::registerRpcMethod, "get_qmd_publish_details", getQmdPublishDetails))
     (boost::bind(module_context::sourceModuleRFile, "SessionQuarto.R"))
     (boost::bind(quarto::serve::initialize))
   ;
   return initBlock.execute();
}

} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
