/*
 * SessionAsyncNAMESPACECompletions.hpp
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

#ifndef SESSION_ASYNC_NAMESPACE_COMPLETIONS_HPP
#define SESSION_ASYNC_NAMESPACE_COMPLETIONS_HPP

#include <core/r_util/RSourceIndex.hpp>

#include <boost/thread/mutex.hpp>

#include <session/SessionAsyncRProcess.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace r_completions {

class AsyncNAMESPACECompletions : public async_r::AsyncRProcess
{
public:
   typedef std::map< std::string, std::vector<std::string> > Completions;
   static void update();
   static Completions get();
   friend class CompleteUpdateOnExit;

protected:

   void onStdout(const std::string& output)
   {
      stdOut_ << output;
   }

   void onStderr(const std::string& output)
   {
      LOG_ERROR_MESSAGE(output);
   }

   void onCompleted(int exitStatus);

private:
   static bool s_isUpdating_;
   std::stringstream stdOut_;
   
   // TODO: Does access to this need to be moderated by mutex?
   static Completions s_completions_;
};

} // end namespace r_completions
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif

