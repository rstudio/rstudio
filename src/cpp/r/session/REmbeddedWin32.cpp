/*
 * REmbeddedWin32.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
#include <boost/date_time/posix_time/posix_time_duration.hpp>

#include <core/FilePath.hpp>
#include <core/Exec.hpp>

#include <r/RInterface.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RExec.hpp>
#include <r/session/REventLoop.hpp>

#include <Rembedded.h>
#include <graphapp.h>

// from Defn.h
extern "C" void R_ProcessEvents(void);

// from Startup.h
extern "C" void R_CleanUp(SA_TYPE, int, int);

// from system.c
extern "C" UImode CharacterMode;

// for do_edit fork
extern "C" FILE *R_fopen(const char *filename, const char *mode);
extern "C" void R_ResetConsole(void);
#define FORSOURCING		95 /* not DELAYPROMISES, used in edit.c */
extern "C" SEXP Rf_deparse1(SEXP,Rboolean,int);


using namespace core;

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

// save callbacks for delegation
Callbacks s_callbacks;

SEXP loadHistoryHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::function_hook::checkArity(op, args, call);
   s_callbacks.loadhistory(call, op, args, rho);
   return R_NilValue;
}

SEXP saveHistoryHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::function_hook::checkArity(op, args, call);
   s_callbacks.savehistory(call, op, args, rho);
   return R_NilValue;
}

SEXP addHistoryHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::function_hook::checkArity(op, args, call);
   s_callbacks.addhistory(call, op, args, rho);
   return R_NilValue;
}



// defines to allow us to make minimal mods to forked do_fileshow & do_edit
extern "C" void Rf_checkArityCall(SEXP op, SEXP args, SEXP call);
#define checkArity(a,b) Rf_checkArityCall(a,b,call)
#define _(String) String

// for Win32 we had to fork this from platform.c (because Win32 doesn't
// make a hook available for R_ShowFiles). The only change made to the
// function is to substitute s_callbacks.showFiles for R_ShowFiles
SEXP fileShowHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
    SEXP fn, tl, hd, pg;
    const char **f, **h, *t, *pager = NULL /* -Wall */;
    Rboolean dl;
    int i, n;

    checkArity(op, args);
    fn = CAR(args); args = CDR(args);
    hd = CAR(args); args = CDR(args);
    tl = CAR(args); args = CDR(args);
    dl = (Rboolean) Rf_asLogical(CAR(args)); args = CDR(args);
    pg = CAR(args);
    n = 0;			/* -Wall */
    if (!Rf_isString(fn) || (n = Rf_length(fn)) < 1)
   Rf_error(_("invalid filename specification"));
    if (!Rf_isString(hd) || Rf_length(hd) != n)
   Rf_error(_("invalid '%s' argument"), "headers");
    if (!Rf_isString(tl))
   Rf_error(_("invalid '%s' argument"), "title");
    if (!Rf_isString(pg))
   Rf_error(_("invalid '%s' argument"), "pager");
    f = (const char**) R_alloc(n, sizeof(char*));
    h = (const char**) R_alloc(n, sizeof(char*));
    for (i = 0; i < n; i++) {
   SEXP el = STRING_ELT(fn, i);
   if (!Rf_isNull(el) && el != NA_STRING)
#ifdef Win32
       f[i] = Rf_acopy_string(Rf_reEnc(CHAR(el), Rf_getCharCE(el), CE_UTF8, 1));
#else
       f[i] = Rf_acopy_string(Rf_translateChar(el));
#endif
   else
            Rf_error(_("invalid filename specification"));
   if (STRING_ELT(hd, i) != NA_STRING)
       h[i] = Rf_acopy_string(Rf_translateChar(STRING_ELT(hd, i)));
   else
            Rf_error(_("invalid '%s' argument"), "headers");
    }
    if (Rf_isValidStringF(tl))
   t = Rf_acopy_string(Rf_translateChar(STRING_ELT(tl, 0)));
    else
   t = "";
    if (Rf_isValidStringF(pg)) {
   SEXP pg0 = STRING_ELT(pg, 0);
        if (pg0 != NA_STRING)
            pager = Rf_acopy_string(CHAR(pg0));
        else
            Rf_error(_("invalid '%s' argument"), "pager");
    } else
   pager = "";
    s_callbacks.showFiles(n, f, h, t, dl, pager);
    return R_NilValue;
}

