/*
 * SessionTexCompiler.cpp
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

#include "SessionTexCompiler.hpp"

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

#include "SessionTexInputs.hpp"

// TODO: investigate other texi2dvi and pdflatex options
//         -- shell-escape
//         -- clean
//         -- alternative output file location

// TODO: emulate texi2dvi on linux to workaround debian tilde
//       escaping bug (http://bugs.debian.org/cgi-bin/bugreport.cgi?bug=534458)

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace compiler {

Error texToPdf(const FilePath& texProgramPath,
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


} // namespace compiler
} // namespace tex
} // namespace modules
} // namesapce session

