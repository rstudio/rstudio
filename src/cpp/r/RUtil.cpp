/*
 * RUtil.cpp
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


#include <r/RUtil.hpp>

#include <gsl/gsl-lite.hpp>

#include <boost/algorithm/string/replace.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Algorithm.hpp>
#include <core/Log.hpp>
#include <core/RegexUtils.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>

#include <r/RExec.hpp>

#include <R_ext/Riconv.h>

#ifndef CP_ACP
# define CP_ACP 0
#endif

#ifdef _WIN32
# define kPathSeparator ";"
#else
# define kPathSeparator ":"
#endif

#ifdef _WIN32

#include <Windows.h>
#include <clocale>
#include <cwchar>
#include <cwctype>
#include <limits>

#include <shared_core/system/Win32StringUtils.hpp>

extern "C" {
__declspec(dllimport) unsigned int localeCP;
}

// The last code page copied from R into our separate C runtime for R < 4.2.
// Start with a sentinel so that localeCP == 0 is synchronized on first use.
unsigned int s_codepage = std::numeric_limits<unsigned int>::max();

// The session locale, as last seen while R and the C runtime still agreed on
// it. Kept as a wide string because it is the narrow form of a locale name
// that fails to round-trip -- see synchronizeLocale().
std::wstring s_locale;

// whether the current divergence has already been reported
bool s_reportedLocaleDrift = false;

#endif

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace util {
namespace {

bool versionTest(const std::string& comparator, const std::string& version)
{
   std::string versionTest("getRversion() " + comparator + " \"" + version + "\"");
   bool hasVersion = false;
   Error error = r::exec::evaluateString(versionTest, &hasVersion);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   else
   {
      return hasVersion;
   }
}

} // anonymous namespace

void setenv(const std::string& key, const std::string& value)
{
   core::system::setenv(key, value);

#ifdef _WIN32
   // NOTE: Sys.getenv reads the environment via R's C runtime, which keeps
   // its own copy of the environment. core::system::setenv writes through
   // both the Win32 environment block and our C runtime, which suffices for
   // UCRT builds of R (R >= 4.2, which share our C runtime), but R built
   // against msvcrt has a separate copy that only R itself can update.
   Error error = r::exec::RFunction("base:::Sys.setenv")
         .addParam(key, value)
         .call();

   if (error)
      LOG_ERROR(error);
#endif
}

std::string getenv(const std::string& key)
{
   std::string value;
   Error error = r::exec::RFunction("base:::Sys.getenv")
         .addParam(key)
         .call(&value);

   if (error)
      LOG_ERROR(error);

   return value;
}

namespace {

void modifySystemPath(const std::string& pathEntry, bool prepend)
{
   std::string oldPath = getenv("PATH");
   std::string newPath = prepend
         ? fmt::format("{}{}{}", pathEntry, kPathSeparator, oldPath)
         : fmt::format("{}{}{}", oldPath, kPathSeparator, pathEntry);

#ifdef _WIN32
   std::replace(newPath.begin(), newPath.end(), '/', '\\');
#endif

   boost::regex reDuplicateSeparators(kPathSeparator "+");
   newPath = boost::regex_replace(newPath, reDuplicateSeparators, kPathSeparator);

   setenv("PATH", newPath);
}

} // end anonymous namespace

void appendToSystemPath(const std::string& pathEntry)
{
   modifySystemPath(pathEntry, false);
}

void appendToSystemPath(const FilePath& pathEntry)
{
   std::string path = string_utils::utf8ToSystem(pathEntry.getAbsolutePath());
   modifySystemPath(path, false);
}

void prependToSystemPath(const std::string& pathEntry)
{
   modifySystemPath(pathEntry, true);
}

void prependToSystemPath(const FilePath& pathEntry)
{
   std::string path = string_utils::utf8ToSystem(pathEntry.getAbsolutePath());
   modifySystemPath(path, true);
}

std::string expandFileName(const std::string& name)
{
   return std::string(R_ExpandFileName(name.c_str()));
}

std::string fixPath(const std::string& path)
{
   // R sometimes gives us a path a double slashes in it ("//"). Eliminate them.
   std::string fixedPath(path);
   boost::algorithm::replace_all(fixedPath, "//", "/");
   return fixedPath;
}

bool hasRequiredVersion(const std::string& version)
{
   return versionTest(">=", version);
}

bool hasExactVersion(const std::string& version)
{
   return versionTest("==", version);
}

bool hasCapability(const std::string& capability)
{
   bool hasCap = false;
   Error error = r::exec::RFunction("capabilities", capability).call(&hasCap);
   if (error)
      LOG_ERROR(error);
   return hasCap;
}

std::string rconsole2utf8(const std::string& encoded)
{
#ifndef _WIN32
   return encoded;
#else
   unsigned int codepage = localeCP;

   // NOTE: On Windows with GUIs, when R attempts to write text to
   // the console, it will surround UTF-8 text with 3-byte escapes:
   //
   //    \002\377\376 <text> \003\377\376
   //
   // strangely, we see these escapes around text that is not UTF-8
   // encoded but rather is encoded according to the active locale.
   // extract those pieces of text (discarding the escapes) and
   // convert to UTF-8. (still not exactly sure what the cause of this
   // behavior is; perhaps there is an extra UTF-8 <-> system conversion
   // happening somewhere in the pipeline?)
   std::string output;
   std::string::const_iterator pos = encoded.begin();
   boost::smatch m;
   boost::regex utf8("\x02\xFF\xFE(.*?)(\x03\xFF\xFE)");
   while (pos != encoded.end() && regex_utils::search(pos, encoded.end(), m, utf8))
   {
      if (pos < m[0].first)
         output.append(string_utils::systemToUtf8(std::string(pos, m[0].first), codepage));
      output.append(m[1].first, m[1].second);
      pos = m[0].second;
   }
   if (pos != encoded.end())
      output.append(string_utils::systemToUtf8(std::string(pos, encoded.end()), codepage));

   return output;
#endif
}

std::string utf82rconsole(const std::string& utf8, bool escapeInvalidChars)
{
#ifndef _WIN32
   return string_utils::utf8ToSystem(utf8, escapeInvalidChars);
#else
   // Convert for the encoding R believes it is using rather than the one the
   // C runtime's LC_CTYPE locale implies. The two normally agree, but the
   // locale can be moved behind R's back (see synchronizeLocale), and text
   // handed to R has to be encoded the way R is going to read it.
   //
   // localeCP is 0 in the "C" locale, and for the locale names R cannot read
   // a code page from; defer to the C runtime in those cases.
   if (localeCP != 0)
      return string_utils::utf8ToCodepage(
               utf8, static_cast<int>(localeCP), escapeInvalidChars);

   return string_utils::utf8ToSystem(utf8, escapeInvalidChars);
#endif
}

namespace {

core::Error iconvstrImpl(const std::string& value,
                         const std::string& from,
                         const std::string& to,
                         bool allowSubstitution,
                         std::string* pResult)
{
   std::vector<char> output;
   output.reserve(value.length());

   void* handle = ::Riconv_open(to.c_str(), from.c_str());
   if (handle == (void*)(-1))
      return systemError(R_ERRNO, ERROR_LOCATION);

   const char* pIn = value.data();
   size_t inBytes = value.size();

   char buffer[256];
   while (inBytes > 0)
   {
      const char* pInOrig = pIn;
      char* pOut = buffer;
      size_t outBytes = sizeof(buffer);

      size_t result = ::Riconv(handle, &pIn, &inBytes, &pOut, &outBytes);
      if (buffer != pOut)
         output.insert(output.end(), buffer, pOut);

      if (result == (size_t)(-1))
      {
         if ((R_ERRNO == EILSEQ || R_ERRNO == EINVAL) && allowSubstitution)
         {
            output.push_back('?');
            pIn++;
            inBytes--;
         }
         else if (R_ERRNO == E2BIG && pInOrig != pIn)
         {
            continue;
         }
         else
         {
            ::Riconv_close(handle);
            Error error = systemError(R_ERRNO, ERROR_LOCATION);
            error.addProperty("str", value);
            error.addProperty("len", gsl::narrow_cast<int>(value.length()));
            error.addProperty("from", from);
            error.addProperty("to", to);
            return error;
         }
      }
   }
   ::Riconv_close(handle);

   *pResult = std::string(output.begin(), output.end());
   return Success();
}

} // end anonymous namespace

core::Error nativeToUtf8(const std::string& value,
                         bool allowSubstitution,
                         std::string *pResult)
{
   return iconvstrImpl(value, "", "UTF-8", allowSubstitution, pResult);
}

core::Error utf8ToNative(const std::string& value,
                         bool allowSubstitution,
                         std::string* pResult)
{
   return iconvstrImpl(value, "UTF-8", "", allowSubstitution, pResult);
}

core::Error iconvstr(const std::string& value,
                     const std::string& from,
                     const std::string& to,
                     bool allowSubstitution,
                     std::string* pResult)
{
   std::string effectiveFrom = from;
   if (effectiveFrom.empty())
      effectiveFrom = "UTF-8";

   std::string effectiveTo = to;
   if (effectiveTo.empty())
      effectiveTo = "UTF-8";

   if (effectiveFrom == effectiveTo)
   {
      *pResult = value;
      return Success();
   }

   return iconvstrImpl(value, from, to, allowSubstitution, pResult);
}


std::set<std::string> makeRKeywords()
{
   std::set<std::string> keywords;
   
   keywords.insert("TRUE");
   keywords.insert("FALSE");
   keywords.insert("NA");
   keywords.insert("NaN");
   keywords.insert("NULL");
   keywords.insert("NA_real_");
   keywords.insert("NA_complex_");
   keywords.insert("NA_integer_");
   keywords.insert("NA_character_");
   keywords.insert("Inf");
   
   keywords.insert("if");
   keywords.insert("else");
   keywords.insert("while");
   keywords.insert("for");
   keywords.insert("in");
   keywords.insert("function");
   keywords.insert("next");
   keywords.insert("break");
   keywords.insert("repeat");
   keywords.insert("...");
   
   return keywords;
}


bool isRKeyword(const std::string& name)
{
   static const std::set<std::string> s_rKeywords = makeRKeywords();
   static const boost::regex s_reDotDotNumbers("\\.\\.[0-9]+");
   return s_rKeywords.count(name) != 0 ||
          regex_utils::textMatches(name, s_reDotDotNumbers, false, false);
}

std::set<std::string> makeWindowsOnlyFunctions()
{
   std::set<std::string> fns;
   
   fns.insert("shell");
   fns.insert("shell.exec");
   fns.insert("Sys.junction");
   
   return fns;
}

bool isWindowsOnlyFunction(const std::string& name)
{
   static const std::set<std::string> s_rWindowsOnly = makeWindowsOnlyFunctions();
   return core::algorithm::contains(s_rWindowsOnly, name);
}

bool isPackageAttached(const std::string& packageName)
{
   SEXP namespaces = R_NilValue;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction("search").call(&namespaces, &protect);
   if (error)
   {
      // not fatal; we'll just presume package is not on the path
      LOG_ERROR(error);
      return false;
   }
   
   std::string fullPackageName = "package:";
   fullPackageName += packageName;
   int len = r::sexp::length(namespaces);
   for (int i = 0; i < len; i++)
   {
      std::string ns = r::sexp::safeAsString(STRING_ELT(namespaces, i), "");
      if (ns == fullPackageName) 
      {
         return true;
      }
   }
   return false;
}

namespace {

#ifdef _WIN32

// Read the code page out of a Windows locale name the same way R computes its
// localeCP in R_check_locale().
bool localeCodepage(const wchar_t* locale, unsigned int* pCodepage)
{
   const wchar_t* suffix = ::wcsrchr(locale, L'.');
   if (suffix != nullptr)
   {
      suffix += 1;

      if (::iswdigit(suffix[0]))
         *pCodepage = static_cast<unsigned int>(::wcstoul(suffix, nullptr, 10));
      else if (::_wcsicmp(suffix, L"UTF-8") == 0 || ::_wcsicmp(suffix, L"UTF8") == 0)
         *pCodepage = 65001;
      else
         *pCodepage = 0;

      return true;
   }

   // R leaves localeCP at 0 for the "C" locale.
   if (::wcscmp(locale, L"C") == 0)
   {
      *pCodepage = 0;
      return true;
   }

   // For a name without a suffix (for example, "en-US"), R asks Windows for
   // the locale's default ANSI code page.
   wchar_t defaultCodepage[6];
   if (::GetLocaleInfoEx(locale,
                         LOCALE_IDEFAULTANSICODEPAGE,
                         defaultCodepage,
                         sizeof(defaultCodepage) / sizeof(defaultCodepage[0])) == 0 ||
       !::iswdigit(defaultCodepage[0]))
   {
      *pCodepage = 0;
   }
   else
   {
      *pCodepage = static_cast<unsigned int>(
               ::wcstoul(defaultCodepage, nullptr, 10));
   }

   return true;
}

bool localeMatchesR()
{
   const wchar_t* pCtype = ::_wsetlocale(LC_CTYPE, nullptr);
   if (pCtype == nullptr)
      return false;

   unsigned int codepage;
   return localeCodepage(pCtype, &codepage) && codepage == localeCP;
}

// R < 4.2 and rsession use different C runtimes. In that case R's locale is
// authoritative and must be pushed into our runtime explicitly.
void synchronizeLocaleFromR()
{
   if (s_codepage == localeCP)
      return;

   std::string rLocale;
   Error error = r::exec::RFunction("base:::Sys.getlocale")
         .addParam("LC_ALL")
         .call(&rLocale);
   if (error)
      LOG_ERROR(error);

   if (!rLocale.empty())
   {
      std::wstring wrLocale = string_utils::utf8ToWide(rLocale);

      const wchar_t* pLocale = ::_wsetlocale(LC_ALL, nullptr);
      std::wstring locale = pLocale != nullptr ? pLocale : L"";

      if (locale != wrLocale && ::_wsetlocale(LC_ALL, wrLocale.c_str()) == nullptr)
      {
         const wchar_t* pCurrent = ::_wsetlocale(LC_ALL, nullptr);
         WLOGF("Failed to synchronize locale: _wsetlocale(LC_ALL, \"{}\") failed; "
               "current locale is \"{}\"",
               rLocale,
               pCurrent != nullptr ? string_utils::wideToUtf8(pCurrent)
                                   : std::string("(unknown)"));
      }
   }

   s_codepage = localeCP;
}

#endif

} // anonymous namespace

void synchronizeLocale()
{
#ifdef _WIN32

   static const bool s_sharedRuntime = versionTest(">=", "4.2.0");
   if (!s_sharedRuntime)
   {
      synchronizeLocaleFromR();
      return;
   }

   // rsession and UCRT builds of R (R >= 4.2) share a single C runtime, so a
   // setlocale() call anywhere in the process changes R's locale as well --
   // silently, without R's cached view of the encoding (localeCP, utf8locale,
   // native_enc, all computed in R_check_locale) being updated to match.
   //
   // Code that saves the locale, switches to "C" for a conversion and then
   // restores it is a ready source of exactly that. The saved name is narrow,
   // and a locale name holding non-ASCII characters -- Turkish_Türkiye.utf8,
   // Norwegian Bokmål_Norway.utf8 -- cannot be parsed back once the process
   // sits in the "C" locale, so the restore fails and the session is left in
   // "C" while R goes on believing it is in a UTF-8 locale. Console input,
   // file listings, and everything else that trusts the C runtime locale then
   // corrupt non-ASCII text.
   // https://github.com/rstudio/rstudio/issues/18139
   //
   // Watch for that divergence on each R busy transition and repair it. R's
   // localeCP is the arbiter: Sys.setlocale() moves R and the C runtime
   // together, so the two disagree only when something moved the locale
   // behind R's back.

   const wchar_t* pCtype = ::_wsetlocale(LC_CTYPE, nullptr);
   if (pCtype == nullptr)
      return;

   // copy the name before any further _wsetlocale() call overwrites the
   // buffer it points into
   std::wstring ctype(pCtype);

   // Record the full locale before trying to interpret LC_CTYPE. Names
   // without an explicit code page (for example, "en-US") are still valid
   // known-good locales and must be available for a later repair.
   const wchar_t* pLocale = ::_wsetlocale(LC_ALL, nullptr);
   std::wstring locale = pLocale != nullptr ? pLocale : L"";

   unsigned int codepage;
   if (!localeCodepage(ctype.c_str(), &codepage))
   {
      if (!locale.empty())
         s_locale = locale;
      return;
   }

   if (codepage == localeCP)
   {
      // R and the C runtime agree -- remember the locale so that it can be
      // put back should something clobber it later
      if (!locale.empty())
         s_locale = locale;

      s_reportedLocaleDrift = false;
      return;
   }

   bool restored = false;

   if (!s_locale.empty())
   {
      ::_wsetlocale(LC_ALL, s_locale.c_str());
      restored = localeMatchesR();
   }

   if (!restored)
   {
      // nothing recorded to restore, so the locale was already wrong the
      // first time we looked; re-adopt the operating system's locale, which
      // is what affected users have been doing by hand with
      // Sys.setlocale("LC_ALL", "")
      ::_wsetlocale(LC_ALL, L"");

      // that overwrote LC_NUMERIC too, which R requires to be "C"
      ::_wsetlocale(LC_NUMERIC, L"C");
      restored = localeMatchesR();
   }

   if (!s_reportedLocaleDrift)
   {
      s_reportedLocaleDrift = true;

      WLOGF("C runtime locale \"{}\" (code page {}) does not match R's code page {}: {}",
            string_utils::wideToUtf8(ctype),
            codepage,
            localeCP,
            restored ? "restored the session locale"
                     : "failed to restore the session locale");
   }

#endif
}

void str(SEXP objectSEXP)
{
   Error error = r::exec::RFunction("utils:::str")
         .addParam(objectSEXP)
         .call();
   
   if (error)
      LOG_ERROR(error);
}

} // namespace util
} // namespace r
} // namespace rstudio


