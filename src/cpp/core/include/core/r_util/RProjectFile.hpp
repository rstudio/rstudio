/*
 * RProjectFile.hpp
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

#ifndef CORE_R_UTIL_R_PROJECT_FILE_HPP
#define CORE_R_UTIL_R_PROJECT_FILE_HPP

#include <string>
#include <iosfwd>

#include <core/r_util/RVersionInfo.hpp>

namespace rstudio {
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

extern const int kLineEndingsUseDefault;

extern const char * const kBuildTypeNone;
extern const char * const kBuildTypePackage;
extern const char * const kBuildTypeMakefile;
extern const char * const kBuildTypeWebsite;
extern const char * const kBuildTypeCustom;

extern const char * const kMarkdownWrapUseDefault;
extern const char * const kMarkdownWrapNone;
extern const char * const kMarkdownWrapColumn;
extern const char * const kMarkdownWrapSentence;

extern const int kMarkdownWrapAtColumnDefault;

extern const char * const kMarkdownReferencesUseDefault;
extern const char * const kMarkdownReferencesBlock;
extern const char * const kMarkdownReferencesSection;
extern const char * const kMarkdownReferencesDocument;


std::ostream& operator << (std::ostream& stream, const YesNoAskValue& val);

struct RProjectBuildDefaults
{
   RProjectBuildDefaults()
      : useDevtools(true)
   {
   }
   bool useDevtools;
};

struct RProjectConfig
{
   RProjectConfig()
      : version(1.0),
        rVersion(kRVersionDefault),
        saveWorkspace(DefaultValue),
        restoreWorkspace(DefaultValue),
        alwaysSaveHistory(DefaultValue),
        enableCodeIndexing(true),
        useSpacesForTab(true),
        numSpacesForTab(2),
        autoAppendNewline(false),
        stripTrailingWhitespace(false),
        lineEndings(kLineEndingsUseDefault),
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
        packageUseDevtools(false),
        makefilePath(),
        websitePath(),
        customScriptPath(),
        tutorialPath(),
        quitChildProcessesOnExit(DefaultValue),
        disableExecuteRprofile(false),
        defaultOpenDocs(),
        defaultTutorial(),
        markdownWrap(kMarkdownWrapUseDefault),
        markdownWrapAtColumn(kMarkdownWrapAtColumnDefault),
        markdownReferences(kMarkdownReferencesUseDefault),
        markdownCanonical(DefaultValue),
        zoteroLibraries(),
        pythonType(),
        pythonVersion(),
        pythonPath(),
        spellingDictionary()
   {
   }

   double version;
   RVersionInfo rVersion;
   int saveWorkspace;
   int restoreWorkspace;
   int alwaysSaveHistory;
   bool enableCodeIndexing;
   bool useSpacesForTab;
   int numSpacesForTab;
   bool autoAppendNewline;
   bool stripTrailingWhitespace;
   int lineEndings;
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
   bool packageUseDevtools;
   std::string makefilePath;
   std::string websitePath;
   std::string customScriptPath;
   std::string tutorialPath;
   int quitChildProcessesOnExit;
   bool disableExecuteRprofile;
   std::string defaultOpenDocs;
   std::string defaultTutorial;
   std::string markdownWrap;
   int markdownWrapAtColumn;
   std::string markdownReferences;
   int markdownCanonical;
   boost::optional<std::vector<std::string>> zoteroLibraries;
   std::string pythonType;
   std::string pythonVersion;
   std::string pythonPath;
   std::string spellingDictionary;
};

Error findProjectFile(FilePath filePath,
                      FilePath anchorPath,
                      FilePath* pProjPath);

Error findProjectConfig(FilePath filePath,
                        const FilePath& anchorPath,
                        RProjectConfig* pConfig);

Error readProjectFile(const FilePath& projectFilePath,
                      RProjectConfig* pConfig,
                      std::string* pUserErrMsg);

Error readProjectFile(const FilePath& projectFilePath,
                      const RProjectConfig& defaultConfig,
                      const RProjectBuildDefaults& buildDefaults,
                      RProjectConfig* pConfig,
                      bool* pProvidedDefaults,
                      std::string* pUserErrMsg);

Error writeProjectFile(const FilePath& projectFilePath,
                       const RProjectBuildDefaults& buildDefaults,
                       const RProjectConfig& config);

FilePath projectFromDirectory(const FilePath& directoryPath);

// update the package install args default (only if it is set to
// the previous default value)
bool updateSetPackageInstallArgsDefault(RProjectConfig* pConfig);

// indicate whether the given directory is an R Markdown website
bool isWebsiteDirectory(const FilePath& projectDir);

// discover website root directory for a filePath
FilePath websiteRootDirectory(const FilePath& filePath);

} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_PROJECT_FILE_HPP

