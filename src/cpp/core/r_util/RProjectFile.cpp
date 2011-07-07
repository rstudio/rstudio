/*
 * RProjectFile.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/r_util/RProjectFile.hpp>

#include <map>

#include <boost/format.hpp>
#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <core/text/DcfParser.hpp>

namespace core {
namespace r_util {

namespace {

Error requiredFieldError(const std::string& field,
                         std::string* pUserErrMsg)
{
   *pUserErrMsg = "Required field '" + field + "' not correctly specified";
   Error error = systemError(boost::system::errc::protocol_error,
                             ERROR_LOCATION);
   error.addProperty("summary", *pUserErrMsg);
   return error;
}

bool interpretYesNoAskValue(const std::string& value,
                            bool acceptAsk,
                            YesNoAskValue* pValue)
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


} // anonymous namespace

Error readProjectFile(const FilePath& projectFilePath,
                      RProjectConfig* pConfig,
                      std::string* pUserErrMsg)
{
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
   if (it == dcfFields.end())
      return requiredFieldError("Version", pUserErrMsg);
   pConfig->version = safe_convert::stringTo<double>(it->second, 0.0);
   if (pConfig->version == 0.0)
      return requiredFieldError("Version", pUserErrMsg);

   // extract id
   it = dcfFields.find("Id");
   if (it == dcfFields.end())
      return requiredFieldError("Id", pUserErrMsg);
   boost::regex guidRegex("^(\\{{0,1}([0-9a-fA-F]){8}-?([0-9a-fA-F]){4}-?([0-9a-fA-F]){4}-?([0-9a-fA-F]){4}-?([0-9a-fA-F]){12}\\}{0,1})$");
   boost::smatch matches;
   if (!regex_match(it->second, matches, guidRegex))
      return requiredFieldError("Id", pUserErrMsg);
   pConfig->id = it->second;

   // extract restore workspace (optional)
   it = dcfFields.find("RestoreWorkspace");
   if (it != dcfFields.end())
   {
      if (!interpretYesNoAskValue(it->second, false, &(pConfig->restoreWorkspace)))
         return requiredFieldError("RestoreWorkspace", pUserErrMsg);
   }
   else
   {
      pConfig->restoreWorkspace = DefaultValue;
   }

   // extract save workspace (optional)
   it = dcfFields.find("SaveWorkspace");
   if (it != dcfFields.end())
   {
      if (!interpretYesNoAskValue(it->second, true, &(pConfig->saveWorkspace)))
         return requiredFieldError("SaveWorkspace", pUserErrMsg);
   }
   else
   {
      pConfig->saveWorkspace = DefaultValue;
   }

   // extract always save history (optional)
   it = dcfFields.find("AlwaysSaveHistory");
   if (it != dcfFields.end())
   {
      if (!interpretYesNoAskValue(it->second, false, &(pConfig->alwaysSaveHistory)))
         return requiredFieldError("AlwaysSaveHistory", pUserErrMsg);
   }
   else
   {
      pConfig->alwaysSaveHistory = DefaultValue;
   }

   return Success();
}


Error writeProjectFile(const std::string& uuid, const FilePath& filePath)
{  
   // generate project file contents
   boost::format fmt(
      "# R project config file\n"
      "# http://www.rstudio.org/docs/r_project/v1.0\n"
      "Version: 1.0\n"
      "Id: %1%\n"
      "\n"
      "# These settings enable per-project workspace save/load behavior\n"
      "# (Default means use the current global preference)\n"
      "RestoreWorkspace: Default\n"
      "SaveWorkspace: Default\n"
      "AlwaysSaveHistory: Default\n");
   std::string contents = boost::str(fmt % uuid);

   // write it
   return writeStringToFile(filePath, contents, string_utils::LineEndingNative);
}

FilePath projectFromDirectory(const FilePath& directoryPath)
{
   // first use simple heuristic of a case sentitive match between
   // directory name and project file name
   FilePath projectFile = directoryPath.childPath(
                                       directoryPath.filename() + ".Rproj");
   if (projectFile.exists())
      return projectFile;

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
   std::string projFileLower = string_utils::toLower(projectFile.filename());
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
      projectFile = rprojFiles.at(0);
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

} // namespace r_util
} // namespace core 



