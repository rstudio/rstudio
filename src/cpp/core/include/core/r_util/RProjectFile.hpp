/*
 * RProjectFile.hpp
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

#ifndef CORE_R_UTIL_R_PROJECT_FILE_HPP
#define CORE_R_UTIL_R_PROJECT_FILE_HPP

#include <string>
#include <iosfwd>

namespace core {

class Error;
class FilePath;

namespace r_util {

enum YesNoAskValue
{
   DefaultValue = 0,
   YesValue = 1,
   NoValue = 2,
   AskValue = 3
};

extern const char * const kBuildTypeNone;
extern const char * const kBuildTypePackage;
extern const char * const kBuildTypeMakefile;
extern const char * const kBuildTypeCustom;

std::ostream& operator << (std::ostream& stream, const YesNoAskValue& val);

struct RProjectConfig
{
   RProjectConfig()
      : version(1.0),
        saveWorkspace(DefaultValue),
        restoreWorkspace(DefaultValue),
        alwaysSaveHistory(DefaultValue),
        enableCodeIndexing(true),
        useSpacesForTab(true),
        numSpacesForTab(2),
        encoding(),
        defaultSweaveEngine(),
        defaultLatexProgram(),
        rootDocument(),
        buildType(),
        packagePath(),
        packageInstallArgs(),
        packageBuildArgs(),
        packageBuildBinaryArgs(),
        packageCheckArgs(),
        packageRoxygenize(),
        makefilePath(),
        customScriptPath(),
        tutorialPath()
   {
   }

   double version;
   int saveWorkspace;
   int restoreWorkspace;
   int alwaysSaveHistory;
   bool enableCodeIndexing;
   bool useSpacesForTab;
   int numSpacesForTab;
   std::string encoding;
   std::string defaultSweaveEngine;
   std::string defaultLatexProgram;
   std::string rootDocument;
   std::string buildType;
   std::string packagePath;
   std::string packageInstallArgs;
   std::string packageBuildArgs;
   std::string packageBuildBinaryArgs;
   std::string packageCheckArgs;
   std::string packageRoxygenize;
   std::string makefilePath;
   std::string customScriptPath;
   std::string tutorialPath;
};


Error readProjectFile(const FilePath& projectFilePath,
                      const RProjectConfig& defaultConfig,
                      RProjectConfig* pConfig,
                      bool* pProvidedDefaults,
                      std::string* pUserErrMsg);

Error writeProjectFile(const FilePath& projectFilePath,
                       const RProjectConfig& config);

FilePath projectFromDirectory(const FilePath& directoryPath);

} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_PROJECT_FILE_HPP

