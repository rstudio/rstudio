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

std::string systemToUtf8(const std::string& str);
std::string systemToUtf8(const std::string& str, int codepage);

std::string toLower(const std::string& str);
std::string toUpper(const std::string& str);
std::string textToHtml(const std::string& str);

std::string htmlEscape(const std::string& str, bool isAttributeValue = false);
std::string jsLiteralEscape(const std::string& str);
std::string jsonLiteralEscape(const std::string& str);
std::string jsonLiteralUnescape(const std::string& str);
std::string jsonHtmlEscape(const std::string& str);
std::string singleQuotedStrEscape(const std::string& str);

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

std::string extractIndent(const std::string& line);

std::string formatDouble(const double d, const int precision);

std::string sprintf(const char* fmt, ...);

// consume all of standard input and return it as a string; used for processes
// that consume standard input. reads a maximum of 1mb by default.
std::string consumeStdin(StdinLines kind, unsigned maxChars = 1048576);

} // namespace string_utils

// wrappers for functions in <cctype>, as those functions normally
// require one to cast arguments from char to unsigned char, otherwise
// one risks bumping into undefined behavior (which MSVC is strict about)
// see e.g. http://en.cppreference.com/w/cpp/string/byte/toupper for
// motivation
//
// we also use some C++ template magic to avoid misuse; that is, we don't want
// to allow callers to accidentally pass wchar_t or other non-char types into
// these functions. then wrap it into a macro to generate code with less pain

inline namespace cctype {

namespace internal {

template <typename T>
using IsCharacter = boost::disjunction<
    boost::is_same<T, char>,
    boost::is_same<T, signed char>,
    boost::is_same<T, unsigned char>
>;

}

#define RS_GENERATE_CCTYPE_ALIAS(__NAME__, __WNAME__)          \
                                                               \
template <typename T>                                          \
inline bool __NAME__(T ch)                                     \
{                                                              \
   static_assert(internal::IsCharacter<T>::value, "");         \
   return std::__NAME__(static_cast<unsigned char>(ch)) != 0;  \
}                                                              \
                                                               \
template <typename T>                                          \
inline bool __WNAME__(T ch)                                    \
{                                                              \
   static_assert(std::is_same<T, wchar_t>::value, "");         \
   return std::__WNAME__(static_cast<wchar_t>(ch)) != 0;       \
}                                                              \

RS_GENERATE_CCTYPE_ALIAS(isalpha,  iswalpha)
RS_GENERATE_CCTYPE_ALIAS(isalnum,  iswalnum)
RS_GENERATE_CCTYPE_ALIAS(islower,  iswlower)
RS_GENERATE_CCTYPE_ALIAS(isupper,  iswupper)
RS_GENERATE_CCTYPE_ALIAS(isdigit,  iswdigit)
RS_GENERATE_CCTYPE_ALIAS(isxdigit, iswxdigit)
RS_GENERATE_CCTYPE_ALIAS(iscntrl,  iswcntrl)
RS_GENERATE_CCTYPE_ALIAS(isgraph,  iswgraph)
RS_GENERATE_CCTYPE_ALIAS(isspace,  iswspace)
RS_GENERATE_CCTYPE_ALIAS(isblank,  iswblank)
RS_GENERATE_CCTYPE_ALIAS(isprint,  iswprint)
RS_GENERATE_CCTYPE_ALIAS(ispunct,  iswpunct)

#undef RS_GENERATE_CCTYPE_ALIAS

} // end inline namespace cctype

} // namespace core 
} // namespace rstudio

// force our safe wrappers to be used
using namespace rstudio::core::cctype;

#endif // CORE_STRING_UTILS_HPP

