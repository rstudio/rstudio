/*
 * REmbeddedPosix.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/FilePath.hpp>

#include <boost/date_time/posix_time/posix_time_duration.hpp>

// after boost stuff to prevent length (Rf_length) symbol conflict issues
#include "REmbedded.hpp"
#include <r/RInterface.hpp>
#include <r/RErrorCategory.hpp>

#include <R_ext/eventloop.h>
#include <R_ext/rlocale.h>

#include <Rembedded.h>

#ifdef __APPLE__
extern "C" void R_ProcessEvents(void);
#endif

extern int R_running_as_main_program;  // from unix/system.c

using namespace core;

namespace r {
namespace session {

void runEmbeddedR(const core::FilePath& /*rHome*/,    // ignored on posix
                  const core::FilePath& /*userHome*/, // ignored on posix
                  bool newSession,
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
   // PIPE: required for prevent interrupts to send in Rhttpd (which we
   // have replaced with our own http listener)
   //
   R_SignalHandlers = 0;

   // set message callback early so we can see initialization error messages
   ptr_R_ShowMessage = callbacks.showMessage ;

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
   R_DefParams(Rp) ;
   Rp->R_Slave = FALSE ;
   Rp->R_Quiet = newSession ? FALSE : TRUE;
   Rp->R_Interactive = TRUE ;
   Rp->SaveAction = defaultSaveAction ;
   Rp->RestoreAction = SA_NORESTORE; // handled within initialize()
   Rp->LoadInitFile = newSession ? TRUE : FALSE;
   R_SetParams(Rp) ;

   // redirect console
   R_Interactive = TRUE; // should have also been set by call to Rf_initialize_R
   R_Consolefile = NULL;
   R_Outputfile = NULL;
   ptr_R_ReadConsole = callbacks.readConsole ;
   ptr_R_WriteConsole = NULL; // must set this to NULL for Ex to be called
   ptr_R_WriteConsoleEx = callbacks.writeConsoleEx ;
   ptr_R_EditFile = callbacks.editFile ;
   ptr_R_Busy = callbacks.busy;

   // hook messages (in case Rf_initialize_R overwrites previously set hook)
   ptr_R_ShowMessage = callbacks.showMessage ;

   // hook file handling
   ptr_R_ChooseFile = callbacks.chooseFile ;
   ptr_R_ShowFiles = callbacks.showFiles ;

   // hook history
   ptr_R_loadhistory = callbacks.loadhistory;
   ptr_R_savehistory = callbacks.savehistory;
   ptr_R_addhistory = callbacks.addhistory;

   // hook suicide, but save reference to internal suicide so we can forward
   pInternal->suicide = ptr_R_Suicide;
   ptr_R_Suicide = callbacks.suicide;

   // hook clean up, but save reference to internal clean up so can forward
   if (callbacks.cleanUp != NULL)
   {
      pInternal->cleanUp = ptr_R_CleanUp;
      ptr_R_CleanUp = callbacks.cleanUp ;
   }

   // NOTE: we do not hook the following callbacks because they are targeted
   // at clients that have a stdio-based console
   //    ptr_R_ResetConsole
   //    ptr_R_FlushConsole
   //    ptr_R_ClearerrConsole

   // run main loop (does not return)
   Rf_mainloop();
}

Error completeEmbeddedRInitialization()
{
   return Success();
}

namespace event_loop {

namespace {

// currently installed polled event handler
void (*s_polledEventHandler)(void) = NULL;

// previously existing polled event handler
void (*s_oldPolledEventHandler)(void) = NULL;

// function we register with R to implement polled event handler
void polledEventHandler()
{
   s_polledEventHandler();
   s_oldPolledEventHandler();
}

} // anonymous namespace


void initializePolledEventHandler(void (*newPolledEventHandler)(void))
{
   // implementation based on addTcl() in tcltk_unix.c

   // can only call this once
   BOOST_ASSERT(!s_polledEventHandler);

   // copy handler function
   s_polledEventHandler = newPolledEventHandler;

   // preserve old handler and set new one
   s_oldPolledEventHandler = R_PolledEvents;
   R_PolledEvents = polledEventHandler;

   // set R_wait_usec
   if (R_wait_usec > 10000 || R_wait_usec == 0)
      R_wait_usec = 10000;
}

bool polledEventHandlerInitialized()
{
   return s_polledEventHandler != NULL;
}

void processEvents()
{
#ifdef __APPLE__
   R_ProcessEvents();
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



