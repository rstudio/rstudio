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

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace engine {

namespace {

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

   if (ensureForwardSlashes)
      boost::algorithm::replace_all(value, "\\", "/");

   core::system::addToPath(&value, extraPath.absolutePath());
   core::system::addToPath(&value, ""); // trailing : required by tex

   return std::make_pair(name, value);
}

core::system::Option pdfLatexEnvVar()
{
   std::string pdfLatexCmd =
      string_utils::utf8ToSystem(texBinaryPath("pdflatex").absolutePath());
   pdfLatexCmd += " -file-line-error -synctex=-1";
   return std::make_pair("PDFLATEX", pdfLatexCmd);
}

core::system::Options texEnvironmentVars()
{
   // custom environment for tex
   core::system::Options envVars;

   // TODO: R texi2dvi sets LC_COLLATE=C before calling texi2dvi, why?
   envVars.push_back(std::make_pair("LC_COLLATE", "C"));


   // first determine the R share directory
   std::string rHomeShare;
   r::exec::RFunction rHomeShareFunc("R.home", "share");
   Error error = rHomeShareFunc.call(&rHomeShare);
   if (error)
   {
      LOG_ERROR(error);
      return core::system::Options();
   }
   FilePath rHomeSharePath(rHomeShare);
   if (!rHomeSharePath.exists())
   {
      LOG_ERROR(core::pathNotFoundError(rHomeShare, ERROR_LOCATION));
      return core::system::Options();
   }

   // R texmf path
   FilePath rTexmfPath(rHomeSharePath.complete("texmf"));

   // fixup tex related environment variables to point to the R
   // tex, bib, and bst directories

   envVars.push_back(texEnvVar("TEXINPUTS",
                               rTexmfPath.childPath("tex/latex"),
                               true));

   envVars.push_back(texEnvVar("BIBINPUTS",
                               rTexmfPath.childPath("bibtex/bib"),
                               false));

   envVars.push_back(texEnvVar("BSTINPUTS",
                               rTexmfPath.childPath("bibtex/bst"),
                               false));

   // define a custom variation of PDFLATEX that includes the
   // command line parameters we need
   envVars.push_back(pdfLatexEnvVar());

   return envVars;
}

shell_utils::ShellArgs texShellArgs()
{
   shell_utils::ShellArgs args;

   args << "--pdf";
   args << "--quiet";

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

   Error error = executeTexToPdf(texBinaryPath("texi2dvi"),
                                 texEnvironmentVars(),
                                 texShellArgs(),
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

