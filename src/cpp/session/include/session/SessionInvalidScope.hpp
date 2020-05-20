/*
 * SessionInvalidScope.hpp
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

// Everything is defined inine here so we can share code between
// rsession and rserver without linking

#ifndef SESSION_INVALID_SCOPE_HPP
#define SESSION_INVALID_SCOPE_HPP

#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <session/SessionLocalStreams.hpp>

namespace rstudio {
namespace session {

namespace {

inline core::FilePath invalidSessionContextFile(
                                const core::r_util::SessionContext& context)
{
   return local_streams::streamPath(
        core::r_util::sessionContextFile(context) +".invalid");
}

} // anonymous namespace


inline void writeInvalidScope(const core::r_util::SessionContext& context,
                              core::r_util::SessionScopeState scopeState)
{
   core::FilePath filePath = invalidSessionContextFile(context);
   core::Error error = core::writeStringToFile(filePath,
         boost::lexical_cast<std::string>(scopeState));
   if (!error)
   {
      // chmod on the file so the server can read it
      core::Error error = filePath.changeFileMode(core::FileMode::ALL_READ_WRITE);
      if (error)
         LOG_ERROR(error);
   }
   else
   {
      LOG_ERROR(error);
   }
}

inline core::r_util::SessionScopeState collectInvalidScope(
      const core::r_util::SessionContext& context)
{
   core::FilePath filePath = invalidSessionContextFile(context);
   if (filePath.exists())
   {
      std::string scopeState;
      core::Error error = core::readStringFromFile(filePath, &scopeState);
      if (error)
      {
         LOG_ERROR(error);
         return core::r_util::ScopeInvalidSession;
      }
      error = filePath.remove();
      if (error)
         LOG_ERROR(error);
      return static_cast<core::r_util::SessionScopeState>(
               boost::lexical_cast<unsigned>(scopeState));
   }
   else
   {
      return core::r_util::ScopeValid;
   }
}


} // namespace session
} // namespace rstudio

#endif /* SESSION_INVALID_SCOPE_HPP */
