/*
 * PosixStringUtils.cpp
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

#include <core/StringUtils.hpp>

#include <cstdlib>

#include <vector>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>

#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Woverloaded-virtual"
#endif

#include <boost/program_options/detail/convert.hpp>
#include <boost/program_options/detail/utf8_codecvt_facet.hpp>

#ifdef __clang__
#pragma clang diagnostic pop
#endif

namespace rstudio {
namespace core {
namespace string_utils {

std::string wideToUtf8(const std::wstring& value)
{
   try
   {
      boost::program_options::detail::utf8_codecvt_facet utf8_facet;
      return boost::to_8_bit(value, utf8_facet);
   }
   catch(const std::exception& e)
   {
      // this should NEVER happen!
      LOG_ERROR_MESSAGE(e.what());
      return std::string();
   }
}

std::wstring utf8ToWide(const std::string& value, const std::string& context)
{
   try
   {
      boost::program_options::detail::utf8_codecvt_facet utf8_facet;
      return boost::from_8_bit(value, utf8_facet);
   }
   catch(const std::exception&)
   {
      // could happen if the inbound data isn't correctly utf8 encoded,
      // in this case just use the system default encoding
      static const std::size_t ERR = -1;
      std::vector<wchar_t> wide(value.length() + 1);
      std::size_t len = ::mbstowcs(&(wide[0]), value.c_str(), wide.size());
      if (len != ERR)
      {
         return std::wstring(&(wide[0]), len);
      }
      else
      {
         std::string message = "Invalid multibyte character";
         if (!context.empty())
            message += " " + context;
         LOG_ERROR_MESSAGE(message);
         return std::wstring();
      }
   }
}

} // namespace string_utils
} // namespace core
} // namespace rstudio



