/*
 * StringUtils.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <core/StringUtils.hpp>

#include <algorithm>
#include <map>
#include <ostream>

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/case_conv.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/regex.hpp>

#include <core/Log.hpp>
#include <core/SafeConvert.hpp>
#include <core/json/Json.hpp>

#ifdef _WIN32
#include <windows.h>
#include <winnls.h>
#endif

#ifndef CP_ACP
# define CP_ACP 0
#endif

namespace rstudio {
namespace core {
namespace string_utils {

bool isSubsequence(std::string const& self,
                   std::string const& other,
                   std::string::size_type other_n)
{
   std::string::size_type self_n = self.length();

   if (other_n == 0)
      return true;

   if (other_n > other.length())
      other_n = other.length();

   if (other_n > self_n)
      return false;

   std::string::size_type self_idx = 0;
   std::string::size_type other_idx = 0;

   while (self_idx < self_n)
   {
      char selfChar = self[self_idx];
      char otherChar = other[other_idx];

      if (otherChar == selfChar)
      {
         ++other_idx;
         if (other_idx == other_n)
         {
            return true;
         }
      }
      ++self_idx;
   }
   return false;
}


bool isSubsequence(std::string const& self,
                   std::string const& other,
                   std::string::size_type other_n,
                   bool caseInsensitive)
{
   return caseInsensitive ?
            isSubsequence(boost::algorithm::to_lower_copy(self),
                          boost::algorithm::to_lower_copy(other),
                          other_n) :
            isSubsequence(self, other, other_n)
            ;
}

bool isSubsequence(std::string const& self,
                   std::string const& other)
{
   return isSubsequence(self, other, other.length());
}

bool isSubsequence(std::string const& self,
                   std::string const& other,
                   bool caseInsensitive)
{
   return isSubsequence(self, other, other.length(), caseInsensitive);
}

std::vector<int> subsequenceIndices(std::string const& sequence,
                                    std::string const& query)
{
   std::string::size_type querySize = query.length();
   std::vector<int> result;
   result.reserve(querySize);

   std::string::size_type prevMatchIndex = -1;
   for (std::string::size_type i = 0; i < querySize; i++)
   {
      std::string::size_type index = sequence.find(query[i], prevMatchIndex + 1);
      if (index == std::string::npos)
         continue;
      
      result.push_back(index);
      prevMatchIndex = index;
   }
   
   return result;
}

bool subsequenceIndices(std::string const& sequence,
                        std::string const& query,
                        std::vector<int> *pIndices)
{
   pIndices->clear();
   pIndices->reserve(query.length());
   
   int query_n = query.length();
   int prevMatchIndex = -1;
   
   for (int i = 0; i < query_n; i++)
   {
      int index = sequence.find(query[i], prevMatchIndex + 1);
      if (index == -1)
         return false;
      
      pIndices->push_back(index);
      prevMatchIndex = index;
   }
   
   return true;
}

std::string getExtension(std::string const& x)
{
   std::size_t lastDotIndex = x.rfind('.');
   if (lastDotIndex != std::string::npos)
      return x.substr(lastDotIndex);
   else
      return std::string();
}

void convertLineEndings(std::string* pStr, LineEnding type)
{
   std::string replacement;
   switch (type)
   {
   case LineEndingWindows:
      replacement = "\r\n";
      break;
   case LineEndingPosix:
      replacement = "\n";
      break;
   case LineEndingNative:
#if _WIN32
      replacement = "\r\n";
#else
      replacement = "\n";
#endif
      break;
   case LineEndingPassthrough:
   default:
      return;
   }

   *pStr = boost::regex_replace(*pStr, boost::regex("\\r?\\n|\\r|\\xE2\\x80[\\xA8\\xA9]"), replacement);
}

bool detectLineEndings(const FilePath& filePath, LineEnding* pType)
{
   if (!filePath.exists())
      return false;

   boost::shared_ptr<std::istream> pIfs;
   Error error = filePath.open_r(&pIfs);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // read file character-by-character using a streambuf
   try
   {
      std::istream::sentry se(*pIfs, true);
      std::streambuf* sb = pIfs->rdbuf();

      while(true)
      {
         int ch = sb->sbumpc();

         if (ch == '\n')
         {
            // using posix line endings
            *pType = string_utils::LineEndingPosix;
            return true;
         }
         else if (ch == '\r' && sb->sgetc() == '\n')
         {
            // using windows line endings
            *pType = string_utils::LineEndingWindows;
            return true;
         }
         else if (ch == EOF)
         {
            break;
         }
         else if (pIfs->fail())
         {
            LOG_WARNING_MESSAGE("I/O Error reading file " +
                                filePath.absolutePath());
            break;
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   // no detection possible (perhaps the file is empty or has only one line)
   return false;
}

std::string utf8ToSystem(const std::string& str,
                         bool escapeInvalidChars)
{
   if (str.empty())
      return std::string();

#ifdef _WIN32
   wchar_t wide[str.length() + 1];
   int chars = ::MultiByteToWideChar(CP_UTF8, 0, str.c_str(), -1, wide, sizeof(wide));
   if (chars < 0)
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
      return str;
   }

   std::ostringstream output;
   char mbbuf[10];
   // Only go up to chars - 1 because last char is \0
   for (int i = 0; i < chars - 1; i++)
   {
      int mbc = wctomb(mbbuf, wide[i]);
      if (mbc == -1)
      {
         if (escapeInvalidChars)
            output << "\\u{" << std::hex << wide[i] << "}";
         else
            output << "?"; // TODO: Use GetCPInfo()
      }
      else
         output.write(mbbuf, mbc);
   }
   return output.str();
#else
   // Assumes that UTF8 is the locale on POSIX
   return str;
#endif
}

std::string systemToUtf8(const std::string& str, int codepage)
{
   if (str.empty())
      return std::string();

#ifdef _WIN32
   wchar_t wide[str.length() + 1];
   int chars = ::MultiByteToWideChar(codepage, 0, str.c_str(), str.length(), wide, sizeof(wide));
   if (chars < 0)
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
      return str;
   }

   int bytesRequired = ::WideCharToMultiByte(CP_UTF8, 0, wide, chars,
                                             NULL, 0,
                                             NULL, NULL);
   if (bytesRequired == 0)
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
      return str;
   }
   std::vector<char> buf(bytesRequired, 0);
   int bytesWritten = ::WideCharToMultiByte(CP_UTF8, 0, wide, chars,
                                            &(buf[0]), buf.size(),
                                            NULL, NULL);
   return std::string(buf.begin(), buf.end());
#else
   return str;
#endif
}

std::string systemToUtf8(const std::string& str)
{
   return systemToUtf8(str, CP_ACP);
}

std::string toUpper(const std::string& str)
{
   std::string upper = str;
   std::transform(upper.begin(), upper.end(), upper.begin(), ::toupper);
   return upper;
}

std::string toLower(const std::string& str)
{
   std::string lower = str;
   std::transform(lower.begin(), lower.end(), lower.begin(), ::tolower);
   return lower;
}
   
std::string textToHtml(const std::string& str)
{
   std::string html = str;
   boost::replace_all(html, "&", "&amp;");
   boost::replace_all(html, "<", "&lt;");
   return html;
}

namespace {
std::string escape(std::string specialChars,
                   const std::map<char, std::string>& replacements,
                   std::string str)
{
   std::string result;
   result.reserve(static_cast<size_t>(str.size() * 1.2));

   size_t tail = 0;
   for (size_t head = 0;
        head < str.size()
           && str.npos != (head = str.find_first_of(specialChars, head));
        tail = ++head)
   {
      if (tail < head)
         result.append(str, tail, head - tail);

      result.append(replacements.find(str.at(head))->second);
   }

   if (tail < str.size())
      result.append(str, tail, std::string::npos);

   return result;

}
} // anonymous namespace

std::string htmlEscape(const std::string& str, bool isAttributeValue)
{
   std::string escapes = isAttributeValue ?
                         "<>&'\"/\r\n" :
                         "<>&'\"/" ;

   std::map<char, std::string> subs;
   subs['<'] = "&lt;";
   subs['>'] = "&gt;";
   subs['&'] = "&amp;";
   subs['\''] = "&#x27;";
   subs['"'] = "&quot;";
   subs['/'] = "&#x2F;";
   if (isAttributeValue)
   {
      subs['\r'] = "&#13;";
      subs['\n'] = "&#10;";
   }

   return escape(escapes, subs, str);
}

std::string jsLiteralEscape(const std::string& str)
{
   std::string escapes = "\\'\"\r\n<";

   std::map<char, std::string> subs;
   subs['\\'] = "\\\\";
   subs['\''] = "\\'";
   subs['"'] = "\\\"";
   subs['\r'] = "\\r";
   subs['\n'] = "\\n";
   subs['<'] = "\074";

   return escape(escapes, subs, str);
}

std::string jsonLiteralEscape(const std::string& str)
{
   std::string escapes = "\\\"\r\n";

   std::map<char, std::string> subs;
   subs['\\'] = "\\\\";
   subs['"'] = "\\\"";
   subs['\r'] = "\\r";
   subs['\n'] = "\\n";

   return escape(escapes, subs, str);
}
// The str that is passed in should INCLUDE the " " around the value!
// (Sorry this is inconsistent with jsonLiteralEscape, but it's more efficient
// than adding double-quotes in this function)
std::string jsonLiteralUnescape(const std::string& str)
{
   json::Value value;
   if (!json::parse(str, &value) || !json::isType<std::string>(value))
   {
      LOG_ERROR_MESSAGE("Failed to unescape JS literal");
      return str;
   }

   return value.get_str();
}

std::string singleQuotedStrEscape(const std::string& str)
{
   std::string escapes = "'\\";

   std::map<char, std::string> subs;
   subs['\\'] = "\\\\";
   subs['\''] = "\\'";

   return escape(escapes, subs, str);
}

std::string filterControlChars(const std::string& str)
{
   // Delete control chars, which can cause errors in JSON parsing (especially
   // \0003)
   return boost::regex_replace(str,
                               boost::regex("[\\0000-\\0010\\0016-\\0037]+"),
                               "");
}

namespace {

std::vector<bool> initLookupTable(wchar_t ranges[][2], size_t rangeCount)
{
   std::vector<bool> results(0xFFFF, false);
   for (size_t i = 0; i < rangeCount; i++)
   {
      for (wchar_t j = ranges[i][0]; j <= ranges[i][1]; j++)
         results[j] = true;
   }
   return results;
}

// See https://gist.github.com/1110629 for range generating script

std::vector<bool> initAlnumLookupTable()
{
   wchar_t ranges[][2] = {
      {0x30, 0x39}, {0x41, 0x5A}, {0x61, 0x7A}, {0xAA, 0xAA}, {0xB5, 0xB5}, {0xBA, 0xBA}, {0xC0, 0xD6}, {0xD8, 0xF6}, {0xF8, 0x2C1}, {0x2C6, 0x2D1}, {0x2E0, 0x2E4}, {0x2EC, 0x2EC}, {0x2EE, 0x2EE}, {0x370, 0x374}, {0x376, 0x37D}, {0x386, 0x386}, {0x388, 0x3F5}, {0x3F7, 0x481}, {0x48A, 0x559}, {0x561, 0x587}, {0x5D0, 0x5F2}, {0x620, 0x64A}, {0x660, 0x669}, {0x66E, 0x66F}, {0x671, 0x6D3}, {0x6D5, 0x6D5}, {0x6E5, 0x6E6}, {0x6EE, 0x6FC}, {0x6FF, 0x6FF}, {0x710, 0x710}, {0x712, 0x72F}, {0x74D, 0x7A5}, {0x7B1, 0x7EA}, {0x7F4, 0x7F5}, {0x7FA, 0x815}, {0x81A, 0x81A}, {0x824, 0x824}, {0x828, 0x828}, {0x840, 0x858}, {0x904, 0x939}, {0x93D, 0x93D}, {0x950, 0x950}, {0x958, 0x961}, {0x966, 0x96F}, {0x971, 0x97F}, {0x985, 0x9B9}, {0x9BD, 0x9BD}, {0x9CE, 0x9CE}, {0x9DC, 0x9E1}, {0x9E6, 0x9F1}, {0xA05, 0xA39}, {0xA59, 0xA6F}, {0xA72, 0xA74}, {0xA85, 0xAB9}, {0xABD, 0xABD}, {0xAD0, 0xAE1}, {0xAE6, 0xAEF}, {0xB05, 0xB39}, {0xB3D, 0xB3D}, {0xB5C, 0xB61}, {0xB66, 0xB6F}, {0xB71, 0xB71}, {0xB83, 0xBB9}, {0xBD0, 0xBD0}, {0xBE6, 0xBEF}, {0xC05, 0xC3D}, {0xC58, 0xC61}, {0xC66, 0xC6F}, {0xC85, 0xCB9}, {0xCBD, 0xCBD}, {0xCDE, 0xCE1}, {0xCE6, 0xCF2}, {0xD05, 0xD3D}, {0xD4E, 0xD4E}, {0xD60, 0xD61}, {0xD66, 0xD6F}, {0xD7A, 0xD7F}, {0xD85, 0xDC6}, {0xE01, 0xE30}, {0xE32, 0xE33}, {0xE40, 0xE46}, {0xE50, 0xE59}, {0xE81, 0xEB0}, {0xEB2, 0xEB3}, {0xEBD, 0xEC6}, {0xED0, 0xF00}, {0xF20, 0xF29}, {0xF40, 0xF6C}, {0xF88, 0xF8C}, {0x1000, 0x102A}, {0x103F, 0x1049}, {0x1050, 0x1055}, {0x105A, 0x105D}, {0x1061, 0x1061}, {0x1065, 0x1066}, {0x106E, 0x1070}, {0x1075, 0x1081}, {0x108E, 0x108E}, {0x1090, 0x1099}, {0x10A0, 0x10FA}, {0x10FC, 0x135A}, {0x1380, 0x138F}, {0x13A0, 0x13F4}, {0x1401, 0x166C}, {0x166F, 0x167F}, {0x1681, 0x169A}, {0x16A0, 0x16EA}, {0x16EE, 0x1711}, {0x1720, 0x1731}, {0x1740, 0x1751}, {0x1760, 0x1770}, {0x1780, 0x17B3}, {0x17D7, 0x17D7}, {0x17DC, 0x17DC}, {0x17E0, 0x17E9}, {0x1810, 0x18A8}, {0x18AA, 0x191C}, {0x1946, 0x19AB}, {0x19C1, 0x19C7}, {0x19D0, 0x19D9}, {0x1A00, 0x1A16}, {0x1A20, 0x1A54}, {0x1A80, 0x1A99}, {0x1AA7, 0x1AA7}, {0x1B05, 0x1B33}, {0x1B45, 0x1B59}, {0x1B83, 0x1BA0}, {0x1BAE, 0x1BE5}, {0x1C00, 0x1C23}, {0x1C40, 0x1C7D}, {0x1CE9, 0x1CEC}, {0x1CEE, 0x1CF1}, {0x1D00, 0x1DBF}, {0x1E00, 0x1FBC}, {0x1FBE, 0x1FBE}, {0x1FC2, 0x1FCC}, {0x1FD0, 0x1FDB}, {0x1FE0, 0x1FEC}, {0x1FF2, 0x1FFC}, {0x2071, 0x2071}, {0x207F, 0x207F}, {0x2090, 0x209C}, {0x2102, 0x2102}, {0x2107, 0x2107}, {0x210A, 0x2113}, {0x2115, 0x2115}, {0x2119, 0x211D}, {0x2124, 0x2124}, {0x2126, 0x2126}, {0x2128, 0x2128}, {0x212A, 0x212D}, {0x212F, 0x2139}, {0x213C, 0x213F}, {0x2145, 0x2149}, {0x214E, 0x214E}, {0x2160, 0x2188}, {0x2C00, 0x2CE4}, {0x2CEB, 0x2CEE}, {0x2D00, 0x2D6F}, {0x2D80, 0x2DDE}, {0x2E2F, 0x2E2F}, {0x3005, 0x3007}, {0x3021, 0x3029}, {0x3031, 0x3035}, {0x3038, 0x303C}, {0x3041, 0x3096}, {0x309D, 0x309F}, {0x30A1, 0x30FA}, {0x30FC, 0x318E}, {0x31A0, 0x31BA}, {0x31F0, 0x31FF}, {0x3400, 0x4DB5}, {0x4E00, 0xA48C}, {0xA4D0, 0xA4FD}, {0xA500, 0xA60C}, {0xA610, 0xA66E}, {0xA67F, 0xA6EF}, {0xA717, 0xA71F}, {0xA722, 0xA788}, {0xA78B, 0xA801}, {0xA803, 0xA805}, {0xA807, 0xA80A}, {0xA80C, 0xA822}, {0xA840, 0xA873}, {0xA882, 0xA8B3}, {0xA8D0, 0xA8D9}, {0xA8F2, 0xA8F7}, {0xA8FB, 0xA925}, {0xA930, 0xA946}, {0xA960, 0xA97C}, {0xA984, 0xA9B2}, {0xA9CF, 0xA9D9}, {0xAA00, 0xAA28}, {0xAA40, 0xAA42}, {0xAA44, 0xAA4B}, {0xAA50, 0xAA59}, {0xAA60, 0xAA76}, {0xAA7A, 0xAA7A}, {0xAA80, 0xAAAF}, {0xAAB1, 0xAAB1}, {0xAAB5, 0xAAB6}, {0xAAB9, 0xAABD}, {0xAAC0, 0xAAC0}, {0xAAC2, 0xAADD}, {0xAB01, 0xABE2}, {0xABF0, 0xD7FB}, {0xF900, 0xFB1D}, {0xFB1F, 0xFB28}, {0xFB2A, 0xFBB1}, {0xFBD3, 0xFD3D}, {0xFD50, 0xFDFB}, {0xFE70, 0xFEFC}, {0xFF10, 0xFF19}, {0xFF21, 0xFF3A}, {0xFF41, 0xFF5A}, {0xFF66, 0xFFDC}
   };

   return initLookupTable(ranges, sizeof(ranges) / sizeof(ranges[0]));
}

std::vector<bool> initAlphaLookupTable()
{
   wchar_t ranges[][2] = {
      {0x41, 0x5A}, {0x61, 0x7A}, {0xAA, 0xAA}, {0xB5, 0xB5}, {0xBA, 0xBA}, {0xC0, 0xD6}, {0xD8, 0xF6}, {0xF8, 0x2C1}, {0x2C6, 0x2D1}, {0x2E0, 0x2E4}, {0x2EC, 0x2EC}, {0x2EE, 0x2EE}, {0x370, 0x374}, {0x376, 0x37D}, {0x386, 0x386}, {0x388, 0x3F5}, {0x3F7, 0x481}, {0x48A, 0x559}, {0x561, 0x587}, {0x5D0, 0x5F2}, {0x620, 0x64A}, {0x66E, 0x66F}, {0x671, 0x6D3}, {0x6D5, 0x6D5}, {0x6E5, 0x6E6}, {0x6EE, 0x6EF}, {0x6FA, 0x6FC}, {0x6FF, 0x6FF}, {0x710, 0x710}, {0x712, 0x72F}, {0x74D, 0x7A5}, {0x7B1, 0x7B1}, {0x7CA, 0x7EA}, {0x7F4, 0x7F5}, {0x7FA, 0x815}, {0x81A, 0x81A}, {0x824, 0x824}, {0x828, 0x828}, {0x840, 0x858}, {0x904, 0x939}, {0x93D, 0x93D}, {0x950, 0x950}, {0x958, 0x961}, {0x971, 0x97F}, {0x985, 0x9B9}, {0x9BD, 0x9BD}, {0x9CE, 0x9CE}, {0x9DC, 0x9E1}, {0x9F0, 0x9F1}, {0xA05, 0xA39}, {0xA59, 0xA5E}, {0xA72, 0xA74}, {0xA85, 0xAB9}, {0xABD, 0xABD}, {0xAD0, 0xAE1}, {0xB05, 0xB39}, {0xB3D, 0xB3D}, {0xB5C, 0xB61}, {0xB71, 0xB71}, {0xB83, 0xBB9}, {0xBD0, 0xBD0}, {0xC05, 0xC3D}, {0xC58, 0xC61}, {0xC85, 0xCB9}, {0xCBD, 0xCBD}, {0xCDE, 0xCE1}, {0xCF1, 0xCF2}, {0xD05, 0xD3D}, {0xD4E, 0xD4E}, {0xD60, 0xD61}, {0xD7A, 0xD7F}, {0xD85, 0xDC6}, {0xE01, 0xE30}, {0xE32, 0xE33}, {0xE40, 0xE46}, {0xE81, 0xEB0}, {0xEB2, 0xEB3}, {0xEBD, 0xEC6}, {0xEDC, 0xF00}, {0xF40, 0xF6C}, {0xF88, 0xF8C}, {0x1000, 0x102A}, {0x103F, 0x103F}, {0x1050, 0x1055}, {0x105A, 0x105D}, {0x1061, 0x1061}, {0x1065, 0x1066}, {0x106E, 0x1070}, {0x1075, 0x1081}, {0x108E, 0x108E}, {0x10A0, 0x10FA}, {0x10FC, 0x135A}, {0x1380, 0x138F}, {0x13A0, 0x13F4}, {0x1401, 0x166C}, {0x166F, 0x167F}, {0x1681, 0x169A}, {0x16A0, 0x16EA}, {0x16EE, 0x1711}, {0x1720, 0x1731}, {0x1740, 0x1751}, {0x1760, 0x1770}, {0x1780, 0x17B3}, {0x17D7, 0x17D7}, {0x17DC, 0x17DC}, {0x1820, 0x18A8}, {0x18AA, 0x191C}, {0x1950, 0x19AB}, {0x19C1, 0x19C7}, {0x1A00, 0x1A16}, {0x1A20, 0x1A54}, {0x1AA7, 0x1AA7}, {0x1B05, 0x1B33}, {0x1B45, 0x1B4B}, {0x1B83, 0x1BA0}, {0x1BAE, 0x1BAF}, {0x1BC0, 0x1BE5}, {0x1C00, 0x1C23}, {0x1C4D, 0x1C4F}, {0x1C5A, 0x1C7D}, {0x1CE9, 0x1CEC}, {0x1CEE, 0x1CF1}, {0x1D00, 0x1DBF}, {0x1E00, 0x1FBC}, {0x1FBE, 0x1FBE}, {0x1FC2, 0x1FCC}, {0x1FD0, 0x1FDB}, {0x1FE0, 0x1FEC}, {0x1FF2, 0x1FFC}, {0x2071, 0x2071}, {0x207F, 0x207F}, {0x2090, 0x209C}, {0x2102, 0x2102}, {0x2107, 0x2107}, {0x210A, 0x2113}, {0x2115, 0x2115}, {0x2119, 0x211D}, {0x2124, 0x2124}, {0x2126, 0x2126}, {0x2128, 0x2128}, {0x212A, 0x212D}, {0x212F, 0x2139}, {0x213C, 0x213F}, {0x2145, 0x2149}, {0x214E, 0x214E}, {0x2160, 0x2188}, {0x2C00, 0x2CE4}, {0x2CEB, 0x2CEE}, {0x2D00, 0x2D6F}, {0x2D80, 0x2DDE}, {0x2E2F, 0x2E2F}, {0x3005, 0x3007}, {0x3021, 0x3029}, {0x3031, 0x3035}, {0x3038, 0x303C}, {0x3041, 0x3096}, {0x309D, 0x309F}, {0x30A1, 0x30FA}, {0x30FC, 0x318E}, {0x31A0, 0x31BA}, {0x31F0, 0x31FF}, {0x3400, 0x4DB5}, {0x4E00, 0xA48C}, {0xA4D0, 0xA4FD}, {0xA500, 0xA60C}, {0xA610, 0xA61F}, {0xA62A, 0xA66E}, {0xA67F, 0xA6EF}, {0xA717, 0xA71F}, {0xA722, 0xA788}, {0xA78B, 0xA801}, {0xA803, 0xA805}, {0xA807, 0xA80A}, {0xA80C, 0xA822}, {0xA840, 0xA873}, {0xA882, 0xA8B3}, {0xA8F2, 0xA8F7}, {0xA8FB, 0xA8FB}, {0xA90A, 0xA925}, {0xA930, 0xA946}, {0xA960, 0xA97C}, {0xA984, 0xA9B2}, {0xA9CF, 0xA9CF}, {0xAA00, 0xAA28}, {0xAA40, 0xAA42}, {0xAA44, 0xAA4B}, {0xAA60, 0xAA76}, {0xAA7A, 0xAA7A}, {0xAA80, 0xAAAF}, {0xAAB1, 0xAAB1}, {0xAAB5, 0xAAB6}, {0xAAB9, 0xAABD}, {0xAAC0, 0xAAC0}, {0xAAC2, 0xAADD}, {0xAB01, 0xABE2}, {0xAC00, 0xD7FB}, {0xF900, 0xFB1D}, {0xFB1F, 0xFB28}, {0xFB2A, 0xFBB1}, {0xFBD3, 0xFD3D}, {0xFD50, 0xFDFB}, {0xFE70, 0xFEFC}, {0xFF21, 0xFF3A}, {0xFF41, 0xFF5A}, {0xFF66, 0xFFDC}
   };

   return initLookupTable(ranges, sizeof(ranges) / sizeof(ranges[0]));
}

} // anonymous namespace

bool isalpha(wchar_t c)
{
   static std::vector<bool> lookup = initAlphaLookupTable();
   if (c >= 0xFFFF)
      return false; // This function only supports BMP
   return lookup.at(c);
}

bool isalnum(wchar_t c)
{
   static std::vector<bool> lookup;
   if (lookup.empty())
      lookup = initAlnumLookupTable();

   if (c >= 0xFFFF)
      return false; // This function only supports BMP
   return lookup.at(c);
}

bool parseVersion(const std::string& str, uint64_t* pVersion)
{
   uint64_t version = 0;

   std::vector<std::string> chunks;
   boost::algorithm::split(chunks, str, boost::algorithm::is_any_of("."));

   if (chunks.empty())
      return false;

   for (size_t i = 0; i < chunks.size() && i < 4; i++)
   {
      uint16_t value = core::safe_convert::stringTo<uint16_t>(
            chunks[i], std::numeric_limits<uint16_t>::max());
      if (value == std::numeric_limits<uint16_t>::max())
         return false;
      version += static_cast<uint64_t>(value) << ((3-i) * 16);
   }
   if (pVersion)
      *pVersion = version;
   return true;
}

bool trimLeadingLines(int maxLines, std::string* pLines)
{
   bool didTrim = false;
   if (pLines->length() > static_cast<unsigned int>(maxLines*2))
   {
      int lineCount = 0;
      std::string::const_iterator begin = pLines->begin();
      std::string::iterator pos = pLines->end();
      while (--pos >= begin)
      {
         if (*pos == '\n')
         {
            if (++lineCount > maxLines)
            {
               pLines->erase(pLines->begin(), pos);
               didTrim = true;
               break;
            }
         }
      }
   }
   return didTrim;
}

std::string strippedOfBackQuotes(const std::string& string)
{
   if (string.length() < 2)
      return string;
   
   std::size_t startIndex = 0;
   std::size_t n = string.length();
   std::size_t endIndex = n;
   
   startIndex += string[0] == '`';
   endIndex   -= string[n - 1] == '`';
   
   return string.substr(startIndex, endIndex - startIndex);
}

void stripQuotes(std::string* pStr)
{
   if (pStr->length() > 0 && (pStr->at(0) == '\'' || pStr->at(0) == '"'))
      *pStr = pStr->substr(1);

   int len = pStr->length();

   if (len > 0 && (pStr->at(len-1) == '\'' || pStr->at(len-1) == '"'))
      *pStr = pStr->substr(0, len -1);
}

std::string strippedOfQuotes(const std::string& string)
{
   std::string::size_type n = string.length();
   if (n < 2) return string;
   
   char first = string[0];
   char last  = string[n - 1];
   
   if ((first == '\'' && last == '\'') ||
       (first == '"' && last == '"') |\
       (first == '`' && last == '`'))
   {
      return string.substr(1, n - 2);
   }
   
   return string;
}

template <typename Iter, typename U>
Iter countNewlinesImpl(Iter begin,
                       Iter end,
                       const U& CR,
                       const U& LF,
                       std::size_t* pNewlineCount)
{
   std::size_t newlineCount = 0;
   Iter it = begin;
   
   Iter lastNewline = end;
   
   for (; it != end; ++it)
   {
      // Detect '\r\n'
      if (*it == CR)
      {
         if (it + 1 != end &&
             *(it + 1) == LF)
         {
            lastNewline = it;
            ++it;
            ++newlineCount;
         }
      }
      
      // Detect '\n'
      if (*it == LF)
      {
         lastNewline = it;
         ++newlineCount;
      }
   }
   
   *pNewlineCount = newlineCount;
   return lastNewline;
}

std::size_t countNewlines(const std::wstring& string)
{
   std::size_t count = 0;
   countNewlinesImpl(string.begin(), string.end(), L'\r', L'\n', &count);
   return count;
}

std::size_t countNewlines(const std::string& string)
{
   std::size_t count = 0;
   countNewlinesImpl(string.begin(), string.end(), '\r', '\n', &count);
   return count;
}

std::size_t countNewlines(std::string::iterator begin,
                          std::string::iterator end)
{
   std::size_t count = 0;
   countNewlinesImpl(begin, end, '\r', '\n', &count);
   return count;
}

std::size_t countNewlines(std::wstring::iterator begin,
                          std::wstring::iterator end)
{
   std::size_t count = 0;
   countNewlinesImpl(begin, end, '\r', '\n', &count);
   return count;
}

std::wstring::const_iterator countNewlines(std::wstring::const_iterator begin,
                                           std::wstring::const_iterator end,
                                           std::size_t* pCount)
{
   return countNewlinesImpl(begin, end, '\r', '\n', pCount);
}

bool isPrefixOf(const std::string& self, const std::string& prefix)
{
   return boost::algorithm::starts_with(self, prefix);
}

std::string makeRandomByteString(std::size_t n)
{
   std::string result;
   result.resize(n);
   for (std::size_t i = 0; i < n; ++i)
      result[i] = (unsigned char) (::rand() % UCHAR_MAX);
   return result;
}

} // namespace string_utils
} // namespace core 
} // namespace rstudio



