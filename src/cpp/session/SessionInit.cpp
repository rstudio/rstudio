/*
 * SessionInit.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionInit.hpp"

#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace init {

namespace {

// have we fully initialized? used by rConsoleRead and clientInit to
// tweak their behavior when the process is first starting
bool s_sessionInitialized = false;

} // anonymous namespace

// certain things are deferred until after we have sent our first response
// take care of these things here
void ensureSessionInitialized()
{
   // note that we are now fully initialized. we defer setting this
   // flag so that consoleRead and handleClientInit know that we have just
   // started up and can act accordingly
   s_sessionInitialized = true;

   // ensure the session is fully deserialized (deferred deserialization
   // is supported so that the workbench UI can load without having to wait
   // for the potentially very lengthy deserialization of the environment)
   rstudio::r::session::ensureDeserialized();
}

bool isSessionInitialized()
{
   return s_sessionInitialized;
}

} // namespace init
} // namespace session
} // namespace rstudio

