/*
 * SessionModuleContextInternal.hpp
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

#ifndef SESSION_MODULE_CONTEXT_INTERNAL_HPP
#define SESSION_MODULE_CONTEXT_INTERNAL_HPP

#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   class Settings;
}
}

namespace r {
namespace session {
   struct RSuspendOptions;
}
}

namespace rstudio {
namespace session {   
namespace module_context {
 
// initialize
core::Error initialize();
      
// suspend and resume

void onSuspended(const r::session::RSuspendOptions& options,
                 core::Settings* pPersistentState);
void onResumed(const core::Settings& persistentState);

// notify of background processing
void onBackgroundProcessing(bool isIdle);

// source diagnostics
core::FilePath sourceDiagnostics();

// record the port the session's tcp listener bound (see sessionPort)
void setSessionPort(const std::string& port);

// The name of this session's restart context among those in its scope, for a
// restarted session to find what the one before it saved. Not the same thing
// as sessionPort() in every mode. Call once the listener has bound.
std::string restartContextId();

} // namespace module_context
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULE_CONTEXT_INTERNAL_HPP

