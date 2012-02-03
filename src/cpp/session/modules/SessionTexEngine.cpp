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

FilePath texProgramPath()
{
   std::string which;
   Error error = r::exec::RFunction("Sys.which", "texi2dvi").call(&which);
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

   return std::make_pair(name, value);
}

core::system::Options texEnvironmentVars()
{
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
   core::system::Options envVars;

   envVars.push_back(texEnvVar("TEXINPUTS",
                               rTexmfPath.childPath("tex/latex"),
                               true));

   envVars.push_back(texEnvVar("BIBINPUTS",
                               rTexmfPath.childPath("bibtex/bib"),
                               false));

   envVars.push_back(texEnvVar("BSTINPUTS",
                               rTexmfPath.childPath("bibtex/bst"),
                               false));

   return envVars;
}

shell_utils::ShellArgs texShellArgs()
{
   shell_utils::ShellArgs args;

   /*
   args << "-file-line-error";  // errors as file:line:error
   args << "-synctex=-1";       // output synctex (non-compressed)
   args << "-interaction=nonstopmode";
   */

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
                              ShellArgs() << args << texFilePath.filename(),
                              procOptions,
                              cb);
}


SEXP rs_texToPdf(SEXP filePathSEXP)
{
   FilePath texFilePath =
         module_context::resolveAliasedPath(r::sexp::asString(filePathSEXP));

   Error error = executeTexToPdf(texProgramPath(),
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

