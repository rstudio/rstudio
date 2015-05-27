/*
 * SessionInvalidScope.hpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

// Everything is defined inine here so we can share code between
// rsession and rserver without linking

#ifndef SESSION_INVALID_SCOPE_HPP
#define SESSION_INVALID_SCOPE_HPP

#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#ifndef _WIN32
#include <core/system/FileMode.hpp>
#endif

#include <core/r_util/RSessionContext.hpp>

#include <session/SessionLocalStreams.hpp>

namespace rstudio {
namespace session {

namespace {

inline core::FilePath invalidSessionContextFile(
                                const core::r_util::SessionContext& context)
{
   return session::local_streams::streamPath(
        core::r_util::sessionContextFile(context) +".invalid");
}

} // anonymous namespace


inline void writeInvalidScope(const core::r_util::SessionContext& context)
{
   core::FilePath filePath = invalidSessionContextFile(context);
   core::Error error = core::writeStringToFile(filePath, "");
   if (!error)
   {
      // chmod on the file so the server can read it
      core::Error error = changeFileMode(filePath,
                                         core::system::EveryoneReadWriteMode);
      if (error)
         LOG_ERROR(error);
   }
   else
   {
      LOG_ERROR(error);
   }
}

inline bool collectInvalidScope(const core::r_util::SessionContext& context)
{
   core::FilePath filePath = invalidSessionContextFile(context);
   if (filePath.exists())
   {
      core::Error error = filePath.remove();
      if (error)
         LOG_ERROR(error);
      return true;
   }
   else
   {
      return false;
   }
}


} // namespace session
} // namespace rstudio

#endif /* SESSION_INVALID_SCOPE_HPP */
