/*
 * RVersionsScanner.cpp
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

#include <server_core/RVersionsScanner.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/r_util/REnvironment.hpp>
#include <core/r_util/RVersionsPosix.hpp>
#include <core/text/DcfParser.hpp>
#include <core/system/Xdg.hpp>

namespace rstudio {
namespace core {
  
RVersionsScanner::RVersionsScanner() :
   checkCommonRLocations_(true),
   whichROverride_(""),
   rLdScriptPath_(FilePath("")),
   rLdLibraryPath_("")
{
}

RVersionsScanner::RVersionsScanner(bool checkCommonRLocations,
                                   const std::string& whichROverride,
                                   const std::string& rLdScriptPath,
                                   const std::string& rLdLibraryPath) :
   checkCommonRLocations_(checkCommonRLocations),
   whichROverride_(whichROverride),
   rLdScriptPath_(rLdScriptPath),
   rLdLibraryPath_(rLdLibraryPath)
{
}

RVersionsScanner::RVersionsScanner(bool checkCommonRLocations,
                                   const std::string& whichROverride,
                                   const std::string& rLdScriptPath,
                                   const std::string& rLdLibraryPath,
                                   const r_util::RVersion& profileDefaultR,
                                   const std::vector<FilePath>& profileRHomeDirs,
                                   const std::string& modulesBinaryPath) :
   checkCommonRLocations_(checkCommonRLocations),
   whichROverride_(whichROverride),
   rLdScriptPath_(rLdScriptPath),
   rLdLibraryPath_(rLdLibraryPath),
   profileDefaultR_(profileDefaultR),
   profileRHomeDirs_(profileRHomeDirs),
   modulesBinaryPath_(modulesBinaryPath)
{
}

bool RVersionsScanner::detectRVersion(const core::FilePath& rScriptPath,
                                      core::r_util::RVersion* pVersion,
                                      std::string* pErrMsg)
{
   std::string rDetectedScriptPath;
   std::string rVersion;
   core::r_util::EnvironmentVars environment;
   bool result = r_util::detectREnvironment(
                                     rScriptPath,
                                     rLdScriptPath_,
                                     rLdLibraryPath_,
                                     &rDetectedScriptPath,
                                     &rVersion,
                                     &environment,
                                     pErrMsg);
   if (result)
   {
      *pVersion = core::r_util::RVersion(rVersion, environment);
   }

   return result;
}

bool RVersionsScanner::detectSystemRVersion(core::r_util::RVersion* pVersion,
                                            std::string* pErrMsg)
{
   // return cached version if we have it
   if (!systemVersion_.empty())
   {
      *pVersion = systemVersion_;
      return true;
   }

   // check for which R override
   FilePath rWhichRPath;
   if (!whichROverride_.empty())
      rWhichRPath = FilePath(whichROverride_);

   // if it's a directory then see if we can find the script
   if (rWhichRPath.isDirectory())
   {
      FilePath rScriptPath = rWhichRPath.completeChildPath("bin/R");
      if (rScriptPath.exists())
         rWhichRPath = rScriptPath;
   }

   // attempt to detect R version
   bool result = detectRVersion(rWhichRPath, pVersion, pErrMsg);

   // if we detected it then cache it
   if (result)
      systemVersion_ = *pVersion;

   // return result
   return result;
}

void RVersionsScanner::setFallbackVersion()
{
   // use the default profile version if one is specified
   if (!profileDefaultR_.empty())
   {
      systemVersion_ = profileDefaultR_;
   }
   else
   {
      // if not take our most recent available R version.
      if (cachedVersions_.size() > 0)
         systemVersion_ = cachedVersions_[0];
   }
}

std::vector<r_util::RVersion> RVersionsScanner::getRVersions()
{
   if (!cachedVersions_.empty())
      return cachedVersions_;

   // first collect other R home dirs we know about
   std::vector<FilePath> rHomeDirs;

   // see if there is a detectable system R
   std::string errMsg;
   core::r_util::RVersion sysVersion;
   if (detectSystemRVersion(&sysVersion, &errMsg))
      rHomeDirs.push_back(sysVersion.homeDir());

   // copy home dirs referenced in user profiles
   std::copy(profileRHomeDirs_.begin(),
             profileRHomeDirs_.end(),
             std::back_inserter(rHomeDirs));

   // read additional user-specified R home directories
   std::vector<r_util::RVersion> rEntries;
   FilePath userRDirsPath = core::system::xdg::systemConfigFile("r-versions");
   if (userRDirsPath.exists())
   {
      std::string contents;
      Error error = core::readStringFromFile(userRDirsPath, &contents, string_utils::LineEndingPosix);
      if (!error)
      {
         parseRVersionsFile(userRDirsPath, contents, &rHomeDirs, &rEntries);
      }
      else
      {
         LOG_ERROR(error);
      }
   }

   // scan for available R versions
   using namespace r_util;
   std::vector<r_util::RVersion> versions = r_util::enumerateRVersions(
            rHomeDirs,
            rEntries,
            checkCommonRLocations_,
            rLdScriptPath_,
            rLdLibraryPath_,
            modulesBinaryPath_);

   // cache the versions that we just found
   cachedVersions_ = versions;

   // override the system version with the closest match of enumerated versions
   // this allows the system default version to pick up any environment overrides
   // specified in the r versions file
   if (!sysVersion.empty())
   {
      r_util::RVersion overrideVersion = r_util::selectVersion(sysVersion.number(),
                                                               sysVersion.homeDir().getAbsolutePath(),
                                                               "",
                                                               versions);
      systemVersion_ = overrideVersion;
   }
   else
   {
      // there was no system version found - set fallback version
      setFallbackVersion();
   }

   // success!
   return cachedVersions_;
}

void RVersionsScanner::parseRVersionsFile(const FilePath& versionsFile,
                                          const std::string& contents,
                                          std::vector<FilePath> *pRPaths,
                                          std::vector<r_util::RVersion> *pREntries)
{
   // split the file contents by two newlines
   // each entry will be treated as its own dcf file
   // this will allow every R entry to use dcf format
   // which is easy to understand and use
   size_t pos = 0;
   while (pos < contents.size() - 1)
   {
      size_t lastFoundPos = contents.find("\n\n", pos);
      if (lastFoundPos == std::string::npos)
         lastFoundPos = contents.size() - 1;

      std::string rEntry = contents.substr(pos, lastFoundPos - pos);

      // check if the lines are all comments or whitespace
      // if so, then we will skip this entry
      bool skipEntry = true;
      std::vector<std::string> rEntryLines;
      boost::algorithm::split(rEntryLines, rEntry, boost::is_any_of("\n"));
      for (const std::string& line : rEntryLines)
      {
         std::string trimmedLine = string_utils::trimWhitespace(line);

         // check if line is a comment or purely whitespace
         if (trimmedLine.empty() || (trimmedLine.size() > 0 && trimmedLine.at(0) == '#'))
            continue;

         skipEntry = false;
         break;
      }

      if (!skipEntry)
      {
         boost::shared_ptr<r_util::RVersion> pVersion = parseREntry(versionsFile, rEntry);

         if (pVersion)
         {
            // dcf parser was successful, so push back the parsed RVersion
            pREntries->push_back(*pVersion);
         }
         else
         {
            // dcf parse failed, so treat each line as a regular file path (legacy mode)
            for (const std::string& line : rEntryLines)
            {
               if (boost::algorithm::starts_with(line, "#"))
                  continue;

               FilePath dirPath(line);
               if (dirPath.isDirectory())
               {
                  pRPaths->push_back(dirPath);
               }
               else
               {
                  LOG_ERROR_MESSAGE(
                     "R version specified in " + versionsFile.getAbsolutePath() +
                      " does not point to a valid directory: " + line);
               }
            }
         }
      }

      pos = lastFoundPos + 2;
   }
}

boost::shared_ptr<r_util::RVersion> RVersionsScanner::parseREntry(
        const FilePath& versionsFile, const std::string& rEntryStr)
{
   std::map<std::string, std::string> fields;
   std::string err;

   boost::shared_ptr<r_util::RVersion> pVersion;

   Error error = text::parseDcfFile(rEntryStr, true, &fields, &err);
   if (error)
   {
      return pVersion;
   }

   std::string path = string_utils::trimWhitespace(fields["Path"]);
   std::string module = string_utils::trimWhitespace(fields["Module"]);

   FilePath rPath = FilePath(path);

   if (module.empty() && (path.empty() || !rPath.exists()))
   {
      LOG_ERROR_MESSAGE("Invalid R path specified in " + versionsFile.getAbsolutePath() + ": " +
                        path + "This version of R will be skipped");
      return pVersion;
   }

   pVersion.reset(new r_util::RVersion);

   std::string label = string_utils::trimWhitespace(fields["Label"]);
   pVersion->setLabel(label);

   pVersion->setHomeDir(rPath);
   pVersion->setModule(module);

   std::string script = string_utils::trimWhitespace(fields["Script"]);
   pVersion->setPrelaunchScript(script);

   std::string repo = string_utils::trimWhitespace(fields["Repo"]);
   pVersion->setRepo(repo);

   std::string library = string_utils::trimWhitespace(fields["Library"]);
   pVersion->setLibrary(library);

   return pVersion;
}

} // namespace core
} // namespace rstudio


