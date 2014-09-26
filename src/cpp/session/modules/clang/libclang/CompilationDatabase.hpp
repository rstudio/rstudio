/*
 * CompilationDatabase.hpp
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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_COMPILATION_DATABASE_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_COMPILATION_DATABASE_HPP

#include <map>
#include <vector>

#include <boost/noncopyable.hpp>

#include <core/FilePath.hpp>

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

class CompilationDatabase : boost::noncopyable
{
private:
   friend CompilationDatabase& compilationDatabase();
   CompilationDatabase() {}

public:
   virtual ~CompilationDatabase();

   void updateForCurrentPackage();

   void updateForPackageCppAddition(const core::FilePath& cppPath);

   void updateForStandaloneCpp(const core::FilePath& cppPath);

   std::vector<std::string> argsForFile(const std::string& cppPath) const;

private:

   std::vector<std::string> rToolsArgs() const;

   void updateIfNecessary(const std::string& cppPath,
                          const std::vector<std::string>& args);
private:

   // Rtools arguments (cache once we successfully get them)
   mutable std::vector<std::string> rToolsArgs_;

   // arguments for various translation units
   typedef std::map<std::string,std::vector<std::string> > ArgsMap;
   ArgsMap argsMap_;

   // track the set of attributes used to derive args (don't re-run
   // detection if attributes haven't changed)
   typedef std::map<std::string,std::string> AttribsMap;
   AttribsMap attribsMap_;
};

// global instance
CompilationDatabase& compilationDatabase();


} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_COMPILATION_DATABASE_HPP
