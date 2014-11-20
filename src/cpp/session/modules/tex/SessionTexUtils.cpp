/*
 * SessionTexUtils.cpp
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

#include "SessionTexUtils.hpp"

#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>

#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionCompilePdfSupervisor.hpp"

using namespace rscore;

namespace session {
namespace modules { 
namespace tex {
namespace utils {

namespace {

// this function attempts to emulate the behavior of tools::texi2dvi
// in appending extra paths to TEXINPUTS, BIBINPUTS, & BSTINPUTS
rscore::system::Option inputsEnvVar(const std::string& name,
                                  const FilePath& extraPath,
                                  bool ensureForwardSlashes)
{
   std::string value = rscore::system::getenv(name);
   if (value.empty())
      value = ".";

   // on windows tools::texi2dvi replaces \ with / when defining the TEXINPUTS
   // environment variable (but for BIBINPUTS and BSTINPUTS)
#ifdef _WIN32
   if (ensureForwardSlashes)
      boost::algorithm::replace_all(value, "\\", "/");
#endif

   std::string sysPath = string_utils::utf8ToSystem(extraPath.absolutePath());
   rscore::system::addToPath(&value, sysPath);
   rscore::system::addToPath(&value, ""); // trailing : required by tex

   return std::make_pair(name, value);
}

shell_utils::ShellArgs buildArgs(const shell_utils::ShellArgs& args,
                                 const FilePath& texFilePath)
{
   shell_utils::ShellArgs procArgs;
   procArgs << args;
   procArgs << texFilePath.filename();
   return procArgs;
}

void ignoreOutput(const std::string& output)
{
}

} // anonymous namespace

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
      LOG_ERROR(rscore::pathNotFoundError(rHomeShare, ERROR_LOCATION));
      return RTexmfPaths();
   }

   // R texmf path
   FilePath rTexmfPath(rHomeSharePath.complete("texmf"));
   if (!rTexmfPath.exists())
   {
      LOG_ERROR(rscore::pathNotFoundError(rTexmfPath.absolutePath(),
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


// build TEXINPUTS, BIBINPUTS etc. by composing any existing value in
// the environment (or . if none) with the R dirs in share/texmf
rscore::system::Options rTexInputsEnvVars()
{
   rscore::system::Options envVars;
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

Error runTexCompile(const FilePath& texProgramPath,
                    const rscore::system::Options& envVars,
                    const shell_utils::ShellArgs& args,
                    const FilePath& texFilePath,
                    rscore::system::ProcessResult* pResult)
{
   // copy extra environment variables
   rscore::system::Options env;
   rscore::system::environment(&env);
   BOOST_FOREACH(const rscore::system::Option& var, envVars)
   {
      rscore::system::setenv(&env, var.first, var.second);
   }

   // set options
   rscore::system::ProcessOptions procOptions;
   procOptions.terminateChildren = true;
   procOptions.redirectStdErrToStdOut = true;
   procOptions.environment = env;
   procOptions.workingDir = texFilePath.parent();

   // run the program
   return rscore::system::runProgram(
               string_utils::utf8ToSystem(texProgramPath.absolutePath()),
               buildArgs(args, texFilePath),
               "",
               procOptions,
               pResult);
}

rscore::Error runTexCompile(
              const rscore::FilePath& texProgramPath,
              const rscore::system::Options& envVars,
              const rscore::shell_utils::ShellArgs& args,
              const rscore::FilePath& texFilePath,
              const boost::function<void(int,const std::string&)>& onExited)
{
   return compile_pdf_supervisor::runProgram(
                              texProgramPath,
                              buildArgs(args, texFilePath),
                              envVars,
                              texFilePath.parent(),
                              ignoreOutput,
                              onExited);

}

} // namespace utils
} // namespace tex
} // namespace modules
} // namesapce session

