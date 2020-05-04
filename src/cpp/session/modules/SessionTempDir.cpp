/*
 * SessionTempDir.cpp
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

#include "SessionTempDir.hpp"

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/Exec.hpp>

#include <shared_core/FilePath.hpp>

#include <session/SessionModuleContext.hpp>

#define kSessionTempPath "session_temp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace temp_dir {
namespace {

// Handles an HTTP request for a file in the session temporary directory.
void handleSessionTempRequest(const http::Request& request,
                              http::Response* pResponse)
{
   std::string prefix = "/" kSessionTempPath;
   std::string fileName = http::util::pathAfterPrefix(request, prefix);
   
   pResponse->setCacheableFile(module_context::tempDir().completeChildPath(fileName),
                               request);
   return;
}


} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/" kSessionTempPath, handleSessionTempRequest));
   return initBlock.execute();
}

} // namespace fonts
} // namespace modules
} // namespace session
} // namespace rstudio
