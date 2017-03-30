/*
 * RProjectFile.cpp
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

#include <core/r_util/RProjectFile.hpp>

#include <map>
#include <iomanip>
#include <ostream>

#include <boost/format.hpp>
#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <core/RegexUtils.hpp>
#include <core/StringUtils.hpp>
#include <core/YamlUtil.hpp>
#include <core/text/DcfParser.hpp>

#include <core/r_util/RPackageInfo.hpp>
#include <core/r_util/RVersionInfo.hpp>

namespace rstudio {
namespace core {
namespace r_util {

const int kLineEndingsUseDefault = -1;

const char * const kLineEndingPassthough = "None";
const char * const kLineEndingNative = "Native";
const char * const kLineEndingWindows = "Windows";
const char * const kLineEndingPosix = "Posix";

const char * const kBuildTypeNone = "None";
const char * const kBuildTypePackage = "Package";
const char * const kBuildTypeMakefile = "Makefile";
const char * const kBuildTypeWebsite = "Website";
const char * const kBuildTypeCustom = "Custom";

namespace {

const char * const kPackageInstallArgsDefault = "--no-multiarch "
                                                "--with-keep.source";
const char * const kPackageInstallArgsPreviousDefault = "--no-multiarch";

Error requiredFieldError(const std::string& field,
                         std::string* pUserErrMsg)
{
   *pUserErrMsg = field + " not correctly specified in project config file";
   Error error = systemError(boost::system::errc::protocol_error, ERROR_LOCATION);
   error.addProperty("user-msg", *pUserErrMsg);
   return error;
}

std::string yesNoAskValueToString(int value)
{
   std::ostringstream ostr;
   ostr << (YesNoAskValue) value;
   return ostr.str();
}

bool interpretYesNoAskValue(const std::string& value,
                            bool acceptAsk,
                            int* pValue)
{
   std::string valueLower = string_utils::toLower(value);
   boost::algorithm::trim(valueLower);
   if (valueLower == "yes")
   {
      *pValue = YesValue;
      return true;
   }
   else if (valueLower == "no")
   {
      *pValue = NoValue;
      return true;
   }
   else if (valueLower == "default")
   {
      *pValue = DefaultValue;
      return true;
   }
   else if (acceptAsk && (valueLower == "ask"))
   {
      *pValue = AskValue;
      return true;
   }
   else
   {
      return false;
   }
}

std::string boolValueToString(bool value)
{
   if (value)
      return "Yes";
   else
      return "No";
}

bool interpretBoolValue(const std::string& value, bool* pValue)
{
   std::string valueLower = string_utils::toLower(value);
   boost::algorithm::trim(valueLower);
   if (valueLower == "yes")
   {
      *pValue = true;
      return true;
   }
   else if (valueLower == "no")
   {
      *pValue = false;
      return true;
   }
   else
   {
      return false;
   }
}

bool interpretBuildTypeValue(const std::string& value, std::string* pValue)
{
   if (value == "" ||
       value == kBuildTypeNone ||
       value == kBuildTypePackage ||
       value == kBuildTypeMakefile ||
       value == kBuildTypeWebsite ||
       value == kBuildTypeCustom)
   {
      *pValue = value;
      return true;
   }
   else
   {
      return false;
   }
}

bool interpretLineEndingsValue(std::string value, int* pValue)
{
   value = boost::algorithm::trim_copy(value);
   if (value == "")
   {
      *pValue = kLineEndingsUseDefault;
      return true;
   }
   else if (value == kLineEndingPassthough)
   {
      *pValue = string_utils::LineEndingPassthrough;
      return true;
   }
   else if (value == kLineEndingNative)
   {
      *pValue = string_utils::LineEndingNative;
      return true;
   }
   else if (value == kLineEndingWindows)
   {
      *pValue = string_utils::LineEndingWindows;
      return true;
   }
   else if (value == kLineEndingPosix)
   {
      *pValue = string_utils::LineEndingPosix;
      return true;
   }
   else
   {
      return false;
   }
}


bool interpretIntValue(const std::string& value, int* pValue)
{
   try
   {
      *pValue = boost::lexical_cast<int>(value);
      return true;
   }
   catch(const boost::bad_lexical_cast& e)
   {
      return false;
   }
}

void setBuildPackageDefaults(const std::string& packagePath,
                             const RProjectBuildDefaults& buildDefaults,
                             RProjectConfig* pConfig)
{
   pConfig->buildType = kBuildTypePackage;
   pConfig->packageUseDevtools = buildDefaults.useDevtools;
   pConfig->packagePath = packagePath;
   pConfig->packageInstallArgs = kPackageInstallArgsDefault;
}

std::string detectBuildType(const FilePath& projectFilePath,
                            const RProjectBuildDefaults& buildDefaults,
                            RProjectConfig* pConfig)
{
   FilePath projectDir = projectFilePath.parent();
   if (r_util::isPackageDirectory(projectDir))
   {
      setBuildPackageDefaults("", buildDefaults ,pConfig);
   }
   else if (projectDir.childPath("pkg/DESCRIPTION").exists())
   {
      setBuildPackageDefaults("pkg", buildDefaults, pConfig);
   }
   else if (projectDir.childPath("Makefile").exists())
   {
      pConfig->buildType = kBuildTypeMakefile;
      pConfig->makefilePath = "";
   }
   else if (isWebsiteDirectory(projectDir))
   {
      pConfig->buildType = kBuildTypeWebsite;
      pConfig->websitePath = "";
   }
   else
   {
      pConfig->buildType = kBuildTypeNone;
   }

   return pConfig->buildType;
}

std::string detectBuildType(const FilePath& projectFilePath,
                            const RProjectBuildDefaults& buildDefaults)
{
   RProjectConfig config;
   return detectBuildType(projectFilePath, buildDefaults, &config);
}

std::string rVersionAsString(const RVersionInfo& rVersion)
{
   std::string ver = rVersion.number;
   if (!rVersion.arch.empty())
      ver += ("/" + rVersion.arch);
   return ver;
}

RVersionInfo rVersionFromString(const std::string& str)
{
   std::size_t pos = str.find('/');
   if (pos == std::string::npos)
      return RVersionInfo(str);
   else
      return RVersionInfo(str.substr(0, pos), str.substr(pos+1));
}

bool interpretRVersionValue(const std::string& value,
                            RVersionInfo* pRVersion)
{
   RVersionInfo version = rVersionFromString(value);

   if (version.number != kRVersionDefault &&
       !regex_utils::match(version.number, boost::regex("[\\d\\.]+")))
   {
      return false;
   }
   else if (version.arch != "" &&
            version.arch != kRVersionArch32 &&
            version.arch != kRVersionArch64)
   {
      return false;
   }
   else
   {
      *pRVersion = version;
      return true;
   }
}

} // anonymous namespace

std::ostream& operator << (std::ostream& stream, const YesNoAskValue& val)
{
   switch(val)
   {
   case YesValue:
      stream << "Yes";
      break;
   case NoValue:
      stream << "No";
      break;
   case AskValue:
      stream << "Ask";
      break;
   case DefaultValue:
   default:
      stream << "Default";
      break;
   }

   return stream ;
}

Error readProjectFile(const FilePath& projectFilePath,
                      RProjectConfig* pConfig,
                      std::string* pUserErrMsg)
{
   bool providedDefaults;
   return readProjectFile(projectFilePath,
                          RProjectConfig(),
                          RProjectBuildDefaults(),
                          pConfig,
                          &providedDefaults,
                          pUserErrMsg);
}

Error readProjectFile(const FilePath& projectFilePath,
                      const RProjectConfig& defaultConfig,
                      const RProjectBuildDefaults& buildDefaults,
                      RProjectConfig* pConfig,
                      bool* pProvidedDefaults,
                      std::string* pUserErrMsg)
{
   // default to not providing defaults
   *pProvidedDefaults = false;

   // first read the project DCF file
   typedef std::map<std::string,std::string> Fields;
   Fields dcfFields;
   Error error = text::parseDcfFile(projectFilePath,
                                    true,
                                    &dcfFields,
                                    pUserErrMsg);
   if (error)
      return error;

   // extract version
   Fields::const_iterator it = dcfFields.find("Version");

   // no version field
   if (it == dcfFields.end())
   {
      *pUserErrMsg = "The project file did not include a Version attribute "
                     "(it may have been created by a more recent version "
                     "of RStudio)";
      return systemError(boost::system::errc::protocol_error,
                         ERROR_LOCATION);
   }

   // invalid version field
   pConfig->version = safe_convert::stringTo<double>(it->second, 0.0);
   if (pConfig->version == 0.0)
   {
      return requiredFieldError("Version", pUserErrMsg);
   }

   // version later than 1.0
   if (pConfig->version != 1.0)
   {
      *pUserErrMsg = "The project file was created by a more recent "
                     "version of RStudio";
       return systemError(boost::system::errc::protocol_error,
                          ERROR_LOCATION);
   }

   // extract R version
   it = dcfFields.find("RVersion");
   if (it != dcfFields.end())
   {
      if (!interpretRVersionValue(it->second, &(pConfig->rVersion)))
         return requiredFieldError("RVersion", pUserErrMsg);
   }
   else
   {
      pConfig->rVersion = defaultConfig.rVersion;
   }

   // extract restore workspace
   it = dcfFields.find("RestoreWorkspace");
   if (it != dcfFields.end())
   {
      if (!interpretYesNoAskValue(it->second, false, &(pConfig->restoreWorkspace)))
         return requiredFieldError("RestoreWorkspace", pUserErrMsg);
   }
   else
   {
      pConfig->restoreWorkspace = defaultConfig.restoreWorkspace;
      *pProvidedDefaults = true;
   }

   // extract save workspace
   it = dcfFields.find("SaveWorkspace");
   if (it != dcfFields.end())
   {
      if (!interpretYesNoAskValue(it->second, true, &(pConfig->saveWorkspace)))
         return requiredFieldError("SaveWorkspace", pUserErrMsg);
   }
   else
   {
      pConfig->saveWorkspace = defaultConfig.saveWorkspace;
      *pProvidedDefaults = true;
   }

   // extract always save history
   it = dcfFields.find("AlwaysSaveHistory");
   if (it != dcfFields.end())
   {
      if (!interpretYesNoAskValue(it->second, false, &(pConfig->alwaysSaveHistory)))
         return requiredFieldError("AlwaysSaveHistory", pUserErrMsg);
   }
   else
   {
      pConfig->alwaysSaveHistory = defaultConfig.alwaysSaveHistory;
      *pProvidedDefaults = true;
   }

   // extract enable code indexing
   it = dcfFields.find("EnableCodeIndexing");
   if (it != dcfFields.end())
   {
      if (!interpretBoolValue(it->second, &(pConfig->enableCodeIndexing)))
         return requiredFieldError("EnableCodeIndexing", pUserErrMsg);
   }
   else
   {
      pConfig->enableCodeIndexing = defaultConfig.enableCodeIndexing;
      *pProvidedDefaults = true;
   }

   // extract spaces for tab
   it = dcfFields.find("UseSpacesForTab");
   if (it != dcfFields.end())
   {
      if (!interpretBoolValue(it->second, &(pConfig->useSpacesForTab)))
         return requiredFieldError("UseSpacesForTab", pUserErrMsg);
   }
   else
   {
      pConfig->useSpacesForTab = defaultConfig.useSpacesForTab;
      *pProvidedDefaults = true;
   }

   // extract num spaces for tab
   it = dcfFields.find("NumSpacesForTab");
   if (it != dcfFields.end())
   {
      if (!interpretIntValue(it->second, &(pConfig->numSpacesForTab)))
         return requiredFieldError("NumSpacesForTab", pUserErrMsg);
   }
   else
   {
      pConfig->numSpacesForTab = defaultConfig.numSpacesForTab;
      *pProvidedDefaults = true;
   }

   // extract auto append newline
   it = dcfFields.find("AutoAppendNewline");
   if (it != dcfFields.end())
   {
      if (!interpretBoolValue(it->second, &(pConfig->autoAppendNewline)))
         return requiredFieldError("AutoAppendNewline", pUserErrMsg);
   }
   else
   {
      pConfig->autoAppendNewline = false;
   }


   // extract strip trailing whitespace
   it = dcfFields.find("StripTrailingWhitespace");
   if (it != dcfFields.end())
   {
      if (!interpretBoolValue(it->second, &(pConfig->stripTrailingWhitespace)))
         return requiredFieldError("StripTrailingWhitespace", pUserErrMsg);
   }
   else
   {
      pConfig->stripTrailingWhitespace = false;
   }

   it = dcfFields.find("LineEndingConversion");
   if (it != dcfFields.end())
   {
      if (!interpretLineEndingsValue(it->second, &(pConfig->lineEndings)))
         return requiredFieldError("LineEndingConversion", pUserErrMsg);
   }
   else
   {
      pConfig->lineEndings = kLineEndingsUseDefault;
   }


   // extract encoding
   it = dcfFields.find("Encoding");
   if (it != dcfFields.end())
   {
      pConfig->encoding = it->second;
   }
   else
   {
      pConfig->encoding = defaultConfig.encoding;
      *pProvidedDefaults = true;
   }

   // extract default sweave engine
   it = dcfFields.find("RnwWeave");
   if (it != dcfFields.end())
   {
      pConfig->defaultSweaveEngine = it->second;
   }
   else
   {
      pConfig->defaultSweaveEngine = defaultConfig.defaultSweaveEngine;
      *pProvidedDefaults = true;
   }

   // extract default latex program
   it = dcfFields.find("LaTeX");
   if (it != dcfFields.end())
   {
      pConfig->defaultLatexProgram = it->second;
   }
   else
   {
      pConfig->defaultLatexProgram = defaultConfig.defaultLatexProgram;
      *pProvidedDefaults = true;
   }

   // extract root document
   it = dcfFields.find("RootDocument");
   if (it != dcfFields.end())
   {
      pConfig->rootDocument = it->second;
   }
   else
   {
      pConfig->rootDocument = "";
   }

   // extract build type
   it = dcfFields.find("BuildType");
   if (it != dcfFields.end())
   {
      if (!interpretBuildTypeValue(it->second, &(pConfig->buildType)))
         return requiredFieldError("BuildType", pUserErrMsg);
   }
   else
   {
      pConfig->buildType = defaultConfig.buildType;
   }

   // extract package path
   it = dcfFields.find("PackagePath");
   if (it != dcfFields.end())
   {
      pConfig->packagePath = it->second;
   }
   else
   {
      pConfig->packagePath = "";
   }

   // extract package install args
   it = dcfFields.find("PackageInstallArgs");
   if (it != dcfFields.end())
   {
      pConfig->packageInstallArgs = it->second;
   }
   else
   {
      pConfig->packageInstallArgs = "";
   }

   // extract package build args
   it = dcfFields.find("PackageBuildArgs");
   if (it != dcfFields.end())
   {
      pConfig->packageBuildArgs = it->second;
   }
   else
   {
      pConfig->packageBuildArgs = "";
   }

   // extract package build binary args
   it = dcfFields.find("PackageBuildBinaryArgs");
   if (it != dcfFields.end())
   {
      pConfig->packageBuildBinaryArgs = it->second;
   }
   else
   {
      pConfig->packageBuildBinaryArgs = "";
   }

   // extract package check args
   it = dcfFields.find("PackageCheckArgs");
   if (it != dcfFields.end())
   {
      pConfig->packageCheckArgs = it->second;
   }
   else
   {
      pConfig->packageCheckArgs = "";
   }

   // extract package roxygenzize
   it = dcfFields.find("PackageRoxygenize");
   if (it != dcfFields.end())
   {
      pConfig->packageRoxygenize = it->second;
   }
   else
   {
      pConfig->packageRoxygenize = "";
   }

   // extract package use devtools
   it = dcfFields.find("PackageUseDevtools");
   if (it != dcfFields.end())
   {
      if (!interpretBoolValue(it->second, &(pConfig->packageUseDevtools)))
         return requiredFieldError("PackageUseDevtools", pUserErrMsg);
   }
   else
   {
      pConfig->packageUseDevtools = false;
   }

   // extract makefile path
   it = dcfFields.find("MakefilePath");
   if (it != dcfFields.end())
   {
      pConfig->makefilePath = it->second;
   }
   else
   {
      pConfig->makefilePath = "";
   }

   // extract websitepath
   it = dcfFields.find("WebsitePath");
   if (it != dcfFields.end())
   {
      pConfig->websitePath = it->second;
   }
   else
   {
      pConfig->websitePath = "";
   }


   // extract custom script path
   it = dcfFields.find("CustomScriptPath");
   if (it != dcfFields.end())
   {
      pConfig->customScriptPath = it->second;
   }
   else
   {
      pConfig->customScriptPath = "";
   }

   // auto-detect build type if necessary
   if (pConfig->buildType.empty())
   {
      // try to detect the build type
      pConfig->buildType = detectBuildType(projectFilePath,
                                           buildDefaults,
                                           pConfig);

      // set *pProvidedDefaults only if we successfully auto-detected
      // (this will prevent us from writing None into the project file,
      // thus allowing auto-detection to work in the future if the user
      // adds a DESCRIPTION or Makefile
      if (pConfig->buildType != kBuildTypeNone)
         *pProvidedDefaults = true;
   }

   // extract tutorial
   it = dcfFields.find("Tutorial");
   if (it != dcfFields.end())
   {
      pConfig->tutorialPath = it->second;
   }
   else
   {
      pConfig->tutorialPath = "";
   }

   return Success();
}


Error writeProjectFile(const FilePath& projectFilePath,
                       const RProjectBuildDefaults& buildDefaults,
                       const RProjectConfig& config)
{  
   // build version field if necessary
   std::string rVersion;
   if (!config.rVersion.isDefault())
      rVersion = "RVersion: " + rVersionAsString(config.rVersion) + "\n\n";

   // generate project file contents
   boost::format fmt(
      "Version: %1%\n"
      "\n"
      "%2%"
      "RestoreWorkspace: %3%\n"
      "SaveWorkspace: %4%\n"
      "AlwaysSaveHistory: %5%\n"
      "\n"
      "EnableCodeIndexing: %6%\n"
      "UseSpacesForTab: %7%\n"
      "NumSpacesForTab: %8%\n"
      "Encoding: %9%\n"
      "\n"
      "RnwWeave: %10%\n"
      "LaTeX: %11%\n");

   std::string contents = boost::str(fmt %
        boost::io::group(std::fixed, std::setprecision(1), config.version) %
        rVersion %
        yesNoAskValueToString(config.restoreWorkspace) %
        yesNoAskValueToString(config.saveWorkspace) %
        yesNoAskValueToString(config.alwaysSaveHistory) %
        boolValueToString(config.enableCodeIndexing) %
        boolValueToString(config.useSpacesForTab) %
        config.numSpacesForTab %
        config.encoding %
        config.defaultSweaveEngine %
        config.defaultLatexProgram);

   // add root-document if provided
   if (!config.rootDocument.empty())
   {
      boost::format rootDocFmt("RootDocument: %1%\n");
      std::string rootDoc = boost::str(rootDocFmt % config.rootDocument);
      contents.append(rootDoc);
   }

   // additional editor settings
   if (config.autoAppendNewline || config.stripTrailingWhitespace ||
       (config.lineEndings != kLineEndingsUseDefault) )
   {
      contents.append("\n");

      if (config.autoAppendNewline)
      {
         contents.append("AutoAppendNewline: Yes\n");
      }

      if (config.stripTrailingWhitespace)
      {
         contents.append("StripTrailingWhitespace: Yes\n");
      }

      if (config.lineEndings != kLineEndingsUseDefault)
      {
         std::string value;
         switch(config.lineEndings)
         {
            case string_utils::LineEndingPassthrough:
               value = kLineEndingPassthough;
               break;
            case string_utils::LineEndingNative:
               value = kLineEndingNative;
               break;
            case string_utils::LineEndingPosix:
               value = kLineEndingPosix;
               break;
            case string_utils::LineEndingWindows:
               value = kLineEndingWindows;
               break;
         }
         if (!value.empty())
            contents.append("LineEndingConversion: " + value + "\n");
      }
   }

   // add build-specific settings if necessary
   if (!config.buildType.empty())
   {
      // if the build type is None and the detected build type is None
      // then don't write any build type into the file (so that auto-detection
      // has a chance to work in the future if the user turns this project
      // into a package or adds a Makefile)
      if (config.buildType != kBuildTypeNone ||
          detectBuildType(projectFilePath, buildDefaults) != kBuildTypeNone)
      {
         // build type
         boost::format buildFmt("\nBuildType: %1%\n");
         std::string build = boost::str(buildFmt % config.buildType);

         // extra fields
         if (config.buildType == kBuildTypePackage)
         {
            if (config.packageUseDevtools)
            {
               build.append("PackageUseDevtools: Yes\n");
            }

            if (!config.packagePath.empty())
            {
               boost::format pkgFmt("PackagePath: %1%\n");
               build.append(boost::str(pkgFmt % config.packagePath));
            }

            if (!config.packageInstallArgs.empty())
            {
               boost::format pkgFmt("PackageInstallArgs: %1%\n");
               build.append(boost::str(pkgFmt % config.packageInstallArgs));
            }

            if (!config.packageBuildArgs.empty())
            {
               boost::format pkgFmt("PackageBuildArgs: %1%\n");
               build.append(boost::str(pkgFmt % config.packageBuildArgs));
            }

            if (!config.packageBuildBinaryArgs.empty())
            {
               boost::format pkgFmt("PackageBuildBinaryArgs: %1%\n");
               build.append(boost::str(pkgFmt % config.packageBuildBinaryArgs));
            }

            if (!config.packageCheckArgs.empty())
            {
               boost::format pkgFmt("PackageCheckArgs: %1%\n");
               build.append(boost::str(pkgFmt % config.packageCheckArgs));
            }

            if (!config.packageRoxygenize.empty())
            {
               boost::format pkgFmt("PackageRoxygenize: %1%\n");
               build.append(boost::str(pkgFmt % config.packageRoxygenize));
            }

         }
         else if (config.buildType == kBuildTypeMakefile)
         {
            if (!config.makefilePath.empty())
            {
               boost::format makefileFmt("MakefilePath: %1%\n");
               build.append(boost::str(makefileFmt % config.makefilePath));
            }
         }
         else if (config.buildType == kBuildTypeWebsite)
         {
            if (!config.websitePath.empty())
            {
               boost::format websiteFmt("WebsitePath: %1%\n");
               build.append(boost::str(websiteFmt % config.websitePath));
            }
         }
         else if (config.buildType == kBuildTypeCustom)
         {
            boost::format customFmt("CustomScriptPath: %1%\n");
            build.append(boost::str(customFmt % config.customScriptPath));
         }

         // add to contents
         contents.append(build);
      }
   }

   // add Tutorial if it's present
   if (!config.tutorialPath.empty())
   {
      boost::format tutorialFmt("\nTutorial: %1%\n");
      contents.append(boost::str(tutorialFmt % config.tutorialPath));
   }

   // write it
   return writeStringToFile(projectFilePath,
                            contents,
                            string_utils::LineEndingNative);
}

FilePath projectFromDirectory(const FilePath& directoryPath)
{
   // first use simple heuristic of a case sentitive match between
   // directory name and project file name
   std::string dirName = directoryPath.filename();
   if (!FilePath::isRootPath(dirName))
   {
      FilePath projectFile = directoryPath.childPath(dirName + ".Rproj");
      if (projectFile.exists())
         return projectFile;
   }

   // didn't satisfy it with simple check so do scan of directory
   std::vector<FilePath> children;
   Error error = directoryPath.children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }

   // build a vector of children with .rproj extensions. at the same
   // time allow for a case insensitive match with dir name and return that
   std::string projFileLower = string_utils::toLower(dirName);
   std::vector<FilePath> rprojFiles;
   for (std::vector<FilePath>::const_iterator it = children.begin();
        it != children.end();
        ++it)
   {
      if (!it->isDirectory() && (it->extensionLowerCase() == ".rproj"))
      {
         if (string_utils::toLower(it->filename()) == projFileLower)
            return *it;
         else
            rprojFiles.push_back(*it);
      }
   }

   // if we found only one rproj file then return it
   if (rprojFiles.size() == 1)
   {
      return rprojFiles.at(0);
   }
   // more than one, take most recent
   else if (rprojFiles.size() > 1 )
   {
      FilePath projectFile = rprojFiles.at(0);
      for (std::vector<FilePath>::const_iterator it = rprojFiles.begin();
           it != rprojFiles.end();
           ++it)
      {
         if (it->lastWriteTime() > projectFile.lastWriteTime())
            projectFile = *it;
      }

      return projectFile;
   }
   // didn't find one
   else
   {
      return FilePath();
   }
}

bool updateSetPackageInstallArgsDefault(RProjectConfig* pConfig)
{
   if (pConfig->packageInstallArgs == kPackageInstallArgsPreviousDefault)
   {
      pConfig->packageInstallArgs = kPackageInstallArgsDefault;
      return true;
   }
   else
   {
      return false;
   }
}

bool isWebsiteDirectory(const FilePath& projectDir)
{
   // look for an index.Rmd or index.md
   FilePath indexFile = projectDir.childPath("index.Rmd");
   if (!indexFile.exists())
      indexFile = projectDir.childPath("index.md");
   if (indexFile.exists())
   {
      // look for _site.yml
      FilePath siteFile = projectDir.childPath("_site.yml");
      if (siteFile.exists())
      {
         return true;
      }
      // no _site.yml, is there a custom site generator?
      else
      {
         static const boost::regex reSite("^site:.*$");
         std::string yaml = yaml::extractYamlHeader(indexFile);
         return regex_utils::search(yaml.begin(), yaml.end(), reSite);
      }
   }
   else
   {
      return false;
   }
}

} // namespace r_util
} // namespace core 
} // namespace rstudio



