/*
 * SessionSSH.hpp
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

#ifndef SESSION_SSH_HPP
#define SESSION_SSH_HPP

#include <vector>
#include <map>
#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/Process.hpp>

namespace rstudio {
namespace session {
namespace ssh {

// Holds a common set of options that need to be applied to child
// processes when they are executed.
class ProcessOptionsCreator
{
public:
   ProcessOptionsCreator(core::system::ProcessOptions baseOptions)
      : baseOptions_(baseOptions)
   {
   }

   // COPYING: Via compiler (copyable members)

   // Add an environment variable that should be added every time.
   // To explicitly unset an existing environment variable every
   // time, pass std::string() for value.
   void addEnv(const std::string& name, const std::string& value);

   // Remove an environment variable from the list of ones that should
   // be added every time. Note that this will not *unset* this var
   // if it's in the current process's environment.
   void rmEnv(const std::string& name);

   // Add a directory to the path of the child process
   void addToPath(const core::FilePath& dir);

   void setWorkingDirectory(const core::FilePath& dir);
   void clearWorkingDirectory();

   // Create the actual ProcessOptions object from the state of this
   // object.
   core::system::ProcessOptions processOptions() const;

private:
   core::system::ProcessOptions baseOptions_;
   std::map<std::string, std::string> env_;
   std::vector<core::FilePath> pathDirs_;
   core::FilePath workingDir_;
};

} // namespace ssh
} // namespace session
} // namespace rstudio

#endif // SESSION_SSH_HPP
