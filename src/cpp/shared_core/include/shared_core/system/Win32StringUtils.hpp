/*
 * Win32StringUtils.hpp
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

#ifndef SHARED_CORE_WIN_32_STRING_UTILS_HPP
#define SHARED_CORE_WIN_32_STRING_UTILS_HPP

#include <string>

namespace rstudio {
namespace core {
namespace string_utils {

/**
 * @brief Converts the wide string value to a UTF-8 string.
 *
 * @param in_value      The string to convert from wide to UTF-8 format.
 *
 * @return The converted UTF-8 string.
 */
std::string wideToUtf8(const std::wstring& in_value);

/**
 * @brief Converts the UTF-8 string value to a wide string.
 *
 * @param in_value      The value to convert from UTF-8 to wide string format.
 * @param in_context    The context of the conversion, for error reporting.
 *
 * @return The converted wide string.
 */
std::wstring utf8ToWide(const std::string& in_value, const std::string& in_context = std::string());

} // namespace string_utils
} // namespace core
} // namespace rstudio

#endif
