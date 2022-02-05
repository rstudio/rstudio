/*
 * SessionQuarto.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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
#include <r/RRoutines.hpp>

#include <core/Exec.hpp>
#include <core/Version.hpp>
#include <core/YamlUtil.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/text/AnsiCodeParser.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/Process.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/SessionQuarto.hpp>
#include <session/projects/SessionProjects.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionQuarto.hpp>

#include "SessionQuartoPreview.hpp"
#include "SessionQuartoServe.hpp"
#include "SessionQuartoXRefs.hpp"
#include "SessionQuartoResources.hpp"

using namespace rstudio::core;

// ignored unused functions when quarto not enabled
#ifndef QUARTO_ENABLED
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-function"
#endif

namespace rstudio {
namespace session {

using namespace quarto;

namespace {

const char * const kQuartoXt = "quarto-document";

FilePath s_userInstalledPath;
FilePath s_quartoPath;
std::string s_quartoVersion;

/*
bool haveRequiredQuartoVersion(const std::string& version)
{
   return Version(s_quartoVersion) >= Version(version);
}
*/

Version readQuartoVersion(const core::FilePath& quartoBinPath)
{
   // return empty string if the file doesn't exist
   if (!quartoBinPath.exists())
      return Version();

   // read version file -- if it doesn't exist we are running the dev
   // version so are free to proceed
   FilePath versionFile = quartoBinPath
      .getParent()
      .getParent()
      .completeChildPath("share")
      .completeChildPath("version");
   std::string version;
   if (versionFile.exists())
   {
      Error error = core::readStringFromFile(versionFile, &version);
      if (error)
      {
         LOG_ERROR(error);
         return Version();
      }
      boost::algorithm::trim(version);
   }
   else
   {
      // dev version
      version = "99.9.9";
   }
   return Version(version);
}

void showQuartoVersionWarning(const Version& version, const Version& requiredVersion)
{
   // enque a warning
   const char * const kUpdateURL = "https://quarto.org/docs/getting-started/installation.html";
   json::Object msgJson;
   msgJson["severe"] = false;
   boost::format fmt(
     "Quarto CLI version %1% is installed, however RStudio requires version %2%. "
     "Please update to the latest version at <a href=\"%3%\" target=\"_blank\">%3%</a>"
   );
   msgJson["message"] = boost::str(fmt %
                                   std::string(version) %
                                   std::string(requiredVersion) %
                                   kUpdateURL);
   ClientEvent event(client_events::kShowWarningBar, msgJson);
   module_context::enqueClientEvent(event);
}


std::tuple<FilePath,Version> userInstalledQuarto()
{
   FilePath quartoPath = module_context::findProgram("quarto");
   if (!quartoPath.isEmpty())
   {
      Error error = core::system::realPath(quartoPath, &quartoPath);
      if (!error)
      {
         Version pathVersion = readQuartoVersion(quartoPath);
         if (!pathVersion.empty())
         {
            return std::make_tuple(quartoPath, pathVersion);
         }
      }
      else
      {
         LOG_ERROR(error);
      }
   }
   return std::make_tuple(FilePath(), Version());
}

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


bool projectHasQuartoContent()
{
   using namespace session::projects;
   const ProjectContext& context = projectContext();
   if (context.hasProject())
   {
      if (!quartoConfigFilePath(context.directory()).isEmpty())
      {
         return true;
      }
      else
      {
         // look for a qmd file at the top level
         std::vector<FilePath> files;
         Error error = context.directory().getChildren(files);
         if (error)
         {
            LOG_ERROR(error);
            return false;
         }
         for (auto file : files)
         {
            if (file.getExtensionLowerCase() == ".qmd")
               return true;
         }
         return false;
      }
   }
   else
   {
      return false;
   }
}


