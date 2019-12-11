/*
 * AsyncRJobManager.hpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#ifndef SESSION_ASYNC_R_JOB_MANAGER_HPP
#define SESSION_ASYNC_R_JOB_MANAGER_HPP

#include <session/SessionAsyncRProcess.hpp>

namespace rstudio {
namespace core {
   class Error;
}

namespace rstudio {
namespace session {
namespace modules { 
namespace jobs {

class AsyncRJob : public async_r::AsyncRProcess
{
public:
   std::string id();
   void cancel();
   void start();
   void onStderr(const std::string& output);

protected:
   bool completed_;
   bool cancelled_;

   boost::shared_ptr<Job> job_;
   boost::function<void()> onComplete_;
};

core::Error startAsyncRJob(boost::shared_ptr<async_r::AsyncRProcess> job,
      std::string *pId);

core::Error stopAsyncRJob(const std::string& id);
 
} // namespace jobs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_ASYNC_R_JOB_MANAGER_HPP

