/*
 * REmbeddedWin32.cpp
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

#include <windows.h>
#undef TRUE
#undef FALSE

#include <Rversion.h>

#define R_INTERNAL_FUNCTIONS
#include <Rversion.h>
#include <r/RInternal.hpp>
#include <r/RUtil.hpp>
#include <r/RVersionInfo.hpp>

#define Win32
#include "REmbedded.hpp"

#include <stdio.h>

#include <boost/format.hpp>
#include <boost/bind/bind.hpp>
#include <boost/date_time/posix_time/posix_time_duration.hpp>

#include <shared_core/FilePath.hpp>

#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/Version.hpp>
#include <core/system/Environment.hpp>
#include <core/system/LibraryLoader.hpp>

#include <r/RInterface.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RExec.hpp>
#include <r/session/REventLoop.hpp>
#include <r/session/RSessionUtils.hpp>

#include <Rembedded.h>
#include <graphapp.h>

#ifdef ReadConsole
# undef ReadConsole
#endif

#ifdef WriteConsole
# undef WriteConsole
#endif

// needed for compilation with older versions of R
#ifndef R_SIZE_T
# include <cstddef>
# define R_SIZE_T std::size_t
#endif


extern "C" void R_ProcessEvents(void);
extern "C" void R_CleanUp(SA_TYPE, int, int);
extern "C" void cmdlineoptions(int, char**);

extern "C" {
__declspec(dllimport) UImode CharacterMode;
}

// Added in R 4.2.0; loaded dynamically
namespace rstudio {
namespace dynload {
int (*R_DefParamsEx)(Rstart, int);
} // namespace dynload
} // namespace rstudio

using namespace rstudio::core;
using namespace boost::placeholders;

// local copy of R startup struct, with support for R_ResetConsole
extern "C" {
typedef struct
{
    Rboolean R_Quiet;
    Rboolean R_NoEcho;
    Rboolean R_Interactive;
    Rboolean R_Verbose;
    Rboolean LoadSiteFile;
    Rboolean LoadInitFile;
    Rboolean DebugInitFile;
    SA_TYPE RestoreAction;
    SA_TYPE SaveAction;
    R_SIZE_T vsize;
    R_SIZE_T nsize;
    R_SIZE_T max_vsize;
    R_SIZE_T max_nsize;
    R_SIZE_T ppsize;
    int NoRenviron;

    char* rhome;
    char* home;
    int (*ReadConsole)(const char *, char *, int, int);
    void (*WriteConsole)(const char *, int);
    void (*CallBack)(void);
    void (*ShowMessage)(const char *);
    int (*YesNoCancel)(const char *);
    void (*Busy)(int);
    UImode CharacterMode;
    void (*WriteConsoleEx)(const char *, int, int);

    // Added in R 4.0.0
    Rboolean EmitEmbeddedUTF8;

    // Added in R 4.2.0
    void (*CleanUp)(SA_TYPE, int, int);
    void (*ClearerrConsole)(void);
    void (*FlushConsole)(void);
    void (*ResetConsole)(void);
    void (*Suicide)(const char*);

    // Padding, to allow for extensions in newer versions of R,
    // in case newer versions of R expect a struct with extra
    // memory available.
    char padding[128];

} RStartup;
} // extern "C"


namespace rstudio {
namespace r {
namespace session {

namespace {

int s_disablePolledEventHandler = 0;

void (*s_polledEventHandler)(void) = nullptr;
void rPolledEventCallback()
{
   if (s_disablePolledEventHandler != 0)
       return;

   if (s_polledEventHandler != nullptr)
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

   // silence compiler warning
   default:
      return CANCEL;
   }
}

void initializeParams(RStartup* pRP)
{
   // use R_DefParams for older versions of R
   Version dllVersion(getDLLVersion());
   if (dllVersion < Version("4.2.0"))
      return R_DefParams((Rstart) pRP);

   // otherwise, try and get the R_DefParamsEx routine and use that
   core::system::Library rLibrary("R.dll");
   if (rLibrary == nullptr)
   {
      LOG_WARNING_MESSAGE("Couldn't load R.dll");
      return R_DefParams((Rstart) pRP);
   }

   // try to load R_DefParamsEx routine
   Error error = core::system::loadSymbol(
            rLibrary,
            "R_DefParamsEx",
            (void**) &rstudio::dynload::R_DefParamsEx);

   if (error)
   {
      LOG_ERROR(error);
      return R_DefParams((Rstart) pRP);
   }

   // invoke it
   rstudio::dynload::R_DefParamsEx((Rstart) pRP, 1);
}

} // end anonymous namespace

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

   // setup command line options
   // note that R does a lot of initialization here that's not accessible
   // in any other way; e.g. the default translation domain is set within
   //
   // https://github.com/rstudio/rstudio/issues/10308
   static const int rargc = 1;
   static const char* rargv[] = { "R.exe" };
   ::cmdlineoptions(rargc, (char**) rargv);

   // setup params structure
   RStartup rp;
   RStartup* pRP = &rp;
   memset(&rp, 0, sizeof(rp));

   // initialize params
   initializeParams(pRP);

   // set paths (copy to new string so we can provide char*)
   std::string* pRHome;
   std::string* pUserHome;

   // With R 4.2.x, using UCRT, we avoid converting to the native encoding
   // and retain the UTF-8 encoding here
   if (core::Version(getDLLVersion()) >= core::Version("4.2.0"))
   {
      pRHome = new std::string(rHome.getAbsolutePath());
      pUserHome = new std::string(userHome.getAbsolutePath());
   }
   else
   {
      using string_utils::utf8ToSystem;
      pRHome = new std::string(utf8ToSystem(rHome.getAbsolutePath()));
      pUserHome = new std::string(utf8ToSystem(userHome.getAbsolutePath()));
   }

   pRP->rhome = const_cast<char*>(pRHome->c_str());
   pRP->home = const_cast<char*>(pUserHome->c_str());

   // more configuration
   pRP->CharacterMode = RGui;
   pRP->R_NoEcho = FALSE;
   pRP->R_Quiet = quiet ? TRUE : FALSE;
   pRP->R_Interactive = TRUE;
   pRP->SaveAction = defaultSaveAction;
   pRP->RestoreAction = SA_NORESTORE;
   pRP->LoadInitFile = loadInitFile ? TRUE : FALSE;

   // hooks
   pRP->ReadConsole = callbacks.readConsole;
   pRP->WriteConsole = nullptr;
   pRP->WriteConsoleEx = callbacks.writeConsoleEx;
   pRP->CallBack = rPolledEventCallback;
   pRP->ShowMessage = showMessage;
   pRP->YesNoCancel = askYesNoCancel;
   pRP->Busy = callbacks.busy;

   // R 4.0.0 hooks
   pRP->EmitEmbeddedUTF8 = TRUE;

   // R 4.2.0 hooks
   pRP->ResetConsole = callbacks.resetConsole;

   // set internal callbacks
   pInternal->cleanUp = R_CleanUp;
   pInternal->suicide = R_Suicide;

   // set command line
   const char *argv[] = {"RStudio", "--interactive"};
   int argc = sizeof(argv) / sizeof(argv[0]);
   ::R_set_command_line_arguments(argc, const_cast<char**>(argv));

   // set params
   ::R_SetParams((Rstart) pRP);

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

Error completeEmbeddedRInitialization()
{
   // register history functions
   Error error = r::exec::RFunction(".rs.registerHistoryFunctions").call();
   if (error)
      LOG_ERROR(error);

   using boost::bind;
   using namespace r::function_hook;
   ExecBlock block;
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
   s_polledEventHandler = nullptr;
}


bool polledEventHandlerInitialized()
{
   return s_polledEventHandler != nullptr;
}

void processEvents()
{
   R_ProcessEvents();
}

DisablePolledEventHandlerScope::DisablePolledEventHandlerScope()
{
   s_disablePolledEventHandler += 1;
}

DisablePolledEventHandlerScope::~DisablePolledEventHandlerScope()
{
   s_disablePolledEventHandler -= 1;
}


} // namespace event_loop
} // namespace session
} // namespace r
} // namespace rstudio


