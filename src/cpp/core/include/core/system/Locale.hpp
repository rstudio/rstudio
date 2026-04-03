/*
 * Locale.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#ifndef CORE_SYSTEM_LOCALE_HPP
#define CORE_SYSTEM_LOCALE_HPP

#include <string>

namespace rstudio {
namespace core {
namespace system {

// Returns the charset name (e.g. "UTF-8", "CP1252") for the current
// LC_CTYPE locale.
std::string currentCharset();

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_LOCALE_HPP
