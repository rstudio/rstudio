/*
 * ShinyAsyncJob.hpp
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

#ifndef SESSION_SHINY_ASYNC_JOB_HPP
#define SESSION_SHINY_ASYNC_JOB_HPP

#include "../jobs/AsyncRJobManager.hpp"

namespace rstudio {
namespace session {
namespace modules { 
namespace shiny {

class ShinyAsyncJob : public jobs::AsyncRJob
{
public:
   ShinyAsyncJob(const std::string& name,
         const core::FilePath& path, 
         const std::string& viewerType, 
         const std::string& runCmd);
   void start();

private:
   void enqueueStateEvent(const std::string& state);
   void onStdout(const std::string& output);
   void onCompleted(int exitStatus);

   core::FilePath path_;
   std::string viewerType_;
   std::string runCmd_;
   std::string url_;
};
                      
} // namespace shiny
} // namespace modules
} // namespace session
} // namespace rstudio

#endif

