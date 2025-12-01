/*
 * ChatTypes.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#ifndef SESSION_CHAT_TYPES_HPP
#define SESSION_CHAT_TYPES_HPP

#include <string>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace types {

// Parse semantic version string into components
struct SemanticVersion
{
   int major;
   int minor;
   int patch;

   SemanticVersion();

   bool parse(const std::string& versionStr);

   bool operator>(const SemanticVersion& other) const;
   bool operator<(const SemanticVersion& other) const;
   bool operator>=(const SemanticVersion& other) const;
   bool operator<=(const SemanticVersion& other) const;
   bool operator==(const SemanticVersion& other) const;
   bool operator!=(const SemanticVersion& other) const;
};

} // namespace types
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_TYPES_HPP
