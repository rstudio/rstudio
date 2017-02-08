/*
 * RInterface.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef R_INTERFACE_HPP
#define R_INTERFACE_HPP

#include <string>
#include <setjmp.h>

#ifdef _WIN32

#include <R_ext/Boolean.h>
#include <R_ext/RStartup.h>

extern "C" void R_RestoreGlobalEnvFromFile(const char *, Rboolean);
extern "C" void R_SaveGlobalEnvToFile(const char *);
extern "C" void R_Suicide(const char *);
extern "C" char *R_HomeDir(void);
extern "C" void Rf_jump_to_toplevel(void);
extern "C" void Rf_onintr(void);
#define R_ClearerrConsole void
extern "C" void R_FlushConsole();
extern "C" int R_SignalHandlers;
extern "C" void run_Rmainloop();
extern "C" void Rf_mainloop(void);
extern "C" void* R_GlobalContext;

typedef struct SEXPREC *SEXP;

#else

#define R_INTERFACE_PTRS 1
#include <Rinterface.h>

#endif

typedef struct RCNTXT_34 {
    struct RCNTXT_34 *nextcontext;
    int callflag;
#ifdef _WIN32
    struct
    {
      jmp_buf buf;
      int sigmask;
      int savedmask;
    } cjumpbuf;
#else
    sigjmp_buf cjmpbuf;
#endif
    int cstacktop;
    int evaldepth;
    SEXP promargs;
    SEXP callfun;
    SEXP sysparent;
    SEXP call;
    SEXP cloenv;
    SEXP conexit;
    void (*cend)(void *);
    void *cenddata;
    void *vmax;
    int intsusp;
    int gcenabled;
    int bcintactive;
    SEXP bcbody;
    void *bcpc;
    SEXP handlerstack;
    SEXP restartstack;
    struct RPRSTACK *prstack;
    struct {
       int tag;
       union {
          int ival;
          double dval;
          SEXP sxpval;
       } u;
    } *nodestack;
#ifdef BC_INT_STACK
    IStackval *intstack;
#endif
    SEXP srcref;
    int browserfinish;
    SEXP returnValue;
    struct RCNTXT_34 *jumptarget;
    int jumpmask;
} RCNTXT_34;

typedef struct RCNTXT_33 {
    struct RCNTXT_33 *nextcontext;
    int callflag;
#ifdef _WIN32
    struct
    {
      jmp_buf buf;
      int sigmask;
      int savedmask;
    } cjumpbuf;
#else
    sigjmp_buf cjmpbuf;
#endif
    int cstacktop;
    int evaldepth;
    SEXP promargs;
    SEXP callfun;
    SEXP sysparent;
    SEXP call;
    SEXP cloenv;
    SEXP conexit;
    void (*cend)(void *);
    void *cenddata;
    void *vmax;
    int intsusp;
    int gcenabled;
    SEXP handlerstack;
    SEXP restartstack;
    struct RPRSTACK *prstack;
    SEXP *nodestack;
#ifdef BC_INT_STACK
    IStackval *intstack;
#endif
    SEXP srcref;
    int browserfinish;
    SEXP returnValue;
    struct RCNTXT_33 *jumptarget;
    int jumpmask;
} RCNTXT_33;

typedef struct RCNTXT_32 {
    struct RCNTXT_32 *nextcontext;
    int callflag;
#ifdef _WIN32
    struct
    {
      jmp_buf buf;
      int sigmask;
      int savedmask;
    } cjumpbuf;
#else
    sigjmp_buf cjmpbuf;
#endif
    int cstacktop;
    int evaldepth;
    SEXP promargs;
    SEXP callfun;
    SEXP sysparent;
    SEXP call;
    SEXP cloenv;
    SEXP conexit;
    void (*cend)(void *);
    void *cenddata;
    void *vmax;
    int intsusp;
    SEXP handlerstack;
    SEXP restartstack;
    struct RPRSTACK *prstack;
    SEXP *nodestack;
#ifdef BC_INT_STACK
    IStackval *intstack;
#endif
    SEXP srcref;
} RCNTXT_32;

enum {
    CTXT_TOPLEVEL = 0,
    CTXT_NEXT	  = 1,
    CTXT_BREAK	  = 2,
    CTXT_LOOP	  = 3,
    CTXT_FUNCTION = 4,
    CTXT_CCODE	  = 8,
    CTXT_RETURN	  = 12,
    CTXT_BROWSER  = 16,
    CTXT_GENERIC  = 20,
    CTXT_RESTART  = 32,
    CTXT_BUILTIN  = 64
};

namespace rstudio {
namespace r {

inline void* getGlobalContext()
{
   return R_GlobalContext;
}

} // namespace r
} // namespace rstudio

#endif // R_INTERFACE_HPP

