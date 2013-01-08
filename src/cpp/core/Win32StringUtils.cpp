/*
 * Win32StringUtils.cpp
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

#include <core/StringUtils.hpp>

#include <windows.h>

#include <core/Log.hpp>
#include <core/Error.hpp>

namespace core {
namespace string_utils {

std::string wideToUtf8(const std::wstring& value)
{
   if (value.size() == 0)
      return std::string();

   const wchar_t * cstr = value.c_str();
   int chars = ::WideCharToMultiByte(CP_UTF8, 0,
                                     cstr, -1,
                                     NULL, 0, NULL, NULL);
   if (chars == 0)
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
      return std::string();
   }

   std::vector<char> result(chars, 0);
   chars = ::WideCharToMultiByte(CP_UTF8, 0,
                                 cstr, -1,
                                 &(result[0]), result.size(),
                                 NULL, NULL);

   return std::string(&(result[0]));
}

std::wstring utf8ToWide(const std::string& value,
                        const std::string& context)
{
   if (value.size() == 0)
      return std::wstring();

   const char * cstr = value.c_str();
   int chars = ::MultiByteToWideChar(CP_UTF8, 0,
                                     cstr, -1,
                                     NULL, 0);
   if (chars == 0)
   {
      Error error = systemError(::GetLastError(), ERROR_LOCATION);
      if (!context.empty())
         error.addProperty("context", context);
      LOG_ERROR(error);
      return std::wstring();
   }

   std::vector<wchar_t> result(chars, 0);
   chars = ::MultiByteToWideChar(CP_UTF8, 0,
                                 cstr, -1,
                                 &(result[0]), result.size());

   return std::wstring(&(result[0]));
}


} // namespace string_utils
} // namespace core



