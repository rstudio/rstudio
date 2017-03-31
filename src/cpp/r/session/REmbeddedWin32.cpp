/*
 * REmbeddedWin32.cpp
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

#include <windows.h>
#undef TRUE
#undef FALSE

#define R_INTERNAL_FUNCTIONS
#include <r/RInternal.hpp>

#define Win32
#include "REmbedded.hpp"

#include <stdio.h>

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/date_time/posix_time/posix_time_duration.hpp>

#include <core/FilePath.hpp>
#include <core/Exec.hpp>
#include <core/StringUtils.hpp>

#include <r/RInterface.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RExec.hpp>
#include <r/session/REventLoop.hpp>
#include <r/session/RSessionUtils.hpp>

#include <Rembedded.h>
#include <graphapp.h>

extern "C" void R_ProcessEvents(void);
extern "C" void R_CleanUp(SA_TYPE, int, int);
extern "C" UImode CharacterMode;

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {

void (*s_polledEventHandler)(void) = NULL;
void rPolledEventCallback()
{
   if (s_polledEventHandler != NULL)
      s_polledEventHandler();
}

void showMessage(const char *info)
{
   ::MessageBoxA(::GetForegroundWindow(),
                 info,
                 "R Message",
                 MB_ICONEXCLAMATION | MB_OK);
}

#define YES    1
#define NO    -1
#define CANCEL 0
int askYesNoCancel(const char* question)
{
   if (!question)
      question = "";

   int result = ::MessageBoxA(::GetForegroundWindow(),
                              question,
                              "Question",
                              MB_ICONQUESTION | MB_YESNOCANCEL);

   switch(result)
   {
   case IDYES:
      return YES;
   case IDNO:
      return NO;
   case IDCANCEL:
      return CANCEL;
   }
}

void setMemoryLimit()
{
   // set defaults for R_max_memory. this code is based on similar code
   // in cmdlineoptions in system.c (but calls memory.limit directly rather
   // than setting R_max_memory directly, which we can't do because it
   // isn't exported from the R.dll

   // some constants
   const DWORDLONG MB_TO_BYTES = 1024 * 1024;
   const DWORDLONG VIRTUAL_OFFSET = 512 * MB_TO_BYTES;

   // interograte physical and virtual memory
   MEMORYSTATUSEX memoryStatus;
   memoryStatus.dwLength = sizeof(memoryStatus);
   ::GlobalMemoryStatusEx(&memoryStatus);
   DWORDLONG virtualMemory = memoryStatus.ullTotalVirtual - VIRTUAL_OFFSET;
   DWORDLONG physicalMem = memoryStatus.ullTotalPhys;

   // use physical memory on win64. on win32 further constrain by
   // virtual memory minus an offset (for the os and other programs)
 #ifdef WIN64
   DWORDLONG maxMemory = physicalMem;
 #else
   DWORDLONG maxMemory = std::min(virtualMemory, physicalMem);
 #endif

   // call the memory.limit function
   maxMemory = maxMemory / MB_TO_BYTES;
   r::exec::RFunction memoryLimit(".rs.setMemoryLimit");
   memoryLimit.addParam((double)maxMemory);
   Error error = memoryLimit.call();
   if (error)
      LOG_ERROR(error);
}

}

void runEmbeddedR(const core::FilePath& rHome,
                  const core::FilePath& userHome,
                  bool quiet,
                  bool loadInitFile,
                  SA_TYPE defaultSaveAction,
                  const Callbacks& callbacks,
                  InternalCallbacks* pInternal)
{
   // no signal handlers (see comment in REmbeddedPosix.cpp for rationale)
   R_SignalHandlers = 0;

   // set start time
   ::R_setStartTime();

   // setup params structure
   structRstart rp;
   Rstart pRP = &rp;
   ::R_DefParams(pRP);

   // set paths (copy to new string so we can provide char*)
   std::string* pRHome = new std::string(
            core::string_utils::utf8ToSystem(rHome.absolutePath()));
   std::string* pUserHome = new std::string(
            core::string_utils::utf8ToSystem(userHome.absolutePath()));
   pRP->rhome = const_cast<char*>(pRHome->c_str());
   pRP->home = const_cast<char*>(pUserHome->c_str());

   // more configuration
   pRP->CharacterMode = RGui;
   pRP->R_Slave = FALSE;
   pRP->R_Quiet = quiet ? TRUE : FALSE;
   pRP->R_Interactive = TRUE;
   pRP->SaveAction = defaultSaveAction;
   pRP->RestoreAction = SA_NORESTORE;
   pRP->LoadInitFile = loadInitFile ? TRUE : FALSE;

   // hooks
   pRP->ReadConsole = callbacks.readConsole;
   pRP->WriteConsole = NULL;
   pRP->WriteConsoleEx = callbacks.writeConsoleEx;
   pRP->CallBack = rPolledEventCallback;
   pRP->ShowMessage = showMessage;
   pRP->YesNoCancel = askYesNoCancel;
   pRP->Busy = callbacks.busy;

   // set internal callbacks
   pInternal->cleanUp = R_CleanUp;
   pInternal->suicide = R_Suicide;

   // set command line
   const char *args[]= {"RStudio", "--interactive"};
   int argc = sizeof(args)/sizeof(args[0]);
   ::R_set_command_line_arguments(argc, (char**)args);

   // set params
   ::R_SetParams(pRP);

   // clear console input buffer
   ::FlushConsoleInputBuffer(GetStdHandle(STD_INPUT_HANDLE));

   // R global ui initialization
   ::GA_initapp(0, 0);
   ::readconsolecfg();

   // Set CharacterMode to LinkDLL during main loop setup. The mode can't be
   // RGui during setup_Rmainloop or calls to history functions (e.g. timestamp)
   // which occur during .Rprofile execution will crash when R attempts to
   // interact with the (non-existent) R gui console data structures. Note that
   // there are no references to CharacterMode within setup_Rmainloop that we
   // can see so the only side effect of this should be the disabling of the
   // console history mechanism.
   CharacterMode = LinkDLL;

   // setup main loop
   ::setup_Rmainloop();

   // reset character mode to RGui
   CharacterMode = RGui;

   // run main loop
   ::run_Rmainloop();
}

Error completeEmbeddedRInitialization(bool useInternet2)
{
   // set memory limit
   setMemoryLimit();

   // use IE proxy settings if requested
   if (!r::session::utils::isR3_3())
   {
      boost::format fmt("suppressWarnings(utils::setInternet2(%1%))");
      Error error = r::exec::executeString(boost::str(fmt % useInternet2));
      if (error)
         LOG_ERROR(error);
   }

   // register history functions
   Error error = r::exec::RFunction(".rs.registerHistoryFunctions").call();
   if (error)
      LOG_ERROR(error);

   using boost::bind;
   using namespace r::function_hook ;
   ExecBlock block ;
   block.addFunctions()
      (bind(registerUnsupported, "bringToTop", "grDevices"))
      (bind(registerUnsupported, "winMenuAdd", "utils"))
      (bind(registerUnsupported, "winMenuAddItem", "utils"))
      (bind(registerUnsupported, "winMenuDel", "utils"))
      (bind(registerUnsupported, "winMenuDelItem", "utils"))
      (bind(registerUnsupported, "winMenuNames", "utils"))
      (bind(registerUnsupported, "winMenuItems", "utils"));
   return block.execute();
}

namespace event_loop {

void initializePolledEventHandler(void (*newPolledEventHandler)(void))
{
   s_polledEventHandler = newPolledEventHandler;
}

void permanentlyDisablePolledEventHandler()
{
   s_polledEventHandler = NULL;
}


bool polledEventHandlerInitialized()
{
   return s_polledEventHandler != NULL;
}

void processEvents()
{
   R_ProcessEvents();
}

} // namespace event_loop
} // namespace session
} // namespace r
} // namespace rstudio



