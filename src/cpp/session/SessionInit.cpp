/*
 * SessionInit.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionInit.hpp"

#include <r/RExec.hpp>

#include <r/session/RSession.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace init {

namespace {

// have we fully initialized? used by rConsoleRead and clientInit to
// tweak their behavior when the process is first starting
std::atomic<bool> s_sessionInitialized(false);

} // anonymous namespace

bool ensureSessionInitializedImpl()
{
   // install condition handlers if requested
   if (r::session::utils::isR4())
   {
      // install global calling handlers
      SEXP initializeSEXP = R_NilValue;
      r::sexp::Protect protect;
      Error error = r::exec::RFunction(".rs.globalCallingHandlers.initializeCall")
            .call(&initializeSEXP, &protect);
      if (error)
         LOG_ERROR(error);

      Rf_eval(initializeSEXP, R_GlobalEnv);
   }

   // note that we are now fully initialized. we defer setting this
   // flag so that consoleRead and handleClientInit know that we have just
   // started up and can act accordingly
   s_sessionInitialized = true;

   // ensure the session is fully deserialized (deferred deserialization
   // is supported so that the workbench UI can load without having to wait
   // for the potentially very lengthy deserialization of the environment)
   rstudio::r::session::ensureDeserialized();

   return true;

}

// certain things are deferred until after we have sent our first response
// take care of these things here
void ensureSessionInitialized()
{
   static bool once = ensureSessionInitializedImpl();
   (void) once;
}

bool isSessionInitialized()
{
   return s_sessionInitialized;
}

bool isSessionInitializedAndRestored()
{
   return isSessionInitialized() && rstudio::r::session::isSessionRestored();
}

} // namespace init
} // namespace session
} // namespace rstudio

