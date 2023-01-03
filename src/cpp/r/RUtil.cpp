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

#include <gsl/gsl>

#include <boost/algorithm/string/replace.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Algorithm.hpp>
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

extern "C" {
__declspec(dllimport) unsigned int localeCP;
}

unsigned int s_codepage = 0;

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
   // NOTE: Required on Windows as R links to a static copy
   // of libc and libgcc, and so has a separate environment
   // block from the rsession.exe executable itself.
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

void synchronizeLocale()
{
#ifdef _WIN32

   // bail if the codepages are still in sync
   if (s_codepage == localeCP)
      return;

   // ask R what the current locale is and then update
   std::string rLocale;
   Error error = r::exec::RFunction("base:::Sys.getlocale")
         .addParam("LC_ALL")
         .call(&rLocale);
   if (error)
      LOG_ERROR(error);

   if (!rLocale.empty())
   {
      std::string locale = ::setlocale(LC_ALL, nullptr);
      if (locale != rLocale)
         ::setlocale(LC_ALL, rLocale.c_str());
   }

   // save the updated codepage
   s_codepage = localeCP;

#endif
}

} // namespace util
} // namespace r
} // namespace rstudio



