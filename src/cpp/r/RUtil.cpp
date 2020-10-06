/*
 * RUtil.cpp
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


#include <r/RUtil.hpp>

#include <gsl/gsl>

#include <boost/algorithm/string/replace.hpp>
#include <boost/regex.hpp>

#include <core/Algorithm.hpp>
#include <shared_core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <shared_core/Error.hpp>
#include <core/RegexUtils.hpp>

#include <r/RExec.hpp>

#include <R_ext/Riconv.h>

#ifndef CP_ACP
# define CP_ACP 0
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



