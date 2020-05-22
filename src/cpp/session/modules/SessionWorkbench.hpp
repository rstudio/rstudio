/*
 * SessionWorkbench.hpp
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

#ifndef SESSION_SESSION_WORKBENCH_HPP
#define SESSION_SESSION_WORKBENCH_HPP

#include <string>

#include <core/system/Environment.hpp>
#include <session/SessionOptions.hpp>

#include "SessionVCS.hpp"
#include "SessionGit.hpp"
#include "SessionSVN.hpp"

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace workbench {
   
std::string editFileCommand();

core::Error initialize();

template <typename T>
void ammendShellPaths(T* pTarget)
{
   // non-path git bin dir
   std::string gitBinDir = git::nonPathGitBinDir();
   if (!gitBinDir.empty())
      core::system::addToPath(pTarget, gitBinDir);

   // non-path svn bin dir
   std::string svnBinDir = svn::nonPathSvnBinDir();
   if (!svnBinDir.empty())
      core::system::addToPath(pTarget, svnBinDir);

#ifdef _WIN32
   // msys_ssh path
   core::system::addToPath(pTarget,
                           session::options().msysSshPath().getAbsolutePath());
#endif
}

} // namespace workbench
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_WORKBENCH_HPP
