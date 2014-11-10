/*
 * DefinitionIndex.hpp
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

#ifndef SESSION_MODULES_CLANG_DEFINITION_INDEX_HPP
#define SESSION_MODULES_CLANG_DEFINITION_INDEX_HPP

#include <string>
#include <iosfwd>

#include <core/FilePath.hpp>
#include <core/libclang/LibClang.hpp>

namespace core {
   class Error;
}

namespace session {
namespace modules {      
namespace clang {

// definition type
enum DefinitionKind
{
   InvalidDefinition = 0,
   NamespaceDefinition = 1,
   ClassDefinition = 2,
   StructDefinition = 3,
   EnumDefinition = 4,
   FunctionDefinition = 5,
   MemberFunctionDefinition = 6
};

// source location
struct Location
{
   Location()
      : line(0), column(0)
   {
   }

   Location(const core::FilePath& filePath, unsigned line, unsigned column)
      : filePath(filePath), line(line), column(column)
   {
   }

   bool empty() const { return filePath.empty(); }

   core::FilePath filePath;
   unsigned line;
   unsigned column;
};

// C++ symbol definition
struct Definition
{
   Definition()
      : kind(InvalidDefinition)
   {
   }

   Definition(const std::string& USR,
              DefinitionKind kind,
              const std::string& displayName,
              const Location& location)
      : USR(USR),
        kind(kind),
        displayName(displayName),
        location(location)
   {
   }

   bool empty() const { return kind == InvalidDefinition; }

   std::string USR;
   DefinitionKind kind;
   std::string displayName;
   Location location;
};

std::ostream& operator<<(std::ostream& os, const Definition& definition);

Location findDefinitionLocation(const Location& location);

core::Error initializeDefinitionIndex();

} // namespace clang
} // namepace modules
} // namesapce session

#endif // SESSION_MODULES_CLANG_DEFINITION_INDEX_HPP
