/* UserPrefValuesNative.cpp
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
 
#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <boost/algorithm/string.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {
namespace {

void setCRANReposOption(const std::string& url, const std::string& secondary)
{
   if (!url.empty())
   {
      Error error = r::exec::RFunction(".rs.setCRANReposFromSettings",
                                       url, secondary).call();
      if (error)
         LOG_ERROR(error);
   }
}

} // anonymous namespace

string_utils::LineEnding UserPrefValuesNative::lineEndings()
{
   std::string pref(lineEndingConversion());

   if (pref == kLineEndingConversionNative)
      return string_utils::LineEndingNative;
   else if (pref == kLineEndingConversionPosix)
      return string_utils::LineEndingPosix;
   else if (pref == kLineEndingConversionWindows)
      return string_utils::LineEndingWindows;
   else if (pref == kLineEndingConversionPassthrough)
      return string_utils::LineEndingPassthrough;

   return string_utils::LineEndingNative;
}

CRANMirror UserPrefValuesNative::getCRANMirror()
{
   // get the settings
   struct CRANMirror mirror;
   json::readObject(cranMirror(), 
         kCranMirrorUrl, mirror.url,
         kCranMirrorName, mirror.name,
         kCranMirrorHost, mirror.host,
         kCranMirrorSecondary, mirror.secondary,
         kCranMirrorCountry, mirror.country);

   // upgrade 1.2 preview builds
   std::vector<std::string> parts;
   boost::split(parts, mirror.url, boost::is_any_of("|"));
   if (parts.size() >= 2)
   {
      mirror.secondary = mirror.url;
      mirror.url = parts.at(1);
   }

   // re-map cran.rstudio.org to cran.rstudio.com
   if (boost::algorithm::starts_with(mirror.url, "http://cran.rstudio.org"))
      mirror.url = "http://cran.rstudio.com/";

   // remap url without trailing slash
   if (!mirror.url.empty() && !boost::algorithm::ends_with(mirror.url, "/"))
      mirror.url += "/";

   return mirror;
}


core::Error UserPrefValuesNative::setCRANMirror(const struct CRANMirror& mirror, 
      bool update)
{
   json::Object obj;
   obj[kCranMirrorName] = mirror.name;
   obj[kCranMirrorHost] = mirror.host;
   obj[kCranMirrorUrl] = mirror.url;
   obj[kCranMirrorSecondary] = mirror.secondary;
   obj[kCranMirrorCountry] = mirror.country;

   // only set the underlying option if it's not empty (some
   // evidence exists that this is possible, it doesn't appear to
   // be possible in the current code however previous releases
   // may have let this in)
   if (!mirror.url.empty() && update)
      setCRANReposOption(mirror.url, mirror.secondary);

   return setCranMirror(obj);
}

console_process::TerminalShell::ShellType UserPrefValuesNative::defaultTerminalShellValue() 
{
   using ShellType = console_process::TerminalShell::ShellType;
#ifdef WIN32
   ShellType type = console_process::TerminalShell::shellTypeFromString(windowsTerminalShell());
#else
   ShellType type = console_process::TerminalShell::shellTypeFromString(posixTerminalShell());
#endif

   // map obsolete 32-bit shell types to their 64-bit equivalents
   if (type == console_process::TerminalShell::ShellType::Cmd32)
      type = console_process::TerminalShell::ShellType::Cmd64;
   else if (type == console_process::TerminalShell::ShellType::PS32)
      type = console_process::TerminalShell::ShellType::PS64;

   return static_cast<console_process::TerminalShell::ShellType>(type);
}

}
}
}
