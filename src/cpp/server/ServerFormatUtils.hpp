/*
 * ServerFormatUtils.hpp
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

#ifndef SERVER_FORMAT_UTILS_HPP
#define SERVER_FORMAT_UTILS_HPP

#include <string>

namespace rstudio {
namespace server {

// Formats an integer number of minutes into a human-friendly duration string
// suitable for the sign-in inactivity-timeout message. Decomposes the value
// into days, hours, and minutes; suppresses zero components; pluralizes each
// part; and joins parts using Oxford-comma style.
//
// Examples:
//   formatLoginTimeoutDuration(4320) -> "3 days"
//   formatLoginTimeoutDuration(4319) -> "2 days, 23 hours, and 59 minutes"
//   formatLoginTimeoutDuration(90)   -> "1 hour and 30 minutes"
//   formatLoginTimeoutDuration(45)   -> "45 minutes"
//   formatLoginTimeoutDuration(1)    -> "1 minute"
//   formatLoginTimeoutDuration(0)    -> "0 minutes"
//   formatLoginTimeoutDuration(-5)   -> "0 minutes"
std::string formatLoginTimeoutDuration(int minutes);

} // namespace server
} // namespace rstudio

#endif // SERVER_FORMAT_UTILS_HPP
