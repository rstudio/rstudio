/*
 * ScriptJob.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#ifndef SESSION_SCRIPT_JOB_HPP
#define SESSION_SCRIPT_JOB_HPP

#include "Job.hpp"

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace modules {      
namespace jobs {

class ScriptLaunchSpec 
{
public:
   ScriptLaunchSpec(const core::FilePath& path,
         const core::FilePath& workingDir,
         bool importEnv,
         bool exportEnv);
   core::FilePath path();
   core::FilePath workingDir();
   bool importEnv();
   bool exportEnv();
private:
   core::FilePath path_;
   core::FilePath workingDir_;
   bool importEnv_;
   bool exportEnv_;
};

core::Error startScriptJob(const ScriptLaunchSpec& spec);

} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
