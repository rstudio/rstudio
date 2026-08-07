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

#include <core/SqlIdentifier.hpp>

namespace rstudio {
namespace core {
namespace database {

namespace {

Error validate(const std::string& name)
{
   // Operate on the char array to avoid any runtime overhead
   // that might be incurred by std::string::operator[]
   const char* cstr = name.c_str();
   int len = name.size();
   if (!len)
   {
      return Error(
         "SQL identifiers cannot be empty",
         boost::system::errc::invalid_argument,
         ERROR_LOCATION
      );
   }
   for (int i = 0; i < len; i++)
   {
      char ch = cstr[i];
      if (ch >= 'A' && ch <= 'Z')
         continue;
      if (ch >= 'a' && ch <= 'z')
         continue;
      if (ch >= '0' && ch <= '9')
         continue;
      if (ch == '_')
         continue;
      return Error(
         std::string("Illegal character ") + ch + " in SQL identifier \"" + name + "\"",
         boost::system::errc::invalid_argument,
         ERROR_LOCATION
      );
   }
   return Error();
}

}

SqlIdentifier::SqlIdentifier(const char* prevalidatedName)
: name(prevalidatedName)
{
   // initializers only
}

SqlIdentifier::SqlIdentifier(const std::string& name)
: name(name)
{
   // initializers only
}

SqlIdentifier::SqlIdentifier(std::string&& name)
: name(std::move(name))
{
   // initializers only
}

Result<SqlIdentifier> SqlIdentifier::from(const std::string& unvalidatedName)
{
   Error error = validate(unvalidatedName);
   if (error)
      return Unexpected(error);
   return SqlIdentifier(unvalidatedName);
}

Result<SqlIdentifier> SqlIdentifier::from(std::string&& unvalidatedName)
{
   Error error = validate(unvalidatedName);
   if (error)
      return Unexpected(error);
   return SqlIdentifier(std::move(unvalidatedName));
}

} // namespace database
} // namespace core
} // namespace rstudio
