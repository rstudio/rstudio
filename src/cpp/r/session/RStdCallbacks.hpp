/*
 * RStdCallbacks.hpp
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

#ifndef R_SESSION_STD_CALLBACKS_HPP
#define R_SESSION_STD_CALLBACKS_HPP

namespace rstudio {
namespace r {
namespace session {

struct RCallbacks;
struct InternalCallbacks;

// callback management
void setRCallbacks(const RCallbacks& callbacks);
RCallbacks& rCallbacks();
InternalCallbacks* stdInternalCallbacks();

// individual callbacks from R
int RReadConsole(const char *pmt, CONSOLE_BUFFER_CHAR* buf, int buflen, int hist);
void RShowMessage(const char* msg);
void RWriteConsoleEx (const char *buf, int buflen, int otype);
int REditFile(const char* file);
void RBusy(int which);
int RChooseFile (int newFile, char *buf, int len);
int RShowFiles (int nfile, 
                const char **file, 
                const char **headers, 
                const char *wtitle, 
                Rboolean del, 
                const char *pager);
void Rloadhistory(SEXP call, SEXP op, SEXP args, SEXP env);
void Rsavehistory(SEXP call, SEXP op, SEXP args, SEXP env);
void Raddhistory(SEXP call, SEXP op, SEXP args, SEXP env);
void RSuicide(const char* s);
void RCleanUp(SA_TYPE saveact, int status, int runLast);

// exported utilities
void rSuicide(const std::string& msg);

bool imageIsDirty();
void setImageDirty(bool imageDirty);

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_STD_CALLBACKS_HPP 
