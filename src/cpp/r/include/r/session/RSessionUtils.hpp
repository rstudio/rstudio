/*
 * RSessionUtils.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_SESSION_UTILS_HPP
#define R_SESSION_UTILS_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace utils {

// check for R 3.0
bool isR3();

// check for R 3.3
bool isR3_3();

// check for R 4.0
bool isR4();

bool isPackratModeOn();

bool isDevtoolsDevModeOn();

bool isDefaultPrompt(const std::string& prompt);

bool isServerMode();

// project path
const core::FilePath& projectPath();

// user home path
const core::FilePath& userHomePath();

core::FilePath safeCurrentPath();
   
core::FilePath tempFile(const std::string& prefix, 
                        const std::string& extension);

core::FilePath tempDir();

core::FilePath logPath();

core::FilePath rSourcePath();

core::FilePath rHistoryDir();

core::FilePath rEnvironmentDir();

core::FilePath sessionScratchPath();

core::FilePath scopedScratchPath();

core::FilePath clientStatePath();

core::FilePath projectClientStatePath();

core::FilePath startupEnvironmentFilePath();

core::FilePath suspendedSessionPath();

std::string sessionPort();

std::string rCRANUrl();

std::string rCRANSecondary();

bool restoreWorkspace();

bool alwaysSaveHistory();

bool restoreEnvironmentOnResume();

// suppress output in scope
class SuppressOutputInScope
{
public:
   SuppressOutputInScope();
   ~SuppressOutputInScope();
};

class ShowOutputInScope
{
public:
   ShowOutputInScope();
   ~ShowOutputInScope();
};

} // namespace utils
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_UTILS_HPP

