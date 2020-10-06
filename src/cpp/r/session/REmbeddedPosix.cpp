/*
 * REmbeddedPosix.cpp
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

#include <Rversion.h>

#include <r/RExec.hpp>

#include <shared_core/FilePath.hpp>

#include <boost/date_time/posix_time/posix_time_duration.hpp>

// after boost stuff to prevent length (Rf_length) symbol conflict issues
#include "REmbedded.hpp"
#include <r/RInterface.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RUtil.hpp>

#include <R_ext/eventloop.h>

#include <Rembedded.h>

#ifdef __APPLE__
#include <dlfcn.h>
extern "C" void R_ProcessEvents(void);
extern "C" void (*ptr_R_ProcessEvents)(void);
#define QCF_SET_PEPTR  1  /* set ProcessEvents function pointer */
#define QCF_SET_FRONT  2  /* set application mode to front */
extern "C"  typedef void (*ptr_QuartzCocoa_SetupEventLoop)(int, unsigned long);
#endif

extern int R_running_as_main_program;  // from unix/system.c

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

void runEmbeddedR(const core::FilePath& /*rHome*/,    // ignored on posix
                  const core::FilePath& /*userHome*/, // ignored on posix
                  bool quiet,
                  bool loadInitFile,
                  SA_TYPE defaultSaveAction,
                  const Callbacks& callbacks,
                  InternalCallbacks* pInternal)
{
   // disable R signal handlers. see src/main/main.c for the default
   // implementations. in our case ignore them for the following reasons:
   //
   // INT - no concept of Ctrl-C based interruption (use flag directly)
   //
   // SEGV, ILL, & BUS: unsupported due to prompt invoking networking
   // code (unsupported from within a signal handler)
   //
   // USR1 & USR2: same as above SEGV, etc. + we use them for other purposes
   //
   // PIPE: we ignore this globally in SessionMain. before doing this we
   // confirmed that asio wasn't in some way manipulating it -- on linux
   // boost passes MSG_NOSIGNAL to sendmsg and on OSX sets the SO_NOSIGPIPE
   // option on all sockets created. note that on other platforms including
   // solaris, hpux, etc. boost uses detail/signal_init to ignore SIGPIPE

   // globally (this is done in io_service.hpp).
   R_SignalHandlers = 0;

   // set message callback early so we can see initialization error messages
   ptr_R_ShowMessage = callbacks.showMessage;

   // running as main program (affects location of R_CStackStart on platforms
   // without HAVE_LIBC_STACK_END or HAVE_KERN_USRSTACK). see also discussion
   // on R_CStackStart in 8.1.5 Threading issues
   R_running_as_main_program = 1;

   // initialize R
   const char *args[]= {"RStudio", "--interactive"};
   Rf_initialize_R(sizeof(args)/sizeof(args[0]), (char**)args);

   // For newSession = false we need to do a few things:
   //
   //   1) set R_Quiet so we startup without a banner
   //
   //   2) set LoadInitFile to supress execution of .Rprofile
   //
   //   3) we also need to make sure that .First is not executed. this is
   //      taken care of via the fact that we set RestoreAction to SA_NORESTORE
   //      which means that when setup_Rmainloop there is no .First function
   //      available to it because we haven't restored the environment yet.
   //      Note that .First is executed in the case of new sessions because
   //      it is read from .Rprofile as part of setup_Rmainloop. This implies
   //      that in our version of R the .First function must be defined in
   //      .Rprofile rather than simply saved into the global environment
   //      of the default workspace
   //
   structRstart rp;
   Rstart Rp = &rp;
   R_DefParams(Rp);
#if R_VERSION < R_Version(4, 0, 0)
   Rp->R_Slave = FALSE;
#else
   Rp->R_NoEcho = FALSE;
#endif
   Rp->R_Quiet = quiet ? TRUE : FALSE;
   Rp->R_Interactive = TRUE;
   Rp->SaveAction = defaultSaveAction;
   Rp->RestoreAction = SA_NORESTORE; // handled within initialize()
   Rp->LoadInitFile = loadInitFile ? TRUE : FALSE;
   R_SetParams(Rp);

   // redirect console
   R_Interactive = TRUE; // should have also been set by call to Rf_initialize_R
   R_Consolefile = nullptr;
   R_Outputfile = nullptr;
   ptr_R_ReadConsole = callbacks.readConsole;
   ptr_R_WriteConsole = nullptr; // must set this to NULL for Ex to be called
   ptr_R_WriteConsoleEx = callbacks.writeConsoleEx;
   ptr_R_EditFile = callbacks.editFile;
   ptr_R_Busy = callbacks.busy;

   // hook messages (in case Rf_initialize_R overwrites previously set hook)
   ptr_R_ShowMessage = callbacks.showMessage;

   // hook file handling
   ptr_R_ChooseFile = callbacks.chooseFile;
   ptr_R_ShowFiles = callbacks.showFiles;

   // hook history
   ptr_R_loadhistory = callbacks.loadhistory;
   ptr_R_savehistory = callbacks.savehistory;
   ptr_R_addhistory = callbacks.addhistory;

   // hook suicide, but save reference to internal suicide so we can forward
   pInternal->suicide = ptr_R_Suicide;
   ptr_R_Suicide = callbacks.suicide;

   // hook clean up, but save reference to internal clean up so can forward
   pInternal->cleanUp = ptr_R_CleanUp;
   ptr_R_CleanUp = callbacks.cleanUp;

   // NOTE: we do not hook the following callbacks because they are targeted
   // at clients that have a stdio-based console
   //    ptr_R_ResetConsole
   //    ptr_R_FlushConsole
   //    ptr_R_ClearerrConsole

   // run main loop (does not return)
   Rf_mainloop();
}

