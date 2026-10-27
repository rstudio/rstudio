/*
 * StringUtils.hpp
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

#ifndef CORE_STRING_UTILS_HPP
#define CORE_STRING_UTILS_HPP

#include <boost/type_traits.hpp>

#include <cctype>
#include <cstdio>
#include <cwctype>

#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/Utility.hpp>
#include <shared_core/system/Win32StringUtils.hpp>

#include <core/collection/Position.hpp>

namespace rstudio {
namespace core {
namespace string_utils {

enum LineEnding {
   LineEndingWindows = 0,
   LineEndingPosix = 1,
   LineEndingNative = 2,
   LineEndingPassthrough = 3
};

enum StdinLines {
   StdinMultiLine,
   StdinSingleLine
};

class Contains
{
public:
   Contains(const std::string& needle)
      : needle_(needle)
   {
   }

   bool operator()(const std::string& haystack)
   {
      return haystack.find(needle_) != std::string::npos;
   }

private:
   std::string needle_;
};

bool hasSubstringAtOffset(
      const std::string& string,
      const std::string& substring,
      std::size_t offset = 0);

bool hasTruthyValue(const std::string& string);
bool hasFalsyValue(const std::string& string);

bool isTruthy(const std::string& string,
              bool valueIfEmpty = false);

bool isSubsequence(std::string const& self,
                   std::string const& other);

bool isSubsequence(std::string const& self,
                   std::string const& other,
                   bool caseInsensitive);

bool isSubsequence(std::string const& self,
                   std::string const& other,
                   std::string::size_type other_n,
                   bool caseInsensitive);

bool isSubsequence(std::string const& self,
                   std::string const& other,
                   std::string::size_type other_n);

std::vector<int> subsequenceIndices(std::string const& sequence,
                                    std::string const& query);

bool subsequenceIndices(std::string const& sequence,
                        std::string const& query,
                        std::vector<int> *pIndices);

std::string getExtension(std::string const& str);

std::string utf8ToSystem(const std::string& str,
                         bool escapeInvalidChars=false);

// Convert to an explicit code page rather than the one implied by the C
// runtime's LC_CTYPE locale. Callers converting text for R should pass R's
// localeCP (see r::util::utf82rconsole): the C runtime locale can be changed
// out from under R, at which point it no longer describes the encoding R
// expects. The code page is ignored on POSIX, where UTF-8 is assumed.
std::string utf8ToCodepage(const std::string& str,
                           int codepage,
                           bool escapeInvalidChars=false);

std::string systemToUtf8(const std::string& str);
std::string systemToUtf8(const std::string& str, int codepage);

// Returns the length (0-3) of a truncated UTF-8 sequence at the end of the
// string -- a lead byte promising more continuation bytes than remain. Returns
// 0 for text ending on a character boundary or in an invalid sequence.
std::size_t utf8IncompleteSuffixLength(const std::string& str);

// Returns the length (1-4) of the UTF-8 sequence starting at pos, or 0 if the
// bytes there aren't a strictly valid sequence. Null characters and positions
// past the end of the string are also considered invalid.
std::size_t utf8SequenceLength(const std::string& str, std::size_t pos);

// True if the whole string is valid UTF-8 per RFC 3629 and contains no null bytes.
bool isValidUtf8(const std::string& str);

// Removes embedded NULs from a UTF-8 string and replaces any invalid bytes
// with U+FFFD, preserving the rest of the string.
std::string cleanUtf8String(const std::string& value);

std::string toLower(const std::string& str);
std::string toUpper(const std::string& str);
std::string textToHtml(const std::string& str);

std::string htmlEscape(const std::string& str, bool isAttributeValue = false);
std::string htmlUnescape(const std::string& str);
std::string htmlToText(const std::string& str);
std::string jsLiteralEscape(const std::string& str);
std::string jsonLiteralEscape(const std::string& str);
std::string jsonLiteralUnescape(const std::string& str);
std::string jsonHtmlEscape(const std::string& str);
std::string singleQuotedStrEscape(const std::string& str);

// Removes the common leading indentation shared by all non-blank lines, and
// drops a single leading and trailing blank line. This allows multi-line raw
// string literals to be indented to match the surrounding source code while
// still producing cleanly-formatted (un-indented) output -- useful for
// embedding inline R code, scripts, or other text. For example:
//
//    std::string code = heredoc(R"(
//       library(knitr)
//       knit('example.Rmd')
//    )");
//
// produces "library(knitr)\nknit('example.Rmd')".
std::string heredoc(const std::string& str);

Error jsonLiteralUnescape(const std::string& string,
                          std::string* pEscaped);

void convertLineEndings(std::string* str, LineEnding type);

bool detectLineEndings(const FilePath& filePath, LineEnding* pType);

std::string filterControlChars(const std::string& str);

bool parseVersion(const std::string& str, uint64_t* pVersion);

// Given a string and an offset into the string, return the corresponding line/column
// position, taking varying line ending possibilities into account.
collection::Position offsetToPosition(const std::string& str, std::size_t offset);

template<typename T>
T hashStable(const std::string& str)
{
   T hash = 5381;

   std::string::const_iterator it;
   for (it = str.begin(); it != str.end(); it++)
      hash = ((hash << 5) + hash) + *it;

   return hash;
}

// Moves the begin pointer the specified number of UTF8
// characters.
template <typename InputIterator>
Error utf8Advance(InputIterator begin,
                  size_t chars,
                  InputIterator end,
                  InputIterator* pResult)
{
   using namespace boost::system;

   for ( ; begin != end && chars > 0; --chars)
   {
      unsigned char byte = static_cast<unsigned char>(*(begin++));
      if (byte > 0x7F)
      {
         // Outside the legal range of UTF-8 bytes
         if (byte > 0xF4)
            return systemError(errc::illegal_byte_sequence, ERROR_LOCATION);

         // Don't count the first bit, which represents the
         // initial character.
         byte <<= 1;

         // Found a continuation byte (10...) where none was expected!
         if (byte < 0x80)
            return systemError(errc::illegal_byte_sequence, ERROR_LOCATION);

         // OK, now eat the appropriate number of continuation bytes,
         // by counting the number of leading bits that are on.
         for ( ; byte >= 0x80 && begin != end; byte <<= 1)
         {
            unsigned char contByte = static_cast<unsigned char>(*(begin++));

            // Expected a continuation byte but didn't get one!
            if ((contByte & 0xC0) != 0x80)
               return systemError(errc::illegal_byte_sequence, ERROR_LOCATION);
         }

         // Premature EOF--malformed UTF-8
         if (byte >= 0x80)
            return systemError(errc::illegal_byte_sequence, ERROR_LOCATION);
      }
   }

   // Premature EOF
   if (chars != 0)
      return systemError(errc::invalid_argument, ERROR_LOCATION);

   *pResult = begin;
   return Success();
}

template <typename Iterator, typename InputIterator>
Error utf8Clean(Iterator begin,
                InputIterator end,
                unsigned char replacementChar)

{
   using namespace boost::system;

   if (replacementChar > 0x7F)
      return systemError(errc::invalid_argument,
                         "Invalid UTF-8 replacement character",
                         ERROR_LOCATION);

   Error error;

   while (begin != end)
   {
      error = utf8Advance(begin, 1, end, &begin);
      if (error)
      {
         *begin = replacementChar;
      }
   }

   return Success();
}


template <typename InputIterator>
Error utf8Distance(InputIterator begin,
                   InputIterator end,
                   size_t* pResult)
{
   *pResult = 0;
   Error error;
   while (begin != end)
   {
      error = utf8Advance(begin, 1, end, &begin);
      if (error)
         return error;
      (*pResult)++;
   }

   return Success();
}

inline bool stringNotEmpty(const std::string& str)
{
   return !str.empty();
}

bool trimLeadingLines(int maxLines, std::string* pLines);

void stripQuotes(std::string* pStr);
std::string strippedOfQuotes(const std::string& str);

std::string strippedOfBackQuotes(const std::string& string);

std::size_t countNewlines(const std::wstring& string);
std::size_t countNewlines(const std::string& string);

std::size_t countNewlines(std::string::iterator begin,
                          std::string::iterator end);

std::size_t countNewlines(std::wstring::iterator begin,
                          std::wstring::iterator end);

std::wstring::const_iterator countNewlines(std::wstring::const_iterator begin,
                                           std::wstring::const_iterator end,
                                           std::size_t* pCount);

bool isPrefixOf(const std::string& self, const std::string& prefix);

template <typename StringType>
inline StringType substring(const StringType& string,
                            std::size_t startPos)
{
   return string.substr(startPos);
}

template <typename StringType>
inline StringType substring(const StringType& string,
                            std::size_t startPos,
                            std::size_t endPos)
{
   // An inverted range returns the rest of the string. That is what the
   // unguarded 'endPos - startPos' returned once its count wrapped, so this
   // keeps the result for existing callers without relying on wraparound.
   if (endPos < startPos)
      return string.substr(startPos);

   return string.substr(startPos, endPos - startPos);
}

namespace detail {

template <typename StringType>
inline StringType trimWhitespace(const StringType& string,
                                 const StringType& whitespace)
{
   std::size_t start = string.find_first_not_of(whitespace);
   if (start == StringType::npos)
      return StringType();

   std::size_t end = string.find_last_not_of(whitespace);
   return substring(string, start, end + 1);
}

} // namespace detail

inline std::string trimWhitespace(const std::string& string)
{
   return detail::trimWhitespace(string, std::string(" \t\n\r\f\v"));
}

inline std::wstring trimWhitespace(const std::wstring& string)
{
   return detail::trimWhitespace(string, std::wstring(L" \t\n\r\f\v"));
}

std::string makeRandomByteString(std::size_t n);
bool extractCommentHeader(const std::string& contents,
                          const std::string& reCommentPrefix,
                          std::string* pHeader);

std::string getCommonPrefix(const std::string& code);

std::string extractIndent(const std::string& line);

std::string formatDouble(const double d, const int precision);

std::string sprintf(const char* fmt, ...);

// consume all of standard input and return it as a string; used for processes
// that consume standard input. reads a maximum of 1mb by default.
std::string consumeStdin(StdinLines kind, unsigned maxChars = 1048576);

} // namespace string_utils

} // namespace core
} // namespace rstudio

#endif // CORE_STRING_UTILS_HPP
