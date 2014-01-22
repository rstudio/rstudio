/*
 * SessionRMarkdown.cpp
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

#include "SessionRMarkdown.hpp"

#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace rmarkdown {

namespace {

void initPandocPath()
{
   // ensure that pandoc is on the path for all platforms save for
   // redhat 5 (where we don't yet have a redistributable pandoc binary)
#if !defined(_WIN32) && !defined(__APPLE__)
   FilePath redhatReleaseFile("/etc/redhat-release");
   if (redhatReleaseFile.exists())
   {
      // get redhat release info
      std::string redhatRelease;
      Error error = core::readStringFromFile(redhatReleaseFile, &redhatRelease);
      if (error)
         LOG_ERROR(error);

      // check for redhat 5
      if (!boost::algorithm::istarts_with(redhatRelease, "fedora") &&
          boost::algorithm::icontains(redhatRelease, "release 5"))
      {
         return;
      }
   }
#endif

   core::system::setenv("RSTUDIO_PANDOC",
                        session::options().pandocPath().absolutePath());
}


} // anonymous namespace

Error initialize()
{
   initPandocPath();

   return Success();
}
   
} // namepsace rmarkdown
} // namespace modules
} // namesapce session

