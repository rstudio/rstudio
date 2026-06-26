/*
 * SessionMain.hpp
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

#ifndef SESSION_MAIN_HPP
#define SESSION_MAIN_HPP

// On Windows, the bulk of the session code -- including the rsessionMain()
// entry point below -- is built into a shared library (rsession.dll)
// shared by two thin launcher executables, rsession.exe and rsession-utf8.exe.
// Those executables are identical apart from the manifest embedded via their
// .rc file: rsession-utf8.exe selects a UTF-8 active code page (for UCRT builds
// of R 4.2.0 and newer) while rsession.exe uses the system default. The active
// code page is fixed at process startup from the executable's manifest and
// applies process-wide, including to code running in rsession.dll, so the two
// stubs are sufficient to give the shared code the correct code page.
//
// RSESSION_MAIN_API exports rsessionMain() from that DLL and imports it into
// the stubs. It expands to nothing on other platforms, where all of the
// session code is linked into a single rsession executable.
#if defined(_WIN32) && defined(RSESSION_BUILDING_SHARED)
# define RSESSION_MAIN_API __declspec(dllexport)
#elif defined(_WIN32)
# define RSESSION_MAIN_API __declspec(dllimport)
#else
# define RSESSION_MAIN_API
#endif

// Real entry point for the R session; invoked by main() in SessionMainStub.cpp.
RSESSION_MAIN_API int rsessionMain(int argc, char* const argv[]);

namespace rstudio {
namespace session {

void terminateAllChildProcesses();

void controlledExit(int statusCode);

void exitEarly(int statusCode);

}
}

#endif // SESSION_MAIN_HPP

