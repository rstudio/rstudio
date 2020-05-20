/*
 * REmbedded.hpp
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

#ifndef R_EMBEDDED_HPP
#define R_EMBEDDED_HPP

#include <string>

typedef struct SEXPREC *SEXP;

#include <R_ext/Boolean.h>
#include <R_ext/RStartup.h>

#ifdef _WIN32
typedef char CONSOLE_BUFFER_CHAR;
#else
typedef unsigned char CONSOLE_BUFFER_CHAR;
#endif

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {

struct Callbacks
{
   void (*showMessage)(const char*);
   int (*readConsole)(const char *, CONSOLE_BUFFER_CHAR*, int, int);
   void (*writeConsoleEx)(const char *, int, int);
   int (*editFile)(const char*);
   void (*busy)(int);
   int (*chooseFile)(int, char *, int);
   int (*showFiles)(int, const char **, const char **, const char *,
                    Rboolean, const char *);
   void (*loadhistory)(SEXP, SEXP, SEXP, SEXP);
   void (*savehistory)(SEXP, SEXP, SEXP, SEXP);
   void (*addhistory)(SEXP, SEXP, SEXP, SEXP);
   void (*suicide)(const char*);
   void (*cleanUp)(SA_TYPE, int, int);
};

struct InternalCallbacks
{
   InternalCallbacks() : suicide(nullptr), cleanUp(nullptr) {}
   void (*suicide)(const char*);
   void (*cleanUp)(SA_TYPE, int, int);
};

void runEmbeddedR(const core::FilePath& rHome,
                  const core::FilePath& userHome,
                  bool quiet,
                  bool loadInitFile,
                  SA_TYPE defaultSaveAction,
                  const Callbacks& callbacks,
                  InternalCallbacks* pInternal);

core::Error completeEmbeddedRInitialization(bool useInternet2);

} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_EMBEDDED_HPP

