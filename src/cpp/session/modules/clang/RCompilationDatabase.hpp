/*
 * RCompilationDatabase.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

#include <core/libclang/LibClang.hpp>

#define kCompilationDbPrefix "clang-compilation-db-"

namespace rstudio {
namespace core {
namespace r_util {
class RPackageInfo;
} // namespace r_util
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace session {
namespace modules {      
namespace clang {

class RCompilationDatabase : boost::noncopyable
{
public:
   RCompilationDatabase();
   virtual ~RCompilationDatabase() {}

   std::vector<std::string> compileArgsForTranslationUnit(
           const std::string& filename, bool usePrecompiledHeaders);

   bool isProjectTranslationUnit(const std::string& filename) const;

   std::vector<std::string> projectTranslationUnits() const;

   void rebuildPackageCompilationDatabase();

private:

   core::Error executeSourceCpp(core::system::Options env,
                                const std::string& rcppPkg,
                                const core::FilePath& srcPath,
                                core::system::ProcessResult* pResult);

   core::Error executeRCmdSHLIB(core::system::Options env,
                                const core::FilePath& srcPath,
                                core::system::ProcessResult* pResult);

   void updateForCurrentPackage();
   void updateForSourceCpp(const core::FilePath& cppPath);
   std::vector<std::string> compileArgsForPackage(
                                     const core::system::Options& env,
                                     const core::FilePath& pkgPath,
                                     bool isCpp);


   void savePackageCompilationConfig();
   void restorePackageCompilationConfig();

   // struct used to represent compilation settings
   struct CompilationConfig
   {
      bool empty() const { return args.empty(); }
      std::vector<std::string> args;
      std::string PCH;
      bool isCpp;
   };
   CompilationConfig configForSourceCpp(const std::string& rcppPkg,
                                        core::FilePath srcFile);

   std::vector<std::string> argsForRCmdSHLIB(core::system::Options env,
                                             core::FilePath tempSrcFile);

   std::vector<std::string> baseCompilationArgs(bool isCppFile) const;
   std::vector<std::string> packageCompilationArgs(
         core::r_util::RPackageInfo* pPkgInfo = nullptr,
         bool* pIsCpp = nullptr);

   std::vector<std::string> rToolsArgs() const;
   core::system::Options compilationEnvironment() const;
   std::vector<std::string> precompiledHeaderArgs(const CompilationConfig& config);

   bool shouldIndexConfig(const CompilationConfig& config);

private:

   // Rtools arguments (cache once we successfully get them)
   mutable std::vector<std::string> rToolsArgs_;

   // track the sourceCpp hash values used to derive args (don't re-run
   // detection if hash hasn't changed)
   typedef std::map<std::string,std::string> SourceCppHashes;
   SourceCppHashes sourceCppHashes_;

   // source file compilation settings
   typedef std::map<std::string,CompilationConfig> ConfigMap;
   ConfigMap sourceCppConfigMap_;

   // package compliation settings (track file modification times on build
   // oriented files to avoid re-running detection)
   std::string packageBuildFileHash_;
   CompilationConfig packageCompilationConfig_;
   bool usePrecompiledHeaders_;
   bool forceRebuildPrecompiledHeaders_;
   bool restoredCompilationConfig_;
};

core::libclang::CompilationDatabase rCompilationDatabase();


} // namespace clang
} // namepace handlers
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_CLANG_R_COMPILATION_DATABASE_HPP
