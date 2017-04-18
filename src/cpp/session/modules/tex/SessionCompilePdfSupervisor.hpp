/*
 * SessionCompilePdfSupervisor.hpp
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

#ifndef SESSION_MODULES_TEX_COMPILE_PDF_SUPERVISOR_HPP
#define SESSION_MODULES_TEX_COMPILE_PDF_SUPERVISOR_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>
#include <boost/date_time/posix_time/posix_time_duration.hpp>

#include <core/system/Types.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   namespace system {
      struct ProcessOptions;
   }
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace tex {
namespace compile_pdf_supervisor {


bool hasRunningChildren();
core::Error terminateAll(const boost::posix_time::time_duration& waitDuration);

core::Error runProgram(const core::FilePath& programFilePath,
                       const std::vector<std::string>& args,
                       const core::system::Options& extraEnvVars,
                       const core::FilePath& workingDir,
                       const boost::function<void(const std::string&)>& onOutput,
                       const boost::function<void(int,const std::string&)>& onExited);

core::Error initialize();

} // namespace compile_pdf_supervisor
} // namespace tex
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_TEX_COMPILE_PDF_SUPERVISOR_HPP
