/*
 * StringUtils.hpp
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

#ifndef CORE_STRING_UTILS_HPP
#define CORE_STRING_UTILS_HPP

#include <string>
#include <core/Error.hpp>
#include <core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace string_utils {

enum LineEnding {
   LineEndingWindows = 0,
   LineEndingPosix = 1,
   LineEndingNative = 2,
   LineEndingPassthrough = 3
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
std::string singleQuotedStrEscape(const std::string& str);

void convertLineEndings(std::string* str, LineEnding type);

bool detectLineEndings(const FilePath& filePath, LineEnding* pType);

std::string filterControlChars(const std::string& str);

bool parseVersion(const std::string& str, uint64_t* pVersion);

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
std::string wideToUtf8(const std::wstring& value);
std::wstring utf8ToWide(const std::string& value,
                        const std::string& context = std::string());

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


bool isalpha(wchar_t c);
bool isalnum(wchar_t c);

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

} // namespace string_utils
} // namespace core 
} // namespace rstudio

#endif // CORE_STRING_UTILS_HPP

