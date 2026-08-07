/*
 * SqlIdentifier.hpp
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

#ifndef CORE_DETAILS_SQLIDENTIFIER_HPP
#define CORE_DETAILS_SQLIDENTIFIER_HPP

#include <core/Result.hpp>

#include <string>
#include <sstream>

namespace rstudio {
namespace core {
namespace database {

class SqlIdentifier
{
public:
   SqlIdentifier() = default;
   SqlIdentifier(const SqlIdentifier&) = default;
   SqlIdentifier(SqlIdentifier&&) = default;
   SqlIdentifier& operator=(const SqlIdentifier&) = default;
   SqlIdentifier& operator=(SqlIdentifier&&) = default;

   SqlIdentifier(const char* prevalidatedName);

   static Result<SqlIdentifier> from(const std::string& unvalidatedName);
   static Result<SqlIdentifier> from(std::string&& unvalidatedName);

   inline operator std::string() const { return name; }
   inline const std::string& toString() const { return name; }

   inline std::string operator+(const char* rhs) const { return name + rhs; }

   inline bool operator==(const SqlIdentifier& rhs) const { return name == rhs.name; }
   inline bool operator==(const std::string& rhs) const { return name == rhs; }
   inline bool operator==(const char* rhs) const { return name == rhs; }

private:
   explicit SqlIdentifier(const std::string& name);
   explicit SqlIdentifier(std::string&& name);

   std::string name;
};

inline std::string operator+(const char* lhs, const SqlIdentifier& rhs)
{
   return lhs + rhs.toString();
}

inline std::string operator+(const std::string& lhs, const SqlIdentifier& rhs)
{
   return lhs + rhs.toString();
}

inline bool operator==(const std::string& lhs, const SqlIdentifier& rhs)
{
   return lhs == rhs.toString();
}

inline std::ostream& operator<<(std::ostream& os, const SqlIdentifier& rhs)
{
   os << rhs.toString();
   return os;
}

} // namespace database
} // namespace core
} // namespace rstudio

#endif // CORE_DETAILS_SQLIDENTIFIER_HPP
