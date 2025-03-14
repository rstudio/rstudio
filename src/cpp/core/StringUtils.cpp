/*
 * StringUtils.cpp
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

#include <core/StringUtils.hpp>

#include <stdarg.h>

#include <algorithm>
#include <cctype>
#include <map>
#include <ostream>

#include <gsl/gsl-lite.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/case_conv.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/regex.hpp>

#include <shared_core/SafeConvert.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Algorithm.hpp>
#include <core/Log.hpp>

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

bool hasSubstringAtOffset(
      const std::string& string,
      const std::string& substring,
      std::size_t offset)
{
   if (substring.size() + offset > string.size())
      return false;
   
   for (std::size_t i = 0, n = substring.size(); i < n; i++)
      if (string[offset + i] != substring[i])
         return false;
   
   return true;
}

bool hasTruthyValue(const std::string& string)
{
   for (const char* value : { "TRUE", "True", "true", "YES", "Yes", "yes", "1" })
      if (string == value)
         return true;
   
   return false;
}

bool hasFalsyValue(const std::string& string)
{
   for (const char* value : { "FALSE", "False", "false", "NO", "No", "no", "0" })
      if (string == value)
         return true;
   
   return false;
}

bool isTruthy(const std::string& string,
              bool valueIfEmpty)
{
   // allow user-configurable behavior for empty strings
   if (string.empty())
      return valueIfEmpty;

   // check for 'falsy' values
   if (hasFalsyValue(string))
      return false;
   
   // assume all other values are 'truthy'
   return true;
}

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
      
      result.push_back(gsl::narrow_cast<int>(index));
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
   
   int query_n = gsl::narrow_cast<int>(query.length());
   int prevMatchIndex = -1;
   
   for (int i = 0; i < query_n; i++)
   {
      int index = gsl::narrow_cast<int>(sequence.find(query[i], prevMatchIndex + 1));
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

   std::shared_ptr<std::istream> pIfs;
   Error error = filePath.openForRead(pIfs);
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
                                   filePath.getAbsolutePath());
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

   std::vector<wchar_t> wide(str.length() + 1);
   int chars = ::MultiByteToWideChar(
            CP_UTF8, 0,
            str.c_str(), -1,
            &wide[0], gsl::narrow_cast<int>(wide.size()));

   if (chars < 0)
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
      return str;
   }

   std::ostringstream output;
   char buffer[16];

   // Only go up to chars - 1 because last char is \0
   for (int i = 0; i < chars - 1; i++)
   {
      int n = wctomb(buffer, wide[i]);

      if (n == -1)
      {
         if (escapeInvalidChars)
         {
            // NOTE: in R, both '\u{1234}' and '\u1234' are valid
            // ways of specifying a unicode literal, but only the
            // latter is accepted by Python, and since the reticulate
            // REPL uses the same conversion routines we prefer the
            // format compatible with both parsers
            output << "\\u" << std::hex << wide[i];
         }
         else
         {
            output << "?"; // TODO: Use GetCPInfo()
         }
      }
      else
      {
         output.write(buffer, n);
      }
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
   std::vector<wchar_t> wide(str.length() + 1);
   int chars = ::MultiByteToWideChar(codepage,
                                     0,
                                     str.c_str(),
                                     gsl::narrow_cast<int>(str.length()),
                                     &wide[0],
                                     gsl::narrow_cast<int>(wide.size()));
   if (chars < 0)
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
      return str;
   }

   int bytesRequired = ::WideCharToMultiByte(CP_UTF8, 0, &wide[0], chars,
                                             nullptr, 0,
                                             nullptr, nullptr);
   if (bytesRequired == 0)
   {
      LOG_ERROR(LAST_SYSTEM_ERROR());
      return str;
   }
   std::vector<char> buf(bytesRequired, 0);
   int bytesWritten = ::WideCharToMultiByte(CP_UTF8, 0, &wide[0], chars,
                                            &(buf[0]), static_cast<int>(buf.size()),
                                            nullptr, nullptr);
   return std::string(buf.begin(), buf.end());
#else
   return str;
#endif
}

std::string systemToUtf8(const std::string& str)
{
   return systemToUtf8(str, CP_ACP);
}

// https://en.cppreference.com/w/cpp/string/byte/toupper#Notes
std::string toUpper(const std::string& str)
{
   std::string upper = str;
   std::transform(upper.begin(), upper.end(), upper.begin(), [](unsigned char ch)
   {
      return std::toupper(ch);
   });
   return upper;
}

// https://en.cppreference.com/w/cpp/string/byte/tolower#Notes
std::string toLower(const std::string& str)
{
   std::string lower = str;
   std::transform(lower.begin(), lower.end(), lower.begin(), [](unsigned char ch)
   {
      return std::tolower(ch);
   });
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
                         "<>&'\"/";

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
   subs['<'] = "\\074";

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

// Escapes possible HTML inside JSON strings. Generally not necessary unless there is a chance
// the JSON could be misconstrued by the browser as HTML.
std::string jsonHtmlEscape(const std::string& str)
{
   std::string escapes = "<>&";
   std::map<char, std::string> subs;

   subs['<'] = "\\u003c"; // JSON unicode character encoding
   subs['>'] = "\\u003e"; // JSON unicode character encoding
   subs['&'] = "\\u0026"; // JSON unicode character encoding

   return escape(escapes, subs, str);
}

// The str that is passed in should INCLUDE the " " around the value!
// (Sorry this is inconsistent with jsonLiteralEscape, but it's more efficient
// than adding double-quotes in this function)
std::string jsonLiteralUnescape(const std::string& str)
{
   json::Value value;
   if (value.parse(str) || !json::isType<std::string>(value))
   {
      LOG_ERROR_MESSAGE("Failed to unescape JS literal");
      return str;
   }

   return value.getString();
}

Error jsonLiteralUnescape(const std::string& str, std::string* pEscaped)
{
   json::Value value;
   
   Error error = value.parse(str);
   if (error)
      return error;
   
   if (!json::isType<std::string>(value))
      return Error(boost::system::errc::invalid_argument, ERROR_LOCATION);
   
   *pEscaped = value.getString();
   return Success();
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

bool parseVersion(const std::string& str, uint64_t* pVersion)
{
   uint64_t version = 0;

   std::vector<std::string> chunks;
   boost::algorithm::split(chunks, str, boost::algorithm::is_any_of("."));

   if (chunks.empty())
      return false;

   for (size_t i = 0; i < chunks.size() && i < 4; i++)
   {
      boost::optional<uint16_t> value = core::safe_convert::stringTo<uint16_t>(chunks[i]);
      if (!value)
         return false;
      version += static_cast<uint64_t>(value.get()) << ((3-i) * 16);
   }
   if (pVersion)
      *pVersion = version;
   return true;
}

bool trimLeadingLines(int maxLines, std::string* pLines)
{
   bool didTrim = false;
   if (pLines->length() > static_cast<unsigned int>(maxLines * 2))
   {
      int lineCount = 0;
      std::string::const_iterator begin = pLines->begin();
      std::string::iterator pos = pLines->end();

      for (;;)
      {
         --pos;

         if (*pos == '\n')
         {
            if (++lineCount > maxLines)
            {
               pLines->erase(pLines->begin(), pos);
               didTrim = true;
               break;
            }
         }

         if (pos == begin)
            break;
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

   auto len = pStr->length();

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
            continue;
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

bool extractCommentHeader(const std::string& contents,
                          const std::string& reCommentPrefix,
                          std::string* pHeader)
{
   // construct newline-based token iterator
   boost::regex reNewline("(?:\\r?\\n|$)");
   boost::sregex_token_iterator it(
            contents.begin(),
            contents.end(),
            reNewline,
            -1);
   boost::sregex_token_iterator end;
   
   // first, skip blank lines
   boost::regex reWhitespace("^\\s*$");
   while (it != end)
   {
      if (boost::regex_match(it->begin(), it->end(), reWhitespace))
      {
         ++it;
         continue;
      }
      
      break;
   }
   
   // if we're at the end now, bail
   if (it == end)
      return false;
   
   // check to see if we landed on our comment prefix and
   // quit early if we haven't
   boost::regex rePrefix(reCommentPrefix);
   if (!boost::regex_search(it->begin(), it->end(), rePrefix))
      return false;
   
   // we have a prefix: start iterating and extracting these
   for (; it != end; ++it)
   {
      boost::smatch match;
      if (!boost::regex_search(it->begin(), it->end(), match, rePrefix))
      {
         // this is no longer a commented line; time to go home
         break;
      }
         
      // extract the line (sans prefix)
      std::string line(it->begin() + match.length(), it->end());
      pHeader->append(line + "\n");
   }
   
   // report success to the user
   return true;
}

std::string extractIndent(const std::string& line)
{
   auto index = line.find_first_not_of(" \t");
   if (index == std::string::npos)
      return std::string();
   return line.substr(0, index);
}

std::string formatDouble(const double d, const int precision)
{
   std::stringstream out;
   out.precision(precision);
   out << std::fixed << d;
   return out.str();
}

std::string sprintf(const char* fmt, ...)
{
   // note: the semantics for vsnprintf are slightly awkward... when vsnprintf
   // is called with a null pointer, it returns the number of characters that
   // would be written, not including the null terminator. however, when called
   // with a buffer, vsnprintf will write a maximum of n - 1 characters, and
   // will always write a null terminator at the end! so we need to ensure we
   // add 1 character to the size returned by vsnprintf(nullptr) to get the
   // full size of the C string we want to generate
   std::size_t n = 0;
   {
      va_list args;
      va_start(args, fmt);
      n = std::vsnprintf(nullptr, 0, fmt, args);
      va_end(args);
   }
   
   if (n == 0)
   {
      return std::string();
   }
   
   // allocate buffer of required size
   // (include space for null pointer)
   std::vector<char> buffer(n + 1);
   
   // write formatted string to buffer
   {
      va_list args;
      va_start(args, fmt);
      std::vsnprintf(&buffer[0], buffer.size(), fmt, args);
      va_end(args);
   }
   
   // return as string
   return std::string(&buffer[0], n);
}

// return all of stdin as a string
std::string consumeStdin(StdinLines kind, unsigned maxChars)
{
   std::string input;
   int ch;
   for (unsigned i = 0; i < maxChars; i++)
   {
      ch = ::fgetc(stdin);
      if (feof(stdin))
      {
         // reached end of standard input
         break;
      }
      if (kind == StdinSingleLine && ch == '\n')
      {
         // reached end of single line and that's all we wanted
         break;
      }
      if (ferror(stdin))
      {
         // something bad happened
         LOG_WARNING_MESSAGE("Error reading from stdin stream!");
         break;
      }

      // all is well, add the character and advance
      input.push_back(ch);

      // warn if we are about to truncate
      if (i == (maxChars - 1))
      {
         LOG_WARNING_MESSAGE("Gave up reading stdin after consuming " +
            safe_convert::numberToString(maxChars) + " characters");
      }
   }

   return input;
}

} // namespace string_utils
} // namespace core 
} // namespace rstudio



