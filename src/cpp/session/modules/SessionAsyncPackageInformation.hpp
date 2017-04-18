/*
 * SessionAsyncRCompletions.hpp
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

#ifndef SESSION_ASYNC_PACKAGE_INFORMATION_HPP
#define SESSION_ASYNC_PACKAGE_INFORMATION_HPP

#include <core/r_util/RSourceIndex.hpp>
#include <session/SessionAsyncRProcess.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace r_packages {

class AsyncPackageInformationProcess : public async_r::AsyncRProcess
{
public:
   static void update();
   
   friend class CompleteUpdateOnExit;

protected:

   void onStdout(const std::string& output)
   {
      stdOut_ << output;
   }

   void onStderr(const std::string& output)
   {
      // since package load can fail for a myriad of reasons, and these reasons
      // are almost never actionable, we don't log errors
   }

   void onCompleted(int exitStatus);

private:
   static bool s_isUpdating_;
   static bool s_updateRequested_;
   static std::vector<std::string> s_pkgsToUpdate_;

   std::stringstream stdOut_;

};

} // end namespace r_completions
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif
