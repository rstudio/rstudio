/*
 * Win32StringUtils.cpp
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

#include <shared_core/system/Win32StringUtils.hpp>

#include <gsl/gsl>

#include <windows.h>

#include <shared_core/Logger.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace string_utils {

std::string wideToUtf8(const std::wstring& in_value)
{
   if (in_value.size() == 0)
      return std::string();

   const wchar_t * cstr = in_value.c_str();
   int chars = ::WideCharToMultiByte(CP_UTF8, 0,
                                     cstr, -1,
                                     nullptr, 0, nullptr, nullptr);
   if (chars == 0)
   {
      log::logError(LAST_SYSTEM_ERROR());
      return std::string();
   }

   std::vector<char> result(chars, 0);
   chars = ::WideCharToMultiByte(CP_UTF8,
                                 0,
                                 cstr,
                                 -1,
                                 &(result[0]),
                                 gsl::narrow_cast<int>(result.size()),
                                 nullptr, nullptr);

   return std::string(&(result[0]));
}

std::wstring utf8ToWide(const std::string& in_value,
                        const std::string& context)
{
   if (in_value.size() == 0)
      return std::wstring();

   const char * cstr = in_value.c_str();
   int chars = ::MultiByteToWideChar(CP_UTF8, 0,
                                     cstr, -1,
                                     nullptr, 0);
   if (chars == 0)
   {
      Error error = LAST_SYSTEM_ERROR();
      if (!context.empty())
         error.addProperty("context", context);
      log::logError(error);
      return std::wstring();
   }

   std::vector<wchar_t> result(chars, 0);
   chars = ::MultiByteToWideChar(CP_UTF8, 0,
                                 cstr, -1,
                                 &(result[0]),
                                 gsl::narrow_cast<int>(result.size()));

   return std::wstring(&(result[0]));
}


} // namespace string_utils
} // namespace core
} // namespace rstudio



