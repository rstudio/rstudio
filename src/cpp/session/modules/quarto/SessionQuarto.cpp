/*
 * SessionQuarto.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

#include <core/Exec.hpp>
#include <core/Version.hpp>
#include <core/YamlUtil.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Process.hpp>
#include <core/text/AnsiCodeParser.hpp>

#include <r/RExec.hpp>
#include <r/RUtil.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/SessionQuarto.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "../rmarkdown/SessionRMarkdown.hpp"
#include "SessionQuartoPreview.hpp"
#include "SessionQuartoXRefs.hpp"
#include "SessionQuartoResources.hpp"

using namespace rstudio::core;

const char * const kRStudioQuarto = "RSTUDIO_QUARTO";

#ifndef _WIN32
# define kQuartoCmd "quarto"
# define kQuartoExe "quarto"
# define kPandocExe "pandoc"
#else
# define kQuartoCmd "quarto.cmd"
# define kQuartoExe "quarto.exe"
# define kPandocExe "pandoc.exe"
#endif


#ifdef __aarch64__
# define kArchDir "aarch64"
#else
# define kArchDir "x86_64"
#endif

namespace rstudio {
namespace session {

using namespace quarto;

namespace {

const char * const kQuartoXt = "quarto-document";

FilePath s_userInstalledPath;
FilePath s_quartoPath;
std::string s_quartoVersion;
QuartoConfig s_quartoConfig;

FilePath quartoPandocPath()
{
   FilePath quartoPandoc;
   
   // find quarto pandoc -- its location has moved over time,
   // so we check a variety of locations just in case
   FilePath quartoBinPath(s_quartoConfig.bin_path);
   quartoPandoc = quartoBinPath.completeChildPath("tools/" kArchDir "/" kPandocExe);
   if (quartoPandoc.exists())
      return quartoPandoc;
   
   quartoPandoc = quartoBinPath.completeChildPath("tools/" kPandocExe);
   if (quartoPandoc.exists())
      return quartoPandoc;
   
   quartoPandoc = quartoBinPath.completeChildPath(kPandocExe);
   if (quartoPandoc.exists())
      return quartoPandoc;
   
   // all else fails, just try to find pandoc on the PATH
   Error error = core::system::findProgramOnPath(kPandocExe, &quartoPandoc);
   if (error)
      LOG_ERROR(error);
   
   return quartoPandoc;
}

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

void showQuartoWarning()
{
   // enque a warning
   const char * const kUpdateURL = "https://quarto.org/docs/getting-started/installation.html";
   json::Object msgJson;
   msgJson["severe"] = false;
   boost::format fmt(
     "Quarto is not installed. "
     "Please install the latest version at <a href=\"%1%\" target=\"_blank\">%1%</a>"
   );
   msgJson["message"] = boost::str(fmt % kUpdateURL);
   ClientEvent event(client_events::kShowWarningBar, msgJson);
   module_context::enqueClientEvent(event);
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


std::tuple<FilePath,Version,bool> userInstalledQuarto()
{
   bool env = false;
   FilePath quartoPath;

   // first check RSTUDIO_QUARTO environment variable
   std::string rstudioQuarto = core::system::getenv(kRStudioQuarto);
   if (!rstudioQuarto.empty())
   {
#ifdef _WIN32
      if (!boost::algorithm::ends_with(rstudioQuarto, ".cmd"))
         rstudioQuarto = rstudioQuarto + ".cmd";
#endif
      if (FilePath::exists(rstudioQuarto))
      {
         env = true;
         quartoPath = FilePath(rstudioQuarto);
      }
   }
   
   // next, look for quarto on the PATH
   if (quartoPath.isEmpty())
   {
      quartoPath = module_context::findProgram("quarto");
   }
   
   // next, look for quarto from qvm
   if (quartoPath.isEmpty())
   {
      FilePath qvmPath = module_context::findProgram("qvm");
      if (qvmPath.exists())
      {
         core::system::ProcessResult result;
         Error error = core::system::runProgram(
                  qvmPath.getAbsolutePath(),
                  { "path", "active" },
                  core::system::ProcessOptions(),
                  &result);
         if (error)
            LOG_ERROR(error);
         else if (result.exitStatus == EXIT_SUCCESS)
         {
            FilePath quartoFolder = FilePath(core::string_utils::trimWhitespace(result.stdOut));
            if (quartoFolder.exists())
            {
               FilePath qvmLink = quartoFolder.completeChildPath(kQuartoCmd);
               if (qvmLink.exists())
                  quartoPath = FilePath(qvmLink.getCanonicalPath());
            }
         }
      }
   }

   // if we found a quarto path, try to use it
   if (!quartoPath.isEmpty())
   {
      Error error = core::system::realPath(quartoPath, &quartoPath);
      if (!error)
      {
         Version pathVersion = readQuartoVersion(quartoPath);
         if (!pathVersion.empty())
         {
            return std::make_tuple(quartoPath, pathVersion, env);
         }
      }
      else
      {
         LOG_ERROR(error);
      }
   }
   
   return std::make_tuple(FilePath(), Version(), false);
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


void detectQuartoInstallation()
{
   // required quarto version (quarto features don't work w/o it)
   const Version kQuartoRequiredVersion("1.1.251");

   // recommended quarto version (a bit more pestery than required)
   const Version kQuartoRecommendedVersion("1.1.251");

   // reset
   s_userInstalledPath = FilePath();
   s_quartoPath = FilePath();
   s_quartoVersion = "";

   // detect user installed version
   auto userInstalled = userInstalledQuarto();
   s_userInstalledPath = std::get<0>(userInstalled);
   s_quartoVersion = std::get<1>(userInstalled);
   bool prepend = std::get<2>(userInstalled);

   std::string sysPath = core::system::getenv("PATH");

   // always use user installed if it's there but subject to version check
   if (!s_userInstalledPath.isEmpty())
   {
      if (s_quartoVersion >= kQuartoRecommendedVersion)
      {
         s_quartoPath = s_userInstalledPath;
         const std::string quartoPath = string_utils::utf8ToSystem(
                  s_quartoPath.getParent().getAbsolutePath());

         if (sysPath.find(quartoPath) != std::string::npos)
            return;

         // prepend to path only if RSTUDIO_QUARTO is defined
         r::util::addToSystemPath(s_quartoPath.getParent(), prepend);
         return;
      }
   }

   // embedded version of quarto (subject to required version)
   FilePath embeddedQuartoPath = session::options().quartoPath()
      .completeChildPath("bin")
      .completeChildPath(kQuartoExe);

   if (embeddedQuartoPath.exists())
   {
      auto embeddedVersion = readQuartoVersion(embeddedQuartoPath);
      if (embeddedVersion < kQuartoRequiredVersion)
      {
         showQuartoVersionWarning(embeddedVersion, kQuartoRequiredVersion);
         return;
      }
      
      s_quartoPath = embeddedQuartoPath;
      s_quartoVersion = embeddedVersion;
      const std::string quartoPath = string_utils::utf8ToSystem(
          s_quartoPath.getParent().getAbsolutePath());
      
      if (sysPath.find(quartoPath) != std::string::npos)
         return;
      
      // append to path
      r::util::addToSystemPath(s_quartoPath.getParent(), prepend);
      return;
   }
   else
   {
      showQuartoWarning();
   }
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
         // (exclude all documents with an 'output' yaml metadata key)
         std::string yamlHeader = yaml::extractYamlHeader(pDoc->contents());
         static const boost::regex reOutput("(^|\\n)output:\\s*");
         static const boost::regex reFormat("(^|\\n)format:\\s*");
         static const boost::regex reJupyter("(^|\\n)jupyter:\\s*");
         static const boost::regex reKnitQuarto("(^|\\n)knit:\\s*quarto\\s+render");
         if (!regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reOutput)) 
         {
             // format key
            if (regex_utils::search(yamlHeader.begin(), yamlHeader.end(), reFormat))
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
      string_utils::utf8ToSystem(quartoExecutablePath()),
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
   // website or manuscript.
   std::string websiteDir, websiteOutputDir, projectType;
   FilePath quartoConfig = quartoProjectConfigFile(qmdPath);

   if (!quartoConfig.isEmpty())
   {
      std::string outputDir;
      readQuartoProjectConfig(quartoConfig, &projectType, &outputDir);
      if (projectType == kQuartoProjectBook || projectType == kQuartoProjectWebsite || projectType == kQuartoProjectManuscript)
      {
         FilePath configPath = quartoConfig.getParent();
         websiteDir = configPath.getAbsolutePath();
         // Infer output directory
         if (outputDir.empty())
         {
            if (projectType == kQuartoProjectBook)
            {
               outputDir = "_book";
            }
            else if (projectType == kQuartoProjectManuscript)
            {
               outputDir = "_manuscript";
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
   result["project_type"] = projectType;
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

SEXP rs_quartoFileProject(SEXP basenameSEXP, SEXP dirnameSEXP)
{
   std::vector<std::string> project;
   std::vector<std::string> resources;

   std::string basename = r::sexp::safeAsString(basenameSEXP);
   std::string dirname = r::sexp::safeAsString(dirnameSEXP);

   json::Object jsonInspect;
   std::string output;
   core::system::ProcessResult result;
   Error error = runQuarto({"inspect", basename}, FilePath(dirname), &result);
   if (!error)
   {
      error = jsonInspect.parse(result.stdOut);
      if (!error)
      {
         json::Value proj = jsonInspect["project"];
         if (proj.isString())
         {
            project.push_back(proj.getString());
         }
         // Schema changed in Quarto v1.2
         else if (proj.isObject())
         {
            json::Value dir = proj.getObject()["dir"];
            if (dir.isString())
            {
               FilePath inspectedFileDir(dirname);
               FilePath projectDir(dir.getString());
               std::string projectDirRelative = projectDir.getRelativePath(inspectedFileDir);
               if (projectDirRelative == ".")
                  projectDirRelative = "";
               project.push_back(projectDirRelative);
            }
         }

         jsonInspect["resources"].getArray().toVectorString(resources);   
      }
   }
   
   r::sexp::Protect protect;
   SEXP out = r::sexp::createList({"project", "resources"}, &protect);
   SET_VECTOR_ELT(out, 0, r::sexp::create(project, &protect));
   SET_VECTOR_ELT(out, 1, r::sexp::create(resources, &protect));
   return out;
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

   // create the project file
   using namespace projects;
   error = r_util::writeProjectFile(
            projectFilePath,
            ProjectContext::buildDefaults(),
            ProjectContext::defaultConfig());
   if (error)
      LOG_ERROR(error);

   // add some first run files
   using namespace module_context;
   std::vector<std::string> projFiles;
   if (boost::algorithm::starts_with(type, kQuartoProjectWebsite) ||
       boost::algorithm::starts_with(type, kQuartoProjectBook) ||
       boost::algorithm::starts_with(type, kQuartoProjectManuscript))
   {
      projFiles.push_back("index.qmd");
      projFiles.push_back("_quarto.yml");
   }
   else
   {
      projFiles.push_back(projDir.getFilename() + ".qmd");
   }
   projects::addFirstRunDocs(projectFilePath, projFiles);

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
     string_utils::utf8ToSystem(quartoExecutablePath()),
      args,
      options,
      pCPI);

   pResponse->setResult(pCP->toJson(console_process::ClientSerialization));
   return Success();
}

void readQuartoConfig()
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
         return;
      }
      string_utils::convertLineEndings(&result.stdOut, string_utils::LineEndingPosix);
      std::vector<std::string> paths;
      boost::algorithm::split(paths, result.stdOut, boost::algorithm::is_any_of("\n"));
      if (paths.size() >= 2)
      {
         s_quartoConfig.bin_path = string_utils::systemToUtf8(paths[0]);
         s_quartoConfig.resources_path = string_utils::systemToUtf8(paths[1]);
         s_quartoConfig.pandoc_path = quartoPandocPath().getAbsolutePath();
      }
      else
      {
         LOG_ERROR_MESSAGE("Unexpected output from quarto --paths: " + result.stdOut);
         s_quartoConfig = QuartoConfig();
         return;
      }
      
   }

   using namespace session::projects;
   const ProjectContext& context = projectContext();
   if (context.hasProject())
   {
      // look for a config file in the project directory
      FilePath configFile = quartoConfigFilePath(context.directory());

      // if we don't find one, then chase up the directory hierarchy until we find one
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
                                 &s_quartoConfig.project_execute_dir,
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
             else if (s_quartoConfig.project_type == kQuartoProjectManuscript)
               s_quartoConfig.project_output_dir = "_manuscript";
         }
      }
   }
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

const char* const kQuartoCrossrefScope = "quarto-crossref";
const char* const kQuartoProjectDefault = "default";
const char* const kQuartoProjectWebsite = "website";
const char* const kQuartoProjectSite = "site"; // 'website' used to be 'site'
const char* const kQuartoProjectBook = "book";
const char* const kQuartoProjectManuscript = "manuscript";

// possible values for the execute-dir project option
const char* const kQuartoExecuteDirProject = "project";
const char* const kQuartoExecuteDirFile = "file";

QuartoConfig quartoConfig()
{
   return s_quartoConfig;
}

bool isFileInSessionQuartoProject(const core::FilePath& file)
{
   QuartoConfig config = quartoConfig();
   if (config.is_project)
   {
      FilePath projDir = module_context::resolveAliasedPath(config.project_dir);
      projDir = FilePath(projDir.getCanonicalPath());
      
      FilePath canonicalFile = FilePath(file.getCanonicalPath());
      return canonicalFile.isWithin(projDir);
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
      // get quarto project directory
      FilePath quartoProjectDir = module_context::resolveAliasedPath(quartoConfig().project_dir);
      
      // resolve symlinks in path, since 'FilePath::isWithin()' doesn't do this for us
      FilePath canonicalOutputFile = FilePath(outputFile.getCanonicalPath());
      quartoProjectDir = FilePath(quartoProjectDir.getCanonicalPath());
      
      // check whether the output file lives within the output directory
      FilePath quartoProjectOutputDir = quartoProjectDir.completeChildPath(quartoConfig().project_output_dir);
      return canonicalOutputFile.isWithin(quartoProjectOutputDir)
            ? canonicalOutputFile.getRelativePath(quartoProjectOutputDir)
            : std::string();
   }
   else
   {
      return "";
   }
}

json::Object quartoConfigJSON()
{
   QuartoConfig config = quartoConfig();
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
   quartoConfigJSON["project_execute_dir"] = config.project_execute_dir;
   quartoConfigJSON["project_formats"] = json::toJsonArray(config.project_formats);
   quartoConfigJSON["project_editor"] = config.project_editor;
   return quartoConfigJSON;
}

FilePath quartoBinary()
{
   return s_quartoPath;
}

std::string quartoExecutablePath()
{
   return file_utils::shortPathName(s_quartoPath.getAbsolutePath());
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

// Looks up the document in the source database and returns true IFF the
// document is Quarto markdown. There are a variety of heuristics that
// are employed to ascertain this (see onDetectQuartoSourceType for
// details).
//
// Takes a docId rather than a path so that we can detect Quarto even
// in unsaved buffers.
bool docIsQuarto(const std::string& docId)
{
    boost::shared_ptr<source_database::SourceDocument> pDoc(new source_database::SourceDocument());
    Error error = source_database::get(docId, pDoc);
    if (error)
    {
        // If it doesn't exist in the source database, presume it isn't Quarto.
        LOG_ERROR(error);
        return false;
    }

    // Detect the document's extended type
    std::string xt = onDetectQuartoSourceType(pDoc);
    return xt == kQuartoXt;
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
   for (FilePath targetPath = filePath.isDirectory() ? filePath : filePath.getParent();
        targetPath.exists();
        targetPath = targetPath.getParent())
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
                             std::string* pExecuteDir,
                             std::vector<std::string>* pFormats,
                             std::vector<std::string>* pBibliographies,
                             json::Object* pEditor)
{
   // determine type based on quarto inspect (allows us to treat custom project
   // types as the correct base type). use cache of previously read project types
   static std::map<std::string,std::string> s_projectTypes;
   std::string projType;
   std::map<std::string,std::string>::iterator it = s_projectTypes.find(configFile.getAbsolutePath());
   if (it != s_projectTypes.end()) {
      projType = it->second;
   } else {
      // Ask Quarto to get the metadata for the file
      json::Object inspect;
      Error error = quartoInspect(configFile.getParent().getAbsolutePath(), &inspect);
      if (!error)
      {
         try
         {
            auto project = inspect["config"].getObject()["project"].getObject();
            auto type = project.find("type");
            if (type != project.end())
            {
               projType = (*type).getValue().getString();
               s_projectTypes.insert({configFile.getAbsolutePath(), projType});
            }
         }
         CATCH_UNEXPECTED_EXCEPTION
      }
      else
      {
         LOG_ERROR(error);
      }
   }

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

                        // use type from inspect if we can
                        if (pType != nullptr)
                        {
                           if (!projType.empty())
                              *pType = projType;
                           else
                              *pType = projValue;
                        }
                     }
                     else if (projKey == "output-dir" && pOutputDir != nullptr) {
                        *pOutputDir = projValue;
                     }
                     else if (projKey == "execute-dir" && pExecuteDir != nullptr) {
                        *pExecuteDir = projValue;
                     }
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
               if (node.Type() == YAML::NodeType::Map)
               {
                  r::sexp::Protect protect;
                  auto editorSEXP = r::sexp::create(node, &protect);

                  SEXP editorJsonSEXP;
                  Error error = r::exec::RFunction(".rs.quarto.editorConfig", editorSEXP)
                       .call(&editorJsonSEXP, &protect);
                  if (!error)
                  {
                     error = r::sexp::extract(editorJsonSEXP, pEditor);
                     if (error)
                        LOG_ERROR(error);
                  }
                  else
                  {
                     LOG_ERROR(error);
                  }
               }
               else if (node.Type() == YAML::NodeType::Scalar)
               {
                  json::Object editor;
                  editor["mode"] = node.as<std::string>();
                  *pEditor = editor;
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

// Given the (aliased) path to a file, return the file path to the working directory where code
// should be executed in the file, based on the settings in _quarto.yml.
//
// Returns an empty FilePath if no directory is specified.
FilePath getQuartoExecutionDir(const std::string& docPath)
{
   // Ensure we have a path to work with (an empty string will resolve to the home directory below)
   if (docPath.empty())
   {
      return FilePath();
   }

   // Find the Quarto configuration file associated with this document
   FilePath qmdPath = module_context::resolveAliasedPath(docPath);
   FilePath quartoConfig = quartoProjectConfigFile(qmdPath);
   if (quartoConfig.isEmpty())
   {
      return FilePath();
   }

   // Read the Quarto configuration file
   std::string type, outputDir, executeDir;
   quarto::readQuartoProjectConfig(quartoConfig, &type, &outputDir, &executeDir);

   if (executeDir == quarto::kQuartoExecuteDirProject)
   {
      // If the execution dir is set to 'project', infer the project root from the location
      // of the Quarto config file and use it as the directory for execution
      return quartoConfig.getParent();
   }
   else if (executeDir == quarto::kQuartoExecuteDirFile)
   {
      // If the execution dir is set to 'file', use the directory of the document
      return qmdPath.getParent();
   }

   // In all other cases, treat the execution directory as unspecified
   return FilePath();
}

} // namespace quarto

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
   boost::regex knitrErr(kKnitrErrorRegex);
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
   RS_REGISTER_CALL_METHOD(rs_quartoFileProject, 2);

   // source SessionQuarto.R so we can call it from config init
   Error error = module_context::sourceModuleRFile("SessionQuarto.R");
   if (error)
      return error;

   // initialize config at startup
   readQuartoConfig();

   module_context::events().onDetectSourceExtendedType
                                        .connect(onDetectQuartoSourceType);

   // additional initialization
   ExecBlock initBlock;
   initBlock.addFunctions()
     (boost::bind(module_context::registerRpcMethod, "quarto_capabilities", quartoCapabilitiesRpc))
     (boost::bind(module_context::registerRpcMethod, "get_qmd_publish_details", getQmdPublishDetails))
     (boost::bind(module_context::registerRpcMethod, "quarto_create_project", quartoCreateProject))
     (boost::bind(quarto::preview::initialize))
     (boost::bind(quarto::xrefs::initialize))
     (boost::bind(quarto::resources::initialize))
   ;
   return initBlock.execute();
}

} // namespace quarto
} // namespace modules
} // namespace session
} // namespace rstudio
