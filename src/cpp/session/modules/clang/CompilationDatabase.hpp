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

#ifndef SESSION_MODULES_CLANG_COMPILATION_DATABASE_HPP
#define SESSION_MODULES_CLANG_COMPILATION_DATABASE_HPP

#include <map>
#include <vector>

#include <boost/noncopyable.hpp>

#include <core/FilePath.hpp>

#include <core/system/Process.hpp>

#include "libclang/SourceIndex.hpp"

namespace session {
namespace modules {      
namespace clang {

class CompilationDatabase : public libclang::SourceIndex::CompilationDatabase,
                            boost::noncopyable
{
private:
   friend CompilationDatabase& compilationDatabase();
   CompilationDatabase() {}

public:
   virtual ~CompilationDatabase();

   virtual std::vector<std::string> compileArgsForTranslationUnit(
                                             const std::string& filename);

   virtual std::vector<std::string> translationUnits();

private:

   core::Error executeSourceCpp(core::system::Options env,
                                const core::FilePath& srcPath,
                                core::system::ProcessResult* pResult);

   core::Error executeRCmdSHLIB(core::system::Options env,
                                const core::FilePath& srcPath,
                                core::system::ProcessResult* pResult);

   void updateForCurrentPackage();
   void updateForStandalone(const core::FilePath& cppPath);

   std::vector<std::string> rToolsArgs() const;
   std::vector<std::string> precompiledHeaderArgs(const std::string& pkgName,
                                                  const std::string& stdArg);

   std::vector<std::string> computeArgsForSourceFile(
                                          const core::FilePath& srcPath);

private:

   // Rtools arguments (cache once we successfully get them)
   mutable std::vector<std::string> rToolsArgs_;

   // track the set of attributes used to derive args (don't re-run
   // detection if attributes haven't changed)
   typedef std::map<std::string,std::string> AttribsMap;
   AttribsMap attribsMap_;

   // arguments for various translation units
   typedef std::map<std::string,std::vector<std::string> > ArgsMap;
   ArgsMap argsMap_;

   // package src args (track file modification times on build
   // oriented files to avoid re-running detection)
   std::string packageBuildFileHash_;
   std::vector<std::string> packageSrcArgs_;
};

// global instance
CompilationDatabase& compilationDatabase();

} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_COMPILATION_DATABASE_HPP
