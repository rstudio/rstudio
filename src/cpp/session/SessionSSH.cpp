/*
 * SessionSSH.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <session/SessionSSH.hpp>

#include <core/system/Environment.hpp>

// TODO: Implement ProcessOptions.workingDir for Windows

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace ssh {

void ProcessOptionsCreator::addEnv(const std::string& name, const std::string& value)
{
   env_[name] = value;
}

void ProcessOptionsCreator::rmEnv(const std::string& name)
{
   env_.erase(name);
}

void ProcessOptionsCreator::addToPath(const core::FilePath& dir)
{
   pathDirs_.push_back(dir);
}

void ProcessOptionsCreator::setWorkingDirectory(const core::FilePath& dir)
{
   workingDir_ = dir;
}

void ProcessOptionsCreator::clearWorkingDirectory()
{
   workingDir_ = FilePath();
}

core::system::ProcessOptions ProcessOptionsCreator::processOptions() const
{
   core::system::ProcessOptions options = baseOptions_;

   // Set up environment
   core::system::Options envOpts;
   core::system::environment(&envOpts);
   typedef std::pair<std::string, std::string> StringPair;
   for (StringPair var : env_)
   {
      if (var.second.empty())
         core::system::unsetenv(&envOpts, var.first);
      else
         core::system::setenv(&envOpts, var.first, var.second);
   }

   if (!pathDirs_.empty())
   {
      std::string path = core::system::getenv(envOpts, "PATH");
      for (FilePath pathDir : pathDirs_)
      {
#ifdef _WIN32
         path += ";";
#else
         path += ":";
#endif
         path += pathDir.getAbsolutePathNative();
      }
      core::system::setenv(&envOpts, "PATH", path);
   }

   if (!workingDir_.isEmpty())
   {
      options.workingDir = workingDir_;
   }

   return options;
}

} // namespace ssh
} // namespace session
} // namespace rstudio
