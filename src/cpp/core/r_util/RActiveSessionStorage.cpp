/*
 * RActiveSessionStorage.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/system/Xdg.hpp>
#include <boost/current_function.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {
Error createError(const std::string& errorName, const std::string& preamble, 
   const std::vector<FilePath>& files, const ErrorLocation& errorLocation)
{
   std::string errorMessage = preamble + "[ ";
   auto iter = files.begin();
   while(iter != files.end())
   {
      errorMessage += "'" + iter->getAbsolutePath() + "'";
      iter++;
      if(iter != files.end())
         errorMessage += ", ";
   }
   errorMessage += " ]";
   return Error(errorName, 1, errorMessage, errorLocation);
}
} // anonymous namespace

FileActiveSessionStorage::FileActiveSessionStorage(const FilePath& scratchPath) :
   scratchPath_ (scratchPath)
{
   Error error = scratchPath_.ensureDirectory();
   if(error)
      LOG_ERROR(error);
}

const std::map<std::string, std::string> FileActiveSessionStorage::fileNames =
   {
      { "last_used" , "last-used" },
      { "r_version" , "r-version" },
      { "r_version_label" , "r-version-label" },
      { "r_version_home" , "r-version-home" },
      { "working_directory" , "working-dir" },
      { "launch_parameters" , "launch-parameters" }
      };

Error FileActiveSessionStorage::readProperty(const std::string& name, std::string* pValue)
{
   std::map<std::string, std::string> propertyValue{};
   *pValue = "";

   Error error = readProperties({ name }, &propertyValue);

   if (error)
      return error;

   std::map<std::string, std::string>::iterator iter = propertyValue.find(name);

   if(iter != propertyValue.end())
      *pValue = iter->second;
   
   return Success();
}

Error FileActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{
   std::vector<FilePath> failedFiles;
   pValues->empty();
   for (const std::string& name : names)
   {
      FilePath readPath = getPropertyFile(name);
      std::string value = "";

      if (readPath.exists())
      {
         Error error = core::readStringFromFile(readPath, &value);
         if (error)
            failedFiles.push_back(readPath);
         boost::algorithm::trim(value);
      }
      pValues->insert(std::pair<std::string, std::string>{name, value});
   }

   if(failedFiles.empty())
      return Success();
   else
      return createError("UnableToReadFiles", "Failed to read from the following files ", 
         failedFiles, ERROR_LOCATION);
}

Error FileActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{
   FilePath propertyDir = getPropertyDir();
   std::vector<FilePath> files{};
   std::vector<FilePath> failedFiles{};
   pValues->empty();
   propertyDir.getChildren(files);

   for(FilePath file : files) {
      std::string value = "";
      Error error = core::readStringFromFile(file, &value);

      if(error)
         failedFiles.push_back(file);

      std::string propertyName = getFileNameProperty(file.getFilename());
      pValues->insert(std::pair<std::string, std::string>{propertyName, value});
   }

   if(failedFiles.empty())
      return Success();
   else
      return createError("UnableToReadFiles", "Failed to read from the following files ",
         failedFiles, ERROR_LOCATION);
}

Error FileActiveSessionStorage::writeProperty(const std::string& name, const std::string& value)
{
   std::map<std::string, std::string> property = {{name, value}};
   return writeProperties(property);
}

Error FileActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
   std::vector<FilePath> failedFiles{};
   for (auto&& prop : properties)
   {
      FilePath writePath = getPropertyFile(prop.first);
      Error error = core::writeStringToFile(writePath, prop.second);
      if (error)
         failedFiles.push_back(writePath);
   }
   
   if (failedFiles.empty())
      return Success();
   else
      return createError("UnableToWriteFiles", "Failed to write to the following files ", 
         failedFiles, ERROR_LOCATION);
}

Error FileActiveSessionStorage::destroy()
{
   return scratchPath_.removeIfExists();
}

Error FileActiveSessionStorage::isEmpty(bool* pValue)
{
   *pValue = (!scratchPath_.exists());
   return Success();
}

Error FileActiveSessionStorage::isValid(bool* pValue)
{
   *pValue = scratchPath_.exists();
   return Success();
}

FilePath FileActiveSessionStorage::getPropertyDir() const
{
   FilePath propertiesDir = scratchPath_.completeChildPath(propertiesDirName_);
   propertiesDir.ensureDirectory();
   return propertiesDir;
}

FilePath FileActiveSessionStorage::getPropertyFile(const std::string& name) const
{
   FilePath propertiesDir = getPropertyDir();
   const std::string& fileName = getPropertyFileName(name);
   return propertiesDir.completeChildPath(fileName);
}

} // namespace r_util
} // namespace core
} // namespace rstudio
