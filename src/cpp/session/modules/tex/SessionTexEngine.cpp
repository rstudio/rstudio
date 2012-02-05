/*
 * SessionTexEngine.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionTexEngine.hpp"

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>

#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

// TODO: investigate other texi2dvi and pdflatex options
//         -- shell-escape
//         -- clean
//         -- alternative output file location

// TODO: emulate texi2dvi on linux to workaround debian tilde
//       escaping bug (http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=534458)


// platform specific constants
#ifdef _WIN32
const char * const kScriptEx = ".cmd";
const char * const kFileLineErrors = "-c-style-errors";
#else
const char * const kScriptEx = ".sh";
const char * const kFileLineErrors = "-file-line-error";
#endif

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace engine {

namespace {


struct PdfLatexOptions
{
   PdfLatexOptions()
      : fileLineError(false), syncTex(false)
   {
   }

   bool fileLineError;
   bool syncTex;

};


// set of environment variables to customize pdflatex invocation
// includes both the core PDFLATEX command (which maps to the location
// of the custom rstudio-pdflatex script) as well as environment
// variables required to pass options to the script
core::system::Options pdfLatexEnvVars(const PdfLatexOptions& options)
{
   core::system::Options envVars;

   // options
   boost::format fmt("RS_PDFLATEX_OPTION_%1%");
   int n = 1;
   if (options.fileLineError)
   {
      envVars.push_back(std::make_pair(boost::str(fmt % n++),
                                       kFileLineErrors));
   }
   if (options.syncTex)
   {
      envVars.push_back(std::make_pair(boost::str(fmt % n++),
                                       "-synctex=-1"));
   }

   // rstudio-pdflatex script
   FilePath texScriptsPath = session::options().texScriptsPath();
   FilePath pdfLatexPath = texScriptsPath.complete("rstudio-pdflatex" +
                                                   std::string(kScriptEx));
   std::string path = string_utils::utf8ToSystem(pdfLatexPath.absolutePath());
   envVars.push_back(std::make_pair("PDFLATEX", path));

   // return envVars
   return envVars;
}


struct RTexmfPaths
{
   bool empty() const { return texInputsPath.empty(); }

   FilePath texInputsPath;
   FilePath bibInputsPath;
   FilePath bstInputsPath;
};

RTexmfPaths rTexmfPaths()
{
   // first determine the R share directory
   std::string rHomeShare;
   r::exec::RFunction rHomeShareFunc("R.home", "share");
   Error error = rHomeShareFunc.call(&rHomeShare);
   if (error)
   {
      LOG_ERROR(error);
      return RTexmfPaths();
   }
   FilePath rHomeSharePath(rHomeShare);
   if (!rHomeSharePath.exists())
   {
      LOG_ERROR(core::pathNotFoundError(rHomeShare, ERROR_LOCATION));
      return RTexmfPaths();
   }

   // R texmf path
   FilePath rTexmfPath(rHomeSharePath.complete("texmf"));
   if (!rTexmfPath.exists())
   {
      LOG_ERROR(core::pathNotFoundError(rTexmfPath.absolutePath(),
                                        ERROR_LOCATION));
      return RTexmfPaths();
   }

   // populate and return struct
   RTexmfPaths texmfPaths;
   texmfPaths.texInputsPath = rTexmfPath.childPath("tex/latex");
   texmfPaths.bibInputsPath = rTexmfPath.childPath("bibtex/bib");
   texmfPaths.bstInputsPath = rTexmfPath.childPath("bibtex/bst");
   return texmfPaths;
}


// this function attempts to emulate the behavior of tools::texi2dvi
// in appending extra paths to TEXINPUTS, BIBINPUTS, & BSTINPUTS
core::system::Option inputsEnvVar(const std::string& name,
                                  const FilePath& extraPath,
                                  bool ensureForwardSlashes)
{
   std::string value = core::system::getenv(name);
   if (value.empty())
      value = ".";

   // on windows tools::texi2dvi replaces \ with / when defining the TEXINPUTS
   // environment variable (but for BIBINPUTS and BSTINPUTS)
#ifdef _WIN32
   if (ensureForwardSlashes)
      boost::algorithm::replace_all(value, "\\", "/");
#endif

   std::string sysPath = string_utils::utf8ToSystem(extraPath.absolutePath());
   core::system::addToPath(&value, sysPath);
   core::system::addToPath(&value, ""); // trailing : required by tex

   return std::make_pair(name, value);
}


// build TEXINPUTS, BIBINPUTS etc. by composing any existing value in
// the environment (or . if none) with the R dirs in share/texmf
core::system::Options inputsEnvironmentVars()
{
   core::system::Options envVars;
   RTexmfPaths texmfPaths = rTexmfPaths();
   if (!texmfPaths.empty())
   {
      envVars.push_back(inputsEnvVar("TEXINPUTS",
                                     texmfPaths.texInputsPath,
                                     true));
      envVars.push_back(inputsEnvVar("BIBINPUTS",
                                     texmfPaths.bibInputsPath,
                                     false));
      envVars.push_back(inputsEnvVar("BSTINPUTS",
                                     texmfPaths.bstInputsPath,
                                     false));
   }
   return envVars;
}

core::system::Options texi2dviEnvironmentVars(const std::string&)
{
   // start with inputs (TEXINPUTS, BIBINPUTS, BSTINPUTS)
   core::system::Options envVars = inputsEnvironmentVars();

   // The tools::texi2dvi function sets these environment variables (on posix)
   // so they are presumably there as workarounds-- it would be good to
   // understand exactly why they are defined and consequently whether we also
   // need to define them
#ifndef _WIN32
   envVars.push_back(std::make_pair("TEXINDY", "false"));
   envVars.push_back(std::make_pair("LC_COLLATE", "C"));
#endif

   // env vars required to customize invocation of pdflatex
   PdfLatexOptions pdfLatexOptions;
   pdfLatexOptions.fileLineError = true;
   pdfLatexOptions.syncTex = true;
   core::system::Options pdfLatexVars = pdfLatexEnvVars(pdfLatexOptions);
   std::copy(pdfLatexVars.begin(),
             pdfLatexVars.end(),
             std::back_inserter(envVars));

   return envVars;
}

shell_utils::ShellArgs texi2dviShellArgs(const std::string& texVersionInfo)
{
   shell_utils::ShellArgs args;

   args << "--pdf";
   args << "--quiet";

#ifdef _WIN32
   // This emulates two behaviors found in tools::texi2dvi:
   //
   //   (1) Detecting MikTeX and in that case passing TEXINPUTS and
   //       BSTINPUTS (but not BIBINPUTS) on the texi2devi command line
   //
   //   (2) Substituting any instances of \ in the paths with /
   //
   if (texVersionInfo.find("MiKTeX") != std::string::npos)
   {
      RTexmfPaths texmfPaths = rTexmfPaths();
      if (!texmfPaths.empty())
      {
         std::string texInputs = string_utils::utf8ToSystem(
                                    texmfPaths.texInputsPath.absolutePath());
         boost::algorithm::replace_all(texInputs, "\\", "/");
         args << "-I" << texInputs;

         std::string bstInputs = string_utils::utf8ToSystem(
                                    texmfPaths.bstInputsPath.absolutePath());
         boost::algorithm::replace_all(bstInputs, "\\", "/");
         args << "-I" << bstInputs;
      }
   }
#endif

   return args;
}

Error executeTexToPdf(const FilePath& texProgramPath,
                      const core::system::Options& envVars,
                      const shell_utils::ShellArgs& args,
                      const FilePath& texFilePath)
{
   // copy extra environment variables
   core::system::Options env;
   core::system::environment(&env);
   BOOST_FOREACH(const core::system::Option& var, envVars)
   {
      core::system::setenv(&env, var.first, var.second);
   }

   // setup args
   shell_utils::ShellArgs procArgs;
   procArgs << args;
   procArgs << texFilePath.filename();

   // set options
   core::system::ProcessOptions procOptions;
   procOptions.terminateChildren = true;
   procOptions.environment = env;
   procOptions.workingDir = texFilePath.parent();

   // setup callbacks
   core::system::ProcessCallbacks cb;
   cb.onStdout = boost::bind(module_context::consoleWriteOutput, _2);
   cb.onStderr = boost::bind(module_context::consoleWriteError, _2);

   // run the program
   using namespace core::shell_utils;
   return module_context::processSupervisor().runProgram(
                    string_utils::utf8ToSystem(texProgramPath.absolutePath()),
                    procArgs,
                    procOptions,
                    cb);
}


struct Texi2DviInfo
{
   bool empty() const { return programFilePath.empty(); }

   FilePath programFilePath;
   const std::string versionInfo;
};

Texi2DviInfo texi2DviInfo()
{
   // get the path to the texi2dvi binary
   FilePath programFilePath = module_context::findProgram("texi2dvi");
   if (programFilePath.empty())
      return Texi2DviInfo();

   // this is enough to return so setup the return structure
   Texi2DviInfo t2dviInfo;
   t2dviInfo.programFilePath = programFilePath;

   // try to get version info from it
   core::system::ProcessResult result;
   Error error = core::system::runProgram(
                  string_utils::utf8ToSystem(programFilePath.absolutePath()),
                  core::shell_utils::ShellArgs() << "--version",
                  "",
                  core::system::ProcessOptions(),
                  &result);
   if (error)
      LOG_ERROR(error);
   else if (result.exitStatus != EXIT_SUCCESS)
      LOG_ERROR_MESSAGE("Error probing for texi2dvi version: "+ result.stdErr);

   // return what we have
   return t2dviInfo;
}



SEXP rs_texToPdf(SEXP filePathSEXP)
{
   FilePath texFilePath =
         module_context::resolveAliasedPath(r::sexp::asString(filePathSEXP));

   Texi2DviInfo t2dviInfo = texi2DviInfo();
   if (t2dviInfo.empty())
   {
      r::exec::warning("Unable to find texi2dvi executable");
      return R_NilValue;
   }

   Error error = executeTexToPdf(t2dviInfo.programFilePath,
                                 texi2dviEnvironmentVars(t2dviInfo.versionInfo),
                                 texi2dviShellArgs(t2dviInfo.versionInfo),
                                 texFilePath);
   if (error)
      module_context::consoleWriteError(error.summary() + "\n");

   return R_NilValue;
}


Error isTexInstalled(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(engine::isInstalled());
   return Success();
}

} // anonymous namespace


bool isInstalled()
{
   return !module_context::findProgram("pdflatex").empty();
}

Error initialize()
{
   R_CallMethodDef runTexToPdfMethodDef;
   runTexToPdfMethodDef.name = "rs_texToPdf" ;
   runTexToPdfMethodDef.fun = (DL_FUNC) rs_texToPdf ;
   runTexToPdfMethodDef.numArgs = 1;
   r::routines::addCallMethod(runTexToPdfMethodDef);

   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (boost::bind(registerRpcMethod, "is_tex_installed", isTexInstalled));
   return initBlock.execute();


   return Success();
}


} // namespace engine
} // namespace tex
} // namespace modules
} // namesapce session

