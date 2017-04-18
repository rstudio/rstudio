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

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {      
namespace clang {

// definition type
enum CppDefinitionKind
{
   CppInvalidDefinition = 0,
   CppNamespaceDefinition = 1,
   CppClassDefinition = 2,
   CppStructDefinition = 3,
   CppEnumDefinition = 4,
   CppEnumValue = 5,
   CppFunctionDefinition = 6,
   CppMemberFunctionDefinition = 7,
   CppTypedefDefinition = 8
};

// C++ symbol definition
struct CppDefinition
{
   CppDefinition()
      : kind(CppInvalidDefinition)
   {
   }

   CppDefinition(const std::string& USR,
                 CppDefinitionKind kind,
                 const std::string& parentName,
                 const std::string& name,
                 const core::libclang::FileLocation& location)
      : USR(USR),
        kind(kind),
        parentName(parentName),
        name(name),
        location(location)
   {
   }

   bool empty() const { return name.empty(); }

   std::string USR;
   CppDefinitionKind kind;
   std::string parentName; // e.g. containing C++ class
   std::string name;
   core::libclang::FileLocation location;
};

std::ostream& operator<<(std::ostream& os, const CppDefinition& definition);

core::libclang::FileLocation findDefinitionLocation(
                     const core::libclang::FileLocation& location);

void searchDefinitions(const std::string& term,
                       std::vector<CppDefinition>* pDefinitions);

core::Error initializeDefinitionIndex();

} // namespace clang
} // namepace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_CLANG_DEFINITION_INDEX_HPP
