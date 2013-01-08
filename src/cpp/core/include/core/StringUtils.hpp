/*
 * StringUtils.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

namespace core {
namespace string_utils {

enum LineEnding {
   LineEndingWindows,
   LineEndingPosix,
   LineEndingNative,
   LineEndingPassthrough
};

std::string utf8ToSystem(const std::string& str,
                         bool escapeInvalidChars=false);
std::string systemToUtf8(const std::string& str);

std::string toLower(const std::string& str);
std::string textToHtml(const std::string& str);

std::string htmlEscape(const std::string& str, bool isAttributeValue);
std::string jsLiteralEscape(const std::string& str);
std::string jsonLiteralEscape(const std::string& str);
std::string jsonLiteralUnescape(const std::string& str);

void convertLineEndings(std::string* str, LineEnding type);

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

void trimLeadingLines(int maxLines, std::string* pLines);

} // namespace string_utils
} // namespace core 

#endif // CORE_STRING_UTILS_HPP

