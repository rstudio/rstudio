/*
 * SessionLocalStreams.hpp
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

#ifndef SESSION_SESSION_LOCAL_STREAMS_HPP
#define SESSION_SESSION_LOCAL_STREAMS_HPP

#include <string>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include <core/http/LocalStreamSocketUtils.hpp>

#define kSessionLocalStreamsDir "/tmp/rstudio-rsession"

namespace session {
namespace local_streams {

inline core::Error createStreamsDir()
{
   core::FilePath sessionStreamsPath(kSessionLocalStreamsDir);
   return core::http::initializeStreamDir(sessionStreamsPath);
}
   
inline core::FilePath streamPath(const std::string& user)
{
   return core::FilePath(kSessionLocalStreamsDir).complete(user);
}

inline void removeStreams(const std::string& user)
{
   core::Error error = streamPath(user).removeIfExists();
   if (error)
      LOG_ERROR(error);
}

} // namepspace local_streams
} // namespace session

#endif // SESSION_SESSION_LOCAL_STREAMS_HPP

