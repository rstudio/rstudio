/*
 * RInterface.hpp
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

#ifndef R_INTERFACE_HPP
#define R_INTERFACE_HPP

#include <string>

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

typedef struct SEXPREC *SEXP;

#else

#define R_INTERFACE_PTRS 1
#include <Rinterface.h>

#endif

#endif // R_INTERFACE_HPP

