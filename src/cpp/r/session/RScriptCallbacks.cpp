/*
 * RScriptCallbacks.cpp
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

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>

#include <r/RExec.hpp>
#include <r/session/RSession.hpp>

#include "REmbedded.hpp"
#include "RInit.hpp"
#include "RScriptCallbacks.hpp"
#include "RStdCallbacks.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {

// script to run, if any
std::string s_runScript;

} // anonymous namespace


void setRunScript(const std::string& runScript)
{
   s_runScript = runScript;
}

// variant of RReadConsole which reads a script instead of using an interactive console
int RReadScript (const char *pmt,
                 CONSOLE_BUFFER_CHAR* buf,
                 int buflen,
                 int hist)
{
   if (s_runScript.empty())
   {
      // exit after we have consumed the script
      return 0;
   }

   // sanity check buffer length
   if (buflen < 2)
   {
      return 0;
   }

   // attempt to initialize
   Error initError;
   Error error = r::exec::executeSafely<Error>(initialize, &initError);
   if (error)
      LOG_ERROR(error);
   if (initError)
      LOG_ERROR(initError);

   // ensure input fits in buffer; we need two extra bytes -- one for the terminating newline and
   // one for the terminating null
   if (s_runScript.length() > static_cast<size_t>(buflen - 2))
   {
      std::string msg = "Script too long (" +
         safe_convert::numberToString(s_runScript.length()) + "), max is " +
         safe_convert::numberToString(buflen - 2) + " characters)";
      LOG_ERROR_MESSAGE(msg);
      rSuicide(msg);
   }

   // copy input into buffer
   s_runScript.copy(reinterpret_cast<char*>(buf), s_runScript.length(), 0);

   // append newline and terminating null
   buf[s_runScript.length()] = '\n';
   buf[s_runScript.length() +1 ] = '\0';

   // remove script
   s_runScript.clear();

   // success
   return 1;
}

// variant of RWriteConsoleEx which writes to standard out
void RWriteStdout (const char *buf, int buflen, int otype)
{
   std::cout << buf;
}

void RScriptCleanUp(SA_TYPE saveact, int status, int runLast)
{
   rCallbacks().quit();
   rCallbacks().cleanup(saveact != SA_SUICIDE);

   // override save action for script runs
   stdInternalCallbacks()->cleanUp(SA_NOSAVE, status, runLast);
}
   
} // namespace session
} // namespace r
} // namespace rstudio