Error completeEmbeddedRInitialization(bool useInternet2)
{
   return Success();
}

namespace event_loop {

namespace {

// currently installed polled event handler
void (*s_polledEventHandler)(void) = nullptr;

// previously existing polled event handler
void (*s_oldPolledEventHandler)(void) = nullptr;

// function we register with R to implement polled event handler
void polledEventHandler()
{
   if (s_polledEventHandler != nullptr)
      s_polledEventHandler();

   if (s_oldPolledEventHandler != nullptr)
      s_oldPolledEventHandler();
}


#ifdef __APPLE__

void logDLError(const std::string& message, const ErrorLocation& location)
{
   std::string errmsg(message);
   char* dlError = ::dlerror();
   if (dlError)
      errmsg += ": " + std::string(dlError);
   core::log::logErrorMessage(errmsg, location);
}

// Note that when we passed QCF_SET_FRONT to QuartzCocoa_SetupEventLoop
// sometimes this resulted in our application having a "bouncing"
// state which we couldn't rid ourselves of.
//
// Note that in researching the way R implements QCF_SET_FRONT I discovered
// that a depricated API is called AND an explicit call to SetFront. Another
// way to go would be to call the TransformProcessType API:
//
//   http://www.cocoadev.com/index.pl?TransformProcessType
//   http://developer.apple.com/library/mac/#documentation/Carbon/Reference/Process_Manager/Reference/reference.html%23//apple_ref/c/func/TransformProcessType
//
// Note this would look something like (cmake and includes for completeness):
/*
   find_library(CARBON_LIBRARY NAMES Carbon)
   set(LINK_FLAGS ${CARBON_LIBRARY})

   #include <Carbon/Carbon.h>
   #undef TRUE
   #undef FALSE

   static const ProcessSerialNumber thePSN = { 0, kCurrentProcess };
   ::TransformProcessType(&thePSN, kProcessTransformToForegroundApplication);
*/

// attempt to setup quartz event loop, if this fails then log and
// return false (as a result we'll have to disable the quartz R
// function so the user doesn't get in trouble)
bool setupQuartzEventLoop()
{
   // first make sure that the gdDevices pacakage is loaded
   Error error = r::exec::executeString("library(grDevices)");
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // get a reference to the grDevices library
   void* pGrDevices = ::dlopen("grDevices.so",
                               RTLD_LAZY | RTLD_LOCAL | RTLD_NOLOAD);
   if (pGrDevices)
   {
      ptr_QuartzCocoa_SetupEventLoop pSetupEventLoop  =
               (ptr_QuartzCocoa_SetupEventLoop)::dlsym(
                                             pGrDevices,
                                            "QuartzCocoa_SetupEventLoop");
      if (pSetupEventLoop)
      {
         // attempt to setup event loop
         pSetupEventLoop(QCF_SET_PEPTR, 100);

         // check that we got the ptr_R_ProcessEvents initialized
         if (ptr_R_ProcessEvents != nullptr)
         {
            return true;
         }
         else
         {
            LOG_ERROR_MESSAGE("ptr_R_ProcessEvents not initialized");
            return false;
         }
      }
      else
      {
         logDLError("Error looking up QuartzCocoa_SetupEventLoop",
                    ERROR_LOCATION);
         return false;
      }
   }
   else
   {
      logDLError("Error loading grDevices.so", ERROR_LOCATION);
      return false;
   }
}

// On versions prior to R 2.12 the event pump is handled by R_ProcessEvents
// rather than by the expected R_PolledEvents mechanism. On the Mac
// R_ProcessEvents includes a hook (ptr_R_ProcessEvents) but this is
// taken by the quartz module. We therefore need a way to hook it but
// still delegate to quartz so the quartz device works. do this by
// ensuring quartz is loaded then calling QuartzCocoa_SetupEventLoop
void installAppleR_2_11_Workaround(void (*newPolledEventHandler)(void))
{
   // attempt to initialize the quartz event loop (init ptr_R_ProcessEvents
   // so that we can delegate to it after we override it)
   if (!setupQuartzEventLoop())
   {
      Error error = r::exec::RFunction(".rs.disableQuartz").call();
      if (error)
         LOG_ERROR(error);
   }

   // copy handler function
   s_polledEventHandler = newPolledEventHandler;

   // preserve old handler and set new one (note that ptr_R_ProcessEvents
   // might be NULL if we didn't succeed in setting up the quartz
   // event loop above. in this case the polled event handler will
   // ignore it
   s_oldPolledEventHandler = ptr_R_ProcessEvents;
   ptr_R_ProcessEvents = polledEventHandler;
}

#endif

} // anonymous namespace


void initializePolledEventHandler(void (*newPolledEventHandler)(void))
{
   // can only call this once
   BOOST_ASSERT(!s_polledEventHandler);

   // special hack for R 2.11.1 on OSX
#ifdef __APPLE__
   if (!r::util::hasRequiredVersion("2.12"))
   {
      installAppleR_2_11_Workaround(newPolledEventHandler);
      return;
   }
#endif

   // implementation based on addTcl() in tcltk_unix.c

   // copy handler function
   s_polledEventHandler = newPolledEventHandler;

   // preserve old handler and set new one
   s_oldPolledEventHandler = R_PolledEvents;
   R_PolledEvents = polledEventHandler;
   
   // set R_wait_usec
   if (R_wait_usec > 10000 || R_wait_usec == 0)
      R_wait_usec = 10000;
}

// NOTE: this call is used in child process after multicore forks
// to make sure all subsequent R code is executed without any
// event handlers (appropriate since the forked child is headless).
// the prefix "permanently" is used because we explicitly don't
// handle the abilty to restore event handling by calling
// initializePolledEventHandler -- this is because we overwrite
// s_oldPolledEventHandler with NULL, thus losing any reference
// we have to a R_PolledEvents value that existed before our
// initialization (it would be possible to implement a temporary
// disable with a bit more complex control flow)
void permanentlyDisablePolledEventHandler()
{
   s_polledEventHandler = nullptr;
   s_oldPolledEventHandler = nullptr;
}

bool polledEventHandlerInitialized()
{
   return s_polledEventHandler != nullptr;
}

void processEvents()
{
#ifdef __APPLE__
   R_ProcessEvents();

   // pickup X11 graphics device events (if any) via X11 input handler
   fd_set* what = R_checkActivity(0,1);
   if (what != nullptr)
      R_runHandlers(R_InputHandlers, what);
#else
   // check for activity on standard input handlers (but ignore stdin).
   // return immediately if there is no input currently available
   fd_set* what = R_checkActivity(0,1);

   // run handlers on the input (or run the polled event handler if there
   // is no input currently available)
   R_runHandlers(R_InputHandlers, what);
#endif
}

} // namespace event_loop
} // namespace session
} // namespace r
} // namespace rstudio



