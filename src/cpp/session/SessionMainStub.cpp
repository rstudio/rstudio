/*
 * SessionMainStub.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

// Thin launcher stub for the R session. On Windows the session code lives in a
// shared library (rsession.dll) loaded by each launcher executable (rsession.exe
// and rsession-utf8.exe); on other platforms it is linked into a single
// executable. Either way this simply forwards to rsessionMain(). See
// SessionMain.hpp for details.
int main(int argc, char* const argv[])
{
   return rsessionMain(argc, argv);
}
