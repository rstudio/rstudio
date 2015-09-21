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
#include <stdlib.h>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include <core/http/LocalStreamSocketUtils.hpp>

namespace rstudio {
namespace session {
namespace local_streams {

inline const std::string getTmpDir()
{
    std::string dir;
    if (getenv("TMPDIR")) {
        dir = std::string(getenv("TMPDIR")) + "/rstudio-rsession";
    } else {
        dir = std::string("/tmp/rstudio-rsession");
    }
    return dir;
}

inline core::Error ensureStreamsDir()
{
   core::FilePath sessionStreamsPath(getTmpDir());
   return core::http::initializeStreamDir(sessionStreamsPath);
}
   
inline core::FilePath streamPath(const std::string& file)
{
   core::FilePath path = core::FilePath(getTmpDir()).complete(file);
   core::Error error = core::http::initializeStreamDir(path.parent());
   if (error)
      LOG_ERROR(error);
   return path;
}

} // namepspace local_streams
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_LOCAL_STREAMS_HPP

