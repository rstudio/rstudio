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

// has R's deferred init hook completed for the current R session? used by
// clientInit so the frontend can distinguish a re-join (deferred init already
// completed) from a fresh start or suspend/resume (event still to fire)
std::atomic<bool> s_deferredInitCompleted(false);

// has ensureSessionInitialized() run? (main thread only.) this is an
// explicit flag, set before the one-time work begins, rather than a
// function-local static: an R error escaping that work longjmps over the
// static's initialization guard and leaves it locked for good, so the next
// client_init blocks forever in __cxa_guard_acquire (#18718)
bool s_ensureSessionInitializedCalled = false;

void installGlobalCallingHandlers()
{
   SEXP initializeSEXP = R_NilValue;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.globalCallingHandlers.initializeCall")
         .call(&initializeSEXP, &protect);
   if (error)
      LOG_ERROR(error);

   // this must be evaluated directly rather than via R_tryEval(): global
   // calling handlers attach to R's top-level context, and would be discarded
   // along with the temporary context established by R_ToplevelExec(). an R
   // error raised here therefore escapes as a longjmp. the R side contains
   // errors while resolving handlers, but registration itself can still fail
   // (e.g. if a caller has condition handlers on the stack)
   Rf_eval(initializeSEXP, R_GlobalEnv);
}

void ensureSessionInitializedImpl()
{
   // note that we are now fully initialized. we defer setting this
   // flag so that consoleRead and handleClientInit know that we have just
   // started up and can act accordingly
   s_sessionInitialized = true;

   // ensure the session is fully deserialized (deferred deserialization
   // is supported so that the workbench UI can load without having to wait
   // for the potentially very lengthy deserialization of the environment)
   rstudio::r::session::ensureDeserialized();

   // install condition handlers last: the raw R evaluation can still longjmp,
   // and must not prevent deserialization or leave s_sessionInitialized false
   // after the one-time initialization flag has been set
   if (r::session::utils::isR4())
      installGlobalCallingHandlers();
}

} // anonymous namespace

// certain things are deferred until after we have sent our first response
// take care of these things here
void ensureSessionInitialized()
{
   if (s_ensureSessionInitializedCalled)
      return;

   s_ensureSessionInitializedCalled = true;
   ensureSessionInitializedImpl();
}

bool isSessionInitialized()
{
   return s_sessionInitialized;
}

bool isSessionInitializedAndRestored()
{
   return isSessionInitialized() && rstudio::r::session::isSessionRestored();
}

void setDeferredInitCompleted(bool completed)
{
   s_deferredInitCompleted = completed;
}

bool isDeferredInitCompleted()
{
   return s_deferredInitCompleted;
}

} // namespace init
} // namespace session
} // namespace rstudio