// for Win32 we had to fork this from edit.c (because Win32 R doesn't
// make a hook avaialabe for R_EditFile).
static char *DefaultFileName = NULL;
static int  EdFileUsed = 0;
SEXP editHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
    int   i;
    SEXP  x, fn, envir, src, srcfile, Rfn;
    char *filename;
    const void *vmaxsave;
    FILE *fp;


   checkArity(op, args);

    vmaxsave = vmaxget();

    x = CAR(args); args = CDR(args);
    if (TYPEOF(x) == CLOSXP) envir = CLOENV(x);
    else envir = R_NilValue;
    PROTECT(envir);

    fn = CAR(args); args = CDR(args);
    if (!Rf_isString(fn))
   Rf_error(_("invalid argument to edit()"));

    if (LENGTH(STRING_ELT(fn, 0)) > 0) {
   const char *ss = Rf_translateChar(STRING_ELT(fn, 0));
   filename = R_alloc(strlen(ss), sizeof(char));
   strcpy(filename, ss);
    }
    else filename = DefaultFileName;

    srcfile = R_NilValue;
    if (x != R_NilValue) {

   if((fp=R_fopen(R_ExpandFileName(filename), "w")) == NULL)
       Rf_errorcall(call, _("unable to open file"));
   if (LENGTH(STRING_ELT(fn, 0)) == 0) EdFileUsed++;
   if (TYPEOF(x) != CLOSXP || Rf_isNull(src = Rf_getAttrib(x, R_SourceSymbol)))
       src = Rf_deparse1(x, FALSE, FORSOURCING); /* deparse for sourcing, not for display */
   for (i = 0; i < LENGTH(src); i++)
       fprintf(fp, "%s\n", Rf_translateChar(STRING_ELT(src, i)));
   fclose(fp);
   PROTECT(Rfn = Rf_findFun(Rf_install("srcfilecopy"), R_BaseEnv));
   PROTECT(srcfile = Rf_lang3(Rfn, Rf_ScalarString(Rf_mkChar("<tmp>")), src));
   PROTECT(srcfile = Rf_eval(srcfile, R_BaseEnv));
   UNPROTECT(3);
    }
    PROTECT(srcfile);

    // edit the file
    s_callbacks.editFile(filename);



    if (!Rf_isNull(srcfile)) {
   PROTECT(Rfn = Rf_findFun(Rf_install("readLines"), R_BaseEnv));
   PROTECT(src = Rf_lang2(Rfn, Rf_ScalarString(Rf_mkChar(R_ExpandFileName(filename)))));
   PROTECT(src =Rf_eval(src, R_BaseEnv));
   Rf_defineVar(Rf_install("lines"), src, srcfile);
   UNPROTECT(3);
    }

   // parse the file by calling parse directly (because we can't access
   // R_ParseFile and related internals)
    r::sexp::Protect rProtect;
    Error error;
    {
       r::exec::RFunction parseFn("parse");
       parseFn.addParam(std::string(R_ExpandFileName(filename)));
       error = parseFn.call(&x, &rProtect);
    }
    if (error)
      Rf_errorcall(call, "expression parsing error");

    R_ResetConsole();
    {   /* can't just eval(x) here */
   int j, n;
   SEXP tmp = R_NilValue;

   n = LENGTH(x);
   for (j = 0 ; j < n ; j++)
       tmp = Rf_eval(VECTOR_ELT(x, j), R_GlobalEnv);
   x = tmp;
    }
    if (TYPEOF(x) == CLOSXP && envir != R_NilValue)
   SET_CLOENV(x, envir);
    UNPROTECT(2);
    vmaxset(vmaxsave);
    return (x);
}

void setMemoryLimit()
{
   // set defaults for R_max_memory. this code is based on similar code
   // in cmdlineoptions in system.c (but calls memory.limit directly rather
   // than setting R_max_memory directly, which we can't do because it
   // isn't exported from the R.dll
   const DWORDLONG Mega = 1048576;
   MEMORYSTATUSEX ms;
   ms.dwLength = sizeof(MEMORYSTATUSEX);
   ::GlobalMemoryStatusEx(&ms);
   DWORDLONG virtualMem = ms.ullTotalVirtual;
   DWORDLONG physicalMem = ms.ullTotalPhys;

 #ifdef WIN64
   DWORDLONG maxMemory = physicalMem;
 #else
   DWORDLONG maxMemory = std::min(virtualMem - 512*Mega, physicalMem);
 #endif
   // need enough to start R, with some head room
   maxMemory = std::max(32 * Mega, maxMemory);

   // call the memory.limit function
   maxMemory = maxMemory / Mega;
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
   // save callbacks for delegation
   s_callbacks = callbacks;

   // no signal handlers (see comment in REmbeddedPosix.cpp for rationale)
   R_SignalHandlers = 0;

   // set start time
   ::R_setStartTime();

   // setup params structure
   structRstart rp;
   Rstart pRP = &rp;
   ::R_DefParams(pRP);

   // set paths (copy to new string so we can provide char*)
   std::string* pRHome = new std::string(rHome.absolutePath());
   std::string* pUserHome = new std::string(userHome.absolutePath());
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

Error completeEmbeddedRInitialization()
{
   // set memory limit
   setMemoryLimit();

   // use IE proxy settings
   Error error = r::exec::executeString("suppressWarnings(utils::setInternet2())");
   if (error)
      LOG_ERROR(error);

   // from InitEd in edit.c
   EdFileUsed = 0;
   DefaultFileName = R_tmpnam("Redit", R_TempDir);

   using boost::bind;
   using namespace r::function_hook ;
   ExecBlock block ;
   block.addFunctions()
      (bind(registerReplaceHook, "loadhistory", loadHistoryHook, (CCODE*)NULL))
      (bind(registerReplaceHook, "savehistory", saveHistoryHook,(CCODE*)NULL))
      (bind(registerReplaceHook, "addhistory", addHistoryHook,(CCODE*)NULL))
      (bind(registerReplaceHook, "file.show", fileShowHook, (CCODE*)NULL))
      (bind(registerReplaceHook, "edit", editHook, (CCODE*)NULL))
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



