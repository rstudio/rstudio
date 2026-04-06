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

// This module replaces R's internal locale2charset(), which is declared in
// Defn.h (not part of R's public API) and is being removed in a future R
// release. Unlike locale2charset(), this implementation:
//
// - Only queries the *current* LC_CTYPE locale, so it does not need R's
//   large locale-name-to-charset lookup table (which maps arbitrary locale
//   strings like "ja_JP" to charset names).
//
// - Does not replicate R's full codepage-to-charset mapping table by
//   default. The raw codepage names (e.g. "CP1252") are valid for iconv
//   and HTTP headers. A separate canonicalize step maps these to display
//   names (e.g. "ISO-8859-1") for the encoding dialog.

#include <core/system/Locale.hpp>

#include <clocale>
#include <cstdlib>
#include <cstring>
#include <map>
#include <string>

#ifndef _WIN32
# include <langinfo.h>
#endif

#include <core/Log.hpp>
#include <core/Thread.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

#ifdef _WIN32

// On Windows, parse the codepage from setlocale(LC_CTYPE, NULL) and
// return a charset name. The locale string typically looks like
// "English_United States.1252" (or "en-US.UTF-8" with the UTF-8
// codepage beta enabled).
std::string currentCharsetImpl()
{
   const char* locale = std::setlocale(LC_CTYPE, nullptr);
   if (locale == nullptr)
   {
      LOG_WARNING_MESSAGE("setlocale(LC_CTYPE) returned NULL; defaulting to ASCII");
      return "ASCII";
   }

   // check for ASCII-like locales
   for (auto variant : { "C", "POSIX" })
      if (!std::strcmp(locale, variant))
         return "ASCII";

   // find the codepage after the last '.'
   const char* dot = std::strrchr(locale, '.');
   if (!dot || !dot[1])
   {
      LOG_WARNING_MESSAGE("No codepage in locale '" + std::string(locale) + "'; defaulting to ASCII");
      return "ASCII";
   }

   const char* enc = dot + 1;

   // check for UTF-8 variants
   for (auto variant : { "utf-8", "utf8" })
      if (!_stricmp(enc, variant))
         return "UTF-8";

   // numeric codepage -- the Windows CRT always returns a numeric
   // codepage here in practice, but if it's somehow a named encoding,
   // pass it through as-is rather than collapsing to ASCII
   int cp = std::atoi(enc);
   if (cp == 0)
      return std::string(enc);
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
   {
      LOG_WARNING_MESSAGE("setlocale(LC_CTYPE) returned NULL; defaulting to ASCII");
      return "ASCII";
   }

   // check for ASCII-like locales
   for (auto variant : { "C", "POSIX" })
      if (!std::strcmp(locale, variant))
         return "ASCII";

   const char* codeset = nl_langinfo(CODESET);

   if (!codeset || !codeset[0])
   {
#ifdef __APPLE__
      // On macOS, nl_langinfo(CODESET) returns "" when LC_CTYPE is set to a
      // locale without an explicit encoding suffix (e.g. "en_US" vs "en_US.UTF-8").
      // This is a long-standing Darwin quirk also worked around by Python (bpo-42236)
      // and GNU Gnulib (localcharset.c). On macOS, all such locales are UTF-8 in
      // practice, so we infer UTF-8 when MB_CUR_MAX >= 4.
      // https://github.com/apple/cups/issues/856
      if (MB_CUR_MAX >= 4)
      {
         LOG_WARNING_MESSAGE("nl_langinfo(CODESET) empty for locale '" +
                             std::string(locale) + "'; assuming UTF-8 based on MB_CUR_MAX");
         return "UTF-8";
      }
      else
      {
         LOG_WARNING_MESSAGE("nl_langinfo(CODESET) empty for locale '" +
                             std::string(locale) + "'; defaulting to ASCII");
         return "ASCII";
      }
#else
      LOG_WARNING_MESSAGE("nl_langinfo(CODESET) empty for locale '" +
                          std::string(locale) + "'; defaulting to ASCII");
      return "ASCII";
#endif
   }

   // glibc returns "ANSI_X3.4-1968" for ASCII locales
   if (!std::strcmp(codeset, "ANSI_X3.4-1968"))
      return "ASCII";

   // normalize "utf8" (no hyphen) to "UTF-8" for consistency
   if (!strcasecmp(codeset, "utf8"))
      return "UTF-8";

   return std::string(codeset);
}

#endif // _WIN32

// Map Windows codepage names to canonical names matching the common
// encoding list in SessionSource.R (.rs.iconvcommon). This ensures
// the encoding dialog can match the system encoding against its list.
std::string canonicalize(const std::string& charset)
{
   static const std::map<std::string, std::string> s_cpMap = {
      { "CP932",   "SHIFT-JIS" },
      { "CP936",   "GB2312" },
      { "CP950",   "BIG5" },
      { "CP1252",  "ISO-8859-1" },
      { "CP28591", "ISO-8859-1" },
      { "CP28592", "ISO-8859-2" },
      { "CP28597", "ISO-8859-7" },
      { "CP54936", "GB18030" },
      { "CP65001", "UTF-8" },
   };

   auto it = s_cpMap.find(charset);
   if (it != s_cpMap.end())
      return it->second;

   return charset;
}

} // anonymous namespace

std::string currentCharset(bool canonicalize_)
{
   ASSERT_MAIN_THREAD("setlocale() is not thread-safe");
   std::string charset = currentCharsetImpl();
   return canonicalize_ ? canonicalize(charset) : charset;
}

} // namespace system
} // namespace core
} // namespace rstudio
