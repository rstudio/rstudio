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
// LC_CTYPE locale. Always returns a non-empty string; falls back to
// "ASCII" if the charset cannot be determined.
//
// When canonicalize is true, maps Windows codepage names to their
// conventional equivalents (e.g. "CP1252" -> "ISO-8859-1") for
// display in the encoding dialog.
//
// NOTE: Calls setlocale() with a NULL argument, which is not thread-safe
// per the C standard. Must be called from the main thread.
std::string currentCharset(bool canonicalize = false);

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_LOCALE_HPP
