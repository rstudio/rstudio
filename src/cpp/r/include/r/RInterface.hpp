/*
 * RInterface.hpp
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

#ifndef R_INTERFACE_HPP
#define R_INTERFACE_HPP

#include <string>

#ifdef _WIN32

#include <R_ext/Boolean.h>
#include <R_ext/RStartup.h>

extern "C" {

void R_RestoreGlobalEnvFromFile(const char *, Rboolean);
void R_SaveGlobalEnvToFile(const char *);
void R_Suicide(const char *);
char *R_HomeDir(void);
void Rf_onintr(void);
#define R_ClearerrConsole void
void R_FlushConsole();
void run_Rmainloop();
void Rf_mainloop(void);

extern __declspec(dllimport) int R_SignalHandlers;
extern __declspec(dllimport) void* R_GlobalContext;

}

typedef struct SEXPREC *SEXP;

#else

#define R_INTERFACE_PTRS 1
#include <Rinterface.h>

#endif

#endif // R_INTERFACE_HPP

