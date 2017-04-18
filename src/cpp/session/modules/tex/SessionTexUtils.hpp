/*
 * SessionTexUtils.hpp
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

#ifndef SESSION_MODULES_TEX_UTILS_HPP
#define SESSION_MODULES_TEX_UTILS_HPP

#include <core/FilePath.hpp>

#include <core/system/ShellUtils.hpp>
#include <core/system/Types.hpp>
#include <core/system/Process.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace tex {
namespace utils {

struct RTexmfPaths
{
   bool empty() const { return texInputsPath.empty(); }

   core::FilePath texInputsPath;
   core::FilePath bibInputsPath;
   core::FilePath bstInputsPath;
};

RTexmfPaths rTexmfPaths();

core::system::Options rTexInputsEnvVars();

core::Error runTexCompile(const core::FilePath& texProgramPath,
                          const core::system::Options& envVars,
                          const core::shell_utils::ShellArgs& args,
                          const core::FilePath& texFilePath,
                          core::system::ProcessResult* pResult);

core::Error runTexCompile(
              const core::FilePath& texProgramPath,
              const core::system::Options& envVars,
              const core::shell_utils::ShellArgs& args,
              const core::FilePath& texFilePath,
              const boost::function<void(int,const std::string&)>& onExited);

} // namespace utils
} // namespace tex
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_TEX_UTILS_HPP
