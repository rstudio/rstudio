/*
 * SessionSVN.cpp
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

#include "SessionSVN.hpp"

#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionOptions.hpp>

using namespace core;
using namespace core::shell_utils;

namespace session {
namespace modules {
namespace svn {

namespace {

/** GLOBAL STATE **/
FilePath s_workingDir;

core::system::ProcessOptions procOptions()
{
   core::system::ProcessOptions options;

   // detach the session so there is no terminal
#ifndef _WIN32
   options.detachSession = true;
#endif

   // get current environment for modification prior to passing to child
   core::system::Options childEnv;
   core::system::environment(&childEnv);

   // TODO: Add Subversion bin dir to path if necessary

   // add postback directory to PATH
   FilePath postbackDir = session::options().rpostbackPath().parent();
   core::system::addToPath(&childEnv, postbackDir.absolutePath());

   options.workingDir = projects::projectContext().directory();

   // on windows set HOME to USERPROFILE
#ifdef _WIN32
   std::string userProfile = core::system::getenv(childEnv, "USERPROFILE");
   core::system::setenv(&childEnv, "HOME", userProfile);
#endif

   // set custom environment
   options.environment = childEnv;

   return options;
}

} // namespace


bool isSvnInstalled()
{
   core::system::ProcessResult result;
   Error error = system::runCommand("svn help", "", procOptions(), &result);

   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return result.exitStatus == EXIT_SUCCESS;
}

bool isSvnDirectory(const core::FilePath& workingDir)
{
   if (workingDir.empty())
      return false;

   core::system::ProcessOptions options = procOptions();
   options.workingDir = workingDir;

   core::system::ProcessResult result;

   Error error = core::system::runCommand("svn info",
                                          "",
                                          options,
                                          &result);

   if (error)
      return false;

   return result.exitStatus == EXIT_SUCCESS;
}

bool isSvnEnabled()
{
   return !s_workingDir.empty();
}

Error initialize()
{
   return Success();
}

Error initializeSvn(const core::FilePath& workingDir)
{
   s_workingDir = workingDir;
   return Success();
}

} // namespace svn
} // namespace modules
} //namespace session