void detectQuartoInstallation()
{
#ifdef QUARTO_ENABLED
   // required quarto version (quarto features don't work w/o it)
   const Version kQuartoRequiredVersion("0.3.96");

   // recommended quarto version (a bit more pestery than required)
   const Version kQuartoRecommendedVersion("0.3.96");

   // reset
   s_userInstalledPath = FilePath();
   s_quartoPath = FilePath();
   s_quartoVersion = "";

   // detect user installed version
   auto userInstalled = userInstalledQuarto();
   s_userInstalledPath = std::get<0>(userInstalled);
   s_quartoVersion = std::get<1>(userInstalled);

   // see if the sysadmin or user has turned off quarto
   if (session::prefs::userPrefs().quartoEnabled() == kQuartoEnabledDisabled ||
       session::prefs::userPrefs().quartoEnabled() == kQuartoEnabledHidden)
   {
      return;
   }

   // always use user installed if it's there but subject to version check
   if (!s_userInstalledPath.isEmpty())
   {
      if (std::get<1>(userInstalled) >= kQuartoRecommendedVersion)
      {
         s_quartoPath = std::get<0>(userInstalled);
         s_quartoVersion = std::get<1>(userInstalled);
      }
      else
      {
         showQuartoVersionWarning(std::get<1>(userInstalled), kQuartoRecommendedVersion);
      }
      return;
   }


   // auto mode will enable quarto if we are in a project w/ _quarto.yml
   // or a qmd file at the root, otherwise not
   if (session::prefs::userPrefs().quartoEnabled() == kQuartoEnabledAuto)
   {
      if (projectHasQuartoContent())
      {
         session::prefs::userPrefs().setQuartoEnabled(kQuartoEnabledEnabled);
      }
      else
      {
         return;
      }
   }


   // embedded version of quarto (subject to required version)
#ifndef WIN32
   std::string target = "quarto";
#else
   std::string target = "quarto.cmd";
#endif
   FilePath embeddedQuartoPath = session::options().quartoPath()
      .completeChildPath("bin")
      .completeChildPath(target);
   auto embeddedVersion = readQuartoVersion(embeddedQuartoPath);
   if (embeddedVersion >= kQuartoRequiredVersion)
   {
      s_quartoPath = embeddedQuartoPath;
      s_quartoVersion = embeddedVersion;
      // append to path
      core::system::addToPath(
         string_utils::utf8ToSystem(s_quartoPath.getParent().getAbsolutePath()),
         false
      );
   }
   else
   {
      showQuartoVersionWarning(embeddedVersion, kQuartoRequiredVersion);
   }
#endif
}


