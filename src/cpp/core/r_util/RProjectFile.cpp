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
#include <iomanip>

#include <boost/format.hpp>

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
   *pUserErrMsg = field + " not correctly specified in project config file";
   return systemError(boost::system::errc::protocol_error, ERROR_LOCATION);
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


Error writeProjectFile(const FilePath& projectFilePath,
                       const RProjectConfig& config)
{  
   // generate project file contents
   boost::format fmt(
      "Version: %1%\n"
      "\n"
      "RestoreWorkspace: %2%\n"
      "SaveWorkspace: %3%\n"
      "AlwaysSaveHistory: %4%\n");

   std::string contents = boost::str(fmt %
        boost::io::group(std::fixed, std::setprecision(1), config.version) %
        config.restoreWorkspace %
        config.saveWorkspace %
        config.alwaysSaveHistory);

   // write it
   return writeStringToFile(projectFilePath,
                            contents,
                            string_utils::LineEndingNative);
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



