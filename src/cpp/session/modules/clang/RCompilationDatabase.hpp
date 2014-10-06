/*
 * RCompilationDatabase.hpp
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

#ifndef SESSION_MODULES_CLANG_R_COMPILATION_DATABASE_HPP
#define SESSION_MODULES_CLANG_R_COMPILATION_DATABASE_HPP

#include <map>
#include <string>
#include <vector>

#include <boost/noncopyable.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

namespace session {
namespace modules {      
namespace clang {

class RCompilationDatabase : boost::noncopyable
{
public:
   virtual ~RCompilationDatabase() {}

   std::vector<std::string> compileArgsForTranslationUnit(
                                             const std::string& filename);
   std::vector<std::string> translationUnits();

private:

   core::Error executeSourceCpp(core::system::Options env,
                                const core::FilePath& srcPath,
                                core::system::ProcessResult* pResult);

   core::Error executeRCmdSHLIB(core::system::Options env,
                                const core::FilePath& srcPath,
                                core::system::ProcessResult* pResult);

   void updateForCurrentPackage();
   void updateForSourceCpp(const core::FilePath& cppPath);

   std::vector<std::string> argsForSourceCpp(core::FilePath srcFile);

   std::vector<std::string> argsForRCmdSHLIB(core::system::Options env,
                                             core::FilePath tempSrcFile);

   std::vector<std::string> rToolsArgs() const;
   std::vector<std::string> precompiledHeaderArgs(const std::string& pkgName,
                                                  const std::string& stdArg);

private:

   // Rtools arguments (cache once we successfully get them)
   mutable std::vector<std::string> rToolsArgs_;

   // track the sourceCpp hash values used to derive args (don't re-run
   // detection if hash hasn't changed)
   typedef std::map<std::string,std::string> SourceCppHashes;
   SourceCppHashes sourceCppHashes_;

   // struct used to represent compilation settings
   struct CompilationConfig
   {
      std::vector<std::string> args;
      std::string PCH;
   };

   // source file compilation settings
   typedef std::map<std::string,CompilationConfig> ConfigMap;
   ConfigMap sourceCppConfigMap_;

   // package compliation settings (track file modification times on build
   // oriented files to avoid re-running detection)
   std::string packageBuildFileHash_;
   CompilationConfig packageCompilationConfig_;
};


} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_R_COMPILATION_DATABASE_HPP
