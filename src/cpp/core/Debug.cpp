/*
 * Debug.cpp
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

#include <core/Debug.hpp>

namespace rstudio {
namespace core {
namespace debug {

#ifdef _WIN32
void logToNotepad(const char* fmt, ...)
{
   // try to find Notepad window
   HWND hNotepad = ::FindWindow("Notepad", NULL);
   if (hNotepad == NULL)
      return;

   // try to find Notepad's edit surface
   HWND hEdit = ::FindWindowEx(hNotepad, NULL, "EDIT", NULL);
   if (hEdit == NULL)
      return;

   // generate log message
   char buffer[512];
   va_list args;
   va_start(args, fmt);
   vsprintf(buffer, fmt, args);
   va_end(args);

   // append newline + null terminator
   strcat(buffer, "\r\n");

   // send message
   ::SendMessage(hEdit, EM_REPLACESEL, TRUE, (LPARAM) buffer);
}
#endif

} // namespace debug
} // namespace core
} // namespace rstudio


