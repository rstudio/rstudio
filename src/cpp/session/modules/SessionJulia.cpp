/*
 * SessionJulia.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "SessionJulia.hpp"

#include <string>
#include <vector>

#ifdef _WIN32
# include <windows.h>
#else
# include <errno.h>
# include <signal.h>
# include <sys/types.h>
#endif

#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/prefs/UserPrefs.hpp>

#define R_NO_REMAP
#include <Rinternals.h>

// R's console read, as R's own REPL calls it (reticulate's REPL reads the same way)
extern "C" int R_ReadConsole(const char*, unsigned char*, int, int);

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace julia {

namespace {

// longer lines are truncated, with a warning (see RReadConsole)
const int kReadConsoleBufferSize = 1 << 16;

// Read a line of console input for the Julia REPL. Unlike readline(), this
// keeps the line's leading and trailing whitespace, which can be part of a
// Julia string, and adds the line to the console's history.
SEXP rs_juliaReadConsole(SEXP promptSEXP)
{
   const char* prompt = Rf_translateChar(STRING_ELT(promptSEXP, 0));

   // static: an interrupt longjmps out of the read, past any destructor here
   static std::vector<unsigned char> buffer(kReadConsoleBufferSize, 0);
   if (R_ReadConsole(prompt, &buffer[0], kReadConsoleBufferSize, 1) == 0)
      return R_NilValue;

   buffer[kReadConsoleBufferSize - 1] = '\0';
   std::string line(reinterpret_cast<const char*>(&buffer[0]));
   while (!line.empty() && (line.back() == '\n' || line.back() == '\r'))
      line.pop_back();

   return Rf_ScalarString(Rf_mkCharLenCE(line.c_str(), static_cast<int>(line.size()), CE_NATIVE));
}

// Interrupt Julia as a terminal would: SIGINT to its process group, which
// RStudio.init made, so the commands Julia runs get it too; to Julia alone
// if it couldn't make one. (R's pskill() takes no process group.)
SEXP rs_juliaInterrupt(SEXP pidSEXP)
{
#ifndef _WIN32
   pid_t pid = static_cast<pid_t>(Rf_asInteger(pidSEXP));
   if (pid > 0 && ::killpg(pid, SIGINT) == -1)
      ::kill(pid, SIGINT);
#endif
   return R_NilValue;
}

// Kill Julia; on Unix, with its process group (see rs_juliaInterrupt), which
// takes the commands it runs and its watchdog (RStudio.start_watchdog) too
SEXP rs_juliaKill(SEXP pidSEXP)
{
   int pid = Rf_asInteger(pidSEXP);
   if (pid <= 0)
      return R_NilValue;

#ifdef _WIN32
   HANDLE hProcess = ::OpenProcess(PROCESS_TERMINATE, FALSE, static_cast<DWORD>(pid));
   if (hProcess != nullptr)
   {
      ::TerminateProcess(hProcess, 1);
      ::CloseHandle(hProcess);
   }
#else
   if (::killpg(pid, SIGKILL) == -1)
      ::kill(pid, SIGKILL);
#endif

   return R_NilValue;
}

SEXP rs_juliaAllowed()
{
   return Rf_ScalarLogical(isAllowed());
}

SEXP rs_juliaEnabled()
{
   return Rf_ScalarLogical(isEnabled());
}

// The console's Julia process, as SessionJulia.R reports it; 0 when there is
// none. Kept here so that isSuspendable() needn't call into R.
int s_juliaPid = 0;

SEXP rs_juliaSetProcess(SEXP pidSEXP)
{
   int pid = Rf_isNull(pidSEXP) ? 0 : Rf_asInteger(pidSEXP);
   s_juliaPid = (pid == NA_INTEGER || pid < 0) ? 0 : pid;
   return R_NilValue;
}

bool isProcessRunning(int pid)
{
#ifdef _WIN32
   HANDLE hProcess = ::OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, static_cast<DWORD>(pid));
   if (hProcess == nullptr)
      return false;

   DWORD exitCode = 0;
   bool running = ::GetExitCodeProcess(hProcess, &exitCode) && exitCode == STILL_ACTIVE;
   ::CloseHandle(hProcess);
   return running;
#else
   // EPERM: there is such a process, though it isn't ours to signal
   return ::kill(static_cast<pid_t>(pid), 0) == 0 || errno == EPERM;
#endif
}

SEXP rs_juliaIsSuspendable()
{
   return Rf_ScalarLogical(isSuspendable());
}

} // anonymous namespace

bool isSuspendable()
{
   // Julia's state can't be saved and restored as R's is, and Julia ends
   // with R, so suspending would silently discard it. A Julia that has
   // since gone (it was killed, say) has nothing left to lose.
   return s_juliaPid == 0 || !isProcessRunning(s_juliaPid);
}

bool isAllowed()
{
   return session::options().juliaEnabled();
}

bool isEnabled()
{
   return isAllowed() && prefs::userPrefs().juliaConsoleEnabled();
}

bool isReplActive()
{
   bool active = false;
   Error error = r::exec::RFunction(".rs.julia.replIsActive").call(&active);
   if (error)
      LOG_ERROR(error);
   return active;
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_juliaReadConsole, 1);
   RS_REGISTER_CALL_METHOD(rs_juliaInterrupt, 1);
   RS_REGISTER_CALL_METHOD(rs_juliaKill, 1);
   RS_REGISTER_CALL_METHOD(rs_juliaAllowed, 0);
   RS_REGISTER_CALL_METHOD(rs_juliaEnabled, 0);
   RS_REGISTER_CALL_METHOD(rs_juliaSetProcess, 1);
   RS_REGISTER_CALL_METHOD(rs_juliaIsSuspendable, 0);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionJulia.R"));

   return initBlock.execute();
}

} // end namespace julia
} // end namespace modules
} // end namespace session
} // end namespace rstudio