bool quartoIsInstalled()
{
   return !s_quartoPath.isEmpty();
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
         else if (filePath.isWithin(projects::projectContext().directory()) && projectIsQuarto())
         {
            return kQuartoXt;

         // file has a parent directory with a quarto config
         } else if (quartoIsInstalled() && !quartoProjectConfigFile(filePath).isEmpty()) {
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

Error runQuarto(const std::vector<std::string>& args,
                const core::FilePath& workingDir,
                core::system::ProcessResult* pResult)
{
   core::system::ProcessOptions options = quartoOptions();
   if (!workingDir.isEmpty())
      options.workingDir = workingDir;

   return core::system::runProgram(
      string_utils::utf8ToSystem(s_quartoPath.getAbsolutePath()),
      args,
      "",
      options,
      pResult
   );
}


Error quartoExec(const std::vector<std::string>& args,
                 const core::FilePath& workingDir,
                 core::system::ProcessResult* pResult)
{
   // run pandoc
   Error error = runQuarto(args, workingDir, pResult);
   if (error)
   {
      return error;
   }
   else if (pResult->exitStatus != EXIT_SUCCESS)
   {
      Error error = systemError(boost::system::errc::state_not_recoverable, pResult->stdErr, ERROR_LOCATION);
      return error;
   }
   else
   {
      return Success();
   }
}

Error quartoExec(const std::vector<std::string>& args,
                 core::system::ProcessResult* pResult)
{
   return quartoExec(args, FilePath(), pResult);
}

bool quartoExec(const std::vector<std::string>& args,
                core::system::ProcessResult* pResult,
                json::JsonRpcResponse* pResponse)
{
   // run pandoc
   Error error = runQuarto(args, FilePath(), pResult);
   if (error)
   {
      json::setErrorResponse(error, pResponse);
      return false;
   }
   else if (pResult->exitStatus != EXIT_SUCCESS)
   {
      json::setProcessErrorResponse(*pResult, ERROR_LOCATION, pResponse);
      return false;
   }
   else
   {
      return true;
   }
}

Error quartoCapabilitiesRpc(const json::JsonRpcRequest&,
                            json::JsonRpcResponse* pResponse)
{
   core::system::ProcessResult result;
   if (quartoExec({"capabilities"}, &result, pResponse))
   {
      json::Object jsonCapabilities;
      if (json::parseJsonForResponse(result.stdOut, &jsonCapabilities, pResponse))
         pResponse->setResult(jsonCapabilities);
   }

   return Success();
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
   json::Object inspect;
   error = quartoInspect(qmdPath.getAbsolutePath(), &inspect);
   if (error)
   {
       return error;
   }

   auto formats = inspect["formats"].getObject();
   auto format = (*formats.begin()).getValue().getObject();

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
         std::string server;
         json::readObject(formatMetadata, "server", server);
         isShinyQmd = server == "shiny";
         if (!isShinyQmd)
         {
            json::Object serverJson;
            error = json::readObject(formatMetadata, "server", serverJson);
            if (!error)
            {
               std::string type;
               error = json::readObject(serverJson, "type", type);
               isShinyQmd = type == "shiny";
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
   auto projectMeta = inspect.find("project");
   if (projectMeta != inspect.end())
   {
      FilePath quartoConfig = quartoProjectConfigFile(qmdPath);
      if (!quartoConfig.isEmpty())
      {
          std::string type, outputDir;
          readQuartoProjectConfig(quartoConfig, &type, &outputDir);
          if (type == kQuartoProjectBook || type == kQuartoProjectWebsite)
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

SEXP rs_quartoFileResources(SEXP targetSEXP)
{
   std::vector<std::string> resources;
   std::string target = r::sexp::safeAsString(targetSEXP);
   if (!target.empty())
   {
      FilePath qmdPath = module_context::resolveAliasedPath(target);
      json::Object jsonInspect;
      Error error = quartoInspect(
        string_utils::utf8ToSystem(qmdPath.getAbsolutePath()), &jsonInspect
      );
      if (!error)
      {
         jsonInspect["resources"].getArray().toVectorString(resources);
      }
      else
      {
         LOG_ERROR(error);
      }
   }

   r::sexp::Protect protect;
   return r::sexp::create(resources, &protect);
}

Error quartoCreateProject(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   std::string projectFile;
   json::Object projectOptionsJson;
   Error error = json::readParams(request.params,
                                  &projectFile,
                                  &projectOptionsJson);
   if (error)
      return error;
   FilePath projectFilePath = module_context::resolveAliasedPath(projectFile);

   // error if the dir already exists
   FilePath projDir = projectFilePath.getParent();
   if (projDir.exists())
      return core::fileExistsError(ERROR_LOCATION);

   // now create it
   error = projDir.ensureDirectory();
   if (error)
      return error;

   std::string type, engine, kernel, venv, condaenv, packages, editor;
   error = json::readObject(projectOptionsJson,
                            "type", type,
                            "engine", engine,
                            "kernel", kernel,
                            "venv", venv,
                            "condaenv", condaenv,
                            "packages", packages,
                            "editor", editor);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // add some first run files
   using namespace module_context;
   std::vector<std::string> projFiles;
   if (boost::algorithm::starts_with(type, kQuartoProjectWebsite) ||
       boost::algorithm::starts_with(type, kQuartoProjectBook))
   {
      projFiles.push_back("index.qmd");
      projFiles.push_back("_quarto.yml");
   }
   else
   {
      projFiles.push_back(projDir.getFilename() + ".qmd");
   }
   projects::addFirstRunDocs(projectFilePath, projFiles);

   // create the project file
   using namespace projects;
   error = r_util::writeProjectFile(projectFilePath,
                                    ProjectContext::buildDefaults(),
                                    ProjectContext::defaultConfig());
   if (error)
      LOG_ERROR(error);


   // create-project command
   std::vector<std::string> args({
      "create-project",
      string_utils::utf8ToSystem(projDir.getAbsolutePath())
   });

   // project type (optional)
   if (!type.empty())
   {
      args.push_back("--type");
      args.push_back(type);
   }

   // project engine/kernel (optional)
   if (!engine.empty())
   {
      std::string qualifiedEngine = engine;
      if (engine == "jupyter" && !kernel.empty())
         qualifiedEngine = qualifiedEngine + ":" + kernel;
      args.push_back("--engine");
      args.push_back(qualifiedEngine);
   }

   // create venv (optional)
   if (engine == "jupyter" && (!venv.empty() || !condaenv.empty()))
   {
      if (!venv.empty())
         args.push_back("--with-venv");
      else
         args.push_back("--with-condaenv");
      if (!packages.empty())
      {
         std::vector<std::string> pkgVector;
         boost::algorithm::split(pkgVector,
                                 packages,
                                 boost::algorithm::is_any_of(", "));
         args.push_back(boost::algorithm::join(pkgVector, ","));
      }
   }

   // visual editor (optional)
   if (!editor.empty())
   {
      args.push_back("--editor");
      args.push_back(editor);
   }

   // create the console process
   using namespace console_process;
   core::system::ProcessOptions options = quartoOptions();
#ifdef _WIN32
   options.detachProcess = true;
#endif
   boost::shared_ptr<ConsoleProcessInfo> pCPI =
         boost::make_shared<ConsoleProcessInfo>("Creating project...",
                                                console_process::InteractionNever);
   boost::shared_ptr<console_process::ConsoleProcess> pCP = ConsoleProcess::create(
     string_utils::utf8ToSystem(s_quartoPath.getAbsolutePath()),
      args,
      options,
      pCPI);

   pResponse->setResult(pCP->toJson(console_process::ClientSerialization));
   return Success();
}

} // anonymous namespace

namespace quarto {

json::Value quartoCapabilities()
{
   if (quartoConfig().enabled)
   {
      core::system::ProcessResult result;
      Error error = quartoExec({ "capabilities" }, &result);
      if (error)
      {
         LOG_ERROR(error);
         return json::Value();
      }
      json::Value jsonCapabilities;
      error = jsonCapabilities.parse(result.stdOut);
      if (error)
      {
         LOG_ERROR(error);
         return json::Value();
      }
      if (!jsonCapabilities.isObject())
      {
         LOG_ERROR_MESSAGE("Unexpected quarto capabilities json: " + result.stdErr);
         return json::Value();
      }
      return jsonCapabilities;
   }
   else
   {
      return json::Value();
   }
}


// Given a path to a Quarto file (usually .qmd), attempt to inspect it
Error quartoInspect(const std::string& path,
                    json::Object *pResultObject)
{
   // Run quarto and retrieve metadata
   std::string output;
   core::system::ProcessResult result;
   Error error = runQuarto({"inspect", path}, FilePath(), &result);
   if (error)
   {
      return error;
   }

   // Parse JSON result
   return pResultObject->parse(result.stdOut);
}



bool handleQuartoPreview(const core::FilePath& sourceFile,
                         const core::FilePath& outputFile,
                         const std::string& renderOutput,
                         bool validateExtendedType)
{
   // don't do anyting if user prefs are set to no preview
   if (prefs::userPrefs().rmdViewerType() == kRmdViewerTypeNone)
      return false;

   // don't do anything if there is no quarto
   if (!quartoIsInstalled())
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
   if ((config.project_type == kQuartoProjectWebsite ||
        config.project_type == kQuartoProjectBook) &&
       sourceFile.isWithin(module_context::resolveAliasedPath(config.project_dir)))
   {
      if (outputFile.hasExtensionLowerCase(".html") || outputFile.hasExtensionLowerCase(".pdf"))
      {
         // preview the doc (but schedule it for later so we can get out of the onCompleted
         // handler this was called from -- launching a new process in the supervisor when
         // an old one is in the middle of executing onCompleted doesn't work
         module_context::scheduleDelayedWork(boost::posix_time::milliseconds(10),
                                             boost::bind(modules::quarto::serve::previewDocPath,
                                                         renderOutput, outputFile),
                                             false);
         return true;
      }
      else
      {
         return false;
      }
   }

   // if this file is within another quarto site or book project then no preview at all
   // (as it will more than likely be broken)
   FilePath configFile = quartoProjectConfigFile(sourceFile);
   if (!configFile.isEmpty())
   {
      std::string type;
      readQuartoProjectConfig(configFile, &type);
      if (type == kQuartoProjectWebsite || type == kQuartoProjectBook)
         return true;
   }

   // continue with preview
   return false;
}

const char* const kQuartoCrossrefScope = "quarto-crossref";
const char* const kQuartoProjectDefault = "default";
const char* const kQuartoProjectWebsite = "website";
const char* const kQuartoProjectSite = "site"; // 'website' used to be 'site'
const char* const kQuartoProjectBook = "book";


QuartoConfig quartoConfig(bool refresh)
{
   static QuartoConfig s_quartoConfig;

   if (refresh)
   {
      // detect installation
      detectQuartoInstallation();
      s_quartoConfig = QuartoConfig();
      s_quartoConfig.userInstalled = s_userInstalledPath;
      s_quartoConfig.enabled = quartoIsInstalled();
      s_quartoConfig.version = s_quartoVersion;

      // if it's installed then detect bin and resources directories
      if (s_quartoConfig.enabled)
      {
         core::system::ProcessResult result;
         Error error = quartoExec({ "--paths" }, &result);
         if (error)
         {
            LOG_ERROR(error);
            s_quartoConfig = QuartoConfig();
            return s_quartoConfig;
         }
         string_utils::convertLineEndings(&result.stdOut, string_utils::LineEndingPosix);
         std::vector<std::string> paths;
         boost::algorithm::split(paths, result.stdOut, boost::algorithm::is_any_of("\n"));
         if (paths.size() >= 2)
         {
            s_quartoConfig.bin_path = string_utils::systemToUtf8(paths[0]);
            s_quartoConfig.resources_path = string_utils::systemToUtf8(paths[1]);
         }
         else
         {
            LOG_ERROR_MESSAGE("Unexpected output from quarto --paths: " + result.stdOut);
            s_quartoConfig = QuartoConfig();
            return s_quartoConfig;
         }
      }

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
                                    &s_quartoConfig.project_formats,
                                    &s_quartoConfig.project_bibliographies,
                                    &s_quartoConfig.project_editor);

            // provide default output dirs
            if (s_quartoConfig.project_output_dir.length() == 0)
            {
               if (s_quartoConfig.project_type == kQuartoProjectWebsite)
                  s_quartoConfig.project_output_dir = "_site";
               else if (s_quartoConfig.project_type == kQuartoProjectBook)
                  s_quartoConfig.project_output_dir = "_book";
            }
         }
      }
   }
   return s_quartoConfig;
}

bool isFileInSessionQuartoProject(const core::FilePath& file)
{
   QuartoConfig config = quartoConfig();
   if (config.is_project)
   {
      FilePath projDir = module_context::resolveAliasedPath(config.project_dir);
      return file.isWithin(projDir);
   }
   else
   {
      return false;
   }

}

std::string urlPathForQuartoProjectOutputFile(const core::FilePath& outputFile)
{
   if (!outputFile.isEmpty())
   {
      FilePath quartoProjectDir = module_context::resolveAliasedPath(
         quartoConfig().project_dir
      );

      FilePath quartoProjectOutputDir = quartoProjectDir.completeChildPath(
         quartoConfig().project_output_dir
      );
      std::string path = outputFile.isWithin(quartoProjectOutputDir)
                            ? outputFile.getRelativePath(quartoProjectOutputDir)
                            :  std::string();
      return path;
   }
   else
   {
      return "";
   }
}

json::Object quartoConfigJSON(bool refresh)
{
   QuartoConfig config = quartoConfig(refresh);
   json::Object quartoConfigJSON;
   if (!config.userInstalled.isEmpty())
      quartoConfigJSON["user_installed"] = module_context::createAliasedPath(config.userInstalled);
   else
      quartoConfigJSON["user_installed"] = "";
   quartoConfigJSON["enabled"] = config.enabled;
   quartoConfigJSON["version"] = config.version;
   quartoConfigJSON["is_project"] = config.is_project;
   quartoConfigJSON["project_dir"] = config.project_dir;
   quartoConfigJSON["project_type"] = config.project_type;
   quartoConfigJSON["project_output_dir"] = config.project_output_dir;
   quartoConfigJSON["project_formats"] = json::toJsonArray(config.project_formats);
   quartoConfigJSON["project_editor"] = config.project_editor;
   return quartoConfigJSON;
}

FilePath quartoBinary()
{
    return s_quartoPath;
}

bool projectIsQuarto()
{
   using namespace session::projects;
   const ProjectContext& context = projectContext();
   if (context.hasProject())
   {
      return quartoConfig().is_project;
   } else {
      return false;
   }
}


FilePath quartoProjectConfigFile(const core::FilePath& filePath)
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

void readQuartoProjectConfig(const FilePath& configFile,
                             std::string* pType,
                             std::string* pOutputDir,
                             std::vector<std::string>* pFormats,
                             std::vector<std::string>* pBibliographies,
                             std::string* pEditor)
{
   // read the config
   std::string configText;
   Error error = core::readStringFromFile(configFile, &configText);
   if (!error)
   {
      try
      {
         YAML::Node node = YAML::Load(configText);
         if (!node.IsMap())
         {
            LOG_ERROR_MESSAGE("Unexpected type for config file yaml (expected a map)");
            return;
         }
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
                     {
                        // migrate 'site' to 'website'
                        if (projValue == kQuartoProjectSite)
                           projValue = kQuartoProjectWebsite;
                        *pType = projValue;
                     }
                     else if (projKey == "output-dir" && pOutputDir != nullptr)
                        *pOutputDir = projValue;
                  }
               }
            }
            else if (key == "format" && pFormats != nullptr)
            {
               auto node = it->second;
               if (node.Type() == YAML::NodeType::Scalar)
               {
                  pFormats->push_back(node.as<std::string>());
               }
               else if (node.Type() == YAML::NodeType::Map)
               {
                  for (auto formatIt = node.begin(); formatIt != node.end(); ++formatIt)
                  {
                     pFormats->push_back(formatIt->first.as<std::string>());
                  }
               }
            }
            else if (key == "bibliography" && pBibliographies != nullptr)
            {
               auto node = it->second;
               if (node.Type() == YAML::NodeType::Scalar)
               {
                  pBibliographies->push_back(node.as<std::string>());
               }
               else if (node.Type() == YAML::NodeType::Sequence)
               {
                  for (auto formatIt = node.begin(); formatIt != node.end(); ++formatIt)
                  {
                     pBibliographies->push_back(formatIt->as<std::string>());
                  }
               }
            }
            else if (key == "editor" && pEditor != nullptr)
            {
               auto node = it->second;
               if (node.Type() == YAML::NodeType::Scalar)
               {
                  *pEditor = node.as<std::string>();
               }
               else if (node.Type() == YAML::NodeType::Map)
               {
                  for (auto editorId = node.begin(); editorId != node.end(); ++editorId)
                  {
                     if ((editorId->first.as<std::string>() == "type" || editorId->first.as<std::string>() == "mode") &&
                         editorId->second.Type() == YAML::NodeType::Scalar)
                     {
                        std::string value = editorId->second.as<std::string>();
                        if (value == "visual" || value == "source")
                           *pEditor = value;
                     }
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



} // namesace quarto

namespace module_context  {


bool navigateToRenderPreviewError(const FilePath& previewFile,
                                  const std::vector<std::string>& previewFileLines,
                                  const std::string& output,
                                  const std::string& allOutput)
{
   // look for an error and do source navigation as necessary
   int errLine = -1;
   FilePath errFile = previewFile;

   // look for knitr error
   const boost::regex knitrErr("Quitting from lines (\\d+)-(\\d+) \\(([^)]+)\\)");
   boost::smatch matches;
   if (regex_utils::search(output, matches, knitrErr))
   {
      errLine = safe_convert::stringTo<int>(matches[1].str(), 1);
      errFile = previewFile.getParent().completePath(matches[3].str());
   }

   // look for jupyter error
   if (errLine == -1)
      errLine = jupyterErrorLineNumber(previewFileLines, allOutput);

   // if there was an error then navigate to it
   if (errLine != -1)
   {
      module_context::editFile(errFile, errLine);
      return true;
   }
   else
   {
      return false;
   }

}

int jupyterErrorLineNumber(const std::vector<std::string>& srcLines, const std::string& output)
{
   // strip ansi codes before searching
   std::string plainOutput = output;
   text::stripAnsiCodes(&plainOutput);

   static boost::regex jupypterErrorRe("An error occurred while executing the following cell:\\s+(-{3,})\\s+([\\S\\s]+?)\\r?\\n(\\1)[\\S\\s]+line (\\d+)\\)");
   boost::smatch matches;
   if (regex_utils::search(plainOutput, matches, jupypterErrorRe))
   {
      // extract the cell lines
      std::string cellText = matches[2].str();
      string_utils::convertLineEndings(&cellText, string_utils::LineEndingPosix);
      std::vector<std::string> cellLines = algorithm::split(cellText, "\n");

      // strip out leading yaml (reading/writing of yaml can lead to src differences)
      int yamlLines = 0;
      for (auto line : cellLines)
      {
         if (boost::algorithm::starts_with(line, "#| "))
            yamlLines++;
         else
            break;
      }
      cellLines = std::vector<std::string>(cellLines.begin() + yamlLines, cellLines.end());

      // find the line number of the cell
      auto it = std::search(srcLines.begin(), srcLines.end(), cellLines.begin(), cellLines.end());
      if (it != srcLines.end())
      {
         int cellLine = static_cast<int>(std::distance(srcLines.begin(), it));
         return cellLine + safe_convert::stringTo<int>(matches[4].str(), 0) - yamlLines;
      }
   }

   // no error
   return -1;
}

} // namespace module_context


namespace modules {
namespace quarto {

Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_quartoFileResources, 1);

   // initialize config at startup
   quartoConfig(true);

   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectQuartoSourceType);

   // additional initialization
   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "quarto_capabilities", quartoCapabilitiesRpc))
     (boost::bind(module_context::registerRpcMethod, "get_qmd_publish_details", getQmdPublishDetails))
     (boost::bind(module_context::registerRpcMethod, "quarto_create_project", quartoCreateProject))
     (boost::bind(module_context::sourceModuleRFile, "SessionQuarto.R"))
     (boost::bind(quarto::preview::initialize))
     (boost::bind(quarto::serve::initialize))
     (boost::bind(quarto::xrefs::initialize))
     (boost::bind(quarto::resources::initialize))
   ;
   return initBlock.execute();
}

} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio

#ifndef QUARTO_ENABLED
#pragma GCC diagnostic pop
#endif

