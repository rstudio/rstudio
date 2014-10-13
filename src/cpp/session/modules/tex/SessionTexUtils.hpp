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

namespace rstudiocore {
   class Error;
}
 
namespace session {
namespace modules { 
namespace tex {
namespace utils {

struct RTexmfPaths
{
   bool empty() const { return texInputsPath.empty(); }

   rstudiocore::FilePath texInputsPath;
   rstudiocore::FilePath bibInputsPath;
   rstudiocore::FilePath bstInputsPath;
};

RTexmfPaths rTexmfPaths();

rstudiocore::system::Options rTexInputsEnvVars();

rstudiocore::Error runTexCompile(const rstudiocore::FilePath& texProgramPath,
                          const rstudiocore::system::Options& envVars,
                          const rstudiocore::shell_utils::ShellArgs& args,
                          const rstudiocore::FilePath& texFilePath,
                          rstudiocore::system::ProcessResult* pResult);

rstudiocore::Error runTexCompile(
              const rstudiocore::FilePath& texProgramPath,
              const rstudiocore::system::Options& envVars,
              const rstudiocore::shell_utils::ShellArgs& args,
              const rstudiocore::FilePath& texFilePath,
              const boost::function<void(int,const std::string&)>& onExited);

} // namespace utils
} // namespace tex
} // namespace modules
} // namesapce session

#endif // SESSION_MODULES_TEX_UTILS_HPP
