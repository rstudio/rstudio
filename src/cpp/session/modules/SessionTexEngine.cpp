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

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

// TODO: refactor

// TODO: why does R texi2dvi set LC_COLLATE=C?

// TODO: investigate other texi2dvi and pdflatex options
//         -- shell-escape
//         -- clean
//         -- alternative output file location

// TODO: emulate texi2dvi on linux to workaround debian tilde
//       escaping bug (http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=534458)

// TODO: verify we got all of the shell/path escaping right

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace engine {

namespace {

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


FilePath texBinaryPath(const std::string& name)
{
   std::string which;
   Error error = r::exec::RFunction("Sys.which", name).call(&which);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   else
   {
      return FilePath(which);
   }
}

// this function attempts to emulate the behavior of tools::texi2dvi
// in appending extra paths to TEXINPUTS, BIBINPUTS, & BSTINPUTS
core::system::Option texEnvVar(const std::string& name,
                               const FilePath& extraPath,
                               bool ensureForwardSlashes)
{
   std::string value = core::system::getenv(name);
   if (value.empty())
      value = ".";

   // TODO: R texi2dvi does this for TEXINPUTS but not for BIBINPUTS and
   // BSTINPUTS, why?
   if (ensureForwardSlashes)
      boost::algorithm::replace_all(value, "\\", "/");

   core::system::addToPath(&value, extraPath.absolutePath());
   core::system::addToPath(&value, ""); // trailing : required by tex

   return std::make_pair(name, value);
}

core::system::Option pdfLatexEnvVar()
{
#ifdef _WIN32
   const char* const kScriptEx = ".cmd";
#else
   const char* const kScriptEx = ".sh";
#endif

   FilePath texScriptsPath = session::options().texScriptsPath();
   FilePath pdfLatexPath = texScriptsPath.complete("rstudio-pdflatex" +
                                                   std::string(kScriptEx));
   std::string path = string_utils::utf8ToSystem(pdfLatexPath.absolutePath());
   return std::make_pair("PDFLATEX", path);
}

core::system::Options texEnvironmentVars(const std::string&)
{
   // custom environment for tex
   core::system::Options envVars;

   // these behaviors are from R texi2dvi -- not sure why the are important
#ifndef _WIN32
   envVars.push_back(std::make_pair("TEXINDY", "false"));
   envVars.push_back(std::make_pair("LC_COLLATE", "C"));
#endif

   RTexmfPaths texmfPaths = rTexmfPaths();
   if (!texmfPaths.empty())
   {
      envVars.push_back(texEnvVar("TEXINPUTS", texmfPaths.texInputsPath, true));
      envVars.push_back(texEnvVar("BIBINPUTS", texmfPaths.bibInputsPath,false));
      envVars.push_back(texEnvVar("BSTINPUTS", texmfPaths.bstInputsPath,false));
   }

   // define a custom variation of PDFLATEX that includes the
   // command line parameters we need
   envVars.push_back(pdfLatexEnvVar());

   return envVars;
}

shell_utils::ShellArgs texShellArgs(const std::string& texVersionInfo)
{
   shell_utils::ShellArgs args;

   args << "--pdf";
   args << "--quiet";

#ifdef _WIN32
   if (texVersionInfo.find("MiKTeX") != std::string::npos)
   {
      // TODO: R texi2dvi doesn't include BIBINPUTS here, why?
      RTexmfPaths texmfPaths = rTexmfPaths();
      if (!texmfPaths.empty())
      {
         std::string texInputs = texmfPaths.texInputsPath.absolutePath();
         boost::algorithm::replace_all(texInputs, "\\", "/");
         args << "-I" << texInputs;

         std::string bstInputs = texmfPaths.bstInputsPath.absolutePath();
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
                                             texProgramPath.absolutePath(),
                                             procArgs,
                                             procOptions,
                                             cb);
}


SEXP rs_texToPdf(SEXP filePathSEXP)
{
   FilePath texFilePath =
         module_context::resolveAliasedPath(r::sexp::asString(filePathSEXP));


   // get the path to the texi2dvi binary
   FilePath texi2dviPath = texBinaryPath("texi2dvi");
   if (texi2dviPath.empty())
   {
      module_context::consoleWriteError("can't find texi2dvi\n");
      return R_NilValue;
   }

   // get version info from it
   core::system::ProcessResult result;
   Error error = core::system::runProgram(
                            texi2dviPath.absolutePath(),
                            core::shell_utils::ShellArgs() << "--version",
                            "",
                            core::system::ProcessOptions(),
                            &result);
   if (error)
   {
      module_context::consoleWriteError(error.summary() + "\n");
      return R_NilValue;
   }
   else if (result.exitStatus != EXIT_SUCCESS)
   {
      module_context::consoleWriteError(result.stdErr);
      return R_NilValue;
   }

   error = executeTexToPdf(texi2dviPath,
                           texEnvironmentVars(result.stdOut),
                           texShellArgs(result.stdOut),
                           texFilePath);
   if (error)
      module_context::consoleWriteError(error.summary() + "\n");

   return R_NilValue;
}


} // anonymous namespace


Error initialize()
{
   R_CallMethodDef runTexToPdfMethodDef;
   runTexToPdfMethodDef.name = "rs_texToPdf" ;
   runTexToPdfMethodDef.fun = (DL_FUNC) rs_texToPdf ;
   runTexToPdfMethodDef.numArgs = 1;
   r::routines::addCallMethod(runTexToPdfMethodDef);



   return Success();
}


} // namespace engine
} // namespace tex
} // namespace modules
} // namesapce session

