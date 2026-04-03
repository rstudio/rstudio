/*
 * Locale.cpp
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

#include <core/system/Locale.hpp>

#include <clocale>
#include <cstdlib>
#include <cstring>
#include <string>

#ifndef _WIN32
# include <langinfo.h>
#endif

namespace rstudio {
namespace core {
namespace system {

namespace {

#ifdef _WIN32

// On Windows, parse the codepage from setlocale(LC_CTYPE, NULL) and
// return a charset name. The locale string typically looks like
// "English_United States.1252" or "en-US.UTF-8".
std::string currentCharsetImpl()
{
   const char* locale = std::setlocale(LC_CTYPE, nullptr);
   if (locale == nullptr)
      return "ASCII";
   
   // check for ASCII-like locales
   for (auto variant : { "C", "POSIX" })
      if (!std::strcmp(locale, variant))
         return "ASCII";

   // find the codepage after the last '.'
   const char* dot = std::strrchr(locale, '.');
   if (!dot || !dot[1])
      return "ASCII";

   const char* enc = dot + 1;

   // check for UTF-8 variants
   for (auto variant : { "utf-8", "utf8" })
      if (!_stricmp(enc, variant))
         return "UTF-8";

   // numeric codepage; atoi returns 0 for unrecognized strings,
   // which we treat as ASCII
   int cp = std::atoi(enc);
   if (cp == 0)
      return "ASCII";
   else if (cp == 65001)
      return "UTF-8";
   else
      return "CP" + std::to_string(cp);
}

#else // POSIX

std::string currentCharsetImpl()
{
   const char* locale = std::setlocale(LC_CTYPE, nullptr);
   if (locale == nullptr)
      return "ASCII";
   
   // check for ASCII-like locales
   for (auto variant : { "C", "POSIX" })
      if (!std::strcmp(locale, variant))
         return "ASCII";

   const char* codeset = nl_langinfo(CODESET);

   if (!codeset || !codeset[0])
   {
#ifdef __APPLE__
      // On macOS, nl_langinfo(CODESET) may return "" for UTF-8 locales
      return MB_CUR_MAX >= 4 ? "UTF-8" : "ASCII";
#else
      // Otherwise, assume ASCII
      return "ASCII";
#endif
   }

   // glibc returns "ANSI_X3.4-1968" for ASCII locales
   if (!std::strcmp(codeset, "ANSI_X3.4-1968"))
      return "ASCII";

   return std::string(codeset);
}

#endif // _WIN32

} // anonymous namespace

std::string currentCharset()
{
   return currentCharsetImpl();
}

} // namespace system
} // namespace core
} // namespace rstudio
