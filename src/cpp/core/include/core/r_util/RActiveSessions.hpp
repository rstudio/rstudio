/*
 * RActiveSessions.hpp
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


#ifndef CORE_R_UTIL_ACTIVE_SESSIONS_HPP
#define CORE_R_UTIL_ACTIVE_SESSIONS_HPP

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>

namespace rstudio {
namespace session {

class ActiveSession
{
public:
   ActiveSession(const core::FilePath& storagePath, const std::string& id)
   {
      scratchPath_ = storagePath.childPath(id);
      core::Error error = scratchPath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

   const core::FilePath& scratchPath() const { return scratchPath_; }

   // Settings& properites()

private:
   core::FilePath scratchPath_;
};


class ActiveSessions
{
   // allocate()
   // list()
};



} // namespace session
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_HPP
