/*
 * TexEngine.cpp
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

#include <core/tex/TexEngine.hpp>

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

namespace core {
namespace tex {


Error TexEngine::typeset(const core::system::Options& extraEnvVars,
                         const FilePath& texFilePath,
                         const RunProgramFunction& runFunction)
{
   // copy extra environment variables
   core::system::Options env;
   core::system::environment(&env);
   BOOST_FOREACH(const core::system::Option& var, extraEnvVars)
   {
      core::system::setenv(&env, var.first, var.second);
   }

   // set options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.environment = env;
   options.workingDir = texFilePath.parent();

   // provide args
   core::shell_utils::ShellArgs args;
   args << texFilePath.filename();

   // run the program
   return runFunction(programFilePath().absolutePath(), args, options);
}

} // namespace tex
} // namespace core 



