/*
 * SessionMainStub.cpp
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

#include <session/SessionMain.hpp>

// Thin launcher stub for the R session.
//
// On Windows the session code lives in a shared library (rsession-shared.dll),
// and this stub is compiled into each launcher executable (rsession.exe and
// rsession-utf8.exe). Those executables are identical apart from the manifest
// embedded via their .rc file. The embedded manifest fixes the process active
// code page at startup, and that setting applies process-wide -- including to
// the code running in the DLL -- so the two stubs give the shared library
// the correct (legacy or UTF-8) code page. On other platforms all of the
// session code is linked into a single executable and this simply forwards to
// rsessionMain(). See SessionMain.hpp for details.
int main(int argc, char* const argv[])
{
   return rsessionMain(argc, argv);
}
